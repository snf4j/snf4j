/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2024 SNF4J contributors
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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.TestConfig;
import org.snf4j.core.TestSession;
import org.snf4j.core.handler.SessionEvent;

public class AbstractBlockingFutureTest {

	private final static boolean compensateTime = TestConfig.compensateTime();
	
	SessionFuturesController sf;
	Thread thread;
	
	void fireEvent(final SessionEvent event, final long delay, final long interruptDelay) {
		Runnable r = new Runnable() {

			@Override
			public void run() {
				try {
					if (interruptDelay == -1) {
						Thread.sleep(delay);
					}
					else {
						Thread.sleep(interruptDelay);
						thread.interrupt();
						if (delay != -1) {
							Thread.sleep(delay - interruptDelay);
						}
					}
				} catch (InterruptedException e) {
				}
				
				if (delay != -1) {
					sf.event(event);
				}
			}
		};
		new Thread(r).start();
	}
	
	void fireEvent(final SessionEvent event, final long delay) {
		fireEvent(event, delay, -1);
	}
	
	@Before
	public void before() {
		sf = new SessionFuturesController(new TestSession());
	}
	
	void assertTime(long expected, long time) {
		time = System.currentTimeMillis() - time;
		assertTrue("expected "+expected+" but was " + time, 
				time > expected - 50 && 
				time < expected + (compensateTime ? 150 : 50));
	}
	
	@Test
	public void testAwait() throws Exception {
		IFuture<Void> f = sf.getCreateFuture();
		thread = Thread.currentThread();
		long t;

		try {
			f.await(-1);
			fail("Illegal argument not thrown");
		}
		catch (IllegalArgumentException e) {
		}
		
		//no interrupt
		t = System.currentTimeMillis();
		assertFalse(f.await(400).isDone());
		assertTime(400, t);
		t = System.currentTimeMillis();
		assertFalse(f.await(600000, TimeUnit.MICROSECONDS).isDone());
		assertTime(600, t);
		fireEvent(SessionEvent.CREATED, 500);
		t = System.currentTimeMillis();
		assertTrue(f.await().isDone());
		assertTime(500, t);
		
		//interrupt before await
		f = sf.getOpenFuture();
		Thread.currentThread().interrupt();
		try {
			t = System.currentTimeMillis();
			f.await(1000);
			fail("no interrupted");
		}
		catch (InterruptedException e) {
		}
		assertTime(0, t);

		//interrupt
		f = sf.getOpenFuture();
		fireEvent(SessionEvent.OPENED, -1, 300);
		try {
			f.await();
			fail("not interrupted");
		}
		catch (InterruptedException e) {
		}
		assertFalse(Thread.interrupted());
		fireEvent(SessionEvent.OPENED, -1, 300);
		try {
			t = System.currentTimeMillis();
			f.await(1000);
			fail("not interrupted");
		}
		catch (InterruptedException e) {
		}
		assertTime(300,t);
		assertFalse(Thread.interrupted());
		fireEvent(SessionEvent.OPENED, -1, 300);
		try {
			t = System.currentTimeMillis();
			f.await(1000000, TimeUnit.MICROSECONDS);
			fail("not interrupted");
		}
		catch (InterruptedException e) {
		}
		assertTime(300,t);
		assertFalse(Thread.interrupted());
		
	}
	
