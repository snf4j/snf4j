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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.SSLSession;
import org.snf4j.core.StreamSession;
import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.core.handler.TestHandler;
import org.snf4j.websocket.DefaultWebSocketSessionConfig;
import org.snf4j.websocket.extensions.GroupIdentifier;
import org.snf4j.websocket.extensions.IExtension;
import org.snf4j.websocket.extensions.InvalidExtensionException;

public class HanshakerTest extends HandshakeTest {

	Boolean upperCase;
	
	String subProtocols;

	String extensions;
	
	String version;
	
	String upgrade;
	
	String connection;
	
	String host;
	
	String key;
	
	@Before
	public void before() {
		upperCase = null;
		subProtocols = null;
		extensions = null;
		version = null;
		host = "snf4j.org";
	}
	
	URI uri(String uri) throws URISyntaxException  {
		return new URI(uri);
	}
	
	URI uri() throws URISyntaxException {
		return uri("ws://snf4j.org/find?100");
	}
	
	@Override
	protected void assertFields(HandshakeFrame f, String s) {
		s = s.replace("UPG;", "Upgrade:websocket;");
		s = s.replace("CON;", "Connection:Upgrade;");
		s = s.replace("KEY;", "Sec-WebSocket-Key:" + f.getValue("Sec-WebSocket-Key") + ";");
		s = s.replace("VER;", "Sec-WebSocket-Version:13;");
		s = s.replace("ACC;", "Sec-WebSocket-Accept:s3pPLMBiTxaQ9kYGzzhZRbK+xOo=;");
		super.assertFields(f, s);
	}
	
	String value(String s) {
		if (upperCase == null) {
			return s;
		}
		if (upperCase) {
			return s.toUpperCase();
		}
		else {
			return s.toLowerCase();
		}
	}

	HandshakeRequest request(String uri) {
		HandshakeRequest r = new HandshakeRequest(uri);
		
		if (host != null) {
			r.addValue(value("Host"), host);
		}
		if (!"no".equals(upgrade)) {
			r.addValue(value("Upgrade"), value(upgrade == null ? "websocket" : upgrade));
		}
		if (!"no".equals(connection)) {
			r.addValue(value("Connection"), value(connection == null ? "Upgrade" : connection));
		}
		if (!"no".equals(key)) {
			r.addValue(value("Sec-WebSocket-Key"), key == null ? "dGhlIHNhbXBsZSBub25jZQ==" : key);
		}
		if (!"no".equals(version)) { 
			r.addValue(value("Sec-WebSocket-Version"), version == null ? "13" : version );
		}
		if (subProtocols != null) {
			r.addValue(value("Sec-WebSocket-Protocol"), subProtocols);
		}
		if (extensions != null) {
			r.addValue(value("Sec-WebSocket-Extensions"), extensions);
		}
		return r;
	}
	
	
	HandshakeResponse response(int status, String reason) {
		HandshakeResponse r = new HandshakeResponse(status, reason);
		
		if (!"no".equals(upgrade)) {
			r.addValue(value("Upgrade"), value(upgrade == null ? "websocket" : upgrade));
		}
		if (!"no".equals(connection)) {
			r.addValue(value("Connection"), value(connection == null ? "Upgrade" : connection));
		}
		if (!"no".equals(key)) {
			r.addValue(value("Sec-WebSocket-Accept"), key == null ? "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=" : key);
		}
		if (subProtocols != null) {
			r.addValue(value("Sec-WebSocket-Protocol"), subProtocols);
		}
		if (extensions != null) {
			r.addValue(value("Sec-WebSocket-Extensions"), extensions);
		}
		return r;
	}
	
	@Test
	public void testContructor() {
		Handshaker h = new Handshaker(new Config(), true);
		
		assertTrue(h.isClientMode());
		assertFalse(h.isFinished());
		assertFalse(h.isClosing());
		assertNull(h.getSubProtocol());
		
		h = new Handshaker(new Config(), false);
		
		assertFalse(h.isClientMode());
		assertFalse(h.isFinished());
		assertFalse(h.isClosing());
		assertNull(h.getSubProtocol());
	}
	
	@Test
	public void testHandshake() throws Exception {	
		Config config = new Config(uri());
		Handshaker h = new Handshaker(config, false);
		assertNull(h.handshake());
		h = new Handshaker(config, true);
		HandshakeFrame f = h.handshake();
		assertFields(f, "Host:snf4j.org;UPG;CON;KEY;VER;");
		config.setRequestOrigin("http://snf4j.org:80");
		f = h.handshake();
		assertFields(f, "Host:snf4j.org;UPG;CON;KEY;Origin:http://snf4j.org:80;VER;");
		config.setSupportedSubProtocols("chat");
		f = h.handshake();
		assertFields(f, "Host:snf4j.org;UPG;CON;KEY;Origin:http://snf4j.org:80;VER;Sec-WebSocket-Protocol:chat;");
		config.setRequestOrigin(null);
		config.setSupportedSubProtocols("superchat", "chat");
		f = h.handshake();
		assertFields(f, "Host:snf4j.org;UPG;CON;KEY;VER;Sec-WebSocket-Protocol:superchat, chat;");
		config.setSupportedSubProtocols();
		f = h.handshake();
		assertFields(f, "Host:snf4j.org;UPG;CON;KEY;VER;");
		config.setReqHeaders("My-Field", "Value1", "Server", "SNF4J");
		f = h.handshake();
		assertFields(f, "Host:snf4j.org;UPG;CON;KEY;VER;My-Field:Value1;Server:SNF4J;");
	}
	
