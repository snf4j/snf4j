/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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
package org.snf4j.websocket.frame;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.session.ISession;
import org.snf4j.websocket.TestWSSession;

public class FrameDecoderTest {

	int frameOff;
	
	FrameDecoder decoder;
	
	byte[] mask = new byte[] {1,2,3,4};
	
	@Before
	public void before() {
		frameOff = 0;
		decoder = new FrameDecoder(true, true, 2400);
	}
	
	long frameLen(long len, boolean mask) {
		long flen = 2;
		
		if (mask) {
			flen += 4;
		}
		if (len > 0xffff) {
			flen += 8;
		}
		else if (len > 125) {
			flen += 2;
		}
		flen += len;
		return flen;
	}
	
	long frameMinLen(long len, boolean mask) {
		return frameLen(len, mask) - len;
	}
	
	@Test
	public void testFrameLen() {
		assertEquals(2+4, frameLen(0,true));
		assertEquals(2, frameLen(0,false));
		assertEquals(2+4+1, frameLen(1,true));
		assertEquals(2+1, frameLen(1,false));
		assertEquals(2+4+125, frameLen(125,true));
		assertEquals(2+125, frameLen(125,false));
		assertEquals(2+4+2+126, frameLen(126,true));
		assertEquals(2+2+126, frameLen(126,false));
		assertEquals(2+4+2+0xffff, frameLen(0xffff,true));
		assertEquals(2+2+0xffff, frameLen(0xffff,false));
		assertEquals(2+4+8+0xffff+1, frameLen(0xffff+1,true));
		assertEquals(2+8+0xffff+1, frameLen(0xffff+1,false));
		assertEquals(2+4+8+0x7fffffffffffffffL, frameLen(0x7fffffffffffffffL,true));
		assertEquals(2+8+0x7fffffffffffffffL, frameLen(0x7fffffffffffffffL,false));
		
	}

	byte[] bytes(char c, int len) {
		return bytes(c, len, false);
	}
	
	byte[] bytes(char c, int len, boolean masked) {
		byte[] b = new byte[len];
		
		for (int i=0; i<len; ++i) {
			if (c == '*') {
				b[i] = (byte)i;
			}
			else {
				b[i] = (byte)c;
			}
			if (masked) {
				b[i] ^= mask[i % 4];
			}
		}
		return b;
	}

	byte[] frame(String text, int off) {
		String[] s = text.split("\\|");
		ByteBuffer buf = ByteBuffer.allocate(0x20100);
		buf.order(ByteOrder.BIG_ENDIAN);
		
		for (int i=0; i<off; ++i) {
			buf.put((byte) -1);
		}
		if (s.length < 4) throw new IllegalArgumentException();
		String s0 = s[0];
		byte b = 0;
		if (s0.length() < 5) throw new IllegalArgumentException();
		if (s0.charAt(0) == 'F') b |= 0x80;
		if (s0.charAt(1) == 'R') b |= 0x40;
		if (s0.charAt(2) == 'R') b |= 0x20;
		if (s0.charAt(3) == 'R') b |= 0x10;
		int i = Integer.parseInt(s0.substring(4));
		if (i > 15) throw new IllegalArgumentException();
		b |= i;
		buf.put(b);
		long l = Long.parseLong(s[1]);
		b = 0;
		if (s[2].charAt(0) == 'M') b |= 128;
		if (l < 126) {
			buf.put((byte) ((byte)l | b));
		}
		else if (l <= 0xffff) {
			buf.put((byte) ((byte) 126 | b));
			buf.putShort((short)l);
		}
		else {
			buf.put((byte) ((byte)127 | b));
			buf.putLong(l);
		}
		if (b != 0) {
			buf.put(mask);
		}
		char c = s[3].charAt(0);
		
		if (c != '-') {
			byte[] payload = bytes(c, (int)l, b != 0);
			if (i == 8 && payload.length > 1) {
				payload[0] = 3;
				payload[1] = (byte)0xe8;
				if (b != 0) {
					payload[0] ^= mask[0];
					payload[1] ^= mask[1];
				}
			}
			buf.put(payload);
		}
		
		buf.flip();
		byte[] frame = new byte[buf.remaining()];
		buf.get(frame);
		return frame;
	}
	
	byte[] frame(String text) {
		return frame(text,frameOff);
	}
	
	void assertFrame(String frame, int... expected) {
		byte[] bytes = frame(frame);
		assertEquals(expected.length, bytes.length);
		for (int i=0; i<bytes.length; ++i) {
			byte b = (byte)expected[i];
			
			assertEquals("byte " + i, b, bytes[i]);
		}
	}
	
