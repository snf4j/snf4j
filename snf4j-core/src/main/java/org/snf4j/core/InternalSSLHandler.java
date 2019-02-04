/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * -----------------------------------------------------------------------------
 */
package org.snf4j.core;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;

import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.future.ITwoThresholdFuture;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.logger.ExceptionLogger;
import org.snf4j.core.logger.IExceptionLogger;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.session.IStreamSession;

class InternalSSLHandler implements IStreamHandler, Runnable {

	private final ILogger logger;
	
	private final IExceptionLogger elogger = ExceptionLogger.getInstance();
	
	private final IStreamHandler handler;
	
	private final SSLEngine engine;
	
	private final IByteBufferAllocator allocator;
	
	private SSLSession session;
	
	private final Object writeLock = new Object();
	
	private volatile ClosingState closing = ClosingState.NONE;
	
	/** Counts total application bytes that was already wrapped */
	private long netCounter;
	
	/** Counts total application bytes that needed wrapping */
	private volatile long appCounter;
	
	/** Tells if the initial handshaking is finished */
	private boolean isReady;
	
	/** Tells if any incoming data is ignored */ 
	private boolean readIgnored;
	
	private final ConcurrentLinkedQueue<ITwoThresholdFuture> pendingFutures = new ConcurrentLinkedQueue<ITwoThresholdFuture>();
	
	private ITwoThresholdFuture polledFuture;
	
	private final int minAppBufferSize;
	
	private final int maxAppBufferSize;
	
	private final int minNetBufferSize;
	
	private final int maxNetBufferSize;
	
	private ByteBuffer[] outAppBuffers;

	private ByteBuffer inAppBuffer;
	
	private ByteBuffer outNetBuffer;

	private ByteBuffer inNetBuffer;
	
	boolean debugEnabled;
	
	boolean traceEnabled;
	
	InternalSSLHandler(IStreamHandler handler, boolean clientMode, ILogger logger) throws Exception {
		this.handler = handler;
		this.logger = logger;
		allocator = handler.getFactory().getAllocator();
		engine = handler.getConfig().createSSLEngine(clientMode);
		minAppBufferSize = engine.getSession().getApplicationBufferSize();
		minNetBufferSize = engine.getSession().getPacketBufferSize();
		maxAppBufferSize = minAppBufferSize * handler.getConfig().getMaxSSLApplicationBufferSizeRatio();
		maxNetBufferSize = minNetBufferSize * handler.getConfig().getMaxSSLNetworkBufferSizeRatio();
	}

	IStreamHandler getHandler() {
		return handler;
	}
	
	/** Method is always running in the same selector loop's thread */
	@SuppressWarnings("incomplete-switch")
	@Override
	public void run() {
		if (closing == ClosingState.FINISHED) {
			return;
		}
		
		boolean running = true;
		HandshakeStatus[] status = new HandshakeStatus[1];
		
		debugEnabled = logger.isDebugEnabled();
		traceEnabled = debugEnabled ? logger.isTraceEnabled() : false;
		
		do {
			
			if (closing != ClosingState.NONE) {
				handleClosing();
			}
			
			if (status[0] == null) {
				status[0] = engine.getHandshakeStatus();
			}
			switch (status[0]) {
			case NOT_HANDSHAKING:	
				running = false;
				if (inNetBuffer.position() != 0) {
					running |= unwrap(status);
				}				
				if (appCounter > netCounter || !isReady || closing == ClosingState.SENDING) {
					running |= wrap(status);
				}
				break;
				
			case NEED_WRAP:
				running = wrap(status);
				break;
				
			case NEED_UNWRAP:
				running = unwrap(status);
				break;
				
			case NEED_TASK:
				Runnable task = engine.getDelegatedTask();
				
				while (task != null) {
					session.loop.getThreadFactory().newThread(new DelegatedTask(this, task)).start();
					task = engine.getDelegatedTask();
				}
				running = false;
				break;
			}
			
			if (status[0] == HandshakeStatus.FINISHED) {
				if (!isReady) {
					if (debugEnabled) {
						logger.debug("Initial handshaking is finished for {}", session);
					}
					isReady = true;
					fireReady();
				}
				status[0] = null;
			}
			
		} while (running);
	}
	
	final void handleClosing() {
		ClosingState closing = this.closing;
		
		if (closing == ClosingState.FINISHING) {
			if (!engine.isOutboundDone()) {
				synchronized (writeLock) {
					for (int i=outAppBuffers.length-1; i>=0; --i) {
						outAppBuffers[i].clear();
					}
					engine.closeOutbound();
				}
			}
		}
	}
	
