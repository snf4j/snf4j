/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2019 SNF4J contributors
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.core.session.ISession;

public class AbstractDatagramHandlerTest {
	
	@Test
	public void testSetSession() {
		TestDatagramHandler h = new TestDatagramHandler();
		
		try {
			h.setSession(new TestSession());
			fail("Exception not thrown");
		}
		catch (IllegalArgumentException e) {
			assertEquals("session is not an instance of IDatagramSession", e.getMessage());
		}
		
		TestDummyDatagramSession s = new TestDummyDatagramSession();
		h.setSession(s);
		assertTrue(s == h.getSession());
		h.setSession(s);
		TestDummyDatagramSession s2 = new TestDummyDatagramSession();
		
		h.setSession(s2);
		assertTrue(s2 == h.getSession());
		
		ISession s3 = s;
		
		h = new TestDatagramHandler();
		h.setSession(s3);
		assertTrue(s3 == h.getSession());
	}
}
