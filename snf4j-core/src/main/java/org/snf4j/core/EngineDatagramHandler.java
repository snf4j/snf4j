/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020-2024 SNF4J contributors
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

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executor;

import org.snf4j.core.EngineDatagramWrapper.EngineDatagramRecord;
import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.core.engine.IEngine;
import org.snf4j.core.engine.IEngineResult;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.future.ITwoThresholdFuture;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.session.IDatagramSession;
import org.snf4j.core.session.IEngineSession;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.ISessionTimer;
import org.snf4j.core.timer.DefaultTimeoutModel;
import org.snf4j.core.timer.ITimeoutModel;
import org.snf4j.core.timer.ITimerTask;

class EngineDatagramHandler extends AbstractEngineHandler<DatagramSession, IDatagramHandler> implements IDatagramHandler {
	
	private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.wrap(new byte[0]);
	
	private Queue<EngineDatagramRecord> outAppBuffers;
	
	private ByteBuffer inAppBuffer;
	
	private Queue<ByteBuffer> inNetBuffers;

	private final SocketAddress remoteAddress;
	
	private ITimerTask retransmissionTimer;

	private final ITimeoutModel timeoutModel;
	
	private static final Object RETRANSMISSION_TIMEOUT_EVENT = new Object();
	
	private static final boolean[] RETRANSMISSION_RESET = new boolean[HandshakeStatus.values().length];
	
	static {
		RETRANSMISSION_RESET[HandshakeStatus.NEED_WRAP.ordinal()] = true;
		RETRANSMISSION_RESET[HandshakeStatus.NEED_TASK.ordinal()] = true;
		RETRANSMISSION_RESET[HandshakeStatus.FINISHED.ordinal()] = true;
		RETRANSMISSION_RESET[HandshakeStatus.NOT_HANDSHAKING.ordinal()] = true;
	}
	
	EngineDatagramHandler(IEngine engine, SocketAddress remoteAddress, IDatagramHandler handler, ILogger logger) {
		super(engine, handler, logger);
		this.remoteAddress = remoteAddress;
		ITimeoutModel model = handler.getFactory().getTimeoutModel();
		timeoutModel = model != null ? model : new DefaultTimeoutModel();
	}

	private final void scheduleRetransmission() {
		ISessionTimer timer = session.getTimer();
		
		if (retransmissionTimer == null && timer.isSupported()) {
			long timeout = timeoutModel.next();

			retransmissionTimer = timer.scheduleEvent(RETRANSMISSION_TIMEOUT_EVENT, timeout);
			if (traceEnabled) {
				logger.trace("Retransmission timer scheduled for execution after {} ms for {}", timeout, session);
			}
		}
	}

	private final void cancelRetransmissionTimer() {
		if (retransmissionTimer != null) {
			retransmissionTimer.cancelTask();
			retransmissionTimer = null;
			if (traceEnabled) {
				logger.trace("Retransmission timer canceled for {}", session);
			}
		}
	}
	
	@Override
	void handleClosed() {
		cancelRetransmissionTimer();
		super.handleClosed();
	}
	
	@Override
	public void timer(Object event) {
		if (event == RETRANSMISSION_TIMEOUT_EVENT) {
			retransmissionTimer = null;
			run(new HandshakeStatus[] {HandshakeStatus.NEED_WRAP});
		}
		else {
			super.timer(event);
		}
	}
	
	@Override
	public void read(byte[] datagram) {
		if (readIgnored) {
			return;
		}
		inNetBuffers.add(ByteBuffer.wrap(datagram));
		run();
	}

	@Override
	public void read(ByteBuffer datagram) {
		if (readIgnored) {
			if (allocator.isReleasable()) {
				allocator.release(datagram);
			}
			return;
		}
		inNetBuffers.add(datagram);
		run();
	}
	
	@Override
	public void read(SocketAddress remoteAddress, byte[] datagram) {
		if (remoteAddress.equals(this.remoteAddress)) {
			read(datagram);
		}
		else {
			(session.codec != null ? session.codec : handler).read(remoteAddress, datagram);
		}
	}
	
