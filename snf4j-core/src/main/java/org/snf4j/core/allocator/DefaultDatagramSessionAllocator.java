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

/**
 * Default implementation of the {@link IByteBufferAllocator} interface for
 * datagram-oriented sessions. It uses two fixed-size caches storing released
 * byte buffers. The first cache stores only buffers which capacity equals the
 * specified {@code minCapacity}. Its purpose is to cache buffers used to store
 * application data passed to write/send methods. The second one is used to
 * cache buffers used by the sessions internally to store network data
 * read/written from/to the associated datagram channel.
 * <p>
 * NOTE: This allocator does not support methods that are used by
 * stream-oriented sessions. Considering that it should only be used for
 * datagram-oriented sessions. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class DefaultDatagramSessionAllocator extends DefaultAllocator {

	private final ByteBuffer[] cache1;

	private final ByteBuffer[] cache2;

	private final int minCapacity;

	private final int size;

	private int size1;

	private int size2;

	private int capacity;

	/**
	 * Constructs a default allocator for datagram-oriented sessions.
	 * 
	 * @param direct      <code>true</code> if the allocator should allocate direct
	 *                    buffers, or <code>false</code> to allocate non-direct
	 *                    buffers that have a backing array
	 * @param size        the size of the caches. It determines the maximum number
	 *                    of released buffers that can be stored in the each of the
	 *                    caches.
	 * @param minCapacity determines the minimum capacity for buffers allocated by
	 *                    the allocator. This value should be adjusted according to
	 *                    the maximum size of the application data passed to
	 *                    write/send methods of the sessions associated with this
	 *                    allocator.
	 */
	public DefaultDatagramSessionAllocator(boolean direct, int size, int minCapacity) {
		super(direct);
		this.size = size;
		this.minCapacity = minCapacity;
		capacity = minCapacity;
		cache1 = new ByteBuffer[size];
		cache2 = new ByteBuffer[size];
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @return <code>true</code>
	 */
	@Override
	public boolean isReleasable() {
		return true;
	}

	@Override
	public void release(ByteBuffer buffer) {
		int c = buffer.capacity();

		if (c == minCapacity) {
			synchronized (cache1) {
				if (size1 < size) {
					cache1[size1++] = buffer;
				}
			}
		} else {
			synchronized (cache2) {
				if (c > capacity) {
					for (int i = 0; i < size2; ++i) {
						cache2[i] = null;
					}
					cache2[0] = buffer;
					size2 = 1;
					capacity = c;
				} else if (c == capacity) {
					if (size2 < size) {
						cache2[size2++] = buffer;
					}
				}
			}
		}
	}

	@Override
	protected ByteBuffer allocate(int capacity, boolean direct) {
		ByteBuffer b = null;

		if (capacity <= minCapacity) {
			synchronized (cache1) {
				if (size1 > 0) {
					b = cache1[--size1];
				}
			}
			capacity = minCapacity;
		} else {
			synchronized (cache2) {
				if (capacity <= this.capacity) {
					if (size2 > 0) {
						b = cache2[--size2];
					}
					capacity = this.capacity;
				}
			}
		}
		if (b != null) {
			b.clear();
			return b;
		}
		return super.allocate(capacity, direct);
	}

	@Override
	public ByteBuffer extend(ByteBuffer buffer, int maxCapacity) {
		ByteBuffer b = super.extend(buffer, maxCapacity);

		if (b != buffer) {
			release(buffer);
		}
		return b;
	}

	/**
	 * Always throws {@link UnsupportedOperationException} as this method is not
	 * supported by the allocator.
	 * 
	 * @throws UnsupportedOperationException is always thrown
	 */
	@Override
	public ByteBuffer ensureSome(ByteBuffer buffer, int minCapacity, int maxCapacity) {
		throw new UnsupportedOperationException("ensureSome is not supported");
	}

	/**
	 * Always throws {@link UnsupportedOperationException} as this method is not
	 * supported by the allocator.
	 * 
	 * @throws UnsupportedOperationException is always thrown
	 */
	@Override
	public ByteBuffer ensure(ByteBuffer buffer, int size, int minCapacity, int maxCapacity) {
		throw new UnsupportedOperationException("ensure is not supported");
	}

	/**
	 * Always throws {@link UnsupportedOperationException} as this method is not
	 * supported by the allocator.
	 * 
	 * @throws UnsupportedOperationException is always thrown
	 */
	@Override
	public ByteBuffer reduce(ByteBuffer buffer, int minCapacity) {
		throw new UnsupportedOperationException("reduce is not supported");
	}
}
