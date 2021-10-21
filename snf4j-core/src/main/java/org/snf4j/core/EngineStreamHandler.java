/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020-2021 SNF4J contributors
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
import java.util.concurrent.Executor;

import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.core.engine.IEngine;
import org.snf4j.core.engine.IEngineResult;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.future.ITwoThresholdFuture;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.IStreamSession;

class EngineStreamHandler extends AbstractEngineHandler<EngineStreamSession, IStreamHandler> implements IStreamHandler {
	
	private final static ByteBuffer[] EMPTY_ARRAY = new ByteBuffer[0];
	
	private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.wrap(new byte[0]);
	
	private final ConcurrentLinkedQueue<ITwoThresholdFuture<Void>> pendingFutures = new ConcurrentLinkedQueue<ITwoThresholdFuture<Void>>();
	
	private final ByteBuffer[] DEFAULT_ARRAY = new ByteBuffer[1];
	
	private ITwoThresholdFuture<Void> polledFuture;
	
	private ByteBuffer[] outAppBuffers;

	private ByteBuffer inAppBuffer;
	
	private ByteBuffer outNetBuffer;

	private ByteBuffer inNetBuffer;
	
	private int maxAppBufferSize;
	
	private int maxNetBufferSize;
	
	public EngineStreamHandler(IEngine engine, IStreamHandler handler, ILogger logger) {
		super(engine, handler, logger);
	}

	IStreamHandler getHandler() {
		return handler;
	}
	
	ByteBuffer getInNetBuffer() {
		return inNetBuffer;
	}
	
	@Override
	final boolean handleClosing() {
		ClosingState closing = this.closing;
		
		if (closing == ClosingState.FINISHING) {
			if (!engine.isOutboundDone()) {
				synchronized (writeLock) {
					for (int i=outAppBuffers.length-1; i>=0; --i) {
						outAppBuffers[i].clear();
					}
					closeOutbound();
					return true;
				}
			}
			isReadyPending = false;
		}
		return closing == ClosingState.SENDING;
	}
	
	final void tryReleaseInAppBuffer() {
		if (session.optimizeBuffers && inAppBuffer.position() == 0) {
			allocator.release(inAppBuffer);
			inAppBuffer = null;
		}
	}
	
	@Override
	boolean unwrap(HandshakeStatus[] status) {
		if (traceEnabled) {
			logger.trace("Unwrapping started for {}", session);
		}
		
		IEngineResult unwrapResult;
		boolean repeat;
		
		do {
			repeat = false;

			if (inNetBuffer != null) {
				inNetBuffer.flip();
			}
			try {
				if (inAppBuffer == null) {
					inAppBuffer = allocator.allocate(minAppBufferSize);
				}
				unwrapResult = engine.unwrap(inNetBuffer == null ? EMPTY_BUFFER : inNetBuffer, inAppBuffer);
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
				tryReleaseInAppBuffer();
				return false;
			}
			finally {
				if (inNetBuffer != null) {
					if (inNetBuffer.hasRemaining()) {
						inNetBuffer.compact();
					}
					else if (session.optimizeBuffers) {
						allocator.release(inNetBuffer);
						inNetBuffer = null;
					}
					else {
						inNetBuffer.clear();
					}
				}
			}

			switch (unwrapResult.getStatus()) {
				case OK:
					if (inAppBuffer.position() != 0) {
						IStreamReader reader = session.codec != null ? session.codec : handler;
						
						try {
							if (session.optimizeBuffers) {
								inAppBuffer = StreamSession.consumeBuffer(inAppBuffer, reader, allocator, session.consumeController);
							}
							else {
								StreamSession.consumeBuffer(inAppBuffer, reader, session.consumeController);
							}
						}
						catch (PipelineDecodeException e) {
							inAppBuffer.compact();
							elogger.error(logger, SessionIncident.DECODING_PIPELINE_FAILURE.defaultMessage(), session, e.getCause());
							fireException(e.getCause());
							return false;
						}
						catch (Exception e) {
							inAppBuffer.compact();
							elogger.error(logger, "Reading from input application buffer failed for {}: {}", session, e);
							fireException(e);
							return false;
						}
					}
					else if (session.optimizeBuffers) {
						allocator.release(inAppBuffer);
						inAppBuffer = null;
					}
					break;

				case BUFFER_OVERFLOW:
					if (debugEnabled) {
						logger.debug("Unwrapping overflow, input application buffer need resizing for {}", session);
					}
					try {
						minAppBufferSize = engine.getMinApplicationBufferSize();
						maxAppBufferSize = engine.getMaxApplicationBufferSize();	
						inAppBuffer = allocator.ensure(inAppBuffer, minAppBufferSize, minAppBufferSize, maxAppBufferSize);
					}
					catch (Exception e) {
						elogger.error(logger, "Unwrapping overflow failed for {}: {}", session, e);
						fireException(e);
						tryReleaseInAppBuffer();
						return false;
					}
					repeat = true;
					break;

				case BUFFER_UNDERFLOW:
					if (traceEnabled) {
						logger.debug("Unwrapping underflow, more data needed for {}", session);
					}
					tryReleaseInAppBuffer();
					return false;

				case CLOSED:
					readIgnored = true;
					if (debugEnabled) {
						logger.debug("Unwrapping has been closed for {}", session);
					}
					if (!engine.isOutboundDone()) {
						synchronized (writeLock) {
							if (closing == ClosingState.NONE) {
								closing = ClosingState.SENDING;
							}
						}
						tryReleaseInAppBuffer();
						return true;
					}
					else {
						superClose();
					}
					tryReleaseInAppBuffer();
					return false;
			}
		} while (repeat);
		
		return true;
	}

