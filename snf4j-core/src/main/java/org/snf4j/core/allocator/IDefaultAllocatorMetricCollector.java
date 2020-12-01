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
package org.snf4j.core.allocator;

/**
 * Collects metric data from the {@link DefaultAllocator}.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IDefaultAllocatorMetricCollector {
	
	/**
	 * Called when an allocation was performed by the associated allocator.
	 * This method is called no matter the buffer was truly allocated or taken for
	 * example from a cache implemented by the associated allocator.
	 * 
	 * @param capacity requested capacity of the buffer to be allocated
	 */
	void allocating(int capacity);

	/**
	 * Called when a true allocation (not taken from a cache or a pool) was
	 * performed by the associated allocator.
	 * 
	 * @param capacity capacity of the allocated buffer
	 */
	void allocated(int capacity);
	
	/**
	 * Called when a buffer was requested for releasing by the associated
	 * allocator.
	 * 
	 * @param capacity capacity of the releasing buffer
	 */
	void releasing(int capacity);
	
	/**
	 * Called when a buffer was released by the associated
	 * allocator.
	 * 
	 * @param capacity capacity of the released buffer
	 */
	void released(int capacity);
	
	/**
	 * Called when allocation was performed as a result of calling
	 * the {@link IByteBufferAllocator#ensureSome ensureSome} method in
	 * the associated allocator.
	 */
	void ensureSome();
	
	/**
	 * Called when allocation was performed as a result of calling
	 * the {@link IByteBufferAllocator#ensure ensure} method in
	 * the associated allocator.
	 */
	void ensure();
	
	/**
	 * Called when allocation was performed as a result of calling
	 * the {@link IByteBufferAllocator#reduce reduce} method in
	 * the associated allocator.
	 */
	void reduce();
	
	/**
	 * Called when allocation was performed as a result of calling
	 * the {@link IByteBufferAllocator#extend extend} method in
	 * the associated allocator.
	 */
	void extend();
}
