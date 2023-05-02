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
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.snf4j.tls.CommonTest;

public class CryptorTest extends CommonTest {

	final byte[] IV = bytes(1,2,3,4,5,6,7,8,9,10,11,12);
	
	byte[] nonce(long sequence) {
		byte[] nonce = new byte[IV.length];
		ByteBuffer buf = ByteBuffer.wrap(nonce);
		
		buf.position(IV.length-8);
		buf.putLong(sequence);
		for (int i=0; i<nonce.length; i++) {
			nonce[i] = (byte) (nonce[i] ^ IV[i]);
		}
		return nonce;
	}
	
	@Test
	public void testNextNonce() {
		Cryptor c = new Cryptor(IV.clone(), 16, 1000) {};
		
		assertEquals(0, c.getSequence());
		byte[] nonce = c.nextNonce();
		assertEquals(1, c.getSequence());
		c.rollbackSequence();
		assertEquals(0, c.getSequence());
		assertArrayEquals(nonce, c.nextNonce());
		assertEquals(1, c.getSequence());
		assertArrayEquals(IV, nonce);
		assertArrayEquals(nonce(1), c.nextNonce());
		c.rollbackSequence();
		c.rollbackSequence();
		c.rollbackSequence();
		assertArrayEquals(nonce(-1), c.nextNonce());
		
		for (int i=0; i<10000; ++i) {
			assertArrayEquals(nonce(i), c.nextNonce());
		}
	}
	
	@Test
	public void testGetExapnsion() {
		Cryptor c = new Cryptor(IV.clone(), 16, 1000) {};
		
		assertEquals(16, c.getExpansion());
		c = new Cryptor(IV.clone(), 17, 1000) {};
		assertEquals(17, c.getExpansion());
	}
	
	@Test
	public void testIsMarkedForUpdate() {
		Cryptor c = new Cryptor(IV.clone(), 16, 1000) {};
		
		assertFalse(c.isMarkedForUpdate());
		c.markForUpdate();
		assertTrue(c.isMarkedForUpdate());
	}
	
	@Test
	public void testIsKeyLimitReached() {
		Cryptor c = new Cryptor(IV.clone(), 16, 1000) {};

		assertFalse(c.isKeyLimitReached());
		c.incProcessedBytes(500);
		assertFalse(c.isKeyLimitReached());
		c.incProcessedBytes(500);
		assertFalse(c.isKeyLimitReached());
		c.incProcessedBytes(1);
		assertTrue(c.isKeyLimitReached());
		c.incProcessedBytes(1);
		assertTrue(c.isKeyLimitReached());
	}
}
