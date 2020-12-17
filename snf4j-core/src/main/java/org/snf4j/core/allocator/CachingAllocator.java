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
import java.util.concurrent.atomic.AtomicLong;

import org.snf4j.core.Constants;

/**
 * A caching allocator for {@link ByteBuffer} allocations.
 * <p>
 * It uses N number of caches for storing released buffers. The caches are
 * organized to store buffers of different capacities. The first N-1 caches are
 * assigned to store buffers of fixed capacities according to the following
 * rules:
 * 
 * <pre>
 * cache 1: stores buffers of capacity determined by the minCapacity 
 * cache 2: stores buffers of capacity determined by the minCapacity * 2
 * cache 3: stores buffers of capacity determined by the minCapacity * 4
 * ...
 * cache N-1: stores buffers of capacity determined by the minCapacity * 2 to the power (N-2)
 * </pre>
 * 
 * The last N-th cache is assign to store buffers of capacities greater or equal
 * to {@code minCapacity} * 2 to the power (N-1). Its current capacity adjusts
 * to the capacities of released buffers. If the capacity of released buffer is
 * greater than the current capacity then all currently stored buffers are
 * garbage collected and the current capacity adjusts to the capacity of the
 * buffer being just released.
 * <p>
 * It implements cache aging mechanism to reduce size of caches that are not
 * used for longer time. 
 * 
 * <p>The behavior of the allocator can be customized by
 * setting following system properties:
 * <ul>
 * <li>{@link Constants#ALLOCATOR_NUM_OF_CACHES_PROPERTY ALLOCATOR_NUM_OF_CACHES_PROPERTY}
 * <li>{@link Constants#ALLOCATOR_MAX_CACHE_SIZE_PROPERTY ALLOCATOR_MAX_CACHE_SIZE_PROPERTY}
 * <li>{@link Constants#ALLOCATOR_MIN_CACHE_SIZE_PROPERTY ALLOCATOR_MIN_CACHE_SIZE_PROPERTY}
 * <li>{@link Constants#ALLOCATOR_CACHE_AGE_THRESHOLD_PROPERTY ALLOCATOR_CACHE_AGE_THRESHOLD_PROPERTY}
 * </ul>
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 *
 */
public class CachingAllocator extends DefaultAllocator {

	private final static int NUM_OF_CACHES = Integer.getInteger(Constants.ALLOCATOR_NUM_OF_CACHES_PROPERTY, 8);
	
	private final static byte[] MAP = new byte[1 << NUM_OF_CACHES];
	
	private final static int MASK;
	
	private final int touchAllThreshold;
	
	private final int minCapacity;

	private final int maxCapacity;
	
	private final int shift;
	
	private final Cache[] caches = new Cache[NUM_OF_CACHES];
	
	private final AtomicLong touch = new AtomicLong(0);
	
	private final AtomicLong touchAll;
	
	static {
		byte cacheIdx = 0;
		int mask = 1;
		for (int i=0; i<MAP.length; ++i) {
			if ((i & mask) != i) {
				mask = (mask << 1) | 1;
				++cacheIdx;
			}
			MAP[i] = cacheIdx;
		}
		MASK = mask;
	}
	
	/**
	 * Constructs a caching allocator with default minimal capacity (128) and specified
	 * metric data collector.
	 * 
	 * @param direct
	 *            <code>true</code> if the allocator should allocate direct
	 *            buffers, or <code>false</code> to allocate non-direct buffers
	 *            that have a backing array
	 * @param metric 
	 *            a metric data collector
	 */
	public CachingAllocator(boolean direct, IDefaultAllocatorMetricCollector metric) {
		this(direct, 128, metric);
	}
	
	/**
	 * Constructs a caching allocator with default minimal capacity (128).
	 * 
	 * @param direct
	 *            <code>true</code> if the allocator should allocate direct
	 *            buffers, or <code>false</code> to allocate non-direct buffers
	 *            that have a backing array
	 */
	public CachingAllocator(boolean direct) {
		this(direct, 128);
	}
	
