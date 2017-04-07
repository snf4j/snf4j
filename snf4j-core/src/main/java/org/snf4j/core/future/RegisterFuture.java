package org.snf4j.core.future;

import org.snf4j.core.session.ISession;

public class RegisterFuture<V> extends AbstractBlockingFuture<V> {

	public RegisterFuture(ISession session) {
		super(session);
	}

	public void success() {
		if (setState(FutureState.SUCCESSFUL)) {
			notifyWaiters();
		}
	}	
	
	public void abort(Throwable cause) {
		if (cause != null) {
			if (setState(FutureState.FAILED)) {
				this.cause = cause;
				notifyWaiters();
			}
		}
		else if (setState(FutureState.CANCELLED)) {
			notifyWaiters();
		}
	}
}
