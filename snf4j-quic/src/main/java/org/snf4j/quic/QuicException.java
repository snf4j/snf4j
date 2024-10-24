/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic;

import java.io.IOException;

/**
 * An exception indicating some kind of error detected by an QUIC subsystem.
 * This class is the general class of exceptions produced by failed QUIC-related
 * operations.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class QuicException extends IOException {

	private static final long serialVersionUID = 1L;

	private final TransportError error;
	
	private final long errorCode;
	
	/**
	 * Constructs an exception reporting an error found by an QUIC subsystem with
	 * the specified detail message.
	 * <p>
	 * NOTE: This constructor should not be used for transport errors with a defined
	 * range of error codes (e.g. {@link TransportError#CRYPTO_ERROR}).
	 * 
	 * @param error   the transport error
	 * @param message the detail message
	 */
	public QuicException(TransportError error, String message) {
		super(message);
		this.error = error;
		this.errorCode = error.code();
	}

	/**
	 * Constructs an exception reporting an error found by an QUIC subsystem with
	 * the specified detail message and cause.
	 * <p>
	 * NOTE: This constructor should not be used for transport errors with a defined
	 * range of error codes (e.g. {@link TransportError#CRYPTO_ERROR}).
	 * 
	 * @param error   the transport error
	 * @param message the detail message
	 * @param cause   the cause
	 */
	public QuicException(TransportError error, String message, Throwable cause) {
		super(message, cause);
		this.error = error;
		this.errorCode = error.code();
	}
	
	/**
	 * Constructs an exception reporting an error found by an QUIC subsystem with
	 * the specified detail message.
	 * <p>
	 * NOTE: This constructor should be used for transport errors with a defined
	 * range of error codes (e.g. {@link TransportError#CRYPTO_ERROR}).
	 * 
	 * @param error     the transport error
	 * @param errorCode the transport error code
	 * @param message   the detail message
	 */
	public QuicException(TransportError error, long errorCode, String message) {
		super(message);
		this.error = error;
		this.errorCode = errorCode;
	}

	/**
	 * Constructs an exception reporting an error found by an QUIC subsystem with
	 * the specified detail message and cause.
	 * <p>
	 * NOTE: This constructor should be used for transport errors with a defined
	 * range of error codes (e.g. {@link TransportError#CRYPTO_ERROR}).
	 * 
	 * @param error     the transport error
	 * @param errorCode the transport error code
	 * @param message   the detail message
	 * @param cause     the cause
	 */
	public QuicException(TransportError error, long errorCode, String message, Throwable cause) {
		super(message, cause);
		this.error = error;
		this.errorCode = errorCode;
	}
	
	/**
	 * Returns the transport error associated with this exception.
	 * 
	 * @return the transport error
	 */
	public TransportError getTransportError() {
		return error;
	}
	
	/**
	 * Returns the error code associated with this exception.
	 * 
	 * @return the error code
	 */
	public long getErrorCode() {
		return errorCode;
	}
}