	final void handleClosed() {
		ClosingState prevClosing;
		
		synchronized (writeLock) {
			prevClosing = closing;
			closing = ClosingState.FINISHED;
		}
		if (!engine.isInboundDone() && !engine.isOutboundDone()) {
			try {
				engine.closeInbound();
			}
			catch (SSLException e) {
				if (prevClosing == ClosingState.NONE && !session.wasException()) {
					if (!session.incident(SessionIncident.SSL_CLOSED_WITHOUT_CLOSE_NOTIFY, e)) {
						elogger.warn(logger, 
								"SSL/TLS close procedure not properly followed by peer for {}: {}", session, e);
					}
				}
			}
		}
		if (!engine.isOutboundDone()) {
			engine.closeOutbound();
		}
	}
	
	
	/** Returns false when the processing loop need to be broken */
	boolean unwrap(HandshakeStatus[] status) {
		if (debugEnabled) {
			logger.debug("Unwrapping started for {}", session);
		}
		
		if (inNetBuffer.position() == 0) {
			if (traceEnabled) {
				logger.trace("No data to unwrap in {}", session);
			}
			return false;
		}
		
		SSLEngineResult unwrapResult;
		boolean repeat;
		
		do {
			repeat = false;

			inNetBuffer.flip();
			try {
				unwrapResult = engine.unwrap(inNetBuffer, inAppBuffer);
				status[0] = unwrapResult.getHandshakeStatus();
				if (traceEnabled) {
					logger.trace(
							"Unwrapping consumed {} byte(s) to produce {} byte(s) for {}",
							unwrapResult.bytesConsumed(),
							unwrapResult.bytesProduced(),
							session);
				}
			} catch (Exception e) {
				elogger.error(logger, "Unwrapping failed for {}: {}", session, e);
				fireException(e);
				return false;
			}
			finally {
				inNetBuffer.compact();
			}

			switch (unwrapResult.getStatus()) {
				case OK:
					if (inAppBuffer.position() != 0) {
						StreamSession.consumeBuffer(inAppBuffer, handler);
					}
					break;

				case BUFFER_OVERFLOW:
					if (debugEnabled) {
						logger.debug("Unwrapping overflow, input application buffer need resizing for {}", session);
					}
					try {
						inAppBuffer = allocator.ensure(inAppBuffer, minAppBufferSize, minAppBufferSize, maxAppBufferSize);
					}
					catch (Exception e) {
						elogger.error(logger, "Unwrapping overflow failed for {}: {}", session, e);
						fireException(e);
						return false;
					}
					repeat = true;
					break;

				case BUFFER_UNDERFLOW:
					if (traceEnabled) {
						logger.debug("Unwrapping underflow, more data needed for {}", session);
					}
					return false;

				case CLOSED:
					readIgnored = true;
					if (debugEnabled) {
						logger.debug("Unwrapping has been closed for {}", session);
					}
					return false;
			}
		} while (repeat);
		
		return true;
	}

