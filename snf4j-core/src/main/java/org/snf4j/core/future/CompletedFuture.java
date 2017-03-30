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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.snf4j.core.session.ISession;

/**
 * A completed future.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class CompletedFuture<V> extends AbstractFuture<V> {

	/**
	 * Constructs a completed future with the specified session.
	 * 
	 * @param session
	 *            the session this future is associated with
	 * @param state
	 *            the state of this future
	 * @throws IllegalArgumentException
	 *             if the <code>state</code> argument is <code>null</code>
	 */
	public CompletedFuture(ISession session, FutureState state) {
		super(session);
		if (state == null) {
			throw new IllegalArgumentException("state is null");
		}
		this.state.set(state);
	}
	
	/**
	 * Does nothing.
	 * 
	 * @return <code>true</code>
	 */
	@Override
	public boolean cancel(boolean arg0) {
		return false;
	}

	/**
	 * Returns immediately
	 * 
	 * @return <code>null</code>
	 */
	@Override
	public V get() throws InterruptedException, ExecutionException {
		return null;
	}

	/**
	 * Returns immediately
	 * 
	 * @return <code>null</code>
	 */
	@Override
	public V get(long arg0, TimeUnit arg1) throws InterruptedException, ExecutionException, TimeoutException {
		return null;
	}

	/**
	 * Tells that this future is completed
	 * 
	 * @return <code>true</code>
	 */
	@Override
	public boolean isDone() {
		return true;
	}

	/**
	 * Returns immediately
	 * 
	 * @return this future
	 */
	@Override
	public IFuture<V> await() {
		return this;
	}

	/**
	 * Returns immediately
	 * 
	 * @return this future
	 */
	@Override
	public IFuture<V> await(long timeoutMillis) {
		return this;
	}

	/**
	 * Returns immediately
	 * 
	 * @return this future
	 */
	@Override
	public IFuture<V> await(long timeout, TimeUnit unit) {
		return this;
	}

	/**
	 * Returns immediately
	 * 
	 * @return this future
	 */
	@Override
	public IFuture<V> awaitUninterruptibly() {
		return this;
	}

	/**
	 * Returns immediately
	 * 
	 * @return this future
	 */
	@Override
	public IFuture<V> awaitUninterruptibly(long timeoutMillis) {
		return this;
	}

	/**
	 * Returns immediately
	 * 
	 * @return this future
	 */
	@Override
	public IFuture<V> awaitUninterruptibly(long timeout, TimeUnit unit) {
		return this;
	}

	/**
	 * Returns immediately
	 * 
	 * @return this future
	 */
	@Override
	public IFuture<V> sync() {
		return this;
	}

	/**
	 * Returns immediately
	 * 
	 * @return this future
	 */
	@Override
	public IFuture<V> sync(long timeoutMillis) {
		return this;
	}

	/**
	 * Returns immediately
	 * 
	 * @return this future
	 */
	@Override
	public IFuture<V> sync(long timeout, TimeUnit unit) {
		return this;
	}

	/**
	 * Returns immediately
	 * 
	 * @return this future
	 */
	@Override
	public IFuture<V> syncUninterruptibly() {
		return this;
	}

	/**
	 * Returns immediately
	 * 
	 * @return this future
	 */
	@Override
	public IFuture<V> syncUninterruptibly(long timeoutMillis) {
		return this;
	}

	/**
	 * Returns immediately
	 * 
	 * @return this future
	 */
	@Override
	public IFuture<V> syncUninterruptibly(long timeout, TimeUnit unit) {
		return this;
	}

	/**
	 * Returns <code>null</code>
	 * 
	 * @return <code>null</code>
	 */
	@Override
	public Throwable cause() {
		return null;
	}

}
