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
import java.security.NoSuchAlgorithmException;
import java.security.Signature;

public class ECDSASignature implements ISignature {
	
	public final static ECDSASignature ECDSA_SHA1 = new ECDSASignature("SHA1withECDSA");

	public final static ECDSASignature ECDSA_SECP256R1_SHA256 = new ECDSASignature("SHA256withECDSA");

	public final static ECDSASignature ECDSA_SECP384R1_SHA384 = new ECDSASignature("SHA384withECDSA");
	
	public final static ECDSASignature ECDSA_SECP521R1_SHA512 = new ECDSASignature("SHA512withECDSA");
	
	private final String algorithm;
	
	public ECDSASignature(String algorithm) {
		this.algorithm = algorithm;
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
	public String keyAlgorithm() {
		return "EC";
	}

	@Override
	public int minKeySize() {
		return -1;
	}
}
