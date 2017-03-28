package org.snf4j.core.concurrent;

import org.snf4j.core.session.ISession;

public final class SuccessfulFuture<V> extends CompletedFuture<V> {
	
	public SuccessfulFuture(ISession session) {
		super(session, FutureState.SUCCESSFUL);
	}

}
