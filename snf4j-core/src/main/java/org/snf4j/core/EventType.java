/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2018 SNF4J contributors
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

/**
 * An <code>enum</code> that represents all session related event types.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public enum EventType {

	/**
	 * The session has just been created. After receiving this event type the
	 * session is still in the <code>SessionState.OPENING</code> state.
	 * 
	 * @since 1.0
	 * @see org.snf4j.core.session.SessionState
	 */
	// Session has assigned channel
	SESSION_CREATED(1, 15, 0),
	
	/**
	 * The session has just been opened. After receiving this event type the
	 * session is in the <code>SessionState.OPEN</code> state.
	 * 
	 * @since 1.0
	 * @see org.snf4j.core.session.SessionState
	 */
	// Session has channel registered with a selector
	SESSION_OPENED(2, 15, 1), 
	
	/**
	 * The session has just been closed. After receiving this event type the
	 * session is in the <code>SessionState.CLOSING</code> state. This event is
	 * only fired if the session was previously in the
	 * <code>SessionState.OPEN</code> state. It is always followed by the
	 * <code>SESSION_ENDING</code> event type.
	 * 
	 * @since 1.0
	 * @see org.snf4j.core.session.SessionState
	 */
	SESSION_CLOSED(4, 15, 3), 
	
	/**
	 * The session is about to end. This is the last event type fired and after
	 * receiving it the session is ready to be garbage collected.
	 * 
	 * @since 1.0
	 */
	SESSION_ENDING(8, 9, 1),
	
	/**
	 * Some data has just been received from the remote end.
	 * 
	 * @since 1.0
	 */
	DATA_RECEIVED(16, 15, 3),
	
	/**
	 * Some data has just been sent to the remote end.
	 * 
	 * @since 1.0
	 */
	DATA_SENT(32, 15, 3), 
	
	/**
	 * An exception has just been caught.
	 * 
	 * @since 1.0
	 */
	EXCEPTION_CAUGHT(64, 9, 1),
	
	/**
	 * The session is fully initialized and is ready to send/receive user data.
	 * 
	 * @since 1.0
	 * @see org.snf4j.core.session.SessionState
	 */	
	SESSION_READY(128, 143, 3); 
	
	private int bitMask, expectedMask, expectedValue;
	
	private EventType(int bitMask, int expectedMask, int expectedValue) {
		this.bitMask = bitMask;
		this.expectedMask = expectedMask;
		this.expectedValue = expectedValue; 
	}
	
	int bitMask() { return bitMask; }
	
	boolean isValid(int currentValue) {
		return (currentValue & expectedMask) == expectedValue;
	}
}
