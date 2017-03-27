package org.snf4j.core.concurrent;

import java.util.concurrent.atomic.AtomicLong;

public class DataFuture<V> extends AbstractFuture<V> {

	AtomicLong dataLength = new AtomicLong(0);
	
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

	@Override
	public V getNow() {
		return null;
	}
}
