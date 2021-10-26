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

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;

import org.junit.Test;
import org.snf4j.core.TestSession;
import org.snf4j.core.TraceBuilder;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISessionPipeline;
import org.snf4j.core.session.IStreamSession;

public class HttpProxyHandlerTest {

	@Test()
	public void testContructor() throws Exception {
		try {
			new HttpProxyHandler(null);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("uri is null", e.getMessage());
		}
		try {
			new HttpProxyHandler(null, 10);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("uri is null", e.getMessage());
		}
		try {
			new HttpProxyHandler(new URI("http://host"), -10);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("connectionTimeout is negative", e.getMessage());
		}
	}
	
	byte[] bytes(String s, int off, int pad) {
		byte[] b = s.replace("|", "\r\n").getBytes();
		byte[] bytes = new byte[off+b.length+pad];
		
		System.arraycopy(b, 0, bytes, off, b.length);
		return bytes;
	}
	
	void assertAvailable(int expected, HttpProxyHandler p, String s) {
		byte[] b;
		
		b = bytes(s, 0, 0);
		assertEquals(expected, p.available(b, 0, b.length));
		b = bytes(s, 10, 0);
		assertEquals(expected, p.available(b, 10, b.length-10));
		b = bytes(s, 0, 8);
		assertEquals(expected, p.available(b, 0, b.length-8));
		b = bytes(s, 2, 8);
		assertEquals(expected, p.available(b, 2, b.length-10));
	}
	
	@Test
	public void testAvailable() throws Exception {
		HttpProxyHandler p = new HttpProxyHandler(new URI("http://host"));
		
		for (int i=0; i<2; ++i) {
			assertAvailable(0, p, "");
			assertAvailable(2, p, "|");
			assertAvailable(4, p, "||");
			assertAvailable(0, p, "A");
			assertAvailable(0, p, "AB");
			assertAvailable(5, p, "ABC|");
			assertAvailable(7, p, "ABC||");
			assertAvailable(0, p, "ABC\r");
			assertAvailable(0, p, "ABC\rD");
			assertAvailable(5, p, "ABC\r\n");
			assertAvailable(5, p, "ABC\r\nD");
			assertAvailable(5, p, "ABC\r\n\r");
			assertAvailable(5, p, "ABC\r\n\rD\n");
			assertAvailable(7, p, "ABC\r\n\r\n");
			assertAvailable(0, p, "ABC\rXXXXX");
			assertAvailable(0, p, "ABC\rDXXXXX");
			assertAvailable(5, p, "ABC\r\nXXXXX");
			assertAvailable(5, p, "ABC\r\nDXXXXX");
			assertAvailable(5, p, "ABC\r\n\rXXXXX");
			assertAvailable(5, p, "ABC\r\n\rD\nXXXXX");
			assertAvailable(7, p, "ABC\r\n\r\nXXXXX");
			p = new HttpProxyHandler(new URI("http://host"), 100);
		}
		
		p = new HttpProxyHandler(new URI("http://host"), true);
		for (int i=0; i<2; ++i) {
			assertAvailable(0, p, "");
			assertAvailable(2, p, "|");
			assertAvailable(1, p, "\n");
			assertAvailable(4, p, "||");
			assertAvailable(3, p, "|\n");
			assertAvailable(3, p, "\n|");
			assertAvailable(3, p, "A|");
			assertAvailable(2, p, "A\n");
			assertAvailable(5, p, "A||");
			assertAvailable(4, p, "A|\n");
			assertAvailable(4, p, "A\n|");	
			assertAvailable(3, p, "A|X");
			assertAvailable(2, p, "A\nX");
			assertAvailable(5, p, "A||X");
			assertAvailable(4, p, "A|\nX");
			assertAvailable(4, p, "A\n|X");
			p = new HttpProxyHandler(new URI("http://host"), 100, true);
		}	
	}
	
