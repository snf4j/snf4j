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
package org.snf4j.quic.engine;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.crypto.QuicKeySchedule;
import org.snf4j.quic.crypto.SecretKeys;
import org.snf4j.quic.engine.CryptorTest.TestProtector;
import org.snf4j.tls.cipher.CipherSuiteSpec;
import org.snf4j.tls.crypto.AESAead;
import org.snf4j.tls.crypto.AeadId;
import org.snf4j.tls.crypto.Hash;
import org.snf4j.tls.crypto.Hkdf;
import org.snf4j.tls.crypto.IAead;
import org.snf4j.tls.crypto.IAeadDecrypt;
import org.snf4j.tls.crypto.TrafficKeys;

public class DecryptorTest extends CommonTest {

	@Test
	public void testFailingDecrypt() throws Exception {
		Hkdf h = new Hkdf(Hash.SHA256.createMac());
		QuicKeySchedule ks = new QuicKeySchedule(h, CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		
		ks.deriveInitialSecrets(new byte[16], bytes("1122334455667788"));
		TrafficKeys tKeys = ks.deriveInitialTrafficKeys();
		SecretKeys hpKeys = ks.deriveInitialHeaderProtectionKeys();
		HeaderProtector hp = new HeaderProtector(AeadId.AES_128_GCM, hpKeys.getClientKey());
		Encryptor e = new Encryptor(tKeys.getAeadEncrypt(true), hp, new byte[12], 1000);
		byte[] payload = bytes("11223344556677889900");
		byte[] encrypted = e.getAead().encrypt(bytes("000000000001"), bytes("100111"), payload);
		
		Decryptor d = new Decryptor(tKeys.getAeadDecrypt(true), hp, new byte[12], 1000, 1000);
		
		byte[] decrypted = d.getAead().decrypt(bytes("000000000001"), bytes("100111"), encrypted);
		assertArrayEquals(payload, decrypted);
		decrypted = d.getAead().decrypt(bytes("000000000001"), bytes("100111"), encrypted);
		assertArrayEquals(payload, decrypted);
		
		encrypted[4] ^= 0xaa;
		try {
			d.getAead().decrypt(bytes("000000000001"), bytes("100111"), encrypted);
			fail();
		}
		catch (Exception ex) {
		}

		encrypted[4] ^= 0xaa;
		decrypted = d.getAead().decrypt(bytes("000000000001"), bytes("100111"), encrypted);
		assertArrayEquals(payload, decrypted);
	}
	
	@Test
	public void testIncInvalidPackets() throws Exception {
		Decryptor d = new Decryptor(new TestDecrypt(), new TestProtector(), bytes("11"), 100, 2000);
		
		assertFalse(d.isIntegrityLimitReached());
		d.incInvalidPackets(1999);
		assertFalse(d.isIntegrityLimitReached());
		d.incInvalidPackets(1);
		assertFalse(d.isIntegrityLimitReached());
		d.incInvalidPackets(1);
		assertTrue(d.isIntegrityLimitReached());
	}

	@Test
	public void testUpdatePacketNumber() throws Exception {
		Decryptor d = new Decryptor(new TestDecrypt(), new TestProtector(), bytes("11"), 100, 2000);

		assertEquals(0, d.getMaxPacketNumber());
		assertEquals(Long.MAX_VALUE, d.getMinPacketNumber());
		
		d.updatePacketNumber(10);
		assertEquals(10, d.getMaxPacketNumber());
		assertEquals(10, d.getMinPacketNumber());
		d.updatePacketNumber(9);
		assertEquals(10, d.getMaxPacketNumber());
		assertEquals(9, d.getMinPacketNumber());
		d.updatePacketNumber(2);
		assertEquals(10, d.getMaxPacketNumber());
		assertEquals(2, d.getMinPacketNumber());
		d.updatePacketNumber(3);
		assertEquals(10, d.getMaxPacketNumber());
		assertEquals(2, d.getMinPacketNumber());
		d.updatePacketNumber(10);
		assertEquals(10, d.getMaxPacketNumber());
		assertEquals(2, d.getMinPacketNumber());
		d.updatePacketNumber(11);
		assertEquals(11, d.getMaxPacketNumber());
		assertEquals(2, d.getMinPacketNumber());
	}
	
	@Test
	public void testIncPackets() throws Exception {
		Cryptor c = new Decryptor(new TestDecrypt(), new TestProtector(), bytes("11"), 1000, 100);
		
		assertFalse(c.isConfidentialityLimitReached());
		c.incPackets(999);
		assertFalse(c.isConfidentialityLimitReached());
		c.incPackets(1);
		assertFalse(c.isConfidentialityLimitReached());
		c.incPackets(1);
		assertTrue(c.isConfidentialityLimitReached());
	}
	
	@Test
	public void testErase() throws Exception {
		TestProtector p = new TestProtector();
		TestDecrypt d = new TestDecrypt();
		byte[] iv = bytes("1020304f");
		Cryptor c = new Decryptor(d, p, iv, 11, 12);
		
		assertEquals(0, p.eraseCount);
		assertEquals(0, d.eraseCount);
		assertArrayEquals(bytes("1020304f"), iv);
		c.erase(true);
		assertEquals(0, p.eraseCount);
		assertEquals(1, d.eraseCount);
		assertArrayEquals(bytes("00000000"), iv);
		assertSame(p, c.getProtector());
		
		p = new TestProtector();
		d = new TestDecrypt();
		iv = bytes("1020304f");
		c = new Decryptor(d, p, iv, 11, 12);
		c.erase(false);
		assertEquals(1, p.eraseCount);
		assertEquals(1, d.eraseCount);
		assertArrayEquals(bytes("00000000"), iv);
		assertSame(p, c.getProtector());
	}

	class TestDecrypt implements IAeadDecrypt {

		int eraseCount;
		
		@Override
		public IAead getAead() {
			return AESAead.AEAD_AES_128_GCM;
		}


		@Override
		public void erase() {
			eraseCount++;
		}


		@Override
		public byte[] decrypt(byte[] nonce, byte[] additionalData, byte[] ciphertext) throws GeneralSecurityException {
			return null;
		}


		@Override
		public void decrypt(byte[] nonce, byte[] additionalData, ByteBuffer ciphertext, ByteBuffer plaintext)
				throws GeneralSecurityException {
		}
	}
}
