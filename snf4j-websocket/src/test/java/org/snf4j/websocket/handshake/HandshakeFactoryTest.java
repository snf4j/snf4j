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
package org.snf4j.websocket.handshake;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.junit.Test;

public class HandshakeFactoryTest extends HandshakeTest {

	HandshakeFactory factory = HandshakeFactory.getDefault();
	
	void assertThrows(String data, boolean request) {
		int[] lines = new int[100];
		
		bytes(data);
		assertEquals(bytes().length, factory.available(bytes(), 0, bytes().length, lines));
		try {
			factory.parse(bytes(), lines, request);
			fail();
		}
		catch (InvalidHandshakeException e) {
		}
	}
	
	void assertParse(String data, String uri) throws InvalidHandshakeException {
		int[] lines = new int[100];

		bytes(data);
		assertEquals(bytes().length, factory.available(bytes(), 0, bytes().length, lines));
		HandshakeFrame f = factory.parse(bytes(), lines, true);
		assertTrue(f.getClass() == HandshakeRequest.class);
		assertEquals(uri,((HandshakeRequest) f).getUri());
	}

	void assertParse(String data, int status, String reason) throws InvalidHandshakeException {
		int[] lines = new int[100];

		bytes(data);
		assertEquals(bytes().length, factory.available(bytes(), 0, bytes().length, lines));
		HandshakeFrame f = factory.parse(bytes(), lines, false);
		assertTrue(f.getClass() == HandshakeResponse.class);
		assertEquals(status,((HandshakeResponse) f).getStatus());
		assertEquals(reason,((HandshakeResponse) f).getReason());
	}

	@Test
	public void testParse() throws Exception {
		assertParse("GET /chat HTTP/1.1|xxx: yyy||", "/chat");
		assertParse("GET   /chat2    HTTP/1.1|xxx: yyy||", "/chat2");
		
		assertThrows("GET /chat HTTP/1.1|xxx: yyy||", false);
		assertThrows(" GET /chat HTTP/1.1|xxx: yyy||", true);
		assertThrows("GET /chat HTTP/1.1 |xxx: yyy||", false);
		assertThrows("gET /chat HTTP/1.1|xxx: yyy||", true);
		assertThrows("GET /chat HTTP/1.0|xxx: yyy||", true);
		assertThrows("GET /chat|xxx: yyy||", false);
		assertThrows("GET /chat |xxx: yyy||", false);
		assertThrows("GET|xxx: yyy||", false);
		assertThrows("GET |xxx: yyy||", false);
		assertThrows("|xxx: yyy||", false);
		
		assertParse("HTTP/1.1 101 Switching Protocols|x: y||", 101, "Switching Protocols");
		assertParse("HTTP/1.1   102   Switching Protocols |x: y||", 102, "Switching Protocols ");

		assertThrows(" HTTP/1.1 A01 Switching Protocols|x: y||", false);
		assertThrows("HTTP/1.1 A01 Switching Protocols|x: y||", false);
		assertThrows("HTTP/1.1 0A1 Switching Protocols|x: y||", false);
		assertThrows("HTTP/1.1 10A Switching Protocols|x: y||", false);
		assertThrows("HTTP/1.1 1010 Switching Protocols|x: y||", false);
		assertThrows("HTTP/1.0 10A Switching Protocols|x: y||", false);
	}
	
	void assertThrows(String data, String message) {
		HandshakeRequest frame = new HandshakeRequest("/chat");
		byte[] b = bytes(data);
		int[] l = new int[100];
		
		assertTrue(HttpUtils.available(b, 0, b.length, l) > 0);
		try {
			factory.parseFields(frame, b, l, 0);
			fail();
		} catch (InvalidHandshakeException e) {
			assertEquals(message, e.getMessage());
		}
	}
	
