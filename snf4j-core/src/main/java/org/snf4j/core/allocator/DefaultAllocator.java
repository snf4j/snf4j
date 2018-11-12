/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2018 SNF4J contributors
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

/**
 * Default implementation of the {@link IByteBufferAllocator} interface.
 */
public class DefaultAllocator implements IByteBufferAllocator {
	
	/**
	 * A constant holding the default non-direct buffer allocator. The allocated
	 * buffers have a backing array.
	 */
	public final static DefaultAllocator DEFAULT = new DefaultAllocator(false); 
	
	private static final int CALCULATE_CHUNK = 1024 * 1024 * 4;

	private final boolean direct;
	
	/**
	 * Constructs a default allocator.
	 * 
	 * @param direct
	 *            <code>true</code> if the allocator should allocate direct
	 *            buffers, or <code>false</code> to allocate non-direct buffers
	 *            that have a backing array
	 */
	public DefaultAllocator(boolean direct) {
		this.direct = direct;
	}
	
	/**
	 * Allocates a new buffer with given capacity and type
	 * 
	 * @param capacity
	 *            the capacity of the allocated buffer
	 * @param direct
	 *            the type of the allocated buffer
	 * @return the new buffer
	 */
	protected ByteBuffer allocate(int capacity, boolean direct) {
		if (direct) {
			return ByteBuffer.allocateDirect(capacity);
		}
		return ByteBuffer.allocate(capacity);
	}
	
	@Override
	public ByteBuffer allocate(int capacity) {
		return allocate(capacity, direct);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation uses following rules to calculate the capacity of the
	 * new buffer:
	 * <p>
	 * a) if there is no room in the buffer it tries to increase the capacity by
	 * doubling its size. However, if the doubled size is greater than the
	 * maximum capacity then the maximum will be used as the new capacity.
	 * <p>
	 * b) if there is no data in the buffer then the new capacity will be
	 * reduced to the minimum unless the buffer's capacity already equals the
	 * minimum capacity.
	 * <p>
	 * c) if the buffer contains data which size is still greater than a quarter
	 * of the buffer's capacity then the capacity will not be changed and no
	 * re-allocation will be performed.
	 * <p>
	 * d) if the buffer contains data which size is equal or less than a quarter
	 * of the buffer's capacity then the capacity will be reduced by half so
	 * many times until the capacity will be still greater or equal to the
	 * doubled size of the data in the buffer, or when the minimum capacity is
	 * reached.
	 */
	@Override
	public ByteBuffer ensureSome(ByteBuffer buffer, int minCapacity, int maxCapacity) {
		int bufferCapacity = buffer.capacity();
		
		if (!buffer.hasRemaining()) {
			if (bufferCapacity < maxCapacity) {
				int newCapacity = Math.min(bufferCapacity << 1, maxCapacity);
			
				ByteBuffer newBuffer = allocate(newCapacity, buffer.isDirect());
				
				buffer.flip();
				return newBuffer.put(buffer);
			}
			else {
				throw new IndexOutOfBoundsException(
						"Buffer allocation failure: maximum capacity (" + maxCapacity + ") reached");
			}
		}
		else if (bufferCapacity > minCapacity) {
			int newCapacity = bufferCapacity;
			int capacity = buffer.position() << 2;
			
			if (capacity < newCapacity) {
				while (capacity < newCapacity && newCapacity > minCapacity) {
					newCapacity >>>= 1;
				}
				newCapacity = Math.max(minCapacity, newCapacity);
				if (newCapacity < bufferCapacity) {
					ByteBuffer newBuffer = allocate(newCapacity, buffer.isDirect());

					buffer.flip();
					return newBuffer.put(buffer);
				}
			}
		}
		return buffer;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation uses following rules to calculate the capacity of the
	 * new buffer:
	 * <p>
	 * a) if the new minimal capacity that is required to store a new data is
	 * less than 4MB the capacity will calculated as a minimum power of two
	 * that is greater or equal to the new minimal required capacity. However,
	 * if the calculated size is greater than the maximum capacity then the
	 * maximum will be used as the new capacity.
	 * 
	 * <p>
	 * b) if the new minimal capacity that is required to store a new data is
	 * greater or equal to 4MB the capacity will calculated as a minimum
	 * multiple of 4MB that is greater or equal to the new minimal required
	 * capacity. However, if the calculated size is greater than the maximum
	 * capacity then the maximum will be used as the new capacity.
	 */
	@Override
	public ByteBuffer ensure(ByteBuffer buffer, int size, int minCapacity, int maxCapacity) {
		if (size <= buffer.remaining()) {
			return buffer;
		}
		
		int newCapacity = size + buffer.position();
		
		if (newCapacity > maxCapacity) {
			throw new IndexOutOfBoundsException("Buffer allocation failure: maximum capacity (" + maxCapacity
					+ ") > current size (" + buffer.position() + ") + additional size (" + size + ")");
		}
		
		final int chunk = CALCULATE_CHUNK;
		
		if (newCapacity > chunk) {
			newCapacity = newCapacity / chunk * chunk;
			if (newCapacity > maxCapacity - chunk) {
				newCapacity = maxCapacity;
			}
			else {
				newCapacity += chunk;
			}
		}
		else if (newCapacity < chunk) {
			int tmpCapacity = 64;
			
			while (tmpCapacity < newCapacity) {
				tmpCapacity <<= 1;
			}
			newCapacity = tmpCapacity <= maxCapacity ? tmpCapacity : maxCapacity;
		}
		ByteBuffer newBuffer = allocate(newCapacity, buffer.isDirect());
		
		buffer.flip();
		return newBuffer.put(buffer);
	}	
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation uses following rules to calculate the capacity of the
	 * new buffer:
	 * <p>
	 * a) if the buffer's capacity equals the minimum capacity then the capacity
	 * will not be changed and no re-allocation will be performed.
	 * <p>
	 * b) if the buffer contains data which size is equal or less than the minimum
	 * capacity then the capacity will be reduced to the minimum value.
	 * <p>
	 * c) if the buffer contains data which size is greater than the minimum
	 * capacity then the capacity will not be changed and no re-allocation will 
	 * be performed.
	 */
	@Override
	public ByteBuffer reduce(ByteBuffer buffer, int minCapacity) {
		if (buffer.capacity() > minCapacity) {
			if (buffer.position() == 0) {
				return allocate(minCapacity, buffer.isDirect());
			}
			else if (buffer.position() <= minCapacity) {
				ByteBuffer newBuffer = allocate(minCapacity, buffer.isDirect());
				
				buffer.flip();
				return newBuffer.put(buffer);
			}
		}
		return buffer;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation uses following rule to calculate the capacity of the
	 * new buffer:
	 * <p>
	 * a) if the buffer's capacity is less than the maximum capacity it will try
	 * to increase the capacity by doubling its size. However, if the doubled
	 * size is greater than the maximum capacity then the maximum will be used
	 * as the new capacity.
	 */
	@Override
	public ByteBuffer extend(ByteBuffer buffer, int maxCapacity) {
		if (buffer.capacity() < maxCapacity) {
			int newCapacity = Math.min(buffer.capacity() << 1, maxCapacity);
			
			if (buffer.position() == 0) {
				return allocate(newCapacity, buffer.isDirect());
			}
			else {
				ByteBuffer newBuffer = allocate(newCapacity, buffer.isDirect());
				
				buffer.flip();
				return newBuffer.put(buffer);
			}
		}
		return buffer;
	}
	
}
