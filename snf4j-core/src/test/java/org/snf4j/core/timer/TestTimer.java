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
package org.snf4j.core.timer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;

public class TestTimer extends DefaultTimer {
	
	List<TaskWrapper> tasks = new ArrayList<TaskWrapper>();
	
	List<TaskWrapper> expired = new ArrayList<TaskWrapper>();
	
	StringBuilder trace = new StringBuilder();
	
	Set<Long> delays = new HashSet<Long>();
	
	private boolean updated;
	
	void update() {
		if (updated) {
			return;
		}
		updated = true;
		synchronized (tasks) {
			Iterator<TaskWrapper> i = tasks.iterator();
			
			while (i.hasNext()) {
				TaskWrapper task = i.next();
				
				if (!((TimerTask)task.task).cancel()) {
					expired.add(task);
					i.remove();
				}
			}
		}
	}
	
	void trace(TaskWrapper task, boolean cancel) {
		synchronized(trace) {
			if (cancel) {
				trace.append("c");
			}
			trace.append(task.delay);
			trace.append("|");
		}
	}
	
	public String getTrace(boolean clear) {
		String s;
		
		synchronized (trace) {
			s = trace.toString();
			if (clear) {
				trace.setLength(0);
			}
		}
		return s;
	}
	
	public Long[] getSortedDelays() {
		Long[] b = delays.toArray(new Long[delays.size()]);
		Arrays.sort(b);
		return b;
	}
	
	public String getDelays() {
		StringBuilder sb = new StringBuilder();

		for (Long l: getSortedDelays()) {
			sb.append(l);
			sb.append("|");
		}
		return sb.toString();
	}
	
	public String get() {
		update();
		StringBuilder sb = new StringBuilder();
		
		for (TaskWrapper task: tasks) {
			sb.append(task.delay);
			sb.append("|");
		}
		return sb.toString();
	}
	
	public int getSize() {
		update();
		return tasks.size();
	}
	
	public String getExpired() {
		update();
		StringBuilder sb = new StringBuilder();
		
		for (TaskWrapper task: expired) {
			sb.append(task.delay);
			sb.append("|");
		}
		return sb.toString();
	}
	
	public int getExpiredSize() {
		update();
		return expired.size();
	}
	
	@Override
	public ITimerTask schedule(Runnable task, long delay) {
		delays.add(delay);
		return new TaskWrapper(super.schedule(task, delay), delay);
	}

	class TaskWrapper implements ITimerTask {
		ITimerTask task;
		long delay;
		
		TaskWrapper(ITimerTask task, long delay) {
			this.task = task;
			this.delay = delay;
			synchronized (tasks) {
				tasks.add(this);
			}
			trace(this, false);
		}
		
		@Override
		public void cancelTask() {
			task.cancelTask();
			synchronized (tasks) {
				for (int i=0; i<tasks.size(); ++i) {
					if (this == tasks.get(i)) {
						tasks.remove(i);
						break;
					}
				}
			}
			trace(this, true);
		}
		
	}
}