	@Override
	public void read(SocketAddress remoteAddress, ByteBuffer datagram) {
		if (remoteAddress.equals(this.remoteAddress)) {
			read(datagram);
		}
		else {
			(session.codec != null ? session.codec : handler).read(remoteAddress, datagram);
		}
	}

	@Override
	boolean handleClosing() {
		ClosingState closing = this.closing;
		
		if (closing == ClosingState.FINISHING) {
			if (!engine.isOutboundDone()) {
				synchronized (writeLock) {
					releaseBuffers(outAppBuffers);
					closeOutbound();
					return true;
				}
			}			
			isReadyPending = false;
		}
		return closing == ClosingState.SENDING;
	}

	private final void pollInNetBuffers() {
		ByteBuffer b = inNetBuffers.poll();
		
		if (session.optimizeBuffers) {
			allocator.release(b);
		}
	}
	
	private final void tryReleaseInAppBuffer() {
		if (session.optimizeBuffers) {
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
		ByteBuffer inNetBuffer;
		boolean repeat, pollNeeded, unwrapAgain;
		
		do {
			repeat = false;
			unwrapAgain = status[0] == HandshakeStatus.NEED_UNWRAP_AGAIN;
			
			if (unwrapAgain) {
				inNetBuffer = EMPTY_BUFFER;
				pollNeeded = false;
			}
			else if ((inNetBuffer = inNetBuffers.peek()) == null) {
				return false;
			}
			else {
				pollNeeded = true;
			}
			
			if (inAppBuffer == null) {
				inAppBuffer = allocator.allocate(minAppBufferSize);
			}
			else {
				inAppBuffer.clear();
			}
			try {
				unwrapResult = engine.unwrap(inNetBuffer, inAppBuffer);
				if (pollNeeded) {
					if (inNetBuffer.remaining() == 0) {
						pollInNetBuffers();
						pollNeeded = false;
					}
					else if (unwrapResult.bytesConsumed() > 0) {
						repeat = true;
						pollNeeded = false;
					}
				}
				status[0] = unwrapResult.getHandshakeStatus();
				if (retransmissionTimer != null) {
					if (RETRANSMISSION_RESET[status[0].ordinal()]) {
						cancelRetransmissionTimer();
						timeoutModel.reset();
					}
				}
				else if (unwrapAgain) {
					if (status[0] == HandshakeStatus.NEED_UNWRAP) {
						scheduleRetransmission();
					}
				}
				if (traceEnabled) {
					logger.trace(
							"Unwrapping consumed {} byte(s) to produce {} byte(s) for {}",
							unwrapResult.bytesConsumed(),
							unwrapResult.bytesProduced(),
							session);
				}
			}
			catch (Exception e) {
				elogger.error(logger, "Unwrapping failed for {}: {}", session, e);
				if (tryDelayedException(e, status)) {
					tryReleaseInAppBuffer();
					return true;
				}
				fireException(e);
				tryReleaseInAppBuffer();
				return false;
			}
			
			switch (unwrapResult.getStatus()) {
				case OK:
					if (inAppBuffer.position() > 0) {
						inAppBuffer.flip();
						try {
							IDatagramReader reader = session.codec != null ? session.codec : handler;
							
							if (session.optimizeBuffers) {
								ByteBuffer datagram = inAppBuffer;
								
								inAppBuffer = null;
								reader.read(datagram);
							}
							else {
								byte[] datagram = new byte[inAppBuffer.remaining()];

								inAppBuffer.get(datagram);
								reader.read(datagram);
							}
						} 
						catch (PipelineDecodeException e) {
							SessionIncident incident = SessionIncident.DECODING_PIPELINE_FAILURE;
							
							if (!session.incident(incident, e.getCause())) {
								elogger.error(logger, incident.defaultMessage(), session, e.getCause());
							}
							return false;
						}
						catch (Exception e) {
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

					int min = engine.getMinApplicationBufferSize();
					
					if (minAppBufferSize < min) {
						minAppBufferSize = min;
						if (allocator.isReleasable()) {
							allocator.release(inAppBuffer);
							inAppBuffer = allocator.allocate(minAppBufferSize);
						}
						repeat = true;
					}
					else {
						Exception e = new Exception("Incorrect maximum fragment size");
						
						elogger.error(logger, "Unwrapping overflow failed for {}: {}", session, e);
						fireException(e);
						tryReleaseInAppBuffer();
						return false;
					}
					break;
					
				case BUFFER_UNDERFLOW:
					if (traceEnabled) {
						logger.debug("Unwrapping underflow, more data needed for {}", session);
					}
					if (status[0] != HandshakeStatus.NOT_HANDSHAKING) {
						Exception e = new Exception("Incorrect maximum fragment size");
						
						elogger.error(logger, "Unwrapping overflow failed for {}: {}", session, e);
						fireException(e);
						tryReleaseInAppBuffer();
						return false;
					}
					else if (pollNeeded) {
						pollInNetBuffers();
						pollNeeded = false;
					}
					tryReleaseInAppBuffer();
					return false;
					
				case CLOSED:
					readIgnored = true;
					if (debugEnabled) {
						logger.debug("Unwrapping has been closed for {}", session);
					}
					if (!engine.isOutboundDone()) {
						tryReleaseInAppBuffer();
						return true;
					}
					else {
						sessionClose();
					}
					tryReleaseInAppBuffer();
					return false;
			}
			
		} while (repeat);
		
		return true;
	}

	@Override
	boolean wrap(HandshakeStatus[] status) {
		if (traceEnabled) {
			logger.trace("Wrapping started for {}", session);
		}
		
		IEngineResult wrapResult;
		boolean repeat;
		boolean releasable = allocator.isReleasable();
		Exception ex = null;
		ByteBuffer outNetBuffer = allocator.allocate(minNetBufferSize);
		EngineDatagramRecord record;
		
		do {
			repeat = false;
			record = null;
			
			synchronized (writeLock) {
				record = outAppBuffers.peek();
				if (record != null) {
					try {
						ByteBuffer[] buffers = record.holder.toArray();
						
						if (buffers.length == 1) {
							wrapResult = engine.wrap(buffers[0], outNetBuffer);
						}
						else {
							wrapResult = engine.wrap(buffers, outNetBuffer);
						}
						if (record.holder.remaining() == 0) {
							outAppBuffers.poll();
							if (releasable && record.release) {
								for (ByteBuffer buffer: buffers) {
									allocator.release(buffer);
								}
							}
							++netCounter;
						}
						else if (engine.isOutboundDone()) {
							releaseBuffers(outAppBuffers);
							closing = ClosingState.FINISHING;
						}
						else {
							record = null;
						}
					} catch (Exception e) {
						wrapResult = null;
						ex = e;
					}
				}
				else {
					if (closing == ClosingState.SENDING || engine.isInboundDone()) {
						closing = ClosingState.FINISHING;
						engine.closeOutbound();
					}
					try {
						wrapResult = engine.wrap(EMPTY_BUFFER, outNetBuffer);
					} catch (Exception e) {
						wrapResult = null;
						ex = e;
					}
				}
			}
			
			if (wrapResult == null) {
				elogger.error(logger, "Wrapping failed for {}: {}", session, ex);
				if (tryDelayedException(ex, status)) {
					repeat = true;
					continue;
				}
				if (releasable) {
					allocator.release(outNetBuffer);
				}
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
					if (flush(record, outNetBuffer)) {
						if (status[0] == HandshakeStatus.NEED_UNWRAP) {
							scheduleRetransmission();
						}
					}
					break;
					
				case BUFFER_OVERFLOW:
					if (debugEnabled) {
						logger.debug("Wrapping overflow, output packet buffer need resizing for {}", session);
					}
					
					int min = engine.getMinNetworkBufferSize();
					
					if (releasable) {
						allocator.release(outNetBuffer);
					}
					if (minNetBufferSize >= min) {
						Exception e = new Exception("Incorrect maximum fragment size");
						
						elogger.error(logger, "Wrapping overflow failed for {}: {}", session, e);
						fireException(e);
						return false;
					}
					minNetBufferSize = min;
					outNetBuffer = allocator.allocate(minNetBufferSize);
					repeat = true;
					break;
					
				case CLOSED:
					if (debugEnabled) {
						logger.debug("Wrapping has been closed for {}", session);
					}
					appCounter = netCounter;
					boolean flushed = flush(record, outNetBuffer);
					if (fireDelayedException()) {
						break;
					}
					if (handler.getConfig().waitForInboundCloseMessage() && !engine.isInboundDone()) {
						if (flushed && status[0] == HandshakeStatus.NEED_UNWRAP) {
							scheduleRetransmission();
						}
						return true;
					}
					sessionClose();
					break;
					
				case BUFFER_UNDERFLOW:
					//Ignore it
					break;
					
			}
		} while (repeat);
		
		return true;
	}

	@Override
	Executor getExecutor() {
		return ((IEngineSession)session).getExecutor();
	}

	@Override
	boolean needUnwrap() {
		return !inNetBuffers.isEmpty();
	}

	@Override
	void superQuickClose() {
		session.superQuickClose();		
	}
	
	void sessionClose() {
		delayedCloseNeeded = false;
		session.superClose();
	}
	
	@Override
	void preCreated() {
		super.preCreated();
		outAppBuffers = new LinkedList<EngineDatagramRecord>();
		if (!session.optimizeBuffers) {
			inAppBuffer = allocator.allocate(minAppBufferSize);
		}
		inNetBuffers = new LinkedList<ByteBuffer>();
	}
	
	private final void releaseBuffers(Queue<EngineDatagramRecord> records) {
		EngineDatagramRecord record;
		
		while ((record = records.poll()) != null) {
			if (record.release) {
				for (ByteBuffer buffer: record.holder.toArray()) {
					allocator.release(buffer);
				}
				record.holder = null;
			}
		}
	}
	
	
	@Override
	void postEnding() {
		super.postEnding();
		if (allocator.isReleasable()) {
			releaseBuffers(outAppBuffers);
			if (inAppBuffer != null) {
				allocator.release(inAppBuffer);
				inAppBuffer = null;
			}
		}
	}
	
	@Override
	public void setSession(ISession session) {
		handler.setSession(session);
		this.session = (DatagramSession) session;
	}

	@Override
	public IDatagramSession getSession() {
		return session;
	}
	
	final IDatagramHandler getHandler() {
		return handler;
	}
	
	private final boolean flush(EngineDatagramRecord record, ByteBuffer buffer) {
		if (buffer.position() == 0) {
			if (allocator.isReleasable()) {
				allocator.release(buffer);
			}
			return false;
		}
		else if (record == null) {
			record = new EngineDatagramRecord(null);
		}
		buffer.flip();
		record.holder = new SingleByteBufferHolder(buffer);
		record.release = true;
		long futureThreshold = session.superWrite(record);
		
		if (record.future != null) {
			record.future.setSecondThreshold(futureThreshold);
		}
		return true;
	}
	
	IFuture<Void> write(EngineDatagramRecord record, boolean needFuture) {
		ITwoThresholdFuture<Void> future;
		
		synchronized (writeLock) {
			if (closing != ClosingState.NONE) {
				if (needFuture) {
					return session.futuresController.getCancelledFuture();
				}
				return null;
			}
			outAppBuffers.add(record);
			appCounter++;
			if (needFuture) {
				future = session.futuresController.getEngineWriteFuture(appCounter);
				record.future = future;
			}
			else {
				future = null;
			}
		}
		session.loop.executenf(this);
		return future;
	}

	@Override
	public void read(SocketAddress remoteAddress, Object msg) {
	}
	
	@Override
	public void event(SocketAddress remoteAddress, DataEvent event, long length) {
	}

}
