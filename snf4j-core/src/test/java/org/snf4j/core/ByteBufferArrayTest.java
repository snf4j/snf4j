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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.InvalidMarkException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ByteBufferArrayTest {
	
	ByteBuffer[] bufs(int off, int pad, int... sizes) {
		ByteBuffer[] bufs = new ByteBuffer[sizes.length + off + pad];
		int b = 1;
		
		for (int i=0; i<sizes.length; ++i) {
			byte[] a = new byte[sizes[i]];
			for (int j=0; j<a.length; ++j) {
				a[j] = (byte) b++; 
			}
			bufs[i+off] = ByteBuffer.wrap(a);
		}
		return bufs;
	}
	
	ByteBufferArray array(int off, int pad, int... sizes) {
		if (pad == -1) {
			return new ByteBufferArray(bufs(off, 0, sizes));
		}
		return new ByteBufferArray(bufs(off, pad, sizes), off, sizes.length);
	}
	
	@Test
	public void testConstructors() {
		ByteBuffer[] bufs = bufs(3,5,4,4,4,4,4,4,4,4,4,4);
		ByteBufferArray array = new ByteBufferArray(bufs, 3, 5);
		assertSame(bufs, array.array());
		assertEquals(3, array.arrayOffset());
		assertEquals(5, array.size());
		assertEquals(8, array.arrayEnd());
		assertEquals(3, array.arrayIndex());

		bufs = bufs(0,0,4,4,4,4,4,4,4,4,4,4);
		array = new ByteBufferArray(bufs);
		assertSame(bufs, array.array());
		assertEquals(0, array.arrayOffset());
		assertEquals(10, array.size());
		assertEquals(10, array.arrayEnd());
		assertEquals(0, array.arrayIndex());
	}

	void testSize(int off, int pad) {
		assertEquals(2, array(off, pad, 3, 4).size());
		assertEquals(1, array(off, pad, 3).size());
		assertEquals(0, array(off, pad).size());
	}
	
	@Test
	public void testSize() {
		testSize(3,7);
		testSize(0,7);
		testSize(3,0);
		testSize(0,0);
		testSize(0,-1);
	}

	void testArrayIndex(int off, int pad) {
		ByteBufferArray array = array(off, pad, 3, 4);
		assertEquals(off, array.arrayIndex());
		array.get();
		assertEquals(off, array.arrayIndex());
		array.getShort();
		assertEquals(off+1, array.arrayIndex());
		array.get();
		assertEquals(off+1, array.arrayIndex());
		array.getShort();
		assertEquals(off+1, array.arrayIndex());
		array.get();
		assertEquals(off+2, array.arrayIndex());
		assertFalse(array.hasRemaining());
	}
	
	@Test
	public void testArrayIndex() {
		testArrayIndex(3,7);
		testArrayIndex(0,7);
		testArrayIndex(3,0);
		testArrayIndex(0,0);
		testArrayIndex(0,-1);
	}

	void testMark(int off, int pad) {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3}));
		bufs.add(ByteBuffer.wrap(new byte[] {4,5,6,7}));

		ByteBufferArray array = array(off, pad, 3, 4);
		try {
			array.reset();
			fail();
		} catch (InvalidMarkException e) {}
		assertSame(array, array.mark());
		assertEquals(1, array.get());
		assertEquals(2, array.get());
		assertEquals(5, array.remaining());
		assertSame(array, array.reset());
		assertEquals(7, array.remaining());
		assertEquals(1, array.get());
		
		array = array(off, pad, 3, 4, 3);
		array.position(9);
		assertEquals(1, array.remaining());
		array.mark();
		array.array()[off+2].limit(2);
		array.reset();
		assertEquals(0, array.remaining());
		array.array()[off+2].limit(1);
		try {
			array.reset();
			fail();
		} catch (InvalidMarkException e) {}
		assertEquals(0, array.remaining());
	}
	
	@Test
	public void testMark() {
		testMark(3,7);
		testMark(0,7);
		testMark(3,0);
		testMark(0,0);
		testMark(0,-1);
	}

	void testRewind(int off, int pad) {
		ByteBufferArray array = array(off, pad, 3, 4);
		assertEquals(1, array.get());
		array.mark();
		assertEquals(2, array.get());
		array.reset();
		assertEquals(2, array.get());
		array.rewind();
		assertEquals(7, array.remaining());
		assertEquals(1, array.get());
		try {
			array.reset();
			fail();
		} catch (InvalidMarkException e) {}
		assertEquals(0x02030405, array.getInt());
		array.mark();
		assertEquals(2, array.remaining());
		array.rewind();
		assertEquals(7, array.remaining());
		try {
			array.reset();
			fail();
		} catch (InvalidMarkException e) {}
		
		
		array = array(off, pad);
		array.mark();
		array.reset();
		array.rewind();
		try {
			array.reset();
			fail();
		} catch (InvalidMarkException e) {}
	}
	
	@Test
	public void testRewind() {
		testRewind(3,7);
		testRewind(0,7);
		testRewind(3,0);
		testRewind(0,0);
		testRewind(0,-1);
	}

	void testLimit(int off, int pad) {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3}));
		bufs.add(ByteBuffer.wrap(new byte[] {4,5,6,7}));

		ByteBufferArray array = array(off, pad, 3, 4);
		assertEquals(7, array.limit());
		
		array = array(off, pad, 3, 4, 3);
		assertEquals(10, array.limit());
		array.array()[off+2].limit(2);
		assertEquals(9, array.limit());
		
		assertEquals(0, array(off, pad).limit());
	}
	
	@Test
	public void testLimit() {
		testLimit(3,7);
		testLimit(0,7);
		testLimit(3,0);
		testLimit(0,0);
		testLimit(0,-1);
	}

	void testPosition(int off, int pad) {
		ByteBufferArray array = array(off, pad, 3, 3);
		for (int i=0; i<6; ++i) {
			assertEquals(i, array.position());
			array.get();
		}
		assertEquals(6, array.position());
		assertEquals(0, array(off, pad).position());
		
		try {
			array.position(-1);
			fail();
		} catch (IllegalArgumentException e) {}
		try {
			array.position(7);
			fail();
		} catch (IllegalArgumentException e) {}

		assertEquals(6, array.position());
		for (int i=0; i<6; ++i) {
			assertSame(array, array.position(i));
			assertEquals(i, array.position());
			if (array.hasRemaining()) {
				assertEquals(i+1, array.get());
			}
		}
		array.position(6);
		
		//test mark
		assertEquals(6, array.position());
		assertFalse(array.hasRemaining());
		array.position(3);
		array.mark();
		assertEquals(3, array.remaining());
		array.position(3);
		assertEquals(3, array.remaining());
		array.reset();
		assertEquals(3, array.remaining());
		array.position(4);
		assertEquals(2, array.remaining());
		array.position(2);
		try {
			array.reset();
			fail();
		} catch (InvalidMarkException e) {}
		
		array = array(off, pad);
		array.position(0);
		try {
			array.position(-1);
			fail();
		} catch (IllegalArgumentException e) {}
		try {
			array.position(1);
			fail();
		} catch (IllegalArgumentException e) {}
	}
	
	@Test
	public void testPosition() {
		testPosition(3,7);
		testPosition(0,7);
		testPosition(3,0);
		testPosition(0,0);
		testPosition(0,-1);
	}

	void testRemaining(int off, int pad) {
		ByteBufferArray array = array(off, pad, 3, 3);
		assertEquals(6, array.remaining());
		assertTrue(array.hasRemaining(1));
		assertTrue(array.hasRemaining(6));
		assertFalse(array.hasRemaining(7));
		assertTrue(array.hasRemaining());
		array.array()[off].get();
		assertEquals(5, array.remaining());
		assertTrue(array.hasRemaining());
		array.array()[off].get();
		array.array()[off].get();
		assertEquals(3, array.remaining());
		assertTrue(array.hasRemaining(1));
		assertTrue(array.hasRemaining(3));
		assertFalse(array.hasRemaining(4));
		assertTrue(array.hasRemaining());
		array.array()[off+1].get();
		array.array()[off+1].get();
		array.array()[off+1].get();
		assertEquals(0, array.remaining());
		assertFalse(array.hasRemaining());
		array.array()[off].clear();
		array.array()[off].put((byte) 1).flip();
		assertEquals(1, array.remaining());
		assertTrue(array.hasRemaining());
		assertFalse(array(off, pad).hasRemaining());
		
		array = array(off, pad, 3, 3);
		array.get();
		array.get();
		assertEquals(4, array.remaining());
		assertTrue(array.hasRemaining());
		array.get();
		array.get();
		assertEquals(2, array.remaining());
		assertTrue(array.hasRemaining(0));
		assertTrue(array.hasRemaining(2));
		assertFalse(array.hasRemaining(3));
		assertTrue(array.hasRemaining());
		array.array()[off].clear();
		array.array()[off].put((byte) 1).flip();
		assertEquals(2, array.remaining());
		assertTrue(array.hasRemaining());
		array.get();
		array.get();
		assertEquals(0, array.remaining());
		assertFalse(array.hasRemaining());
		assertEquals(0, array(off, pad).remaining());
	}
	
	@Test
	public void testRemaining() {
		testRemaining(3,7);
		testRemaining(0,7);
		testRemaining(3,0);
		testRemaining(0,0);
		testRemaining(0,-1);
	}

	void testEmptyArray(int off, int pad) {
		ByteBufferArray array = array(off, pad);
		assertEquals(0, array.remaining());
		assertFalse(array.hasRemaining());
		try {
			array.get(0);
			fail();
		}
		catch (IndexOutOfBoundsException e) {}
		try {
			array.get();
			fail();
		}
		catch (BufferUnderflowException e) {}
	}
	
	@Test
	public void testEmptyArray() {
		testEmptyArray(3,7);
		testEmptyArray(0,7);
		testEmptyArray(3,0);
		testEmptyArray(0,0);
		testEmptyArray(0,-1);
	}

	void testGet(int off, int pad) {
		ByteBufferArray array = array(off, pad, 3, 3);
		assertEquals(1, array.get());
		assertEquals(2, array.get());
		assertEquals(3, array.get());
		assertEquals(4, array.get());
		assertEquals(5, array.get());
		assertEquals(6, array.get());
		try {
			array.get();
			fail();
		}
		catch (BufferUnderflowException e) {}
	}
	
	@Test
	public void testGet() {
		testGet(3,7);
		testGet(0,7);
		testGet(3,0);
		testGet(0,0);
		testGet(0,-1);
	}

	@Test
	public void testGetUnsigned() {
		ByteBufferArray array = ByteBufferArray.wrap(new ByteBuffer[] {ByteBuffer.wrap(new byte[] {
				(byte) 0xff, (byte)0x7f})});

		assertEquals(0xff, array.getUnsigned(0));
		assertEquals(0x7f, array.getUnsigned(1));
		assertEquals(0xff, array.getUnsigned());
		assertEquals(0x7f, array.getUnsigned());
	}
	
	void testAbsoluteGet(int off, int pad) {
		ByteBufferArray array = array(off, pad, 3, 3);
		assertEquals(1, array.get(0));
		assertEquals(2, array.get(1));
		assertEquals(3, array.get(2));
		assertEquals(4, array.get(3));
		assertEquals(5, array.get(4));
		assertEquals(6, array.get(5));
		try {
			array.get(6);
			fail();
		}
		catch (IndexOutOfBoundsException e) {}
		try {
			array.get(-1);
			fail();
		}
		catch (IndexOutOfBoundsException e) {}
		
		array.array()[off].get();
		array.array()[off+1].get();
		assertEquals(1, array.get(0));
		assertEquals(2, array.get(1));
		assertEquals(3, array.get(2));
		assertEquals(4, array.get(3));
		assertEquals(5, array.get(4));
		assertEquals(6, array.get(5));
		
		array.array()[off].limit(2);
		array.array()[off+1].limit(2);
		assertEquals(1, array.get(0));
		assertEquals(2, array.get(1));
		assertEquals(4, array.get(2));
		assertEquals(5, array.get(3));
		try {
			array.get(4);
			fail();
		}
		catch (IndexOutOfBoundsException e) {}
	}
	
	@Test
	public void testAbsoluteGet() {
		testAbsoluteGet(3,7);
		testAbsoluteGet(0,7);
		testAbsoluteGet(3,0);
		testAbsoluteGet(0,0);
		testAbsoluteGet(0,-1);
	}

	void testGetBytes(int off, int pad) {
		byte[] data = new byte[5];
		
		ByteBufferArray array = array(off, pad, 6, 6, 2, 2, 2, 2, 2);
		assertEquals(22, array.remaining());
		assertSame(array, array.get(data, 0, 2));
		assertArrayEquals(new byte[] {1,2,0,0,0}, data);
		assertEquals(20, array.remaining());
		array.get(data, 3, 2);
		assertArrayEquals(new byte[] {1,2,0,3,4}, data);
		assertEquals(18, array.remaining());
		array.get(data, 1, 4);
		assertArrayEquals(new byte[] {1,5,6,7,8}, data);
		assertEquals(14, array.remaining());
		array.get(data);
		assertArrayEquals(new byte[] {9,10,11,12,13}, data);
		assertEquals(9, array.remaining());
		array.get(data);
		assertArrayEquals(new byte[] {14,15,16,17,18}, data);
		assertEquals(4, array.remaining());
		try {
			array.get(data);
			fail();
		}
		catch (BufferUnderflowException e) {}
		assertArrayEquals(new byte[] {14,15,16,17,18}, data);

		data = new byte[4];
		array.get(data);
		assertArrayEquals(new byte[] {19,20,21,22}, data);
		assertEquals(0, array.remaining());
		array.get(data,1,0);
		assertArrayEquals(new byte[] {19,20,21,22}, data);
		
		try {
			array.get(data, 0, 1);
			fail();
		}
		catch (BufferUnderflowException e) {}
		
		try {
			array.get(data, 0, -1);
			fail();
		}
		catch (IndexOutOfBoundsException e) {}
	}
	
	@Test
	public void testGetBytes() {
		testGetBytes(3,7);
		testGetBytes(0,7);
		testGetBytes(3,0);
		testGetBytes(0,0);
		testGetBytes(0,-1);
	}

	void testAbsoluteGetChar(int off, int pad) {
		ByteBufferArray array = array(off, pad, 6, 6, 2);
		array.array()[off].putChar(1, 'a');
		assertEquals('a', array.getChar(1));
		byte b = array.array()[off].get(2);
		array.array()[off].limit(2);
		assertNotEquals('a', array.getChar(1));
		array.array()[off+1].put(0, b);
		assertEquals('a', array.getChar(1));
	}

	void testGetBytesIntoBuffer(int off, int pad) {
		ByteBuffer buf = ByteBuffer.allocate(20);

		ByteBufferArray array = array(off, pad, 4, 4, 2, 2, 2, 2);
		assertSame(array, array.get(buf, 3));
		buf.flip();
		assertEquals(3, buf.remaining());
		assertEquals(1,buf.get());
		assertEquals(2,buf.get());
		assertEquals(3,buf.get());
		assertEquals(13, array.remaining());
		assertEquals(4, array.get());
		
		array.rewind();
		assertEquals(16, array.remaining());
		buf.clear();
		array.get(buf,4);
		buf.flip();
		assertEquals(4, buf.remaining());
		assertEquals(12, array.remaining());
		assertEquals(0x01020304, buf.getInt());
		assertEquals(5, array.get());
		
		array.rewind();
		buf.clear();
		array.get(buf,7);
		buf.flip();
		assertEquals(7, buf.remaining());
		assertEquals(9, array.remaining());
		assertEquals(0x01020304, buf.getInt());
		assertEquals(0x0506, buf.getShort());
		assertEquals(7, buf.get());
		assertEquals(8, array.get());

		array.rewind();
		buf.clear();
		array.get(buf,8);
		buf.flip();
		assertEquals(8, buf.remaining());
		assertEquals(8, array.remaining());
		assertEquals(0x01020304, buf.getInt());
		assertEquals(0x05060708, buf.getInt());
		assertEquals(9, array.get());
		
		array.rewind();
		buf.clear();
		array.get(buf,16);
		buf.flip();
		assertEquals(16, buf.remaining());
		assertEquals(0, array.remaining());
		assertEquals(0x01020304, buf.getInt());
		assertEquals(0x05060708, buf.getInt());
		assertEquals(0x090a0b0c, buf.getInt());
		assertEquals(0x0d0e0f10, buf.getInt());
		
		array.rewind();
		buf.clear();
		try {
			array.get(buf,17);
			fail();
		} catch (BufferUnderflowException e) {}
		assertEquals(0, buf.position());
		assertEquals(16, array.remaining());
	
		array.get(buf, 0);
		assertEquals(0, buf.position());
		assertEquals(16, array.remaining());
		try {
			array.get(buf, -1);
			fail();
		} catch (IndexOutOfBoundsException e) {}
		buf.limit(8);
		try {
			array.get(buf, 9);
			fail();
		} catch (IndexOutOfBoundsException e) {}
		array.get(buf, 8);
		assertEquals(0, buf.remaining());
		assertEquals(8, array.remaining());
		buf.flip();
		assertEquals(0x01020304, buf.getInt());
		assertEquals(0x05060708, buf.getInt());
		
	}
	
	@Test
	public void testGetBytesIntoBuffer() {
		testGetBytesIntoBuffer(3,7);
		testGetBytesIntoBuffer(0,7);
		testGetBytesIntoBuffer(3,0);
		testGetBytesIntoBuffer(0,0);
		testGetBytesIntoBuffer(0,-1);
	}
	
	@Test
	public void testAbsoluteGetChar() {
		testAbsoluteGetChar(3,7);
		testAbsoluteGetChar(0,7);
		testAbsoluteGetChar(3,0);
		testAbsoluteGetChar(0,0);
		testAbsoluteGetChar(0,-1);
	}

	void testGetChar(int off, int pad) {
		ByteBufferArray array = array(off, pad, 6, 6, 2);
		array.array()[off].putChar(0, 'a');
		array.array()[off].putChar(4, 'x');
		array.array()[off+1].putChar(0, 'B');
		array.array()[off+2].put(0, array.array()[off+1].get(1));
		array.array()[off+1].limit(1);

		assertEquals('a', array.getChar());
		assertEquals(0x0304, array.getShort());
		assertEquals('x', array.getChar());
		assertEquals('B', array.getChar());
		try {
			array.getChar();
			fail();
		}
		catch (BufferUnderflowException e) {}
		assertEquals(14, array.get());
	}
	
	@Test
	public void testGetChar() {
		testGetChar(3,7);
		testGetChar(0,7);
		testGetChar(3,0);
		testGetChar(0,0);
		testGetChar(0,-1);
	}

	void testAbsoluteGetShort(int off, int pad) {
		ByteBufferArray array = array(off, pad, 6, 6);
		assertEquals(0x0203, array.getShort(1));
		array.array()[off].limit(2);
		assertEquals(0x0207, array.getShort(1));
		assertEquals(0x0207, array.getShort(1));
		assertEquals(0x0b0c, array.getShort(6));
		try {
			array.getShort(7);
			fail();
		}
		catch (IndexOutOfBoundsException e) {}
	}
	
	@Test
	public void testAbsoluteGetShort() {
		testAbsoluteGetShort(3,7);
		testAbsoluteGetShort(0,7);
		testAbsoluteGetShort(3,0);
		testAbsoluteGetShort(0,0);
		testAbsoluteGetShort(0,-1);
	}

	void testGetShort(int off, int pad) {
		ByteBufferArray array = array(off, pad, 6, 1, 4);
		assertEquals(0x0102, array.getShort());
		assertEquals(0x0304, array.getShort());
		assertEquals(0x0506, array.getShort());
		assertEquals(0x0708, array.getShort());
		assertEquals(0x090a, array.getShort());
		try {
			array.getShort();
			fail();
		}
		catch (BufferUnderflowException e) {}
		assertEquals(11, array.get());
		try {
			array.getShort();
			fail();
		}
		catch (BufferUnderflowException e) {}

		array = array(off, pad, 2);
		assertEquals(0x0102, array.getShort());
	}
	
	@Test
	public void testGetShort() {
		testGetShort(3,7);
		testGetShort(0,7);
		testGetShort(3,0);
		testGetShort(0,0);
		testGetShort(0,-1);
	}

	@Test
	public void testGetUnsignedShort() {
		ByteBufferArray array = ByteBufferArray.wrap(new ByteBuffer[] {ByteBuffer.wrap(new byte[] {
				(byte) 0xff, (byte) 0xff, (byte)0x7f, (byte) 0xff})});

		assertEquals(0xffff, array.getUnsignedShort(0));
		assertEquals(0x7fff, array.getUnsignedShort(2));
		assertEquals(0xffff, array.getUnsignedShort());
		assertEquals(0x7fff, array.getUnsignedShort());
	}
	
	void testAbsoluteGetInt(int off, int pad) {
		ByteBufferArray array = array(off, pad, 6, 6, 2);
		assertEquals(0x01020304, array.getInt(0));
		assertEquals(0x02030405, array.getInt(1));
		assertEquals(0x03040506, array.getInt(2));
		assertEquals(0x04050607, array.getInt(3));
		assertEquals(0x05060708, array.getInt(4));
		assertEquals(0x06070809, array.getInt(5));
		assertEquals(0x0708090a, array.getInt(6));
		assertEquals(0x08090a0b, array.getInt(7));
		assertEquals(0x090a0b0c, array.getInt(8));
		assertEquals(0x0a0b0c0d, array.getInt(9));
		assertEquals(0x0b0c0d0e, array.getInt(10));
		try {
			array.getInt(11);
			fail();
		}
		catch (IndexOutOfBoundsException e) {}
		try {
			array.getInt(14);
			fail();
		}
		catch (IndexOutOfBoundsException e) {}
		
		array.array()[off].limit(2);
		array.array()[off+1].limit(1);
		assertEquals(0x0102070d, array.getInt(0));
		assertEquals(0x02070d0e, array.getInt(1));
		try {
			array.getInt(2);
			fail();
		}
		catch (IndexOutOfBoundsException e) {}
		
		array.array()[off].order(ByteOrder.LITTLE_ENDIAN);
		array.array()[off+1].order(ByteOrder.LITTLE_ENDIAN);
		array.array()[off+2].order(ByteOrder.LITTLE_ENDIAN);
		assertEquals(0x0d070201, array.getInt(0));
	}
	
	@Test
	public void testAbsoluteGetInt() {
		testAbsoluteGetInt(3,7);
		testAbsoluteGetInt(0,7);
		testAbsoluteGetInt(3,0);
		testAbsoluteGetInt(0,0);
		testAbsoluteGetInt(0,-1);
	}

	void testGetInt(int off, int pad) {
		ByteBufferArray array = array(off, pad, 6, 6, 3);
		assertEquals(0x01020304, array.getInt());
		assertEquals(0x05060708, array.getInt());
		assertEquals(0x090a0b0c, array.getInt());
		try {
			array.getInt();
			fail();
		}
		catch (BufferUnderflowException e) {}
		assertEquals(13, array.get());
		try {
			array.getInt();
			fail();
		}
		catch (BufferUnderflowException e) {}
		assertEquals(14, array.get());
		try {
			array.getInt();
			fail();
		}
		catch (BufferUnderflowException e) {}
		assertEquals(15, array.get());
		try {
			array.getInt();
			fail();
		}
		catch (BufferUnderflowException e) {}

		array = array(off, pad, 4);
		assertEquals(0x01020304, array.getInt());

		array = array(off, pad, 5, 3);
		array.array()[off].order(ByteOrder.LITTLE_ENDIAN);
		assertEquals(0x04030201, array.getInt());
		assertEquals(0x08070605, array.getInt());
	}
	
	@Test
	public void testGetInt() {
		testGetInt(3,7);
		testGetInt(0,7);
		testGetInt(3,0);
		testGetInt(0,0);
		testGetInt(0,-1);
	}

	@Test
	public void testGetUnsignedInt() {
		ByteBufferArray array = ByteBufferArray.wrap(new ByteBuffer[] {ByteBuffer.wrap(new byte[] {
				(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 
				(byte) 0x7f, (byte) 0xff, (byte) 0xff, (byte) 0xff})});

		assertEquals(0xffffffffL, array.getUnsignedInt(0));
		assertEquals(0x7fffffffL, array.getUnsignedInt(4));
		assertEquals(0xffffffffL, array.getUnsignedInt());
		assertEquals(0x7fffffffL, array.getUnsignedInt());
	}
	
	void testAbsoluteGetLong(int off, int pad) {
		ByteBufferArray array = array(off, pad, 8, 4, 8);
		assertEquals(0x0102030405060708L, array.getLong(0));
		assertEquals(0x0203040506070809L, array.getLong(1));
		assertEquals(0x0d0e0f1011121314L, array.getLong(12));
		array.getLong(12);
		try {
			array.getLong(13);
			fail();
		}
		catch (IndexOutOfBoundsException e) {}
	}
	
	@Test
	public void testAbsoluteGetLong() {
		testAbsoluteGetLong(3,7);
		testAbsoluteGetLong(0,7);
		testAbsoluteGetLong(3,0);
		testAbsoluteGetLong(0,0);
		testAbsoluteGetLong(0,-1);
	}

	void testGetLong(int off, int pad) {
		ByteBufferArray array = array(off, pad, 8, 4, 11);
		assertEquals(0x0102030405060708L, array.getLong());
		assertEquals(0x090a0b0c0d0e0f10L, array.getLong());
		try {
			array.getLong();
			fail();
		}
		catch (BufferUnderflowException e) {}
		assertEquals(0x11121314, array.getInt());
		assertEquals(0x1516, array.getShort());
		try {
			array.getLong();
			fail();
		}
		catch (BufferUnderflowException e) {}
		assertEquals(0x17, array.get());
		try {
			array.getLong();
			fail();
		}
		catch (BufferUnderflowException e) {}
	}
	
	@Test
	public void testGetLong() {
		testGetLong(3,7);
		testGetLong(0,7);
		testGetLong(3,0);
		testGetLong(0,0);
		testGetLong(0,-1);
	}

	void testAbsoluteGetFloat(int off, int pad) {
		ByteBufferArray array = array(off, pad, 8, 4, 8);
		array.array()[off].putFloat(1, 43323.432F);
		array.array()[off+1].put(0, array.array()[off].get(4));
		assertTrue(43323.432F == array.getFloat(1));
		array.array()[off].limit(4);
		assertTrue(43323.432F == array.getFloat(1));
		array.getFloat(12);
		try {
			array.getFloat(13);
			fail();
		}
		catch (IndexOutOfBoundsException e) {}
	}
	
	@Test
	public void testAbsoluteGetFloat() {
		testAbsoluteGetFloat(3,7);
		testAbsoluteGetFloat(0,7);
		testAbsoluteGetFloat(3,0);
		testAbsoluteGetFloat(0,0);
		testAbsoluteGetFloat(0,-1);
	}

	void testGetFloat(int off, int pad) {
		ByteBufferArray array = array(off, pad, 8, 4);
		array.array()[off].putFloat(0, 43323.432F);
		array.array()[off].putFloat(4, 65523.33432F);
		array.array()[off+1].put(0, array.array()[off].get(7));
		array.array()[off].limit(7);

		assertTrue(43323.432F == array.getFloat());
		assertTrue(65523.33432F == array.getFloat());
		try {
			array.getFloat();
			fail();
		}
		catch (BufferUnderflowException e) {}
	}
	
	@Test
	public void testGetFloat() {
		testGetFloat(3,7);
		testGetFloat(0,7);
		testGetFloat(3,0);
		testGetFloat(0,0);
		testGetFloat(0,-1);
	}

	void testAbsoluteGetDouble(int off, int pad) {
		ByteBufferArray array = array(off, pad, 9, 3, 8);
		array.array()[off].putDouble(1, 4332343434.32554);
		array.array()[off+1].put(0, array.array()[off].get(8));

		assertTrue(4332343434.32554 == array.getDouble(1));
		array.array()[off].limit(8);
		assertTrue(4332343434.32554 == array.getDouble(1));
		array.getDouble(11);
		try {
			array.getDouble(12);
			fail();
		}
		catch (IndexOutOfBoundsException e) {}
	}
	
	@Test
	public void testAbsoluteGetDouble() {
		testAbsoluteGetDouble(3,7);
		testAbsoluteGetDouble(0,7);
		testAbsoluteGetDouble(3,0);
		testAbsoluteGetDouble(0,0);
		testAbsoluteGetDouble(0,-1);
	}

	void testGetDouble(int off, int pad) {
		ByteBufferArray array = array(off, pad, 9, 3, 8);
		array.array()[off].putDouble(0, 4332343434.32554);
		array.array()[off+1].put(0, array.array()[off].get(7));
		assertTrue(4332343434.32554 == array.getDouble());
		array.array()[off].limit(7);
		array.array()[off].position(0);
		assertTrue(4332343434.32554 == array.getDouble());
		assertEquals(0x0b0c, array.getShort());
		assertEquals(0x0d, array.get());
		try {
			array.getDouble();
			fail();
		}
		catch (BufferUnderflowException e) {}
		assertEquals(0x0e0f1011, array.getInt());
		assertEquals(0x1213, array.getShort());
		try {
			array.getDouble();
			fail();
		}
		catch (BufferUnderflowException e) {}
	}

	@Test
	public void testGetDouble() {
		testGetDouble(3,7);
		testGetDouble(0,7);
		testGetDouble(3,0);
		testGetDouble(0,0);
		testGetDouble(0,-1);
	}

	void testDuplicate(int off, int pad) {
		ByteBufferArray array1 = array(off, pad, 9, 3, 8);
		assertEquals(20, array1.remaining());
		array1.mark();
		ByteBufferArray array2 = array1.duplicate();
		assertEquals(20, array2.remaining());
		assertEquals(0x01020304, array1.getInt());
		assertEquals(0x0102, array2.getShort());
		array2.reset();
		assertEquals(20, array2.remaining());
		assertEquals(0x0102, array2.getShort());
		array2 = array1.duplicate();
		assertEquals(0x05060708, array2.getInt());
		assertEquals(0x0090a, array2.getShort());
		assertEquals(5, array1.get());
		array1 = array2.duplicate();
		assertEquals(0x00b0c, array1.getShort());
		array2 = new ByteBufferArray(new ByteBuffer[0]).duplicate();
		assertEquals(0, array2.position());
		assertEquals(0, array2.limit());

		array1 = array(off, pad, 9, 3);
		array2 = array1.duplicate();
		array1.mark();
		try {
			array2.reset();
			fail();
		} catch (InvalidMarkException e) {}
	}	
	
	@Test
	public void testDuplicate() {
		testDuplicate(3,7);
		testDuplicate(0,7);
		testDuplicate(3,0);
		testDuplicate(0,0);
		testDuplicate(0,-1);
	}	
	
	@Test
	public void testWrap() {
		ByteBufferArray array = ByteBufferArray.wrap(bufs(0,0,10));
		assertSame(ByteBufferArray.OneByteBufferArray.class, array.getClass());
		assertEquals(0, array.arrayOffset());
		assertEquals(1, array.arrayEnd());
		assertEquals(1, array.size());
		
		array = ByteBufferArray.wrap(bufs(0,0));
		assertSame(ByteBufferArray.class, array.getClass());
		assertEquals(0, array.arrayOffset());
		assertEquals(0, array.arrayEnd());
		assertEquals(0, array.size());
		
		array = ByteBufferArray.wrap(bufs(0,0,10,10));
		assertSame(ByteBufferArray.class, array.getClass());
		assertEquals(0, array.arrayOffset());
		assertEquals(2, array.arrayEnd());
		assertEquals(2, array.size());

		array = ByteBufferArray.wrap(bufs(3,5,10),3,1);
		assertSame(ByteBufferArray.OneByteBufferArray.class, array.getClass());
		assertEquals(3, array.arrayOffset());
		assertEquals(4, array.arrayEnd());
		assertEquals(1, array.size());
		
		array = ByteBufferArray.wrap(bufs(3,5),3,0);
		assertSame(ByteBufferArray.class, array.getClass());
		assertEquals(3, array.arrayOffset());
		assertEquals(3, array.arrayEnd());
		assertEquals(0, array.size());
		
		array = ByteBufferArray.wrap(bufs(3,5,10,10),3,2);
		assertSame(ByteBufferArray.class, array.getClass());
		assertEquals(3, array.arrayOffset());
		assertEquals(5, array.arrayEnd());
		assertEquals(2, array.size());
	
	}
	
	@Test 
	public void testOneByteBufferArray() {
		ByteBuffer b1 = ByteBuffer.allocate(20);
		ByteBuffer b2 = ByteBuffer.allocate(20);
		ByteBuffer b3 = ByteBuffer.allocate(20);
		ByteBuffer[] a = new ByteBuffer[] {b1};
		
		ByteBufferArray dup, ba = ByteBufferArray.wrap(a);
		assertSame(ByteBufferArray.OneByteBufferArray.class, ba.getClass());
		assertSame(a, ba.array());
		assertEquals(1, ba.arrayEnd());
		assertEquals(0, ba.arrayOffset());
		assertEquals(0, ba.arrayIndex());
		assertEquals(1, ba.size());
		b1.putInt(0x01020304).flip();
		
		//duplicate
		dup = ba.duplicate();
		assertSame(ByteBufferArray.OneByteBufferArray.class, dup.getClass());
		assertNotSame(dup, ba);
		assertNotSame(dup.array(), ba.array());
		assertNotSame(dup.array()[0], ba.array()[0]);
		assertEquals(1, dup.array().length);
		assertEquals(1, dup.arrayEnd());
		assertEquals(0, dup.arrayIndex());
		assertEquals(0, dup.arrayOffset());
		assertEquals(4, dup.remaining());
		assertEquals(0x01020304, dup.getInt());
		
		assertEquals(1, ba.get());
		byte[] b = new byte[2];
		assertSame(ba, ba.get(b));
		assertArrayEquals(new byte[] {2,3}, b);
		assertSame(ba, ba.get(b, 1, 1));
		assertArrayEquals(new byte[] {2,4}, b);
		assertEquals(2, ba.get(1));
		b1.clear();
		b1.putChar('a').putChar('b').flip();
		assertEquals('a', ba.getChar());
		assertEquals('b', ba.getChar(2));
		b1.clear();
		b1.putDouble(3443.9837).putDouble(1111.454).flip();
		assertTrue(3443.9837 == ba.getDouble());
		assertTrue(1111.454 == ba.getDouble(8));
		b1.clear();
		b1.putFloat(3443.9837F).putFloat(1111.454F).flip();
		assertTrue(3443.9837F == ba.getFloat());
		assertTrue(1111.454F == ba.getFloat(4));
		b1.clear();
		b1.putInt(0x01020304).putInt(0x05060708).flip();
		assertEquals(0x01020304, ba.getInt());
		assertEquals(0x05060708, ba.getInt(4));
		b1.clear();
		b1.putLong(0x0102030405060708L).putLong(0x090a0b0c0d0e0f10L).flip();
		assertEquals(0x0102030405060708L, ba.getLong());
		assertEquals(0x090a0b0c0d0e0f10L, ba.getLong(8));
		b1.clear();
		b1.putShort((short) 0x0102).putShort((short) 0x0304).flip();
		assertEquals((short) 0x0102, ba.getShort());
		assertEquals((short) 0x0304, ba.getShort(2));
		
		b1.clear().flip();
		assertEquals(0, ba.remaining());
		assertFalse(ba.hasRemaining());
		b1.clear();
		b1.putInt(0x01020304).flip();
		assertEquals(4, ba.remaining());
		assertTrue(ba.hasRemaining(0));
		assertTrue(ba.hasRemaining(4));
		assertFalse(ba.hasRemaining(5));
		assertTrue(ba.hasRemaining());
		ba.getShort();
		assertEquals(2, ba.remaining());
		assertTrue(ba.hasRemaining());
		b1.getShort();
		assertEquals(0, ba.remaining());
		assertFalse(ba.hasRemaining());
		
		assertEquals(4, ba.limit());
		b1.clear();
		assertEquals(20, ba.limit());
		assertEquals(0, ba.position());
		b1.putLong(1).flip();
		assertEquals(0, ba.position());
		b1.get();
		assertEquals(1, ba.position());
		ba.get();
		assertEquals(2, ba.position());
		ba.position(5);
		assertEquals(5, ba.position());
		assertEquals(5, b1.position());
		
		ba.mark();
		b1.get();
		assertEquals(6, b1.position());
		ba.reset();
		assertEquals(5, b1.position());
		ba.get();
		assertEquals(6, ba.position());
		b1.reset();
		assertEquals(5, ba.position());
		
		assertEquals(8, ba.limit());
		ba.rewind();
		assertEquals(0, ba.position());
		assertEquals(0, b1.position());
		assertEquals(8, ba.limit());

		a = new ByteBuffer[] {b1,b2,b3};
		ba = ByteBufferArray.wrap(a, 1, 1);
		assertSame(ByteBufferArray.OneByteBufferArray.class, ba.getClass());
		assertSame(a, ba.array());
		assertEquals(2, ba.arrayEnd());
		assertEquals(1, ba.arrayIndex());
		assertEquals(1, ba.arrayOffset());
		assertEquals(1, ba.size());
		b2.putInt(0x01020304).flip();
		assertEquals(4, ba.remaining());
		b2.get();
		assertEquals(3, ba.remaining());
		assertEquals(2, ba.get());

		dup = ba.duplicate();
		assertSame(ByteBufferArray.OneByteBufferArray.class, dup.getClass());
		assertNotSame(dup, ba);
		assertNotSame(dup.array(), ba.array());
		assertNotNull(dup.array()[1]);
		assertNotSame(dup.array()[1], ba.array()[1]);
		assertEquals(3, dup.array().length);
		assertEquals(2, dup.arrayEnd());
		assertEquals(1, dup.arrayIndex());
		assertEquals(1, dup.arrayOffset());
		assertEquals(2, dup.remaining());
		assertEquals(0x0304, dup.getShort());
		assertEquals(0x0304, ba.getShort());
		
		ba.rewind();
		assertEquals(4, ba.remaining());
		b3.clear();
		assertSame(ba, ba.get(b3, 4));
		b3.flip();
		assertEquals(4, b3.remaining());
		assertEquals(0x01020304, b3.getInt());
		assertEquals(0, ba.remaining());
		b3.clear();
		ba.rewind();
		ba.get(b3, 3);
		b3.flip();
		assertEquals(3, b3.remaining());
		assertEquals(0x0102, b3.getShort());
		assertEquals(3, b3.get());
		assertEquals(4, ba.get());
		b3.clear();
		ba.rewind();
		ba.get(b3, 0);
		assertEquals(4, ba.remaining());
		assertEquals(0, b3.position());
		try {
			ba.get(b3, 5);
			fail();
		} catch (BufferUnderflowException e) {}
		
		

	}
}
