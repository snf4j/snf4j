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
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.core.engine.IEngine;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.HandshakeLoopsThresholdException;
import org.snf4j.core.handler.HandshakeTimeoutException;
import org.snf4j.core.handler.IAllocatingHandler;
import org.snf4j.core.handler.IHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.handler.SessionIncidentException;
import org.snf4j.core.logger.ExceptionLogger;
import org.snf4j.core.logger.IExceptionLogger;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.session.ISessionTimer;
import org.snf4j.core.timer.ITimerTask;

abstract class AbstractEngineHandler<S extends InternalSession, H extends IHandler> implements IHandler, Runnable, IAllocatingHandler {
	
	private final static AtomicLong nextDelegatedTaskId = new AtomicLong(0); 
	
	private final static int MAX_HANDSHAKE_LOOPS_THRESHOLD = Integer.getInteger(Constants.MAX_HANDSHAKE_LOOPS_THRESHOLD, 500);
	
	private static final Object HANDSHAKE_TIMEOUT_EVENT = new Object();
	
	final ILogger logger;
	
	final IExceptionLogger elogger = ExceptionLogger.getInstance();
	
	S session;
	
	final IEngine engine;
	
	final H handler;
	
	volatile int minAppBufferSize;
	
	int minNetBufferSize;
	
	final IByteBufferAllocator allocator;
	
	final Object writeLock = new Object();
	
	volatile ClosingState closing = ClosingState.NONE;
	
	/** Counts total application bytes that was already wrapped */
	long netCounter;
	
	/** Counts total application bytes that needed wrapping */
	volatile long appCounter;

	/** Tells if the initial handshaking is pending */
	boolean isReadyPending = true;
	
	/** Tells if any incoming data is ignored */ 
	boolean readIgnored;
	
	boolean handshaking;

	private ITimerTask handshakeTimer;
	
	int handshakeLoops;
	
	enum Handshake {NONE, REQUESTED, STARTED};
	
	final AtomicReference<Handshake> handshake = new AtomicReference<Handshake>(Handshake.NONE); 
	
	boolean debugEnabled;
	
	boolean traceEnabled;
	
	AbstractEngineHandler(IEngine engine, H handler, ILogger logger) {
		this.engine = engine;
		this.handler = handler;
		this.logger = logger;
		allocator = handler.getFactory().getAllocator();
	}
	
	/** Return true if wrap needed */
	abstract boolean handleClosing();
	
	/** Returns false when the processing loop need to be broken */
	abstract boolean unwrap(HandshakeStatus[] status);
	
	/** Returns false when the processing loop need to be broken */
	abstract boolean wrap(HandshakeStatus[] status);
	
	abstract Executor getExecutor();
	
	abstract boolean needUnwrap();
	
	/** Quickly closes super session */
	abstract void superQuickClose();
	
	/** Gently closes super session */
	abstract void superClose();
	
	/** Method is always running in the same selector loop's thread */
	@Override
	public void run() {
		if (closing == ClosingState.FINISHED) {
			return;
		}
		
		if (handshake.compareAndSet(Handshake.REQUESTED, Handshake.STARTED)) {
			if (closing == ClosingState.NONE) {
				try {
					engine.beginHandshake();
				} catch (Exception e) {
					elogger.error(logger, "Handshake initialization failed for {}: {}", session, e);
					fireException(e);
					return;
				}
			}
		}
		
		run(new HandshakeStatus[1]);
	}
	
	@SuppressWarnings("incomplete-switch")
	void run(HandshakeStatus[] status) {
		boolean running = true;
		
		debugEnabled = logger.isDebugEnabled();
		traceEnabled = debugEnabled ? logger.isTraceEnabled() : false;
		
		do {
			boolean wrapNeeded;
			
			if (closing != ClosingState.NONE) {
				if (closing == ClosingState.FINISHED) {
					break;
				}
				synchronized (writeLock) {
					if (handshaking && closing == ClosingState.SENDING) {
						closing = ClosingState.FINISHING;
					}
				}
				wrapNeeded = handleClosing();
			}
			else {
				wrapNeeded = false;
			}
			
			if (status[0] == null) {
				status[0] = engine.getHandshakeStatus();
			}
			
			if (handshaking) {
				if (handshakeLoops < MAX_HANDSHAKE_LOOPS_THRESHOLD) {
					++handshakeLoops;
				}
				else {
					logger.error("Maximum handshake loops threshold has reached for {}", session);
					fireException(new HandshakeLoopsThresholdException());
					return;
				}
			}
			else if (status[0] != HandshakeStatus.NOT_HANDSHAKING) {
				handshaking = true;
				handshakeLoops = 0;
				handleBeginHandshake();
			}
			
			switch (status[0]) {
			case NOT_HANDSHAKING:	
				running = false;
				if (needUnwrap()) {
					running |= unwrap(status);
				}				
				if (appCounter > netCounter || isReadyPending || wrapNeeded) {
					running |= wrap(status);
				}
				break;
				
			case NEED_WRAP:
				running = wrap(status);
				break;
				
			case NEED_UNWRAP:
			case NEED_UNWRAP_AGAIN:
				running = unwrap(status);
				break;
				
			case NEED_TASK:
				Runnable task = engine.getDelegatedTask();
				
				try {
					while (task != null) {
						if (traceEnabled) {
							logger.trace("Starting execution of delegated task {} for {}" , task, session);
						}
						getExecutor().execute(new DelegatedTask(task, traceEnabled));
						task = engine.getDelegatedTask();
					}
				}
				catch (Exception e) {
					elogger.error(logger, "Execution of delegated task failed for {}: {}", session, e);
					fireException(e);
					return;
				}
				running = false;
				break;
			}
			
			if (status[0] == HandshakeStatus.FINISHED) {
				handshake.set(Handshake.NONE);
				handshaking = false;
				handleFinished();
				if (isReadyPending) {
					if (debugEnabled) {
						logger.debug("Initial handshaking is finished for {}", session);
					}
					isReadyPending = false;
					handleReady();
				}
				status[0] = null;
			}
			
		} while (running);
	}
	