	@Test
	public void testFrame() {
		assertFrame("FRRR1|0|M|*|", 0xf1, 0x80, 1, 2, 3, 4);
		assertFrame("fRRR1|0|m|*|", 0x71, 0);
		assertFrame("frRR15|0|m|*|", 0x3f, 0);
		assertFrame("fRrR0|0|m|*|", 0x50, 0);
		assertFrame("fRRr15|0|m|*|", 0x6f, 0);
		
		assertFrame("fRRr2|1|m|*|", 0x62, 1, 0);
		assertFrame("fRRr2|1|M|*|", 0x62, 0x81, 1, 2, 3, 4, 0^1);
		assertFrame("fRRr2|2|M|*|", 0x62, 0x82, 1, 2, 3, 4, 0^1, 1^2);
		assertFrame("fRRr2|3|M|*|", 0x62, 0x83, 1, 2, 3, 4, 0^1, 1^2, 2^3);
		assertFrame("fRRr2|4|M|*|", 0x62, 0x84, 1, 2, 3, 4, 0^1, 1^2, 2^3, 3^4);
		assertFrame("fRRr2|5|M|*|", 0x62, 0x85, 1, 2, 3, 4, 0^1, 1^2, 2^3, 3^4, 4^1);
		assertFrame("fRRr2|1|m|*|", 0x62, 1, 0);
		assertFrame("fRRr2|2|m|*|", 0x62, 2, 0, 1);
		assertFrame("fRRr2|3|m|*|", 0x62, 3, 0, 1, 2);
		assertFrame("fRRr2|4|m|*|", 0x62, 4, 0, 1, 2, 3);
		assertFrame("fRRr2|5|m|*|", 0x62, 5, 0, 1, 2, 3, 4);
		
		assertFrame("fRRr2|125|m|-|", 0x62, 0x7d);
		assertFrame("fRRr2|125|M|-|", 0x62, 0xfd, 1, 2, 3, 4);
		assertFrame("fRRr2|126|m|-|", 0x62, 0x7e, 0, 0x7e);
		assertFrame("fRRr2|126|M|-|", 0x62, 0xfe, 0, 0x7e, 1, 2, 3, 4);
		assertFrame("fRRr2|65535|m|-|", 0x62, 0x7e, 0xff, 0xff);
		assertFrame("fRRr2|65535|M|-|", 0x62, 0xfe, 0xff, 0xff, 1, 2, 3, 4);
		assertFrame("fRRr2|65536|m|-|", 0x62, 0x7f, 0,0,0,0,0,1,0,0);
		assertFrame("fRRr2|65536|M|-|", 0x62, 0xff, 0,0,0,0,0,1,0,0, 1, 2, 3, 4);
		assertFrame("fRRr2|9223372036854775807|m|-|", 0x62, 0x7f, 0x7f,0xff,0xff,0xff,0xff,0xff,0xff,0xff);
		assertFrame("fRRr2|9223372036854775807|M|-|", 0x62, 0xff, 0x7f,0xff,0xff,0xff,0xff,0xff,0xff,0xff, 1, 2, 3, 4);
	}
	
	@Test
	public void testInboundOutboundTypes() {
		FrameDecoder dec = new FrameDecoder(true, true, 0x20000);
		
		assertTrue(dec.getInboundType() == ByteBuffer.class);
		assertTrue(dec.getOutboundType() == Frame.class);
	}
	
	void assertAvailable(byte[] data, long expectedLen, long payloadLen) {
		long minLen = expectedLen - payloadLen;
		
		assertEquals(expectedLen, decoder.available(null, data, frameOff, (int)expectedLen));
		assertEquals(expectedLen, decoder.available(null, data, frameOff, (int)expectedLen+1));
		for (int i=1; i<=expectedLen; ++i) {
			int len = (int)expectedLen-i;
			int expected = len >= minLen ? len : 0;
			
			assertEquals(expected, decoder.available(null, data, frameOff, len));
		}
	}
	
	void assertAvailableBuffer(byte[] data, long expectedLen, long payloadLen) {
		ByteBuffer bb = ByteBuffer.allocate(data.length*2+100);
		long minLen = expectedLen - payloadLen;
		
		for (int i=0; i<2; ++i) {
			bb.put(data);
			assertEquals(expectedLen, decoder.available(null, bb, false));
			bb.put((byte) -1);
			assertEquals(expectedLen, decoder.available(null, bb, false));
			bb.position(bb.position()-2);
			int len = bb.position() >= minLen ? bb.position() : 0;
			assertEquals(len, decoder.available(null, bb, false));
			bb.position(bb.position()+1);
			bb.flip();
			assertEquals(expectedLen, decoder.available(null, bb, true));
			bb.clear();
			bb.put(data);
			bb.put(data);
			bb.flip();
			bb.position(bb.position());
			assertEquals(expectedLen, decoder.available(null, bb, true));
			bb = ByteBuffer.allocateDirect(bb.capacity());
		}
	}
	