	void assertResponse(HandshakeFrame f, int status) {
		assertTrue(f instanceof HandshakeResponse);
		HandshakeResponse r = (HandshakeResponse) f;
		assertEquals(status, r.getStatus());
	}
	
	@Test
	public void testServerHandshake() throws Exception {	
		Config config = new Config(uri());
		Handshaker h = new Handshaker(config, false);
		
		HandshakeFrame f = h.handshake(request("/uri"));
		assertResponse(f,101);
		assertFields(f, "UPG;CON;ACC;");
		upperCase = true;
		f = h.handshake(request("/uri"));
		assertResponse(f,101);
		assertFields(f, "UPG;CON;ACC;");
		upperCase = false;
		f = h.handshake(request("/uri"));
		assertResponse(f,101);
		assertFields(f, "UPG;CON;ACC;");
		
		upperCase = null;
		subProtocols = "chat";
		config.setSupportedSubProtocols("chat");
		f = h.handshake(request("/uri"));
		assertResponse(f,101);
		assertFields(f, "UPG;CON;ACC;Sec-WebSocket-Protocol:chat;");
		upperCase = true;
		f = h.handshake(request("/uri"));
		assertResponse(f,101);
		assertFields(f, "UPG;CON;ACC;Sec-WebSocket-Protocol:chat;");
		upperCase = false;
		f = h.handshake(request("/uri"));
		assertResponse(f,101);
		assertFields(f, "UPG;CON;ACC;Sec-WebSocket-Protocol:chat;");
		
		try {
			h.handshake(response(400, "Bad Request"));
		}
		catch (InvalidHandshakeException e) {
		}
	}
	
	@Test
	public void testAcceptVersion() throws Exception {
		Config config = new Config(uri());
		Handshaker h = new Handshaker(config, false);
		
		version = "no";
		HandshakeFrame f = h.handshake(request("/uri"));
		assertResponse(f,400);
		assertFields(f, "");
		version = "ab";
		f = h.handshake(request("/uri"));
		assertResponse(f,400);
		assertFields(f, "");		
		version = "14";
		f = h.handshake(request("/uri"));
		assertResponse(f,426);
		assertFields(f, "Sec-WebSocket-Version:13;");	
		version = "13, 14";
		f = h.handshake(request("/uri"));
		assertResponse(f,101);
		version = "12, 13, 14";
		f = h.handshake(request("/uri"));
		assertResponse(f,101);
		version = "12, 14";
		f = h.handshake(request("/uri"));
		assertResponse(f,426);
		assertFields(f, "Sec-WebSocket-Version:13;");	
	}
	
	@Test
	public void testAcceptBasicFields() throws Exception {
		Config config = new Config(uri());
		Handshaker h = new Handshaker(config, false);
	
		upgrade = "no";
		HandshakeFrame f = h.handshake(request("/uri"));
		assertResponse(f,400);
		assertFields(f, "");
		assertEquals("Missing websocket upgrade", h.getClosingReason());
		upgrade = "xxx";
		f = h.handshake(request("/uri"));
		assertResponse(f,400);
		assertFields(f, "");
		assertEquals("Invalid websocket upgrade: xxx", h.getClosingReason());
		upgrade = null;
		connection = "no";
		f = h.handshake(request("/uri"));
		assertResponse(f,400);
		assertFields(f, "");
		assertEquals("Missing websocket connection", h.getClosingReason());
		connection = "xxx";
		f = h.handshake(request("/uri"));
		assertResponse(f,400);
		assertFields(f, "");
		assertEquals("Invalid websocket connection: xxx", h.getClosingReason());
	}
	
	void assertUri(Handshaker h, String expected, int status, String uri) throws InvalidHandshakeException {
		HandshakeFrame f = h.handshake(request(uri));
		assertResponse(f,status);
		if (status == 101) {
			assertFields(f, "UPG;CON;ACC;");
		}
		assertEquals(expected, h.getUri().toASCIIString());
	}
	
