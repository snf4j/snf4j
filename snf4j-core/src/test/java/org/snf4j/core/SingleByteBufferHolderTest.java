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
package org.snf4j.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.junit.Test;

public class SingleByteBufferHolderTest {
	
	@Test
	public void testAll() {
		ByteBuffer b = ByteBuffer.allocate(100);
		
		b.put(new byte[] {1,2}).flip();
		SingleByteBufferHolder h = new SingleByteBufferHolder(b);
		assertEquals(2, h.remaining());
		assertTrue(h.hasRemaining());
		assertSame(b, h.toArray()[0]);
		assertEquals(1, h.toArray().length);
		b.get();
		assertEquals(1, h.remaining());
		assertTrue(h.hasRemaining());
		b.get();
		assertEquals(0, h.remaining());
		assertFalse(h.hasRemaining());
		b.clear().flip();
		assertEquals(0, h.remaining());
		assertFalse(h.hasRemaining());
		assertFalse(h.isMessage());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testFailure() {
		new SingleByteBufferHolder(null);
	}
	
}