	@Test
	public void testAvailableBuffer() {
		assertAvailableBuffer(frame("FRRR1|0|M|*|"), frameLen(0,true), 0);
		assertAvailableBuffer(frame("FRRR1|0|m|*|"), frameLen(0,false), 0);
		assertAvailableBuffer(frame("FRRR1|1|M|*|"), frameLen(1,true), 1);
		assertAvailableBuffer(frame("FRRR1|1|m|*|"), frameLen(1,false), 1);
		assertAvailableBuffer(frame("FRRR1|125|M|*|"), frameLen(125,true), 125);
		assertAvailableBuffer(frame("FRRR1|125|m|*|"), frameLen(125,false), 125);

		assertAvailableBuffer(frame("FRRR1|126|M|*|"), frameLen(126,true), 126);
		assertAvailableBuffer(frame("FRRR1|126|m|*|"), frameLen(126,false), 126);
		assertAvailableBuffer(frame("FRRR1|65535|M|*|"), frameLen(0xffff,true), 0xffff);
		assertAvailableBuffer(frame("FRRR1|65535|m|*|"), frameLen(0xffff,false), 0xffff);
		assertAvailableBuffer(frame("FRRR1|65536|M|*|"), frameLen(0xffff+1,true), 0xffff+1);
		assertAvailableBuffer(frame("FRRR1|65536|m|*|"), frameLen(0xffff+1,false), 0xffff+1);
	}
	
	@Test
	public void testAvailableArray() {
		assertAvailable(frame("FRRR1|0|M|-|"), frameLen(0,true), 0);
		assertAvailable(frame("FRRR1|0|m|-|"), frameLen(0,false), 0);
		assertAvailable(frame("FRRR1|1|M|-|"), frameLen(1,true), 1);
		assertAvailable(frame("FRRR1|1|m|-|"), frameLen(1,false), 1);
		assertAvailable(frame("FRRR1|125|M|-|"), frameLen(125,true), 125);
		assertAvailable(frame("FRRR1|125|m|-|"), frameLen(125,false), 125);
		
		assertAvailable(frame("FRRR1|126|M|-|"), frameLen(126,true), 126);
		assertAvailable(frame("FRRR1|126|m|-|"), frameLen(126,false), 126);
		assertAvailable(frame("FRRR1|65535|M|-|"), frameLen(0xffff,true), 0xffff);
		assertAvailable(frame("FRRR1|65535|m|-|"), frameLen(0xffff,false), 0xffff);
		assertAvailable(frame("FRRR1|65536|M|-|"), frameLen(0xffff+1,true), 0xffff+1);
		assertAvailable(frame("FRRR1|65536|m|-|"), frameLen(0xffff+1,false), 0xffff+1);
		
		frameOff = 5;
		assertAvailable(frame("FRRR1|0|M|-|"), frameLen(0,true), 0);
		assertAvailable(frame("FRRR1|0|m|-|"), frameLen(0,false), 0);
		assertAvailable(frame("FRRR1|1|M|-|"), frameLen(1,true), 1);
		assertAvailable(frame("FRRR1|1|m|-|"), frameLen(1,false), 1);
		assertAvailable(frame("FRRR1|125|M|-|"), frameLen(125,true), 125);
		assertAvailable(frame("FRRR1|125|m|-|"), frameLen(125,false), 125);
		
		assertAvailable(frame("FRRR1|126|M|-|"), frameLen(126,true), 126);
		assertAvailable(frame("FRRR1|126|m|-|"), frameLen(126,false), 126);
		assertAvailable(frame("FRRR1|65535|M|-|"), frameLen(0xffff,true), 0xffff);
		assertAvailable(frame("FRRR1|65535|m|-|"), frameLen(0xffff,false), 0xffff);
		assertAvailable(frame("FRRR1|65536|M|-|"), frameLen(0xffff+1,true), 0xffff+1);
		assertAvailable(frame("FRRR1|65536|m|-|"), frameLen(0xffff+1,false), 0xffff+1);
		
		TestWSSession s = new TestWSSession();
		long len = frameLen(0,true);
		for (int i=0; i<126; ++i) {
			assertEquals(len, decoder.available(null, frame("FRRR1|"+i+"|M|-|"), 5, (int)len));
			++len;
		}
		
		len = frameLen(126,true);
		for (int i=126; i<0xffff; ++i) {
			assertEquals(len, decoder.available(null, frame("FRRR1|"+i+"|M|-|"), 5, (int)len));
			++len;
		}
		
		frameOff = 0;
		len = frameLen(0x7fffffffL,true);
		int d = (int) (len - 0x7fffffffL);
		len = frameLen(0x7fffffffL-d,true);
		byte[] f = frame("FRRR1|"+(0x7fffffffL-d)+"|M|-|");
		assertEquals(len, decoder.available(null, f, 0, (int)len));
		assertEquals(Integer.MAX_VALUE, decoder.available(null, f, 0,Integer.MAX_VALUE));
		
		f = frame("FRRR1|"+(0x7fffffffL-d+1)+"|M|-|");
		try {
			decoder.available(s, f, 0,Integer.MAX_VALUE);
			fail();
		}
		catch (InvalidFrameException e) {
			assertEquals("Extended payload length (2147483634) > 2147483633", e.getMessage());
		}
		decoder = new FrameDecoder(true, true, 2400);
		f = frame("FRRR1|"+(0x7fffffffffffffffL-d)+"|M|-|");
		try {
			decoder.available(s, f, 0,Integer.MAX_VALUE);
			fail();
		}
		catch (InvalidFrameException e) {
			assertEquals("Extended payload length (9223372036854775793) > 2147483633", e.getMessage());
		}
		decoder = new FrameDecoder(true, true, 2400);
		f = frame("FRRR1|"+0x7fffffffffffffffL+"|M|-|");
		f[2] = (byte)0xff;
		try {
			decoder.available(s, f, 0,Integer.MAX_VALUE);
			fail();
		}
		catch (InvalidFrameException e) {
			assertEquals("Negative payload length (-1)", e.getMessage());
		}
		
		decoder = new FrameDecoder(true, true, 2400);
		f = frame("FRRR1|"+(0x7fffffffffffffffL-d+1)+"|M|-|");
		assertEquals(0, decoder.available(null, f, 0,Integer.MAX_VALUE));
	}
	
