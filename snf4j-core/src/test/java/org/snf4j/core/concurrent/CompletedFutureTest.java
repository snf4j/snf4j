package org.snf4j.core.concurrent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.TestSession;

public class CompletedFutureTest {

	TestSession session;
	SessionFutures futures;
	
	@Before
	public void before() {
		session = new TestSession();
		futures = new SessionFutures(session);
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
	public void testSuccessfulFuture() throws Exception {
		assertFuture(futures.getSuccessfulFuture(), FutureState.SUCCESSFUL, null);
	}

	@Test
	public void testCancelledFuture() throws Exception {
		assertFuture(futures.getCancelledFuture(), FutureState.CANCELLED, null);
	}

	@Test
	public void testFailedFuture() throws Exception {
		Exception cause = new Exception();
		assertFuture(futures.getFailedFuture(cause), FutureState.FAILED, cause);
	}
}
