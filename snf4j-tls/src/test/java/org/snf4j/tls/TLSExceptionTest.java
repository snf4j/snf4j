/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.AlertDescription;
import org.snf4j.tls.alert.AlertLevel;

public class TLSExceptionTest {

	@Test
	public void testAll() {
		Alert a = new Alert("xxx", AlertLevel.FATAL, AlertDescription.CERTIFICATE_EXPIRED) {
			private static final long serialVersionUID = 1L;
		};
		Exception ex = new Exception("ex");
		
		TLSException e;
		
		e = new TLSException();
		assertNull(e.getAlert());
		assertNull(e.getMessage());

		e = new TLSException(ex);
		assertNull(e.getAlert());
		assertEquals("java.lang.Exception: ex", e.getMessage());
		assertSame(ex, e.getCause());
		
		e = new TLSException("test", ex);
		assertNull(e.getAlert());
		assertEquals("test", e.getMessage());
		assertSame(ex, e.getCause());
		
		e = new TLSException(a);
		assertSame(a, e.getAlert());
		assertTrue(e.getMessage().endsWith(": xxx"));
		assertSame(a, e.getCause());
		e = new TLSException((Exception)a);
		assertSame(a, e.getAlert());
		assertTrue(e.getMessage().endsWith(": xxx"));
		assertSame(a, e.getCause());
		
		e = new TLSException("test", a);
		assertSame(a, e.getAlert());
		assertEquals("test", e.getMessage());
		assertSame(a, e.getCause());
		e = new TLSException("test", (Exception)a);
		assertSame(a, e.getAlert());
		assertEquals("test", e.getMessage());
		assertSame(a, e.getCause());		
	}
}
