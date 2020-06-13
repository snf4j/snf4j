/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2020 SNF4J contributors
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
package org.snf4j.core.future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.snf4j.core.TestSession;

public class DataFutureTest {
	
	@Test
	public void testAll() {
		DataFuture<Void> f = new DataFuture<Void>(new TestSession());
		Exception cause = new Exception();
		
		assertEquals(0, f.size());
		f.add(100);
		assertEquals(100, f.size());
		
		assertFalse(f.cancel(true));
		assertFalse(f.cancel(false));
		assertFalse(f.isDone());
		f.cancel();
		assertTrue(f.isDone());
		assertTrue(f.isCancelled());
		f.cancel();
		
		f.add(50);
		assertEquals(150, f.size());
		f.add(1);
		assertEquals(151, f.size());
		f.add(0);
		assertEquals(151, f.size());
		
		f = new DataFuture<Void>(new TestSession());
		f.failure(cause);
		assertTrue(f.isDone());
		assertTrue(f.isFailed());
		assertTrue(cause == f.cause());
		f.failure(new Exception());
		assertTrue(cause == f.cause());
		
		
	}
}
