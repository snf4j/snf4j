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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.codec.DefaultCodecExecutor;

public class DatagramServerSessionTest {

	long TIMEOUT = 2000;
	int PORT = 7779;
	
	DatagramHandler c;
	DatagramHandler c2;
	DatagramHandler s;
	
	@Before
	public void before() {
		s = c = c2 = null;
	}
	
	@After
	public void after() throws InterruptedException {
		if (c != null) c.stop(TIMEOUT);
		if (c2 != null) c2.stop(TIMEOUT);
		if (s != null) s.stop(TIMEOUT);
	}
	
	private void waitFor(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}

	private DefaultCodecExecutor codec() {
		TestCodec codec = new TestCodec();
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("3", codec.PBE());
		p.getPipeline().add("4", codec.BPE());
		return p;
	}
	
	byte[] nop(String s) {
		return new Packet(PacketType.NOP, s).toBytes();
	}
	
	byte[] nop() {
		return nop("");
	}
	
	DatagramSession getDelegate(DatagramSession session) throws Exception {
		Field f = DatagramServerSession.class.getDeclaredField("delegate");
		f.setAccessible(true);
		
		return (DatagramSession) f.get(session);
	}
	
	@Test
	public void testWrite() throws Exception {
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.codecPipeline = codec();
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		
		DatagramSession session = c.getSession();
		session.write(new Packet(PacketType.NOP).toBytes());
		s.waitForDataRead(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		session = s.getSession();
		assertTrue(session instanceof DatagramServerSession);

		session.write(new Packet(PacketType.NOP, "1").toBytes()).sync(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(1e)|", c.getRecordedData(true));
		session.writenf(new Packet(PacketType.NOP, "2").toBytes());
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(2e)|", c.getRecordedData(true));
		
		session.write(new Packet(PacketType.NOP, "12").toBytes(3, 5), 3, 5).sync(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(12e)|", c.getRecordedData(true));
		session.writenf(new Packet(PacketType.NOP, "23").toBytes(3, 5), 3, 5);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(23e)|", c.getRecordedData(true));
	
		session.write(ByteBuffer.wrap(new Packet(PacketType.NOP).toBytes())).sync(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(e)|", c.getRecordedData(true));
		session.writenf(ByteBuffer.wrap(new Packet(PacketType.NOP, "1").toBytes()));
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(1e)|", c.getRecordedData(true));

		session.write(ByteBuffer.wrap(new Packet(PacketType.NOP).toBytes(0,5)), 3).sync(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(e)|", c.getRecordedData(true));
		session.writenf(ByteBuffer.wrap(new Packet(PacketType.NOP, "2").toBytes(0,6)), 4);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(2e)|", c.getRecordedData(true));
	
		session.write(new Packet(PacketType.NOP)).sync(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(e)|", c.getRecordedData(true));
		session.writenf(new Packet(PacketType.NOP, "3"));
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(3e)|", c.getRecordedData(true));
	}
	
	@Test
	public void testSend() throws Exception {
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		c2 = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.codecPipeline = codec();
		s.startServer();
		c.startClient();
		c2.startClient();
		c.waitForSessionReady(TIMEOUT);
		c2.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", c2.getRecordedData(true));
		
		c.getSession().write(new Packet(PacketType.NOP).toBytes());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		c2.getSession().write(new Packet(PacketType.NOP).toBytes());
		s.waitForDataRead(TIMEOUT);
		c2.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		assertEquals("DS|", c2.getRecordedData(true));
		DatagramSession session = s.getSession();
		assertTrue(session instanceof DatagramServerSession);
		SocketAddress addr = c.getSession().getLocalAddress();
		
		session.send(addr, new Packet(PacketType.NOP, "1").toBytes()).sync(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(1e)|", c.getRecordedData(true));
		session.sendnf(addr, new Packet(PacketType.NOP, "2").toBytes());
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(2e)|", c.getRecordedData(true));
	
		session.send(addr, new Packet(PacketType.NOP, "12").toBytes(3, 5), 3, 5).sync(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(12e)|", c.getRecordedData(true));
		session.sendnf(addr, new Packet(PacketType.NOP, "23").toBytes(3, 5), 3, 5);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(23e)|", c.getRecordedData(true));
	
		session.send(addr, ByteBuffer.wrap(new Packet(PacketType.NOP).toBytes())).sync(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(e)|", c.getRecordedData(true));
		session.sendnf(addr, ByteBuffer.wrap(new Packet(PacketType.NOP, "1").toBytes()));
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(1e)|", c.getRecordedData(true));

		session.send(addr, ByteBuffer.wrap(new Packet(PacketType.NOP).toBytes(0,5)), 3).sync(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(e)|", c.getRecordedData(true));
		session.sendnf(addr, ByteBuffer.wrap(new Packet(PacketType.NOP, "2").toBytes(0,6)), 4);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(2e)|", c.getRecordedData(true));
	
		session.send(addr, new Packet(PacketType.NOP)).sync(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(e)|", c.getRecordedData(true));
		session.sendnf(addr, new Packet(PacketType.NOP, "3"));
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(3e)|", c.getRecordedData(true));
		
	}	
	
	@Test
	public void testClose() throws Exception {
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		DatagramSession sup = s.getSession();
		
		c.getSession().write(nop());
		s.waitForSessionReady(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		s.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertFalse(s.getSession().isOpen());
		assertTrue(sup.isOpen());
		
		c.getSession().write(nop());
		s.waitForSessionReady(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		s.getSession().quickClose();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertFalse(s.getSession().isOpen());
		assertTrue(sup.isOpen());

		c.getSession().write(nop());
		s.waitForSessionReady(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		s.getSession().dirtyClose();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertFalse(s.getSession().isOpen());
		assertTrue(sup.isOpen());
		
	}
	
	@Test
	public void testSuspend() throws Exception {
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		
		c.getSession().write(nop());
		s.waitForSessionReady(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));

		DatagramSession session = s.getSession();
		assertFalse(session.isWriteSuspended());
		assertFalse(session.isReadSuspended());
		
		session.suspendRead();
		assertTrue(session.isReadSuspended());
		c.getSession().write(nop());
		c.waitForDataSent(TIMEOUT);
		waitFor(200);
		assertEquals("", s.getRecordedData(true));
		session.resumeRead();
		assertFalse(session.isReadSuspended());
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP()|", s.getRecordedData(true));
		
		c.getRecordedData(true);
		session.suspendWrite();
		assertTrue(session.isWriteSuspended());
		session.write(nop());
		waitFor(200);
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		session.resumeWrite();
		assertFalse(session.isWriteSuspended());
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|", s.getRecordedData(true));
		assertEquals("DR|NOP()|", c.getRecordedData(true));
	}
	
	@Test
	public void testFutures() throws Exception {
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		
		c.getSession().write(nop());
		s.waitForSessionReady(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		
		assertTrue(s.getSession().getCreateFuture().isDone());
		assertTrue(s.getSession().getCreateFuture().isSuccessful());
		assertTrue(s.getSession().getOpenFuture().isDone());
		assertTrue(s.getSession().getOpenFuture().isSuccessful());
		assertTrue(s.getSession().getReadyFuture().isDone());
		assertTrue(s.getSession().getReadyFuture().isSuccessful());
		assertFalse(s.getSession().getCloseFuture().isDone());
		assertFalse(s.getSession().getEndFuture().isDone());
		
		s.getSession().close();
		s.getSession().getCloseFuture().sync(TIMEOUT);
		s.getSession().getEndFuture().sync(TIMEOUT);	
	}
	
	@Test 
	public void testTimes() throws Exception {
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		c2 = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.startServer();
		c.startClient();
		c2.startClient();
		c.waitForSessionReady(TIMEOUT);
		c2.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		
		c2.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		DatagramSession session0 = s.getSession();
		
		
		long ms = System.currentTimeMillis();
		c.getSession().write(nop());
		s.waitForSessionReady(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		long ms2 = System.currentTimeMillis();
		DatagramSession session = s.getSession();
		
		assertTrue(ms2 >= session.getCreationTime());
		assertTrue(session.getCreationTime() >= ms);
		
		assertTrue(session.getLastIoTime() == session.getLastReadTime());
		assertTrue(session.getLastIoTime() >= session.getCreationTime());
		assertTrue(session.getLastIoTime() <= ms2);
		
		waitFor(10);
		assertTrue(session.getLastWriteTime() == session.getCreationTime());
		session.write(nop()).sync(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertTrue(session.getLastWriteTime() > session.getCreationTime());
		assertTrue(session.getLastWriteTime() == session.getLastIoTime());
		assertTrue(session.getLastWriteTime() > session.getLastReadTime());
		assertTrue(session.getLastWriteTime() <= System.currentTimeMillis());
		
		ms = session.getLastReadTime();
		waitFor(10);
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		assertTrue(session.getLastReadTime() > ms);
		assertTrue(session.getLastReadTime() == session.getLastIoTime());
		assertTrue(session.getLastReadTime() > session.getLastWriteTime());
		assertTrue(session.getLastReadTime() <= System.currentTimeMillis());
		
		assertTrue(session0.getCreationTime() == session0.getLastIoTime());
		assertTrue(session0.getCreationTime() == session0.getLastReadTime());
		assertTrue(session0.getCreationTime() == session0.getLastWriteTime());
	}
	
	@Test
	public void testThroughput() throws Exception {
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		c2 = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.startServer();
		c.startClient();
		c2.startClient();
		c.waitForSessionReady(TIMEOUT);
		c2.waitForSessionReady(TIMEOUT);

		c2.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		DatagramSession session0 = s.getSession();

		long ms = System.currentTimeMillis();
		c.getSession().write(nop());
		s.waitForSessionReady(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		DatagramSession session = s.getSession();
		
		assertEquals(0.0, session0.getReadBytesThroughput(), 0.001);
		assertEquals(0.0, session.getReadBytesThroughput(), 0.001);
		assertEquals(0.0, session0.getWrittenBytesThroughput(), 0.001);
		assertEquals(0.0, session.getWrittenBytesThroughput(), 0.001);
		
		byte[] bytes = nop(new String(new byte[1000]));
		long size = 0;
		for (int i=0; i<350; i++) {
			c.getSession().write(bytes);
			s.waitForDataRead(TIMEOUT);
			waitFor(10);
			size += bytes.length;
		}
		long ms2 = System.currentTimeMillis();
		double avg = size * 1000.0 / (ms2 - ms);
		
		assertEquals(0.0, session0.getReadBytesThroughput(), 0.001);
		assertEquals(avg, session.getReadBytesThroughput(), avg * 20 / 100);
		assertEquals(0.0, session0.getWrittenBytesThroughput(), 0.001);
		assertEquals(0.0, session.getWrittenBytesThroughput(), 0.001);
		
		ms = System.currentTimeMillis();
		for (int i=0; i<350; i++) {
			session.write(bytes);
			c.waitForDataRead(TIMEOUT);
			waitFor(10);
		}
		ms2 = System.currentTimeMillis();
		avg = size * 1000.0 / (ms2 - ms);
		assertEquals(0.0, session0.getWrittenBytesThroughput(), 0.001);
		assertEquals(avg, session.getWrittenBytesThroughput(), avg * 20 / 100);
		
	}
	
	@Test
	public void testMiscFunctions() throws Exception {
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.codecPipeline = codec();
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		DatagramSession session = s.getSession();
		assertTrue(getDelegate(session) == session.getParent());
		assertEquals("org.snf4j.core.DatagramHandler$Handler", session.getHandler().getClass().getName());
		assertTrue(session.getConfig().getClass() == session.getHandler().getConfig().getClass());
		assertNull(session.getCodecPipeline());
		session.preCreated();
		session.postEnding();
	}
}