	/**
	 * Constructs a caching allocator with specified metric data collector.
	 * 
	 * @param direct
	 *            <code>true</code> if the allocator should allocate direct
	 *            buffers, or <code>false</code> to allocate non-direct buffers
	 *            that have a backing array
	 * @param minCapacity the minimal capacity for buffers allocated by this 
	 *                    allocator
	 * @param metric a metric data collector
	 */
	public CachingAllocator(boolean direct, int minCapacity, IDefaultAllocatorMetricCollector metric) {
		super(direct, metric);
		
		int maxCacheSize = Integer.getInteger(Constants.ALLOCATOR_MAX_CACHE_SIZE_PROPERTY, 512);
		int minCacheSize = Integer.getInteger(Constants.ALLOCATOR_MIN_CACHE_SIZE_PROPERTY, 256);
		int cacheAgeThreshold = Integer.getInteger(Constants.ALLOCATOR_CACHE_AGE_THRESHOLD_PROPERTY, 1000000);	
		int min = 1;
		int shift = 0;
		int i = 0;
		
		while (min < minCapacity) {
			min <<= 1;
			shift++;
		}
		if (shift > 0) {
			--shift;
		}
		this.shift = shift;
		this.minCapacity = min;
		maxCapacity = min << (NUM_OF_CACHES-1);
		touchAllThreshold = cacheAgeThreshold * 2;
		touchAll = new AtomicLong(touchAllThreshold);
		for (; i<NUM_OF_CACHES-1; ++i) {
			caches[i] = new Cache(minCapacity << i, minCacheSize, maxCacheSize, cacheAgeThreshold);
		}
		caches[i] = new LastCache(minCapacity << i, minCacheSize, maxCacheSize, cacheAgeThreshold);
	}

	/**
	 * Constructs a caching allocator.
	 * 
	 * @param direct
	 *            <code>true</code> if the allocator should allocate direct
	 *            buffers, or <code>false</code> to allocate non-direct buffers
	 *            that have a backing array
	 * @param minCapacity the minimal capacity for buffers allocated by this 
	 *                    allocator
	 */
	public CachingAllocator(boolean direct, int minCapacity) {
		this(direct, minCapacity, null);
	}
	
	final int cacheIdx(int capacity) {
		if (capacity == 0) {
			return 0;
		}
		else if (capacity >= maxCapacity) {
			return NUM_OF_CACHES-1;
		}
		return MAP[(capacity-1 >>> shift) & MASK];
	}
	
	final Cache cache(int capacity) {
		return caches[cacheIdx(capacity)];
	}
	
	/**
	 * Gets the minimal capacity for buffers allocated by this allocator.
	 * 
	 * @return the minimal capacity
	 */
	public final int getMinCapacity() {
		return minCapacity;
	}
	
	/**
	 * Purges all caches used by this allocator.
	 */
	public void purge() {
		for (int i=0; i<NUM_OF_CACHES; ++i) {
			caches[i].purge();
		}
	}
	
	final long touch() {
		long t = touch.incrementAndGet();
		
		if (touchAll.compareAndSet(t, touchAll.get() + touchAllThreshold)) {
			for (int i=0; i<NUM_OF_CACHES; ++i) {
				caches[i].touch(t);
			}
		}
		return t;
	}
	
	@Override
	public boolean isReleasable() {
		return true;
	}
	
	@Override
	public void release(ByteBuffer buffer) {
		int capacity = buffer.capacity();
		
		metric.releasing(capacity);
		if (buffer.isDirect() == direct) {
			if (cache(capacity).put(buffer, touch())) {
				metric.released(capacity);
			}
		}
	}	
	
	@Override
	protected ByteBuffer allocate(int capacity, boolean direct) {
		if (this.direct == direct) {
			Cache cache = cache(capacity);
			ByteBuffer buffer = cache.get(capacity);
			
			touch();
			if (buffer != null) {
				metric.allocating(capacity);
				return buffer;
			}
			return super.allocate(Math.max(cache.capacity(), capacity), direct);
		}
		return super.allocate(capacity, direct);
	}	
	
	@Override
	protected ByteBuffer allocateEmpty(int capacity, ByteBuffer buffer) {
		release(buffer);
		return allocate(capacity, buffer.isDirect());
	}
	
	@Override
	protected ByteBuffer allocate(int capacity, ByteBuffer buffer) {
		ByteBuffer b = allocate(capacity, buffer.isDirect());
		
		buffer.flip();
		b.put(buffer);
		release(buffer);
		return b;
	}
	
}
