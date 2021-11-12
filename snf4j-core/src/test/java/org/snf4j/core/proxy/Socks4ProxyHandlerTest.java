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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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

public class Socks4ProxyHandlerTest {

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

	void assertContructor(Socks4ProxyHandler h, SocketAddress a, Socks4Command command, String username, long timeout, 
			ISessionConfig config, ISessionStructureFactory factory) throws Exception {
		Field f;
		
		assertTrue(h.state.getClass() == Socks4CommandState.class);
		f = Socks4CommandState.class.getDeclaredField("command");
		f.setAccessible(true);
		assertTrue(command == f.get(h.state));
		f = Socks4CommandState.class.getDeclaredField("username");
		f.setAccessible(true);
		assertEquals(username, f.get(h.state));
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
		Socks4Command C = Socks4Command.CONNECT;
		Socks4Command B = Socks4Command.BIND;
		DefaultSessionConfig c = new DefaultSessionConfig();
		DefaultSessionStructureFactory f = new DefaultSessionStructureFactory() {};

		assertContructor(new Socks4ProxyHandler(a, "u1"), a, C, "u1", 10000, null, null);
		assertContructor(new Socks4ProxyHandler(a, "u1", c), a, C, "u1", 10000, c, null);
		assertContructor(new Socks4ProxyHandler(a, "u1", c, f), a, C, "u1", 10000, c, f);
		assertContructor(new Socks4ProxyHandler(a, B, "u2"), a, B, "u2", 10000, null, null);
		assertContructor(new Socks4ProxyHandler(a, B, "u2", c), a, B, "u2", 10000, c, null);
		assertContructor(new Socks4ProxyHandler(a, B, "u2", c, f), a, B, "u2", 10000, c, f);
		assertContructor(new Socks4ProxyHandler(a, "u1", 333), a, C, "u1", 333, null, null);
		assertContructor(new Socks4ProxyHandler(a, "u1", 333, c), a, C, "u1", 333, c, null);
		assertContructor(new Socks4ProxyHandler(a, "u1", 333, c, f), a, C, "u1", 333, c, f);
		assertContructor(new Socks4ProxyHandler(a, B, "u1", 333), a, B, "u1", 333, null, null);
		assertContructor(new Socks4ProxyHandler(a, B, "u1", 333, c), a, B, "u1", 333, c, null);
		
		assertContructor(new Socks4ProxyHandler(a, C, null, c, f), a, C, "", 10000, c, f);
		assertContructor(new Socks4ProxyHandler(a, C, "u1", null, null), a, C, "u1", 10000, null, null);
		assertContructor(new Socks4ProxyHandler(a, C, null, 100, c, f), a, C, "", 100, c, f);
		assertContructor(new Socks4ProxyHandler(a, C, "u1", 100, null, null), a, C, "u1", 100, null, null);
		try {
			new Socks4ProxyHandler(null, C, "u1", c, f);
		}
		catch (IllegalArgumentException e) {
			assertEquals("address is null", e.getMessage());
		}
		try {
			new Socks4ProxyHandler(a, null, "u1", c, f);
		}
		catch (IllegalArgumentException e) {
			assertEquals("command is null", e.getMessage());
		}
		try {
			new Socks4ProxyHandler(null, C, "u1", 100, c, f);
		}
		catch (IllegalArgumentException e) {
			assertEquals("address is null", e.getMessage());
		}
		try {
			new Socks4ProxyHandler(a, null, "u1", 100, c, f);
		}
		catch (IllegalArgumentException e) {
			assertEquals("command is null", e.getMessage());
		}
	}
	
	@Test
	public void testAvailable() {
		Socks4ProxyHandler p = new Socks4ProxyHandler(InetSocketAddress.createUnresolved("1.2.3.4", 80), "user1");
		
		assertEquals(0, p.available(null, 0, 0));
		assertEquals(0, p.available(null, 0, 1));
		assertEquals(0, p.available(null, 0, 7));
		assertEquals(8, p.available(null, 0, 8));
		assertEquals(8, p.available(null, 0, 9));
		
		ByteBuffer b = ByteBuffer.allocate(10);
		assertEquals(0, p.available(b, false));
		b.flip();
		assertEquals(0, p.available(b, true));

		b.clear();
		b.put(new byte[1]);
		assertEquals(0, p.available(b, false));
		b.flip();
		assertEquals(0, p.available(b, true));
		
		b.clear();
		b.put(new byte[7]);
		assertEquals(0, p.available(b, false));
		b.flip();
		assertEquals(0, p.available(b, true));

		b.clear();
		b.put(new byte[8]);
		assertEquals(8, p.available(b, false));
		b.flip();
		assertEquals(8, p.available(b, true));
		
		b.clear();
		b.put(new byte[9]);
		assertEquals(8, p.available(b, false));
		b.flip();
		assertEquals(8, p.available(b, true));
	}
	
