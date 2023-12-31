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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.junit.Test;
import org.snf4j.tls.CommonTest;
import org.snf4j.tls.alert.NoApplicationProtocolAlert;
import org.snf4j.tls.extension.ALPNExtension;
import org.snf4j.tls.extension.ServerNameExtension;
import org.snf4j.tls.record.ContentType;
import org.snf4j.tls.session.ISessionManager;
import org.snf4j.tls.session.SessionManager;

public class EngineHandlerTest extends CommonTest {

	final static char[] PASSWORD = "password".toCharArray();
	
	KeyManagerFactory kmf;
	
	X509KeyManager km;

	TrustManagerFactory tmf;
	
	X509TrustManager tm;
	
	SecureRandom random = new SecureRandom();
	
	ISessionManager sm = new SessionManager();
	
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
	}
	
	void assertValidator(EngineHandler h, X509TrustManager manager) throws Exception {
		ICertificateValidator s = h.getCertificateValidator();
		Field f = X509TrustManagerCertificateValidator.class.getDeclaredField("manager");
		f.setAccessible(true);
		assertSame(manager, f.get(s));
	}

	void assertSelector(EngineHandler h, X509KeyManager manager, String alias) throws Exception {
		ICertificateSelector s = h.getCertificateSelector();
		Field f = X509KeyManagerCertificateSelector.class.getDeclaredField("manager");
		f.setAccessible(true);
		assertSame(manager, f.get(s));
		f = X509KeyManagerCertificateSelector.class.getDeclaredField("alias");
		f.setAccessible(true);
		assertSame(alias, f.get(s));
	}
	
	void assertPadding(EngineHandler h, int padding) {
		assertEquals(1, h.calculatePadding(ContentType.APPLICATION_DATA, padding-1));
		assertEquals(0, h.calculatePadding(ContentType.APPLICATION_DATA, padding));
		assertEquals(padding-1, h.calculatePadding(ContentType.APPLICATION_DATA, padding+1));
	}
	
	@Test
	public void testConstructor() throws Exception {
		EngineHandler h = new EngineHandler(km, "key", tm, random, sm, 16);
		assertSame(random, h.getSecureRandom());
		assertSame(sm, h.getSessionManager());
		assertSelector(h, km, "key");
		assertValidator(h, tm);
		assertPadding(h, 16);

		h = new EngineHandler(km, "key", tm, random, sm);
		assertSame(random, h.getSecureRandom());
		assertSame(sm, h.getSessionManager());
		assertSelector(h, km, "key");
		assertValidator(h, tm);
		assertPadding(h, 4096);
		
		h = new EngineHandler(km, "key", tm, sm, 16);
		assertNotSame(random, h.getSecureRandom());
		assertNotSame(new EngineHandler(km, "key", tm, sm, 16).getSecureRandom(), h.getSecureRandom());
		assertSame(sm, h.getSessionManager());
		assertSelector(h, km, "key");
		assertValidator(h, tm);
		assertPadding(h, 16);

		h = new EngineHandler(km, "key", tm, sm);
		assertNotSame(random, h.getSecureRandom());
		assertSame(sm, h.getSessionManager());
		assertSelector(h, km, "key");
		assertValidator(h, tm);
		assertPadding(h, 4096);

		h = new EngineHandler(km, "key", tm, random);
		assertSame(random, h.getSecureRandom());
		assertNotNull(h.getSessionManager());
		assertNotSame(sm, h.getSessionManager());
		ISessionManager commonSm = h.getSessionManager();
		assertSelector(h, km, "key");
		assertValidator(h, tm);
		assertPadding(h, 4096);

		h = new EngineHandler(km, "key", tm);
		assertNotSame(random, h.getSecureRandom());
		assertSame(commonSm, h.getSessionManager());
		assertSelector(h, km, "key");
		assertValidator(h, tm);
		assertPadding(h, 4096);

	
		h = new EngineHandler(km, tm, random, sm, 16);
		assertSame(random, h.getSecureRandom());
		assertSame(sm, h.getSessionManager());
		assertSelector(h, km, null);
		assertValidator(h, tm);
		assertPadding(h, 16);

		h = new EngineHandler(km, tm, random, sm);
		assertSame(random, h.getSecureRandom());
		assertSame(sm, h.getSessionManager());
		assertSelector(h, km, null);
		assertValidator(h, tm);
		assertPadding(h, 4096);
		
		h = new EngineHandler(km, tm, sm, 16);
		assertNotSame(random, h.getSecureRandom());
		assertNotSame(new EngineHandler(km, "key", tm, sm, 16).getSecureRandom(), h.getSecureRandom());
		assertSame(sm, h.getSessionManager());
		assertSelector(h, km, null);
		assertValidator(h, tm);
		assertPadding(h, 16);

		h = new EngineHandler(km, tm, sm);
		assertNotSame(random, h.getSecureRandom());
		assertSame(sm, h.getSessionManager());
		assertSelector(h, km, null);
		assertValidator(h, tm);
		assertPadding(h, 4096);

		h = new EngineHandler(km, tm, random);
		assertSame(random, h.getSecureRandom());
		assertNotNull(h.getSessionManager());
		assertNotSame(sm, h.getSessionManager());
		assertSelector(h, km, null);
		assertValidator(h, tm);
		assertPadding(h, 4096);

		h = new EngineHandler(km, tm);
		assertNotSame(random, h.getSecureRandom());
		assertSame(commonSm, h.getSessionManager());
		assertSelector(h, km, null);
		assertValidator(h, tm);
		assertPadding(h, 4096);
	}
	
	@Test
	public void testVerify() {
		assertTrue(new EngineHandler(km, tm).verifyServerName(new ServerNameExtension("xxx")));
	}

	@Test
	public void testGetKeyLimit() {
		assertEquals(1111, new EngineHandler(km, tm).getKeyLimit(null, 1111));
	}
	
	@Test
	public void testCalculatePadding() {
		EngineHandler h = new EngineHandler(km, "key", tm, random, sm, 16);
		assertEquals(0, h.calculatePadding(ContentType.APPLICATION_DATA, 16));
		assertEquals(1, h.calculatePadding(ContentType.APPLICATION_DATA, 15));
		assertEquals(1, h.calculatePadding(ContentType.APPLICATION_DATA, 16383));
		assertEquals(0, h.calculatePadding(ContentType.APPLICATION_DATA, 16384));
		assertEquals(0, h.calculatePadding(ContentType.APPLICATION_DATA, 16385));
		assertEquals(15, h.calculatePadding(ContentType.APPLICATION_DATA, 17));
		assertEquals(15, h.calculatePadding(ContentType.APPLICATION_DATA, 17));

		h = new EngineHandler(km, "key", tm, random, sm, 17);
		assertEquals(1, h.calculatePadding(ContentType.APPLICATION_DATA, 16383));
		assertEquals(0, h.calculatePadding(ContentType.APPLICATION_DATA, 16384));
		assertEquals(0, h.calculatePadding(ContentType.APPLICATION_DATA, 16385));
		
	}
	
	@Test
	public void testCreateNewTickets() {
		TicketInfo[] tickets = new EngineHandler(km, "key", tm, random, sm, 16).createNewTickets();
		
		assertEquals(1, tickets.length);
		assertSame(TicketInfo.NO_MAX_EARLY_DATA_SIZE, tickets[0]);
		assertEquals(-1, tickets[0].getMaxEarlyDataSize());
	}

	@Test
	public void testEarlyData() {
		EngineHandler h = new EngineHandler(km, "key", tm, random, sm, 16);
		
		assertEquals(0, h.getEarlyDataHandler().getMaxEarlyDataSize());
		assertFalse(h.getEarlyDataHandler().hasEarlyData());
		assertNull(h.getEarlyDataHandler().nextEarlyData(null));
	}
	
	String[] names(String... names) {
		return names.clone();
	}
	
	@Test
	public void testSelectApplicationProtocol() throws Exception {
		EngineHandler h = new EngineHandler(km, "key", tm, random, sm, 16);
		ALPNExtension alpn = new ALPNExtension("xxx", "yyy");
		
		assertNull(h.selectApplicationProtocol(alpn, names()));
		assertEquals("yyy", h.selectApplicationProtocol(alpn, names("yyy")));
		assertEquals("yyy", h.selectApplicationProtocol(alpn, names("yyy","xxx")));
		assertEquals("xxx", h.selectApplicationProtocol(alpn, names("yy","xxx")));
		assertEquals("xxx", h.selectApplicationProtocol(alpn, names("xxx", "yyy")));
		assertEquals("xxx", h.selectApplicationProtocol(alpn, names("xxx")));
		assertNull(h.selectApplicationProtocol(null, names()));
		assertNull(h.selectApplicationProtocol(null, names("xxx")));
		assertNull(h.selectApplicationProtocol(null, names("xxx","yyy")));
	}
	
	@Test(expected = NoApplicationProtocolAlert.class)
	public void testSelectApplicationProtocolAlert1() throws Exception {
		EngineHandler h = new EngineHandler(km, "key", tm, random, sm, 16);
		ALPNExtension alpn = new ALPNExtension("xxx", "yyy");
		
		assertEquals("yyy", h.selectApplicationProtocol(alpn, names("yy","xx")));
	}	
}
