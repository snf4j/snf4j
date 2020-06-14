/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020 SNF4J contributors
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
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.AbstractEngineHandler.Handshake;
import org.snf4j.core.engine.EngineResult;
import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.core.engine.IEngine;
import org.snf4j.core.engine.Status;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.core.timer.DefaultTimer;

public class EngineStreamHandlerTest {

	private final static ILogger LOGGER = LoggerFactory.getLogger(SSLSession.class);

	long TIMEOUT = 2000;
	int PORT = 7777;

	Server s;
	Client c;

	static final String CLIENT_RDY_TAIL = SSLSessionTest.CLIENT_RDY_TAIL;
	
	private final static int APPBUFSIZE = 100;
	private final static int NETBUFSIZE = 200;
	private final static int APPBUFRATIO = 2;
	private final static int NETBUFRATIO = 3;
	
	TestHandler handler = new TestHandler("Test");
	
	TestEngine engine = new TestEngine();
	
	TestSSLEngine sslEngine = new TestSSLEngine();
	
	TestSSLSession session = new TestSSLSession();
	
	@Before
	public void before() {
		s = c = null;
		engine.appBufferSize = APPBUFSIZE;
		engine.netBufferSize = NETBUFSIZE;
		sslEngine.session = session;
		handler.engine = sslEngine;
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
	
	ByteBuffer getBuffer(EngineStreamHandler handler, String name) throws Exception {
		Field field = handler.getClass().getDeclaredField(name);
		
		field.setAccessible(true);
		return (ByteBuffer) field.get(handler);
	}

	ByteBuffer[] getBuffers(EngineStreamHandler handler, String name) throws Exception {
		Field field = handler.getClass().getDeclaredField(name);
		
		field.setAccessible(true);
		return (ByteBuffer[]) field.get(handler);
	}
	
	void set(Handler handler, String name, Object value) throws Exception {
		Field field = AbstractEngineHandler.class.getDeclaredField(name);
		
		field.setAccessible(true);
		field.set(handler, value);
	}
	
	@Test
	public void testBuffers() throws Exception {
		EngineStreamHandler h = new EngineStreamHandler(engine, handler, LOGGER);

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
		
		Handler h = new Handler(engine, handler, LOGGER);
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
	
		assertTrue(session.isOpen());
		set(h, "closing", ClosingState.FINISHING);
		h.dirtyClose();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals(3, h.runCounter);

		c = new Client(PORT, true);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		session = (SSLSession) c.getSession(); 
		h.setSession(session);
		assertTrue(session.isOpen());
		set(h, "closing", ClosingState.FINISHED);
		h.dirtyClose();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals(3, h.runCounter);

		c = new Client(PORT, true);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		session = (SSLSession) c.getSession(); 
		h.setSession(session);
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
		EngineStreamHandler h = new EngineStreamHandler(engine, handler, LOGGER);
		EngineStreamHandler h2 = new EngineStreamHandler(engine, handler, LOGGER);
		String prefix = "org.snf4j.core.EngineStreamHandler[session=";
		assertNull(h.getSession());
		assertEquals(prefix+"null]", h.toString());
		SSLSession session = new SSLSession(h, false);
		assertTrue(session == h.getSession());
		assertEquals(prefix+session+"]", h.toString());
		
		session = new SSLSession(h2, false);
		h.setSession(session);
		assertTrue(session == h.getSession());
	}
	
	@Test
	public void testIgnoreRead() throws Exception {
		EngineStreamHandler h = new EngineStreamHandler(engine, handler, LOGGER);
		HandshakeStatus[] status = new HandshakeStatus[1];
		
		h.preCreated();
		h.setSession(new SSLSession(h, false));
		
		//10 bytes to unwrap, ready to read 0
		sslEngine.unwrapBytes = 10;
		handler.availableBytes = 0;
		ByteBuffer inNet = getBuffer(h, "inNetBuffer");
		ByteBuffer inApp = getBuffer(h, "inAppBuffer");
		assertEquals(0, inNet.position());
		assertEquals(0, inApp.position());
		inNet.put(ByteUtils.getBytes(15, 1));
		engine.unwrapResult = new EngineResult(Status.CLOSED,
				HandshakeStatus.NOT_HANDSHAKING, 0, 0);
		assertTrue(h.unwrap(status));
		h.read((byte[])null);
		h.read((Object)null);

	}
	
	@Test
	public void testReadHandlerWithException() throws Exception {
		s = new Server(PORT, true);
		s.throwInRead = true;
		c = new Client(PORT, true);
		s.start();
		c.start();

		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		c.write(new Packet(PacketType.ECHO));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertEquals("DS|DR|ECHO()|EXC|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
	}
	
	@Test
	public void testTimer() throws Exception {
		s = new Server(PORT, true);
		s.throwInRead = true;
		s.timer = new DefaultTimer();
		c = new Client(PORT, true);
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		waitFor(100);
		
		s.getRecordedData(true);
		s.getSession().getTimer().scheduleEvent("1", 10);
		waitFor(5);
		assertEquals("", s.getRecordedData(true));
		waitFor(6);
		assertEquals("TIM;1|", s.getRecordedData(true));
		
		Runnable task = new Runnable() {

			@Override
			public void run() {
			}

			@Override
			public String toString() {
				return "t1";	
			}
		};
		
		s.getSession().getTimer().scheduleTask(task, 10, true);
		waitFor(5);
		assertEquals("", s.getRecordedData(true));
		waitFor(6);
		assertEquals("TIM;t1|", s.getRecordedData(true));
		
		
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}
	
	@SuppressWarnings("unchecked")
	AtomicReference<Handshake> getHandshake(EngineStreamHandler handler) throws Exception {
		Field f = AbstractEngineHandler.class.getDeclaredField("handshake");
		
		f.setAccessible(true);
		return (AtomicReference<Handshake>) f.get(handler);
	}
	
	@Test
	public void testBeginHandshake() throws Exception {
		EngineStreamHandler h = new EngineStreamHandler(engine, handler, LOGGER);
		
		AtomicReference<Handshake> handshake = getHandshake(h);
		assertEquals(Handshake.NONE, handshake.get());
		h.beginHandshake(true);
		assertEquals(Handshake.REQUESTED, handshake.get());
		h.beginHandshake(true);
		assertEquals(Handshake.REQUESTED, handshake.get());
		handshake.set(Handshake.STARTED);
		h.beginHandshake(true);
		assertEquals(Handshake.STARTED, handshake.get());
		handshake.set(Handshake.NONE);
		h.beginHandshake(true);
		assertEquals(Handshake.REQUESTED, handshake.get());
	}
	
	static class Handler extends EngineStreamHandler {
		int runCounter;
		
		Handler(IEngine engine, IStreamHandler handler, ILogger logger) throws Exception {
			super(engine, handler, logger);
		}
		
		@Override
		public void run() {
			++runCounter;
		}		
		
	}
	
}
