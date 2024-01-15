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
package org.snf4j.tls.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.security.cert.Certificate;

import org.junit.Test;
import org.snf4j.tls.cipher.CipherSuite;

public class SessionInfoTest {

	@Test
	public void testDefaults() {
		SessionInfo info = new SessionInfo();
		
		assertNull(info.peerHost());
		assertEquals(-1, info.peerPort());
		assertNull(info.cipher());
		assertNull(info.localCerts());
		assertNull(info.peerCerts());
	}
	
	@Test
	public void testUpdates() {
		Certificate[] certs1 = new Certificate[0];
		Certificate[] certs2 = new Certificate[0];
		SessionInfo info = new SessionInfo()
				.peerHost("xxx")
				.peerPort(100)
				.cipher(CipherSuite.TLS_CHACHA20_POLY1305_SHA256)
				.peerCerts(certs1)
				.localCerts(certs2);
		
		assertEquals("xxx", info.peerHost());
		assertEquals(100, info.peerPort());
		assertSame(CipherSuite.TLS_CHACHA20_POLY1305_SHA256, info.cipher());
		assertSame(certs1, info.peerCerts());
		assertSame(certs2, info.localCerts());
	}
}
