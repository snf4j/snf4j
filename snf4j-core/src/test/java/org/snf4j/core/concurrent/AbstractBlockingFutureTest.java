package org.snf4j.core.concurrent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.TestSession;
import org.snf4j.core.handler.SessionEvent;

public class AbstractBlockingFutureTest {

	SessionFutures sf;
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
		sf = new SessionFutures(new TestSession());
	}
	
	void assertTime(long expected, long time) {
		time = System.currentTimeMillis() - time;
		assertTrue(time > expected - 10 && time < expected + 10);
	}
	
	@Test
	public void testAwait() throws Exception {
		IFuture<Void> f = sf.getCreateFuture();
		thread = Thread.currentThread();
		long t;
		
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
}
