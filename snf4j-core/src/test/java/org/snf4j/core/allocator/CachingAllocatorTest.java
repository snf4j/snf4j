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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.Constants;

public class CachingAllocatorTest {
	
	@Before
	public void before() {
		System.setProperty(Constants.ALLOCATOR_MIN_CACHE_SIZE_PROPERTY, "0");
		System.setProperty(Constants.ALLOCATOR_MAX_CACHE_SIZE_PROPERTY, "256");
		System.setProperty(Constants.ALLOCATOR_CACHE_AGE_THRESHOLD_PROPERTY, "2048");
	}
	
	void assertNotIn(ByteBuffer b, ByteBuffer... in) {
		assertNotNull(b);
		for (ByteBuffer i: in) {
			assertFalse(b == i);
		}
	}

	void assertCaches(int min, int max, int threshold, CachingAllocator a) {
		ByteBuffer b1 = a.allocate(1);
		ByteBuffer b2 = a.allocate(1);
		ByteBuffer b3 = a.allocate(1);
		ByteBuffer b4 = a.allocate(1);
		int i;
		
		for (i=0; i<max-1; i++) {
			a.release(b1);
		}
		a.release(b2);
		a.release(b3);
		assertTrue(a.allocate(1) == b2);
		
		while (b1 == a.allocate(1));
		
		for (i=0; i<min-1; ++i) {
			a.release(b1);
		}
		if (i<min) {
			a.release(b2);
		}
		a.release(b3);
		a.release(b3);
		
		for (i=0; i<threshold/2; ++i) {
			a.release(b3);
			a.allocate(1);
		}
		
		if (min > 0) {
			assertTrue(b2 == a.allocate(1));
		}
		else {
			a.allocate(1);
			a.release(b4);
			assertTrue(b4 == a.allocate(1));
			assertNotIn(a.allocate(1), b1, b2, b3);
		}
	}
	
	@Test
	public void testConstructor() throws Exception {
		CachingAllocator a = new CachingAllocator(true);	
		
		assertEquals(512, a.allocate(1).capacity());
		assertCaches(0, 256, 2048, a);
		
		System.setProperty(Constants.ALLOCATOR_MIN_CACHE_SIZE_PROPERTY, "10");
		System.setProperty(Constants.ALLOCATOR_MAX_CACHE_SIZE_PROPERTY, "50");
		System.setProperty(Constants.ALLOCATOR_CACHE_AGE_THRESHOLD_PROPERTY, "1000");
		a = new CachingAllocator(true,8);
		assertEquals(8, a.allocate(1).capacity());
		assertCaches(10, 50, 1000, a);
		
		DefaultAllocatorMetric m = new DefaultAllocatorMetric();
		a = new CachingAllocator(false, m);
		assertFalse(a.allocate(10).isDirect());
		assertTrue(a.metric == m);
		assertEquals(512, a.allocate(1).capacity());
		a = new CachingAllocator(true, m);
		assertTrue(a.allocate(10).isDirect());
		assertTrue(a.metric == m);
		a = new CachingAllocator(false, null);
		assertFalse(a.allocate(10).isDirect());
		assertTrue(a.metric == NopAllocatorMetric.DEFAULT);	

		m = new DefaultAllocatorMetric();
		a = new CachingAllocator(false, 4, m);
		assertFalse(a.allocate(10).isDirect());
		assertTrue(a.metric == m);
		a = new CachingAllocator(true, 4, m);
		assertTrue(a.allocate(10).isDirect());
		assertTrue(a.metric == m);
		a = new CachingAllocator(false, 4, null);
		assertFalse(a.allocate(10).isDirect());
		assertTrue(a.metric == NopAllocatorMetric.DEFAULT);	
		
		//test default properties
		System.clearProperty(Constants.ALLOCATOR_MIN_CACHE_SIZE_PROPERTY);
		System.clearProperty(Constants.ALLOCATOR_MAX_CACHE_SIZE_PROPERTY);
		System.setProperty(Constants.ALLOCATOR_CACHE_AGE_THRESHOLD_PROPERTY, "1000");
		a = new CachingAllocator(true,2);
		assertCaches(256, 512, 1000, a);
		System.clearProperty(Constants.ALLOCATOR_CACHE_AGE_THRESHOLD_PROPERTY);
		a = new CachingAllocator(true,2);
		Field f = CachingAllocator.class.getDeclaredField("touchAllThreshold");
		f.setAccessible(true);
		assertEquals(2000000, f.getInt(a));
	}
	
