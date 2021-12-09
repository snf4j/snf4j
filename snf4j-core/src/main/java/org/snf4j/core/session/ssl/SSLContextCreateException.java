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
package org.snf4j.core.session.ssl;

/**
 * Indicates problems with creation of the SSL context.
 *  
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class SSLContextCreateException extends Exception {
	
	private static final long serialVersionUID = -8805355746177801884L;

	/**
	 * Constructs a new exception without the detail message.
	 */
    public SSLContextCreateException() {
        super();
    }

	/**
	 * Constructs a new exception with the specified detail message.
	 *
	 * @param message
	 *            the detail message.
	 */
    public SSLContextCreateException(String message) {
        super(message);
    }

	/**
	 * Constructs a new exception with the specified detail message and cause.
	 *
	 * @param message
	 *            the detail message.
	 * @param cause
	 *            the cause
	 */
    public SSLContextCreateException(String message, Throwable cause) {
        super(message, cause);
    }

	/**
	 * Constructs a new exception with the specified cause and a detail message
	 * of <tt>(cause==null ? null : cause.toString())</tt> (which typically
	 * contains the class and detail message of <tt>cause</tt>). This
	 * constructor is useful for exceptions that are little more than wrappers
	 * for other throwables.
	 *
	 * @param cause
	 *            the cause
	 */
    public SSLContextCreateException(Throwable cause) {
        super(cause);
    }	

}
