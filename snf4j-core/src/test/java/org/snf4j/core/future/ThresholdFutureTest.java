/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2018 SNF4J contributors
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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.snf4j.core.TestSession;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ThresholdFutureTest {

	void update(final Runnable r) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				r.run();
			}
			
		}).start();
	}
	
	@Test
	public void testIsDone() {
		DataFuture<Void> df = new DataFuture<Void>(new TestSession());
		ThresholdFuture<Void> f = new ThresholdFuture<Void>(df, 100);
		
		assertFalse(f.isCancelled());
		assertFalse(f.isDone());
		assertFalse(f.isFailed());
		assertFalse(f.isSuccessful());
		
		df.add(99);
		assertFalse(f.isCancelled());
		assertFalse(f.isDone());
		assertFalse(f.isFailed());
		assertFalse(f.isSuccessful());

		df.add(1);
		assertFalse(f.isCancelled());
		assertTrue(f.isDone());
		assertFalse(f.isFailed());
		assertTrue(f.isSuccessful());
		
		df.cancel();
		assertFalse(f.isCancelled());
		assertTrue(f.isDone());
		assertFalse(f.isFailed());
		assertTrue(f.isSuccessful());
	}
	
	@Test
	public void testIsCancelled() {
		DataFuture<Void> df = new DataFuture<Void>(new TestSession());
		ThresholdFuture<Void> f = new ThresholdFuture<Void>(df, 100);
		
		df.cancel();
		assertTrue(f.isCancelled());
		assertTrue(f.isDone());
		assertFalse(f.isFailed());
		assertFalse(f.isSuccessful());
	}	

	@Test
	public void testIsFailed() {
		DataFuture<Void> df = new DataFuture<Void>(new TestSession());
		ThresholdFuture<Void> f = new ThresholdFuture<Void>(df, 100);
		Exception e = new Exception("Test1");

		assertNull(f.cause());
		df.failure(e);
		assertTrue(f.cause() == e);
		assertFalse(f.isCancelled());
		assertTrue(f.isDone());
		assertTrue(f.isFailed());
		assertFalse(f.isSuccessful());
		
		df.add(100);
		assertNull(f.cause());
	}
	
	@Test
	public void testSyncIsSuccessful() throws Exception {
		final DataFuture<Void> df = new DataFuture<Void>(new TestSession());
		ThresholdFuture<Void> f = new ThresholdFuture<Void>(df, 100);
		
		update(new Runnable() {

			@Override
			public void run() {
				df.add(100);
			}
		});
		f.sync(500);
	}

	@Test
	public void testSyncIsCancelled() throws Exception {
		final DataFuture<Void> df = new DataFuture<Void>(new TestSession());
		ThresholdFuture<Void> f = new ThresholdFuture<Void>(df, 100);
		
		update(new Runnable() {

			@Override
			public void run() {
				df.cancel();
			}
		});
		try {
			f.sync(500);
			fail("Exception should be thrown");
		}
		catch (CancellationException e) {
		}
	}

	@Test
	public void testSyncIsFailed() throws Exception {
		final DataFuture<Void> df = new DataFuture<Void>(new TestSession());
		final Exception e = new Exception("Test2");
		ThresholdFuture<Void> f = new ThresholdFuture<Void>(df, 100);
		
		update(new Runnable() {

			@Override
			public void run() {
				df.failure(e);
			}
		});
		try {
			f.sync(500);
			fail("Exception should be thrown");
		}
		catch (ExecutionException ex) {
			assertTrue(ex.getCause() == e);
		}
	}
}
