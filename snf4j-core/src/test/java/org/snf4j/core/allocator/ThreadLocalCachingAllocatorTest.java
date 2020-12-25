/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020 SNF4J contributors
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
package org.snf4j.core.allocator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.Constants;
import org.snf4j.core.LockUtils;
import org.snf4j.core.thread.FastThreadLocal;
import org.snf4j.core.thread.FastThreadLocalThread;

public class ThreadLocalCachingAllocatorTest {

	private ThreadLoop loop;
	
	@Before
	public void before() {
		loop = null;
	}
	
	@After
	public void after() {
		if (loop != null) {
			loop.stop();
			loop = null;
		}
	}
	
	private void assertForAllThreads(ThreadLocalCachingAllocator a, boolean expected) throws Exception {
		Field f = ThreadLocalCachingAllocator.class.getDeclaredField("threadCaches");
		
		f.setAccessible(true);
		@SuppressWarnings("unchecked")
		FastThreadLocal<Cache> c = (FastThreadLocal<Cache>) f.get(a);
		assertEquals(expected, c.isForAllThreads());
	}
	
	private int getIndex(ThreadLocalCachingAllocator a) throws Exception {
		Field f = ThreadLocalCachingAllocator.class.getDeclaredField("threadCaches");
		
		f.setAccessible(true);
		@SuppressWarnings("unchecked")
		FastThreadLocal<Cache> c = (FastThreadLocal<Cache>) f.get(a);
		f = FastThreadLocal.class.getDeclaredField("index");
		f.setAccessible(true);
		return f.getInt(c);
	}
	
	@Test
	public void testConstructors() throws Exception {
		DefaultAllocatorMetric m = new DefaultAllocatorMetric();
		
		ThreadLocalCachingAllocator a = new ThreadLocalCachingAllocator(true);
		ByteBuffer b = a.allocate(10);
		assertTrue(b.isDirect());
		assertEquals(128, b.capacity());
		assertForAllThreads(a, false);
		a = new ThreadLocalCachingAllocator(false);
		b = a.allocate(11);
		assertFalse(b.isDirect());
		assertEquals(128, b.capacity());
		assertForAllThreads(a, false);
		
		a = new ThreadLocalCachingAllocator(true, m);
		assertEquals(0, m.getAllocatingCount());
		b = a.allocate(3);
		assertEquals(1, m.getAllocatingCount());
		assertEquals(128, b.capacity());
		assertTrue(b.isDirect());
		assertForAllThreads(a, false);
		a = new ThreadLocalCachingAllocator(false, m);
		assertEquals(1, m.getAllocatingCount());
		b = a.allocate(3);
		assertEquals(2, m.getAllocatingCount());
		assertEquals(128, b.capacity());
		assertFalse(b.isDirect());
		assertForAllThreads(a, false);
		
		a = new ThreadLocalCachingAllocator(true, 8);
		b = a.allocate(3);
		assertTrue(b.isDirect());
		assertEquals(8, b.capacity());
		assertForAllThreads(a, false);
		a = new ThreadLocalCachingAllocator(false, 8);
		b = a.allocate(1);
		assertFalse(b.isDirect());
		assertEquals(8, b.capacity());
		assertForAllThreads(a, false);

		a = new ThreadLocalCachingAllocator(true, 8, m);
		assertEquals(2, m.getAllocatingCount());
		b = a.allocate(3);
		assertEquals(3, m.getAllocatingCount());
		assertEquals(8, b.capacity());
		assertTrue(b.isDirect());
		assertForAllThreads(a, false);
		a = new ThreadLocalCachingAllocator(false, 8, m);
		assertEquals(3, m.getAllocatingCount());
		b = a.allocate(3);
		assertEquals(4, m.getAllocatingCount());
		assertEquals(8, b.capacity());
		assertFalse(b.isDirect());
		assertForAllThreads(a, false);
		
		a = new ThreadLocalCachingAllocator(true, 8, false, null);
		b = a.allocate(3);
		assertTrue(b.isDirect());
		assertEquals(8, b.capacity());
		assertForAllThreads(a, false);
		a = new ThreadLocalCachingAllocator(false, 8, false, null);
		b = a.allocate(1);
		assertFalse(b.isDirect());
		assertEquals(8, b.capacity());
		assertForAllThreads(a, false);
		
		a = new ThreadLocalCachingAllocator(true, 8, true, null);
		b = a.allocate(3);
		assertTrue(b.isDirect());
		assertEquals(8, b.capacity());
		assertForAllThreads(a, true);
		a = new ThreadLocalCachingAllocator(false, 8, true, null);
		b = a.allocate(1);
		assertFalse(b.isDirect());
		assertEquals(8, b.capacity());
		assertForAllThreads(a, true);
	
		a = new ThreadLocalCachingAllocator(true, 8, true, m);
		assertEquals(4, m.getAllocatingCount());
		b = a.allocate(3);
		assertEquals(5, m.getAllocatingCount());
		assertTrue(b.isDirect());
		assertEquals(8, b.capacity());
		assertForAllThreads(a, true);
		a = new ThreadLocalCachingAllocator(false, 8, false, m);
		assertEquals(5, m.getAllocatingCount());
		b = a.allocate(1);
		assertEquals(6, m.getAllocatingCount());
		assertFalse(b.isDirect());
		assertEquals(8, b.capacity());
		assertForAllThreads(a, false);
	}
	
