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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.Version;
import org.snf4j.quic.engine.EncryptionLevel;
import org.snf4j.quic.frame.FrameDecoder;
import org.snf4j.quic.frame.MultiPaddingFrame;
import org.snf4j.quic.frame.PaddingFrame;
import org.snf4j.quic.frame.PingFrame;

public class HandshakePacketTest extends CommonTest {
	
	static final IPacketParser parser = HandshakePacket.getParser();
	
	HandshakePacket parse(String data, int remaining, long largestPn) throws Exception {
		return parser.parse(
				buffer(data), 
				remaining == -1 ? buffer.remaining() : remaining, 
				new ParseContext(largestPn), 
				FrameDecoder.INSTANCE);
	}
	
	static HandshakePacket packet(String destId, long pn, String srcId) {
		return new HandshakePacket(bytes(destId), pn, bytes(srcId), Version.V1);
	}

	@Test
	public void testGetType() {
		assertTrue(PacketType.HANDSHAKE.hasLongHeader());
		assertSame(EncryptionLevel.HANDSHAKE, PacketType.HANDSHAKE.encryptionLevel());
		assertSame(PacketType.HANDSHAKE, HandshakePacket.getParser().getType());
		assertSame(PacketType.HANDSHAKE, packet("",0,"").getType());
	}
	
	@Test
	public void testGetHeaderBytes() {
		HandshakePacket p = new HandshakePacket(bytes(255), 1, bytes(255), Version.V1);
		p.getFrames().add(new MultiPaddingFrame(99));
		p.getFrames().add(new PingFrame());
		buffer.clear();
		assertEquals(100+16+1, p.getHeaderBytes(-1, 16, buffer));
		assertEquals(100, p.getPayloadLength());
		assertEquals(p.getFramesLength(), p.getPayloadLength());
	}
	
	@Test
	public void testParser() throws Exception {
		//1-byte packet number
		HandshakePacket p = parse("e0 00000001 03 010203 02 0102 03 30 00 01", -1, -1);
		assertNotNull(p);
		assertSame(Version.V1, p.getVersion());
		assertArrayEquals(bytes("010203"), p.getDestinationId());
		assertArrayEquals(bytes("0102"), p.getSourceId());
		assertEquals(0x30L, p.getPacketNumber());
		assertEquals(2, p.getFrames().size());
		assertEquals(16, p.getLength(-1));
		assertEquals(19, p.getMaxLength());
		
		//2-byte packet number
		p = parse("e1 00000001 03 010203 02 0102 04 30 31 00 01", -1, -1);
		assertNotNull(p);
		assertSame(Version.V1, p.getVersion());
		assertArrayEquals(bytes("010203"), p.getDestinationId());
		assertArrayEquals(bytes("0102"), p.getSourceId());
		assertEquals(0x3031L, p.getPacketNumber());
		assertEquals(2, p.getFrames().size());
		assertEquals(17, p.getLength(-1));
		assertEquals(19, p.getMaxLength());
		
		//3-byte packet number
		p = parse("e2 00000001 03 010203 02 0102 05 29 30 31 00 01", -1, -1);
		assertNotNull(p);
		assertSame(Version.V1, p.getVersion());
		assertArrayEquals(bytes("010203"), p.getDestinationId());
		assertArrayEquals(bytes("0102"), p.getSourceId());
		assertEquals(0x293031L, p.getPacketNumber());
		assertEquals(2, p.getFrames().size());
		assertEquals(18, p.getLength(-1));
		assertEquals(19, p.getMaxLength());

		//4-byte packet number
		p = parse("e3 00000001 03 010203 02 0102 06 28 29 30 31 00 01", -1, -1);
		assertNotNull(p);
		assertSame(Version.V1, p.getVersion());
		assertArrayEquals(bytes("010203"), p.getDestinationId());
		assertArrayEquals(bytes("0102"), p.getSourceId());
		assertEquals(0x28293031L, p.getPacketNumber());
		assertEquals(2, p.getFrames().size());
		assertEquals(19, p.getLength(-1));
		assertEquals(19, p.getMaxLength());
		assertEquals(0, buffer.remaining());
		
		//more data in buffer
		p = parse("e3 00000001 03 010203 02 0102 06 28 29 30 31 00 01 00", 19, -1);
		assertNotNull(p);
		assertSame(Version.V1, p.getVersion());
		assertArrayEquals(bytes("010203"), p.getDestinationId());
		assertArrayEquals(bytes("0102"), p.getSourceId());
		assertEquals(0x28293031L, p.getPacketNumber());
		assertEquals(2, p.getFrames().size());
		assertEquals(19, p.getLength(-1));
		assertEquals(19, p.getMaxLength());
		assertEquals(1, buffer.remaining());
	}	
	
