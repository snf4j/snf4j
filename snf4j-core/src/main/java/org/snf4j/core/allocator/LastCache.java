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

class LastCache extends Cache {

	private int capacity;
	
	private final int capacityThreshold;
	
	LastCache(int capacity, int minSize, int maxSize, int reduceThreshold, Cache[] group) {
		super(capacity, minSize, maxSize, reduceThreshold, group);
		this.capacity = capacity;
		this.capacityThreshold = capacity << 1;
	}
	
	@Override
	int capacity() {
		return capacity;
	}
	
	@Override
	void purge() {
		super.purge();
		capacity = super.capacity;
	}
	
	@Override
	boolean put(ByteBuffer b, long touch, long touchAll) {
		int bc = b.capacity();

		touchAll(touch, touchAll);
		if (capacity > bc) {
			return false;
		}
		else if (capacity < bc) {
			capacity = bc;
			for (int i=1; i<size; ++i) {
				cache[i] = null;
			}
			size = 0;
		}
		if (prePut(touch)) {
			cache[size++] = b;
			return true;
		}
		return false;
	}
	
	@Override
	ByteBuffer get(int capacity, long touch, long touchAll) {
		if (capacity <= this.capacity) {
			ByteBuffer b = super.get(capacity, touch, touchAll);
			
			if (b != null && size == 0) {
				if (this.capacity > capacityThreshold) {
					this.capacity = capacityThreshold;
				}
			}
			return b;
		}
		touchAll(touch, touchAll);
		return null;
	}

}
