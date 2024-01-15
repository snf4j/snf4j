/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.record;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import javax.security.auth.DestroyFailedException;

import org.junit.Test;
import org.snf4j.tls.CommonTest;
import org.snf4j.tls.crypto.AESAead;
import org.snf4j.tls.crypto.AeadEncrypt;

public class EncryptorTest extends CommonTest {

	final byte[] IV = bytes(1,2,3,4,5,6,7,8,9,10,11,12);
	
	@Test
	public void testAll() throws Exception {
		AESAead aead = AESAead.AEAD_AES_256_GCM;
		Encryptor d = new Encryptor(new AeadEncrypt(aead.createKey(new byte[32]),aead), IV);
		
		assertEquals(IV.length, aead.getIvLength());
		assertSame(aead, d.getAead().getAead());
		assertEquals(16, d.getExpansion());
		assertArrayEquals(IV, d.nextNonce());
	}
	
	@Test
	public void testErase() throws Exception {
		AESAead aead = AESAead.AEAD_AES_256_GCM;
		byte[] iv = IV.clone();
		TestSecretKey key = new TestSecretKey();
		Encryptor e = new Encryptor(new AeadEncrypt(key, aead), iv);
		
		e.erase();
		assertArrayEquals(bytes(0,0,0,0,0,0,0,0,0,0,0,0), iv);
		assertEquals(1, key.destroyCount);
		assertEquals(0, key.destroyExceptionCount);

		iv = IV.clone();
		key.destroyException = new DestroyFailedException();
		e = new Encryptor(new AeadEncrypt(key, aead), iv);

		e.erase();
		assertArrayEquals(bytes(0,0,0,0,0,0,0,0,0,0,0,0), iv);
		assertEquals(2, key.destroyCount);
		assertEquals(1, key.destroyExceptionCount);
	}

	@Test
	public void testKeyLimit() throws Exception {
		AESAead aead = AESAead.AEAD_AES_256_GCM;
		Encryptor d = new Encryptor(new AeadEncrypt(new TestSecretKey(), aead), IV);
		
		assertFalse(d.isKeyLimitReached());
		long keyLimit = 137438953472L;	
		
		while(keyLimit > 0) {
			long chunk = Math.min((long)Integer.MAX_VALUE, keyLimit);
			
			keyLimit -= chunk;
			d.incProcessedBytes((int) chunk);
			assertFalse(d.isKeyLimitReached());
		}
		d.incProcessedBytes(1);
		assertTrue(d.isKeyLimitReached());
		
		d = new Encryptor(new AeadEncrypt(new TestSecretKey(), aead), IV, 1000);
		assertFalse(d.isKeyLimitReached());
		d.incProcessedBytes(1000);
		assertFalse(d.isKeyLimitReached());
		d.incProcessedBytes(1);
		assertTrue(d.isKeyLimitReached());
	}

}
