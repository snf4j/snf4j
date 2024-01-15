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
package org.snf4j.tls.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.snf4j.tls.crypto.ECDSASignature;
import org.snf4j.tls.crypto.EdDSASignature;
import org.snf4j.tls.crypto.RSAPKCS1Signature;
import org.snf4j.tls.crypto.RSASSAPSSSignature;
import org.snf4j.tls.crypto.SignatureTest;

public class SignatureSchemeSpecTest extends SignatureTest {

	void assertScheme(String name, boolean implemented, Class<?> clazz) throws Exception {
		SignatureSchemeSpec spec = (SignatureSchemeSpec) SignatureSchemeSpec.class.getDeclaredField(name).get(null);
		
		assertEquals(implemented, spec.isImplemented());
		assertSame(clazz.getDeclaredField(name).get(null), spec.getSignature());
		assertNotNull(spec.getSignature());
	}
	
	@Test
	public void testAll() throws Exception {
		assertScheme("RSA_PSS_RSAE_SHA256", JAVA11 || JAVA8_GEQ_U392, RSASSAPSSSignature.class);
		assertScheme("RSA_PSS_RSAE_SHA384", JAVA11 || JAVA8_GEQ_U392, RSASSAPSSSignature.class);
		assertScheme("RSA_PSS_RSAE_SHA512", JAVA11 || JAVA8_GEQ_U392, RSASSAPSSSignature.class);
		assertScheme("RSA_PSS_PSS_SHA256", JAVA11 || JAVA8_GEQ_U392, RSASSAPSSSignature.class);
		assertScheme("RSA_PSS_PSS_SHA384", JAVA11 || JAVA8_GEQ_U392, RSASSAPSSSignature.class);
		assertScheme("RSA_PSS_PSS_SHA512", JAVA11 || JAVA8_GEQ_U392, RSASSAPSSSignature.class);

		assertScheme("ECDSA_SHA1", true, ECDSASignature.class);
		assertScheme("ECDSA_SECP256R1_SHA256", true, ECDSASignature.class);
		assertScheme("ECDSA_SECP384R1_SHA384", true, ECDSASignature.class);
		assertScheme("ECDSA_SECP521R1_SHA512", true, ECDSASignature.class);

		assertScheme("RSA_PKCS1_SHA1", true, RSAPKCS1Signature.class);
		assertScheme("RSA_PKCS1_SHA256", true, RSAPKCS1Signature.class);
		assertScheme("RSA_PKCS1_SHA384", true, RSAPKCS1Signature.class);
		assertScheme("RSA_PKCS1_SHA512", true, RSAPKCS1Signature.class);

		assertScheme("ED25519", JAVA15, EdDSASignature.class);
		assertScheme("ED448", JAVA15, EdDSASignature.class);
	}
}
