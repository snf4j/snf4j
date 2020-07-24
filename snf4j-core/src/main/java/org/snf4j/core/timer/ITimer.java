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

import org.snf4j.core.session.ISessionTimer;

/**
 * A timer that can be used by the default implementation of
 * {@link ISessionTimer} to provide the scheduling functionality.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ITimer {
	
	/**
	 * Schedules the specified task for execution after the specified delay.
	 * 
	 * @param task
	 *            the task to be scheduled for execution
	 * @param delay
	 *            the delay in milliseconds
	 * @return the timer task
	 */
	ITimerTask schedule(Runnable task, long delay);
	
	/**
	 * Schedules the specified task for repeated execution with the specified
	 * initial delay.
	 * 
	 * @param task
	 *            the task to be scheduled for execution
	 * @param delay
	 *            the delay in milliseconds
	 * @param period
	 *            time in milliseconds between successive task executions
	 * @return the timer task
	 */
	ITimerTask schedule(Runnable task, long delay, long period);
}
