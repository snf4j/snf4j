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
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESAead implements IAead {

	public final static AESAead AEAD_AES_128_GCM = new AESAead(16, 16, 137438953472L, 12);
	
	public final static AESAead AEAD_AES_256_GCM = new AESAead(16, 32, 137438953472L, 12);
	
	private final static String TRANSFORMATION = "AES/GCM/NoPadding";

	private final static String ALGORITHM = "AES";
	
	private final int tagLength;
	
	private final int tagBits;
	
	private final int keyLength;
	
	private final long keyLimit;
	
	private final int ivLength;
	
	public AESAead(int tagLength, int keyLength, long keyLimit, int ivLength) {
		this.tagLength = tagLength;
		this.keyLength = keyLength;
		this.keyLimit = keyLimit;
		this.ivLength = ivLength;
		tagBits = tagLength*8;
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
	public long getKeyLimit() {
		return keyLimit;
	}
	
	@Override
	public int getIvLength() {
		return ivLength;
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}

	@Override
	public Cipher createCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
		return Cipher.getInstance(TRANSFORMATION);
	}

	@Override
	public SecretKey createKey(byte[] key) {
		return new SecretKeySpec(key, ALGORITHM);
	}

	@Override
	public void initDecrypt(Cipher cipher, SecretKey key, byte[] nonce) throws InvalidKeyException, InvalidAlgorithmParameterException {
		cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(tagBits, nonce));
	}

	@Override
	public void initEncrypt(Cipher cipher, SecretKey key, byte[] nonce) throws InvalidKeyException, InvalidAlgorithmParameterException {
		cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(tagBits, nonce));
	}

}
