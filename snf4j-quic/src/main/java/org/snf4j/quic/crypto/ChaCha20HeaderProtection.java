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

import java.lang.reflect.Constructor;
import java.security.GeneralSecurityException;
import java.security.KeyException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.snf4j.tls.Args;
import org.snf4j.tls.crypto.ChaCha20Aead;

/**
 * A header protection with the ChaCha20 algorithm.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class ChaCha20HeaderProtection implements IHeaderProtection {

	/** A header protection with ChaCha20 */
	public final static ChaCha20HeaderProtection HP_CHACHA20 = new ChaCha20HeaderProtection(32);
	
	private final static String ALGORITHM = "ChaCha20";
	
	private final static boolean IMPLEMENTED = ChaCha20Aead.AEAD_CHACHA20_POLY1305.isImplemented();
	
	private final static byte[] ZEROS = new byte[5];
	
	private final static Constructor<?> CHACHA20_PARAM;
	
	private final int keyLength;
	
	static Constructor<?> contructor(String name) {
		try {
			return Class.forName(name).getConstructor(byte[].class, int.class);
		} catch (Exception e) {
			return null;
		}
	}
	
	static {
		CHACHA20_PARAM = contructor("javax.crypto.spec.ChaCha20ParameterSpec");
	}
	
	public ChaCha20HeaderProtection(int keyLength) {
		this.keyLength = keyLength;
	}
	
	@Override
	public int getKeyLength() {
		return keyLength;
	}

	@Override
	public boolean isImplemented() {
		return IMPLEMENTED;
	}

	Cipher cipher(String transformation) throws GeneralSecurityException {
		return Cipher.getInstance(transformation);
	}

	@Override
	public Cipher createCipher() throws GeneralSecurityException {
		return cipher(ALGORITHM);
	}

	@Override
	public SecretKey createKey(byte[] key) {
		Args.checkFixed(key, keyLength, "key");
		return new SecretKeySpec(key, ALGORITHM);
	}

	static int counter(byte[] sample, int offset) {
		return ((int)sample[offset+3] << 24) |
				((int)sample[offset+2] << 16) & 0xff0000 | 
				((int)sample[offset+1] <<  8) & 0xff00 | 
				((int)sample[offset]        & 0xff); 
	}
	
	AlgorithmParameterSpec parameterSpec(Constructor<?> contructor, byte[] nonce, int counter) throws InvalidParameterSpecException {
		try {
			return (AlgorithmParameterSpec) contructor.newInstance(nonce, counter);
		} catch (Exception e) {
			throw new InvalidParameterSpecException("ChaCha20 no implemented");
		}
	}
	
	@Override
	public byte[] deriveMask(Cipher cipher, SecretKey key, byte[] sample, int offset) throws GeneralSecurityException, KeyException {
		byte[] nonce = Arrays.copyOfRange(sample, offset+4, offset+16);
		AlgorithmParameterSpec param = parameterSpec(CHACHA20_PARAM, nonce,	counter(sample, offset));
		
		try {
			cipher.init(Cipher.ENCRYPT_MODE, key, param);
		}
		catch (Exception e) {
			byte[] nonce2 = new byte[nonce.length];
			
			nonce2[0] = (byte) (nonce[0] + 1);
			cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec(CHACHA20_PARAM,	nonce2, -1));
			cipher.init(Cipher.ENCRYPT_MODE, key, param);
		}
		return cipher.doFinal(ZEROS);
	}

}
