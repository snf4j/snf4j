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

public class EdDSASignature implements ISignature {

	public final static EdDSASignature ED25519 = new EdDSASignature("Ed25519");

	public final static EdDSASignature ED448 = new EdDSASignature("Ed448");
	
	private final String algorithm;

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
		IMPLEMENTED = implemented("Ed25519");
	}
	
	public EdDSASignature(String algorithm) {
		this.algorithm = algorithm;
	}
	
	@Override
	public boolean isImplemented() {
		return IMPLEMENTED;
	}

	@Override
	public String keyAlgorithm() {
		return algorithm;
	}

	@Override
	public int minKeySize() {
		return -1;
	}
	
	@Override
	public Signature createSignature() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		return Signature.getInstance(algorithm);
	}

}
