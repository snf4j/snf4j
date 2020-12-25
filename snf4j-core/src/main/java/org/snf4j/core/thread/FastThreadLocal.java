/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020 SNF4J contributors
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
package org.snf4j.core.thread;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class provides fast thread-local variables for threads implementing
 * {@link IFastThreadLocalThread}. For other threads it can provide thread-local
 * variables or none if the class is created with the {@code forAllThreads}
 * argument set to {@code false}.
 * 
 * @param <T> the type of the fast thread-local variable
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 *
 */
public class FastThreadLocal<T> extends ThreadLocal<T> {
	
	private static AtomicInteger nextIndex = new AtomicInteger(-1);
	
	private final int index = nextIndex.incrementAndGet();
	
	private final boolean forAllThreads;
	
	/**
	 * Creates a fast thread-local variable.
	 * 
	 * @param forAllThreads determines if the variable will be provided for all
	 *                      threads or only for threads implementing
	 *                      {@link IFastThreadLocalThread}.
	 */
	public FastThreadLocal(boolean forAllThreads) {
		this.forAllThreads = forAllThreads;
	}
	
	/**
	 * Creates a fast thread-local variable for all threads.
	 */
	public FastThreadLocal() {
		forAllThreads = true;
	}
	
	/**
	 * Informs if the variable is provided for all threads or only for threads
	 * implementing {@link IFastThreadLocalThread}.
	 * 
	 * @return {@code true} if the variable is provided for all threads
	 */
	public boolean isForAllThreads() {
		return forAllThreads;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * It does nothing if the current thread does not implement
	 * {@link IFastThreadLocalThread} and the fast thread-local was created to
	 * provide the variable not for all threads.
	 * 
	 * @return the current thread's value of this fast thread-local or {@code null}
	 *         if the current thread does not implement
	 *         {@link IFastThreadLocalThread} and the fast thread-local was created
	 *         to provide the variable not for all threads.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T get() {
		Thread t = Thread.currentThread();
		
		if (t instanceof IFastThreadLocalThread) {
			IFastThreadLocalThread ft = (IFastThreadLocalThread)t;
			Object value = ft.getFastThreadLocal(index);
			
			if (value == null) {
				value = initialValue();
				ft.setFastThreadLocal(index, value);
			}
			return (T)value;
		}
		return forAllThreads ? super.get() : null;
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * It does nothing if the current thread does not implement
	 * {@link IFastThreadLocalThread} and the fast thread-local was created to
	 * provide the variable not for all threads.
	 */
	@Override
	public void set(T value) {
		Thread t = Thread.currentThread();

		if (t instanceof IFastThreadLocalThread) {
			((IFastThreadLocalThread)t).setFastThreadLocal(index, value);
			return;
		}	
		else if (forAllThreads) {
			super.set(value);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * It does nothing if the current thread does not implement
	 * {@link IFastThreadLocalThread} and the fast thread-local was created to
	 * provide the variable not for all threads.
	 */
	@Override
	public void remove() {
		Thread t = Thread.currentThread();

		if (t instanceof IFastThreadLocalThread) {
			((IFastThreadLocalThread)t).removeFastThreadLocal(index);
			return;
		}
		else if (forAllThreads) {
			super.remove();
		}
	}
}