	@Test
	public void testCache() throws Exception {
		int lastCapacity = 8 << 7;
		
		ThreadLocalCachingAllocator a = new ThreadLocalCachingAllocator(true, 8);
		Cache c = a.cache(7);
		assertTrue(a.caches[0] == c);
		assertEquals(8, c.capacity());
		assertTrue(SyncCache.class == c.getClass());
		c = a.cache(lastCapacity);
		assertTrue(a.caches[7] == c);
		assertEquals(lastCapacity, c.capacity());
		assertTrue(SyncLastCache.class == c.getClass());
		assertEquals(8, a.caches.length);
		for (int i=0; i<7; ++i) {
			assertTrue(SyncCache.class == a.caches[i].getClass());
		}
		
		a = new ThreadLocalCachingAllocator(true, 8, true, null);
		c = a.cache(7);
		assertTrue(a.caches[0] != c);
		assertEquals(8, c.capacity());
		assertTrue(Cache.class == c.getClass());
		c = a.cache(lastCapacity);
		assertTrue(a.caches[7] != c);
		assertEquals(lastCapacity, c.capacity());
		assertTrue(LastCache.class == c.getClass());
		for (int i=0; i<7; ++i) {
			c = a.cache(8 << i);
			assertTrue(Cache.class == c.getClass());
			assertEquals(8 << i, c.capacity());
		}
		
		a = new ThreadLocalCachingAllocator(true, 8, false, null);
		AllocatorTask task = new AllocatorTask(a, AllocatorTask.Action.CACHE);
		FastThreadLocalThread t = new FastThreadLocalThread(task);
		t.start();
		t.join(1000);
		assertTrue(a.caches[0] != task.cache1);
		assertEquals(8, task.cache1.capacity());
		assertTrue(Cache.class == task.cache1.getClass());
		assertTrue(a.caches[7] != task.cache2);
		assertEquals(lastCapacity, task.cache2.capacity());
		assertTrue(LastCache.class == task.cache2.getClass());
	}
	
	@Test
	public void testPurge() throws Exception {
		int lastCapacity = 8 << 7;
		loop = new ThreadLoop();
		FastThreadLocalThread t = new FastThreadLocalThread(loop);
		t.start();
		
		//purge in this thread
		ThreadLocalCachingAllocator a = new ThreadLocalCachingAllocator(true, 8);
		ByteBuffer b1 = a.allocate(8);
		ByteBuffer b2 = a.allocate(lastCapacity);
		a.release(b1);
		a.release(b2);
		AllocatorTask task = new AllocatorTask(a, AllocatorTask.Action.ALLOCATE);
		loop.runTask(task);
		ByteBuffer tb1 = task.buffer1;
		ByteBuffer tb2 = task.buffer2;
		task.action = AllocatorTask.Action.RELEASE;
		loop.runTask(task);
		task.buffer1 = null;
		task.buffer2 = null;
		a.purge();
		assertFalse(b1 == a.allocate(8));
		assertFalse(b2 == a.allocate(lastCapacity));
		task.action = AllocatorTask.Action.ALLOCATE;
		loop.runTask(task);
		assertTrue(tb1 == task.buffer1);
		assertTrue(tb2 == task.buffer2);
		
		//purge in the loop
		b1 = a.allocate(8);
		b2 = a.allocate(lastCapacity);
		a.release(b1);
		a.release(b2);
		task.action = AllocatorTask.Action.RELEASE;
		loop.runTask(task);
		task.action = AllocatorTask.Action.PURGE;
		loop.runTask(task);
		assertTrue(b1 == a.allocate(8));
		assertTrue(b2 == a.allocate(lastCapacity));
		task.action = AllocatorTask.Action.ALLOCATE;
		loop.runTask(task);
		assertFalse(tb1 == task.buffer1);
		assertFalse(tb2 == task.buffer2);
		
		loop.stop();
		t.join();
	}
	
