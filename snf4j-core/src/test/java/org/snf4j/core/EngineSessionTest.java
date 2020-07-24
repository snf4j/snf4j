/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019-2020 SNF4J contributors
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

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLEngine;

import org.junit.After;
import org.junit.Test;
import org.snf4j.core.AbstractEngineHandler.Handshake;
import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.core.engine.IEngine;
import org.snf4j.core.logger.LoggerFactory;

public class EngineSessionTest {

	int TIMEOUT = 2000;
	int PORT = 7778;
	
	EngineServer s;
	EngineClient c;

	@After
	public void after() throws InterruptedException {
		if (c != null) {
			c.stop();
			c = null;
		}
		if (s != null) {
			s.stop();
			s = null;
		}
	}
	
	public static IEngine getEngine(EngineStreamSession session) throws Exception {
		Field f = EngineStreamSession.class.getDeclaredField("internal");
		
		f.setAccessible(true);
		Object o = f.get(session);
		f = AbstractEngineHandler.class.getDeclaredField("engine");
		f.setAccessible(true);
		
		return (IEngine) f.get(o);
	}
	
	public static SSLEngine getSSLEngine(EngineStreamSession session) throws Exception {
		InternalSSLEngine engine = (InternalSSLEngine) getEngine(session);
		Field f = InternalSSLEngine.class.getDeclaredField("engine");
		f.setAccessible(true);
		
		return (SSLEngine) f.get(engine);
	}
	
	private void waitFor(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}
	
	private void startAndWaitForReady(TestEngine server, TestEngine client, boolean directAllocation) throws Exception {
		s = new EngineServer(PORT, TIMEOUT);
		s.directAllocator = directAllocation;
		c = new EngineClient(PORT, TIMEOUT);
		c.directAllocator = directAllocation;
		s.start(server);
		c.start(client);
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
	}

