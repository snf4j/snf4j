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
package org.snf4j.core;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.junit.Test;
import org.snf4j.core.timer.ITimerTask;

public class AbstractSessionTimerTest {
	
	@Test
	public void testConstructor() throws Exception {
		TestStreamSession session1 = new TestStreamSession(new TestHandler(""));
		TestDatagramSession session2 = new TestDatagramSession(new TestDatagramHandler());
		
		TestSessionTimer timer = new TestSessionTimer(session1);
		Field f = AbstractSessionTimer.class.getDeclaredField("session");
		f.setAccessible(true);
		assertTrue(session1 == f.get(timer));
		
		timer = new TestSessionTimer(session2);
		f.setAccessible(true);
		assertTrue(session2 == f.get(timer));
	}
	
	static class TestSessionTimer extends AbstractSessionTimer {

		protected TestSessionTimer(DatagramSession session) {
			super(session);
		}
		
		protected TestSessionTimer(StreamSession session) {
			super(session);
		}

		@Override
		public boolean isSupported() {
			return false;
		}

		@Override
		public ITimerTask scheduleTask(Runnable task, long delay,
				boolean inHandler) {
			return null;
		}

		@Override
		public ITimerTask scheduleTask(Runnable task, long delay, long period,
				boolean inHandler) {
			return null;
		}

		@Override
		public ITimerTask scheduleEvent(Object event, long delay) {
			return null;
		}

		@Override
		public ITimerTask scheduleEvent(Object event, long delay, long period) {
			return null;
		}
		
	}
}
