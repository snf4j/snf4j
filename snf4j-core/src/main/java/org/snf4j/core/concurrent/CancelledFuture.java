package org.snf4j.core.concurrent;

import org.snf4j.core.session.ISession;

public final class CancelledFuture<V> extends CompletedFuture<V> {

	public CancelledFuture(ISession session) {
		super(session, FutureState.CANCELLED);
	}
	
}
