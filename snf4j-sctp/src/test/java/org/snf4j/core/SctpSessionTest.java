package org.snf4j.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Set;

import org.junit.Test;
import org.snf4j.core.allocator.TestAllocator;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.pool.DefaultSelectorLoopPool;

import com.sun.nio.sctp.SctpChannel;

public class SctpSessionTest extends SctpTest {	
	
	@Test
	public void testGetParent() throws Exception {
		assumeSupported();
		startClientServer();
		assertNull(c.session.getParent());
		assertNull(s.session.getParent());
	}
	
	@Test
	public void testClose() throws Exception {
		assumeSupported();
		startClientServer();
		SctpSession session = c.session;

		//when suspended
		session.suspendWrite();
		session.writenf(nopb("123456"), info(0));
		session.writenf(nopb("7890"), info(0));
		session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getTrace());
		assertEquals("SCL|SEN|", s.getTrace());
		c.stop(TIMEOUT);
		
		//close in loop
		c = new SctpClient(PORT);
		c.start();
		waitForReady(TIMEOUT);
		session = c.session;
		session.writenf(new Packet(PacketType.WRITE_AND_CLOSE).toBytes(), info(0));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|DR|WRITE_AND_CLOSE_RESPONSE()|SCL|SEN|", c.getTrace());
		assertEquals("DR|WRITE_AND_CLOSE()|DS|SCL|SEN|", s.getTrace());
		c.stop(TIMEOUT);
		