	@Test
	public void testMinCapacity() {
		assertEquals(1, new CachingAllocator(true, 0).getMinCapacity());
		assertEquals(1, new CachingAllocator(true, 1).getMinCapacity());
		assertEquals(64, new CachingAllocator(true, 63).getMinCapacity());
		assertEquals(64, new CachingAllocator(true, 64).getMinCapacity());
		assertEquals(128, new CachingAllocator(true, 65).getMinCapacity());
	}
	
	@Test
	public void testCache() {
		CachingAllocator a = new CachingAllocator(true, 64);
		
		assertEquals(0, a.cacheIdx(0));
		assertEquals(0, a.cacheIdx(1));
		assertEquals(0, a.cacheIdx(63));
		assertEquals(0, a.cacheIdx(64));
		assertEquals(1, a.cacheIdx(65));
		assertEquals(1, a.cacheIdx(128));
		assertEquals(2, a.cacheIdx(129));
		assertEquals(6, a.cacheIdx(4096));
		assertEquals(7, a.cacheIdx(4097));
		assertEquals(7, a.cacheIdx(8191));
		assertEquals(7, a.cacheIdx(8192));
		assertEquals(7, a.cacheIdx(8193));
		
		a = new CachingAllocator(true, 0);
		assertEquals(0, a.cacheIdx(1));
		assertEquals(0, a.cacheIdx(2));
		assertEquals(1, a.cacheIdx(3));
		assertEquals(1, a.cacheIdx(4));
		assertEquals(2, a.cacheIdx(6));
		assertEquals(2, a.cacheIdx(8));
		
	}
	
	@Test
	public void testThreshold() {
		CachingAllocator a = new CachingAllocator(true, 64);
		
		ByteBuffer b1 = ByteBuffer.allocateDirect(64);
		ByteBuffer b2 = ByteBuffer.allocateDirect(64);
		ByteBuffer b3 = ByteBuffer.allocateDirect(64);
		
		for (int i=0; i<14; ++i) {
			a.release(b1);
		}
		a.release(b2);
		for (int i=0; i<17; ++i) {
			a.release(b3);
		}
		
		for (int i=0; i<2048/2 - 16; ++i) {
			a.release(b3);
			a.allocate(64);
		}
		assertTrue(b3 == a.allocate(64));
		a.release(b3);
		assertTrue(b3 == a.allocate(64));
		assertTrue(b2 == a.allocate(64));
	}
	
	@Test
	public void testThresholdInNotUsedCache() {
		CachingAllocator a = new CachingAllocator(true, 64);
		
		ByteBuffer b1 = ByteBuffer.allocateDirect(64);
		ByteBuffer b2 = ByteBuffer.allocateDirect(64);
		ByteBuffer b3 = ByteBuffer.allocateDirect(64);
		ByteBuffer b4 = ByteBuffer.allocateDirect(128);
		
		a.release(b1);
		a.release(b2);
		a.release(b3);
		
		for (int i=0; i<2048 - 2; ++i) {
			a.release(b4);
			a.allocate(128);
		}
		assertTrue(b3 == a.allocate(64));
		assertTrue(b1 == a.allocate(64));
		
		a = new CachingAllocator(true, 64);
		a.release(b1);
		a.release(b2);
		a.release(b3);
		
		for (int i=0; i<2048 - 2; ++i) {
			a.release(b4);
			a.allocate(128);
		}
		a.release(b4);
		assertTrue(b1 == a.allocate(64));

		a = new CachingAllocator(true, 64);
		a.release(b1);
		a.release(b2);
		a.release(b3);
		
		for (int i=0; i<2048 - 2; ++i) {
			a.release(b4);
			a.allocate(128);
		}
		a.allocate(128);
		assertTrue(b1 == a.allocate(64));
		
	}	
	
