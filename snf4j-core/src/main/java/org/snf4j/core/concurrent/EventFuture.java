package org.snf4j.core.concurrent;


public class EventFuture extends AbstractFuture<Void> {

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

	@Override
	public Void getNow() {
		return null;
	}

}
