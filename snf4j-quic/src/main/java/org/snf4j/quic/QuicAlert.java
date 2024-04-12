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

import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.AlertDescription;

/**
 * A special alert used to report errors from TLS subsystem that should be
 * mapped to a specific QUIC error.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class QuicAlert extends Alert {

	private static final long serialVersionUID = 1L;

	private final static AlertDescription DESCRIPTION = new AlertDescription("quic_error", -1) {}; 
	
	private final TransportError error;
	
	/**
	 * Constructs a TLS alert that should be mapped to the given QUIC transport
	 * error.
	 * 
	 * @param error   the QUIC transport error
	 * @param message the detail message
	 */
	public QuicAlert(TransportError error, String message) {
		super(message, DESCRIPTION);
		this.error = error;
	}

	/**
	 * Return the QUIC transport error this alert should be mapped to.
	 * 
	 * @return the QUIC transport error
	 */
	public TransportError getTransportError() {
		return error;
	}

}
