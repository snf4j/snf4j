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
package org.snf4j.core.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ThreadFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.Client;
import org.snf4j.core.Packet;
import org.snf4j.core.PacketType;
import org.snf4j.core.SelectorLoop;
import org.snf4j.core.Server;

public class DefaultSelectorLoopPoolTest {
	final long TIMEOUT = 2000;
	final int PORT = 7780;
	final static long DELAY = 1500;

	Server s;
	Client c, c1, c2, c3, c4;
	
	@Before
	public void before() {
		s = c = c1 = c2 = c3 = c4 = null;
	}

	@After
	public void after() throws InterruptedException {
		if (c != null) c.stop(TIMEOUT);
		if (c1 != null) c1.stop(TIMEOUT);
		if (c2 != null) c2.stop(TIMEOUT);
		if (c3 != null) c3.stop(TIMEOUT);
		if (c4 != null) c4.stop(TIMEOUT);
		if (s != null) s.stop(TIMEOUT);
	}

	private void waitFor(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}	
	public static ThreadFactory THREAD_FACTORY = new ThreadFactory() {

		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, "ThreadName");
		}
	};
	
	static class DelayedThread extends Thread {
		Runnable r;
		long sleep;
		
		DelayedThread(Runnable r, long sleep) {
			this.r = r;
			this.sleep = sleep;
		}
		
		@Override
		public void run() {
			r.run();
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
			}
		}
	}
	
	public static ThreadFactory DELAYED_THREAD_FACTORY = new ThreadFactory() {

		@Override
		public Thread newThread(Runnable r) {
			return new DelayedThread(r, DELAY);
		}
	};
	
	@Test
	public void testConstructor() throws Exception {
		DefaultSelectorLoopPool pool = new DefaultSelectorLoopPool(10);
		
		assertEquals(10, pool.getCapacity());
		assertEquals(0, pool.getSize());
		assertEquals(0, pool.getPool().length);
		assertEquals("SelectorPool-" + pool.getId(), pool.getName());
		assertEquals("SelectorPool-" + pool.getId(), pool.toString());
		pool.getLoop(null);
		assertEquals(1, pool.getSize());
		assertEquals(1, pool.getPool().length);
		pool.getLoop(null);
		assertEquals(1, pool.getSize());
		assertEquals(1, pool.getPool().length);
		assertEquals(pool.getPool()[0].getId(), pool.getPool()[0].getId());
		assertEquals("SelectorPool-" + pool.getId() + "-1", pool.getPool()[0].getName());
		pool.quickStop();
		assertTrue(pool.join(TIMEOUT));
		
		pool = new DefaultSelectorLoopPool("Pool", 2);
		assertEquals(2, pool.getCapacity());
		assertEquals("Pool", pool.getName());
		pool.getLoop(null);
		assertEquals("Pool-1", pool.getPool()[0].getName());
		pool.stop();
		assertTrue(pool.join(TIMEOUT));
		
		s = new Server(PORT);
		c = new Client(PORT);
		pool = new DefaultSelectorLoopPool(1);
		s.pool = pool;
		s.start();
		c.start();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
		c.write(new Packet(PacketType.GET_THREAD));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|GET_THREAD_RESPONSE(SelectorLoop-"+pool.getPool()[0].getName()+")|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		s = new Server(PORT);
		c = new Client(PORT);
		pool = new DefaultSelectorLoopPool("Pool", 1);
		s.pool = pool;
		s.start();
		c.start();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
		c.write(new Packet(PacketType.GET_THREAD));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|GET_THREAD_RESPONSE(SelectorLoop-Pool-1)|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		s = new Server(PORT);
		c = new Client(PORT);
		pool = new DefaultSelectorLoopPool("Pool", 1, THREAD_FACTORY);
		s.pool = pool;
		s.start();
		c.start();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
		c.write(new Packet(PacketType.GET_THREAD));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|GET_THREAD_RESPONSE(ThreadName)|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}
	
	@Test
	public void testGetLoop() throws Exception {
		s = new Server(PORT);
		c1 = new Client(PORT);
		c2 = new Client(PORT);
		c3 = new Client(PORT);
		c4 = new Client(PORT);
		DefaultSelectorLoopPool pool = new DefaultSelectorLoopPool(2);
		s.pool = pool;
		s.start();
		c1.start();
		c1.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals(1, pool.getSize());
		assertEquals(1, pool.getPool().length);
		
		c2.start();
		c2.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals(2, pool.getSize());
		assertEquals(2, pool.getPool().length);

		c3.start();
		c3.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals(2, pool.getSize());
		assertEquals(2, pool.getPool().length);
		assertEquals(2, pool.getPool()[0].getSize());
		assertEquals(1, pool.getPool()[1].getSize());
		
		c2.stop(TIMEOUT);
		c2.waitForSessionEnding(TIMEOUT);
		waitFor(500);
		assertEquals(2, pool.getPool().length);
		assertEquals(2, pool.getPool()[0].getSize());
		assertEquals(0, pool.getPool()[1].getSize());
		
		c4.start();
		c4.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals(2, pool.getSize());
		assertEquals(2, pool.getPool().length);
		assertEquals(2, pool.getPool()[0].getSize());
		assertEquals(1, pool.getPool()[1].getSize());
		
		c4.stop(TIMEOUT);
		c3.stop(TIMEOUT);
		c1.stop(TIMEOUT);
		s.stop(TIMEOUT);
		waitFor(500);
		assertEquals(2, pool.getSize());
		assertEquals(2, pool.getPool().length);
		assertEquals(0, pool.getPool()[0].getSize());
		assertEquals(0, pool.getPool()[1].getSize());
		
		assertFalse(pool.join(TIMEOUT));
		
		pool.stop();
		pool.join();
	}
	
	static class TestDefaultSelectLoopPool extends DefaultSelectorLoopPool {
		TestDefaultSelectLoopPool() {
			super(2);
		}
		
		@Override
		SelectorLoop createLoop(String name) throws Exception {
			throw new IllegalArgumentException("Unable to create loop");
		}

	}
	
	@Test
	public void testGetWithException() throws Exception {
		assertNull(new TestDefaultSelectLoopPool().getLoop(null));
		

		SocketChannel sc = SocketChannel.open();
		
		//pool capacity = 1
		DefaultSelectorLoopPool pool = new DefaultSelectorLoopPool("Pool", 1);
		SelectorLoop loop = pool.getLoop(sc);
		assertNotNull(loop);
		loop.stop();
		loop.join(2000);
		assertNull(pool.getLoop(sc));
		assertFalse(loop.isOpen());
		
		//pool capacity > 1
		pool = new DefaultSelectorLoopPool(2);
		loop = pool.getLoop(sc);
		assertNotNull(loop);
		loop.stop();
		loop.join(2000);
		SelectorLoop loop2 = pool.getLoop(sc);
		assertNotNull(loop2);
		assertTrue(loop2.isOpen());
		assertFalse(loop.isOpen());
		
	}
	
	@Test
	public void testJoin() throws Exception {
		s = new Server(PORT);
		c1 = new Client(PORT);
		c2 = new Client(PORT);
		c3 = new Client(PORT);
		
		DefaultSelectorLoopPool pool = new DefaultSelectorLoopPool("Pool", 1, DELAYED_THREAD_FACTORY);
		s.pool = pool;

		s.start();
		c1.start();
		c2.start();
		c3.start();
		s.waitForSessionOpen(TIMEOUT);
		c1.waitForSessionOpen(TIMEOUT);
		c2.waitForSessionOpen(TIMEOUT);
		c3.waitForSessionOpen(TIMEOUT);

		pool.stop();
		pool.join(500);
		pool.join();
		c1.stop(TIMEOUT);
		c2.stop(TIMEOUT);
		c3.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}
}
