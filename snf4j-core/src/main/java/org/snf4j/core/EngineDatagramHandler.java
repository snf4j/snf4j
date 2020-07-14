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
import org.snf4j.core.handler.SessionIncidentException;
import org.snf4j.core.handler.SessionReadyTimeoutException;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.session.IDatagramSession;
import org.snf4j.core.session.IEngineSession;
import org.snf4j.core.session.ISession;
import org.snf4j.core.timer.ITimerTask;

class EngineDatagramHandler extends AbstractEngineHandler<DatagramSession, IDatagramHandler> implements IDatagramHandler {
	
	private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.wrap(new byte[0]);
	
	private Queue<EngineDatagramRecord> outAppBuffers;
	
	private ByteBuffer inAppBuffer;
	
	private Queue<ByteBuffer> inNetBuffers;

	private ITimerTask readyTimer;
	
	private static final Object SESSION_READY_TIMEOUT_EVENT = new Object();
	
	private final SocketAddress remoteAddress;
	
	EngineDatagramHandler(IEngine engine, SocketAddress remoteAddress, IDatagramHandler handler, ILogger logger) {
		super(engine, handler, logger);
		this.remoteAddress = remoteAddress;
	}

	private final void cancelReadyTimer() {
		if (readyTimer != null) {
			readyTimer.cancelTask();
			readyTimer = null;
		}
	}
	
	@Override
	void handleOpened() {
		if (session.getTimer().isSupported()) {
			readyTimer = session.getTimer().scheduleEvent(SESSION_READY_TIMEOUT_EVENT, session.getConfig().getMaxWaitTimeForDatagramSessionReady());
		}
		super.handleOpened();
	}
	
	@Override
	void handleReady() {
		cancelReadyTimer();
		super.handleReady();
	}
	
	@Override
	void handleClosed() {
		cancelReadyTimer();
		super.handleClosed();
	}
	
	@Override
	public void timer(Object event) {
		if (event == SESSION_READY_TIMEOUT_EVENT) {
			fireException(new SessionReadyTimeoutException());
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
	public void read(SocketAddress remoteAddress, byte[] datagram) {
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
					engine.closeOutbound();
					if (isReadyPending) {
						try {
							engine.closeInbound();
						} catch (SessionIncidentException e) {
							// Ignore
						}
					}
					return true;
				}
			}			
			isReadyPending = false;
		}
		return closing == ClosingState.SENDING;
	}

	@Override
	boolean unwrap(HandshakeStatus[] status) {
		if (traceEnabled) {
			logger.trace("Unwrapping started for {}", session);
		}
		
		IEngineResult unwrapResult;
		ByteBuffer inNetBuffer;
		boolean repeat, pollNeeded;
		
		do {
			repeat = false;
			
			if (status[0] == HandshakeStatus.NEED_UNWRAP_AGAIN) {
				inNetBuffer = EMPTY_BUFFER;
				pollNeeded = false;
			}
			else if ((inNetBuffer = inNetBuffers.peek()) == null) {
				return false;
			}
			else {
				pollNeeded = true;
			}
			
			inAppBuffer.clear();
			try {
				unwrapResult = engine.unwrap(inNetBuffer, inAppBuffer);
				if (pollNeeded) {
					if (inNetBuffer.remaining() == 0) {
						inNetBuffers.poll();
						pollNeeded = false;
					}
					else if (unwrapResult.bytesConsumed() > 0) {
						repeat = true;
						pollNeeded = false;
					}
				}
				status[0] = unwrapResult.getHandshakeStatus();
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
				fireException(e);
				return false;
			}
			
			switch (unwrapResult.getStatus()) {
				case OK:
					if (inAppBuffer.position() > 0) {
						inAppBuffer.flip();
						try {
							byte[] datagram = new byte[inAppBuffer.remaining()];

							inAppBuffer.get(datagram);
							(session.codec != null ? session.codec : handler).read(datagram);
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
						return false;
					}
					else if (pollNeeded) {
						inNetBuffers.poll();
						pollNeeded = false;
					}
					return false;
					
				case CLOSED:
					readIgnored = true;
					if (debugEnabled) {
						logger.debug("Unwrapping has been closed for {}", session);
					}
					if (!engine.isOutboundDone()) {
						return true;
					}
					else {
						session.superQuickClose();
					}
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
						wrapResult = engine.wrap(record.buffer, outNetBuffer);
						if (record.buffer.remaining() == 0) {
							outAppBuffers.poll();
							if (releasable && record.release) {
								allocator.release(record.buffer);
							}
							++netCounter;
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
				if (releasable) {
					allocator.release(outNetBuffer);
				}
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
					flush(record, outNetBuffer);
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
					flush(record, outNetBuffer);
					if (handler.getConfig().waitForInboundCloseMessage() && !engine.isInboundDone()) {
						return true;
					}
					session.superClose();
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
	
	@Override
	void preCreated() {
		super.preCreated();
		outAppBuffers = new LinkedList<EngineDatagramRecord>();
		inAppBuffer = allocator.allocate(minAppBufferSize);
		inNetBuffers = new LinkedList<ByteBuffer>();
	}
	
	private final void releaseBuffers(Queue<EngineDatagramRecord> records) {
		EngineDatagramRecord record;
		
		while ((record = records.poll()) != null) {
			if (record.release) {
				allocator.release(record.buffer);
				record.buffer = null;
			}
		}
	}
	
	
	@Override
	void postEnding() {
		super.postEnding();
		if (allocator.isReleasable()) {
			releaseBuffers(outAppBuffers);
			allocator.release(inAppBuffer);
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
	
	private final void flush(EngineDatagramRecord record, ByteBuffer buffer) {
		if (buffer.position() == 0) {
			if (allocator.isReleasable()) {
				allocator.release(buffer);
			}
			return;
		}
		else if (record == null) {
			record = new EngineDatagramRecord(null);
		}
		buffer.flip();
		record.buffer = buffer;
		record.release = true;
		long futureThreshold = session.superWrite(record);
		
		if (record.future != null) {
			record.future.setSecondThreshold(futureThreshold);
		}
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