	@Test
	public void testAwaitUninterruptibly() {
		IFuture<Void> f = sf.getCreateFuture();
		thread = Thread.currentThread();
		long t;

		//no interrupt
		t = System.currentTimeMillis();
		assertFalse(f.awaitUninterruptibly(400).isDone());
		assertTime(400, t);
		t = System.currentTimeMillis();
		assertFalse(f.awaitUninterruptibly(600000, TimeUnit.MICROSECONDS).isDone());
		assertTime(600, t);
		fireEvent(SessionEvent.CREATED, 500);
		t = System.currentTimeMillis();
		assertTrue(f.awaitUninterruptibly().isDone());
		assertTime(500, t);

		//interrupt
		f = sf.getOpenFuture();
		fireEvent(SessionEvent.OPENED, 1000, 300);
		t = System.currentTimeMillis();
		assertTrue(f.awaitUninterruptibly().isDone());
		assertTrue(Thread.interrupted());
		assertTime(1000, t);
		f = sf.getCloseFuture();
		fireEvent(SessionEvent.CLOSED, -1, 300);
		t = System.currentTimeMillis();
		assertFalse(f.awaitUninterruptibly(400).isDone());
		assertTime(400,t);
		assertTrue(Thread.interrupted());
		fireEvent(SessionEvent.CLOSED, 600, 300);
		t = System.currentTimeMillis();
		assertTrue(f.awaitUninterruptibly(1000).isDone());
		assertTime(600,t);
		assertTrue(Thread.interrupted());

		f = sf.getEndFuture();
		fireEvent(SessionEvent.ENDING, -1, 300);
		t = System.currentTimeMillis();
		assertFalse(f.awaitUninterruptibly(400000, TimeUnit.MICROSECONDS).isDone());
		assertTime(400,t);
		assertTrue(Thread.interrupted());
		fireEvent(SessionEvent.ENDING, 600, 300);
		t = System.currentTimeMillis();
		assertTrue(f.awaitUninterruptibly(1000000, TimeUnit.MICROSECONDS).isDone());
		assertTime(600,t);
		assertTrue(Thread.interrupted());
		
	}
	
	@Test
	public void testSyncNoInterrupt() throws InterruptedException, ExecutionException, TimeoutException {
		IFuture<Void> f = sf.getCreateFuture();
		thread = Thread.currentThread();
		Exception cause = new Exception();
		long t;
		
		//successful
		t = System.currentTimeMillis();
		try {
			f.sync(400).isDone();
			fail("timeout not thrown");
		} catch (TimeoutException e1) {
		}
		assertTime(400, t);
		t = System.currentTimeMillis();
		try {
			f.sync(600000, TimeUnit.MICROSECONDS);
			fail("timeout not thrown");
		} catch (TimeoutException e1) {
		}
		assertTime(600, t);
		fireEvent(SessionEvent.CREATED, 500);
		t = System.currentTimeMillis();
		assertTrue(f.sync().isDone());
		assertTime(500, t);
		f = sf.getOpenFuture();
		fireEvent(SessionEvent.OPENED, 300);
		t = System.currentTimeMillis();
		assertTrue(f.sync(1000).isDone());
		assertTime(300, t);
		f = sf.getCloseFuture();
		fireEvent(SessionEvent.CLOSED, 300);
		t = System.currentTimeMillis();
		assertTrue(f.sync(1000000, TimeUnit.MICROSECONDS).isDone());
		assertTime(300, t);

		//failure
		before();
		sf.exception(cause);
		f = sf.getCreateFuture();
		fireEvent(SessionEvent.CREATED, 100);
		t = System.currentTimeMillis();
		try {
			f.sync(1000);
			fail("exception not frown");
		}
		catch (ExecutionException e) {
			assertTrue(cause == e.getCause());
		}
		assertTime(100, t);
		assertTrue(f.isDone());
		assertTrue(f.isFailed());
		assertTrue(cause == f.cause());

		before();
		sf.exception(cause);
		f = sf.getCreateFuture();
		fireEvent(SessionEvent.CREATED, 100);
		t = System.currentTimeMillis();
		try {
			f.sync(1000000, TimeUnit.MICROSECONDS);
			fail("exception not frown");
		}
		catch (ExecutionException e) {
			assertTrue(cause == e.getCause());
		}
		assertTime(100, t);
		assertTrue(f.isDone());
		assertTrue(f.isFailed());
		assertTrue(cause == f.cause());
		
		before();
		sf.exception(cause);
		f = sf.getCreateFuture();
		fireEvent(SessionEvent.CREATED, 100);
		t = System.currentTimeMillis();
		try {
			f.sync();
			fail("exception not frown");
		}
		catch (ExecutionException e) {
			assertTrue(cause == e.getCause());
		}
		assertTime(100, t);
		assertTrue(f.isDone());
		assertTrue(f.isFailed());
		assertTrue(cause == f.cause());
		
		sf.abort(null);
		try {
			sf.getOpenFuture().sync();
			fail("exception not frown");
		} catch (CancellationException e) {
		}
	}
	
