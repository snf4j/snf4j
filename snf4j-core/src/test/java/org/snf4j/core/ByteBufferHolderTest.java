/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022 SNF4J contributors
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
package org.snf4j.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.junit.Before;
import org.junit.Test;

public class ByteBufferHolderTest {

	ByteBuffer b1,b2,b3;
	
	@Before
	public void before() {
		b1 = ByteBuffer.allocate(100);
		b2 = ByteBuffer.allocate(100);
		b3 = ByteBuffer.allocate(100);
	}
	
	void putAndFlip(String b1, String b2, String b3) {
		this.b1.put(b1.getBytes()).flip();
		this.b2.put(b2.getBytes()).flip();
		this.b3.put(b3.getBytes()).flip();
	}
	
	@Test
	public void testConstructors() {
		ByteBufferHolder h = new ByteBufferHolder();
		assertEquals(0, h.toArray().length);
		h.add(b1);
		assertEquals(1, h.toArray().length);

		h = new ByteBufferHolder(110);
		assertEquals(0, h.toArray().length);
		h.add(b1);
		assertEquals(1, h.toArray().length);
		
		List<ByteBuffer> l = new ArrayList<>();
		l.add(b2);
		h = new ByteBufferHolder(l);
		assertEquals(1, h.toArray().length);
		l.clear();
		assertEquals(1, h.toArray().length);
		assertSame(b2, h.toArray()[0]);
		assertFalse(h.isMessage());
	}
	
	@Test
	public void testRemaining() {
		ByteBufferHolder h = new ByteBufferHolder();
		putAndFlip("123", "4", "56");
		assertFalse(h.hasRemaining());
		assertEquals(0, h.remaining());
		
		h.add(b1);
		assertTrue(h.hasRemaining());
		assertEquals(3, h.remaining());
		h.add(b2);
		assertTrue(h.hasRemaining());
		assertEquals(4, h.remaining());
		b1.get();
		assertTrue(h.hasRemaining());
		assertEquals(3, h.remaining());
		b1.get();
		b1.get();
		assertTrue(h.hasRemaining());
		assertEquals(1, h.remaining());
		b2.get();
		assertFalse(h.hasRemaining());
		assertEquals(0, h.remaining());
	}
	
	@Test
	public void testToArray() {
		ByteBufferHolder h = new ByteBufferHolder();
		assertEquals(0, h.toArray().length);
		assertEquals(0, h.size());
		assertTrue(h.isEmpty());
		h.add(b1);
		assertEquals(1, h.toArray().length);
		assertSame(b1, h.toArray()[0]);
		assertEquals(1, h.size());
		assertFalse(h.isEmpty());
		h.add(b3);
		assertEquals(2, h.toArray().length);
		assertSame(b1, h.toArray()[0]);
		assertSame(b3, h.toArray()[1]);
		assertEquals(2, h.size());
		assertFalse(h.isEmpty());
	}
	
	@Test
	public void testListUpdate() {
		ByteBufferHolder h = new ByteBufferHolder();
		putAndFlip("123", "4", "56");
		
		h.add(b1);
		assertArrayEquals(new ByteBuffer[] {b1}, h.toArray());
		h.add(b2);
		assertArrayEquals(new ByteBuffer[] {b1,b2}, h.toArray());
		assertTrue(h.remove(b1));
		assertArrayEquals(new ByteBuffer[] {b2}, h.toArray());
		h.add(0, b3);
		assertArrayEquals(new ByteBuffer[] {b3,b2}, h.toArray());
		assertSame(b2, h.remove(1));
		assertArrayEquals(new ByteBuffer[] {b3}, h.toArray());
		h.addAll(Arrays.asList(b2,b1));
		assertArrayEquals(new ByteBuffer[] {b3,b2,b1}, h.toArray());
		h.addAll(2,Arrays.asList(b3,b3));
		assertArrayEquals(new ByteBuffer[] {b3,b2,b3,b3,b1}, h.toArray());
		assertTrue(h.removeAll(Arrays.asList(b3,b1)));
		assertArrayEquals(new ByteBuffer[] {b2}, h.toArray());
		h.addAll(Arrays.asList(b3,b1));
		assertArrayEquals(new ByteBuffer[] {b2,b3,b1}, h.toArray());
		assertTrue(h.retainAll(Arrays.asList(b1,b2)));
		assertArrayEquals(new ByteBuffer[] {b2,b1}, h.toArray());
		h.clear();
		assertArrayEquals(new ByteBuffer[] {}, h.toArray());
		h.add(b3);
		h.set(0, b1);
		assertArrayEquals(new ByteBuffer[] {b1}, h.toArray());
	}
	
	@Test
	public void testListRead() {
		ByteBufferHolder h = new ByteBufferHolder();
		putAndFlip("123", "4", "56");
		h.add(b1);
		h.add(b2);
		h.add(b3);
		h.add(b3);
		assertSame(b2, h.get(1));
		assertSame(b3, h.get(2));
		assertEquals(0, h.indexOf(b1));
		assertEquals(2, h.indexOf(b3));
		assertEquals(3, h.lastIndexOf(b3));
		assertTrue(h.contains(b2));
		h.remove(b2);
		assertFalse(h.contains(b2));
		
		h.add(b2);
		assertArrayEquals(new ByteBuffer[] {b1,b3,b3,b2}, h.toArray());
		List<ByteBuffer> l = h.subList(0, 3);
		assertEquals(3, l.size());
		assertSame(b1, l.get(0));
		assertSame(b3, l.get(1));
		assertSame(b3, l.get(2));
		
		ByteBuffer[] a1 = new ByteBuffer[h.size()];
		ByteBuffer[] a2 = h.toArray(a1);
		assertSame(a1,a2);
		assertArrayEquals(new ByteBuffer[] {b1,b3,b3,b2}, a2);
		
		h.remove(0);
		assertArrayEquals(new ByteBuffer[] {b3,b3,b2}, h.toArray());
		assertTrue(h.containsAll(Arrays.asList(b3,b2)));
	}
	
	@Test
	public void testListIterator() {
		ByteBufferHolder h = new ByteBufferHolder();
		putAndFlip("123", "4", "56");
		h.add(b1);
		h.add(b2);
		h.add(b3);
		
		Iterator<ByteBuffer> i = h.iterator();
		assertSame(b1, i.next());
		assertSame(b2, i.next());
		assertSame(b3, i.next());
		assertFalse(i.hasNext());
		
		ListIterator<ByteBuffer> li = h.listIterator();
		assertSame(b1, li.next());
		assertSame(b2, li.next());
		assertSame(b3, li.next());
		assertFalse(i.hasNext());
		
		li = h.listIterator(1);
		assertSame(b2, li.next());
		assertSame(b3, li.next());
		assertFalse(i.hasNext());
		
	}
}