	@Test
	public void testParseFields() throws Exception {
		HandshakeRequest frame = new HandshakeRequest("/chat");
		int[] l = new int[100];
		
		byte[] b = bytes("name1: value1|n2:val| ue2|na|\t \tme3:v3||");
		assertTrue(HttpUtils.available(b, 0, b.length, l) > 0);
		factory.parseFields(frame, b, l, 0);
		assertFields(frame, "name1:value1;n2:val ue2;name3:v3;");
		
		frame = new HandshakeRequest("/chat");
		b = bytes("very| big| name1: value1||");
		assertTrue(HttpUtils.available(b, 0, b.length, l) > 0);
		factory.parseFields(frame, b, l, 0);
		assertFields(frame, "verybigname1:value1;");
		
		frame = new HandshakeRequest("/chat");
		b = bytes("n1:v1| n2:  v2||");
		assertTrue(HttpUtils.available(b, 0, b.length, l) > 0);
		factory.parseFields(frame, b, l, 0);
		assertFields(frame, "n1:v1 n2:  v2;");
		
		assertThrows(" n1:v1||", "No header field to extend");
		assertThrows("n1|n2:v2||", "No value in header field");
		assertThrows("n1|n2||", "No value in header field");
		
		l = new int[5];
		frame = new HandshakeRequest("/chat");
		b = bytes("n1:v1|n2:v2||");
		assertEquals(16, HttpUtils.available(b, 0, b.length, l));
		assertEquals(-1, l[4]);
		factory.parseFields(frame, b, l, 0);
		assertFields(frame, "n1:v1;n2:v2;");
		
		frame = new HandshakeRequest("/chat");
		l[4] = 0;
		factory.parseFields(frame, b, l, 0);
		assertFields(frame, "n1:v1;n2:v2;");
		
		frame = new HandshakeRequest("/chat");
		l[4] = 0;
		factory.parseFields(frame, b, l, 3);
		assertFields(frame, "");
		l = new int[0];
		factory.parseFields(frame, b, l, 0);
		assertFields(frame, "");
		
		l = new int[100];
		b = bytes("name1: 5|name2: text|name1: 6, 7||");
		assertTrue(HttpUtils.available(b, 0, b.length, l) > 0);
		factory.parseFields(frame, b, l, 0);
		assertFields(frame, "name1:5, 6, 7;name2:text;");
		
		frame = new HandshakeRequest("/chat");
		b = bytes("name1: \t5 |na| me2: 6\t|name3: xxx | yyy \t|name4: zzz |  aa:77 ||");
		assertTrue(HttpUtils.available(b, 0, b.length, l) > 0);
		factory.parseFields(frame, b, l, 0);
		assertFields(frame, "name1:5;name2:6;name3:xxx yyy;name4:zzz aa:77;");
	}
	
	@Test
	public void testAvailable() {
		byte[] b = bytes("GET /xxx HTTP/1.1|name1:value1||");
		int[] l0 = new int[5];
		int[] l1 = new int[5];
		ByteBuffer bb = ByteBuffer.allocate(100);
		
		assertEquals(b.length, factory.available(b, 0, b.length, l0));
		assertEquals(-1, l0[4]);
		assertEquals(b.length, factory.available(ByteBuffer.wrap(b), true, l1));
		assertArrayEquals(l0, l1);
		bb.clear();
		bb.put(b);
		assertEquals(b.length, factory.available(bb, false, l1));
		assertArrayEquals(l0, l1);
	}
	
	@Test
	public void testFormat() throws Exception {
		HandshakeFrame f = new HandshakeRequest("/uri");
		ByteBuffer bb = ByteBuffer.allocate(100);
		
		f.addValue("name1", "value1");
		factory.format(f, bb, true);
		bb.flip();
		byte[] b = new byte[bb.remaining()];
		bb.get(b);
		assertArrayEquals(bytes("GET /uri HTTP/1.1|name1: value1||"), b);
		try {
			factory.format(f, bb, false);
			fail();
		}
		catch (InvalidHandshakeException e) {
		}
		
		f = new HandshakeResponse(404, "Not Found");
		bb.clear();
		factory.format(f, bb, false);
		bb.flip();
		b = new byte[bb.remaining()];
		bb.get(b);
		assertArrayEquals(bytes("HTTP/1.1 404 Not Found||"), b);
		try {
			factory.format(f, bb, true);
			fail();
		}
		catch (InvalidHandshakeException e) {
		}
	}
}
