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
	
	@Test
	public void testSimpleConnection() throws Exception {
		assumeSupported();
		startClientServer();
		
		c.session.close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getTrace());
		assertEquals("SCL|SEN|", s.getTrace());
	}
}
