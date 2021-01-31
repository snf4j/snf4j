package org.snf4j.core;

import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Assume;
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
	}
	
	public void assertAllocator(int allocated, int released, int size) {
		waitFor(50);
		assertEquals("alloceded", allocated, allocator.getAllocatedCount());
		assertEquals("released", released, allocator.getReleasedCount());
		assertEquals("szie", size, allocator.getSize());
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
	
	
}
