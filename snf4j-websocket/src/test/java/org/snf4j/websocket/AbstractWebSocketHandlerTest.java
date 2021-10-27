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
package org.snf4j.websocket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Test;

public class AbstractWebSocketHandlerTest {

	@Test
	public void testAll() throws Exception {
		Handler h = new Handler();
		URI uri = new URI("http://snf4j.org");
		
		assertNull(h.getName());
		assertTrue(h.getConfig().getClass() == DefaultWebSocketSessionConfig.class);
		assertNull(h.getConfig().getRequestUri());
		
		h = new Handler("Name1");
		assertEquals("Name1", h.getName());
		assertTrue(h.getConfig().getClass() == DefaultWebSocketSessionConfig.class);
		assertNull(h.getConfig().getRequestUri());
		
		h = new Handler(uri);
		assertNull(h.getName());
		assertTrue(h.getConfig().getClass() == DefaultWebSocketSessionConfig.class);
		assertTrue(h.getConfig().getRequestUri() == uri);

		h = new Handler("Name2", uri);
		assertEquals("Name2", h.getName());
		assertTrue(h.getConfig().getClass() == DefaultWebSocketSessionConfig.class);
		assertTrue(h.getConfig().getRequestUri() == uri);
		
		DefaultWebSocketSessionConfig config = new DefaultWebSocketSessionConfig();
		h = new Handler(config);
		assertNull(h.getName());
		assertTrue(config == h.getConfig());
		
		h = new Handler("Name3", config);
		assertEquals("Name3", h.getName());
		assertTrue(config == h.getConfig());
		
	}
	
	static class Handler extends AbstractWebSocketHandler {

		protected Handler() {
		}

		protected Handler(URI requestUri) {
			super(requestUri);
		}

		protected Handler(String name) {
			super(name);
		}

		protected Handler(String name, URI requestUri) {
			super(name, requestUri);
		}
		
		protected Handler(IWebSocketSessionConfig config) {
			super(config);
		}

		protected Handler(String name, IWebSocketSessionConfig config) {
			super(name, config);
		}
		
		@Override
		public void read(Object msg) {
		}
		
	}
}
