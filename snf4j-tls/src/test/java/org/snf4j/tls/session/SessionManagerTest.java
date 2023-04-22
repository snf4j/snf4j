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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.security.cert.Certificate;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.snf4j.tls.CommonTest;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.cipher.HashSpec;
import org.snf4j.tls.cipher.IHashSpec;
import org.snf4j.tls.crypto.Hkdf;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.crypto.TranscriptHash;
import org.snf4j.tls.engine.EngineState;
import org.snf4j.tls.engine.MachineState;
import org.snf4j.tls.engine.TestHandler;
import org.snf4j.tls.engine.TestHandshakeHandler;
import org.snf4j.tls.engine.TestParameters;
import org.snf4j.tls.extension.OfferedPsk;
import org.snf4j.tls.extension.PskIdentity;
import org.snf4j.tls.handshake.NewSessionTicket;

public class SessionManagerTest extends CommonTest {

	TestParameters parameters;
	
	TestHandler handler;
	
	TestHandshakeHandler handshakeHandler;
	
	SessionManager mgr;
	
	static KeySchedule keySchedule(IHashSpec hashSpec) throws Exception {
		return new KeySchedule(
				new Hkdf(hashSpec.getHash().createMac()), 
				new TranscriptHash(hashSpec.getHash().createMessageDigest()), 
				hashSpec);
	}
	
	EngineState state(ISession session, MachineState machineState, CipherSuite cipher) throws Exception {
		EngineState state = new EngineState(machineState, parameters, handler, handshakeHandler);
		
		state.initialize(keySchedule(cipher.spec().getHashSpec()), cipher);
		state.getKeySchedule().deriveEarlySecret();
		state.getKeySchedule().deriveHandshakeSecret(new byte[cipher.spec().getHashSpec().getHashLength()]);
		state.getKeySchedule().deriveMasterSecret();
		state.getKeySchedule().deriveResumptionMasterSecret();
		state.setSession(session);
		return state;
	}
	
	OfferedPsk[] psks(byte[]... tickets) {
		OfferedPsk[] psks = new OfferedPsk[tickets.length];
		int i = 0;
		
		for (byte[] ticket: tickets) {
			PskIdentity identity = new PskIdentity(ticket, 0);
			psks[i++] = new OfferedPsk(identity, new byte[10]);
		}
		return psks;
	}
	
	@Before
	public void before() {
		mgr = new SessionManager();
		parameters = new TestParameters();
		handler = new TestHandler();
		handshakeHandler = new TestHandshakeHandler();
		handshakeHandler.sessionManager = mgr;
	}
	
	@Test
	public void testPrepareCerts() throws Exception {
		Certificate[] certs = new Certificate[] {cert("rsapsssha256"), cert("rsasha1")};
		assertNull(mgr.prepareCerts(null));
		assertNotSame(certs, mgr.prepareCerts(certs));
		assertArrayEquals(certs, mgr.prepareCerts(certs));
	}
	
	@Test
	public void testInvalidateSession() throws Exception {
		SessionInfo sinfo = new SessionInfo().cipher(CipherSuite.TLS_AES_128_GCM_SHA256);
		SessionInfo cinfo = new SessionInfo().peerHost("xxx").cipher(CipherSuite.TLS_AES_256_GCM_SHA384);

		ISession s1 = mgr.newSession(sinfo,1000);
		assertTrue(s1.isValid());
		ISession s2 = mgr.newSession(cinfo.peerPort(100),1000);
		assertTrue(s2.isValid());
		
		assertNotNull(mgr.getSession(s1.getId(),1000));
		assertNotNull(mgr.getSession(s2.getId(),1000));
		assertNotNull(mgr.getSession("xxx", 100,1000));
		s1.invalidate();
		assertFalse(s1.isValid());
		assertNull(mgr.getSession(s1.getId(),1000));
		assertTrue(s2.isValid());
		assertNotNull(mgr.getSession(s2.getId(),1000));
		assertNotNull(mgr.getSession("xxx", 100,1000));
		s2.invalidate();
		assertFalse(s2.isValid());
		assertNull(mgr.getSession(s2.getId(),1000));
		assertNull(mgr.getSession("xxx", 100,1000));
		s1.invalidate();
		s2.invalidate();
	}
	
