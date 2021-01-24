package org.snf4j.core;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;

public class SctpSessionTest {
	
	long TIMEOUT = 2000;
	
	int PORT = 7779;
	
	SctpServer s;
	
	SctpClient c;
	
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
		assertEquals("", c.getTrace());
		assertEquals("", s.getTrace());
	}
	
	@Test
	public void testSimpleConnection() throws Exception {
		startClientServer();
	}
}
