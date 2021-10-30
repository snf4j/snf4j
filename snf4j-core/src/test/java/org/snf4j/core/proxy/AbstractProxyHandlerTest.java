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

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Test;
import org.snf4j.core.Client;
import org.snf4j.core.Server;
import org.snf4j.core.TraceBuilder;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.timer.ITimer;
import org.snf4j.core.timer.TestTimer;

public class AbstractProxyHandlerTest {

	long TIMEOUT = 2000;
	
	int PORT = 7777;

	Server s;

	Client c;

	@After
	public void after() throws InterruptedException {
		if (c != null) c.stop(TIMEOUT);
		if (s != null) s.stop(TIMEOUT);
	}
	
	void waitFor(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}

	@Test
	public void testDefaultTimeout() throws Exception {
		TestHandler p = new TestHandler();
		
		Field f = AbstractProxyHandler.class.getDeclaredField("connectionTimeout");
		f.setAccessible(true);
		assertEquals(10000, f.getLong(p));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNegativeTimeout() {
		new TestHandler(-1);
	}
	
	@Test
	public void testConstructor() {
		AbstractProxyHandler h = new AbstractProxyHandler(10, null, null) {

			@Override
			public void read(Object msg) {
			}

			@Override
			protected void handleReady() throws Exception {
			}
		};
		assertTrue(h.getConfig().getClass() == DefaultSessionConfig.class);
		assertTrue(h.getFactory() == DefaultSessionStructureFactory.DEFAULT);
		
		DefaultSessionConfig config = new DefaultSessionConfig();
		DefaultSessionStructureFactory factory = new DefaultSessionStructureFactory() {};
		h = new AbstractProxyHandler(10, config, factory) {

			@Override
			public void read(Object msg) {
			}

			@Override
			protected void handleReady() throws Exception {
			}
		};
		assertTrue(h.getConfig() == config);
		assertTrue(h.getFactory() == factory);

		h = new AbstractProxyHandler(null, null) {

			@Override
			public void read(Object msg) {
			}

			@Override
			protected void handleReady() throws Exception {
			}
		};
		assertTrue(h.getConfig().getClass() == DefaultSessionConfig.class);
		assertTrue(h.getFactory() == DefaultSessionStructureFactory.DEFAULT);
		
		h = new AbstractProxyHandler(config, factory) {

			@Override
			public void read(Object msg) {
			}

			@Override
			protected void handleReady() throws Exception {
			}
		};
		assertTrue(h.getConfig() == config);
		assertTrue(h.getFactory() == factory);
	}
	
	@Test
	public void testAvailable() {
		TestHandler p = new TestHandler();
		ByteBuffer b = ByteBuffer.allocate(100);
		
		b.put("ABCD".getBytes());
		assertEquals(4, p.available(b, false));
		assertEquals("ABCD", p.available);
		assertTrue(b.array() == p.buffer);
		b.flip();
		assertEquals(4, p.available(b, true));
		assertEquals("ABCD", p.available);
		assertTrue(b.array() == p.buffer);
		b.get();
		assertEquals(3, p.available(b, true));
		assertEquals("BCD", p.available);
		assertTrue(b.array() == p.buffer);
		
		b = ByteBuffer.allocateDirect(100);
		b.put("EFGHIJ".getBytes());
		assertEquals(6, p.available(b, false));
		assertEquals("EFGHIJ", p.available);
		b.flip();
		assertEquals(6, p.available(b, true));
		assertEquals("EFGHIJ", p.available);
		b.get();
		assertEquals(5, p.available(b, true));
		assertEquals("FGHIJ", p.available);
	}
	
	@Test
	public void testConnect() throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);
		TestHandler h = new TestHandler();
		
		//successful connection
		c.addPreSession("C0", false, h);
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		assertEquals("R|", h.trace.get(true));
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);

		//successful connection (cancel timer)
		h = new TestHandler(200);
		h.timer = new TestTimer();
		c = new Client(PORT);
		c.addPreSession("C0", false, h);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		waitFor(500);
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		
		//failed connection
		h = new TestHandler(1000);
		h.readyException = new Exception("E");
		c = new Client(PORT);
		c.addPreSession("C0", false, h);
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|SCL|SEN|", s.getRecordedData(true));
		assertEquals("R|E|", h.trace.get(true));
		c.stop(TIMEOUT);
		
		//timed out connection
		h = new TestHandler(1000);
		h.skipClose = true;
		h.timer = new TestTimer();
		c = new Client(PORT);
		c.addPreSession("C0", false, h);
		c.start();
		waitFor(500);
		assertEquals("", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		waitFor(600);
		assertEquals("SCR|EXC|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		c.stop(TIMEOUT);

		//timed out connection
		h = new TestHandler(1000);
		h.skipClose = true;
		c = new Client(PORT);
		c.addPreSession("C0", false, h);
		c.start();
		waitFor(500);
		assertEquals("", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		waitFor(600);
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		c.stop(TIMEOUT);
		
		//connection with 0 timeout
		waitFor(100);
		s.getRecordedData(true);
		h = new TestHandler(0);
		h.skipClose = true;
		h.timer = new TestTimer();
		c = new Client(PORT);
		c.addPreSession("C0", false, h);
		c.start();
		Object key = c.registeredSession.getPipeline().getKeys().get(0);
		c.registeredSession.getPipeline().get(key).getTimer().scheduleEvent(new Object(), 100);
		waitFor(500);
		assertEquals("", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		waitFor(600);
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		c.stop(TIMEOUT);
		
	}	
		
	class TestHandler extends AbstractProxyHandler {

		final TraceBuilder trace = new TraceBuilder();

		String available;
		
		byte[] buffer;
		
		volatile Exception readyException;
		
		volatile boolean skipClose;
		
		volatile ITimer timer;
		
		TestHandler(long connectionTimeout) {
			super(connectionTimeout);
		}
		
		TestHandler() {
		}
		
		@Override
		public int available(byte[] buffer, int off, int len) {
			available = new String(buffer, off, len);
			this.buffer = buffer;
			return len;
		}

		@Override
		public void read(Object msg) {
		}

		@Override
		protected void handleReady() throws Exception {
			trace.append("R");
			if (readyException != null) {
				trace.append("E");
				throw readyException;
			}
			if (!skipClose) {
				getSession().close();
			}
		}
	
		@Override
		public ISessionStructureFactory getFactory() {
			return new Factory(); 
		}
		
		class Factory extends DefaultSessionStructureFactory {
			
			@Override
			public ITimer getTimer() {
				return timer;
			}
		}
	}
}
