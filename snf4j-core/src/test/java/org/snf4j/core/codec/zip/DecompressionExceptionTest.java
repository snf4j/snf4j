/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020 SNF4J contributors
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
package org.snf4j.core.codec.zip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DecompressionExceptionTest {

	@Test
	public void testConstructor() {
		DecompressionException e = new DecompressionException();
		assertNull(e.getMessage());
		assertNull(e.getCause());
		
		e = new DecompressionException("Test1");
		assertEquals("Test1", e.getMessage());
		assertNull(e.getCause());
		
		Exception c = new Exception("Test2");
		e = new DecompressionException("Test1", c);
		assertEquals("Test1", e.getMessage());
		assertTrue(c == e.getCause());
	
		e = new DecompressionException(c);
		assertEquals("java.lang.Exception: Test2", e.getMessage());
		assertTrue(c == e.getCause());
		
	}

}
