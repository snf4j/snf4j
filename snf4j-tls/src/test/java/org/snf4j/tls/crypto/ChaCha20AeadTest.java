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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;
import org.snf4j.tls.CommonTest;

public class ChaCha20AeadTest extends CommonTest {

	@Test
	public void testAEAD_CHACHA20_POLY1305() throws Exception {
		ChaCha20Aead aead = ChaCha20Aead.AEAD_CHACHA20_POLY1305;
		SecretKey key = aead.createKey(bytes("1c9240a5eb55d38af333888604f6b5f0473917c1402b80099dca5cbc207075c0"));

		byte[] tag = bytes("eead9d67890cbb22392336fea1851f38");
		byte[] nonce = bytes("000000000102030405060708");
		
		if (JAVA11) {
			AeadDecrypt dec = new AeadDecrypt(key, aead);
			AeadEncrypt enc = new AeadEncrypt(key, aead);

			byte[] plaintext = bytes("496e7465726e65742d4472616674732061726520647261667420646f63756d656e74732076616c69"+
					"6420666f722061206d6178696d756d206f6620736978206d6f6e74687320616e64206d617920626520757064617465642"+
					"c207265706c616365642c206f72206f62736f6c65746564206279206f7468657220646f63756d656e747320617420616e"+
					"792074696d652e20497420697320696e617070726f70726961746520746f2075736520496e7465726e65742d447261667"+
					"473206173207265666572656e6365206d6174657269616c206f7220746f2063697465207468656d206f74686572207468"+
					"616e206173202fe2809c776f726b20696e2070726f67726573732e2fe2809d");
			byte[] additionalData = bytes("f33388860000000000004e91");
			byte[] encrypted = enc.encrypt(nonce, additionalData, plaintext);
			byte[] expected = cat(
					bytes("64a0861575861af460f062c79be643bd5e805cfd345cf389f108670ac76c8cb24c6cfc18755d43eea09ee94e382"+
							"d26b0bdb7b73c321b0100d4f03b7f355894cf332f830e710b97ce98c8a84abd0b948114ad176e008d33bd60f9"+
							"82b1ff37c8559797a06ef4f0ef61c186324e2b3506383606907b6a7c02b0f9f6157b53c867e4b9166c767b804"+
							"d46a59b5216cde7a4e99040c5a40433225ee282a1b0a06c523eaf4534d7f83fa1155b0047718cbc546a0d072b"+
							"04b3564eea1b422273f548271a0bb2316053fa76991955ebd63159434ecebb4e466dae5a1073a6727627097a1"+
							"049e617d91d361094fa68f0ff77987130305beaba2eda04df997b714d6c6f2c29a6ad5cb4022b02709b"),
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
		}

		assertEquals(JAVA11, aead.isImplemented());
		assertEquals(16, tag.length);
		assertEquals(16, aead.getTagLength());
		assertEquals(32, key.getEncoded().length);
		assertEquals(32, aead.getKeyLength());
		assertEquals(12, nonce.length);
		assertEquals(12, aead.getIvLength());
		assertEquals(1L<<62, aead.getKeyLimit());
	}
	
	@Test
	public void testImplemented() {
		assertTrue(ChaCha20Aead.implemented("AES/GCM/NoPadding"));
		assertFalse(ChaCha20Aead.implemented("AES/GCMXXX/NoPadding"));
	}
	
	@Test
	public void testInit() throws Exception {
		ChaCha20Aead aead = new ChaCha20Aead(16,16,100000,12) {
			
			@Override
			Cipher createCipher(String transformation) throws NoSuchAlgorithmException, NoSuchPaddingException {
				return Cipher.getInstance("AES/CBC/NoPadding");
			}
		};

		Cipher enc = ChaCha20Aead.AEAD_CHACHA20_POLY1305.createCipher("AES/CBC/NoPadding");
		Cipher dec = aead.createCipher();
		SecretKey key = new SecretKeySpec(random(16), "AES");
		byte[] iv = random(16);
		byte[] plaintext = random(32);
		
		ChaCha20Aead.AEAD_CHACHA20_POLY1305.initDecrypt(dec, key, iv);
		ChaCha20Aead.AEAD_CHACHA20_POLY1305.initEncrypt(enc, key, iv);
		
		byte[] encrypted = enc.doFinal(plaintext);
		byte[] decrypted = dec.doFinal(encrypted);
		assertArrayEquals(plaintext, decrypted);
	}
	
	@Test
	public void testCreateCipher() {
	}
}
