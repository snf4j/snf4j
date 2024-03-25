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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.snf4j.tls.crypto.AeadId;

public class AeadSpecTest {
	
	@Test
	public void testGetConfidentialityLimit() {
		assertEquals((long)Math.pow(2, 23), AeadSpec.getConfidentialityLimit(AeadId.AES_128_GCM));
		assertEquals((long)Math.pow(2, 23), AeadSpec.getConfidentialityLimit(AeadId.AES_256_GCM));
		assertTrue(AeadSpec.getConfidentialityLimit(AeadId.CHACHA20_POLY1305) > (long)Math.pow(2, 62));
	}

	@Test
	public void testGetIntegrityLimitLimit() {
		assertEquals((long)Math.pow(2, 52), AeadSpec.getIntegrityLimit(AeadId.AES_128_GCM));
		assertEquals((long)Math.pow(2, 52), AeadSpec.getIntegrityLimit(AeadId.AES_256_GCM));
		assertEquals((long)Math.pow(2, 36), AeadSpec.getIntegrityLimit(AeadId.CHACHA20_POLY1305));
	}
	
	@Test
	public void testGetHeaderProtection() {
		assertSame(AESHeaderProtection.HP_AES_128, AeadSpec.getHeaderProtection(AeadId.AES_128_GCM));
		assertSame(AESHeaderProtection.HP_AES_256, AeadSpec.getHeaderProtection(AeadId.AES_256_GCM));
		assertSame(ChaCha20HeaderProtection.HP_CHACHA20, AeadSpec.getHeaderProtection(AeadId.CHACHA20_POLY1305));
	}
}
