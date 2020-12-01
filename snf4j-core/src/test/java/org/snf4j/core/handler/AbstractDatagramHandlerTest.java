/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2020 SNF4J contributors
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
package org.snf4j.core.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.junit.Test;
import org.snf4j.core.allocator.DefaultAllocator;
import org.snf4j.core.factory.DefaultSessionStructureFactory;

public class AbstractDatagramHandlerTest {
	
	SocketAddress readAddr;
	
	Object readMsg;
	
	@Test
	public void testAll() {
		TestDatagramHandler h = new TestDatagramHandler();
		TestDatagramHandler h2 = new TestDatagramHandler("HandlerName");
		
		assertNull(h.getName());
		assertEquals("HandlerName", h2.getName());
		assertTrue(DefaultSessionStructureFactory.DEFAULT == h.getFactory());
		assertNull(h.getFactory().getExecutor());
		assertNull(h.getFactory().getAttributes());
		assertTrue(DefaultAllocator.DEFAULT == h.getFactory().getAllocator());
		h.event(null, null, -1);
	}
	
	@Test
	public void testRead() {
		AbstractDatagramHandler h = new AbstractDatagramHandler() {

			@Override
			public void read(SocketAddress remoteAddress, Object msg) {
				readAddr = remoteAddress;
				readMsg = msg;
			}

			@Override
			public void read(Object msg) {
				readAddr = null;
				readMsg = msg;
			}
		};
		
		SocketAddress a = new InetSocketAddress(555);
		byte[] b = new byte[3];
		ByteBuffer bb = ByteBuffer.allocate(10);
		readAddr = a;
		h.read(b);
		assertNull(readAddr);
		assertTrue(b == readMsg);
		readAddr = a;
		h.read(bb);
		assertNull(readAddr);
		assertTrue(bb == readMsg);
		
		b = new byte[6];
		h.read(a, b);
		assertTrue(readAddr == a);
		assertTrue(b == readMsg);
		readAddr = null;
		h.read(a, bb);
		assertTrue(readAddr == a);
		assertTrue(bb == readMsg);
		
	}
}