	@Test
	public void testCacheMinSize() {
		System.setProperty(Constants.ALLOCATOR_MIN_CACHE_SIZE_PROPERTY, "10");
		System.setProperty(Constants.ALLOCATOR_CACHE_AGE_THRESHOLD_PROPERTY, "1000");
		
		CachingAllocator a = new CachingAllocator(true, 64);

		ByteBuffer b1 = ByteBuffer.allocateDirect(64);
		ByteBuffer b2 = ByteBuffer.allocateDirect(64);
		ByteBuffer b3 = ByteBuffer.allocateDirect(64);
		ByteBuffer b4 = ByteBuffer.allocateDirect(128);
		
		for (int i=0; i<9; ++i) {
			a.release(b1);
		}
		a.release(b2);
		a.release(b3);
		
		for (int i=0; i<1000; ++i) {
			a.release(b4);
			a.allocate(128);
		}
		assertTrue(b2 == a.allocate(64));
		a.release(b2);
		for (int i=0; i<1000; ++i) {
			a.release(b4);
			a.allocate(128);
		}
		assertTrue(b2 == a.allocate(64));
		
		
	}
	
	@Test
	public void testCacheMaxSize() {
		int max = 256;
		
		CachingAllocator a = new CachingAllocator(true, 8);
		ByteBuffer b1 = a.allocate(8);
		ByteBuffer b2 = a.allocate(8);
		ByteBuffer b3 = a.allocate(8);
		
		for (int i=0; i<max-1; ++i) {
			a.release(b1);
		}
		a.release(b2);
		a.release(b3);
		assertTrue(b2 == a.allocate(3));
		assertTrue(b1 == a.allocate(3));
		assertTrue(b1 == a.allocate(3));
		
		int lastCapacity = 8 << 7;
		b1 = a.allocate(lastCapacity);
		b2 = a.allocate(lastCapacity);
		b3 = a.allocate(lastCapacity);
		
		for (int i=0; i<max-1; ++i) {
			a.release(b1);
		}
		a.release(b2);
		a.release(b3);
		assertTrue(b2 == a.allocate(lastCapacity));
		assertTrue(b1 == a.allocate(lastCapacity));
		assertTrue(b1 == a.allocate(lastCapacity));
		
	}
	
	@Test
	public void testPurge() {
		CachingAllocator a = new CachingAllocator(true, 8);

		ByteBuffer b1 = ByteBuffer.allocateDirect(8);
		ByteBuffer b2 = ByteBuffer.allocateDirect(8 << 7);
		ByteBuffer b3 = ByteBuffer.allocateDirect((8 << 7) + 1);
		
		a.release(b1);
		a.release(b1);
		a.release(b1);
		a.release(b2);
		a.release(b2);
		a.release(b2);
		
		assertTrue(b1 == a.allocate(8));
		assertTrue(b2 == a.allocate(8 << 7));
		a.purge();
		assertNotIn(a.allocate(8), b1, b2);
		assertNotIn(a.allocate(8 << 7), b1, b2);
		
		a.release(b1);
		assertTrue(b1 == a.allocate(8));
		a.release(b2);
		assertTrue(b2 == a.allocate(8 << 7));
		
		a.release(b3);
		a.release(b3);
		assertTrue(b3 == a.allocate(8 << 7));
		a.purge();
		assertEquals(8 << 7, a.allocate(8 << 7).capacity());
		
	}
	
