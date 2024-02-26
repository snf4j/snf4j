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
import javax.crypto.spec.SecretKeySpec;

import org.snf4j.tls.Args;

/**
 * A header protection with the AES algorithm in the Electronic Codebook (ECB)
 * mode.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class AESHeaderProtection implements IHeaderProtection {

	/** A header protection with 128-bit AES in ECB mode */
	public final static AESHeaderProtection HP_AES_128 = new AESHeaderProtection(16);

	/** A header protection with 256-bit AES in ECB mode */
	public final static AESHeaderProtection HP_AES_256 = new AESHeaderProtection(32);
	
	private final static String TRANSFORMATION = "AES/ECB/NoPadding";
	
	private final static String ALGORITHM = "AES";
	
	private final int keyLength;
	
	public AESHeaderProtection(int keyLength) {
		this.keyLength = keyLength;
	}
	
	@Override
	public int getKeyLength() {
		return keyLength;
	}

	@Override
	public boolean isImplemented() {
		return true;
	}

	@Override
	public Cipher createCipher() throws GeneralSecurityException {
		return Cipher.getInstance(TRANSFORMATION);
	}

	@Override
	public SecretKey createKey(byte[] key) {
		Args.checkFixed(key, keyLength, "key");
		return new SecretKeySpec(key, ALGORITHM);
	}

	@Override
	public byte[] deriveMask(Cipher cipher, SecretKey key, byte[] sample, int offset) throws GeneralSecurityException, KeyException {
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher.doFinal(sample, offset, 16);
	}

}
