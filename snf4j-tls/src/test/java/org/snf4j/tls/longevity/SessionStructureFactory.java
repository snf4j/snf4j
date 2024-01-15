/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.longevity;

import org.snf4j.core.allocator.CachingAllocator;
import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.allocator.ThreadLocalCachingAllocator;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.timer.DefaultTimer;
import org.snf4j.core.timer.ITimer;

public class SessionStructureFactory extends DefaultSessionStructureFactory implements Config {
	
	public static final CachingAllocator SERVER_ALLOCATOR;

	public static final CachingAllocator CLIENT_ALLOCATOR;
	
	private final boolean client;
	
	static {
		int minCapacity = ALLOCATOR_MIN_CAPACITY;
		if (ENABLE_THREAD_LOCAL_ALLOCATOR) {
			if (ENABLE_ALLOCATOR_METRIC) {
				SERVER_ALLOCATOR = new ThreadLocalCachingAllocator(true, minCapacity, Metric.ALLOCATOR_METRIC);
				CLIENT_ALLOCATOR = SINGLE_ALLOCATOR ? SERVER_ALLOCATOR 
						: new ThreadLocalCachingAllocator(true, minCapacity, Metric.ALLOCATOR_METRIC);
			}
			else {
				SERVER_ALLOCATOR = new ThreadLocalCachingAllocator(true, minCapacity);
				CLIENT_ALLOCATOR = SINGLE_ALLOCATOR ? SERVER_ALLOCATOR 
						: new ThreadLocalCachingAllocator(true, minCapacity);
			}
		}
		else {
			if (ENABLE_ALLOCATOR_METRIC) {
				SERVER_ALLOCATOR = new CachingAllocator(true, minCapacity, Metric.ALLOCATOR_METRIC);
				CLIENT_ALLOCATOR = SINGLE_ALLOCATOR ? SERVER_ALLOCATOR 
						: new CachingAllocator(true, minCapacity, Metric.ALLOCATOR_METRIC);
			}
			else {
				SERVER_ALLOCATOR = new CachingAllocator(true, minCapacity);
				CLIENT_ALLOCATOR = SINGLE_ALLOCATOR ? SERVER_ALLOCATOR 
						: new CachingAllocator(true, minCapacity);
			}
		}
	}
	
	public SessionStructureFactory(boolean client) {
		this.client = client;
	}
	
	@SuppressWarnings("unused")
	public static final ITimer TIMER = CLIENT_RESPONSE_DELAY > 0 ? new DefaultTimer() : null;
	
	@Override
	public IByteBufferAllocator getAllocator() {
		return client ? CLIENT_ALLOCATOR : SERVER_ALLOCATOR;
	}
	
	@Override
	public ITimer getTimer() {
		return TIMER;
	}
}
