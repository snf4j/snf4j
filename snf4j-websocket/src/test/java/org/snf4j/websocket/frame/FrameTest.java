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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class FrameTest {
	
	static byte[] UTF8_BYTES = new byte[] {(byte) 0xc2, (byte) 0xA9, (byte) 0xc2, (byte) 0xA3};
	static String UTF8_TEXT = "\u00A9\u00A3";
	static byte[] CLOSE_UTF8_BYTES = new byte[] {3, (byte)0xe8, (byte) 0xc2, (byte) 0xA9, (byte) 0xc2, (byte) 0xA3};
	
	void assertRsvBits(Frame f, boolean b1, boolean b2, boolean b3) {
		assertEquals(b1, f.isRsvBit1());
		assertEquals(b2, f.isRsvBit2());
		assertEquals(b3, f.isRsvBit3());
	}
	
	@Test
	public void testFrame() {
		Frame f = new TestFrame(Opcode.BINARY, true, 0, null);
		assertTrue(Opcode.BINARY == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertEquals(0, f.getPayload().length);
		assertEquals(0, f.getPayloadLength());
		assertRsvBits(f, false, false, false);
		
		byte[] bytes = new byte[10];
		f = new TestFrame(Opcode.CONTINUATION, false, 7, bytes);
		assertTrue(Opcode.CONTINUATION == f.getOpcode());
		assertTrue(bytes == f.getPayload());
		assertEquals(10, f.getPayloadLength());
		assertFalse(f.isFinalFragment());
		
		f = new TestFrame(Opcode.BINARY, true, 1, null);
		assertRsvBits(f, false, false, true);
		f = new TestFrame(Opcode.BINARY, true, 2, null);
		assertRsvBits(f, false, true, false);
		f = new TestFrame(Opcode.BINARY, true, 4, null);
		assertRsvBits(f, true, false, false);
	}
	
	@Test
	public void testBinaryFrame() {
		Frame f = new BinaryFrame(true, 4, null);
		assertTrue(Opcode.BINARY == f.getOpcode());
		assertEquals(4, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertEquals(0, f.getPayload().length);
		assertEquals(0, f.getPayloadLength());
		
		f = new BinaryFrame(null);
		assertTrue(Opcode.BINARY == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertEquals(0, f.getPayload().length);
		assertEquals(0, f.getPayloadLength());
		
		byte[] bytes = new byte[10];
		f = new BinaryFrame(false, 3, bytes);
		assertTrue(Opcode.BINARY == f.getOpcode());
		assertEquals(3, f.getRsvBits());
		assertFalse(f.isFinalFragment());
		assertTrue(bytes == f.getPayload());
		assertEquals(10, f.getPayloadLength());

		f = new BinaryFrame(bytes);
		assertTrue(Opcode.BINARY == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertTrue(bytes == f.getPayload());
		assertEquals(10, f.getPayloadLength());

		f = new BinaryFrame();
		assertTrue(Opcode.BINARY == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertArrayEquals(new byte[0], f.getPayload());		
	}
	
	@Test
	public void testTextFrame() {
		TextFrame f = new TextFrame(false, 4, UTF8_BYTES);
		assertTrue(Opcode.TEXT == f.getOpcode());
		assertEquals(4, f.getRsvBits());
		assertFalse(f.isFinalFragment());
		assertEquals(UTF8_TEXT, f.getText());
		assertTrue(UTF8_BYTES == f.getPayload());
		
		f = new TextFrame(true, 6, UTF8_TEXT);
		assertTrue(Opcode.TEXT == f.getOpcode());
		assertEquals(6, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertEquals(UTF8_TEXT, f.getText());
		assertArrayEquals(UTF8_BYTES, f.getPayload());
		
		f = new TextFrame(UTF8_BYTES);
		assertTrue(Opcode.TEXT == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertEquals(UTF8_TEXT, f.getText());
		assertTrue(UTF8_BYTES == f.getPayload());
		
		f = new TextFrame(UTF8_TEXT);
		assertTrue(Opcode.TEXT == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertEquals(UTF8_TEXT, f.getText());
		assertArrayEquals(UTF8_BYTES, f.getPayload());		
		
		f = new TextFrame();
		assertTrue(Opcode.TEXT == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertEquals("", f.getText());
		assertArrayEquals(new byte[0], f.getPayload());		
		
		f = new TextFrame((String)null);
		assertTrue(Opcode.TEXT == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertEquals("", f.getText());
		assertArrayEquals(new byte[0], f.getPayload());		

		f = new TextFrame("");
		assertTrue(Opcode.TEXT == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertEquals("", f.getText());
		assertArrayEquals(new byte[0], f.getPayload());		
	}
	
	@Test
	public void testContinuationFrame() {
		ContinuationFrame f = new ContinuationFrame(false, 3, UTF8_BYTES);
		assertTrue(Opcode.CONTINUATION == f.getOpcode());
		assertEquals(3, f.getRsvBits());
		assertFalse(f.isFinalFragment());
		assertTrue(UTF8_BYTES == f.getPayload());
		assertEquals(UTF8_TEXT, f.getText());

		f = new ContinuationFrame(true, 7, UTF8_TEXT);
		assertTrue(Opcode.CONTINUATION == f.getOpcode());
		assertEquals(7, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertArrayEquals(UTF8_BYTES, f.getPayload());
		assertEquals(UTF8_TEXT, f.getText());
		
		f = new ContinuationFrame(UTF8_BYTES);
		assertTrue(Opcode.CONTINUATION == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertTrue(UTF8_BYTES == f.getPayload());
		assertEquals(UTF8_TEXT, f.getText());
		
		f = new ContinuationFrame(UTF8_TEXT);
		assertTrue(Opcode.CONTINUATION == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertArrayEquals(UTF8_BYTES, f.getPayload());
		assertEquals(UTF8_TEXT, f.getText());
		
		f = new ContinuationFrame();
		assertTrue(Opcode.CONTINUATION == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertArrayEquals(new byte[0], f.getPayload());
	}
	
	@Test
	public void testPingFrame() {
		Frame f = new PingFrame(1, UTF8_BYTES);
		assertTrue(Opcode.PING == f.getOpcode());
		assertEquals(1, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertTrue(UTF8_BYTES == f.getPayload());
	
		f = new PingFrame(UTF8_BYTES);
		assertTrue(Opcode.PING == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertTrue(UTF8_BYTES == f.getPayload());

		f = new PingFrame(null);
		assertTrue(Opcode.PING == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertEquals(0, f.getPayload().length);
		assertEquals(0, f.getPayloadLength());
		
		f = new PingFrame();
		assertTrue(Opcode.PING == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertEquals(0, f.getPayload().length);
		assertEquals(0, f.getPayloadLength());

		new PingFrame(1, new byte[125]);
		new PingFrame(new byte[125]);
		try {
			new PingFrame(1, new byte[126]);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		try {
			new PingFrame(new byte[126]);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testPongFrame() {
		Frame f = new PongFrame(1, UTF8_BYTES);
		assertTrue(Opcode.PONG == f.getOpcode());
		assertEquals(1, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertTrue(UTF8_BYTES == f.getPayload());
	
		f = new PongFrame(UTF8_BYTES);
		assertTrue(Opcode.PONG == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertTrue(UTF8_BYTES == f.getPayload());

		f = new PongFrame(null);
		assertTrue(Opcode.PONG == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertEquals(0, f.getPayload().length);
		assertEquals(0, f.getPayloadLength());
		
		f = new PongFrame();
		assertTrue(Opcode.PONG == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertEquals(0, f.getPayload().length);
		assertEquals(0, f.getPayloadLength());
		
		new PongFrame(1, new byte[125]);
		new PongFrame(new byte[125]);
		try {
			new PongFrame(1, new byte[126]);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		try {
			new PongFrame(new byte[126]);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testCloseStatusCodes() {
		assertEquals(1000, CloseFrame.NORMAL);
		assertEquals(1001, CloseFrame.GOING_AWAY);
		assertEquals(1002, CloseFrame.PROTOCOL_ERROR);
		assertEquals(1003, CloseFrame.NOT_ACCEPTED);
		assertEquals(1005, CloseFrame.NO_CODE);
		assertEquals(1006, CloseFrame.ABNORMAL);
		assertEquals(1007, CloseFrame.NON_UTF8);
		assertEquals(1008, CloseFrame.POLICY_VALIDATION);
		assertEquals(1009, CloseFrame.TOO_BIG);
		assertEquals(1010, CloseFrame.MISSED_EXTENSION);
		assertEquals(1011, CloseFrame.UNEXPECTED_CONDITION);
		assertEquals(1012, CloseFrame.SERVICE_RESTART);
		assertEquals(1013, CloseFrame.SERVICE_OVERLOAD);
		assertEquals(1014, CloseFrame.BAD_GATEWAY);
		assertEquals(1015, CloseFrame.TLS_FAILURE);
	}
	
	@Test
	public void testCloseFrame() {
		CloseFrame f = new CloseFrame(4, CLOSE_UTF8_BYTES);
		assertTrue(Opcode.CLOSE == f.getOpcode());
		assertEquals(4, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertTrue(CLOSE_UTF8_BYTES == f.getPayload());
		assertEquals(1000, f.getStatus());
		assertEquals(UTF8_TEXT, f.getReason());
		
		f = new CloseFrame(CLOSE_UTF8_BYTES);
		assertTrue(Opcode.CLOSE == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertTrue(CLOSE_UTF8_BYTES == f.getPayload());
		assertEquals(1000, f.getStatus());
		assertEquals(UTF8_TEXT, f.getReason());		
		
		f = new CloseFrame();
		assertTrue(Opcode.CLOSE == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertEquals(0, f.getPayload().length);
		assertEquals(-1, f.getStatus());
		assertEquals("", f.getReason());		

		f = new CloseFrame(1, 1000);
		assertTrue(Opcode.CLOSE == f.getOpcode());
		assertEquals(1, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertEquals(2, f.getPayload().length);
		assertEquals(3, f.getPayload()[0]);
		assertEquals((byte)0xe8, f.getPayload()[1]);
		assertEquals(1000, f.getStatus());
		assertEquals("", f.getReason());		

		f = new CloseFrame(1001);
		assertTrue(Opcode.CLOSE == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertEquals(2, f.getPayload().length);
		assertEquals(3, f.getPayload()[0]);
		assertEquals((byte)0xe9, f.getPayload()[1]);
		assertEquals(1001, f.getStatus());
		assertEquals("", f.getReason());		

		f = new CloseFrame(7, 1000, UTF8_TEXT);
		assertTrue(Opcode.CLOSE == f.getOpcode());
		assertEquals(7, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertEquals(6, f.getPayload().length);
		assertEquals(1000, f.getStatus());
		assertEquals(UTF8_TEXT, f.getReason());		
		assertArrayEquals(CLOSE_UTF8_BYTES, f.getPayload());

		f = new CloseFrame(1000, UTF8_TEXT);
		assertTrue(Opcode.CLOSE == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertEquals(6, f.getPayload().length);
		assertEquals(1000, f.getStatus());
		assertEquals(UTF8_TEXT, f.getReason());		
		assertArrayEquals(CLOSE_UTF8_BYTES, f.getPayload());
		
		f = new CloseFrame(1000, "");
		assertTrue(Opcode.CLOSE == f.getOpcode());
		assertEquals(0, f.getRsvBits());
		assertTrue(f.isFinalFragment());
		assertEquals(2, f.getPayload().length);
		assertEquals(3, f.getPayload()[0]);
		assertEquals((byte)0xe8, f.getPayload()[1]);
		assertEquals(1000, f.getStatus());
		assertEquals("", f.getReason());		
		
		new CloseFrame(1000, new String(new byte[123]));
		try {
			new CloseFrame(1000, new String(new byte[124]));
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		try {
			new CloseFrame(1000, new byte[1]);
			fail();
		}
		catch (IllegalArgumentException e) {
		}

	}
	
	static class TestFrame extends Frame {

		TestFrame(Opcode opcode, boolean finalFragment, int rsvBits, byte[] payload) {
			super(opcode, finalFragment, rsvBits, payload);
		}
		
	}
}
