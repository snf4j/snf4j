/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019-2024 SNF4J contributors
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.allocator.TestAllocator;
import org.snf4j.core.codec.DefaultCodecExecutor;
import org.snf4j.core.engine.IEngine;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.logger.LoggerRecorder;
import org.snf4j.core.pool.DefaultSelectorLoopPool;
import org.snf4j.core.proxy.HttpProxyHandler;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.session.IllegalSessionStateException;
import org.snf4j.core.session.SSLEngineCreateException;
import org.snf4j.core.timer.TestTimer;

public class SSLSessionTest {
	
	private final static boolean compensateTime = TestConfig.compensateTime();
	
	long TIMEOUT = 2000;
	int PORT = 7777;
	long AFTER_TIMEOUT = 0;
	
	Server s;
	Client c;
	HttpProxy p;
	
	static final String CLIENT_RDY_TAIL;
	static final boolean TLS1_3;
	
	static {
		double version = Double.parseDouble(System.getProperty("java.specification.version"));
		String longerVersion = System.getProperty("java.version");
		
		//as of java 11 the SSLEngine works in different way
		if (version >= 11.0) {
			CLIENT_RDY_TAIL = "DR|";
			TLS1_3 = true;
		}
		else if (version == 1.8 && upd(longerVersion) >= 392) {
			CLIENT_RDY_TAIL = "DR|";
			TLS1_3 = true;
		}
		else {
			CLIENT_RDY_TAIL = "";
			TLS1_3 = false;
		}
	}
	
	static int upd(String fullVersion) {
		int i = fullVersion.indexOf('_');
		
		if (i > -1) {
			return Integer.parseInt(fullVersion.substring(i+1));
		}
		return 0;
	}
	
	@Before
	public void before() {
		s = c = null;
		p = null;
		AFTER_TIMEOUT = 0;
	}
	
	@After
	public void after() throws InterruptedException {
		if (c != null) c.stop(TIMEOUT+AFTER_TIMEOUT);
		if (s != null) s.stop(TIMEOUT+AFTER_TIMEOUT);
		if (p != null) p.stop(TIMEOUT+AFTER_TIMEOUT);
	}

	static void assertTLSVariants(String expected, String actual) {
		SessionTest.assertVaraints(expected, actual, TLS1_3);
	}
	
	private byte[] getBytes(int size, int value) {
		return ByteUtils.getBytes(size, value);
	}
	
	private ByteBuffer getBuffer(int size, int value) {
		return ByteBuffer.wrap(getBytes(size, value));
	}
	
	private void waitFor(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}
	
	static ByteBuffer getBuffer(EngineStreamHandler handler, String name) throws Exception {
		Field field = handler.getClass().getDeclaredField(name);
		
		field.setAccessible(true);
		return (ByteBuffer) field.get(handler);
	}

	static ByteBuffer[] getBuffers(EngineStreamHandler handler, String name) throws Exception {
		Field field = handler.getClass().getDeclaredField(name);
		
		field.setAccessible(true);
		return (ByteBuffer[]) field.get(handler);
	}

	static void setBuffer(EngineStreamHandler handler, String name, ByteBuffer buf) throws Exception {
		Field field = handler.getClass().getDeclaredField(name);
		
		field.setAccessible(true);
		field.set(handler,buf);
	}

	static void setBuffers(EngineStreamHandler handler, String name, ByteBuffer[] buf) throws Exception {
		Field field = handler.getClass().getDeclaredField(name);
		
		field.setAccessible(true);
		field.set(handler,buf);
	}

	static ByteBuffer getBuffer(SSLSession session, String name) throws Exception {
		Field field = EngineStreamSession.class.getDeclaredField("internal");
		
		field.setAccessible(true);
		return getBuffer((EngineStreamHandler) field.get(session), name);
	}

	static ByteBuffer[] getBuffers(SSLSession session, String name) throws Exception {
		Field field = EngineStreamSession.class.getDeclaredField("internal");
		
		field.setAccessible(true);
		return getBuffers((EngineStreamHandler) field.get(session), name);
	}
	
	static ByteBuffer[] getAllBuffers(SSLSession session) throws Exception {
		Field f = StreamSession.class.getDeclaredField("outBuffers");
		
		f.setAccessible(true);
		ByteBuffer[] bs1 = getBuffers(session, "outAppBuffers");
		ByteBuffer[] bs0 = (ByteBuffer[]) f.get(session);
		ByteBuffer[] buffers = new ByteBuffer[3+1+bs1.length+bs0.length];
		int i = 0;
		
		for (int j=0; j<bs0.length; ++j) {
			buffers[i++] = bs0[j];
		}
		f = StreamSession.class.getDeclaredField("inBuffer");
		f.setAccessible(true);
		buffers[i++] = (ByteBuffer) f.get(session);
		for (int j=0; j<bs1.length; ++j) {
			buffers[i++] = bs1[j];
		}
		buffers[i++] = getBuffer(session, "outNetBuffer");
		buffers[i++] = getBuffer(session, "inAppBuffer");
		buffers[i++] = getBuffer(session, "inNetBuffer");
		return buffers;
	}
	

	static void setBuffer(EngineStreamSession session, String name, ByteBuffer buf) throws Exception {
		Field field = EngineStreamSession.class.getDeclaredField("internal");
		
		field.setAccessible(true);
		setBuffer((EngineStreamHandler) field.get(session), name, buf);
	}

	static void setBuffers(EngineStreamSession session, String name, ByteBuffer[] bufs) throws Exception {
		Field field = EngineStreamSession.class.getDeclaredField("internal");
		
		field.setAccessible(true);
		setBuffers((EngineStreamHandler) field.get(session), name, bufs);
	}
	
	EngineStreamHandler getInternal(EngineStreamSession session) throws Exception {
		Field field = EngineStreamSession.class.getDeclaredField("internal");
		
		field.setAccessible(true);
		return (EngineStreamHandler) field.get(session);
	}

	TestSSLEngine getSSLEngine(SSLSession session) throws Exception {
		EngineStreamHandler internal = getInternal(session);
		Field field = AbstractEngineHandler.class.getDeclaredField("engine");
		
		field.setAccessible(true);
		InternalSSLEngine engine = (InternalSSLEngine) field.get(internal);
		field = InternalSSLEngine.class.getDeclaredField("engine");
		field.setAccessible(true);
		return (TestSSLEngine) field.get(engine);
	}
	
	@Test
	public void testConstructor() throws Exception {
		TestHandler handler = new TestHandler("Test1");
		handler.engine = new TestSSLEngine(new TestSSLSession());

		SSLSession session = new SSLSession(handler, true);
		assertTrue(handler == session.getHandler());
		assertEquals("Test1", session.getName());
		assertEquals("true", handler.engineArguments);
		
		session = new SSLSession(handler, false);
		assertTrue(handler == session.getHandler());
		assertEquals("Test1", session.getName());
		assertEquals("false", handler.engineArguments);
		
		session = new SSLSession("Test2", handler, true);
		assertTrue(handler == session.getHandler());
		assertEquals("Test2", session.getName());
		assertEquals("true", handler.engineArguments);
		
		session = new SSLSession("Test2", handler, false);
		assertTrue(handler == session.getHandler());
		assertEquals("Test2", session.getName());
		assertEquals("false", handler.engineArguments);

		InetSocketAddress a = new InetSocketAddress("127.0.0.1", 7000);
		String s = "" + a;
		
		session = new SSLSession(a, handler, true);
		assertTrue(handler == session.getHandler());
		assertEquals("Test1", session.getName());
		assertEquals(s+"|true", handler.engineArguments);
		
		session = new SSLSession(a, handler, false);
		assertTrue(handler == session.getHandler());
		assertEquals("Test1", session.getName());
		assertEquals(s+"|false", handler.engineArguments);
		
		session = new SSLSession("Test2", a, handler, true);
		assertTrue(handler == session.getHandler());
		assertEquals("Test2", session.getName());
		assertEquals(s+"|true", handler.engineArguments);
		
		session = new SSLSession("Test2", a, handler, false);
		assertTrue(handler == session.getHandler());
		assertEquals("Test2", session.getName());
		assertEquals(s+"|false", handler.engineArguments);

		session = new SSLSession((SocketAddress)null, handler, true);
		assertTrue(handler == session.getHandler());
		assertEquals("Test1", session.getName());
		assertEquals("true", handler.engineArguments);
		
		session = new SSLSession((SocketAddress)null, handler, false);
		assertTrue(handler == session.getHandler());
		assertEquals("Test1", session.getName());
		assertEquals("false", handler.engineArguments);
		
		session = new SSLSession("Test2", (SocketAddress)null, handler, true);
		assertTrue(handler == session.getHandler());
		assertEquals("Test2", session.getName());
		assertEquals("true", handler.engineArguments);
		
		session = new SSLSession("Test2", (SocketAddress)null, handler, false);
		assertTrue(handler == session.getHandler());
		assertEquals("Test2", session.getName());
		assertEquals("false", handler.engineArguments);
		
		handler =  new TestHandler("Test1");
		assertEquals(0, handler.allocatorCount);
		session = new SSLSession("Test2", (SocketAddress)null, handler, false);
		assertEquals(1, handler.allocatorCount);
		assertTrue(getInternal(session).allocator == session.allocator);
	}
	
	@Test
	public void testCreateSessionException() throws Exception {
		s = new Server(PORT, true);
		s.throwInCreateSession = true;
		c = new Client(PORT, true);
		s.start();
		c.start();
		
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", c.getRecordedData(true));
	}
	
	@Test
	public void testBufferMaxRanges() throws Exception {
		s = new Server(PORT, true);
		s.maxSSLAppBufRatio = 100;
		s.maxSSLNetBufRatio = 100;
		c = new Client(PORT, true);
		c.maxSSLAppBufRatio = 150;
		c.maxSSLNetBufRatio = 170;
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		
		IEngine engine = EngineSessionTest.getEngine((SSLSession)s.getSession());
		assertEquals(engine.getMinApplicationBufferSize(), engine.getMaxApplicationBufferSize());
		assertEquals(engine.getMinNetworkBufferSize(), engine.getMaxNetworkBufferSize());
		engine = EngineSessionTest.getEngine((SSLSession)c.getSession());
		assertEquals(engine.getMinApplicationBufferSize()*150/100, engine.getMaxApplicationBufferSize());
		assertEquals(engine.getMinNetworkBufferSize()*170/100, engine.getMaxNetworkBufferSize());
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		c.stop(TIMEOUT);

		c = new Client(PORT, true);
		c.maxSSLAppBufRatio = 99;
		c.maxSSLNetBufRatio = -1;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		engine = EngineSessionTest.getEngine((SSLSession)c.getSession());
		assertEquals(engine.getMinApplicationBufferSize(), engine.getMaxApplicationBufferSize());
		assertEquals(engine.getMinNetworkBufferSize(), engine.getMaxNetworkBufferSize());
		
		
	}
	
