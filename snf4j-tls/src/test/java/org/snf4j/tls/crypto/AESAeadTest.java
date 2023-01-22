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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import javax.crypto.SecretKey;

import org.junit.Test;
import org.snf4j.tls.CommonTest;

public class AESAeadTest extends CommonTest {
	
	@Test
	public void testAeadAes128Gcm() throws Exception {
		AESAead aead = AESAead.AEAD_AES_128_GCM;
		SecretKey key = aead.createKey(bytes("feffe9928665731c6d6a8f9467308308"));
		
		AeadDecrypt dec = new AeadDecrypt(key, aead);
		AeadEncrypt enc = new AeadEncrypt(key, aead);

		byte[] tag = bytes("5bc94fbc3221a5db94fae95ae7121a47");
		byte[] plaintext = bytes("d9313225f88406e5a55909c5aff5269a86a7a9531534f7da2e4c303d8a318a721c3c0c95956809532fcf0e2449a6b525b16aedf5aa0de657ba637b39");
		byte[] nonce = bytes("cafebabefacedbaddecaf888");
		byte[] additionalData = bytes("feedfacedeadbeeffeedfacedeadbeefabaddad2");
		byte[] encrypted = enc.encrypt(nonce, additionalData, plaintext);
		byte[] expected = cat(
				bytes("42831ec2217774244b7221b784d0d49ce3aa212f2c02a4e035c17e2329aca12e21d514b25466931c7d8f6a5aac84aa051ba30b396a0aac973d58e091"),
				tag);
		assertArrayEquals(expected, encrypted);

		enc = new AeadEncrypt(key, aead);
		enc.encrypt(nonce, additionalData, ByteBuffer.wrap(plaintext), buffer);
		assertArrayEquals(expected, buffer());
		buffer.clear();
		
		enc = new AeadEncrypt(key, aead);
		enc.encrypt(nonce, additionalData, array(plaintext,0,4), buffer);
		assertArrayEquals(expected, buffer());
		
		byte[] decrypted = dec.decrypt(nonce, additionalData, encrypted);
		assertArrayEquals(plaintext, decrypted);
		
		for (int i=0; i<10; ++i) {
			nonce[0] += 1;
			encrypted = enc.encrypt(nonce, additionalData, plaintext);
			decrypted = dec.decrypt(nonce, additionalData, encrypted);
			assertArrayEquals(plaintext, decrypted);
		}

		assertTrue(aead.isImplemented());
		assertEquals(16, tag.length);
		assertEquals(16, aead.getTagLength());
		assertEquals(16, key.getEncoded().length);
		assertEquals(16, aead.getKeyLength());
		assertEquals(12, nonce.length);
		assertEquals(12, aead.getIvLength());
	}

	@Test
	public void testAeadAes256Gcm() throws Exception {
		AESAead aead = AESAead.AEAD_AES_256_GCM;
		SecretKey key = aead.createKey(bytes("feffe9928665731c6d6a8f9467308308feffe9928665731c6d6a8f9467308308"));
		
		AeadDecrypt dec = new AeadDecrypt(key, aead);
		AeadEncrypt enc = new AeadEncrypt(key, aead);

		byte[] tag = bytes("76fc6ece0f4e1768cddf8853bb2d551b");
		byte[] plaintext = bytes("d9313225f88406e5a55909c5aff5269a86a7a9531534f7da2e4c303d8a318a721c3c0c95956809532fcf0e2449a6b525b16aedf5aa0de657ba637b39");
		byte[] nonce = bytes("cafebabefacedbaddecaf888");
		byte[] additionalData = bytes("feedfacedeadbeeffeedfacedeadbeefabaddad2");
		byte[] encrypted = enc.encrypt(nonce, additionalData, plaintext);
		byte[] expected = cat(
				bytes("522dc1f099567d07f47f37a32a84427d643a8cdcbfe5c0c97598a2bd2555d1aa8cb08e48590dbb3da7b08b1056828838c5f61e6393ba7a0abcc9f662"),
				tag);
		assertArrayEquals(expected, encrypted);

		byte[] decrypted = dec.decrypt(nonce, additionalData, encrypted);
		assertArrayEquals(plaintext, decrypted);
		
		for (int i=0; i<10; ++i) {
			nonce[0] += 1;
			encrypted = enc.encrypt(nonce, additionalData, plaintext);
			decrypted = dec.decrypt(nonce, additionalData, encrypted);
			assertArrayEquals(plaintext, decrypted);
		}
		
		assertTrue(aead.isImplemented());
		assertEquals(16, tag.length);
		assertEquals(16, aead.getTagLength());
		assertEquals(32, key.getEncoded().length);
		assertEquals(32, aead.getKeyLength());
		assertEquals(12, nonce.length);
		assertEquals(12, aead.getIvLength());
	}
	
}
