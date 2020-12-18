package org.snf4j.core.thread;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class FastThreadLocalThreadTest {
	
	Object v1 = new Object();
	Object v2 = new Object();
	
	AtomicInteger counter = new AtomicInteger(0);
	
	Runnable task = new Runnable() {

		@Override
		public void run() {
			counter.incrementAndGet();
		}
		
	};
	
	@Test
	public void testConstructors() throws Exception {
		Thread t0 = new Thread();
		ThreadGroup group = new ThreadGroup("test");
		
		FastThreadLocalThread t = new FastThreadLocalThread();
		assertTrue(t0.getThreadGroup() == t.getThreadGroup());

		t = new FastThreadLocalThread(task);
		assertTrue(t0.getThreadGroup() == t.getThreadGroup());
		assertEquals(0, counter.get());
		t.start();
		t.join();
		assertEquals(1, counter.get());
		
		t = new FastThreadLocalThread(group, task);
		assertTrue(group == t.getThreadGroup());
		t.start();
		t.join();
		assertEquals(2, counter.get());
		
		t = new FastThreadLocalThread("Test1");
		assertTrue(t0.getThreadGroup() == t.getThreadGroup());
		assertEquals("Test1", t.getName());
		
		t = new FastThreadLocalThread(group, "Test2");
		assertTrue(group == t.getThreadGroup());
		assertEquals("Test2", t.getName());
		
		t = new FastThreadLocalThread(task, "Test3");
		assertTrue(t0.getThreadGroup() == t.getThreadGroup());
		assertEquals("Test3", t.getName());
		t.start();
		t.join();
		assertEquals(3, counter.get());
		
		t = new FastThreadLocalThread(group, task, "Test4");
		assertTrue(group == t.getThreadGroup());
		assertEquals("Test4", t.getName());
		t.start();
		t.join();
		assertEquals(4, counter.get());
			
		t = new FastThreadLocalThread(group, task, "Test5", 200);
		assertTrue(group == t.getThreadGroup());
		assertEquals("Test5", t.getName());
		t.start();
		t.join();
		assertEquals(5, counter.get());
		
		
	}
	
	@Test
	public void testGetFastThreadLocal() {
		FastThreadLocalThread t = new FastThreadLocalThread();
		
		assertNull(t.getFastThreadLocal(0));
		t.setFastThreadLocal(0, v1);
		assertTrue(v1 == t.getFastThreadLocal(0));
		assertNull(t.getFastThreadLocal(10000));
		try {
			t.getFastThreadLocal(-1);
			fail();
		}
		catch (ArrayIndexOutOfBoundsException e) {
		}
	}
	
	@Test
	public void testSetFastThreadLocal() {
		FastThreadLocalThread t = new FastThreadLocalThread();
		
		t.setFastThreadLocal(7, v1);
		assertTrue(v1 == t.getFastThreadLocal(7));
		assertNull(t.getFastThreadLocal(8));
		t.setFastThreadLocal(8, v2);
		assertTrue(v2 == t.getFastThreadLocal(8));
		t.setFastThreadLocal(1000, v1);
		assertTrue(v1 == t.getFastThreadLocal(1000));
		assertTrue(v1 == t.getFastThreadLocal(7));
		assertTrue(v2 == t.getFastThreadLocal(8));
		
		try {
			t.setFastThreadLocal(-1, v1);
			fail();
		}
		catch (ArrayIndexOutOfBoundsException e) {
		}
	}
	
	@Test
	public void testRemoveFastThreadLocal() {
		FastThreadLocalThread t = new FastThreadLocalThread();
		
		t.removeFastThreadLocal(0);
		t.removeFastThreadLocal(10000);
		t.setFastThreadLocal(1, v1);
		t.removeFastThreadLocal(1);
		assertNull(t.getFastThreadLocal(1));
		try {
			t.removeFastThreadLocal(-1);
			fail();
		}
		catch (ArrayIndexOutOfBoundsException e) {
		}
	}
}