	@Test
	public void testAssertUri() throws Exception {
		Config config = new Config(uri());
		Handshaker h = new Handshaker(config, false);
		
		HandshakeFrame f = h.handshake(request("/u ri"));
		assertResponse(f,400);
		assertFields(f, "");
		assertNull(h.getUri());
		assertEquals("Invalid websocket request uri: Illegal character in path at index 2: /u ri", h.getClosingReason());
		
		assertUri(h, "ws://host/uri", 101, "http://host/uri");
		assertUri(h, "wss://host/uri", 101, "https://host/uri");
		assertUri(h, "ws://host/uri", 101, "HTTP://host/uri");
		assertUri(h, "wss://host/uri", 101, "HTTPS://host/uri");
		assertUri(h, "ws://host/uri", 101, "ws://host/uri");
		assertUri(h, "wss://host/uri", 101, "wss://host/uri");
		
		h.setSession(new StreamSession(new TestHandler()));
		assertUri(h, "ws://snf4j.org/uri?find%20c", 101, "/uri?find%20c");
		assertUri(h, "ws://snf4j.org//host/uri", 101, "//host/uri");
		assertUri(h, "ws://snf4j.orghost/uri", 101, "host/uri");

		h.setSession(new SSLSession(new TestHandler(), false));
		assertUri(h, "wss://snf4j.org/uri?find%20c", 101, "/uri?find%20c");
		assertUri(h, "wss://snf4j.org//host/uri", 101, "//host/uri");
		assertUri(h, "wss://snf4j.orghost/uri", 101, "host/uri");

		config.acceptRequestUri = false;
		h = new Handshaker(config, false);
		f = h.handshake(request("/uri"));
		assertResponse(f,404);
		assertFields(f, "");
		assertNull(h.getUri());
		config.acceptRequestUri = true;
		assertEquals("Unacceptable websocket request uri: ws://snf4j.org/uri", h.getClosingReason());
		
		h = new Handshaker(config, false);
		host = null;
		f = h.handshake(request("/uri"));
		assertResponse(f,400);
		assertFields(f, "");
		assertNull(h.getUri());
		assertEquals("Missing websocket request host", h.getClosingReason());
		config.setIgnoreHostHeaderField(true);
		assertUri(h, "ws://null/find", 101, "/find");
	}
	
	@Test
	public void testAcceptKey() throws Exception {
		Config config = new Config(uri());
		Handshaker h = new Handshaker(config, false);
		
		key = "no";
		HandshakeFrame f = h.handshake(request("/uri"));
		assertResponse(f,400);
		assertFields(f, "");
		assertEquals("Missing websocket key", h.getClosingReason());
		key = "AAAA";
		f = h.handshake(request("/uri"));
		assertResponse(f,400);
		assertFields(f, "");
		assertEquals("Invalid websocket key: AAAA", h.getClosingReason());
		key = null;
		f = h.handshake(request("/uri"));
		assertResponse(f,101);
		assertFields(f, "UPG;CON;ACC;");
	}
	
	void assertSubProtocols(Handshaker h, String fields, String subProtocol) throws Exception {
		HandshakeFrame f = h.handshake(request("/uri"));
		assertResponse(f,101);
		assertFields(f, fields);
		assertEquals(subProtocol, h.getSubProtocol());
	}
	
	@Test
	public void testAcceptSubProtocols() throws Exception {
		Config config = new Config(uri());
		Handshaker h = new Handshaker(config, false);

		assertSubProtocols(h, "UPG;CON;ACC;", null);
		subProtocols = "proto1";
		assertSubProtocols(h, "UPG;CON;ACC;", null);
		
		subProtocols = null;
		config.setSupportedSubProtocols("proto1");
		assertSubProtocols(h, "UPG;CON;ACC;", null);
		subProtocols = "";
		assertSubProtocols(h, "UPG;CON;ACC;", null);
		subProtocols = "protoX";
		assertSubProtocols(h, "UPG;CON;ACC;", null);
		subProtocols = "proto1";
		assertSubProtocols(h, "UPG;CON;ACC;Sec-WebSocket-Protocol:proto1;", "proto1");

		subProtocols = "proto1, proto2";
		assertSubProtocols(h, "UPG;CON;ACC;Sec-WebSocket-Protocol:proto1;", "proto1");
		subProtocols = "proto2,proto1";
		assertSubProtocols(h, "UPG;CON;ACC;Sec-WebSocket-Protocol:proto1;", "proto1");
		
		subProtocols = "proto3";
		config.setSupportedSubProtocols("proto2","proto3");
		assertSubProtocols(h, "UPG;CON;ACC;Sec-WebSocket-Protocol:proto3;", "proto3");
		subProtocols = "proto2";
		assertSubProtocols(h, "UPG;CON;ACC;Sec-WebSocket-Protocol:proto2;", "proto2");
		h = new Handshaker(config, false);
		subProtocols = "Proto2";
		assertSubProtocols(h, "UPG;CON;ACC;", null);
		
		subProtocols = "proto3, proto2";
		assertSubProtocols(h, "UPG;CON;ACC;Sec-WebSocket-Protocol:proto3;", "proto3");

		config.setSupportedSubProtocols("*");
		subProtocols = "proto4";
		assertSubProtocols(h, "UPG;CON;ACC;Sec-WebSocket-Protocol:proto4;", "proto4");
		h = new Handshaker(config, false);
		subProtocols = null;
		assertSubProtocols(h, "UPG;CON;ACC;", null);
	}
	
	@Test
	public void testClientHandshake() throws Exception {
		Config sconfig = new Config();
		Handshaker sh = new Handshaker(sconfig, false);
		Config cconfig = new Config(uri());
		Handshaker ch = new Handshaker(cconfig, true);

		HandshakeFrame r = ch.handshake();
		assertFalse(ch.isFinished());
		assertFalse(ch.isClosing());
		r = sh.handshake(r);
		assertTrue(sh.isFinished());
		assertFalse(sh.isClosing());
		assertNull(ch.handshake(r));
		assertTrue(ch.isFinished());
		assertFalse(ch.isClosing());
		
		ch = new Handshaker(cconfig, true);
		r = ch.handshake();
		connection = "xxx";
		assertNull(ch.handshake(response(101, "Switching Protocols")));
		assertFalse(ch.isFinished());
		assertTrue(ch.isClosing());
		
		ch = new Handshaker(cconfig, true);
		r = ch.handshake();
		try {
			ch.handshake(request("/ur"));
			fail();
		}
		catch (InvalidHandshakeException e) {
		}
	}
	