	static InetSocketAddress uaddr(String host, int port) {
		InetSocketAddress a = InetSocketAddress.createUnresolved(host, port);
		assertTrue(a.isUnresolved());
		return a;
	}
	
	static InetSocketAddress addr(byte[] addr, int port) throws Exception {
		InetSocketAddress a = new InetSocketAddress(InetAddress.getByAddress(addr), port);
		assertFalse(a.isUnresolved());
		return a;
	}
	
	static byte[] bytes(int... b) {
		byte[] bytes = new byte[b.length];
		
		for (int i=0; i<b.length; ++i) {
			bytes[i] = (byte)b[i];
		}
		return bytes;
	}
	
	@Test
	public void testEvent() {
		Socks4ProxyHandler p = new Socks4ProxyHandler(uaddr("1.2.3.4", 80), "user1");
		Session session = new HttpProxyHandlerTest.Session();
		p.setSession(session);
		p.event(SessionEvent.CREATED);
		assertEquals("", session.trace.get(true));
		p.event(SessionEvent.OPENED);
		assertEquals("UNDONE(Incomplete SOCKS4 proxy protocol)|", session.trace.get(true));
	}
	
	static String untag(String s) {
		for(;;) {
			int p0 = s.indexOf('<');
			int p1 = s.indexOf('>');
			if (p0 != -1 && p1 != -1) {
				String tag = s.substring(p0, p1+1);
				String s2 = Arrays.toString(s.substring(p0+1, p1).getBytes(StandardCharsets.US_ASCII));
				s2 = s2.replace("[", "").replace("]", "").replace(" ", "");
				s = s.replace(tag, s2);
			}
			else {
				break;
			}
		}
		return s;
	}
	
	void assertHandleReady(InetSocketAddress addr, boolean connect, String user, String expected, boolean optimized) {
		Socks4ProxyHandler p = new Socks4ProxyHandler(addr, connect ? Socks4Command.CONNECT : Socks4Command.BIND, user);
		Session session = new HttpProxyHandlerTest.Session();
		session.writeBytes = true;
		session.dataCopyingOptimized = optimized;
		p.setSession(session);
		p.event(SessionEvent.READY);
		String trace = session.trace.get(true);
		expected = untag(expected);
		trace = trace.replace(" ", "");
		assertEquals(expected, trace);
	}
	
