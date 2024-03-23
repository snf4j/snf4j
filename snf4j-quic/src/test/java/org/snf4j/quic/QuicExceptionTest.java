/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class QuicExceptionTest {

	@Test
	public void testAll() {
		Exception cause = new Exception();

		QuicException e = new QuicException(TransportError.CONNECTION_REFUSED, "xxx");
		assertSame(TransportError.CONNECTION_REFUSED, e.getTransportError());
		assertEquals(TransportError.CONNECTION_REFUSED.code(), e.getErrorCode());
		assertEquals("xxx", e.getMessage());
		assertNull(e.getCause());
		
		e = new QuicException(TransportError.CONNECTION_REFUSED, "xxx", cause);
		assertSame(TransportError.CONNECTION_REFUSED, e.getTransportError());
		assertEquals(TransportError.CONNECTION_REFUSED.code(), e.getErrorCode());
		assertEquals("xxx", e.getMessage());
		assertSame(cause, e.getCause());

		e = new QuicException(TransportError.CONNECTION_REFUSED, 1000, "xxx");
		assertSame(TransportError.CONNECTION_REFUSED, e.getTransportError());
		assertEquals(1000, e.getErrorCode());
		assertEquals("xxx", e.getMessage());
		assertNull(e.getCause());
		
		e = new QuicException(TransportError.CONNECTION_REFUSED, 1000, "xxx", cause);
		assertSame(TransportError.CONNECTION_REFUSED, e.getTransportError());
		assertEquals(1000, e.getErrorCode());
		assertEquals("xxx", e.getMessage());
		assertSame(cause, e.getCause());
	
	}
}
