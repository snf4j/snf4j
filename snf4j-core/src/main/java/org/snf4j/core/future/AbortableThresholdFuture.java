/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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

class AbortableThresholdFuture<V> extends ThresholdFuture<V> implements IAbortableFuture<V> {
	
	AbortableThresholdFuture(DataFuture<V> future, long threshold) {
		super(future, threshold);
	}
	
	@Override
	public boolean isDone() {
		return state.get() != null ? true : super.isDone();
	}
	
	@Override
	public boolean isSuccessful() {
		return state.get() != null ? false : super.isSuccessful();
	}

	@Override
	public boolean isCancelled() {
		return state.get() != null ? state.get() == FutureState.CANCELLED : super.isCancelled(); 
	}
	
	@Override
	public boolean isFailed() {
		return state.get() != null ? state.get() == FutureState.FAILED : super.isFailed(); 
	}
	
	public Throwable cause() {
		return state.get() != null ? cause : super.cause();
	}
	
	@Override
	public void abort(Throwable cause) {
		if (cause != null) {
			if (setState(FutureState.FAILED)) {
				this.cause = cause;
				notifyWaiters();
			}
		}
		else if (setState(FutureState.CANCELLED)) {
			notifyWaiters();
		}
	}
}
