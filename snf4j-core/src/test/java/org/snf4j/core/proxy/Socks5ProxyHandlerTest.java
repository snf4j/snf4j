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
package org.snf4j.core.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.Client;
import org.snf4j.core.Packet;
import org.snf4j.core.PacketType;
import org.snf4j.core.Server;
import org.snf4j.core.TraceBuilder;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.proxy.HttpProxyHandlerTest.Session;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISessionConfig;

public class Socks5ProxyHandlerTest {
	
	long TIMEOUT = 2000;
	int PORT = 1080;//7777;

	Server s;
	Client c;

	@Before
	public void before() {
		s = c = null;
	}
	
	@After
	public void after() throws InterruptedException {
		if (c != null) c.stop(TIMEOUT);
		if (s != null) s.stop(TIMEOUT);
	}

	static InetSocketAddress uaddr(String host, int port) {
		return Socks4ProxyHandlerTest.uaddr(host, port);
	}
	
	static InetSocketAddress addr(byte[] addr, int port) throws Exception {
		return Socks4ProxyHandlerTest.addr(addr, port);
	}
	
	static byte[] bytes(int... b) {
		return Socks4ProxyHandlerTest.bytes(b);
	}
	
	ByteBuffer fbb(int... b) {
		return ByteBuffer.wrap(bytes(b));
		
	}

	ByteBuffer nfbb(int... b) {
		ByteBuffer bb = ByteBuffer.allocate(b.length);
		bb.put(bytes(b));
		return bb;
	}
	
	void assertContructor(Socks5ProxyHandler h, SocketAddress a, Socks5Command command, String username, String password, long timeout, 
			ISessionConfig config, ISessionStructureFactory factory) throws Exception {
		Field f;
		Object o;
		
		assertTrue(h.state.getClass() == Socks5InitState.class);
		f = Socks5ProxyHandler.class.getDeclaredField("commandState");
		f.setAccessible(true);
		o = f.get(h);
		f = Socks5CommandState.class.getDeclaredField("command");
		f.setAccessible(true);
		assertTrue(command == f.get(o));
		f = Socks5ProxyHandler.class.getDeclaredField("passwordAuthState");
		f.setAccessible(true);
		o = f.get(h);
		f = Socks5PasswordAuthState.class.getDeclaredField("username");
		f.setAccessible(true);
		assertEquals(username, f.get(o));
		f = Socks5PasswordAuthState.class.getDeclaredField("password");
		f.setAccessible(true);
		assertEquals(password, f.get(o));
		assertTrue(a == h.getAddress());
		f = AbstractProxyHandler.class.getDeclaredField("connectionTimeout");
		f.setAccessible(true);
		assertEquals(timeout, f.getLong(h));
		if (config == null) {
			assertTrue(h.getConfig().getClass() == DefaultSessionConfig.class);
		}
		else {
			assertTrue(h.getConfig() == config);
		}
		if (factory == null) {
			factory = DefaultSessionStructureFactory.DEFAULT;
		}
		assertTrue(h.getFactory() == factory);
	}
	
