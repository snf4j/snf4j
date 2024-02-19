/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023-2024 SNF4J contributors
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
package org.snf4j.tls.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.security.cert.X509Certificate;

import org.junit.Test;
import org.snf4j.tls.crypto.SignatureTest;
import org.snf4j.tls.extension.SignatureScheme;

public class CertificateValidateCriteriaTest extends SignatureTest {

	SignatureScheme[] schemes(SignatureScheme... schemes) {
		return schemes;
	}
	
	X509Certificate[] certs(String... names) throws Exception {
		X509Certificate[] certs = new X509Certificate[names.length];
		
		for (int i=0; i<names.length; ++i) {
			certs[i] = cert(names[i]);
		}
		return certs;
	}
	
	@Test
	public void testConstructor() {
		SignatureScheme[] schemes = new SignatureScheme[] {SignatureScheme.ECDSA_SECP256R1_SHA256};
		CertificateValidateCriteria c = new CertificateValidateCriteria(true, "host", schemes);
		assertTrue(c.isServer());
		assertEquals("host", c.getHostName());
		assertSame(schemes, c.getLocalSchemes());

		c = new CertificateValidateCriteria(false, null, schemes);
		assertFalse(c.isServer());
		assertNull(c.getHostName());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void textIllegalArgument() {
		new CertificateValidateCriteria(true, "host", null);
	}
	
	@Test
	public void testMatch() throws Exception {
		CertificateValidateCriteria c = new CertificateValidateCriteria(true, "host", schemes(
				SignatureScheme.RSA_PKCS1_SHA1, 
				SignatureScheme.RSA_PKCS1_SHA256));
		
		assertTrue(c.allMatch(certs("rsasha1")));
		assertTrue(c.allMatch(certs("rsasha256")));
		assertTrue(c.allMatch(certs("rsasha1","rsasha256")));
		assertFalse(c.allMatch(certs("rsasha1","rsasha384")));
		assertFalse(c.allMatch(certs("rsasha1","rsasha256","rsasha384")));
		assertTrue(c.allMatch(certs("rsasha1","rsasha256","rsasha384"), 0, 2));
		assertTrue(c.allMatch(certs("rsasha512", "rsasha1","rsasha256","rsasha384"), 1, 2));
	}
}
