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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

import org.junit.Test;
import org.snf4j.core.session.IEngineDatagramSession;
import org.snf4j.core.session.IEngineSession;
import org.snf4j.core.timer.DefaultTimer;

public class EngineDatagramWrapperTest extends DTLSTest {

	@Test
	public void testSetExecutor() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.timer = new DefaultTimer();
		s.ssl = true;
		s.testEngine = new TestDTLSEngine();
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.testEngine = new TestDTLSEngine();
		s.startServer();
		c.startClient();
		assertReady(c, s);
		
		DatagramHandler h = c;
		
		for (int i=0; i<2; ++i) {
			IEngineDatagramSession session = (IEngineDatagramSession) h.getSession();

			assertNotNull(h.loop.getExecutor());
			assertTrue(session.getExecutor() == h.loop.getExecutor());
			Executor e = new Executor() {

				@Override
				public void execute(Runnable command) {
				}
			};
			session.setExecutor(e);
			assertTrue(session.getExecutor() == e);
			session.setExecutor(null);
			assertTrue(session.getExecutor() == h.loop.getExecutor());
			((DatagramSession)session).loop = null;
			assertNull(session.getExecutor());
			session.setExecutor(e);
			assertTrue(session.getExecutor() == e);
			((DatagramSession)session).loop = h.loop;
			h = s;
		}
	}
	
	@Test
	public void testConnectedTo() throws Exception {
		SocketAddress a1 = address(100);
		SocketAddress a2 = address(101);
		EngineDatagramHandler h = new EngineDatagramHandler(null, null, new TestDatagramHandler(), null);
		h.setSession(new DTLSSession(h.getHandler(), true));
		
		EngineDatagramWrapper w = new EngineDatagramWrapper(null, h);
		assertFalse(w.connectedTo(a1));
		assertTrue(w.connectedTo(null));
		w = new EngineDatagramWrapper(a1, h);
		assertFalse(w.connectedTo(a2));
		assertTrue(w.connectedTo(null));
		assertTrue(w.connectedTo(a1));
	}
	
	@Test
	public void testGetHandler() throws Exception {
		TestDatagramHandler h0 = new TestDatagramHandler();
		EngineDatagramHandler h = new EngineDatagramHandler(null, null, h0, null);
		h.setSession(new DTLSSession(h.getHandler(), true));
		
		EngineDatagramWrapper w = new EngineDatagramWrapper(null, h);
		assertTrue(h0 == w.getHandler());
	}
	
	@Test
	public void testWriteArguments() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.timer = new DefaultTimer();
		s.ssl = true;
		s.testEngine = new TestDTLSEngine();
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.testEngine = new TestDTLSEngine();
		s.startServer();
		c.startClient();
		assertReady(c, s);

		DatagramHandler h = c;
		
		for (int i=0; i<2; ++i) {
			DatagramSession session = h.getSession();

			try {session.write((byte[])null);fail();} catch (NullPointerException e) {}
			try {session.writenf((byte[])null);fail();} catch (NullPointerException e) {}
			try {session.write((byte[])null, 0, 0);fail();} catch (NullPointerException e) {}
			try {session.writenf((byte[])null, 0, 0);fail();} catch (NullPointerException e) {}
			try {session.write((ByteBuffer)null);fail();} catch (NullPointerException e) {}
			try {session.writenf((ByteBuffer)null);fail();} catch (NullPointerException e) {}
			try {session.write((ByteBuffer)null, 0);fail();} catch (NullPointerException e) {}
			try {session.writenf((ByteBuffer)null, 0);fail();} catch (NullPointerException e) {}
			try {session.write((Object)null);fail();} catch (NullPointerException e) {}
			try {session.writenf((Object)null);fail();} catch (NullPointerException e) {}

			byte[] b = new byte[0];
			ByteBuffer bb = ByteBuffer.wrap(b);
			byte[] b10 = new byte[10];
			ByteBuffer bb10 = ByteBuffer.wrap(b);
			assertTrue(session.write(b).isSuccessful());
			session.writenf(b);
			assertTrue(session.write(b10,0,0).isSuccessful());
			session.writenf(b10,0,0);
			assertTrue(session.write(bb).isSuccessful());
			session.writenf(bb);
			assertTrue(session.write(bb10,0).isSuccessful());
			session.writenf(bb10,0);

			try {session.write(b10, -1, 1);fail();} catch (IndexOutOfBoundsException e) {}
			try {session.writenf(b10, 9, 2);fail();} catch (IndexOutOfBoundsException e) {}
			try {session.write(bb10, 11);fail();} catch (IndexOutOfBoundsException e) {}
			try {session.writenf(bb10, 11);fail();} catch (IndexOutOfBoundsException e) {}
			try {session.write(bb10, -1);fail();} catch (IndexOutOfBoundsException e) {}
			try {session.writenf(bb10, -1);fail();} catch (IndexOutOfBoundsException e) {}

			try {session.write(new String());fail();} catch (IllegalArgumentException e) {}
			try {session.writenf(new String());fail();} catch (IllegalArgumentException e) {}

			session.closing = ClosingState.SENDING;
			assertTrue(session.write(b10).isCancelled());
			session.writenf(b10);
			session.closing = ClosingState.NONE;

			waitFor(50);
			assertEquals("", h.getRecordedData(true));
			h = s;
		}
	}
	
	@Test
	public void testClose() throws Exception {
		assumeJava8();
		
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.timer = new DefaultTimer();
		s.ssl = true;
		s.testEngine = new TestDTLSEngine();
		s.startServer();
		
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.testEngine = new TestDTLSEngine();
		c.startClient();
		assertReady(c, s);
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		c.getSession().close();
		c.getSession().key = null;
		c.getSession().close();
		
		
		s.getRecordedData(true);
		s.testEngine = new TestDTLSEngine();
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.testEngine = new TestDTLSEngine();
		c.startClient();
		assertReady(c, s);
		c.getSession().quickClose();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		c.getSession().quickClose();
		c.getSession().key = null;
		c.getSession().quickClose();

		s.getRecordedData(true);
		s.testEngine = new TestDTLSEngine();
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.testEngine = new TestDTLSEngine();
		c.startClient();
		assertReady(c, s);
		c.getSession().dirtyClose();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		c.getSession().dirtyClose();
		c.getSession().key = null;
		c.getSession().dirtyClose();

		//server side
		
		s.getRecordedData(true);
		s.testEngine = new TestDTLSEngine();
		s.waitForCloseMessage = true;
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.testEngine = new TestDTLSEngine();
		c.startClient();
		assertReady(c, s);
		s.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|DS|SCL|SEN|", c.getRecordedData(true));
		s.getSession().close();
		s.getSession().key = null;
		s.getSession().close();
		
		s.getRecordedData(true);
		s.testEngine = new TestDTLSEngine();
		s.waitForCloseMessage = true;
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.testEngine = new TestDTLSEngine();
		c.startClient();
		assertReady(c, s);
		s.getSession().quickClose();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|DS|SCL|SEN|", c.getRecordedData(true));

		s.getRecordedData(true);
		s.testEngine = new TestDTLSEngine();
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.testEngine = new TestDTLSEngine();
		c.startClient();
		assertReady(c, s);
		s.getSession().dirtyClose();
		s.waitForSessionEnding(TIMEOUT);
		waitFor(100);
		assertEquals("", c.getRecordedData(true));
		
	}
	
	@Test
	public void testGetEngineSession() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.timer = new DefaultTimer();
		s.ssl = true;
		s.testEngine = new TestDTLSEngine();
		s.startServer();

		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.testEngine = new TestDTLSEngine();
		c.startClient();
		assertReady(c, s);
		
		assertNotNull(s.testEngine.getSession());
		assertNotNull(c.testEngine.getSession());
		assertTrue(s.testEngine.getSession() == ((IEngineSession)s.getSession()).getEngineSession());
		assertTrue(c.testEngine.getSession() == ((IEngineSession)c.getSession()).getEngineSession());
	}
	
	@Test
	public void testBeginHandshake() throws Exception {
		assumeFailingOrNoRehandshake();
		
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.timer = new DefaultTimer();
		s.ssl = true;
		s.testEngine = new TestDTLSEngine();
		s.startServer();

		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.testEngine = new TestDTLSEngine();
		c.startClient();
		assertReady(c, s);
		
		
		assertEquals(0, s.testEngine.handshakeCount);
		assertEquals(0, c.testEngine.handshakeCount);

		((IEngineSession)c.getSession()).beginHandshake();
		waitFor(50);
		assertEquals(1, c.testEngine.handshakeCount);
		((IEngineSession)s.getSession()).beginHandshake();
		waitFor(50);
		assertEquals(1, s.testEngine.handshakeCount);
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		
		s.testEngine = new TestDTLSEngine();
		s.getRecordedData(true);
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.testEngine = new TestDTLSEngine();
		c.getRecordedData(true);
		c.startClient();
		assertReady(c, s);
		
		((IEngineSession)c.getSession()).beginLazyHandshake();
		((IEngineSession)s.getSession()).beginLazyHandshake();
		waitFor(50);
		assertEquals(0, c.testEngine.handshakeCount);
		assertEquals(0, s.testEngine.handshakeCount);
		
		c.getSession().write(nop());
		waitFor(50);
		assertEquals(1, c.testEngine.handshakeCount);
		assertEquals(1, s.testEngine.handshakeCount);
		
	}
}
