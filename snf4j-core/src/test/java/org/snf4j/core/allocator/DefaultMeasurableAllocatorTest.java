/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2019 SNF4J contributors
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

import org.junit.Test;
import org.snf4j.core.allocator.DefaultMeasurableAllocator;

import static org.junit.Assert.assertEquals;

public class DefaultMeasurableAllocatorTest {

	DefaultMeasurableAllocator heap = new DefaultMeasurableAllocator(false);
	
	private void assertCounters(long allocateCount,	long ensureSomeCount, long ensureCount, long reduceCount,long extendCount) {
		assertEquals(allocateCount, heap.getAllocateCount());
		assertEquals(ensureSomeCount, heap.getEnsureSomeCount());
		assertEquals(ensureCount, heap.getEnsureCount());
		assertEquals(reduceCount, heap.getReduceCount());
		assertEquals(extendCount, heap.getExtendCount());
	}
	
	@Test
	public void testMaxCapacity() {
		DefaultMeasurableAllocator a = new DefaultMeasurableAllocator(false);
		assertEquals(0, a.getMaxCapacity());
		a.allocate(40);
		assertEquals(40, a.getMaxCapacity());
		a.allocate(39);
		assertEquals(40, a.getMaxCapacity());
		a.allocate(41);
		assertEquals(41, a.getMaxCapacity());
	}
	
	@Test
	public void testCounters() {
		ByteBuffer b;
		
		assertCounters(0,0,0,0,0);
		b = heap.allocate(8);
		assertCounters(1,0,0,0,0);

		b = heap.ensureSome(b, 4, 16);
		assertEquals(4, b.capacity());
		assertCounters(2,1,0,0,0);
		b = heap.ensureSome(b, 4, 16);
		assertEquals(4, b.capacity());
		assertCounters(2,1,0,0,0);

		b = heap.extend(b, 16);
		assertCounters(3,1,0,0,1);
		assertEquals(8, b.capacity());
		b = heap.extend(b, 8);
		assertCounters(3,1,0,0,1);
		assertEquals(8, b.capacity());
		
		b = heap.reduce(b, 4);
		assertCounters(4,1,0,1,1);
		assertEquals(4, b.capacity());
		b = heap.reduce(b, 4);
		assertCounters(4,1,0,1,1);
		assertEquals(4, b.capacity());

		b = heap.ensure(b, 17, 4, 128);
		assertCounters(5,1,1,1,1);
		assertEquals(64, b.capacity());
		b = heap.ensure(b, 17, 4, 128);
		assertCounters(5,1,1,1,1);
		assertEquals(64, b.capacity());
	}
}
