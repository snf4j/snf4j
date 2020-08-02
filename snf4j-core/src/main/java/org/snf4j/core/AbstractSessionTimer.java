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

import org.snf4j.core.handler.IHandler;
import org.snf4j.core.session.ISessionTimer;

/**
 * Base implementation of the {@link ISessionTimer} interface. It provides
 * mechanisms for integration with the session's handler.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
abstract public class AbstractSessionTimer implements ISessionTimer {

	private final InternalSession session;

	AbstractSessionTimer(InternalSession session) {
		this.session = session;
	}

	/**
	 * Constructs a session timer that can be associated with stream-oriented
	 * sessions.
	 * 
	 * @param session
	 *            the session to which this the timer is to be associated with
	 */
	protected AbstractSessionTimer(StreamSession session) {
		this((InternalSession)session);
	}
	
	/**
	 * Constructs a session timer that can be associated with datagram-oriented
	 * sessions.
	 * 
	 * @param session
	 *            the session to which this the timer is to be associated with
	 */
	protected AbstractSessionTimer(DatagramSession session) {
		this((InternalSession)session);
	}
	
	/**
	 * Wraps the specified event object into a task object that can be used for
	 * delivering the event object to the session's handler. The return task can
	 * be safely executed in any thread and it guarantees that
	 * {@link IHandler#timer(Object)} will be always called in selector loop's
	 * thread that handles the associated session.
	 * 
	 * @param event
	 *            the event object to be wrapped
	 * @return the task object that wraps the event object
	 */
	protected final Runnable wrapEvent(Object event) {
		return new Task(event);
	}
	
	/**
	 * Wraps the specified task into a task object that can be used for
	 * delivering the specified task to the session's handler. The return task
	 * can be safely executed in any thread and it guarantees that
	 * {@link IHandler#timer(Runnable)} will be always called in selector loop's
	 * thread that handles the associated session.
	 * <p>
	 * The return wrapping task will never execute the task being wrapped. It
	 * will only deliver it safely to the hadnler's method.
	 * 
	 * @param task
	 *            the task to be wrapped
	 * @return the task object that wraps the specified task
	 */
	protected final Runnable wrapTask(Runnable task) {
		return new Task(task);
	}
	
	private class Task implements Runnable {

		final private Object event;
		
		final private Runnable task;
		
		private Task(Object event) {
			this.event = event;
			task = null;
		}
		
		private Task(Runnable task) {
			this.task = task;
			event = null;
		}
		
		@Override
		public void run() {
			if (session.loop.inLoop()) {
				if (event != null) {
					session.timer(event);
				}
				else {
					session.timer(task);
				}
			}
			else {
				session.loop.execute0(this);
			}
		}
	}	
}
