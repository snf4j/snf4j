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

import org.snf4j.core.engine.IEngineTimerTask;
import org.snf4j.core.session.ISessionTimer;
import org.snf4j.core.timer.ITimerTask;

/**
 * The default QUIC timer.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class QuicTimer {

	private final static ITimerTask CANCELED_TIMER_TASK = new ITimerTask() {

		@Override
		public void cancelTask() {
		}
	};
	
	private ISessionTimer timer;
	
	private Runnable awakeningTask;

	/**
	 * Constructs a QUIC timer.
	 */
	public QuicTimer() {
	}

	/**
	 * Initializes this QUIC timer with the given session timer and awakening task.
	 * 
	 * @param timer         the session timer
	 * @param awakeningTask the task that should be used for awakening the engine
	 *                      handler. It should be run after completing of a
	 *                      scheduled task to pass the control back to the engine
	 *                      handler
	 */
	public void init(ISessionTimer timer, Runnable awakeningTask) {
		this.timer = timer;
		this.awakeningTask = awakeningTask;
	}
	
	/**
	 * Cancels this QUIC timer along with all scheduled tasks.
	 */
	public void cancel() {
		timer = null;
		awakeningTask = null;
	}
	
	/**
	 * Tells if this QUIC timer has been initialized.
	 * 
	 * @return {@code true} if the timer has been initialized
	 */
	public boolean isInitialized() {
		return timer != null;
	}
	
	/**
	 * Schedules the specified task for execution after the specified delay in
	 * nanoseconds.
	 * <p>
	 * NOTE: All scheduled tasks will be run in the same I/O thread that is
	 * used to handle the I/O events of the associated session.
	 * 
	 * @param task  the task to be scheduled
	 * @param delay the delay in nanoseconds
	 * @return the timer task that is associated with the scheduled task
	 */
	public ITimerTask scheduleTask(Runnable task, long delay) {
		if (timer != null) {
			TimerTask timerTask = new TimerTask(task);

			return new TimerTaskWrapper(timer.scheduleTask(timerTask, delay/1000000, true), timerTask);
		}
		return CANCELED_TIMER_TASK;
	}
	
	static class TimerTaskWrapper implements ITimerTask {

		private final ITimerTask delegate;

		private final TimerTask task;
		
		TimerTaskWrapper(ITimerTask delegate, TimerTask task) {
			this.delegate = delegate;
			this.task = task;
		}

		@Override
		public void cancelTask() {
			task.cancel();
			delegate.cancelTask();
		}
	}
	
	class TimerTask implements Runnable, IEngineTimerTask {

		private final Runnable task;
		
		private boolean canceled;
		
		TimerTask(Runnable task) {
			this.task = task;
		}
		
		void cancel() {
			canceled = true;
		}
		
		@Override
		public void run() {
			if (!canceled && awakeningTask != null) {
				task.run();
				awakeningTask.run();
			}
		}
	}
}
