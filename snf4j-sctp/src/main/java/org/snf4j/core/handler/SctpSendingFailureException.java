/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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

import java.nio.ByteBuffer;

import org.snf4j.core.ImmutableSctpMessageInfo;

/**
 * An exception passed to the {@link org.snf4j.core.handler.IHandler#incident}
 * method along with the
 * {@link org.snf4j.core.handler.SessionIncident#SCTP_SENDING_FAILURE
 * SCTP_SENDING_FAILURE} incident.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class SctpSendingFailureException extends Exception {
	
	private static final long serialVersionUID = -8776550059570729769L;

	private final ImmutableSctpMessageInfo msgInfo;
	
	private final ByteBuffer buffer;
	
	/**
	 * Constructs an exception with the message that was to be sent along with the
	 * immutable ancillary data about the message and the cause of the failure.
	 * 
	 * @param buffer  the message that was to be sent
	 * @param msgInfo the immutable ancillary data about the message that was to be
	 *                sent
	 * @param cause   the cause of the failure
	 */
	public SctpSendingFailureException(ByteBuffer buffer, ImmutableSctpMessageInfo msgInfo, Throwable cause) {
		super(cause);
		this.msgInfo = msgInfo;
		this.buffer = buffer;
	}
	
	/**
	 * Returns the immutable ancillary data about the message that was to be sent
	 * 
	 * @return the immutable ancillary data about the message
	 */
	public ImmutableSctpMessageInfo getMessageInfo() {
		return msgInfo;
	}
	
	/**
	 * Returns the message that was to be sent.
	 * 
	 * @return the message
	 */
	public ByteBuffer getBuffer() {
		return buffer;
	}
}
