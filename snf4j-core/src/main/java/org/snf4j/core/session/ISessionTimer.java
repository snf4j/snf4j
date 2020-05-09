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
package org.snf4j.core.session;

import org.snf4j.core.timer.ITimerTask;

/**
 * A timer that can be associated with a session.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ISessionTimer {
	
	/**
	 * Tells if the implementation supports the session timer.
	 * 
	 * @return {@code true} if the session timer is supported.
	 */
	boolean isSupported();
	
	/**
	 * Schedules the specified task for execution after the specified delay.
	 * 
	 * @param task
	 *            the task to be scheduled
	 * @param delay
	 *            the delay in milliseconds
	 * @param inHandler
	 *            if {@code true} then the task will be passed to the session's
	 *            handler, otherwise the task will be executed in the internal timer's
	 *            thread.
	 * @return the timer task that is associated with the scheduled task
	 */
	ITimerTask scheduleTask(Runnable task, long delay, boolean inHandler);

	/**
	 * Schedules the specified task for repeated execution with the specified
	 * initial delay. The way the task is repeated (fixed-delay or fixed-rate)
	 * is determined by the internal timer's implementation.
	 * 
	 * @param task
	 *            the task to be scheduled
	 * @param delay
	 *            the delay in milliseconds
	 * @param period
	 *            time in milliseconds between successive task executions
	 * @param inHandler
	 *            if {@code true} then the task will be passed to the session's
	 *            handler, otherwise the task will be executed in the timer's
	 *            thread.
	 * @return the timer task that is associated with the scheduled task
	 */
	ITimerTask scheduleTask(Runnable task, long delay, long period, boolean inHandler);
	
	/**
	 * Schedules the specified timer event to be triggered in the session's
	 * handler after the specified delay.
	 * 
	 * @param event
	 *            the event to be scheduled
	 * @param delay
	 *            the delay in milliseconds
	 * @return the timer task that is associated with the scheduled event
	 */
	ITimerTask scheduleEvent(Object event, long delay);
	
	/**
	 * Schedules the specified timer event for repeated triggering in the
	 * session's handler with the specified initial delay. The way the timer
	 * event is repeatedly triggered (fixed-delay or fixed-rate) is determined
	 * by the internal timer's implementation.
	 * 
	 * @param event
	 *            the event to be scheduled
	 * @param delay
	 *            the delay in milliseconds
	 * @param period
	 *            time in milliseconds between successive timer event triggering 
	 * @return the timer task that is associated with the scheduled task
	 */
	ITimerTask scheduleEvent(Object event, long delay, long period);
	
}
