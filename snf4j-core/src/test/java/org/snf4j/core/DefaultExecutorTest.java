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
package org.snf4j.core;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class DefaultExecutorTest {

	AtomicBoolean lock = new AtomicBoolean(false);
	
	volatile Thread thread;
	
	class TestRunnable implements Runnable {

		String name; 
		
		TestRunnable(String name) {
			this.name = name;
		}
		
		@Override
		public void run() {
			thread = Thread.currentThread();
			LockUtils.notify(lock);
		}
		
		@Override
		public String toString() {
			return name;
		}
	};

	@Test
	public void testExecute() throws InterruptedException {
		DefaultExecutor.DEFAULT.execute(new TestRunnable("Test1"));
		LockUtils.waitFor(lock, 2000);
		Thread th1 = thread;
		thread = null;
		DefaultExecutor.DEFAULT.execute(new TestRunnable("Test2"));
		LockUtils.waitFor(lock, 2000);
		Thread th2 = thread;
		
		assertNotNull(th1);
		assertNotNull(th2);
		assertFalse(th1 == th2);
		assertEquals("Test1", th1.getName());
		assertEquals("Test2", th2.getName());
	}
}
