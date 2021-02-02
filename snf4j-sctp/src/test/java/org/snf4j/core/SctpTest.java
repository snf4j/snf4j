package org.snf4j.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Assume;
import org.junit.Test;
import org.snf4j.core.SctpSession.SctpRecord;
import org.snf4j.core.allocator.TestAllocator;

import com.sun.nio.sctp.SctpChannel;

public class SctpTest {
	
	long TIMEOUT = 2000;
	
	int PORT = 7779;
	
	SctpServer s;
	
	SctpClient c;

	TestAllocator allocator;
	
	public final static boolean SUPPORTED;
	
	static {
		boolean supported;
		
		try {
			SctpChannel.open();
			supported = true;
		}
		catch (Exception e) {
			supported = false;
		}
		SUPPORTED = supported;
	}
	
	public static void assumeSupported() {
		Assume.assumeTrue(SUPPORTED);
	}
	
	@After
	public void after() throws Exception {
		if (c != null) c.stop(TIMEOUT);
		if (s != null) s.stop(TIMEOUT);
	}

	void startClientServer() throws Exception {
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getTrace());
		assertEquals("SCR|SOP|RDY|", s.getTrace());
	}
	
	void waitFor(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
	}
	
	void waitForReady(long millis) throws Exception {
		c.waitForSessionReady(millis);
		s.waitForSessionReady(millis);
		assertEquals("SCR|SOP|RDY|", c.getTrace());
		assertEquals("SCR|SOP|RDY|", s.getTrace());
	}
	
	void clearTraces() {
		c.getTrace();
		s.getTrace();
	}
	
	Packet nop(String payload) {
		return new Packet(PacketType.NOP, payload);
	}
	
	byte[] nopb(String payload) {
		return nop(payload).toBytes();
	}
	
	Packet nop(String head, String tail, char mid, int midLength) {
		char[] payload = new char[tail.length() + tail.length() + midLength];
		
		Arrays.fill(payload, head.length(), head.length() + midLength, mid);
		System.arraycopy(head.toCharArray(), 0, payload, 0, head.length());
		System.arraycopy(tail.toCharArray(), 0, payload, payload.length - tail.length(), tail.length());
		return nop(new String(payload));
	}
	
	byte[] nopb(String head, String tail, char mid, int midLength) {
		return nop(head, tail, mid, midLength).toBytes();
	}
	
	ImmutableSctpMessageInfo info(int streamNumber) {
		return ImmutableSctpMessageInfo.create(streamNumber);
	}
	
	ImmutableSctpMessageInfo info(int streamNumber, int protocolID) {
		return ImmutableSctpMessageInfo.create(streamNumber, protocolID);
	}
	
	ImmutableSctpMessageInfo info(int streamNumber, int protocolID, boolean unordered) {
		return ImmutableSctpMessageInfo.create(streamNumber, protocolID, unordered);
	}

	public static int countRDNOP(String s, byte[] payload) {
		int off = 0;
		String rdnop = "DR|NOP(" + new String(payload) + ")|";
		int i;
		int count = 0;
		
		while ((i = s.indexOf(rdnop, off)) != -1) {
			off = i + rdnop.length();
			count++;
		}
		return count;
	}

	public static SelectionKey getKey(SctpServer server) {
		return server.session.channel.keyFor(server.loop.selector);
	}
	
	public void setAllocator(TestAllocator a) {
		this.allocator = a;
		prevAllocated = prevReleased = 0;
	}
	
	int prevAllocated, prevReleased;
	
	public void assertAllocator(int allocated, int released, int size) {
		waitFor(50);
		prevAllocated = allocator.getAllocatedCount();
		prevReleased = allocator.getReleasedCount();
		assertEquals("alloceded", allocated, prevAllocated);
		assertEquals("released", released, prevReleased);
		assertEquals("szie", size, allocator.getSize());
	}
	
	public void assertAllocatorDeltas(int allocated, int released, int size) {
		waitFor(50);
		int newAllocated = allocator.getAllocatedCount();
		int newReleased = allocator.getReleasedCount();
		assertEquals("alloceded", allocated, newAllocated - prevAllocated);
		assertEquals("released", released, newReleased - prevReleased);
		assertEquals("szie", size, allocator.getSize());
		prevReleased = newReleased;
		prevAllocated = newAllocated;
	}
	
	ByteBuffer allocated(int i) {
		return allocator.getAllocated().get(i);
	}
	
	ByteBuffer released(int i) {
		return allocator.getReleased().get(i);
	}
	
	public void releaseReadBuffer(SctpServer server) {
		server.allocator.release(server.readBuffer);
	}
	
	public void sleepLoop(SelectorLoop loop, final long millis) throws InterruptedException {
		final AtomicBoolean lock = new AtomicBoolean();
		
		loop.execute(new Runnable() {

			@Override
			public void run() {
				LockUtils.notify(lock);
				try {
					Thread.sleep(millis);
				} catch (InterruptedException e) {
				}
			}
		});
		LockUtils.waitFor(lock, millis);
	}
	
	InetSocketAddress address(int port) {
		return new InetSocketAddress("127.0.0.1", port);
	}
	
	InetSocketAddress address(SocketAddress address, int port) {
		return new InetSocketAddress(((InetSocketAddress)address).getAddress(), port);
	}
	
	InetSocketAddress address(InetAddress address, int port) {
		return new InetSocketAddress(address, port);
	}
	
	ByteBuffer getFragment(SctpSession session) throws Exception {
		Field f = SctpSession.class.getDeclaredField("fragments");
		Field f2 = SctpFragments.class.getDeclaredField("fragment");
		
		f.setAccessible(true);
		f2.setAccessible(true);
		return (ByteBuffer) f2.get(f.get(session));
	}

	ByteBuffer getFragment(SctpServer server) throws Exception {
		return getFragment(server.session);
	}
	
	ByteBuffer getIn(SctpSession session) throws Exception {
		Field f = SctpSession.class.getDeclaredField("inBuffer");
		
		f.setAccessible(true);
		return (ByteBuffer) f.get(session);
	}
	
	ByteBuffer getIn(SctpServer server) throws Exception {
		return getIn(server.session);
	}
	
	void setIn(SctpSession session, ByteBuffer in) throws Exception {
		Field f = SctpSession.class.getDeclaredField("inBuffer");
		
		f.setAccessible(true);
		f.set(session, in);
	}

	void setIn(SctpServer server, ByteBuffer in) throws Exception {
		setIn(server.session, in);
	}
	
	@SuppressWarnings("unchecked")
	Queue<SctpRecord> getOut(SctpSession session) throws Exception {
		Field f = SctpSession.class.getDeclaredField("outQueue");
		
		f.setAccessible(true);
		return (Queue<SctpRecord>) f.get(session);
	}
	
	Queue<SctpRecord> getOut(SctpServer server) throws Exception {
		return getOut(server.session);
	}
	
	@Test
	public void testRegister() throws Exception {
		SelectorLoop loop = new SelectorLoop();
		
		loop.start();
		try {
			Sctp.register(loop,new TestSctpChannel(), null);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("session is null", e.getMessage());
		}
		
		try {
			Sctp.register(loop,new TestSctpServerChannel(), null);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("factory is null", e.getMessage());
		}
	}
	
}
