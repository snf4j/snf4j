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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Test;
import org.snf4j.core.ByteUtils;
import org.snf4j.core.allocator.DefaultAllocator;
import org.snf4j.core.allocator.IByteBufferAllocator;

public class DefaultAllocatorTest {

	private IByteBufferAllocator heap = DefaultAllocator.DEFAULT;
	private IByteBufferAllocator direct = new DefaultAllocator(true);
	
	@Test
	public void testUsesArray() {
		assertTrue(heap.usesArray());
		assertFalse(direct.usesArray());
	}
	
	@Test
	public void testAllocate() {
		ByteBuffer b = heap.allocate(10);
		
		assertEquals(10, b.capacity());
		assertEquals(0, b.position());
		assertEquals(10, b.limit());
		assertFalse(b.isDirect());

		b = direct.allocate(11);
		
		assertEquals(11, b.capacity());
		assertEquals(0, b.position());
		assertEquals(11, b.limit());
		assertTrue(b.isDirect());
	}
	
	@Test
	public void testEnsureSome() {
		ByteBuffer b = heap.allocate(16);
		
		b.put(ByteUtils.getBytes("3=1,2=2"));
		ByteBuffer b2 = heap.ensureSome(b, 16, 64);
		assertTrue(b == b2);
		assertEquals(16, b2.capacity());
		assertEquals(5, b2.position());
		
		b.put(ByteUtils.getBytes("11=3"));
		b2 = heap.ensureSome(b, 16, 64);
		assertFalse(b == b2);
		assertEquals(32, b2.capacity());
		assertEquals(16, b2.position());
		assertFalse(b2.isDirect());
		b = b2;
		
		b.put(ByteUtils.getBytes("16=4"));
		b2 = heap.ensureSome(b, 16, 64);
		assertFalse(b == b2);
		assertEquals(64, b2.capacity());
		assertEquals(32, b2.position());
		b = b2;

		b.put(ByteUtils.getBytes("32=5"));
		try {
			b2 = heap.ensureSome(b, 16, 64);
			fail("Exception not thrown");
		}
		catch (IndexOutOfBoundsException e) {
		}
		assertEquals(64, b.capacity());
		assertEquals(64, b.position());
		
		byte[] expected = ByteUtils.getBytes("3=1,2=2,11=3,16=4,32=5");
		byte[] data = new byte[64];
		b.flip();
		b.get(data);
		assertEquals(Arrays.toString(expected), Arrays.toString(data));
		assertEquals(0, b.remaining());
		
		//check if direct is set
		b = direct.allocate(16);
		b.put(ByteUtils.getBytes("16=4"));
		b2 = heap.ensureSome(b, 16, 64);
		assertTrue(b != b2);
		assertTrue(b2.isDirect());
		
		//reducing
		b = heap.allocate(64);
		b.put(ByteUtils.getBytes("32=1,16=2,8=3,4=4,2=5,2=6"));
		
		b.flip(); b.get();	b.compact();
		b2 = heap.ensureSome(b, 8, 64);
		assertTrue(b == b2);
		assertEquals(63, b.position());
	
		b.flip(); b.get(new byte[30]); b.compact();
		b2 = heap.ensureSome(b, 8, 64);
		assertTrue(b == b2);
		assertEquals(33, b.position());
		
		b.flip(); b.get(new byte[1]); b.compact();
		b2 = heap.ensureSome(b, 8, 64);
		assertTrue(b == b2);
		assertEquals(32, b.position());

		b.flip(); b.get(new byte[16]); b.compact();
		b2 = heap.ensureSome(b, 8, 64);
		assertTrue(b == b2);
		assertEquals(16, b.position());
		
		byte[] out = new byte[1];
		b.flip(); b.get(out); b.compact();
		b2 = heap.ensureSome(b, 8, 64);
		assertTrue(b != b2);
		assertEquals(32, b2.capacity());
		assertEquals(15, b2.position());
		assertEquals(Arrays.toString(ByteUtils.getBytes("1=3")), Arrays.toString(out));
		b = b2;
		
		out = new byte[12];
		b.flip(); b.get(out); b.compact();
		b2 = heap.ensureSome(b, 8, 64);
		assertTrue(b != b2);
		assertEquals(8, b2.capacity());
		assertEquals(3, b2.position());
		assertEquals(Arrays.toString(ByteUtils.getBytes("7=3,4=4,1=5")), Arrays.toString(out));
		b = b2;
		
		out = new byte[3];
		b.flip(); b.get(out); b.compact();
		b2 = heap.ensureSome(b, 7, 64);
		assertTrue(b != b2);
		assertEquals(7, b2.capacity());
		assertEquals(0, b2.position());
		assertEquals(Arrays.toString(ByteUtils.getBytes("1=5,2=6")), Arrays.toString(out));

		//max is less than double buffer capacity
		b = heap.allocate(16);
		b.put(ByteUtils.getBytes("16=1"));
		b2 = heap.ensureSome(b, 7, 17);
		assertTrue(b != b2);
		assertEquals(17, b2.capacity());
		assertEquals(16, b2.position());
		
	}