	@Test
	public void testNewSession() throws Exception {
		Certificate[] certs1 = new Certificate[] {cert("rsasha1")};
		Certificate[] certs2 = new Certificate[] {cert("rsasha1"),cert("rsasha256")};
		SessionInfo sinfo = new SessionInfo().cipher(CipherSuite.TLS_AES_128_GCM_SHA256);
		SessionInfo cinfo = new SessionInfo()
				.peerHost("xxx")
				.cipher(CipherSuite.TLS_AES_256_GCM_SHA384)
				.peerCerts(certs1)
				.localCerts(certs2);
		
		ISession s1 = mgr.newSession(sinfo, 1000);
		assertNull(s1.getPeerHost());
		assertEquals(-1, s1.getPeerPort());
		assertSame(CipherSuite.TLS_AES_128_GCM_SHA256, s1.getCipherSuite());
		assertEquals(1000, s1.getCreationTime());
		assertSame(mgr, s1.getManager());
		assertNull(s1.getPeerCertificates());
		assertNull(s1.getLocalCertificates());

		ISession s2 = mgr.newSession(cinfo.peerPort(100), 1001);
		assertEquals("xxx",s2.getPeerHost());
		assertEquals(100, s2.getPeerPort());
		assertSame(CipherSuite.TLS_AES_256_GCM_SHA384, s2.getCipherSuite());
		assertEquals(1001, s2.getCreationTime());
		assertSame(mgr, s2.getManager());
		assertNotSame(certs1, s2.getPeerCertificates());
		assertArrayEquals(certs1, s2.getPeerCertificates());
		assertNotSame(certs2, s2.getLocalCertificates());
		assertArrayEquals(certs2, s2.getLocalCertificates());
		
		assertSame(s1, mgr.getSession(s1.getId(), 1000+86400*1000));
		assertSame(s1, mgr.getSession(s1.getId(), 1000+86400*1000));
		assertSame(s2, mgr.getSession(s2.getId(), 1001+86400*1000));
		assertSame(s2, mgr.getSession("xxx", 100, 1001+86400*1000));
		
		assertNull(mgr.getSession(s1.getId(), 1000+1+86400*1000));
		assertNull(mgr.getSession("xxx", 100, 1001+1+86400*1000));
		assertNull(mgr.getSession(s1.getId(), 1000));
		assertNull(mgr.getSession("xxx", 100, 1001));
		assertSame(s2, mgr.getSession(s2.getId(), 1001+86400*1000));
		assertNull(mgr.getSession(s2.getId(), 1001+1+86400*1000));
		
		s1 = mgr.newSession(cinfo.peerPort(-1),1000);
		assertSame(s1, mgr.getSession(s1.getId(), 1000));
		assertNull(mgr.getSession("xxx", -1, 1000));

		s2 = mgr.newSession(cinfo.peerHost(null).peerPort(101),1000);
		assertSame(s2, mgr.getSession(s2.getId(), 1000));
		assertNull(mgr.getSession(null, 101, 1000));
		
		mgr = new SessionManager(1, 2000);
		handshakeHandler.sessionManager = mgr;
		long time = System.currentTimeMillis();
		s1 = mgr.newSession(sinfo);
		assertTrue(s1.getCreationTime() >= time);
		waitFor(500);
		assertNotNull(mgr.getSession(s1.getId()));
		waitFor(600);
		assertNull(mgr.getSession(s1.getId()));
		time = System.currentTimeMillis();
		s2 = mgr.newSession(cinfo.peerHost("xx").peerPort(33));
		assertTrue(s2.getCreationTime() >= time);
		waitFor(500);
		assertNotNull(mgr.getSession(s2.getId()));
		assertNotNull(mgr.getSession("xx",33));
		waitFor(600);
		assertNull(mgr.getSession(s2.getId()));
		assertNull(mgr.getSession("xx",33));
	}
	
