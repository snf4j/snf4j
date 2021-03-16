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
package org.snf4j.core.future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.snf4j.core.TestSession;

public class AbortableThresholdFutureTest extends ThresholdFutureTest {
	
	ThresholdFuture<Void> future(DataFuture<Void> df, long threshold) {
		return new AbortableThresholdFuture<Void>(df, threshold);
	}

	@Test
	public void testIsDone() {
		super.testIsDone();
	}
	
	@Test
	public void testIsCancelled() {
		super.testIsCancelled();
	}	
	
	@Test
	public void testIsFailed() {
		super.testIsFailed();
	}	
	
	@Test
	public void testSyncIsSuccessful() throws Exception {
		super.testSyncIsSuccessful();
	}
	
	@Test
	public void testSyncIsCancelled() throws Exception {
		super.testSyncIsCancelled();
	}	
	
	@Test
	public void testSyncIsFailed() throws Exception {
		super.testSyncIsFailed();
	}	

	void assertFuture(String expected, Throwable expectedCause, IFuture<Void> f) {
		assertEquals(expected.indexOf('C') != -1, f.isCancelled());
		assertEquals(expected.indexOf('D') != -1, f.isDone());
		assertEquals(expected.indexOf('F') != -1, f.isFailed());
		assertEquals(expected.indexOf('S') != -1, f.isSuccessful());
		if (expectedCause == null) {
			assertNull(f.cause());
		}
		else {
			assertNotNull(f.cause());
			assertTrue(expectedCause == f.cause());
		}
	}
	
	@Test
	public void testAbort() throws Exception {
		DataFuture<Void> df = new DataFuture<Void>(new TestSession());
		AbortableThresholdFuture<Void> f = new AbortableThresholdFuture<Void>(df, 100);
		final Exception e = new Exception();
		
		assertFuture("", null, f);
		f.abort(null);
		assertFuture("CD", null, f);
		f.abort(e);
		assertFuture("CD", null, f);
		df.add(200);
		assertFuture("CD", null, f);
		
		f = new AbortableThresholdFuture<Void>(df, 1000);
		assertFuture("", null, f);
		f.abort(e);
		assertFuture("FD", e, f);
		f.abort(null);
		assertFuture("FD", e, f);
		df.add(1000);
		assertFuture("FD", e, f);

		f = new AbortableThresholdFuture<Void>(df, 2000);
		assertFuture("", null, f);
		df.add(1000);
		assertFuture("SD", null, f);
		f.abort(null);
		assertFuture("CD", null, f);
		
		final AbortableThresholdFuture<Void> f2 = new AbortableThresholdFuture<Void>(df, 3000);
		assertFuture("", null, f2);
		update(new Runnable() {

			@Override
			public void run() {
				f2.abort(null);;
			}
		});
		assertFuture("", null, f2);
		f2.await(50);
		assertFuture("", null, f2);
		long t = System.currentTimeMillis();
		f2.await(1000);
		t = System.currentTimeMillis() - t;
		assertTrue(t < 100);
		assertFuture("CD", null, f2);

		final AbortableThresholdFuture<Void> f3 = new AbortableThresholdFuture<Void>(df, 3000);
		assertFuture("", null, f3);
		update(new Runnable() {

			@Override
			public void run() {
				f3.abort(e);;
			}
		});
		assertFuture("", null, f3);
		f3.await(50);
		assertFuture("", null, f3);
		t = System.currentTimeMillis();
		f3.await(1000);
		t = System.currentTimeMillis() - t;
		assertTrue(t < 100);
		assertFuture("FD", e, f3);
	
	}
}
