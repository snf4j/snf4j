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
package org.snf4j.tls.engine;

import java.util.Arrays;

import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.cipher.ICipherSuiteSpec;
import org.snf4j.tls.extension.INamedGroupSpec;
import org.snf4j.tls.extension.ISignatureSchemeSpec;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.SignatureScheme;

public class EngineDefaults {

	private EngineDefaults() {}
	
	private final static CipherSuite[] DEFAULT_CIPHER_SUITES = new CipherSuite[] {
			CipherSuite.TLS_AES_256_GCM_SHA384,
			CipherSuite.TLS_CHACHA20_POLY1305_SHA256,
			CipherSuite.TLS_AES_128_GCM_SHA256
			};
	
	private final static CipherSuite[] IMPLEMENTED_CIPHER_SUITES = implemented(DEFAULT_CIPHER_SUITES);
	
	private final static NamedGroup[] DEFAULT_NAMED_GROUPS = new NamedGroup[] { 
			NamedGroup.X25519,
			NamedGroup.SECP256R1,
			NamedGroup.X448,
			NamedGroup.SECP521R1,
			NamedGroup.SECP384R1,
			NamedGroup.FFDHE2048,
			NamedGroup.FFDHE3072,
			NamedGroup.FFDHE4096,
			NamedGroup.FFDHE6144,
			NamedGroup.FFDHE8192
			};

	private final static NamedGroup[] IMPLEMENTED_NAMED_GROUPS = implemented(DEFAULT_NAMED_GROUPS);
	
	private final static SignatureScheme[] DEFAULT_SIGNATURE_SCHEMES = new SignatureScheme[] {
			SignatureScheme.ECDSA_SECP256R1_SHA256,
			SignatureScheme.ECDSA_SECP384R1_SHA384,
			SignatureScheme.ECDSA_SECP521R1_SHA512,
			SignatureScheme.ED25519,
			SignatureScheme.ED448,
			SignatureScheme.RSA_PSS_PSS_SHA256,
			SignatureScheme.RSA_PSS_PSS_SHA384,
			SignatureScheme.RSA_PSS_PSS_SHA512,
			SignatureScheme.RSA_PSS_RSAE_SHA256,
			SignatureScheme.RSA_PSS_RSAE_SHA384,
			SignatureScheme.RSA_PSS_RSAE_SHA512,
			SignatureScheme.RSA_PKCS1_SHA256,
			SignatureScheme.RSA_PKCS1_SHA384,
			SignatureScheme.RSA_PKCS1_SHA512,
			SignatureScheme.ECDSA_SHA1,
			SignatureScheme.RSA_PKCS1_SHA1
			};

	private final static SignatureScheme[] IMPLEMENTED_SIGNATURE_SCHEMES = implemented(DEFAULT_SIGNATURE_SCHEMES);

	public final static int LEGACY_VERSION = 0x0303;
	
	private static CipherSuite[] implemented(CipherSuite[] suites) {
		CipherSuite[] implemented = new CipherSuite[suites.length];
		int i=0;
		
		for (CipherSuite suite: suites) {
			ICipherSuiteSpec spec = suite.spec();
			
			if (spec != null && spec.isImplemented()) {
				implemented[i++] = suite;
			}
		}
		if (i < implemented.length) {
			return Arrays.copyOf(implemented, i);
		}
		return implemented;
	}

	private static NamedGroup[] implemented(NamedGroup[] groups) {
		NamedGroup[] implemented = new NamedGroup[groups.length];
		int i=0;
		
		for (NamedGroup group: groups) {
			 INamedGroupSpec spec = group.spec();
			
			if (spec != null && spec.isImplemented()) {
				implemented[i++] = group;
			}
		}
		if (i < implemented.length) {
			return Arrays.copyOf(implemented, i);
		}
		return implemented;
	}

	private static SignatureScheme[] implemented(SignatureScheme[] schemes) {
		SignatureScheme[] implemented = new SignatureScheme[schemes.length];
		int i=0;
		
		for (SignatureScheme scheme: schemes) {
			 ISignatureSchemeSpec spec = scheme.spec();
			
			if (spec != null && spec.isImplemented()) {
				implemented[i++] = scheme;
			}
		}
		if (i < implemented.length) {
			return Arrays.copyOf(implemented, i);
		}
		return implemented;
	}

	static CipherSuite[] getDefaultCipherSuites() {
		return IMPLEMENTED_CIPHER_SUITES.clone();
	}

	static NamedGroup[] getDefaultNamedGroups() {
		return IMPLEMENTED_NAMED_GROUPS.clone();
	}
	
	static SignatureScheme[] getDefaulSignatureSchemes() {
		return IMPLEMENTED_SIGNATURE_SCHEMES.clone();
	}

}
