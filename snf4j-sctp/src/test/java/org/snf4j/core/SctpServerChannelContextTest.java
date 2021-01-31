package org.snf4j.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.junit.Test;

public class SctpServerChannelContextTest extends SctpTest {

	void assertToString(String expected, String value) {
		assertEquals(TestSctpServerChannel.class.getName() + expected, value);
	}
	
	@Test
	public void testToString() {
		SctpServerChannelContext ctx = new SctpServerChannelContext(null);
		TestSctpServerChannel ssc = new TestSctpServerChannel();
		
		InetSocketAddress a1 = address(2001);
		InetSocketAddress a2 = address(2002);
		
		assertToString("[not bound]", ctx.toString(ssc));
		assertNull(ctx.toString(null));
		ssc.localAddresses.add(a1);
		assertToString("["+a1+"]", ctx.toString(ssc));
		ssc.localAddresses.add(a2);
		String s = ctx.toString(ssc);
		assertTrue(s.startsWith(TestSctpServerChannel.class.getName()));
		s = s.substring(TestSctpServerChannel.class.getName().length());
		if (!s.equals("["+a1+","+a2+"]")) {
			assertToString("["+a2+","+a1+"]", ctx.toString(ssc));
		}
		ssc.localAddressesException = new IOException();
		assertToString("[unknown]", ctx.toString(ssc));
		
	}
}
