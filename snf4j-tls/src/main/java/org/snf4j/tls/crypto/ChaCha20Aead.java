/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022-2023 SNF4J contributors
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
package org.snf4j.tls.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ChaCha20Aead implements IAead {

	public final static ChaCha20Aead AEAD_CHACHA20_POLY1305 = new ChaCha20Aead(16,32,4611686018427387904L,12);
	
	private final static String TRANSFORMATION = "ChaCha20-Poly1305";
	
	private final static String ALGORITHM = "ChaCha20";
	
	private final static boolean IMPLEMENTED;
	
	private final int tagLength;
	
	private final int keyLength;
	
	private final long keyLimit;
	
	private final int ivLength;

	static boolean implemented(String transformation) {
		try {
			Cipher.getInstance(transformation);
			return true;
		} catch (Exception e) {
			return false;
		}		
	}

	static {
		IMPLEMENTED = implemented(TRANSFORMATION);
	}
	
	public ChaCha20Aead(int tagLength, int keyLength, long keyLimit, int ivLength) {
		this.tagLength = tagLength;
		this.keyLength = keyLength;
		this.keyLimit = keyLimit;
		this.ivLength = ivLength;
	}
	
	@Override
	public int getTagLength() {
		return tagLength;
	}

	@Override
	public int getKeyLength() {
		return keyLength;
	}

	@Override
	public int getIvLength() {
		return ivLength;
	}

	@Override
	public long getKeyLimit() {
		return keyLimit;
	}

	@Override
	public boolean isImplemented() {
		return IMPLEMENTED;
	}

	Cipher createCipher(String transformation) throws NoSuchAlgorithmException, NoSuchPaddingException {
		return Cipher.getInstance(transformation);
	}
	
	@Override
	public Cipher createCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
		return createCipher(TRANSFORMATION);
	}

	@Override
	public SecretKey createKey(byte[] key) {
		return new SecretKeySpec(key, ALGORITHM);
	}

	@Override
	public void initDecrypt(Cipher cipher, SecretKey key, byte[] nonce)	throws InvalidKeyException, InvalidAlgorithmParameterException {
		cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(nonce));
	}

	@Override
	public void initEncrypt(Cipher cipher, SecretKey key, byte[] nonce) throws InvalidKeyException, InvalidAlgorithmParameterException {
		cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(nonce));
	}

}