	Handshaker assertValidate(boolean finished, Config config) throws Exception {
		Handshaker h = new Handshaker(config, true);
		
		HandshakeFrame r = h.handshake();
		if (key == null) {
			key = HandshakeUtils.generateAnswerKey(r.getValue(HandshakeUtils.SEC_WEB_SOCKET_KEY));
		}
		assertNull(h.handshake(response(101, "X")));
		assertEquals(finished, h.isFinished());
		assertEquals(!finished, h.isClosing());
		return h;
	}

	Handshaker assertValidate(boolean finished) throws Exception {
		return assertValidate(finished, new Config(uri()));
	}
	
	@Test
	public void testValidateStatus() throws Exception {
		Handshaker h = new Handshaker(new Config(uri()), true);
		
		HandshakeFrame r = h.handshake();
		if (key == null) {
			key = HandshakeUtils.generateAnswerKey(r.getValue(HandshakeUtils.SEC_WEB_SOCKET_KEY));
		}
		assertNull(h.handshake(response(100, "X")));
		assertEquals(false, h.isFinished());
		assertEquals(true, h.isClosing());
		assertEquals("Invalid websocket response status: 100", h.getClosingReason());
	}
	
	@Test
	public void testValidateBasicFields() throws Exception {
		assertValidate(true);
		upperCase = true;
		key = null;
		assertValidate(true);
		upperCase = false;
		upgrade = "websocket";
		key = null;
		assertValidate(true);
		upgrade = "Websocket";
		key = null;
		assertValidate(true);
		upgrade = "Websocket,xxx";
		key = null;
		assertValidate(true);
		upgrade = "yyy, Websocket ";
		key = null;
		assertValidate(true);
		upgrade = "yyy, websocket , xxx";
		key = null;
		assertValidate(true);
		
		upgrade = "no";
		key = null;
		assertValidate(false);
		upgrade = "xxx";
		key = null;
		assertValidate(false);
		upgrade = null;
		key = null;
		assertValidate(true);
		connection = "no";
		key = null;
		assertValidate(false);
		connection = "xxx";
		key = null;
		assertValidate(false);
		connection = "xxx,yyy, zzz";
		key = null;
		assertValidate(false);
	}
	
	@Test
	public void testValidateAnswerKey() throws Exception {
		assertNull(assertValidate(true).getClosingReason());
		key = "no";
		assertEquals("Missing websocket key challenge", assertValidate(false).getClosingReason());
		key = "AAAA";
		String s = assertValidate(false).getClosingReason();
		assertTrue(s.startsWith("Invalid websocket key challenge. Actual: AAAA. Expected: "));
		assertTrue(s.endsWith("="));
	}
	
	@Test
	public void testValidateSubProtocol() throws Exception {
		Config config = new Config(uri());
		
		assertNull(assertValidate(true, config).getSubProtocol());
		config.setSupportedSubProtocols();
		key = null;
		assertNull(assertValidate(true, config).getSubProtocol());
		key = null;
		subProtocols = "proto1";
		assertNull(assertValidate(false, config).getSubProtocol());
		
		key = null;
		subProtocols = null;
		config.setSupportedSubProtocols("proto1");
		assertNull(assertValidate(false, config).getSubProtocol());
		key = null;
		subProtocols = "proto1";
		assertEquals("proto1", assertValidate(true, config).getSubProtocol());
		key = null;
		subProtocols = "proto2";
		assertNull(assertValidate(false, config).getSubProtocol());
		
		key = null;
		subProtocols = null;
		config.setSupportedSubProtocols("proto1", "proto2");
		assertNull(assertValidate(false, config).getSubProtocol());
		key = null;
		subProtocols = "proto3";
		assertNull(assertValidate(false, config).getSubProtocol());
		key = null;
		subProtocols = "proto1, proto2";
		assertNull(assertValidate(false, config).getSubProtocol());
		key = null;
		subProtocols = "proto2";
		assertEquals("proto2", assertValidate(true, config).getSubProtocol());
		key = null;
		subProtocols = "";
		assertNull(assertValidate(false, config).getSubProtocol());
		
	}
	
	@Test
	public void testHasExtensions() throws Exception {
		Config config = new Config(uri());
		Extension ext0 = new Extension("ext0").offer("ext0", "param0");
		
		Handshaker h = assertValidate(true, config);
		assertFalse(h.hasExtensions());
		assertEquals(0, h.getExtensions().length);
		key = null;
		config.setSupportedExtensions();
		assertFalse(h.hasExtensions());
		assertEquals(0, h.getExtensions().length);
		key = null;
		extensions = "ext0; param0";
		config.setSupportedExtensions(ext0);
		h = assertValidate(true, config);
		assertTrue(h.hasExtensions());
		assertEquals(1, h.getExtensions().length);
	}
	
