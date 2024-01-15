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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import org.junit.Assume;
import org.junit.Test;

public class RSASSAPSSSignatureTest extends SignatureTest {

	@Test
	public void testMinKeySize() {
		assertEquals(528, RSASSAPSSSignature.RSA_PSS_RSAE_SHA256.minKeySize());
		assertEquals(528, RSASSAPSSSignature.RSA_PSS_PSS_SHA256.minKeySize());
		assertEquals(784, RSASSAPSSSignature.RSA_PSS_RSAE_SHA384.minKeySize());
		assertEquals(784, RSASSAPSSSignature.RSA_PSS_PSS_SHA384.minKeySize());
		assertEquals(1040, RSASSAPSSSignature.RSA_PSS_RSAE_SHA512.minKeySize());
		assertEquals(1040, RSASSAPSSSignature.RSA_PSS_PSS_SHA512.minKeySize());
	}
	
	@Test
	public void testIsImplemented() {
		assertEquals(JAVA11 || JAVA8_GEQ_U392, RSASSAPSSSignature.RSA_PSS_RSAE_SHA256.isImplemented());
		assertEquals(JAVA11 || JAVA8_GEQ_U392, RSASSAPSSSignature.RSA_PSS_RSAE_SHA384.isImplemented());
		assertEquals(JAVA11 || JAVA8_GEQ_U392, RSASSAPSSSignature.RSA_PSS_RSAE_SHA512.isImplemented());
		assertEquals(JAVA11 || JAVA8_GEQ_U392, RSASSAPSSSignature.RSA_PSS_PSS_SHA256.isImplemented());
		assertEquals(JAVA11 || JAVA8_GEQ_U392, RSASSAPSSSignature.RSA_PSS_PSS_SHA384.isImplemented());
		assertEquals(JAVA11 || JAVA8_GEQ_U392, RSASSAPSSSignature.RSA_PSS_PSS_SHA512.isImplemented());
	}
	
	@Test
	public void testKeyAlgorithm() {
		assertEquals("RSA", RSASSAPSSSignature.RSA_PSS_RSAE_SHA256.keyAlgorithm());
		assertEquals("RSA", RSASSAPSSSignature.RSA_PSS_RSAE_SHA384.keyAlgorithm());
		assertEquals("RSA", RSASSAPSSSignature.RSA_PSS_RSAE_SHA512.keyAlgorithm());
		assertEquals("RSASSA-PSS", RSASSAPSSSignature.RSA_PSS_PSS_SHA256.keyAlgorithm());
		assertEquals("RSASSA-PSS", RSASSAPSSSignature.RSA_PSS_PSS_SHA384.keyAlgorithm());
		assertEquals("RSASSA-PSS", RSASSAPSSSignature.RSA_PSS_PSS_SHA512.keyAlgorithm());
	}

	@Test
	public void testAlgorithm() {
		assertEquals("RSASSA-PSS", RSASSAPSSSignature.RSA_PSS_RSAE_SHA256.algorithm());
		assertEquals("RSASSA-PSS", RSASSAPSSSignature.RSA_PSS_RSAE_SHA384.algorithm());
		assertEquals("RSASSA-PSS", RSASSAPSSSignature.RSA_PSS_RSAE_SHA512.algorithm());
		assertEquals("RSASSA-PSS", RSASSAPSSSignature.RSA_PSS_PSS_SHA256.algorithm());
		assertEquals("RSASSA-PSS", RSASSAPSSSignature.RSA_PSS_PSS_SHA384.algorithm());
		assertEquals("RSASSA-PSS", RSASSAPSSSignature.RSA_PSS_PSS_SHA512.algorithm());
	}
	
	@Test
	public void testCreateParameter() {
		PSSParameterSpec p = (PSSParameterSpec) RSASSAPSSSignature.RSA_PSS_RSAE_SHA256.createParameter();
		assertEquals("MGF1", p.getMGFAlgorithm());
		assertEquals(32, p.getSaltLength());
		assertEquals("SHA-256", p.getDigestAlgorithm());
		assertEquals(1, p.getTrailerField());
		assertEquals("SHA-256", ((MGF1ParameterSpec)p.getMGFParameters()).getDigestAlgorithm());

		p = (PSSParameterSpec) RSASSAPSSSignature.RSA_PSS_RSAE_SHA512.createParameter();
		assertEquals("MGF1", p.getMGFAlgorithm());
		assertEquals(64, p.getSaltLength());
		assertEquals("SHA-512", p.getDigestAlgorithm());
		assertEquals(1, p.getTrailerField());
		assertEquals("SHA-512", ((MGF1ParameterSpec)p.getMGFParameters()).getDigestAlgorithm());
	}
	