	final void tryReleaseOutNetBuffer() {
		if (session.optimizeBuffers && outNetBuffer.position() == 0) {
			allocator.release(outNetBuffer);
			outNetBuffer = null;
		}
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override
	boolean wrap(HandshakeStatus[] status) {
		if (traceEnabled) {
			logger.trace("Wrapping started for {}", session);
		}
		
		IEngineResult wrapResult;
		boolean repeat;
		Exception ex = null;
		
		do {
			repeat = false;
			
			if (outNetBuffer == null) {
				outNetBuffer = allocator.allocate(minNetBufferSize);
			}
			synchronized (writeLock) {
				int lastIndex = outAppBuffers.length - 1;

				if (lastIndex >= 0) {
					outAppBuffers[lastIndex].flip();
				}
				if (lastIndex != -1 && (lastIndex > 0 || outAppBuffers[lastIndex].hasRemaining())) {
					int consumed;
					try {
						wrapResult = lastIndex == 0 ? engine.wrap(outAppBuffers[0], outNetBuffer) 
								                    : engine.wrap(outAppBuffers, outNetBuffer);
						consumed = wrapResult.bytesConsumed();
					} catch (Exception e) {
						wrapResult = null;
						ex = e;
						consumed = 0;
					}
					if (consumed != 0) {
						outAppBuffers = StreamSession.compactBuffers(outAppBuffers, allocator, minAppBufferSize, session.optimizeBuffers);
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
					if (closing == ClosingState.SENDING || engine.isInboundDone()) {
						closing = ClosingState.FINISHING;
						closeOutbound();
					}
					if (lastIndex >= 0) {
						try {
							wrapResult = engine.wrap(outAppBuffers[lastIndex], outNetBuffer);
						} catch (Exception e) {
							wrapResult = null;
							ex = e;
						}
						outAppBuffers[lastIndex].clear();
					}
					else {
						try {
							wrapResult = engine.wrap(EMPTY_BUFFER, outNetBuffer);
						} catch (Exception e) {
							wrapResult = null;
							ex = e;
						}
					}
				}
			}

			if (wrapResult == null) {
				elogger.error(logger, "Wrapping failed for {}: {}", session, ex);
				fireException(ex);
				tryReleaseOutNetBuffer();
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
						minNetBufferSize = engine.getMinNetworkBufferSize();
						maxNetBufferSize = engine.getMaxNetworkBufferSize();	
						outNetBuffer = allocator.ensure(outNetBuffer, minNetBufferSize, minNetBufferSize, maxNetBufferSize);
					}
					catch (Exception e) {
						elogger.error(logger, "Wrapping overflow failed for {}: {}", session, e);
						fireException(e);
						tryReleaseOutNetBuffer();
						return false;
					}
					repeat = true;
					break;

				case CLOSED:
					if (debugEnabled) {
						logger.debug("Wrapping has been closed for {}", session);
					}
					flush();
					if (handler.getConfig().waitForInboundCloseMessage() && !engine.isInboundDone()) {
						return true;
					}
					session.close(true);
					break;
			}
		} while (repeat);

		return true;
	}	
	
	@Override
	final Executor getExecutor() {
		return session.getExecutor();
	}
	
	@Override
	final boolean needUnwrap() {
		return inNetBuffer != null && inNetBuffer.position() != 0;
	}
	
	@Override
	final void superQuickClose() {
		session.superQuickClose();		
	}
	
	@Override
	final void superClose() {
		session.superClose();
	}
	
	private final void flush() {
		int position = outNetBuffer.position();

		if (position != 0) {
			boolean skipUpdate = false;
			outNetBuffer.flip();
			long futureThreshold = session.write0(outNetBuffer);
			
			if (session.optimizeBuffers) {
				outNetBuffer = null;
			}
			else {
				outNetBuffer.compact();
			}
			
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
		else if (session.optimizeBuffers) {
			allocator.release(outNetBuffer);
			outNetBuffer = null;
		}
	}
	
	IFuture<Void> write(byte[] data, int offset, int length, boolean needFuture) {
		ITwoThresholdFuture<Void> future;
		
		synchronized (writeLock) {
			if (closing != ClosingState.NONE) {
				if (needFuture) {
					return session.futuresController.getCancelledFuture();
				}
				return null;
			}
			if (outAppBuffers.length == 0) {
				outAppBuffers = DEFAULT_ARRAY;
				outAppBuffers[0] = allocator.allocate(minAppBufferSize);
			}
			outAppBuffers = StreamSession.putToBuffers(outAppBuffers, allocator, minAppBufferSize, data, offset, length, false);
			appCounter += length;
			if (needFuture) {
				future = session.futuresController.getEngineWriteFuture(appCounter);
				pendingFutures.add(future);
			}
			else {
				future = null;
			}
		}
		session.loop.executenf(this);
		return future;
	}	

	IFuture<Void> write(ByteBuffer data, int length, boolean needFuture) {
		ITwoThresholdFuture<Void> future;
		
		synchronized (writeLock) {
			if (closing != ClosingState.NONE) {
				if (needFuture) {
					return session.futuresController.getCancelledFuture();
				}
				return null;
			}
			
			boolean optimize = session.optimizeBuffers;
			
			if (optimize && data.remaining() == length) {
				if (outAppBuffers.length == 0) {
					outAppBuffers = DEFAULT_ARRAY;
					outAppBuffers[0] = null;
				}
				outAppBuffers = StreamSession.putToBuffers(outAppBuffers, allocator, data);				
			}
			else {
				if (outAppBuffers.length == 0) {
					outAppBuffers = DEFAULT_ARRAY;
					outAppBuffers[0] = allocator.allocate(minAppBufferSize);
				}
				outAppBuffers = StreamSession.putToBuffers(outAppBuffers, allocator, minAppBufferSize, data, 0, length, true);
			}
			appCounter += length;
			if (needFuture) {
				future = session.futuresController.getEngineWriteFuture(appCounter);
				pendingFutures.add(future);
			}
			else {
				future = null;
			}
		}
		session.loop.executenf(this);
		return future;
	}	
	
	@Override
	void preCreated() {
		super.preCreated();
		maxAppBufferSize = engine.getMaxApplicationBufferSize();
		maxNetBufferSize = engine.getMaxNetworkBufferSize();
		if (!session.optimizeBuffers) {
			outAppBuffers = DEFAULT_ARRAY;
			outAppBuffers[0] = allocator.allocate(minAppBufferSize);
			inAppBuffer = allocator.allocate(minAppBufferSize);
			outNetBuffer = allocator.allocate(minNetBufferSize);
			inNetBuffer = allocator.allocate(minNetBufferSize);
		}
		else {
			outAppBuffers = EMPTY_ARRAY;
		}
	}
	
	void postEnding() {
		super.postEnding();
		if (allocator.isReleasable()) {
			for (int i=outAppBuffers.length-1;i>=0; --i) {
				allocator.release(outAppBuffers[i]);
				outAppBuffers[i] = null;
			}
			outAppBuffers = EMPTY_ARRAY;
			if (inAppBuffer != null) {
				allocator.release(inAppBuffer);
				inAppBuffer = null;
			}
			if (outNetBuffer != null) {
				allocator.release(outNetBuffer);
				outNetBuffer = null;
			}
			if (inNetBuffer != null) {
				allocator.release(inNetBuffer);
				inNetBuffer = null;
			}
		}
	}
	private final boolean ensure(int size) {
		if (size > inNetBuffer.remaining()) {
			try {
				minNetBufferSize = engine.getMinNetworkBufferSize();
				maxNetBufferSize = engine.getMaxNetworkBufferSize();		
				inNetBuffer = allocator.ensure(inNetBuffer, size, minNetBufferSize, maxNetBufferSize);
			}
			catch (Exception e) {
				elogger.error(logger, "Reading failed for {}: {}", session, e);
				fireException(e);
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void read(byte[] data) {
		if (readIgnored) {
			return;
		}
		if (ensure(data.length)) {
			inNetBuffer.put(data);
			run();	
		}
	}

	@Override
	public void read(ByteBuffer data) {
		if (readIgnored) {
			allocator.release(data);
			return;
		}
		if (inNetBuffer == null) {
			inNetBuffer = data;
			data.position(data.limit());
			data.limit(data.capacity());
			run();
		}
		else if (inNetBuffer.position() == 0) {
			allocator.release(inNetBuffer);
			inNetBuffer = data;
			data.position(data.limit());
			data.limit(data.capacity());
			run();
		}
		else if (ensure(data.remaining())) {
			inNetBuffer.put(data);
			allocator.release(data);
			run();	
		}
	}

	@Override
	public void setSession(ISession session) {
		handler.setSession(session);
		this.session = (EngineStreamSession) session;
	}

	@Override
	public IStreamSession getSession() {
		return session;
	}

	@Override
	public int available(ByteBuffer buffer, boolean flipped) {
		int maxLen = maxNetBufferSize;
		int len = flipped ? buffer.remaining() : buffer.position();
		
		if (inNetBuffer != null) {
			maxLen -= inNetBuffer.position();
		}
		if (maxLen >= len || maxLen == 0) {
			return len;
		}
		return maxLen;
	}

	@Override
	public int available(byte[] buffer, int off, int len) {
		int maxLen = maxNetBufferSize - inNetBuffer.position();

		if (maxLen >= len || maxLen == 0) {
			return len;
		}
		return maxLen;
	}

}
