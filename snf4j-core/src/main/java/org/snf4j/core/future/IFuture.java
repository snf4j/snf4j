/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017 SNF4J contributors
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.snf4j.core.session.ISession;

/**
 * A future that represents the result of an asynchronous operation related with
 * the session.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IFuture<V> extends Future<V> {
	
	/**
	 * Waits for this future to be completed.
	 * 
	 * @return this future
	 * @throws InterruptedException
	 *             if the current thread was interrupted.
	 */
	IFuture<V> await() throws InterruptedException;

	/**
	 * Waits for this future to be completed within the specified time limit.
	 * 
	 * @param timeoutMillis
	 *            the time limit in milliseconds to wait
	 * @return this future
	 * @throws InterruptedException
	 *             if the current thread was interrupted.
	 */
	IFuture<V> await(long timeoutMillis) throws InterruptedException;

	/**
	 * Waits for this future to be completed within the specified time limit.
	 * 
	 * @param timeout
	 *            the time limit to wait
	 * @param unit
	 *            the time unit of the time limit
	 * @return this future
	 * @throws InterruptedException
	 *             if the current thread was interrupted.
	 */
	IFuture<V> await(long timeout, TimeUnit unit) throws InterruptedException;

	/**
	 * Waits for this future to be completed without interruption.
	 * 
	 * @return this future
	 */
	IFuture<V> awaitUninterruptibly();
	
	/**
	 * Waits for this future to be completed within the specified time limit
	 * without interruption.
	 * 
	 * @param timeoutMillis
	 *            the time limit in milliseconds to wait
	 * @return this future
	 */
	IFuture<V> awaitUninterruptibly(long timeoutMillis);

	/**
	 * Waits for this future to be completed within the specified time limit
	 * without interruption.
	 * 
	 * @param timeout
	 *            the time limit to wait
	 * @param unit
	 *            the time unit of the time limit
	 * @return this future
	 */
	IFuture<V> awaitUninterruptibly(long timeout, TimeUnit unit);

	/**
	 * Waits for this future to be completed, and throws an exception that wraps
	 * the cause of the failure if this future failed.
	 * 
	 * @return this future
	 * @throws InterruptedException
	 *             if the current thread was interrupted.
	 * @throws ExecutionException
	 *             if this future failed
	 * @throws CancellationException
	 *             if this future was cancelled
	 */
	IFuture<V> sync() throws InterruptedException, ExecutionException;
	
	/**
	 * Waits for this future to be completed within the specified time limit,
	 * and throws an exception that wraps the cause of the failure if this
	 * future failed.
	 * 
	 * @param timeoutMillis
	 *            the time limit in milliseconds to wait
	 * @return this future
	 * @throws InterruptedException
	 *             if the current thread was interrupted.
	 * @throws ExecutionException
	 *             if this future failed
	 * @throws TimeoutException
	 *             if the time limit expired without completion of this future
	 * @throws CancellationException
	 *             if this future was cancelled
	 */
	IFuture<V> sync(long timeoutMillis) throws InterruptedException, ExecutionException, TimeoutException;

	/**
	 * Waits for this future to be completed within the specified time limit,
	 * and throws an exception that wraps the cause of the failure if this
	 * future failed.

	 * @param timeout
	 *            the time limit to wait
	 * @param unit
	 *            the time unit of the time limit
	 * @return this future
	 * @throws InterruptedException
	 *             if the current thread was interrupted.
	 * @throws ExecutionException
	 *             if this future failed
	 * @throws TimeoutException
	 *             if the time limit expired without completion of this future
	 * @throws CancellationException
	 *             if this future was cancelled
	 */
	IFuture<V> sync(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;

	/**
	 * Waits for this future to be completed without interruption, and throws an
	 * exception that wraps the cause of the failure if this future failed.
	 * 
	 * @return this future
	 * @throws ExecutionException
	 *             if this future failed
	 * @throws CancellationException
	 *             if this future was cancelled
	 */
	IFuture<V> syncUninterruptibly() throws ExecutionException;

	/**
	 * Waits for this future to be completed within the specified time limit
	 * without interruption, and throws an exception that wraps the cause of the
	 * failure if this future failed.
	 * 
	 * @param timeoutMillis
	 *            the time limit in milliseconds to wait
	 * @return this future
	 * @throws ExecutionException
	 *             if this future failed
	 * @throws TimeoutException
	 *             if the time limit expired without completion of this future
	 * @throws CancellationException
	 *             if this future was cancelled
	 */
	IFuture<V> syncUninterruptibly(long timeoutMillis) throws ExecutionException, TimeoutException;

	/**
	 * Waits for this future to be completed within the specified time limit
	 * without interruption, and throws an exception that wraps the cause of the
	 * failure if this future failed.
	 * 
	 * @param timeout
	 *            the time limit to wait
	 * @param unit
	 *            the time unit of the time limit
	 * @return this future
	 * @throws ExecutionException
	 *             if this future failed
	 * @throws TimeoutException
	 *             if the time limit expired without completion of this future
	 * @throws CancellationException
	 *             if this future was cancelled
	 */
	IFuture<V> syncUninterruptibly(long timeout, TimeUnit unit) throws ExecutionException, TimeoutException;
	
	/**
	 * Returns the session this future is associated with.
	 * 
	 * @return the associated session
	 */
	ISession getSession();
	
	/**
	 * Returns the result without blocking, or <code>null</code> if this future is not done
	 * yet or when <code>null</code> is expected value.
	 * 
	 * @return the result of this future.
	 */
	V getNow();
	
	/**
	 * Returns the cause of the failed I/O operation.
	 * 
	 * @return the cause of the failure, or <code>null</code> if succeeded or
	 * this future is not completed yet.
	 */
	Throwable cause();
	
	/**
	 * Tells if the operation associated with this future was completed
	 * successfully.
	 * 
	 * @return <code>true</code> if and only if the associated operation was
	 *         completed successfully
	 */
	boolean isSuccessful();
	
	/**
	 * Tells if the operation associated with this future was completed
	 * with a failure.
	 * 
	 * @return <code>true</code> if and only if the associated operation was
	 *         completed with a failure
	 */
	boolean isFailed();
	
}