	@Test
	public void testHandleReady() throws Exception {
		assertHandleReady(uaddr("1.2.3.4", 80), true, "user1", "WR=[4,1,0,80,1,2,3,4,<user1>,0]|R|", false);
		assertHandleReady(uaddr("0.0.0.0", 80), false, "user1", "WR=[4,2,0,80,0,0,0,0,<user1>,0]|", true);
		assertHandleReady(uaddr("255.255.255.255", 0x7fff), true, "user1", "WR=[4,1,127,-1,-1,-1,-1,-1,<user1>,0]|R|", false);
		assertHandleReady(uaddr("255.255.255.255", 0xff00), true, "user1", "WR=[4,1,-1,0,-1,-1,-1,-1,<user1>,0]|R|", false);
		assertHandleReady(uaddr("localhost", 256), true, "user1", "WR=[4,1,1,0,0,0,0,1,<user1>,0,<localhost>,0]|R|", false);
		assertHandleReady(uaddr("x.x.snf4j.org", 256), true, "user1", "WR=[4,1,1,0,0,0,0,1,<user1>,0,<x.x.snf4j.org>,0]|R|", false);
		assertHandleReady(addr(bytes(127,0,0,1), 256), true, "user1", "WR=[4,1,1,0,127,0,0,1,<user1>,0]|R|", false);
		assertHandleReady(addr(bytes(127,0,0,1), 256), true, null, "WR=[4,1,1,0,127,0,0,1,0]|R|", false);
		assertHandleReady(addr(bytes(127,0,0,1), 256), true, "", "WR=[4,1,1,0,127,0,0,1,0]|R|", false);
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
	
	void assertException(String expected, boolean connect, byte[] data, String cause) {
		Socks4ProxyHandler p = new Socks4ProxyHandler(uaddr("1.0.0.1",90), connect ? Socks4Command.CONNECT : Socks4Command.BIND, "user1");
		p.addReplyListener(replyListener);
		p.setSession(new HttpProxyHandlerTest.Session());
		try {
			p.read(data);
			fail();
		}
		catch (Exception e) {
			assertEquals(expected, e.getMessage());
			assertTrue(e.getClass() == ProxyConnectionException.class);
			if (cause != null) {
				assertEquals(cause, e.getCause().getMessage());
			}
		}
	}
	
	@Test
	public void testReadException() {
		assertException("Unsupported SOCKS4 reply version: 1 (expected: 0)", true, bytes(1,90,0,80,1,2,3,4), null);
		assertEquals("", replyListener.tracer.get(true));
		assertException("SOCKS4 proxy response status code: 89", true, bytes(0,89,0,80,1,2,3,4), null);
		assertEquals("1;false;89;IPV4;1.2.3.4;80;|", replyListener.tracer.get(true));
		assertException("SOCKS4 proxy response status code: 94", true, bytes(0,94,0,80,1,2,3,4), null);
		assertEquals("1;false;94;IPV4;1.2.3.4;80;|", replyListener.tracer.get(true));
		assertException("SOCKS4 proxy response status code: 91", true, bytes(0,91,0,80,1,2,3,4), null);
		assertEquals("1;false;91;IPV4;1.2.3.4;80;|", replyListener.tracer.get(true));
		assertException("SOCKS4 proxy response status code: 92", true, bytes(0,92,0,80,1,2,3,4), null);
		assertEquals("1;false;92;IPV4;1.2.3.4;80;|", replyListener.tracer.get(true));
		assertException("SOCKS4 proxy response status code: 93", true, bytes(0,93,0,80,1,2,3,4), null);
		assertEquals("1;false;93;IPV4;1.2.3.4;80;|", replyListener.tracer.get(true));
	}
	
	@Test
	public void testRead() {
		Socks4ProxyHandler p = new Socks4ProxyHandler(uaddr("1.0.0.1",90), Socks4Command.CONNECT, "user1");
		p.addReplyListener(replyListener);
		assertEquals(0, p.getReplies().length);
		Session session = new HttpProxyHandlerTest.Session();
		p.setSession(session);
		
		p.read(bytes(0,90,0,88,1,2,3,4));
		assertEquals("1;true;90;IPV4;1.2.3.4;88;|", replyListener.tracer.get(true));
		assertEquals("DONE|C|", session.trace.get(true));
		assertEquals(1, p.getReplies().length);
		
		p = new Socks4ProxyHandler(uaddr("1.0.0.1",90), Socks4Command.BIND, "user1");
		p.addReplyListener(replyListener);
		session = new HttpProxyHandlerTest.Session();
		p.setSession(session);
		
		p.read(bytes(0,90,0,88,1,2,3,4));
		assertEquals("1;true;90;IPV4;1.2.3.4;88;|", replyListener.tracer.get(true));
		assertEquals("", session.trace.get(true));
		assertEquals(1, p.getReplies().length);
		assertEquals("1.2.3.4", p.getReplies()[0].getAddress());

		ReplyListener listener2 = new ReplyListener();
		p.addReplyListener(listener2);
		p.read(ByteBuffer.wrap(bytes(0,90,0,89,11,22,33,44)));
		assertEquals("2;true;90;IPV4;11.22.33.44;89;|", replyListener.tracer.get(true));
		assertEquals("2;true;90;IPV4;11.22.33.44;89;|", listener2.tracer.get(true));
		assertEquals("R|DONE|C|", session.trace.get(true));
		assertEquals(2, p.getReplies().length);
		assertEquals("1.2.3.4", p.getReplies()[0].getAddress());
		assertEquals("11.22.33.44", p.getReplies()[1].getAddress());
		assertEquals("Request granted", p.getReplies()[0].getStatusDescription());

		p = new Socks4ProxyHandler(uaddr("1.0.0.1",90), Socks4Command.BIND, "user1");
		session = new HttpProxyHandlerTest.Session();
		p.setSession(session);

		p.read(bytes(0,90,0,88,1,2,3,4));
		assertEquals("", session.trace.get(true));
		p.addReplyListener(listener2);
		try {
			p.read(ByteBuffer.wrap(bytes(0,91,0,89,11,22,33,44)));
			fail();
		}
		catch (ProxyConnectionException e) {
		}
		assertEquals("2;false;91;IPV4;11.22.33.44;89;|", listener2.tracer.get(true));
		assertEquals(2, p.getReplies().length);
		assertEquals("1.2.3.4", p.getReplies()[0].getAddress());
		assertEquals("11.22.33.44", p.getReplies()[1].getAddress());
		assertEquals("Request rejected or failed", p.getReplies()[1].getStatusDescription());
		
		p.read((Object)null);
	}
	
	@Test
	public void testConnect() throws Exception {
		Server s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(0,90,0,10,1,2,3,4)));
		s.start();
		Client c = new Client(PORT);
		c.addPreSession("C", false, new Socks4ProxyHandler(uaddr("127.0.0.1", 80), "user1"));
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