	@Test
	public void testContructor() throws Exception {
		InetSocketAddress a = uaddr("127.0.0.1", 80);
		Socks5Command C = Socks5Command.CONNECT;
		Socks5Command B = Socks5Command.BIND;
		Socks5Command U = Socks5Command.UDP_ASSOCIATE;
		DefaultSessionConfig c = new DefaultSessionConfig();
		DefaultSessionStructureFactory f = new DefaultSessionStructureFactory() {};

		assertContructor(new Socks5ProxyHandler(a), a, C, null, null, 10000, null, null);
		assertContructor(new Socks5ProxyHandler(a,c), a, C, null, null, 10000, c, null);
		assertContructor(new Socks5ProxyHandler(a,c,f), a, C, null, null, 10000, c, f);
		assertContructor(new Socks5ProxyHandler(a, "u1", "p1"), a, C, "u1", "p1", 10000, null, null);
		assertContructor(new Socks5ProxyHandler(a, "u1", "p1", c), a, C, "u1", "p1", 10000, c, null);
		assertContructor(new Socks5ProxyHandler(a, "u1", "p1", c, f), a, C, "u1", "p1", 10000, c, f);
		assertContructor(new Socks5ProxyHandler(a,B), a, B, null, null, 10000, null, null);
		assertContructor(new Socks5ProxyHandler(a,B,c), a, B, null, null, 10000, c, null);
		assertContructor(new Socks5ProxyHandler(a,B,c,f), a, B, null, null, 10000, c, f);
		assertContructor(new Socks5ProxyHandler(a,U, "u1", "p1"), a, U, "u1", "p1", 10000, null, null);
		assertContructor(new Socks5ProxyHandler(a,U, "u1", "p1", c), a, U, "u1", "p1", 10000, c, null);
		assertContructor(new Socks5ProxyHandler(a,U, "u1", "p1", c, f), a, U, "u1", "p1", 10000, c, f);

		assertContructor(new Socks5ProxyHandler(a,33), a, C, null, null, 33, null, null);
		assertContructor(new Socks5ProxyHandler(a,33,c), a, C, null, null, 33, c, null);
		assertContructor(new Socks5ProxyHandler(a,33,c,f), a, C, null, null, 33, c, f);
		assertContructor(new Socks5ProxyHandler(a, "u1", "p1", 33), a, C, "u1", "p1", 33, null, null);
		assertContructor(new Socks5ProxyHandler(a, "u1", "p1", 33, c), a, C, "u1", "p1", 33, c, null);
		assertContructor(new Socks5ProxyHandler(a, "u1", "p1", 33, c, f), a, C, "u1", "p1", 33, c, f);
		assertContructor(new Socks5ProxyHandler(a,B,33), a, B, null, null, 33, null, null);
		assertContructor(new Socks5ProxyHandler(a,B,33,c), a, B, null, null, 33, c, null);
		assertContructor(new Socks5ProxyHandler(a,B,33,c,f), a, B, null, null, 33, c, f);
		assertContructor(new Socks5ProxyHandler(a,U, "u1", "p1",33), a, U, "u1", "p1", 33, null, null);
		assertContructor(new Socks5ProxyHandler(a,U, "u1", "p1",33, c), a, U, "u1", "p1", 33, c, null);
		assertContructor(new Socks5ProxyHandler(a,U, "u1", "p1",33, c, f), a, U, "u1", "p1", 33, c, f);
		
		assertContructor(new Socks5ProxyHandler(a,U, null, "p1", c, f), a, U, null, "p1", 10000, c, f);
		assertContructor(new Socks5ProxyHandler(a,U, "u1", null, c, f), a, U, "u1", null, 10000, c, f);
		assertContructor(new Socks5ProxyHandler(a,U, "", "", c, f), a, U, null, null, 10000, c, f);
		assertContructor(new Socks5ProxyHandler(a,U, "u1", null, null, null), a, U, "u1", null, 10000, null, null);
		assertContructor(new Socks5ProxyHandler(a,U, null, "p1",33, c, f), a, U, null, "p1", 33, c, f);
		assertContructor(new Socks5ProxyHandler(a,U, "u1", null,33, c, f), a, U, "u1", null, 33, c, f);
		assertContructor(new Socks5ProxyHandler(a,U, "", "",33, c, f), a, U, null, null, 33, c, f);
		assertContructor(new Socks5ProxyHandler(a,U, "u1", "p1",33, null, null), a, U, "u1", "p1", 33, null, null);
		
		try {
			new Socks5ProxyHandler(a, null, "u1", "p1", c, f);
		}
		catch (IllegalArgumentException e) {
			assertEquals("command is null", e.getMessage());
		}
		try {
			new Socks5ProxyHandler(a, null, "u1", "p1", 100, c, f);
		}
		catch (IllegalArgumentException e) {
			assertEquals("command is null", e.getMessage());
		}
		try {
			new Socks5ProxyHandler(a, C, new String(new byte[256]), "p1", c, f);
		}
		catch (IllegalArgumentException e) {
			assertEquals("username length is too long (expected less than 256)", e.getMessage());
		}
		try {
			new Socks5ProxyHandler(a, C, "u1", new String(new byte[256]), c, f);
		}
		catch (IllegalArgumentException e) {
			assertEquals("password length is too long (expected less than 256)", e.getMessage());
		}


	}
	
