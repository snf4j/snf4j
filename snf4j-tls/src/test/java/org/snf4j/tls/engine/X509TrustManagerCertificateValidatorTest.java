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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.security.KeyStore;
import java.security.cert.CRLReason;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.CertificateRevokedException;
import java.security.cert.Extension;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

import org.junit.Test;
import org.snf4j.tls.CommonTest;
import org.snf4j.tls.alert.AlertDescription;

public class X509TrustManagerCertificateValidatorTest extends CommonTest {

	final static char[] PASSWORD = "password".toCharArray();
	
	KeyStore ks;
	
	@Override
	public void before() throws Exception {
		super.before();
		ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null, null);
	}

	void add(String alias, X509Certificate cert) throws Exception {
		ks.setCertificateEntry(alias, cert);
	}

	void del(String alias) throws Exception {
		ks.deleteEntry(alias);
	}
	
	X509Certificate[] certs(X509Certificate... certs) {
		return certs;
	}
	
	X509TrustManager trustManager() throws Exception {
        TrustManagerFactory kmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

        kmf.init(ks);
        return (X509TrustManager) kmf.getTrustManagers()[0];
	}

	@Test
	public void testValidateCertificatesSelfSigned() throws Exception {
		X509Certificate cert1 = cert("rsasha256");
		X509Certificate cert2 = cert("rsasha384");
		X509Certificate cert3 = cert("rsapsssha256");
		
		add("cert1", cert1);
		TestTrustMannager tm = new TestTrustMannager(trustManager());
		X509TrustManagerCertificateValidator v = new X509TrustManagerCertificateValidator(tm);
		CertificateValidateCriteria c = new CertificateValidateCriteria(true, "host");
		assertNull(v.validateCertificates(c, certs(cert1)));
		assertEquals("C|", tm.trace());
		assertNull(v.validateCertificates(c, certs(cert2)));
		assertSame(AlertDescription.BAD_CERTIFICATE, v.validateCertificates(c, certs(cert3)).getDescription());
		
		add("cert3", cert3);
		v = new X509TrustManagerCertificateValidator(trustManager());
		assertNull(v.validateCertificates(c, certs(cert1)));
		assertNull(v.validateCertificates(c, certs(cert2)));
		assertNull(v.validateCertificates(c, certs(cert3)));
	}
	
	@Test
	public void testValidateCertificatesCAs() throws Exception {
		X509Certificate ca = cert("rsasha256");
		X509Certificate sub1 = cert("rsasha256_sub1");
		X509Certificate sub2 = cert("rsasha256_sub2");
		
		add("ca", ca);
		TestTrustMannager tm = new TestTrustMannager(trustManager());
		X509TrustManagerCertificateValidator v = new X509TrustManagerCertificateValidator(tm);
		CertificateValidateCriteria c = new CertificateValidateCriteria(false, "host");
		assertSame(AlertDescription.BAD_CERTIFICATE, v.validateCertificates(c, certs(sub2)).getDescription());
		assertEquals("S|", tm.trace());
		assertNull(v.validateCertificates(c, certs(sub2,sub1,ca)));
		assertNull(v.validateCertificates(c, certs(sub2,sub1)));
		assertSame(AlertDescription.BAD_CERTIFICATE, v.validateCertificates(c, certs(sub2,ca)).getDescription());
		
		add("sub1", sub1);
		v = new X509TrustManagerCertificateValidator(trustManager());
		assertNull(v.validateCertificates(c, certs(sub2,sub1,ca)));
		assertNull(v.validateCertificates(c, certs(sub2,sub1)));
		assertNull(v.validateCertificates(c, certs(sub2)));
	}
	
	@Test
	public void testValidateCertificatesResults() throws Exception {
		X509Certificate cert = cert("rsasha256");

		add("cert", cert);
		TestTrustMannager tm = new TestTrustMannager(trustManager());
		X509TrustManagerCertificateValidator v = new X509TrustManagerCertificateValidator(tm);
		CertificateValidateCriteria c = new CertificateValidateCriteria(false, "host");
		tm.exception = new CertificateEncodingException();
		assertSame(AlertDescription.BAD_CERTIFICATE, v.validateCertificates(c, certs(cert)).getDescription());
		tm.exception = new CertificateExpiredException();
		assertSame(AlertDescription.CERTIFICATE_EXPIRED, v.validateCertificates(c, certs(cert)).getDescription());
		tm.exception = new CertificateNotYetValidException();
		assertSame(AlertDescription.CERTIFICATE_EXPIRED, v.validateCertificates(c, certs(cert)).getDescription());
		tm.exception = new CertificateParsingException();
		assertSame(AlertDescription.BAD_CERTIFICATE, v.validateCertificates(c, certs(cert)).getDescription());
		tm.exception = new CertificateRevokedException(
				new Date(), 
				CRLReason.CERTIFICATE_HOLD, 
				new X500Principal("CN=SNF4J, C=PL"), 
				new HashMap<String, Extension>());
		assertSame(AlertDescription.CERTIFICATE_REVOKED, v.validateCertificates(c, certs(cert)).getDescription());
	}

	@Test
	public void testValidateRawKeyResults() throws Exception {
		X509Certificate cert = cert("rsasha256");

		add("cert", cert);
		TestTrustMannager tm = new TestTrustMannager(trustManager());
		X509TrustManagerCertificateValidator v = new X509TrustManagerCertificateValidator(tm);
		CertificateValidateCriteria c = new CertificateValidateCriteria(false, "host");
		assertSame(AlertDescription.UNSUPPORTED_CERTIFICATE, v.validateRawKey(c, cert.getPublicKey()).getDescription());
	}
	
	class TestTrustMannager implements X509TrustManager {

		X509TrustManager mgr;
		
		CertificateException exception;
		
		StringBuilder trace = new StringBuilder();
		
		void trace(String s) {
			trace.append(s).append('|');
		}
		
		String trace() {
			String s = trace.toString();
			
			trace.setLength(0);
			return s;
		}
		
		TestTrustMannager(X509TrustManager mgr) {
			this.mgr = mgr;
		}
		
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			if (exception != null) {
				throw exception;
			}
			trace("C");
			mgr.checkClientTrusted(chain, authType);
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			if (exception != null) {
				throw exception;
			}
			trace("S");
			mgr.checkServerTrusted(chain, authType);
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return mgr.getAcceptedIssuers();
		}
		
	}
	
}
