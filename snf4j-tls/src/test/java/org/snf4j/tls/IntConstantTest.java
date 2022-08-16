/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022 SNF4J contributors
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IntConstantTest {

	@Test
	public void testValue() {
		IntConstant v = new IntConstant("xxx", 4);
		
		assertEquals("xxx", v.name());
		assertEquals(4, v.value());
		assertTrue(v.isKnown());

		v = new IntConstant("unknown", 4);
		assertEquals("unknown", v.name());
		assertEquals(4, v.value());
		assertTrue(v.isKnown());

		v = new IntConstant(4);
		assertEquals("unknown", v.name());
		assertEquals(4, v.value());
		assertFalse(v.isKnown());
	}
	
	@Test
	public void testHashCode() {
		IntConstant v = new IntConstant("xxx", 4);
		assertEquals(Integer.hashCode(4), v.hashCode());
		v = new IntConstant("xxx", 0);
		assertEquals(Integer.hashCode(0), v.hashCode());
		v = new IntConstant("xxx", -1);
		assertEquals(Integer.hashCode(-1), v.hashCode());
	}
	
	@Test
	public void testEquals() {
		IntConstant v = new IntConstant("xxx", 45);
		
		assertTrue(v.equals(new IntConstant(45)));
		assertTrue(v.equals(new IntConstant("x",45)));
		assertTrue(v.equals(new IntConstant("xxx",45)));
		
		assertFalse(v.equals(new IntConstant("xxx",46)));
		assertFalse(v.equals(new Integer(45)));
		assertFalse(v.equals(null));
	}
}
