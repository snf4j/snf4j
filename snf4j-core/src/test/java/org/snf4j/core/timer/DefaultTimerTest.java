/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020-2023 SNF4J contributors
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
package org.snf4j.core.timer;

import static org.junit.Assert.*;

import org.junit.Test;

public class DefaultTimerTest {
	
	StringBuilder trace = new StringBuilder();
	
	void trace(String text) {
		synchronized (trace) {
			trace.append(text);
			trace.append('|');
		}
	}
	
	String getTrace() {
		String s;
		
		synchronized (trace) {
			s = trace.toString();
			trace.setLength(0);
		}
		return s;
	}
	
	void waitFor(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
		}
	}
	
	@Test
	public void testConstructor() {
		DefaultTimer timer = new DefaultTimer();
		timer.cancel();
		
		timer.schedule(new TraceTask(), 10);
		waitFor(50);
		String name = getTrace();
		String prefix = "default-timer-";
		assertTrue(name.startsWith(prefix));
		name = name.substring(0, name.indexOf('|'));
		int id = Integer.parseInt(name.substring(prefix.length()));
		timer.schedule(new TraceTask(), 10);
		waitFor(50);
		assertEquals(prefix + id +"|false|", getTrace());
		timer.cancel();
		
		timer = new DefaultTimer(true);
		timer.schedule(new TraceTask(), 10);
		waitFor(50);
		id++;
		assertEquals(prefix + id +"|true|", getTrace());
		timer.cancel();

		timer = new DefaultTimer("test-1");
		timer.schedule(new TraceTask(), 10);
		waitFor(50);
		assertEquals("test-1|false|", getTrace());
		timer.cancel();

		timer = new DefaultTimer("test-2", true);
		timer.schedule(new TraceTask(), 10);
		waitFor(50);
		assertEquals("test-2|true|", getTrace());
		timer.cancel();
		
		timer = new DefaultTimer();
		timer.schedule(new TraceTask(), 10);
		waitFor(50);
		id++;
		assertEquals(prefix + id +"|false|", getTrace());
		timer.cancel();
		
	}
	
	@Test
	public void testSchedule() {
		DefaultTimer timer = new DefaultTimer();
		
		ITimerTask task = timer.schedule(new Task("T1"), 100);
		assertEquals("", getTrace());
		waitFor(80);
		assertEquals("", getTrace());
		waitFor(40);
		assertEquals("T1|", getTrace());
		
		task = timer.schedule(new Task("T2"), 100);
		timer.schedule(new Task("T3"), 100);
		task.cancelTask();
		waitFor(200);
		assertEquals("T3|", getTrace());
		
		task = timer.schedule(new Task("TT1"), 100, 100);
		assertEquals("", getTrace());
		waitFor(80);
		assertEquals("", getTrace());
		waitFor(40);
		assertEquals("TT1|", getTrace());
		waitFor(60);
		assertEquals("", getTrace());
		waitFor(40);
		assertEquals("TT1|", getTrace());
		task.cancelTask();
		waitFor(100);
		assertEquals("", getTrace());

		task = timer.schedule(new Task("TT2"), 100, 100);
		timer.schedule(new Task("TT3"), 100);
		task.cancelTask();
		waitFor(200);
		assertEquals("TT3|", getTrace());
		
		timer.cancel();
		
	}
	
	class TraceTask implements Runnable {
	
		@Override
		public void run() {
			Thread t = Thread.currentThread();
			trace(t.getName());
			trace(""+t.isDaemon());
		}
		
	}

	class Task implements Runnable {
		String id;
		
		Task(String id) {
			this.id = id;
		}
		
		@Override
		public void run() {
			trace(id);
		}
		
	}
}
