/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic.frame;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;

public class FrameUtilTest extends CommonTest {

	int[] remaining = new int[1];

	int[] remaining(int i) {
		remaining[0] = i;
		return remaining;
	}
	
	int remaining() {
		return remaining[0];
	}
	
	@Test
	public void testencodedIntegerLength() {
		assertEquals(1, FrameUtil.encodedIntegerLength(0));
		assertEquals(1, FrameUtil.encodedIntegerLength(63));
		assertEquals(2, FrameUtil.encodedIntegerLength(64));
		assertEquals(2, FrameUtil.encodedIntegerLength(16383));
		assertEquals(4, FrameUtil.encodedIntegerLength(16384));
		assertEquals(4, FrameUtil.encodedIntegerLength(1073741823));
		assertEquals(8, FrameUtil.encodedIntegerLength(1073741824));
		assertEquals(8, FrameUtil.encodedIntegerLength(4611686018427387903L));

		try {
			FrameUtil.encodedIntegerLength(4611686018427387904L);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		try {
			FrameUtil.encodedIntegerLength(-1);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testEncodeInteger() throws Exception {
		FrameUtil.encodeInteger(0, buffer);
		assertArrayEquals(bytes("00"), bytes());
		buffer.flip();
		assertEquals(0L, FrameUtil.decodeInteger(buffer, remaining(1)));
		buffer.clear();
		FrameUtil.encodeInteger(63, buffer);
		assertArrayEquals(bytes("3f"), bytes());
		buffer.flip();
		assertEquals(63L, FrameUtil.decodeInteger(buffer, remaining(1)));
		buffer.clear();

		FrameUtil.encodeInteger(64, buffer);
		assertArrayEquals(bytes("4040"), bytes());
		buffer.flip();
		assertEquals(64L, FrameUtil.decodeInteger(buffer, remaining(2)));
		buffer.clear();
		FrameUtil.encodeInteger(16383, buffer);
		assertArrayEquals(bytes("7fff"), bytes());
		buffer.flip();
		assertEquals(16383L, FrameUtil.decodeInteger(buffer, remaining(2)));
		buffer.clear();
		
		FrameUtil.encodeInteger(16384, buffer);
		assertArrayEquals(bytes("80004000"), bytes());
		buffer.flip();
		assertEquals(16384L, FrameUtil.decodeInteger(buffer, remaining(4)));
		buffer.clear();
		FrameUtil.encodeInteger(1073741823, buffer);
		assertArrayEquals(bytes("bfffffff"), bytes());
		buffer.flip();
		assertEquals(1073741823L, FrameUtil.decodeInteger(buffer, remaining(4)));
		buffer.clear();
		
		FrameUtil.encodeInteger(1073741824, buffer);
		assertArrayEquals(bytes("c000000040000000"), bytes());
		buffer.flip();
		assertEquals(1073741824L, FrameUtil.decodeInteger(buffer, remaining(8)));
		buffer.clear();
		FrameUtil.encodeInteger(4611686018427387903L, buffer);
		assertArrayEquals(bytes("ffffffffffffffff"), bytes());
		buffer.flip();
		assertEquals(4611686018427387903L, FrameUtil.decodeInteger(buffer, remaining(8)));
		buffer.clear();
	
		try {
			FrameUtil.encodeInteger(4611686018427387904L, buffer);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		try {
			FrameUtil.encodeInteger(-1, buffer);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testDecodeInteger() throws Exception {
		assertEquals(0L, FrameUtil.decodeInteger(buffer("00"), remaining(1)));
		assertEquals(0, remaining());
		assertEquals(0, buffer.remaining());
		assertEquals(63L, FrameUtil.decodeInteger(buffer("3f00"), remaining(2)));
		assertEquals(1, remaining());
		assertEquals(1, buffer.remaining());
		
		assertEquals(0L, FrameUtil.decodeInteger(buffer("4000"), remaining(2)));
		assertEquals(0, remaining());
		assertEquals(0, buffer.remaining());
		assertEquals(16383L, FrameUtil.decodeInteger(buffer("7fff00"), remaining(3)));
		assertEquals(1, remaining());
		assertEquals(1, buffer.remaining());
		try {
			FrameUtil.decodeInteger(buffer("4000"), remaining(1));
			fail();
		}
		catch (QuicException e) {
			assertSame(TransportError.FRAME_ENCODING_ERROR, e.getTransportError());
		}

		assertEquals(0L, FrameUtil.decodeInteger(buffer("80000000"), remaining(4)));
		assertEquals(0, remaining());
		assertEquals(0, buffer.remaining());
		assertEquals(1073741823L, FrameUtil.decodeInteger(buffer("bfffffff00"), remaining(5)));
		assertEquals(1, remaining());
		assertEquals(1, buffer.remaining());
		try {
			FrameUtil.decodeInteger(buffer("80000000"), remaining(3));
			fail();
		}
		catch (QuicException e) {
			assertSame(TransportError.FRAME_ENCODING_ERROR, e.getTransportError());
		}
		
		assertEquals(0L, FrameUtil.decodeInteger(buffer("c000000000000000"), remaining(8)));
		assertEquals(0, remaining());
		assertEquals(0, buffer.remaining());
		assertEquals(4611686018427387903L, FrameUtil.decodeInteger(buffer("ffffffffffffffff00"), remaining(9)));
		assertEquals(1, remaining());
		assertEquals(1, buffer.remaining());
		try {
			FrameUtil.decodeInteger(buffer("c000000000000000"), remaining(7));
			fail();
		}
		catch (QuicException e) {
			assertSame(TransportError.FRAME_ENCODING_ERROR, e.getTransportError());
		}
	}
	
}