	void handleOpened() {
		run();
	}
	
	Object getEngineSession() {
		return engine.getSession();
	}
	
	void handleBeginHandshake() {
		ISessionTimer timer = session.getTimer();
		
		if (handshakeTimer == null && timer.isSupported()) {
			long timeout = session.getConfig().getEngineHandshakeTimeout();
			
			handshakeTimer = timer.scheduleEvent(HANDSHAKE_TIMEOUT_EVENT, timeout);
			if (traceEnabled) {
				logger.trace("Handshake expiration timer scheduled for execution after {} ms for {}", timeout, session);
			}
		}
	}

	private final void cancelHandshakeTimer() {
		if (handshakeTimer != null) {
			handshakeTimer.cancelTask();
			handshakeTimer = null;
			if (traceEnabled) {
				logger.trace("Handshake expiration timer canceled for {}", session);
			}
		}
	}
	
	void handleFinished() {
		cancelHandshakeTimer();
	}
	
	
	void handleReady() {
		fireReady();
	}

	void handleClosed() {
		ClosingState prevClosing;
		
		cancelHandshakeTimer();
		synchronized (writeLock) {
			prevClosing = closing;
			closing = ClosingState.FINISHED;
		}
		if (!engine.isInboundDone() && !engine.isOutboundDone()) {
			try {
				engine.closeInbound();
			}
			catch (SessionIncidentException e) {
				if (prevClosing == ClosingState.NONE && !session.wasException()) {
					if (!session.incident(e.getIncident(), e)) {
						elogger.warn(logger, e.getIncident().defaultMessage(), session, e);
					}
				}
			}
		}
		if (!engine.isOutboundDone()) {
			engine.closeOutbound();
		}
	}
	
	void preCreated() {
		engine.init();
		minAppBufferSize = engine.getMinApplicationBufferSize();
		minNetBufferSize = engine.getMinNetworkBufferSize();
	}
	
	void postEnding() {
		engine.cleanup();
	}
	
	void beginHandshake(boolean lazy) {
		handshake.compareAndSet(Handshake.NONE, Handshake.REQUESTED);
		if (!lazy) {
			session.loop.executenf(this);
		}
	}
	
	final void fireReady() {
		if (debugEnabled) {
			logger.debug("Firing event {} for {}", EventType.SESSION_READY, session);
		}
		session.event(SessionEvent.READY);
		if (traceEnabled) {
			logger.trace("Ending event {} for {}", EventType.SESSION_READY, session);
		}
	}
	
	final void fireException(Throwable t) {
		if (debugEnabled) {
			logger.debug("Firing event {} for {}", EventType.EXCEPTION_CAUGHT, session);
		}
		session.exception(t);
		if (traceEnabled) {
			logger.trace("Ending event {} for {}", EventType.EXCEPTION_CAUGHT, session);
		}
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
			session.loop.executenf(this);
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
			session.loop.executenf(this);
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
		superQuickClose();
		if (stateChanged) {
			session.loop.executenf(this);
		}
	}
	
	@Override
	public IByteBufferAllocator getAllocator() {
		return allocator;
	}
	
	@Override
	public String getName() {
		return handler.getName();
	}

	@Override
	public void read(Object msg) {
	}
	
	@Override
	public void read(ByteBuffer data) {
	}
	
	@Override
	public void event(SessionEvent event) {
		if (event == SessionEvent.CLOSED) {
			handleClosed();
		}
		handler.event(event);
		if (event == SessionEvent.OPENED) {
			handleOpened();
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
	public void timer(Object event) {
		if (event == HANDSHAKE_TIMEOUT_EVENT) {
			handshakeTimer = null;
			fireException(new HandshakeTimeoutException());
		}
		else {
			handler.timer(event);
		}
	}
	
	@Override
	public void timer(Runnable task) {
		handler.timer(task);
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
	public String toString() {
		return getClass().getName() + "[session=" + session + "]";
	}
	
	private class FailureTask implements Runnable {
		private final Exception e;
		
		FailureTask(Exception e) {
			this.e = e;
		}
		
		@Override
		public void run() {
			fireException(e);
			return;
		}

		@Override
		public String toString() {
			return getClass().getName() + "[session=" + session + "]";
		}
	}
	
	class DelegatedTask implements Runnable {
		
		private final long id;
		
		private final Runnable delegate;
		
		private final boolean trace;
		
		DelegatedTask(Runnable delegate, boolean trace) {
			this.delegate = delegate;
			this.trace = trace;
			id = nextDelegatedTaskId.incrementAndGet();
		}

		@Override
		public void run() {
			try {
				delegate.run();
			}
			catch (Exception e) {
				elogger.error(logger, "Execution of delegated task {} failed for {}: {}" , delegate, session, e);
				session.loop.execute0(new FailureTask(e));
				return;
			}
			
			if (trace) {
				logger.trace("Finished execution of delegated task {} for {}" , delegate, session);
			}
			session.loop.execute0(AbstractEngineHandler.this);
		}
		
		@Override
		public String toString() {
			return "engine-delegated-task-" + id;
		}
	}
}
