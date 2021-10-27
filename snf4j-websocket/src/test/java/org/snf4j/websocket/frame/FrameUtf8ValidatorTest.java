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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.snf4j.websocket.TestWSSession;
import org.snf4j.websocket.frame.Utf8.ValidationContext;

public class FrameUtf8ValidatorTest {

	byte[] NON_UTF8 = new byte[] {(byte) 0xdf, (byte) 0xdf};
	byte[] UTF8 = new byte[] {(byte) 0xdf, (byte) 0xbf};
	byte[] INCOMPLETE_UTF8 = new byte[] {UTF8[0]};
	byte[] TAIL_UTF8 = new byte[] {UTF8[1]};
	
	void assertDecode(FrameUtf8Validator v, Frame frame) throws Exception {
		assertDecode(v,frame, false);
	}
	
	Throwable assertDecode(FrameUtf8Validator v, Frame frame, boolean returnException) throws Exception {
		List<Frame> out = new ArrayList<Frame>();
		TestWSSession s = new TestWSSession();
		
		try {
			v.decode(s, frame, out);
		}
		catch (Exception e) {
			if (returnException) {
				return e;
			}
			throw e;
		}
		assertEquals(1, out.size());
		assertTrue(frame == out.get(0));
		return null;
	}
	
	@Test
	public void testMisc() {
		assertFalse(Utf8.validate(new Utf8.ValidationContext(), NON_UTF8, 0, NON_UTF8.length));
		assertTrue(Utf8.validate(new Utf8.ValidationContext(), UTF8, 0, UTF8.length));
		ValidationContext ctx = new Utf8.ValidationContext();
		assertTrue(Utf8.validate(ctx, INCOMPLETE_UTF8, 0, INCOMPLETE_UTF8.length));
		assertFalse(Utf8.isValid(ctx));
		
		FrameUtf8Validator v = new FrameUtf8Validator();
		assertTrue(Frame.class == v.getInboundType());
		assertTrue(Frame.class == v.getOutboundType());
	}
	
	@Test
	public void testDecode() throws Exception {
		FrameUtf8Validator v = new FrameUtf8Validator();
	
		assertDecode(v, new ContinuationFrame(true, 0, NON_UTF8));
		assertDecode(v, new BinaryFrame(true, 0, NON_UTF8));
		assertDecode(v, new CloseFrame(0, NON_UTF8));
		assertDecode(v, new PingFrame(0, NON_UTF8));
		assertDecode(v, new PongFrame(0, NON_UTF8));
		
		assertDecode(v, new TextFrame(true, 0, UTF8));
		assertDecode(v, new ContinuationFrame(true, 0, NON_UTF8));
		
		v = new FrameUtf8Validator();
		assertEquals("Invalid text frame payload: bytes are not UTF-8",
				assertDecode(v, new TextFrame(true, 0, NON_UTF8), true).getMessage());
		v = new FrameUtf8Validator();
		assertEquals("Invalid text frame payload: bytes are not UTF-8",
				assertDecode(v, new TextFrame(true, 0, INCOMPLETE_UTF8), true).getMessage());
		
		v = new FrameUtf8Validator();
		assertDecode(v, new ContinuationFrame(false, 0, NON_UTF8));
		assertDecode(v, new BinaryFrame(false, 0, NON_UTF8));
		
		assertDecode(v, new TextFrame(false, 0, INCOMPLETE_UTF8));
		assertDecode(v, new BinaryFrame(true, 0, NON_UTF8));
		assertDecode(v, new CloseFrame(0, NON_UTF8));
		assertDecode(v, new PingFrame(0, NON_UTF8));
		assertDecode(v, new PongFrame(0, NON_UTF8));
		assertDecode(v, new ContinuationFrame(true, 0, TAIL_UTF8));
		assertDecode(v, new ContinuationFrame(true, 0, NON_UTF8));
		
		assertDecode(v, new TextFrame(false, 0, INCOMPLETE_UTF8));
		assertDecode(v, new ContinuationFrame(false, 0, TAIL_UTF8));
		assertDecode(v, new BinaryFrame(true, 0, NON_UTF8));
		assertDecode(v, new CloseFrame(0, NON_UTF8));
		assertDecode(v, new PingFrame(0, NON_UTF8));
		assertDecode(v, new PongFrame(0, NON_UTF8));
		assertDecode(v, new ContinuationFrame(false, 0, INCOMPLETE_UTF8));
		assertDecode(v, new ContinuationFrame(false, 0, new byte[0]));
		assertDecode(v, new ContinuationFrame(true, 0, TAIL_UTF8));
		assertDecode(v, new ContinuationFrame(true, 0, NON_UTF8));
		
		assertDecode(v, new TextFrame(false, 0, INCOMPLETE_UTF8));
		assertEquals("Invalid text frame payload: bytes are not UTF-8",
				assertDecode(v, new ContinuationFrame(true, 0, new byte[0]), true).getMessage());
		
		v = new FrameUtf8Validator();
		assertDecode(v, new TextFrame(false, 0, INCOMPLETE_UTF8));
		assertDecode(v, new ContinuationFrame(false, 0, new byte[0]));
		assertEquals("Invalid text frame payload: bytes are not UTF-8",
				assertDecode(v, new ContinuationFrame(false, 0, UTF8), true).getMessage());
	}
}
