/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017 SNF4J contributors
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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.session.SessionState;

public class DatagramSessionTest {
	long TIMEOUT = 2000;
	int PORT = 7779;
	IDatagramHandler handler = new TestDatagramHandler();
	
	DatagramHandler c;
	DatagramHandler s;

	private void waitFor(long millis) throws InterruptedException {
		Thread.sleep(millis);
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
	public void testGetAddress() throws Exception {
		DatagramSession session = new DatagramSession(handler);

		assertTrue(handler == session.getHandler());
		
		assertNull(session.getLocalAddress());
		assertNull(session.getRemoteAddress());
		
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		
		s.startServer();
		c.startClient();
		s.waitForSessionOpen(TIMEOUT);
		c.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));

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
	public void testWrite() throws Exception {
		DatagramSession session = new DatagramSession(handler);
		
		session.write(new byte[10]);
		assertTrue(session.outQueue.isEmpty());
		
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		s.waitForSessionOpen(TIMEOUT);
		c.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));

		c.write(new Packet(PacketType.ECHO));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE()|", c.getRecordedData(true));
		assertEquals("DR|$ECHO()|DS|", s.getRecordedData(true));
		
		s.getSession().write(c.getSession().getLocalAddress(), new Packet(PacketType.ECHO, "1").toBytes());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DR|ECHO(1)|DS|", c.getRecordedData(true));
		assertEquals("DS|DR|$ECHO_RESPONSE(1)|", s.getRecordedData(true));
		
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		
		c.write(new Packet(PacketType.ECHO, "2"));
		assertTrue(c.getSession().outQueue.isEmpty());
		waitFor(2000);
		assertEquals("", c.getRecordedData(true));
	}
	
	@Test
	public void testSuspendAndResume() throws Exception {
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		s.waitForSessionOpen(TIMEOUT);
		c.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
		
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
		session.suspendWrite();
		assertFalse(session.isWriteSuspended());
		session.resumeWrite();
		session.suspendRead();
		assertFalse(session.isReadSuspended());
		session.resumeRead();
		assertFalse(session.suspend(SelectionKey.OP_READ));
		assertFalse(session.resume(SelectionKey.OP_READ));
		
		//when key is null
		session = new DatagramSession(handler);
		session.suspendWrite();
		assertFalse(session.isWriteSuspended());
		session.resumeWrite();
		session.suspendRead();
		assertFalse(session.isReadSuspended());
		session.resumeRead();

		c = null;
		
	}	
	
	@Test
	public void testStatistics() throws Exception {
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		
		long t0 = System.currentTimeMillis();
		s.startServer();
		c.startClient();
		
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		long t1 = System.currentTimeMillis();
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));

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

		ss.write(cs.getLocalAddress(), new Packet(PacketType.NOP, "12345").toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|", s.getRecordedData(true));
		assertEquals("DR|NOP(12345)|", c.getRecordedData(true));
		assertEquals(8, cs.getReadBytes());
		assertEquals(7, cs.getWrittenBytes());
		assertEquals(7, ss.getReadBytes());
		assertEquals(8, ss.getWrittenBytes());

		ss.write(cs.getLocalAddress(), new Packet(PacketType.ECHO, "").toBytes());
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		
	}
	
	private void testCloseOutsideSuspendWrite(boolean write, boolean quickClose) throws Exception {
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
		s.getSession().suspendWrite();
		c.getSession().suspendWrite();
		if (write) s.getSession().write(c.getSession().getLocalAddress(), new Packet(PacketType.NOP).toBytes());
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
		
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
		StringBuilder sb = new StringBuilder();
		if (write) sb.append("W");
		if (quickClose) sb.append("Q");
		
		s.getSession().write(c.getSession().getLocalAddress(), new Packet(PacketType.SUSPEND_WRITE_CLOSE, sb.toString()).toBytes());
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|SUSPEND_WRITE_CLOSE(" + sb.toString() +")|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		assertEquals("", c.getRecordedData(true));
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|", s.getRecordedData(true));
		c = new DatagramHandler(PORT); c.startClient();
		c.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		
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
		
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
		session = c.getSession();
		session.suspendRead();
		session.suspendWrite();
		c.write(new Packet(PacketType.NOP));
		session.key.interestOps(session.key.interestOps() | SelectionKey.OP_WRITE);
		session.close();
		session.loop.selector.wakeup(); //need to wake up as we set OP_WRITE in illegal way
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|$NOP()|", s.getRecordedData(true));

		//close with written data (other side)
		c = new DatagramHandler(PORT); c.startClient();
		c.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		session = s.getSession();
		session.suspendRead();
		session.suspendWrite();
		session.write(c.getSession().getLocalAddress(), new Packet(PacketType.NOP).toBytes());
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
	public void testIgnorePossiblyIncomplete() throws Exception {
		s = new DatagramHandler(PORT); s.startServer();
		c = new DatagramHandler(PORT); c.startClient();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));

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

		//sending pocket with size that equals than buffer size
		bytes = new byte[min];
		Arrays.fill(bytes, (byte)'B');
		payload = new String(bytes);
		c.write(new Packet(PacketType.NOP, payload));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataReceived(TIMEOUT);
		waitFor(1000);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|", s.getRecordedData(true));

		//sending pocket with size that equals than buffer size (retry)
		bytes = new byte[min];
		Arrays.fill(bytes, (byte)'B');
		payload = new String(bytes);
		c.write(new Packet(PacketType.NOP, payload));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		waitFor(1000);
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));

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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));

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
	public void testToString() throws Exception {
		SelectorLoop loop = new SelectorLoop();
		
		SocketChannel sc = SocketChannel.open();
		assertEquals(sc.toString(), loop.toString(sc));
		DatagramChannel dc = DatagramChannel.open();
		assertEquals("sun.nio.ch.DatagramChannelImpl[local=unknown]", loop.toString(dc));
		dc.socket().bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 7788));
		assertEquals("sun.nio.ch.DatagramChannelImpl[local=/127.0.0.1:7788]", loop.toString(dc));
		dc.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.2"), 7789));
		assertEquals("sun.nio.ch.DatagramChannelImpl[local=/127.0.0.1:7788,remote=/127.0.0.2:7789]", loop.toString(dc));
		assertNull(loop.toString(null));
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
}
