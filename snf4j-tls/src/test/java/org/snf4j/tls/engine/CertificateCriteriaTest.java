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
package org.snf4j.tls.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.security.cert.X509Certificate;

import org.junit.Assume;
import org.junit.Test;
import org.snf4j.tls.crypto.SignatureTest;
import org.snf4j.tls.extension.SignatureScheme;
import org.snf4j.tls.handshake.CertificateType;

public class CertificateCriteriaTest extends SignatureTest {

	SignatureScheme[] schemes(SignatureScheme... schemes) {
		return schemes;
	}
	
	@Test
	public void testConstructor() {
		SignatureScheme[] s1 = new SignatureScheme[] {SignatureScheme.ECDSA_SECP256R1_SHA256, SignatureScheme.RSA_PKCS1_SHA256};
		SignatureScheme[] s2 = new SignatureScheme[] {SignatureScheme.RSA_PKCS1_SHA256};
		SignatureScheme[] s3 = new SignatureScheme[] {SignatureScheme.RSA_PKCS1_SHA512};
		
		CertificateCriteria c = new CertificateCriteria(true, CertificateType.RAW_PUBLIC_KEY, "host", s1, s2, s3);
		assertTrue(c.isServer());
		assertSame(CertificateType.RAW_PUBLIC_KEY, c.getType());
		assertEquals("host", c.getHostName());
		assertSame(s1, c.getSchemes());
		assertSame(s2, c.getCertSchemes());
		assertSame(s3, c.getLocalSchemes());

		c = new CertificateCriteria(false, CertificateType.X509, null, s1, null, s3);
		assertFalse(c.isServer());
		assertSame(CertificateType.X509, c.getType());
		assertNull(c.getHostName());
		assertSame(s1, c.getSchemes());
		assertNull(c.getCertSchemes());
		assertSame(s3, c.getLocalSchemes());
	}
	
	@Test
	public void testMatch() throws Exception {
		SignatureScheme[] s1;

		s1 = schemes(
				SignatureScheme.ECDSA_SECP384R1_SHA384,
				SignatureScheme.ECDSA_SECP256R1_SHA256, 
				SignatureScheme.RSA_PKCS1_SHA256, 
				SignatureScheme.RSA_PKCS1_SHA1);
		assertSame(SignatureScheme.RSA_PKCS1_SHA1, CertificateCriteria.match(cert("rsasha1"), s1, null));
		assertSame(SignatureScheme.RSA_PKCS1_SHA256, CertificateCriteria.matchByKey(cert("rsasha1"), s1, null));
		assertSame(SignatureScheme.RSA_PKCS1_SHA256, CertificateCriteria.match(cert("rsasha256"), s1, null));
		assertSame(SignatureScheme.RSA_PKCS1_SHA256, CertificateCriteria.matchByKey(cert("rsasha256"), s1, null));
		
		assertSame(SignatureScheme.RSA_PKCS1_SHA1, CertificateCriteria.match(cert("rsasha1"), s1, schemes(SignatureScheme.RSA_PKCS1_SHA1)));
		assertNull(CertificateCriteria.match(cert("rsasha1"), s1, schemes()));
		assertNull(CertificateCriteria.match(cert("rsasha1"), s1, schemes(SignatureScheme.ECDSA_SECP521R1_SHA512)));
		
		assertSame(SignatureScheme.RSA_PKCS1_SHA1, CertificateCriteria.matchByKey(cert("rsasha1"), s1, schemes(SignatureScheme.RSA_PKCS1_SHA1)));
		assertSame(SignatureScheme.RSA_PKCS1_SHA256, CertificateCriteria.matchByKey(cert("rsasha1"), s1, schemes(SignatureScheme.RSA_PKCS1_SHA1,SignatureScheme.RSA_PKCS1_SHA256)));
		assertNull(CertificateCriteria.matchByKey(cert("rsasha1"), s1, schemes()));
		assertNull(CertificateCriteria.matchByKey(cert("rsasha1"), s1, schemes(SignatureScheme.ECDSA_SECP521R1_SHA512)));

		assertSame(SignatureScheme.ECDSA_SECP256R1_SHA256, CertificateCriteria.matchByKey(cert("secp256r1"), s1, null));
		
		CertificateCriteria c = new CertificateCriteria(true, CertificateType.X509, "host", schemes(
					SignatureScheme.RSA_PKCS1_SHA384,
					SignatureScheme.RSA_PKCS1_SHA256
				), 
				null, schemes(
					SignatureScheme.RSA_PKCS1_SHA256,
					SignatureScheme.RSA_PKCS1_SHA384 
				));
		assertSame(SignatureScheme.RSA_PKCS1_SHA256, c.match(cert("rsasha256")));
		assertSame(SignatureScheme.RSA_PKCS1_SHA384, c.match(cert("rsasha384")));
		assertSame(SignatureScheme.RSA_PKCS1_SHA256, c.matchByKey(cert("rsasha256")));
		assertSame(SignatureScheme.RSA_PKCS1_SHA256, c.matchByKey(cert("rsasha384")));
		
	}

