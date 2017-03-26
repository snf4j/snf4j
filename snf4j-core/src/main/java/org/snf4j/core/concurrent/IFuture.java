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
package org.snf4j.core.concurrent;

import java.util.concurrent.Future;

public interface IFuture<V> extends Future<V> {
	
	IFuture<V> await() throws InterruptedException;
	
	IFuture<V> await(long timeout) throws InterruptedException;
	
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
	
	boolean isSuccessful();
	
	boolean isFailed();
	
}
