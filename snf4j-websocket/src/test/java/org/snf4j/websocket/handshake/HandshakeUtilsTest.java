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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.junit.Test;

public class HandshakeUtilsTest {

	URI uri(String s) throws Exception {
		return new URI("ws://server.org" + s);
	}
	
	public static List<String> list(String[] items) {
		ArrayList<String> list = new ArrayList<String>();
		
		for (String item: items) {
			list.add(item);
		}
		return list;
	}
	
	public static List<String> parseExtension(String s) {
		return HandshakeUtils.extension(s);
	}
	
	public static String parseExtension(String[] s) {
		return HandshakeUtils.extension(list(s));
	}

	public static String parseExtension(List<String> s) {
		return HandshakeUtils.extension(s);
	}
	
	@Test
	public void testRequestUri() throws Exception {
		assertEquals("/", HandshakeUtils.requestUri(uri("")));
		assertEquals("/", HandshakeUtils.requestUri(new URI(null, null, null, null)));
		assertEquals("/", HandshakeUtils.requestUri(uri("/")));
		assertEquals("/abc", HandshakeUtils.requestUri(uri("/abc")));
		assertEquals("/ab%20c", HandshakeUtils.requestUri(uri("/ab%20c")));

		assertEquals("/", HandshakeUtils.requestUri(uri("?")));
		assertEquals("/", HandshakeUtils.requestUri(uri("/?")));
		assertEquals("/?abc", HandshakeUtils.requestUri(uri("?abc")));
		assertEquals("/?abc", HandshakeUtils.requestUri(uri("/?abc")));
		assertEquals("/?ab%20c", HandshakeUtils.requestUri(uri("/?ab%20c")));

		assertEquals("/xx%20x?ab%20c", HandshakeUtils.requestUri(uri("/xx%20x?ab%20c")));
	}
	
	URI host(String s) throws Exception {
		return new URI(s + "/chat");
	}
	
	@Test
	public void testHost() throws Exception {
		assertEquals("host", HandshakeUtils.host(host("ws://host")));

		assertEquals("host", HandshakeUtils.host(host("ws://host:80")));
		assertEquals("host", HandshakeUtils.host(host("WS://host:80")));
		assertEquals("host", HandshakeUtils.host(host("http://host:80")));
		assertEquals("host", HandshakeUtils.host(host("HTTP://host:80")));
		assertEquals("host:443", HandshakeUtils.host(host("ws://host:443")));
		assertEquals("host:443", HandshakeUtils.host(host("WS://host:443")));
		assertEquals("host:443", HandshakeUtils.host(host("http://host:443")));
		assertEquals("host:443", HandshakeUtils.host(host("HTTP://host:443")));

		assertEquals("host:80", HandshakeUtils.host(host("wss://host:80")));
		assertEquals("host:80", HandshakeUtils.host(host("WSS://host:80")));
		assertEquals("host:80", HandshakeUtils.host(host("https://host:80")));
		assertEquals("host:80", HandshakeUtils.host(host("HTTPS://host:80")));
		assertEquals("host", HandshakeUtils.host(host("wss://host:443")));
		assertEquals("host", HandshakeUtils.host(host("WSS://host:443")));
		assertEquals("host", HandshakeUtils.host(host("https://host:443")));
		assertEquals("host", HandshakeUtils.host(host("HTTPS://host:443")));
	}
	
	@Test
	public void testPort() throws Exception {
		assertEquals(80, HandshakeUtils.port(host("ws://host")));
		assertEquals(80, HandshakeUtils.port(host("http://host")));
		assertEquals(81, HandshakeUtils.port(host("http://host:81")));
		assertEquals(81, HandshakeUtils.port(host("ws://host:81")));
		
		assertEquals(443, HandshakeUtils.port(host("wss://host")));
		assertEquals(443, HandshakeUtils.port(host("https://host")));
		assertEquals(81, HandshakeUtils.port(host("https://host:81")));
		assertEquals(81, HandshakeUtils.port(host("wss://host:81")));
		
		assertEquals(-1, HandshakeUtils.port(host("wss2://host")));
	}
	
