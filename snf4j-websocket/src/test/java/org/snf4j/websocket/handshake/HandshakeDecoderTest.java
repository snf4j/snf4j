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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.snf4j.websocket.TestWSSession;

public class HandshakeDecoderTest extends HandshakeTest {

	void assertHandshakeRequest(TestWSSession s, List<HandshakeFrame> out) {
		assertEquals(0,  s.msgs().size());
		assertFalse(s.isClosed());
		assertEquals(1, out.size());
		assertTrue(out.get(0) instanceof HandshakeRequest);
		HandshakeRequest frame = (HandshakeRequest) out.get(0);
		assertEquals("/uri", frame.getUri());
		assertEquals("snf4j.org", frame.getValue("Host"));
		assertEquals(1, frame.getNames().size());
		out.clear();
		s.clear();
	}
	
	@Test
	public void testConstructor() {
		try {
			new HandshakeDecoder(false, 1000, 0);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testTypes() {
		HandshakeDecoder d = new HandshakeDecoder(false);
		
		assertTrue(d.getInboundType() == byte[].class);
		assertTrue(d.getOutboundType() == HandshakeFrame.class);
	}
	
	@Test
	public void testDecode() throws Exception {
		HandshakeDecoder d = new HandshakeDecoder(false);
		TestWSSession s = new TestWSSession();
		List<HandshakeFrame> out = new ArrayList<HandshakeFrame>();
		
		byte[] b = bytes("GET /uri HTTP/1.1|Host: snf4j.org||");
		int len = d.available(s, b, 0, b.length);
		assertTrue(len > 0);
		d.decode(s, Arrays.copyOf(b, len), out);
		assertHandshakeRequest(s, out);
		len = d.available(s, ByteBuffer.wrap(b), true);
		assertTrue(len > 0);
		d.decode(s, Arrays.copyOf(b, len), out);
		assertHandshakeRequest(s, out);
		ByteBuffer bb = ByteBuffer.allocate(1000);
		bb.put(b);
		len = d.available(s, bb, false);
		assertTrue(len > 0);
		d.decode(s, Arrays.copyOf(b, len), out);
		assertHandshakeRequest(s, out);
		
		byte[] b2 = Arrays.copyOf(b, 4);
		assertEquals(0, d.available(s, b2, 0, b2.length));
		b2 = Arrays.copyOf(b, 23);
		len = d.available(s, b2, 0, b2.length);
		assertEquals(19, len);
		d.decode(s, Arrays.copyOf(b2, len), out);
		assertEquals(0, out.size());
		assertEquals(0,  s.msgs().size());
		assertFalse(s.isClosed());
		b2 = Arrays.copyOfRange(b, len, b.length);
		len = d.available(s, b2, 0, b2.length);
		assertEquals(19, len);
		d.decode(s, Arrays.copyOf(b2, len), out);
		assertHandshakeRequest(s, out);
		
		b = bytes("HTTP/1.1 400 Bad Request||");
		d = new HandshakeDecoder(true);
		len = d.available(s, b, 0, b.length);
		assertEquals(len, b.length);
		d.decode(s, b, out);
		assertEquals(1, out.size());
		HandshakeResponse r = (HandshakeResponse) out.get(0);
		assertEquals(400, r.getStatus());
	}
	
	@Test
	public void testDecodeTooBigFrame() throws Exception {
		byte[] b = bytes("GET /uri HTTP/1.1|Host: snf4j.org||");
		HandshakeDecoder d = new HandshakeDecoder(false, b.length);
		List<HandshakeFrame> out = new ArrayList<HandshakeFrame>();
		TestWSSession s = new TestWSSession();
		
		int len = d.available(s, b, 0, b.length);
		assertEquals(b.length, len);
		d.decode(s, b, out);
		assertHandshakeRequest(s, out);
		
		d = new HandshakeDecoder(false, b.length-1);
		len = d.available(s, b, 0, b.length);
		assertEquals(b.length, len);
		d.decode(s, b, out);
		assertTrue(s.isClosed());
		assertEquals(1, s.msgs().size());
		HandshakeResponse r = (HandshakeResponse) s.msgs().get(0);
		assertEquals(HttpStatus.REQUEST_ENTITY_TOO_LARGE.getStatus(), r.getStatus());	
	}
	
	@Test
	public void testDecodeFailures() throws Exception {
		byte[] b = bytes("POST /uri HTTP/1.1|Host: snf4j.org||");
		HandshakeDecoder d = new HandshakeDecoder(false);
		List<HandshakeFrame> out = new ArrayList<HandshakeFrame>();
		TestWSSession s = new TestWSSession();

		int len = d.available(s, b, 0, b.length);
		assertEquals(b.length, len);
		d.decode(s, b, out);
		assertEquals(0, out.size());
		assertTrue(s.isClosed());
		assertEquals(1, s.msgs().size());
		HandshakeResponse r = (HandshakeResponse) s.msgs().get(0);
		assertEquals(HttpStatus.FORBIDDEN.getStatus(), r.getStatus());
		s.clear();
		
		b = bytes("GET /uri HTTP/1.2|Host: snf4j.org||");
		len = d.available(s, b, 0, b.length);
		assertEquals(b.length, len);
		d.decode(s, b, out);
		assertEquals(0, out.size());
		assertTrue(s.isClosed());
		assertEquals(1, s.msgs().size());
		r = (HandshakeResponse) s.msgs().get(0);
		assertEquals(HttpStatus.BAD_REQUEST.getStatus(), r.getStatus());
		s.clear();
		
		d = new HandshakeDecoder(true);
		b = bytes("HTTP/1.3 400 Bad Request||");
		len = d.available(s, b, 0, b.length);
		assertEquals(b.length, len);
		try {
			d.decode(s, b, out);
			fail();
		}
		catch (InvalidHandshakeException e) {
		}
		
	}

	@Test
	public void testAvailable() throws Exception {
		HandshakeDecoder d = new HandshakeDecoder(false,1000,1);
		byte[] b = bytes("GET /uri HTTP/1.1|Host: snf4j.org||");
		List<HandshakeFrame> out = new ArrayList<HandshakeFrame>();
		TestWSSession s = new TestWSSession();
		
		int len = d.available(s, b, 0, b.length);
		assertEquals(19, len);
		d.decode(s, Arrays.copyOf(b, len), out);
		assertEquals(0, out.size());
	
		b = Arrays.copyOfRange(b, len, b.length);
		len = d.available(s, b, 0, b.length);
		assertEquals(19, len);
		d.decode(s, Arrays.copyOf(b, len), out);
		assertHandshakeRequest(s, out);
		
		d = new HandshakeDecoder(false,1000,2);
		b = bytes("GET /uri HTTP/1.1|Host: snf4j.org||");
		len = d.available(s, b, 0, b.length);
		assertEquals(b.length, len);
		Field f = HandshakeDecoder.class.getDeclaredField("lines");
		f.setAccessible(true);
		int[] lines = (int[]) f.get(d);
		assertEquals(5, lines.length);
		lines[4] = 0;
		Method m = HandshakeDecoder.class.getDeclaredMethod("available0", int.class);
		m.setAccessible(true);
		assertEquals(0, m.invoke(d, 0));
	}
	
	@Test
	public void testNoBaseDecoder() throws Exception {
		HandshakeDecoder d = new HandshakeDecoder(false,1000,2);
		byte[] b = bytes("GET /uri HTTP/1.1|Host: snf4j.org||");
		List<HandshakeFrame> out = new ArrayList<HandshakeFrame>();
		TestWSSession s = new TestWSSession();
		
		d.decode(s, b, out);
		assertEquals(1, out.size());
		assertHandshakeRequest(s, out);
		
		out.clear();
		d = new HandshakeDecoder(false,1000,1);
		d.decode(s, b, out);
		assertEquals(1, out.size());
		assertHandshakeRequest(s, out);
		
		b = bytes("GET /uri HTTP/1.1|Host: snf4j.org||");
		out.clear();
		d = new HandshakeDecoder(false,b.length,2);
		d.decode(s, b, out);
		assertEquals(1, out.size());
		assertHandshakeRequest(s, out);
		
		d = new HandshakeDecoder(false,1000,2);
		try {
			d.decode(s, Arrays.copyOf(b, 6), out);
			fail();
		}
		catch (IllegalArgumentException e) {
		}

		d = new HandshakeDecoder(false,1000,2);
		try {
			d.decode(s, Arrays.copyOf(b, b.length-1), out);
			fail();
		}
		catch (IllegalArgumentException e) {
		}

		out.clear();
		d = new HandshakeDecoder(false,b.length,2);
		d.decode(s, b, out);
		assertEquals(1, out.size());
		assertHandshakeRequest(s, out);
	
		b = bytes("GET /uri HTTP/1.1|Host: snf4j.org|a");
		d = new HandshakeDecoder(false,b.length-1,2);
		try {
			d.decode(s, b, out);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		
		assertEquals(0, s.msgs().size());
		out.clear();
		d = new HandshakeDecoder(false,b.length-2,2);
		d.decode(s, b, out);
		assertEquals(0, out.size());
		assertEquals(1, s.msgs().size());
		HandshakeResponse r = (HandshakeResponse)s.msgs().get(0);
		assertEquals(HttpStatus.REQUEST_ENTITY_TOO_LARGE.getStatus(), r.getStatus());
	}
}
