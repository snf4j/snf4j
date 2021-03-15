/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019-2021 SNF4J contributors
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

/**
 * An <code>enum</code> that represents session incidents that may occur during processing
 * of I/O or protocol related operations.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public enum SessionIncident {

	/**
	 * SSL/TLS connection closed by peer without sending close_notify. It may
	 * indicate a possibility of an truncation attack.
	 * <p>
	 * <b>Default action</b>: the default message is logged at the WARN level. 
	 */
	SSL_CLOSED_WITHOUT_CLOSE_NOTIFY("SSL/TLS close procedure not properly followed by peer for {}: {}"),
	
	/**
	 * A connection closed by peer without sending proper close message.
	 * <p>
	 * <b>Default action</b>: the default message is logged at the WARN level. 
	 */
	CLOSED_WITHOUT_CLOSE_MESSAGE("Close procedure not properly followed by peer for {}: {}"),
	
	/**
	 * A failure occurred while encoding data passed to write/send methods.
	 * <p>
	 * <b>Default action</b>: the default message is logged at the ERROR level. 
	 */
	ENCODING_PIPELINE_FAILURE("Encoding pipeline failed for {}: {}"),
	
	/**
	 * A failure occurred while decoding data received from a remote peer. This
	 * incident is only reported for the datagram-oriented sessions. 
	 * <p>
	 * NOTE: If such failure occurs while decoding data for the stream-oriented 
	 * sessions then only an exception is reported. 
	 * <p>
	 * <b>Default action</b>: the default message is logged at the ERROR level. 
	 */
	DECODING_PIPELINE_FAILURE("Decoding pipeline failed for {}: {}"),
	
	/**
	 * A failure occurred while sending a message via the SCTP channel. This
	 * incident is only reported for the SCTP multi sessions for all exceptions
	 * not being an instance of the {@link java.io.IOException}.
	 * <p>
	 * <b>Default action</b>: the default message is logged at the ERROR level. 
	 */
	SCTP_SENDING_FAILURE("Sending via SCTP channel failed for {}: {}");
	
	private String defaultMessage;
	
	private SessionIncident(String defaultMessage) {
		this.defaultMessage = defaultMessage;
	}
	
	/**
	 * Gets the default warning or error message that will be logged when an
	 * implementation of {@link IHandler#incident} method returns
	 * <code>false</code>.
	 * 
	 * @return the default warning or error message
	 */
	public String defaultMessage() {
		return defaultMessage;
	}
}
