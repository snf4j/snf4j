package org.snf4j.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Set;

import org.junit.Test;

import com.sun.nio.sctp.SctpChannel;

public class SctpSessionTest extends SctpTest {	
	
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
}
