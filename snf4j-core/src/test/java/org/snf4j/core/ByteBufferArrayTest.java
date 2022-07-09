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
	
	@Test
	public void testMark() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3}));
		bufs.add(ByteBuffer.wrap(new byte[] {4,5,6,7}));

		ByteBufferArray array = new ByteBufferArray(bufs);
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
		
		ByteBuffer buf = ByteBuffer.wrap(new byte[] {8,9,10});
		bufs.add(buf);
		array = new ByteBufferArray(bufs);
		array.position(9);
		assertEquals(1, array.remaining());
		array.mark();
		buf.limit(2);
		array.reset();
		assertEquals(0, array.remaining());
		buf.limit(1);
		try {
			array.reset();
			fail();
		} catch (InvalidMarkException e) {}
		assertEquals(0, array.remaining());
	}
	
	@Test
	public void testRewind() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3}));
		bufs.add(ByteBuffer.wrap(new byte[] {4,5,6,7}));

		ByteBufferArray array = new ByteBufferArray(bufs);
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
		
		
		array = new ByteBufferArray(new ByteBuffer[0]);
		array.mark();
		array.reset();
		array.rewind();
		try {
			array.reset();
			fail();
		} catch (InvalidMarkException e) {}
	}
	
	@Test
	public void testLimit() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3}));
		bufs.add(ByteBuffer.wrap(new byte[] {4,5,6,7}));

		ByteBufferArray array = new ByteBufferArray(bufs);
		assertEquals(7, array.limit());
		
		ByteBuffer buf = ByteBuffer.wrap(new byte[] {8,9,10});
		bufs.add(buf);
		array = new ByteBufferArray(bufs);
		assertEquals(10, array.limit());
		buf.limit(2);
		assertEquals(9, array.limit());
		
		assertEquals(0, new ByteBufferArray(new ByteBuffer[0]).limit());
	}
	
	@Test
	public void testPosition() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3}));
		bufs.add(ByteBuffer.wrap(new byte[] {4,5,6}));

		ByteBufferArray array = new ByteBufferArray(bufs);
		for (int i=0; i<6; ++i) {
			assertEquals(i, array.position());
			array.get();
		}
		assertEquals(6, array.position());
		assertEquals(0, new ByteBufferArray(new ByteBuffer[0]).position());
		
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
		
		array = new ByteBufferArray(new ByteBuffer[0]);
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
	public void testRemaining() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3}));
		bufs.add(ByteBuffer.wrap(new byte[] {4,5,6}));

		ByteBufferArray array = new ByteBufferArray(bufs);
		assertEquals(6, array.remaining());
		assertTrue(array.hasRemaining());
		bufs.get(0).get();
		assertEquals(5, array.remaining());
		assertTrue(array.hasRemaining());
		bufs.get(0).get();
		bufs.get(0).get();
		assertEquals(3, array.remaining());
		assertTrue(array.hasRemaining());
		bufs.get(1).get();
		bufs.get(1).get();
		bufs.get(1).get();
		assertEquals(0, array.remaining());
		assertFalse(array.hasRemaining());
		bufs.get(0).clear();
		bufs.get(0).put((byte) 1).flip();
		assertEquals(1, array.remaining());
		assertTrue(array.hasRemaining());
		assertFalse(new ByteBufferArray(new ByteBuffer[0]).hasRemaining());
		
		bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3}));
		bufs.add(ByteBuffer.wrap(new byte[] {4,5,6}));
		
		array = new ByteBufferArray(bufs);
		array.get();
		array.get();
		assertEquals(4, array.remaining());
		assertTrue(array.hasRemaining());
		array.get();
		array.get();
		assertEquals(2, array.remaining());
		assertTrue(array.hasRemaining());
		bufs.get(0).clear();
		bufs.get(0).put((byte) 1).flip();
		assertEquals(2, array.remaining());
		assertTrue(array.hasRemaining());
		array.get();
		array.get();
		assertEquals(0, array.remaining());
		assertFalse(array.hasRemaining());
		assertEquals(0, new ByteBufferArray(new ByteBuffer[0]).remaining());
	}
	
	@Test
	public void testEmptyArray() {
		ByteBufferArray array = new ByteBufferArray(new ByteBuffer[0]);
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
	public void testGet() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3}));
		bufs.add(ByteBuffer.wrap(new byte[] {4,5,6}));
		
		ByteBufferArray array = new ByteBufferArray(bufs);
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
	public void testAbsoluteGet() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3}));
		bufs.add(ByteBuffer.wrap(new byte[] {4,5,6}));
		
		ByteBufferArray array = new ByteBufferArray(bufs);
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
		
		bufs.get(0).get();
		bufs.get(1).get();
		assertEquals(1, array.get(0));
		assertEquals(2, array.get(1));
		assertEquals(3, array.get(2));
		assertEquals(4, array.get(3));
		assertEquals(5, array.get(4));
		assertEquals(6, array.get(5));
		
		bufs.get(0).limit(2);
		bufs.get(1).limit(2);
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
	public void testGetBytes() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3,4,5,6}));
		bufs.add(ByteBuffer.wrap(new byte[] {7,8,9,10,11,12}));
		bufs.add(ByteBuffer.wrap(new byte[] {13,14}));
		bufs.add(ByteBuffer.wrap(new byte[] {15,16}));
		bufs.add(ByteBuffer.wrap(new byte[] {17,18}));
		bufs.add(ByteBuffer.wrap(new byte[] {19,20}));
		bufs.add(ByteBuffer.wrap(new byte[] {21,22}));
		byte[] data = new byte[5];
		
		ByteBufferArray array = new ByteBufferArray(bufs);
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
	public void testAbsoluteGetChar() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3,4,5,6}));
		bufs.add(ByteBuffer.wrap(new byte[] {7,8,9,10,11,12}));
		bufs.add(ByteBuffer.wrap(new byte[] {13,14}));
		bufs.get(0).putChar(1, 'a');

		ByteBufferArray array = new ByteBufferArray(bufs);
		assertEquals('a', array.getChar(1));
		byte b = bufs.get(0).get(2);
		bufs.get(0).limit(2);
		assertNotEquals('a', array.getChar(1));
		bufs.get(1).put(0, b);
		assertEquals('a', array.getChar(1));
	}

	@Test
	public void testGetChar() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3,4,5,6}));
		bufs.add(ByteBuffer.wrap(new byte[] {7,8,9,10,11,12}));
		bufs.add(ByteBuffer.wrap(new byte[] {13,14}));
		bufs.get(0).putChar(0, 'a');
		bufs.get(0).putChar(4, 'x');
		bufs.get(1).putChar(0, 'B');
		bufs.get(2).put(0, bufs.get(1).get(1));
		bufs.get(1).limit(1);
		
		ByteBufferArray array = new ByteBufferArray(bufs);
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
	public void testAbsoluteGetShort() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3,4,5,6}));
		bufs.add(ByteBuffer.wrap(new byte[] {7,8,9,10,11,12}));

		ByteBufferArray array = new ByteBufferArray(bufs);
		assertEquals(0x0203, array.getShort(1));
		bufs.get(0).limit(2);
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
	public void testGetShort() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3,4,5,6}));
		bufs.add(ByteBuffer.wrap(new byte[] {7}));
		bufs.add(ByteBuffer.wrap(new byte[] {8,9,10,11}));

		ByteBufferArray array = new ByteBufferArray(bufs);
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

		bufs.clear();
		bufs.add(ByteBuffer.wrap(new byte[] {5,6}));
		array = new ByteBufferArray(bufs);
		assertEquals(0x0506, array.getShort());

	}
	
	@Test
	public void testAbsoluteGetInt() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3,4,5,6}));
		bufs.add(ByteBuffer.wrap(new byte[] {7,8,9,10,11,12}));
		bufs.add(ByteBuffer.wrap(new byte[] {13,14}));

		ByteBufferArray array = new ByteBufferArray(bufs);
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
		
		bufs.get(0).limit(2);
		bufs.get(1).limit(1);
		assertEquals(0x0102070d, array.getInt(0));
		assertEquals(0x02070d0e, array.getInt(1));
		try {
			array.getInt(2);
			fail();
		}
		catch (IndexOutOfBoundsException e) {}
		
		bufs.get(0).order(ByteOrder.LITTLE_ENDIAN);
		bufs.get(1).order(ByteOrder.LITTLE_ENDIAN);
		bufs.get(2).order(ByteOrder.LITTLE_ENDIAN);
		assertEquals(0x0d070201, array.getInt(0));
		
	}

	@Test
	public void testGetInt() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3,4,5,6}));
		bufs.add(ByteBuffer.wrap(new byte[] {7,8,9,10,11,12}));
		bufs.add(ByteBuffer.wrap(new byte[] {13,14,15}));

		ByteBufferArray array = new ByteBufferArray(bufs);
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

		bufs.clear();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3,4}));
		array = new ByteBufferArray(bufs);
		assertEquals(0x01020304, array.getInt());

		bufs.clear();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3,4,5}));
		bufs.add(ByteBuffer.wrap(new byte[] {6,7,8}));
		bufs.get(0).order(ByteOrder.LITTLE_ENDIAN);
		array = new ByteBufferArray(bufs);
		assertEquals(0x04030201, array.getInt());
		assertEquals(0x08070605, array.getInt());
		
	}
	
	@Test
	public void testAbsoluteGetLong() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3,4,5,6,7,8}));
		bufs.add(ByteBuffer.wrap(new byte[] {9,10,11,12}));
		bufs.add(ByteBuffer.wrap(new byte[] {8,7,6,5,4,3,2,1}));

		ByteBufferArray array = new ByteBufferArray(bufs);
		assertEquals(0x0102030405060708L, array.getLong(0));
		assertEquals(0x0203040506070809L, array.getLong(1));
		assertEquals(0x0807060504030201L, array.getLong(12));
		array.getLong(12);
		try {
			array.getLong(13);
			fail();
		}
		catch (IndexOutOfBoundsException e) {}
	}

	@Test
	public void testGetLong() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3,4,5,6,7,8}));
		bufs.add(ByteBuffer.wrap(new byte[] {9,10,11,12}));
		bufs.add(ByteBuffer.wrap(new byte[] {8,7,6,5,4,3,2,1,2,3,4}));

		ByteBufferArray array = new ByteBufferArray(bufs);
		assertEquals(0x0102030405060708L, array.getLong());
		assertEquals(0x090a0b0c08070605L, array.getLong());
		try {
			array.getLong();
			fail();
		}
		catch (BufferUnderflowException e) {}
		assertEquals(0x04030201, array.getInt());
		assertEquals(0x0203, array.getShort());
		try {
			array.getLong();
			fail();
		}
		catch (BufferUnderflowException e) {}
		assertEquals(4, array.get());
		try {
			array.getLong();
			fail();
		}
		catch (BufferUnderflowException e) {}
	}
	
	@Test
	public void testAbsoluteGetFloat() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3,4,5,6,7,8}));
		bufs.add(ByteBuffer.wrap(new byte[] {9,10,11,12}));
		bufs.add(ByteBuffer.wrap(new byte[] {8,7,6,5,4,3,2,1}));
		bufs.get(0).putFloat(1, 43323.432F);
		bufs.get(1).put(0, bufs.get(0).get(4));
		
		ByteBufferArray array = new ByteBufferArray(bufs);
		assertTrue(43323.432F == array.getFloat(1));
		bufs.get(0).limit(4);
		assertTrue(43323.432F == array.getFloat(1));
		array.getFloat(12);
		try {
			array.getFloat(13);
			fail();
		}
		catch (IndexOutOfBoundsException e) {}
	}

	@Test
	public void testGetFloat() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3,4,5,6,7,8}));
		bufs.add(ByteBuffer.wrap(new byte[] {9,10,11,12}));
		bufs.get(0).putFloat(0, 43323.432F);
		bufs.get(0).putFloat(4, 65523.33432F);
		bufs.get(1).put(0, bufs.get(0).get(7));
		bufs.get(0).limit(7);
		
		ByteBufferArray array = new ByteBufferArray(bufs);
		assertTrue(43323.432F == array.getFloat());
		assertTrue(65523.33432F == array.getFloat());
		try {
			array.getFloat();
			fail();
		}
		catch (BufferUnderflowException e) {}
	}
	
	@Test
	public void testAbsoluteGetDouble() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3,4,5,6,7,8,9}));
		bufs.add(ByteBuffer.wrap(new byte[] {10,11,12}));
		bufs.add(ByteBuffer.wrap(new byte[] {8,7,6,5,4,3,2,1}));
		bufs.get(0).putDouble(1, 4332343434.32554);
		bufs.get(1).put(0, bufs.get(0).get(8));
		
		ByteBufferArray array = new ByteBufferArray(bufs);
		assertTrue(4332343434.32554 == array.getDouble(1));
		bufs.get(0).limit(8);
		assertTrue(4332343434.32554 == array.getDouble(1));
		array.getDouble(11);
		try {
			array.getDouble(12);
			fail();
		}
		catch (IndexOutOfBoundsException e) {}
	}

	@Test
	public void testGetDouble() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3,4,5,6,7,8,9}));
		bufs.add(ByteBuffer.wrap(new byte[] {10,11,12}));
		bufs.add(ByteBuffer.wrap(new byte[] {8,7,6,5,4,3,2,1}));
		bufs.get(0).putDouble(0, 4332343434.32554);
		bufs.get(1).put(0, bufs.get(0).get(7));
		
		ByteBufferArray array = new ByteBufferArray(bufs);
		assertTrue(4332343434.32554 == array.getDouble());
		bufs.get(0).limit(7);
		bufs.get(0).position(0);
		assertTrue(4332343434.32554 == array.getDouble());
		assertEquals(0x0b0c, array.getShort());
		assertEquals(8, array.get());
		try {
			array.getDouble();
			fail();
		}
		catch (BufferUnderflowException e) {}
		assertEquals(0x07060504, array.getInt());
		assertEquals(0x0302, array.getShort());
		try {
			array.getDouble();
			fail();
		}
		catch (BufferUnderflowException e) {}
		
	}

	@Test
	public void testDuplicate() {
		List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3,4,5,6,7,8,9}));
		bufs.add(ByteBuffer.wrap(new byte[] {10,11,12}));
		bufs.add(ByteBuffer.wrap(new byte[] {8,7,6,5,4,3,2,1}));
		
		ByteBufferArray array1 = new ByteBufferArray(bufs);
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

		bufs = new ArrayList<ByteBuffer>();
		bufs.add(ByteBuffer.wrap(new byte[] {1,2,3,4,5,6,7,8,9}));
		bufs.add(ByteBuffer.wrap(new byte[] {10,11,12}));
		array1 = new ByteBufferArray(bufs);
		array2 = array1.duplicate();
		array1.mark();
		try {
			array2.reset();
			fail();
		} catch (InvalidMarkException e) {}

	}	
}