	@Test
	public void testRemoveSession() throws Exception {
		SessionInfo sinfo = new SessionInfo().cipher(CipherSuite.TLS_AES_128_GCM_SHA256);
		SessionInfo cinfo = new SessionInfo().peerHost("xxx").cipher(CipherSuite.TLS_AES_256_GCM_SHA384);
		
		ISession s1 = mgr.newSession(sinfo, 1000);
		ISession s2 = mgr.newSession(cinfo.peerPort(100), 1001);
		
		assertNotNull(mgr.getSession(s1.getId(), 1000 + 86400*1000));
		mgr.removeSession(s1.getId(), 1000 + 86400*1000);
		assertNull(mgr.getSession(s1.getId(), 1000 + 86400*1000));
		
		assertNotNull(mgr.getSession(s2.getId(), 1000 + 86400*1000));
		assertNotNull(mgr.getSession("xxx",100, 1000 + 86400*1000));
		mgr.removeSession(s2.getId(), 1000 + 86400*1000);
		assertNull(mgr.getSession(s2.getId(), 1000 + 86400*1000));
		assertNull(mgr.getSession("xxx",100, 1000 + 86400*1000));
		
		s1 = mgr.newSession(cinfo.peerPort(101), 1000);
		mgr.removeSession(s1.getId(), 1000 + 1 + 86400*1000);
		assertNull(mgr.getSession(s1.getId(), 1000 + 86400*1000));
		assertNotNull(mgr.getSession("xxx",101, 1000 + 86400*1000));
		assertNull(mgr.getSession("xxx",101, 1000 + 1 + 86400*1000));

		mgr = new SessionManager(1, 2000);
		handshakeHandler.sessionManager = mgr;
		long time = System.currentTimeMillis();

		s1 = mgr.newSession(sinfo);
		s2 = mgr.newSession(cinfo.peerPort(101));
		
		waitFor(500);
		assertNotNull(mgr.getSession(s1.getId()));
		assertNotNull(mgr.getSession(s2.getId()));
		assertNotNull(mgr.getSession("xxx", 101));
		mgr.removeSession(s1.getId());
		assertNull(mgr.getSession(s1.getId()));
		waitFor(600);
		mgr.removeSession(s2.getId());
		assertNull(mgr.getSession(s2.getId(), time+500));
		assertNotNull(mgr.getSession("xxx", 101, time+500));
	}
	
	@Test
	public void testNewTicket() throws Exception {
		SessionInfo info = new SessionInfo().cipher(CipherSuite.TLS_AES_128_GCM_SHA256);
		
		Session session = (Session) mgr.newSession(info, 1000);
		EngineState state = state(session, MachineState.SRV_INIT, CipherSuite.TLS_AES_128_GCM_SHA256);

		assertEquals(0, session.getTickets(1000).length);
		NewSessionTicket ticket1 = mgr.newTicket(state, 1000);
		assertArrayEquals(bytes(0,1), ticket1.getNonce());
		assertEquals(1, session.getTickets(1000).length);
		SessionTicket ticket = session.getTickets(1000)[0]; 
		assertArrayEquals(ticket1.getTicket(), ticket.getTicket());
		assertEquals(ticket1.getAgeAdd(), ticket.getAgeAdd());
		assertEquals(0, ticket1.getExtensions().size());
		assertEquals(86400, ticket1.getLifetime());
		assertSame(HashSpec.SHA256, ticket.getHashSpec());
		assertEquals(1000, ticket.getCreationTime());
		assertArrayEquals(state.getKeySchedule().computePsk(bytes(0,1)), ticket.getPsk());
		assertEquals(-1L, ticket.getMaxEarlyDataSize());
		assertTrue(ticket.isValid(1000));
		assertTrue(ticket.isValid(1000-1+86400*1000));
		assertFalse(ticket.isValid(1000+86400*1000));
		
		NewSessionTicket ticket2 = mgr.newTicket(state, 1000);
		assertArrayEquals(bytes(0,2), ticket2.getNonce());
		assertFalse(ticket1.getAgeAdd() == ticket2.getAgeAdd());	
		
		long time = System.currentTimeMillis();
		mgr.newTicket(state);
		assertEquals(3, mgr.getTickets(session,1000).length);
		assertEquals(1, mgr.getTickets(session).length);
		ticket = mgr.getTickets(session)[0];
		assertTrue(ticket.getCreationTime() >= time);
		
		time = System.currentTimeMillis();
		mgr = new SessionManager(1, 2000);
		handshakeHandler.sessionManager = mgr;
		session = (Session) mgr.newSession(info);
		assertTrue(session.getCreationTime() >= time);
		assertTrue(session.getId() > 1);
		state = state(session, MachineState.SRV_INIT, CipherSuite.TLS_AES_128_GCM_SHA256);
		ticket1 = mgr.newTicket(state);
		ticket2 = mgr.newTicket(state);
		assertEquals(1L, ticket1.getLifetime());
		assertEquals(2, mgr.getTickets(session).length);
		UsedSession used = mgr.useSession(psks(ticket1.getTicket()), HashSpec.SHA256);
		assertTrue(used.getTicket().isValid());
		assertEquals(0, used.getSelectedIdentity());
		assertEquals(1, mgr.getTickets(session).length);
		waitFor(1100);
		assertFalse(used.getTicket().isValid());
		assertEquals(0, mgr.getTickets(session).length);
	}
	
