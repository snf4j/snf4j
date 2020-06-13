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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.TestSession;

public class CompletedFutureTest {

	TestSession session;
	SessionFuturesController futures;
	
	@Before
	public void before() {
		session = new TestSession();
		futures = new SessionFuturesController(session);
	}
	
	void assertFuture(IFuture<?> future, FutureState state, Throwable cause) throws Exception {
		boolean successful = state == FutureState.SUCCESSFUL;
		boolean failed = state == FutureState.FAILED;
		boolean cancelled = state == FutureState.CANCELLED;

		assertTrue(session == future.getSession());
		assertFalse(future.cancel(false));
		assertFalse(future.cancel(true));
		assertTrue(future.isSuccessful() == successful);
		assertTrue(future.isFailed() == failed);
		assertTrue(future.isCancelled() == cancelled);
		assertTrue(future.isDone());
		long t0 = System.currentTimeMillis();
		assertNull(future.getNow());
		assertNull(future.get());
		assertNull(future.get(100000000, TimeUnit.MILLISECONDS));
		assertTrue(future == future.await());
		assertTrue(future == future.await(100000000));
		assertTrue(future == future.await(100000000, TimeUnit.MILLISECONDS));
		assertTrue(future == future.awaitUninterruptibly());
		assertTrue(future == future.awaitUninterruptibly(100000000));
		assertTrue(future == future.awaitUninterruptibly(100000000, TimeUnit.MILLISECONDS));
		assertTrue(future == future.sync());
		assertTrue(future == future.sync(100000000));
		assertTrue(future == future.sync(100000000, TimeUnit.MILLISECONDS));
		assertTrue(future == future.syncUninterruptibly());
		assertTrue(future == future.syncUninterruptibly(100000000));
		assertTrue(future == future.syncUninterruptibly(100000000, TimeUnit.MILLISECONDS));
		long t = System.currentTimeMillis() - t0;
		assertTrue(t < 100);
		assertTrue(cause == future.cause());
	}
	
	@Test
	public void testCompletedFuture() throws Exception {
		assertFuture(new CompletedFuture<Void>(session, FutureState.SUCCESSFUL), FutureState.SUCCESSFUL, null);
		try {
			new CompletedFuture<Void>(session, null);
			fail("exception not thrown");
		}
		catch (IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testSuccessfulFuture() throws Exception {
		assertFuture(futures.getSuccessfulFuture(), FutureState.SUCCESSFUL, null);
		assertFuture(new SuccessfulFuture<Void>(session), FutureState.SUCCESSFUL, null);
	}

	@Test
	public void testCancelledFuture() throws Exception {
		assertFuture(futures.getCancelledFuture(), FutureState.CANCELLED, null);
		assertFuture(new CancelledFuture<Void>(session), FutureState.CANCELLED, null);
	}

	@Test
	public void testFailedFuture() throws Exception {
		Exception cause = new Exception();
		assertFuture(futures.getFailedFuture(cause), FutureState.FAILED, cause);
		assertFuture(new FailedFuture<Void>(session,cause), FutureState.FAILED, cause);
	}
}
