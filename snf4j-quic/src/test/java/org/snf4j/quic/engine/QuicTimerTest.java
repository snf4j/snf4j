/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.engine.IEngineTimerTask;
import org.snf4j.core.timer.DefaultTimer;
import org.snf4j.core.timer.ITimerTask;

public class QuicTimerTest {

	DefaultTimer timer;
	
	TestSessionTimer stimer;
	
	@Before
	public void before() {
		timer = new DefaultTimer();
		stimer = new TestSessionTimer(timer);
	}
	
	@After
	public void after() {
		timer.cancel();
	}
	
	void waitFor(long millis) throws Exception {
		Thread.sleep(millis);
	}
	
	@Test
	public void testInit() {
		QuicTimer t = new QuicTimer();
		stimer = new TestSessionTimer(null);
		assertFalse(t.isInitialized());
		t.init(stimer, new AwakeningTask());
		assertTrue(t.isInitialized());
		t.cancel();
		assertFalse(t.isInitialized());
	}
	
	@Test
	public void testScheduleTask() {	
		QuicTimer t = new QuicTimer();
		stimer = new TestSessionTimer(null);
		t.scheduleTask(new Task("T1"), 98000000).cancelTask();;
		t.init(stimer, new AwakeningTask());
		t.scheduleTask(new Task("T2"), 98000000);
		assertEquals("TimerTask;98;true|", stimer.trace());
		t.cancel();
		t.scheduleTask(new Task("T2"), 98000000);
		assertEquals("", stimer.trace());
	}
	
	long m2n(long millis) {
		return millis*1000*1000;
	}
	
	@Test
	public void testSchedule() throws Exception {
		QuicTimer t = new QuicTimer();
		t.init(stimer, new AwakeningTask());
		
		ITimerTask task = t.scheduleTask(new Task("T1"), m2n(200));
		waitFor(150);
		assertEquals("", stimer.trace());
		waitFor(100);
		assertEquals("T1|AWK|", stimer.trace());

		//schedule in past
		try {
			task = t.scheduleTask(new Task("T0"), m2n(-1));
			fail();
		}
		catch (Exception e) {
		}
		
		//schedule now
		task = t.scheduleTask(new Task("T2"), 0);
		waitFor(50);
		assertEquals("T2|AWK|", stimer.trace());
		
		//cancel task
		task = t.scheduleTask(new Task("T3"), m2n(200));
		waitFor(150);
		assertEquals("", stimer.trace());
		task.cancelTask();
		waitFor(200);
		assertEquals("", stimer.trace());
		Field f = QuicTimer.TimerTaskWrapper.class.getDeclaredField("task");
		f.setAccessible(true);
		Runnable r = (Runnable) f.get(task);
		r.run();
		assertEquals("", stimer.trace());
		assertTrue(r instanceof IEngineTimerTask);
		
		//cancel timer
		task = t.scheduleTask(new Task("T4"), m2n(200));
		waitFor(150);
		assertEquals("", stimer.trace());
		t.cancel();
		waitFor(200);
		assertEquals("", stimer.trace());	
	}
		
	class AwakeningTask implements Runnable {
		
		@Override
		public void run() {
			stimer.trace("AWK");
		}		
	}

	class Task implements Runnable {

		String name;
		
		Task(String name) {
			this.name = name;
		}
		
		@Override
		public void run() {
			stimer.trace(name);
		}		
	}
}
