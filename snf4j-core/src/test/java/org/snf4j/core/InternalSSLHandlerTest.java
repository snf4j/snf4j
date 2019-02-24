/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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
package org.snf4j.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;

public class InternalSSLHandlerTest {

	private final static ILogger LOGGER = LoggerFactory.getLogger(SSLSession.class);

	long TIMEOUT = 2000;
	int PORT = 7777;

	Server s;
	Client c;

	private final static int APPBUFSIZE = 100;
	private final static int NETBUFSIZE = 200;
	private final static int APPBUFRATIO = 2;
	private final static int NETBUFRATIO = 3;
	
	TestHandler handler = new TestHandler("Test");
	
	TestSSLEngine engine = new TestSSLEngine();
	
	TestSSLSession session = new TestSSLSession();
	
	
	@Before
	public void before() {
		s = c = null;
		session.appBufferSize = APPBUFSIZE;
		session.netBufferSize = NETBUFSIZE;
		engine.session = session;
		handler.engine = engine;
		handler.maxAppBufRatio = APPBUFRATIO;
		handler.maxNetBufRatio = NETBUFRATIO;
	}
	
	@After
	public void after() throws InterruptedException {
		if (c != null) c.stop(TIMEOUT);
		if (s != null) s.stop(TIMEOUT);
	}
	
	private void waitFor(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}
	
	ByteBuffer getBuffer(InternalSSLHandler handler, String name) throws Exception {
		Field field = handler.getClass().getDeclaredField(name);
		
		field.setAccessible(true);
		return (ByteBuffer) field.get(handler);
	}

	ByteBuffer[] getBuffers(InternalSSLHandler handler, String name) throws Exception {
		Field field = handler.getClass().getDeclaredField(name);
		
		field.setAccessible(true);
		return (ByteBuffer[]) field.get(handler);
	}
	
	void set(Handler handler, String name, Object value) throws Exception {
		Field field = handler.getClass().getSuperclass().getDeclaredField(name);
		
		field.setAccessible(true);
		field.set(handler, value);
	}
	
	@Test
	public void testBuffers() throws Exception {
		InternalSSLHandler h = new InternalSSLHandler(handler, true, LOGGER);

		h.preCreated();
		assertEquals(APPBUFSIZE, getBuffer(h, "inAppBuffer").capacity());
		assertEquals(1, getBuffers(h, "outAppBuffers").length);
		assertEquals(APPBUFSIZE, getBuffers(h, "outAppBuffers")[0].capacity());
		assertEquals(NETBUFSIZE, getBuffer(h, "inNetBuffer").capacity());
		assertEquals(NETBUFSIZE, getBuffer(h, "outNetBuffer").capacity());
	}

	@Test
	public void testCloseMethods() throws Exception {
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		s.start();
		c.start();

		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		SSLSession session = (SSLSession) c.getSession(); 
		
		Handler h = new Handler(handler, true, LOGGER);
		h.setSession(session);
		
		set(h, "closing", ClosingState.SENDING);
		h.close();
		assertEquals(0, h.runCounter);
		set(h, "closing", ClosingState.FINISHING);
		h.close();
		assertEquals(0, h.runCounter);
		set(h, "closing", ClosingState.FINISHED);
		h.close();
		assertEquals(0, h.runCounter);
		set(h, "closing", ClosingState.NONE);
		h.close();
		waitFor(500);
		assertEquals(1, h.runCounter);

		set(h, "closing", ClosingState.FINISHING);
		h.quickClose();
		assertEquals(1, h.runCounter);
		set(h, "closing", ClosingState.FINISHED);
		h.quickClose();
		assertEquals(1, h.runCounter);
		set(h, "closing", ClosingState.SENDING);
		h.quickClose();
		waitFor(500);
		assertEquals(2, h.runCounter);
		set(h, "closing", ClosingState.NONE);
		h.quickClose();
		waitFor(500);
		assertEquals(3, h.runCounter);
	
		set(h, "closing", ClosingState.FINISHING);
		h.dirtyClose();
		assertEquals(3, h.runCounter);
		set(h, "closing", ClosingState.FINISHED);
		h.dirtyClose();
		assertEquals(3, h.runCounter);
		assertTrue(session.isOpen());
		set(h, "closing", ClosingState.SENDING);
		h.dirtyClose();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		waitFor(500);
		assertEquals(4, h.runCounter);
	}
	
	@Test 
	public void testSetAndGetSession() throws Exception {
		InternalSSLHandler h = new InternalSSLHandler(handler, true, LOGGER);
		InternalSSLHandler h2 = new InternalSSLHandler(handler, true, LOGGER);
		assertNull(h.getSession());
		SSLSession session = new SSLSession(h, false);
		assertTrue(session == h.getSession());
		
		session = new SSLSession(h2, false);
		h.setSession(session);
		assertTrue(session == h.getSession());
	}
	
	@Test
	public void testIgnoreRead() throws Exception {
		InternalSSLHandler h = new InternalSSLHandler(handler, true, LOGGER);
		HandshakeStatus[] status = new HandshakeStatus[1];
		
		h.preCreated();
		h.setSession(new SSLSession(h, false));
		
		//10 bytes to unwrap, ready to read 0
		engine.unwrapBytes = 10;
		handler.availableBytes = 0;
		ByteBuffer inNet = getBuffer(h, "inNetBuffer");
		ByteBuffer inApp = getBuffer(h, "inAppBuffer");
		assertEquals(0, inNet.position());
		assertEquals(0, inApp.position());
		inNet.put(ByteUtils.getBytes(15, 1));
		engine.unwrapResult[0] = new SSLEngineResult(SSLEngineResult.Status.CLOSED,
				SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, 0, 0);
		assertTrue(h.unwrap(status));
		h.read(null);
	}
	
	static class Handler extends InternalSSLHandler {
		int runCounter;
		
		Handler(IStreamHandler handler, boolean clientMode, ILogger logger) throws Exception {
			super(handler, clientMode, logger);
		}
		
		@Override
		public void run() {
			++runCounter;
		}		
		
	}
	
}
