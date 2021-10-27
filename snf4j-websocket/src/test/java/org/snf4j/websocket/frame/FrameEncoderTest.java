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
import static org.junit.Assert.assertFalse;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.TestSession;

public class FrameEncoderTest {

	List<ByteBuffer> out = new ArrayList<ByteBuffer>();

	TestSession session = new TestSession() {
		@Override
		public ByteBuffer allocate(int capacity) {
			return ByteBuffer.allocate(capacity);
		}
	};
	
	String frame(ByteBuffer data) {
		StringBuilder sb = new StringBuilder();
		int b = data.get() & 0xff; 
		
		sb.append((b & 0x80) != 0 ? "F" : "f");
		sb.append((b & 0x40) != 0 ? "R" : "r");
		sb.append((b & 0x20) != 0 ? "R" : "r");
		sb.append((b & 0x10) != 0 ? "R" : "r");
		sb.append(b & 0x0f);
		
		b = data.get() & 0xff;
		boolean masked = (b & 0x80) != 0;
		sb.append(masked ? "M" : "m");
		sb.append("|");
		b &= 0x7f;
		sb.append(b);
		sb.append("|");
		
		long len = b;
		
		if (len == 126) {
			len = data.getShort() & 0xffff;
			sb.append(len);
			sb.append("(2)");
		}
		else if (len == 127) {
			len = data.getLong();
			sb.append(len);
			sb.append("(8)");
		}
		
		byte[] mask = new byte[4];
		if (masked) {
			data.get(mask);
			sb.append("M(4)");
		}
		
		if (len == data.remaining()) {
			sb.append("=");
		}
		
		if (data.remaining() > 0) {
			byte[] bytes = new byte[data.remaining()];
			data.get(bytes);
			
			if (masked) {
				for (int i=0; i<bytes.length; ++i) {
					bytes[i] ^= mask[i % 4];
				}
			}
			
			String s = new String(bytes);
			if (s.length() > 20) {
				sb.append(s.substring(0, 10));
				sb.append("...");
				sb.append(s.substring(s.length()-10));
			}
			else {
				sb.append(s);
			}
		}
		return sb.toString();
	}
	
	ByteBuffer out() {
		ByteBuffer bb = out.get(0);
		out.clear();
		return bb;
	}
	
	@Before
	public void before() {
		out.clear();
	}
	
	byte[] bytes(int length, char fill) {
		byte[] bytes = new byte[length];
		
		Arrays.fill(bytes, (byte)fill);
		bytes[0] = (byte)(fill+1);
		bytes[bytes.length-1] = (byte)(fill+2);
		return bytes;
	}
	
