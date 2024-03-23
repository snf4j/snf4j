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
package org.snf4j.quic.engine.crypto;

import org.snf4j.quic.TransportError;
import org.snf4j.quic.QuicException;
import org.snf4j.tls.alert.Alert;

/**
 * An exception indicating some kind of error detected by an QUIC subsystem during TLS handshake.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class CryptoException extends QuicException {

	private static final long serialVersionUID = 1L;

	private final Alert alert;
	
	/**
	 * Constructs a TLS handshake exception with the specified TLS alert and detail
	 * message.
	 * 
	 * @param alert   the TLS alert
	 * @param message the detail message
	 */
	public CryptoException(Alert alert, String message) {
		super(TransportError.CRYPTO_ERROR, TransportError.CRYPTO_ERROR.code() + alert.getDescription().value(), message, alert);
		this.alert = alert;
	}

	/**
	 * Constructs a TLS handshake exception with the specified TLS alert.
	 * 
	 * @param alert the TLS alert
	 */
	public CryptoException(Alert alert) {
		this(alert, "Crypto error: " + alert.getMessage());
	}

	/**
	 * Returns the TLS alert associated with this exception
	 * 
	 * @return the TLS alert
	 */
	public Alert getAlert() {
		return alert;
	}
}