	@Test
	public void testRead() throws Exception {
		HttpProxyHandler p = new HttpProxyHandler(new URI("http://host:80"));
		Session s = new Session();
		p.setSession(s);
		p.read((Object)null);
		assertEquals("", s .getTrace());
		p.read("HTTP/1.1 200 OK\r\n\r\n".getBytes());
		assertEquals("DONE|C|", s .getTrace());

		p = new HttpProxyHandler(new URI("http://host:80"));
		p.setSession(s);
		ByteBuffer bb = ByteBuffer.allocate(100);
		bb.put("HTTP/1.1 200 OK\r\n\r\n".getBytes()).flip();
		p.read(bb);
		assertEquals("R|DONE|C|", s.getTrace());
		assertTrue(bb == s.released);
		
		p = new HttpProxyHandler(new URI("http://host:80"));
		p.setSession(s);
		p.read("HTTP/1.1 200 OK\r\nUser-Agent: Unknown/0.0\r\n\r\n".getBytes());
		assertEquals("DONE|C|", s.getTrace());

		p = new HttpProxyHandler(new URI("http://host:80"));
		p.setSession(s);
		try {
			p.read("HTTP/1.1 200 OK\nUser-Agent: Unknown/0.0\r\n\n".getBytes());
			fail();
		}
		catch (ProxyConnectionException e) {
			assertEquals("Unexpected internal EOF handling", e.getMessage());
		}
		assertEquals("DONE|", s.getTrace());

		p = new HttpProxyHandler(new URI("http://host:80"), true);
		p.setSession(s);
		p.read("HTTP/1.1 200 OK\nUser-Agent: Unknown/0.0\r\n\n".getBytes());
		assertEquals("DONE|C|", s.getTrace());
		
		p = new HttpProxyHandler(new URI("http://host:80"));
		p.setSession(s);
		p.read("HTTP/1.1 200 OK\r\n".getBytes());
		assertEquals("DONE|", s.getTrace());
		p.read("\r\n".getBytes());
		assertEquals("C|", s.getTrace());

		p = new HttpProxyHandler(new URI("http://host:80"));
		p.setSession(s);
		p.read("HTTP/1.1 200 OK OK OK\r\n".getBytes());
		assertEquals("DONE|", s.getTrace());
		p.read("User-Agent: Unknown/0.0\r\n".getBytes());
		assertEquals("", s.getTrace());
		p.read("\r\n".getBytes());
		assertEquals("C|", s.getTrace());
	}
	
	void assertException(String expected, String data) throws Exception {
		HttpProxyHandler p = new HttpProxyHandler(new URI("http://host:80"));
		Session s = new Session();
		p.setSession(s);
		try {
			p.read(data.getBytes());
		}
		catch (ProxyConnectionException e) {
			assertEquals(expected, e.getMessage());
		}
	}
	
	@Test
	public void testReadExceptions() throws Exception {
		assertException("Unexpected HTTP proxy response version", "HTTP/1.2 200 OK\r\n\r\n");
		assertException("Invalid HTTP proxy response", "HTTP/1.2 200\r\n\r\n");
		assertException("Invalid HTTP proxy response", "\r\n");
		assertException("Invalid HTTP proxy response", "XXX\r\n\r\n");
		assertException("Invalid status code format in HTTP proxy response", "HTTP/1.1 AAA OK\r\n\r\n");
		assertException("HTTP proxy response status code: 201", "HTTP/1.1 201 OK\r\n\r\n");	
	}
	
	void assertConnect(URI uri, String expected) {
		HttpProxyHandler p = new HttpProxyHandler(uri);
		Session s = new Session();
		p.setSession(s);
		
		p.event(SessionEvent.READY);
		assertEquals(expected, s.getTrace());
	}

	void assertConnect(String uri, String expected) throws Exception {
		assertConnect(new URI(uri), expected);
	}
	
	@Test
	public void testConnectRequest() throws Exception {
		assertConnect("http://host:90", "WR=CONNECT host:90 HTTP/1.1;Host: host:90;;|");
		assertConnect("http://host:80", "WR=CONNECT host:80 HTTP/1.1;Host: host;;|");
		assertConnect("http://host", "WR=CONNECT host:80 HTTP/1.1;Host: host;;|");
		assertConnect("https://host:80", "WR=CONNECT host:80 HTTP/1.1;Host: host:80;;|");
		assertConnect("https://host:443", "WR=CONNECT host:443 HTTP/1.1;Host: host;;|");
		assertConnect("https://host", "WR=CONNECT host:443 HTTP/1.1;Host: host;;|");

		assertConnect("ws://host:90", "WR=CONNECT host:90 HTTP/1.1;Host: host:90;;|");
		assertConnect("ws://host:80", "WR=CONNECT host:80 HTTP/1.1;Host: host:80;;|");
		assertConnect("ws://host", "WR=CONNECT host HTTP/1.1;Host: host;;|");

		HttpProxyHandler p = new HttpProxyHandler(new URI(null, null, "", ""));
		Session s = new Session();
		p.setSession(s);
		try {
			p.handleReady();
			fail();
		}
		catch (ProxyConnectionException e) {
			assertEquals("Undefined host", e.getMessage());
		}
	}
	
