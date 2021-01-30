package org.snf4j.core;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.Arrays;

import org.junit.After;
import org.junit.Assume;
import org.junit.Test;

import com.sun.nio.sctp.SctpChannel;

public class SctpSessionTest {
	
	long TIMEOUT = 2000;
	
	int PORT = 7779;
	
	SctpServer s;
	
	SctpClient c;
	
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
	
	@Test
	public void testConnection() throws Exception {
		assumeSupported();
		startClientServer();

		c.session.write(nopb("1"), info(1,2));
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|", c.getTrace());
		assertEquals("DR|NOP(1)[1p2]|", s.getTrace());

		c.session.close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getTrace());
		assertEquals("SCL|SEN|", s.getTrace());
	}
	
	@Test
	public void testRegisterConnectedChannel() throws Exception {
		assumeSupported();
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.start();
		
		SctpChannel sc = SctpChannel.open();
		sc.connect(new InetSocketAddress(InetAddress.getByName(c.ip), c.port));
		c.start(sc);
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);	
		assertEquals("SCR|SOP|RDY|", c.getTrace());
		assertEquals("SCR|SOP|RDY|", s.getTrace());
		
		c.session.write(nopb("1"), info(1,2));
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|", c.getTrace());
		assertEquals("DR|NOP(1)[1p2]|", s.getTrace());
		
		c.session.close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getTrace());
		assertEquals("SCL|SEN|", s.getTrace());
	}
	
	@Test
	public void testRegisterOpenChannel() throws Exception {
		assumeSupported();
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.start();
		
		SctpChannel sc = SctpChannel.open();
		c.start(sc);
		waitFor(50);
		assertEquals("", c.getTrace());
		assertEquals("", s.getTrace());
			
		sc.connect(new InetSocketAddress(InetAddress.getByName(c.ip), c.port));
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);	
		assertEquals("SCR|SOP|RDY|", c.getTrace());
		assertEquals("SCR|SOP|RDY|", s.getTrace());
		
		c.session.write(nopb("1"), info(1,2));
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|", c.getTrace());
		assertEquals("DR|NOP(1)[1p2]|", s.getTrace());
		
		c.session.close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getTrace());
		assertEquals("SCL|SEN|", s.getTrace());
	}
	
	@Test
	public void testRegisterClosedChannel() throws Exception {
		assumeSupported();
		c = new SctpClient(PORT);
		SctpChannel sc = SctpChannel.open();
		c.start(sc);
		waitFor(50);
		assertEquals("", c.getTrace());
		
		SelectionKey k = sc.keyFor(c.loop.selector);
		SctpChannelContext ctx = (SctpChannelContext) k.attachment();
		sc.close();
		
		c.loop.execute(new Runnable() {

			@Override
			public void run() {
				try {
					ctx.completeRegistration(c.loop, k, sc);
				} catch (Exception e) {
				}
			}			
		});
		
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SEN|", c.getTrace());
		c.stop(TIMEOUT);
		
		c = new SctpClient(PORT);
		SctpChannel sc2 = SctpChannel.open();
		c.start(sc2);
		waitFor(50);
		assertEquals("", c.getTrace());
		
		SelectionKey k2 = sc2.keyFor(c.loop.selector);
		SctpChannelContext ctx2 = (SctpChannelContext) k2.attachment();
		TestSctpChannel tsc = new TestSctpChannel();
		tsc.remoteAddressesException = new ClosedChannelException();
		sc2.close();

		c.loop.execute(new Runnable() {

			@Override
			public void run() {
				try {
					ctx2.completeRegistration(c.loop, k2, tsc);
				} catch (Exception e) {
				}
			}			
		});
		
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SEN|", c.getTrace());
		c.stop(TIMEOUT);
		
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
	
	@Test
	public void testWriteSpinCount() throws Exception {
		assumeSupported();
		startClientServer();
		
		byte[] payload = new byte[1000];
		Arrays.fill(payload, (byte)'1');
		byte[] data = nopb(new String(payload));
		
		SctpSession session = c.session;
		ImmutableSctpMessageInfo msgInfo = info(0);
		session.suspendWrite();
		for (int i=0; i<32; ++i) {
			session.writenf(data, msgInfo);
		}
		session.resumeWrite();
		waitFor(500);
		assertEquals("DS|DS|", c.getTrace());
		assertEquals(32, countRDNOP(s.getTrace(), payload));
		c.stop(TIMEOUT);
		
		c = new SctpClient(PORT);
		c.maxWriteSpinCount = 1;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getTrace());
		session = c.session;
		session.suspendWrite();
		for (int i=0; i<32; ++i) {
			session.write(data, msgInfo);
		}
		session.resumeWrite();
		waitFor(500);
		assertEquals("DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|DS|", c.getTrace());
		assertEquals(32, countRDNOP(s.getTrace(), payload));
		c.stop(TIMEOUT);
		
		c = new SctpClient(PORT);
		c.maxWriteSpinCount = 16;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getTrace());
		s.waitForSessionReady(TIMEOUT);
		s.getTrace();
		session = c.session;
		session.suspendWrite();
		session.write(nopb("1234"), msgInfo);
		TestSelectionKey key = new TestSelectionKey(new TestSctpChannel());
		SctpChannelContext ctx = (SctpChannelContext) session.channel.keyFor(c.loop.selector).attachment();
		Method m = SctpChannelContext.class.getDeclaredMethod("handleWriting", SelectorLoop.class, SctpSession.class, SelectionKey.class, int.class);
		m.setAccessible(true);
		assertEquals(new Integer(0), m.invoke(ctx, c.loop, session, key, 1));
		session.resumeWrite();
		waitFor(100);
		assertEquals("DS|", c.getTrace());
		assertEquals("DR|NOP(1234)|", s.getTrace());
		
		c.closeInEvent = EventType.DATA_SENT;
		c.closeType = StoppingType.DIRTY;
		session.suspendWrite();
		for (int i=0; i<15; ++i) {
			session.write(data, msgInfo);
		}
		session.resumeWrite();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getTrace());
		waitFor(500);
		assertEquals(15, countRDNOP(s.getTrace(), payload));
		c.stop(TIMEOUT);
		
		c = new SctpClient(PORT);
		c.maxWriteSpinCount = 16;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getTrace());
		s.waitForSessionReady(TIMEOUT);
		s.getTrace();
		session = c.session;
		session.suspendWrite();
		session.writenf(nopb("1234"), msgInfo);
		c.writeInEvent = EventType.DATA_SENT;
		c.packetToWriteInEvent = nop("5678");
		session.resumeWrite();
		waitFor(100);
		assertEquals("DS|DS|", c.getTrace());
		assertEquals("DR|NOP(1234)|DR|NOP(5678)|", s.getTrace());
	}
	
}
