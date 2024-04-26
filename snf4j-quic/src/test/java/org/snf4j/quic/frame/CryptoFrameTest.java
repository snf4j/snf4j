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

public class CryptoFrameTest extends CommonTest {

	final static byte TYPE = 6;
	
	static final IFrameParser parser = CryptoFrame.getParser();
	
	CryptoFrame parse(String data, int remaining) throws Exception {
		buffer(data);
		assertEquals(TYPE, buffer.get());
		return parser.parse(
			buffer,
			remaining == -1 ? buffer.remaining() : remaining,
			TYPE);
	}
	
	@Test
	public void testGetType() {
		assertEquals(0x06, FrameType.CRYPTO.firstValue());
		assertEquals(0x06, FrameType.CRYPTO.lastValue());
		assertSame(FrameType.CRYPTO, new CryptoFrame(10, buffer).getType());
		assertSame(FrameType.CRYPTO, CryptoFrame.getParser().getType());
	}
	
	@Test
	public void testGetLength() {
		assertEquals(5, new CryptoFrame(63, buffer("0001")).getLength());
		assertEquals(6, new CryptoFrame(64, buffer("0001")).getLength());
		assertEquals(6, new CryptoFrame(16383, buffer("0001")).getLength());
		assertEquals(8, new CryptoFrame(16384, buffer("0001")).getLength());
		assertEquals(8, new CryptoFrame(1073741823, buffer("0001")).getLength());
		assertEquals(12, new CryptoFrame(1073741826, buffer("0001")).getLength());

		assertEquals(3+63, new CryptoFrame(63, buffer(hex(bytes(63)))).getLength());
		assertEquals(4+64, new CryptoFrame(63, buffer(hex(bytes(64)))).getLength());
		assertEquals(4+16383, new CryptoFrame(63, buffer(hex(bytes(16383)))).getLength());
		assertEquals(6+16384, new CryptoFrame(63, buffer(hex(bytes(16384)))).getLength());
	}
	
	@Test
	public void testParse() throws Exception {
		CryptoFrame f = parse("06 07 04 01020304", -1);
		assertEquals(7L, f.getDataOffset());
		assertEquals(4, f.getDataLength());
		assertEquals(4, f.getData().remaining());
		assertArrayEquals(bytes("01020304"), bytes(f.getData()));
		
		f = parse("06 41 23 04 01020304", -1);
		assertEquals(0x123, f.getDataOffset());
		assertEquals(4, f.getDataLength());
		assertEquals(4, f.getData().remaining());
		assertArrayEquals(bytes("01020304"), bytes(f.getData()));

		f = parse("06 23 7fff " + hex(bytes(16383)), -1);
		assertEquals(0x23, f.getDataOffset());
		assertEquals(16383, f.getDataLength());
		assertEquals(16383, f.getData().remaining());
		assertArrayEquals(bytes(16383), bytes(f.getData()));
	}
	
	@Test
	public void testGetBytes() {
		CryptoFrame f = new CryptoFrame(63, buffer(bytes("0001")));
		f.getBytes(buffer);
		assertArrayEquals(bytes("06 3f 02 0001"), bytesAndClear());
		f.getBytes(buffer);
		assertArrayEquals(bytes("06 3f 02 0001"), bytesAndClear());

		f = new CryptoFrame(64, buffer(bytes("0001")));
		f.getBytes(buffer);
		assertArrayEquals(bytes("06 4040 02 0001"), bytesAndClear());
		
		f = new CryptoFrame(64, buffer(bytes(16383)));
		f.getBytes(buffer);
		assertArrayEquals(bytes("06 4040 7fff " + hex(bytes(16383))), bytesAndClear());
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
		String data = "06 4040 0a " + hex(bytes(10));
		int len = bytes(data).length-1;
		
		for (int i=1; i<len; ++i) {
			assertFailure(data, i);
		}
		data = "06 4040 c000000080000000 " + hex(bytes(10));
		len = bytes(data).length-1;
		assertFailure(data, len);
	}
	
}
