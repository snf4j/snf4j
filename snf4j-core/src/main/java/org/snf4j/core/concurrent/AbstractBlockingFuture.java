package org.snf4j.core.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.snf4j.core.session.ISession;

abstract class AbstractBlockingFuture<V> extends AbstractFuture<V> {
	
	volatile Throwable cause;
	
	private final FutureLock lock = new FutureLock();
	
	private volatile IFutureExecutor executor;

	AbstractBlockingFuture(ISession session) {
		super(session);
	}
	
	protected FutureLock getLock() {
		return lock;
	}
	
	@Override
	public IFuture<V> await() throws InterruptedException {
		return await0(0, true);
	}
	
	@Override
	public IFuture<V> await(long timeoutMillis) throws InterruptedException {
		return await0(TimeUnit.MILLISECONDS.toNanos(timeoutMillis), true);
	}
	
	@Override
	public IFuture<V> await(long timeout, TimeUnit unit) throws InterruptedException {
		return await0(unit.toNanos(timeout), true);
	}

	@Override
	public IFuture<V> awaitUninterruptibly() {
		return awaitUninterruptibly0(0);
	}
	
	@Override
	public IFuture<V> awaitUninterruptibly(long timeoutMillis) {
		return awaitUninterruptibly0(TimeUnit.MILLISECONDS.toNanos(timeoutMillis));
	}
	
	@Override
	public IFuture<V> awaitUninterruptibly(long timeout, TimeUnit unit) {
		return awaitUninterruptibly0(unit.toNanos(timeout));
	}

	IFuture<V> rethrow() throws FutureFailureException {
		Throwable cause = this.cause;
		
		if (cause == null) {
			return this;
		}
		throw new FutureFailureException(cause);
	}
	
	@Override
	public IFuture<V> sync() throws InterruptedException, FutureFailureException {
		await0(0, true);
		return rethrow();
	}
	
	@Override
	public IFuture<V> sync(long timeoutMillis) throws InterruptedException, FutureFailureException {
		await0(TimeUnit.MILLISECONDS.toNanos(timeoutMillis), true);
		return rethrow();
	}
	
	@Override
	public IFuture<V> sync(long timeout, TimeUnit unit) throws InterruptedException, FutureFailureException {
		await0(unit.toNanos(timeout), true);
		return rethrow();
	}

	@Override
	public IFuture<V> syncUninterruptibly() throws FutureFailureException {
		awaitUninterruptibly0(0);
		return rethrow();
	}
	
	@Override
	public IFuture<V> syncUninterruptibly(long timeoutMillis) throws FutureFailureException {
		awaitUninterruptibly0(TimeUnit.MILLISECONDS.toNanos(timeoutMillis));
		return rethrow();
	}
	
	@Override
	public IFuture<V> syncUninterruptibly(long timeout, TimeUnit unit) throws FutureFailureException {
		awaitUninterruptibly0(unit.toNanos(timeout));
		return rethrow();
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
	public Throwable cause() {
		return cause;
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
	
	final IFuture<V> awaitUninterruptibly0(long nanos) {
		try {
			await0(nanos, false);
		} catch (InterruptedException e) {
			//Ignore
		}
		return this;
	}
	
	final IFuture<V> await0(long nanos, boolean interruptable) throws InterruptedException {
		
		if (isDone()) {
			return this;
		}
		
		if (interruptable && Thread.interrupted()) {
			throw new InterruptedException(toString());
		}
		
		long base = System.nanoTime();
		long now = 0;
		boolean interrupted = false;

		if (nanos < 0) {
			throw new IllegalArgumentException("timeout value is negative");
		}

		checkDeadLock();
		
		FutureLock lock = getLock();
		
		synchronized (lock) {
			lock.incWaiters();
			try {
				if (nanos == 0) {
					while (!isDone()) {
						try {
							lock.wait();
						}
						catch (InterruptedException e) {
							if (interruptable) {
								throw e;
							}
							else {
								interrupted = true;
							}
						}
					}
				} else {
					while (!isDone()) {
						long delay = nanos - now;
						
						if (delay <= 0) {
							break;
						}
						
						try {
							lock.wait(delay / 1000000, (int) (delay % 1000000));
						}
						catch (InterruptedException e) {
							if (interruptable) {
								throw e;
							}
							else {
								interrupted = true;
							}
						}
						
						now = System.nanoTime() - base;
					}
				}
			}
			finally {
				lock.decWaiters();
				if (interrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
		return this;
	}

}
