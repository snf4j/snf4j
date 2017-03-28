package org.snf4j.core.concurrent;

import java.util.concurrent.atomic.AtomicLong;

import org.snf4j.core.session.ISession;

class DataFuture<V> extends AbstractBlockingFuture<V> {

	AtomicLong dataLength = new AtomicLong(0);
	
	DataFuture(ISession session) {
		super(session);
	}
	
	@Override
	public boolean cancel(boolean arg0) {
		if (setState(FutureState.CANCELLED)) {
			notifyWaiters();
			return true;
		}		
		return false;
	}
	
	void failure(Throwable cause) {
		if (setState(FutureState.FAILED)) {
			this.cause = cause;
			notifyWaiters();
		}		
	}
	
	void newData(long delta) {
		dataLength.addAndGet(delta);
		notifyWaiters();
	}
	
	long length() {
		return dataLength.get();
	}

}
