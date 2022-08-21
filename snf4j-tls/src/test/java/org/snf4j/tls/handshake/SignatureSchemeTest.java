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

import org.junit.Test;
import org.snf4j.tls.IntConstantTester;

public class SignatureSchemeTest {

	final static String ENTRIES = 
			"|" + "rsa_pkcs1_sha256(0x0401),"+
			"|" + "rsa_pkcs1_sha384(0x0501),"+
			"|" + "rsa_pkcs1_sha512(0x0601),"+
			"|" + "ecdsa_secp256r1_sha256(0x0403),"+
			"|" + "ecdsa_secp384r1_sha384(0x0503),"+
			"|" + "ecdsa_secp521r1_sha512(0x0603),"+
			"|" + "rsa_pss_rsae_sha256(0x0804),"+
			"|" + "rsa_pss_rsae_sha384(0x0805),"+
			"|" + "rsa_pss_rsae_sha512(0x0806),"+
			"|" + "ed25519(0x0807),"+
			"|" + "ed448(0x0808),"+
			"|" + "rsa_pss_pss_sha256(0x0809),"+
			"|" + "rsa_pss_pss_sha384(0x080a),"+
			"|" + "rsa_pss_pss_sha512(0x080b),"+
			"|" + "rsa_pkcs1_sha1(0x0201),"+
			"|" + "ecdsa_sha1(0x0203),";

	@Test
	public void testValues() throws Exception {
		new IntConstantTester<SignatureScheme>(ENTRIES, SignatureScheme.class, SignatureScheme[].class).assertValues("0x%04x");
	}

	@Test
	public void testOf() throws Exception {
		new IntConstantTester<SignatureScheme>(ENTRIES, SignatureScheme.class, SignatureScheme[].class).assertOf(0, 0x900);
	}
	
}
