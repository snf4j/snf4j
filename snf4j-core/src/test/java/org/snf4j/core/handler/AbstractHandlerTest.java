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

import java.nio.ByteBuffer;

import org.junit.Test;
import org.snf4j.core.StreamSession;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISessionConfig;

public class AbstractHandlerTest {

	Object readMsg;
	
	@Test
	public void testAll() {
		TestHandler h = new TestHandler();
		TestHandler h2 = new TestHandler("HandlerName");
		
		assertEquals("HandlerName", h2.getName());
		assertNull(h.getName());
		assertNull(h.getSession());
		h.exception(null);
		h.timer((Object)null);
		h.timer((Runnable)null);
		assertTrue(DefaultSessionStructureFactory.DEFAULT == h.getFactory());
		assertTrue(h.getConfig() instanceof DefaultSessionConfig);
		assertTrue(h.getConfig() == h.getConfig());
		
		StreamSession s = new StreamSession(h);
		StreamSession s2 = new StreamSession(h2);
		assertTrue(s == h.getSession());
		assertTrue(h == s.getHandler());
		
		h.setSession(s2);
		assertTrue(s2 == h.getSession());
	}
	
	@Test
	public void testConstructor() {
		AbstractHandler h = new AbstractHandler("n1", null) {
			@Override
			public void read(Object msg) {
			}
		};
		assertTrue(h.getConfig().getClass() == DefaultSessionConfig.class);
		assertEquals("n1", h.getName());
		
		DefaultSessionConfig config = new DefaultSessionConfig();
		h = new AbstractHandler(null, config) {
			@Override
			public void read(Object msg) {
			}
		};
		assertTrue(h.getConfig() == config);
		assertNull(h.getName());
		
		h = new AbstractHandler((ISessionConfig)null) {
			@Override
			public void read(Object msg) {
			}
		};
		assertTrue(h.getConfig().getClass() == DefaultSessionConfig.class);

		h = new AbstractHandler(config) {
			@Override
			public void read(Object msg) {
			}
		};
		assertTrue(h.getConfig() == config);
	}
	
	@Test
	public void testAvailable() {
		TestHandler h = new TestHandler();

		ByteBuffer b = ByteBuffer.allocate(100);
		assertEquals(0, h.available(b, false));
		b.put((byte) 44);
		assertEquals(1, h.available(b, false));
		b.put((byte) 55);
		assertEquals(2, h.available(b, false));
		b.flip();
		assertEquals(2, h.available(b, true));
		
		byte[] a = new byte[100];
		assertEquals(0, h.available(a, 0, 0));
		assertEquals(6, h.available(a, 1, 6));
		
	}
	
	@Test
	public void testRead() {
		AbstractHandler h = new AbstractHandler() {

			@Override
			public void read(Object msg) {
				readMsg = msg;
			}
			
		};

		byte[] b = new byte[6];
		h.read(b);
		assertTrue(b == readMsg);
	}
}
