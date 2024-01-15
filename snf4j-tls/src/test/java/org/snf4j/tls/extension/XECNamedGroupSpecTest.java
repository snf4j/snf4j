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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import org.junit.Assume;
import org.junit.Test;
import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.alert.AlertDescription;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.crypto.IXDHKeyExchange;

public class XECNamedGroupSpecTest extends ExtensionTest {

	@Test
	public void test() throws Exception {
		Assume.assumeTrue(JAVA11);

		XECNamedGroupSpec spec = XECNamedGroupSpec.X25519;
		KeyPair kp = spec.getKeyExchange().generateKeyPair(RANDOM);
		spec.getData(buffer, kp.getPublic());
		byte[] data = buffer();
		ParsedKey pk = spec.parse(ByteBufferArray.wrap(array(data,0)), data.length);
		assertEquals(kp.getPublic(),spec.generateKey(pk));
		buffer.clear();
		
		spec = XECNamedGroupSpec.X448;
		kp = spec.getKeyExchange().generateKeyPair(RANDOM);
		spec.getData(buffer, kp.getPublic());
		data = buffer();
		pk = spec.parse(ByteBufferArray.wrap(array(data,0)), data.length);
		assertEquals(kp.getPublic(),spec.generateKey(pk));
	}
	
	@Test
	public void testGetData() throws Exception {
		TestXDHKeyExchange kex = new TestXDHKeyExchange();
		XECNamedGroupSpec spec = new XECNamedGroupSpec(kex, 32);
		kex.u = new BigInteger(1, bytes(1,2,3));
		spec.getData(buffer, (PublicKey)null);
		assertArrayEquals(cat(bytes(3,2,1),new byte[29]), buffer());
		buffer.clear();
		
		byte[] d1to32 = bytes(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32);
		byte[] d32to1 = bytes(32,31,30,29,28,27,26,25,24,23,22,21,20,19,18,17,16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1);
		
		kex.u = new BigInteger(1, d1to32);
		spec.getData(buffer, (PublicKey)null);
		assertArrayEquals(d32to1, buffer());
		buffer.clear();

		kex.u = new BigInteger(1, bytes(0,255,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32));
		spec.getData(buffer, (PublicKey)null);
		assertArrayEquals(bytes(32,31,30,29,28,27,26,25,24,23,22,21,20,19,18,17,16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,255), buffer());
		buffer.clear();

		kex.u = new BigInteger(1, cat(bytes(0),d1to32));
		spec.getData(buffer, (PublicKey)null);
		assertArrayEquals(d32to1, buffer());
		buffer.clear();

		spec.getData(buffer, d32to1);
		assertArrayEquals(d32to1, buffer());
		buffer.clear();

		spec.getData(buffer, cat(d32to1,bytes(0)));
		assertArrayEquals(d32to1, buffer());
		buffer.clear();

		spec.getData(buffer, cat(d32to1,bytes(0,0,0)));
		assertArrayEquals(d32to1, buffer());
		buffer.clear();
		
        kex.u = new BigInteger(1, bytes(32,(byte)1,(byte)2,(byte)3));
		spec.getData(buffer, (PublicKey)null);
		byte[] data = buffer();
		ParsedKey pk = spec.parse(ByteBufferArray.wrap(array(data,0)), data.length);
		buffer.clear();
		spec.getData(buffer, pk);
		assertArrayEquals(data, buffer());
		
		kex.u = new BigInteger(1, bytes(0,0,1,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32));
		try {
			spec.getData(buffer, (PublicKey)null);
		} catch (IllegalArgumentException e) {}

		kex.u = new BigInteger(1, bytes(1,0,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32));
		try {
			spec.getData(buffer, (PublicKey)null);
		} catch (IllegalArgumentException e) {}
		try {
			spec.getData(buffer, cat(d32to1,bytes(1,0,0)));
		} catch (IllegalArgumentException e) {}
		try {
			spec.getData(buffer, cat(d32to1,bytes(0,0,1)));
		} catch (IllegalArgumentException e) {}
	}
	
	@Test
	public void testParse() throws Exception {
		TestXDHKeyExchange kex = new TestXDHKeyExchange();
		XECNamedGroupSpec spec = new XECNamedGroupSpec(kex, 32);
		kex.u = new BigInteger(1, bytes(32,(byte)1,(byte)2,(byte)3));
		
		spec.getData(buffer, (PublicKey)null);
		byte[] data = buffer();
		ParsedKey pk = spec.parse(ByteBufferArray.wrap(array(data,0)), data.length);
		TestPublicKey k = (TestPublicKey) spec.generateKey(pk);
		assertEquals(kex.u, k.u);
	}
	
