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
package org.snf4j.tls.cipher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.snf4j.tls.CommonTest;
import org.snf4j.tls.crypto.AESAead;
import org.snf4j.tls.crypto.ChaCha20Aead;

public class CipherSuiteSpecTest extends CommonTest {

	@Test
	public void testTlsAes128GcmSha256() {
		CipherSuiteSpec i = CipherSuiteSpec.TLS_AES_128_GCM_SHA256;
		
		assertSame(AESAead.AEAD_AES_128_GCM, i.getAead());
		assertSame(HashSpec.SHA256, i.getHashSpec());
		assertTrue(i.isImplemented());
	}

	@Test
	public void testTlsAes256GcmSha384() {
		CipherSuiteSpec i = CipherSuiteSpec.TLS_AES_256_GCM_SHA384;
		
		assertSame(AESAead.AEAD_AES_256_GCM, i.getAead());
		assertSame(HashSpec.SHA384, i.getHashSpec());
		assertTrue(i.isImplemented());
	}

	@Test
	public void testTlsChaCha20Poly1305Sha256() {
		CipherSuiteSpec i = CipherSuiteSpec.TLS_CHACHA20_POLY1305_SHA256;
		
		assertSame(ChaCha20Aead.AEAD_CHACHA20_POLY1305, i.getAead());
		assertSame(HashSpec.SHA256, i.getHashSpec());
		assertEquals(JAVA11, i.isImplemented());
	}
}