	@Test
	public void testAppendHeader() throws Exception {
		HttpProxyHandler p = new HttpProxyHandler(new URI("http://host:99"));
		Session s = new Session();
		p.setSession(s);
		
		p.appendHeader("User-Agent", "snf4j");
		p.event(SessionEvent.READY);
		assertEquals("WR=CONNECT host:99 HTTP/1.1;Host: host:99;User-Agent: snf4j;;|", s.getTrace());
		p.appendHeader("Proxy-Authorization", "Basic dGVzdDoxMjPCow==");
		p.event(SessionEvent.READY);
		assertEquals("WR=CONNECT host:99 HTTP/1.1;Host: host:99;User-Agent: snf4j;Proxy-Authorization: Basic dGVzdDoxMjPCow==;;|", s.getTrace());
	}
	
	@Test
	public void testGetDefaultPort() throws Exception {
		HttpProxyHandler p = new HttpProxyHandler(new URI("ws://host")) {
			@Override
			protected int getDefaultPort(URI uri) throws Exception {
				if ("ws".equalsIgnoreCase(uri.getScheme())) {
					return 80;
				}
				return super.getDefaultPort(uri);
			}
		};

		Session s = new Session();
		p.setSession(s);
		p.event(SessionEvent.READY);
		assertEquals("WR=CONNECT host:80 HTTP/1.1;Host: host;;|", s.getTrace());
	}
	
	static class Session extends TestSession implements IStreamSession {
		
		final TraceBuilder trace = new TraceBuilder();
		
		ByteBuffer released;
		
		String getTrace() {
			return trace.get(true);
		}
		
		@Override
		public ByteBuffer allocate(int capacity) {
			return ByteBuffer.allocate(capacity);
		}

		@Override
		public void release(ByteBuffer buffer) {
			trace.append("R");
			released = buffer;
		}
		
		@Override
		public IStreamHandler getHandler() {
			return null;
		};

		@Override
		public void close() {
			trace.append("C");
		}

		@Override
		public void quickClose() {
			trace.append("QC");
		}

		@Override
		public void dirtyClose() {
			trace.append("DC");
		}

		@Override
		public ISessionPipeline<IStreamSession> getPipeline() {
			return new ISessionPipeline<IStreamSession>() {

				@Override
				public void addFirst(Object key, IStreamSession session) {
				}

				@Override
				public void addAfter(Object baseKey, Object key, IStreamSession session) {
				}

				@Override
				public void add(Object key, IStreamSession session) {
				}

				@Override
				public void addBefore(Object baseKey, Object key, IStreamSession session) {
				}

				@Override
				public IStreamSession replace(Object oldKey, Object key, IStreamSession session) {
					return null;
				}

				@Override
				public IStreamSession remove(Object key) {
					return null;
				}

				@Override
				public IStreamSession get(Object key) {
					return null;
				}

				@Override
				public IStreamSession getOwner() {
					return null;
				}

				@Override
				public List<Object> getKeys() {
					return null;
				}

				@Override
				public void markClosed() {
				}

				@Override
				public void markClosed(Throwable cause) {
				}

				@Override
				public void markUndone() {
				}

				@Override
				public void markUndone(Throwable cause) {
				}

				@Override
				public void markDone() {
					trace.append("DONE");
				}

				@Override
				public void close() {
				}

				@Override
				public void quickClose() {
				}

				@Override
				public void dirtyClose() {
				}
				
			};
		}

		@Override
		public IFuture<Void> write(byte[] data) {
			trace.append("WR");
			return null;
		}

		@Override
		public void writenf(byte[] data) {
			trace.append("WR");
		}

		@Override
		public IFuture<Void> write(byte[] data, int offset, int length) {
			trace.append("WR");
			return null;
		}

		@Override
		public void writenf(byte[] data, int offset, int length) {
			trace.append("WR");
		}

		@Override
		public IFuture<Void> write(ByteBuffer data) {
			trace.append("WR");
			return null;
		}

		@Override
		public void writenf(ByteBuffer data) {
			byte[] d = new byte[data.remaining()];
			
			data.get(d);
			trace.append("WR=" + new String(d).replace("\r\n", ";"));
		}

		@Override
		public IFuture<Void> write(ByteBuffer data, int length) {
			trace.append("WR");
			return null;
		}

		@Override
		public void writenf(ByteBuffer data, int length) {
			trace.append("WR");
		}

		@Override
		public IFuture<Void> write(Object msg) {
			trace.append("WR");
			return null;
		}

		@Override
		public void writenf(Object msg) {
			trace.append("WR");
		}
		
	}
	
}
