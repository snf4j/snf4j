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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHPublicKeySpec;

import org.junit.Assume;
import org.junit.Test;
import org.snf4j.tls.CommonTest;

public class XDHKeyExchangeTest extends CommonTest {

	BigInteger getU(PublicKey key) throws Exception {
		Method m = Class.forName("java.security.interfaces.XECPublicKey").getMethod("getU");
		return (BigInteger) m.invoke(key);
	}
	
	void assertKeyExchange(XDHKeyExchange dh, String algo, int len) throws Exception {
		KeyPair kp1 = dh.generateKeyPair();
		KeyPair kp2 = dh.generateKeyPair();
		
		assertEquals(getU(kp1.getPublic()), dh.getU(kp1.getPublic()));
		assertEquals(TLS1_3, dh.isImplemented());
		assertEquals(algo, dh.getAlgorithm());
		
		assertEquals(kp1.getPublic(), dh.generatePublicKey(getU(kp1.getPublic())));
		
		byte[] s1 = dh.generateSecret(kp1.getPrivate(), kp2.getPublic());
		byte[] s2 = dh.generateSecret(kp2.getPrivate(), kp1.getPublic());
		assertArrayEquals(s1,s2);
		assertEquals(len,s1.length);
	}

	@Test
	public void testAll() throws Exception {
		Assume.assumeTrue(TLS1_3);
		assertKeyExchange(XDHKeyExchange.X25519, "X25519", 32);
		assertKeyExchange(XDHKeyExchange.X448, "X448", 56);
	}

	@Test
	public void testGenerateSecret() throws Exception {
		KeyPair kp1 = ECKeyExchange.SECP256R1.generateKeyPair();
		KeyPair kp2 = ECKeyExchange.SECP256R1.generateKeyPair();
		
		XDHKeyExchange e = new XDHKeyExchange("ECDH", ECKeyExchange.SECP256R1.getAlgorithm());
		byte[] s1 = e.generateSecret(kp1.getPrivate(), kp2.getPublic());
		byte[] s2 = e.generateSecret(kp2.getPrivate(), kp1.getPublic());
		assertArrayEquals(s1, s2);
	}
	
	@Test
	public void testParamSpec() throws Exception {
		Constructor<?> c = Class.forName("java.security.spec.ECGenParameterSpec").getConstructor(String.class);
		
		ECGenParameterSpec spec = (ECGenParameterSpec) XDHKeyExchange.X25519.paramSpec(c);
		assertNotNull(spec);
		
		c = Class.forName("java.lang.String").getConstructor(String.class);
		try {
			XDHKeyExchange.X25519.paramSpec(c);
			fail();
		} catch (NoSuchAlgorithmException e) {}
	}
	
	@Test
	public void testGenerateKeyPair() throws Exception {
		KeyPair kp = new TestXDHKeyExchange("EC", ECKeyExchange.SECP256R1.getAlgorithm()).generateKeyPair();
		assertEquals("EC", kp.getPublic().getAlgorithm());
	}
	
	@Test
	public void testKeySpec() throws Exception {
		Constructor<?> c1 = TestAlgorithmParameterSpec.class.getConstructor(String.class);
		Constructor<?> c2 = TestKeySpec.class.getConstructor(AlgorithmParameterSpec.class, BigInteger.class);
		
		TestKeySpec spec = (TestKeySpec) XDHKeyExchange.X25519.keySpec(c1, c2, BigInteger.valueOf(1));
		assertNotNull(spec);

		c1 = Class.forName("java.lang.String").getConstructor(String.class);
		try {
			XDHKeyExchange.X25519.keySpec(c1, c2, BigInteger.valueOf(1));
		} catch (NoSuchAlgorithmException e) {}
	}
	
	@Test
	public void testgeneratePublicKey() throws Exception {
		KeyPair kp = DHKeyExchange.FFDHE2048.generateKeyPair();
		PublicKey k = new TestXDHKeyExchange("DH", DHKeyExchange.FFDHE2048.getAlgorithm()).generatePublicKey(((DHPublicKey)kp.getPublic()).getY());
		assertEquals("DH", k.getAlgorithm());
	}
	
	@Test
	public void testGetU() throws Exception {
		Method m = DHPublicKey.class.getMethod("getY");
		KeyPair pk = DHKeyExchange.FFDHE2048.generateKeyPair();
		assertEquals(((DHPublicKey)pk.getPublic()).getY(), XDHKeyExchange.X25519.getU(m, pk.getPublic()));
		
		try {
			XDHKeyExchange.X25519.getU(null, pk.getPublic());
			fail();
		} catch (UnsupportedOperationException e) {}
		m = String.class.getMethod("toString");
		try {
			XDHKeyExchange.X25519.getU(m, pk.getPublic());
			fail();
		} catch (UnsupportedOperationException e) {}
		assertEquals(BigInteger.TEN, new TestXDHKeyExchange("XDH", XDHKeyExchange.X25519.getAlgorithm()).getU(pk.getPublic()));
	}
	
	@Test
	public void testGetAlgorithm() {
		assertEquals("X25519", XDHKeyExchange.X25519.getAlgorithm());
		assertEquals("X448", XDHKeyExchange.X448.getAlgorithm());
	}
	
	@Test
	public void testIsImplemented() {
		assertEquals(TLS1_3, XDHKeyExchange.X25519.isImplemented());
	}
	
	@Test
	public void testConstructor() {
		assertNotNull(XDHKeyExchange.constructor("java.lang.String", String.class));
		assertNull(XDHKeyExchange.constructor("java.langXXX.String", String.class));
	}

	@Test
	public void testMethod() {
		assertNotNull(XDHKeyExchange.method("java.lang.String", "length"));
		assertNull(XDHKeyExchange.method("java.langXXX.String", "length"));
	}
	
	@Test
	public void testImplemented() {
		assertTrue(XDHKeyExchange.implemented("java.lang.String"));
		assertFalse(XDHKeyExchange.implemented("java.langXXX.String"));
	}
	
	static class TestXDHKeyExchange extends XDHKeyExchange {

		TestXDHKeyExchange(String dh, String algorithm) {
			super(dh, algorithm);
		}

		AlgorithmParameterSpec paramSpec(Constructor<?> paramSpec) throws NoSuchAlgorithmException {
			return new ECGenParameterSpec(getAlgorithm());
		}
		
		KeySpec keySpec(Constructor<?> paramSpec, Constructor<?> keySpec, BigInteger u) throws NoSuchAlgorithmException {
			return new DHPublicKeySpec(u, DHGroups.FFDHE2048_P, DHGroups.FFDHE_G);
		}
		
		BigInteger getU(Method getU, PublicKey key) {
			return BigInteger.TEN;
		}
	}
	
	static class TestAlgorithmParameterSpec implements AlgorithmParameterSpec {
		public TestAlgorithmParameterSpec(String s) {}
	}
	
	static class TestKeySpec implements KeySpec {
		public TestKeySpec(AlgorithmParameterSpec a, BigInteger b) {}
	}
	
}
