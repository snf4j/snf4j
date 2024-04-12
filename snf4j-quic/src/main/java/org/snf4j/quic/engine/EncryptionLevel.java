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
package org.snf4j.quic.engine;

/**
 * The encryption levels that identify keys being used for data and header
 * protection.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public enum EncryptionLevel {

	/** Initial keys */
	INITIAL(0),
	
	/** Early data (0-RTT) keys */
	EARLY_DATA(-1),
	
	/** Handshake keys */
	HANDSHAKE(1),
	
	/** Application data (1-RTT) keys */
	APPLICATION_DATA(2);
	
	private final int cryptoOrdinal;
	
	private EncryptionLevel(int cryptoOrdinal) {
		this.cryptoOrdinal = cryptoOrdinal;
	}
	
	/**
	 * Returns the ordinal of the cryptographic data space associated with this
	 * encryption level.
	 * 
	 * @return the ordinal of the cryptographic data space, or -1 if this encryption
	 *         level is not used for carrying the cryptographic data.
	 */
	public int cryptoOrdinal() {
		return cryptoOrdinal;
	}
}