	@Test
	public void testGenerateKey() {
		String[] testVectors = new String[] {
				"","","f","Zg==","fo","Zm8=",
				"foo","Zm9v","foob","Zm9vYg==","fooba",
				"Zm9vYmE=","foobar","Zm9vYmFy"};
		
		for (int i=0; i<testVectors.length; i+=2) {
			assertEquals(testVectors[i+1], HandshakeUtils.generateKey(testVectors[i].getBytes()));
		}
		
		byte[] b = new byte[] {
				0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 
				0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10};
		String s = HandshakeUtils.generateKey(b);
		
		assertEquals(Base64.getEncoder().encodeToString(b), s);
		assertArrayEquals(b, Base64.getDecoder().decode(s));
		assertArrayEquals(b, Base64.getDecoder().decode("AQIDBAUGBwgJCgsMDQ4PEA=="));
		
		s = HandshakeUtils.generateKey();
		b = Base64.getDecoder().decode(s);
		assertEquals(16, b.length);
		assertFalse(s.equals(HandshakeUtils.generateKey()));
	}
	
	@Test
	public void testGenerateAnswerKey() {
		String key = "dGhlIHNhbXBsZSBub25jZQ==";
		String answerKey = HandshakeUtils.generateAnswerKey(key);
		
		assertEquals("s3pPLMBiTxaQ9kYGzzhZRbK+xOo=", answerKey);
		assertNull(HandshakeUtils.generateAnswerKey(null));
	}
	
	@Test
	public void testParseKey() {
		byte[] b = new byte[] {
				0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 
				0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10};
		
		String key = Base64.getEncoder().encodeToString(b);
		assertArrayEquals(b, HandshakeUtils.parseKey(key));
		key = Base64.getEncoder().encodeToString(Arrays.copyOf(b, 15));
		assertNull(HandshakeUtils.parseKey(key));
		key = Base64.getEncoder().encodeToString(Arrays.copyOf(b, 17));
		assertNull(HandshakeUtils.parseKey(key));
		assertNull(HandshakeUtils.parseKey("A???"));
	}
	
	@Test
	public void testExtensions() {
		assertEquals("ext1", HandshakeUtils.extension(list(new String[] {"ext1"})));
		assertEquals("ext1; p1", HandshakeUtils.extension(list(new String[] {"ext1", "p1", null})));
		assertEquals("ext1; p1=v1", HandshakeUtils.extension(list(new String[] {"ext1", "p1", "v1"})));
		assertEquals("ext1; p1=v1; p2; p3=v3", HandshakeUtils.extension(list(new String[] {"ext1", "p1", "v1", "p2", null, "p3", "v3"})));

		assertArrayEquals(new String[0], HandshakeUtils.extension("").toArray());
		assertArrayEquals(new String[] {"ext1"}, HandshakeUtils.extension("ext1").toArray());
		assertArrayEquals(new String[] {"ext1"}, HandshakeUtils.extension(" ext1 ").toArray());
		assertArrayEquals(new String[] {"ext1", "p1", null}, HandshakeUtils.extension("ext1;p1").toArray());
		assertArrayEquals(new String[] {"ext1", "p1", null}, HandshakeUtils.extension("ext1 ; p1 ").toArray());
		assertArrayEquals(new String[] {"ext1", "p1", "v1"}, HandshakeUtils.extension("ext1;p1=v1").toArray());
		assertArrayEquals(new String[] {"ext1", "p1", "v1"}, HandshakeUtils.extension("ext1;p1=\"v1\"").toArray());
		assertArrayEquals(new String[] {"ext1", "p1", "v1"}, HandshakeUtils.extension("ext1; p1 =  \"v1\" ").toArray());
		assertArrayEquals(new String[] {"ext1", "p1", ""}, HandshakeUtils.extension("ext1;p1= \"\" ").toArray());
		assertArrayEquals(new String[] {"ext1", "p1", "\""}, HandshakeUtils.extension("ext1;p1= \" ").toArray());
		assertArrayEquals(new String[] {"ext1", "p1", "v1", "p2", null}, HandshakeUtils.extension("ext1;p1=v1; p2").toArray());
		assertArrayEquals(new String[] {"ext1", "p1", "v1", "p2", null, "p3", "v3"}, HandshakeUtils.extension("ext1;p1=v1; p2;p3=v3").toArray());
		
		assertArrayEquals(new String[] {"ext1", "p1", "v1"}, HandshakeUtils.extension("ext1;p1=v1;;").toArray());
		assertArrayEquals(new String[] {"ext1", "p1", "v1"}, HandshakeUtils.extension(";ext1;p1=v1;;").toArray());
	}
}
