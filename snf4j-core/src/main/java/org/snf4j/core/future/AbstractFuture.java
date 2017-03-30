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

import java.util.concurrent.atomic.AtomicReference;

import org.snf4j.core.session.ISession;

/**
 * Base implementation of the {@link IFuture} interface.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public abstract class AbstractFuture<V> implements IFuture<V> {
	
	AtomicReference<FutureState> state = new AtomicReference<FutureState>();

	private final ISession session;
	
	/**
	 * Constructs a base implementation with the specified session.
	 * 
	 * @param session
	 *            the session this future is associated with
	 */
	protected AbstractFuture(ISession session) {
		this.session = session;
	}
	
	boolean setState(FutureState state) {
		return this.state.compareAndSet(null, state);
	}
	
	@Override
	public ISession getSession() {
		return session;
	}
	
	String toStringDetails() {
		return null;
	}
	
	/**
	 * Returns a string representation of this future.
	 * 
	 * @return a string representation of this future.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(50);
		String details;
		
		if (session != null) {
			sb.append(session.toString());
			sb.append('-');
		}
		sb.append(getClass().getSimpleName());
		sb.append("[");
		
		if (isSuccessful()) {
			sb.append("successful");
		}
		else if (isFailed()) {
			sb.append("failed:");
			sb.append(cause());
		}
		else if (isCancelled()) {
			sb.append("canceled");
		}
		else {
			sb.append("incomplete");
		}
		details = toStringDetails();
		if (details != null) {
			sb.append(',');
			sb.append(details);
		}
		sb.append("]");
		return sb.toString();
	}
	
	/**
	 * Tells if if the operation associated with this future completed.
	 * 
	 * @return <code>true</code> if the operation completed
	 */
	@Override
	public boolean isDone() {
		return state.get() != null;
	}
	
	/**
	 * Tells if the operation associated with this future was cancelled
	 * before it completed normally.
	 * 
	 * @return <code>true</code> if the operation was cancelled
	 */
	@Override
	public boolean isCancelled() {
		return state.get() == FutureState.CANCELLED;
	}	
	
	@Override
	public boolean isSuccessful() {
		return state.get() == FutureState.SUCCESSFUL;
	}

	@Override
	public boolean isFailed() {
		return state.get() == FutureState.FAILED;
	}
	
	/**
	 * Returns <code>null</code>.
	 * 
	 * @return <code>null</code>
	 */
	@Override
	public V getNow() {
		return null;
	}
	
}