	Throwable assertDecode(FrameDecoder decoder, ISession session, byte[] data, Frame frame, boolean clientMode, boolean returnException) throws Exception {
		List<Frame> out = new ArrayList<Frame>();

		if (decoder == null) {
			decoder = new FrameDecoder(clientMode, true, 0x20000);
		}
		
		try {
			decoder.decode(session, ByteBuffer.wrap(data), out);
			assertEquals(frame != null ? 1 : 0, out.size());
			if (frame != null) {
				Frame f = out.get(0);
				
				assertEquals(1, out.size());
				assertEquals(frame.isFinalFragment(), f.isFinalFragment());
				assertEquals(frame.getRsvBits(), f.getRsvBits());
				assertArrayEquals(frame.getPayload(), f.getPayload());
				assertTrue(frame.getClass() == f.getClass());
			}
			else {
				assertEquals(0, out.size());
			}
		}
		catch (Exception e) {
			if (returnException) {
				return e;
			}
			throw e;
		}
		return null;
	}

	void assertDecode(ISession session, byte[] data, Frame frame, boolean clientMode) throws Exception {
		assertDecode(null, session, data, frame, clientMode, false);
	}
	
	@Test
	public void testDecodeFinRsv() throws Exception {
		TestWSSession s = new TestWSSession();
		assertDecode(s, frame("FRRR1|0|m|-|"), new TextFrame(true, 7, new byte[0]), true);
		assertDecode(s, frame("fRRR1|0|m|-|"), new TextFrame(false, 7, new byte[0]), true);
		assertDecode(s, frame("FrRR1|0|m|-|"), new TextFrame(true, 3, new byte[0]), true);
		assertDecode(s, frame("FrrR1|0|m|-|"), new TextFrame(true, 1, new byte[0]), true);
		assertDecode(s, frame("frrr1|0|m|-|"), new TextFrame(false, 0, new byte[0]), true);
		assertEquals("Fragmented control frame",
		assertDecode(null, s, frame("frrr8|0|m|-|"), null, true, true).getMessage());
		assertEquals("Fragmented control frame",
		assertDecode(null, s, frame("frrr9|0|m|-|"), null, true, true).getMessage());
		assertEquals("Fragmented control frame",
		assertDecode(null, s, frame("frrr10|0|m|-|"), null, true, true).getMessage());
		
		FrameDecoder decoder = new FrameDecoder(true, true, 0x20000);
		assertDecode(decoder, s, frame("FRRR1|0|m|-|"), new TextFrame(true, 7, new byte[0]), true, false);
		assertDecode(decoder, s, frame("Frrr1|0|m|-|"), new TextFrame(true, 0, new byte[0]), true, false);
		decoder = new FrameDecoder(true, false, 0x20000);
		assertDecode(decoder, s, frame("Frrr1|0|m|-|"), new TextFrame(true, 0, new byte[0]), true, false);
		assertEquals("Unexpected non-zero RSV bits (4)",
		assertDecode(decoder, s, frame("FRrr1|0|m|-|"), new TextFrame(true, 4, new byte[0]), true, true).getMessage());
	}
	