	@Test
	public void testAllocate() {
		CachingAllocator a = new CachingAllocator(true, 8);
		
		assertTrue(a.isReleasable());
		
		assertEquals(8, a.allocate(1).capacity());
		assertEquals(8, a.allocate(8).capacity());
		assertEquals(16, a.allocate(9).capacity());
		assertEquals(16, a.allocate(16).capacity());
		assertEquals(32, a.allocate(17).capacity());
		
		assertEquals(17, a.allocate(17, false).capacity());
		
		ByteBuffer b1 = a.allocate(8);
		ByteBuffer b2 = a.allocate(8);
		ByteBuffer b3;
		assertFalse(b1 == b2);
		b1.put((byte) 1);
		a.release(b1);
		ByteBuffer b = a.allocate(4);
		assertTrue(b == b1);
		assertEquals(8, b.remaining());
		
		b3 = a.allocate(7);
		assertNotIn(b3,b1,b2);
		a.release(b3);
		a.release(b1);
		assertTrue(b1 == a.allocate(4));
		assertTrue(b3 == a.allocate(3));
		assertNotIn(a.allocate(7),b1,b2,b3);
		
		a.release(b1);
		a.release(ByteBuffer.allocate(8));
		assertTrue(b1 == a.allocate(4));
		
		b1 = ByteBuffer.allocateDirect(7);
		a.release(b1);
		assertFalse(b1 == a.allocate(7));
		
		
		int lastCapacity = 8 << 7;
		
		assertEquals(lastCapacity, a.allocate(lastCapacity-1).capacity());
		assertEquals(lastCapacity, a.allocate(lastCapacity).capacity());
		assertEquals(lastCapacity+1, a.allocate(lastCapacity+1).capacity());
		
		b1 = a.allocate(lastCapacity-1);
		b2 = a.allocate(lastCapacity);
		b3 = a.allocate(lastCapacity+1);
		a.release(b1);
		assertTrue(a.allocate(lastCapacity-2) == b1);
		a.release(b1);
		a.release(b2);
		assertTrue(b2 == a.allocate(lastCapacity));
		assertTrue(b1 == a.allocate(lastCapacity));
		assertNotIn(a.allocate(lastCapacity),b1,b2);
		
		a.release(b1);
		a.release(b2);
		a.release(b3);
		assertTrue(b3 == a.allocate(lastCapacity-2));
		b = a.allocate(lastCapacity);
		assertNotIn(b,b1,b2);
		assertEquals(lastCapacity, b.capacity());
		
		a.release(b3);
		b = a.allocate(lastCapacity+2);
		assertEquals(lastCapacity+2, b.capacity());
		assertTrue(b3 == a.allocate(lastCapacity-3));
		
		a.release(b3);
		a.release(b1);
		a.release(b2);
		assertTrue(b3 == a.allocate(lastCapacity-3));
		assertNotIn(a.allocate(lastCapacity-3),b1,b2);
		
	}
	
	@Test
	public void testEnsureSome() {
		CachingAllocator a = new CachingAllocator(false, 8);
		
		ByteBuffer b1 = a.allocate(8);
		ByteBuffer b2,b3;
		b1.position(b1.limit());
		b2 = a.ensureSome(b1, 8, 32);
		assertFalse(b2 == b1);
		assertEquals(16, b2.capacity());
		assertEquals(8, b2.position());
		b2.clear();
		b3 = a.ensureSome(b2, 8, 32);
		assertTrue(b3 == b1);
	}
	
	@Test
	public void testEnsure() {
		CachingAllocator a = new CachingAllocator(false,64);
		
		ByteBuffer b1 = a.allocate(8);
		assertEquals(64, b1.capacity());
		ByteBuffer b2 = a.ensure(b1, 256, 64, 512);
		assertEquals(256, b2.capacity());
		ByteBuffer b3 = a.ensure(b2, 64, 64, 512);
		assertTrue(b2 == b3);
		assertTrue(b1 == a.allocate(1));
		
		a.release(b2);
		b1.put((byte) 77);
		b3 = a.ensure(b1, 128, 64, 512);
		assertTrue(b3 == b2);
		assertEquals(1, b3.position());
		b3.flip();
		assertEquals(77, b3.get());
	}
	
	@Test
	public void testReduce() {
		CachingAllocator a = new CachingAllocator(false, 8);
		
		ByteBuffer b1 = a.allocate(16);
		ByteBuffer b2 = a.reduce(b1, 8);
		assertFalse(b2 == b1);
		assertEquals(8, b2.capacity());
		assertTrue(b1 == a.allocate(12));
		assertEquals(0, b2.position());
		
		a.release(b2);
		b1.put((byte) 5);
		ByteBuffer b3 = a.reduce(b1, 8);
		assertTrue(b3 == b2);
		b3.flip();
		assertEquals(5, b3.get());
		
	}
	
