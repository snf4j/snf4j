/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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

import org.snf4j.core.session.ISession;

class DelegatingBlockingFuture<V> extends AbstractBlockingFuture<V> implements IDelegatingFuture<V> {

	private volatile IFuture<V> delegate;
	
	protected DelegatingBlockingFuture(ISession session) {
		super(session);
	}

	@Override
	public boolean isDone() {
		return (delegate != null) ? delegate.isDone() : super.isDone();
	}
	
	@Override
	public Throwable cause() {
		return (delegate != null) ? delegate.cause() : super.cause();
	}

	@Override
	public boolean isCancelled() {
		return (delegate != null) ? delegate.isCancelled() : super.isCancelled();
	}	
	
	@Override
	public boolean isSuccessful() {
		return (delegate != null) ? delegate.isSuccessful() : super.isSuccessful();
	}

	@Override
	public boolean isFailed() {
		return (delegate != null) ? delegate.isFailed() : super.isFailed();
	}
	
	@Override
	public void setDelegate(IFuture<V> delegate) {
		synchronized (this) {
			if (this.delegate == null) {
				this.delegate = delegate;
			}
			else if (delegate == this.delegate) {
				return;
			}
			else {
				throw new IllegalStateException("delegate is already set");
			}
		}
		
		FutureLock superLock = super.getLock();
		
		synchronized (superLock) {
			if (superLock.hasWaiters()) {
				superLock.notifyAll();
			}
		}
	}
	
	protected FutureLock getLock() {
		synchronized (this) {
			if (delegate instanceof AbstractBlockingFuture) {
				return ((AbstractBlockingFuture<V>) delegate).getLock();
			}
		}
		return super.getLock();
	}
	
	
}