	@Test
	public void testDecodeOpcode() throws Exception {
		TestWSSession s = new TestWSSession();
		FrameDecoder dec = new FrameDecoder(true, true, 0x20000);
		assertDecode(dec, s, frame("fRrR1|0|m|-|"), new TextFrame(false, 5, new byte[0]), true, false);
		assertDecode(dec, s, frame("fRrR0|0|m|-|"), new ContinuationFrame(false, 5, new byte[0]), true, false);
		assertDecode(s, frame("FrrR2|0|m|-|"), new BinaryFrame(true, 1, new byte[0]), true);
		assertDecode(s, frame("FrrR8|0|m|-|"), new CloseFrame(1, new byte[0]), true);
		assertDecode(s, frame("FRrR9|0|m|-|"), new PingFrame(5, new byte[0]), true);
		assertDecode(s, frame("FRRR10|0|m|-|"), new PongFrame(7, new byte[0]), true);
		for (int i=3; i<= 7; ++i) {
			assertEquals("Unexpected opcode value ("+i+")", assertDecode(null, s, frame("FRRR"+i+"|0|m|-|"), null, true, true).getMessage());
		}
		for (int i=11; i<= 15; ++i) {
			assertEquals("Unexpected opcode value ("+i+")", assertDecode(null, s, frame("FRRR"+i+"|0|m|-|"), null, true, true).getMessage());
		}
	}				
	
