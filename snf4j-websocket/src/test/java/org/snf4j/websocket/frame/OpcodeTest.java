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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OpcodeTest {

	@Test
	public void testFindByValue() {
		assertTrue(Opcode.CONTINUATION == Opcode.findByValue(0));
		assertTrue(Opcode.TEXT == Opcode.findByValue(1));
		assertTrue(Opcode.BINARY == Opcode.findByValue(2));
		assertTrue(Opcode.CLOSE == Opcode.findByValue(8));
		assertTrue(Opcode.PING == Opcode.findByValue(9));
		assertTrue(Opcode.PONG == Opcode.findByValue(10));
		assertNull(Opcode.findByValue(3));
		assertNull(Opcode.findByValue(4));
		assertNull(Opcode.findByValue(5));
		assertNull(Opcode.findByValue(6));
		assertNull(Opcode.findByValue(7));
		assertNull(Opcode.findByValue(11));
		assertNull(Opcode.findByValue(12));
		assertNull(Opcode.findByValue(13));
		assertNull(Opcode.findByValue(14));
		assertNull(Opcode.findByValue(15));
	}

	@Test(expected=ArrayIndexOutOfBoundsException.class)
	public void testFindByNegativeValue() {
		Opcode.findByValue(-1);
	}

	@Test(expected=ArrayIndexOutOfBoundsException.class)
	public void testFindByTooBigValue() {
		Opcode.findByValue(16);
	}
	
}