	void assertAvailable(int exp, Socks5ProxyHandler p, byte[] b, int len) {
		assertEquals(exp, p.available(b, 0, len));
		byte[] b2 = new byte[len+1];
		System.arraycopy(b, 0, b2, 1, len);
		assertEquals(exp, p.available(b2, 1, len));
		ByteBuffer bb = ByteBuffer.allocate(len+1);
		bb.put(b, 0, len);
		assertEquals(exp, p.available(bb, false));
		assertEquals(len, bb.position());
		bb.flip();
		assertEquals(exp, p.available(bb, true));
		assertEquals(len, bb.remaining());
	}
	
	@Test
	public void testAvailable() {
		Socks5ProxyHandler p = new Socks5ProxyHandler(uaddr("127.0.0.1", 80), "user1", "pass1");
		p.setSession(new HttpProxyHandlerTest.Session());
		byte[] b = bytes(5,0,0);
		
		//INIT
		assertAvailable(0, p, b, 0);
		assertAvailable(0, p, b, 1);
		assertAvailable(2, p, b, 2);
		assertAvailable(2, p, b, 3);
		
		//PASSWORD
		p.read(bytes(5,2));
		assertAvailable(0, p, b, 0);
		assertAvailable(0, p, b, 1);
		assertAvailable(2, p, b, 2);
		assertAvailable(2, p, b, 3);

		//COMMAND
		p = new Socks5ProxyHandler(uaddr("127.0.0.1", 80), "user1", "pass1");
		p.setSession(new HttpProxyHandlerTest.Session());
		p.read(bytes(5,0));
		assertEquals(0, p.state.readSize());
		
		//ipv4
		b = bytes(5,0,0,1,1,2,3,4,0,80,0);
		assertAvailable(0, p, b, 0);
		assertAvailable(0, p, b, 5);
		assertAvailable(0, p, b, 6);
		assertAvailable(0, p, b, 9);
		assertAvailable(10, p, b, 10);
		assertAvailable(10, p, b, 11);

		//domain
		b = bytes(5,0,0,3,4,7,7,7,7,0,80,0);
		assertAvailable(0, p, b, 6);
		assertAvailable(0, p, b, 10);
		assertAvailable(11, p, b, 11);
		assertAvailable(11, p, b, 12);
		
		//ipv6
		b = bytes(5,0,0,4,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,0,80,0);
		assertAvailable(0, p, b, 0);
		assertAvailable(0, p, b, 6);
		assertAvailable(0, p, b, 21);
		assertAvailable(22, p, b, 22);
		assertAvailable(22, p, b, 23);
		
		//wrong atyp
		b = bytes(5,0,0,0,0,80,0);
		assertAvailable(0, p, b, 0);
		assertAvailable(0, p, b, 5);
		assertAvailable(6, p, b, 6);
		assertAvailable(6, p, b, 7);
	}
	
	@Test
	public void testEvent() {
		Socks5ProxyHandler p = new Socks5ProxyHandler(uaddr("1.2.3.4", 80), "user1", "pass1");
		Session session = new HttpProxyHandlerTest.Session();
		p.setSession(session);
		p.event(SessionEvent.CREATED);
		assertEquals("", session.trace.get(true));
		p.event(SessionEvent.OPENED);
		assertEquals("UNDONE(Incomplete SOCKS5 proxy protocol)|", session.trace.get(true));
	}

	Socks5Command cmd(int cmd) {
		switch (cmd) {
		case 0: 
			return Socks5Command.CONNECT;
			
		case 1:
			return Socks5Command.BIND;
			
		default:
			return Socks5Command.UDP_ASSOCIATE;
		}
	}
	
	void state(Socks5ProxyHandler p, Session session, int state) {
		if (state == 1) {
			session.getTrace();
			p.read(bytes(5,2));
		}
		else if (state == 2) {
			session.getTrace();
			p.read(bytes(5,0));
		}
	}
	