	@Test
	public void testMethod() {
		assertNotNull(RSASSAPSSSignature.method(Object.class.getName(), "toString"));
		assertNull(RSASSAPSSSignature.method(Object.class.getName(), "XXX"));
	}
	
	@Test
	public void testCreateSignature() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		RSASSAPSSSignature signature = new RSASSAPSSSignature("RSASSA-PSS", "RSA", 528, "SHA-256", 32) {
			
			String algorithm;
			
			@Override
			AlgorithmParameterSpec createParameter() {
				return null;
			}

			@Override
			public Signature createSignature(String algorithm) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
				this.algorithm = algorithm;
				return new Signature("xxx") {

					@Override
					protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
					}

					@Override
					protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
					}

					@Override
					protected void engineUpdate(byte b) throws SignatureException {
					}

					@Override
					protected void engineUpdate(byte[] b, int off, int len) throws SignatureException {
					}

					@Override
					protected byte[] engineSign() throws SignatureException {
						return null;
					}

					@Override
					protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
						return false;
					}

					@Override
					protected void engineSetParameter(String param, Object value) throws InvalidParameterException {
					}

					@Override
					protected void engineSetParameter(AlgorithmParameterSpec params) throws InvalidAlgorithmParameterException {
					}
					
					@Override
					protected Object engineGetParameter(String param) throws InvalidParameterException {
						return null;
					}
				};
			}
			
			@Override
			public String toString() {
				return algorithm;
			}
		};
		assertNull(signature.toString());
		signature.createSignature();
		assertEquals("RSASSA-PSS", signature.toString());
		assertEquals("SHA256withRSA", RSASSAPSSSignature.RSA_PSS_RSAE_SHA256.createSignature("SHA256withRSA").getAlgorithm());
	}
	
	@Test
	public void testRsaPssRsaeSha256() throws Exception {
		Assume.assumeTrue(JAVA11);
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(528);
		KeyPair pair = keyGen.generateKeyPair();
		assertVerify(RSASSAPSSSignature.RSA_PSS_RSAE_SHA256, pair.getPrivate(), pair.getPublic());
		
		X509Certificate cert = cert("rsapsssha256");
		assertNotNull(cert);
		assertEquals("RSASSA-PSS", cert.getSigAlgName());
		cert.verify(cert.getPublicKey());
		assertVerify(RSASSAPSSSignature.RSA_PSS_RSAE_SHA256, rsaKey("rsa2048"), cert.getPublicKey());
		assertTrue(RSASSAPSSSignature.RSA_PSS_RSAE_SHA256.matches(cert));
		assertFalse(RSASSAPSSSignature.RSA_PSS_RSAE_SHA384.matches(cert));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.matches(cert));
		assertTrue(RSASSAPSSSignature.RSA_PSS_RSAE_SHA512.matchesByKey(cert));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.matchesByKey(cert));
		assertTrue(RSAPKCS1Signature.RSA_PKCS1_SHA256.matchesByKey(cert));
		assertTrue(RSASSAPSSSignature.RSA_PSS_RSAE_SHA384.matchesByKey(cert("rsasha256")));
		assertFalse(RSASSAPSSSignature.RSA_PSS_RSAE_SHA512.matchesByKey(cert("rsasha256")));
		
		assertEquals("RSA", RSASSAPSSSignature.RSA_PSS_RSAE_SHA256.keyAlgorithm());
		assertX509Encoding("RSA", pair.getPublic());	
	}

	@Test
	public void testRsaPssRsaeSha384() throws Exception {
		Assume.assumeTrue(JAVA11);
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(784);
		KeyPair pair = keyGen.generateKeyPair();
		assertVerify(RSASSAPSSSignature.RSA_PSS_RSAE_SHA384, pair.getPrivate(), pair.getPublic());
		
		X509Certificate cert = cert("rsapsssha384");
		assertNotNull(cert);
		assertEquals("RSASSA-PSS", cert.getSigAlgName());
		cert.verify(cert.getPublicKey());
		assertVerify(RSASSAPSSSignature.RSA_PSS_RSAE_SHA384, rsaKey("rsa2048"), cert.getPublicKey());
		assertTrue(RSASSAPSSSignature.RSA_PSS_RSAE_SHA384.matches(cert));
		assertFalse(RSASSAPSSSignature.RSA_PSS_RSAE_SHA256.matches(cert));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA384.matches(cert));

		assertEquals("RSA", RSASSAPSSSignature.RSA_PSS_RSAE_SHA384.keyAlgorithm());
		assertX509Encoding("RSA", pair.getPublic());	
	}

	@Test
	public void testRsaPssRsaeSha512() throws Exception {
		Assume.assumeTrue(JAVA11);
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(1040);
		KeyPair pair = keyGen.generateKeyPair();
		assertVerify(RSASSAPSSSignature.RSA_PSS_RSAE_SHA512, pair.getPrivate(), pair.getPublic());

		X509Certificate cert = cert("rsapsssha512");
		assertNotNull(cert);
		assertEquals("RSASSA-PSS", cert.getSigAlgName());
		cert.verify(cert.getPublicKey());
		assertVerify(RSASSAPSSSignature.RSA_PSS_RSAE_SHA512, rsaKey("rsa2048"), cert.getPublicKey());
		assertTrue(RSASSAPSSSignature.RSA_PSS_RSAE_SHA512.matches(cert));
		assertFalse(RSASSAPSSSignature.RSA_PSS_RSAE_SHA256.matches(cert));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA512.matches(cert));

		assertEquals("RSA", RSASSAPSSSignature.RSA_PSS_RSAE_SHA512.keyAlgorithm());
		assertX509Encoding("RSA", pair.getPublic());	
	}

	@Test
	public void testRsaPssPssSha256() throws Exception {
		Assume.assumeTrue(JAVA11);
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSASSA-PSS");
		keyGen.initialize(528);
		KeyPair pair = keyGen.generateKeyPair();
		assertVerify(RSASSAPSSSignature.RSA_PSS_PSS_SHA256, pair.getPrivate(), pair.getPublic());

		X509Certificate cert = cert("rsapsspsssha256");
		assertNotNull(cert);
		assertEquals("RSASSA-PSS", cert.getSigAlgName());
		cert.verify(cert.getPublicKey());
		assertVerify(RSASSAPSSSignature.RSA_PSS_PSS_SHA256, key("RSASSA-PSS", "rsapsssha256"), cert.getPublicKey());
		assertTrue(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.matches(cert));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA384.matches(cert));
		assertFalse(RSASSAPSSSignature.RSA_PSS_RSAE_SHA256.matches(cert));
		assertTrue(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.matchesByKey(cert));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA384.matchesByKey(cert));
		assertFalse(RSASSAPSSSignature.RSA_PSS_RSAE_SHA256.matchesByKey(cert));
		assertFalse(RSAPKCS1Signature.RSA_PKCS1_SHA256.matchesByKey(cert));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.matchesByKey(cert("rsapsssha256")));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.matchesByKey(cert("rsasha256")));

		assertEquals("RSASSA-PSS", RSASSAPSSSignature.RSA_PSS_PSS_SHA256.keyAlgorithm());
		assertX509Encoding("RSASSA-PSS", pair.getPublic());	
	}

	@Test
	public void testRsaPssPssSha384() throws Exception {
		Assume.assumeTrue(JAVA11);
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSASSA-PSS");
		keyGen.initialize(784);
		KeyPair pair = keyGen.generateKeyPair();
		assertVerify(RSASSAPSSSignature.RSA_PSS_PSS_SHA384, pair.getPrivate(), pair.getPublic());

		X509Certificate cert = cert("rsapsspsssha384");
		assertNotNull(cert);
		assertEquals("RSASSA-PSS", cert.getSigAlgName());
		cert.verify(cert.getPublicKey());
		assertVerify(RSASSAPSSSignature.RSA_PSS_PSS_SHA384, key("RSASSA-PSS", "rsapsssha384"), cert.getPublicKey());
		assertTrue(RSASSAPSSSignature.RSA_PSS_PSS_SHA384.matches(cert));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.matches(cert));
		assertFalse(RSASSAPSSSignature.RSA_PSS_RSAE_SHA384.matches(cert));

		assertEquals("RSASSA-PSS", RSASSAPSSSignature.RSA_PSS_PSS_SHA384.keyAlgorithm());
		assertX509Encoding("RSASSA-PSS", pair.getPublic());	
	}

	@Test
	public void testRsaPssPssSha512() throws Exception {
		Assume.assumeTrue(JAVA11);
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSASSA-PSS");
		keyGen.initialize(1040);
		KeyPair pair = keyGen.generateKeyPair();
		assertVerify(RSASSAPSSSignature.RSA_PSS_PSS_SHA512, pair.getPrivate(), pair.getPublic());

		X509Certificate cert = cert("rsapsspsssha512");
		assertNotNull(cert);
		assertEquals("RSASSA-PSS", cert.getSigAlgName());
		cert.verify(cert.getPublicKey());
		assertVerify(RSASSAPSSSignature.RSA_PSS_PSS_SHA512, key("RSASSA-PSS", "rsapsssha512"), cert.getPublicKey());
		assertTrue(RSASSAPSSSignature.RSA_PSS_PSS_SHA512.matches(cert));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA384.matches(cert));
		assertFalse(RSASSAPSSSignature.RSA_PSS_RSAE_SHA512.matches(cert));

		assertEquals("RSASSA-PSS", RSASSAPSSSignature.RSA_PSS_PSS_SHA512.keyAlgorithm());
		assertX509Encoding("RSASSA-PSS", pair.getPublic());	
	}
	
	@Test
	public void testImplemented() {
		assertTrue(RSASSAPSSSignature.implemented("SHA1withECDSA"));
		assertFalse(RSASSAPSSSignature.implemented("XXXSignature"));
	}
	
	@Test
	public void testKeyMatches() throws Exception {
		Method m1 = TestKey.class.getDeclaredMethod("getParams");
		PSSParameterSpec pss = new PSSParameterSpec("SHA-256", "MGF1", new MGF1ParameterSpec("SHA-256"), 32, 1);
		
		assertTrue(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.keyMatches(new TestKey("SHA-256", pss), m1));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA384.keyMatches(new TestKey("SHA-256", pss), m1));
		assertFalse(RSASSAPSSSignature.RSA_PSS_RSAE_SHA256.keyMatches(new TestKey("SHA-256", pss), m1));

		assertTrue(RSASSAPSSSignature.RSA_PSS_RSAE_SHA256.keyMatches(new TestKey(null), m1));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.keyMatches(new TestKey(null), m1));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.keyMatches(new TestKey(null), m1));

		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.keyMatches(new TestKey(null), String.class.getDeclaredMethod("toString")));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.keyMatches(new TestKey(null), null));	
				
		X509Certificate cert = cert("rsasha256");
		RSASSAPSSSignature s = new RSASSAPSSSignature("SHA256withRSA", "RSA", 1024, "RSA", 16);
		assertTrue(s.keyMatches(cert, cert.getPublicKey(), m1));
		s = new RSASSAPSSSignature("SHA256withRSA", "RSA", 1025, "RSA", 16);
		assertFalse(s.keyMatches(cert, cert.getPublicKey(), m1));

		s = new RSASSAPSSSignature("RSASSA-PSS", "RSASSA-PSS", 1024, "SHA-256", 16);
		assertTrue(s.keyMatches(cert, new TestKey("RSASSA-PSS", pss, (RSAKey) cert.getPublicKey()), m1));
		s = new RSASSAPSSSignature("RSASSA-PSS", "RSASSA-PSS", 1024, "SHA-384", 16);
		assertFalse(s.keyMatches(cert, new TestKey("RSASSA-PSS", pss, (RSAKey) cert.getPublicKey()), m1));
	}
	
	@Test
	public void testPssParamsMatches() throws Exception {
		byte[] sha256 = new byte[] {48, 48, -96, 13, 48, 11, 6, 9, 96, -122, 72, 1, 101, 3, 4, 2, 1, -95, 26, 48, 24, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 8, 48, 11, 6, 9, 96, -122, 72, 1, 101, 3, 4, 2, 1, -94, 3, 2, 1, 32};
		byte[] sha384 = new byte[] {48, 48, -96, 13, 48, 11, 6, 9, 96, -122, 72, 1, 101, 3, 4, 2, 2, -95, 26, 48, 24, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 8, 48, 11, 6, 9, 96, -122, 72, 1, 101, 3, 4, 2, 2, -94, 3, 2, 1, 48};
		byte[] sha512 = new byte[] {48, 48, -96, 13, 48, 11, 6, 9, 96, -122, 72, 1, 101, 3, 4, 2, 3, -95, 26, 48, 24, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 8, 48, 11, 6, 9, 96, -122, 72, 1, 101, 3, 4, 2, 3, -94, 3, 2, 1, 64};

		assertTrue(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.pssParamsMatches(sha256));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA384.pssParamsMatches(sha256));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA512.pssParamsMatches(sha256));
		assertTrue(RSASSAPSSSignature.RSA_PSS_PSS_SHA384.pssParamsMatches(sha384));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.pssParamsMatches(sha384));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA512.pssParamsMatches(sha384));
		assertTrue(RSASSAPSSSignature.RSA_PSS_PSS_SHA512.pssParamsMatches(sha512));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA384.pssParamsMatches(sha512));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.pssParamsMatches(sha512));
		
		sha256 = new byte[] {-96, 13, 48, 11, 6, 9, 96, -122, 72, 1, 101, 3, 4, 2, 1};
		assertTrue(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.pssParamsMatches(sha256));
		sha256 = new byte[] {-96, 13, 48, 11, 6, 9, 96, -122, 72, 1, 101, 4, 4, 2, 1};
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.pssParamsMatches(sha256));
		sha256 = new byte[] {-96, 13, 48, 11, 6, 9, 96, -122, 72, 1, 101, 3, 4, 2, 9};
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.pssParamsMatches(sha256));
		sha256 = new byte[] {-96, 13, 48, 11, 6, 9, 96, -122, 72, 1, 101, 3, 4, 2};
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.pssParamsMatches(sha256));

		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.pssParamsMatches(bytes()));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.pssParamsMatches(bytes(1)));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.pssParamsMatches(bytes(1)));

		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.pssParamsMatches(null));
		assertTrue(RSASSAPSSSignature.RSA_PSS_RSAE_SHA256.pssParamsMatches(null));
	}
	
	@Test
	public void testCertMatches() throws Exception {
		Method m1 = Key.class.getDeclaredMethod("getAlgorithm");
		
		X509Certificate cert = cert("rsasha256");
		RSASSAPSSSignature s = new RSASSAPSSSignature("SHA256withRSA", "RSA", 1024, "RSA", 16) {
			boolean pssParamsMatches(byte[] pssParams) { return true;}
		};
		assertTrue(s.certMatches(cert, null, m1));
		s = new RSASSAPSSSignature("SHA256withRSA", "RSA", 1024, "RSA", 16) {
			boolean pssParamsMatches(byte[] pssParams) { return false;}
		};
		assertFalse(s.certMatches(cert, null, m1));
		s = new RSASSAPSSSignature("SHA256withRSA", "RSA", 1025, "RSA", 16) {
			boolean pssParamsMatches(byte[] pssParams) { return true;}
		};
		assertFalse(s.certMatches(cert, null, m1));
		s = new RSASSAPSSSignature("SHA256withRSA2", "RSA", 1024, "RSA", 16) {
			boolean pssParamsMatches(byte[] pssParams) { return true;}
		};
		assertFalse(s.certMatches(cert, null, m1));
		s = new RSASSAPSSSignature("SHA256withRSA", "RSA2", 1024, "RSA", 16) {
			boolean pssParamsMatches(byte[] pssParams) { return true;}
		};
		assertFalse(s.certMatches(cert, null, m1));
	}

	@Test
	public void testKeyMatche2s() throws Exception {
	}
	
	@Test
	public void testMatches() throws Exception { 
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.matches(cert("rsapsssha384")));
		assertFalse(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.matchesByKey(cert("rsapsssha384")));
	}
	
	class TestKey implements Key, RSAKey {

		private static final long serialVersionUID = 1L;

		String algorithm;
		
		PSSParameterSpec params;
		
		RSAKey key;
		
		TestKey(String algorithm) {
			this.algorithm = algorithm;
		}

		TestKey(String algorithm, PSSParameterSpec params) {
			this.algorithm = algorithm;
			this.params = params;
		}

		TestKey(String algorithm, PSSParameterSpec params, RSAKey key) {
			this.algorithm = algorithm;
			this.params = params;
			this.key = key;
		}
		
		@Override
		public String getAlgorithm() {
			return algorithm;
		}

		@Override
		public String getFormat() {
			return null;
		}

		@Override
		public byte[] getEncoded() {
			return null;
		}
		
		public PSSParameterSpec getParams() {
			return params;
		}

		@Override
		public BigInteger getModulus() {
			return key.getModulus();
		}
	}
}