	private void startAndWaitForReady(TestEngine server, TestEngine client) throws Exception {
		startAndWaitForReady(server, client, false);
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
	
	ByteBuffer getBuffer(EngineStreamSession session, String name) throws Exception {
		Field field = EngineStreamSession.class.getDeclaredField("internal");
		
		field.setAccessible(true);
		return getBuffer((EngineStreamHandler) field.get(session), name);
	}

	ByteBuffer[] getBuffers(EngineStreamSession session, String name) throws Exception {
		Field field = EngineStreamSession.class.getDeclaredField("internal");
		
		field.setAccessible(true);
		return getBuffers((EngineStreamHandler) field.get(session), name);
	}
	
	@SuppressWarnings("unchecked")
	AtomicReference<Handshake> getHandshake(EngineStreamSession session) throws Exception {
		Field field = EngineStreamSession.class.getDeclaredField("internal");
		field.setAccessible(true);
		Object internal = field.get(session);
		
		field = AbstractEngineHandler.class.getDeclaredField("handshake");
		field.setAccessible(true);
		return (AtomicReference<Handshake>) field.get(internal);
	}
	
	private void assertOneTask(String expected, String value) {
		int i = value.indexOf("GET_TASK|GET_TASK|");
		assertTrue(i >= 0);
		i = value.indexOf("|TASK|", i);
		assertTrue(i >= 0);
		value = value.substring(0, i) + value.substring(i+5);
		assertEquals(expected,value);
	}
	
	@Test
	public void testNotHandshaking() throws Exception {
		//initial state = NOT_HANDSHAKING
		TestEngine se = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		TestEngine ce = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		
		assertEquals(HandshakeStatus.NOT_HANDSHAKING, se.getHandshakeStatus());
		assertEquals(HandshakeStatus.NOT_HANDSHAKING, ce.getHandshakeStatus());
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce);
		
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		
		se.addRecord("W|NH|-|-|C|-|");
		ce.addRecord("W|NH|-|-|C|-|");
		c.getSession().close();
		
		c.waitForFinish(TIMEOUT);
		s.waitForFinish(TIMEOUT);
		
		assertEquals("INI|CR|OP|W0|RE|W3|CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("INI|CR|OP|W0|RE|U3|R789|CI|CO|CL|EN|FIN|", s.getTrace(true));
		c.stop();
		s.stop();
		
		//initial state = NEED_WRAP
		se = new TestEngine(HandshakeStatus.NEED_WRAP);
		ce = new TestEngine(HandshakeStatus.NEED_WRAP);
		
		assertEquals(HandshakeStatus.NEED_WRAP, se.getHandshakeStatus());
		assertEquals(HandshakeStatus.NEED_WRAP, ce.getHandshakeStatus());
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");

		startAndWaitForReady(se, ce);
		
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		
		se.addRecord("W|NH|-|-|C|-|");
		ce.addRecord("W|NH|-|-|C|-|");
		c.getSession().close();
		
		c.waitForFinish(TIMEOUT);
		s.waitForFinish(TIMEOUT);
		
		assertEquals("INI|CR|OP|W0|RE|W3|CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("INI|CR|OP|W0|RE|U3|R789|CI|CO|CL|EN|FIN|", s.getTrace(true));
		c.stop();
		s.stop();
		
		//initial state = NEED_UNWRAP
		se = new TestEngine(HandshakeStatus.NEED_UNWRAP);
		ce = new TestEngine(HandshakeStatus.NEED_UNWRAP);
		
		assertEquals(HandshakeStatus.NEED_UNWRAP, se.getHandshakeStatus());
		assertEquals(HandshakeStatus.NEED_UNWRAP, ce.getHandshakeStatus());
		se.addRecord("U|NH|-|-|OK|F|");
		ce.addRecord("U|NH|-|-|OK|F|");

		startAndWaitForReady(se, ce);
		
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		
		se.addRecord("W|NH|-|-|C|-|");
		ce.addRecord("W|NH|-|-|C|-|");
		c.getSession().close();
		
		c.waitForFinish(TIMEOUT);
		s.waitForFinish(TIMEOUT);
		
		assertEquals("INI|CR|OP|U0|RE|W3|CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("INI|CR|OP|U0|RE|U3|R789|CI|CO|CL|EN|FIN|", s.getTrace(true));
		c.stop();
		s.stop();
		
		//initial state = NEED_TASK
		se = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		ce = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		se.addTask();
		
		assertEquals(HandshakeStatus.NEED_TASK, se.getHandshakeStatus());
		assertEquals(HandshakeStatus.NOT_HANDSHAKING, ce.getHandshakeStatus());
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");

		startAndWaitForReady(se, ce);
		
		ce.addTask();
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		waitFor(500);

		assertOneTask("INI|CR|OP|W0|RE|GET_TASK|GET_TASK|W3|", c.getTrace(true));
		assertOneTask("INI|CR|OP|GET_TASK|GET_TASK|W0|RE|U3|R789|", s.getTrace(true));
		
		se.addRecord("W|NH|-|-|C|-|");
		ce.addRecord("W|NH|-|-|C|-|");
		c.getSession().close();
		
		c.waitForFinish(TIMEOUT);
		s.waitForFinish(TIMEOUT);
		
		assertEquals("CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("CI|CO|CL|EN|FIN|", s.getTrace(true));
	}

	@Test
	public void testHandshaking() throws Exception {
		//initial state = NOT_HANDSHAKING
		TestEngine se = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		TestEngine ce = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		
		assertEquals(HandshakeStatus.NOT_HANDSHAKING, se.getHandshakeStatus());
		assertEquals(HandshakeStatus.NOT_HANDSHAKING, ce.getHandshakeStatus());

		ce.addRecord("W|NU|-|123|OK|-|");
		se.addRecord("W|NU|-|-|OK|-|");
		se.addRecord("U|NW|123|-|OK|-|");
		se.addRecord("W|NH|-|456|OK|F|");
		ce.addRecord("U|NH|456|-|OK|F|");

		startAndWaitForReady(se, ce);
		
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		
		se.addRecord("W|NH|-|-|C|-|");
		ce.addRecord("W|NH|-|-|C|-|");
		c.getSession().close();
		
		c.waitForFinish(TIMEOUT);
		s.waitForFinish(TIMEOUT);
		
		assertEquals("INI|CR|OP|W0|U0|U3|RE|W3|CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("INI|CR|OP|W0|U0|U3|W0|RE|U3|R789|CI|CO|CL|EN|FIN|", s.getTrace(true));
		c.stop();
		s.stop();
		
		//initial state = NEED_WRAP
		se = new TestEngine(HandshakeStatus.NEED_WRAP);
		ce = new TestEngine(HandshakeStatus.NEED_WRAP);
		
		assertEquals(HandshakeStatus.NEED_WRAP, se.getHandshakeStatus());
		assertEquals(HandshakeStatus.NEED_WRAP, ce.getHandshakeStatus());

		ce.addRecord("W|NU|-|123|OK|-|");
		se.addRecord("W|NU|-|-|OK|-|");
		se.addRecord("U|NW|123|-|OK|-|");
		se.addRecord("W|NH|-|456|OK|F|");
		ce.addRecord("U|NH|456|-|OK|F|");

		startAndWaitForReady(se, ce);
		
		se.addRecord("W|NH|-|-|C|-|");
		ce.addRecord("W|NH|-|-|C|-|");
		c.getSession().close();
		
		c.waitForFinish(TIMEOUT);
		s.waitForFinish(TIMEOUT);
		
		assertEquals("INI|CR|OP|W0|U0|U3|RE|CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("INI|CR|OP|W0|U0|U3|W0|RE|CI|CO|CL|EN|FIN|", s.getTrace(true));
		c.stop();
		s.stop();

		//initial state = NEED_UNWRAP
		se = new TestEngine(HandshakeStatus.NEED_UNWRAP);
		ce = new TestEngine(HandshakeStatus.NEED_UNWRAP);
		
		assertEquals(HandshakeStatus.NEED_UNWRAP, se.getHandshakeStatus());
		assertEquals(HandshakeStatus.NEED_UNWRAP, ce.getHandshakeStatus());

		ce.addRecord("U|NW|-|-|OK|-|");
		ce.addRecord("W|NU|-|123|OK|-|");
		se.addRecord("U|NU|-|-|BU|-|");
		se.addRecord("U|NW|123|-|OK|-|");
		se.addRecord("W|NH|-|456|OK|F|");
		ce.addRecord("U|NH|456|-|OK|F|");
		
		startAndWaitForReady(se, ce);
		
		se.addRecord("W|NH|-|-|C|-|");
		ce.addRecord("W|NH|-|-|C|-|");
		c.getSession().close();
		
		c.waitForFinish(TIMEOUT);
		s.waitForFinish(TIMEOUT);
		
		assertEquals("INI|CR|OP|U0|W0|U0|U3|RE|CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("INI|CR|OP|U0|U3|W0|RE|CI|CO|CL|EN|FIN|", s.getTrace(true));
	}	
	
	private TestEngine createEngine(HandshakeStatus status, int min, int max) {
		TestEngine e = new TestEngine(status);
		e.appBufferSize=min;
		e.netBufferSize=min;
		e.maxAppBufferSize=max;
		e.maxNetBufferSize=max;
		return e;
	}
	
	private void assertCapacity(EngineStreamSession s, int inApp, int inNet, int outNet) throws Exception {
		assertEquals("inAppBuffer", inApp, getBuffer(s, "inAppBuffer").capacity());
		assertEquals("inNetBuffer", inNet, getBuffer(s, "inNetBuffer").capacity());
		assertEquals("outNetBuffer", outNet, getBuffer(s, "outNetBuffer").capacity());
	}

	@Test
	public void testWrappingBufferOverflow() throws Exception {
		TestEngine se = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 10);
		TestEngine ce = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 10);
		
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce);
		
		s.getTrace(true);
		c.getTrace(true);

		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);

		//buffer overflow successful
		ce.addRecord("W|NH|-|-|BO|-|");
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W3|W3|", c.getTrace(true));
		assertEquals("U3|R789|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);
		
		//buffer overflow successful (NEED_WRAP ignored)
		ce.addRecord("W|NW|-|-|BO|-|");
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W3|W3|", c.getTrace(true));
		assertEquals("U3|R789|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);

		//buffer overflow successful (NEED_UNWRAP ignored)
		ce.addRecord("W|NU|-|-|BO|-|");
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W3|W3|", c.getTrace(true));
		assertEquals("U3|R789|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);

		//buffer overflow successful (NEED_TASK ignored)
		ce.addRecord("W|NT|-|-|BO|-|");
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W3|W3|", c.getTrace(true));
		assertEquals("U3|R789|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);

		//buffer overflow successful update min mix
		ce.maxAppBufferSize += 1;
		ce.appBufferSize -= 1;
		ce.maxNetBufferSize += 2;
		ce.netBufferSize -= 2;
		ce.addRecord("W|NT|-|-|BO|-|");
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W3|W3|", c.getTrace(true));
		assertEquals("U3|R789|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);
		assertMinMax(c.getSession(), 10, 10, 8, 12);
		
		se.addRecord("W|NH|-|-|C|-|");
		ce.addRecord("W|NH|-|-|C|-|");
		c.getSession().close();

		s.waitForFinish(TIMEOUT);
		c.waitForFinish(TIMEOUT);
		
		assertEquals("CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("CI|CO|CL|EN|FIN|", s.getTrace(true));
	}

	public void assertMinMax(StreamSession session, int minApp, int maxApp, int minNet, int maxNet) throws Exception {
		Field f = EngineStreamSession.class.getDeclaredField("internal");
		
		f.setAccessible(true);
		Object o = f.get(session);
		Field f0 = EngineStreamHandler.class.getDeclaredField("maxAppBufferSize");
		f0.setAccessible(true);
		assertEquals(maxApp, f0.getInt(o));
		f0 = EngineStreamHandler.class.getDeclaredField("maxNetBufferSize");
		f0.setAccessible(true);
		assertEquals(maxNet, f0.getInt(o));
		f0 = AbstractEngineHandler.class.getDeclaredField("minAppBufferSize");
		f0.setAccessible(true);
		assertEquals(minApp, f0.getInt(o));
		f0 = AbstractEngineHandler.class.getDeclaredField("minNetBufferSize");
		f0.setAccessible(true);
		assertEquals(minNet, f0.getInt(o));		
	}
	
	@Test
	public void testUnwrappingBufferOverflow() throws Exception {
		TestEngine se = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 10);
		TestEngine ce = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 10);
		
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce);
		
		s.getTrace(true);
		c.getTrace(true);

		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);

		//buffer overflow successful
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|-|-|BO|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W3|", c.getTrace(true));
		assertEquals("U3|U3|R789|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);
		
		//buffer overflow successful (NEED_WRAP ignored)
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NW|-|-|BO|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W3|", c.getTrace(true));
		assertEquals("U3|U3|R789|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);

		//buffer overflow successful (NEED_UNWRAP ignored)
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NU|-|-|BO|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W3|", c.getTrace(true));
		assertEquals("U3|U3|R789|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);

		//buffer overflow successful (NEED_TASK ignored)
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NT|-|-|BO|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W3|", c.getTrace(true));
		assertEquals("U3|U3|R789|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);

		//buffer overflow update min max
		se.maxAppBufferSize += 1;
		se.appBufferSize -= 1;
		se.maxNetBufferSize += 2;
		se.netBufferSize -= 2;
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|-|-|BO|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W3|", c.getTrace(true));
		assertEquals("U3|U3|R789|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);
		assertMinMax(s.getSession(), 9, 11, 10, 10);
		
		se.addRecord("W|NH|-|-|C|-|");
		ce.addRecord("W|NH|-|-|C|-|");
		c.getSession().close();

		s.waitForFinish(TIMEOUT);
		c.waitForFinish(TIMEOUT);
		
		assertEquals("CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("CI|CO|CL|EN|FIN|", s.getTrace(true));
	}
	
	@Test
	public void testUnwrappingBufferUnderflow() throws Exception {
		TestEngine se = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 10);
		TestEngine ce = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 10);
		
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce);
		
		s.getTrace(true);
		c.getTrace(true);

		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);

		//buffer underflow
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|-|-|BU|-|");
		ce.addRecord("W|NH|4|7|OK|-|");
		se.addRecord("U|NH|4567|89|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		waitFor(100);
		c.getSession().write("4".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W3|W1|", c.getTrace(true));
		assertEquals("U3|U4|R89|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);

		//buffer underflow (NEED_UNWRAP)
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NU|-|-|BU|-|");
		ce.addRecord("W|NH|4|7|OK|-|");
		se.addRecord("U|NH|4567|89|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		waitFor(100);
		c.getSession().write("4".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W3|W1|", c.getTrace(true));
		assertEquals("U3|U4|R89|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);

		//buffer underflow (NEED_WRAP, NOT_HANDSHAKING)
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NW|-|-|BU|-|");
		se.addRecord("W|NH|-|-|OK|-|");
		ce.addRecord("W|NH|4|7|OK|-|");
		se.addRecord("U|NH|4567|89|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		waitFor(100);
		c.getSession().write("4".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W3|W1|", c.getTrace(true));
		assertEquals("U3|W0|U4|R89|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);

		//buffer underflow (NEED_WRAP, NEED_UNWRAP)
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NW|-|-|BU|-|");
		se.addRecord("W|NU|-|-|OK|-|");
		ce.addRecord("W|NH|4|7|OK|-|");
		se.addRecord("U|NH|4567|89|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		waitFor(100);
		c.getSession().write("4".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W3|W1|", c.getTrace(true));
		assertEquals("U3|W0|U4|R89|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);
		
		se.addRecord("W|NH|-|-|C|-|");
		ce.addRecord("W|NH|-|-|C|-|");
		c.getSession().close();

		s.waitForFinish(TIMEOUT);
		c.waitForFinish(TIMEOUT);
		
		assertEquals("CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("CI|CO|CL|EN|FIN|", s.getTrace(true));		
		
	}
	
	@Test
	public void testWrappingWithNoRoomToExtendBuffers() throws Exception {
		TestEngine se = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 10);
		TestEngine ce = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 10);
		
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce);
		
		s.getTrace(true);
		c.getTrace(true);

		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);
		
		//app size < min size
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W3|", c.getTrace(true));
		assertEquals("U3|R789|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);

		//app size = min size
		ce.addRecord("W|NH|1122334455|6677889900|OK|-|");
		se.addRecord("U|NH|6677889900|1627384950|OK|-|");
		c.getSession().write("1122334455".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W10|", c.getTrace(true));
		assertEquals("U10|R1627384950|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);
		
		//app size > min size
		ce.addRecord("W|NH|1122334455|6677889900|OK|-|");
		se.addRecord("U|NH|6677889900|1627384950|OK|-|");
		ce.addRecord("W|NH|9|0|OK|-|");
		se.addRecord("U|NH|0|1|OK|-|");
		c.getSession().write("11223344559".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		waitFor(100);
		s.resetDataRead();
		assertEquals("W11|W1|", c.getTrace(true));
		assertEquals("U10|R1627384950|U1|R1|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);
		
		//buffer overflow successful
		ce.addRecord("W|NH|-|-|BO|-|");
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W3|W3|", c.getTrace(true));
		assertEquals("U3|R789|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);
		
		//buffer overflow failed
		ByteBuffer outNet = getBuffer(c.getSession(), "outNetBuffer");
		outNet.put("1".getBytes());
		ce.addRecord("W|NH|-|-|BO|-|");
		c.getSession().write("23".getBytes());
		
		s.waitForFinish(TIMEOUT);
		c.waitForFinish(TIMEOUT);

		assertEquals("W2|EX|CI|CO|CL|EN|FIN|", c.getTrace(true));
		assertEquals("CI|CO|CL|EN|FIN|", s.getTrace(true));
	}	

	@Test
	public void testWrappingWithRoomToExtendBuffers() throws Exception {
		TestEngine se = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 20);
		TestEngine ce = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 20);
		ce.traceAllWrapMethods = true;
		
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce);
		
		s.getTrace(true);
		c.getTrace(true);

		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);
		
		//app size < min size
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("w|W3|", c.getTrace(true));
		assertEquals("U3|R789|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);

		//app size = min size
		ce.addRecord("W|NH|1122334455|6677889900|OK|-|");
		se.addRecord("U|NH|6677889900|1627384950|OK|-|");
		c.getSession().write("1122334455".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("w|W10|", c.getTrace(true));
		assertEquals("U10|R1627384950|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);
		
		//app size > min size
		ce.addRecord("W|NH|1122334455|6677889900|OK|-|");
		se.addRecord("U|NH|6677889900|1627384950|OK|-|");
		ce.addRecord("W|NH|9|0|OK|-|");
		se.addRecord("U|NH|0|1|OK|-|");
		c.getSession().write("11223344559".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		waitFor(100);
		s.resetDataRead();
		assertEquals("W11|w|W1|", c.getTrace(true));
		assertEquals("U11|R1627384950|U1|R1|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 20, 10);

		//buffer overflow successful
		ByteBuffer outNet = getBuffer(c.getSession(), "outNetBuffer");
		outNet.put("1".getBytes());
		ce.addRecord("W|NH|-|-|BO|-|");
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|1456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("w|W3|w|W3|", c.getTrace(true));
		assertEquals("U4|R789|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 20);
		assertCapacity(s.getSession(), 10, 20, 10);

		se.addRecord("W|NH|-|-|C|-|");
		ce.addRecord("W|NH|-|-|C|-|");
		c.getSession().close();

		s.waitForFinish(TIMEOUT);
		c.waitForFinish(TIMEOUT);
		
		assertEquals("CO|w|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("CI|CO|CL|EN|FIN|", s.getTrace(true));
		
		c.stop();
		s.stop();
		
		se = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 20);
		ce = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 20);
		
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce);
		
		s.getTrace(true);
		c.getTrace(true);

		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);
		
		//app size = min size and buffer overflow
		outNet = getBuffer(c.getSession(), "outNetBuffer");
		outNet.put("1".getBytes());
		ce.addRecord("W|NH|-|-|BO|-|");
		ce.addRecord("W|NH|1122334455|6677889900|OK|-|");
		se.addRecord("U|NH|16677889900|1627384950|OK|-|");
		c.getSession().write("1122334455".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		s.resetDataRead();
		assertEquals("W10|W10|", c.getTrace(true));
		assertEquals("U11|R1627384950|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 20);
		assertCapacity(s.getSession(), 10, 20, 10);

		se.addRecord("W|NH|-|-|C|-|");
		ce.addRecord("W|NH|-|-|C|-|");
		c.getSession().close();

		s.waitForFinish(TIMEOUT);
		c.waitForFinish(TIMEOUT);
		
		assertEquals("CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("CI|CO|CL|EN|FIN|", s.getTrace(true));		
		
		c.stop();
		s.stop();
		
		se = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 20);
		ce = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 20);
		
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce);
		
		s.getTrace(true);
		c.getTrace(true);

		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);
		
		//update min max
		se.maxAppBufferSize += 1;
		se.appBufferSize -= 1;
		se.maxNetBufferSize += 2;
		se.netBufferSize -= 2;
		outNet = getBuffer(c.getSession(), "outNetBuffer");
		outNet.put("1".getBytes());
		ce.addRecord("W|NH|-|-|BO|-|");
		ce.addRecord("W|NH|1122334455|6677889900|OK|-|");
		se.addRecord("U|NH|16677889900|1627384950|OK|-|");
		c.getSession().write("1122334455".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		s.resetDataRead();
		assertEquals("W10|W10|", c.getTrace(true));
		assertEquals("U11|R1627384950|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 20);
		assertCapacity(s.getSession(), 10, 22, 10);
		assertMinMax(s.getSession(), 10, 20, 8, 22);

		se.addRecord("W|NH|-|-|C|-|");
		ce.addRecord("W|NH|-|-|C|-|");
		c.getSession().close();

		s.waitForFinish(TIMEOUT);
		c.waitForFinish(TIMEOUT);
		
		assertEquals("CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("CI|CO|CL|EN|FIN|", s.getTrace(true));		
	
	}	

	@Test
	public void testUnwrappingWithNoRoomToExtendBuffers() throws Exception {
		TestEngine se = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 10);
		TestEngine ce = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 10);
		
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce);
		
		s.getTrace(true);
		c.getTrace(true);

		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);
		
		//app size < min size
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W3|", c.getTrace(true));
		assertEquals("U3|R789|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);

		//app size = min size
		ce.addRecord("W|NH|1122334455|6677889900|OK|-|");
		se.addRecord("U|NH|6677889900|1234567890|OK|-|");
		c.getSession().write("1122334455".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W10|", c.getTrace(true));
		assertEquals("U10|R1234567890|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);
		
		//buffer overflow failed
		waitFor(100);
		ByteBuffer inApp = getBuffer(s.getSession(), "inAppBuffer");
		inApp.put("1".getBytes());
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|-|-|BO|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		
		s.waitForFinish(TIMEOUT);
		c.waitForFinish(TIMEOUT);
		
		assertEquals("W3|CI|CO|CL|EN|FIN|", c.getTrace(true));
		assertEquals("U3|EX|CI|CO|CL|EN|FIN|", s.getTrace(true));
	}	

	@Test
	public void testUnwrappingWithRoomToExtendBuffers() throws Exception {
		TestEngine se = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 20);
		TestEngine ce = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 20);
		
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce);
		
		s.getTrace(true);
		c.getTrace(true);

		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);
		
		//app size < min size
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W3|", c.getTrace(true));
		assertEquals("U3|R789|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);
		waitFor(100);
		
		//app size = min size
		ce.addRecord("W|NH|1122334455|6677889900|OK|-|");
		se.addRecord("U|NH|6677889900|1234567890|OK|-|");
		c.getSession().write("1122334455".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W10|", c.getTrace(true));
		assertEquals("U10|R1234567890|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 10, 10, 10);
		waitFor(100);
		
		//buffer overflow successful
		ByteBuffer inApp = getBuffer(s.getSession(), "inAppBuffer");
		inApp.put("1".getBytes());
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|-|-|BO|-|");
		se.addRecord("U|NH|456|2233445566|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W3|", c.getTrace(true));
		assertEquals("U3|U3|R12233445566|", s.getTrace(true));
		assertCapacity(c.getSession(), 10, 10, 10);
		assertCapacity(s.getSession(), 20, 10, 10);

		se.addRecord("W|NH|-|-|C|-|");
		ce.addRecord("W|NH|-|-|C|-|");
		c.getSession().close();
		
		s.waitForFinish(TIMEOUT);
		c.waitForFinish(TIMEOUT);
		
		assertEquals("CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("CI|CO|CL|EN|FIN|", s.getTrace(true));
	}	

	@Test
	public void testUnwrappingWithNoRoomForNetData() throws Exception {
		// heap buffer
		TestEngine se = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 10);
		TestEngine ce = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 10);

		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce);
		
		s.getTrace(true);
		c.getTrace(true);
		
		ByteBuffer inNet = getBuffer(s.getSession(), "inNetBuffer");
		inNet.put("1122334455".getBytes());
		ce.addRecord("W|NH|123|4|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		
		s.waitForFinish(TIMEOUT);
		c.waitForFinish(TIMEOUT);
		
		assertEquals("W3|CI|CO|CL|EN|FIN|", c.getTrace(true));
		assertEquals("EX|CI|CO|CL|EN|FIN|", s.getTrace(true));
		
		c.stop();
		s.stop();
		
		// direct buffer
		se = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 10);
		ce = createEngine(HandshakeStatus.NOT_HANDSHAKING, 10, 10);

		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce, true);
		
		s.getTrace(true);
		c.getTrace(true);
		
		inNet = getBuffer(s.getSession(), "inNetBuffer");
		inNet.put("1122334455".getBytes());
		ce.addRecord("W|NH|123|4|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		
		s.waitForFinish(TIMEOUT);
		c.waitForFinish(TIMEOUT);
		
		assertEquals("W3|CI|CO|CL|EN|FIN|", c.getTrace(true));
		assertEquals("EX|CI|CO|CL|EN|FIN|", s.getTrace(true));
	}
	
	@Test
	public void testClosing() throws Exception {
		//closing message not required
		TestEngine se = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		TestEngine ce = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce, true);
		s.getTrace(true);
		c.getTrace(true);
		
		ce.addRecord("W|NH|-|-|C|-|");
		se.addRecord("W|NH|-|-|C|-|");
		c.getSession().close();
		
		c.waitForFinish(TIMEOUT);
		s.waitForFinish(TIMEOUT);
		
		assertEquals("CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("CI|CO|CL|EN|FIN|", s.getTrace(true));
		c.stop();
		s.stop();
		
		//closing message required
		se = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		ce = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce, true);
		s.getTrace(true);
		c.getTrace(true);
		
		ce.addRecord("W|NH|-|000|C|-|");
		se.addRecord("U|NW|000|-|C|-|");
		se.addRecord("W|NH|-|111|C|-|");
		c.getSession().close();
		
		c.waitForFinish(TIMEOUT);
		s.waitForFinish(TIMEOUT);
		
		assertEquals("CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("U3|CO|W0|CL|EN|FIN|", s.getTrace(true));
		c.stop();
		s.stop();

		//closing message required (initiator does not wait for closing message)
		se = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		ce = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce, true);
		s.getTrace(true);
		c.getTrace(true);
		
		ce.addRecord("W|NH|-|000|C|-|");
		ce.addRecord("U|NH|111|-|C|-|");
		se.addRecord("U|NW|000|-|C|-|");
		se.addRecord("W|NH|-|111|C|-|");
		c.getSession().close();
		
		c.waitForFinish(TIMEOUT);
		s.waitForFinish(TIMEOUT);
		
		assertEquals("CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("U3|CO|W0|CL|EN|FIN|", s.getTrace(true));
		c.stop();
		s.stop();
		
		//closing message required (initiator does not wait for closing message, peer delays responses)
		se = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		ce = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce, true);
		s.getTrace(true);
		c.getTrace(true);
		
		ce.addRecord("W|NH|-|000|C|-|");
		ce.addRecord("U|NH|111|-|C|-|");
		se.addRecord("U|NW|000|-|C|-|");
		se.addRecord("W|NH|-|111|C|-|");
		s.getSession().suspendRead();
		c.getSession().close();
		
		c.waitForFinish(TIMEOUT);
		waitFor(1000);
		assertEquals("CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertFalse(s.getSession().getCloseFuture().isDone());
		s.getSession().resumeRead();
		s.waitForFinish(TIMEOUT);
		
		assertEquals("U3|CO|W0|CL|EN|FIN|", s.getTrace(true));
		c.stop();
		s.stop();

		//closing message required (initiator waits for closing message, peer delays responses)
		se = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		ce = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce, true);
		c.waitForInboundCloseMessage = true;
		s.waitForInboundCloseMessage = true;
		s.getTrace(true);
		c.getTrace(true);
		
		ce.addRecord("W|NH|-|000|C|-|");
		ce.addRecord("U|NH|111|-|C|-|");
		se.addRecord("U|NW|000|-|C|-|");
		se.addRecord("W|NH|-|111|C|-|");
		s.getSession().suspendRead();
		c.getSession().close();
		waitFor(1000);
		assertFalse(c.getSession().getCloseFuture().isDone());
		assertTrue(c.getSession().write(new byte[] {1}).isCancelled());
		s.getSession().resumeRead();
		
		c.waitForFinish(TIMEOUT);
		s.waitForFinish(TIMEOUT);
		
		assertEquals("CO|W0|U3|CL|EN|FIN|", c.getTrace(true));
		assertEquals("U3|CO|W0|CL|EN|FIN|", s.getTrace(true));
		c.stop();
		s.stop();

		//closing message required (initiator waits for closing message, peer delays responses, initiator close dirtily)
		se = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		ce = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce, true);
		c.waitForInboundCloseMessage = true;
		s.waitForInboundCloseMessage = true;
		s.getTrace(true);
		c.getTrace(true);
		
		ce.addRecord("W|NH|-|000|C|-|");
		ce.addRecord("U|NH|111|-|C|-|");
		se.addRecord("U|NW|000|-|C|-|");
		se.addRecord("W|NH|-|111|C|-|");
		s.getSession().suspendRead();
		c.getSession().close();
		waitFor(1000);
		assertFalse(c.getSession().getCloseFuture().isDone());
		c.getSession().dirtyClose();
		c.waitForFinish(TIMEOUT);
		assertEquals("CO|W0|CL|EN|FIN|", c.getTrace(true));

		s.getSession().resumeRead();
		
		s.waitForFinish(TIMEOUT);
		
		assertEquals("U3|CO|W0|CL|EN|FIN|", s.getTrace(true));
		c.stop();
		s.stop();
		
	}	

	@Test
	public void testBeginHandshake() throws Exception {
		
		//lazy handshake before write, read and close
		TestEngine se = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		TestEngine ce = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce, true);
		s.getTrace(true);
		c.getTrace(true);

		c.getSession().beginLazyHandshake();
		s.getSession().beginLazyHandshake();
		waitFor(500);
		assertEquals("", c.getTrace(true));
		assertEquals("", s.getTrace(true));
		ce.addRecord("W|NH|123|456|OK|F|");
		se.addRecord("U|NH|456|789|OK|F|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("HAND|W3|", c.getTrace(true));
		assertEquals("HAND|U3|R789|", s.getTrace(true));
		
		AtomicReference<Handshake> handshake = getHandshake(c.getSession());
		assertEquals(Handshake.NONE, handshake.get());
		c.getSession().beginLazyHandshake();
		waitFor(500);
		assertEquals("", c.getTrace(true));
		
		ce.addRecord("W|NH|-|-|C|-|");
		se.addRecord("W|NH|-|-|C|-|");
		c.getSession().close();
		
		c.waitForFinish(TIMEOUT);
		s.waitForFinish(TIMEOUT);
		
		assertEquals("CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("CI|CO|CL|EN|FIN|", s.getTrace(true));
		c.stop();
		s.stop();

		//handshake before close
		se = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		ce = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce, true);
		s.getTrace(true);
		c.getTrace(true);
		
		c.getSession().beginHandshake();
		waitFor(100);
		assertEquals("HAND|", c.getTrace(true));
		
		ce.addRecord("W|NH|-|-|C|-|");
		se.addRecord("W|NH|-|-|C|-|");
		c.getSession().close();
		
		c.waitForFinish(TIMEOUT);
		s.waitForFinish(TIMEOUT);
		
		assertEquals("CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("CI|CO|CL|EN|FIN|", s.getTrace(true));
		c.stop();
		s.stop();

		//handshake exception
		se = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		ce = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce, true);
		s.getTrace(true);
		c.getTrace(true);
		
		ce.beginHandshakeException =true;
		c.getSession().beginHandshake();

		c.waitForFinish(TIMEOUT);
		s.waitForFinish(TIMEOUT);
		
		assertEquals("EX|CI|CO|CL|EN|FIN|", c.getTrace(true));
		assertEquals("CI|CO|CL|EN|FIN|", s.getTrace(true));
		c.stop();
		s.stop();
		
		//handshake immediately after lazy handshake
		se = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		ce = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce, true);
		s.getTrace(true);
		c.getTrace(true);
		
		c.getSession().beginLazyHandshake();
		waitFor(500);
		assertEquals("", c.getTrace(true));
		c.getSession().beginHandshake();
		waitFor(100);
		assertEquals("HAND|", c.getTrace(true));

		ce.addRecord("W|NH|-|-|C|-|");
		se.addRecord("W|NH|-|-|C|-|");
		c.getSession().close();
		
		c.waitForFinish(TIMEOUT);
		s.waitForFinish(TIMEOUT);
		
		assertEquals("CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("CI|CO|CL|EN|FIN|", s.getTrace(true));
		c.stop();
		s.stop();

		//handshake when previous handshake in progress
		se = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		ce = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce, true);
		s.getTrace(true);
		c.getTrace(true);
		
		c.getSession().beginHandshake();
		waitFor(100);
		assertEquals("HAND|", c.getTrace(true));
		c.getSession().beginHandshake();
		waitFor(500);
		assertEquals("", c.getTrace(true));

		ce.addRecord("W|NH|123|456|OK|F|");
		se.addRecord("U|NH|456|789|OK|F|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("W3|", c.getTrace(true));
		assertEquals("U3|R789|", s.getTrace(true));

		c.getSession().beginHandshake();
		waitFor(100);
		assertEquals("HAND|", c.getTrace(true));
		
		ce.addRecord("W|NH|-|-|C|-|");
		se.addRecord("W|NH|-|-|C|-|");
		c.getSession().close();
		
		c.waitForFinish(TIMEOUT);
		s.waitForFinish(TIMEOUT);
		
		assertEquals("CO|W0|CL|EN|FIN|", c.getTrace(true));
		assertEquals("CI|CO|CL|EN|FIN|", s.getTrace(true));
		c.stop();
		s.stop();
	
	}
	
	static class TestExecutor implements Executor {

		AtomicInteger count = new AtomicInteger();
		
		@Override
		public void execute(Runnable command) {
			count.incrementAndGet();
			DefaultExecutor.DEFAULT.execute(command);
		}
	}
	
	@Test
	public void testSetExecutor() throws Exception {
		EngineStreamSession session = new EngineStreamSession(new TestEngine(HandshakeStatus.NOT_HANDSHAKING), 
				new TestHandler("Handler"), 
				LoggerFactory.getLogger(this.getClass()));
	
		Executor e1 = new Executor() {

			@Override
			public void execute(Runnable command) {
			}
			
		};
		
		assertNull(session.getExecutor());
		session.setExecutor(e1);
		assertTrue(e1 == session.getExecutor());
		session.setExecutor(DefaultExecutor.DEFAULT);
		assertTrue(DefaultExecutor.DEFAULT == session.getExecutor());
		session.setExecutor(null);
		assertNull(session.getExecutor());
		
		TestEngine se = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		TestEngine ce = new TestEngine(HandshakeStatus.NOT_HANDSHAKING);
		se.addRecord("W|NH|-|-|OK|F|");
		ce.addRecord("W|NH|-|-|OK|F|");
		
		startAndWaitForReady(se, ce, true);
		s.getTrace(true);
		c.getTrace(true);
		
		session = c.getSession();
		assertTrue(DefaultExecutor.DEFAULT == session.getExecutor());
		session.setExecutor(e1);
		assertTrue(e1 == session.getExecutor());
		session.setExecutor(null);
		assertTrue(DefaultExecutor.DEFAULT == session.getExecutor());
		
		TestExecutor te1 = new TestExecutor();
		TestExecutor te2 = new TestExecutor();
		
		assertEquals(0, te1.count.get());
		assertEquals(0, te2.count.get());

		//use executor from the loop 
		session.setExecutor(null);
		c.loop.setExecutor(te1);
		ce.addTask();
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		waitFor(500);
		assertEquals(1, te1.count.get());
		assertEquals(0, te2.count.get());

		//use executor from the session 
		session.setExecutor(te2);
		ce.addTask();
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		waitFor(500);
		assertEquals(1, te1.count.get());
		assertEquals(1, te2.count.get());

		//exception from executor
		c.getTrace(true);
		s.getTrace(true);
		te2.count = null;
		ce.addTask();
		ce.addRecord("W|NH|123|456|OK|-|");
		se.addRecord("U|NH|456|789|OK|-|");
		c.getSession().write("123".getBytes());
		
		c.waitForFinish(TIMEOUT);
		s.waitForFinish(TIMEOUT);
		assertEquals("GET_TASK|EX|CI|CO|CL|EN|FIN|", c.getTrace(true));
		assertEquals("CI|CO|CL|EN|FIN|", s.getTrace(true));
		c.stop();
		s.stop();
		
	}
	
}
