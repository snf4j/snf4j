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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.KeyPair;
import java.security.MessageDigest;
import java.util.ArrayList;

import javax.crypto.Mac;

import org.junit.Test;
import org.snf4j.tls.CommonTest;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.crypto.DHKeyExchange;
import org.snf4j.tls.crypto.Hkdf;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.crypto.TranscriptHash;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.PskKeyExchangeMode;
import org.snf4j.tls.handshake.CertificateType;
import org.snf4j.tls.handshake.ClientHello;
import org.snf4j.tls.handshake.ServerHello;
import org.snf4j.tls.record.RecordType;
import org.snf4j.tls.session.ISession;
import org.snf4j.tls.session.TestSession;

public class EngineStateTest extends CommonTest {

	static ServerHello serverHello(int version) {
		return new ServerHello(
				version, 
				new byte[32], 
				new byte[0], 
				CipherSuite.TLS_AES_128_GCM_SHA256, 
				(byte)0, 
				new ArrayList<IExtension>());
	}

	static ClientHello clientHello(int version) {
		return new ClientHello(
				version, 
				new byte[32], 
				new byte[0], 
				new CipherSuite[] {CipherSuite.TLS_AES_128_GCM_SHA256}, 
				new byte[0], 
				new ArrayList<IExtension>());
	}
	
	static ProducedHandshake handshake(int id) {
		return new ProducedHandshake(serverHello(id), RecordType.INITIAL);
	}
	
	static int id(ProducedHandshake handshake) {
		return ((ServerHello)handshake.getHandshake()).getLegacyVersion();
	}
	
	@Test
	public void testConstructor() {
		TestParameters p = new TestParameters();
		TestHandshakeHandler h = new TestHandshakeHandler();
		TestHandshakeHandler l = new TestHandshakeHandler();
		EngineState state = new EngineState(MachineState.CLI_INIT,p,h,l); 
		assertSame(p, state.getParameters());
		assertSame(h, state.getHandler());
		assertSame(l, state.getListener());
	}
	
	@Test
	public void testSetPskModes() {
		EngineState state = new EngineState(
				MachineState.CLI_INIT, 
				new TestParameters(), 
				new TestHandshakeHandler(),
				new TestHandshakeHandler());

		state.setPskModes(new PskKeyExchangeMode[] {PskKeyExchangeMode.PSK_KE});
		assertTrue(state.hasPskMode(PskKeyExchangeMode.PSK_KE));
		assertFalse(state.hasPskMode(PskKeyExchangeMode.PSK_DHE_KE));

		state.setPskModes(new PskKeyExchangeMode[] {PskKeyExchangeMode.PSK_DHE_KE});
		assertFalse(state.hasPskMode(PskKeyExchangeMode.PSK_KE));
		assertTrue(state.hasPskMode(PskKeyExchangeMode.PSK_DHE_KE));
	
		state.setPskModes(new PskKeyExchangeMode[] {PskKeyExchangeMode.PSK_KE, PskKeyExchangeMode.PSK_DHE_KE});
		assertTrue(state.hasPskMode(PskKeyExchangeMode.PSK_KE));
		assertTrue(state.hasPskMode(PskKeyExchangeMode.PSK_DHE_KE));

		state.setPskModes(new PskKeyExchangeMode[] {});
		assertFalse(state.hasPskMode(PskKeyExchangeMode.PSK_KE));
		assertFalse(state.hasPskMode(PskKeyExchangeMode.PSK_DHE_KE));
	}
	
	@Test
	public void testStorePrivateKey() throws Exception {
		KeyPair pair1 = DHKeyExchange.FFDHE2048.generateKeyPair(RANDOM);
		KeyPair pair2 = DHKeyExchange.FFDHE3072.generateKeyPair(RANDOM);
		EngineState state = new EngineState(
				MachineState.CLI_INIT, 
				new TestParameters(), 
				new TestHandshakeHandler(),
				new TestHandshakeHandler());
		
		assertNull(state.getPrivateKey(NamedGroup.FFDHE2048));
		state.addPrivateKey(NamedGroup.FFDHE2048, pair1.getPrivate());
		assertSame(pair1.getPrivate(), state.getPrivateKey(NamedGroup.FFDHE2048));
		assertNull(state.getPrivateKey(NamedGroup.FFDHE3072));
		state.addPrivateKey(NamedGroup.FFDHE3072, pair2.getPrivate());
		assertSame(pair1.getPrivate(), state.getPrivateKey(NamedGroup.FFDHE2048));
		assertSame(pair2.getPrivate(), state.getPrivateKey(NamedGroup.FFDHE3072));
		state.clearPrivateKeys();
		assertNull(state.getPrivateKey(NamedGroup.FFDHE2048));
		assertNull(state.getPrivateKey(NamedGroup.FFDHE3072));
		state.clearPrivateKeys();
	}
	