	@Test
	public void testNonce() {
		assertArrayEquals(bytes(0,0), mgr.nonce(0));
		assertArrayEquals(bytes(0,1), mgr.nonce(1));
		assertArrayEquals(bytes(0,255), mgr.nonce(255));
		assertArrayEquals(bytes(1,0), mgr.nonce(256));
		assertArrayEquals(bytes(1,255), mgr.nonce(0x1ff));
		assertArrayEquals(bytes(255,255), mgr.nonce(0xffff));
		assertArrayEquals(bytes(0,1,0,0), mgr.nonce(0x10000));
		assertArrayEquals(bytes(1,2,3,4), mgr.nonce(0x01020304));
		assertArrayEquals(bytes(255,255,255,255), mgr.nonce(0xffffffffL));
		assertArrayEquals(bytes(0,0,0,1,0,0,0,0), mgr.nonce(0x0100000000L));
		assertArrayEquals(bytes(1,2,3,4,5,6,7,8), mgr.nonce(0x0102030405060708L));
	}
	
	@Test
	public void testUseSession() throws Exception {
		SessionInfo info = new SessionInfo().cipher(CipherSuite.TLS_AES_128_GCM_SHA256);
		
		Session session1 = (Session) mgr.newSession(info, 1000);
		EngineState state1 = state(session1, MachineState.SRV_INIT, CipherSuite.TLS_AES_128_GCM_SHA256);
		NewSessionTicket ticket1 = mgr.newTicket(state1, 1000);
		Session session2 = (Session) mgr.newSession(info, 1000);
		EngineState state2 = state(session2, MachineState.SRV_INIT, CipherSuite.TLS_AES_128_GCM_SHA256);
		NewSessionTicket ticket2 = mgr.newTicket(state2, 1000);

		byte[] identity = ticket2.getTicket().clone();
		for (int i=0; i<identity.length; ++i) {
			identity[i]++;
			assertNull("i=" + i, mgr.useSession(psks(identity), HashSpec.SHA256, 1000));
			identity[i]--;
		}
		assertNull(mgr.useSession(psks(identity), HashSpec.SHA384, 1000));
		assertNull(mgr.useSession(psks(Arrays.copyOf(identity, identity.length+1)), HashSpec.SHA256, 1000));
		assertNull(mgr.useSession(psks(Arrays.copyOf(identity, identity.length-1)), HashSpec.SHA256, 1000));
		UsedSession used = mgr.useSession(psks(new byte[10], identity), HashSpec.SHA256, 1000);
		assertNotNull(used);
		assertSame(session2, used.getSession());
		assertEquals(1, used.getSelectedIdentity());
		assertNull(mgr.useSession(psks(identity), HashSpec.SHA256, 1000));
		
		assertEquals(1, mgr.getTickets(session1, 1000).length);
		identity = ticket1.getTicket().clone();
		assertNull(mgr.useSession(psks(identity), HashSpec.SHA256));
		assertEquals(1, mgr.getTickets(session1, 1000-1+86400*1000).length);
		assertEquals(0, mgr.getTickets(session1, 1000+86400*1000).length);

		mgr = new SessionManager(1, 2000);
		handshakeHandler.sessionManager = mgr;
		session1 = (Session) mgr.newSession(info);
		state1 = state(session1, MachineState.SRV_INIT, CipherSuite.TLS_AES_128_GCM_SHA256);
		ticket1 = mgr.newTicket(state1);
		session2 = (Session) mgr.newSession(info);
		state2 = state(session2, MachineState.SRV_INIT, CipherSuite.TLS_AES_128_GCM_SHA256);
		ticket2 = mgr.newTicket(state2);
		
		used = mgr.useSession(psks(ticket1.getTicket()), HashSpec.SHA256);
		assertNotNull(used);
		assertSame(session1, used.getSession());
		waitFor(1100);
		assertNull(mgr.useSession(psks(ticket2.getTicket()), HashSpec.SHA256));
	}
	
