/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyBoundException;
import java.util.Set;

import org.junit.Test;
import org.snf4j.core.allocator.TestAllocator;
import org.snf4j.core.codec.IEventDrivenCodec;
import org.snf4j.core.codec.bytes.ArrayToBufferDecoder;
import org.snf4j.core.codec.bytes.ArrayToBufferEncoder;
import org.snf4j.core.codec.bytes.BufferToArrayDecoder;
import org.snf4j.core.codec.bytes.BufferToArrayEncoder;
import org.snf4j.core.future.FailedFuture;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.future.SuccessfulFuture;
import org.snf4j.core.future.TaskFuture;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.pool.DefaultSelectorLoopPool;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.IllegalSessionStateException;

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
	public void testGetAssociation() throws Exception {
		assumeSupported();
		InternalSctpSession session = new SctpSession(new TestSctpHandler());
		assertNull(session.getAssociation());
		
		startClientServer();
		session = c.session;
		assertNotNull(session.getAssociation());
		session.close();
		session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertNull(session.getAssociation());
		
		TestSctpChannel tsc = new TestSctpChannel();
		tsc.associationException = new IOException();
		session.channel = tsc;
		assertNull(session.getAssociation());
		tsc.close();
	}
	
	@Test
	public void testBindUnbindAddress() throws Exception {
		assumeSupported();
		startClientServer();
		InternalSctpSession session = c.session;
		InetAddress addr0 = address(0).getAddress();
		InetAddress addr1 = null;
		
		for (InetAddress a: addresses(session)) {
			if (!a.equals(addr0)) {
				addr1 = a;
				break;
			}
		}
		assertNotNull(addr1);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		c.localAddresses.add(address(addr0,0));
		s.start();
		c.start();
		waitForReady(TIMEOUT);
		session = c.session;
		assertEquals(1, session.getLocalAddresses().size());
		session.bindAddress(addr1).sync(TIMEOUT);
		waitFor(50);
		assertEquals(2, session.getLocalAddresses().size());
		session.unbindAddress(addr1).sync(TIMEOUT);
		waitFor(50);
		assertEquals(1, session.getLocalAddresses().size());
		
		IFuture<Void> f = session.bindAddress(addr0);
		f.await(TIMEOUT);
		assertTrue(f.isFailed());
		assertTrue(f.cause() instanceof AlreadyBoundException);
		assertTrue(f.getClass() == TaskFuture.class);
		f = session.unbindAddress(addr0);
		f.await(TIMEOUT);
		assertTrue(f.isFailed());
		assertNotNull(f.cause());
		
		BindUnbindTask task = new BindUnbindTask(session, addr1, true);
		c.loop.execute(task).sync(TIMEOUT);
		assertTrue(task.future.isSuccessful());
		waitFor(50);
		assertEquals(2, session.getLocalAddresses().size());
		assertTrue(task.future.getClass() == SuccessfulFuture.class);
		task = new BindUnbindTask(session, addr1, false);		
		c.loop.execute(task).sync(TIMEOUT);
		assertTrue(task.future.isSuccessful());
		waitFor(50);
		assertEquals(1, session.getLocalAddresses().size());
		assertTrue(task.future.getClass() == SuccessfulFuture.class);
		
		task = new BindUnbindTask(session, addr0, true);	
		c.loop.execute(task).sync(TIMEOUT);
		assertTrue(task.future.isFailed());
		assertTrue(task.future.getClass() == FailedFuture.class);
		assertTrue(task.future.cause() instanceof AlreadyBoundException);
		task = new BindUnbindTask(session, addr0, false);	
		c.loop.execute(task).sync(TIMEOUT);
		assertTrue(task.future.isFailed());
		assertTrue(task.future.getClass() == FailedFuture.class);
		assertNotNull(task.future.cause());
		
		session.close();
		c.waitForSessionEnding(TIMEOUT);
		session.channel = null;
		assertTrue(session.bindAddress(addr1).isCancelled());
		session.loop = null;
		assertTrue(session.bindAddress(addr1).isCancelled());
	}
	
	class BindUnbindTask implements Runnable {
		
		InternalSctpSession session;
		
		boolean bind;
		
		volatile IFuture<Void> future;
		
		InetAddress address;
		
		BindUnbindTask(InternalSctpSession session, InetAddress address, boolean bind) {
			this.session = session;
			this.bind = bind;
			this.address = address;
		}
		
		@Override
		public void run() {
			future = bind ? session.bindAddress(address) : session.unbindAddress(address);
		}
	}
	
	@Test
	public void testClose() throws Exception {
		assumeSupported();
		startClientServer();
		InternalSctpSession session = c.session;

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
		InternalSctpSession session = c.session;

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
		InternalSctpSession session = c.session;
		
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
		InternalSctpSession session = c.session;
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
		InternalSctpSession session = c.session;
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
		InternalSctpSession session = c.session;
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
		assumeSupported();

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
		assumeSupported();

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
		assumeSupported();

		testCloseInSessionClosedOrEndingEvent(StoppingType.GENTLE, EventType.SESSION_CLOSED);
		testCloseInSessionClosedOrEndingEvent(StoppingType.QUICK, EventType.SESSION_CLOSED);
		testCloseInSessionClosedOrEndingEvent(StoppingType.DIRTY, EventType.SESSION_CLOSED);
	}
	
	@Test
	public void testCloseInSessionEndingEvent() throws Exception {
		assumeSupported();

		testCloseInSessionClosedOrEndingEvent(StoppingType.GENTLE, EventType.SESSION_ENDING);
		testCloseInSessionClosedOrEndingEvent(StoppingType.QUICK, EventType.SESSION_ENDING);
		testCloseInSessionClosedOrEndingEvent(StoppingType.DIRTY, EventType.SESSION_ENDING);
	}	
	
	@Test
	public void testPostEnding() throws Exception {
		assumeSupported();
		
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
		assertAllocator(2,2,0);	
		assertNull(getIn(c));
		assertNull(getOut(c));
		c.stop(TIMEOUT);
		clearTraces();	
	}
	
	@Test
	public void testDefaultMessageInfo() throws Exception {
		assumeSupported();
		
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		c.defaultSctpStreamNumber = 3;
		c.defaultSctpPayloadProtocolID = 11;
		c.defaultSctpUnorderedFlag = true;
		s.start();
		c.start();
		waitForReady(TIMEOUT);
		
		c.session.write(nopb("10"));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(10)[3p11u]|", s.getTrace());	
	}
	
	void assertIllegalSessionState(String method, Class<?>... args) throws Exception {
		Method m = SctpSession.class.getDeclaredMethod(method, args);
		Object[] vals = new Object[args.length];
		
		for (int i=0; i<vals.length; ++i) {
			Class<?> clazz = args[i];
			
			if (clazz == int.class) {
				vals[i] = 1;
			}
			else if (clazz == byte[].class) {
				vals[i] = new byte[10];
			}
			else if (clazz == ByteBuffer.class) {
				vals[i] = ByteBuffer.allocate(10);
			}
		}
		try {
			m.invoke(c.session, vals);
			fail();
		}
		catch (InvocationTargetException e) {
			assertTrue(e.getCause().getClass() == IllegalSessionStateException.class);
		}
	}
	
	void assertNullSession(Class<?>... args) throws Exception {
		Class<?>[] args2 = new Class[args.length+1];
		
		System.arraycopy(args, 0, args2, 0, args.length);
		args2[args.length] = ImmutableSctpMessageInfo.class;
		Method m2 = SctpSession.class.getDeclaredMethod("write", args2);
		Method mnf2 = SctpSession.class.getDeclaredMethod("writenf", args2);
		Method m = SctpSession.class.getDeclaredMethod("write", args);
		Method mnf = SctpSession.class.getDeclaredMethod("writenf", args);
		
		Object[] vals = new Object[args.length];
		Object[] vals2 = new Object[args2.length];
		vals2[args.length] = ImmutableSctpMessageInfo.create(10);
		
		for (int i=1; i<vals.length; ++i) {
			vals[i] = 1;
		}
		for (int i=1; i<vals2.length-1; ++i) {
			vals2[i] = 1;
		}
		
		try {
			m2.invoke(c.session, vals2);
			fail();
		}
		catch (InvocationTargetException e) {
			assertTrue(e.getCause().getClass() == NullPointerException.class);
		}
		try {
			mnf2.invoke(c.session, vals2);
			fail();
		}
		catch (InvocationTargetException e) {
			assertTrue(e.getCause().getClass() == NullPointerException.class);
		}
		
		try {
			m.invoke(c.session, vals);
			fail();
		}
		catch (InvocationTargetException e) {
			assertTrue(e.getCause().getClass() == NullPointerException.class);
		}
		try {
			mnf.invoke(c.session, vals);
			fail();
		}
		catch (InvocationTargetException e) {
			assertTrue(e.getCause().getClass() == NullPointerException.class);
		}	
	}
	
	void assertWrite(String expectedTrace) throws Exception {
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals(expectedTrace, s.getTrace());
	}
	
	void assertOutOfBoundException(InternalSctpSession session, byte[] data, int off, int len) {
		try {
			session.write(data, off, len);
			fail();
		} catch (IndexOutOfBoundsException e) {}
		try {
			session.writenf(data, off, len);
			fail();
		} catch (IndexOutOfBoundsException e) {}
	}

	void assertOutOfBoundException(InternalSctpSession session, ByteBuffer data, int len) {
		try {
			session.write(data, len);
			fail();
		} catch (IndexOutOfBoundsException e) {}
		try {
			session.writenf(data, len);
			fail();
		} catch (IndexOutOfBoundsException e) {}
	}
	
	void assertUnexpectedObject(InternalSctpSession session) {
		try {
			session.write("Text");
			fail();
		} catch (IllegalArgumentException e) {}
		try {
			session.writenf("Text");
			fail();
		} catch (IllegalArgumentException e) {}
	}
	
	@Test
	public void testWriteMethods() throws Exception {
		assumeSupported();

		startClientServer();
		
		InternalSctpSession session = c.session;
		byte[] emptyb = new byte[0];
		ByteBuffer emptybb = ByteBuffer.wrap(emptyb);
		byte[] nemptyb = new byte[10];
		ByteBuffer nemptybb = ByteBuffer.wrap(nemptyb);
		
		//session is null
		assertNullSession(byte[].class);
		assertNullSession(byte[].class, int.class, int.class);
		assertNullSession(ByteBuffer.class);
		assertNullSession(ByteBuffer.class, int.class);
		assertNullSession(Object.class);
		
		//index out of bounds
		assertOutOfBoundException(session, new byte[10], -1, 4);
		assertOutOfBoundException(session, new byte[10], 10, 1);
		assertOutOfBoundException(session, new byte[10], 0, -1);
		assertOutOfBoundException(session, new byte[10], 5, 6);
		assertOutOfBoundException(session, new byte[10], 0x7fffffff, 1);
		assertOutOfBoundException(session, ByteBuffer.wrap(new byte[10]), 11);
		assertOutOfBoundException(session, ByteBuffer.wrap(new byte[10]), -1);
		
		//length = 0
		assertTrue(session.write(emptyb).isSuccessful());
		session.writenf(emptyb);
		assertTrue(session.write(emptyb, info(4)).isSuccessful());
		session.writenf(emptyb, info(4));

		assertTrue(session.write(nemptyb,0,0).isSuccessful());
		session.writenf(nemptyb,0,0);
		assertTrue(session.write(nemptyb,0,0,info(4)).isSuccessful());
		session.writenf(nemptyb,0,0,info(4));
		
		assertTrue(session.write(emptybb).isSuccessful());
		session.writenf(emptybb);
		assertTrue(session.write(emptybb, info(4)).isSuccessful());
		session.writenf(emptybb, info(4));
		
		assertTrue(session.write(nemptybb,0).isSuccessful());
		session.writenf(nemptybb,0);
		assertTrue(session.write(nemptybb, 0, info(4)).isSuccessful());
		session.writenf(nemptybb, 0, info(4));
		
		waitFor(100);
		assertEquals("", c.getTrace());
		assertEquals("", s.getTrace());
		
		//lenght > 0
		session.write(nopb("1")).sync(TIMEOUT);
		assertWrite("DR|NOP(1)|");
		session.writenf(nopb("1nf"));
		assertWrite("DR|NOP(1nf)|");
		session.write(nopb("12"), info(4,5,true)).sync(TIMEOUT);
		assertWrite("DR|NOP(12)[4p5u]|");
		session.writenf(nopb("12nf"), info(4,5,true));
		assertWrite("DR|NOP(12nf)[4p5u]|");

		session.write(nopb("1",4,7),4,4).sync(TIMEOUT);
		assertWrite("DR|NOP(1)|");
		session.writenf(nopb("1nf",4,7),4,6);
		assertWrite("DR|NOP(1nf)|");
		session.write(nopb("12",0,8), 0, 5, info(4,5,true)).sync(TIMEOUT);
		assertWrite("DR|NOP(12)[4p5u]|");
		session.writenf(nopb("12nf",3,1), 3, 7, info(4,5,true));
		assertWrite("DR|NOP(12nf)[4p5u]|");
		
		session.write(nopbb("123")).sync(TIMEOUT);
		assertWrite("DR|NOP(123)|");
		assertEquals(0, nopbb.remaining());
		session.writenf(nopbb("123nf"));
		assertWrite("DR|NOP(123nf)|");
		assertEquals(0, nopbb.remaining());
		session.write(nopbb("1234"), info(5,6,true)).sync(TIMEOUT);
		assertWrite("DR|NOP(1234)[5p6u]|");
		assertEquals(0, nopbb.remaining());
		session.writenf(nopbb("1234nf"), info(5,6,true));
		assertWrite("DR|NOP(1234nf)[5p6u]|");
		assertEquals(0, nopbb.remaining());

		session.write(nopbb("123", 7),6).sync(TIMEOUT);
		assertWrite("DR|NOP(123)|");
		assertEquals(7, nopbb.remaining());
		session.writenf(nopbb("123nf",0),8);
		assertWrite("DR|NOP(123nf)|");
		assertEquals(0, nopbb.remaining());
		session.write(nopbb("1234",3), 7, info(5,6,true)).sync(TIMEOUT);
		assertWrite("DR|NOP(1234)[5p6u]|");
		assertEquals(3, nopbb.remaining());
		session.writenf(nopbb("1234nf",0), 9, info(5,6,true));
		assertWrite("DR|NOP(1234nf)[5p6u]|");
		assertEquals(0, nopbb.remaining());
		
		session.write((Object)nopb("1")).sync(TIMEOUT);
		assertWrite("DR|NOP(1)|");
		session.writenf((Object)nopb("1"));
		assertWrite("DR|NOP(1)|");
		session.write((Object)nopbb("12")).sync(TIMEOUT);
		assertWrite("DR|NOP(12)|");
		session.writenf((Object)nopbb("12"));
		assertWrite("DR|NOP(12)|");

		session.write((Object)nopb("1"), info(3,6)).sync(TIMEOUT);
		assertWrite("DR|NOP(1)[3p6]|");
		session.writenf((Object)nopb("1"), info(3,6));
		assertWrite("DR|NOP(1)[3p6]|");
		session.write((Object)nopbb("12"), info(3,5)).sync(TIMEOUT);
		assertWrite("DR|NOP(12)[3p5]|");
		session.writenf((Object)nopbb("12"), info(3,5));
		assertWrite("DR|NOP(12)[3p5]|");
		
		assertUnexpectedObject(session);
		
		//closing
		session.closing = ClosingState.SENDING;
		assertTrue(session.write(nopb("44")).isCancelled());
		assertTrue(session.write(nopb("44", 0, 5), 0, 5).isCancelled());
		assertTrue(session.write(nopbb("446")).isCancelled());
		assertTrue(session.write(nopbb("446", 5), 6).isCancelled());
		session.closing = ClosingState.NONE;
		
		session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		
		assertIllegalSessionState("write", byte[].class);
		assertIllegalSessionState("write", byte[].class, int.class, int.class);
		assertIllegalSessionState("write", ByteBuffer.class);
		assertIllegalSessionState("write", ByteBuffer.class, int.class);
		assertIllegalSessionState("writenf", byte[].class);
		assertIllegalSessionState("writenf", byte[].class, int.class, int.class);
		assertIllegalSessionState("writenf", ByteBuffer.class);
		assertIllegalSessionState("writenf", ByteBuffer.class, int.class);
	}
	
	@Test
	public void testWriteMethodsWithCodec() throws Exception {
		assumeSupported();
		
		TestCodec codec = new TestCodec();
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		addCodecs(c, 0,0, codec.PBE(), codec.BPE());
		addCodecs(c, 1,2, codec.PBE(), codec.BPE());
		addCodecs(c, 1,3, codec.PBE(), codec.BPE());
		addCodecs(c, 5,0, codec.PBE(), codec.BPE());
		s.start();
		c.start();
		waitForReady(TIMEOUT);
		InternalSctpSession session = c.session;
		
		session.write(nopb("11")).sync(TIMEOUT);
		assertWrite("DR|NOP(11e)|");
		session.write(nopb("11"), info(1,2,true)).sync(TIMEOUT);
		assertWrite("DR|NOP(11e)[1p2u]|");
		
		session.writenf(nopb("12"));
		assertWrite("DR|NOP(12e)|");
		session.writenf(nopb("12"), info(1,2,true));
		assertWrite("DR|NOP(12e)[1p2u]|");
		
		session.write(nopb("1134", 4, 5),4,7).sync(TIMEOUT);
		assertWrite("DR|NOP(1134e)|");	
		session.write(nopb("1134", 4, 5),4,7, info(1,3)).sync(TIMEOUT);
		assertWrite("DR|NOP(1134e)[1p3]|");	

		session.writenf(nopb("1135", 4, 5),4,7);
		assertWrite("DR|NOP(1135e)|");	
		session.writenf(nopb("1135", 4, 5),4,7, info(1,3));
		assertWrite("DR|NOP(1135e)[1p3]|");	
		
		c.session.getCodecPipeline().addFirst("0", new ArrayToBufferEncoder());
		
		session.write(nopbb("11")).sync(TIMEOUT);
		assertWrite("DR|NOP(11e)|");
		session.write(nopbb("11"), info(1,2,true)).sync(TIMEOUT);
		assertWrite("DR|NOP(11e)[1p2u]|");
		
		session.writenf(nopbb("11"));
		assertWrite("DR|NOP(11e)|");
		session.writenf(nopbb("11"), info(1,2,true));
		assertWrite("DR|NOP(11e)[1p2u]|");

		session.write(nopbb("11",5),5).sync(TIMEOUT);
		assertWrite("DR|NOP(11e)|");
		session.write(nopbb("11",6),5, info(1,2,true)).sync(TIMEOUT);
		assertWrite("DR|NOP(11e)[1p2u]|");
		
		session.writenf(nopbb("11",5),5);
		assertWrite("DR|NOP(11e)|");
		session.writenf(nopbb("11",6),5, info(1,2,true));
		assertWrite("DR|NOP(11e)[1p2u]|");
		
		assertNull(session.getEncodeTaskWriter().write((SocketAddress)null, nopb("1233"), true));
		assertNull(session.getEncodeTaskWriter().write((SocketAddress)null, nopbb("1233"), true));
		
		session.write(nop("456")).sync(TIMEOUT);
		assertWrite("DR|NOP(456e)|");
		session.write(nop("456"), info(5)).sync(TIMEOUT);
		assertWrite("DR|NOP(456e)[5]|");

		session.writenf(nop("456"));
		assertWrite("DR|NOP(456e)|");
		session.writenf(nop("456"), info(5));
		assertWrite("DR|NOP(456e)[5]|");
		stopClientAndClearTraces(TIMEOUT);
		
		c = new SctpClient(PORT);
		addCodecs(c, 0,0, codec.PBE(), codec.BPE(), new BufferToArrayEncoder(true));
		c.allocator = new TestAllocator(false, true);
		c.optimizeCopying = true;
		c.start();
		waitForReady(TIMEOUT);
		session = c.session;
		setAllocator(c.allocator);
		assertAllocator(1,1,0);
		
		session.write(nopbba("123456"));
		assertWrite("DR|NOP(123456e)|");
		assertAllocator(2,2,0);
		
		c.getCodec(0, 0).getPipeline().replace("E1", "E1", codec.PBBE());
		session.write(nopbba("123456"));
		assertWrite("DR|NOP(123456e2)|");
		assertAllocator(3,4,0);
		session.writenf(nopbba("12345"));
		assertWrite("DR|NOP(12345e2)|");
		assertAllocator(4,6,0);
	}	
	
	@Test
	public void testReadingWithCodec() throws Exception {
		assumeSupported();
		
		TestCodec codec = new TestCodec();
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		addCodecs(s,0,0,codec.BPD(), codec.PBD());
		s.start();
		c.start();
		waitForReady(TIMEOUT);
		InternalSctpSession session = c.session;
		
		session.write(nopb("1111"));
		assertWrite("DR|NOP(1111d)|");
		clearTraces();
		codec.discardingDecode = true;
		session.write(nopb("11112"));
		waitFor(100);
		codec.discardingDecode = false;
		assertEquals("DR|", s.getTrace());
		assertEquals("DS|", c.getTrace());
		session.write(nopb("1122"), info(3));
		assertWrite("DR|NOP(1122)[3]|");
		codec.duplicatingDecode = true;
		session.write(nopb("1111"));
		s.waitForDataRead(TIMEOUT);
		waitFor(100);
		assertEquals("DR|NOP(1111d)|NOP(1111d)|NOP(1111d)|", s.getTrace());
		codec.duplicatingDecode = false;
		
		s.getCodec(0,0).getPipeline().add("3", codec.BPD());
		
		s.resetLocks();
		session.write(nopb("123"));
		assertWrite("DR|M(NOP[123d])|");
		codec.duplicatingDecode = true;
		session.write(nopb("1111"));
		s.waitForDataRead(TIMEOUT);
		waitFor(100);
		assertEquals("DR|M(NOP[1111d])|M(NOP[1111d])|M(NOP[1111d])|", s.getTrace());
		codec.duplicatingDecode = false;
		clearTraces();
		
		codec.decodeException = new Exception("E1");
		session.writenf(nopb("456"));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getTrace());
		assertEquals("DR|EXC|SCL|SEN|", s.getTrace());
		c.stop(TIMEOUT);
		codec.decodeException = null;
		
		s.allocator = new TestAllocator(false, true);
		s.optimizeCopying = true;
		addCodecs(s,0,0,codec.BPD(), codec.PBD());
		c = new SctpClient(PORT);
		c.start();
		waitForReady(TIMEOUT);
		session = c.session;
		
		session.write(nopb("1111"));
		assertWrite("DR|NOP(1111d)|");
		clearTraces();
		codec.discardingDecode = true;
		session.write(nopb("11112"));
		waitFor(100);
		codec.discardingDecode = false;
		assertEquals("DR|", s.getTrace());
		assertEquals("DS|", c.getTrace());
		session.write(nopb("1122"), info(3));
		assertWrite("DR|BUF|NOP(1122)[3]|");
		codec.duplicatingDecode = true;
		s.getCodec(0, 0).getPipeline().add("X", new ArrayToBufferDecoder());
		session.write(nopb("1111"));
		s.waitForDataRead(TIMEOUT);
		waitFor(100);
		assertEquals("DR|BUF|NOP(1111d)|BUF|NOP(1111d)|BUF|NOP(1111d)|", s.getTrace());
		codec.duplicatingDecode = false;
		clearTraces();
		
		codec.decodeException = new Exception("E1");
		session.writenf(nopb("456"));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getTrace());
		assertEquals("DR|EXC|SCL|SEN|", s.getTrace());
		c.stop(TIMEOUT);
		codec.decodeException = null;
		
	}
	
	void addEncoders(TestCodec codec, char type) {
		addCodecs(c,codec.PBE(type), codec.BPE());
	}
	
	void addEncoders(TestCodec codec, int streamNum, int protoID, char type) {
		addCodecs(c,streamNum,protoID,codec.PBE(type), codec.BPE());
	}
	
	@Test
	public void testCodecConfiguration() throws Exception {
		assumeSupported();

		TestCodec codec = new TestCodec();
		s = new SctpServer(PORT);
		s.start();

		//defaults
		c = new SctpClient(PORT);
		addEncoders(codec, 'A');
		c.start();
		waitForReady(TIMEOUT);
		c.session.writenf(nopb("111"));
		assertWrite("DR|NOP(111)|");
		c.session.writenf(nopb("111"),info(1,2));
		assertWrite("DR|NOP(111)[1p2]|");
		addEncoders(codec,1,2,'B');
		c.session.writenf(nopb("111"),info(1,2));
		assertWrite("DR|NOP(111)[1p2]|");
		addEncoders(codec,1,3,'C');
		c.session.writenf(nopb("111"),info(1,3));
		assertWrite("DR|NOP(111C)[1p3]|");
		addEncoders(codec,1,3,'D');
		c.session.writenf(nopb("111"),info(1,3));
		assertWrite("DR|NOP(111C)[1p3]|");
		stopClientAndClearTraces(TIMEOUT);
		
		//min stream number
		c = new SctpClient(PORT);
		addEncoders(codec, 'A');
		addEncoders(codec,2,0, 'B');
		c.minSctpStreamNumber = 2;
		c.start();
		waitForReady(TIMEOUT);
		c.session.writenf(nopb("111"));
		assertWrite("DR|NOP(111A)|");
		c.session.writenf(nopb("111"), info(1));
		assertWrite("DR|NOP(111A)[1]|");
		c.session.writenf(nopb("111"), info(2));
		assertWrite("DR|NOP(111B)[2]|");
		c.session.writenf(nopb("111"), info(3));
		assertWrite("DR|NOP(111)[3]|");
		stopClientAndClearTraces(TIMEOUT);

		//max stream number
		c = new SctpClient(PORT);
		addEncoders(codec, 'A');
		addEncoders(codec,3,0, 'B');
		c.maxSctpStreamNumber = 3;
		c.start();
		waitForReady(TIMEOUT);
		c.session.writenf(nopb("111"),info(4));
		assertWrite("DR|NOP(111A)[4]|");
		c.session.writenf(nopb("111"), info(3));
		assertWrite("DR|NOP(111B)[3]|");
		c.session.writenf(nopb("111"), info(2));
		assertWrite("DR|NOP(111)[2]|");
		c.session.writenf(nopb("111"));
		assertWrite("DR|NOP(111)|");
		stopClientAndClearTraces(TIMEOUT);

		//min proto ID
		c = new SctpClient(PORT);
		addEncoders(codec, 'A');
		addEncoders(codec,0,2, 'B');
		c.minSctpPayloadProtocolID = 2;
		c.start();
		waitForReady(TIMEOUT);
		c.session.writenf(nopb("111"));
		assertWrite("DR|NOP(111A)|");
		c.session.writenf(nopb("111"), info(0,1));
		assertWrite("DR|NOP(111A)[p1]|");
		c.session.writenf(nopb("111"), info(0,2));
		assertWrite("DR|NOP(111B)[p2]|");
		c.session.writenf(nopb("111"), info(0,3));
		assertWrite("DR|NOP(111)[p3]|");
		stopClientAndClearTraces(TIMEOUT);
		
		//max stream number
		c = new SctpClient(PORT);
		addEncoders(codec, 'A');
		addEncoders(codec,0,3, 'B');
		c.maxSctpPayloadProtocolID = 3;
		c.start();
		waitForReady(TIMEOUT);
		c.session.writenf(nopb("111"),info(0,4));
		assertWrite("DR|NOP(111A)[p4]|");
		c.session.writenf(nopb("111"), info(0,3));
		assertWrite("DR|NOP(111B)[p3]|");
		c.session.writenf(nopb("111"), info(0,2));
		assertWrite("DR|NOP(111)[p2]|");
		c.session.writenf(nopb("111"));
		assertWrite("DR|NOP(111)|");
		stopClientAndClearTraces(TIMEOUT);
		
		//all
		c = new SctpClient(PORT);
		addEncoders(codec, 'A');
		addEncoders(codec,2,6, 'B');
		addEncoders(codec,2,7, 'C');
		addEncoders(codec,3,6, 'D');
		addEncoders(codec,3,7, 'E');
		c.minSctpStreamNumber = 2;
		c.maxSctpStreamNumber = 3;
		c.minSctpPayloadProtocolID = 6;
		c.maxSctpPayloadProtocolID = 7;
		c.start();
		waitForReady(TIMEOUT);
		c.session.writenf(nopb("111"));
		assertWrite("DR|NOP(111A)|");
		c.session.writenf(nopb("111"),info(1,5));
		assertWrite("DR|NOP(111A)[1p5]|");
		c.session.writenf(nopb("111"),info(1,6));
		assertWrite("DR|NOP(111A)[1p6]|");
		c.session.writenf(nopb("111"),info(2,5));
		assertWrite("DR|NOP(111A)[2p5]|");
		
		c.session.writenf(nopb("111"),info(2,6));
		assertWrite("DR|NOP(111B)[2p6]|");
		c.session.writenf(nopb("111"),info(2,7));
		assertWrite("DR|NOP(111C)[2p7]|");
		c.session.writenf(nopb("111"),info(3,6));
		assertWrite("DR|NOP(111D)[3p6]|");
		c.session.writenf(nopb("111"),info(3,7));
		assertWrite("DR|NOP(111E)[3p7]|");
		
		c.session.writenf(nopb("111"),info(3,8));
		assertWrite("DR|NOP(111A)[3p8]|");
		c.session.writenf(nopb("111"),info(4,7));
		assertWrite("DR|NOP(111A)[4p7]|");
		c.session.writenf(nopb("111"),info(4,8));
		assertWrite("DR|NOP(111A)[4p8]|");
		c.session.writenf(nopb("111"),info(4,Integer.MIN_VALUE));
		assertWrite("DR|NOP(111A)[4p-2147483648]|");
		c.session.writenf(nopb("111"),info(4,Integer.MAX_VALUE));
		assertWrite("DR|NOP(111A)[4p2147483647]|");	
	}
	
	void assertTracedEvents(String s, long id) {
		s = s.replaceAll("X", ""+id);
		assertEquals(s, getTrace());
	}
	
	@Test
	public void testEventDrivenCodecs() throws Exception {
		assumeSupported();

		s = new SctpServer(PORT);
		s.start();
		c = new SctpClient(PORT);
		addCodecs(c, new EventEncoder(""));
		addCodecs(c,0,0,new EventEncoder("0,0"));
		c.start();
		waitForReady(TIMEOUT);
		InternalSctpSession session = c.session;
		assertTracedEvents("#ADD(X)||#CREATED(X)||#OPENED(X)||#READY(X)||", session.getId());
		
		session.writenf(nopb("111"));
		assertWrite("DR|NOP(111)|");
		assertTracedEvents("0,0#ADD(X)||0,0#CREATED(X)||0,0#OPENED(X)||0,0#READY(X)||", session.getId());
		session.writenf(nopb("222"));
		assertWrite("DR|NOP(222)|");
		assertTracedEvents("", session.getId());
		c.getCodec(0,0).getPipeline().remove("E1");
		session.writenf(nopb("3"));
		assertWrite("DR|NOP(3)|");
		assertTracedEvents("", session.getId());
		session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertTracedEvents("#CLOSED(X)||0,0#REM(X)||#ENDING(X)||", session.getId());
		c.stop(TIMEOUT);
		clearTraces();
		
		c = new SctpClient(PORT);
		addCodecs(c, new EventDecoder(""));
		addCodecs(c,1,1,new EventDecoder("1,1"));
		c.start();
		waitForReady(TIMEOUT);
		session = c.session;
		assertTracedEvents("#ADD(X)||#CREATED(X)||#OPENED(X)||#READY(X)||", session.getId());
		
		s.session.writenf(nopb("123"));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(123)|", c.getTrace());
		assertTracedEvents("", session.getId());
		s.session.writenf(nopb("123"), info(1,1));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(123)[1p1]|", c.getTrace());
		assertTracedEvents("1,1#ADD(X)||1,1#CREATED(X)||1,1#OPENED(X)||1,1#READY(X)||", session.getId());
		session.close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertTracedEvents("#CLOSED(X)||1,1#CLOSED(X)||#ENDING(X)||1,1#ENDING(X)||", session.getId());	
	}
	
	class EventEncoder extends BufferToArrayEncoder implements IEventDrivenCodec {

		final String id;
		
		EventEncoder(String id) {
			this.id = id;
		}
		
		@Override
		public void added(ISession session) {
			trace(id + "#ADD(" + session.getId() + ")|");
		}

		@Override
		public void event(ISession session, SessionEvent event) {
			trace(id + "#" + event.name() +"(" + session.getId() + ")|");
		}

		@Override
		public void removed(ISession session) {
			trace(id + "#REM(" + session.getId() + ")|");
		}
	}
	
	class EventDecoder extends BufferToArrayDecoder implements IEventDrivenCodec {

		final String id;
		
		EventDecoder(String id) {
			this.id = id;
		}
		
		@Override
		public void added(ISession session) {
			trace(id + "#ADD(" + session.getId() + ")|");
		}

		@Override
		public void event(ISession session, SessionEvent event) {
			trace(id + "#" + event.name() +"(" + session.getId() + ")|");
		}

		@Override
		public void removed(ISession session) {
			trace(id + "#REM(" + session.getId() + ")|");
		}
	}
	
	@Test
	public void testOptimizeCopying() throws Exception {
		assumeSupported();
		
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		c.allocator = new TestAllocator(false, false);
		s.start();
		c.start();
		waitForReady(TIMEOUT);
		setAllocator(c.allocator);
		assertAllocator(1,0,1);
		
		byte[] msgb = nopb("12345");
		c.session.suspendWrite();
		c.session.writenf(msgb);
		msgb[3]++;
		c.session.resumeWrite();
		assertWrite("DR|NOP(12345)|");
		assertAllocator(2,0,2);
		
		ByteBuffer msgbb = ByteBuffer.wrap(nopb("123"));
		c.session.suspendWrite();
		c.session.writenf(msgbb);
		msgbb.array()[3]++;
		c.session.resumeWrite();
		assertWrite("DR|NOP(123)|");
		assertAllocator(3,0,3);
		assertEquals(0, msgbb.remaining());
		
		msgbb = ByteBuffer.wrap(nopb("123",0,4));
		c.session.suspendWrite();
		c.session.writenf(msgbb,6);
		msgbb.array()[3]++;
		c.session.resumeWrite();
		assertWrite("DR|NOP(123)|");
		assertAllocator(4,0,4);
		assertEquals(4, msgbb.remaining());
		stopClientAndClearTraces(TIMEOUT);
		
		c = new SctpClient(PORT);
		c.allocator = new TestAllocator(false, false);
		c.optimizeCopying = true;
		c.start();
		waitForReady(TIMEOUT);
		setAllocator(c.allocator);
		assertAllocator(1,0,1);
		
		msgb = nopb("5678");
		c.session.suspendWrite();
		c.session.writenf(msgb);
		msgb[3]++;
		c.session.resumeWrite();
		assertWrite("DR|NOP(6678)|");
		assertAllocator(1,0,1);
		
		msgbb = ByteBuffer.wrap(nopb("12344"));
		c.session.suspendWrite();
		c.session.writenf(msgbb);
		msgbb.array()[3]++;
		c.session.resumeWrite();
		assertWrite("DR|NOP(22344)|");
		assertAllocator(1,0,1);

		msgbb = ByteBuffer.wrap(nopb("1234",0,4));
		c.session.suspendWrite();
		c.session.writenf(msgbb,7);
		msgbb.array()[3]++;
		c.session.resumeWrite();
		assertWrite("DR|NOP(1234)|");
		assertAllocator(2,0,2);
		assertEquals(4, msgbb.remaining());
		stopClientAndClearTraces(TIMEOUT);

		c = new SctpClient(PORT);
		c.allocator = new TestAllocator(true, false);
		c.optimizeCopying = true;
		c.start();
		waitForReady(TIMEOUT);
		setAllocator(c.allocator);
		assertAllocator(1,0,1);
		
		msgb = nopb("5678");
		c.session.suspendWrite();
		c.session.writenf(msgb);
		msgb[3]++;
		c.session.resumeWrite();
		assertWrite("DR|NOP(5678)|");
		assertAllocator(2,0,2);
		stopClientAndClearTraces(TIMEOUT);

		c = new SctpClient(PORT);
		c.allocator = new TestAllocator(true, true);
		c.optimizeCopying = true;
		c.start();
		waitForReady(TIMEOUT);
		setAllocator(c.allocator);
		assertAllocator(1,1,0);

		msgb = nopb("5678");
		c.session.suspendWrite();
		c.session.writenf(msgb);
		msgb[3]++;
		c.session.resumeWrite();
		assertWrite("DR|NOP(5678)|");
		assertAllocator(2,2,0);

		msgbb = ByteBuffer.wrap(nopb("12344"));
		c.session.suspendWrite();
		c.session.writenf(msgbb);
		msgbb.array()[3]++;
		c.session.resumeWrite();
		assertWrite("DR|NOP(22344)|");
		assertAllocator(2,3,0);	
	}
	
	@Test
	public void testOptimizedDataCopyingRead() throws Exception {
		assumeSupported();

		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.allocator = new TestAllocator(false, true);
		s.optimizeCopying = true;
		s.start();
		c.start();
		waitForReady(TIMEOUT);
		InternalSctpSession session = c.session;
		setAllocator(s.allocator);
		assertAllocatorDeltas(1, 1, 0);
		assertNull(getIn(s));
		assertEquals(0, getOut(s).size());
		
		session.writenf(nopb("1"));
		assertWrite("DR|BUF|NOP(1)|");
		assertAllocatorDeltas(1, 0, 1);
		s.session.release(s.readBuffer);
		assertAllocatorDeltas(0, 1, 0);
		assertNull(getIn(s));
		assertEquals(0, getOut(s).size());
		
		Packet p = nop("1", "0", '2', 6000);
		session.writenf(p.toBytes());
		assertWrite("DR|DR|DR|BUF|NOP("+p.payload+")|");
		assertAllocatorDeltas(3, 2, 1);
		s.session.release(s.readBuffer);
		assertAllocatorDeltas(0, 1, 0);
		assertNull(getIn(s));
		assertEquals(0, getOut(s).size());
			
		session.writenf(p.toBytes());
		assertWrite("DR|BUF|NOP("+p.payload+")|");
		assertAllocatorDeltas(1, 0, 1);
		s.session.release(s.readBuffer);
		assertAllocatorDeltas(0, 1, 0);
		assertNull(getIn(s));
		assertEquals(0, getOut(s).size());
		stopClientAndClearTraces(TIMEOUT);
		s.stop(TIMEOUT);
		assertAllocatorDeltas(1, 1, 0);
		
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.allocator = new TestAllocator(false, true);
		s.start();
		c.start();
		waitForReady(TIMEOUT);
		session = c.session;
		setAllocator(s.allocator);
		assertAllocatorDeltas(1, 0, 1);

		session.writenf(nopb("1"));
		assertWrite("DR|NOP(1)|");
		assertAllocatorDeltas(0, 0, 1);
		assertNotNull(getIn(s));
		assertEquals(2048, getIn(s).capacity());
		assertEquals(0, getOut(s).size());

		session.writenf(p.toBytes());
		assertWrite("DR|DR|DR|NOP("+p.payload+")|");
		assertAllocatorDeltas(1, 1, 1);
		assertNotNull(getIn(s));
		assertEquals(2048*4, getIn(s).capacity());
		assertEquals(0, getOut(s).size());

		session.writenf(p.toBytes());
		assertWrite("DR|NOP("+p.payload+")|");
		assertAllocatorDeltas(0, 0, 1);
		assertNotNull(getIn(s));
		assertEquals(0, getOut(s).size());
		stopClientAndClearTraces(TIMEOUT);
		s.stop(TIMEOUT);
		assertAllocatorDeltas(0, 1, 0);
		assertNull(getIn(s));
	
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.allocator = new TestAllocator(false, false);
		s.start();
		c.start();
		waitForReady(TIMEOUT);
		session = c.session;
		setAllocator(s.allocator);
		assertAllocatorDeltas(1, 0, 1);

		session.writenf(nopb("1"));
		assertWrite("DR|NOP(1)|");
		assertAllocatorDeltas(0, 0, 1);
		assertNotNull(getIn(s));
		assertEquals(2048, getIn(s).capacity());
		assertEquals(0, getOut(s).size());

		session.writenf(p.toBytes());
		assertWrite("DR|DR|DR|NOP("+p.payload+")|");
		assertAllocatorDeltas(1, 0, 2);
		assertNotNull(getIn(s));
		assertEquals(2048*4, getIn(s).capacity());
		assertEquals(0, getOut(s).size());

		session.writenf(p.toBytes());
		assertWrite("DR|NOP("+p.payload+")|");
		assertAllocatorDeltas(0, 0, 2);
		assertNotNull(getIn(s));
		assertEquals(0, getOut(s).size());
		stopClientAndClearTraces(TIMEOUT);
		s.stop(TIMEOUT);
		assertAllocatorDeltas(0, 0, 2);
		assertNull(getIn(s));
	}
	
	@Test
	public void testOptimizedDataCopyingReadWithCodec() throws Exception {
		assumeSupported();

		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.allocator = new TestAllocator(false, true);
		s.optimizeCopying = true;
		addCodecs(s,0,0,new ArrayToBufferDecoder(true));
		s.start();
		c.start();
		waitForReady(TIMEOUT);
		InternalSctpSession session = c.session;
		setAllocator(s.allocator);
		assertAllocatorDeltas(1, 1, 0);
		assertNull(getIn(s));
		assertEquals(0, getOut(s).size());
		
		session.writenf(nopb("1"));
		assertWrite("DR|BUF|NOP(1)|");
		assertAllocatorDeltas(2, 1, 1);
		s.session.release(s.readBuffer);
		assertAllocatorDeltas(0, 1, 0);
		assertNull(getIn(s));
		assertEquals(0, getOut(s).size());
		
		s.getCodec(0,0).getPipeline().replace("D1", "D1", new BufferToArrayDecoder(true));
		session.writenf(nopb("1"));
		assertWrite("DR|NOP(1)|");
		assertAllocatorDeltas(1, 1, 0);
		assertNull(getIn(s));
		assertEquals(0, getOut(s).size());
		
		s.getCodec(0,0).getPipeline().remove("D1");
		session.writenf(nopb("1"));
		assertWrite("DR|BUF|NOP(1)|");
		assertAllocatorDeltas(1, 0, 1);
		s.session.release(s.readBuffer);
		assertAllocatorDeltas(0, 1, 0);
		assertNull(getIn(s));
		assertEquals(0, getOut(s).size());
		stopClientAndClearTraces(TIMEOUT);
		s.stop(TIMEOUT);
		assertAllocatorDeltas(1, 1, 0);
		
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.allocator = new TestAllocator(false, true);
		addCodecs(s,0,0,new ArrayToBufferDecoder(true));
		s.start();
		c.start();
		waitForReady(TIMEOUT);
		session = c.session;
		setAllocator(s.allocator);
		assertAllocatorDeltas(1, 0, 1);
		assertNotNull(getIn(s));
		assertEquals(0, getOut(s).size());

		session.writenf(nopb("1"));
		assertWrite("DR|BUF|NOP(1)|");
		assertAllocatorDeltas(1, 0, 2);
		s.session.release(s.readBuffer);
		assertAllocatorDeltas(0, 1, 1);
		assertNotNull(getIn(s));
		assertEquals(0, getOut(s).size());

		s.getCodec(0,0).getPipeline().replace("D1", "D1", new BufferToArrayDecoder(false));
		session.writenf(nopb("1"));
		assertWrite("DR|NOP(1)|");
		assertAllocatorDeltas(0, 0, 1);
		assertNotNull(getIn(s));
		assertEquals(0, getOut(s).size());

		s.getCodec(0,0).getPipeline().remove("D1");
		session.writenf(nopb("1"));
		assertWrite("DR|NOP(1)|");
		assertAllocatorDeltas(0, 0, 1);
		assertNotNull(getIn(s));
		assertEquals(0, getOut(s).size());
		stopClientAndClearTraces(TIMEOUT);
		s.stop(TIMEOUT);
		assertAllocatorDeltas(0, 1, 0);

		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.allocator = new TestAllocator(false, false);
		addCodecs(s,0,0,new ArrayToBufferDecoder(false));
		s.start();
		c.start();
		waitForReady(TIMEOUT);
		session = c.session;
		setAllocator(s.allocator);
		assertAllocatorDeltas(1, 0, 1);
		assertNotNull(getIn(s));
		assertEquals(0, getOut(s).size());

		session.writenf(nopb("1"));
		assertWrite("DR|BUF|NOP(1)|");
		assertAllocatorDeltas(0, 0, 1);
		assertNotNull(getIn(s));
		assertEquals(0, getOut(s).size());

		s.getCodec(0,0).getPipeline().replace("D1", "D1", new BufferToArrayDecoder(false));
		session.writenf(nopb("1"));
		assertWrite("DR|NOP(1)|");
		assertAllocatorDeltas(0, 0, 1);
		assertNotNull(getIn(s));
		assertEquals(0, getOut(s).size());

		s.getCodec(0,0).getPipeline().remove("D1");
		session.writenf(nopb("1"));
		assertWrite("DR|NOP(1)|");
		assertAllocatorDeltas(0, 0, 1);
		assertNotNull(getIn(s));
		assertEquals(0, getOut(s).size());
		stopClientAndClearTraces(TIMEOUT);
		s.stop(TIMEOUT);
		assertAllocatorDeltas(0, 0, 1);
	}	
}
