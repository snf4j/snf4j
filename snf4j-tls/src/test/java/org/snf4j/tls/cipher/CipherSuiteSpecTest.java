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

public class CipherSuiteSpecTest {

	@Test
	public void testTlsAes128GcmSha256() {
		CipherSuiteSpec i = CipherSuiteSpec.TLS_AES_128_GCM_SHA256;
		
		assertEquals(16, i.getAuthenticationTagLength());
		assertEquals("AES", i.getKeyAlgorithm());
		assertEquals(16, i.getKeyLength());
		assertEquals(12, i.getIvLength());
		assertSame(HashSpec.SHA256, i.getHashSpec());
		assertTrue(i.isImplemented());
	}

	@Test
	public void testTlsAes256GcmSha384() {
		CipherSuiteSpec i = CipherSuiteSpec.TLS_AES_256_GCM_SHA384;
		
		assertEquals(16, i.getAuthenticationTagLength());
		assertEquals("AES", i.getKeyAlgorithm());
		assertEquals(32, i.getKeyLength());
		assertEquals(12, i.getIvLength());
		assertSame(HashSpec.SHA384, i.getHashSpec());
		assertTrue(i.isImplemented());
	}
}
