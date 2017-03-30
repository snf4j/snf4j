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

class WriteFuture<V> extends AbstractBlockingFuture<V> {

	private final DataFuture<V> future;
	
	private final long expectedSize;
	
	WriteFuture(DataFuture<V> future, long expectedSize) {
		super(future.getSession());
		this.future = future;
		this.expectedSize = expectedSize;
	}
	
	@Override
	protected String toStringDetails() {
		return "expectedSize=" + expectedSize;
	}
	
	@Override
	public boolean isDone() {
		return isSuccessful() || future.isDone();
	}
	
	@Override
	public boolean isSuccessful() {
		return future.size() >= expectedSize;
	}
	
	@Override
	public boolean isCancelled() {
		return !isSuccessful() && future.isCancelled(); 
	}
	
	@Override
	public boolean isFailed() {
		return !isSuccessful() && future.isFailed();
	}
	
	public Throwable cause() {
		return isSuccessful() ? null : future.cause();
	}
	
	@Override
	protected FutureLock getLock() {
		return future.getLock();
	}

}
