/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022 SNF4J contributors
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
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;

public class RSASSAPSSSignature implements ISignature {

	public final static RSASSAPSSSignature RSA_PSS_RSAE_SHA256 = new RSASSAPSSSignature("RSA", "SHA-256", 32);

	public final static RSASSAPSSSignature RSA_PSS_RSAE_SHA384 = new RSASSAPSSSignature("RSA", "SHA-384", 48);
	
	public final static RSASSAPSSSignature RSA_PSS_RSAE_SHA512 = new RSASSAPSSSignature("RSA", "SHA-512", 64);

	public final static RSASSAPSSSignature RSA_PSS_PSS_SHA256 = new RSASSAPSSSignature("RSASSA-PSS", "SHA-256", 32);

	public final static RSASSAPSSSignature RSA_PSS_PSS_SHA384 = new RSASSAPSSSignature("RSASSA-PSS", "SHA-384", 48);
	
	public final static RSASSAPSSSignature RSA_PSS_PSS_SHA512 = new RSASSAPSSSignature("RSASSA-PSS", "SHA-512", 64);
	
	private final String hashName;
	
	private final String keyAlgorithm;
	
	private final int saltLength;
	
	private final static boolean IMPLEMENTED;
	
	static boolean implemented(String algorithm) {
		try {
			Signature.getInstance(algorithm);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	static {
		IMPLEMENTED = implemented("RSASSA-PSS");
	}
	
	public RSASSAPSSSignature(String keyAlgorithm, String hashName, int saltLength) {
		this.keyAlgorithm = keyAlgorithm;
		this.hashName = hashName;
		this.saltLength = saltLength;
	}
	
	AlgorithmParameterSpec createParameter() {
		return new PSSParameterSpec(hashName, "MGF1", new MGF1ParameterSpec(hashName), saltLength, 1);
	}

	public Signature createSignature(String algorithm) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		return Signature.getInstance(algorithm);
	}
	
	@Override
	public Signature createSignature() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		Signature signature = createSignature("RSASSA-PSS");

		signature.setParameter(createParameter());
		return signature;
	}

	public String keyAlgorithm() {
		return keyAlgorithm;
	}
	
	@Override
	public boolean isImplemented() {
		return IMPLEMENTED;
	}

}
