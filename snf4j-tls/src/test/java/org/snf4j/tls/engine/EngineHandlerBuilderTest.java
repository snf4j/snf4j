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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.junit.Test;
import org.snf4j.tls.CommonTest;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.extension.ALPNExtension;
import org.snf4j.tls.extension.ServerNameExtension;
import org.snf4j.tls.extension.SignatureScheme;
import org.snf4j.tls.handshake.CertificateType;
import org.snf4j.tls.record.ContentType;
import org.snf4j.tls.session.ISessionManager;
import org.snf4j.tls.session.SessionManager;

public class EngineHandlerBuilderTest extends CommonTest {
	
	final static char[] PASSWORD = "password".toCharArray();
	
	KeyManagerFactory kmf;
	
	X509KeyManager km;

	TrustManagerFactory tmf;
	
	X509TrustManager tm;
	
	SecureRandom random = new SecureRandom();
	
	ISessionManager sm = new SessionManager();

	SignatureScheme[] schemes = new SignatureScheme[] {SignatureScheme.ECDSA_SECP256R1_SHA256};
	
	X509Certificate[] certs;
	
	StringBuilder trace = new StringBuilder();
	
	@Override
	public void before() throws Exception {
		super.before();
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null, null);
		ks.setKeyEntry("key", key("EC", "secp256r1"), PASSWORD, new X509Certificate[] {cert("secp256r1")});
        kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks,PASSWORD);
        km =  (X509KeyManager) kmf.getKeyManagers()[0];
		
        ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null, null);
		ks.setCertificateEntry("ca", cert("secp256r1"));
        tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        tm = (X509TrustManager) tmf.getTrustManagers()[0];
        
        certs = new X509Certificate[] {cert("secp256r1")};
	}
	
	@Test
	public void testConstructors() throws Exception {
		EngineHandlerBuilder b = new EngineHandlerBuilder(km, "key", tm);
		EngineHandler h = b.build();
		CertificateCriteria c = new CertificateCriteria(true, CertificateType.X509, "host", schemes, null, schemes);
		CertificateValidateCriteria vc = new CertificateValidateCriteria(true, "host");
		SelectedCertificates sc = h.getCertificateSelector().selectCertificates(c);
		assertSame(SignatureScheme.ECDSA_SECP256R1_SHA256, sc.getAlgorithm());
		assertNull(h.getCertificateValidator().validateCertificates(vc, certs));
		
		b = new EngineHandlerBuilder(km, "key2", tm);
		h = b.build();
		try {
			h.getCertificateSelector().selectCertificates(c);
			fail();
		}
		catch(CertificateSelectorException e) {
			assertEquals("No certificate chain found for key2 alias", e.getMessage());
		}
		assertNull(h.getCertificateValidator().validateCertificates(vc, certs));
		
		b = new EngineHandlerBuilder(km, null, tm);
		h = b.build();
		sc = h.getCertificateSelector().selectCertificates(c);
		assertSame(SignatureScheme.ECDSA_SECP256R1_SHA256, sc.getAlgorithm());
		assertNull(h.getCertificateValidator().validateCertificates(vc, certs));

		b = new EngineHandlerBuilder(km, tm);
		h = b.build();
		sc = h.getCertificateSelector().selectCertificates(c);
		assertSame(SignatureScheme.ECDSA_SECP256R1_SHA256, sc.getAlgorithm());
		assertNull(h.getCertificateValidator().validateCertificates(vc, certs));
		
		b = new EngineHandlerBuilder(km, "key");
		h = b.build();
		sc = h.getCertificateSelector().selectCertificates(c);
		assertSame(SignatureScheme.ECDSA_SECP256R1_SHA256, sc.getAlgorithm());
		assertNotNull(h.getCertificateValidator().validateCertificates(vc, certs));
		assertNotNull(h.getCertificateValidator().validateRawKey(vc, certs[0].getPublicKey()));
		
		b = new EngineHandlerBuilder(km, "key2");
		h = b.build();
		try {
			h.getCertificateSelector().selectCertificates(c);
			fail();
		}
		catch(CertificateSelectorException e) {
			assertEquals("No certificate chain found for key2 alias", e.getMessage());
		}
		assertNotNull(h.getCertificateValidator().validateCertificates(vc, certs));
		assertNotNull(h.getCertificateValidator().validateRawKey(vc, certs[0].getPublicKey()));
		
		b = new EngineHandlerBuilder(km, (String)null);
		h = b.build();
		sc = h.getCertificateSelector().selectCertificates(c);
		assertSame(SignatureScheme.ECDSA_SECP256R1_SHA256, sc.getAlgorithm());
		assertNotNull(h.getCertificateValidator().validateCertificates(vc, certs));
		assertNotNull(h.getCertificateValidator().validateRawKey(vc, certs[0].getPublicKey()));

		b = new EngineHandlerBuilder(km);
		h = b.build();
		sc = h.getCertificateSelector().selectCertificates(c);
		assertSame(SignatureScheme.ECDSA_SECP256R1_SHA256, sc.getAlgorithm());
		assertNotNull(h.getCertificateValidator().validateCertificates(vc, certs));
		assertNotNull(h.getCertificateValidator().validateRawKey(vc, certs[0].getPublicKey()));
		
		b = new EngineHandlerBuilder(tm);
		h = b.build();
		try {
			h.getCertificateSelector().selectCertificates(c);
			fail();
		}
		catch(CertificateSelectorException e) {
			assertEquals("No certificate chain found", e.getMessage());
		}
		assertSame(SignatureScheme.ECDSA_SECP256R1_SHA256, sc.getAlgorithm());
		assertNull(h.getCertificateValidator().validateCertificates(vc, certs));
		
		X509KeyManagerCertificateSelector selector = new X509KeyManagerCertificateSelector(km, "key");
		X509TrustManagerCertificateValidator validator = new X509TrustManagerCertificateValidator(tm);
		b = new EngineHandlerBuilder(selector, validator);
		h = b.build();
		assertSame(SignatureScheme.ECDSA_SECP256R1_SHA256, sc.getAlgorithm());
		assertNull(h.getCertificateValidator().validateCertificates(vc, certs));
		
		b = new EngineHandlerBuilder(selector);
		h = b.build();
		assertSame(SignatureScheme.ECDSA_SECP256R1_SHA256, sc.getAlgorithm());
		assertNotNull(h.getCertificateValidator().validateCertificates(vc, certs));
		
		b = new EngineHandlerBuilder(validator);
		h = b.build();
		try {
			h.getCertificateSelector().selectCertificates(c);
			fail();
		}
		catch(CertificateSelectorException e) {
			assertEquals("No certificate chain found", e.getMessage());
		}
		assertNull(h.getCertificateValidator().validateCertificates(vc, certs));

	}
	
	@Test
	public void testHostNameVerifier() {
		EngineHandlerBuilder b = new EngineHandlerBuilder(km, "key", tm);
		EngineHandler h = b.build();
		
		assertNull(b.getHostNameVerifier());
		assertTrue(h.verifyServerName(new ServerNameExtension("xxx")));
		
		b.hostNameVerifier(new IHostNameVerifier() {

			@Override
			public boolean verifyHostName(String hostname) {
				return "yyy".equals(hostname);
			}
		});
		h = b.build();
		assertNotNull(b.getHostNameVerifier());
		assertFalse(h.verifyServerName(new ServerNameExtension("xxx")));
		assertTrue(h.verifyServerName(new ServerNameExtension("yyy")));
		
		b.hostNameVerifier(null);
		h = b.build();
		assertNull(b.getHostNameVerifier());
		assertTrue(h.verifyServerName(new ServerNameExtension("xxx")));
	}
	
	@Test
	public void testPadding() {
		EngineHandlerBuilder b = new EngineHandlerBuilder(km, "key", tm);
		EngineHandler h = b.build();
		
		assertEquals(4096, b.getPadding());
		assertEquals(4095, h.calculatePadding(ContentType.APPLICATION_DATA, 1));
		
		b.padding(16);
		h = b.build();
		assertEquals(16, b.getPadding());
		assertEquals(15, h.calculatePadding(ContentType.APPLICATION_DATA, 1));
		
		b.padding(1);
		h = b.build();
		assertEquals(1, b.getPadding());
		assertEquals(0, h.calculatePadding(ContentType.APPLICATION_DATA, 3));
		
		try {
			b.padding(0);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("padding is less than 1", e.getMessage());
		}			
	}
	
	void assertTickets(TicketInfo[] tickets, long... sizes) {
		assertEquals(sizes.length, tickets.length);
		for (int i=0; i<tickets.length; ++i) {
			assertEquals(sizes[i], tickets[i].getMaxEarlyDataSize());
		}
	}
	
	@Test
	public void testTicketInfos() {
		EngineHandlerBuilder b = new EngineHandlerBuilder(km, "key", tm);
		EngineHandler h = b.build();
		assertTickets(b.getTicketInfos(), -1);
		assertTickets(h.createNewTickets(), -1);
		assertNotSame(b.getTicketInfos(), h.createNewTickets());
		
		b.ticketInfos((long[])null);
		h = b.build();
		assertNull(b.getTicketInfos());
		assertTickets(h.createNewTickets());
		
		b.ticketInfos();
		h = b.build();
		assertTickets(b.getTicketInfos());
		assertTickets(h.createNewTickets());
		assertSame(b.getTicketInfos(), h.createNewTickets());
		
		b.ticketInfos(-1);
		h = b.build();
		assertTickets(b.getTicketInfos(), -1);
		assertTickets(h.createNewTickets(), -1);
		assertNotSame(b.getTicketInfos(), h.createNewTickets());
		
		b.ticketInfos(-1, 100, 1000, 0);
		h = b.build();
		assertTickets(b.getTicketInfos(), -1, 100, 1000, -1);
		assertTickets(h.createNewTickets(), -1, 100, 1000, -1);
		assertNotSame(b.getTicketInfos(), h.createNewTickets());
	}
	
	@Test
	public void testSessionManager() {
		EngineHandlerBuilder b = new EngineHandlerBuilder(km, "key", tm);
		EngineHandler h = b.build();

		ISessionManager mgr0 = h.getSessionManager();
		assertNull(b.getSessionManager());
		assertNotNull(h.getSessionManager());
		assertSame(h.getSessionManager(), b.build().getSessionManager());
		
		SessionManager mgr = new SessionManager();
		b.sessionManager(mgr);
		h = b.build();
		assertSame(mgr, b.getSessionManager());
		assertSame(mgr, h.getSessionManager());
		assertSame(mgr, b.build().getSessionManager());
		
		b.sessionManager(null);
		h = b.build();
		assertNull(b.getSessionManager());
		assertSame(mgr0, h.getSessionManager());
	}
	
	@Test
	public void testSecureRandom() {
		EngineHandlerBuilder b = new EngineHandlerBuilder(km, "key", tm);
		EngineHandler h = b.build();
		
		assertNull(b.getSecureRandom());
		assertNotNull(h.getSecureRandom());
		assertNotSame(h.getSecureRandom(), b.build().getSecureRandom());
		
		SecureRandom r = new SecureRandom();
		b.secureRandom(r);
		h = b.build();
		assertSame(r, b.getSecureRandom());
		assertSame(r, b.getSecureRandom());
		
		b.secureRandom(null);
		h = b.build();
		assertNull(b.getSecureRandom());
		assertNotSame(r, h.getSecureRandom());
		assertNotNull(h.getSecureRandom());
		assertNotSame(h.getSecureRandom(), b.build().getSecureRandom());
	}
	
	@Test
	public void testEarlyDataHandler() {
		EngineHandlerBuilder b = new EngineHandlerBuilder(km, "key", tm);
		EngineHandler h = b.build();
		
		assertNull(b.getEarlyDataHandler());
		assertSame(NoEarlyDataHandler.INSTANCE, h.getEarlyDataHandler());
		
		IEarlyDataHandler ed = new IEarlyDataHandler() {

			@Override
			public long getMaxEarlyDataSize() {
				return 0;
			}

			@Override
			public boolean hasEarlyData() {
				return false;
			}

			@Override
			public byte[] nextEarlyData(String protocol) {
				return null;
			}			
			
			@Override
			public void acceptedEarlyData() {
			}
			
			@Override
			public void rejectedEarlyData() {
			}

		};
		b.earlyDataHandler(ed);
		h = b.build();
		assertSame(ed, b.getEarlyDataHandler());
		assertSame(ed, h.getEarlyDataHandler());
		
		b.earlyDataHandler(null);
		h = b.build();
		assertNull(b.getEarlyDataHandler());
		assertSame(NoEarlyDataHandler.INSTANCE, h.getEarlyDataHandler());		
	}
	
	@Test
	public void testProtocolHandler() throws Exception {
		EngineHandlerBuilder b = new EngineHandlerBuilder(km, "key", tm);
		EngineHandler h = b.build();
		String[] supported = new String[] {"p1", "p2"};
		
		assertNull(b.getProtocolHandler());
		h.selectedApplicationProtocol(null);
		h.selectedApplicationProtocol("xxx");
		assertEquals("p1", h.selectApplicationProtocol(new ALPNExtension("p1"), supported));
		
		StringBuilder trace = new StringBuilder();
		AtomicReference<String> selected = new AtomicReference<String>(); 
		IApplicationProtocolHandler aph = new IApplicationProtocolHandler() {

			@Override
			public void selectedApplicationProtocol(String protocol) throws Alert {
				trace.append(protocol).append('|');
			}

			@Override
			public String selectApplicationProtocol(String[] offeredProtocols, String[] supportedProtocols)
					throws Alert {
				return selected.get();
			}
		};
		b.protocolHandler(aph);
		h = b.build();
		assertSame(aph, b.getProtocolHandler());
		assertEquals("", trace.toString());
		h.selectedApplicationProtocol(null);
		assertEquals("null|", trace.toString());
		h.selectedApplicationProtocol("yy");
		assertEquals("null|yy|", trace.toString());
		assertEquals("p1", h.selectApplicationProtocol(new ALPNExtension("p1"), supported));
		selected.set("");
		assertNull(h.selectApplicationProtocol(new ALPNExtension("p1"), supported));
		selected.set("xx");
		assertEquals("xx", h.selectApplicationProtocol(new ALPNExtension("p1"), supported));
		
		trace.setLength(0);
		b.protocolHandler(null);
		h = b.build();
		assertNull(b.getProtocolHandler());
		h.selectedApplicationProtocol(null);
		assertEquals("", trace.toString());
		assertEquals("p1", h.selectApplicationProtocol(new ALPNExtension("p1"), supported));
	}
	
	void assertHandler(EngineHandler h, String expected) throws Exception {
		trace.setLength(0);
		h.getEarlyDataHandler().hasEarlyData();
		h.verifyServerName(new ServerNameExtension("x"));
		h.selectedApplicationProtocol("y");
		assertEquals(expected, trace.toString());
		
	}
	
	@Test
	public void testBuild() throws Exception {
		EngineHandlerBuilder b = new EngineHandlerBuilder(km, "key", tm);
		IEarlyDataHandler edh1 = new TestEarlyDataHandler("EDH");		
		IEarlyDataHandler edh2 = new TestEarlyDataHandler("edh");
		IHostNameVerifier hnv1 = new TestHostNameVerifier("HNV");
		IHostNameVerifier hnv2 = new TestHostNameVerifier("hnv");
		IApplicationProtocolHandler aph1 = new TestApplicationProtocolHandler("APH");
		IApplicationProtocolHandler aph2 = new TestApplicationProtocolHandler("aph");
		b.earlyDataHandler(edh1);
		b.hostNameVerifier(hnv1);
		b.protocolHandler(aph1);

		assertHandler(b.build(), "EDH|HNV|x|APH|y|");
		assertHandler(b.build(edh2), "edh|HNV|x|APH|y|");
		assertHandler(b.build(hnv2), "EDH|hnv|x|APH|y|");
		assertHandler(b.build(aph2), "EDH|HNV|x|aph|y|");
		assertHandler(b.build(edh2, hnv2), "edh|hnv|x|APH|y|");
		assertHandler(b.build(edh2, aph2), "edh|HNV|x|aph|y|");
		assertHandler(b.build(hnv2, aph2), "EDH|hnv|x|aph|y|");
		assertHandler(b.build(edh2, hnv2, aph2), "edh|hnv|x|aph|y|");		
	}
	
	class TestEarlyDataHandler implements IEarlyDataHandler {

		String id;
		
		TestEarlyDataHandler(String id) {
			this.id = id;
		}

		@Override
		public long getMaxEarlyDataSize() {
			return 0;
		}

		@Override
		public boolean hasEarlyData() {
			trace.append(id).append('|');
			return false;
		}

		@Override
		public byte[] nextEarlyData(String protocol) {
			return null;
		}

		@Override
		public void acceptedEarlyData() {
		}

		@Override
		public void rejectedEarlyData() {
		}
	}
	
	class TestHostNameVerifier implements IHostNameVerifier {

		String id;
		
		TestHostNameVerifier(String id) {
			this.id = id;
		}
		
		@Override
		public boolean verifyHostName(String hostname) {
			trace.append(id).append('|').append(hostname).append('|');
			return true;
		}
	}
	
	class TestApplicationProtocolHandler implements IApplicationProtocolHandler {

		String id;
		
		TestApplicationProtocolHandler(String id) {
			this.id = id;
		}

		@Override
		public String selectApplicationProtocol(String[] offeredProtocols, String[] supportedProtocols) throws Alert {
			return null;
		}

		@Override
		public void selectedApplicationProtocol(String protocol) throws Alert {
			trace.append(id).append('|').append(protocol).append('|');
		}		
	}
}
