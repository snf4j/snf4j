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
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAKey;

import javax.crypto.Cipher;

public class RSAPKCS1Signature implements ISignature {

	private final static String RSA = "RSA";
	
	public final static RSAPKCS1Signature RSA_PKCS1_SHA1 = new RSAPKCS1Signature("SHA1withRSA", 511);

	public final static RSAPKCS1Signature RSA_PKCS1_SHA256 = new RSAPKCS1Signature("SHA256withRSA", 511);
	
	public final static RSAPKCS1Signature RSA_PKCS1_SHA384 = new RSAPKCS1Signature("SHA384withRSA", 768);

	public final static RSAPKCS1Signature RSA_PKCS1_SHA512 = new RSAPKCS1Signature("SHA512withRSA", 768);
	
	private final String algorithm;
	
	private final int minKeySize;
	
	public RSAPKCS1Signature(String algorithm, int minKeySize) {
		this.algorithm = algorithm;
		this.minKeySize = minKeySize;
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}

	@Override
	public Signature createSignature() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		return Signature.getInstance(algorithm);
	}

	@Override
	public String algorithm() {
		return algorithm;
	}
	
	@Override
	public String keyAlgorithm() {
		return RSA;
	}

	@Override
	public boolean matches(X509Certificate cert) {
		return algorithm.equals(cert.getSigAlgName()) && keySizeMatches(RSA, cert.getPublicKey(), minKeySize); 
	}
	
	@Override
	public boolean matchesByKey(X509Certificate cert) {
		PublicKey key = cert.getPublicKey();
		
		return RSA.equals(key.getAlgorithm()) && keySizeMatches(RSA, key, minKeySize);
	}

	static boolean keySizeMatches(String algorithm, Key key, int minKeySize) {
		if (key instanceof RSAKey) {
			if (((RSAKey)key).getModulus().bitLength() < minKeySize) {
				try {
					Cipher rsa = Cipher.getInstance(algorithm);

					rsa.init(Cipher.ENCRYPT_MODE, key);
					return rsa.getOutputSize(0) * Byte.SIZE >= minKeySize;
				}
				catch (Exception e) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	@Override
	public int minKeySize() {
		return minKeySize;
	}
	
}