	@Test
	public void testExecutor() throws Exception {
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		s.start();
		c.start();

		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertTrue(c.loop.getExecutor() == ((SSLSession)c.getSession()).getExecutor());
		assertTrue(c.loop.getExecutor() == DefaultExecutor.DEFAULT);
		SSLSession session = new SSLSession("name", c.getSession().getHandler(), true);
		assertNull(session.getExecutor());
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		
		
		ExecutorService pool1 = Executors.newFixedThreadPool(2);
		ExecutorService pool2 = Executors.newFixedThreadPool(2);
		try {
			c = new Client(PORT, true);
			c.executor = pool1;
			c.start();

			c.waitForSessionReady(TIMEOUT);
			s.waitForSessionReady(TIMEOUT);
			assertTrue(pool1 == ((SSLSession)c.getSession()).getExecutor());
			assertTrue(c.loop.getExecutor() == DefaultExecutor.DEFAULT);
			session = new SSLSession("name", c.getSession().getHandler(), true);
			assertTrue(pool1 == session.getExecutor());
			c.loop.setExecutor(pool2);
			assertTrue(c.loop.getExecutor() == pool2);
			assertTrue(pool1 == ((SSLSession)c.getSession()).getExecutor());
			((SSLSession)c.getSession()).setExecutor(null);
			assertTrue(pool2 == ((SSLSession)c.getSession()).getExecutor());
			c.stop(TIMEOUT);
			c.waitForSessionEnding(TIMEOUT);
			s.waitForSessionEnding(TIMEOUT);
		}
		finally {
			pool1.shutdownNow();
			pool2.shutdownNow();
		}
	}
	
	@Test
	public void testHandshakeTimeout() throws Exception {
		s = new Server(PORT, false);
		c = new Client(PORT, true);
		c.timer = new TestTimer();
		c.handshakeTimeout = 1000;
		c.exceptionRecordException = true;
		s.start();
		c.start();

		waitFor(900);
		assertEquals("SCR|SOP|DS|", c.getRecordedData(true));
		waitFor(200);
		assertEquals("EXC|(Session handshake timed out)|SCL|SEN|", c.getRecordedData(true));
		c.waitForSessionEnding(TIMEOUT);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		if (!TLS1_3) {
			s = new Server(PORT, true);
			c = new Client(PORT, true);
			c.timer = new TestTimer();
			c.handshakeTimeout = 500;
			c.exceptionRecordException = true;
			s.start();
			c.start();
			c.waitForSessionReady(TIMEOUT);
			s.waitForSessionReady(TIMEOUT);
			c.getSession().write(new Packet(PacketType.NOP).toBytes());
			c.waitForDataSent(TIMEOUT);
			s.waitForDataRead(TIMEOUT);
			c.getRecordedData(true);

			s.getSession().suspendRead();
			((SSLSession)c.getSession()).beginHandshake();
			waitFor(400);
			assertEquals("DS|", c.getRecordedData(true));
			waitFor(200);
			assertEquals("EXC|(Session handshake timed out)|SCL|SEN|", c.getRecordedData(true));
			s.getSession().resumeRead();
			s.waitForSessionEnding(TIMEOUT);
		}
	}
	
	@Test
	public void testSessionResumtion() throws Exception {
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		s.start();
		c.sslRemoteAddress = true;
		c.start();

		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		waitFor(50);
		javax.net.ssl.SSLSession session1 = getSSLEngine((SSLSession) c.getSession()).getSession();
		assertTrue(session1 == ((SSLSession)c.getSession()).getEngineSession());
		c.getRecordedData(true);
		s.getRecordedData(true);
		c.resetDataLocks();
		s.resetDataLocks();
		c.getSession().write(new Packet(PacketType.ECHO, "1").toBytes());
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(1)|", c.getRecordedData(true));
		assertEquals("DR|ECHO(1)|DS|", s.getRecordedData(true));
		
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		c.stop(TIMEOUT);
		
		c = new Client(PORT, true);
		c.sslRemoteAddress = true;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		waitFor(50);
		javax.net.ssl.SSLSession session2 = getSSLEngine((SSLSession) c.getSession()).getSession();
		assertTrue(session2 == ((SSLSession)c.getSession()).getEngineSession());
		if (!TLS1_3) {
			assertTrue(session2 == session1);
		}
		c.getRecordedData(true);
		s.getRecordedData(true);
		c.resetDataLocks();
		s.resetDataLocks();
		c.getSession().write(new Packet(PacketType.ECHO, "2").toBytes());
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(2)|", c.getRecordedData(true));
		assertEquals("DR|ECHO(2)|DS|", s.getRecordedData(true));
			
	}
	