	@Test
	public void testEnsure() {
		ByteBuffer b = heap.allocate(16);
		
		ByteBuffer b2 = heap.ensure(b, 5, 16, 64);
		assertTrue(b == b2);
		assertEquals(16, b2.capacity());
		assertEquals(0, b2.position());	
		
		b.put(ByteUtils.getBytes("3=1,2=2"));
		b2 = heap.ensure(b, 5, 16, 64);
		assertTrue(b == b2);
		assertEquals(16, b2.capacity());
		assertEquals(5, b2.position());

		b2 = heap.ensure(b, 10, 16, 64);
		assertTrue(b == b2);
		assertEquals(16, b2.capacity());
		assertEquals(5, b2.position());
		b.put(ByteUtils.getBytes("3=3,7=7"));
		
		b2 = heap.ensure(b, 1, 16, 64);
		assertTrue(b == b2);
		assertEquals(16, b2.capacity());
		assertEquals(15, b2.position());
		b2.put(ByteUtils.getBytes("1=8"));
		
		b2 = heap.ensure(b, 1, 16, 64);
		assertTrue(b != b2);
		assertEquals(64, b2.capacity());
		assertEquals(16, b2.position());
		b2.put(ByteUtils.getBytes("1=9"));
		byte[] out = new byte[17];
		b2.flip(); b2.get(out); b2.compact();
		assertEquals(Arrays.toString(ByteUtils.getBytes("3=1,2=2,3=3,7=7,1=8,1=9")), Arrays.toString(out));
		assertEquals(64, b2.capacity());
		assertEquals(0, b2.position());

		b = b2;
		b.put(ByteUtils.getBytes("4=1"));
		b2 = heap.ensure(b, 60, 16, 64);
		assertTrue(b == b2);
		assertEquals(64, b2.capacity());
		assertEquals(4, b2.position());
		try {
			b2 = heap.ensure(b, 61, 16, 64);
			fail("Exception not thrown");
		}
		catch (IndexOutOfBoundsException e) {
		}
		
		b = heap.allocate(16);
		b.put(ByteUtils.getBytes("4=3"));
		b2 = heap.ensure(b, 61, 16, 128);
		assertTrue(b != b2);
		assertEquals(128, b2.capacity());
		assertEquals(4, b2.position());

		b = heap.allocate(16);
		b.put(ByteUtils.getBytes("4=3"));
		b2 = heap.ensure(b, 61, 16, 128);
		assertTrue(b != b2);
		assertEquals(128, b2.capacity());
		assertEquals(4, b2.position());

		b = heap.allocate(16);
		b.put(ByteUtils.getBytes("4=3"));
		b2 = heap.ensure(b, 61, 16, 127);
		assertTrue(b != b2);
		assertEquals(127, b2.capacity());
		assertEquals(4, b2.position());
		
		int chunk = 1024 * 1024 * 4;
		b = b2;
		b2 = heap.ensure(b, chunk+1, 16, chunk*2);
		assertTrue(b != b2);
		assertEquals(chunk*2, b2.capacity());
		assertEquals(4, b2.position());

		b = b2;
		b2 = heap.ensure(b, chunk*4, 16, chunk*4+4);
		assertTrue(b != b2);
		assertEquals(chunk*4+4, b2.capacity());
		assertEquals(4, b2.position());
        
		b = heap.allocate(16);
		b.put(ByteUtils.getBytes("4=3"));
		b2 = heap.ensure(b, chunk-4, 16, chunk*4);
		assertTrue(b != b2);
		assertEquals(chunk, b2.capacity());
		assertEquals(4, b2.position());
		
	}
	
