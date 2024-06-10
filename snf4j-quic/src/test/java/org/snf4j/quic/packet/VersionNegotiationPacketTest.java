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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.Version;
import org.snf4j.quic.frame.FrameDecoder;

public class VersionNegotiationPacketTest extends CommonTest {
	
	static final IPacketParser parser = VersionNegotiationPacket.getParser();
	
	VersionNegotiationPacket parse(String data, int remaining) throws Exception {
		return parser.parse(
				buffer(data), 
				remaining == -1 ? buffer.remaining() : remaining, 
				new ParseContext(), 
				FrameDecoder.INSTANCE);
	}
	
	static VersionNegotiationPacket packet(String destId, String srcId, Version... versions) {
		return new VersionNegotiationPacket(bytes(destId), bytes(srcId), versions);
	}

	@Test
	public void testGetType() {
		assertTrue(PacketType.VERSION_NEGOTIATION.hasLongHeader());
		assertNull(PacketType.VERSION_NEGOTIATION.encryptionLevel());
		assertSame(PacketType.VERSION_NEGOTIATION, VersionNegotiationPacket.getParser().getType());
		assertSame(PacketType.VERSION_NEGOTIATION, packet("","",Version.V1).getType());
	}
	
	@Test
	public void testParser() throws Exception {
		//1 version
		VersionNegotiationPacket p = parse("c0 00000000 03 010203 02 0102 00000001", -1);
		assertNotNull(p);
		assertSame(Version.V0, p.getVersion());
		assertArrayEquals(bytes("010203"), p.getDestinationId());
		assertArrayEquals(bytes("0102"), p.getSourceId());
		assertEquals(-1, p.getPacketNumber());
		assertNull(p.getFrames());
		assertEquals(16, p.getLength(-1,5));
		assertEquals(16, p.getMaxLength(10));
		assertEquals(0, buffer.remaining());
		assertArrayEquals(new Version[] {Version.V1}, p.getSupportedVersions());

		//2 versions
		p = parse("c0 00000000 03 010203 02 0102 00000001 00000000", -1);
		assertNotNull(p);
		assertSame(Version.V0, p.getVersion());
		assertArrayEquals(bytes("010203"), p.getDestinationId());
		assertArrayEquals(bytes("0102"), p.getSourceId());
		assertEquals(-1, p.getPacketNumber());
		assertNull(p.getFrames());
		assertEquals(20, p.getLength(-1,0));
		assertEquals(20, p.getMaxLength(0));
		assertEquals(0, buffer.remaining());
		assertArrayEquals(new Version[] {Version.V1, Version.V0}, p.getSupportedVersions());

		//3 versions but one uknown
		p = parse("c0 00000000 03 010203 02 0102 00000001 00000002 00000000", -1);
		assertNotNull(p);
		assertSame(Version.V0, p.getVersion());
		assertArrayEquals(bytes("010203"), p.getDestinationId());
		assertArrayEquals(bytes("0102"), p.getSourceId());
		assertEquals(-1, p.getPacketNumber());
		assertNull(p.getFrames());
		assertEquals(24, p.getLength(-1,0));
		assertEquals(24, p.getMaxLength(0));
		assertEquals(0, buffer.remaining());
		assertArrayEquals(new Version[] {Version.V1, null, Version.V0}, p.getSupportedVersions());
		
		//no versions
		p = parse("c0 00000000 03 010203 02 0102", -1);
		assertNotNull(p);
		assertSame(Version.V0, p.getVersion());
		assertArrayEquals(bytes("010203"), p.getDestinationId());
		assertArrayEquals(bytes("0102"), p.getSourceId());
		assertEquals(-1, p.getPacketNumber());
		assertNull(p.getFrames());
		assertEquals(12, p.getLength(-1,0));
		assertEquals(12, p.getMaxLength(0));
		assertEquals(0, buffer.remaining());
		assertArrayEquals(new Version[0], p.getSupportedVersions());

		//more data in buffer
		p = parse("c0 00000000 03 010203 02 0102 00000001 00000002 00000000 00", 24);
		assertNotNull(p);
		assertSame(Version.V0, p.getVersion());
		assertArrayEquals(bytes("010203"), p.getDestinationId());
		assertArrayEquals(bytes("0102"), p.getSourceId());
		assertEquals(-1, p.getPacketNumber());
		assertNull(p.getFrames());
		assertEquals(24, p.getLength(-1,0));
		assertEquals(24, p.getMaxLength(0));
		assertEquals(1, buffer.remaining());
		assertArrayEquals(new Version[] {Version.V1, null, Version.V0}, p.getSupportedVersions());
		
		assertNull(parser.parseHeader(null, 10, null));
		assertNull(parser.parse(null, null, null, null));
	}
	
	@Test
	public void testGetPayloadBytes() {
		VersionNegotiationPacket p = packet("010203", "0405");
		
		assertEquals(0, p.getPayloadLength());
		p.getPayloadBytes(buffer);
		assertEquals(0, buffer.position());
	}
	
	@Test
	public void testGetBytes() {
		
		//no versions
		VersionNegotiationPacket p = packet("010203", "0405");
		p.getBytes(-1, buffer);
		assertArrayEquals(bytes("c0 00000000 03 010203 02 0405"), bytesAndClear());

		//1 version
		p = packet("010203", "0405", Version.V1);
		p.getBytes(-1, buffer);
		assertArrayEquals(bytes("c0 00000000 03 010203 02 0405 00000001"), bytesAndClear());

		//2 versions
		p = packet("010203", "0405", Version.V1, Version.V0);
		p.getBytes(-1, buffer);
		assertArrayEquals(bytes("c0 00000000 03 010203 02 0405 00000001 00000000"), bytesAndClear());
	}
	
	@Test
	public void testMaxValues() throws Exception { 
		VersionNegotiationPacket p = new VersionNegotiationPacket(bytes(255), bytes(255), Version.V1);
		p.getBytes(-1, buffer);
		assertArrayEquals(bytes("c0 00000000 ff" 
				+ hex(bytes(255)) + "ff" 
				+ hex(bytes(255)) + "00000001"), bytes());
		buffer.flip();
		p = parser.parse(buffer, buffer.remaining(), new ParseContext(), FrameDecoder.INSTANCE);
		assertArrayEquals(bytes(255), p.getDestinationId());
		assertArrayEquals(bytes(255), p.getSourceId());
		buffer.clear();
	}
	
	void assertFailure(String data, int remaining) throws Exception {
		try {
			parse(data, remaining);
			fail("Remaining " + remaining);
		} 
		catch (QuicException e) {
			assertSame(TransportError.PROTOCOL_VIOLATION, e.getTransportError());
		}
	}
	
	@Test
	public void testParseFailure() throws Exception {
		String data = "c0 00000000 03 010203 02 0102";
		int len = bytes(data).length;
		for (int i=1; i<len; ++i) {
			assertFailure(data, i);
		}

		data = "c0 00000000 03 010203 02 0102 00000001";
		int len2 = bytes(data).length;
		for (int i=len+1; i<len2; ++i) {
			assertFailure(data, i);
		}

		len = len2;
		data = "c0 00000000 03 010203 02 0102 00000001 00000000";
		len2 = bytes(data).length;
		for (int i=len+1; i<len2; ++i) {
			assertFailure(data, i);
		}
	}
	
}
