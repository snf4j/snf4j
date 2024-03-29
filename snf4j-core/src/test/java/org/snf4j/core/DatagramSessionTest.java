/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2022 SNF4J contributors
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.DatagramSession.DatagramRecord;
import org.snf4j.core.allocator.TestAllocator;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.session.IllegalSessionStateException;
import org.snf4j.core.session.SessionState;
import org.snf4j.core.session.UnsupportedSessionTimer;
import org.snf4j.core.timer.DefaultTimer;

public class DatagramSessionTest {
	long TIMEOUT = 2000;
	int PORT = 7779;
	IDatagramHandler handler = new TestDatagramHandler();
	
	DatagramHandler c;
	DatagramHandler s;

	private void waitFor(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}

	private byte[] getBytes(int size, int value) {
		return ByteUtils.getBytes(size, value);
	}
	
	private ByteBuffer getBuffer(int size, int value) {
		return ByteBuffer.wrap(getBytes(size, value));
	}
	
	static ByteBuffer getInBuffer(DatagramSession session) throws Exception {
		Field f = DatagramSession.class.getDeclaredField("inBuffer");
		
		f.setAccessible(true);
		return (ByteBuffer) f.get(session);
	}
	
	@Before
	public void before() {
		s = c = null;
	}
	
	@After
	public void after() throws InterruptedException {
		if (c != null) c.stop(TIMEOUT);
		if (s != null) s.stop(TIMEOUT);
	}
	
	@Test
	public void testCreatePipeline() {
		DatagramSession session = new DatagramSession(handler);
		assertNull(session.createPipeline());
		assertNull(session.getPipeline0());
		assertNull(session.getPipeline0());
	}
	
	@Test
	public void testGetAddress() throws Exception {
		DatagramSession session = new DatagramSession(handler);
		assertNull(session.getParent());

		assertTrue(handler == session.getHandler());
		
		assertNull(session.getLocalAddress());
		assertNull(session.getRemoteAddress());
		
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		
		s.startServer();
		c.startClient();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		assertNotNull(c.getSession().getLocalAddress());
		assertNotNull(s.getSession().getLocalAddress());
		assertEquals("/127.0.0.1:" + PORT, c.getSession().getRemoteAddress().toString());
		assertNull(s.getSession().getRemoteAddress());
		
		s.getSession().close();
		c.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);

