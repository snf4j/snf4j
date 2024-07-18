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

import org.snf4j.core.session.ISessionTimer;
import org.snf4j.core.timer.DefaultTimer;
import org.snf4j.core.timer.ITimerTask;

public class TestSessionTimer implements ISessionTimer {
	
	DefaultTimer timer;
	
	final StringBuilder trace = new StringBuilder();
	
	synchronized void trace(String s) {
		trace.append(s).append('|');
	}
	
	public synchronized String trace() {
		String s = trace.toString();
		trace.setLength(0);
		return s;
	}
	
	public TestSessionTimer(DefaultTimer timer) {
		this.timer = timer;
	}
	
	@Override
	public boolean isSupported() {
		return true;
	}

	@Override
	public ITimerTask scheduleTask(Runnable task, long delay, boolean inHandler) {
		if (timer != null) {
			return timer.schedule(task, delay);
		}
		trace(task.getClass().getSimpleName() + ";" + delay +";" + inHandler);
		return new CanceledTask();
	}

	@Override
	public ITimerTask scheduleTask(Runnable task, long delay, long period, boolean inHandler) {
		trace("ERR");
		return new CanceledTask();
	}

	@Override
	public ITimerTask scheduleEvent(Object event, long delay) {
		trace("ERR");
		return new CanceledTask();
	}

	@Override
	public ITimerTask scheduleEvent(Object event, long delay, long period) {
		trace("ERR");
		return new CanceledTask();
	}		
	
	class CanceledTask implements ITimerTask {

		@Override
		public void cancelTask() {
			trace("CANCEL");
		}
	}
}
