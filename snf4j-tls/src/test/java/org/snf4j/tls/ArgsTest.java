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
import static org.junit.Assert.fail;

import org.junit.Test;

public class ArgsTest {

	@Test
	public void testCheckNull() {
		Args.checkNull("", "name");
		try {
			Args.checkNull(null, "name");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("name is null", e.getMessage());		
		}
	}
	
	@Test
	public void testCheckRange() {
		Args.checkRange(100, 100, 1000, "name");
		Args.checkRange(500, 100, 1000, "name");
		Args.checkRange(1000, 100, 1000, "name");
		try {
			Args.checkRange(99, 100, 1000, "name");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("name is less than 100", e.getMessage());		
		}
		try {
			Args.checkRange(1001, 100, 1000, "name");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("name is greater than 1000", e.getMessage());		
		}
	}
	
	@Test
	public void testFixedArray() {
		Args.checkFixed(new byte[10], 10, "name");
		try {
			Args.checkFixed(new byte[10], 9, "name");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("name's length is not 9", e.getMessage());		
		}
		try {
			Args.checkFixed(new byte[10], 11, "name");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("name's length is not 11", e.getMessage());		
		}
		try {
			Args.checkFixed(null, 11, "name");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("name is null", e.getMessage());		
		}
	}
	
	@Test
	public void testMax() {
		Args.checkMax(100, 100, "name");
		Args.checkMax(99, 100, "name");
		Args.checkMax(0, 100, "name");
		Args.checkMax(-1, 100, "name");
		try {
			Args.checkMax(100, 99, "name");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("name is greater than 99", e.getMessage());		
		}
	}

	@Test
	public void testMaxArray() {
		Args.checkMax(new byte[100], 100, "name");
		Args.checkMax(new byte[99], 100, "name");
		Args.checkMax(new byte[0], 100, "name");
		try {
			Args.checkMax(new byte[100], 99, "name");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("name's length is greater than 99", e.getMessage());		
		}
		try {
			Args.checkMax(null, 99, "name");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("name is null", e.getMessage());		
		}
	}
}
