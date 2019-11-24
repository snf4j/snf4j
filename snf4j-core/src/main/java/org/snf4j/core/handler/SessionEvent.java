/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2019 SNF4J contributors
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
package org.snf4j.core.handler;

import org.snf4j.core.EventType;

/**
 * An <code>enum</code> that represents session events related with changes of the session state.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public enum SessionEvent {
	/** 
	 * The session has just been created. 
	 * After receiving this event the session is still in the <code>SessionState.OPENING</code> state. 
	 * @since 1.0
	 * @see org.snf4j.core.session.SessionState
	 */
	CREATED(EventType.SESSION_CREATED),
	
	/**
	 * The session has just been opened.
	 * After receiving this event the session is in the <code>SessionState.OPEN</code> state.
	 * @since 1.0
	 * @see org.snf4j.core.session.SessionState
	 */
	OPENED(EventType.SESSION_OPENED),
	
	/**
	 * The session is fully initialized and is ready to send/receive user data.
	 * 
	 * @since 1.0
	 * @see org.snf4j.core.session.SessionState
	 */		
	READY(EventType.SESSION_READY),
	
	/**
	 * The session has just been closed.
	 * After receiving this event the session is in the <code>SessionState.CLOSING</code> state. This
	 * event is only fired if the session was previously in the <code>SessionState.OPEN</code> state. It is
	 * always followed by the <code>SESSION_ENDING</code> event.
	 * @since 1.0
	 * @see org.snf4j.core.session.SessionState
	 */
	CLOSED(EventType.SESSION_CLOSED),
	
	/**
	 * The session is about to end. This is the last event fired and after receiving it the session is
	 * ready to be garbage collected.
	 * @since 1.0
	 */
	ENDING(EventType.SESSION_ENDING);
	
	EventType type;
	
	private SessionEvent(EventType type) {
		this.type = type;
	}
	
	public EventType type() {
		return type;
	}
}
