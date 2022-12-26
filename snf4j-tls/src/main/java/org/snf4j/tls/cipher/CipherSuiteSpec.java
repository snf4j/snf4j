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

import org.snf4j.tls.Args;
import org.snf4j.tls.crypto.AESAead;
import org.snf4j.tls.crypto.ChaCha20Aead;
import org.snf4j.tls.crypto.IAead;

public class CipherSuiteSpec implements ICipherSuiteSpec {

	public final static CipherSuiteSpec TLS_AES_128_GCM_SHA256 = new CipherSuiteSpec(AESAead.AEAD_AES_128_GCM, HashSpec.SHA256);
	
	public final static CipherSuiteSpec TLS_AES_256_GCM_SHA384 = new CipherSuiteSpec(AESAead.AEAD_AES_256_GCM, HashSpec.SHA384);
	
	public final static CipherSuiteSpec TLS_CHACHA20_POLY1305_SHA256 = new CipherSuiteSpec(ChaCha20Aead.AEAD_CHACHA20_POLY1305, HashSpec.SHA256);
	
	private final IAead aead;
	
	private final IHashSpec hashSpec;

	public CipherSuiteSpec(IAead aead, IHashSpec hashSpec) {
		super();
		Args.checkNull(aead, "aead");
		Args.checkNull(hashSpec, "hashSpec");
		this.aead = aead;
		this.hashSpec = hashSpec;
	}
	
	@Override
	public boolean isImplemented() {
		return aead.isImplemented();
	}
	
	@Override
	public IAead getAead() {
		return aead;
	}
	
	@Override
	public IHashSpec getHashSpec() {
		return hashSpec;
	}

}