	@Test
	public void testSyncUninterruptibly() throws ExecutionException, TimeoutException {
		IFuture<Void> f = sf.getCreateFuture();
		thread = Thread.currentThread();
		Exception cause = new Exception();
		long t;
		
		//successful
		t = System.currentTimeMillis();
		try {
			f.syncUninterruptibly(400);
			fail("timeout not thrown");
		} catch (TimeoutException e1) {
		}
		assertTime(400, t);
		t = System.currentTimeMillis();
		try {
			f.syncUninterruptibly(600000, TimeUnit.MICROSECONDS);
			fail("timeout not thrown");
		} catch (TimeoutException e1) {
		}
		assertTime(600, t);
		fireEvent(SessionEvent.CREATED, 500);
		t = System.currentTimeMillis();
		assertTrue(f.syncUninterruptibly().isDone());
		assertTime(500, t);

		f = sf.getOpenFuture();
		fireEvent(SessionEvent.OPENED, 300);
		t = System.currentTimeMillis();
		assertTrue(f.syncUninterruptibly(1000).isDone());
		assertTime(300, t);
		f = sf.getCloseFuture();
		fireEvent(SessionEvent.CLOSED, 300);
		t = System.currentTimeMillis();
		assertTrue(f.syncUninterruptibly(1000000, TimeUnit.MICROSECONDS).isDone());
		assertTime(300, t);

		//failure
		before();
		sf.exception(cause);
		f = sf.getCreateFuture();
		fireEvent(SessionEvent.CREATED, 100);
		t = System.currentTimeMillis();
		try {
			f.syncUninterruptibly(1000);
			fail("exception not frown");
		}
		catch (ExecutionException e) {
			assertTrue(cause == e.getCause());
		}
		assertTime(100, t);
		assertTrue(f.isDone());
		assertTrue(f.isFailed());
		assertTrue(cause == f.cause());

		before();
		sf.exception(cause);
		f = sf.getCreateFuture();
		fireEvent(SessionEvent.CREATED, 100);
		t = System.currentTimeMillis();
		try {
			f.syncUninterruptibly(1000000, TimeUnit.MICROSECONDS);
			fail("exception not frown");
		}
		catch (ExecutionException e) {
			assertTrue(cause == e.getCause());
		}
		assertTime(100, t);
		assertTrue(f.isDone());
		assertTrue(f.isFailed());
		assertTrue(cause == f.cause());
		
		before();
		sf.exception(cause);
		f = sf.getCreateFuture();
		fireEvent(SessionEvent.CREATED, 100);
		t = System.currentTimeMillis();
		try {
			f.syncUninterruptibly();
			fail("exception not frown");
		}
		catch (ExecutionException e) {
			assertTrue(cause == e.getCause());
		}
		assertTime(100, t);
		assertTrue(f.isDone());
		assertTrue(f.isFailed());
		assertTrue(cause == f.cause());
	}
	
	@Test
	public void testGet() throws InterruptedException, ExecutionException, TimeoutException {
		IFuture<Void> f = sf.getCreateFuture();
		thread = Thread.currentThread();
		long t;
		Exception cause1 = new Exception();
		CancellationException cause2 = new CancellationException();
		
		fireEvent(SessionEvent.CREATED, 500);
		t = System.currentTimeMillis();
		assertNull(f.get());
		assertTime(500, t);
		f = sf.getOpenFuture();
		fireEvent(SessionEvent.OPENED, -1, 300);
		t = System.currentTimeMillis();
		try {
			f.get();
			fail("not interrupted");
		}
		catch (InterruptedException e) {
		}
		assertTime(300, t);
		sf.exception(cause1);
		fireEvent(SessionEvent.OPENED, 300);
		t = System.currentTimeMillis();
		try {
			f.get();
			fail("exception not thrown");
		}
		catch (ExecutionException e) {
			assertTrue(cause1 == e.getCause());
		}
		assertTime(300, t);
		before();
		f = sf.getOpenFuture();
		sf.exception(cause2);
		fireEvent(SessionEvent.OPENED, 300);
		t = System.currentTimeMillis();
		try {
			f.get();
			fail("exception not thrown");
		}
		catch (CancellationException e) {
			assertTrue(cause2 == e);
		}
		assertTime(300, t);
		
		//with timeout
		before();
		f = sf.getCreateFuture();
		try {
			f.get(300, TimeUnit.MILLISECONDS);
			fail("exception not thrown");
		} catch (TimeoutException e) {
		}
		fireEvent(SessionEvent.CREATED, 500);
		t = System.currentTimeMillis();
		assertNull(f.get(1000, TimeUnit.MILLISECONDS));
		assertTime(500, t);
	}
	
}
