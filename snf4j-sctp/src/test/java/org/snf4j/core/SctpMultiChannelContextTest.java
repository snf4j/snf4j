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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;

import org.junit.Test;

public class SctpMultiChannelContextTest extends SctpTest {
	
	void assertToString(String expected, String value) {
		assertEquals(TestSctpMultiChannel.class.getName() + expected, value);
	}
	
	@Test
	public void testToString() {
		ChannelContext<?> ctx = new SctpMultiChannelContext(null);
		TestSctpMultiChannel sc = new TestSctpMultiChannel();
		
		InetSocketAddress a1 = address(2001);
		InetSocketAddress a2 = address(2002);
		InetSocketAddress a3 = address(2003);
		InetSocketAddress a4 = address(2004);
		
		assertNull(ctx.toString((SelectableChannel)null));
		assertToString("[local=not-bound]", ctx.toString(sc));
		sc.localAddresses.add(a1);
		sc.localAddresses.add(a2);
		assertToString("[local="+a1+","+a2+"]", ctx.toString(sc));
		sc.localAddressesException = new IOException();
		assertToString("[local=unknown]", ctx.toString(sc));
		sc.localAddresses.clear();
		sc.localAddressesException = null;
		sc.associationsException = new IOException();
		assertToString("[local=not-bound remote=unknown]", ctx.toString(sc));
		sc.associationsException = null;
		sc.associations.add(sc.association(0));
		assertToString("[local=not-bound remote=shutdown]", ctx.toString(sc));
		sc.remoteAddresses[0].add(a3);
		sc.remoteAddresses[0].add(a4);
		assertToString("[local=not-bound remote="+a3+","+a4+"]", ctx.toString(sc));
		sc.associations.add(sc.association(1));
		assertToString("[local=not-bound remote="+a3+","+a4+";shutdown]", ctx.toString(sc));
		sc.remoteAddresses[1].add(a1);
		assertToString("[local=not-bound remote="+a3+","+a4+";"+a1+"]", ctx.toString(sc));
		sc.remoteAddressesException[1] = new IOException();
		assertToString("[local=not-bound remote="+a3+","+a4+";unknown]", ctx.toString(sc));
		sc.remoteAddressesException[1] = null;
		sc.remoteAddressesException[0] = new IOException();
		assertToString("[local=not-bound remote=unknown]", ctx.toString(sc));
	}
}
