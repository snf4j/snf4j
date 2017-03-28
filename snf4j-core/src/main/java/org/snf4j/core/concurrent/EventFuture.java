package org.snf4j.core.concurrent;

import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISession;

class EventFuture<V> extends AbstractBlockingFuture<V> {

	private final SessionEvent event;
	
	EventFuture(ISession session, SessionEvent event) {
		super(session);
		this.event = event;
	}
	
	@Override
	protected String toStringDetails() {
		return "event=" + event.name();
	}
	
	void success() {
		if (setState(FutureState.SUCCESSFUL)) {
			notifyWaiters();
		}
	}
	
	void failure(Throwable cause) {
		if (setState(FutureState.FAILED)) {
			this.cause = cause;
			notifyWaiters();
		}
	}

}
