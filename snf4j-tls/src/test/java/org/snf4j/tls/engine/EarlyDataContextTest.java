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
package org.snf4j.tls.engine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EarlyDataContextTest {

	@Test
	public void testAll() {
		EarlyDataContext ctx = new EarlyDataContext(100);
		assertSame(EarlyDataState.IN_PROGRESS, ctx.getState());
		ctx.complete();
		assertSame(EarlyDataState.COMPLETED, ctx.getState());
		ctx.reject();
		assertSame(EarlyDataState.COMPLETED, ctx.getState());
		assertFalse(ctx.isSizeLimitExceeded());
		ctx.incProcessedBytes(99);
		assertFalse(ctx.isSizeLimitExceeded());
		ctx.incProcessedBytes(1);
		assertFalse(ctx.isSizeLimitExceeded());
		ctx.incProcessedBytes(1);
		assertTrue(ctx.isSizeLimitExceeded());
		
		ctx = new EarlyDataContext(true, 100);
		assertSame(EarlyDataState.REJECTED, ctx.getState());
		ctx.complete();
		assertSame(EarlyDataState.REJECTED, ctx.getState());
		ctx.reject();
		assertSame(EarlyDataState.REJECTED, ctx.getState());

		ctx = new EarlyDataContext(false, 100);
		assertSame(EarlyDataState.IN_PROGRESS, ctx.getState());
		ctx.reject();
		assertSame(EarlyDataState.REJECTED, ctx.getState());
		ctx.complete();
		assertSame(EarlyDataState.REJECTED, ctx.getState());		
	}
}
