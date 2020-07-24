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
package org.snf4j.core.timer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DefaultTimeoutModelTest {
	
	@Test
	public void testAll() {
		DefaultTimeoutModel m = new DefaultTimeoutModel(1, 8);
		
		assertEquals(1, m.next());
		assertEquals(2, m.next());
		assertEquals(4, m.next());
		assertEquals(8, m.next());
		assertEquals(8, m.next());
		m.reset();
		assertEquals(1, m.next());
		assertEquals(2, m.next());
		
		m = new DefaultTimeoutModel(1, 3);
		assertEquals(1, m.next());
		assertEquals(2, m.next());
		assertEquals(3, m.next());
		assertEquals(3, m.next());
		
		m = new DefaultTimeoutModel();
		assertEquals(1000, m.next());
		assertEquals(2000, m.next());
		assertEquals(4000, m.next());
		assertEquals(8000, m.next());
		assertEquals(16000, m.next());
		assertEquals(32000, m.next());
		assertEquals(60000, m.next());
		assertEquals(60000, m.next());
		
	}
}