	@Test 
	public void testClose() throws Exception {
		TestHandler handler = new TestHandler("Test1");
		handler.engine = new TestSSLEngine(new TestSSLSession());
		SSLSession session = new SSLSession(handler, true);
		session.close();
		
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		s.start();
		c.start();

		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		session = (SSLSession) c.getSession(); 
		ByteBuffer[] bufs = getBuffers(session, "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "567").toBytes());
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertTLSVariants("DS|DR|NOP(567)|?{DS|}SCL|SEN|", s.getRecordedData(true));
		
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		//close by stopping loop
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		s.start();
		c.start();
	
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		session = (SSLSession) c.getSession(); 
		bufs = getBuffers(session, "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "567").toBytes());

		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertTLSVariants("DS|DR|NOP(567)|?{DS|}SCL|SEN|", s.getRecordedData(true));
		c.getSession().exception(new Exception());
		assertEquals("", c.getRecordedData(true));
		
		//close already closed session
		session.close();
	}

	@Test 
	public void testDirtyClose() throws Exception {
		TestHandler handler = new TestHandler("Test1");
		handler.engine = new TestSSLEngine(new TestSSLSession());
		SSLSession session = new SSLSession(handler, true);
		session.dirtyClose();

		s = new Server(PORT, true);
		c = new Client(PORT, true);
		
		s.start();
		c.start();

		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		session = (SSLSession) c.getSession(); 
		ByteBuffer[] bufs = getBuffers(session, "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "567").toBytes());
		LoggerRecorder.enableRecording();
		c.getSession().dirtyClose();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		List<String> recording = LoggerRecorder.disableRecording();
		assertTLSVariants("?{DS|}SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));
		String warnMsg = "[ WARN] " + SessionIncident.SSL_CLOSED_WITHOUT_CLOSE_NOTIFY.defaultMessage();
		assertTrue(recording.contains(warnMsg));
		
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		//close by stopping loop
		s = new Server(PORT, true);
		s.incident = true;
		c = new Client(PORT, true);
		s.start();
		c.start();
	
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		session = (SSLSession) c.getSession(); 
		bufs = getBuffers(session, "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "567").toBytes());
		waitFor(100);
		
		LoggerRecorder.enableRecording();
		c.dirtyStop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		recording = LoggerRecorder.disableRecording();
		assertTLSVariants("?{DS|}SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));
		assertFalse(recording.contains(warnMsg));
	
		session.dirtyClose();
	}
	
	@Test 
	public void testQuickClose() throws Exception {
		TestHandler handler = new TestHandler("Test1");
		handler.engine = new TestSSLEngine(new TestSSLSession());
		SSLSession session = new SSLSession(handler, true);
		session.quickClose();

		s = new Server(PORT, true);
		c = new Client(PORT, true);
		
		s.start();
		c.start();

		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		session = (SSLSession) c.getSession(); 
		ByteBuffer[] bufs = getBuffers(session, "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "567").toBytes());
		c.getSession().quickClose();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertTLSVariants("DS|DR|?{DS|}SCL|SEN|", s.getRecordedData(true));

		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		//close by stopping loop
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		s.start();
		c.start();
	
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		session = (SSLSession) c.getSession(); 
		bufs = getBuffers(session, "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "567").toBytes());

		c.quickStop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertTLSVariants("DS|DR|?{DS|}SCL|SEN|", s.getRecordedData(true));
	
		session.quickClose();
	}

	@Test
	public void testClosingAction() throws Exception {
		s = new Server(PORT, true);
		s.start();

		//stop with more sessions in the loop
		c = new Client(PORT, true);
		c.endingAction = EndingAction.STOP;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		ByteBuffer[] bufs = getBuffers((SSLSession)c.getSession(), "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "123").toBytes());
		StreamSession session1 = c.getSession();
		c.endingAction = EndingAction.DEFAULT;
		c.start(false, c.loop);
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		bufs = getBuffers((SSLSession)c.getSession(), "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "567").toBytes());
		waitFor(200);
		s.recordSessionId = true;
		session1.close();
		waitFor(500);
		assertTLSVariants("DS|DR@1|NOP(123)@1|?{DS@1|}SCL@1|SEN@1|DR@2|NOP(567)@2|?{DS@2|}SCL@2|SEN@2|", s.getOrderedRecordedData(true));
		assertTrue(c.loop.join(TIMEOUT));
		s.recordSessionId = false;
		
		//quick stop with more sessions in the loop
		c = new Client(PORT, true);
		c.endingAction = EndingAction.QUICK_STOP;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		bufs = getBuffers((SSLSession)c.getSession(), "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "123").toBytes());
		session1 = c.getSession();
		c.endingAction = EndingAction.DEFAULT;
		c.start(false, c.loop);
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		bufs = getBuffers((SSLSession)c.getSession(), "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "567").toBytes());
		waitFor(200);
		s.recordSessionId = true;
		session1.close();
		waitFor(500);
		assertTLSVariants("DS|DR@1|NOP(123)@1|?{DS@1|}SCL@1|SEN@1|DR@2|?{DS@2|}SCL@2|SEN@2|", s.getOrderedRecordedData(true));
		assertTrue(c.loop.join(TIMEOUT));
		s.recordSessionId = false;

		//dirty stop with more sessions in the loop
		c = new Client(PORT, true);
		c.endingAction = EndingAction.DIRTY_STOP;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		bufs = getBuffers((SSLSession)c.getSession(), "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "123").toBytes());
		session1 = c.getSession();
		c.endingAction = EndingAction.DEFAULT;
		c.start(false, c.loop);
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		bufs = getBuffers((SSLSession)c.getSession(), "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "567").toBytes());
		waitFor(200);
		s.recordSessionId = true;
		session1.close();
		waitFor(500);
		assertTLSVariants("DS|DR@1|NOP(123)@1|?{DS@1|}SCL@1|SEN@1|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY@2|SCL@2|SEN@2|", s.getOrderedRecordedData(true));
		assertTrue(c.loop.join(TIMEOUT));
		
	}

	private void assertOutOfBoundException(StreamSession session, byte[] data, int off, int len) {
		try {
			session.write(data, off, len);
			fail("Exception not thrown");
		}
		catch (IndexOutOfBoundsException e) {}
		try {
			session.writenf(data, off, len);
			fail("Exception not thrown");
		}
		catch (IndexOutOfBoundsException e) {}
	}

	private void assertIllegalStateException(StreamSession session, byte[] data, int off, int len) {
		try {
			session.write(data, off, len);
			fail("Exception not thrown");
		}
		catch (IllegalStateException e) {}
		try {
			session.writenf(data, off, len);
			fail("Exception not thrown");
		}
		catch (IllegalStateException e) {}
	}
	
	@Test
	public void testEventException() throws Exception {
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		c.throwInEvent = true;
		c.throwInEventType = EventType.SESSION_CREATED;
		c.incident = true;
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertTrue(c.getRecordedData(true).startsWith("SCR|SESSION_EVENT_FAILURE|SOP"));
		c.throwInEventType = EventType.DATA_SENT;
		c.write(new Packet(PacketType.ECHO));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertTrue(c.getRecordedData(true).endsWith("DS|DATA_EVENT_FAILURE|DR|ECHO_RESPONSE()|"));
		c.throwInEventType = EventType.DATA_RECEIVED;
		c.write(new Packet(PacketType.ECHO));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertTrue(c.getRecordedData(true).endsWith("DR|DATA_EVENT_FAILURE|ECHO_RESPONSE()|"));
		c.throwInEventType = EventType.SESSION_CLOSED;
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertTrue(c.getRecordedData(true).endsWith("SCL|SESSION_EVENT_FAILURE|SEN|"));
		c.stop(TIMEOUT);
		
		c.throwInEventType = EventType.SESSION_OPENED;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertTrue(c.getRecordedData(true).startsWith("SCR|SOP|SESSION_EVENT_FAILURE|"));
		c.throwInEventType = EventType.SESSION_ENDING;
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertTrue(c.getRecordedData(true).endsWith("SCL|SEN|SESSION_EVENT_FAILURE|"));
		c.stop(TIMEOUT);

		c.throwInEventType = EventType.SESSION_READY;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		waitFor(50);
		assertTrue(c.getRecordedData(true).contains("|RDY|SESSION_EVENT_FAILURE|"));
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		c.stop(TIMEOUT);

		c.throwInEventType = EventType.SESSION_CREATED;
		c.incident = false;
		c.getRecordedData(true);
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SESSION_EVENT_FAILURE|EXC|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);

		c.throwInEventType = EventType.SESSION_OPENED;
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|SESSION_EVENT_FAILURE|EXC|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);

		c.throwInEventType = EventType.SESSION_READY;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertTrue(c.getRecordedData(true).endsWith("RDY|SESSION_EVENT_FAILURE|EXC|SCL|SEN|"));
		c.stop(TIMEOUT);

		c.throwInEventType = EventType.DATA_SENT;
		c.throwInEvent = false;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.throwInEvent = true;
		c.session.write(new Packet(PacketType.NOP).toBytes());
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertTrue(c.getRecordedData(true).endsWith("DS|DATA_EVENT_FAILURE|EXC|SCL|SEN|"));
		c.stop(TIMEOUT);
		
		c.throwInEventType = EventType.DATA_RECEIVED;
		c.throwInEvent = false;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.throwInEvent = true;
		s.session.write(new Packet(PacketType.NOP).toBytes());
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertTrue(c.getRecordedData(true).contains("DR|DATA_EVENT_FAILURE|EXC|"));
		c.stop(TIMEOUT);
		
		c.throwInEventType = EventType.SESSION_CLOSED;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		s.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertTrue(c.getRecordedData(true).endsWith("SCL|SESSION_EVENT_FAILURE|EXC|SEN|"));
		c.stop(TIMEOUT);

		c.throwInEventType = EventType.SESSION_ENDING;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		s.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertTrue(c.getRecordedData(true).endsWith("SCL|SEN|SESSION_EVENT_FAILURE|"));
		c.stop(TIMEOUT);
		
		boolean ready = false;
		for (int i=0 ;i<10; ++i) {
			c.throwInEventType = EventType.DATA_SENT;
			c.throwInEventDelay = i;
			c.start();
			waitFor(100);
			String s = c.getRecordedData(true);
			if (s.contains("|RDY|")) {
				ready = true;
				break;
			}
			assertTrue(s.indexOf("DS|DATA_EVENT_FAILURE|EXC|") != -1);
			c.stop(TIMEOUT);
		}
		assertTrue(ready);
		
		ready = false;
		for (int i=0 ;i<10; ++i) {
			c.throwInEventType = EventType.DATA_RECEIVED;
			c.throwInEventDelay = i;
			c.start();
			waitFor(100);
			String s = c.getRecordedData(true);
			if (s.contains("|RDY|")) {
				ready = true;
				break;
			}
			assertTrue(s.indexOf("DR|DATA_EVENT_FAILURE|EXC|") != -1);
			c.stop(TIMEOUT);
		}
		assertTrue(ready);
	}
	
	@Test
	public void testWriteByteBufferHolder() throws Exception {
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		c.allocator = new TestAllocator(true, true);
		c.ignoreAvailableException = true;
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		waitFor(50);
		c.getRecordedData(true);
		
		SSLSession session = (SSLSession) c.getSession();
		assertEquals(6, c.allocator.getSize());
		
		setBuffers(session, "outAppBuffers", new ByteBuffer[0]);
		
		byte[] data = new Packet(PacketType.ECHO, "01234567").toBytes();
		session.write(SessionTest.createHolder(session, data, 1,2,3));
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(01234567)|", c.getRecordedData(true));
		assertEquals(11, c.allocator.getSize());
		
	}
	
	@Test
	public void testWriteArguments() throws Exception {
		s = new Server(PORT, true);
		c = new Client(PORT, true);

		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		
		SSLSession session = (SSLSession) c.getSession();
		session.write(new Packet(PacketType.ECHO, "1234").toBytes());
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(1234)|", c.trimRecordedData(CLIENT_RDY_TAIL));
		session.writenf(new Packet(PacketType.ECHO, "134").toBytes());
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(134)|", c.getRecordedData(true));

		byte[] data = new Packet(PacketType.ECHO, "567").toBytes(0, 4);
		session.write(data, 0, data.length-4);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(567)|", c.getRecordedData(true));
		data = new Packet(PacketType.ECHO, "5767").toBytes(0, 4);
		session.writenf(data, 0, data.length-4);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(5767)|", c.getRecordedData(true));

		data = new Packet(PacketType.ECHO, "89").toBytes(3, 0);
		session.write(data, 3, data.length-3);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(89)|", c.getRecordedData(true));
		data = new Packet(PacketType.ECHO, "891").toBytes(3, 0);
		session.writenf(data, 3, data.length-3);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(891)|", c.getRecordedData(true));

		data = new Packet(PacketType.ECHO, "0").toBytes(7, 10);
		session.write(data, 7, data.length-17);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(0)|", c.getRecordedData(true));
		data = new Packet(PacketType.ECHO, "02").toBytes(7, 10);
		session.writenf(data, 7, data.length-17);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(02)|", c.getRecordedData(true));
		
		data = new Packet(PacketType.ECHO, "57").toBytes();
		session.write(ByteBuffer.wrap(data));
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(57)|", c.getRecordedData(true));
		data = new Packet(PacketType.ECHO, "577").toBytes();
		session.writenf(ByteBuffer.wrap(data));
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(577)|", c.getRecordedData(true));
		
		data = new Packet(PacketType.ECHO, "574").toBytes(0, 7);
		session.write(ByteBuffer.wrap(data), data.length - 7);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(574)|", c.getRecordedData(true));
		data = new Packet(PacketType.ECHO, "5746").toBytes(0, 7);
		session.writenf(ByteBuffer.wrap(data), data.length - 7);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(5746)|", c.getRecordedData(true));

		data = new Packet(PacketType.ECHO, "01234567").toBytes();
		ByteBufferHolder holder = new ByteBufferHolder();
		holder.add(ByteBuffer.wrap(data, 0, 4));
		holder.add(ByteBuffer.wrap(data, 4, data.length-4));
		session.write(holder).sync(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(01234567)|", c.getRecordedData(true));
		holder = new ByteBufferHolder();
		holder.add(ByteBuffer.wrap(data, 0, 4));
		holder.add(ByteBuffer.wrap(data, 4, data.length-4));
		session.writenf(holder);
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(01234567)|", c.getRecordedData(true));
		
		session.closing = ClosingState.SENDING;
		assertFalse(session.write(new byte[3], 0, 1).isSuccessful());
		assertFalse(session.write(new byte[3]).isSuccessful());
		assertFalse(session.write(getBuffer(10,0)).isSuccessful());
		assertFalse(session.write(getBuffer(10,0), 5).isSuccessful());
		holder = new ByteBufferHolder();
		holder.add(ByteBuffer.wrap(data, 0, 4));
		holder.add(ByteBuffer.wrap(data, 4, data.length-4));
		assertFalse(session.write(holder).isSuccessful());
		session.writenf(new byte[3], 0, 1);
		session.writenf(new byte[3]);
		session.writenf(getBuffer(10,0));
		session.writenf(getBuffer(10,0), 5);
		holder = new ByteBufferHolder();
		holder.add(ByteBuffer.wrap(data, 0, 4));
		holder.add(ByteBuffer.wrap(data, 4, data.length-4));
		session.writenf(holder);
		session.closing = ClosingState.NONE;

		EngineStreamHandler internal = getInternal(session);
		Field field = AbstractEngineHandler.class.getDeclaredField("closing");
		field.setAccessible(true);
		field.set(internal, ClosingState.SENDING);
		assertFalse(session.write(new byte[3], 0, 1).isSuccessful());
		assertFalse(session.write(new byte[3]).isSuccessful());
		assertFalse(session.write(getBuffer(10,0)).isSuccessful());
		assertFalse(session.write(getBuffer(10,0), 5).isSuccessful());
		session.writenf(new byte[3], 0, 1);
		session.writenf(new byte[3]);
		session.writenf(getBuffer(10,0));
		session.writenf(getBuffer(10,0), 5);
		field.set(internal, ClosingState.NONE);
		
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		try {
			session.write((byte[])null);
			fail("Exception not thrown");
		}
		catch (NullPointerException e) {}
		try {
			session.write((byte[])null, 0, 0);
			fail("Exception not thrown");
		}
		catch (NullPointerException e) {}
		try {
			session.write((ByteBuffer)null);
			fail("Exception not thrown");
		}
		catch (NullPointerException e) {}
		try {
			session.write((ByteBuffer)null, 0);
			fail("Exception not thrown");
		}
		catch (NullPointerException e) {}
		try {
			session.write((IByteBufferHolder)null);
			fail("Exception not thrown");
		}
		catch (NullPointerException e) {}
		try {
			session.writenf((byte[])null);
			fail("Exception not thrown");
		}
		catch (NullPointerException e) {}
		try {
			session.writenf((byte[])null, 0, 0);
			fail("Exception not thrown");
		}
		catch (NullPointerException e) {}
		try {
			session.writenf((ByteBuffer)null);
			fail("Exception not thrown");
		}
		catch (NullPointerException e) {}
		try {
			session.writenf((ByteBuffer)null, 0);
			fail("Exception not thrown");
		}
		catch (NullPointerException e) {}
		try {
			session.writenf((IByteBufferHolder)null);
			fail("Exception not thrown");
		}
		catch (NullPointerException e) {}
		
		assertTrue(session.write(new byte[0]).isSuccessful());
		assertTrue(session.write(new byte[3], 0, 0).isSuccessful());
		assertTrue(session.write(getBuffer(0,0)).isSuccessful());
		assertTrue(session.write(getBuffer(10,0), 0).isSuccessful());
		assertTrue(session.write(new byte[3], 1, 0).isSuccessful());
		assertTrue(session.write(new ByteBufferHolder()).isSuccessful());
		session.writenf(new byte[0]);
		session.writenf(new byte[3], 0, 0);
		session.writenf(getBuffer(0,0));
		session.writenf(getBuffer(10,0), 0);
		session.writenf(new byte[3], 1, 0);
		session.writenf(new ByteBufferHolder());
		
		assertOutOfBoundException(session, new byte[10], -1, 4);
		assertOutOfBoundException(session, new byte[10], 10, 1);
		assertOutOfBoundException(session, new byte[10], 0, -1);
		assertOutOfBoundException(session, new byte[10], 5, 6);
		assertOutOfBoundException(session, new byte[10], 0x7fffffff, 1);
		try {
			session.write(getBuffer(0,90), 11);
			fail("Exception not thrown");
		}
		catch (IndexOutOfBoundsException e) {}
		try {
			session.writenf(getBuffer(0,90), 11);
			fail("Exception not thrown");
		}
		catch (IndexOutOfBoundsException e) {}		
		try {
			session.write(getBuffer(0,90), -1);
			fail("Exception not thrown");
		}
		catch (IndexOutOfBoundsException e) {}
		try {
			session.writenf(getBuffer(0,90), -1);
			fail("Exception not thrown");
		}
		catch (IndexOutOfBoundsException e) {}		
		
		assertIllegalStateException(session, new byte[10], 0, 10);
		assertIllegalStateException(session, new byte[10], 1, 9);
		assertIllegalStateException(session, new byte[10], 0, 1);
		try {
			session.write(new byte[10]);
			fail("Exception not thrown");
		}
		catch (IllegalStateException e) {}
		try {
			session.writenf(new byte[10]);
			fail("Exception not thrown");
		}
		catch (IllegalStateException e) {}
		try {
			session.write(getBuffer(10,0));
			fail("Exception not thrown");
		}
		catch (IllegalStateException e) {}
		try {
			session.writenf(getBuffer(10,0));
			fail("Exception not thrown");
		}
		catch (IllegalStateException e) {}
		try {
			session.write(getBuffer(10,0), 3);
			fail("Exception not thrown");
		}
		catch (IllegalStateException e) {}
		try {
			session.writenf(getBuffer(10,0), 3);
			fail("Exception not thrown");
		}
		catch (IllegalStateException e) {}
		
	}	
	
	@Test
	public void testBufferOverflow() throws Exception {

		//unwrap overflow with allocator exception
		TestAllocator allocator = new TestAllocator(false, false);
		s = new Server(PORT, true);
		s.allocator = allocator;
		c = new Client(PORT, true);
		
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		
		TestSSLEngine engine = getSSLEngine((SSLSession) s.getSession());
		
		engine.unwrapCounter = 0;
		engine.unwrapResult[0] = new SSLEngineResult(SSLEngineResult.Status.BUFFER_OVERFLOW,
				SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING,
				0, 0);
		allocator.ensureException = true;
		SSLSession session = (SSLSession) c.getSession();
		session.write(new Packet(PacketType.NOP, "1234").toBytes());
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertEquals("DS|DR|EXC|SCL|SEN|", s.getRecordedData(true));
		
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		//wrap overflow with allocator exception (inTask = true)
		allocator.ensureException = false;
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		c.allocator = allocator;
		
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		
		engine = getSSLEngine((SSLSession) c.getSession());
		
		engine.wrapCounter = 0;
		engine.wrapResult[0] = new SSLEngineResult(SSLEngineResult.Status.BUFFER_OVERFLOW,
				SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING,
				0, 0);
		allocator.ensureException = true;
		session = (SSLSession) c.getSession();
		session.write(new Packet(PacketType.NOP, "1234").toBytes());
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("EXC|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));
		
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		//wrap overflow with allocator exception (inTask = false)
		allocator.ensureException = false;
		s = new Server(PORT, true);
		s.allocator = allocator;
		c = new Client(PORT, true);
		
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		
		engine = getSSLEngine((SSLSession) s.getSession());
		
		engine.wrapCounter = 0;
		engine.wrapResult[0] = new SSLEngineResult(SSLEngineResult.Status.BUFFER_OVERFLOW,
				SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING,
				0, 0);
		allocator.ensureException = true;
		session = (SSLSession) c.getSession();
		session.write(new Packet(PacketType.ECHO, "1234").toBytes());
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertEquals("DS|DR|ECHO(1234)|EXC|SCL|SEN|", s.getRecordedData(true));
		
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		//unwrap overflow
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		
		ByteBuffer buf = getBuffer((SSLSession) s.getSession(), "inAppBuffer");
		ByteBuffer buf2 = ByteBuffer.allocate(buf.position());
		buf.flip();
		buf2.put(buf);
		setBuffer((SSLSession) s.getSession(), "inAppBuffer", buf2);
		
		session = (SSLSession) c.getSession();
		session.write(new Packet(PacketType.NOP, "1234").toBytes());
		session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertTLSVariants("?{DS|}SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertTLSVariants("DS|DR|NOP(1234)|?{DS|}SCL|SEN|", s.getRecordedData(true));

		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		//unwrap overflow
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		waitFor(50);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		
		buf = getBuffer((SSLSession) c.getSession(), "outNetBuffer");
		buf2 = ByteBuffer.allocate(buf.position());
		buf.flip();
		buf2.put(buf);
		setBuffer((SSLSession) s.getSession(), "outNetBuffer", buf2);
		
		session = (SSLSession) c.getSession();
		session.write(new Packet(PacketType.NOP, "1234").toBytes());
		session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertTLSVariants("DS|?{DS|}SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertTLSVariants("DS|DR|NOP(1234)|?{DS|}SCL|SEN|", s.trimRecordedData(CLIENT_RDY_TAIL));

		c.stop(TIMEOUT);
		s.stop(TIMEOUT);		
	}
	
	@Test
	public void testByfferAllocationOnRead() throws Exception {
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		
		waitFor(100);
		
		ByteBuffer buf = getBuffer((SSLSession) s.getSession(), "inNetBuffer");
		ByteBuffer buf2 = ByteBuffer.allocate(buf.position());
		buf.flip();
		buf2.put(buf);
		setBuffer((SSLSession) s.getSession(), "inNetBuffer", buf2);
		
		SSLSession session = (SSLSession) c.getSession();
		session.write(new Packet(PacketType.NOP, "1234").toBytes());
		session.close();
		
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertTLSVariants("DS|DR|NOP(1234)|?{DS|}SCL|SEN|", s.getRecordedData(true));

		c.stop(TIMEOUT);
		s.stop(TIMEOUT);		
		
		//with allocator exception
		TestAllocator allocator = new TestAllocator(false, false);
		s = new Server(PORT, true);
		s.allocator = allocator;
		c = new Client(PORT, true);
		
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);

		waitFor(100);
		
		buf = getBuffer((SSLSession) c.getSession(), "inNetBuffer");
		buf2 = ByteBuffer.allocate(buf.position());
		buf.flip();
		buf2.put(buf);
		setBuffer((SSLSession) s.getSession(), "inNetBuffer", buf2);
		
		allocator.ensureException = true;
		session = (SSLSession) c.getSession();
		session.write(new Packet(PacketType.NOP, "1234").toBytes());
		
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertEquals("DS|DR|EXC|SCL|SEN|", s.getRecordedData(true));

		c.stop(TIMEOUT);
		s.stop(TIMEOUT);		
	}
	
	String multiString(String s, int count) {
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i<count; ++i) {
			sb.append(s);
		}
		return sb.toString();
	}
	
	@Test
	public void testWriteBigData() throws Exception {
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);

		byte[] data = new byte[1024];
		Arrays.fill(data, (byte)'A');
		c.write(new Packet(PacketType.BIG_NOP, new String(data)));
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS|"+multiString("DR|", 2) + "BIG_NOP(1024)|", s.getRecordedData(true));
		
		data = new byte[20000];
		Arrays.fill(data, (byte)'A');
		c.write(new Packet(PacketType.BIG_NOP, new String(data)));
		s.waitForDataRead(TIMEOUT);
		assertEquals(multiString("DR|", 20) + "BIG_NOP(20000)|", s.getRecordedData(true));

		c.stop(TIMEOUT);
		s.stop(TIMEOUT);		
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		
		s = new Server(PORT, true);
		s.directAllocator = true;
		c = new Client(PORT, true);
		c.directAllocator = true;
		
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);

		data = new byte[1024];
		Arrays.fill(data, (byte)'A');
		c.write(new Packet(PacketType.BIG_NOP, new String(data)));
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS|" + multiString("DR|", 2) + "BIG_NOP(1024)|", s.getRecordedData(true));
		
		data = new byte[20000];
		Arrays.fill(data, (byte)'A');
		c.write(new Packet(PacketType.BIG_NOP, new String(data)));
		s.waitForDataRead(TIMEOUT);
		assertEquals(multiString("DR|", 20) + "BIG_NOP(20000)|", s.getRecordedData(true));

		c.stop(TIMEOUT);
		s.stop(TIMEOUT);		
	}
	
	@Test
	public void testReleaseOfAllocatedBuffers() throws Exception {
		TestHandler handler = new TestHandler("Test");
		handler.engine = new TestSSLEngine(new TestSSLSession());
		TestAllocator allocator = handler.allocator;
		
		assertEquals(0, allocator.getSize());
		SSLSession session = new SSLSession(handler, true);
		assertEquals(0, allocator.getSize());
		try {
			session.write(new byte[10]);
			fail("Exception not thrown");
		}
		catch (IllegalSessionStateException e) {}
		assertEquals(0, allocator.getSize());
		assertEquals(0, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());

		allocator = new TestAllocator(false, true);
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		c.allocator = allocator;
		c.minInBufferCapacity = 1024*10;
		c.minOutBufferCapacity = 1024*10;
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals(6, allocator.getSize());
		assertEquals(6, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		waitFor(100);
		assertEquals(0, allocator.getSize());
		assertEquals(6, allocator.getAllocatedCount());
		assertEquals(6, allocator.getReleasedCount());

		allocator = new TestAllocator(false, true);
		c = new Client(PORT, true);
		c.minInBufferCapacity = 30000;
		c.minOutBufferCapacity = 30000;
		c.allocator = allocator;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		
		waitFor(100);
		
		byte[] data = new byte[20000];
		Arrays.fill(data, (byte)'A');
		c.getSession().write(new Packet(PacketType.BIG_NOP, new String(data)).toBytes());
		s.waitForDataRead(TIMEOUT);
		waitFor(100);
		assertEquals(6, allocator.getSize());
		assertEquals(7, allocator.getAllocatedCount());
		assertEquals(1, allocator.getReleasedCount());
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		waitFor(100);
		assertEquals(0, allocator.getSize());
		assertEquals(7, allocator.getAllocatedCount());
		assertEquals(7, allocator.getReleasedCount());

		allocator = new TestAllocator(false, false);
		c = new Client(PORT, true);
		c.minInBufferCapacity = 30000;
		c.minOutBufferCapacity = 30000;
		c.allocator = allocator;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		
		waitFor(100);
		
		data = new byte[20000];
		Arrays.fill(data, (byte)'A');
		c.getSession().write(new Packet(PacketType.BIG_NOP, new String(data)).toBytes());
		s.waitForDataRead(TIMEOUT);
		waitFor(100);
		assertEquals(7, allocator.getSize());
		assertEquals(7, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		waitFor(100);
		assertEquals(7, allocator.getSize());
		assertEquals(7, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		
		s.stop(TIMEOUT);
	}

	@Test
	public void testDelegatedTaskException() throws Exception {
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		c.useTestSession = true;
		c.exceptionRecordException = true;
		
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);

		waitFor(100);
		
		TestSSLEngine engine = getSSLEngine((SSLSession) c.getSession());
		engine.needTaskCounter = 1;
		engine.delegatedTaskCounter = 1;
		engine.delegatedTaskException = new NullPointerException("E1");

		
		c.write(new Packet(PacketType.NOP));
		
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("EXC|(E1)|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));
	}
	
	@Test
	public void testWrapException() throws Exception {
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		
		TestSSLEngine engine = getSSLEngine((SSLSession) c.getSession());
		engine.wrapException = new SSLException("");
		c.write(new Packet(PacketType.NOP, ""));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("EXC|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		
		c = new Client(PORT, true);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		
		engine = getSSLEngine((SSLSession) c.getSession());
		engine.wrapException = new SSLException("");
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("EXC|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);

		c = new Client(PORT, true);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);

		engine = getSSLEngine((SSLSession) c.getSession());
		engine.wrapException = new SSLException("");
		c.throwInException = true;
		c.write(new Packet(PacketType.NOP, ""));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("EXC|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		
		c = new Client(PORT, true);
		c.allocator = new TestAllocator(false, true);
		c.optimizeDataCopying = true;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);

		engine = getSSLEngine((SSLSession) c.getSession());
		engine.wrapException = new SSLException("");
		((SSLSession) c.getSession()).beginHandshake();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("EXC|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		
		
		
	}

	@Test
	public void testUnwrapException() throws Exception {
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);

		TestSSLEngine engine = getSSLEngine((SSLSession) s.getSession());
		engine.unwrapException = new SSLException("");
		c.write(new Packet(PacketType.NOP, ""));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|DR|EXC|SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
	}
	
	@Test
	public void testIncidentException() throws Exception {
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);

		TestSSLEngine engine = getSSLEngine((SSLSession) s.getSession());
		engine.unwrapException = new SSLException("");
		c.throwInIncident = true;
		c.write(new Packet(PacketType.NOP, ""));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|DR|EXC|SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|EXC|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
	}
	
	void resumeWithDelay(final SSLSession session, final long delay) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					waitFor(delay);
				} catch (InterruptedException e) {
				}
				session.resumeWrite();
			}
			
		}).start();
		
	}
	
	@Test
	public void testWriteFuture() throws Exception {

		s = new Server(PORT, true);
		c = new Client(PORT, true);
		
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);

		SSLSession session = (SSLSession) c.getSession();
		
		session.suspendWrite();
		IFuture<Void> f = session.write(new Packet(PacketType.NOP, "").toBytes());
		resumeWithDelay(session, 500);
		long time = System.currentTimeMillis();
		f.sync(TIMEOUT);
		time = System.currentTimeMillis() - time;
		assertTrue("expected 500 but was " + time, time > 490 && time < (compensateTime ? 600 : 520));
		
		session.suspendWrite();
		byte[] data = new byte[20000];
		Arrays.fill(data, (byte)'A');
		f = session.write(new Packet(PacketType.BIG_NOP, new String(data)).toBytes());
		resumeWithDelay(session, 500);
		time = System.currentTimeMillis();
		f.sync(TIMEOUT);
		time = System.currentTimeMillis() - time;
		assertTrue("expected 500 but was " + time, time > 490 && time < (compensateTime ? 600 : 520));
		
		session.suspendWrite();
		data = new byte[40000];
		Arrays.fill(data, (byte)'A');
		f = session.write(new Packet(PacketType.BIG_NOP, new String(data)).toBytes());
		resumeWithDelay(session, 500);
		time = System.currentTimeMillis();
		f.sync(TIMEOUT);
		time = System.currentTimeMillis() - time;
		assertTrue("expected 500 but was " + time, time > 490 && time < (compensateTime ? 600 : 520));
	}

	@Test
	public void testRenegotiation() throws Exception {
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);

		SSLSession session = (SSLSession) c.getSession();
		session.write(new Packet(PacketType.ECHO, "1").toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		waitFor(500);
		assertEquals("DS|DR|ECHO_RESPONSE(1)|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertEquals("DS|DR|ECHO(1)|DS|", s.getRecordedData(true));
		TestSSLEngine sengine = getSSLEngine((SSLSession) s.getSession());
		TestSSLEngine cengine = getSSLEngine((SSLSession) c.getSession());
		assertEquals("WHF|", sengine.getTrace());
		assertEquals(TLS1_3 ? "WHF|UHF|" : "UHF|", cengine.getTrace());
		
		session.beginHandshake();
		
		session.write(new Packet(PacketType.ECHO, "2").toBytes());
		waitFor(500);
		
		assertEquals("WHF|", sengine.getTrace());
		assertEquals(TLS1_3 ? "BH|WHF|UHF|" : "BH|UHF|", cengine.getTrace());
		if (TLS1_3) {
			assertTLSVariants("DS|?{DS|}DR|ECHO_RESPONSE(2)|", c.getRecordedData(true));
			assertEquals("DR|ECHO(2)|DS|", s.getRecordedData(true));
		}
		else {
			assertEquals("DS|DR|DR|DS|DR|DS|DR|ECHO_RESPONSE(2)|", c.getRecordedData(true));
			assertEquals("DR|DS|DR|DR|DS|DR|ECHO(2)|DS|", s.getRecordedData(true));
		}

		((SSLSession) s.getSession()).beginLazyHandshake();
		
		session.write(new Packet(PacketType.ECHO, "3").toBytes());
		waitFor(500);

		assertEquals(TLS1_3 ? "BH|WHF|UHF|" : "BH|WHF|", sengine.getTrace());
		assertEquals(TLS1_3 ? "WHF|" : "UHF|", cengine.getTrace());
		if (TLS1_3) {
			assertEquals("DS|DR|ECHO_RESPONSE(3)|DS|", c.getRecordedData(true));
			assertEquals("DR|ECHO(3)|DS|DR|", s.getRecordedData(true));
		}
		else {
			assertEquals("DS|DR|DS|DR|DR|DS|DR|ECHO_RESPONSE(3)|", c.getRecordedData(true));
			assertEquals("DR|ECHO(3)|DS|DR|DS|DR|DR|DS|", s.getRecordedData(true));
		}

		session.write(new Packet(PacketType.ECHO, "4").toBytes());
		waitFor(500);
		assertEquals("DS|DR|ECHO_RESPONSE(4)|", c.getRecordedData(true));
		assertEquals("DR|ECHO(4)|DS|", s.getRecordedData(true));
		
		session.close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertTLSVariants("DR|?{DS|}SCL|SEN|", s.getRecordedData(true));
			
	}
	
	private SocketChannel connect() throws IOException {
		SocketChannel channel = SocketChannel.open();
		channel.configureBlocking(true);
		channel.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT)); 
		return channel;
	}
	
	@Test
	public void testGentleCloseWithDifferentPeerInteraction() throws Exception {
		s = new Server(PORT, true);
		s.start();
		SocketChannel channel;

		//peer only shutdownOutput
		channel = connect();
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", s.getRecordedData(true));
		channel.shutdownOutput();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));
		channel.close();

		//peer sends random data
		channel = connect();
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", s.getRecordedData(true));
		channel.write(ByteBuffer.wrap("27736437rhfbbhjhfgjfg".getBytes()));
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|EXC|SCL|SEN|", s.getRecordedData(true));
		channel.close();
		
		//SSL established and peer sends close_notify but doesn't shutdown output
		if (!TLS1_3) {
			c = new Client(PORT, true);
			c.useTestSession = true;
			c.start();
			s.waitForSessionReady(TIMEOUT);
			c.waitForSessionReady(TIMEOUT);
			c.getRecordedData(true);
			s.getRecordedData("RDY|", true);

			TestOwnSSLSession session = (TestOwnSSLSession) c.getSession();
			session.sleepHandleClosingInProgress = 4000;
			session.close();
			AFTER_TIMEOUT = 4000;

			s.waitForSessionEnding(TIMEOUT);
			assertEquals("DS|DR|DS|SCL|SEN|", s.getRecordedData(true));
			assertEquals("", c.trimRecordedData(CLIENT_RDY_TAIL));

			c.waitForSessionEnding(4500);
			assertEquals("DS|SCL|SEN|", c.getRecordedData(true));

			//quick close after receiving close_notify
			c = new Client(PORT, true);
			c.useTestSession = true;
			c.start();
			s.waitForSessionReady(TIMEOUT);
			c.waitForSessionReady(TIMEOUT);
			c.getRecordedData(true);
			s.getRecordedData("RDY|", true);

			session = (TestOwnSSLSession) c.getSession();
			session.skipClose = 1;
			session.close();

			s.waitForSessionEnding(TIMEOUT);
			c.waitForSessionEnding(TIMEOUT);
			assertEquals("DS|DR|DS|SCL|SEN|", s.getRecordedData(true));
			assertEquals("DS|DR|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		}
		
		
	}	

	@Test
	public void testWaitForCloseMessage() throws Exception {
		s = new Server(PORT, true);
		s.start();

		c = new Client(PORT, true);
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		c.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertTLSVariants("DS|DR|?{DS|}SCL|SEN|", s.getRecordedData(true));
		assertTLSVariants("DS|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		
		c = new Client(PORT, true);
		c.waitForCloseMessage = !TLS1_3;
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		c.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertTLSVariants("DS|DR|?{DS|}SCL|SEN|", s.getRecordedData(true));
		assertTLSVariants("DS|?{DR|}SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		
		c = new Client(PORT, true);
		c.waitForCloseMessage = !TLS1_3;
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		c.getSession().quickClose();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertTLSVariants("DS|DR|?{DS|}SCL|SEN|", s.getRecordedData(true));
		assertTLSVariants("DS|?{DR|}SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));

		c = new Client(PORT, true);
		c.waitForCloseMessage = !TLS1_3;
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		waitFor(500);
		c.getSession().dirtyClose();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));
		assertEquals("SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));

	}

	private void testCloseInSessionCreatedEvent(StoppingType type) throws Exception{
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		c.closeInEvent = EventType.SESSION_CREATED;
		c.closeType = type;
		s.start();
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SEN|", c.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, c.getSession().closing);
		s.stop(TIMEOUT);

		s = new Server(PORT, true);
		c = new Client(PORT, true);
		s.closeInEvent = EventType.SESSION_CREATED;
		s.closeType = type;
		s.start();
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SEN|", s.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, s.getSession().closing);
		s.stop(TIMEOUT);

		s = new Server(PORT, true);
		s.closeInEvent = EventType.SESSION_CREATED;
		s.closeType = type;
		TestSelectorPool pool = new TestSelectorPool();
		s.start();
		s.getSelectLoop().setPool(pool);
		pool.getException = true;

		c = new Client(PORT, true);
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|SEN|", s.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, s.getSession().closing);
		s.stop(TIMEOUT);
	}	
	
	@Test
	public void testCloseInSessionCreatedEvent() throws Exception{
		testCloseInSessionCreatedEvent(StoppingType.GENTLE);
		testCloseInSessionCreatedEvent(StoppingType.QUICK);
		testCloseInSessionCreatedEvent(StoppingType.DIRTY);
	}	

	public void testCloseInSessionOpenedEvent(StoppingType type) throws Exception {
		s = new Server(PORT,true);
		c = new Client(PORT,true);
		c.closeInEvent = EventType.SESSION_OPENED;
		c.closeType = type;
		s.start();
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionOpen(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		c.getRecordedData("SOP|", true);
		assertEquals("SCL|SEN|", filterDSDR(c.getRecordedData(true)).replace("SESSION_EVENT_FAILURE|EXC|SCL", "SCL"));
		assertEquals(ClosingState.FINISHED, c.getSession().closing);
		s.stop(TIMEOUT);
		
		s = new Server(PORT,true);
		c = new Client(PORT,true);
		s.closeInEvent = EventType.SESSION_OPENED;
		s.closeType = type;
		s.start();
		c.start();
		s.waitForSessionOpen(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.getRecordedData("SOP|", true);
		assertEquals("SCL|SEN|", filterDSDR(s.getRecordedData(true)).replace("SESSION_EVENT_FAILURE|EXC|SCL", "SCL"));
		assertEquals(ClosingState.FINISHED, s.getSession().closing);
		s.stop(TIMEOUT);

		s = new Server(PORT,true);
		c = new Client(PORT,true);
		s.closeInEvent = EventType.SESSION_OPENED;
		s.closeType = type;
		s.start();
		s.getSelectLoop().setPool(new DefaultSelectorLoopPool(2));
		c.start();
		s.waitForSessionOpen(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.getRecordedData("SOP|", true);
		assertEquals("SCL|SEN|", filterDSDR(s.getRecordedData(true)).replace("SESSION_EVENT_FAILURE|EXC|SCL", "SCL"));
		assertEquals(ClosingState.FINISHED, s.getSession().closing);
		s.stop(TIMEOUT);
		
	}	
	
	@Test
	public void testCloseInSessionOpenedEvent() throws Exception {
		testCloseInSessionOpenedEvent(StoppingType.GENTLE);
		testCloseInSessionOpenedEvent(StoppingType.QUICK);
		testCloseInSessionOpenedEvent(StoppingType.DIRTY);
	}	
	
	public void testCloseInSessionReadyEvent(StoppingType type) throws Exception {
		s = new Server(PORT,true);
		c = new Client(PORT,true);
		c.closeInEvent = EventType.SESSION_READY;
		c.closeType = type;
		s.start();
		c.start();
		waitFor(100);
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		c.getRecordedData("RDY|", true);
		s.getRecordedData("RDY|", true);
		if (type == StoppingType.DIRTY) {
			assertEquals("SCL|SEN|", c.getRecordedData(true));
		}
		else {
			assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		}
		assertEquals(ClosingState.FINISHED, c.getSession().closing);
		s.stop(TIMEOUT);
		
		s = new Server(PORT,true);
		c = new Client(PORT,true);
		s.closeInEvent = EventType.SESSION_READY;
		s.closeType = type;
		s.start();
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.getRecordedData("RDY|", true);
		if (type == StoppingType.DIRTY) {
			assertEquals("SCL|SEN|", s.getRecordedData(true));
		}
		else {
			assertEquals("DS|SCL|SEN|", s.getRecordedData(true));
		}
		assertEquals(ClosingState.FINISHED, s.getSession().closing);
		s.stop(TIMEOUT);
	
		s = new Server(PORT,true);
		c = new Client(PORT,true);
		s.closeInEvent = EventType.SESSION_READY;
		s.closeType = type;
		s.start();
		s.getSelectLoop().setPool(new DefaultSelectorLoopPool(2));
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.getRecordedData("RDY|", true);
		if (type == StoppingType.DIRTY) {
			assertEquals("SCL|SEN|", s.getRecordedData(true));
		}
		else {
			assertEquals("DS|SCL|SEN|", s.getRecordedData(true));
		}
		assertEquals(ClosingState.FINISHED, s.getSession().closing);
		s.stop(TIMEOUT);
		
	}	
	
	@Test
	public void testCloseInSessionReadyEvent() throws Exception {
		testCloseInSessionReadyEvent(StoppingType.GENTLE);
		testCloseInSessionReadyEvent(StoppingType.QUICK);
		testCloseInSessionReadyEvent(StoppingType.DIRTY);
	}	
	
	String filterDSDR(String s) {
		while (true) {
			if (s.startsWith("DS|") || s.startsWith("DR|")) {
				s = s.substring(3);
			}
			else {
				return s;
			}
		}
	}
	
	private void testCloseInSessionClosedOrEndingEvent(StoppingType type, EventType event) throws Exception {
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		if (event != null) {
			c.closeInEvent = event;
			c.closeType = type;
		}
		s.start();
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		c.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		c.getRecordedData("RDY|", true);
		s.getRecordedData("RDY|", true);
		assertEquals("SCL|SEN|", filterDSDR(c.getRecordedData(true)));
		assertEquals("SCL|SEN|", filterDSDR(s.getRecordedData(true)));
		assertEquals(ClosingState.FINISHED, c.getSession().closing);
		s.stop(TIMEOUT);
		c.stop(TIMEOUT);
		
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		if (event != null) {
			c.closeInEvent = event;
			c.closeType = type;
		}
		s.start();
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		s.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		c.getRecordedData("RDY|", true);
		s.getRecordedData("RDY|", true);
		assertEquals("SCL|SEN|", filterDSDR(c.getRecordedData(true)));
		assertEquals("SCL|SEN|", filterDSDR(s.getRecordedData(true)));
		assertEquals(ClosingState.FINISHED, c.getSession().closing);
		s.stop(TIMEOUT);
		c.stop(TIMEOUT);
		
	}
	
	@Test
	public void testCloseInSessionClosedEvent() throws Exception {
		testCloseInSessionClosedOrEndingEvent(StoppingType.GENTLE, null);
		testCloseInSessionClosedOrEndingEvent(StoppingType.GENTLE, EventType.SESSION_CLOSED);
		testCloseInSessionClosedOrEndingEvent(StoppingType.QUICK, EventType.SESSION_CLOSED);
		testCloseInSessionClosedOrEndingEvent(StoppingType.DIRTY, EventType.SESSION_CLOSED);
	}
	
	@Test
	public void testCloseInSessionEndingEvent() throws Exception {
		testCloseInSessionClosedOrEndingEvent(StoppingType.GENTLE, null);
		testCloseInSessionClosedOrEndingEvent(StoppingType.GENTLE, EventType.SESSION_ENDING);
		testCloseInSessionClosedOrEndingEvent(StoppingType.QUICK, EventType.SESSION_ENDING);
		testCloseInSessionClosedOrEndingEvent(StoppingType.DIRTY, EventType.SESSION_ENDING);
	}
	
	static ByteBuffer[] diff(ByteBuffer[] b1, List<ByteBuffer> b2) {
		return diff(b1, b2.toArray(new ByteBuffer[b2.size()]));
	}
	
	static ByteBuffer[] diff(ByteBuffer[] b1, ByteBuffer[] b2) {
		List<ByteBuffer> l = new ArrayList<ByteBuffer>();
		
		b1 = b1.clone();
		b2 = b2.clone();
		
		for (int i=0; i<b1.length; ++i) {
			if (b1[i] == null) {
				continue;
			}
			for (int j=0; j<b2.length; ++j) {
				if (b2[j] == null) {
					continue;
				}
				if (b2[j] == b1[i]) {
					b2[j] = null;
					b1[i] = null;
					break;
				}
			}
		}
		
		for (int i=0; i<b1.length; ++i) {
			if (b1[i] != null) {
				l.add(b1[i]);
			}
		}
		for (int i=0; i<b2.length; ++i) {
			if (b2[i] != null) {
				l.add(b2[i]);
			}
		}
		return l.toArray(new ByteBuffer[l.size()]);
	}
	
	private void testOptimizedDataCopyingRead(DefaultCodecExecutor p) throws Exception {
		boolean codec = p != null;
		ByteBuffer[] nulls = new ByteBuffer[] {null,null,null,null};

		s = new Server(PORT, true);
		c = new Client(PORT, true);
		s.allocator = new TestAllocator(false, true);
		s.optimizeDataCopying = true;
		s.codecPipeline = p;
		s.ignoreAvailableException = true;
		s.start();
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		waitFor(50);
		SSLSession session = (SSLSession) s.getSession();
		int acount = s.allocator.getAllocatedCount();
		int rcount = s.allocator.getReleasedCount();
		assertEquals(0, s.allocator.getSize());
		assertArrayEquals(nulls, getAllBuffers(session));
		s.getRecordedData(true);
		c.getRecordedData(true);

		c.write(new Packet(PacketType.NOP));
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(50);
		assertEquals(codec ? "DR|BUF|NOP2()|" : "DR|BUF|NOP()|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals(acount+2, s.allocator.getAllocatedCount());
		assertEquals(rcount+1, s.allocator.getReleasedCount());
		assertTrue(s.bufferRead == s.allocator.getAllocated().get(acount+1));
		assertArrayEquals(nulls, getAllBuffers(session));
		assertEquals(1, s.allocator.getSize());
		s.allocator.release(s.bufferRead);
		assertEquals(0, s.allocator.getSize());
	
		ByteBuffer[] bs;
		if (!codec) { 
			byte[] bytes = new Packet(PacketType.NOP,"10").toBytes();
			c.getSession().write(bytes, 0, 2);
			c.waitForDataSent(TIMEOUT);
			waitFor(50);
			assertEquals(acount+4, s.allocator.getAllocatedCount());
			assertEquals(rcount+3, s.allocator.getReleasedCount());
			bs = getAllBuffers(session);
			assertTrue(bs[2] == s.allocator.getAllocated().get(acount+3));
			bs[2] = null;
			assertArrayEquals(nulls, bs);
			assertEquals(1, s.allocator.getSize());
			c.getSession().write(bytes, 2, bytes.length-2);
			s.waitForDataRead(TIMEOUT);
			c.waitForDataSent(TIMEOUT);
			waitFor(50);
			assertEquals(acount+5, s.allocator.getAllocatedCount());
			assertEquals(rcount+4, s.allocator.getReleasedCount());
			assertArrayEquals(nulls, getAllBuffers(session));
			assertEquals(1, s.allocator.getSize());
			session.release(s.bufferRead);
			assertEquals(0, s.allocator.getSize());
			assertEquals("DS|DS|", c.getRecordedData(true));
			assertEquals("DR|DR|BUF|NOP(10)|", s.getRecordedData(true));
			
			byte[] bytes2 = new byte[bytes.length*2];
			System.arraycopy(bytes, 0, bytes2, 0, bytes.length);
			System.arraycopy(bytes, 0, bytes2, bytes.length, bytes.length);
			c.getSession().write(bytes2);
			c.waitForDataSent(TIMEOUT);
			s.waitForDataRead(TIMEOUT);
			waitFor(50);
			assertArrayEquals(nulls, getAllBuffers(session));
			assertEquals(acount+8, s.allocator.getAllocatedCount());
			assertEquals(rcount+6, s.allocator.getReleasedCount());
			assertEquals(2, s.allocator.getSize());
			session.release(s.bufferRead);
			assertEquals(1, s.allocator.getSize());
			assertEquals("DS|", c.getRecordedData(true));
			assertEquals("DR|BUF|NOP(10)|BUF|NOP(10)|", s.getRecordedData(true));
			
			ByteBuffer b = session.allocate(1024);
			setBuffer(session, "inNetBuffer", b);
			c.getSession().write(new Packet(PacketType.NOP,"1").toBytes());
			c.waitForDataSent(TIMEOUT);
			s.waitForDataRead(TIMEOUT);
			waitFor(50);
			assertArrayEquals(nulls, getAllBuffers(session));
			assertEquals("DS|", c.getRecordedData(true));
			assertEquals("DR|BUF|NOP(1)|", s.getRecordedData(true));
			assertEquals(acount+11, s.allocator.getAllocatedCount());
			assertEquals(2, s.allocator.getSize());
			session.release(s.bufferRead);
			assertEquals(1, s.allocator.getSize());
		}
		
		s.allocator.ensureException = true;
		ByteBuffer bb = session.allocate(2);
		byte[] b = new Packet(PacketType.NOP).toBytes();
		bb.put(b[0]);
		setBuffer(session, "inNetBuffer", bb);
		c.getSession().write(b, 1, 2);
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertArrayEquals(nulls, getAllBuffers(session));
		assertEquals("DR|EXC|SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		s = new Server(PORT, true);
		c = new Client(PORT, true);
		s.allocator = new TestAllocator(false, false);
		s.optimizeDataCopying = true;
		s.codecPipeline = p;
		s.start();
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		waitFor(50);
		session = (SSLSession) s.getSession();
		acount = s.allocator.getAllocatedCount();
		rcount = s.allocator.getReleasedCount();
		assertEquals(7, s.allocator.getSize());
		bs = getAllBuffers(session);
		assertEquals(1, diff(bs, s.allocator.get()).length);
		s.getRecordedData(true);
		c.getRecordedData(true);		

		c.write(new Packet(PacketType.NOP));
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(50);
		assertEquals(codec ? "DR|BUF|NOP2()|" : "DR|NOP()|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		assertArrayEquals(bs, getAllBuffers(session));
		assertEquals(acount, s.allocator.getAllocatedCount());
		assertEquals(rcount, s.allocator.getReleasedCount());
		assertEquals(1, diff(bs, s.allocator.get()).length);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		s.allocator = new TestAllocator(false, true);
		s.codecPipeline = p;
		s.start();
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		waitFor(50);
		session = (SSLSession) s.getSession();
		acount = s.allocator.getAllocatedCount();
		rcount = s.allocator.getReleasedCount();
		assertEquals(6, s.allocator.getSize());
		bs = getAllBuffers(session);
		assertEquals(0, diff(bs, s.allocator.get()).length);
		s.getRecordedData(true);
		c.getRecordedData(true);		
		
		c.write(new Packet(PacketType.NOP));
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(50);
		assertEquals(codec ? "DR|BUF|NOP2()|" : "DR|NOP()|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		assertArrayEquals(bs, getAllBuffers(session));
		assertEquals(acount, s.allocator.getAllocatedCount());
		assertEquals(rcount, s.allocator.getReleasedCount());
		assertEquals(0, diff(bs, s.allocator.get()).length);
		
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
	}
	
	@Test
	public void testOptimizedDataCopyingRead() throws Exception {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		TestCodec codec = new TestCodec();
		codec.nopToNop2 = true;
		p.getPipeline().add("1", codec.BBBBD());
		testOptimizedDataCopyingRead(p);
		testOptimizedDataCopyingRead(null);
	}

	public void testOptimizedDataCopyingWrite(DefaultCodecExecutor p) throws Exception {
		boolean codec = p != null;
		ByteBuffer[] nulls = new ByteBuffer[] {null,null,null,null};
		
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		c.allocator = new TestAllocator(false, true);
		c.optimizeDataCopying = true;
		c.codecPipeline = p;
		s.start();
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		waitFor(50);
		SSLSession session = (SSLSession) c.getSession();
		int acount = c.allocator.getAllocatedCount();
		int rcount = c.allocator.getReleasedCount();
		assertEquals(0, c.allocator.getSize());
		assertArrayEquals(nulls, getAllBuffers(session));
		s.getRecordedData(true);
		c.getRecordedData(true);

		ByteBuffer b = session.allocate(1025);
		b.put(new Packet(PacketType.NOP).toBytes());
		b.flip();
		assertEquals(acount+1, c.allocator.getAllocatedCount());
		session.write(b);
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(50);
		assertEquals(codec ? "DR|NOP2()|" : "DR|NOP()|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals(rcount+2, c.allocator.getReleasedCount());
		assertArrayEquals(nulls, getAllBuffers(session));
		assertEquals(0, c.allocator.getSize());
		
		b = session.allocate(1025);
		b.put(new Packet(PacketType.NOP).toBytes(0, 10));
		b.flip();
		assertEquals(acount+3, c.allocator.getAllocatedCount());
		acount = c.allocator.getAllocatedCount();
		rcount = c.allocator.getReleasedCount();
		session.write(b, b.remaining()-10);
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(50);
		assertEquals(codec ? "DR|NOP2()|" : "DR|NOP()|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		assertArrayEquals(nulls, getAllBuffers(session));
		assertEquals(rcount+2, c.allocator.getReleasedCount());
		if (codec) {
			assertEquals(acount+1, c.allocator.getAllocatedCount());
		}
		else {
			assertEquals(acount+2, c.allocator.getAllocatedCount());			
		}
		assertEquals(1, c.allocator.getSize());
		session.release(b);
		assertEquals(0, c.allocator.getSize());
		
		
		byte[] bytes = new Packet(PacketType.NOP , "1234567890").toBytes();
		session.write(bytes, 0, 5).sync(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(50);
		assertEquals("DR|", s.getRecordedData(true));
		assertArrayEquals(nulls, getAllBuffers(session));
		session.write(bytes, 5, bytes.length-5).sync(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals(codec ? "DR|NOP2(1234567890)|" : "DR|NOP(1234567890)|", s.getRecordedData(true));
		assertArrayEquals(nulls, getAllBuffers(session));
		assertEquals(rcount+7, c.allocator.getReleasedCount());
		if (codec) {
			assertEquals(acount+3, c.allocator.getAllocatedCount());
		}
		else {
			assertEquals(acount+6, c.allocator.getAllocatedCount());			
		}
		assertEquals(0, c.allocator.getSize());

		if (!codec) {
			bytes = new Packet(PacketType.NOP, "12345").toBytes();
			EngineStreamHandler h = getInternal(session);
			synchronized (h.writeLock) {
				b = session.allocate(1024);
				b.put(bytes, 0, 4);
				b.flip();
				session.write(b);
				b = session.allocate(1024);
				b.put(bytes, 4, bytes.length-4);
				b.flip();
				session.write(b);
			}
			s.waitForDataRead(TIMEOUT);
			assertEquals("DR|NOP(12345)|", s.getRecordedData(true));	
		}
		
		c.stop(TIMEOUT);

		ByteBuffer[] bs;
		c = new Client(PORT, true);
		c.allocator = new TestAllocator(false, false);
		c.optimizeDataCopying = true;
		c.codecPipeline = p;
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		waitFor(50);
		session = (SSLSession) c.getSession();
		acount = c.allocator.getAllocatedCount();
		rcount = c.allocator.getReleasedCount();
		assertEquals(7, c.allocator.getSize());
		assertEquals(0, rcount);
		assertEquals(7, acount);
		bs = getAllBuffers(session);
		assertEquals(1, diff(bs, c.allocator.get()).length);
		s.getRecordedData(true);
		c.getRecordedData(true);
		
		b = session.allocate(1026);
		b.put(new Packet(PacketType.NOP).toBytes());
		b.flip();
		assertEquals(acount+1, c.allocator.getAllocatedCount());		
		bs = getBuffers(session, "outAppBuffers").clone();
		session.write(b);
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(50);
		assertEquals(codec ? "DR|NOP2()|" : "DR|NOP()|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals(0, c.allocator.getReleasedCount());
		assertEquals(acount+1, c.allocator.getAllocatedCount());		
		assertEquals(0, diff(bs, getBuffers(session, "outAppBuffers")).length);
		c.stop(TIMEOUT);
		
		c = new Client(PORT, true);
		c.allocator = new TestAllocator(false, true);
		c.codecPipeline = p;
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		waitFor(50);
		session = (SSLSession) c.getSession();
		acount = c.allocator.getAllocatedCount();
		rcount = c.allocator.getReleasedCount();
		assertEquals(6, c.allocator.getSize());
		assertEquals(1, rcount);
		assertEquals(7, acount);
		bs = getAllBuffers(session);
		assertEquals(0, diff(bs, c.allocator.get()).length);
		s.getRecordedData(true);
		c.getRecordedData(true);

		b = session.allocate(1027);
		b.put(new Packet(PacketType.NOP).toBytes());
		b.flip();
		assertEquals(acount+1, c.allocator.getAllocatedCount());		
		bs = getBuffers(session, "outAppBuffers").clone();
		session.write(b);
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(50);
		assertEquals(codec ? "DR|NOP2()|" : "DR|NOP()|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals(1, c.allocator.getReleasedCount());
		assertEquals(acount+1, c.allocator.getAllocatedCount());		
		assertEquals(0, diff(bs, getBuffers(session, "outAppBuffers")).length);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}
	
	@Test
	public void testOptimizedDataCopyingWrite() throws Exception {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		TestCodec codec = new TestCodec();
		codec.nopToNop2 = true;
		p.getPipeline().add("1", codec.BBBBE());
		testOptimizedDataCopyingWrite(p);
		testOptimizedDataCopyingWrite(null);
	}
	
	@Test
	public void testCloseAndResponseWithClose() throws Exception {
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		c.waitForCloseMessage = true;
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.loop.execute(new Runnable() {
			@Override
			public void run() {
				c.session.write(new Packet(PacketType.WRITE_AND_CLOSE, "12345").toBytes());
				c.session.close();
			}
		});
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		String r = c.getRecordedData(true);
		r = r.replace("DR|", "").replace("DS|", "");
		assertEquals("SCR|SOP|RDY|WRITE_AND_CLOSE_RESPONSE(12345)|SCL|SEN|", r);
		r = s.getRecordedData(true);
		r = r.replace("DR|", "").replace("DS|", "");
		assertEquals("SCR|SOP|RDY|WRITE_AND_CLOSE(12345)|SCL|SEN|", r);
	}
	
	void counter(EngineStreamSession s, String name, long value) throws Exception {
		Field f = AbstractEngineHandler.class.getDeclaredField(name);	
		f.setAccessible(true);
		f.set(getInternal(s), value);
	}

	long counter(EngineStreamSession s, String name) throws Exception {
		Field f = AbstractEngineHandler.class.getDeclaredField(name);	
		f.setAccessible(true);
		return f.getLong(getInternal(s));
	}
	
	@Test
	public void testCloseAndWaitForCloseMessage() throws Exception {
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		c.waitForCloseMessage = true;
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		s = new Server(PORT, true);
		c = new Client(PORT, true);
		c.waitForCloseMessage = true;
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		ByteBuffer[] bufs = getBuffers((SSLSession)s.session, "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP).toBytes());
		counter((EngineStreamSession) s.session, "appCounter", bufs[0].position());
		s.getRecordedData(true);
		c.getRecordedData(true);
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		assertEquals(3, counter((EngineStreamSession) s.session, "netCounter"));
		assertEquals(3, counter((EngineStreamSession) s.session, "appCounter"));
		
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		c.waitForCloseMessage = true;
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		EngineStreamHandler h = getInternal((EngineStreamSession) s.session);
		h.closing = ClosingState.SENDING;
		c.session.quickClose();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		c.waitForCloseMessage = true;
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		bufs = getBuffers((SSLSession)s.session, "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP).toBytes());
		counter((EngineStreamSession) s.session, "appCounter", bufs[0].position());
		assertEquals(0, counter((EngineStreamSession) s.session, "netCounter"));
		assertTrue(counter((EngineStreamSession) s.session, "appCounter") > 0);
		getSSLEngine((SSLSession) s.session).closeOutbound();
		s.getRecordedData(true);
		c.getRecordedData(true);
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		assertEquals(0, counter((EngineStreamSession) s.session, "appCounter"));
		assertEquals(0, counter((EngineStreamSession) s.session, "netCounter"));
	}
		
	@Test
	public void testCopyInBuffer() throws Exception {
		Client c = new Client(PORT);
		c.minInBufferCapacity = 16;
		c.maxInBufferCapacity = 64;
		SSLSession s1 = new SSLSession(c.createHandler(), true);
		c.minInBufferCapacity = 128;
		c.maxInBufferCapacity = 128;
		SSLSession s2 = new SSLSession(c.createHandler(), true);
		
		assertEquals(0, s2.getInBuffersForCopying().length);
		ByteBuffer b1 = s1.getInBuffer();
		ByteBuffer b2 = s2.getInBuffer();
		ByteBuffer b2i = getBuffer(s2, "inNetBuffer");
		assertEquals(0, s2.getInBuffersForCopying().length);
		
		assertEquals(16, b1.capacity());
		assertEquals(128, b2.capacity());

		assertNull(b2i);
		SessionTest.assertBuffer(b1, 0, 16);
		b1.clear();
		b2.clear();
		b2.put(SessionTest.bytes(10));
		assertEquals(1, s2.getInBuffersForCopying().length);
		assertEquals(10, s1.copyInBuffer(s2));
		SessionTest.assertBuffer(b1, 10, 16);

		s2.preCreated();
		b2 = s2.getInBuffer();
		b2i = getBuffer(s2, "inNetBuffer");
		assertNotNull(b2i);
		b1.clear();
		b2.clear();
		b2.put(SessionTest.bytes(10));
		assertEquals(10, s1.copyInBuffer(s2));
		SessionTest.assertBuffer(b1, 10, 16);
		
		b1.clear();
		b2.clear();
		byte[] b = SessionTest.bytes(10);
		b2.put(b, 5, 5);
		b2i.put(b, 0, 5);
		assertEquals(2, s2.getInBuffersForCopying().length);
		assertEquals(10, s1.copyInBuffer(s2));
		SessionTest.assertBuffer(b1, 10, 16);
	}
	
	@Test
	public void testConnectByProxy() throws Exception {
		p = new HttpProxy(PORT+1);
		p.start(TIMEOUT);
		s = new Server(PORT,true);
		s.start();
		c = new Client(PORT+1,true);
		c.addPreSession("C", false, new HttpProxyHandler(new URI("http://127.0.0.1:" + PORT)));
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		waitFor(50);
		c.getRecordedData(true);
		c.resetDataLocks();
		s.getRecordedData(true);
		s.resetDataLocks();
		c.session.writenf(new Packet(PacketType.ECHO, "22").toBytes());
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(22)|", c.getRecordedData(true));
		assertEquals("DR|ECHO(22)|DS|", s.getRecordedData(true));
		assertTrue(c.session instanceof SSLSession);
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|DS|SCL|SEN|", s.getRecordedData(true));
	}

	@Test
	public void testConnectByProxySsl() throws Exception {
		p = new HttpProxy(PORT+1);
		p.start(TIMEOUT, true);
		s = new Server(PORT,true);
		s.start();
		c = new Client(PORT+1,true);
		c.waitForCloseMessage = true;
		c.addPreSession("C", true, new HttpProxyHandler(new URI("http://127.0.0.1:" + PORT)) {
			@Override
			public ISessionConfig getConfig() {
				DefaultSessionConfig config = new DefaultSessionConfig() {
				
					@Override
					public SSLEngine createSSLEngine(boolean clientMode) throws SSLEngineCreateException {
						return Server.createSSLEngine(null, clientMode);
					}
				};
				config.setWaitForInboundCloseMessage(true);
				return config;
			}
		});
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		waitFor(50);
		c.getRecordedData(true);
		c.resetDataLocks();
		s.getRecordedData(true);
		s.resetDataLocks();
		c.session.writenf(new Packet(PacketType.ECHO, "22").toBytes());
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(22)|", c.getRecordedData(true));
		assertEquals("DR|ECHO(22)|DS|", s.getRecordedData(true));
		assertTrue(c.session instanceof SSLSession);
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|DR|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|DS|SCL|SEN|", s.getRecordedData(true));
	}
}
