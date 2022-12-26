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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import org.junit.Test;

public class ECDSASignatureTest extends SignatureTest {

	@Test
	public void testEcdsaSecp256r1Sha256() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
		keyGen.initialize(256);
		KeyPair pair = keyGen.generateKeyPair();
		assertTrue(ECDSASignature.ECDSA_SECP256R1_SHA256.isImplemented());
		assertVerify(ECDSASignature.ECDSA_SECP256R1_SHA256, pair.getPrivate(), pair.getPublic());

		X509Certificate cert = cert("secp256r1");
		assertNotNull(cert);
		assertEquals("SHA256withECDSA", cert.getSigAlgName());
		cert.verify(cert.getPublicKey());
		assertVerify(ECDSASignature.ECDSA_SECP256R1_SHA256, ecKey("secp256r1"), cert.getPublicKey());
		
		assertEquals("EC", ECDSASignature.ECDSA_SECP256R1_SHA256.keyAlgorithm());
		assertX509Encoding("EC", pair.getPublic());
	}

	@Test
	public void testEcdsaSecp384r1Sha384() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
		keyGen.initialize(384);
		KeyPair pair = keyGen.generateKeyPair();
		assertTrue(ECDSASignature.ECDSA_SECP384R1_SHA384.isImplemented());
		assertVerify(ECDSASignature.ECDSA_SECP384R1_SHA384, pair.getPrivate(), pair.getPublic());

		X509Certificate cert = cert("secp384r1");
		assertNotNull(cert);
		assertEquals("SHA384withECDSA", cert.getSigAlgName());
		cert.verify(cert.getPublicKey());
		assertVerify(ECDSASignature.ECDSA_SECP384R1_SHA384, ecKey("secp384r1"), cert.getPublicKey());
		
		assertEquals("EC", ECDSASignature.ECDSA_SECP384R1_SHA384.keyAlgorithm());
		assertX509Encoding("EC", pair.getPublic());
	}

	@Test
	public void testEcdsaSecp521r1Sha512() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
		keyGen.initialize(521);
		KeyPair pair = keyGen.generateKeyPair();
		assertTrue(ECDSASignature.ECDSA_SECP521R1_SHA512.isImplemented());
		assertVerify(ECDSASignature.ECDSA_SECP521R1_SHA512, pair.getPrivate(), pair.getPublic());

		X509Certificate cert = cert("secp521r1");
		assertNotNull(cert);
		assertEquals("SHA512withECDSA", cert.getSigAlgName());
		cert.verify(cert.getPublicKey());
		assertVerify(ECDSASignature.ECDSA_SECP521R1_SHA512, ecKey("secp521r1"), cert.getPublicKey());
		
		assertEquals("EC", ECDSASignature.ECDSA_SECP521R1_SHA512.keyAlgorithm());
		assertX509Encoding("EC", pair.getPublic());
	}

	@Test
	public void testEcdsaSha1() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
		keyGen.initialize(256);
		KeyPair pair = keyGen.generateKeyPair();
		assertTrue(ECDSASignature.ECDSA_SHA1.isImplemented());
		assertVerify(ECDSASignature.ECDSA_SHA1, pair.getPrivate(), pair.getPublic());

		X509Certificate cert = cert("ecdsasha1");
		assertNotNull(cert);
		assertEquals("SHA1withECDSA", cert.getSigAlgName());
		cert.verify(cert.getPublicKey());
		assertVerify(ECDSASignature.ECDSA_SHA1, ecKey("ecdsasha1"), cert.getPublicKey());
		
		assertEquals("EC", ECDSASignature.ECDSA_SHA1.keyAlgorithm());
		assertX509Encoding("EC", pair.getPublic());
	}
	
}