	void assertStateReady(InetSocketAddress addr, int state, int cmd, String user, String pass, String expected, boolean optimized) {
		Socks5ProxyHandler p = new Socks5ProxyHandler(addr, cmd(cmd), user, pass);
		Session session = new HttpProxyHandlerTest.Session();
		session.writeBytes = true;
		session.dataCopyingOptimized = optimized;
		p.setSession(session);
		p.event(SessionEvent.READY);
		state(p, session, state);
		String trace = session.trace.get(true);
		expected = Socks4ProxyHandlerTest.untag(expected);
		trace = trace.replace(" ", "");
		assertEquals(expected, trace);
	}
	
	@Test
	public void testStateReady() throws Exception {
		//INIT
		assertStateReady(uaddr("1.2.3.4", 80), 0, 0, "user1", "pass", "WR=[5,2,0,2]|R|", false);
		assertStateReady(uaddr("1.2.3.4", 80), 0, 0, "user1", "pass", "WR=[5,2,0,2]|", true);
		assertStateReady(uaddr("1.2.3.4", 80), 0, 0, "user1", null, "WR=[5,2,0,2]|R|", false);
		assertStateReady(uaddr("1.2.3.4", 80), 0, 0, null, "pass", "WR=[5,2,0,2]|R|", false);
		assertStateReady(uaddr("1.2.3.4", 80), 0, 0, null, null, "WR=[5,1,0]|R|", false);
		assertStateReady(uaddr("1.2.3.4", 80), 0, 0, "", "", "WR=[5,1,0]|R|", false);
		
		//PASSWORD
		assertStateReady(uaddr("1.2.3.4", 80), 1, 0, "user1", "pass", "WR=[1,5,<user1>,4,<pass>]|R|", false);
		assertStateReady(uaddr("1.2.3.4", 80), 1, 0, null, "p", "WR=[1,0,1,<p>]|R|", false);
		assertStateReady(uaddr("1.2.3.4", 80), 1, 0, "u", null, "WR=[1,1,<u>,0]|R|", false);
		try {
			assertStateReady(uaddr("1.2.3.4", 80), 1, 0, null, null, "WR=[1,1,<u>,0]|R|", false);
			fail();
		}
		catch (ProxyConnectionException e) {
			assertEquals("Unexpected authentication method: 2", e.getMessage());
		}
		byte[] v = new byte[255];
		Arrays.fill(v, (byte)'a');
		v[0] += 1;
		v[254] += 2;
		String longest = new String(v);
		assertStateReady(uaddr("1.2.3.4", 80), 1, 0, longest, "pass", "WR=[1,-1,<"+longest+">,4,<pass>]|R|", false);
		assertStateReady(uaddr("1.2.3.4", 80), 1, 0, "user", longest, "WR=[1,4,<user>,-1,<"+longest+">]|", true);
		assertStateReady(uaddr("1.2.3.4", 80), 1, 0, longest, longest, "WR=[1,-1,<"+longest+">,-1,<"+longest+">]|R|", false);
		
		//COMMAND
		String ipv6;
		assertStateReady(uaddr("1.2.3.4", 80), 2, 0, "u", "p", "WR=[5,1,0,1,1,2,3,4,0,80]|R|", false);
		assertStateReady(uaddr("1.2.3.4", 80), 2, 1, "u", "p", "WR=[5,2,0,1,1,2,3,4,0,80]|", true);
		assertStateReady(uaddr("1.2.3.4", 80), 2, 2, "u", "p", "WR=[5,3,0,1,1,2,3,4,0,80]|", true);
		assertStateReady(uaddr("snf4j.org", 80), 2, 0, "u", "p", "WR=[5,1,0,3,9,<snf4j.org>,0,80]|", true);
		ipv6 = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
		assertStateReady(uaddr("::", 256), 2, 0, "u", "p", "WR=[5,1,0,4,"+ipv6+",1,0]|", true);
		ipv6 = "0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,2";
		assertStateReady(uaddr("1::2", 256), 2, 0, "u", "p", "WR=[5,1,0,4,"+ipv6+",1,0]|", true);
		assertStateReady(uaddr(longest, 256), 2, 0, "u", "p", "WR=[5,1,0,3,-1,<"+longest+">,1,0]|", true);
		try {
			assertStateReady(uaddr(longest+"1", 256), 2, 0, "u", "p", "WR=[5,1,0,3,-1,<"+longest+"1>,1,0]|", true);
		}
		catch (ProxyConnectionException e) {
			assertEquals("Destination domain name length too long", e.getMessage());
		}
		assertStateReady(addr(bytes(1,2,3,4), 80), 2, 0, "u", "p", "WR=[5,1,0,1,1,2,3,4,0,80]|R|", false);
		ipv6 = "1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2";
		assertStateReady(addr(bytes(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2), 256), 2, 0, "u", "p", "WR=[5,1,0,4,"+ipv6+",1,0]|", true);
	}
	