		//close outside loop
		c = new SctpClient(PORT);
		c.start();
		waitForReady(TIMEOUT);
		session = c.session;
		sleepLoop(c.loop, 100);
		session.writenf(nopb("12345"), info(0));
		session.close();
		session.writenf(nopb("67"), info(0));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getTrace());
		assertEquals("DR|NOP(12345)|SCL|SEN|", s.getTrace());
	}
	
	@Test
	public void testQuickClose() throws Exception {
		assumeSupported();
		startClientServer();
		SctpSession session = c.session;

		//when suspended
		session.suspendWrite();
		session.writenf(nopb("123456"), info(0));
		session.writenf(nopb("7890"), info(0));
		session.quickClose();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getTrace());
		assertEquals("SCL|SEN|", s.getTrace());
		c.stop(TIMEOUT);
		
		//close in loop
		c = new SctpClient(PORT);
		c.start();
		waitForReady(TIMEOUT);
		session = c.session;
		session.writenf(new Packet(PacketType.WRITE_AND_QUICK_CLOSE).toBytes(), info(0));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getTrace());
		assertEquals("DR|WRITE_AND_QUICK_CLOSE()|SCL|SEN|", s.getTrace());
		c.stop(TIMEOUT);
		
		//close outside loop
		c = new SctpClient(PORT);
		c.start();
		waitForReady(TIMEOUT);
		session = c.session;
		sleepLoop(c.loop, 100);
		session.writenf(nopb("12345"), info(0));
		session.quickClose();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getTrace());
		assertEquals("SCL|SEN|", s.getTrace());
	}	
	
	void assertIn(Set<SocketAddress> addrs, SocketAddress addr) {
		for (SocketAddress a: addrs) {
			if (a.equals(addr)) {
				return;
			}
		}
		fail();
	}
	
	@Test
	public void testAddresses() throws Exception {
		assumeSupported();
		
		//find local address
		startClientServer();
		Set<SocketAddress> addrs = c.session.getLocalAddresses();
		InetAddress localhost = address(10).getAddress();
		InetAddress localIp = null;
		for (SocketAddress a: addrs) {
			if (a instanceof InetSocketAddress) {
				InetSocketAddress ia = (InetSocketAddress) a;
				
				if (!ia.getAddress().equals(localhost)) {
					localIp = ia.getAddress();
					break;
				}
			}
		}
		c.stop(TIMEOUT);
		
		//one address
		c = new SctpClient(PORT);
		c.localAddresses.add(address(0));
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		
		addrs = c.session.getLocalAddresses();
		assertEquals(1, addrs.size());
		assertEquals(addrs.iterator().next(), c.session.getLocalAddress());
		assertIn(((SctpChannel)c.session.channel).getAllLocalAddresses(), c.session.getLocalAddress());
		addrs = s.session.getRemoteAddresses();
		assertEquals(1, addrs.size());
		assertEquals(addrs.iterator().next(), s.session.getRemoteAddress());
		assertIn(((SctpChannel)s.session.channel).getRemoteAddresses(), s.session.getRemoteAddress());
		c.stop(TIMEOUT);
		
		//two addresses
		if (localIp != null) {
			InetSocketAddress ia = address(0);
			c = new SctpClient(PORT);
			c.localAddresses.add(address(0));
			c.localAddresses.add(address(localIp, ia.getPort()));
			c.start();
			c.waitForSessionReady(TIMEOUT);
			s.waitForSessionReady(TIMEOUT);
			addrs = c.session.getLocalAddresses();
			assertEquals(2, addrs.size());
			assertIn(addrs, c.session.getLocalAddress());
			addrs = s.session.getRemoteAddresses();
			assertEquals(2, addrs.size());
			assertIn(addrs, s.session.getRemoteAddress());
		}
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		
		assertEquals(0, c.session.getLocalAddresses().size());
		assertEquals(0, c.session.getRemoteAddresses().size());
		assertNull(c.session.getLocalAddress());
		assertNull(c.session.getRemoteAddress());
		
		c.session.channel = null;
		assertEquals(0, c.session.getLocalAddresses().size());
		assertEquals(0, c.session.getRemoteAddresses().size());
		assertNull(c.session.getLocalAddress());
		assertNull(c.session.getRemoteAddress());
		
		TestSctpChannel tc = new TestSctpChannel();
		tc.localAddressesException = new IOException();
		tc.remoteAddressesException = new IOException();
		c.session.channel = tc;
		assertEquals(0, c.session.getLocalAddresses().size());
		assertEquals(0, c.session.getRemoteAddresses().size());
		assertNull(c.session.getLocalAddress());
		assertNull(c.session.getRemoteAddress());
		
		c.session.channel = new TestSocketChannel();
		assertEquals(0, c.session.getLocalAddresses().size());
		assertEquals(0, c.session.getRemoteAddresses().size());
		assertNull(c.session.getLocalAddress());
		assertNull(c.session.getRemoteAddress());
	}
	
	@Test
	public void testWriteFuture() throws Exception { 
		assumeSupported();

		startClientServer();
		SctpSession session = c.session;
		
		IFuture<Void> f = session.write(nopb("123"), info(0));
		f.sync(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(123)|", s.getTrace());
		
		session.suspendWrite();
		f = session.write(nopb(""), info(0));
		waitFor(1000);
		assertFalse(f.isDone());
		session.resumeWrite();
		waitFor(50);
		assertTrue(f.isDone());
		f.sync(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP()|", s.getTrace());
		
		sleepLoop(c.loop, 200);
		f = session.write(nopb("XX"), info(0));
		f.sync(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(XX)|", s.getTrace());
	}

	@Test
	public void testFragmentationWithRelease() throws Exception {
		assumeSupported();
		
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.minInBufferCapacity = 1024;
		s.maxInBufferCapacity = 1024*8;
		s.allocator = new TestAllocator(false, true);
		s.start();
		c.start();
		waitForReady(TIMEOUT);
		SctpSession session = c.session;
		c.traceDataLength = true;
		s.traceDataLength = true;
		setAllocator(s.allocator);
		assertAllocatorDeltas(1,0,1);
		ByteBuffer in = allocated(0);
		assertTrue(getIn(s) == in);
		assertEquals(1024, getIn(s).capacity());
		
		Packet p = nop("ST", "EN", '1', 1024-7);
		byte[] msg = p.toBytes();
		session.writenf(msg, info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS(1024)|", c.getTrace());
		assertEquals("DR(1024)|NOP(" + p.payload + ")|", s.getTrace());
		assertNull(getFragment(s));
		assertTrue(getIn(s) == in);
		assertEquals(1024, getIn(s).capacity());
		assertAllocatorDeltas(0,0,1);
		
		p = nop("ST", "EN", '1', 1024-6);
		msg = p.toBytes();
		session.writenf(msg, info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS(1025)|", c.getTrace());
		assertEquals("DR(1024)|DR(1)|NOP(" + p.payload + ")|", s.getTrace());
		assertNull(getFragment(s));
		assertTrue(getIn(s) != in);
		in = getIn(s);
		assertEquals(2048, in.capacity());
		assertAllocatorDeltas(1,1,1);

		p = nop("ST", "EN", '1', 4*1024-6);
		msg = p.toBytes();
		session.writenf(msg, info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS(4097)|", c.getTrace());
		assertEquals("DR(2048)|DR(2048)|DR(1)|NOP(" + p.payload + ")|", s.getTrace());		
		assertNull(getFragment(s));
		assertTrue(getIn(s) != in);
		in = getIn(s);
		assertEquals(8192, in.capacity());
		assertAllocatorDeltas(1,1,1);

		p = nop("ST", "EN", '1', 4*1024-6);
		msg = p.toBytes();
		session.writenf(msg, info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS(4097)|", c.getTrace());
		assertEquals("DR(4097)|NOP(" + p.payload + ")|", s.getTrace());		
		assertNull(getFragment(s));
		assertTrue(getIn(s) == in);
		assertEquals(8192, in.capacity());
		assertAllocatorDeltas(0,0,1);
		
		//max capacity reached
		p = nop("ST", "EN", '1', 8*1024-7);
		msg = p.toBytes();
		session.writenf(msg, info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS(8192)|", c.getTrace());
		assertEquals("DR(8192)|NOP(" + p.payload + ")|", s.getTrace());
		assertNull(getFragment(s));
		assertTrue(getIn(s) == in);
		assertEquals(8192, in.capacity());
		assertAllocatorDeltas(0,0,1);
		p = nop("ST", "EN", '1', 8*1024-6);
		msg = p.toBytes();
		session.writenf(msg, info(0));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS(8193)|SCL|SEN|", c.getTrace());
		assertEquals("DR(8192)|DR(1)|EXC|SCL|SEN|", s.getTrace());
		assertAllocatorDeltas(1,2,0);
		assertNull(getFragment(s));
		assertNull(getIn(s));
	}
	
	@Test
	public void testFragmentationWithOptimize() throws Exception {
		assumeSupported();
		
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.minInBufferCapacity = 1024;
		s.maxInBufferCapacity = 1024*8;
		s.allocator = new TestAllocator(false, true);
		s.optimizeCopying = true;
		s.start();
		c.start();
		waitForReady(TIMEOUT);
		SctpSession session = c.session;
		c.traceDataLength = true;
		s.traceDataLength = true;
		setAllocator(s.allocator);
		assertAllocatorDeltas(1,1,0);
		
		Packet p = nop("ST", "EN", '1', 1024-7);
		byte[] msg = p.toBytes();
		session.writenf(msg, info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS(1024)|", c.getTrace());
		assertEquals("DR(1024)|BUF|NOP(" + p.payload + ")|", s.getTrace());
		assertNull(getFragment(s));
		assertNull(getIn(s));
		releaseReadBuffer(s);
		assertAllocatorDeltas(1,1,0);

		p = nop("ST", "EN", '1', 1024-6);
		msg = p.toBytes();
		session.writenf(msg, info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS(1025)|", c.getTrace());
		assertEquals("DR(1024)|DR(1)|BUF|NOP(" + p.payload + ")|", s.getTrace());
		assertNull(getFragment(s));
		assertNull(getIn(s));
		releaseReadBuffer(s);
		assertAllocatorDeltas(2,2,0);

		p = nop("ST", "EN", '1', 4*1024-6);
		msg = p.toBytes();
		session.writenf(msg, info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS(4097)|", c.getTrace());
		assertEquals("DR(2048)|DR(2048)|DR(1)|BUF|NOP(" + p.payload + ")|", s.getTrace());		
		assertNull(getFragment(s));
		assertNull(getIn(s));
		releaseReadBuffer(s);
		assertAllocatorDeltas(3,3,0);

		p = nop("ST", "EN", '1', 4*1024-6);
		msg = p.toBytes();
		session.writenf(msg, info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS(4097)|", c.getTrace());
		assertEquals("DR(4097)|BUF|NOP(" + p.payload + ")|", s.getTrace());		
		assertNull(getFragment(s));
		assertNull(getIn(s));
		releaseReadBuffer(s);
		assertAllocatorDeltas(1,1,0);
		
		//max capacity reached
		p = nop("ST", "EN", '1', 8*1024-7);
		msg = p.toBytes();
		session.writenf(msg, info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS(8192)|", c.getTrace());
		assertEquals("DR(8192)|BUF|NOP(" + p.payload + ")|", s.getTrace());
		assertNull(getFragment(s));
		assertNull(getIn(s));
		releaseReadBuffer(s);
		assertAllocatorDeltas(1,1,0);
		p = nop("ST", "EN", '1', 8*1024-6);
		msg = p.toBytes();
		session.writenf(msg, info(0));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS(8193)|SCL|SEN|", c.getTrace());
		assertEquals("DR(8192)|DR(1)|EXC|SCL|SEN|", s.getTrace());
		assertAllocatorDeltas(2,2,0);
		assertNull(getFragment(s));
		assertNull(getIn(s));
	}
	
	@Test
	public void testFragmentation() throws Exception {
		assumeSupported();
		
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.minInBufferCapacity = 1024;
		s.maxInBufferCapacity = 1024*8;
		s.allocator = new TestAllocator(false, false);
		s.start();
		c.start();
		waitForReady(TIMEOUT);
		SctpSession session = c.session;
		c.traceDataLength = true;
		s.traceDataLength = true;
		setAllocator(s.allocator);
		assertAllocator(1,0,1);

		Packet p = nop("ST", "EN", '1', 1024-7);
		byte[] msg = p.toBytes();
		session.writenf(msg, info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS(1024)|", c.getTrace());
		assertEquals("DR(1024)|NOP(" + p.payload + ")|", s.getTrace());
		assertNull(getFragment(s));
		assertEquals(1024, getIn(s).capacity());
		assertAllocator(1,0,1);
		
		p = nop("ST", "EN", '1', 1024-6);
		msg = p.toBytes();
		session.writenf(msg, info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS(1025)|", c.getTrace());
		assertEquals("DR(1024)|DR(1)|NOP(" + p.payload + ")|", s.getTrace());
		assertEquals(1024, getFragment(s).capacity());
		assertEquals(2048, getIn(s).capacity());
		assertAllocator(2,0,2);
		
		p = nop("ST", "EN", '1', 4*1024-6);
		msg = p.toBytes();
		session.writenf(msg, info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS(4097)|", c.getTrace());
		assertEquals("DR(2048)|DR(2048)|DR(1)|NOP(" + p.payload + ")|", s.getTrace());		
		assertEquals(2048, getFragment(s).capacity());
		assertEquals(2048*4, getIn(s).capacity());
		assertAllocator(2,0,2);

		p = nop("ST", "EN", '1', 4*1024-6);
		msg = p.toBytes();
		session.writenf(msg, info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS(4097)|", c.getTrace());
		assertEquals("DR(4097)|NOP(" + p.payload + ")|", s.getTrace());		
		assertEquals(2048, getFragment(s).capacity());
		assertEquals(2048*4, getIn(s).capacity());
		assertAllocator(2,0,2);
		
		setIn(s, s.allocator.allocate(1024));
		assertAllocator(3,0,3);
		p = nop("ST", "EN", '1', 1024-6);
		msg = p.toBytes();
		session.writenf(msg, info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS(1025)|", c.getTrace());
		assertEquals("DR(1024)|DR(1)|NOP(" + p.payload + ")|", s.getTrace());
		assertEquals(1024, getFragment(s).capacity());
		assertEquals(2048, getIn(s).capacity());
		assertAllocator(3,0,3);
		
		//max capacity reached
		p = nop("ST", "EN", '1', 8*1024-7);
		msg = p.toBytes();
		session.writenf(msg, info(0));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DS(8192)|", c.getTrace());
		assertEquals("DR(2048)|DR(2048)|DR(2048)|DR(2048)|NOP(" + p.payload + ")|", s.getTrace());
		assertEquals(2048, getFragment(s).capacity());
		assertEquals(8192, getIn(s).capacity());
		assertAllocator(3,0,3);
		p = nop("ST", "EN", '1', 8*1024-6);
		msg = p.toBytes();
		session.writenf(msg, info(0));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS(8193)|SCL|SEN|", c.getTrace());
		assertEquals("DR(8192)|DR(1)|EXC|SCL|SEN|", s.getTrace());
		assertAllocator(3,0,3);
		assertNull(getFragment(s));
		assertNull(getIn(s));
	}

	private void testCloseInSessionCreatedEvent(StoppingType type) throws Exception {
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		c.closeInEvent = EventType.SESSION_CREATED;
		c.closeType = type;
		s.start();
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SEN|", c.getTrace());
		assertEquals("SCR|SOP|RDY|SCL|SEN|", s.getTrace());
		assertEquals(ClosingState.FINISHED, c.session.closing);
		s.stop(TIMEOUT);
		c.stop(TIMEOUT);
		
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.closeInEvent = EventType.SESSION_CREATED;
		s.closeType = type;
		s.start();
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SEN|", s.getTrace());
		assertEquals("SCR|SOP|RDY|SCL|SEN|", c.getTrace());
		assertEquals(ClosingState.FINISHED, s.session.closing);
		s.stop(TIMEOUT);
		c.stop(TIMEOUT);
		
		s = new SctpServer(PORT);
		s.closeInEvent = EventType.SESSION_CREATED;
		s.closeType = type;
		s.start();
		TestSelectorPool pool = new TestSelectorPool();
		s.loop.setPool(pool);
		pool.getException = true;
		c = new SctpClient(PORT);
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|SEN|", s.getTrace());
		assertEquals("SCR|SOP|RDY|SCL|SEN|", c.getTrace());
		assertEquals(ClosingState.FINISHED, s.session.closing);
		s.stop(TIMEOUT);
		c.stop(TIMEOUT);
	}
	
	@Test
	public void testCloseInSessionCreatedEvent() throws Exception {
		testCloseInSessionCreatedEvent(StoppingType.GENTLE);
		testCloseInSessionCreatedEvent(StoppingType.QUICK);
		testCloseInSessionCreatedEvent(StoppingType.DIRTY);
	}
	
	private void testCloseInSessionOpenedEvent(StoppingType type) throws Exception {
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		c.closeInEvent = EventType.SESSION_OPENED;
		c.closeType = type;
		s.start();
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|SCL|SEN|", c.getTrace());
		assertEquals("SCR|SOP|RDY|SCL|SEN|", s.getTrace());
		assertEquals(ClosingState.FINISHED, c.session.closing);
		s.stop(TIMEOUT);
		c.stop(TIMEOUT);

		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.closeInEvent = EventType.SESSION_OPENED;
		s.closeType = type;
		s.start();
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|SCL|SEN|", s.getTrace());
		assertEquals("SCR|SOP|RDY|SCL|SEN|", c.getTrace());
		assertEquals(ClosingState.FINISHED, s.session.closing);
		s.stop(TIMEOUT);
		c.stop(TIMEOUT);
	
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.closeInEvent = EventType.SESSION_OPENED;
		s.closeType = type;
		s.start();
		s.loop.setPool(new DefaultSelectorLoopPool(2));
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|SCL|SEN|", s.getTrace());
		assertEquals("SCR|SOP|RDY|SCL|SEN|", c.getTrace());
		assertEquals(ClosingState.FINISHED, s.session.closing);
		s.stop(TIMEOUT);
		c.stop(TIMEOUT);
	}
	
	@Test
	public void testCloseInSessionOpenedEvent() throws Exception {
		testCloseInSessionOpenedEvent(StoppingType.GENTLE);
		testCloseInSessionOpenedEvent(StoppingType.QUICK);
		testCloseInSessionOpenedEvent(StoppingType.DIRTY);
	}	
	
	private void testCloseInSessionClosedOrEndingEvent(StoppingType type, EventType event) throws Exception {
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		c.closeInEvent = event;
		c.closeType = type;
		s.start();
		c.start();
		waitForReady(TIMEOUT);
		c.session.close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getTrace());
		assertEquals("SCL|SEN|", s.getTrace());
		assertEquals(ClosingState.FINISHED, c.session.closing);
		s.stop(TIMEOUT);
		c.stop(TIMEOUT);
		
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		c.closeInEvent = event;
		c.closeType = type;
		s.start();
		c.start();
		waitForReady(TIMEOUT);
		s.session.close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getTrace());
		assertEquals("SCL|SEN|", s.getTrace());
		assertEquals(ClosingState.FINISHED, c.session.closing);
		s.stop(TIMEOUT);
		c.stop(TIMEOUT);
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
	
	@Test
	public void testPostEnding() throws Exception {
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.start();
		c.allocator = new TestAllocator(false, false);
		c.start();
		waitForReady(TIMEOUT);
		setAllocator(c.allocator);
		assertAllocator(1,0,1);
		
		c.session.suspendWrite();
		c.session.write(nopb("12345"), info(0));
		c.session.quickClose();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertAllocator(2,0,2);	
		assertNull(getIn(c));
		assertNull(getOut(c));
		c.stop(TIMEOUT);
		clearTraces();
		
		//release only
		c = new SctpClient(PORT);
		c.allocator = new TestAllocator(false, true);
		c.start();
		waitForReady(TIMEOUT);
		setAllocator(c.allocator);
		assertAllocator(1,0,1);

		c.session.suspendWrite();
		c.session.write(nopb("12345"), info(0));
		c.session.quickClose();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertAllocator(2,2,0);	
		assertNull(getIn(c));
		assertNull(getOut(c));
		c.stop(TIMEOUT);
		clearTraces();

		//optimize
		c = new SctpClient(PORT);
		c.allocator = new TestAllocator(false, true);
		c.optimizeCopying = true;
		c.start();
		waitForReady(TIMEOUT);
		setAllocator(c.allocator);
		assertAllocator(1,1,0);

		c.session.suspendWrite();
		c.session.write(nopb("12345"), info(0));
		c.session.quickClose();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertAllocator(1,1,0);	
		assertNull(getIn(c));
		assertNull(getOut(c));
		c.stop(TIMEOUT);
		clearTraces();
		
	}
}
