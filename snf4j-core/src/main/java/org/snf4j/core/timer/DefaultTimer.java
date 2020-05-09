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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default timer implementation that is backed by the {@link Timer}.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class DefaultTimer implements ITimer {
	
	private static final AtomicInteger nextId = new AtomicInteger(0);
	
	private volatile Timer timer;
	
	private final String name;
	
	private final boolean isDaemon;

	/**
	 * Constructs a new default timer. The associated thread does not run as a
	 * daemon.
	 */
	public DefaultTimer() {
		this(null, false);
	}
	
	/**
	 * Constructs a new default timer whose associated thread may be specified
	 * to run as a daemon.
	 * 
	 * @param isDaemon
	 *            {@code true} if the associated thread should run as a daemon.
	 */
	public DefaultTimer(boolean isDaemon) {
		this(null, isDaemon);
	}

	/**
	 * Constructs a new default timer whose associated thread has the specified
	 * name. The associated thread does not run as a daemon.
	 * 
	 * @param name
	 *            the name of the associated thread
	 */
	public DefaultTimer(String name) {
		this(name, false);
	}
	
	/**
	 * Constructs a new default timer whose associated thread has the specified
	 * name, and may be specified to run as a daemon.
	 * 
	 * @param name
	 *            the name of the associated thread
	 * @param isDaemon
	 *            {@code true} if the associated thread should run as a daemon.
	 */
	public DefaultTimer(String name, boolean isDaemon) {
		this.name = name == null ? "default-timer-" + nextId.incrementAndGet() : name;
		this.isDaemon = isDaemon;
	}
	
	/**
	 * Terminates this timer, discarding any currently scheduled tasks. 
	 */
	public void cancel() {
		if (timer != null) {
			timer.cancel();
		}
	}
	
	private Timer getTimer() {
		if (timer == null) {
			synchronized (this) {
				if (timer == null) {
					timer = new Timer(name, isDaemon);
				}
			}
		}
		return timer;
	}
	
	@Override
	public ITimerTask schedule(Runnable task, long delay) {
		Task t = new Task(task);
		
		getTimer().schedule(t, delay);
		return t;
	}
	
	/**
	 * {@inheritDoc} The task is repeated by using fixed-delay execution what
	 * means that each execution is scheduled relative to the actual execution
	 * time of the previous execution.
	 */
	@Override
	public ITimerTask schedule(Runnable task, long delay, long period) {
		Task t = new Task(task);
		
		getTimer().schedule(t, delay, period);
		return t;
	}
	
	private static class Task extends TimerTask implements ITimerTask {
		Runnable task;
		
		Task(Runnable task) {
			this.task = task;
		}

		@Override
		public void run() {
			task.run();
		}

		@Override
		public void cancelTask() {
			cancel();
		}
	}
}
