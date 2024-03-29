/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2021 SNF4J contributors
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
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.snf4j.core.DatagramSession;
import org.snf4j.core.StreamSession;
import org.snf4j.core.allocator.DefaultAllocator;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.ISessionConfig;

public class AbstractStreamHandlerTest {

	Object readMsg;
	
	@Test
	public void testAll() {
		TestHandler h = new TestHandler();
		ISession s = new StreamSession(new TestHandler());
		
		h.setSession(s);
		assertTrue(h.getSession() == s);
		
		s = new DatagramSession(new TestDatagramHandler());
		try {
			h.setSession(s);
			fail("exception should be thrown");
		}
		catch (IllegalArgumentException e) {
		}

		assertTrue(DefaultSessionStructureFactory.DEFAULT == h.getFactory());
		assertNull(h.getFactory().getExecutor());
		assertNull(h.getFactory().getAttributes());
		assertTrue(DefaultAllocator.DEFAULT == h.getFactory().getAllocator());
		
		DefaultSessionConfig config = new DefaultSessionConfig();
		StreamHandler handler = new StreamHandler(config);
		assertTrue(config == handler.getConfig());
		assertNull(handler.getName());
		handler = new StreamHandler("Name", config);
		assertTrue(config == handler.getConfig());
		assertEquals("Name", handler.getName());
	}

	@Test
	public void testRead() {
		AbstractStreamHandler h = new AbstractStreamHandler() {

			@Override
			public void read(Object msg) {
				readMsg = msg;
			}			
		};
		
		byte[] b = new byte[3];
		ByteBuffer bb = ByteBuffer.allocate(10);
		h.read(b);
		assertTrue(b == readMsg);
		h.read(bb);
		assertTrue(bb == readMsg);
		
	}	
	
	static class StreamHandler extends AbstractStreamHandler {

		 protected StreamHandler(ISessionConfig config) {
				super(config);
		}

		protected StreamHandler(String name, ISessionConfig config) {
			super(name, config);
		}
		 
		@Override
		public void read(Object msg) {
		}
		
	}
}