	@Test
	public void testTouchAll() throws Exception {
		loop = new ThreadLoop();
		FastThreadLocalThread t = new FastThreadLocalThread(loop);
		t.start();
		
		//touchAll in this thread
		System.setProperty(Constants.ALLOCATOR_MIN_CACHE_SIZE_PROPERTY, "0");
		System.setProperty(Constants.ALLOCATOR_CACHE_AGE_THRESHOLD_PROPERTY, "10");
		ThreadLocalCachingAllocator a = new ThreadLocalCachingAllocator(true, 8);
		ByteBuffer b = a.allocate(32);
		a.release(b);
		AllocatorTask task = new AllocatorTask(a, AllocatorTask.Action.ALLOCATE);
		loop.runTask(task);
		task.action = AllocatorTask.Action.RELEASE;
		loop.runTask(task);
		assertEquals(1, a.caches[2].size);
		for (int i=0; i<20; i++) {
			a.allocate(8);
		}
		assertEquals(0, a.caches[2].size);
		int index = getIndex(a);
		Cache[] caches = (Cache[]) t.getFastThreadLocal(index);
		assertEquals(1, caches[0].size);
		assertEquals(1, caches[7].size);
		
		//touchAll in loop
		task.action = AllocatorTask.Action.ALLOCATE_32;
		loop.runTask(task);
		assertEquals(0, caches[0].size);
		assertEquals(0, caches[7].size);
		
		loop.stop();
		t.join();
	}
	
	static class ThreadLoop implements Runnable {

		volatile boolean running = true;
		
		volatile Runnable task;
		
		final Object taskLock = new Object();
		
		final AtomicBoolean taskFinished = new AtomicBoolean(false);
		
		void runTask(Runnable task) throws InterruptedException {
			taskFinished.set(false);
			synchronized (taskLock) {
				this.task = task;
				taskLock.notify();
			}
			LockUtils.waitFor(taskFinished, 1000);
		}
		
		void stop() {
			synchronized (taskLock) {
				running = false;
				taskLock.notify();
			}
		}
		
		@Override
		public void run() {
			int loopMaxCount = 100;
			boolean finished;
			
			while (running && loopMaxCount-- > 0) {
				finished = false;
				
				synchronized (taskLock) {
					try {
						if (task == null) {
							taskLock.wait(1000);
						}
					} catch (InterruptedException e) {
					}
					if (task != null) {
						task.run();
						task = null;
						finished = true;
					}
				}
				
				if (finished) {
					LockUtils.notify(taskFinished);
				}
			}
		}
		
	}
	
	static class AllocatorTask implements Runnable {
		
		enum Action {CACHE, ALLOCATE, RELEASE, PURGE, ALLOCATE_32};
		
		final ThreadLocalCachingAllocator allocator;
		
		volatile Action action;
		
		volatile Cache cache1;
		
		volatile Cache cache2;
		
		volatile ByteBuffer buffer1;
		
		volatile ByteBuffer buffer2;
		
		AllocatorTask(ThreadLocalCachingAllocator allocator, Action action) {
			this.allocator = allocator;
			this.action = action;
		}
		
		public void run() {
			switch (action) {
			case CACHE:
				cache1 = allocator.cache(8);
				cache2 = allocator.cache(8 << 7);
				break;
				
			case ALLOCATE:
				buffer1 = allocator.allocate(8);
				buffer2 = allocator.allocate(8 << 7);
				break;
				
			case ALLOCATE_32:
				allocator.allocate(32);
				break;
				
			case RELEASE:
				allocator.release(buffer1);
				allocator.release(buffer2);
				break;
				
			case PURGE:
				allocator.purge();
				break;
			}
			
		}
	}
}
