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

class Cache {
	
	final int maxSize;
	
	final int minSize;
	
	final int ageThreshold;
	
	final int capacity;
	
	final Cache[] group;
	
	ByteBuffer[] cache;
	
	int size;
	
	long age;
	
	long touchThreshold;
	
	Cache(int capacity, int minSize, int maxSize, int ageThreshold, Cache[] group) {
		this.capacity = capacity;
		this.minSize = minSize;
		this.maxSize = maxSize;
		this.ageThreshold = ageThreshold;
		this.group = group;
		touchThreshold = ageThreshold * 2;
	}
	
	final boolean prePut(long touch) {
		if (cache == null) {
			cache = new ByteBuffer[maxSize];
		}
		
		if (size == 0) {
			age = touch;
		}
		else if (touch - age > ageThreshold) {
			reduce(touch);
		}
		
		if (size == cache.length) {
			return false;
		}
		return true;
	}
	
	int capacity() {
		return capacity;
	}
	
	private final void reduce(long touch) {
		int prevSize = size;
		
		if (prevSize > minSize) {
			size = Math.max(prevSize >> 1, minSize);
			for (int i=size; i<prevSize; ++i) {
				cache[i] = null;
			}
			age = touch;
		}
	}
	
	void purge() {
		cache = null;
		size = 0;
	}
	
	private final void touch(long touch) {
		if (cache != null) {
			long threshold = touch - age;
			
			if (threshold > ageThreshold) {
				reduce(touch);
			}
		}
	}
	
	final void touchAll(long touch, long touchThreshold) {
		if (touchThreshold > this.touchThreshold) {
			this.touchThreshold = touchThreshold;
			for (int i=0; i<group.length; ++i) {
				group[i].touch(touch);
			}
		}
	}
	
	boolean put(ByteBuffer b, long touch, long touchAll) {
		touchAll(touch, touchAll);
		if (capacity != b.capacity()) {
			return false;
		}
		if (prePut(touch)) {			
			cache[size++] = b;
			return true;
		}
		return false;
	}
	
	ByteBuffer get(int capacity, long touch, long touchAll) {
		if (size > 0) {
			ByteBuffer b = cache[--size];
			
			cache[size] = null;
			b.clear();
			touchAll(touch, touchAll);
			return b;
		}
		touchAll(touch, touchAll);
		return null;
	}
}
