/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2018 SNF4J contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * -----------------------------------------------------------------------------
 */
package org.snf4j.core.future;

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

	/**
	 * @throws CancellationException
	 *             if the future was cancelled
	 */
	IFuture<V> rethrow() throws ExecutionException {
		Throwable cause = cause();
		
		if (cause == null) {
			if (isCancelled()) {
				throw new CancellationException();
			}
			return this;
		}
		throw new ExecutionException(cause);
	}
	
	@Override
	public IFuture<V> sync() throws InterruptedException, ExecutionException {
		await0(0, true);
		return rethrow();
	}
	
	@Override
	public IFuture<V> sync(long timeoutMillis) throws InterruptedException, ExecutionException, TimeoutException {
		if (!await0(TimeUnit.MILLISECONDS.toNanos(timeoutMillis), true).isDone()) {
			throw new TimeoutException();
		}
		return rethrow();
	}
	
	@Override
	public IFuture<V> sync(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (!await0(unit.toNanos(timeout), true).isDone()) {
			throw new TimeoutException();
		}
		return rethrow();
	}

	@Override
	public IFuture<V> syncUninterruptibly() throws ExecutionException {
		awaitUninterruptibly0(0);
		return rethrow();
	}
	
	@Override
	public IFuture<V> syncUninterruptibly(long timeoutMillis) throws ExecutionException, TimeoutException {
		if (!awaitUninterruptibly0(TimeUnit.MILLISECONDS.toNanos(timeoutMillis)).isDone()) {
			throw new TimeoutException();
		}
		return rethrow();
	}
	
	@Override
	public IFuture<V> syncUninterruptibly(long timeout, TimeUnit unit) throws ExecutionException, TimeoutException {
		if (!awaitUninterruptibly0(unit.toNanos(timeout)).isDone()) {
			throw new TimeoutException();
		}
		return rethrow();
	}
	
	final void setExecutor(IFutureExecutor executor) {
		this.executor = executor;
	}
	
	@Override
	public V get() throws InterruptedException, ExecutionException {
		await();
		return get0();
	}

	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (await(timeout, unit).isDone()) {
			return get0();
		}
		throw new TimeoutException();
	}
	
	final V get0() throws InterruptedException, ExecutionException {
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
			throw new BlockingFutureOperationException(toString());
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
