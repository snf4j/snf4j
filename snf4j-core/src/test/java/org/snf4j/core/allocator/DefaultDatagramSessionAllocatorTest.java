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

import java.nio.ByteBuffer;

import org.junit.Test;

public class DefaultDatagramSessionAllocatorTest {
	
	void assertNotIn(ByteBuffer b, ByteBuffer... in) {
		assertNotNull(b);
		for (ByteBuffer i: in) {
			assertFalse(b == i);
		}
	}
	
	@Test
	public void testConstructor() {
		IByteBufferAllocator a = new DefaultDatagramSessionAllocator(false, 3, 50);
		
		assertTrue(a.isReleasable());
		assertTrue(a.usesArray());
		ByteBuffer b = a.allocate(10);
		assertTrue(b.hasArray());
		a = new DefaultDatagramSessionAllocator(true, 3, 50);
		assertTrue(a.isReleasable());
		assertFalse(a.usesArray());
		b = a.allocate(30);
		assertFalse(b.hasArray());
	}
	
	@Test
	public void testAllocate() {
		IByteBufferAllocator a = new DefaultDatagramSessionAllocator(false, 3, 50);
		
		//empty pool
		ByteBuffer b1 = a.allocate(49);
		ByteBuffer b2 = a.allocate(1);
		ByteBuffer b3 = a.allocate(50);
		assertEquals(50, b1.capacity());
		assertEquals(50, b2.capacity());
		assertEquals(50, b3.capacity());
		assertNotIn(b2, b1);
		assertNotIn(b3, b1, b2);
		
		b2.putInt(44);
		a.release(b2);
		
		//one in pool
		ByteBuffer b = a.allocate(48);
		assertTrue(b == b2);
		assertEquals(0, b.position());
	
		//full pool
		a.release(b3);
		a.release(b2);
		a.release(b1);
		assertTrue(a.allocate(1) == b1);
		assertTrue(a.allocate(25) == b2);
		assertTrue(a.allocate(50) == b3);
		
		//empty big pool
		ByteBuffer b4 = a.allocate(51);
		ByteBuffer b5 = a.allocate(60);
		ByteBuffer b6 = a.allocate(55);
		assertEquals(51, b4.capacity());
		assertEquals(60, b5.capacity());
		assertEquals(55, b6.capacity());
		
		//one in big pool
		a.release(b6);
		b = a.allocate(52);
		assertTrue(b == b6);
		assertEquals(0, b.position());
		
		//empty big pool size = capacity
		b = a.allocate(55);
		assertNotIn(b, b1, b2, b3, b4, b5, b6);
		
		//empty big pool size < capacity
		b = a.allocate(54);
		assertNotIn(b, b1, b2, b3, b4, b5, b6);

		//full pool
		a.release(b6);
		a.release(b5);
		a.release(b4);
		assertTrue(a.allocate(51) == b5);
		assertNotIn(a.allocate(51), b1, b2, b3, b4, b5, b6);
	}
	
	@Test
	public void testRelease() {
		IByteBufferAllocator a = new DefaultDatagramSessionAllocator(false, 3, 50);
		
		assertTrue(a.isReleasable());
		
		ByteBuffer b1 = a.allocate(49);
		ByteBuffer b2 = a.allocate(1);
		ByteBuffer b3 = a.allocate(50);

		a.release(b3);
		ByteBuffer b = a.allocate(1);
		assertTrue(b == b3);
		assertNotIn(a.allocate(1), b1, b2, b3);
		a.release(b3);
		a.release(b2);
		a.release(b1);
		b = a.allocate(50);
		assertTrue(b == b1);
		a.release(b1);
		b = ByteBuffer.allocate(50);
		a.release(b);
		assertTrue(a.allocate(3) == b1);
		b = ByteBuffer.allocate(10);
		a.release(b);
		assertTrue(a.allocate(3) == b2);
		
		ByteBuffer b4 = a.allocate(51);
		ByteBuffer b5 = a.allocate(60);
		ByteBuffer b6 = a.allocate(55);
		a.release(b4);
		assertTrue(a.allocate(51) == b4);
		a.release(b6);
		assertTrue(a.allocate(51) == b6);
		b = a.allocate(51);
		assertNotIn(b, b1, b2, b3, b4, b5, b6);
		assertEquals(55, b.capacity());
		a.release(b6);
		b = a.allocate(51);
		assertTrue(b == b6);
		a.release(b4);
		assertNotIn(a.allocate(51), b1, b2, b3, b4, b5, b6);
		a.release(b6);
		a.release(b5);
		assertTrue(a.allocate(51) == b5);
		assertNotIn(a.allocate(51), b1, b2, b3, b4, b5, b6);
		
		b4 = a.allocate(60);
		b6 = a.allocate(60);
		a.release(b4);
		a.release(b5);
		a.release(b6);
		assertTrue(a.allocate(51) == b6);
		a.release(b6);
		a.release(ByteBuffer.allocate(60));
		assertTrue(a.allocate(51) == b6);
		
	}
	
	@Test
	public void testExtend() {
		IByteBufferAllocator a = new DefaultDatagramSessionAllocator(false, 3, 50);
		
		ByteBuffer b1 = a.allocate(30);
		assertEquals(50, b1.capacity());
		b1.putInt(33);
		ByteBuffer b2 = a.extend(b1, 60);
		assertTrue(b1 != b2);
		assertEquals(60, b2.capacity());
		b2.flip();
		assertEquals(33, b2.getInt());
		assertTrue(b1 == a.allocate(5));
		assertTrue(b2 == a.extend(b2, 60));
		
		a = new DefaultDatagramSessionAllocator(false, 3, 50);
		b1 = a.allocate(55);
		b2 = a.extend(b1, 60);
		assertTrue(b1 != b2);
		assertEquals(60, b2.capacity());
		ByteBuffer b = a.allocate(51);
		assertTrue(b == b1);
		a.release(b2);
		b = a.allocate(51);
		assertTrue(b == b2);
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testEnsureSome() {
		new DefaultDatagramSessionAllocator(false, 3, 50).ensureSome(ByteBuffer.allocate(1), 10, 100);
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testEnsure() {
		new DefaultDatagramSessionAllocator(false, 3, 50).ensure(ByteBuffer.allocate(1), 10, 10, 100);
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testReduce() {
		new DefaultDatagramSessionAllocator(false, 3, 50).reduce(ByteBuffer.allocate(1), 100);
	}
	
}
