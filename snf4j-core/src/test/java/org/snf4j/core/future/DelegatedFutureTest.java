/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.snf4j.core.TestSession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DelegatedFutureTest {

	private DelegatingBlockingFuture<Void> future;
	DataFuture<Void> dataFuture;
	ThresholdFuture<Void> delegateFuture;
	SessionFuturesController futureController;

	
	private void initFutures() {
		futureController = new SessionFuturesController(null);
		future = new DelegatingBlockingFuture<Void>(new TestSession());
		dataFuture = new DataFuture<Void>(new TestSession());
		delegateFuture = new ThresholdFuture<Void>(dataFuture, 100);
	}
	
	void update(final Runnable r, final long delay) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
				}
				r.run();
			}
			
		}).start();
	}

	void setDelegate(final DelegatingBlockingFuture<Void> future, final IFuture<Void> delegate, long delay) {
		update(new Runnable() {

			@Override
			public void run() {
				future.setDelegate(delegate);
			}
		}, delay);
	}

	void setDelegateSilently(final DelegatingBlockingFuture<Void> future, final IFuture<Void> delegate, long delay) {
		update(new Runnable() {

			@Override
			public void run() {
				try {
					Field field = DelegatingBlockingFuture.class.getDeclaredField("delegate");
					field.setAccessible(true);
					field.set(future, delegate);
				}
				catch (Exception e) {}
			}
		}, delay);
	}

	void setSuccessful(final DataFuture<Void> df, long delay) {
		update(new Runnable() {

			@Override
			public void run() {
				df.add(100);
			}
		}, delay);
	}

	void setCancelled(final DataFuture<Void> df, long delay) {
		update(new Runnable() {

			@Override
			public void run() {
				df.cancel();
			}
		}, delay);
	}

	void setFailed(final DataFuture<Void> df, final Throwable cause, long delay) {
		update(new Runnable() {

			@Override
			public void run() {
				df.failure(cause);
			}
		}, delay);
	}
	
	void interrupt(final Thread thread, long delay) {
		update(new Runnable() {

			@Override
			public void run() {
				thread.interrupt();
			}
		}, delay);
	}
	
	void assertTime(long expected, long savedTime) {
		savedTime = System.currentTimeMillis() - savedTime;
		assertTrue("expected " + expected + " but was " + savedTime, savedTime > expected - 10);
		assertTrue("expected " + expected + " but was " + savedTime, savedTime < expected + 10);
	}

	@Test
	public void testAwait() throws Exception {
		
		//not in wait before set
		initFutures();
		future.setDelegate(delegateFuture);
		setSuccessful(dataFuture, 300);
		long t = System.currentTimeMillis();
		future.await();
		assertTime(300, t);

		//in wait before set
		initFutures();
		setDelegate(future, delegateFuture, 100);
		setSuccessful(dataFuture, 400);
		t = System.currentTimeMillis();
		future.await();
		assertTime(400, t);

		//not in wait before set and interrupted
		initFutures();
		future.setDelegate(delegateFuture);
		setSuccessful(dataFuture, 600);
		interrupt(Thread.currentThread(), 200);
		t = System.currentTimeMillis();
		try {
			future.await();
			fail("not interrupted");
		}
		catch (InterruptedException e) {
		}
		assertTime(200, t);

		//in wait before set and interrupted
		initFutures();
		setDelegate(future, delegateFuture, 100);
		interrupt(Thread.currentThread(), 200);
		setSuccessful(dataFuture, 600);
		t = System.currentTimeMillis();
		try {
			future.await();
			fail("not interrupted");
		}
		catch (InterruptedException e) {
		}
		assertTime(200, t);
	}

	@Test
	public void testAwaitForCompletedFutures() throws Exception {
		
		//successful
		initFutures();
		setDelegate(future, futureController.getSuccessfulFuture(), 100);
		long t = System.currentTimeMillis();
		future.await();
		assertTime(100, t);
		assertFuture(future, "DS", null);
		
		//cancelled
		initFutures();
		setDelegate(future, futureController.getCancelledFuture(), 100);
		t = System.currentTimeMillis();
		future.await();
		assertTime(100, t);
		assertFuture(future, "DC", null);

		//failed
		initFutures();
		Exception cause = new Exception();
		setDelegate(future, futureController.getFailedFuture(cause), 100);
		t = System.currentTimeMillis();
		future.await();
		assertTime(100, t);
		assertFuture(future, "DF", cause);
		
		//data written
		initFutures();
		dataFuture.add(100);
		assertFuture(delegateFuture, "DS", null);
		setDelegate(future, delegateFuture, 100);
		t = System.currentTimeMillis();
		future.await();
		assertTime(100, t);
		assertFuture(future, "DS", null);
		
		//data canceled
		initFutures();
		dataFuture.cancel();
		assertFuture(delegateFuture, "DC", null);
		setDelegate(future, delegateFuture, 100);
		t = System.currentTimeMillis();
		future.await();
		assertTime(100, t);
		assertFuture(future, "DC", null);

		//data canceled after set
		initFutures();
		future.setDelegate(delegateFuture);
		setCancelled(dataFuture, 110);
		t = System.currentTimeMillis();
		future.await();
		assertTime(110, t);
		assertFuture(future, "DC", null);
		
		//data failed
		initFutures();
		dataFuture.failure(cause);
		assertFuture(delegateFuture, "DF", cause);
		setDelegate(future, delegateFuture, 100);
		t = System.currentTimeMillis();
		future.await();
		assertTime(100, t);
		assertFuture(future, "DF", cause);

		//data failed after set
		initFutures();
		future.setDelegate(delegateFuture);
		setFailed(dataFuture, cause, 110);
		t = System.currentTimeMillis();
		future.await();
		assertTime(110, t);
		assertFuture(future, "DF", cause);
	}
	
	@Test
	public void testAwaitWithTimeout() throws Exception {
		
		//not in wait before set
		initFutures();
		future.setDelegate(delegateFuture);
		long t = System.currentTimeMillis();
		future.await(100);
		assertTime(100, t);
		assertFalse(future.isDone());
		setSuccessful(dataFuture, 300);
		t = System.currentTimeMillis();
		future.await(1000);
		assertTime(300, t);
		assertTrue(future.isDone());

		//not in wait before set (timed out)
		initFutures();
		future.setDelegate(delegateFuture);
		assertFalse(future.isDone());
		t = System.currentTimeMillis();
		future.await(200);
		assertTime(200, t);
		assertFalse(future.isDone());
		
		//in wait before set
		initFutures();
		t = System.currentTimeMillis();
		future.await(120000, TimeUnit.MICROSECONDS);
		assertTime(120, t);
		setDelegate(future, delegateFuture, 100);
		setSuccessful(dataFuture, 400);
		t = System.currentTimeMillis();
		future.await(1000);
		assertTime(400, t);
		assertTrue(future.isDone());

		//in wait before set (timed out)
		initFutures();
		setDelegate(future, delegateFuture, 100);
		t = System.currentTimeMillis();
		future.await(300, TimeUnit.MILLISECONDS);
		assertTime(300, t);
		assertFalse(future.isDone());
		
		//not in wait before set and interrupted
		initFutures();
		future.setDelegate(delegateFuture);
		setSuccessful(dataFuture, 600);
		interrupt(Thread.currentThread(), 200);
		t = System.currentTimeMillis();
		try {
			future.await(1000);
			fail("not interrupted");
		}
		catch (InterruptedException e) {
		}
		assertTime(200, t);
		assertFalse(future.isDone());
		future.await();
		assertTime(600, t);
		assertTrue(future.isDone());
		
		//in wait before set and interrupted
		initFutures();
		setDelegate(future, delegateFuture, 100);
		interrupt(Thread.currentThread(), 200);
		setSuccessful(dataFuture, 600);
		t = System.currentTimeMillis();
		try {
			future.await(1000);
			fail("not interrupted");
		}
		catch (InterruptedException e) {
		}
		assertTime(200, t);
		assertFalse(future.isDone());
		future.await();
		assertTime(600, t);
		assertTrue(future.isDone());
	}
	
	@Test
	public void testAwaitUninterruptibly() throws Exception {

		//not in wait before set
		initFutures();
		future.setDelegate(delegateFuture);
		setSuccessful(dataFuture, 300);
		long t = System.currentTimeMillis();
		future.awaitUninterruptibly();
		assertTime(300, t);
		assertFalse(Thread.interrupted());

		//in wait before set
		initFutures();
		setDelegate(future, delegateFuture, 100);
		setSuccessful(dataFuture, 400);
		t = System.currentTimeMillis();
		future.awaitUninterruptibly();
		assertTime(400, t);
		assertFalse(Thread.interrupted());

		//not in wait before set and interrupted
		initFutures();
		future.setDelegate(delegateFuture);
		setSuccessful(dataFuture, 600);
		interrupt(Thread.currentThread(), 200);
		t = System.currentTimeMillis();
		future.awaitUninterruptibly();
		assertTime(600, t);
		assertTrue(Thread.interrupted());

		//in wait before set and interrupted
		initFutures();
		setDelegate(future, delegateFuture, 100);
		interrupt(Thread.currentThread(), 200);
		setSuccessful(dataFuture, 600);
		t = System.currentTimeMillis();
		future.awaitUninterruptibly();
		assertTime(600, t);
		assertTrue(Thread.interrupted());
	}

	@Test
	public void testAwaitUninterruptiblyWithTimeout() throws Exception {

		//not in wait before set
		initFutures();
		future.setDelegate(delegateFuture);
		long t = System.currentTimeMillis();
		future.awaitUninterruptibly(100);
		assertTime(100, t);
		assertFalse(future.isDone());
		setSuccessful(dataFuture, 300);
		t = System.currentTimeMillis();
		future.awaitUninterruptibly(1000);
		assertTime(300, t);
		assertFalse(Thread.interrupted());
		assertTrue(future.isDone());

		//not in wait before set (timed out)
		initFutures();
		future.setDelegate(delegateFuture);
		assertFalse(future.isDone());
		t = System.currentTimeMillis();
		future.awaitUninterruptibly(200);
		assertTime(200, t);
		assertFalse(future.isDone());
		
		//in wait before set
		initFutures();
		t = System.currentTimeMillis();
		future.awaitUninterruptibly(120000, TimeUnit.MICROSECONDS);
		assertTime(120, t);
		setDelegate(future, delegateFuture, 100);
		setSuccessful(dataFuture, 400);
		t = System.currentTimeMillis();
		future.awaitUninterruptibly(1000);
		assertTime(400, t);
		assertTrue(future.isDone());

		//in wait before set (timed out)
		initFutures();
		setDelegate(future, delegateFuture, 100);
		t = System.currentTimeMillis();
		future.awaitUninterruptibly(300, TimeUnit.MILLISECONDS);
		assertTime(300, t);
		assertFalse(future.isDone());

		//not in wait before set and interrupted
		initFutures();
		future.setDelegate(delegateFuture);
		setSuccessful(dataFuture, 600);
		interrupt(Thread.currentThread(), 200);
		t = System.currentTimeMillis();
		future.awaitUninterruptibly(1000);
		assertTime(600, t);
		assertTrue(Thread.interrupted());
		assertTrue(future.isDone());

		//not in wait before set and interrupted (timed out)
		initFutures();
		future.setDelegate(delegateFuture);
		interrupt(Thread.currentThread(), 100);
		t = System.currentTimeMillis();
		future.awaitUninterruptibly(300);
		assertTime(300, t);
		assertTrue(Thread.interrupted());
		assertFalse(future.isDone());
		
		//in wait before set and interrupted
		initFutures();
		setDelegate(future, delegateFuture, 100);
		interrupt(Thread.currentThread(), 200);
		setSuccessful(dataFuture, 400);
		t = System.currentTimeMillis();
		future.awaitUninterruptibly(1000);
		assertTime(400, t);
		assertTrue(Thread.interrupted());
		assertTrue(future.isDone());

		//in wait before set and interrupted (timed out)
		initFutures();
		setDelegate(future, delegateFuture, 100);
		interrupt(Thread.currentThread(), 200);
		t = System.currentTimeMillis();
		future.awaitUninterruptibly(400000, TimeUnit.MICROSECONDS);
		assertTime(400, t);
		assertTrue(Thread.interrupted());
		assertFalse(future.isDone());
	}

	@Test
	public void testAwaitUninterruptiblyWithSettingDelegate() throws Exception {
		
		initFutures();
		setDelegateSilently(future, delegateFuture, 50);
		interrupt(Thread.currentThread(), 100);
		setSuccessful(dataFuture, 200);
		long t = System.currentTimeMillis();
		future.awaitUninterruptibly(400);
		assertTime(200, t);
		assertTrue(Thread.interrupted());
		assertTrue(future.isDone());
		
		initFutures();
		setDelegateSilently(future, delegateFuture, 50);
		interrupt(Thread.currentThread(), 100);
		setSuccessful(dataFuture, 200);
		t = System.currentTimeMillis();
		future.awaitUninterruptibly();
		assertTime(200, t);
		assertTrue(Thread.interrupted());
		assertTrue(future.isDone());

	}
	
	private void assertFuture(IFuture<Void> future, String expected, Throwable expectedCause) {
		assertTrue(future.isDone() == expected.contains("D"));
		assertTrue(future.isSuccessful() == expected.contains("S"));
		assertTrue(future.isCancelled() == expected.contains("C"));
		assertTrue(future.isFailed()  == expected.contains("F"));
		assertTrue(future.cause() == expectedCause);
	}
	
	@Test
	public void testStates() {
		initFutures();
		assertFuture(future, "", null);
		future.setDelegate(futureController.getSuccessfulFuture());
		assertFuture(future, "DS", null);

		initFutures();
		future.setDelegate(futureController.getCancelledFuture());
		assertFuture(future, "DC", null);
		
		initFutures();
		Exception cause = new Exception();
		future.setDelegate(futureController.getFailedFuture(cause));
		assertFuture(future, "DF", cause);
	}
	
	@Test
	public void testSetDelegate() {
		initFutures();
		assertFuture(future, "", null);
		IFuture<Void> f = futureController.getCancelledFuture();
		future.setDelegate(f);
		assertFuture(future, "DC", null);

		//set the same
		future.setDelegate(f);
		assertFuture(future, "DC", null);
		
		//set different
		try {
			future.setDelegate(futureController.getSuccessfulFuture());
			fail("not thrown");
		}
		catch (IllegalStateException e) {
			assertEquals("delegate is already set", e.getMessage());
		}
	}
	
}