	class ReplyListener implements ISocksReplyListener {

		TraceBuilder tracer = new TraceBuilder();
		
		@Override
		public void replyReceived(ISocksReply reply, int replyIndex) {
			String s = "" +replyIndex + ";" +
					reply.isSuccessful() + ";" +
					reply.getStatus() + ";" +
					reply.getAddressType() + ";" +
					reply.getAddress() + ";" +
					reply.getPort() + ";";
			tracer.append(s);
		}
	};
	
	ReplyListener replyListener = new ReplyListener();
	
	void assertException(String expected, int state, int cmd, byte[] data, String user) {
		Socks5ProxyHandler p = new Socks5ProxyHandler(uaddr("1.0.0.1",90), cmd(cmd), user, user);
		Session session = new HttpProxyHandlerTest.Session();
		p.addReplyListener(replyListener);
		p.setSession(session);
		state(p, session, state);
		try {
			p.read(data);
			fail();
		}
		catch (Exception e) {
			assertEquals(expected, e.getMessage());
			assertTrue(e.getClass() == ProxyConnectionException.class);
		}
	}
	
	@Test
	public void testStateException() {
		//INIT
		assertException("Unsupported SOCKS5 reply version: 4", 0, 0, bytes(4,0), "user");
		assertException("No acceptable authentication method", 0, 0, bytes(5,255), "user");
		assertException("Unexpected authentication method: 1", 0, 0, bytes(5,1), "user");	
		assertException("Unexpected authentication method: 3", 0, 0, bytes(5,3), "user");	
		assertException("Unexpected authentication method: 2", 0, 0, bytes(5,2), null);
		
		//PASSWORD
		assertException("Unsupported SOCKS5 subnegotiation reply version: 5", 1, 0, bytes(5,0), "user");
		assertException("Username/Password authentication response status code: 1", 1, 0, bytes(1,1), "user");
		
		//COMMAND
		assertException("Unsupported SOCKS5 reply version: 4", 2, 0, bytes(4,0,0,1,1,2,3,4,0,80), "user");
		assertException("Unexpected address type: 0", 2, 0, bytes(5,0,0,0,1,2,3,4,0,80), "user");
		assertException("Unexpected address type: 2", 2, 0, bytes(5,0,0,2,1,2,3,4,0,80), "user");
		assertException("Unexpected address type: 5", 2, 0, bytes(5,0,0,5,1,2,3,4,0,80), "user");
		assertException("Unexpected address type: 5", 2, 0, bytes(5,0,0,5,1,2), "user");
		assertEquals("", replyListener.tracer.get(true));
		assertException("SOCKS5 proxy response status code: 1", 2, 0, bytes(5,1,0,1,1,2,3,4,0,80), "user");
		assertEquals("1;false;1;IPV4;1.2.3.4;80;|", replyListener.tracer.get(true));
		assertException("SOCKS5 proxy response status code: 2", 2, 0, bytes(5,2,0,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,80), "user");
		assertEquals("1;false;2;IPV6;::;80;|", replyListener.tracer.get(true));
		assertException("SOCKS5 proxy response status code: 20", 2, 0, bytes(5,20,0,3,3,48,49,50,0,80), "user");
		assertEquals("1;false;20;DOMAIN;012;80;|", replyListener.tracer.get(true));
	}
	