		s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(0,90,0,10,1,2,3,4,0,3,PacketType.NOP.ordinal())));
		s.start();
		c = new Client(PORT);
		c.addPreSession("C", false, new Socks4ProxyHandler(uaddr("snf4j.org", 80), "user1"));
		c.start();
		c.waitForSessionReady(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(0,91,0,10,1,2,3,4)));
		s.start();
		c = new Client(PORT);
		c.exceptionRecordException = true;
		c.addPreSession("C", false, new Socks4ProxyHandler(uaddr("127.0.0.1", 80), "user1"));
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|(SOCKS4 proxy response status code: 91)|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(0,91,0,10,1,2,3,4)).fastClose());
		s.start();
		c = new Client(PORT);
		c.exceptionRecordException = true;
		c.addPreSession("C", false, new Socks4ProxyHandler(uaddr("127.0.0.1", 80), "user1"));
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|(SOCKS4 proxy response status code: 91)|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(0,90,0,10,1,2,3,4)).fastClose());
		s.start();
		c = new Client(PORT);
		c.exceptionRecordException = true;
		c.addPreSession("C", false, new Socks4ProxyHandler(uaddr("127.0.0.1", 80), "user1"));
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|RDY|SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	
		s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(0,90,0,10,1,2,3)).fastClose());
		s.start();
		c = new Client(PORT);
		c.exceptionRecordException = true;
		c.addPreSession("C", false, new Socks4ProxyHandler(uaddr("127.0.0.1", 80), "user1"));
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|(Incomplete SOCKS4 proxy protocol)|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}
	
	@Test
	public void testBind() throws Exception {
		Server s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(0,90,0,10,1,2,3,4,0,90,0,11,10,2,3,4)));
		s.start();
		Client c = new Client(PORT);
		Socks4ProxyHandler p = new Socks4ProxyHandler(uaddr("127.0.0.1", 80), Socks4Command.BIND, "user1");
		p.addReplyListener(replyListener);
		c.addPreSession("C", false, p);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		assertEquals("1;true;90;IPV4;1.2.3.4;10;|2;true;90;IPV4;10.2.3.4;11;|", replyListener.tracer.get(true));
		c.session.writenf(new Packet(PacketType.NOP, "1").toBytes());
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|NOP(1)|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(0,91,0,10,1,2,3,4)).fastClose());
		s.start();
		c = new Client(PORT);
		p = new Socks4ProxyHandler(uaddr("127.0.0.1", 80), Socks4Command.BIND, "user1");
		p.addReplyListener(replyListener);
		c.addPreSession("C", false, p);
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SEN|", s.getRecordedData(true));
		assertEquals("1;false;91;IPV4;1.2.3.4;10;|", replyListener.tracer.get(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		s = new Server(PORT);
		s.addPreSession("S", false, new ProxyHandler(bytes(0,90,0,10,1,2,3,4,0,92,0,11,10,2,3,4)).fastClose());
		s.start();
		c = new Client(PORT);
		p = new Socks4ProxyHandler(uaddr("127.0.0.1", 80), Socks4Command.BIND, "user1");
		p.addReplyListener(replyListener);
		c.addPreSession("C", false, p);
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SEN|", s.getRecordedData(true));
		assertEquals("1;true;90;IPV4;1.2.3.4;10;|2;false;92;IPV4;10.2.3.4;11;|", replyListener.tracer.get(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}
	
	class ProxyHandler extends AbstractStreamHandler {

		byte[] response;
		
		boolean fastClose;
		
		ProxyHandler(byte[] response) {
			this.response = response;
		}
		
		ProxyHandler fastClose() {
			fastClose = true;
			return this;
		}
		
		@Override
		public void read(Object msg) {
			getSession().write(response);
			if (fastClose) {
				getSession().getPipeline().markClosed();
			}
			getSession().close();
		}
	}
}