	/** Returns false when the processing loop need to be broken */
	@SuppressWarnings("incomplete-switch")
	boolean wrap(HandshakeStatus[] status) {
		if (debugEnabled) {
			logger.debug("Wrapping started for {}", session);
		}
		
		SSLEngineResult wrapResult;
		boolean repeat;
		Exception ex = null;
		
		do {
			repeat = false;
			
			synchronized (writeLock) {
				int lastIndex = outAppBuffers.length - 1;

				outAppBuffers[lastIndex].flip();
				if (lastIndex > 0 || outAppBuffers[lastIndex].hasRemaining()) {
					int consumed;
					try {
						wrapResult = engine.wrap(outAppBuffers, outNetBuffer);
						consumed = wrapResult.bytesConsumed();
					} catch (Exception e) {
						wrapResult = null;
						ex = e;
						consumed = 0;
					}
					if (consumed != 0) {
						outAppBuffers = StreamSession.compactBuffers(outAppBuffers, allocator, minAppBufferSize);
						netCounter += consumed;
						if (outAppBuffers.length == 1 && outAppBuffers[0].position() == 0) {
							if (closing == ClosingState.SENDING) {
								closing = ClosingState.FINISHING;
								engine.closeOutbound();
								repeat = true;
							}
						}
					}
					else {
						outAppBuffers[lastIndex].compact();
					}
				}
				else {
					if (closing == ClosingState.SENDING) {
						closing = ClosingState.FINISHING;
						engine.closeOutbound();
					}
					try {
						wrapResult = engine.wrap(outAppBuffers[lastIndex], outNetBuffer);
					} catch (Exception e) {
						wrapResult = null;
						ex = e;
					}
					outAppBuffers[lastIndex].clear();
				}
			}

			if (wrapResult == null) {
				elogger.error(logger, "Wrapping failed for {}: {}", session, ex);
				fireException(ex);
				return false;
			}

			status[0] = wrapResult.getHandshakeStatus();
			if (traceEnabled) {
				logger.trace(
						"Wrapping consumed {} byte(s) to produce {} byte(s) for {}",
						wrapResult.bytesConsumed(),
						wrapResult.bytesProduced(),
						session);
			}

			switch (wrapResult.getStatus()) {
				case OK:
					flush();
					break;

				case BUFFER_OVERFLOW:
					if (debugEnabled) {
						logger.debug("Wrapping overflow, output packet buffer need resizing for {}", session);
					}
					try {
						outNetBuffer = allocator.ensure(outNetBuffer, minNetBufferSize, minNetBufferSize, maxNetBufferSize);
					}
					catch (Exception e) {
						elogger.error(logger, "Wrapping overflow failed for {}: {}", session, e);
						fireException(e);
						return false;
					}
					repeat = true;
					break;

				case CLOSED:
					if (debugEnabled) {
						logger.debug("Wrapping has been closed for {}", session);
					}
					flush();
					session.close(false);
					break;
			}
		} while (repeat);
		
		return true;
	}	
	
	private final void flush() {
		int position = outNetBuffer.position();

		if (position != 0) {
			boolean skipUpdate = false;
			outNetBuffer.flip();
			long futureThreshold = session.write0(outNetBuffer);
			outNetBuffer.compact();

			//update futures
			if (polledFuture != null) {
				if (polledFuture.getFirstThreshold() <= netCounter) {
					polledFuture.setSecondThreshold(futureThreshold);
				}
				else {
					skipUpdate = true;
				}
			}
			if (!skipUpdate) {
				while ((polledFuture = pendingFutures.poll()) != null && polledFuture.getFirstThreshold() <= netCounter) {
					polledFuture.setSecondThreshold(futureThreshold);
				}
			}
		}
	}
	
	private final void fireReady() {
		if (debugEnabled) {
			logger.debug("Firing event {} for {}", EventType.SESSION_READY, session);
		}
		session.event(SessionEvent.READY);
		if (traceEnabled) {
			logger.trace("Ending event {} for {}", EventType.SESSION_READY, session);
		}
	}
	
	private final void fireException(Throwable t) {
		if (debugEnabled) {
			logger.debug("Firing event {} for {}", EventType.EXCEPTION_CAUGHT, session);
		}
		session.exception(t);
		if (traceEnabled) {
			logger.trace("Ending event {} for {}", EventType.EXCEPTION_CAUGHT, session);
		}
	}
	
	IFuture<Void> write(byte[] data, int offset, int length, boolean needFuture) {
		IFuture<Void> future;
		
		synchronized (writeLock) {
			if (closing != ClosingState.NONE) {
				if (needFuture) {
					return session.futuresController.getCancelledFuture();
				}
				return null;
			}
			outAppBuffers = StreamSession.putToBuffers(outAppBuffers, allocator, minAppBufferSize, data, offset, length, false);
			appCounter += length;
			if (needFuture) {
				future = session.futuresController.getSSLWriteFuture(appCounter);
				pendingFutures.add((ITwoThresholdFuture) future);
			}
			else {
				future = null;
			}
		}
		session.loop.registerTask(this);
		return future;
	}	

	IFuture<Void> write(ByteBuffer data, int length, boolean needFuture) {
		IFuture<Void> future;
		
		synchronized (writeLock) {
			if (closing != ClosingState.NONE) {
				if (needFuture) {
					return session.futuresController.getCancelledFuture();
				}
				return null;
			}
			outAppBuffers = StreamSession.putToBuffers(outAppBuffers, allocator, minAppBufferSize, data, 0, length, true);
			appCounter += length;
			if (needFuture) {
				future = session.futuresController.getSSLWriteFuture(appCounter);
				pendingFutures.add((ITwoThresholdFuture) future);
			}
			else {
				future = null;
			}
		}
		session.loop.registerTask(this);
		return future;
	}	
	
