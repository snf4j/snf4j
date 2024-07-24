/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020-2024 SNF4J contributors
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
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
	private final static int APPBUFRATIO = 200;
	private final static int NETBUFRATIO = 300;
	
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

	void setBuffer(EngineStreamHandler handler, String name, ByteBuffer buffer) throws Exception {
		Field field = handler.getClass().getDeclaredField(name);
		
		field.setAccessible(true);
		field.set(handler, buffer);
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
		new SSLSession(h, false);
		
		h.preCreated();
		assertEquals(APPBUFSIZE, getBuffer(h, "inAppBuffer").capacity());
		assertEquals(1, getBuffers(h, "outAppBuffers").length);
		assertEquals(APPBUFSIZE, getBuffers(h, "outAppBuffers")[0].capacity());
		assertEquals(NETBUFSIZE, getBuffer(h, "inNetBuffer").capacity());
		assertEquals(NETBUFSIZE, getBuffer(h, "outNetBuffer").capacity());
	}

	@Test
	public void testTryReleaseOutNetBuffer() throws Exception {
		TestHandler h0 = new TestHandler("Test");
		EngineStreamHandler h = new EngineStreamHandler(engine, h0, LOGGER);
		new SSLSession(h, false);
		h.preCreated();
		h.tryReleaseOutNetBuffer();
		assertNotNull(getBuffer(h, "outNetBuffer"));
		assertEquals(0, h0.allocator.getReleasedCount());
		
		h0.optimizeDataCopy = true;
		h = new EngineStreamHandler(engine, h0, LOGGER);
		new SSLSession(h, false);
		h.preCreated();
		assertNull(getBuffer(h, "outNetBuffer"));
		ByteBuffer b = ByteBuffer.allocate(100);
		setBuffer(h, "outNetBuffer", b);
		assertNotNull(getBuffer(h, "outNetBuffer"));
		b.put((byte) 0);
		h.tryReleaseOutNetBuffer();
		assertNotNull(getBuffer(h, "outNetBuffer"));
		assertEquals(0, h0.allocator.getReleasedCount());
		b.clear();
		h.tryReleaseOutNetBuffer();
		assertNull(getBuffer(h, "outNetBuffer"));
		assertEquals(1, h0.allocator.getReleasedCount());
	}
	
	@Test
	public void testTryReleaseInAppBuffer() throws Exception {
		TestHandler h0 = new TestHandler("Test");
		EngineStreamHandler h = new EngineStreamHandler(engine, h0, LOGGER);
		new SSLSession(h, false);
		h.preCreated();
		h.tryReleaseInAppBuffer();
		assertNotNull(getBuffer(h, "inAppBuffer"));
		assertEquals(0, h0.allocator.getReleasedCount());
		
		h0.optimizeDataCopy = true;
		h = new EngineStreamHandler(engine, h0, LOGGER);
		new SSLSession(h, false);
		h.preCreated();
		assertNull(getBuffer(h, "inAppBuffer"));
		ByteBuffer b = ByteBuffer.allocate(100);
		setBuffer(h, "inAppBuffer", b);
		assertNotNull(getBuffer(h, "inAppBuffer"));
		b.put((byte) 0);
		h.tryReleaseInAppBuffer();
		assertNotNull(getBuffer(h, "inAppBuffer"));
		assertEquals(0, h0.allocator.getReleasedCount());
		b.clear();
		h.tryReleaseInAppBuffer();
		assertNull(getBuffer(h, "inAppBuffer"));
		assertEquals(1, h0.allocator.getReleasedCount());
		
	}
	
	@Test
	public void testTryDelayedException() {
		TestHandler h0 = new TestHandler("Test");
		EngineStreamHandler h = new EngineStreamHandler(engine, h0, LOGGER);
		Exception e1 = new Exception("E1");
		Exception e2 = new Exception("E2");
		HandshakeStatus[] status = new HandshakeStatus[1];
		
		assertNull(h.delayedException);
		for (HandshakeStatus hs: HandshakeStatus.values()) {
			if (hs == HandshakeStatus.NEED_WRAP) {
				continue;
			}
			engine.status = hs;
			assertFalse(h.tryDelayedException(e1, status));
			assertNull(h.delayedException);
			assertNull(status[0]);
		}
		engine.status = HandshakeStatus.NEED_WRAP;
		h0.quicklyCloseEngine = true;
		assertFalse(h.tryDelayedException(e1, status));
		h0.quicklyCloseEngine = false;
		assertTrue(h.tryDelayedException(e1, status));
		assertNotNull(h.delayedException);
		assertFalse(h.delayedException.isFired());
		assertSame(e1, h.delayedException.getClosingCause());
		assertFalse(h.tryDelayedException(e2, status));
		assertNotNull(h.delayedException);
		assertSame(e1, h.delayedException.getClosingCause());
		assertFalse(h.delayedException.isFired());
		
		h.delayedException = null;
		h.debugEnabled = true;
		assertTrue(h.tryDelayedException(e1, status));
	}
	
	@Test
	public void testfireDelayedException() throws Exception {
		TestHandler h0 = new TestHandler("Test");
		EngineStreamHandler h = new EngineStreamHandler(engine, h0, LOGGER);
		Exception e1 = new Exception("E1");
		HandshakeStatus[] status = new HandshakeStatus[1];
		
		engine.status = HandshakeStatus.NEED_WRAP;
		assertFalse(h.fireDelayedException());
		h.tryDelayedException(e1, status);
		SSLSession session = new SSLSession(h, false);
		h.setSession(session);
		assertTrue(h.fireDelayedException());
		assertTrue(h.delayedException.isFired());
		assertFalse(h.fireDelayedException());
		
		h.delayedException = null;
		h.tryDelayedException(e1, status);
		h.debugEnabled = true;
		assertTrue(h.fireDelayedException());
	}

	@Test
	public void testEngineNeedWrapUnwrap() throws Exception {
		TestHandler h0 = new TestHandler("Test");
		EngineStreamHandler h = new EngineStreamHandler(engine, h0, LOGGER);
		Field f = AbstractEngineHandler.class.getDeclaredField("isReadyPending");
		f.setAccessible(true);
		f.setBoolean(h, false);
		engine.setTrace(new TraceBuilder());
		h.setSession(new SSLSession(h, false));
		h.preCreated();
		assertEquals("INI|", engine.trace.get(true));
		h.run();
		assertEquals("", engine.trace.get(true));
		engine.needWrap = 1;
		h.run();
		assertEquals("W0|", engine.trace.get(true));
		engine.needWrap = 2;
		h.run();
		assertEquals("W0|W0|", engine.trace.get(true));
		h.run();
		assertEquals("", engine.trace.get(true));
		engine.needUnwrap = 1;
		h.run();
		assertEquals("U0|", engine.trace.get(true));
		engine.needUnwrap = 2;
		h.run();
		assertEquals("U0|U0|", engine.trace.get(true));
		h.run();
		assertEquals("", engine.trace.get(true));
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
		new SSLSession(h, false);
		
		h.preCreated();
		
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
		h.read((ByteBuffer)null);
		ByteBuffer b = handler.allocator.allocate(100);
		int count = handler.allocator.getSize();
		h.read(b);
		assertEquals(count-1, handler.allocator.getSize());
		count = handler.allocator.getReleasedCount();
		assertTrue(b == handler.allocator.getReleased().get(count-1));
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
		
		Throwable t = new RuntimeException("Ex2");
		s = new Server(PORT, true);
		s.throwInRead = true;
		s.throwIn = new SessionTest.CloseControllingException("Ex1", ICloseControllingException.CloseType.NONE, t);
		s.exceptionRecordException = true;
		c = new Client(PORT, true);
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		s.resetDataLocks();
		c.resetDataLocks();
		c.write(new Packet(PacketType.ECHO));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE()|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertEquals("DS|DR|ECHO()|EXC|(Ex2)|DS|", s.getRecordedData(true));
		assertFalse(s.session.getCloseFuture().isDone());
		s.throwIn = new SessionTest.CloseControllingException("Ex1", ICloseControllingException.CloseType.GENTLE, t);
		c.write(new Packet(PacketType.ECHO));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		String recordedData = c.trimRecordedData(CLIENT_RDY_TAIL);
		if (!"DS|DR|ECHO_RESPONSE()|SCL|SEN|".equals(recordedData)) {
			assertEquals("DS|DR|ECHO_RESPONSE()|DS|SCL|SEN|", recordedData);
		}
		assertEquals("DR|ECHO()|EXC|(Ex2)|DS|SCL|SEN|", s.getRecordedData(true));
		assertTrue(s.session.getReadyFuture().isSuccessful());
		assertTrue(s.session.getCloseFuture().isFailed());
		assertTrue(s.session.getEndFuture().isFailed());		
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
		s.getSession().getTimer().scheduleEvent("1", 100);
		waitFor(80);
		assertEquals("", s.getRecordedData(true));
		waitFor(120);
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
		
		s.getSession().getTimer().scheduleTask(task, 100, true);
		waitFor(80);
		assertEquals("", s.getRecordedData(true));
		waitFor(120);
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