	@Test
	public void testValidateExtensions() throws Exception {
		Config config = new Config(uri());
		String USER1 = "USER1";
		
		assertEquals(0, assertValidate(true, config).getExtensions().length);
		config.setSupportedExtensions();
		key = null;
		assertEquals(0, assertValidate(true, config).getExtensions().length);
		key = null;
		extensions = "ext1; param1";
		assertEquals(0, assertValidate(false, config).getExtensions().length);
		
		Extension ext0 = new Extension("ext0").offer("ext0", "param0");
		Extension ext1 = new Extension("ext1").offer("ext1", "param1");
		key = null;
		extensions = null;
		config.setSupportedExtensions(ext0);
		assertEquals(0, assertValidate(true, config).getExtensions().length);
		key = null;
		extensions = "ext0; param0";
		IExtension[] e = assertValidate(true, config).getExtensions();
		assertEquals(1, e.length);
		assertTrue(ext0 == e[0]);
		key = null;
		extensions = "ext1; param1";
		assertEquals(0, assertValidate(false, config).getExtensions().length);
		
		key = null;
		extensions = null;
		config.setSupportedExtensions(ext0, ext1);
		assertEquals(0, assertValidate(true, config).getExtensions().length);		
		key = null;
		extensions = "ext3; param3";
		assertEquals(0, assertValidate(false, config).getExtensions().length);
		
		key = null;
		extensions = "ext0; param0, ext1; param1";
		e = assertValidate(false, config).getExtensions();
		assertEquals(0, e.length);

		ext1.group(USER1);
		key = null;
		extensions = "ext0; param0, ext1; param1";
		e = assertValidate(true, config).getExtensions();
		assertEquals(2, e.length);
		assertTrue(ext0 == e[0]);
		assertTrue(ext1 == e[1]);
		key = null;
		extensions = "ext1; param1, ext0; param0";
		e = assertValidate(true, config).getExtensions();
		assertEquals(2, e.length);
		assertTrue(ext0 == e[1]);
		assertTrue(ext1 == e[0]);
		ext1.group(GroupIdentifier.COMPRESSION);
		
		key = null;
		extensions = "ext1; param1";
		e = assertValidate(true, config).getExtensions();
		assertEquals(1, e.length);
		assertTrue(ext1 == e[0]);
		key = null;
		extensions = "";
		assertEquals(0, assertValidate(true, config).getExtensions().length);
		
		Extension ext0_2 = new Extension("ext0").offer("ext0", "param0_2");
		key = null;
		extensions = "ext0; param0";
		config.setSupportedExtensions(ext0, ext0_2);
		e = assertValidate(true, config).getExtensions();
		assertEquals(1, e.length);
		assertTrue(ext0 == e[0]);
		key = null;
		extensions = "ext0; param0_2";
		e = assertValidate(true, config).getExtensions();
		assertEquals(1, e.length);
		assertTrue(ext0_2 == e[0]);
		key = null;
		extensions = "ext0; param0_2, ext0; param0";
		e = assertValidate(false, config).getExtensions();
		assertEquals(0, e.length);
		ext0_2.group(USER1);
		key = null;
		extensions = "ext0; param0_2, ext0; param0";
		e = assertValidate(false, config).getExtensions();
		assertEquals(0, e.length);
		
		ext0_2.acceptException = new InvalidExtensionException("E2");
		key = null;
		extensions = "ext0; param0_2";
		e = assertValidate(false, config).getExtensions();
		assertEquals(0, e.length);

	}
	
	@Test
	public void testExtensionsOffer() throws Exception {
		Config config = new Config(uri());
		Handshaker h = new Handshaker(config, true);
		
		HandshakeFrame r = h.handshake();
		assertNull(r.getValue(HandshakeUtils.SEC_WEB_SOCKET_EXTENSIONS));
		config.setSupportedExtensions(new Extension("").offer("ext1"));
		r = h.handshake();
		assertEquals("ext1", r.getValue(HandshakeUtils.SEC_WEB_SOCKET_EXTENSIONS));
		config.setSupportedExtensions(new Extension("").offer("ext1","param1=v1"));
		r = h.handshake();
		assertEquals("ext1; param1=v1", r.getValue(HandshakeUtils.SEC_WEB_SOCKET_EXTENSIONS));
		config.setSupportedExtensions(new Extension("").offer("ext1","param1=v1","param2"));
		r = h.handshake();
		assertEquals("ext1; param1=v1; param2", r.getValue(HandshakeUtils.SEC_WEB_SOCKET_EXTENSIONS));
		config.setSupportedExtensions();
		r = h.handshake();
		assertNull(r.getValue(HandshakeUtils.SEC_WEB_SOCKET_EXTENSIONS));

		config.setSupportedExtensions(new Extension("").offer("ext1","param1=v1"), new Extension("").offer("ext2","param1"));
		r = h.handshake();
		assertEquals("ext1; param1=v1, ext2; param1", r.getValue(HandshakeUtils.SEC_WEB_SOCKET_EXTENSIONS));
	}
	
