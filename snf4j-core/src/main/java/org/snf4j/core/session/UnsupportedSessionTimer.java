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
 * Unsupported session timer. It is associated with sessions that do not support
 * the session timer.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class UnsupportedSessionTimer implements ISessionTimer {

	private final static String MSG = "session timer is not configured";

	/**
	 * A constant holding the instance of the unsupported session timer.
	 */
	public final static ISessionTimer INSTANCE = new UnsupportedSessionTimer();
	
	private UnsupportedSessionTimer() {
	}
	
	/**
	 * @return {@code false}
	 */
	@Override
	public boolean isSupported() {
		return false;
	}
	
	/**
	 * Always throws the UnsupportedOperationException.
	 */
	@Override
	public ITimerTask scheduleTask(Runnable task, long delay, boolean inHandler) {
		throw new UnsupportedOperationException(MSG);
	}

	/**
	 * Always throws the UnsupportedOperationException.
	 */
	@Override
	public ITimerTask scheduleEvent(Object event, long delay) {
		throw new UnsupportedOperationException(MSG);
	}

	/**
	 * Always throws the UnsupportedOperationException.
	 */
	@Override
	public ITimerTask scheduleTask(Runnable task, long delay, long period,
			boolean inHandler) {
		throw new UnsupportedOperationException(MSG);
	}

	/**
	 * Always throws the UnsupportedOperationException.
	 */
	@Override
	public ITimerTask scheduleEvent(Object event, long delay, long period) {
		throw new UnsupportedOperationException(MSG);
	}
		
}
