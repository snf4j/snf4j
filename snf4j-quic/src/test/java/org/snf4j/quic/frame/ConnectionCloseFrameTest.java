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

public class ConnectionCloseFrameTest extends CommonTest {

	static final IFrameParser parser = ConnectionCloseFrame.getParser();

	ConnectionCloseFrame parse(String data, int remaining) throws Exception {
		buffer(data);
		int type = buffer.get();
		return parser.parse(
			buffer,
			remaining == -1 ? buffer.remaining() : remaining,
			type);
	}

	@Test
	public void testFrameType() {
		int min = Integer.MAX_VALUE, max = 0;
		for (FrameType type: FrameType.values()) {
			if (type.firstValue() < min) {
				min = type.firstValue();
			}
			if (type.lastValue() > max) {
				max = type.lastValue();
			}
		}
		new ConnectionCloseFrame(0, min, null);
		new ConnectionCloseFrame(0, max, null);
		try {
			new ConnectionCloseFrame(0, min-1, null);
		}
		catch (IllegalArgumentException e) {}
		try {
			new ConnectionCloseFrame(0, max+1, null);
		}
		catch (IllegalArgumentException e) {}
	}
	
	@Test
	public void testGetType() {
		assertEquals(0x1c, FrameType.CONNECTION_CLOSE.firstValue());
		assertEquals(0x1d, FrameType.CONNECTION_CLOSE.lastValue());
		assertSame(FrameType.CONNECTION_CLOSE, new ConnectionCloseFrame(10, 5, null).getType());
		assertSame(FrameType.CONNECTION_CLOSE, new ConnectionCloseFrame(10, null).getType());
		assertSame(FrameType.CONNECTION_CLOSE, ConnectionCloseFrame.getParser().getType());
		assertEquals(0x1c, new ConnectionCloseFrame(10, 5, null).getTypeValue());
		assertEquals(0x1d, new ConnectionCloseFrame(10, null).getTypeValue());
	}

	@Test
	public void testGetLength() {
		assertEquals(3, new ConnectionCloseFrame(10, null).getLength());
		assertEquals(4, new ConnectionCloseFrame(100, null).getLength());
		assertEquals(104, new ConnectionCloseFrame(1, new String(new byte[100])).getLength());
		assertEquals(4, new ConnectionCloseFrame(10, 0x1e, null).getLength());
		assertEquals(5, new ConnectionCloseFrame(100, 0x1e, null).getLength());
		assertEquals(105, new ConnectionCloseFrame(1, 0x1e,new String(new byte[100])).getLength());
	}
	
	String reason(int length) {
		StringBuilder reason = new StringBuilder();
		
		for (int i=0; i<length; ++i) {
			reason.append((char)('a'+i%10));
		}
		return reason.toString();
	}

	String reasonHex(int length) {
		return hex(reason(length).toString().getBytes());
	}
	
	@Test
	public void testParse() throws Exception {
		ConnectionCloseFrame f = parse("1c 05 04 03" + reasonHex(3), -1);
		assertEquals(5, f.getError());
		assertEquals(4, f.getFrameType());
		assertEquals("abc", f.getReason());

		f = parse("1c 05 1e 00", -1);
		assertEquals(5, f.getError());
		assertEquals(0x1e, f.getFrameType());
		assertEquals("", f.getReason());

		f = parse("1c 4101 1e 00", -1);
		assertEquals(257, f.getError());
		assertEquals(0x1e, f.getFrameType());
		assertEquals("", f.getReason());

		f = parse("1c 05 04 40ff" + reasonHex(255), -1);
		assertEquals(5, f.getError());
		assertEquals(4, f.getFrameType());
		assertEquals(reason(255), f.getReason());
		
		f = parse("1d 05 03" + reasonHex(3), -1);
		assertEquals(5, f.getError());
		assertEquals(-1, f.getFrameType());
		assertEquals("abc", f.getReason());

		f = parse("1d 05 00", -1);
		assertEquals(5, f.getError());
		assertEquals(-1, f.getFrameType());
		assertEquals("", f.getReason());

		f = parse("1d 4101 00", -1);
		assertEquals(257, f.getError());
		assertEquals(-1, f.getFrameType());
		assertEquals("", f.getReason());

		f = parse("1d 05 40ff" + reasonHex(255), -1);
		assertEquals(5, f.getError());
		assertEquals(-1, f.getFrameType());
		assertEquals(reason(255), f.getReason());
	}
	
	@Test
	public void testGetBytes() {
		ConnectionCloseFrame f = new ConnectionCloseFrame(10, null);
		f.getBytes(buffer);
		assertArrayEquals(bytes("1d 0a 00"), bytesAndClear());
		
		f = new ConnectionCloseFrame(257, null);
		f.getBytes(buffer);
		assertArrayEquals(bytes("1d 4101 00"), bytesAndClear());

		f = new ConnectionCloseFrame(1, reason(255));
		f.getBytes(buffer);
		assertArrayEquals(bytes("1d 01 40ff" + reasonHex(255)), bytesAndClear());

		f = new ConnectionCloseFrame(10, 0x1e, null);
		f.getBytes(buffer);
		assertArrayEquals(bytes("1c 0a 1e 00"), bytesAndClear());

		f = new ConnectionCloseFrame(254, 0x1e, null);
		f.getBytes(buffer);
		assertArrayEquals(bytes("1c 40fe 1e 00"), bytesAndClear());

		f = new ConnectionCloseFrame(1, 0x1e, reason(255));
		f.getBytes(buffer);
		assertArrayEquals(bytes("1c 01 1e 40ff" + reasonHex(255)), bytesAndClear());
	}
	
	void assertFailure(String data, int remaining) throws Exception {
		try {
			parse(data, remaining);
			fail("Remaining " + remaining);
		} 
		catch (QuicException e) {
			assertSame(TransportError.FRAME_ENCODING_ERROR, e.getTransportError());
		}
	}

	@Test
	public void testParseFailure() throws Exception {
		String data = "1c 4040 1e 400a" + reasonHex(10);
		int len = bytes(data).length-1;
		for (int i=1; i<len; ++i) {
			assertFailure(data, i);
		}

		data = "1c 01 401e 00";
		len = bytes(data).length-1;
		for (int i=1; i<len; ++i) {
			assertFailure(data, i);
		}
		
		data = "1c 01 1f 00";
		len = bytes(data).length-1;
		assertFailure(data, len);
	}
}