	void quickClose() {
		boolean stateChanged = false;
		
		synchronized (writeLock) {
			if (closing == ClosingState.NONE || closing == ClosingState.SENDING) {
				stateChanged = true;
				closing = ClosingState.FINISHING;
			}
		}
		if (stateChanged) {
			session.loop.registerTask(this);
		}
	}
	
	void close() {
		boolean stateChanged = false;

		synchronized (writeLock) {
			if (closing == ClosingState.NONE) {
				stateChanged = true;
				closing = ClosingState.SENDING;
			}
		}		
		if (stateChanged) {
			session.loop.registerTask(this);
		}
	}
	
	void dirtyClose() {
		boolean stateChanged = false;

		synchronized (writeLock) {
			if (closing == ClosingState.NONE || closing == ClosingState.SENDING) {
				stateChanged = true;
				closing = ClosingState.FINISHING;
			}
		}
		if (stateChanged) {
			session.superQuickClose();
			session.loop.registerTask(this);
		}
	}
	
	void preCreated() {
		outAppBuffers = new ByteBuffer[] {allocator.allocate(minAppBufferSize)};
		inAppBuffer = allocator.allocate(minAppBufferSize);
		outNetBuffer = allocator.allocate(minNetBufferSize);
		inNetBuffer = allocator.allocate(minNetBufferSize);
	}
	
	void postEnding() {
		if (allocator.isReleasable()) {
			for (int i=outAppBuffers.length-1;i>=0; --i) {
				allocator.release(outAppBuffers[i]);
				outAppBuffers[i] = null;
			}
			allocator.release(inAppBuffer);
			allocator.release(outNetBuffer);
			allocator.release(inNetBuffer);
			outAppBuffers = null;
			inAppBuffer = null;
			outNetBuffer = null;
			inNetBuffer = null;
		}
	}
	
	@Override
	public String getName() {
		return handler.getName();
	}

	@Override
	public void read(byte[] data) {
		if (readIgnored) {
			return;
		}
		if (data.length > inNetBuffer.remaining()) {
			try {
				inNetBuffer = allocator.ensure(inNetBuffer, data.length, minNetBufferSize, maxNetBufferSize);
			}
			catch (Exception e) {
				elogger.error(logger, "Reading failed for {}: {}", session, e);
				fireException(e);
				return;
			}
		}
		inNetBuffer.put(data);
		run();	
	}

	@Override
	public void event(SessionEvent event) {
		if (event == SessionEvent.CLOSED) {
			handleClosed();
		}
		handler.event(event);
		if (event == SessionEvent.OPENED) {
			run();
		}
	}	

	@Override
	public void event(DataEvent event, long length) {
		handler.event(event, length);
	}

	@Override
	public void exception(Throwable t) {
		handler.exception(t);
	}

	@Override
	public boolean incident(SessionIncident incident, Throwable t) {
		return handler.incident(incident, t);
	}

	@Override
	public ISessionStructureFactory getFactory() {
		return handler.getFactory();
	}

	@Override
	public ISessionConfig getConfig() {
		return handler.getConfig();
	}

	@Override
	public void setSession(IStreamSession session) {
		handler.setSession(session);
		this.session = (SSLSession) session;
	}

	@Override
	public void setSession(ISession session) {
		handler.setSession(session);
		this.session = (SSLSession) session;
	}

	@Override
	public IStreamSession getSession() {
		return session;
	}

	@Override
	public int toRead(ByteBuffer buffer, boolean flipped) {
		int maxLen = maxNetBufferSize - inNetBuffer.position();
		int len = flipped ? buffer.remaining() : buffer.position();
		
		if (maxLen >= len || maxLen == 0) {
			return len;
		}
		return maxLen;
	}

	@Override
	public int toRead(byte[] buffer, int off, int len) {
		int maxLen = maxNetBufferSize - inNetBuffer.position();

		if (maxLen >= len || maxLen == 0) {
			return len;
		}
		return maxLen;
	}

	private static class DelegatedTask implements Runnable {
		
		private final Runnable delegate;
		
		private final InternalSSLHandler handler;
		
		DelegatedTask(InternalSSLHandler handler, Runnable delegate) {
			this.handler = handler;
			this.delegate = delegate;
		}

		@Override
		public void run() {
			delegate.run();
			handler.session.loop.registerTask(handler);
		}
	}
}
