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
package org.snf4j.quic.packet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.frame.FrameDecoder;
import org.snf4j.quic.frame.PaddingFrame;
import org.snf4j.quic.frame.PingFrame;

public class OneRttPacketTest extends CommonTest {

	static final IPacketParser parser = OneRttPacket.getParser();
	
	OneRttPacket parse(String data, int remaining, long largestPn) throws Exception {
		return parser.parse(
				buffer(data), 
				remaining == -1 ? buffer.remaining() : remaining, 
				new ParseContext(largestPn, 4), 
				FrameDecoder.INSTANCE);
	}
	
	static OneRttPacket packet(String destId, long pn, boolean spin, boolean phase) {
		return new OneRttPacket(bytes(destId), pn, spin, phase);
	}

	@Test
	public void testGetType() {
		assertFalse(PacketType.ONE_RTT.hasLongHeader());
		assertSame(PacketType.ONE_RTT, OneRttPacket.getParser().getType());
		assertSame(PacketType.ONE_RTT, packet("",0,false,false).getType());
	}
	
	@Test(expected=QuicException.class)
	public void testReservedBits10() throws Exception {
		parse("50 01020304 30 00 01", -1, -1);
	}

	@Test(expected=QuicException.class)
	public void testReservedBits08() throws Exception {
		parse("48 01020304 30 00 01", -1, -1);
	}

	@Test(expected=QuicException.class)
	public void testReservedBits18() throws Exception {
		parse("58 01020304 30 00 01", -1, -1);
	}

	@Test
	public void testParser() throws Exception {
		//1-byte packet number
		OneRttPacket p = parse("40 01020304 30 00 01", -1, -1);
		assertNotNull(p);
		assertArrayEquals(bytes("01020304"), p.getDestinationId());
		assertFalse(p.getSpinBit());
		assertFalse(p.getKeyPhase());
		assertEquals(0x30L, p.getPacketNumber());
		assertEquals(2, p.getFrames().size());
		assertEquals(8, p.getLength(-1));
		assertEquals(11, p.getMaxLength());

		//2-byte packet number
		p = parse("61 01020304 2930 00 01", -1, -1);
		assertNotNull(p);
		assertArrayEquals(bytes("01020304"), p.getDestinationId());
		assertTrue(p.getSpinBit());
		assertFalse(p.getKeyPhase());
		assertEquals(0x2930L, p.getPacketNumber());
		assertEquals(2, p.getFrames().size());
		assertEquals(9, p.getLength(-1));
		assertEquals(11, p.getMaxLength());

		//3-byte packet number
		p = parse("46 01020304 282930 00 01", -1, -1);
		assertNotNull(p);
		assertArrayEquals(bytes("01020304"), p.getDestinationId());
		assertFalse(p.getSpinBit());
		assertTrue(p.getKeyPhase());
		assertEquals(0x282930L, p.getPacketNumber());
		assertEquals(2, p.getFrames().size());
		assertEquals(10, p.getLength(-1));
		assertEquals(11, p.getMaxLength());

		//4-byte packet number
		p = parse("67 01020304 27282930 00 01", -1, -1);
		assertNotNull(p);
		assertArrayEquals(bytes("01020304"), p.getDestinationId());
		assertTrue(p.getSpinBit());
		assertTrue(p.getKeyPhase());
		assertEquals(0x27282930L, p.getPacketNumber());
		assertEquals(2, p.getFrames().size());
		assertEquals(11, p.getLength(-1));
		assertEquals(11, p.getMaxLength());

		//more data in buffer
		p = parse("67 01020304 27282930 00 01 00", 11, -1);
		assertNotNull(p);
		assertArrayEquals(bytes("01020304"), p.getDestinationId());
		assertTrue(p.getSpinBit());
		assertTrue(p.getKeyPhase());
		assertEquals(0x27282930L, p.getPacketNumber());
		assertEquals(2, p.getFrames().size());
		assertEquals(11, p.getLength(-1));
		assertEquals(11, p.getMaxLength());
	}	
	
