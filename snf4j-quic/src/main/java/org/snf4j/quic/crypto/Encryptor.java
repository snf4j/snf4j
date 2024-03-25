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

import org.snf4j.tls.crypto.IAeadEncrypt;

/**
 * The default QUIC encryptor.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class Encryptor extends Cryptor {

	private final IAeadEncrypt aead;
	
	/**
	 * Constructs a QUIC encryptor with the given AEAD encryptor, header protector,
	 * initialization vector and confidentiality limit.
	 * 
	 * @param aead                 the AEAD encryptor
	 * @param protector            the header protector
	 * @param iv                   the initialization vector
	 * @param confidentialityLimit the number of packets that can be encrypted with
	 *                             a given key
	 */
	public Encryptor(IAeadEncrypt aead, HeaderProtector protector, byte[] iv, long confidentialityLimit) {
		super(protector, iv, aead.getAead().getTagLength(), confidentialityLimit);
		this.aead = aead;
	}

	/**
	 * Returns the associated AEAD encryptor.
	 * @return the associated AEAD encryptor
	 */
	public IAeadEncrypt getAead() {
		return aead;
	}

	@Override
	public void erase(boolean skipProtector) {
		super.erase(skipProtector);
		aead.erase();
	}	

}