	@Test
	public void testDecodeLengthMask() throws Exception {
		TestWSSession s = new TestWSSession();
		assertDecode(s, frame("FRRR2|0|M|-|"), new BinaryFrame(true, 7, new byte[0]), false);
		assertDecode(s, frame("FRRR2|0|m|-|"), new BinaryFrame(true, 7, new byte[0]), true);
		assertDecode(s, frame("FRRR2|1|M|*|"), new BinaryFrame(true, 7, bytes('*',1)), false);
		assertDecode(s, frame("FRRR2|1|m|*|"), new BinaryFrame(true, 7, bytes('*',1)), true);
		assertDecode(s, frame("FRRR2|125|M|*|"), new BinaryFrame(true, 7, bytes('*',125)), false);
		assertDecode(s, frame("FRRR2|125|m|*|"), new BinaryFrame(true, 7, bytes('*',125)), true);
		assertDecode(s, frame("FRRR2|126|M|*|"), new BinaryFrame(true, 7, bytes('*',126)), false);
		assertDecode(s, frame("FRRR2|126|m|*|"), new BinaryFrame(true, 7, bytes('*',126)), true);
		assertDecode(s, frame("FRRR2|65535|M|*|"), new BinaryFrame(true, 7, bytes('*',65535)), false);
		assertDecode(s, frame("FRRR2|65535|m|*|"), new BinaryFrame(true, 7, bytes('*',65535)), true);
		assertDecode(s, frame("FRRR2|65536|M|*|"), new BinaryFrame(true, 7, bytes('*',65536)), false);
		assertDecode(s, frame("FRRR2|65536|m|*|"), new BinaryFrame(true, 7, bytes('*',65536)), true);
		
		assertEquals("Unexpected payload masking", assertDecode(null, s, 
				frame("FRRR2|0|M|*|"), null, true, true).getMessage());
		assertEquals("Unexpected payload masking", assertDecode(null, s, 
				frame("FRRR2|0|m|*|"), null, false, true).getMessage());
		
		assertDecode(s, frame("FRRR8|0|M|*|"), new CloseFrame(7, bytes('*',0)), false);
		assertEquals("Invalid payload length (1) in close frame", 
				assertDecode(null, s, frame("FRRR8|1|M|*|"), null, false, true).getMessage());
		assertDecode(s, frame("FRRR8|2|M|*|"), new CloseFrame(7, new byte[] {3, (byte) 0xe8}), false);
		
		byte[] bytes = bytes('*',125);
		bytes[0] = 3;
		bytes[1] = (byte) 0xe8;
		assertDecode(s, frame("FRRR8|125|M|*|"), new CloseFrame(7, bytes), false);
		assertDecode(s, frame("FRRR9|125|M|*|"), new PingFrame(7, bytes('*',125)), false);
		assertDecode(s, frame("FRRR10|125|M|*|"), new PongFrame(7, bytes('*',125)), false);
		for (int i=8; i<=10; ++i) {
			assertEquals("Invalid payload length (126) in control frame", 
					assertDecode(null, s, frame("FRRR"+i+"|126|M|-|"), null, false, true).getMessage());
		}
		
		assertEquals("Invalid minimal payload length",
				assertDecode(null, s, new byte[] {(byte)0xf2, 126, 0, 125}, null, true, true).getMessage());
		assertEquals("Invalid minimal payload length",
				assertDecode(null, s, new byte[] {(byte)0xf2, 127, 0,0,0,0,0,0,0, 125}, null, true, true).getMessage());
		assertEquals("Invalid minimal payload length",
				assertDecode(null, s, new byte[] {(byte)0xf2, 127, 
						0,0,0,0,0,0,(byte)0xff,(byte)0xff}, null, true, true).getMessage());

		assertEquals("Invalid maximum payload length",
				assertDecode(null, s, new byte[] {(byte)0xf2, 127, 
						(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff}, 
						null, true, true).getMessage());
		assertEquals("Invalid maximum payload length",
				assertDecode(null, s, new byte[] {(byte)0xf2, 127, 
						(byte)0x80,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff}, 
						null, true, true).getMessage());
		assertEquals("Invalid maximum payload length",
				assertDecode(null, s, new byte[] {(byte)0xf2, 127, 
						(byte)0x7f,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff}, 
						null, true, true).getMessage());
		assertEquals("Invalid maximum payload length",
				assertDecode(null, s, new byte[] {(byte)0xf2, 127, 
						0,0,0,0,(byte)0x80,(byte)0xff,(byte)0xff,(byte)0xff}, 
						null, true, true).getMessage());
		assertEquals("Maximum frame length (131072) has been exceeded",
				assertDecode(null, s, new byte[] {(byte)0xf2, 127, 
						0,0,0,0,0,2,0,1}, 
						null, true, true).getMessage());
		assertDecode(s, frame("FRRR2|131072|m|*|"), new BinaryFrame(true, 7, bytes('*',131072)), true);
	}
	
	void assertFragment(FrameDecoder decoder, ISession session, Opcode opcode, boolean fin, String exception) throws Exception {
		Frame frame = null;
		byte[] b = new byte[0];
		
		switch (opcode) {
		case CONTINUATION:
			frame = new ContinuationFrame(fin, 7, b);
			break;
			
		case TEXT:
			frame = new TextFrame(fin, 7, b);
			break;
			
		case BINARY:
			frame = new BinaryFrame(fin, 7, b);
			break;
			
		case CLOSE:
			frame = new CloseFrame(7, b);
			break;
			
		case PING:
			frame = new PingFrame(7, b);
			break;
			
		case PONG:
			frame = new PongFrame(7, b);
			break;
		}
		
		String s = (fin ? "F" : "f") + "RRR"+frame.getOpcode().value()+"|0|m|-|";
		if (exception == null) {
			assertDecode(decoder, session, frame(s), frame, false, false);
		}
		else {
			assertEquals(exception,
					assertDecode(decoder, session, frame(s), frame, false, true).getMessage());
			
		}
	}
	
	void assertFragment(FrameDecoder decoder, ISession session, Opcode opcode, boolean fin) throws Exception {
		assertFragment(decoder, session, opcode, fin, null);
	}

	void assertControls(FrameDecoder decoder, ISession session) throws Exception {
		assertFragment(decoder, session, Opcode.CLOSE, true);
		assertFragment(decoder, session, Opcode.PING, true);
		assertFragment(decoder, session, Opcode.PONG, true);
	}
	
	@Test
	public void testDecodeFragemntation() throws Exception {
		TestWSSession s = new TestWSSession();
		FrameDecoder d = new FrameDecoder(true, true, 0x20000);
		
		assertFragment(d, s, Opcode.TEXT, true);
		assertFragment(d, s, Opcode.BINARY, true);
		assertControls(d, s);		
		assertFragment(d, s, Opcode.CONTINUATION, true, "Continuation frame outside fragmented message");
		
		d = new FrameDecoder(true, true, 0x20000);
		assertFragment(d, s, Opcode.TEXT, false);
		assertFragment(d, s, Opcode.TEXT, true, "Non-continuation frame while inside fragmented massage");
		d = new FrameDecoder(true, true, 0x20000);
		assertFragment(d, s, Opcode.TEXT, false);
		assertFragment(d, s, Opcode.BINARY, true, "Non-continuation frame while inside fragmented massage");
		d = new FrameDecoder(true, true, 0x20000);
		assertFragment(d, s, Opcode.TEXT, false);
		assertFragment(d, s, Opcode.TEXT, false, "Non-continuation frame while inside fragmented massage");
		d = new FrameDecoder(true, true, 0x20000);
		assertFragment(d, s, Opcode.TEXT, false);
		assertFragment(d, s, Opcode.BINARY, false, "Non-continuation frame while inside fragmented massage");
		d = new FrameDecoder(true, true, 0x20000);
		assertFragment(d, s, Opcode.TEXT, false);
		assertControls(d, s);	

		d = new FrameDecoder(true, true, 0x20000);
		assertFragment(d, s, Opcode.TEXT, false);
		assertFragment(d, s, Opcode.CONTINUATION, false);
		assertControls(d, s);	
		assertFragment(d, s, Opcode.TEXT, true, "Non-continuation frame while inside fragmented massage");
		d = new FrameDecoder(true, true, 0x20000);
		assertFragment(d, s, Opcode.TEXT, false);
		assertFragment(d, s, Opcode.CONTINUATION, false);
		assertFragment(d, s, Opcode.BINARY, true, "Non-continuation frame while inside fragmented massage");
		d = new FrameDecoder(true, true, 0x20000);
		assertFragment(d, s, Opcode.TEXT, false);
		assertFragment(d, s, Opcode.CONTINUATION, false);
		assertFragment(d, s, Opcode.TEXT, false, "Non-continuation frame while inside fragmented massage");
		d = new FrameDecoder(true, true, 0x20000);
		assertFragment(d, s, Opcode.TEXT, false);
		assertFragment(d, s, Opcode.CONTINUATION, false);
		assertFragment(d, s, Opcode.BINARY, false, "Non-continuation frame while inside fragmented massage");
	
		d = new FrameDecoder(true, true, 0x20000);
		assertFragment(d, s, Opcode.TEXT, false);
		assertFragment(d, s, Opcode.CONTINUATION, false);
		assertFragment(d, s, Opcode.CONTINUATION, true);
		assertFragment(d, s, Opcode.TEXT, true);
		assertFragment(d, s, Opcode.BINARY, true);
		assertControls(d, s);		
	}
	
	byte[][] split(byte[] data) {
		int l = data.length/2;
		
		byte[][] splitted = new byte[2][];
		
		byte[] b = new byte[l];
		System.arraycopy(data, 0, b, 0, b.length);
		splitted[0] = b;
		b = new byte[data.length - l];
		System.arraycopy(data, l, b, 0, b.length);
		splitted[1] = b;
		return splitted;
	}
	
	@Test
	public void testSplittedFrame() throws Exception {
		TestWSSession s = new TestWSSession();
		FrameDecoder dec = new FrameDecoder(true, true, 0x20000);
		List<Frame> out = new ArrayList<Frame>();
		byte[] b = frame("FRRR2|100|m|*|");
		
		dec.decode(s, ByteBuffer.wrap(b, 0, 50), out);
		assertEquals(0, out.size());
		byte[] b2 = new byte[b.length-50];
		System.arraycopy(b, 50, b2, 0, b2.length);
		assertDecode(dec, s, b2, new BinaryFrame(true, 7, bytes('*',100)), true, false);
		assertDecode(dec, s, frame("FRRR2|1|m|*|"), new BinaryFrame(true, 7, bytes('*',1)), false, false);
		
		dec.decode(s, ByteBuffer.wrap(b, 0, 50), out);
		assertEquals(0, out.size());
		byte[] b3 = new byte[b2.length/2];
		byte[] b4 = new byte[b2.length - b3.length];
		System.arraycopy(b2, 0, b3, 0, b3.length);
		System.arraycopy(b2, b3.length, b4, 0, b4.length);
		dec.decode(s, ByteBuffer.wrap(b3), out);
		assertEquals(0, out.size());
		assertDecode(dec, s, b4, new BinaryFrame(true, 7, bytes('*',100)), true, false);
		assertDecode(dec, s, frame("FRRR2|1|m|*|"), new BinaryFrame(true, 7, bytes('*',1)), true, false);
		
		dec = new FrameDecoder(false, true, 0x20000);	
		out.clear();
		b = frame("FRRR2|100|M|*|");
		
		dec.decode(s, ByteBuffer.wrap(b, 0, 50), out);
		assertEquals(0, out.size());
		b2 = new byte[b.length-50];
		System.arraycopy(b, 50, b2, 0, b2.length);
		assertDecode(dec, s, b2, new BinaryFrame(true, 7, bytes('*',100)), false, false);
		assertDecode(dec, s, frame("FRRR2|1|M|*|"), new BinaryFrame(true, 7, bytes('*',1)), false, false);
	}
	
	@Test
	public void testSplittedFrameAvailable() throws Exception {
		TestWSSession s = new TestWSSession();
		FrameDecoder dec = new FrameDecoder(true, true, 0x20000);
		List<Frame> out = new ArrayList<Frame>();
		byte[] b = frame("FRRR2|100|m|*|");
		
		byte[][] sp1 = split(b);
		assertEquals(sp1[0].length, dec.available(s, sp1[0], 0, sp1[0].length));
		assertEquals(sp1[0].length, dec.available(s, ByteBuffer.wrap(sp1[0]), true));
		
		dec.decode(s, ByteBuffer.wrap(sp1[0]), out);
		assertEquals(0, out.size());
		
		byte[][] sp2 = split(sp1[0]);
		byte[] b2 = new byte[sp1[1].length + 1];

		assertEquals(1, dec.available(s, new byte[1], 0, 1));
		assertEquals(sp2[0].length, dec.available(s, sp2[0], 0, sp2[0].length));
		assertEquals(sp1[1].length, dec.available(s, sp1[1], 0, sp1[1].length));
		assertEquals(sp1[1].length, dec.available(s, b2, 0, b2.length));
		
		assertEquals(1, dec.available(s, ByteBuffer.wrap(new byte[1]), true));
		assertEquals(sp2[0].length, dec.available(s, ByteBuffer.wrap(sp2[0]), true));
		assertEquals(sp1[1].length, dec.available(s, ByteBuffer.wrap(sp1[1]), true));
		assertEquals(sp1[1].length, dec.available(s, ByteBuffer.wrap(b2), true));
	}
	
	@Test
	public void testCloseFrame() throws Exception {
		TestWSSession s = new TestWSSession();
		assertDecode(s, frame("FRRR8|0|m|*|"), new CloseFrame(7, new byte[0]), true);
		
		byte[] frame = frame("FRRR8|2|m|*|");
		int i = frame.length-2;
		frame[i] = 0;
		frame[i+1] = 0;
		assertEquals("Invalid close frame status code (0)",
				assertDecode(null, s, frame, null, true, true).getMessage());
		frame[i] = -1;
		frame[i+1] = -1;
		assertEquals("Invalid close frame status code (65535)",
				assertDecode(null, s, frame, null, true, true).getMessage());
		frame[i] = 3;
		frame[i+1] = (byte)0xe7;
		assertEquals("Invalid close frame status code (999)",
				assertDecode(null, s, frame, null, true, true).getMessage());
		frame[i] = 3;
		frame[i+1] = (byte)0xe8;
		assertDecode(s, frame, new CloseFrame(7, new byte[] {3, (byte)0xe8}), true);
		frame[i] = 3;
		frame[i+1] = (byte)0xe9;
		assertDecode(s, frame, new CloseFrame(7, new byte[] {3, (byte)0xe9}), true);
		frame[i] = 0x13;
		frame[i+1] = (byte)0x87;
		assertDecode(s, frame, new CloseFrame(7, new byte[] {0x13, (byte)0x87}), true);
		frame[i] = 0x13;
		frame[i+1] = (byte)0x88;
		assertEquals("Invalid close frame status code (5000)",
				assertDecode(null, s, frame, null, true, true).getMessage());
		
		frame = frame("FRRR8|5|m|*|");
		i = frame.length-5;	
		frame[i] = 3;
		frame[i+1] = (byte)0xe8;
		frame[i+2] = (byte)0xdf;
		frame[i+3] = (byte)0xdf;
		frame[i+4] = (byte)0xbf;
		assertEquals("Invalid close frame reason value: bytes are not UTF-8",
				assertDecode(null, s, frame, null, true, true).getMessage());
	}
	
	@Test
	public void testClosedDecoder() throws Exception{
		TestWSSession s = new TestWSSession();
		FrameDecoder decoder = new FrameDecoder(true, true, 0x20000);
		List<Frame> out = new ArrayList<Frame>();
		byte[] frame = frame("FRRR8|2|m|*|");
		int i = frame.length-2;
		frame[i] = 0;
		frame[i+1] = 0;
		assertEquals("Invalid close frame status code (0)",
				assertDecode(decoder, s, frame, null, true, true).getMessage());
		decoder.decode(s, ByteBuffer.wrap(frame) , out);
		assertEquals(0, out.size());
		byte[] frame2 = new byte[frame.length*2];
		System.arraycopy(frame, 0, frame2, 0, frame.length);
		assertEquals(frame2.length, decoder.available(s, frame2, 0, frame2.length));
		assertEquals(frame2.length, decoder.available(s, ByteBuffer.wrap(frame2), true));
		ByteBuffer b = ByteBuffer.allocate(frame2.length);
		b.put(frame2);
		assertEquals(frame2.length, decoder.available(s, b, false));
	}
	
}