	@Test
	public void testEncode() throws Exception {
		FrameEncoder enc = new FrameEncoder(true);	
		enc.encode(session, new BinaryFrame(true, 0, new byte[0]), out);
		assertEquals("Frrr2M|0|M(4)=", frame(out()));
		enc.encode(session, new BinaryFrame(true, 0, "A".getBytes()), out);
		assertEquals("Frrr2M|1|M(4)=A", frame(out()));
		enc.encode(session, new BinaryFrame(true, 0, "ABCDEFGH".getBytes()), out);
		assertEquals("Frrr2M|8|M(4)=ABCDEFGH", frame(out()));
		enc.encode(session, new BinaryFrame(true, 7, bytes(125,'D')), out);
		assertEquals("FRRR2M|125|M(4)=EDDDDDDDDD...DDDDDDDDDF", frame(out()));
		enc.encode(session, new BinaryFrame(true, 4, bytes(126,'E')), out);
		assertEquals("FRrr2M|126|126(2)M(4)=FEEEEEEEEE...EEEEEEEEEG", frame(out()));
		enc.encode(session, new BinaryFrame(true, 2, bytes(127,'E')), out);
		assertEquals("FrRr2M|126|127(2)M(4)=FEEEEEEEEE...EEEEEEEEEG", frame(out()));
		enc.encode(session, new BinaryFrame(true, 0, bytes(0xfffe,'C')), out);
		assertEquals("Frrr2M|126|65534(2)M(4)=DCCCCCCCCC...CCCCCCCCCE", frame(out()));
		enc.encode(session, new BinaryFrame(true, 0, bytes(0xffff,'C')), out);
		assertEquals("Frrr2M|126|65535(2)M(4)=DCCCCCCCCC...CCCCCCCCCE", frame(out()));
		enc.encode(session, new BinaryFrame(true, 0, bytes(0xffff+1,'D')), out);
		assertEquals("Frrr2M|127|65536(8)M(4)=EDDDDDDDDD...DDDDDDDDDF", frame(out()));
		enc.encode(session, new BinaryFrame(false, 1, bytes(100000,'D')), out);
		assertEquals("frrR2M|127|100000(8)M(4)=EDDDDDDDDD...DDDDDDDDDF", frame(out()));

		
		enc = new FrameEncoder(false);
		enc.encode(session, new BinaryFrame(true, 0, new byte[0]), out);
		assertEquals("Frrr2m|0|=", frame(out()));
		enc.encode(session, new BinaryFrame(true, 0, "A".getBytes()), out);
		assertEquals("Frrr2m|1|=A", frame(out()));
		enc.encode(session, new BinaryFrame(true, 0, "ABCDEFGH".getBytes()), out);
		assertEquals("Frrr2m|8|=ABCDEFGH", frame(out()));
		enc.encode(session, new BinaryFrame(true, 7, bytes(125,'D')), out);
		assertEquals("FRRR2m|125|=EDDDDDDDDD...DDDDDDDDDF", frame(out()));
		enc.encode(session, new BinaryFrame(true, 4, bytes(126,'E')), out);
		assertEquals("FRrr2m|126|126(2)=FEEEEEEEEE...EEEEEEEEEG", frame(out()));
		enc.encode(session, new BinaryFrame(true, 2, bytes(127,'E')), out);
		assertEquals("FrRr2m|126|127(2)=FEEEEEEEEE...EEEEEEEEEG", frame(out()));
		enc.encode(session, new BinaryFrame(true, 0, bytes(0xfffe,'C')), out);
		assertEquals("Frrr2m|126|65534(2)=DCCCCCCCCC...CCCCCCCCCE", frame(out()));
		enc.encode(session, new BinaryFrame(true, 0, bytes(0xffff,'C')), out);
		assertEquals("Frrr2m|126|65535(2)=DCCCCCCCCC...CCCCCCCCCE", frame(out()));
		enc.encode(session, new BinaryFrame(true, 0, bytes(0xffff+1,'D')), out);
		assertEquals("Frrr2m|127|65536(8)=EDDDDDDDDD...DDDDDDDDDF", frame(out()));
		enc.encode(session, new BinaryFrame(false, 1, bytes(100000,'D')), out);
		assertEquals("frrR2m|127|100000(8)=EDDDDDDDDD...DDDDDDDDDF", frame(out()));
	}
	
	@Test
	public void testMasking() throws Exception {
		byte[] payload = "ABCDEFGHIJ".getBytes();
		FrameEncoder enc = new FrameEncoder(true);	
		enc.encode(session, new BinaryFrame(true, 0, payload), out);
		ByteBuffer bb = out.get(0);
		byte[] b = new byte[bb.remaining()];
		bb.get(b);
		assertEquals(16, b.length);
		byte[] masked = new byte[10];
		byte[] mask = new byte[4];
		System.arraycopy(b, 2, mask, 0, 4);
		System.arraycopy(b, 2+4, masked, 0, 10);
		for (int i=0; i<10; ++i) {
			assertEquals("byte " + (i+1), masked[i], payload[i] ^ mask[i % 4]);
		}
		out.clear();
		enc.encode(session, new BinaryFrame(true, 0, payload), out);
		bb = out.get(0);
		b = new byte[bb.remaining()];
		bb.get(b);
		assertEquals(16, b.length);
		byte[] mask2 = new byte[4];
		System.arraycopy(b, 2, mask2, 0, 4);
		boolean equals = true;
		for (int i=0; i<4; ++i) {
			if (mask2[i] != mask[1]) {
				equals = false;
				break;
			}
		}
		assertFalse(equals);
		
		enc = new FrameEncoder(false);	
		out.clear();
		enc.encode(session, new BinaryFrame(true, 0, payload), out);
		bb = out.get(0);
		b = new byte[bb.remaining()];
		bb.get(b);
		assertEquals(12, b.length);
		System.arraycopy(b, 2, masked, 0, 10);
		assertArrayEquals(masked, payload);
	}
}
