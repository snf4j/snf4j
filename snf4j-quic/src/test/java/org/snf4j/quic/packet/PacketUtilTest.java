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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.Version;
import org.snf4j.quic.frame.FrameDecoder;
import org.snf4j.quic.frame.FrameType;

public class PacketUtilTest extends CommonTest {
	
	int[] remaining = new int[1];

	int[] remaining(int i) {
		remaining[0] = i;
		return remaining;
	}
	
	int remaining() {
		return remaining[0];
	}
	
	@Test
	public void testDecodeFrames() throws Exception {
		InitialPacket p = new InitialPacket(bytes("00"), 0, bytes("01"), Version.V1, bytes(""));
		
		PacketUtil.decodeFrames(p, buffer("00000100"), 4, FrameDecoder.INSTANCE);
		assertEquals(3, p.getFrames().size());
		assertSame(FrameType.PADDING, p.getFrames().get(0).getType());
		assertSame(FrameType.PING, p.getFrames().get(1).getType());
		assertSame(FrameType.PADDING, p.getFrames().get(2).getType());
		p.getFrames().clear();
		
		PacketUtil.decodeFrames(p, buffer("00000100"), 1, FrameDecoder.INSTANCE);
		assertEquals(1, p.getFrames().size());
		assertSame(FrameType.PADDING, p.getFrames().get(0).getType());
		p.getFrames().clear();

		try {
			PacketUtil.decodeFrames(p, buffer("00000100"), 0, FrameDecoder.INSTANCE);
			fail();
		}
		catch (QuicException e) {}
	}
	
	@Test
	public void testPutFirstBits() {
		PacketUtil.putFirstBits(0b00000000, 1, buffer);
		assertArrayEquals(bytes("00"), bytesAndClear());
		PacketUtil.putFirstBits(0b00000000, 2, buffer);
		assertArrayEquals(bytes("01"), bytesAndClear());
		PacketUtil.putFirstBits(0b00000000, 3, buffer);
		assertArrayEquals(bytes("02"), bytesAndClear());
		PacketUtil.putFirstBits(0b00000000, 4, buffer);
		assertArrayEquals(bytes("03"), bytesAndClear());

		PacketUtil.putFirstBits(0b11111100, 1, buffer);
		assertArrayEquals(bytes("fc"), bytesAndClear());
		PacketUtil.putFirstBits(0b11111100, 2, buffer);
		assertArrayEquals(bytes("fd"), bytesAndClear());
		PacketUtil.putFirstBits(0b11111100, 3, buffer);
		assertArrayEquals(bytes("fe"), bytesAndClear());
		PacketUtil.putFirstBits(0b11111100, 4, buffer);
		assertArrayEquals(bytes("ff"), bytesAndClear());
	
	}
	
	@Test
	public void testGetUnsigned() {
		assertEquals(0, PacketUtil.getUnsigned(buffer("00ff")));
		assertEquals(1, PacketUtil.getUnsigned(buffer("01ff")));
		assertEquals(127, PacketUtil.getUnsigned(buffer("7fff")));
		assertEquals(128, PacketUtil.getUnsigned(buffer("80ff")));
		assertEquals(255, PacketUtil.getUnsigned(buffer("ffee")));
	}
	
	@Test
	public void testIdentifyVersion() {
		assertSame(Version.V0, PacketUtil.identifyVersion(0));
		assertSame(Version.V1, PacketUtil.identifyVersion(1));
		assertNull(PacketUtil.identifyVersion(2));
	}
	
	@Test
	public void testCheckReservedBits() throws Exception {
		PacketUtil.checkReservedBits(0b00000000);
		PacketUtil.checkReservedBits(0b11110011);
		try {
			PacketUtil.checkReservedBits(0b00000100);
			fail();
		}
		catch (QuicException e) {}
		try {
			PacketUtil.checkReservedBits(0b00001000);
			fail();
		}
		catch (QuicException e) {}
		try {
			PacketUtil.checkReservedBits(0b00001100);
			fail();
		}
		catch (QuicException e) {}
	}
	
