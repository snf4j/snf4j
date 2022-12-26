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

public class RSAPKCS1SignatureTest extends SignatureTest {

	@Test
	public void testRsaPkcs1Sha256() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(512);
		KeyPair pair = keyGen.generateKeyPair();
		assertTrue(RSAPKCS1Signature.RSA_PKCS1_SHA256.isImplemented());
		assertVerify(RSAPKCS1Signature.RSA_PKCS1_SHA256, pair.getPrivate(), pair.getPublic());

		X509Certificate cert = cert("rsasha256");
		assertNotNull(cert);
		assertEquals("SHA256withRSA", cert.getSigAlgName());
		cert.verify(cert.getPublicKey());
		assertVerify(RSAPKCS1Signature.RSA_PKCS1_SHA256, rsaKey("rsa"), cert.getPublicKey());
		
		assertEquals("RSA", RSAPKCS1Signature.RSA_PKCS1_SHA256.keyAlgorithm());
		assertX509Encoding("RSA", pair.getPublic());
	}

	@Test
	public void testRsaPkcs1Sha384() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(624);
		KeyPair pair = keyGen.generateKeyPair();
		assertTrue(RSAPKCS1Signature.RSA_PKCS1_SHA384.isImplemented());
		assertVerify(RSAPKCS1Signature.RSA_PKCS1_SHA384, pair.getPrivate(), pair.getPublic());

		X509Certificate cert = cert("rsasha384");
		assertNotNull(cert);
		assertEquals("SHA384withRSA", cert.getSigAlgName());
		cert.verify(cert.getPublicKey());
		assertVerify(RSAPKCS1Signature.RSA_PKCS1_SHA384, rsaKey("rsa"), cert.getPublicKey());

		assertEquals("RSA", RSAPKCS1Signature.RSA_PKCS1_SHA384.keyAlgorithm());
		assertX509Encoding("RSA", pair.getPublic());
	}

	@Test
	public void testRsaPkcs1Sha512() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(752);
		KeyPair pair = keyGen.generateKeyPair();
		assertTrue(RSAPKCS1Signature.RSA_PKCS1_SHA512.isImplemented());
		assertVerify(RSAPKCS1Signature.RSA_PKCS1_SHA512, pair.getPrivate(), pair.getPublic());

		X509Certificate cert = cert("rsasha512");
		assertNotNull(cert);
		assertEquals("SHA512withRSA", cert.getSigAlgName());
		cert.verify(cert.getPublicKey());
		assertVerify(RSAPKCS1Signature.RSA_PKCS1_SHA512, rsaKey("rsa"), cert.getPublicKey());

		assertEquals("RSA", RSAPKCS1Signature.RSA_PKCS1_SHA512.keyAlgorithm());
		assertX509Encoding("RSA", pair.getPublic());
	}
	
	@Test
	public void testRsaPkcs1Sha1() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(512);
		KeyPair pair = keyGen.generateKeyPair();
		assertTrue(RSAPKCS1Signature.RSA_PKCS1_SHA1.isImplemented());
		assertVerify(RSAPKCS1Signature.RSA_PKCS1_SHA1, pair.getPrivate(), pair.getPublic());

		X509Certificate cert = cert("rsasha1");
		assertNotNull(cert);
		assertEquals("SHA1withRSA", cert.getSigAlgName());
		cert.verify(cert.getPublicKey());
		assertVerify(RSAPKCS1Signature.RSA_PKCS1_SHA1, rsaKey("rsa"), cert.getPublicKey());

		assertEquals("RSA", RSAPKCS1Signature.RSA_PKCS1_SHA1.keyAlgorithm());
		assertX509Encoding("RSA", pair.getPublic());
	}
	
}
