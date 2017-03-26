package org.snf4j.core.concurrent;

public class FutureLock {
	private int waiters;
	
	public final void incWaiters() {
		++waiters;
	}
	
	public final void decWaiters() {
		--waiters;
	}
	
	public final boolean hasWaiters() {
		return waiters > 0;
	}
}
