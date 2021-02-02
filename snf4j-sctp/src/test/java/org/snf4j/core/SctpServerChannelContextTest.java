package org.snf4j.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class SctpServerChannelContextTest extends SctpTest {

	void assertToString(String expected, String value) {
		assertEquals(TestSctpServerChannel.class.getName() + expected, value);
	}
	
	@Test
	public void testSessionFactoryNotification() throws Exception {
		assumeSupported();
		s = new SctpServer(PORT);
		s.traceSessionFactory = true;
		s.start().sync(TIMEOUT);
		assertEquals("REGISTERED|", s.getTrace());
		
		AtomicBoolean lock = new AtomicBoolean();
		s.loop.execute(new Runnable() {

			@Override
			public void run() {
				s.loop.fireException(s.ssc.keyFor(s.loop.selector), new Exception("EE"));
				LockUtils.notify(lock);
			}
		});
		LockUtils.waitFor(lock, TIMEOUT);
		assertEquals("EXCEPTION(EE)|", s.getTrace());
		s.stop(TIMEOUT);
		assertEquals("CLOSED|", s.getTrace());
	}
	
	@Test
	public void testToString() {
		SctpServerChannelContext ctx = new SctpServerChannelContext(null);
		TestSctpServerChannel ssc = new TestSctpServerChannel();
		
		InetSocketAddress a1 = address(2001);
		InetSocketAddress a2 = address(2002);
		
		assertNull(ctx.toString(null));
		assertToString("[not-bound]", ctx.toString(ssc));
		ssc.localAddresses.add(a1);
		assertToString("["+a1+"]", ctx.toString(ssc));
		ssc.localAddresses.add(a2);
		String s = ctx.toString(ssc);
		assertTrue(s.startsWith(TestSctpServerChannel.class.getName()));
		s = s.substring(TestSctpServerChannel.class.getName().length());
		assertToString("["+a1+","+a2+"]", ctx.toString(ssc));
		ssc.localAddressesException = new IOException();
		assertToString("[unknown]", ctx.toString(ssc));		
		
		TestSctpChannel sc = new TestSctpChannel();
		assertEquals("org.snf4j.core.TestSctpChannel[not-connected local=not-bound]", ctx.toString(sc));
	}
}
