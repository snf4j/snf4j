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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAKey;

import org.junit.Assume;
import org.junit.Test;

public class RSAPKCS1SignatureTest extends SignatureTest {

	@Test
	public void testMinKeySize() {
		assertEquals(511, RSAPKCS1Signature.RSA_PKCS1_SHA1.minKeySize());
		assertEquals(511, RSAPKCS1Signature.RSA_PKCS1_SHA256.minKeySize());
		assertEquals(768, RSAPKCS1Signature.RSA_PKCS1_SHA384.minKeySize());
		assertEquals(768, RSAPKCS1Signature.RSA_PKCS1_SHA512.minKeySize());
	}
	
	@Test
	public void testRsaPkcs1Sha256() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(512);
		KeyPair pair = keyGen.generateKeyPair();
		assertTrue(RSAPKCS1Signature.RSA_PKCS1_SHA256.isImplemented());
		assertVerify(RSAPKCS1Signature.RSA_PKCS1_SHA256, pair.getPrivate(), pair.getPublic());
		assertEquals("SHA256withRSA", RSAPKCS1Signature.RSA_PKCS1_SHA256.algorithm());
		
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
		assertEquals("SHA384withRSA", RSAPKCS1Signature.RSA_PKCS1_SHA384.algorithm());

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
		assertEquals("SHA512withRSA", RSAPKCS1Signature.RSA_PKCS1_SHA512.algorithm());

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
		assertEquals("SHA1withRSA", RSAPKCS1Signature.RSA_PKCS1_SHA1.algorithm());

		X509Certificate cert = cert("rsasha1");
		assertNotNull(cert);
		assertEquals("SHA1withRSA", cert.getSigAlgName());
		cert.verify(cert.getPublicKey());
		assertVerify(RSAPKCS1Signature.RSA_PKCS1_SHA1, rsaKey("rsa"), cert.getPublicKey());

		assertEquals("RSA", RSAPKCS1Signature.RSA_PKCS1_SHA1.keyAlgorithm());
		assertX509Encoding("RSA", pair.getPublic());
	}
	
	@Test
	public void testKeySizeMatches() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(512);
		KeyPair pair = keyGen.generateKeyPair();
		int keySize = ((RSAKey)pair.getPublic()).getModulus().bitLength();
		
		assertTrue(RSAPKCS1Signature.keySizeMatches("RSA", pair.getPublic(), keySize-1));
		assertTrue(RSAPKCS1Signature.keySizeMatches("RSA", pair.getPublic(), keySize));
		assertFalse(RSAPKCS1Signature.keySizeMatches("RSA", pair.getPublic(), keySize+1));

		keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(513);
		pair = keyGen.generateKeyPair();
		keySize = ((RSAKey)pair.getPublic()).getModulus().bitLength();
		assertFalse(RSAPKCS1Signature.keySizeMatches("XXX", pair.getPublic(), keySize+1));
		assertTrue(RSAPKCS1Signature.keySizeMatches("RSA", pair.getPublic(), keySize+1));
		assertFalse(RSAPKCS1Signature.keySizeMatches("RSA", new Key() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getAlgorithm() {
				return null;
			}

			@Override
			public String getFormat() {
				return null;
			}

			@Override
			public byte[] getEncoded() {
				return null;
			}}, keySize+1));	
	}
	
	@Test
	public void testMatches() throws Exception {
		X509Certificate cert = cert("rsasha1");
		
		assertTrue(RSAPKCS1Signature.RSA_PKCS1_SHA1.matches(cert));
		assertTrue(RSAPKCS1Signature.RSA_PKCS1_SHA1.matchesByKey(cert));
		assertFalse(RSAPKCS1Signature.RSA_PKCS1_SHA512.matches(cert));
		assertTrue(RSAPKCS1Signature.RSA_PKCS1_SHA512.matchesByKey(cert));
		assertTrue(RSAPKCS1Signature.RSA_PKCS1_SHA256.matchesByKey(cert));
		assertTrue(RSAPKCS1Signature.RSA_PKCS1_SHA384.matchesByKey(cert));
		assertFalse(new RSAPKCS1Signature("SHA1withRSA", 1025).matches(cert));
		assertFalse(new RSAPKCS1Signature("SHA1withRSA", 1025).matchesByKey(cert));
		
		cert = cert("secp256r1");
		assertFalse(RSAPKCS1Signature.RSA_PKCS1_SHA1.matches(cert));
		assertFalse(RSAPKCS1Signature.RSA_PKCS1_SHA1.matchesByKey(cert));
	}

	@Test
	public void testMatchesWithPSS() throws Exception {
		Assume.assumeTrue(JAVA11);
		X509Certificate cert = cert("rsapsssha256");

		assertTrue(RSAPKCS1Signature.RSA_PKCS1_SHA1.matchesByKey(cert));
		assertTrue(RSAPKCS1Signature.RSA_PKCS1_SHA512.matchesByKey(cert));
		assertTrue(RSAPKCS1Signature.RSA_PKCS1_SHA256.matchesByKey(cert));
		assertTrue(RSAPKCS1Signature.RSA_PKCS1_SHA384.matchesByKey(cert));
		
		cert = cert("rsapsspsssha256");
		assertFalse(RSAPKCS1Signature.RSA_PKCS1_SHA1.matchesByKey(cert));
		assertFalse(RSAPKCS1Signature.RSA_PKCS1_SHA512.matchesByKey(cert));
		assertFalse(RSAPKCS1Signature.RSA_PKCS1_SHA256.matchesByKey(cert));
		assertFalse(RSAPKCS1Signature.RSA_PKCS1_SHA384.matchesByKey(cert));
	}
	
}
