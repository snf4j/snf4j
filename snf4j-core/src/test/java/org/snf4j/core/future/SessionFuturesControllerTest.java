/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017 SNF4J contributors
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.Client;
import org.snf4j.core.DelayedThreadFactory;
import org.snf4j.core.Packet;
import org.snf4j.core.PacketType;
import org.snf4j.core.Server;
import org.snf4j.core.StreamSession;
import org.snf4j.core.future.EventFuture;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.future.SessionFuturesController;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.SessionEvent;

public class SessionFuturesControllerTest {
	
	static final int PORT = 7777;
	static final long TIMEOUT = 2000;
	static final long AWAIT = 1000;
	
	SessionFuturesController sf;
	
	Server s;
	Client c;
	
	@Before
	public void before() {
		s = c = null;
	}

	@After
	public void after() throws InterruptedException {
		if (c != null) c.stop(TIMEOUT);
		if (s != null) s.stop(TIMEOUT);
	}
	
	void fireEvent(final SessionEvent event, final long delay) {
		Runnable r = new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
				}
				
				sf.event(event);
			}
		};
		new Thread(r).start();
	}

	void fireException(final Throwable t, final long delay) {
		Runnable r = new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
				}
				
				sf.exception(t);
			}
		};
		new Thread(r).start();
	}
	
	void fireData(final DataEvent event, final long delay, final long size) {
		Runnable r = new Runnable() {

			@SuppressWarnings("incomplete-switch")
			@Override
			public void run() {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
				}
				
				switch (event) {
					case SENT:
						sf.event(DataEvent.SENT, size);
						break;
				}
			}
		};
		new Thread(r).start();
	}
	
	void delayedClose(final StreamSession session, final long delay) {
		Runnable r = new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
				}
				session.close();
			}
		};
		new Thread(r).start();
	}

	void delayedResume(final StreamSession session, final long delay) {
		Runnable r = new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
				}
				session.resumeWrite();
			}
		};
		new Thread(r).start();
	}
	
	@Test
	public void testSessionEventFuture() throws Exception {
		Exception cause = new Exception();
		sf = new SessionFuturesController(null);

		IFuture<Void> f = sf.getCreateFuture();
		assertNotDone(f);
		
		//cancel
		assertFalse(f.cancel(false));
		assertFalse(f.cancel(true));
		assertNotDone(f);
	
		//success
		sf.event(SessionEvent.CREATED);
		assertSuccessful(f);
		
		//failure after success
		((EventFuture<?>)f).failure(cause);
		assertSuccessful(f);
		
		//failure
		f = sf.getOpenFuture();
		((EventFuture<?>)f).failure(cause);
		assertFailed(f, cause);
		
		//success after failure
		sf.event(SessionEvent.OPENED);
		assertFailed(f, cause);
	}
	
	@Test
	public void testSessionFuturesAwait() throws Exception {
		sf = new SessionFuturesController(null);
		
		long t0 = System.currentTimeMillis();
		fireEvent(SessionEvent.CREATED, 1000);
		sf.getCreateFuture().await();
		long t1 = System.currentTimeMillis();
		long delta = t1-t0;
		assertTrue(delta > 950 && delta < 1050);
		sf.getCreateFuture().await();
		
		IFuture<Void> f = sf.getWriteFuture(100);
		t0 = System.currentTimeMillis();
		fireData(DataEvent.SENT, 1000, 99);
		fireData(DataEvent.SENT, 2000, 1);
		f.await();
		t1 = System.currentTimeMillis();
		delta = t1-t0;
		assertTrue(delta > 1950 && delta < 2050);
		f.await();
	}
	
	private IFuture<?> await(StreamSession session, SessionEvent type, long timeout, long expectedTimeout) throws InterruptedException {
		IFuture<Void> f = null;
		
		switch (type) {
		case CREATED:
			f = session.getCreateFuture();
			break;
			
		case OPENED:
			f = session.getOpenFuture();
			break;
			
		case CLOSED:
			f = session.getCloseFuture();
			break;
			
		case ENDING:
			f = session.getEndFuture();
			break;
		}
		
		long t0 = System.currentTimeMillis();
		f.await(timeout);
		long t = System.currentTimeMillis()-t0;
		if (expectedTimeout > 0) {
			assertTrue("expected "+expectedTimeout+" but was "+t,t <= (expectedTimeout * 100 / 95) && t >= (expectedTimeout * 95 / 100));
		}
		else {
			assertTrue(t < 50);
		}
		return f;
	}
	
	void assertNotDone(StreamSession session, SessionEvent type) throws InterruptedException {
		assertFalse(await(session, type, AWAIT, AWAIT).isDone());
	}

	void assertDone(StreamSession session, SessionEvent type) throws InterruptedException {
		assertTrue(await(session, type, AWAIT, 0).isDone());
	}
	
	@Test
	public void testStreamSessionWriteFuture() throws Exception {
		s = new Server(PORT);
		s.start();

		//write without suspend
		c = new Client(PORT);
		c.start();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		s.getRecordedData(true);
		c.getRecordedData(true);
		IFuture<Void> f = c.getSession().write(new Packet(PacketType.ECHO).toBytes());
		assertTrue(f.await(TIMEOUT).isDone());
		assertTrue(f.await(TIMEOUT).isDone());
		assertTrue(f.isSuccessful());
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE()|", c.getRecordedData(true));
		assertEquals("DR|ECHO()|DS|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		
		//write with suspend
		c = new Client(PORT);
		c.start();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		s.getRecordedData(true);
		c.getRecordedData(true);
		c.getSession().suspendWrite();
		f = c.getSession().write(new Packet(PacketType.ECHO).toBytes());
		delayedResume(c.getSession(), 1000);
		long t = System.currentTimeMillis();
		assertTrue(f.await(2000).isDone());
		t = System.currentTimeMillis() - t;
		assertTrue(t > 950 && t <1050);
		assertTrue(f.isSuccessful());
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE()|", c.getRecordedData(true));
		assertEquals("DR|ECHO()|DS|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
	}
	
	@Test
	public void testStreamSessionEventFutures() throws Exception {
		s = new Server(PORT);
		s.start();

		//futures already done or not done yet
		c = new Client(PORT);
		StreamSession session = c.createSession();
		assertNotDone(session, SessionEvent.CREATED);
		assertNotDone(session, SessionEvent.OPENED);
		c.start();
		assertDone(session, SessionEvent.CREATED);
		assertDone(session, SessionEvent.OPENED);
		assertNotDone(session, SessionEvent.CLOSED);
		assertNotDone(session, SessionEvent.ENDING);
		session.close();
		assertDone(session, SessionEvent.CLOSED);
		assertDone(session, SessionEvent.ENDING);
		c.stop(TIMEOUT);

		//futures done while waiting
		c = new Client(PORT);
		c.setThreadFactory(new DelayedThreadFactory(500));
		session = c.createSession();
		c.start();
		assertTrue(await(session, SessionEvent.CREATED, AWAIT, 500).isDone());
		assertDone(session, SessionEvent.OPENED);
		delayedClose(session, 500);
		assertTrue(await(session, SessionEvent.CLOSED, AWAIT, 500).isDone());
		assertDone(session, SessionEvent.ENDING);
		c.stop(TIMEOUT);

		//futures done while waiting
		c = new Client(PORT);
		c.setThreadFactory(new DelayedThreadFactory(500));
		session = c.createSession();
		c.start();
		assertTrue(await(session, SessionEvent.OPENED, AWAIT, 500).isDone());
		assertDone(session, SessionEvent.CREATED);
		delayedClose(session, 500);
		assertTrue(await(session, SessionEvent.ENDING, AWAIT, 500).isDone());
		assertDone(session, SessionEvent.CLOSED);
		c.stop(TIMEOUT);
	}
	
	void assertSuccessful(IFuture<?> future) {
		assertTrue(future.isDone());
		assertTrue(future.isSuccessful());
		assertFalse(future.isCancelled());
		assertFalse(future.isFailed());
		assertNull(future.cause());
	}

	void assertFailed(IFuture<?> future, Throwable t) {
		assertTrue(future.isDone());
		assertFalse(future.isSuccessful());
		assertFalse(future.isCancelled());
		assertTrue(future.isFailed());
		assertTrue(future.cause() == t);
	}

	void assertCanceled(IFuture<?> future) {
		assertTrue(future.isDone());
		assertFalse(future.isSuccessful());
		assertTrue(future.isCancelled());
		assertFalse(future.isFailed());
		assertNull(future.cause());
	}
	
	void assertNotDone(IFuture<?> future) {
		assertFalse(future.isDone());
		assertFalse(future.isSuccessful());
		assertFalse(future.isCancelled());
		assertFalse(future.isFailed());
		assertNull(future.cause());
	}
	
	@Test
	public void testSessionEventFuturesWithExeception() {
		Exception cause1 = new Exception("Ex1");
		Exception cause2 = new Exception("Ex2");
		
		//no exception
		sf = new SessionFuturesController(null);
		assertNotDone(sf.getCreateFuture());
		assertNotDone(sf.getOpenFuture());
		assertNotDone(sf.getCloseFuture());
		assertNotDone(sf.getEndFuture());
		sf.event(SessionEvent.CREATED);
		assertSuccessful(sf.getCreateFuture());
		assertNotDone(sf.getOpenFuture());
		assertNotDone(sf.getCloseFuture());
		assertNotDone(sf.getEndFuture());
		sf.event(SessionEvent.OPENED);
		assertSuccessful(sf.getCreateFuture());
		assertSuccessful(sf.getOpenFuture());
		assertNotDone(sf.getCloseFuture());
		assertNotDone(sf.getEndFuture());
		sf.event(SessionEvent.CLOSED);
		assertSuccessful(sf.getCreateFuture());
		assertSuccessful(sf.getOpenFuture());
		assertSuccessful(sf.getCloseFuture());
		assertNotDone(sf.getEndFuture());
		sf.event(SessionEvent.ENDING);
		assertSuccessful(sf.getCreateFuture());
		assertSuccessful(sf.getOpenFuture());
		assertSuccessful(sf.getCloseFuture());
		assertSuccessful(sf.getEndFuture());
		
		//exception before opened
		sf = new SessionFuturesController(null);
		sf.event(SessionEvent.CREATED);
		sf.exception(cause1);
		sf.exception(cause2);
		assertSuccessful(sf.getCreateFuture());
		assertNotDone(sf.getOpenFuture());
		assertNotDone(sf.getCloseFuture());
		assertNotDone(sf.getEndFuture());
		sf.event(SessionEvent.ENDING);
		assertSuccessful(sf.getCreateFuture());
		assertFailed(sf.getOpenFuture(),cause1);
		assertFailed(sf.getCloseFuture(), cause1);
		assertFailed(sf.getEndFuture(), cause1);

		//exception after opened
		sf = new SessionFuturesController(null);
		sf.event(SessionEvent.CREATED);
		sf.event(SessionEvent.OPENED);
		sf.exception(cause1);
		sf.exception(cause2);
		assertSuccessful(sf.getCreateFuture());
		assertSuccessful(sf.getOpenFuture());
		assertNotDone(sf.getCloseFuture());
		assertNotDone(sf.getEndFuture());
		sf.event(SessionEvent.CLOSED);
		assertSuccessful(sf.getCreateFuture());
		assertSuccessful(sf.getOpenFuture());
		assertFailed(sf.getCloseFuture(), cause1);
		assertNotDone(sf.getEndFuture());
		sf.event(SessionEvent.ENDING);
		assertSuccessful(sf.getCreateFuture());
		assertSuccessful(sf.getOpenFuture());
		assertFailed(sf.getCloseFuture(), cause1);
		assertFailed(sf.getEndFuture(), cause1);
	}
	
	@Test
	public void testSessionDataFutureAndItsProxy() {
		Exception cause1 = new Exception();
		Exception cause2 = new Exception();
		
		//exception after opened
		sf = new SessionFuturesController(null);
		sf.event(SessionEvent.CREATED);
		sf.event(SessionEvent.OPENED);
		IFuture<Void> f0 = sf.getWriteFuture(100);
		IFuture<Void> f1 = sf.getWriteFuture(101);
		assertNotDone(f0);
		assertNotDone(f1);
		sf.event(DataEvent.RECEIVED, 101);
		assertNotDone(f0);
		assertNotDone(f1);
		sf.event(DataEvent.SENT, 100);
		assertSuccessful(f0);
		assertNotDone(f1);
		sf.exception(cause1);
		sf.exception(cause2);
		assertSuccessful(f0);
		assertFailed(f1, cause1);
		sf.event(SessionEvent.CLOSED);
		sf.event(SessionEvent.ENDING);
		assertSuccessful(sf.getCreateFuture());
		assertSuccessful(sf.getOpenFuture());
		assertFailed(sf.getCloseFuture(), cause1);
		assertFailed(sf.getEndFuture(), cause1);
		
		//exception before opened
		sf = new SessionFuturesController(null);
		sf.event(SessionEvent.CREATED);
		f0 = sf.getWriteFuture(100);
		f1 = sf.getWriteFuture(101);
		sf.exception(cause1);
		sf.exception(cause2);
		sf.event(SessionEvent.ENDING);
		assertSuccessful(sf.getCreateFuture());
		assertFailed(sf.getOpenFuture(), cause1);
		assertFailed(sf.getCloseFuture(), cause1);
		assertFailed(sf.getEndFuture(), cause1);
		assertFailed(f0, cause1);
		assertFailed(f1, cause1);
		
		//cancel
		sf = new SessionFuturesController(null);
		sf.event(SessionEvent.CREATED);
		sf.event(SessionEvent.OPENED);
		f0 = sf.getWriteFuture(100);
		f1 = sf.getWriteFuture(101);
		sf.event(DataEvent.SENT, 100);
		sf.event(SessionEvent.CLOSED);
		sf.event(SessionEvent.ENDING);
		assertSuccessful(sf.getCreateFuture());
		assertSuccessful(sf.getOpenFuture());
		assertSuccessful(f0);
		assertCanceled(f1);
		assertSuccessful(sf.getCloseFuture());
		assertSuccessful(sf.getEndFuture());
	}
	
	@Test
	public void testDeadLock() throws Exception {
		s = new Server(PORT);
		s.start();
		c = new Client(PORT);
		c.start();
		
		s.waitForSessionOpen(TIMEOUT);
		c.waitForSessionOpen(TIMEOUT);
		s.getRecordedData(false);
		c.getRecordedData(false);
		c.write(new Packet(PacketType.DEADLOCK));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|DS|DR|DEADLOCK_RESPONSE(YES)|",c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}
}