	@Test
	public void testState() {
		//INIT
		Socks5ProxyHandler p = new Socks5ProxyHandler(uaddr("1.0.0.1",90), "user", "pass");
		p.setSession(new HttpProxyHandlerTest.Session());
		p.read(bytes(5,0));
		assertEquals(0, p.available(bytes(1,2,3), 0, 3));

		p = new Socks5ProxyHandler(uaddr("1.0.0.1",90), "user", "pass");
		p.setSession(new HttpProxyHandlerTest.Session());
		p.read(bytes(5,2));
		assertEquals(2, p.available(bytes(1,2,3), 0, 3));

		p = new Socks5ProxyHandler(uaddr("1.0.0.1",90), (String)null, null);
		p.setSession(new HttpProxyHandlerTest.Session());
		p.read(bytes(5,0));
		assertEquals(0, p.available(bytes(1,2,3), 0, 3));
		
		//PASSWORD
		p = new Socks5ProxyHandler(uaddr("1.0.0.1",90), "user", "pass");
		p.setSession(new HttpProxyHandlerTest.Session());
		p.read(bytes(5,2));
		assertEquals(2, p.available(bytes(1,2,3), 0, 3));
		p.read(bytes(1,0));
		assertEquals(0, p.available(bytes(1,2,3), 0, 3));
		
		//COMMAND
		p = new Socks5ProxyHandler(uaddr("1.0.0.1",90), "user", "pass");
		Session session = new HttpProxyHandlerTest.Session();
		p.setSession(session);
		p.addReplyListener(replyListener);
		p.read(bytes(5,0));
		session.trace.get(true);
		p.read(bytes(5,0,0,1,1,2,3,4,0,80));
		assertEquals("1;true;0;IPV4;1.2.3.4;80;|", replyListener.tracer.get(true));
		assertEquals("DONE|C|", session.trace.get(true));
		assertEquals("Succeeded", p.getReplies()[0].getStatusDescription());
		p.read((byte[])null);
		
		p = new Socks5ProxyHandler(uaddr("1.0.0.1",90), Socks5Command.CONNECT, "user", "pass");
		session = new HttpProxyHandlerTest.Session();
		p.setSession(session);
		p.addReplyListener(replyListener);
		p.read(bytes(5,0));
		session.trace.get(true);
		p.read(bytes(5,0,0,1,1,2,3,4,0,80));
		assertEquals("1;true;0;IPV4;1.2.3.4;80;|", replyListener.tracer.get(true));
		assertEquals("DONE|C|", session.trace.get(true));
		p.read((byte[])null);

		p = new Socks5ProxyHandler(uaddr("1.0.0.1",90), Socks5Command.BIND, "user", "pass");
		session = new HttpProxyHandlerTest.Session();
		p.setSession(session);
		p.addReplyListener(replyListener);
		p.read(bytes(5,0));
		session.trace.get(true);
		p.read(bytes(5,0,0,4,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,0,80));
		assertEquals("1;true;0;IPV6;100::2;80;|", replyListener.tracer.get(true));
		assertEquals("", session.trace.get(true));
		p.read(bytes(5,0,0,4,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,1,1));
		assertEquals("2;true;0;IPV6;102:304:506:708:90a:b0c:d0e:f10;257;|", replyListener.tracer.get(true));
		assertEquals("DONE|C|", session.trace.get(true));
		p.read((byte[])null);

		p = new Socks5ProxyHandler(uaddr("1.0.0.1",90), Socks5Command.UDP_ASSOCIATE, "user", "pass");
		session = new HttpProxyHandlerTest.Session();
		p.setSession(session);
		p.addReplyListener(replyListener);
		p.read(bytes(5,0));
		session.trace.get(true);
		p.read(bytes(5,0,0,3,4,48,49,50,51,0,80));
		assertEquals("1;true;0;DOMAIN;0123;80;|", replyListener.tracer.get(true));
		assertEquals("DONE|C|", session.trace.get(true));
		p.read((byte[])null);
		
		p = new Socks5ProxyHandler(uaddr("1.0.0.1",90), Socks5Command.CONNECT, "user", "pass");
		session = new HttpProxyHandlerTest.Session();
		p.setSession(session);
		p.addReplyListener(replyListener);
		p.read(bytes(5,0));
		session.trace.get(true);
		byte[] bytes = new byte[255+4+1+2];
		bytes[0] = 5;
		bytes[3] = 3;
		bytes[4] = -1;
		bytes[5] = 'a';
		Arrays.fill(bytes, 6, 6+253, (byte)'b');
		bytes[6+253] = (byte)'c';
		bytes[6+255] = 80;
		p.read(bytes);
		bytes = new byte[255];
		Arrays.fill(bytes, (byte)'b');
		bytes[0] = 'a';
		bytes[254] = 'c';
		assertEquals("1;true;0;DOMAIN;"+new String(bytes)+";80;|", replyListener.tracer.get(true));
		assertEquals("DONE|C|", session.trace.get(true));
		assertTrue(SocksDoneState.class == p.state.getClass());
		assertTrue(p.state == p.state.read(null));
		p.state.handleReady();
		assertEquals(0, p.state.readSize());
		p.read((byte[])null);
	}	
	