	@Test
	public void testisImplemented() {
		assertEquals(JAVA11, XECNamedGroupSpec.X25519.isImplemented());
		assertEquals(JAVA11, XECNamedGroupSpec.X448.isImplemented());
	}
	
	@Test
	public void testGetDataLength() {
		assertEquals(32, XECNamedGroupSpec.X25519.getDataLength());
		assertEquals(56, XECNamedGroupSpec.X448.getDataLength());
	}
	
	@Test
	public void testReverse() {
		byte[] reversed = bytes();
		XECNamedGroupSpec.reverse(reversed);
		assertArrayEquals(bytes(), reversed);
		
		reversed = bytes(1);
		XECNamedGroupSpec.reverse(reversed);
		assertArrayEquals(bytes(1), reversed);

		reversed = bytes(1,2);
		XECNamedGroupSpec.reverse(reversed);
		assertArrayEquals(bytes(2,1), reversed);

		reversed = bytes(1,2,3);
		XECNamedGroupSpec.reverse(reversed);
		assertArrayEquals(bytes(3,2,1), reversed);

		reversed = bytes(1,2,3,4);
		XECNamedGroupSpec.reverse(reversed);
		assertArrayEquals(bytes(4,3,2,1), reversed);

		reversed = bytes(1,2,3,4,5);
		XECNamedGroupSpec.reverse(reversed);
		assertArrayEquals(bytes(5,4,3,2,1), reversed);
	}
	
	void assertFailure(XECNamedGroupSpec spec, byte[] data, int remaining, AlertDescription desc, String message) {
		try {
			ParsedKey key = spec.parse(ByteBufferArray.wrap(array(data,0)), remaining);
			spec.generateKey(key);
			fail();
		} catch (Alert e) {
			assertSame(desc, e.getDescription());
			assertEquals(message, e.getMessage());
		}
	}

	@Test
	public void testParseFailures() throws Exception {
		assertFailure(XECNamedGroupSpec.X25519, new byte[32], 31, AlertDescription.DECODE_ERROR, 
				"Extension 'key_share' parsing failure: XEC key exchange unexpected size");

		TestXDHKeyExchange kex = new TestXDHKeyExchange();
		XECNamedGroupSpec spec = new XECNamedGroupSpec(kex, 32);
		kex.u = new BigInteger(1, bytes(32,(byte)1,(byte)2,(byte)3));
		
		spec.getData(buffer, (PublicKey)null);
		byte[] data = buffer();
		
		kex.e = new NoSuchAlgorithmException();
		assertFailure(spec, data, data.length, AlertDescription.INTERNAL_ERROR, 
				"Extension 'key_share' internal failure: No XDH algorithm");
		kex.e = new InvalidKeySpecException();
		assertFailure(spec, data, data.length, AlertDescription.INTERNAL_ERROR, 
				"Extension 'key_share' internal failure: Invalid XEC key specification");
		kex.e = new Exception();
		assertFailure(spec, data, data.length, AlertDescription.INTERNAL_ERROR, 
				"Extension 'key_share' internal failure: XEC key generation failure");
	}
	
	static class TestXDHKeyExchange implements IXDHKeyExchange {

		BigInteger u;
		
		Exception e;
		
		@Override
		public String getAlgorithm() {
			return "x25519";
		}

		@Override
		public boolean isImplemented() {
			return true;
		}

		@Override
		public byte[] generateSecret(PrivateKey privateKey, PublicKey publicKey, SecureRandom random) throws NoSuchAlgorithmException, InvalidKeyException {
			return null;
		}

		@Override
		public KeyPair generateKeyPair(SecureRandom random) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
			return null;
		}

		@Override
		public PublicKey generatePublicKey(BigInteger u) throws NoSuchAlgorithmException, InvalidKeySpecException {
			if (e != null) {
				if (e instanceof NoSuchAlgorithmException) {
					throw (NoSuchAlgorithmException)e;
				} else if (e instanceof InvalidKeySpecException) {
					throw (InvalidKeySpecException)e;
				}
				throw new RuntimeException(e);
			}
			return new TestPublicKey(u);
		}

		@Override
		public BigInteger getU(PublicKey key) {
			return u;
		}
	}
	
	static class TestPublicKey implements PublicKey {

		private static final long serialVersionUID = 1L;

		BigInteger u;
		
		TestPublicKey(BigInteger u) {
			this.u = u;
		}
		
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
		}
	}
}
