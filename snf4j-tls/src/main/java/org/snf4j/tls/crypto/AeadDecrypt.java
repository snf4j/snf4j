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

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

import org.snf4j.tls.Args;

public class AeadDecrypt implements IAeadDecrypt {

	private final SecretKey key;
	
	private final Cipher cipher;
	
	private final IAead aead;
	
	public AeadDecrypt(SecretKey key, IAead aead) throws NoSuchAlgorithmException, NoSuchPaddingException {
		Args.checkNull(key, "key");
		Args.checkNull(aead, "aead");
		this.key = key;
		this.aead = aead;
		cipher = aead.createCipher();
	}
	
	@Override
	public IAead getAead() {
		return aead;
	}
	
	@Override
	public byte[] decrypt(byte[] nonce, byte[] additionalData, byte[] ciphertext) throws GeneralSecurityException {
		aead.initDecrypt(cipher, key, nonce);
		cipher.updateAAD(additionalData);
		return cipher.doFinal(ciphertext);
	}

	@Override
	public void decrypt(byte[] nonce, byte[] additionalData, ByteBuffer ciphertext, ByteBuffer plaintext) throws GeneralSecurityException {
		aead.initDecrypt(cipher, key, nonce);
		cipher.updateAAD(additionalData);
		cipher.doFinal(ciphertext, plaintext);
	}

	@Override
	public void erase() {
		try {
			key.destroy();
		} catch (DestroyFailedException e) {
			//Ignore
		}
	}
	
}
