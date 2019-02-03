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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.allocator.TestAllocator;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.session.IllegalSessionStateException;

public class SSLSessionTest {
	long TIMEOUT = 2000;
	int PORT = 7777;

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

	private byte[] getBytes(int size, int value) {
		return ByteUtils.getBytes(size, value);
	}
	
	private ByteBuffer getBuffer(int size, int value) {
		return ByteBuffer.wrap(getBytes(size, value));
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

	void setBuffer(InternalSSLHandler handler, String name, ByteBuffer buf) throws Exception {
		Field field = handler.getClass().getDeclaredField(name);
		
		field.setAccessible(true);
		field.set(handler,buf);
	}

	ByteBuffer getBuffer(SSLSession session, String name) throws Exception {
		Field field = session.getClass().getDeclaredField("internal");
		
		field.setAccessible(true);
		return getBuffer((InternalSSLHandler) field.get(session), name);
	}

	ByteBuffer[] getBuffers(SSLSession session, String name) throws Exception {
		Field field = session.getClass().getDeclaredField("internal");
		
		field.setAccessible(true);
		return getBuffers((InternalSSLHandler) field.get(session), name);
	}

	void setBuffer(SSLSession session, String name, ByteBuffer buf) throws Exception {
		Field field = session.getClass().getDeclaredField("internal");
		
		field.setAccessible(true);
		setBuffer((InternalSSLHandler) field.get(session), name, buf);
	}
	
	InternalSSLHandler getInternal(SSLSession session) throws Exception {
		Field field = session.getClass().getDeclaredField("internal");
		
		field.setAccessible(true);
		return (InternalSSLHandler) field.get(session);
	}

	TestSSLEngine getSSLEngine(SSLSession session) throws Exception {
		InternalSSLHandler internal = getInternal(session);
		Field field = internal.getClass().getDeclaredField("engine");
		
		field.setAccessible(true);
		return (TestSSLEngine) field.get(internal);
	}
	
	@Test
	public void testConstructor() throws Exception {
		TestHandler handler = new TestHandler("Test1");
		handler.engine = new TestSSLEngine(new TestSSLSession());

		SSLSession session = new SSLSession(handler, true);
		assertTrue(handler == session.getHandler());
		assertEquals("Test1", session.getName());
		
		session = new SSLSession("Test2", handler, true);
		assertTrue(handler == session.getHandler());
		assertEquals("Test2", session.getName());
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
		s.getRecordedData(true);
		session = (SSLSession) c.getSession(); 
		ByteBuffer[] bufs = getBuffers(session, "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "567").toBytes());
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|NOP(567)|SCL|SEN|", s.getRecordedData(true));
		
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
		s.getRecordedData(true);
		session = (SSLSession) c.getSession(); 
		bufs = getBuffers(session, "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "567").toBytes());

		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|NOP(567)|SCL|SEN|", s.getRecordedData(true));
		
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
		s.getRecordedData(true);
		session = (SSLSession) c.getSession(); 
		ByteBuffer[] bufs = getBuffers(session, "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "567").toBytes());
		c.getSession().dirtyClose();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		// TODO why incident could occurred in closing session (need to better synchronize dirtyClose
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));

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
		s.getRecordedData(true);
		session = (SSLSession) c.getSession(); 
		bufs = getBuffers(session, "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "567").toBytes());

		c.dirtyStop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));
	
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
		s.getRecordedData(true);
		session = (SSLSession) c.getSession(); 
		ByteBuffer[] bufs = getBuffers(session, "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "567").toBytes());
		c.getSession().quickClose();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|SCL|SEN|", s.getRecordedData(true));

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
		s.getRecordedData(true);
		session = (SSLSession) c.getSession(); 
		bufs = getBuffers(session, "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "567").toBytes());

		c.quickStop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|SCL|SEN|", s.getRecordedData(true));
	
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
		s.getRecordedData(true);
		ByteBuffer[] bufs = getBuffers((SSLSession)c.getSession(), "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "123").toBytes());
		StreamSession session1 = c.getSession();
		c.endingAction = EndingAction.DEFAULT;
		c.start(false, c.loop);
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
		bufs = getBuffers((SSLSession)c.getSession(), "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "567").toBytes());
		session1.close();
		waitFor(500);
		assertEquals("DR|NOP(123)|SCL|SEN|DR|NOP(567)|SCL|SEN|", s.getRecordedData(true));
		assertTrue(c.loop.join(TIMEOUT));
		
		//quick stop with more sessions in the loop
		c = new Client(PORT, true);
		c.endingAction = EndingAction.QUICK_STOP;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
		bufs = getBuffers((SSLSession)c.getSession(), "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "123").toBytes());
		session1 = c.getSession();
		c.endingAction = EndingAction.DEFAULT;
		c.start(false, c.loop);
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
		bufs = getBuffers((SSLSession)c.getSession(), "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "567").toBytes());
		session1.close();
		waitFor(500);
		assertEquals("DR|NOP(123)|SCL|SEN|DR|SCL|SEN|", s.getRecordedData(true));
		assertTrue(c.loop.join(TIMEOUT));

		//dirty stop with more sessions in the loop
		c = new Client(PORT, true);
		c.endingAction = EndingAction.DIRTY_STOP;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
		bufs = getBuffers((SSLSession)c.getSession(), "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "123").toBytes());
		session1 = c.getSession();
		c.endingAction = EndingAction.DEFAULT;
		c.start(false, c.loop);
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
		bufs = getBuffers((SSLSession)c.getSession(), "outAppBuffers");
		bufs[0].put(new Packet(PacketType.NOP, "567").toBytes());
		session1.close();
		waitFor(500);
		assertEquals("DR|NOP(123)|SCL|SEN|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));
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
		assertEquals("DS|DR|ECHO_RESPONSE(1234)|", c.getRecordedData(true));
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

		session.closing = ClosingState.SENDING;
		assertFalse(session.write(new byte[3], 0, 1).isSuccessful());
		assertFalse(session.write(new byte[3]).isSuccessful());
		assertFalse(session.write(getBuffer(10,0)).isSuccessful());
		assertFalse(session.write(getBuffer(10,0), 5).isSuccessful());
		session.writenf(new byte[3], 0, 1);
		session.writenf(new byte[3]);
		session.writenf(getBuffer(10,0));
		session.writenf(getBuffer(10,0), 5);
		session.closing = ClosingState.NONE;

		InternalSSLHandler internal = getInternal(session);
		Field field = internal.getClass().getDeclaredField("closing");
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
		
		assertTrue(session.write(new byte[0]).isSuccessful());
		assertTrue(session.write(new byte[3], 0, 0).isSuccessful());
		assertTrue(session.write(getBuffer(0,0)).isSuccessful());
		assertTrue(session.write(getBuffer(10,0), 0).isSuccessful());
		assertTrue(session.write(new byte[3], 1, 0).isSuccessful());
		session.writenf(new byte[0]);
		session.writenf(new byte[3], 0, 0);
		session.writenf(getBuffer(0,0));
		session.writenf(getBuffer(10,0), 0);
		session.writenf(new byte[3], 1, 0);
		
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
		s.getRecordedData(true);
		
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
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|EXC|SCL|SEN|", s.getRecordedData(true));
		
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
		s.getRecordedData(true);
		
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
		assertEquals("EXC|SCL|SEN|", c.getRecordedData(true));
		assertEquals("SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));
		
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
		s.getRecordedData(true);
		
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
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|ECHO(1234)|EXC|SCL|SEN|", s.getRecordedData(true));
		
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
		s.getRecordedData(true);
		
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
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|NOP(1234)|SCL|SEN|", s.getRecordedData(true));

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
		s.getRecordedData(true);
		
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
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|NOP(1234)|SCL|SEN|", s.getRecordedData(true));

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
		s.getRecordedData(true);

		ByteBuffer buf = getBuffer((SSLSession) c.getSession(), "inNetBuffer");
		ByteBuffer buf2 = ByteBuffer.allocate(buf.position());
		buf.flip();
		buf2.put(buf);
		setBuffer((SSLSession) s.getSession(), "inNetBuffer", buf2);
		
		SSLSession session = (SSLSession) c.getSession();
		session.write(new Packet(PacketType.NOP, "1234").toBytes());
		session.close();
		
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|NOP(1234)|SCL|SEN|", s.getRecordedData(true));

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
		s.getRecordedData(true);

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
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|EXC|SCL|SEN|", s.getRecordedData(true));

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
		s.getRecordedData(true);

		byte[] data = new byte[1024];
		Arrays.fill(data, (byte)'A');
		c.write(new Packet(PacketType.BIG_NOP, new String(data)));
		s.waitForDataRead(TIMEOUT);
		assertEquals(multiString("DR|", 2) + "BIG_NOP(1024)|", s.getRecordedData(true));
		
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
		s.getRecordedData(true);

		data = new byte[1024];
		Arrays.fill(data, (byte)'A');
		c.write(new Packet(PacketType.BIG_NOP, new String(data)));
		s.waitForDataRead(TIMEOUT);
		assertEquals(multiString("DR|", 2) + "BIG_NOP(1024)|", s.getRecordedData(true));
		
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
		byte[] data = new byte[20000];
		Arrays.fill(data, (byte)'A');
		c.getSession().write(new Packet(PacketType.BIG_NOP, new String(data)).toBytes());
		s.waitForDataRead(TIMEOUT);
		assertEquals(6, allocator.getSize());
		assertEquals(7, allocator.getAllocatedCount());
		assertEquals(1, allocator.getReleasedCount());
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
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
		data = new byte[20000];
		Arrays.fill(data, (byte)'A');
		c.getSession().write(new Packet(PacketType.BIG_NOP, new String(data)).toBytes());
		s.waitForDataRead(TIMEOUT);
		assertEquals(7, allocator.getSize());
		assertEquals(7, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals(7, allocator.getSize());
		assertEquals(7, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		
		s.stop(TIMEOUT);
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
		s.getRecordedData(true);
		
		TestSSLEngine engine = getSSLEngine((SSLSession) c.getSession());
		engine.wrapException = new SSLException("");
		c.write(new Packet(PacketType.NOP, ""));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("EXC|SCL|SEN|", c.getRecordedData(true));
		assertEquals("SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		
		c = new Client(PORT, true);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
		
		engine = getSSLEngine((SSLSession) c.getSession());
		engine.wrapException = new SSLException("");
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("EXC|SCL|SEN|", c.getRecordedData(true));
		assertEquals("SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);

		c = new Client(PORT, true);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);

		engine = getSSLEngine((SSLSession) c.getSession());
		engine.wrapException = new SSLException("");
		c.throwInException = true;
		c.write(new Packet(PacketType.NOP, ""));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("EXC|SCL|SEN|", c.getRecordedData(true));
		assertEquals("SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));
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
		s.getRecordedData(true);

		TestSSLEngine engine = getSSLEngine((SSLSession) s.getSession());
		engine.unwrapException = new SSLException("");
		c.write(new Packet(PacketType.NOP, ""));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|EXC|SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", c.getRecordedData(true));
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
		c.getRecordedData(true);
		s.getRecordedData(true);

		SSLSession session = (SSLSession) c.getSession();
		
		session.suspendWrite();
		IFuture<Void> f = session.write(new Packet(PacketType.NOP, "").toBytes());
		resumeWithDelay(session, 500);
		long time = System.currentTimeMillis();
		f.sync(TIMEOUT);
		time = System.currentTimeMillis() - time;
		assertTrue("expected 500 but was " + time, time > 490 && time < 510);
		
		session.suspendWrite();
		byte[] data = new byte[20000];
		Arrays.fill(data, (byte)'A');
		f = session.write(new Packet(PacketType.BIG_NOP, new String(data)).toBytes());
		resumeWithDelay(session, 500);
		time = System.currentTimeMillis();
		f.sync(TIMEOUT);
		time = System.currentTimeMillis() - time;
		assertTrue("expected 500 but was " + time, time > 490 && time < 510);
		
		session.suspendWrite();
		data = new byte[40000];
		Arrays.fill(data, (byte)'A');
		f = session.write(new Packet(PacketType.BIG_NOP, new String(data)).toBytes());
		resumeWithDelay(session, 500);
		time = System.currentTimeMillis();
		f.sync(TIMEOUT);
		time = System.currentTimeMillis() - time;
		assertTrue("expected 500 but was " + time, time > 490 && time < 510);
	}

}