	@Test
	public void testGetBytes() {
		
		//1-byte packet number
		HandshakePacket p = packet("010203", 0x3f, "0405");
		p.getBytes(-1, buffer);
		assertArrayEquals(bytes("e0 00000001 03 010203 02 0405 01 3f"), bytesAndClear());
		
		//2-byte packet number
		p = packet("010203", 0x3ffe, "0405");
		p.getBytes(-1, buffer);
		assertArrayEquals(bytes("e1 00000001 03 010203 02 0405 02 3ffe"), bytesAndClear());

		//2-byte packet number
		p = packet("010203", 0x3ffefd, "0405");
		p.getBytes(-1, buffer);
		assertArrayEquals(bytes("e2 00000001 03 010203 02 0405 03 3ffefd"), bytesAndClear());

		//4-byte packet number
		p = packet("010203", 0x3ffefdfc, "0405");
		p.getBytes(-1, buffer);
		assertArrayEquals(bytes("e3 00000001 03 010203 02 0405 04 3ffefdfc"), bytesAndClear());

		//frames
		p = packet("010203", 0x3ffe, "0405");
		p.getFrames().add(PaddingFrame.INSTANCE);
		p.getFrames().add(PingFrame.INSTANCE);
		p.getBytes(-1, buffer);
		assertArrayEquals(bytes("e1 00000001 03 010203 02 0405 04 3ffe 00 01"), bytesAndClear());
	}
	
	@Test
	public void testPacketNumberDecoding() throws Exception {
		HandshakePacket p = packet("01", 0x0102030405060778L, "02");
		p.getFrames().add(PingFrame.INSTANCE);
		
		long largest = 0x0102030405060777L;
		p.getBytes(largest, buffer);
		assertArrayEquals(bytes("e0 00000001 01 01 01 02 02 78 01"), bytes());
		buffer.flip();
		assertEquals(buffer.remaining(), p.getLength(largest));
		p = parser.parse(buffer, buffer.remaining(), new ParseContext(largest), FrameDecoder.INSTANCE);
		assertEquals(0x0102030405060778L, p.getPacketNumber());
		assertEquals(buffer.limit(), p.getLength(largest));
		buffer.clear();
		
		largest = 0x0102030405060777L - 0x0fff;
		p.getBytes(largest, buffer);
		assertArrayEquals(bytes("e1 00000001 01 01 01 02 03 0778 01"), bytes());
		buffer.flip();
		assertEquals(buffer.remaining(), p.getLength(largest));
		p = parser.parse(buffer, buffer.remaining(), new ParseContext(largest), FrameDecoder.INSTANCE);
		assertEquals(0x0102030405060778L, p.getPacketNumber());
		assertEquals(buffer.limit(), p.getLength(largest));
		buffer.clear();

		largest = 0x0102030405060777L - 0x0fffff;
		p.getBytes(largest, buffer);
		assertArrayEquals(bytes("e2 00000001 01 01 01 02 04 060778 01"), bytes());
		buffer.flip();
		assertEquals(buffer.remaining(), p.getLength(largest));
		p = parser.parse(buffer, buffer.remaining(), new ParseContext(largest), FrameDecoder.INSTANCE);
		assertEquals(0x0102030405060778L, p.getPacketNumber());
		assertEquals(buffer.limit(), p.getLength(largest));
		buffer.clear();

		largest = 0x0102030405060777L - 0x0fffffff;
		p.getBytes(largest, buffer);
		assertArrayEquals(bytes("e3 00000001 01 01 01 02 05 05060778 01"), bytes());
		buffer.flip();
		assertEquals(buffer.remaining(), p.getLength(largest));
		p = parser.parse(buffer, buffer.remaining(), new ParseContext(largest), FrameDecoder.INSTANCE);
		assertEquals(0x0102030405060778L, p.getPacketNumber());
		assertEquals(buffer.limit(), p.getLength(largest));
		buffer.clear();
	}
	
	@Test
	public void testMaxValues() throws Exception { 
		HandshakePacket p = new HandshakePacket(bytes(255), 0x0a, bytes(255), Version.V1);
		p.getFrames().add(PingFrame.INSTANCE);
		p.getBytes(-1, buffer);
		assertArrayEquals(bytes("e0 00000001 ff" 
				+ hex(bytes(255)) + "ff" 
				+ hex(bytes(255)) + "02 0a 01"), bytes());
		buffer.flip();
		p = parser.parse(buffer, buffer.remaining(), new ParseContext(), FrameDecoder.INSTANCE);
		assertArrayEquals(bytes(255), p.getDestinationId());
		assertArrayEquals(bytes(255), p.getSourceId());
		buffer.clear();
		
		p = new HandshakePacket(bytes(1), 0x0a, bytes(1), Version.V1);
		p.getFrames().add(new MultiPaddingFrame(68000));
		p.getBytes(-1, buffer);
		assertArrayEquals(bytes("e0 00000001 01 00 01 00 800109a1 0a" + hex(new byte[68000])), bytes());
		buffer.flip();
		p = parser.parse(buffer, buffer.remaining(), new ParseContext(), FrameDecoder.INSTANCE);
		assertArrayEquals(bytes(1), p.getDestinationId());
		assertArrayEquals(bytes(1), p.getSourceId());
		assertEquals(1, p.getFrames().size());
		assertEquals(68000, p.getFrames().get(0).getLength());
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
		String data = "c0 00000001 03 010203 02 0102 03 31 00 01";
		int len = bytes(data).length;
		
		for (int i=1; i<len; ++i) {
			assertFailure(data, i);
		}
		assertFailure("c1 00000001 03 010203 02 0102 02 00 31 00 01", 15);
	}
	
}
