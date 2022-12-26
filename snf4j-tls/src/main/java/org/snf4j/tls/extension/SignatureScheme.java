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
package org.snf4j.tls.extension;

import org.snf4j.tls.IntConstant;

public class SignatureScheme extends IntConstant {

	public static final SignatureScheme RSA_PKCS1_SHA256 = new SignatureScheme("rsa_pkcs1_sha256",0x0401, SignatureSchemeSpec.RSA_PKCS1_SHA256);
	
	public static final SignatureScheme RSA_PKCS1_SHA384 = new SignatureScheme("rsa_pkcs1_sha384",0x0501, SignatureSchemeSpec.RSA_PKCS1_SHA384);
	
	public static final SignatureScheme RSA_PKCS1_SHA512 = new SignatureScheme("rsa_pkcs1_sha512",0x0601, SignatureSchemeSpec.RSA_PKCS1_SHA512);
	
	public static final SignatureScheme ECDSA_SECP256R1_SHA256 = new SignatureScheme("ecdsa_secp256r1_sha256",0x0403, SignatureSchemeSpec.ECDSA_SECP256R1_SHA256);
	
	public static final SignatureScheme ECDSA_SECP384R1_SHA384 = new SignatureScheme("ecdsa_secp384r1_sha384",0x0503, SignatureSchemeSpec.ECDSA_SECP384R1_SHA384);
	
	public static final SignatureScheme ECDSA_SECP521R1_SHA512 = new SignatureScheme("ecdsa_secp521r1_sha512",0x0603, SignatureSchemeSpec.ECDSA_SECP521R1_SHA512);
	
	public static final SignatureScheme RSA_PSS_RSAE_SHA256 = new SignatureScheme("rsa_pss_rsae_sha256",0x0804, SignatureSchemeSpec.RSA_PSS_RSAE_SHA256);
	
	public static final SignatureScheme RSA_PSS_RSAE_SHA384 = new SignatureScheme("rsa_pss_rsae_sha384",0x0805, SignatureSchemeSpec.RSA_PSS_RSAE_SHA384);
	
	public static final SignatureScheme RSA_PSS_RSAE_SHA512 = new SignatureScheme("rsa_pss_rsae_sha512",0x0806, SignatureSchemeSpec.RSA_PSS_RSAE_SHA512);
	
	public static final SignatureScheme ED25519 = new SignatureScheme("ed25519",0x0807, SignatureSchemeSpec.ED25519);
	
	public static final SignatureScheme ED448 = new SignatureScheme("ed448",0x0808, SignatureSchemeSpec.ED448);
	
	public static final SignatureScheme RSA_PSS_PSS_SHA256 = new SignatureScheme("rsa_pss_pss_sha256",0x0809, SignatureSchemeSpec.RSA_PSS_PSS_SHA256);
	
	public static final SignatureScheme RSA_PSS_PSS_SHA384 = new SignatureScheme("rsa_pss_pss_sha384",0x080a, SignatureSchemeSpec.RSA_PSS_PSS_SHA384);
	
	public static final SignatureScheme RSA_PSS_PSS_SHA512 = new SignatureScheme("rsa_pss_pss_sha512",0x080b, SignatureSchemeSpec.RSA_PSS_PSS_SHA512);
	
	public static final SignatureScheme RSA_PKCS1_SHA1 = new SignatureScheme("rsa_pkcs1_sha1",0x0201, SignatureSchemeSpec.RSA_PKCS1_SHA1);
	
	public static final SignatureScheme ECDSA_SHA1 = new SignatureScheme("ecdsa_sha1",0x0203,SignatureSchemeSpec.ECDSA_SHA1);
	
	private final static SignatureScheme[] KNOWN = new SignatureScheme[0x90];

	private static void known(SignatureScheme... knowns) {
		for (SignatureScheme known: knowns) {
			int value = known.value() | (known.value() >> 4);
			
			KNOWN[value&0xff] = known;
		}
	}
	
	static {
		known(
				RSA_PKCS1_SHA256,
				RSA_PKCS1_SHA384,
				RSA_PKCS1_SHA512,
				ECDSA_SECP256R1_SHA256,
				ECDSA_SECP384R1_SHA384,
				ECDSA_SECP521R1_SHA512,
				RSA_PSS_RSAE_SHA256,
				RSA_PSS_RSAE_SHA384,
				RSA_PSS_RSAE_SHA512,
				ED25519,
				ED448,
				RSA_PSS_PSS_SHA256,
				RSA_PSS_PSS_SHA384,
				RSA_PSS_PSS_SHA512,
				RSA_PKCS1_SHA1,
				ECDSA_SHA1
				);
	}	

	private final ISignatureSchemeSpec spec;
	
	protected SignatureScheme(String name, int value, ISignatureSchemeSpec spec) {
		super(name, value);
		this.spec = spec;
	}

	protected SignatureScheme(int value) {
		super(value);
		spec = null;
	}
	
	public ISignatureSchemeSpec spec() {
		return spec;
	}
	
	public static SignatureScheme of(int value) {
		int index = (value | (value >> 4)) & 0xff;
		
		if (index >= 0 && index < KNOWN.length) {
			SignatureScheme known = KNOWN[index];
			
			if (known != null && known.value() == value) {
				return known;
			}
		}
		return new SignatureScheme(value);
	}
}
