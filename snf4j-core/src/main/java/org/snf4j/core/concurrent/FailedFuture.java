package org.snf4j.core.concurrent;

import org.snf4j.core.session.ISession;

public final class FailedFuture<V> extends CompletedFuture<V> {
	
	private final Throwable cause;
	
	FailedFuture(ISession session, Throwable cause) {
		super(session, FutureState.FAILED);
		this.cause = cause;
	}
	
	@Override
	public Throwable cause() {
		return cause;
	}

}