	@Test
	public void testConnect() throws Exception {
		//client auth, server no auth (IPv4)
		Server s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(5,0), bytes(5,0,0,1,1,2,3,4,0,80)));
		s.start();
		Client c = new Client(PORT);
		c.addPreSession("C", false, new Socks5ProxyHandler(uaddr("127.0.0.1", 80), "user1", "pass1"));
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.session.writenf(new Packet(PacketType.NOP, "1").toBytes());
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|NOP(1)|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		//client auth, server auth (IPv6)
		s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(5,2), bytes(1,0), bytes(5,0,0,4,0,0,0,0,0,0,0,0,0,0,0,0,1,2,3,4,0,80)));
		s.start();
		c = new Client(PORT);
		c.addPreSession("C", false, new Socks5ProxyHandler(uaddr("::127.0.0.2", 80), "user1", "pass1"));
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.session.writenf(new Packet(PacketType.NOP, "1").toBytes());
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|NOP(1)|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		//client no auth, server no auth (DOMAIN)
		s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(5,2), bytes(1,0), bytes(5,0,0,3,3,'a','b','c',0,80)));
		s.start();
		c = new Client(PORT);
		c.addPreSession("C", false, new Socks5ProxyHandler(uaddr("snf4j.org", 80), "user1", "pass1"));
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.session.writenf(new Packet(PacketType.NOP, "1").toBytes());
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|NOP(1)|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		//server failure (negotiation)
		s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(5,255)).fastClose());
		s.start();
		c = new Client(PORT);
		c.exceptionRecordException = true;
		c.addPreSession("C", false, new Socks5ProxyHandler(uaddr("127.0.0.1", 80), "user1", "pass1"));
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|(No acceptable authentication method)|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(5,255)));
		s.start();
		c = new Client(PORT);
		c.exceptionRecordException = true;
		c.addPreSession("C", false, new Socks5ProxyHandler(uaddr("127.0.0.1", 80), "user1", "pass1"));
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|(No acceptable authentication method)|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		//server failure (auth)
		s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(5,2), bytes(1,1)).fastClose());
		s.start();
		c = new Client(PORT);
		c.exceptionRecordException = true;
		c.addPreSession("C", false, new Socks5ProxyHandler(uaddr("127.0.0.1", 80), "user1", "pass1"));
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|(Username/Password authentication response status code: 1)|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(5,2), bytes(1,1)));
		s.start();
		c = new Client(PORT);
		c.exceptionRecordException = true;
		c.addPreSession("C", false, new Socks5ProxyHandler(uaddr("127.0.0.1", 80), "user1", "pass1"));
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|(Username/Password authentication response status code: 1)|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		//server failure (connect)
		s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(5,2), bytes(1,0), bytes(5,1,0,1,1,2,3,4,0,80)).fastClose());
		s.start();
		c = new Client(PORT);
		c.exceptionRecordException = true;
		c.addPreSession("C", false, new Socks5ProxyHandler(uaddr("127.0.0.1", 80), "user1", "pass1"));
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|(SOCKS5 proxy response status code: 1)|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(5,2), bytes(1,0), bytes(5,1,0,1,1,2,3,4,0,80)));
		s.start();
		c = new Client(PORT);
		c.exceptionRecordException = true;
		c.addPreSession("C", false, new Socks5ProxyHandler(uaddr("127.0.0.1", 80), "user1", "pass1"));
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|(SOCKS5 proxy response status code: 1)|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		//proto incomplete
		s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(5,2), bytes(1,0), bytes(5,1,0,1,1,2,3,4,0)).fastClose());
		s.start();
		c = new Client(PORT);
		c.exceptionRecordException = true;
		c.addPreSession("C", false, new Socks5ProxyHandler(uaddr("127.0.0.1", 80), "user1", "pass1"));
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|(Incomplete SOCKS5 proxy protocol)|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}

	@Test
	public void testBind() throws Exception {
		Server s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(5,0), bytes(5,0,0,1,1,2,3,4,0,80,5,0,0,1,10,20,30,40,1,80)));
		s.start();
		Client c = new Client(PORT);
		Socks5ProxyHandler p = new Socks5ProxyHandler(uaddr("127.0.0.1", 80), Socks5Command.BIND, "user1", "pass1");
		p.addReplyListener(replyListener);
		c.addPreSession("C", false, p);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		assertEquals("1;true;0;IPV4;1.2.3.4;80;|2;true;0;IPV4;10.20.30.40;336;|", replyListener.tracer.get(true));
		c.session.writenf(new Packet(PacketType.NOP, "1").toBytes());
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|NOP(1)|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(5,0), bytes(5,0,0,1,1,2,3,4,0,80,5,1,0,1,10,20,30,40,1,80)).fastClose());
		s.start();
		c = new Client(PORT);
		p = new Socks5ProxyHandler(uaddr("127.0.0.1", 80), Socks5Command.BIND, "user1", "pass1");
		p.addReplyListener(replyListener);
		c.addPreSession("C", false, p);
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SEN|", s.getRecordedData(true));
		assertEquals("1;true;0;IPV4;1.2.3.4;80;|2;false;1;IPV4;10.20.30.40;336;|", replyListener.tracer.get(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	
		s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(5,0), bytes(5,2,0,1,1,2,3,4,0,80)).fastClose());
		s.start();
		c = new Client(PORT);
		p = new Socks5ProxyHandler(uaddr("127.0.0.1", 80), Socks5Command.BIND, "user1", "pass1");
		p.addReplyListener(replyListener);
		c.addPreSession("C", false, p);
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SEN|", s.getRecordedData(true));
		assertEquals("1;false;2;IPV4;1.2.3.4;80;|", replyListener.tracer.get(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}

	@Test
	public void testUdpAssociate() throws Exception {
		Server s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(5,0), bytes(5,0,0,1,1,2,3,4,0,80)));
		s.start();
		Client c = new Client(PORT);
		Socks5ProxyHandler p = new Socks5ProxyHandler(uaddr("127.0.0.1", 80), Socks5Command.UDP_ASSOCIATE, "user1", "pass1");
		p.addReplyListener(replyListener);
		c.addPreSession("C", false, p);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		assertEquals("1;true;0;IPV4;1.2.3.4;80;|", replyListener.tracer.get(true));
		c.session.writenf(new Packet(PacketType.NOP, "1").toBytes());
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|NOP(1)|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(5,0), bytes(5,0,0,1,1,2,3,4,0,80)));
		s.start();
		c = new Client(PORT);
		p = new Socks5ProxyHandler(uaddr("0.0.0.0", 0), Socks5Command.UDP_ASSOCIATE, "user1", "pass1");
		p.addReplyListener(replyListener);
		c.addPreSession("C", false, p);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		assertEquals("1;true;0;IPV4;1.2.3.4;80;|", replyListener.tracer.get(true));
		c.session.writenf(new Packet(PacketType.NOP, "1").toBytes());
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|NOP(1)|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}
	
	class ProxyHandler extends AbstractStreamHandler {

		byte[][] responses;
		
		int responseCount;
		
		boolean fastClose;
		
		ProxyHandler(byte[]... responses) {
			this.responses = responses;
		}
		
		ProxyHandler fastClose() {
			fastClose = true;
			return this;
		}
		
		@Override
		public void read(Object msg) {
			if (responseCount >= responses.length) {
				return;
			}
			getSession().write(responses[responseCount++]);
			if (responseCount == responses.length) {
				if (fastClose) {
					getSession().getPipeline().markClosed();
				}
				getSession().close();
			}
		}
	}

}