		assertNull(c.getSession().getLocalAddress());
		assertNull(s.getSession().getLocalAddress());
		assertNull(c.getSession().getRemoteAddress());
		assertNull(s.getSession().getRemoteAddress());
	}

	@Test
	public void testEventException() throws Exception {
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		c.throwInEvent = true;
		c.throwInEventType = EventType.SESSION_CREATED;
		c.incident = true;
		s.startServer();
		c.startClient();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SESSION_EVENT_FAILURE|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.throwInEventType = EventType.DATA_SENT;
		c.write(new Packet(PacketType.ECHO));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|DATA_EVENT_FAILURE|DR|ECHO_RESPONSE()|", c.getRecordedData(true));
		assertEquals("DR|$ECHO()|DS|", s.getRecordedData(true));
		c.throwInEventType = EventType.DATA_RECEIVED;
		c.write(new Packet(PacketType.ECHO));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|DATA_EVENT_FAILURE|ECHO_RESPONSE()|", c.getRecordedData(true));
		assertEquals("DR|$ECHO()|DS|", s.getRecordedData(true));
		c.throwInEventType = EventType.SESSION_CLOSED;
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SESSION_EVENT_FAILURE|SEN|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		c.stop(TIMEOUT);

		c.throwInEventType = EventType.SESSION_OPENED;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|SESSION_EVENT_FAILURE|RDY|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		c.throwInEventType = EventType.SESSION_ENDING;
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertEquals("SCL|SEN|SESSION_EVENT_FAILURE|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		
		c.throwInEventType = EventType.SESSION_READY;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		waitFor(50);
		assertEquals("SCR|SOP|RDY|SESSION_EVENT_FAILURE|", c.getRecordedData(true));
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		c.stop(TIMEOUT);

		c.throwInEventType = EventType.SESSION_CREATED;
		c.incident = false;
		c.getRecordedData(true);
		c.startClient();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SESSION_EVENT_FAILURE|EXC|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);

		c.throwInEventType = EventType.SESSION_OPENED;
		c.startClient();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|SESSION_EVENT_FAILURE|EXC|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);

		c.throwInEventType = EventType.SESSION_READY;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|RDY|SESSION_EVENT_FAILURE|EXC|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);

		c.throwInEventType = EventType.DATA_SENT;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		c.session.write(new Packet(PacketType.NOP).toBytes());
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|DATA_EVENT_FAILURE|EXC|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		
		c.throwInEventType = EventType.DATA_RECEIVED;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		s.session.send(c.session.getLocalAddress(), new Packet(PacketType.NOP).toBytes());
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|DATA_EVENT_FAILURE|EXC|NOP()|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		
		c.throwInEventType = EventType.SESSION_CLOSED;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SESSION_EVENT_FAILURE|EXC|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);

		c.throwInEventType = EventType.SESSION_ENDING;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertEquals("SCL|SEN|SESSION_EVENT_FAILURE|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		
	}	
	
	@Test
	public void testEvent() throws Exception {
		s = new DatagramHandler(PORT);
		s.recordDataEventDetails = true;
		c = new DatagramHandler(PORT);
		c.recordDataEventDetails = true;
		s.startServer();
		c.startClient();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		
		s.getSession().send(c.getSession().getLocalAddress(), new Packet(PacketType.NOP).toBytes()).sync(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		waitFor(50);
		assertEquals("DR|3|NOP()|", c.getRecordedData(true));
		assertEquals("DS|3;" + c.getSession().getLocalAddress() +"|", s.getRecordedData(true));
		
		c.getSession().event(s.getSession().getLocalAddress(), DataEvent.SENT, 50);
		assertEquals("DS|50;"+s.getSession().getLocalAddress()+"|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));

		s.recordDataEventDetails = false;
		c.recordDataEventDetails = false;
		c.throwInEvent = true;
		c.incident = true;
		c.getSession().event(s.getSession().getLocalAddress(), DataEvent.SENT, 50);
		assertEquals("DS|DATA_EVENT_FAILURE|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		assertEquals(1, c.throwInEventCount.get());
		
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertEquals("SCL|SESSION_EVENT_FAILURE|SEN|SESSION_EVENT_FAILURE|", c.getRecordedData(true));
		waitFor(100);
		
		c.getSession().event(s.getSession().getLocalAddress(), DataEvent.SENT, 50);
		assertEquals("", c.getRecordedData(true));
		assertEquals(3, c.throwInEventCount.get());
		
		c=null;
	}
	
	@SuppressWarnings("unchecked")
	private ConcurrentLinkedQueue<DatagramRecord> getQueue(DatagramSession session) throws Exception {
		Field f = DatagramSession.class.getDeclaredField("outQueue");
		
		f.setAccessible(true);
		return (ConcurrentLinkedQueue<DatagramRecord>) f.get(session);
	}
	
	@Test
	public void testWrite() throws Exception {
		DatagramSession session = new DatagramSession(handler);

		try {
			session.write(new byte[10]);
			fail("exception not thrown");
		} catch (IllegalSessionStateException e) {
			assertEquals(SessionState.OPENING, e.getIllegalState());
		}
		
		assertNull(getQueue(session));
		
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		c.write(new Packet(PacketType.ECHO));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE()|", c.getRecordedData(true));
		assertEquals("DR|$ECHO()|DS|", s.getRecordedData(true));
		
		s.getSession().send(c.getSession().getLocalAddress(), new Packet(PacketType.ECHO, "1").toBytes());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DR|ECHO(1)|DS|", c.getRecordedData(true));
		assertEquals("DS|DR|$ECHO_RESPONSE(1)|", s.getRecordedData(true));
		
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));

		try {
			c.write(new Packet(PacketType.ECHO, "2"));
			fail("exception not thrown");
		} catch (IllegalSessionStateException e) {
			assertEquals(SessionState.CLOSING, e.getIllegalState());
		}
		
		assertTrue(getQueue(c.getSession()).isEmpty());
		waitFor(2000);
		assertEquals("", c.getRecordedData(true));
	}
	
	@Test
	public void testSuspendAndResume() throws Exception {
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		
		//write suspend&resume
		DatagramSession session = c.getSession();
		assertFalse(session.isWriteSuspended());
		session.suspendWrite();
		assertTrue(session.isWriteSuspended());
		c.write(new Packet(PacketType.ECHO, "2"));
		c.write(new Packet(PacketType.ECHO, "3"));
		waitFor(2000);
		assertEquals("", c.getRecordedData(true));
		assertFalse(session.suspend(SelectionKey.OP_WRITE));
		session.suspendWrite();
		session.resumeWrite();
		assertFalse(session.isWriteSuspended());
		waitFor(500);
		c.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(2)|DR|ECHO_RESPONSE(3)|", c.getRecordedData(true));
		assertFalse(session.resume(SelectionKey.OP_WRITE));
		session.resumeWrite();
		s.waitForDataReceived(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		
		//read suspend&resume
		s.getRecordedData(true);
		assertFalse(session.isReadSuspended());
		session.suspendRead();
		assertTrue(session.isReadSuspended());
		c.write(new Packet(PacketType.ECHO, "4"));
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|", c.getRecordedData(true));
		s.waitForDataReceived(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(4)|DS|", s.getRecordedData(true));
		waitFor(2000);
		assertFalse(session.suspend(SelectionKey.OP_READ));
		session.suspendRead();
		assertEquals("", c.getRecordedData(true));
		session.resumeRead();
		assertFalse(session.isReadSuspended());
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|ECHO_RESPONSE(4)|", c.getRecordedData(true));
		assertFalse(session.resume(SelectionKey.OP_READ));
		session.resumeRead();
		assertFalse(session.isReadSuspended());
		
		//when key is invalid
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		SessionTest.assertResumeSuspendException(session, SessionTest.RSType.SUSPEND_WRITE, SessionState.CLOSING);
		assertFalse(session.isWriteSuspended());
		SessionTest.assertResumeSuspendException(session, SessionTest.RSType.RESUME_WRITE, SessionState.CLOSING);
		SessionTest.assertResumeSuspendException(session, SessionTest.RSType.SUSPEND_READ, SessionState.CLOSING);
		assertFalse(session.isReadSuspended());
		SessionTest.assertResumeSuspendException(session, SessionTest.RSType.RESUME_READ, SessionState.CLOSING);
		assertFalse(session.suspend(SelectionKey.OP_READ));
		assertFalse(session.resume(SelectionKey.OP_READ));
		
		//when key is null
		session = new DatagramSession(handler);
		SessionTest.assertResumeSuspendException(session, SessionTest.RSType.SUSPEND_WRITE, SessionState.OPENING);
		assertFalse(session.isWriteSuspended());
		SessionTest.assertResumeSuspendException(session, SessionTest.RSType.RESUME_WRITE, SessionState.OPENING);
		SessionTest.assertResumeSuspendException(session, SessionTest.RSType.SUSPEND_READ, SessionState.OPENING);
		assertFalse(session.isReadSuspended());
		SessionTest.assertResumeSuspendException(session, SessionTest.RSType.RESUME_READ, SessionState.OPENING);

		c = null;
		
	}	
	
	@Test
	public void testStatistics() throws Exception {
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		
		long t0 = System.currentTimeMillis();
		s.startServer();
		c.startClient();
		
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		long t1 = System.currentTimeMillis();
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		DatagramSession cs = c.getSession();
		DatagramSession ss = s.getSession();
		
		assertTrue(cs.getCreationTime() >= ss.getCreationTime());
		assertTrue(cs.getCreationTime() >= t0 && cs.getCreationTime() <= t1);
		assertTrue(ss.getCreationTime() >= t0 && ss.getCreationTime() <= t1);
		assertEquals(cs.getCreationTime(), cs.getLastIoTime());
		assertEquals(cs.getCreationTime(), cs.getLastReadTime());
		assertEquals(cs.getCreationTime(), cs.getLastWriteTime());
		
		assertEquals(0, cs.getReadBytes());
		assertEquals(0, cs.getWrittenBytes());
		assertEquals(0, ss.getReadBytes());
		assertEquals(0, ss.getWrittenBytes());

		waitFor(10);
		t0 = System.currentTimeMillis();
		c.write(new Packet(PacketType.NOP, "1234"));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		t1 = System.currentTimeMillis();
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|$NOP(1234)|", s.getRecordedData(true));
		assertEquals(0, cs.getReadBytes());
		assertEquals(7, cs.getWrittenBytes());
		assertEquals(7, ss.getReadBytes());
		assertEquals(0, ss.getWrittenBytes());
		assertEquals(cs.getLastWriteTime(), cs.getLastIoTime());
		assertEquals(cs.getCreationTime(), cs.getLastReadTime());
		assertTrue(cs.getLastWriteTime() >= t0 && cs.getLastWriteTime() <= t1);
		assertEquals(ss.getLastReadTime(), ss.getLastIoTime());
		assertEquals(ss.getCreationTime(), ss.getLastWriteTime());
		assertTrue(ss.getLastReadTime() >= t0 && ss.getLastReadTime() <= t1);

		ss.send(cs.getLocalAddress(), new Packet(PacketType.NOP, "12345").toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|", s.getRecordedData(true));
		assertEquals("DR|NOP(12345)|", c.getRecordedData(true));
		assertEquals(8, cs.getReadBytes());
		assertEquals(7, cs.getWrittenBytes());
		assertEquals(7, ss.getReadBytes());
		assertEquals(8, ss.getWrittenBytes());

		ss.send(cs.getLocalAddress(), new Packet(PacketType.ECHO, "").toBytes());
		s.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|$ECHO_RESPONSE()|", s.getRecordedData(true));
		assertEquals("DR|ECHO()|DS|", c.getRecordedData(true));
		assertEquals(11, cs.getReadBytes());
		assertEquals(10, cs.getWrittenBytes());
		assertEquals(10, ss.getReadBytes());
		assertEquals(11, ss.getWrittenBytes());
	}
	
	@Test
	public void testCalculateThroughput() throws Exception {
		s = new DatagramHandler(PORT);
		s.throughputCalcInterval = 1000;
		c = new DatagramHandler(PORT);
		c.throughputCalcInterval = 1000;
	
		s.startServer();
		c.startClient();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		DatagramSession cs = c.getSession();
		DatagramSession ss = s.getSession();
		
		long t0 = System.currentTimeMillis();
		c.write(new Packet(PacketType.NOP, new String(new byte[1000])));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);

		waitFor(1010 - (System.currentTimeMillis() - t0));
		c.write(new Packet(PacketType.ECHO));
		s.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		
		double vCR = cs.getReadBytesThroughput();
		double vSR = ss.getReadBytesThroughput();
		double vCW = cs.getWrittenBytesThroughput();
		double vSW = ss.getWrittenBytesThroughput();
		
		assertTrue(vCR < 0.00000001);
		assertTrue(vSW < 0.00000001);
		assertTrue(Double.toString(vSR), vSR > 950.0 && vSR < 1010.0);
		assertTrue(Double.toString(vCW), vCW > 950.0 && vCW < 1010.0);
	}
	
	@Test
	public void testDisabledCalculateThroughput() throws Exception {
		s = new DatagramHandler(PORT);
		s.throughputCalcInterval = 0;
		c = new DatagramHandler(PORT);
		c.throughputCalcInterval = 0;
		
		s.startServer();
		c.startClient();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		DatagramSession cs = c.getSession();
		DatagramSession ss = s.getSession();
		
		c.write(new Packet(PacketType.NOP, new String(new byte[1000])));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		waitFor(2000);
		c.write(new Packet(PacketType.ECHO));
		s.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		
		double vCR = cs.getReadBytesThroughput();
		double vSR = ss.getReadBytesThroughput();
		double vCW = cs.getWrittenBytesThroughput();
		double vSW = ss.getWrittenBytesThroughput();
		
		assertTrue(vCR < 0.00000001);
		assertTrue(vSW < 0.00000001);
		assertTrue(vSR < 0.00000001);
		assertTrue(vCW < 0.00000001);	
	}
	
	@Test
	public void testClose() throws Exception {
		DatagramSession session = new DatagramSession(handler);
		
		//close when key == null
		session.close();
		session.quickClose();
		
		//internal close method
		DatagramChannel channel = DatagramChannel.open();
		channel.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT));
		assertTrue(channel.isConnected());
		assertTrue(channel.isOpen());
		session.close(channel);
		assertTrue(!channel.isConnected());
		assertTrue(!channel.isOpen());
		SocketChannel schannel = SocketChannel.open();
		session.close(schannel);
		
		//close inside the loop
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.CLOSE));
		c.waitForDataSent(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|$CLOSE()|SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		
		//close outside the loop
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		c.getSession().close();
		waitFor(500);
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertEquals("", c.getRecordedData(true));

		//close inside the loop with data to send
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.WRITE_AND_CLOSE));
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|DR|WRITE_AND_CLOSE_RESPONSE()|", c.getRecordedData(true));
		assertEquals("DR|$WRITE_AND_CLOSE()|DS|SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));

		//close outside the loop with data to send
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.WRITE_AND_WAIT, "1000"));
		waitFor(500);
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		s.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$WRITE_AND_WAIT(1000)|DS|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertEquals("", c.getRecordedData(true));

		//close outside the loop with data to send (other side)
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.WRITE_AND_WAIT, "1000"));
		waitFor(500);
		s.getSession().close();
		s.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|$WRITE_AND_WAIT(1000)|DS|SCL|SEN|", s.getRecordedData(true));
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|WRITE_AND_WAIT_RESPONSE(1000)|", c.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		
		//quick close inside the loop
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.QUICK_CLOSE));
		c.waitForDataSent(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|$QUICK_CLOSE()|SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));

		//quick close outside the loop
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.getSession().quickClose();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		c.getSession().quickClose();
		waitFor(500);
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertEquals("", c.getRecordedData(true));
		
		//quick close inside the loop with data to send
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.WRITE_AND_QUICK_CLOSE));
		c.waitForDataSent(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|$WRITE_AND_QUICK_CLOSE()|SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));

		//quick close outside the loop with data to send
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.WRITE_AND_WAIT, "1000"));
		waitFor(500);
		c.getSession().quickClose();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		s.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$WRITE_AND_WAIT(1000)|DS|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertEquals("", c.getRecordedData(true));
		
		//quick close outside the loop with data to send (other side)
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.WRITE_AND_WAIT, "1000"));
		waitFor(500);
		s.getSession().quickClose();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|$WRITE_AND_WAIT(1000)|SCL|SEN|", s.getRecordedData(true));
		c.waitForDataSent(TIMEOUT);
		waitFor(500);
		assertEquals("DS|", c.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));

		//dirty close outside the loop
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.getSession().dirtyClose();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		c.getSession().dirtyClose();
		waitFor(500);
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertEquals("", c.getRecordedData(true));
		
	}
	
	private void testCloseOutsideSuspendWrite(boolean write, boolean quickClose) throws Exception {
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		s.getSession().suspendWrite();
		c.getSession().suspendWrite();
		if (write) s.getSession().send(c.getSession().getLocalAddress(), new Packet(PacketType.NOP).toBytes());
		if (write) c.write(new Packet(PacketType.NOP));
		if (quickClose) {
			c.getSession().quickClose();
			s.getSession().quickClose();
		}
		else {
			c.getSession().close();
			s.getSession().close();
		}
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));

	}
	
	private void testCloseInsideSuspendWrite(boolean write, boolean quickClose) throws Exception {
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		StringBuilder sb = new StringBuilder();
		if (write) sb.append("W");
		if (quickClose) sb.append("Q");
		
		s.getSession().send(c.getSession().getLocalAddress(), new Packet(PacketType.SUSPEND_WRITE_CLOSE, sb.toString()).toBytes());
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|SUSPEND_WRITE_CLOSE(" + sb.toString() +")|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		assertEquals("", c.getRecordedData(true));
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|", s.getRecordedData(true));
		c = new DatagramHandler(PORT); c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		
		c.write(new Packet(PacketType.SUSPEND_WRITE_CLOSE, sb.toString()));
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|$SUSPEND_WRITE_CLOSE(" + sb.toString() +")|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		
	}
	
	@Test
	public void testCloseWhenSuspendedWrite() throws Exception {
		//closed outside the loop 
		testCloseOutsideSuspendWrite(false, false);
		testCloseOutsideSuspendWrite(false, true);

		//closed with written data outside the loop 
		testCloseOutsideSuspendWrite(true, false);
		testCloseOutsideSuspendWrite(true, true);
		
		testCloseInsideSuspendWrite(false, false);
		testCloseInsideSuspendWrite(false, true);
		testCloseInsideSuspendWrite(true, false);
		testCloseInsideSuspendWrite(true, true);
	}
	
	private void testCloseOutsideSuspendRead(boolean quickClose) throws Exception {
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.getSession().suspendRead();
		s.getSession().suspendRead();
		if (quickClose) {
			c.getSession().quickClose();
			s.getSession().quickClose();
		}
		else {
			c.getSession().close();
			s.getSession().close();
		}		
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}
	
	@Test
	public void testCloseWhenSuspendedRead() throws Exception {
		testCloseOutsideSuspendRead(false);
		testCloseOutsideSuspendRead(true);
		
		//close with written data (data length==0 but OP_WRITE set)
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		DatagramSession session = c.getSession();
		session.suspendRead();
		session.key.interestOps(session.key.interestOps() | SelectionKey.OP_WRITE);
		session.close();
		session.loop.selector.wakeup(); //need to wake up as we set OP_WRITE in illegal way
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		session = s.getSession();
		session.suspendRead();
		session.key.interestOps(session.key.interestOps() | SelectionKey.OP_WRITE);
		session.close();
		session.loop.selector.wakeup(); //need to wake up as we set OP_WRITE in illegal way
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT);
		c.stop(TIMEOUT);
		
		//close with written data
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		session = c.getSession();
		session.suspendRead();
		session.suspendWrite();
		c.write(new Packet(PacketType.NOP));
		waitFor(100);
		session.key.interestOps(session.key.interestOps() | SelectionKey.OP_WRITE);
		session.close();
		session.loop.selector.wakeup(); //need to wake up as we set OP_WRITE in illegal way
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|$NOP()|", s.getRecordedData(true));

		//close with written data (other side)
		c = new DatagramHandler(PORT); c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		session = s.getSession();
		session.suspendRead();
		session.suspendWrite();
		session.send(c.getSession().getLocalAddress(), new Packet(PacketType.NOP).toBytes());
		session.key.interestOps(session.key.interestOps() | SelectionKey.OP_WRITE);
		session.close();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", s.getRecordedData(true));
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP()|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}
	
	@Test
	public void testIgnorePossiblyIncompleteWithOptimize() throws Exception {
		s = new DatagramHandler(PORT); 
		s.allocator = new TestAllocator(false, true);
		s.optimizeDataCopying = true;
		s.startServer();
		c = new DatagramHandler(PORT); 
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		
		int min = s.getSession().getConfig().getMinInBufferCapacity()-3;

		//sending pocket with size that equals the buffer size
		byte[] bytes = new byte[min];
		Arrays.fill(bytes, (byte)'B');
		String payload = new String(bytes);
		c.write(new Packet(PacketType.NOP, payload));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataReceived(TIMEOUT);
		waitFor(100);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|", s.getRecordedData(true));
		assertEquals(1, s.allocator.getSize());
		assertTrue(getInBuffer(s.getSession()) == s.allocator.get().get(0));
		assertNotNull(getInBuffer(s.getSession()));
		
		//sending pocket with size that equals than buffer size (retry)
		bytes = new byte[min];
		Arrays.fill(bytes, (byte)'B');
		payload = new String(bytes);
		c.write(new Packet(PacketType.NOP, payload));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		waitFor(100);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|BUF|$NOP(" + payload + ")|", s.getRecordedData(true));
		assertEquals(1, s.allocator.getSize());
		s.allocator.release(s.bufferRead);
		assertEquals(0, s.allocator.getSize());
		assertNull(getInBuffer(s.getSession()));
		
		//sending pocket with size greater than buffer size
		bytes = new byte[min+1];
		Arrays.fill(bytes, (byte)'B');
		payload = new String(bytes);
		c.write(new Packet(PacketType.NOP, payload));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|BUF|$NOP(" + payload + ")|", s.getRecordedData(true));
		assertEquals(1, s.allocator.getSize());
		s.allocator.release(s.bufferRead);
		assertEquals(0, s.allocator.getSize());
		assertNull(getInBuffer(s.getSession()));
		
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
	}
	
	@Test
	public void testIgnorePossiblyIncomplete() throws Exception {
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		int min = s.getSession().getConfig().getMinInBufferCapacity()-3;

		//sending pocket with size less than buffer size
		byte[] bytes = new byte[min-1];
		Arrays.fill(bytes, (byte)'A');
		String payload = new String(bytes);
		c.write(new Packet(PacketType.NOP, payload));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|$NOP(" + payload + ")|", s.getRecordedData(true));

		//sending pocket with size that equals the buffer size
		bytes = new byte[min];
		Arrays.fill(bytes, (byte)'B');
		payload = new String(bytes);
		c.write(new Packet(PacketType.NOP, payload));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataReceived(TIMEOUT);
		waitFor(200);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|", s.getRecordedData(true));

		//sending pocket with size that equals than buffer size (retry)
		bytes = new byte[min];
		Arrays.fill(bytes, (byte)'B');
		payload = new String(bytes);
		c.write(new Packet(PacketType.NOP, payload));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		waitFor(200);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|$NOP(" + payload + ")|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		s = new DatagramHandler(PORT);
		s.ignorePossiblyIncomplete = false;
		s.startServer();
		c = new DatagramHandler(PORT);
		c.ignorePossiblyIncomplete = false;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		//sending pocket with size that equals than buffer size
		bytes = new byte[min];
		Arrays.fill(bytes, (byte)'B');
		payload = new String(bytes);
		c.write(new Packet(PacketType.NOP, payload));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|$NOP(" + payload + ")|", s.getRecordedData(true));

		//sending pocket with size greater than buffer size
		bytes = new byte[min+1];
		Arrays.fill(bytes, (byte)'B');
		payload = new String(bytes);
		c.write(new Packet(PacketType.NOP, payload));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|$NOP(" + payload + ")|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		s = new DatagramHandler(PORT);
		s.ignorePossiblyIncomplete = false;
		s.startServer();
		c = new DatagramHandler(PORT);
		c.ignorePossiblyIncomplete = false;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		//sending pocket with size greater than buffer size
		bytes = new byte[min-1];
		Arrays.fill(bytes, (byte)'B');
		payload = new String(bytes) + "CD";
		c.write(new Packet(PacketType.NOP, payload));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|$NOP(" + payload.substring(0, payload.length()-1) + ")|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
	}
	
	@Test
	public void testState() throws Exception {
		DatagramSession session = new DatagramSession(handler);
		
		assertEquals(SessionState.OPENING, session.getState());
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();

		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals(SessionState.OPEN, c.getSession().getState());
		assertEquals(SessionState.OPEN, s.getSession().getState());
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		waitFor(200);
		assertEquals(SessionState.CLOSING, c.getSession().getState());
		assertEquals(SessionState.OPEN, s.getSession().getState());
		s.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals(SessionState.CLOSING, s.getSession().getState());
		s.stop(TIMEOUT);
		c.stop(TIMEOUT);
	}
	
	private void assertOutOfBoundException(DatagramSession session, byte[] data, int off, int len) {
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
		try {
			session.send(null, data, off, len);
			fail("Exception not thrown");
		}
		catch (IndexOutOfBoundsException e) {}
		try {
			session.sendnf(null, data, off, len);
			fail("Exception not thrown");
		}
		catch (IndexOutOfBoundsException e) {}
	}
	
	private void assertIllegalStateException(DatagramSession session, byte[] data, int off, int len) {
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
		s = new DatagramHandler(PORT); s.startServer();
		s.waitForSessionReady(TIMEOUT);

		DatagramSession session = s.getSession();

		session.closing = ClosingState.SENDING;
		assertFalse(session.write(new byte[3], 0, 1).isSuccessful());
		assertFalse(session.write(new byte[3]).isSuccessful());
		assertFalse(session.write(getBuffer(10,0)).isSuccessful());
		assertFalse(session.write(getBuffer(10,0), 5).isSuccessful());
		assertFalse(session.write(SessionTest.createHolder(session, new byte[10], 1,2)).isSuccessful());
		assertFalse(session.send(null, new byte[3], 0, 1).isSuccessful());
		assertFalse(session.send(null, new byte[3]).isSuccessful());
		assertFalse(session.send(null, getBuffer(10,0)).isSuccessful());
		assertFalse(session.send(null, getBuffer(10,0), 5).isSuccessful());
		assertFalse(session.send(null, SessionTest.createHolder(session, new byte[10], 1,2)).isSuccessful());
		session.closing = ClosingState.NONE;

		s.stop(TIMEOUT);

		try {
			session.write((byte[]) null); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.writenf((byte[]) null); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.write((byte[]) null, 0, 0); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.writenf((byte[]) null, 0, 0); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.send(null, (byte[]) null); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.sendnf(null, (byte[]) null); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.send(null, (byte[]) null, 0, 0); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.sendnf(null, (byte[]) null, 0, 0); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.write((ByteBuffer) null); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.writenf((ByteBuffer) null); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.write((ByteBuffer) null, 0); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.writenf((ByteBuffer) null, 0); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.write((IByteBufferHolder) null); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.writenf((IByteBufferHolder) null); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.write((Object) null); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.writenf((Object) null); fail("Exception not thrown");
		} catch (NullPointerException e) {}

		try {
			session.send(null, (ByteBuffer) null); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.sendnf(null, (ByteBuffer) null); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.send(null, (ByteBuffer) null, 0); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.sendnf(null, (ByteBuffer) null, 0); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.send(null, (IByteBufferHolder) null); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.sendnf(null, (IByteBufferHolder) null); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.send(null, (Object) null); fail("Exception not thrown");
		} catch (NullPointerException e) {}
		try {
			session.sendnf(null, (Object) null); fail("Exception not thrown");
		} catch (NullPointerException e) {}

		assertTrue(session.write(new byte[0]).isSuccessful());
		assertTrue(session.write(new byte[3], 0, 0).isSuccessful());
		assertTrue(session.write(new byte[3], 1, 0).isSuccessful());
		assertTrue(session.write(getBuffer(0,0)).isSuccessful());
		assertTrue(session.write(getBuffer(10,0), 0).isSuccessful());
		assertTrue(session.write(new ByteBufferHolder()).isSuccessful());
		assertTrue(session.send(null, new byte[0]).isSuccessful());
		assertTrue(session.send(null, new byte[3], 0, 0).isSuccessful());
		assertTrue(session.send(null, new byte[3], 1, 0).isSuccessful());
		assertTrue(session.send(null, getBuffer(0,0)).isSuccessful());
		assertTrue(session.send(null, getBuffer(10,0), 0).isSuccessful());
		assertTrue(session.send(null, new ByteBufferHolder()).isSuccessful());
		session.writenf(new byte[0]);
		session.writenf(new byte[3], 0, 0);
		session.writenf(new byte[3], 1, 0);
		session.writenf(getBuffer(0,0));
		session.writenf(getBuffer(10,0), 0);
		session.writenf(new ByteBufferHolder());
		session.sendnf(null, new byte[0]);
		session.sendnf(null, new byte[3], 0, 0);
		session.sendnf(null, new byte[3], 1, 0);
		session.sendnf(null, getBuffer(0,0));
		session.sendnf(null, getBuffer(10,0), 0);
		session.sendnf(null, new ByteBufferHolder());
		
		assertOutOfBoundException(session, new byte[10], -1, 4);
		assertOutOfBoundException(session, new byte[10], 10, 1);
		assertOutOfBoundException(session, new byte[10], 0, -1);
		assertOutOfBoundException(session, new byte[10], 5, 6);
		assertOutOfBoundException(session, new byte[10], 0x7fffffff, 1);
		int len = 11;
		for (int i=0; i<2; ++i) {
			try {
				session.write(getBuffer(0,90), len);
				fail("Exception not thrown");
			}
			catch (IndexOutOfBoundsException e) {}
			try {
				session.writenf(getBuffer(0,90), len);
				fail("Exception not thrown");
			}
			catch (IndexOutOfBoundsException e) {}	
			try {
				session.send(null, getBuffer(0,90), len);
				fail("Exception not thrown");
			}
			catch (IndexOutOfBoundsException e) {}
			try {
				session.sendnf(null, getBuffer(0,90), len);
				fail("Exception not thrown");
			}
			catch (IndexOutOfBoundsException e) {}
			len = -1;
		}
		
		assertIllegalStateException(session, new byte[10], 0, 10);
		assertIllegalStateException(session, new byte[10], 1, 9);
		assertIllegalStateException(session, new byte[10], 0, 1);

	}
	
	@Test
	public void testSendWhenConnected() throws Exception {
		s = new DatagramHandler(PORT); s.startServer();
		DatagramHandler s2 = new DatagramHandler(PORT+1); s2.startServer();
		c = new DatagramHandler(PORT); c.startClient();

		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		s2.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s2.getRecordedData(true));
		
		c.getSession().send(s2.getSession().getLocalAddress(), new Packet(PacketType.ECHO).toBytes());
		waitFor(100);
		assertEquals("DS|DR|ECHO_RESPONSE()|", c.getRecordedData(true));
		assertEquals("DR|$ECHO()|DS|", s.getRecordedData(true));
		assertEquals("", s2.getRecordedData(true));
		s2.stop(TIMEOUT);
	}
	
	@Test
	public void testWriteWhenConnected() throws Exception {
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		DatagramSession session = c.getSession();

		byte[] data = new Packet(PacketType.ECHO, "567").toBytes(0, 0);
		session.write(data);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(567)|DS|", s.getRecordedData(true));
		data = new Packet(PacketType.ECHO, "5").toBytes(0, 0);
		session.writenf(data);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(5)|DS|", s.getRecordedData(true));
		
		data = new Packet(PacketType.ECHO, "77").toBytes(4, 5);
		session.write(data, 4, data.length-9);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(77)|DS|", s.getRecordedData(true));
		data = new Packet(PacketType.ECHO, "778").toBytes(4, 5);
		session.writenf(data, 4, data.length-9);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(778)|DS|", s.getRecordedData(true));

		ByteBuffer buf;
		data = new Packet(PacketType.ECHO, "33").toBytes(0, 0);
		buf = ByteBuffer.wrap(data);
		session.write(buf);
		assertEquals(0, buf.remaining());
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(33)|DS|", s.getRecordedData(true));
		data = new Packet(PacketType.ECHO, "339").toBytes(0, 0);
		buf = ByteBuffer.wrap(data);
		session.writenf(buf);
		assertEquals(0, buf.remaining());
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(339)|DS|", s.getRecordedData(true));
		
		data = new Packet(PacketType.ECHO, "343").toBytes(0, 3);
		buf = ByteBuffer.wrap(data);
		session.write(buf, data.length-3);
		assertEquals(3, buf.remaining());
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(343)|DS|", s.getRecordedData(true));
		data = new Packet(PacketType.ECHO, "3369").toBytes(0, 3);
		buf = ByteBuffer.wrap(data);
		session.writenf(buf, data.length-3);
		assertEquals(3, buf.remaining());
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(3369)|DS|", s.getRecordedData(true));

		data = new Packet(PacketType.ECHO, "1234567890").toBytes();
		IByteBufferHolder holder = SessionTest.createHolder(session, data, 3,4);
		session.write(holder).sync(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals(0, holder.remaining());
		assertEquals("DR|$ECHO(1234567890)|DS|", s.getRecordedData(true));
		holder = SessionTest.createHolder(session, data, 3,4);
		session.writenf(holder);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(1234567890)|DS|", s.getRecordedData(true));
		assertEquals(0, holder.remaining());

		holder = SessionTest.createHolder(session, data, 3,4);
		session.write((Object)holder).sync(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals(0, holder.remaining());
		assertEquals("DR|$ECHO(1234567890)|DS|", s.getRecordedData(true));
		holder = SessionTest.createHolder(session, data, 3,4);
		session.writenf((Object)holder);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(1234567890)|DS|", s.getRecordedData(true));
		assertEquals(0, holder.remaining());
		
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}

	@Test
	public void testWriteWhenNotConnected() throws Exception {
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT+1); c.startServer();
		SocketAddress addr = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT);
		
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		DatagramSession session = c.getSession();

		byte[] data = new Packet(PacketType.ECHO, "567").toBytes(0, 0);
		session.send(addr, data);
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|$ECHO_RESPONSE(567)|", c.getRecordedData(true));
		assertEquals("DR|$ECHO(567)|DS|", s.getRecordedData(true));
		data = new Packet(PacketType.ECHO, "5").toBytes(0, 0);
		session.sendnf(addr, data);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(5)|DS|", s.getRecordedData(true));

		data = new Packet(PacketType.ECHO, "77").toBytes(4, 5);
		session.send(addr, data, 4, data.length-9);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(77)|DS|", s.getRecordedData(true));
		data = new Packet(PacketType.ECHO, "778").toBytes(4, 5);
		session.sendnf(addr, data, 4, data.length-9);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(778)|DS|", s.getRecordedData(true));

		ByteBuffer buf;
		data = new Packet(PacketType.ECHO, "33").toBytes(0, 0);
		buf = ByteBuffer.wrap(data);
		session.send(addr, buf);
		assertEquals(0, buf.remaining());
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(33)|DS|", s.getRecordedData(true));
		data = new Packet(PacketType.ECHO, "339").toBytes(0, 0);
		buf = ByteBuffer.wrap(data);
		session.sendnf(addr, buf);
		assertEquals(0, buf.remaining());
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(339)|DS|", s.getRecordedData(true));
		
		data = new Packet(PacketType.ECHO, "343").toBytes(0, 3);
		buf = ByteBuffer.wrap(data);
		session.send(addr, buf, data.length-3);
		assertEquals(3, buf.remaining());
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(343)|DS|", s.getRecordedData(true));
		data = new Packet(PacketType.ECHO, "3369").toBytes(0, 3);
		buf = ByteBuffer.wrap(data);
		session.sendnf(addr, buf, data.length-3);
		assertEquals(3, buf.remaining());
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(3369)|DS|", s.getRecordedData(true));

		data = new Packet(PacketType.ECHO, "1234567890").toBytes();
		IByteBufferHolder holder = SessionTest.createHolder(session, data, 3,4);
		session.send(addr, holder).sync(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals(0, holder.remaining());
		assertEquals("DR|$ECHO(1234567890)|DS|", s.getRecordedData(true));
		holder = SessionTest.createHolder(session, data, 3,4);
		session.sendnf(addr, holder);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(1234567890)|DS|", s.getRecordedData(true));
		assertEquals(0, holder.remaining());

		holder = SessionTest.createHolder(session, data, 3,4);
		session.send(addr, (Object)holder).sync(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals(0, holder.remaining());
		assertEquals("DR|$ECHO(1234567890)|DS|", s.getRecordedData(true));
		holder = SessionTest.createHolder(session, data, 3,4);
		session.sendnf(addr, (Object)holder);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(1234567890)|DS|", s.getRecordedData(true));
		assertEquals(0, holder.remaining());
		
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}
	
	@Test
	public void testOwningOfPassedDatagrams() throws Exception {
		//flag == false, heap buffer
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		DatagramSession session = c.getSession();

		byte[] data = new Packet(PacketType.ECHO, "567").toBytes(0, 0);
		session.suspendWrite();
		session.write(data);
		Arrays.fill(data, (byte)0);
		session.resumeWrite();
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(567)|DS|", s.getRecordedData(true));

		ByteBuffer buf;
		data = new Packet(PacketType.ECHO, "33").toBytes(0, 0);
		buf = ByteBuffer.wrap(data);
		session.suspendWrite();
		session.write(buf);
		Arrays.fill(data, (byte)0);
		assertEquals(0, buf.remaining());
		session.resumeWrite();
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(33)|DS|", s.getRecordedData(true));
		
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		//flag == true, heap buffer
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.optimizeDataCopying = true; c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		session = c.getSession();

		data = new Packet(PacketType.ECHO, "567").toBytes(0, 0);
		session.suspendWrite();
		session.write(data);
		data[data.length-1] = '8';
		session.resumeWrite();
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(568)|DS|", s.getRecordedData(true));

		data = new Packet(PacketType.ECHO, "33").toBytes(0, 0);
		buf = ByteBuffer.wrap(data);
		session.suspendWrite();
		session.write(buf);
		assertEquals(data.length, buf.remaining());
		data[data.length-1] = '4';
		session.resumeWrite();
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(34)|DS|", s.getRecordedData(true));

		data = new Packet(PacketType.ECHO, "33").toBytes(0, 4);
		buf = ByteBuffer.wrap(data);
		session.suspendWrite();
		session.write(buf, data.length-4);
		assertEquals(4, buf.remaining());
		Arrays.fill(data, (byte)0);
		session.resumeWrite();
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(33)|DS|", s.getRecordedData(true));
		
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		//flag == true, direct buffer
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); 
		c.optimizeDataCopying = true;
		c.directAllocator = true;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		session = c.getSession();
	
		data = new Packet(PacketType.ECHO, "567").toBytes(0, 0);
		session.suspendWrite();
		session.write(data);
		Arrays.fill(data, (byte)0);
		session.resumeWrite();
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(567)|DS|", s.getRecordedData(true));
		
		data = new Packet(PacketType.ECHO, "33").toBytes(0, 0);
		buf = ByteBuffer.wrap(data);
		session.suspendWrite();
		session.write(buf);
		assertEquals(data.length, buf.remaining());
		data[data.length-1] = '4';
		session.resumeWrite();
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(34)|DS|", s.getRecordedData(true));
		
		data = new Packet(PacketType.ECHO, "3388").toBytes(0, 2);
		buf = ByteBuffer.wrap(data);
		session.suspendWrite();
		session.write(buf, data.length-2);
		assertEquals(2, buf.remaining());
		Arrays.fill(data, (byte)0);
		data[data.length-1] = '4';
		session.resumeWrite();
		c.waitForDataSent(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO(3388)|DS|", s.getRecordedData(true));

		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}

	@Test
	public void testReleaseOfAllocatedBuffers() throws Exception {
		TestDatagramHandler handler = new TestDatagramHandler();
		TestAllocator allocator = new TestAllocator(false, true);
		handler.allocator = allocator;
		InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT);
		
		assertEquals(0, allocator.getSize());
		DatagramSession session = new DatagramSession(handler);
		assertEquals(0, allocator.getSize());
		try {
			session.send(address, new byte[10]);
			fail("Exception not thrown");
		}
		catch (IllegalSessionStateException e) {}

		s = new DatagramHandler(PORT);
		s.startServer();
		s.waitForSessionReady(TIMEOUT);
		
		//start and stop, releasing
		c = new DatagramHandler(PORT);
		allocator = new TestAllocator(false, true);
		c.allocator = allocator;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals(1, allocator.getSize());
		assertEquals(1, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals(0, allocator.getSize());
		assertEquals(1, allocator.getAllocatedCount());
		assertEquals(1, allocator.getReleasedCount());
		
		//writing, releasing
		c = new DatagramHandler(PORT);
		allocator = new TestAllocator(false, true);
		c.allocator = allocator;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		c.getSession().suspendWrite();
		byte[] data = new Packet(PacketType.ECHO, "33").toBytes();
		c.getSession().send(address, data);
		assertEquals(2, allocator.getSize());
		assertEquals(2, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		c.getSession().resumeWrite();
		c.waitForDataSent(TIMEOUT);
		assertEquals(1, allocator.getSize());
		assertEquals(2, allocator.getAllocatedCount());
		assertEquals(1, allocator.getReleasedCount());
		c.stop(TIMEOUT);
		assertEquals(0, allocator.getSize());
		assertEquals(2, allocator.getAllocatedCount());
		assertEquals(2, allocator.getReleasedCount());
		assertEquals(data.length, allocator.getReleased().get(0).capacity());

		//write suspended, releasing
		c = new DatagramHandler(PORT);
		allocator = new TestAllocator(false, true);
		c.allocator = allocator;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		c.getSession().suspendWrite();
		c.getSession().send(address, data);
		assertEquals(2, allocator.getSize());
		assertEquals(2, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		c.getSession().quickClose();
		c.waitForSessionEnding(TIMEOUT);
		c.stop(TIMEOUT);
		assertEquals(0, allocator.getSize());
		assertEquals(2, allocator.getAllocatedCount());
		assertEquals(2, allocator.getReleasedCount());

		//write, no releasing
		c = new DatagramHandler(PORT);
		allocator = new TestAllocator(false, false);
		c.allocator = allocator;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		c.getSession().suspendWrite();
		c.getSession().send(address, data);
		assertEquals(2, allocator.getSize());
		assertEquals(2, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		c.getSession().resumeWrite();
		c.waitForDataSent(TIMEOUT);
		assertEquals(2, allocator.getSize());
		assertEquals(2, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		c.stop(TIMEOUT);
		assertEquals(2, allocator.getSize());
		assertEquals(2, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		
		//write suspended, not releasing
		c = new DatagramHandler(PORT);
		allocator = new TestAllocator(false, false);
		c.allocator = allocator;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		c.getSession().suspendWrite();
		c.getSession().send(address, new Packet(PacketType.ECHO, "33").toBytes());
		assertEquals(2, allocator.getSize());
		assertEquals(2, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		c.getSession().quickClose();
		c.waitForSessionEnding(TIMEOUT);
		c.stop(TIMEOUT);
		assertEquals(2, allocator.getSize());
		assertEquals(2, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		
		//write suspended, releasing, can own passed data
		c = new DatagramHandler(PORT);
		c.optimizeDataCopying = true;
		allocator = new TestAllocator(false, true);
		c.allocator = allocator;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		c.getSession().suspendWrite();
		c.getSession().send(address, new Packet(PacketType.ECHO, "33").toBytes());
		assertEquals(0, allocator.getSize());
		assertEquals(0, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		c.getSession().quickClose();
		c.waitForSessionEnding(TIMEOUT);
		c.stop(TIMEOUT);
		assertEquals(0, allocator.getSize());
		assertEquals(0, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		
		s.stop(TIMEOUT);
	}

	@Test
	public void testExceptionInHandleReading() throws Exception {
		s = new DatagramHandler(PORT);
		s.useTestSession = true;
		s.startServer();
		c = new DatagramHandler(PORT); 
		c.useTestSession = true;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		
		((TestDatagramSession)s.getSession()).getInBufferException = true;
		c.write(new Packet(PacketType.NOP));
		
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("EXC|SCL|SEN|", s.getRecordedData(true));
		
	}

	@Test
	public void testExceptionInHandleWriting() throws Exception {
		s = new DatagramHandler(PORT);
		s.useTestSession = true;
		s.startServer();
		c = new DatagramHandler(PORT); 
		c.useTestSession = true;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		
		((TestDatagramSession)c.getSession()).calculateThroughputException = true;
		c.write(new Packet(PacketType.NOP));
		
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|EXC|SCL|SEN|", c.getRecordedData(true));
		
	}

	@Test
	public void testDataEventDetails() throws Exception {
		s = new DatagramHandler(PORT);
		s.recordDataEventDetails = true;
		s.startServer();
		c = new DatagramHandler(PORT); 
		c.recordDataEventDetails = true;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		
		Packet p = new Packet(PacketType.NOP);
		int pLen = p.toBytes().length;
		c.write(p);
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|"+pLen+"|", c.getRecordedData(true));
		assertEquals("DR|"+pLen+";"+c.getSession().getLocalAddress()+"|$NOP()|", s.getRecordedData(true));

		p = new Packet(PacketType.NOP,"1");
		pLen = p.toBytes().length;
		s.getSession().send(c.getSession().getLocalAddress(),p.toBytes());
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|"+pLen+"|NOP(1)|", c.getRecordedData(true));
		assertEquals("DS|"+pLen+";"+c.getSession().getLocalAddress()+"|", s.getRecordedData(true));
		
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
	}
	
	@Test
	public void testWriteWhenChannelIsNotConnected() throws Exception {
		s = new DatagramHandler(PORT);
		s.startServer();
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		IFuture<Void> f = s.getSession().write(new byte[1]);
		f.await(TIMEOUT);
		assertTrue(f.isFailed());
		assertTrue(f.isDone());
		assertTrue(f.cause().getClass() == NotYetConnectedException.class);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("EXC|SCL|SEN|", s.getRecordedData(true));
		s = null;
	}	
	
	public static int countRDNOP(String s, byte[] payload) {
		int off = 0;
		String rdnop = "DR|$NOP(" + new String(payload) + ")|";
		int i;
		int count = 0;
		
		while ((i = s.indexOf(rdnop, off)) != -1) {
			off = i + rdnop.length();
			count++;
		}
		return count;
	}
	
	@Test
	public void testWriteSpinCount() throws Exception {
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		s.getRecordedData(true);
		c.getRecordedData(true);
		
		byte[] payload = new byte[1000];
		Arrays.fill(payload, (byte)'1');
		byte[] data = new Packet(PacketType.NOP, new String(payload)).toBytes();
		
		DatagramSession session = c.getSession();
		session.suspendWrite();
		for (int i=0; i<32; i++) {
			session.write(data);
		}
		session.resumeWrite();
		waitFor(100);
		assertEquals("DS|DS|", c.getRecordedData(true));
		assertEquals(32, countRDNOP(s.getRecordedData(true), payload));
		c.stop(TIMEOUT);
		
		c = new DatagramHandler(PORT);
		c.maxWriteSpinCount = 1;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		session = c.getSession();
		session.suspendWrite();
		for (int i=0; i<32; i++) {
			session.write(data);
		}
		session.resumeWrite();
		waitFor(100);
		assertEquals("DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|", c.getRecordedData(true));
		assertEquals(32, countRDNOP(s.getRecordedData(true), payload));
		c.stop(TIMEOUT);
		
		c = new DatagramHandler(PORT);
		c.maxWriteSpinCount = 16;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		session = c.getSession();
		session.suspendWrite();
		session.write(new Packet(PacketType.NOP, "1234").toBytes());
		TestSelectionKey key = new TestSelectionKey(new TestDatagramChannel());
		Method m = SelectorLoop.class.getDeclaredMethod("handleWriting", DatagramSession.class, SelectionKey.class, int.class);
		m.setAccessible(true);
		assertEquals(new Integer(0), m.invoke(c.loop, session, key, 1));
		session.resumeWrite();
		waitFor(50);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|$NOP(1234)|", s.getRecordedData(true));
		
		c.closeInEvent = EventType.DATA_SENT;
		c.closeType = StoppingType.DIRTY;
		session.suspendWrite();
		for (int i=0; i<15; i++) {
			session.write(data);
		}
		session.resumeWrite();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		waitFor(100);
		assertEquals(15, countRDNOP(s.getRecordedData(true), payload));
		c.stop(TIMEOUT);
		
		c = new DatagramHandler(PORT);
		c.maxWriteSpinCount = 16;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		session = c.getSession();
		session.suspendWrite();
		session.write(new Packet(PacketType.NOP, "1234").toBytes());
		c.writeInEvent = EventType.DATA_SENT;
		c.packetToWriteInEvent = new Packet(PacketType.NOP, "5678");
		session.resumeWrite();
		waitFor(100);
		assertEquals("DS|DS|", c.getRecordedData(true));
		assertEquals("DR|$NOP(1234)|DR|$NOP(5678)|", s.getRecordedData(true));
	}
	
	@Test
	public void testSendWhenChannelIsConnected() throws Exception {
		s = new DatagramHandler(PORT);
		DatagramHandler s2 = new DatagramHandler(PORT+1);
		c = new DatagramHandler(PORT);
		s.startServer();
		s2.startServer();
		c.startClient();
		
		s.waitForSessionReady(TIMEOUT);
		s2.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		
		s.getRecordedData(true);
		s2.getRecordedData(true);
		c.write(new Packet(PacketType.NOP));
		s.waitForDataRead(TIMEOUT);
		assertEquals("", s2.getRecordedData(true));
		assertEquals("DR|$NOP()|", s.getRecordedData(true));
		
		c.getSession().send(s2.getSession().getLocalAddress(), new Packet(PacketType.NOP, "1").toBytes());
		s.waitForDataRead(TIMEOUT);
		assertEquals("", s2.getRecordedData(true));
		assertEquals("DR|$NOP(1)|", s.getRecordedData(true));
		s2.stop(TIMEOUT);
	}
	
	@Test
	public void testTimer() throws Exception {
		s = new DatagramHandler(PORT);
		s.timer = new DefaultTimer();
		c = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		s.getRecordedData(true);
		c.getRecordedData(true);
		
		assertTrue(c.getSession().getTimer() == UnsupportedSessionTimer.INSTANCE);
		s.getSession().getTimer().scheduleEvent("t1", 100);
		waitFor(80);
		assertEquals("", s.getRecordedData(true));
		waitFor(40);
		assertEquals("TIM;t1|", s.getRecordedData(true));
		
		((DefaultTimer)s.timer).cancel();
	}
	
	private void testCloseInSessionCreatedEvent(StoppingType type) throws Exception{
		s = new DatagramHandler(PORT);
		s.startServer();
		c = new DatagramHandler(PORT);
		c.closeInEvent = EventType.SESSION_CREATED;
		c.closeType = type;
		c.startClient();
		waitFor(100);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		assertEquals("SCR|SEN|", c.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, c.getSession().closing);
		s.stop(TIMEOUT);
		
		s = new DatagramHandler(PORT);
		s.closeInEvent = EventType.SESSION_CREATED;
		s.closeType = type;
		s.startServer();
		c = new DatagramHandler(PORT);
		c.startClient();
		waitFor(100);
		assertEquals("SCR|SEN|", s.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, s.getSession().closing);
		c.stop(TIMEOUT);
	}	
	
	@Test
	public void testCloseInSessionCreatedEvent() throws Exception{
		testCloseInSessionCreatedEvent(StoppingType.GENTLE);
		testCloseInSessionCreatedEvent(StoppingType.QUICK);
		testCloseInSessionCreatedEvent(StoppingType.DIRTY);
	}	

	private void testCloseInSessionOpenedEvent(StoppingType type) throws Exception{
		s = new DatagramHandler(PORT);
		s.startServer();
		c = new DatagramHandler(PORT);
		c.closeInEvent = EventType.SESSION_OPENED;
		c.closeType = type;
		c.startClient();
		waitFor(100);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		assertEquals("SCR|SOP|SCL|SEN|", c.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, c.getSession().closing);
		s.stop(TIMEOUT);
		
		s = new DatagramHandler(PORT);
		s.closeInEvent = EventType.SESSION_OPENED;
		s.closeType = type;
		s.startServer();
		c = new DatagramHandler(PORT);
		c.startClient();
		waitFor(100);
		assertEquals("SCR|SOP|SCL|SEN|", s.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, s.getSession().closing);
		c.stop(TIMEOUT);
	}	
	
	@Test
	public void testCloseInSessionOpenedEvent() throws Exception{
		testCloseInSessionOpenedEvent(StoppingType.GENTLE);
		testCloseInSessionOpenedEvent(StoppingType.QUICK);
		testCloseInSessionOpenedEvent(StoppingType.DIRTY);
	}	
	
	private void testCloseInSessionClosedOrEndingEvent(StoppingType type, EventType event) throws Exception{
		s = new DatagramHandler(PORT);
		s.startServer();
		c = new DatagramHandler(PORT);
		c.closeInEvent = event;
		c.closeType = type;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getSession().close();
		waitFor(100);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|SCL|SEN|", c.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, c.getSession().closing);
		s.stop(TIMEOUT);
	}
	
	@Test
	public void testOptimizedDataCopyingRead() throws Exception {
		s = new DatagramHandler(PORT);
		s.startServer();
		c = new DatagramHandler(PORT);
		c.optimizeDataCopying = true;
		c.allocator = new TestAllocator(false, true);
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		
		SocketAddress a = c.getSession().getLocalAddress();
		assertNull(getInBuffer(c.getSession()));
		assertEquals(0, c.allocator.getAllocatedCount());
		s.getSession().send(a, new Packet(PacketType.NOP).toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|BUF|NOP()|", c.getRecordedData(true));
		assertEquals(1, c.allocator.getAllocatedCount());
		c.allocator.release(c.bufferRead);
		assertNull(getInBuffer(c.getSession()));
		assertEquals(0, c.allocator.getSize());

		c.getSession().suspendRead();
		s.getSession().send(a, new Packet(PacketType.NOP, "123456").toBytes());
		Method m = SelectorLoop.class.getDeclaredMethod("handleReading", DatagramSession.class, SelectionKey.class);
		m.setAccessible(true);
		TestSelectionKey key = new TestSelectionKey(new TestDatagramChannel());
		m.invoke(c.loop, c.getSession(), key);
		assertEquals(0, c.allocator.getSize());
		assertNull(getInBuffer(c.getSession()));
		key = new TestSelectionKey(new TestDatagramChannel(false));
		m.invoke(c.loop, c.getSession(), key);
		assertEquals(0, c.allocator.getSize());
		assertNull(getInBuffer(c.getSession()));
		c.getSession().resumeRead();
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|BUF|NOP(123456)|", c.getRecordedData(true));
		assertNull(getInBuffer(c.getSession()));
		assertEquals(1, c.allocator.getSize());
		c.allocator.release(c.bufferRead);
		assertEquals(0, c.allocator.getSize());
		
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		s = new DatagramHandler(PORT);
		s.optimizeDataCopying = true;
		s.allocator = new TestAllocator(false, true);
		s.startServer();
		c = new DatagramHandler(PORT);
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		
		assertEquals(0, s.allocator.getAllocatedCount());
		c.getSession().write(new Packet(PacketType.NOP).toBytes());
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|BUF|$NOP()|", s.getRecordedData(true));
		assertEquals(1, s.allocator.getAllocatedCount());
		s.allocator.release(s.bufferRead);
		assertNull(getInBuffer(s.getSession()));
		assertEquals(0, s.allocator.getSize());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		
		s = new DatagramHandler(PORT);
		s.startServer();
		c = new DatagramHandler(PORT);
		c.optimizeDataCopying = true;
		c.allocator = new TestAllocator(false, false);
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		
		a = c.getSession().getLocalAddress();
		ByteBuffer b = getInBuffer(c.getSession());
		assertEquals(1, c.allocator.getAllocatedCount());
		s.getSession().send(a, new Packet(PacketType.NOP).toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", c.getRecordedData(true));
		assertEquals(1, c.allocator.getAllocatedCount());
		assertTrue(b == getInBuffer(c.getSession()));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	
		s = new DatagramHandler(PORT);
		s.optimizeDataCopying = true;
		s.allocator = new TestAllocator(false, false);
		s.startServer();
		c = new DatagramHandler(PORT);
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		
		b = getInBuffer(s.getSession());
		assertEquals(1, s.allocator.getAllocatedCount());
		c.getSession().write(new Packet(PacketType.NOP).toBytes());
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|$NOP()|", s.getRecordedData(true));
		assertEquals(1, s.allocator.getAllocatedCount());
		assertTrue(b == getInBuffer(s.getSession()));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		s = new DatagramHandler(PORT);
		s.optimizeDataCopying = false;
		s.allocator = new TestAllocator(false, true);
		s.startServer();
		c = new DatagramHandler(PORT);
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		s.getRecordedData(true);
		s.getSession().suspendRead();
		c.getSession().write(new Packet(PacketType.NOP, "12").toBytes());
		key = new TestSelectionKey(new TestDatagramChannel(false));
		assertEquals(1, s.allocator.getSize());
		b = getInBuffer(c.getSession());
		assertNotNull(b);
		m.invoke(c.loop, c.getSession(), key);
		assertEquals(1, s.allocator.getSize());
		assertTrue(b == getInBuffer(c.getSession()));
		s.getSession().resumeRead();
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|$NOP(12)|", s.getRecordedData(true));
		
	}
	
	@Test
	public void testOptimizedDataCopyingWrite() throws Exception {
		s = new DatagramHandler(PORT);
		s.startServer();
		c = new DatagramHandler(PORT);
		c.optimizeDataCopying = true;
		c.allocator = new TestAllocator(false, true);
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		
		DatagramSession session = c.getSession();
		ByteBuffer b = session.allocate(100);
		b.put(new Packet(PacketType.NOP).toBytes());
		b.flip();
		session.write(b);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|$NOP()|", s.getRecordedData(true));
		assertEquals(1, c.allocator.getReleasedCount());
		assertEquals(1, c.allocator.getAllocatedCount());
		assertTrue(b == c.allocator.getReleased().get(0));
		assertEquals(0, c.allocator.getSize());
		
		byte[] data = new Packet(PacketType.NOP, "1234567890").toBytes();
		ByteBufferHolder holder = SessionTest.createHolder(session, data, 2,3);
		assertEquals(1, c.allocator.getReleasedCount());
		assertEquals(4, c.allocator.getAllocatedCount());
		assertEquals(3, c.allocator.getSize());
		session.write(holder);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|$NOP(1234567890)|", s.getRecordedData(true));
		assertEquals(4, c.allocator.getReleasedCount());
		assertEquals(4, c.allocator.getAllocatedCount());
		assertEquals(0, c.allocator.getSize());
		assertTrue(holder.toArray()[0] == c.allocator.getReleased().get(1));
		assertTrue(holder.toArray()[1] == c.allocator.getReleased().get(2));
		assertTrue(holder.toArray()[2] == c.allocator.getReleased().get(3));
		c.stop(TIMEOUT);
		
		c = new DatagramHandler(PORT);
		c.optimizeDataCopying = true;
		c.allocator = new TestAllocator(false, false);
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		
		session = c.getSession();
		b = session.allocate(100);
		b.put(new Packet(PacketType.NOP).toBytes());
		b.flip();
		session.write(b);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|$NOP()|", s.getRecordedData(true));
		assertEquals(0, c.allocator.getReleasedCount());
		assertEquals(2, c.allocator.getAllocatedCount());

		holder = SessionTest.createHolder(session, data, 2,3);
		assertEquals(0, c.allocator.getReleasedCount());
		assertEquals(5, c.allocator.getAllocatedCount());
		session.write(holder);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|$NOP(1234567890)|", s.getRecordedData(true));
		assertEquals(0, c.allocator.getReleasedCount());
		assertEquals(5, c.allocator.getAllocatedCount());
		c.stop(TIMEOUT);
		
		c = new DatagramHandler(PORT);
		c.optimizeDataCopying = false;
		c.allocator = new TestAllocator(false, true);
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		
		session = c.getSession();
		b = session.allocate(100);
		b.put(new Packet(PacketType.NOP).toBytes());
		b.flip();
		session.write(b);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|$NOP()|", s.getRecordedData(true));
		assertEquals(1, c.allocator.getReleasedCount());
		assertFalse(b == c.allocator.getReleased().get(0));
		assertEquals(3, c.allocator.getAllocatedCount());
		
		holder = SessionTest.createHolder(session, data, 2,3);
		assertEquals(1, c.allocator.getReleasedCount());
		assertEquals(6, c.allocator.getAllocatedCount());
		session.write(holder);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|$NOP(1234567890)|", s.getRecordedData(true));
		assertEquals(2, c.allocator.getReleasedCount());
		assertEquals(7, c.allocator.getAllocatedCount());
	}

	@Test
	public void testOptimizedDataCopyingSend() throws Exception {
		s = new DatagramHandler(PORT);
		s.startServer();
		c = new DatagramHandler(PORT+1);
		c.optimizeDataCopying = true;
		c.allocator = new TestAllocator(false, true);
		c.startServer();
		SocketAddress addr = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT);
	
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		DatagramSession session = c.getSession();
		
		assertEquals(0, c.allocator.getSize());
		assertEquals(0, c.allocator.getAllocatedCount());
		assertEquals(0, c.allocator.getReleasedCount());
		byte[] data = new Packet(PacketType.ECHO, "1234567890").toBytes();
		ByteBufferHolder holder = SessionTest.createHolder(session, data);
		assertEquals(1, c.allocator.getSize());
		assertEquals(1, c.allocator.getAllocatedCount());
		assertEquals(0, c.allocator.getReleasedCount());
		session.send(addr, holder).sync(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|BUF|$ECHO_RESPONSE(1234567890)|", c.getRecordedData(true));
		assertEquals("DR|$ECHO(1234567890)|DS|", s.getRecordedData(true));
		assertEquals(1, c.allocator.getSize());
		session.release(c.bufferRead);
		assertEquals(0, c.allocator.getSize());
		assertEquals(2, c.allocator.getAllocatedCount());
		assertEquals(2, c.allocator.getReleasedCount());
		
		holder = SessionTest.createHolder(session, data, 1, 2, 3);
		assertEquals(4, c.allocator.getSize());
		assertEquals(6, c.allocator.getAllocatedCount());
		assertEquals(2, c.allocator.getReleasedCount());
		session.sendnf(addr, holder);
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|BUF|$ECHO_RESPONSE(1234567890)|", c.getRecordedData(true));
		assertEquals("DR|$ECHO(1234567890)|DS|", s.getRecordedData(true));
		assertEquals(1, c.allocator.getSize());
		session.release(c.bufferRead);
		assertEquals(0, c.allocator.getSize());
		assertEquals(8, c.allocator.getAllocatedCount());
		assertEquals(8, c.allocator.getReleasedCount());
		c.stop(TIMEOUT);

		c = new DatagramHandler(PORT+1);
		c.optimizeDataCopying = true;
		c.allocator = new TestAllocator(false, false);
		c.startServer();
		addr = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT);
		c.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		session = c.getSession();
		assertEquals(1, c.allocator.getSize());
		assertEquals(1, c.allocator.getAllocatedCount());

		holder = SessionTest.createHolder(session, data, 1, 2, 3);
		assertEquals(5, c.allocator.getSize());
		assertEquals(5, c.allocator.getAllocatedCount());
		assertEquals(0, c.allocator.getReleasedCount());
		session.send(addr, holder).sync(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|$ECHO_RESPONSE(1234567890)|", c.getRecordedData(true));
		assertEquals("DR|$ECHO(1234567890)|DS|", s.getRecordedData(true));
		assertEquals(6, c.allocator.getSize());
		assertEquals(6, c.allocator.getAllocatedCount());
		assertEquals(0, c.allocator.getReleasedCount());
		
	}
	
	@Test
	public void testCloseInSessionClosedEvent() throws Exception {
		testCloseInSessionClosedOrEndingEvent(StoppingType.GENTLE, EventType.SESSION_CLOSED);
		testCloseInSessionClosedOrEndingEvent(StoppingType.QUICK, EventType.SESSION_CLOSED);
		testCloseInSessionClosedOrEndingEvent(StoppingType.DIRTY, EventType.SESSION_CLOSED);
	}
	
	@Test
	public void testCloseInSessionEndingEvent() throws Exception {
		testCloseInSessionClosedOrEndingEvent(StoppingType.GENTLE, EventType.SESSION_ENDING);
		testCloseInSessionClosedOrEndingEvent(StoppingType.QUICK, EventType.SESSION_ENDING);
		testCloseInSessionClosedOrEndingEvent(StoppingType.DIRTY, EventType.SESSION_ENDING);
	}

}
