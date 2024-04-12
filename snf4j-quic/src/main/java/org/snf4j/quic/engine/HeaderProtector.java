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

import java.security.GeneralSecurityException;
import java.security.KeyException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

import org.snf4j.quic.crypto.AeadSpec;
import org.snf4j.quic.crypto.IHeaderProtection;
import org.snf4j.tls.crypto.AeadId;
import org.snf4j.tls.crypto.IAead;

/**
 * The default QUIC header protector.
 *  
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class HeaderProtector {
	
	private final SecretKey key;
	
	private final Cipher cipher;
	
	private final IHeaderProtection protection;

	/**
	 * Constructs a QUIC header protector for the given AEAD algorithm identifier
	 * and secret key.
	 * 
	 * @param aeadId the AEAD algorithm identifier
	 * @param key    the secret key
	 * @throws GeneralSecurityException if a general security error occurred during
	 *                                  creation of the associated cipher
	 */
	public HeaderProtector(AeadId aeadId, SecretKey key) throws GeneralSecurityException   {
		protection = AeadSpec.getHeaderProtection(aeadId);
		this.key = key;
		cipher = protection.createCipher();
	}

	/**
	 * Constructs a QUIC header protector for the given AEAD algorithm and secret
	 * key.
	 * 
	 * @param aead the AEAD algorithm
	 * @param key    the secret key
	 * @throws GeneralSecurityException if a general security error occurred during
	 *                                  creation of the associated cipher
	 */
	public HeaderProtector(IAead aead, SecretKey key) throws GeneralSecurityException   {
		this(aead.getId(), key);
	}
	
	/**
	 * Returns the associated header protection algorithm.
	 * 
	 * @return the header protection algorithm
	 */
	public IHeaderProtection getProtection() {
		return protection;
	}

	/**
	 * Derives a 5-byte mask from the given sampled data to be applied to the
	 * protected header fields. The returned array may have more than 5 bytes and if
	 * so only the first 5 bytes should be used.
	 * 
	 * @param sample an array with the sampled data
	 * @param offset the offset to the first byte of the 16-byte
	 *               sampled data in the given array
	 * @return the derived mask
	 * @throws KeyException if an key error occurred
	 * @throws GeneralSecurityException f a general security error occurred
	 */
	public byte[] deriveMask(byte[] sample, int offset) throws KeyException, GeneralSecurityException {
		return protection.deriveMask(cipher, key, sample, offset);
	}
	
	/**
	 * Erases associated secret key. 
	 */
	public void erase() {
		try {
			key.destroy();
		} catch (DestroyFailedException e) {
			//Ignore
		}
	}

}