	@Test
	public void testGetBytes() {
		//1-byte packet number
		OneRttPacket p = packet("010203", 0x3f, false, false);
		p.getBytes(-1, buffer);
		assertArrayEquals(bytes("40 010203 3f"), bytesAndClear());

		//2-byte packet number
		p = packet("010203", 0x3ffe, true, false);
		p.getBytes(-1, buffer);
		assertArrayEquals(bytes("61 010203 3ffe"), bytesAndClear());

		//3-byte packet number
		p = packet("010203", 0x3ffefc, false, true);
		p.getBytes(-1, buffer);
		assertArrayEquals(bytes("46 010203 3ffefc"), bytesAndClear());

		//4-byte packet number
		p = packet("010203", 0x3ffefcfd, true, true);
		p.getBytes(-1, buffer);
		assertArrayEquals(bytes("67 010203 3ffefcfd"), bytesAndClear());
		
		//frames
		p = packet("010203", 0x3ffefcfd, true, true);
		p.getFrames().add(PaddingFrame.INSTANCE);
		p.getFrames().add(PingFrame.INSTANCE);
		p.getBytes(-1, buffer);
		assertArrayEquals(bytes("67 010203 3ffefcfd 00 01"), bytesAndClear());
	}
	
	@Test
	public void testPacketNumberDecoding() throws Exception {
		OneRttPacket p = packet("0102", 0x0102030405060778L, false, false);
		p.getFrames().add(PingFrame.INSTANCE);
		
		long largest = 0x0102030405060777L;
		p.getBytes(largest, buffer);
		assertArrayEquals(bytes("40 0102 78 01"), bytes());
		buffer.flip();
		assertEquals(buffer.remaining(), p.getLength(largest));
		p = parser.parse(buffer, buffer.remaining(), new ParseContext(largest,2), FrameDecoder.INSTANCE);
		assertEquals(0x0102030405060778L, p.getPacketNumber());
		assertEquals(buffer.limit(), p.getLength(largest));
		buffer.clear();

		largest = 0x0102030405060777L - 0x0fff;
		p.getBytes(largest, buffer);
		assertArrayEquals(bytes("41 0102 0778 01"), bytes());
		buffer.flip();
		assertEquals(buffer.remaining(), p.getLength(largest));
		p = parser.parse(buffer, buffer.remaining(), new ParseContext(largest,2), FrameDecoder.INSTANCE);
		assertEquals(0x0102030405060778L, p.getPacketNumber());
		assertEquals(buffer.limit(), p.getLength(largest));
		buffer.clear();

		largest = 0x0102030405060777L - 0x0fffff;
		p.getBytes(largest, buffer);
		assertArrayEquals(bytes("42 0102 060778 01"), bytes());
		buffer.flip();
		assertEquals(buffer.remaining(), p.getLength(largest));
		p = parser.parse(buffer, buffer.remaining(), new ParseContext(largest,2), FrameDecoder.INSTANCE);
		assertEquals(0x0102030405060778L, p.getPacketNumber());
		assertEquals(buffer.limit(), p.getLength(largest));
		buffer.clear();
		
		largest = 0x0102030405060777L - 0x0fffffff;
		p.getBytes(largest, buffer);
		assertArrayEquals(bytes("43 0102 05060778 01"), bytes());
		buffer.flip();
		assertEquals(buffer.remaining(), p.getLength(largest));
		p = parser.parse(buffer, buffer.remaining(), new ParseContext(largest,2), FrameDecoder.INSTANCE);
		assertEquals(0x0102030405060778L, p.getPacketNumber());
		assertEquals(buffer.limit(), p.getLength(largest));
		buffer.clear();
	}
	
	void assertFailure(String data, int remaining) throws Exception {
		try {
			parse(data, remaining, -1);
			fail();
		} 
		catch (QuicException e) {
			assertSame(TransportError.PROTOCOL_VIOLATION, e.getTransportError());
		}
	}
	
	@Test
	public void testParseFailure() throws Exception {
		String data = "40 01020304 30 00 01";
		int len = bytes(data).length - 1;
		
		for (int i=1; i<len; ++i) {
			assertFailure(data, i);
		}
	}


}