	@Test
	public void testExtend() {
		CachingAllocator a = new CachingAllocator(false, 8);
		
		ByteBuffer b1 = a.allocate(8);
		ByteBuffer b2 = a.extend(b1, 512);
		assertEquals(16, b2.capacity());
		assertEquals(0, b2.position());
		assertTrue(b1 == a.allocate(4));
		
		b2.put((byte) 3);
		a.release(b2);
		ByteBuffer b3 = a.extend(b1, 512);
		assertTrue(b3 == b2);
		assertEquals(0, b2.position());
		
		a.release(b2);
		b1.put((byte) 44);
		b3 = a.extend(b1, 512);
		assertTrue(b3 == b2);
		assertEquals(1, b3.position());
		b3.flip();
		assertEquals(44, b3.get());
		
	}	
	
	@Test
	public void testCacheReduce() {
		CachingAllocator a = new CachingAllocator(false, 2);
		
		
		ByteBuffer b1 = a.allocate(8);
		ByteBuffer b2 = a.allocate(8);
		ByteBuffer b3 = a.allocate(8);
		ByteBuffer b4 = a.allocate(8);
		ByteBuffer b5 = a.allocate(8);
		
		a.release(b1);
		a.release(b2);
		a.release(b3);
		a.release(b4);
		
		for (int i=0; i<2048-4; ++i) {
			a.allocate(1);
		}
		
		a.release(b5);
		assertTrue(b5 == a.allocate(8));
		assertTrue(b4 == a.allocate(8));
		a.release(b5);
		assertTrue(b5 == a.allocate(8));
		assertTrue(b1 == a.allocate(8));
		
	}
	
	@Test
	public void testMetric() {
		DefaultAllocatorMetric m = new DefaultAllocatorMetric();
		CachingAllocator a = new CachingAllocator(false, 2, m);	
		
		DefaultAllocatorMetricTest.assertMetric(m, "00000000", 0);	
		a.allocate(15);
		DefaultAllocatorMetricTest.assertMetric(m, "11000000", 16);	
		a.allocate(16);
		DefaultAllocatorMetricTest.assertMetric(m, "22000000", 16);	
		ByteBuffer b = a.allocate(17);
		DefaultAllocatorMetricTest.assertMetric(m, "33000000", 32);	
		a.release(ByteBuffer.allocateDirect(32));
		DefaultAllocatorMetricTest.assertMetric(m, "33100000", 32);	
		a.release(ByteBuffer.allocate(31));
		DefaultAllocatorMetricTest.assertMetric(m, "33200000", 32);	
		a.release(b);
		DefaultAllocatorMetricTest.assertMetric(m, "33310000", 32);	
		ByteBuffer b2 = a.allocate(30);
		assertTrue(b2 == b);
		DefaultAllocatorMetricTest.assertMetric(m, "43310000", 32);	
		for (int i=0; i<255; ++i) {
			a.release(b);
		}
		DefaultAllocatorMetricTest.assertMetric(m, "4;3;258;256;0;0;0;0", 32);	
		a.release(b);
		DefaultAllocatorMetricTest.assertMetric(m, "4;3;259;257;0;0;0;0", 32);	
		a.release(b);
		DefaultAllocatorMetricTest.assertMetric(m, "4;3;260;257;0;0;0;0", 32);	
		
		b = a.allocate((2 << 7) + 10);
		a.release(b);
		DefaultAllocatorMetricTest.assertMetric(m, "5;4;261;258;0;0;0;0", 266);	
		a.release(a.allocate(267));
		DefaultAllocatorMetricTest.assertMetric(m, "6;5;262;259;0;0;0;0", 267);	
		a.release(ByteBuffer.allocate(266));
		DefaultAllocatorMetricTest.assertMetric(m, "6;5;263;259;0;0;0;0", 267);	
		
		m = new DefaultAllocatorMetric();
		a = new CachingAllocator(false, 2, m);	
		for (int i=0; i<255; ++i) {
			a.release(b);
		}
		DefaultAllocatorMetricTest.assertMetric(m, "0;0;255;255;0;0;0;0", 0);	
		a.release(b);
		DefaultAllocatorMetricTest.assertMetric(m, "0;0;256;256;0;0;0;0", 0);	
		a.release(b);
		DefaultAllocatorMetricTest.assertMetric(m, "0;0;257;256;0;0;0;0", 0);	
		
	}	
}
