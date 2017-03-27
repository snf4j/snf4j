package org.snf4j.core.concurrent;

import java.util.concurrent.atomic.AtomicReference;

import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.SessionEvent;

public class SessionFutures {
	
	private final static int CREATED_IDX = SessionEvent.CREATED.ordinal();
	
	private final static int OPENED_IDX = SessionEvent.OPENED.ordinal();
	
	private final static int CLOSED_IDX = SessionEvent.CLOSED.ordinal();
	
	private final static int ENDING_IDX = SessionEvent.ENDING.ordinal();
	
	private EventFuture[] eventFutures = new EventFuture[SessionEvent.values().length];
	
	private DataFuture<Void> sentFuture = new DataFuture<Void>();
	
	private AtomicReference<Throwable> cause = new AtomicReference<Throwable>();
	
	public SessionFutures() {
		int len = eventFutures.length;
		
		for (int i=0; i<len; ++i) {
			eventFutures[i] = new EventFuture();
		}
	}
	
	public void setExecutor(IFutureExecutor executor) {
		for (EventFuture future: eventFutures) {
			future.setExecutor(executor);
		}
	}
	
	public void event(SessionEvent event) {
		Throwable cause = this.cause.get();
		
		if (cause != null) {
			if (event == SessionEvent.ENDING) {
				if (!eventFutures[OPENED_IDX].isDone()) {
					eventFutures[OPENED_IDX].failure(cause);
				}
				if (!eventFutures[CLOSED_IDX].isDone()) {
					eventFutures[CLOSED_IDX].failure(cause);
				}
			}
			eventFutures[event.ordinal()].failure(cause);
		}
		else {
			if (event == SessionEvent.ENDING) {
				sentFuture.cancel(false);
			}
			eventFutures[event.ordinal()].success();
		}
	}
	
	public void event(DataEvent event, long length) {
		if (event == DataEvent.SENT) {
			sentFuture.newData(length);
		}
	}
	
	public void exception(Throwable t) {
		if (cause.compareAndSet(null, t)) {
			sentFuture.failure(t);
		}
	}
	
	public final IFuture<Void> getCreateFuture() {
		return eventFutures[CREATED_IDX];
	}

	public final IFuture<Void> getOpenFuture() {
		return eventFutures[OPENED_IDX];
	}

	public final IFuture<Void> getCloseFuture() {
		return eventFutures[CLOSED_IDX];
	}
	
	public final IFuture<Void> getEndFuture() {
		return eventFutures[ENDING_IDX];
	}
	
	public IFuture<Void> getWriteFuture(long expected) {
		return new ProxyDataFuture<Void>(sentFuture, expected);
	}
}