	@Test
	public void testencodedIntegerLength() {
		assertEquals(1, PacketUtil.encodedIntegerLength(0));
		assertEquals(1, PacketUtil.encodedIntegerLength(63));
		assertEquals(2, PacketUtil.encodedIntegerLength(64));
		assertEquals(2, PacketUtil.encodedIntegerLength(16383));
		assertEquals(4, PacketUtil.encodedIntegerLength(16384));
		assertEquals(4, PacketUtil.encodedIntegerLength(1073741823));
		assertEquals(8, PacketUtil.encodedIntegerLength(1073741824));
		assertEquals(8, PacketUtil.encodedIntegerLength(4611686018427387903L));

		try {
			PacketUtil.encodedIntegerLength(4611686018427387904L);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		try {
			PacketUtil.encodedIntegerLength(-1);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
	
	}
	
	@Test
	public void testEncodeInteger() throws Exception {
		PacketUtil.encodeInteger(0, buffer);
		assertArrayEquals(bytes("00"), bytes());
		buffer.flip();
		assertEquals(0L, PacketUtil.decodeInteger(buffer, remaining(1)));
		buffer.clear();
		PacketUtil.encodeInteger(63, buffer);
		assertArrayEquals(bytes("3f"), bytes());
		buffer.flip();
		assertEquals(63L, PacketUtil.decodeInteger(buffer, remaining(1)));
		buffer.clear();

		PacketUtil.encodeInteger(64, buffer);
		assertArrayEquals(bytes("4040"), bytes());
		buffer.flip();
		assertEquals(64L, PacketUtil.decodeInteger(buffer, remaining(2)));
		buffer.clear();
		PacketUtil.encodeInteger(16383, buffer);
		assertArrayEquals(bytes("7fff"), bytes());
		buffer.flip();
		assertEquals(16383L, PacketUtil.decodeInteger(buffer, remaining(2)));
		buffer.clear();
		
		PacketUtil.encodeInteger(16384, buffer);
		assertArrayEquals(bytes("80004000"), bytes());
		buffer.flip();
		assertEquals(16384L, PacketUtil.decodeInteger(buffer, remaining(4)));
		buffer.clear();
		PacketUtil.encodeInteger(1073741823, buffer);
		assertArrayEquals(bytes("bfffffff"), bytes());
		buffer.flip();
		assertEquals(1073741823L, PacketUtil.decodeInteger(buffer, remaining(4)));
		buffer.clear();
		
		PacketUtil.encodeInteger(1073741824, buffer);
		assertArrayEquals(bytes("c000000040000000"), bytes());
		buffer.flip();
		assertEquals(1073741824L, PacketUtil.decodeInteger(buffer, remaining(8)));
		buffer.clear();
		PacketUtil.encodeInteger(4611686018427387903L, buffer);
		assertArrayEquals(bytes("ffffffffffffffff"), bytes());
		buffer.flip();
		assertEquals(4611686018427387903L, PacketUtil.decodeInteger(buffer, remaining(8)));
		buffer.clear();
	
		try {
			PacketUtil.encodeInteger(4611686018427387904L, buffer);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		try {
			PacketUtil.encodeInteger(-1, buffer);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testEncodeZero() {
		PacketUtil.encodeZero(buffer);
		assertArrayEquals(bytes("00"), bytes());
	}
	
	@Test
	public void testDecodeInteger() throws Exception {
		assertEquals(0L, PacketUtil.decodeInteger(buffer("00"), remaining(1)));
		assertEquals(0, remaining());
		assertEquals(0, buffer.remaining());
		assertEquals(63L, PacketUtil.decodeInteger(buffer("3f00"), remaining(2)));
		assertEquals(1, remaining());
		assertEquals(1, buffer.remaining());
		
		assertEquals(0L, PacketUtil.decodeInteger(buffer("4000"), remaining(2)));
		assertEquals(0, remaining());
		assertEquals(0, buffer.remaining());
		assertEquals(16383L, PacketUtil.decodeInteger(buffer("7fff00"), remaining(3)));
		assertEquals(1, remaining());
		assertEquals(1, buffer.remaining());
		try {
			PacketUtil.decodeInteger(buffer("4000"), remaining(1));
			fail();
		}
		catch (QuicException e) {
			assertSame(TransportError.PROTOCOL_VIOLATION, e.getTransportError());
		}

		assertEquals(0L, PacketUtil.decodeInteger(buffer("80000000"), remaining(4)));
		assertEquals(0, remaining());
		assertEquals(0, buffer.remaining());
		assertEquals(1073741823L, PacketUtil.decodeInteger(buffer("bfffffff00"), remaining(5)));
		assertEquals(1, remaining());
		assertEquals(1, buffer.remaining());
		try {
			PacketUtil.decodeInteger(buffer("80000000"), remaining(3));
			fail();
		}
		catch (QuicException e) {
			assertSame(TransportError.PROTOCOL_VIOLATION, e.getTransportError());
		}
		
		assertEquals(0L, PacketUtil.decodeInteger(buffer("c000000000000000"), remaining(8)));
		assertEquals(0, remaining());
		assertEquals(0, buffer.remaining());
		assertEquals(4611686018427387903L, PacketUtil.decodeInteger(buffer("ffffffffffffffff00"), remaining(9)));
		assertEquals(1, remaining());
		assertEquals(1, buffer.remaining());
		try {
			PacketUtil.decodeInteger(buffer("c000000000000000"), remaining(7));
			fail();
		}
		catch (QuicException e) {
			assertSame(TransportError.PROTOCOL_VIOLATION, e.getTransportError());
		}
	}
	
	@Test
	public void testEncodePacketNumber() {
		assertEquals(1, PacketUtil.encodePacketNumber(0, -1, buffer));
		assertArrayEquals(bytes("00"), bytesAndClear());
		assertEquals(1, PacketUtil.encodePacketNumber(1, -1, buffer));
		assertArrayEquals(bytes("01"), bytesAndClear());
		assertEquals(1, PacketUtil.encodePacketNumber(0xfe, -1, buffer));
		assertArrayEquals(bytes("fe"), bytesAndClear());
		assertEquals(1, PacketUtil.encodePacketNumber(0x7f, -1, buffer));
		assertArrayEquals(bytes("7f"), bytesAndClear());
		assertEquals(1, PacketUtil.encodePacketNumber(0x3f, -1, buffer));
		assertArrayEquals(bytes("3f"), bytesAndClear());
		assertEquals(2, PacketUtil.encodePacketNumber(0xff, -1, buffer));
		assertArrayEquals(bytes("00ff"), bytesAndClear());
		assertEquals(2, PacketUtil.encodePacketNumber(0xfffe, -1, buffer));
		assertArrayEquals(bytes("fffe"), bytesAndClear());
		assertEquals(2, PacketUtil.encodePacketNumber(0xabfe, -1, buffer));
		assertArrayEquals(bytes("abfe"), bytesAndClear());
		assertEquals(3, PacketUtil.encodePacketNumber(0xffff, -1, buffer));
		assertArrayEquals(bytes("00ffff"), bytesAndClear());
		assertEquals(3, PacketUtil.encodePacketNumber(0xfffffe, -1, buffer));
		assertArrayEquals(bytes("fffffe"), bytesAndClear());
		assertEquals(3, PacketUtil.encodePacketNumber(0x1234fe, -1, buffer));
		assertArrayEquals(bytes("1234fe"), bytesAndClear());
		assertEquals(4, PacketUtil.encodePacketNumber(0xffffff, -1, buffer));
		assertArrayEquals(bytes("00ffffff"), bytesAndClear());
		assertEquals(4, PacketUtil.encodePacketNumber(0xfffffffeL, -1, buffer));
		assertArrayEquals(bytes("fffffffe"), bytesAndClear());
		assertEquals(4, PacketUtil.encodePacketNumber(0x123456feL, -1, buffer));
		assertArrayEquals(bytes("123456fe"), bytesAndClear());
		try {
			PacketUtil.encodePacketNumber(0xffffffffL, -1, buffer);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		
		assertEquals(2, PacketUtil.encodePacketNumber(0xac5c02, 0xabe8b3, buffer));
		assertArrayEquals(bytes("5c02"), bytesAndClear());
		assertEquals(3, PacketUtil.encodePacketNumber(0xace8fe, 0xabe8b3, buffer));
		assertArrayEquals(bytes("ace8fe"), bytesAndClear());
		try {
			PacketUtil.encodePacketNumber(0xac5c02, 0xac5c02, buffer);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		try {
			PacketUtil.encodePacketNumber(0xac5c02, 0xac5c03, buffer);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testDecodePacketNumber() {
		assertEquals(0x00L, PacketUtil.decodePacketNumber(buffer("00"), 1, -1));
		assertEquals(0x01L, PacketUtil.decodePacketNumber(buffer("01"), 1, -1));
		assertEquals(0xfeL, PacketUtil.decodePacketNumber(buffer("fe"), 1, -1));
		assertEquals(0xffL, PacketUtil.decodePacketNumber(buffer("ff"), 1, -1));
		assertEquals(0x7fL, PacketUtil.decodePacketNumber(buffer("7f"), 1, -1));
		assertEquals(0x0100L, PacketUtil.decodePacketNumber(buffer("0100"), 2, -1));
		assertEquals(0xfffeL, PacketUtil.decodePacketNumber(buffer("fffe"), 2, -1));
		assertEquals(0xffffL, PacketUtil.decodePacketNumber(buffer("ffff"), 2, -1));
		assertEquals(0x7fffL, PacketUtil.decodePacketNumber(buffer("7fff"), 2, -1));
		assertEquals(0x010000L, PacketUtil.decodePacketNumber(buffer("010000"), 3, -1));
		assertEquals(0xfffffeL, PacketUtil.decodePacketNumber(buffer("fffffe"), 3, -1));
		assertEquals(0xffffffL, PacketUtil.decodePacketNumber(buffer("ffffff"), 3, -1));
		assertEquals(0x7ffcfeL, PacketUtil.decodePacketNumber(buffer("7ffcfe"), 3, -1));
		assertEquals(0x01000000L, PacketUtil.decodePacketNumber(buffer("01000000"), 4, -1));
		assertEquals(0xfffffffeL, PacketUtil.decodePacketNumber(buffer("fffffffe"), 4, -1));
		assertEquals(0xffffffffL, PacketUtil.decodePacketNumber(buffer("ffffffff"), 4, -1));
		assertEquals(0x7ffcfefdL, PacketUtil.decodePacketNumber(buffer("7ffcfefd"), 4, -1));
		try {
			PacketUtil.decodePacketNumber(buffer("00"), 0, -1);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		try {
			PacketUtil.decodePacketNumber(buffer("0011223344"), 5, -1);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		
		assertEquals(0xa82f9b32L, PacketUtil.decodePacketNumber(buffer("9b32"), 2, 0xa82f30eaL));
		assertEquals(0x00eaL, PacketUtil.decodePacketNumber(buffer("00ea"), 2, 33000));
		assertEquals(33001+32768, PacketUtil.decodePacketNumber(buffer("00e9"), 2, 33000));
		assertEquals(0xD5F1, PacketUtil.decodePacketNumber(buffer("D5F1"), 2, 22000));
		assertEquals(0xD5F2, PacketUtil.decodePacketNumber(buffer("D5F2"), 2, 22000));
		assertEquals(0xFFFF, PacketUtil.decodePacketNumber(buffer("FFFF"), 2, 0xfffe));
		assertEquals(0xD5F2, PacketUtil.decodePacketNumber(buffer("D5F2"), 2, 66000));
	}	
}
