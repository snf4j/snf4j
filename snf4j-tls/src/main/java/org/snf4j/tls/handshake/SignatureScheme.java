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
package org.snf4j.tls.handshake;

import org.snf4j.tls.IntConstant;

public class SignatureScheme extends IntConstant {

	public static final SignatureScheme RSA_PKCS1_SHA256 = new SignatureScheme(
			"rsa_pkcs1_sha256", 0x0401);
	
	public static final SignatureScheme RSA_PKCS1_SHA384 = new SignatureScheme(
			"rsa_pkcs1_sha384", 0x0501);
	
	public static final SignatureScheme RSA_PKCS1_SHA512 = new SignatureScheme(
			"rsa_pkcs1_sha512", 0x0601);
	
	public static final SignatureScheme ECDSA_SECP256R1_SHA256 = new SignatureScheme(
			"ecdsa_secp256r1_sha256", 0x0403);
	
	public static final SignatureScheme ECDSA_SECP384R1_SHA384 = new SignatureScheme(
			"ecdsa_secp384r1_sha384", 0x0503);
	
	public static final SignatureScheme ECDSA_SECP521R1_SHA512 = new SignatureScheme(
			"ecdsa_secp521r1_sha512", 0x0603);
	
	public static final SignatureScheme RSA_PSS_RSAE_SHA256 = new SignatureScheme(
			"rsa_pss_rsae_sha256", 0x0804);
	
	public static final SignatureScheme RSA_PSS_RSAE_SHA384 = new SignatureScheme(
			"rsa_pss_rsae_sha384", 0x0805);
	
	public static final SignatureScheme RSA_PSS_RSAE_SHA512 = new SignatureScheme(
			"rsa_pss_rsae_sha512", 0x0806);
	
	public static final SignatureScheme ED25519 = new SignatureScheme(
			"ed25519", 0x0807);
	
	public static final SignatureScheme ED448 = new SignatureScheme(
			"ed448", 0x0808);
	
	public static final SignatureScheme RSA_PSS_PSS_SHA256 = new SignatureScheme(
			"rsa_pss_pss_sha256", 0x0809);
	
	public static final SignatureScheme RSA_PSS_PSS_SHA384 = new SignatureScheme(
			"rsa_pss_pss_sha384", 0x080a);
	
	public static final SignatureScheme RSA_PSS_PSS_SHA512 = new SignatureScheme(
			"rsa_pss_pss_sha512", 0x080b);
	
	public static final SignatureScheme RSA_PKCS1_SHA1 = new SignatureScheme(
			"rsa_pkcs1_sha1", 0x0201);
	
	public static final SignatureScheme ECDSA_SHA1 = new SignatureScheme(
			"ecdsa_sha1", 0x0203);

	private static int index(int value) {
		return (value & 0xf) | ((value >> 4) & 0xf0);
	}
	
	private static final int MIN_INDEX = index(RSA_PKCS1_SHA1.value());
	
	private static final int MAX_INDEX = index(RSA_PSS_PSS_SHA512.value());
	
	private final static SignatureScheme[] KNOWN = new SignatureScheme[MAX_INDEX-MIN_INDEX+1];
	
	private static void known(SignatureScheme... knowns) {
		for (SignatureScheme known: knowns) {
			KNOWN[index(known.value()) - MIN_INDEX] = known;
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
	
	protected SignatureScheme(String name, int value) {
		super(name, value);
	}

	protected SignatureScheme(int value) {
		super(value);
	}

	public static SignatureScheme of(int value) {
		int index = index(value);
		
		if (index >= MIN_INDEX && index <= MAX_INDEX) {
			SignatureScheme known = KNOWN[index - MIN_INDEX];
			
			if (known != null && known.value() == value) {
				return known;
			}
		}
		return new SignatureScheme(value);
	}
	
}
