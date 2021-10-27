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
package org.snf4j.websocket.frame;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.snf4j.core.ICloseControllingException.CloseType;

public class InvalidFrameExceptionTest {

	@Test
	public void testAll() {
		Throwable t = new Throwable("T");
		InvalidFrameException e = new InvalidFrameException("Text1", t, false);
		
		assertEquals("Text1", e.getMessage());
		assertEquals(CloseType.DEFAULT, e.getCloseType());
		assertTrue(e == e.getClosingCause());
		assertTrue(t == e.getCause());
		
		e = new InvalidFrameException("Text2", t, true);
		assertEquals("Text2", e.getMessage());
		assertEquals(CloseType.GENTLE, e.getCloseType());
		assertTrue(e == e.getClosingCause());
		assertTrue(t == e.getCause());

		e = new InvalidFrameException(t, false);
		assertEquals("T", e.getMessage());
		assertEquals(CloseType.DEFAULT, e.getCloseType());
		assertTrue(e == e.getClosingCause());
		assertTrue(t == e.getCause());

		e = new InvalidFrameException(null, false);
		assertNull(e.getMessage());
		assertEquals(CloseType.DEFAULT, e.getCloseType());
		assertTrue(e == e.getClosingCause());
		assertNull(e.getCause());
		
		e = new InvalidFrameException(t, true);
		assertEquals("T", e.getMessage());
		assertEquals(CloseType.GENTLE, e.getCloseType());
		assertTrue(e == e.getClosingCause());
		assertTrue(t == e.getCause());
		
		e = new InvalidFrameException("Text2", t);
		assertEquals("Text2", e.getMessage());
		assertEquals(CloseType.GENTLE, e.getCloseType());
		assertTrue(e == e.getClosingCause());
		assertTrue(t == e.getCause());
		
		e = new InvalidFrameException("Text3");
		assertEquals("Text3", e.getMessage());
		assertEquals(CloseType.GENTLE, e.getCloseType());
		assertTrue(e == e.getClosingCause());
		assertNull(e.getCause());
		
		e = new InvalidFrameException(t);
		assertEquals("T", e.getMessage());
		assertEquals(CloseType.GENTLE, e.getCloseType());
		assertTrue(e == e.getClosingCause());
		assertTrue(t == e.getCause());

		e = new InvalidFrameException((Throwable)null);
		assertNull(e.getMessage());
		assertEquals(CloseType.GENTLE, e.getCloseType());
		assertTrue(e == e.getClosingCause());
		assertNull(e.getCause());
		
		e = new InvalidFrameException();
		assertNull(e.getMessage());
		assertEquals(CloseType.GENTLE, e.getCloseType());
		assertTrue(e == e.getClosingCause());
		assertNull(e.getCause());
	}
}
