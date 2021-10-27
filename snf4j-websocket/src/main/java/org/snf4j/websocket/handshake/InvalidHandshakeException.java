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
package org.snf4j.websocket.handshake;

import org.snf4j.core.ICloseControllingException;

/**
 * Indicates problems with decoding or validation of Web Socket handshake frames.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class InvalidHandshakeException extends RuntimeException implements ICloseControllingException {

	private static final long serialVersionUID = -6453657199661910834L;

	private final CloseType closeType;
	
	/**
	 * Constructs a new exception with the specified detail message, cause and
	 * session closing type.
	 *
	 * @param message     the detail message.
	 * @param cause       the cause
	 * @param gentleClose the session closing type. If {@code true} the affected
	 *                    session will be gently closed, or otherwise the default
	 *                    closing will be performed
	 */
	public InvalidHandshakeException(String message, Throwable cause, boolean gentleClose) {
		super(message, cause);
		closeType = gentleClose ? CloseType.GENTLE : CloseType.DEFAULT; 
	}

	/**
	 * Constructs a new exception with the specified cause and session closing type.
	 *
	 * @param cause       the cause
	 * @param gentleClose the session closing type. If {@code true} the affected
	 *                    session will be gently closed, or otherwise the default
	 *                    closing will be performed
	 */
	public InvalidHandshakeException(Throwable cause, boolean gentleClose) {
		this(cause == null ? null : cause.getMessage(), cause, gentleClose);
	}
	
	/**
	 * Constructs a new exception with the specified detail message, cause and
	 * with the default closing type.
	 *
	 * @param message     the detail message.
	 * @param cause       the cause
	 */
	public InvalidHandshakeException(String message, Throwable cause) {
		this(message, cause, false);
	}
	
	/**
	 * Constructs a new exception with the specified detail message and with the
	 * default closing type.
	 *
	 * @param message the detail message.
	 */
	public InvalidHandshakeException(String message) {
		this(message, null);
	}
	
	/**
	 * Constructs a new exception without the detail message and with the default
	 * closing type.
	 */
	public InvalidHandshakeException() {
		this(null, null);
	}

	/**
	 * Constructs a new exception with the specified cause and with the default
	 * closing type.
	 *
	 * @param cause the cause
	 */
	public InvalidHandshakeException(Throwable cause) {
		this(cause == null ? null : cause.getMessage(), cause);
	}

	@Override
	public CloseType getCloseType() {
		return closeType;
	}

	@Override
	public Throwable getClosingCause() {
		return this;
	}
}