	@Test
	public void testReduce() {
		ByteBuffer b = heap.allocate(16);
		
		ByteBuffer b2 = heap.reduce(b, 16);
		assertTrue(b == b2);
		assertEquals(16, b2.capacity());
		assertEquals(0, b2.position());
		
		b.put(ByteUtils.getBytes("1=1"));
		b2 = heap.reduce(b, 16);
		assertTrue(b == b2);
		assertEquals(16, b2.capacity());
		assertEquals(1, b2.position());
		
		b.put(ByteUtils.getBytes("15=2"));
		b2 = heap.reduce(b, 16);
		assertTrue(b == b2);
		assertEquals(16, b2.capacity());
		assertEquals(16, b2.position());
		
		b2 = heap.reduce(b, 15);
		assertTrue(b == b2);
		assertEquals(16, b2.capacity());
		assertEquals(16, b2.position());
		
		b.flip();
		b.get();
		b.compact();
		b2 = heap.reduce(b, 15);
		assertTrue(b != b2);
		assertEquals(15, b2.capacity());
		assertEquals(15, b2.position());
		assertFalse(b2.isDirect());
		b = b2;

		b.clear();
		b2 = heap.reduce(b, 10);
		assertTrue(b != b2);
		assertEquals(10, b2.capacity());
		assertEquals(0, b2.position());
		assertFalse(b2.isDirect());
		
		b = direct.allocate(16);
		b2 = heap.reduce(b, 15);
		assertTrue(b != b2);
		assertEquals(15, b2.capacity());
		assertTrue(b2.isDirect());
		b = b2;
		
		b.put(ByteUtils.getBytes("4=1"));
		b2 = heap.reduce(b, 10);
		assertTrue(b != b2);
		assertEquals(10, b2.capacity());
		assertTrue(b2.isDirect());
		
	}	

	@Test
	public void testExtend() {
		ByteBuffer b = heap.allocate(8);
		ByteBuffer b2;
		
		//empty buffer
		b2 = heap.extend(b, 8);
		assertTrue(b2 == b);
		assertEquals(0, b2.position());
		assertEquals(8, b2.limit());
		assertEquals(8, b2.capacity());
		
		b2 = heap.extend(b, 7);
		assertTrue(b2 == b);
		assertEquals(0, b2.position());
		assertEquals(8, b2.limit());
		assertEquals(8, b2.capacity());
		
		b2 = heap.extend(b, 9);
		assertTrue(b2 != b);
		assertEquals(0, b2.position());
		assertEquals(9, b2.limit());
		assertEquals(9, b2.capacity());

		b2 = heap.extend(b, 16);
		assertTrue(b2 != b);
		assertEquals(0, b2.position());
		assertEquals(16, b2.limit());
		assertEquals(16, b2.capacity());
		
		b2 = heap.extend(b, 17);
		assertTrue(b2 != b);
		assertEquals(0, b2.position());
		assertEquals(16, b2.limit());
		assertEquals(16, b2.capacity());
		
		//not empty buffer
		byte[] out = new byte[3];
		b.put(ByteUtils.getBytes("1=1,2=2"));
		b2 = heap.extend(b, 8);
		assertTrue(b2 == b);
		assertEquals(3, b2.position());
		assertEquals(8, b2.limit());
		assertEquals(8, b2.capacity());
		((ByteBuffer)b2.duplicate().flip()).get(out);
		assertEquals(Arrays.toString(ByteUtils.getBytes("1=1,2=2")), Arrays.toString(out));

		b2 = heap.extend(b, 7);
		assertTrue(b2 == b);
		assertEquals(3, b2.position());
		assertEquals(8, b2.limit());
		assertEquals(8, b2.capacity());
		((ByteBuffer)b2.duplicate().flip()).get(out);
		assertEquals(Arrays.toString(ByteUtils.getBytes("1=1,2=2")), Arrays.toString(out));

		b2 = heap.extend(b, 9);
		assertTrue(b2 != b);
		assertEquals(3, b2.position());
		assertEquals(9, b2.limit());
		assertEquals(9, b2.capacity());
		((ByteBuffer)b2.flip()).get(out);
		assertEquals(Arrays.toString(ByteUtils.getBytes("1=1,2=2")), Arrays.toString(out));
		
		b2 = heap.extend(b, 16);
		assertTrue(b2 != b);
		assertEquals(3, b2.position());
		assertEquals(16, b2.limit());
		assertEquals(16, b2.capacity());
		((ByteBuffer)b2.flip()).get(out);
		assertEquals(Arrays.toString(ByteUtils.getBytes("1=1,2=2")), Arrays.toString(out));

		b2 = heap.extend(b, 17);
		assertTrue(b2 != b);
		assertEquals(3, b2.position());
		assertEquals(16, b2.limit());
		assertEquals(16, b2.capacity());
		((ByteBuffer)b2.flip()).get(out);
		assertEquals(Arrays.toString(ByteUtils.getBytes("1=1,2=2")), Arrays.toString(out));
	}
	
}
