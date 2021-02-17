/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * -----------------------------------------------------------------------------
 */
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
