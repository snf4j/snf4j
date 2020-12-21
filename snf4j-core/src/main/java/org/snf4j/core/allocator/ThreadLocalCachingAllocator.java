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

import java.nio.ByteBuffer;

import org.snf4j.core.thread.FastThreadLocal;
import org.snf4j.core.thread.IFastThreadLocalThread;

/**
 * A thread-local caching allocator for {@link ByteBuffer} allocations. It
 * provides the same functionality as the {@link CachingAllocator} but to
 * eliminate latency issues in multi-thread application it uses
 * {@link FastThreadLocal} to store more sets of caches so each supported thread
 * will have its own separate set.
 * <p>
 * By default it supports only threads implementing the
 * {@link IFastThreadLocalThread} interface. For other threads it uses one
 * shared set of caches as in the {@link CachingAllocator}. To extend the
 * support for all threads use the constructor with the {@code forAllThreads}
 * argument.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 *
 */
public class ThreadLocalCachingAllocator extends CachingAllocator {
	
	private final FastThreadLocal<Cache[]> threadCaches;
	
	/**
	 * Constructs a thread-local caching allocator with default minimal capacity
	 * (128) and specified metric data collector.
	 * 
	 * @param direct <code>true</code> if the allocator should allocate direct
	 *               buffers, or <code>false</code> to allocate non-direct buffers
	 *               that have a backing array
	 * @param metric a metric data collector
	 */
	public ThreadLocalCachingAllocator(boolean direct, IDefaultAllocatorMetricCollector metric) {
		this(direct, 128, metric);
	}

	/**
	 * Constructs a thread-local caching allocator with default minimal capacity
	 * (128).
	 * 
	 * @param direct <code>true</code> if the allocator should allocate direct
	 *               buffers, or <code>false</code> to allocate non-direct buffers
	 *               that have a backing array
	 */
	public ThreadLocalCachingAllocator(boolean direct) {
		this(direct, 128, null);
	}
	
	/**
	 * Constructs a thread-local caching allocator with specified metric data
	 * collector.
	 * 
	 * @param direct      <code>true</code> if the allocator should allocate direct
	 *                    buffers, or <code>false</code> to allocate non-direct
	 *                    buffers that have a backing array
	 * @param minCapacity the minimal capacity for buffers allocated by this
	 *                    allocator
	 * @param metric      a metric data collector
	 */
	public ThreadLocalCachingAllocator(boolean direct, int minCapacity, IDefaultAllocatorMetricCollector metric) {
		this(direct, minCapacity, false, metric);
	}	

	/**
	 * Constructs a thread-local caching allocator.
	 * 
	 * @param direct      <code>true</code> if the allocator should allocate direct
	 *                    buffers, or <code>false</code> to allocate non-direct
	 *                    buffers that have a backing array
	 * @param minCapacity the minimal capacity for buffers allocated by this
	 *                    allocator
	 */
	public ThreadLocalCachingAllocator(boolean direct, int minCapacity) {
		this(direct, minCapacity, null);
	}	
	
	/**
	 * Constructs a thread-local caching allocator supporting all types of threads
	 * threads.
	 * 
	 * @param direct        <code>true</code> if the allocator should allocate
	 *                      direct buffers, or <code>false</code> to allocate
	 *                      non-direct buffers that have a backing array
	 * @param minCapacity   the minimal capacity for buffers allocated by this
	 *                      allocator
	 * @param forAllThreads determines if the allocator should support all types of
	 *                      threads
	 * @param metric        a metric data collector
	 */
	public ThreadLocalCachingAllocator(boolean direct, int minCapacity, boolean forAllThreads, IDefaultAllocatorMetricCollector metric) {
		super(direct, minCapacity, metric);
		threadCaches = new ThreadCaches(forAllThreads);
	}	
	
	@Override
	Cache cache(int capacity) {
		Cache[] caches = threadCaches.get();
		
		if (caches != null) {
			return caches[cacheIdx(capacity)];
		}
		return super.cache(capacity);
	}
	
	/**
	 * Purges all caches used by this allocator for the current thread.
	 */
	@Override
	public void purge() {
		Cache[] caches = threadCaches.get();
		
		if (caches != null) {
			for (int i=0; i<caches.length; ++i) {
				caches[i].purge();
			}
		}
		else {
			super.purge();
		}
	}

	private class ThreadCaches extends FastThreadLocal<Cache[]> {
		
		private ThreadCaches(boolean forAllThreads) {
			super(forAllThreads);
		}
		
		@Override
		protected Cache[] initialValue() {
			Cache[] value = new Cache[caches.length];
			int i = 0;
			Cache c;
			
			for (; i<value.length-1; ++i) {
				c = caches[i];			
				value[i] = new Cache(c.capacity, c.minSize, c.maxSize, c.ageThreshold, value);
			}
			c = caches[i];			
			value[i] = new LastCache(c.capacity, c.minSize, c.maxSize, c.ageThreshold, value);
			return value;
		}
	}
		
}