	void assertExtensions(Handshaker h, String fields) throws Exception {
		HandshakeFrame f = h.handshake(request("/uri"));
		assertResponse(f,101);
		assertFields(f, fields);
	}
	
	@Test
	public void testAcceptExtensionGrouping() throws Exception {
		Config config = new Config(uri());
		Handshaker h;
		String USER1 = "USER1";
		
		Extension e1_1 = new Extension("e1").offer("e1");
		Extension e1_2 = new Extension("e1").offer("e1","p1");
		Extension e2_1 = new Extension("e2").offer("e2");
		Extension e2_2 = new Extension("e2").offer("e2","p1").group(USER1);
		
		e1_1.accept(e1_1).response("e1");
		e1_2.accept(e1_2).response("e1","p1");
		extensions = "e1,e1;p1";
		config.setSupportedExtensions(e1_1,e1_2);
		h = new Handshaker(config, false);		
		assertExtensions(h,"UPG;CON;ACC;Sec-WebSocket-Extensions:e1;");
		
		e2_1.accept(e2_1).response("e2");
		extensions = "e1,e2";
		config.setSupportedExtensions(e1_1,e2_1);
		h = new Handshaker(config, false);		
		assertExtensions(h,"UPG;CON;ACC;Sec-WebSocket-Extensions:e1;");
		
		e2_2.accept(e2_2).response("e2","p1");
		extensions = "e1,e2; p1";
		config.setSupportedExtensions(e1_1,e2_2);
		h = new Handshaker(config, false);		
		assertExtensions(h,"UPG;CON;ACC;Sec-WebSocket-Extensions:e1, e2; p1;");
	}
	
