/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2019 SNF4J contributors
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
package org.snf4j.core.future;

import java.util.concurrent.atomic.AtomicReference;

import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISession;

/**
 * A class that holds all session's futures and controls their states based on
 * the session's events.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class SessionFuturesController {
	
	private final static int CREATED_IDX = SessionEvent.CREATED.ordinal();
	
	private final static int OPENED_IDX = SessionEvent.OPENED.ordinal();
	
	private final static int READY_IDX = SessionEvent.READY.ordinal();
	
	private final static int CLOSED_IDX = SessionEvent.CLOSED.ordinal();
	
	private final static int ENDING_IDX = SessionEvent.ENDING.ordinal();
	
	@SuppressWarnings("unchecked")
	private EventFuture<Void>[] eventFutures = new EventFuture[SessionEvent.values().length];
	
	private DataFuture<Void> sentFuture;
	
	private AtomicReference<Throwable> cause = new AtomicReference<Throwable>();
	
	private ISession session;
	
	/**
	 * Constructs a future controller associated with the specified session
	 * 
	 * @param session
	 *            the associated session
	 */
	public SessionFuturesController(ISession session) {
		int len = eventFutures.length;
		
		this.session = session;
		for (int i=0; i<len; ++i) {
			eventFutures[i] = new EventFuture<Void>(session, SessionEvent.values()[i]);
		}
		sentFuture = new DataFuture<Void>(session);
	}
	
	/**
	 * Sets the future executor that will be responsible for completion of the
	 * operations related with the futures holden by this controller.
	 * 
	 * @param executor
	 *            the future executor
	 */
	public void setExecutor(IFutureExecutor executor) {
		for (EventFuture<?> future: eventFutures) {
			future.setExecutor(executor);
		}
	}
	
	/**
	 * Notifies this controller about events related with changes of the
	 * session state.
	 * 
	 * @param event
	 *            an event related with a change of the session state.
	 */
	public void event(SessionEvent event) {
		Throwable cause = this.cause.get();
		
		if (cause != null) {
			eventFutures[event.ordinal()].failure(cause);
			if (event == SessionEvent.ENDING) {
				for (EventFuture<Void> future: eventFutures) {
					if (!future.isDone()) {
						future.failure(cause);
					}
				}
			}
		}
		else {
			eventFutures[event.ordinal()].success();
			if (event == SessionEvent.ENDING) {
				for (EventFuture<Void> future: eventFutures) {
					if (!future.isDone()) {
						future.cancel();
					}
				}
				sentFuture.cancel();
			}
		}
	}

	/**
	 * Notifies this controller about events related with I/O operations.
	 * 
	 * @param event
	 *            an event related with I/O operations.
	 * @param length
	 *            the length of data
	 */
	public void event(DataEvent event, long length) {
		if (event == DataEvent.SENT) {
			sentFuture.add(length);
		}
	}
	
	/**
	 * Notifies this controller about an failure.
	 * 
	 * @param cause
	 *            the cause of the failure
	 */
	public void exception(Throwable cause) {
		if (this.cause.compareAndSet(null, cause)) {
			sentFuture.failure(cause);
		}
	}
	
	/**
	 * Notifies this controller about aborting registration.
	 * 
	 * @param cause
	 *            the cause of the failure, or <code>null</code> if controlled
	 *            futures should be cancelled
	 */
	public void abort(Throwable cause) {
		if (cause == null) {
			for (EventFuture<Void> future: eventFutures) {
				future.cancel();
			}
			sentFuture.cancel();
		}
		else {
			for (EventFuture<Void> future: eventFutures) {
				future.failure(cause);
			}
			sentFuture.failure(cause);
		}
	}
	
	/**
	 * Returns the future that can be use to wait for the completion of the
	 * session's creation phase.
	 * 
	 * @return the future associated with the session's creation phase
	 */	
	public final IFuture<Void> getCreateFuture() {
		return eventFutures[CREATED_IDX];
	}

	/**
	 * Returns the future that can be use to wait for the completion of the
	 * session's opening phase.
	 * 
	 * @return the future associated with the session's opening phase
	 */	
	public final IFuture<Void> getOpenFuture() {
		return eventFutures[OPENED_IDX];
	}
	
	/**
	 * Returns the future that can be use to wait for the completion of the
	 * session's ready phase.
	 * 
	 * @return the future associated with the session's ready phase
	 */	
	public final IFuture<Void> getReadyFuture() {
		return eventFutures[READY_IDX];
	}
	
	/**
	 * Returns the future that can be use to wait for the completion of the
	 * session's closing phase.
	 * 
	 * @return the future associated with the session's closing phase
	 */	
	public final IFuture<Void> getCloseFuture() {
		return eventFutures[CLOSED_IDX];
	}
	
	/**
	 * Returns the future that can be use to wait for the completion of the
	 * session's ending phase.
	 * 
	 * @return the future associated with the session's ending phase
	 */	
	public final IFuture<Void> getEndFuture() {
		return eventFutures[ENDING_IDX];
	}
	
	/**
	 * Returns a future that can be used to wait for the completion of a write
	 * operation.
	 * 
	 * @param expectedSize
	 *            the expected size of total bytes sent by the future executor
	 *            that completes this future
	 * @return a future associated with a write operation
	 */
	public IFuture<Void> getWriteFuture(long expectedSize) {
		return new ThresholdFuture<Void>(sentFuture, expectedSize);
	}
	
	/**
	 * Returns a future that can be used to wait for the completion of a SSL write
	 * operation.
	 * 
	 * @param expectedUnwrappedSize
	 *            the expected unwrapped (before encryption) size of total bytes 
	 *            sent by the future's executor that completes this future
	 * @return a future associated with a write operation
	 */
	@Deprecated
	public IFuture<Void> getSSLWriteFuture(long expectedUnwrappedSize) {
		return new TwoThresholdFuture<Void>(sentFuture, expectedUnwrappedSize);
	}

	/**
	 * Returns a future that can be used to wait for the completion of write
	 * operations from engine driven sessions.
	 * 
	 * @param expectedUnwrappedSize
	 *            the expected unwrapped size of total bytes sent by the 
	 *            future's executor that completes this future
	 * @return a future associated with a write operation
	 */
	public IFuture<Void> getEngineWriteFuture(long expectedUnwrappedSize) {
		return new TwoThresholdFuture<Void>(sentFuture, expectedUnwrappedSize);
	}
	
	/**
	 * Returns a cancelled future.
	 * 
	 * @return a cancelled future
	 */
	public IFuture<Void> getCancelledFuture() {
		return new CancelledFuture<Void>(session);
	}

	/**
	 * Returns a successful future.
	 * 
	 * @return a successful future
	 */
	public IFuture<Void> getSuccessfulFuture() {
		return new SuccessfulFuture<Void>(session);
	}

	/**
	 * Returns a failed future with the specified cause.
	 * 
	 * @param cause
	 *            the cause of the failure
	 * @return a failed future
	 */
	public IFuture<Void> getFailedFuture(Throwable cause) {
		return new FailedFuture<Void>(session, cause);
	}
	
	/**
	 * Returns a future which can be represented by other delegate future.
	 * 
	 * @return a delegating future
	 */
	public IDelegatingFuture<Void> getDelegatingFuture() {
		return new DelegatingBlockingFuture<Void>(session);
	}
}
