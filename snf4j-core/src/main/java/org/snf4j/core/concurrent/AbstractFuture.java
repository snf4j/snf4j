package org.snf4j.core.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractFuture<V> implements IFuture<V> {
	
	AtomicReference<FutureState> state = new AtomicReference<FutureState>();
	
	volatile Throwable cause;
	
	private final FutureLock lock = new FutureLock();
	
	private volatile IFutureExecutor executor;
	
	protected FutureLock getLock() {
		return lock;
	}
	
	@Override
	public boolean isDone() {
		return state.get() != null;
	}
	
	@Override
	public IFuture<V> await() throws InterruptedException {
		return await(0);
	}
	
	@Override
	public IFuture<V> await(long timeout) throws InterruptedException {
		await0(timeout);
		return this;
	}
	
	final void setExecutor(IFutureExecutor executor) {
		this.executor = executor;
	}
	
	@Override
	public V get() throws InterruptedException, ExecutionException {
		await();

		Throwable cause = cause();
		
		if (cause == null) {
			return getNow();
		}
		if (cause instanceof CancellationException) {
			throw (CancellationException)cause;
		}
		throw new ExecutionException(cause);
	}

	@Override
	public V get(long arg0, TimeUnit arg1) throws InterruptedException, ExecutionException, TimeoutException {
		//TODO implement it
		return null;
	}
	
	@Override
	public boolean cancel(boolean arg0) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return state.get() == FutureState.CANCELED;
	}	
	
	@Override
	public boolean isSuccessful() {
		return state.get() == FutureState.SUCCESSFUL;
	}

	@Override
	public boolean isFailed() {
		return state.get() == FutureState.FAILED;
	}
	
	@Override
	public Throwable cause() {
		return cause;
	}
	
	boolean setState(FutureState state) {
		return this.state.compareAndSet(null, state);
	}
	
	void notifyWaiters() {
		FutureLock lock = getLock();
		
		synchronized (lock) {
			if (lock.hasWaiters()) {
				lock.notifyAll();
			}
		}
	}
	
	final void checkDeadLock() {
		IFutureExecutor executor = this.executor;
		
		if (executor != null && executor.inExecutor()) {
			throw new BlockingOperationException(toString());
		}
	}
	
	void await0(long millis) throws InterruptedException {
		
		if (isDone()) {
			return;
		}
		
		long base = System.currentTimeMillis();
		long now = 0;

		if (millis < 0) {
			throw new IllegalArgumentException("timeout value is negative");
		}

		checkDeadLock();
		
		FutureLock lock = getLock();
		
		synchronized (lock) {
			lock.incWaiters();
			try {
				if (millis == 0) {
					while (!isDone()) {
						lock.wait(0);
					}
				} else {
					while (!isDone()) {
						long delay = millis - now;
						if (delay <= 0) {
							break;
						}
						lock.wait(delay);
						now = System.currentTimeMillis() - base;
					}
				}
			}
			finally {
				lock.decWaiters();
			}
		}
	}

}