	@Test
	public void testPskContext() throws Exception {
		EngineState state = new EngineState(
				MachineState.CLI_INIT, 
				new TestParameters(), 
				new TestHandshakeHandler(),
				new TestHandshakeHandler());
		TranscriptHash th = new TranscriptHash(MessageDigest.getInstance("SHA-256"));
		Hkdf h = new Hkdf(Mac.getInstance("HmacSHA256"));
		KeySchedule ks = new KeySchedule(h, th, CipherSuite.TLS_AES_128_GCM_SHA256.spec());
		PskContext psk1 = new PskContext(ks);
		PskContext psk2 = new PskContext(ks);
		
		assertNull(state.getPskContexts());
		state.addPskContext(psk1);
		assertEquals(1, state.getPskContexts().size());
		assertSame(psk1, state.getPskContexts().get(0));
		state.addPskContext(psk2);
		assertEquals(2, state.getPskContexts().size());
		assertSame(psk1, state.getPskContexts().get(0));
		assertSame(psk2, state.getPskContexts().get(1));
		assertSame(state.getPskContexts(), state.getPskContexts());
		state.clearPskContexts();
		assertNull(state.getPskContexts());
		state.clearPskContexts();
		assertNull(state.getPskContexts());
	}
	
	@Test
	public void testState() throws Exception {
		EngineState state = new EngineState(
				MachineState.CLI_INIT, 
				new TestParameters(), 
				new TestHandshakeHandler(),
				new TestHandshakeHandler());

		assertSame(MachineState.CLI_INIT, state.getState());
		assertFalse(state.isStarted());
		assertFalse(state.isConnected());
		assertTrue(state.isClientMode());
		state.changeState(MachineState.CLI_WAIT_1_SH);
		assertSame(MachineState.CLI_WAIT_1_SH, state.getState());
		assertTrue(state.isStarted());
		assertFalse(state.isConnected());
		state.changeState(MachineState.CLI_CONNECTED);
		assertSame(MachineState.CLI_CONNECTED, state.getState());
		assertTrue(state.isStarted());
		assertTrue(state.isConnected());
		
		state = new EngineState(
				MachineState.SRV_INIT, 
				new TestParameters(), 
				new TestHandshakeHandler(),
				new TestHandshakeHandler());

		assertSame(MachineState.SRV_INIT, state.getState());
		assertFalse(state.isStarted());
		assertFalse(state.isConnected());
		assertFalse(state.isClientMode());
		state.changeState(MachineState.SRV_WAIT_1_CH);
		assertSame(MachineState.SRV_WAIT_1_CH, state.getState());
		assertTrue(state.isStarted());
		assertFalse(state.isConnected());
		state.changeState(MachineState.SRV_CONNECTED);
		assertSame(MachineState.SRV_CONNECTED, state.getState());
		assertTrue(state.isStarted());
		assertTrue(state.isConnected());
	}

	@Test
	public void testInitialize() throws Exception {
		EngineState state = new EngineState(
				MachineState.CLI_INIT, 
				new TestParameters(), 
				new TestHandshakeHandler(),
				new TestHandshakeHandler());
		
		assertFalse(state.isInitialized());
		
		Hkdf h = new Hkdf(Mac.getInstance("HmacSHA256"));
		TranscriptHash th1 = new TranscriptHash(MessageDigest.getInstance("SHA-256"));
		TranscriptHash th2 = new TranscriptHash(MessageDigest.getInstance("SHA-256"));
		KeySchedule ks = new KeySchedule(h, th1, CipherSuite.TLS_AES_128_GCM_SHA256.spec());
		state.initialize(ks, CipherSuite.TLS_AES_128_GCM_SHA256);
		assertTrue(state.isInitialized());
		assertSame(CipherSuite.TLS_AES_128_GCM_SHA256, state.getCipherSuite());
		assertSame(th1, state.getTranscriptHash());
		state.setTranscriptHash(th2);
		assertSame(th2, state.getTranscriptHash());
		assertSame(ks, state.getKeySchedule());
	}
	
