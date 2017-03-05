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
package org.snf4j.core.allocator;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of the {@link IMeasurableAllocator} interface
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class DefaultMeasurableAllocator extends DefaultAllocator implements IMeasurableAllocator {

	AtomicLong allocateCount = new AtomicLong();

	AtomicLong assureCount = new AtomicLong();
	
	AtomicLong reduceCount = new AtomicLong();
	
	AtomicLong extendCount = new AtomicLong();
	
	AtomicInteger maxCapacity = new AtomicInteger();
	
	/**
	 * Constructs a default measurable allocator.
	 * 
	 * @param direct
	 *            <code>true</code> if the allocator should allocate direct
	 *            buffers, or <code>false</code> to allocate non-direct buffers
	 *            that have a backing array
	 */
	public DefaultMeasurableAllocator(boolean direct) {
		super(direct);
	}
	
	private final void setMaxCapacity(final int size) {
		int current = maxCapacity.get();
		
		if (current < size) {
			if (maxCapacity.compareAndSet(current, size)) {
				return;
			}
			for (;;) {
				current = maxCapacity.get();
				if (current < size) {
					if (maxCapacity.compareAndSet(current, size)) {
						return;
					}
				}
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ByteBuffer allocate(int capacity, boolean direct) {
		allocateCount.incrementAndGet();
		setMaxCapacity(capacity);
		return super.allocate(capacity, direct);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ByteBuffer assure(ByteBuffer buffer, int minCapacity, int maxCapacity) {
		ByteBuffer newBuffer = super.assure(buffer, minCapacity, maxCapacity);
		
		if (newBuffer != buffer) {
			assureCount.incrementAndGet();
			setMaxCapacity(newBuffer.capacity());
		}
		return newBuffer;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ByteBuffer reduce(ByteBuffer buffer, int minCapacity) {
		ByteBuffer newBuffer = super.reduce(buffer, minCapacity);

		if (newBuffer != buffer) {
			reduceCount.incrementAndGet();
			setMaxCapacity(newBuffer.capacity());
		}
		return newBuffer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ByteBuffer extend(ByteBuffer buffer, int maxCapacity) {
		ByteBuffer newBuffer = super.extend(buffer, maxCapacity);

		if (newBuffer != buffer) {
			extendCount.incrementAndGet();
			setMaxCapacity(newBuffer.capacity());
		}
		return newBuffer;
	}
	
	@Override
	public long getAllocateCount() {
		return allocateCount.get();
	}

	@Override
	public long getAssureCount() {
		return assureCount.get();
	}

	@Override
	public long getReduceCount() {
		return reduceCount.get();
	}

	@Override
	public long getExtendCount() {
		return extendCount.get();
	}

	@Override
	public int getMaxCapacity() {
		return maxCapacity.get();
	}


}