	@Test
	public void testAcceptExtensions() throws Exception {
		Config config = new Config(uri());
		Handshaker h;
		String USER1 = "USER1";
		String USER2 = "USER2";
		
		Extension e1 = new Extension("e1").offer("e1","p1");
		Extension e2 = new Extension("e2").response("e2", "p2").group(USER1);
		Extension e3 = new Extension("e3").offer("e3","e3").accept(e2);
		Extension e4 = new Extension("e4").offer("e4","p4");
		Extension e5 = new Extension("e5").response("e5", "p5").group(USER2);
		Extension e6 = new Extension("e6").offer("e6","e6").accept(e5);
		
		//no extension
		h = new Handshaker(config, false);		
		assertExtensions(h,"UPG;CON;ACC;");
		assertEquals("", e1.getTrace());
		assertEquals(0, h.getExtensions().length);
		h = new Handshaker(config, false);		
		extensions = "e1";
		assertExtensions(h,"UPG;CON;ACC;");
		assertEquals("", e1.getTrace());
		assertEquals(0, h.getExtensions().length);
		config.setSupportedExtensions();
		h = new Handshaker(config, false);		
		extensions = "e1";
		assertExtensions(h,"UPG;CON;ACC;");
		assertEquals("", e1.getTrace());
		assertEquals(0, h.getExtensions().length);
		
		//one extension (no accept)
		extensions = null;
		config.setSupportedExtensions(e1);
		h = new Handshaker(config, false);		
		assertExtensions(h,"UPG;CON;ACC;");
		assertEquals("", e1.getTrace());
		assertEquals(0, h.getExtensions().length);
		h = new Handshaker(config, false);		
		extensions = "e1";
		assertExtensions(h,"UPG;CON;ACC;");
		assertEquals("A:e1|", e1.getTrace());
		assertEquals(0, h.getExtensions().length);
		h = new Handshaker(config, false);		
		extensions = "";
		assertExtensions(h,"UPG;CON;ACC;");
		assertEquals("", e1.getTrace());
		assertEquals(0, h.getExtensions().length);
		
		//one extension (accept)
		extensions = null;
		config.setSupportedExtensions(e3);
		h = new Handshaker(config, false);		
		assertExtensions(h,"UPG;CON;ACC;");
		assertEquals("", e3.getTrace());
		assertEquals(0, h.getExtensions().length);
		h = new Handshaker(config, false);		
		extensions = "e3; param1";
		assertExtensions(h,"UPG;CON;ACC;Sec-WebSocket-Extensions:e2; p2;");
		assertEquals("A:e3, param1, null|", e3.getTrace());
		assertEquals(1, h.getExtensions().length);
		assertEquals("e2", h.getExtensions()[0].getName());
		assertEquals("e2", h.getExtension("e2").getName());
		
		//two extensions (no accept)
		extensions = null;
		config.setSupportedExtensions(e1,e4);
		h = new Handshaker(config, false);		
		assertExtensions(h,"UPG;CON;ACC;");
		assertEquals("", e1.getTrace());
		assertEquals("", e4.getTrace());
		assertEquals(0, h.getExtensions().length);
		h = new Handshaker(config, false);		
		extensions = "e1; p1, e4; p4";
		assertExtensions(h,"UPG;CON;ACC;");
		assertEquals("A:e1, p1, null|A:e4, p4, null|", e1.getTrace());
		assertEquals("A:e1, p1, null|A:e4, p4, null|", e4.getTrace());
		assertEquals(0, h.getExtensions().length);

		//two extensions (accept first)
		extensions = null;
		config.setSupportedExtensions(e3,e4);
		h = new Handshaker(config, false);		
		assertExtensions(h,"UPG;CON;ACC;");
		assertEquals("", e3.getTrace());
		assertEquals("", e4.getTrace());
		assertEquals(0, h.getExtensions().length);
		h = new Handshaker(config, false);		
		extensions = "e3; p3, e4; p4";
		assertExtensions(h,"UPG;CON;ACC;Sec-WebSocket-Extensions:e2; p2;");
		assertEquals("A:e3, p3, null|A:e4, p4, null|", e3.getTrace());
		assertEquals("A:e3, p3, null|A:e4, p4, null|", e4.getTrace());
		assertEquals(1, h.getExtensions().length);
		assertEquals("e2", h.getExtensions()[0].getName());
		assertEquals("e2", h.getExtension("e2").getName());
		
		//two extensions (accept last)
		extensions = null;
		config.setSupportedExtensions(e1,e6);
		h = new Handshaker(config, false);		
		assertExtensions(h,"UPG;CON;ACC;");
		assertEquals("", e1.getTrace());
		assertEquals("", e6.getTrace());
		assertEquals(0, h.getExtensions().length);
		h = new Handshaker(config, false);		
		extensions = "e1; p1, e6; p6";
		assertExtensions(h,"UPG;CON;ACC;Sec-WebSocket-Extensions:e5; p5;");
		assertEquals("A:e1, p1, null|A:e6, p6, null|", e1.getTrace());
		assertEquals("A:e1, p1, null|A:e6, p6, null|", e6.getTrace());
		assertEquals(1, h.getExtensions().length);
		assertEquals("e5", h.getExtensions()[0].getName());
		assertEquals("e5", h.getExtension("e5").getName());

		//two extensions (accept all)
		extensions = null;
		config.setSupportedExtensions(e3,e6);
		h = new Handshaker(config, false);		
		assertExtensions(h,"UPG;CON;ACC;");
		assertEquals("", e3.getTrace());
		assertEquals("", e6.getTrace());
		assertEquals(0, h.getExtensions().length);
		h = new Handshaker(config, false);		
		extensions = "e3; p3, e6; p6";
		assertExtensions(h,"UPG;CON;ACC;Sec-WebSocket-Extensions:e2; p2, e5; p5;");
		assertEquals("A:e3, p3, null|A:e6, p6, null|", e3.getTrace());
		assertEquals("A:e3, p3, null|A:e6, p6, null|", e6.getTrace());
		assertEquals(2, h.getExtensions().length);
		assertEquals("e2", h.getExtensions()[0].getName());
		assertEquals("e2", h.getExtension("e2").getName());
		assertEquals("e5", h.getExtensions()[1].getName());
		assertEquals("e5", h.getExtension("e5").getName());

		//two extensions (accept all)
		extensions = null;
		config.setSupportedExtensions(e3,e6);
		h = new Handshaker(config, false);		
		assertExtensions(h,"UPG;CON;ACC;");
		assertEquals("", e3.getTrace());
		assertEquals("", e6.getTrace());
		assertEquals(0, h.getExtensions().length);
		h = new Handshaker(config, false);		
		extensions = "e6; p6, e3; p3";
		assertExtensions(h,"UPG;CON;ACC;Sec-WebSocket-Extensions:e2; p2, e5; p5;");
		assertEquals("A:e6, p6, null|A:e3, p3, null|", e3.getTrace());
		assertEquals("A:e6, p6, null|A:e3, p3, null|", e6.getTrace());
		assertEquals(2, h.getExtensions().length);
		assertEquals("e2", h.getExtensions()[0].getName());
		assertEquals("e2", h.getExtension("e2").getName());
		assertEquals("e5", h.getExtensions()[1].getName());
		assertEquals("e5", h.getExtension("e5").getName());
		
		Extension e6_1 = new Extension("e6").response("e6","p6_1");
		Extension e6_2 = new Extension("e6").offer("e6","p6_2").accept(e6_1);
		Extension e6_3 = new Extension("e6").response("e6","p6_3");
		Extension e6_4 = new Extension("e6").offer("e6","p6_4").accept(e6_3);

		//two extensions of the same type (accept all)
		config.setSupportedExtensions(e6_2,e6_4);
		h = new Handshaker(config, false);
		assertNull(h.getExtension("e6"));
		assertEquals(0, h.getExtensions().length);
		extensions = "e6; p6";
		assertExtensions(h,"UPG;CON;ACC;Sec-WebSocket-Extensions:e6; p6_1;");
		assertEquals("A:e6, p6, null|", e6_2.getTrace());
		assertEquals("A:e6, p6, null|", e6_4.getTrace());
		assertEquals(1, h.getExtensions().length);
		assertEquals("e6", h.getExtensions()[0].getName());
		assertEquals("e6", h.getExtension("e6").getName());
		config.setSupportedExtensions(e6_4,e6_2);
		h = new Handshaker(config, false);		
		extensions = "e6; p6";
		assertExtensions(h,"UPG;CON;ACC;Sec-WebSocket-Extensions:e6; p6_3;");
		assertEquals("A:e6, p6, null|", e6_2.getTrace());
		assertEquals("A:e6, p6, null|", e6_4.getTrace());
		assertEquals(1, h.getExtensions().length);
		assertEquals("e6", h.getExtensions()[0].getName());
		assertEquals("e6", h.getExtension("e6").getName());
		h = new Handshaker(config, false);		
		extensions = "e6; p6, e6; v6";
		assertExtensions(h,"UPG;CON;ACC;Sec-WebSocket-Extensions:e6; p6_3;");
		assertEquals("A:e6, p6, null|A:e6, v6, null|", e6_2.getTrace());
		assertEquals("A:e6, p6, null|A:e6, v6, null|", e6_4.getTrace());
		assertEquals(1, h.getExtensions().length);
		assertEquals("e6", h.getExtensions()[0].getName());
		assertEquals("e6", h.getExtension("e6").getName());
		assertNull(h.getExtension("X"));
		
		Extension ex = new Extension("ex", new InvalidExtensionException("Wrong"));
		
		config.setSupportedExtensions(e3,ex);
		extensions = "e3; p33";
		h = new Handshaker(config, false);		
		HandshakeFrame f = h.handshake(request("/uri"));
		assertResponse(f,400);
		assertEquals(0, h.getExtensions().length);
	}
	