	@Test
	public void testAllMatch() throws Exception {
		X509Certificate[] certs = new X509Certificate[] {cert("rsasha1"), cert("rsasha256")};
		
		assertTrue(CertificateCriteria.allMatch(certs, 0, certs.length, schemes(SignatureScheme.RSA_PKCS1_SHA256, SignatureScheme.RSA_PKCS1_SHA1)));
		assertFalse(CertificateCriteria.allMatch(certs, 0, certs.length, schemes(SignatureScheme.RSA_PKCS1_SHA256)));
		assertFalse(CertificateCriteria.allMatch(certs, 0, certs.length, schemes(SignatureScheme.RSA_PKCS1_SHA1)));
		assertTrue(CertificateCriteria.allMatch(certs, 1, 1, schemes(SignatureScheme.RSA_PKCS1_SHA256)));
		assertTrue(CertificateCriteria.allMatch(certs, 0, 1, schemes(SignatureScheme.RSA_PKCS1_SHA1)));
		
		CertificateCriteria c = new CertificateCriteria(true, CertificateType.X509, "host", schemes(
				SignatureScheme.RSA_PKCS1_SHA1,
				SignatureScheme.RSA_PKCS1_SHA256
			), 
			null, schemes());
		assertTrue(c.allMatch(certs, 0, certs.length));
		assertTrue(c.allMatch(certs));
		
		c = new CertificateCriteria(true, CertificateType.X509, "host", schemes(
				SignatureScheme.RSA_PKCS1_SHA1
			), 
			null, schemes());
		assertFalse(c.allMatch(certs, 0, certs.length));
		assertFalse(c.allMatch(certs));
		assertTrue(c.allMatch(certs, 0, 1));
		assertFalse(c.allMatch(certs, 1, 1));
		
		c = new CertificateCriteria(true, CertificateType.X509, "host", schemes(
				SignatureScheme.RSA_PKCS1_SHA1
			), 
			schemes(
				SignatureScheme.RSA_PKCS1_SHA256
			), schemes());
		assertTrue(c.allMatch(certs, 1, 1));
		
	}
	
	@Test
	public void testMatchWithPss() throws Exception {
		Assume.assumeTrue(JAVA11);

		SignatureScheme[] s1;

		s1 = schemes(
				SignatureScheme.RSA_PSS_PSS_SHA512, 
				SignatureScheme.RSA_PSS_RSAE_SHA512, 
				SignatureScheme.RSA_PSS_PSS_SHA256, 
				SignatureScheme.RSA_PSS_RSAE_SHA384, 
				SignatureScheme.ECDSA_SECP256R1_SHA256, 
				SignatureScheme.RSA_PKCS1_SHA256, 
				SignatureScheme.RSA_PKCS1_SHA1);
		assertSame(SignatureScheme.RSA_PKCS1_SHA1, CertificateCriteria.match(cert("rsasha1"), s1, null));
		assertSame(SignatureScheme.RSA_PSS_RSAE_SHA384, CertificateCriteria.matchByKey(cert("rsasha1"), s1, null));
		assertSame(SignatureScheme.RSA_PKCS1_SHA256, CertificateCriteria.matchByKey(cert("rsasha1"), s1, schemes(SignatureScheme.RSA_PKCS1_SHA256)));
		assertSame(SignatureScheme.RSA_PSS_RSAE_SHA512, CertificateCriteria.matchByKey(cert("rsapsssha256"), s1, null));
		assertSame(SignatureScheme.RSA_PSS_PSS_SHA256, CertificateCriteria.matchByKey(cert("rsapsspsssha256"), s1, null));
	}

	@Test
	public void testMatchWithEd() throws Exception {
		Assume.assumeTrue(JAVA15);

		SignatureScheme[] s1;

		s1 = schemes(
				SignatureScheme.RSA_PSS_PSS_SHA512,
				SignatureScheme.ED448,
				SignatureScheme.ED25519,
				SignatureScheme.RSA_PSS_RSAE_SHA384, 
				SignatureScheme.ECDSA_SECP256R1_SHA256, 
				SignatureScheme.RSA_PKCS1_SHA256, 
				SignatureScheme.RSA_PKCS1_SHA1);
		assertSame(SignatureScheme.ED25519, CertificateCriteria.match(cert("ed25519"), s1, null));
		assertSame(SignatureScheme.ED25519, CertificateCriteria.matchByKey(cert("ed25519"), s1, null));
		assertSame(SignatureScheme.ED448, CertificateCriteria.matchByKey(cert("ed448"), s1, null));
	}
	
}
