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

	private volatile int capacity;
	
	LastCache(int capacity, int minSize, int maxSize, int reduceThreshold) {
		super(capacity, minSize, maxSize, reduceThreshold);
		this.capacity = capacity;
	}
	
	int capacity() {
		return capacity;
	}
	
	@Override
	synchronized void purge() {
		super.purge();
		capacity = super.capacity;
	}
	
	@Override
	synchronized boolean put(ByteBuffer b, long touch) {
		int bc = b.capacity();

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
	synchronized ByteBuffer get(int capacity) {
		if (capacity <= this.capacity) {
			ByteBuffer b = super.get(capacity);
			
			if (b != null && size == 0) {
				this.capacity = capacity;
			}
			return b;
		}
		return null;
	}

}
