/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017 SNF4J contributors
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.snf4j.core.StreamSession;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.session.DefaultSessionConfig;

public class AbstractHandlerTest {

	@Test
	public void testAll() {
		TestHandler h = new TestHandler();
		TestHandler h2 = new TestHandler("HandlerName");
		
		assertEquals("HandlerName", h2.getName());
		assertNull(h.getName());
		assertNull(h.getSession());
		assertFalse(h.exception(null));
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
}
