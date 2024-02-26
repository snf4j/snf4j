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

import java.security.GeneralSecurityException;
import java.security.KeyException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;

/**
 * An {@code interface} representing the header protection algorithm.
 *  
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IHeaderProtection {
	
	/**
	 * The length of the key used by the algorithm.
	 *  
	 * @return the key length in bytes.
	 */
	int getKeyLength();
	
	/**
	 * Tells if this algorithm is implemented.
	 * 
	 * @return {@code true} if the algorithm is implemented
	 */
	boolean isImplemented();

	/**
	 * Creates a cipher for this algorithm.
	 * 
	 * @return a cipher for this algorithm
	 * @throws GeneralSecurityException if a general security error occurred
	 */
	Cipher createCipher() throws GeneralSecurityException;
	
	/**
	 * Creates a key for this algorithm from the given key material.
	 * 
	 * @param key the key material
	 * @return a key for this algorithm
	 */
	SecretKey createKey(byte[] key);
	
	/**
	 * Derives a 5-byte mask from the given sampled data to be applied to the
	 * protected header fields. The returned array may have more than 5 bytes and if
	 * so only the first 5 bytes should be used.
	 * 
	 * @param cipher the cipher used for the derivation
	 * @param key    the key used for the derivation
	 * @param sample an array with the sampled data
	 * @param offset the offset to the first byte of the 16-byte
	 *               sampled data in the given array
	 * @return the derived mask
	 * @throws KeyException if an key error occurred
	 * @throws GeneralSecurityException f a general security error occurred
	 */
	byte[] deriveMask(Cipher cipher, SecretKey key, byte[] sample, int offset) throws KeyException, GeneralSecurityException;
}