	@Test
	public void testGetTickets() throws Exception {
		SessionInfo info = new SessionInfo().cipher(CipherSuite.TLS_AES_128_GCM_SHA256);
		Session session = (Session) mgr.newSession(info, 1000);
		EngineState state = state(session, MachineState.SRV_INIT, CipherSuite.TLS_AES_128_GCM_SHA256);
		
		NewSessionTicket ticket1 = mgr.newTicket(state, 1000);
		NewSessionTicket ticket2 = mgr.newTicket(state, 1000);
		NewSessionTicket ticket3 = mgr.newTicket(state, 1000);

		SessionTicket[] tickets = mgr.getTickets(session, 1000);
		assertEquals(3, tickets.length);
		assertArrayEquals(ticket1.getTicket(), tickets[0].getTicket());
		assertArrayEquals(ticket2.getTicket(), tickets[1].getTicket());
		assertArrayEquals(ticket3.getTicket(), tickets[2].getTicket());
		
		mgr.removeTicket(session, tickets[1]);
		SessionTicket[] tickets2 = mgr.getTickets(session, 1000);
		assertEquals(2, tickets2.length);
		assertArrayEquals(ticket1.getTicket(), tickets[0].getTicket());
		assertArrayEquals(ticket3.getTicket(), tickets[2].getTicket());
		
		mgr.putTicket(session, tickets[1]);
		tickets2 = mgr.getTickets(session, 1000);
		assertEquals(3, tickets2.length);
		assertArrayEquals(ticket1.getTicket(), tickets[0].getTicket());
		assertArrayEquals(ticket3.getTicket(), tickets[2].getTicket());
		assertArrayEquals(ticket2.getTicket(), tickets[1].getTicket());
		
		mgr.newTicket(state, 1001);
		tickets = mgr.getTickets(session, 1000);
		assertEquals(4, tickets.length);
		assertEquals(4, mgr.getTickets(session, 1001).length);
		assertEquals(4, mgr.getTickets(session, 1000 - 1 + 86400*1000).length);
		assertEquals(1, mgr.getTickets(session, 1000 + 86400*1000).length);
		assertEquals(0, mgr.getTickets(session, 1000 + 1 + 86400*1000).length);
		
		mgr = new SessionManager(1, 2000);
		handshakeHandler.sessionManager = mgr;
		session = (Session) mgr.newSession(info);
		state = state(session, MachineState.SRV_INIT, CipherSuite.TLS_AES_128_GCM_SHA256);
		
		ticket1 = mgr.newTicket(state);
		ticket2 = mgr.newTicket(state);
		waitFor(500);
		ticket3 = mgr.newTicket(state);
		assertEquals(3, mgr.getTickets(session).length);
		waitFor(600);
		assertEquals(1, mgr.getTickets(session).length);
		waitFor(500);
		assertEquals(0, mgr.getTickets(session).length);	
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCheckSessionDifferentManager() {
		SessionInfo info = new SessionInfo().cipher(CipherSuite.TLS_AES_128_GCM_SHA256);
		Session session = (Session) mgr.newSession(info, 1000);
		mgr = new SessionManager();
		mgr.checkSession(session);
	}

	@Test(expected=NullPointerException.class)
	public void testCheckSessionNull() {
		mgr.checkSession(null);
	}
	
	@Test(expected=ClassCastException.class)
	public void testCheckSessionDifferentImplementation() {
		ISession session = new ISession() {

			@Override
			public ISessionManager getManager() {
				return mgr;
			}

			@Override
			public long getId() {
				return 0;
			}

			@Override
			public long getCreationTime() {
				return 0;
			}

			@Override
			public CipherSuite getCipherSuite() {
				return null;
			}

			@Override
			public String getPeerHost() {
				return null;
			}

			@Override
			public int getPeerPort() {
				return 0;
			}

			@Override
			public void invalidate() {
			}

			@Override
			public boolean isValid() {
				return false;
			}

			@Override
			public Certificate[] getPeerCertificates() {
				return null;
			}

			@Override
			public Certificate[] getLocalCertificates() {
				return null;
			}
		};
		mgr.checkSession(session);
	}
	
}
