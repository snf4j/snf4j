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
package org.snf4j.quic.crypto;

import javax.crypto.SecretKey;

/**
 * A holder for client and server secret keys.

 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class SecretKeys {
	
	private SecretKey clientKey;
	
	private SecretKey serverKey;

	/**
	 * Constructs a holder with client and server secret keys.
	 * 
	 * @param clientKey the client secret key
	 * @param serverKey the server secret key
	 */
	public SecretKeys(SecretKey clientKey, SecretKey serverKey) {
		this.clientKey = clientKey;
		this.serverKey = serverKey;
	}

	/**
	 * Constructs a holder with a client secret key.
	 * 
	 * @param clientKey the client secret key
	 */
	public SecretKeys(SecretKey clientKey) {
		this.clientKey = clientKey;
	}

	/**
	 * Returns the client or server secret key.
	 * 
	 * @param client {@code true} for the client secret key
	 * @return the requested secret key, or {@code null} if the key does not exist or
	 *         has been cleared
	 */
	public SecretKey getKey(boolean client) {
		return client ? clientKey : serverKey;
	}

	/**
	 * Returns the client secret key.
	 * 
	 * @return the client secret key, or {@code null} if the key does not exist or
	 *         has been cleared
	 */
	public SecretKey getClientKey() {
		return clientKey;
	}

	/**
	 * Returns the server secret key.
	 * 
	 * @return the client secret key, or {@code null} if the key does not exist or
	 *         has been cleared
	 */
	public SecretKey getServerKey() {
		return serverKey;
	}
	
	/**
	 * Clears this holder. 
	 * <p>
	 * NOTE: Calling this method does not destroy the keys.  
	 */
	public void clear() {
		clientKey = null;
		serverKey = null;
	}
		
}
