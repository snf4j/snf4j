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

	private final ImmutableSctpMessageInfo messageInfo;
	
	public SctpSendingFailureException(ImmutableSctpMessageInfo messageInfo, Throwable cause) {
		super(cause);
		this.messageInfo = messageInfo;
	}
	
	/**
	 * Returns the immutable SCTP message info object that was passed to the failing
	 * send method of the SCTP channel.
	 * 
	 * @return the immutable SCTP message info object
	 */
	public ImmutableSctpMessageInfo getMessageInfo() {
		return messageInfo;
	}
}