	@Test
	public void testGetters() {
		EngineState state = new EngineState(
				MachineState.CLI_INIT, 
				new TestParameters(), 
				new TestHandshakeHandler(),
				new TestHandshakeHandler());
		ClientHello ch = clientHello(111);
		CertificateCriteria cc = new CertificateCriteria(CertificateType.X509, null, null, null);
		ISession session = new TestSession(1,100);
		EarlyDataContext ctx = new EarlyDataContext(CipherSuite.TLS_AES_128_GCM_SHA256,100);
		
		assertNull(state.getHostName());
		state.setHostName("host");
		assertEquals("host", state.getHostName());
		assertEquals(0, state.getVersion());
		state.setVersion(111);
		assertEquals(111, state.getVersion());
		assertNull(state.getClientHello());
		state.setClientHello(ch);
		assertSame(ch, state.getClientHello());
		assertNull(state.getCertCryteria());
		state.setCertCryteria(cc);
		assertSame(cc, state.getCertCryteria());
		assertSame(NoneEarlyDataContext.INSTANCE, state.getEarlyDataContext());
		state.setEarlyDataContext(ctx);
		assertSame(ctx, state.getEarlyDataContext());
		state.setEarlyDataContext(null);
		assertSame(NoneEarlyDataContext.INSTANCE, state.getEarlyDataContext());
		assertNull(state.getNamedGroup());
		state.setNamedGroup(NamedGroup.FFDHE3072);
		assertSame(NamedGroup.FFDHE3072, state.getNamedGroup());
		assertNull(state.getSession());
		state.setSession(session);
		assertSame(session, state.getSession());
	}

	@Test
	public void testGetSessionInfo() {
		EngineState state = new EngineState(
				MachineState.CLI_INIT, 
				new TestParameters(), 
				new TestHandshakeHandler(),
				new TestHandshakeHandler());
		
		assertNotNull(state.getSessionInfo());
		assertSame(state.getSessionInfo(), state.getSessionInfo());
	}
	
	@Test
	public void testChangeState() throws Exception {
		EngineState state = new EngineState(
				MachineState.CLI_INIT, 
				new TestParameters(), 
				new TestHandshakeHandler(),
				new TestHandshakeHandler());
		
		assertTrue(state.hadState(MachineState.CLI_INIT));
		assertFalse(state.hadState(MachineState.CLI_WAIT_1_SH));
		state.changeState(MachineState.CLI_WAIT_1_SH);
		assertTrue(state.hadState(MachineState.CLI_WAIT_1_SH));
		assertFalse(state.hadState(MachineState.CLI_WAIT_2_SH));
		state.changeState(MachineState.CLI_WAIT_2_SH);
		assertTrue(state.hadState(MachineState.CLI_WAIT_2_SH));
		assertTrue(state.hadState(MachineState.CLI_INIT));
		assertTrue(state.hadState(MachineState.CLI_WAIT_1_SH));
		assertFalse(state.hadState(MachineState.CLI_WAIT_EE));
		try {
			state.changeState(MachineState.SRV_INIT);
			fail();
		} catch (InternalErrorAlert e) {}
	}
	
	@Test
	public void testGetMaxFragmentLength() {
		EngineState state = new EngineState(
				MachineState.CLI_INIT, 
				new TestParameters(), 
				new TestHandshakeHandler(),
				new TestHandshakeHandler());
		assertEquals(16384, state.getMaxFragmentLength());
	}
	
