/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Constructor;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Assume;
import org.junit.Test;
import org.snf4j.quic.CommonTest;

public class ChaCha20HeaderProtectionTest extends CommonTest {

	@Test
	public void testIsImplemented() {
		assertEquals(JAVA11, ChaCha20HeaderProtection.HP_CHACHA20.isImplemented());
	}
	
	@Test
	public void testGetKeyLength() {
		assertEquals(32, ChaCha20HeaderProtection.HP_CHACHA20.getKeyLength());
	}
	
	@Test
	public void testContructor() {
		assertNull(ChaCha20HeaderProtection.contructor("org.snf4j.XXX"));
		assertNotNull(ChaCha20HeaderProtection.contructor("org.snf4j.quic.crypto.TestChaCha20ParameterSpec"));
	}
	
	@Test
	public void testParameterSpec() throws Exception {
		Constructor<?> c = ChaCha20HeaderProtection.contructor("org.snf4j.quic.crypto.TestChaCha20ParameterSpec");
		
		assertNotNull(ChaCha20HeaderProtection.HP_CHACHA20.parameterSpec(c, new byte[0], 1));
	}
	
	@Test(expected = InvalidParameterSpecException.class)
	public void testParameterSpecEx() throws Exception {
		Constructor<?> c = ChaCha20HeaderProtection.contructor("java.lang.Object");
		
		assertNotNull(ChaCha20HeaderProtection.HP_CHACHA20.parameterSpec(c, new byte[0], 1));
	}
	
	@Test
	public void testCounter() {
		assertEquals(0, ChaCha20HeaderProtection.counter(bytes("00000000"),0));
		assertEquals(1, ChaCha20HeaderProtection.counter(bytes("01000000"),0));
		assertEquals(0x04030201, ChaCha20HeaderProtection.counter(bytes("01020304"),0));
		assertEquals(-1, ChaCha20HeaderProtection.counter(bytes("ffffffff"),0));
		assertEquals(-1, ChaCha20HeaderProtection.counter(bytes("00ffffffff00"),1));
	}

	@Test
	public void testCreateCipher() throws Exception {
		ChaCha20HeaderProtection hp = new ChaCha20HeaderProtection(32) {
			@Override
			Cipher cipher(String transformation) throws NoSuchAlgorithmException, NoSuchPaddingException {
				return Cipher.getInstance("AES/CBC/NoPadding");
			}
		};
		assertNotNull(hp.createCipher());
		assertNotNull(ChaCha20HeaderProtection.HP_CHACHA20.cipher("AES/CBC/NoPadding"));
	}

	@Test
	public void testCreateKey() throws Exception {
		ChaCha20HeaderProtection hp = ChaCha20HeaderProtection.HP_CHACHA20;
		assertNotNull(hp.createKey(bytes("25a282b9e82f06f21f488917a4fc8f1b73573685608597d0efcb076b0ab7a7a4")));
	}
	
	@Test
	public void testDeriveMask() throws Exception {
		Assume.assumeTrue(JAVA11);
		ChaCha20HeaderProtection hp = ChaCha20HeaderProtection.HP_CHACHA20;
		Cipher c = hp.createCipher();
		SecretKey k = hp.createKey(bytes("25a282b9e82f06f21f488917a4fc8f1b73573685608597d0efcb076b0ab7a7a4"));

		byte[] mask = hp.deriveMask(c, k, bytes("5e5cd55c41f69080575d7999c25a5bfb"), 0);
		assertArrayEquals(bytes("aefefe7d03"), mask);
		mask = hp.deriveMask(c, k, bytes("5e5cd55c41f69080575d7999c25a5bfb"), 0);
		assertArrayEquals(bytes("aefefe7d03"), mask);
		mask = hp.deriveMask(c, k, bytes("5e5cd55c41f69080575d7999c25a5bfb00"), 0);
		assertArrayEquals(bytes("aefefe7d03"), mask);
		mask = hp.deriveMask(c, k, bytes("005e5cd55c41f69080575d7999c25a5bfb00"), 1);
		assertArrayEquals(bytes("aefefe7d03"), mask);
	}
	
	@Test
	public void testDerieMaskJdk8() throws Exception {
		HeaderProtection hp = new HeaderProtection();
		Cipher c = hp.createCipher();
		SecretKey k = new SecretKeySpec(bytes("9f50449e04a0e810283a1e9933adedd2"), "AES");
		
		hp.deriveMask(c, k, bytes("d1b1c98dd7689fb8ec11d242b123dc9b"), 0);
		assertArrayEquals(bytes("d7689fb8ec11d242b123dc9b"), hp.nonce);
		assertEquals(0x8dc9b1d1, hp.counter);
	}
	
	static class HeaderProtection extends ChaCha20HeaderProtection {

		byte[] nonce;
		
		int counter;
		
		public HeaderProtection() {
			super(16);
		}

		@Override
		Cipher cipher(String transformation) throws NoSuchAlgorithmException, NoSuchPaddingException {
			return Cipher.getInstance("AES/GCM/NoPadding");
		}
		
		@Override
		AlgorithmParameterSpec parameterSpec(Constructor<?> contructor, byte[] nonce, int counter) throws InvalidParameterSpecException {
			this.nonce = nonce;
			this.counter = counter;
			return new GCMParameterSpec(8*16, new byte[16]);
		}
	}
}
