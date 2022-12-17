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
package org.snf4j.tls.cipher;

public class CipherSuiteSpec implements ICipherSuiteSpec {

	public final static CipherSuiteSpec TLS_AES_128_GCM_SHA256 = new CipherSuiteSpec(16, "AES", 16, 12, HashSpec.SHA256);
	
	public final static CipherSuiteSpec TLS_AES_256_GCM_SHA384 = new CipherSuiteSpec(16, "AES", 32, 12, HashSpec.SHA384);
	
	private final int authenticationTagLength;
	
	private final String KeyAlgorithm;
	
	private final int keyLength;
	
	private final int ivLength;
	
	private final IHashSpec hashSpec;

	public CipherSuiteSpec(int authenticationTagLength, String keyAlgorithm, int keyLength, int ivLength,
			IHashSpec hashSpec) {
		super();
		this.authenticationTagLength = authenticationTagLength;
		KeyAlgorithm = keyAlgorithm;
		this.keyLength = keyLength;
		this.ivLength = ivLength;
		this.hashSpec = hashSpec;
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}
	
	@Override
	public int getAuthenticationTagLength() {
		return authenticationTagLength;
	}

	@Override
	public String getKeyAlgorithm() {
		return KeyAlgorithm;
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
	public IHashSpec getHashSpec() {
		return hashSpec;
	}

}