	@Test
	public void testGetProduced() throws Exception {
		EngineState state = new EngineState(
				MachineState.SRV_WAIT_1_CH, 
				new TestParameters(), 
				new TestHandshakeHandler(),
				new TestHandshakeHandler());
		
		assertEquals(0, state.getProduced().length);
		assertFalse(state.hasProduced());
		state.produce(handshake(1));
		assertTrue(state.hasProduced());
		ProducedHandshake[] produced = state.getProduced();
		assertFalse(state.hasProduced());
		assertEquals(1, produced.length);
		assertEquals(1, id(produced[0]));
		assertEquals(0, state.getProduced().length);
		
		state.produce(handshake(2));
		state.produce(handshake(3));
		produced = state.getProduced();
		assertEquals(2, produced.length);
		assertEquals(2, id(produced[0]));
		assertEquals(3, id(produced[1]));
		assertEquals(0, state.getProduced().length);
		
		state.prepare(handshake(4));
		produced = state.getProduced();
		assertEquals(1, produced.length);
		assertEquals(4, id(produced[0]));
		assertEquals(0, state.getProduced().length);

		state.prepare(handshake(5));
		state.produce(handshake(6));
		produced = state.getProduced();
		assertEquals(2, produced.length);
		assertEquals(5, id(produced[0]));
		assertEquals(6, id(produced[1]));
		assertEquals(0, state.getProduced().length);
		
		state.produce(handshake(7));
		state.prepare(handshake(8));
		assertFalse(state.hasTasks());
		assertFalse(state.hasRunningTasks());
		assertFalse(state.hasProducingTasks());
		state.addTask(new Task(9));
		assertTrue(state.hasTasks());
		assertFalse(state.hasRunningTasks());
		assertTrue(state.hasProducingTasks());
		produced = state.getProduced();
		assertEquals(1, produced.length);
		assertEquals(7, id(produced[0]));
		assertEquals(0, state.getProduced().length);
		Runnable task = state.getTask();
		assertFalse(state.hasTasks());
		assertTrue(state.hasRunningTasks());
		assertTrue(state.hasProducingTasks());
		assertEquals(0, state.getProduced().length);
		task.run();
		assertFalse(state.hasTasks());
		assertTrue(state.hasRunningTasks());
		assertTrue(state.hasProducingTasks());
		produced = state.getProduced();
		assertFalse(state.hasTasks());
		assertFalse(state.hasRunningTasks());
		assertFalse(state.hasProducingTasks());
		assertEquals(2, produced.length);
		assertEquals(8, id(produced[0]));
		assertEquals(9, id(produced[1]));
		assertEquals(0, state.getProduced().length);
		assertNull(state.getTask());
		
		state.produce(handshake(10));
		state.prepare(handshake(11));
		state.addTask(new Task(false,12));
		assertTrue(state.hasTasks());
		assertFalse(state.hasRunningTasks());
		assertFalse(state.hasProducingTasks());
		state.addTask(new Task(13));
		task = state.getTask();
		Runnable task2 = state.getTask();
		assertNull(state.getTask());
		produced = state.getProduced();
		assertEquals(1, produced.length);
		assertEquals(10, id(produced[0]));
		assertEquals(0, state.getProduced().length);
		task2.run();
		assertEquals(0, state.getProduced().length);
		task.run();
		produced = state.getProduced();
		assertEquals(3, produced.length);
		assertEquals(11, id(produced[0]));
		assertEquals(12, id(produced[1]));
		assertEquals(13, id(produced[2]));
		assertEquals(0, state.getProduced().length);

		Exception e = new Exception();
		state.addTask(new Task(e, 14));
		assertEquals(0, state.getProduced().length);
		task = state.getTask();
		assertEquals(0, state.getProduced().length);
		task.run();
		try {
			state.getProduced();
			fail();
		} catch (InternalErrorAlert e2) {
			assertSame(e, e2.getCause());
		}
		try {
			task.run();
			fail();
		} catch (IllegalStateException e2) {}
	}
	
	static class Task extends AbstractEngineTask {

		int[] ids;
		
		Exception e;
		
		boolean producing = true;
		
		Task(int... ids) {
			this.ids = ids;
		}

		Task(boolean producing, int... ids) {
			this.producing = producing;
			this.ids = ids;
		}
		
		Task(Exception e, int... ids) {
			this.e = e;
			this.ids = ids;
		}
		
		@Override
		public boolean isProducing() {
			return producing;
		}

		@Override
		public void finish(EngineState state) throws Alert {
			for (int id: ids) {
				state.prepare(handshake(id));
			}
		}

		@Override
		void execute() throws Exception {
			if (e != null) {
				throw e;
			}
		}

		@Override
		public String name() {
			return "Test";
		}
	}
}
