package org.snf4j.core;

import static org.junit.Assert.assertEquals;

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
		return ImmutableSctpMessageInfo.create(1);
	}
	
	@Test
	public void testSimpleConnection() throws Exception {
		assumeSupported();
		startClientServer();
		
		c.session.write(nopb("1"), info(1));
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|", c.getTrace());
		assertEquals("DR|NOP(1)|", s.getTrace());
		
		c.session.close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getTrace());
		assertEquals("SCL|SEN|", s.getTrace());
	}
}
