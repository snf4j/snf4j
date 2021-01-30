package org.snf4j.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

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
}
