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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;

import org.junit.Assume;
import org.junit.Test;

public class EdDSASignatureTest extends SignatureTest {

	@Test
	public void testIsImplemented() {
		assertEquals(JAVA15, EdDSASignature.ED25519.isImplemented());
		assertEquals(JAVA15, EdDSASignature.ED448.isImplemented());
	}
	
	@Test
	public void testKeyAlgorithm() {
		assertEquals("Ed25519", EdDSASignature.ED25519.keyAlgorithm());
		assertEquals("Ed448", EdDSASignature.ED448.keyAlgorithm());
	}
	
	@Test
	public void testCreateSignature() throws Exception {
		assertEquals("SHA256withRSA", new EdDSASignature("SHA256withRSA").createSignature().getAlgorithm());
	}
	
	@Test
	public void testEd25519() throws Exception {
		Assume.assumeTrue(JAVA15);
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("Ed25519");
		KeyPair pair = keyGen.generateKeyPair();
		assertTrue(EdDSASignature.ED25519.isImplemented());
		assertVerify(EdDSASignature.ED25519, pair.getPrivate(), pair.getPublic());
		
		X509Certificate cert = cert("ed25519");
		assertNotNull(cert);
		assertEquals("Ed25519", cert.getSigAlgName());
		cert.verify(cert.getPublicKey());
		assertVerify(EdDSASignature.ED25519, key("Ed25519", "ed25519"), cert.getPublicKey());
		
		assertEquals("Ed25519", EdDSASignature.ED25519.keyAlgorithm());
		assertX509Encoding("Ed25519", pair.getPublic());
	}

	@Test
	public void testEd448() throws Exception {
		Assume.assumeTrue(JAVA15);
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("Ed448");
		KeyPair pair = keyGen.generateKeyPair();
		assertTrue(EdDSASignature.ED448.isImplemented());
		assertVerify(EdDSASignature.ED448, pair.getPrivate(), pair.getPublic());
		
		X509Certificate cert = cert("ed448");
		assertNotNull(cert);
		assertEquals("Ed448", cert.getSigAlgName());
		cert.verify(cert.getPublicKey());
		assertVerify(EdDSASignature.ED448, key("Ed448", "ed448"), cert.getPublicKey());

		assertEquals("Ed448", EdDSASignature.ED448.keyAlgorithm());
		assertX509Encoding("Ed448", pair.getPublic());
	}
	
	@Test
	public void testImplemented() {
		assertTrue(EdDSASignature.implemented("SHA1withECDSA"));
		assertFalse(EdDSASignature.implemented("XXXSignature"));
	}

}