	class Extension implements IExtension {

		String name;
		
		List<String> offer;
		
		List<String> response;
		
		IExtension accept;
		
		InvalidExtensionException acceptException;
		
		Object group = GroupIdentifier.COMPRESSION;
		
		StringBuilder trace = new StringBuilder();
		
		Extension(String name) {
			this.name = name;
		}
		
		Extension(String name, InvalidExtensionException acceptException) {
			this.name = name;
			this.acceptException = acceptException;
		}
		
		void trace(String s) {
			trace.append(s).append("|");
		}
		
		String getTrace() {
			String s = trace.toString();
			
			trace.setLength(0);
			return s;
		}
		
		@Override
		public String getName() {
			return name;
		}

		@Override
		public Object getGroupId() {
			return group;
		}

		Extension group(Object group) {
			this.group = group;
			return this;
		}
		
		@Override
		public IExtension acceptOffer(List<String> extension) throws InvalidExtensionException {
			if (acceptException != null) {
				throw acceptException;
			}
			
			String[] values = extension.toArray(new String[extension.size()]);
			
			trace("A:"+(extension == null ? "null" :HttpUtils.values(values)));
			return accept;
		}

		@Override
		public List<String> offer() {
			return offer;
		}
		
		@Override
		public List<String> response() {
			return response;
		}

		Extension accept(IExtension accept) {
			this.accept = accept;
			return this;
		}
		
		List<String> prepare(String[] items) {
			List<String> list = new ArrayList<String>();
			if (items.length < 2) {
				for (String item: items) {
					list.add(item);
				}
				return list;
			}
			
			list.add(items[0]);
			for (int i=1; i<items.length; ++i) {
				String item = items[i];
				int pos = item.indexOf('=');
				
				if (pos >= 0) {
					list.add(item.substring(0, pos).trim());
					list.add(item.substring(pos+1));
				}
				else {
					list.add(item);
					list.add(null);
				}
			}
			return list;
		}
		
		Extension offer(String... offer) {
			this.offer = prepare(offer);
			return this;
		}
		
		Extension response(String... response) {
			this.response = prepare(response);
			return this;
		}

		@Override
		public IExtension validateResponse(List<String> extension) throws InvalidExtensionException {
			if (acceptException != null) {
				throw acceptException;
			}
			
			if (offer.size() == extension.size()) {
				for (int i=0; i<offer.size(); ++i) {
					if (offer.get(i) == null) {
						if (extension.get(i) != null) {
							return null;
						}
					}
					else if (!offer.get(i).equals(extension.get(i))) {
						return null;
					}
				}
				return this;
			}
			return null;
		}

		@Override
		public void updateEncoders(ICodecPipeline pipeline) {
		}

		@Override
		public void updateDecoders(ICodecPipeline pipeline) {
		}
		
	}
	
	class Config extends DefaultWebSocketSessionConfig {

		String[] reqHeaders, resHeaders;
		
		boolean acceptRequestUri = true;
		
		public Config(URI requestUri) {
			super(requestUri);
		}
		
		public Config() {
			this(null);
		}
		
		void setReqHeaders(String... headers) {
			if (headers.length == 0) {
				reqHeaders = null;
			}
			reqHeaders = headers;
		}

		void setResHeaders(String... headers) {
			if (headers.length == 0) {
				resHeaders = null;
			}
			resHeaders = headers;
		}
		
		@Override
		public void customizeHeaders(HandshakeRequest request) {
			if (reqHeaders == null) {
				super.customizeHeaders(request);
				return;
			}
			for (int i=0; i<reqHeaders.length; i+=2) {
				request.addValue(reqHeaders[i], reqHeaders[i+1]);
			}
		}
		
		@Override
		public void customizeHeaders(HandshakeResponse response) {
			if (resHeaders == null) {
				super.customizeHeaders(response);
				return;
			}
			for (int i=0; i<resHeaders.length; i+=2) {
				response.addValue(resHeaders[i], resHeaders[i+1]);
			}
		}

		@Override
		public boolean acceptRequestUri(URI uri) {
			return acceptRequestUri;
		}

	}
}
