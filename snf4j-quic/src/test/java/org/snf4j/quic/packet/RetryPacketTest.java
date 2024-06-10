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

public class RetryPacketTest extends CommonTest {
	
	static final IPacketParser parser = RetryPacket.getParser();
	
	RetryPacket parse(String data, int remaining) throws Exception {
		return parser.parse(
				buffer(data), 
				remaining == -1 ? buffer.remaining() : remaining, 
				new ParseContext(), 
				FrameDecoder.INSTANCE);
	}
	
	static RetryPacket packet(String destId, String srcId, String token, String tag) {
		return new RetryPacket(bytes(destId), bytes(srcId), Version.V1, bytes(token), bytes(tag));
	}

	@Test
	public void testGetType() {
		assertTrue(PacketType.RETRY.hasLongHeader());
		assertNull(PacketType.RETRY.encryptionLevel());
		assertSame(PacketType.RETRY, RetryPacket.getParser().getType());
		assertSame(PacketType.RETRY, packet("","","",hex(bytes(16))).getType());
	}
	
	@Test
	public void testParser() throws Exception {
		//no token
		RetryPacket p = parse("f0 00000001 03 010203 02 0102" + hex(bytes(16)), -1);
		assertNotNull(p);
		assertSame(Version.V1, p.getVersion());
		assertArrayEquals(bytes("010203"), p.getDestinationId());
		assertArrayEquals(bytes("0102"), p.getSourceId());
		assertSame(PacketUtil.EMPTY_ARRAY, p.getToken());
		assertArrayEquals(bytes(16), p.getIntegrityTag());
		assertNull(p.getFrames());
		assertEquals(12+16, p.getLength(-1,10));
		assertEquals(12+16, p.getMaxLength(10));

		assertEquals(0, buffer.remaining());

		//token
		p = parse("f0 00000001 03 010203 02 0102 01" + hex(bytes(16)), -1);
		assertNotNull(p);
		assertSame(Version.V1, p.getVersion());
		assertArrayEquals(bytes("010203"), p.getDestinationId());
		assertArrayEquals(bytes("0102"), p.getSourceId());
		assertArrayEquals(bytes("01"), p.getToken());
		assertArrayEquals(bytes(16), p.getIntegrityTag());
		assertEquals(13+16, p.getLength(-1,10));
		assertEquals(13+16, p.getMaxLength(10));
		assertEquals(0, buffer.remaining());

		//more data in buffer
		p = parse("f0 00000001 03 010203 02 0102 01" + hex(bytes(16)) + "00", 13 + 16);
		assertNotNull(p);
		assertSame(Version.V1, p.getVersion());
		assertArrayEquals(bytes("010203"), p.getDestinationId());
		assertArrayEquals(bytes("0102"), p.getSourceId());
		assertArrayEquals(bytes("01"), p.getToken());
		assertArrayEquals(bytes(16), p.getIntegrityTag());
		assertEquals(1, buffer.remaining());
		
		assertNull(parser.parseHeader(null, 10, null));
		assertNull(parser.parse(null, null, null, null));
	}
	
	@Test
	public void testGetPayloadBytes() {
		RetryPacket p = packet("010203", "0405", "", hex(bytes(16)));
		
		assertEquals(0, p.getPayloadLength());
		p.getPayloadBytes(buffer);
		assertEquals(0, buffer.position());
	}
	
	@Test
	public void testGetBytes() {
		RetryPacket p = packet("010203", "0405", "", hex(bytes(16)));
		p.getBytes(-1, buffer);
		assertArrayEquals(bytes("f0 00000001 03 010203 02 0405" + hex(bytes(16))), bytesAndClear());

		p = packet("010203", "0405", "060708", hex(bytes(16)));
		p.getBytes(-1, buffer);
		assertArrayEquals(bytes("f0 00000001 03 010203 02 0405 060708" + hex(bytes(16))), bytesAndClear());
	}
	
	@Test
	public void testMaxValues() throws Exception { 
		RetryPacket p = new RetryPacket(bytes(255), bytes(255), Version.V1, bytes(20000), bytes(16));
		p.getBytes(-1, buffer);
		assertArrayEquals(bytes("f0 00000001 ff" 
				+ hex(bytes(255)) + "ff" 
				+ hex(bytes(255))  
				+ hex(bytes(20000))
				+ hex(bytes(16))), bytes());
		buffer.flip();
		p = parser.parse(buffer, buffer.remaining(), new ParseContext(), FrameDecoder.INSTANCE);
		assertArrayEquals(bytes(255), p.getDestinationId());
		assertArrayEquals(bytes(255), p.getSourceId());
		assertArrayEquals(bytes(20000), p.getToken());
		assertArrayEquals(bytes(16), p.getIntegrityTag());
		buffer.clear();
	}
	
	void assertFailure(String data, int remaining) throws Exception {
		try {
			parse(data, remaining);
			fail();
		} 
		catch (QuicException e) {
			assertSame(TransportError.PROTOCOL_VIOLATION, e.getTransportError());
		}
	}

	@Test
	public void testParseFailure() throws Exception {
		String data = "f0 00000001 03 010203 02 0405" + hex(bytes(16));
		int len = bytes(data).length;
		
		for (int i=1; i<len; ++i) {
			assertFailure(data, i);
		}
	}
	
}
