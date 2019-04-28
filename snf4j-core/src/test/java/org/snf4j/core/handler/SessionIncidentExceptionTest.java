/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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

import org.junit.Test;

public class SessionIncidentExceptionTest {

	@Test
	public void testConstructor() {
		SessionIncidentException e = new SessionIncidentException(SessionIncident.SSL_CLOSED_WITHOUT_CLOSE_NOTIFY); 
		assertNull(e.getMessage());
		assertEquals(SessionIncident.SSL_CLOSED_WITHOUT_CLOSE_NOTIFY, e.getIncident());
		
		e = new SessionIncidentException("Test1", SessionIncident.CLOSED_WITHOUT_CLOSE_MESSAGE);
		assertEquals("Test1", e.getMessage());
		assertEquals(SessionIncident.CLOSED_WITHOUT_CLOSE_MESSAGE, e.getIncident());

		Exception cause = new Exception("Cause1");
		e = new SessionIncidentException("Test2", cause, SessionIncident.CLOSED_WITHOUT_CLOSE_MESSAGE);
		assertEquals("Test2", e.getMessage());
		assertEquals(SessionIncident.CLOSED_WITHOUT_CLOSE_MESSAGE, e.getIncident());
		assertTrue(cause == e.getCause());
		
		e = new SessionIncidentException(cause, SessionIncident.CLOSED_WITHOUT_CLOSE_MESSAGE);
		assertEquals(cause.toString(), e.getMessage());
		assertEquals(SessionIncident.CLOSED_WITHOUT_CLOSE_MESSAGE, e.getIncident());
		assertTrue(cause == e.getCause());
	}
}
