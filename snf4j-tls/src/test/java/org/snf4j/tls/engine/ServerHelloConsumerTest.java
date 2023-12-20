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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.snf4j.tls.cipher.CipherSuite.TLS_AES_128_GCM_SHA256;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.snf4j.tls.alert.IllegalParameterAlert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.alert.MissingExtensionAlert;
import org.snf4j.tls.alert.ProtocolVersionAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.cipher.HashSpec;
import org.snf4j.tls.crypto.Hkdf;
import org.snf4j.tls.crypto.IHash;
import org.snf4j.tls.crypto.IHkdf;
import org.snf4j.tls.crypto.ITranscriptHash;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.crypto.TranscriptHash;
import org.snf4j.tls.extension.CookieExtension;
import org.snf4j.tls.extension.ExtensionType;
import org.snf4j.tls.extension.ExtensionsUtil;
import org.snf4j.tls.extension.ICookieExtension;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.IKeyShareExtension;
import org.snf4j.tls.extension.KeyShareExtension;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.PreSharedKeyExtension;
import org.snf4j.tls.handshake.ClientHello;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.handshake.ServerHello;
import org.snf4j.tls.handshake.ServerHelloRandom;
import org.snf4j.tls.session.ISession;
import org.snf4j.tls.session.SessionTicket;

public class ServerHelloConsumerTest extends EngineTest {

	HandshakeEngine engine;
	
	ClientHello ch;
	
	EngineState state;
	
	List<IExtension> extensions;
	
	@Override
	public void before() throws Exception {
		super.before();
		params.delegatedTaskMode = DelegatedTaskMode.CERTIFICATES;
		engine = new HandshakeEngine(true, params, handler, handler);
		engine.start();
		ch = (ClientHello) engine.produce()[0].getHandshake();
		state = state(engine);
		
		extensions = new ArrayList<IExtension>();
		extensions.add(versions(0x0304));
		extensions.add(keyShare(NamedGroup.SECP256R1));
	}
	
	ByteBuffer[] data(ServerHello ch, int... sizes) {
		ch.getBytes(buffer);
		byte[] data = buffer();
		buffer.clear();
		return array(data,0,sizes);
	}

	EngineState state(HandshakeEngine engine) throws Exception {
		Field f = HandshakeEngine.class.getDeclaredField("state");
		
		f.setAccessible(true);
		return (EngineState) f.get(engine);
	}
	
	ServerHello serverHello(int legacyVersion) {
		return new ServerHello(legacyVersion, random, legacySessionId, TLS_AES_128_GCM_SHA256, (byte)0, extensions);
	}

	ServerHello serverHello(byte[] legacySessionId) {
		return new ServerHello(0x0303, random, legacySessionId, TLS_AES_128_GCM_SHA256, (byte)0, extensions);
	}

	ServerHello serverHello(CipherSuite suite) {
		return new ServerHello(0x0303, random, legacySessionId, suite, (byte)0, extensions);
	}

	ServerHello serverHello(byte legacyCompression) {
		return new ServerHello(0x0303, random, legacySessionId, TLS_AES_128_GCM_SHA256, legacyCompression, extensions);
	}
	
	ServerHello serverHelloEx(IExtension... extensions) {
		return new ServerHello(0x0303, random, legacySessionId, TLS_AES_128_GCM_SHA256, (byte)0, Arrays.asList(extensions));
	}
	
	@Test
	public void testConsume() throws Exception {
		ServerHello sh = serverHello(0x0303);
		new ServerHelloConsumer().consume(state, sh, data(sh), false);
		assertSame(MachineState.CLI_WAIT_EE, state.getState());
		assertEquals(0, state.getProduced().length);
	}
	
	@Test
	public void testConsumeHRRMoreSharedGroups() throws Exception {
		params.numberOfOfferedSharedKeys = 2;
		engine = new HandshakeEngine(true, params, handler, handler);
		engine.start();
		ch = (ClientHello) engine.produce()[0].getHandshake();
		state = state(engine);
		
		params.delegatedTaskMode = DelegatedTaskMode.ALL;
		random = ServerHelloRandom.getHelloRetryRequestRandom();
		replace(extensions, new KeyShareExtension(NamedGroup.SECP384R1));
		ServerHello sh = serverHello(0x0303);
		assertNotNull(state.getPrivateKey(NamedGroup.SECP256R1));
		assertNotNull(state.getPrivateKey(NamedGroup.SECP384R1));
		new ServerHelloConsumer().consume(state, sh, data(sh), true);
		assertSame(MachineState.CLI_WAIT_1_SH, state.getState());
		ProducedHandshake[] produced = state.getProduced();
		assertEquals(1, produced.length);
		assertNull(state.getPrivateKey(NamedGroup.SECP256R1));
		assertNotNull(state.getPrivateKey(NamedGroup.SECP384R1));
		ClientHello ch = (ClientHello) produced[0].getHandshake();
		IKeyShareExtension keyShare = ExtensionsUtil.find(ch, ExtensionType.KEY_SHARE);
		assertEquals(1, keyShare.getEntries().length);
		assertSame(NamedGroup.SECP384R1, keyShare.getEntries()[0].getNamedGroup());
		assertSame(this.ch, ch);
	}

	@Test
	public void testConsumeHRRMoreSharedGroupsWithCompatibilityMode() throws Exception {
		params.numberOfOfferedSharedKeys = 2;
		params.compatibilityMode = true;
		engine = new HandshakeEngine(true, params, handler, handler);
		engine.start();
		ch = (ClientHello) engine.produce()[0].getHandshake();
		state = state(engine);
		
		params.delegatedTaskMode = DelegatedTaskMode.ALL;
		random = ServerHelloRandom.getHelloRetryRequestRandom();
		replace(extensions, new KeyShareExtension(NamedGroup.SECP384R1));
		legacySessionId = ch.getLegacySessionId().clone();
		ServerHello sh = serverHello(0x0303);
		assertNotNull(state.getPrivateKey(NamedGroup.SECP256R1));
		assertNotNull(state.getPrivateKey(NamedGroup.SECP384R1));
		assertEquals("", handler.trace());
		new ServerHelloConsumer().consume(state, sh, data(sh), true);
		assertEquals("prodCCS|", handler.trace());
		assertSame(MachineState.CLI_WAIT_1_SH, state.getState());
		ProducedHandshake[] produced = state.getProduced();
		assertEquals(1, produced.length);
		assertNull(state.getPrivateKey(NamedGroup.SECP256R1));
		assertNotNull(state.getPrivateKey(NamedGroup.SECP384R1));
		ClientHello ch = (ClientHello) produced[0].getHandshake();
		IKeyShareExtension keyShare = ExtensionsUtil.find(ch, ExtensionType.KEY_SHARE);
		assertEquals(1, keyShare.getEntries().length);
		assertSame(NamedGroup.SECP384R1, keyShare.getEntries()[0].getNamedGroup());
		assertSame(this.ch, ch);
	}

	@Test
	public void testConsumeHRRMoreSharedGroupsWithCompatibilityModeAndEarlyData() throws Exception {
		params.numberOfOfferedSharedKeys = 2;
		params.compatibilityMode = true;
		engine = new HandshakeEngine(true, params, handler, handler);
		engine.start();
		ch = (ClientHello) engine.produce()[0].getHandshake();
		state = state(engine);
		
		params.delegatedTaskMode = DelegatedTaskMode.ALL;
		random = ServerHelloRandom.getHelloRetryRequestRandom();
		replace(extensions, new KeyShareExtension(NamedGroup.SECP384R1));
		legacySessionId = ch.getLegacySessionId().clone();
		ServerHello sh = serverHello(0x0303);
		assertNotNull(state.getPrivateKey(NamedGroup.SECP256R1));
		assertNotNull(state.getPrivateKey(NamedGroup.SECP384R1));
		assertEquals("", handler.trace());
		state.setEarlyDataContext(new EarlyDataContext(sh.getCipherSuite(), 100));
		new ServerHelloConsumer().consume(state, sh, data(sh), true);
		assertEquals("", handler.trace());
		assertSame(MachineState.CLI_WAIT_1_SH, state.getState());
		ProducedHandshake[] produced = state.getProduced();
		assertEquals(1, produced.length);
		assertNull(state.getPrivateKey(NamedGroup.SECP256R1));
		assertNotNull(state.getPrivateKey(NamedGroup.SECP384R1));
		ClientHello ch = (ClientHello) produced[0].getHandshake();
		IKeyShareExtension keyShare = ExtensionsUtil.find(ch, ExtensionType.KEY_SHARE);
		assertEquals(1, keyShare.getEntries().length);
		assertSame(NamedGroup.SECP384R1, keyShare.getEntries()[0].getNamedGroup());
		assertSame(this.ch, ch);
	}
	
	@Test
	public void testConsumeHRR() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.ALL;
		random = ServerHelloRandom.getHelloRetryRequestRandom();
		replace(extensions, new KeyShareExtension(NamedGroup.SECP384R1));
		ServerHello sh = serverHello(0x0303);
		assertNotNull(state.getPrivateKey(NamedGroup.SECP256R1));
		new ServerHelloConsumer().consume(state, sh, data(sh), true);
		assertSame(MachineState.CLI_WAIT_TASK, state.getState());
		state.getTask().run();
		ProducedHandshake[] produced = state.getProduced();
		assertSame(MachineState.CLI_WAIT_2_SH, state.getState());
		assertEquals(1, produced.length);
		assertNull(state.getPrivateKey(NamedGroup.SECP256R1));
		assertNotNull(state.getPrivateKey(NamedGroup.SECP384R1));
		ClientHello ch = (ClientHello) produced[0].getHandshake();
		IKeyShareExtension keyShare = ExtensionsUtil.find(ch, ExtensionType.KEY_SHARE);
		assertEquals(1, keyShare.getEntries().length);
		assertSame(NamedGroup.SECP384R1, keyShare.getEntries()[0].getNamedGroup());
		assertSame(this.ch, ch);
	}

	@Test
	public void testConsumeHRRNoTask() throws Exception {
		random = ServerHelloRandom.getHelloRetryRequestRandom();
		replace(extensions, new KeyShareExtension(NamedGroup.SECP384R1));
		ServerHello sh = serverHello(0x0303);
		assertNotNull(state.getPrivateKey(NamedGroup.SECP256R1));
		new ServerHelloConsumer().consume(state, sh, data(sh), true);
		assertSame(MachineState.CLI_WAIT_2_SH, state.getState());
		ProducedHandshake[] produced = state.getProduced();
		assertEquals(1, produced.length);
		assertNull(state.getPrivateKey(NamedGroup.SECP256R1));
		assertNotNull(state.getPrivateKey(NamedGroup.SECP384R1));
		ClientHello ch = (ClientHello) produced[0].getHandshake();
		IKeyShareExtension keyShare = ExtensionsUtil.find(ch, ExtensionType.KEY_SHARE);
		assertEquals(1, keyShare.getEntries().length);
		assertSame(NamedGroup.SECP384R1, keyShare.getEntries()[0].getNamedGroup());
		assertSame(this.ch, ch);
	}
	
	@Test
	public void testConsumeHRRWithCookieNoTask() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.ALL;
		random = ServerHelloRandom.getHelloRetryRequestRandom();
		extensions.add(new CookieExtension(bytes(1,2,3,4,5,6,7,8,9,10)));
		replace(extensions, new KeyShareExtension(NamedGroup.SECP256R1));
		ServerHello sh = serverHello(0x0303);
		new ServerHelloConsumer().consume(state, sh, data(sh), true);
		ProducedHandshake[] produced = state.getProduced();
		assertEquals(1, produced.length);
		ClientHello ch = (ClientHello) produced[0].getHandshake();
		IKeyShareExtension keyShare = ExtensionsUtil.find(ch, ExtensionType.KEY_SHARE);
		assertEquals(1, keyShare.getEntries().length);
		assertSame(NamedGroup.SECP256R1, keyShare.getEntries()[0].getNamedGroup());
		ICookieExtension cookie = ExtensionsUtil.find(ch, ExtensionType.COOKIE);
		assertArrayEquals(bytes(1,2,3,4,5,6,7,8,9,10), cookie.getCookie());
		assertSame(this.ch, ch);
	}
	
	@Test
	public void testConsumeHRRWithCookie() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.ALL;
		random = ServerHelloRandom.getHelloRetryRequestRandom();
		extensions.add(new CookieExtension(bytes(1,2,3,4,5,6,7,8,9,10)));
		replace(extensions, new KeyShareExtension(NamedGroup.SECP384R1));
		ServerHello sh = serverHello(0x0303);
		new ServerHelloConsumer().consume(state, sh, data(sh), true);
		AbstractEngineTask task = (AbstractEngineTask) state.getTask();
		assertEquals("Key exchange", task.name());
		assertTrue(task.isProducing());
		task.run();
		ProducedHandshake[] produced = state.getProduced();
		assertEquals(1, produced.length);
		ClientHello ch = (ClientHello) produced[0].getHandshake();
		IKeyShareExtension keyShare = ExtensionsUtil.find(ch, ExtensionType.KEY_SHARE);
		assertEquals(1, keyShare.getEntries().length);
		assertSame(NamedGroup.SECP384R1, keyShare.getEntries()[0].getNamedGroup());
		ICookieExtension cookie = ExtensionsUtil.find(ch, ExtensionType.COOKIE);
		assertArrayEquals(bytes(1,2,3,4,5,6,7,8,9,10), cookie.getCookie());
		assertSame(this.ch, ch);
	}

	@Test
	public void testConsumeHRRWithNoKeyShareInStoredClientHello() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.ALL;
		random = ServerHelloRandom.getHelloRetryRequestRandom();
		extensions.add(new CookieExtension(bytes(1,2,3,4,5,6,7,8,9,10)));
		replace(extensions, new KeyShareExtension(NamedGroup.SECP384R1));
		ServerHello sh = serverHello(0x0303);
		new ServerHelloConsumer().consume(state, sh, data(sh), true);
		AbstractEngineTask task = (AbstractEngineTask) state.getTask();
		assertEquals("Key exchange", task.name());
		assertTrue(task.isProducing());
		remove(state.getClientHello().getExtensions(), ExtensionType.KEY_SHARE);
		task.run();
		try {
			state.getProduced();
			fail();
		}
		catch (InternalErrorAlert e) {
			assertEquals("No key share extension in stored ClientHello", e.getMessage());
		}
	}
	
	@Test
	public void testConsumeHRRAndSH() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.ALL;
		random = ServerHelloRandom.getHelloRetryRequestRandom();
		replace(extensions, new KeyShareExtension(NamedGroup.SECP384R1));
		ServerHello sh = serverHello(0x0303);
		assertNotNull(state.getPrivateKey(NamedGroup.SECP256R1));
		new ServerHelloConsumer().consume(state, sh, data(sh), true);
		assertSame(MachineState.CLI_WAIT_TASK, state.getState());
		state.getTask().run();
		ProducedHandshake[] produced = state.getProduced();
		assertSame(MachineState.CLI_WAIT_2_SH, state.getState());
		assertEquals(1, produced.length);
		replace(extensions, keyShare(NamedGroup.SECP384R1));
		random = bytes(32, (byte)1, (byte)2, (byte)3);
		sh = serverHello(0x0303);
		new ServerHelloConsumer().consume(state, sh, data(sh), false);
		assertSame(MachineState.CLI_WAIT_EE, state.getState());
	}
	
	@Test(expected = UnexpectedMessageAlert.class)
	public void testConsumeHRRAndHRR() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.ALL;
		random = ServerHelloRandom.getHelloRetryRequestRandom();
		replace(extensions, new KeyShareExtension(NamedGroup.SECP384R1));
		ServerHello sh = serverHello(0x0303);
		assertNotNull(state.getPrivateKey(NamedGroup.SECP256R1));
		new ServerHelloConsumer().consume(state, sh, data(sh), true);
		assertSame(MachineState.CLI_WAIT_TASK, state.getState());
		state.getTask().run();
		ProducedHandshake[] produced = state.getProduced();
		assertSame(MachineState.CLI_WAIT_2_SH, state.getState());
		assertEquals(1, produced.length);
		replace(extensions, new KeyShareExtension(NamedGroup.SECP256R1));
		sh = serverHello(0x0303);
		new ServerHelloConsumer().consume(state, sh, data(sh), true);
	}
	
	@Test(expected = IllegalParameterAlert.class)
	public void testConsumeHRRUnexpectedNamedGroup() throws Exception {
		random = ServerHelloRandom.getHelloRetryRequestRandom();
		replace(extensions, new KeyShareExtension(NamedGroup.FFDHE3072));
		ServerHello sh = serverHello(0x0303);
		new ServerHelloConsumer().consume(state, sh, data(sh), true);
	}
	
	@Test(expected = InternalErrorAlert.class)
	public void testConsumeHRRNoKeyShareInStoredClientHello() throws Exception {
		random = ServerHelloRandom.getHelloRetryRequestRandom();
		ServerHello sh = serverHello(0x0303);
		remove(state.getClientHello().getExtensions(), ExtensionType.KEY_SHARE);
		new ServerHelloConsumer().consume(state, sh, data(sh), true);
	}
	
	@Test(expected = IllegalParameterAlert.class)
	public void testConsumeHRRNoChange() throws Exception {
		random = ServerHelloRandom.getHelloRetryRequestRandom();
		replace(extensions, new KeyShareExtension(NamedGroup.SECP256R1));
		ServerHello sh = serverHello(0x0303);
		new ServerHelloConsumer().consume(state, sh, data(sh), true);
	}

	@Test(expected = IllegalParameterAlert.class)
	public void testConsumeHRRNoChange2() throws Exception {
		random = ServerHelloRandom.getHelloRetryRequestRandom();
		remove(extensions, ExtensionType.KEY_SHARE);
		ServerHello sh = serverHello(0x0303);
		new ServerHelloConsumer().consume(state, sh, data(sh), true);
	}
	
	@Test(expected = IllegalParameterAlert.class)
	public void testConsumeUnexpectedNamedGroup() throws Exception {
		replace(extensions, keyShare(NamedGroup.FFDHE2048));
		ServerHello sh = serverHello(0x0303);
		new ServerHelloConsumer().consume(state, sh, data(sh), false);
	}
	
	@Test(expected = InternalErrorAlert.class)
	public void testConsumeEx1() throws Exception {
		ServerHello sh = serverHello(0x0303);
		handler.onHTSException = new InternalErrorAlert("");
		new ServerHelloConsumer().consume(state, sh, data(sh), false);
	}

	@Test(expected = InternalErrorAlert.class)
	public void testConsumeEx2() throws Exception {
		ServerHello sh = serverHello(0x0303);
		handler.getSecureRandomException = new NullPointerException();
		new ServerHelloConsumer().consume(state, sh, data(sh), false);
	}
	
	@Test(expected = UnexpectedMessageAlert.class)
	public void testInvalidState() throws Exception {
		ServerHello sh = serverHello(0x0303);
		state.changeState(MachineState.CLI_CONNECTED);
		new ServerHelloConsumer().consume(state, sh, data(sh), false);
	}
	
	@Test(expected = ProtocolVersionAlert.class)
	public void testInvalidLegacyVersion() throws Exception {
		ServerHello sh = serverHello(0x0302);
		new ServerHelloConsumer().consume(state, sh, data(sh), false);
	}

	@Test(expected = IllegalParameterAlert.class)
	public void testInvalidLegacySessionId() throws Exception {
		ServerHello sh = serverHello(new byte[this.legacySessionId.length+1]);
		new ServerHelloConsumer().consume(state, sh, data(sh), false);
	}

	@Test(expected = IllegalParameterAlert.class)
	public void testInvalidLegacySessionId2() throws Exception {
		params.compatibilityMode = true;
		engine = new HandshakeEngine(true, params, handler, handler);
		engine.start();
		ch = (ClientHello) engine.produce()[0].getHandshake();
		state = state(engine);
		legacySessionId = ch.getLegacySessionId().clone();
		legacySessionId[0]++;
		ServerHello sh = serverHello(legacySessionId);
		new ServerHelloConsumer().consume(state, sh, data(sh), false);
	}
	
	@Test(expected = IllegalParameterAlert.class)
	public void testInvalidCipherSuite() throws Exception {
		ServerHello sh = serverHello(CipherSuite.TLS_CHACHA20_POLY1305_SHA256);
		new ServerHelloConsumer().consume(state, sh, data(sh), false);
	}

	@Test(expected = IllegalParameterAlert.class)
	public void testInvalidLegacyCompression() throws Exception {
		ServerHello sh = serverHello((byte)1);
		new ServerHelloConsumer().consume(state, sh, data(sh), false);
	}
	
	@Test(expected = ProtocolVersionAlert.class)
	public void testNoSupportedVersions() throws Exception {
		remove(extensions, ExtensionType.SUPPORTED_VERSIONS);
		ServerHello sh = serverHello(0x0303);
		new ServerHelloConsumer().consume(state, sh, data(sh), false);
	}

	@Test(expected = IllegalParameterAlert.class)
	public void testInvalidSupportedVersions() throws Exception {
		replace(extensions, versions(0x0303));
		ServerHello sh = serverHello(0x0303);
		new ServerHelloConsumer().consume(state, sh, data(sh), false);
	}

	@Test(expected = MissingExtensionAlert.class)
	public void testNoKeyshare() throws Exception {
		remove(extensions, ExtensionType.KEY_SHARE);
		ServerHello sh = serverHello(0x0303);
		new ServerHelloConsumer().consume(state, sh, data(sh), false);
	}
	
	@Test
	public void testRemoveNoPskKeySchedule() throws Exception {
		EngineState state = new EngineState(
				MachineState.CLI_INIT, 
				new TestParameters(), 
				new TestHandshakeHandler(),
				new TestHandshakeHandler());
		IHash hash = HashSpec.SHA256.getHash();
		ITranscriptHash th = new TranscriptHash(hash.createMessageDigest());
		IHkdf hkdf = new Hkdf(hash.createMac());
		
		KeySchedule ks1 = new KeySchedule(hkdf, th, HashSpec.SHA256);
		KeySchedule ks2 = new KeySchedule(hkdf, th, HashSpec.SHA256);
		KeySchedule ks3 = new KeySchedule(hkdf, th, HashSpec.SHA256);

		assertNull(ServerHelloConsumer.removeNoPskKeySchedule(state));
		
		SessionTicket st = new SessionTicket(CipherSuite.TLS_AES_128_GCM_SHA256, null, bytes(1), bytes(2), 0, 0, -1);
		state.addPskContext(new PskContext(ks1, st));
		state.addPskContext(new PskContext(ks2, st));
		
		assertEquals(2, state.getPskContexts().size());
		assertNull(ServerHelloConsumer.removeNoPskKeySchedule(state));
		assertEquals(2, state.getPskContexts().size());
		state.addPskContext(new PskContext(ks3));
		
		assertEquals(3, state.getPskContexts().size());
		KeySchedule ks = ServerHelloConsumer.removeNoPskKeySchedule(state);
		assertSame(ks3, ks);
		assertEquals(2, state.getPskContexts().size());
		assertSame(ks1, state.getPskContexts().get(0).getKeySchedule());
		assertSame(ks2, state.getPskContexts().get(1).getKeySchedule());
	}
	
	@Test
	public void testEarlyDataWithHRRAndNoEarlyDataInFirstClientHello() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		params.peerHost = "snf4j.org";
		params.peerPort = 80;
		params.cipherSuites = new CipherSuite[] {
				CipherSuite.TLS_AES_256_GCM_SHA384,
				};
		handler.ticketInfos = new TicketInfo[] {new TicketInfo(100)};
		handler.earlyData.add(bytes(1,2));
		HandshakeEngine cli = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);

		HandshakeController c = new HandshakeController(cli, srv);
		c.run(false, null);
		ISession session = cli.getState().getSession();
		assertEquals(1, session.getManager().getTickets(session).length);
		
		TestParameters params2 = new TestParameters();
		params2.delegatedTaskMode = DelegatedTaskMode.NONE;
		params2.namedGroups = new NamedGroup[] {
				NamedGroup.SECP384R1
				};
		cli = new HandshakeEngine(true, params, handler, handler);
		srv = new HandshakeEngine(false, params2, handler, handler);
		c = new HandshakeController(cli, srv);
		c.run(false, HandshakeType.SERVER_HELLO);
		IHandshake h = c.get(false);
		assertNotNull(h);
		remove(((EngineState)cli.getState()).getClientHello().getExtensions(), ExtensionType.EARLY_DATA);
		h.prepare();
		session = cli.getState().getSession();
		assertEquals(1, session.getManager().getTickets(session).length);
		assertEquals("client_hello|EarlyData|", c.trace(false));
		c.run(false, null);
		assertEquals("client_hello|finished|", c.trace(false));
	}

	@Test
	public void testPskWithHRR() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		params.peerHost = "snf4j.org";
		params.peerPort = 80;
		params.numberOfOfferedSharedKeys = 2;
		params.namedGroups = new NamedGroup[] {
				NamedGroup.SECP256R1, NamedGroup.SECP384R1
				};
		params.cipherSuites = new CipherSuite[] {
				CipherSuite.TLS_AES_256_GCM_SHA384,
				};
		handler.ticketInfos = new TicketInfo[] {new TicketInfo(100)};
		HandshakeEngine cli = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);

		HandshakeController c = new HandshakeController(cli, srv);
		c.run(false, null);
		ISession session = cli.getState().getSession();
		assertEquals(1, session.getManager().getTickets(session).length);
		
		TestParameters params2 = new TestParameters();
		params2.delegatedTaskMode = DelegatedTaskMode.NONE;
		params2.namedGroups = new NamedGroup[] {
				NamedGroup.SECP384R1
				};
		cli = new HandshakeEngine(true, params, handler, handler);
		srv = new HandshakeEngine(false, params2, handler, handler);
		c = new HandshakeController(cli, srv);
		c.run(false, HandshakeType.SERVER_HELLO);
		ClientHello ch1 = (ClientHello)((EngineState)cli.getState()).getClientHello();
		assertNotNull(ch1);
		ServerHello h = (ServerHello) c.get(false);
		assertNotNull(h);
		List<IExtension> extensions = new ArrayList<IExtension>();
		
		ServerHello hrr = new ServerHello(
				h.getLegacyVersion(), 
				ServerHelloRandom.getHelloRetryRequestRandom(),
				h.getLegacySessionId(),
				h.getCipherSuite(),
				h.getLegacyCompressionMethod(),
				extensions);
		extensions.add(ExtensionsUtil.find(h, ExtensionType.SUPPORTED_VERSIONS));
		extensions.add(new KeyShareExtension(NamedGroup.SECP384R1));
		extensions.add(new CookieExtension(bytes(1,2,3,4,5)));
		c.clear(false);
		c.add(false, hrr);
		c.run(true, HandshakeType.CLIENT_HELLO);
		ClientHello ch2 = (ClientHello) c.get(true);
		assertNotNull(ch2);
		
		assertNotNull(ExtensionsUtil.find(ch2, ExtensionType.PRE_SHARED_KEY));
		assertNotNull(ExtensionsUtil.find(ch2, ExtensionType.COOKIE));
	}
	
	@Test
	public void testEarlyDataWithHRR2() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		params.peerHost = "snf4j.org";
		params.peerPort = 80;
		params.numberOfOfferedSharedKeys = 1;
		params.cipherSuites = new CipherSuite[] {
				CipherSuite.TLS_AES_256_GCM_SHA384,
				};
		handler.ticketInfos = new TicketInfo[] {new TicketInfo(100)};
		handler.earlyData.add(bytes(1,2));
		HandshakeEngine cli = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);

		HandshakeController c = new HandshakeController(cli, srv);
		c.run(false, null);
		ISession session = cli.getState().getSession();
		assertEquals(1, session.getManager().getTickets(session).length);
		
		TestParameters params2 = new TestParameters();
		params2.delegatedTaskMode = DelegatedTaskMode.NONE;
		params2.namedGroups = new NamedGroup[] {
				NamedGroup.SECP384R1
				};
		cli = new HandshakeEngine(true, params, handler, handler);
		srv = new HandshakeEngine(false, params2, handler, handler);
		c = new HandshakeController(cli, srv);
		c.run(false, HandshakeType.SERVER_HELLO);
		IHandshake h = c.get(false);
		assertNotNull(h);
		remove(((EngineState)cli.getState()).getClientHello().getExtensions(), ExtensionType.EARLY_DATA);
		h.prepare();
		session = cli.getState().getSession();
		assertEquals(1, session.getManager().getTickets(session).length);
		assertEquals("client_hello|EarlyData|", c.trace(false));
		c.run(false, null);
		assertEquals("client_hello|finished|", c.trace(false));
	}
	
	@Test
	public void testFailingPskBinding() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		params.peerHost = "snf4j.org";
		params.peerPort = 80;
		params.numberOfOfferedSharedKeys = 1;
		params.cipherSuites = new CipherSuite[] {
				CipherSuite.TLS_AES_256_GCM_SHA384,
				};
		handler.ticketInfos = new TicketInfo[] {new TicketInfo(100)};
		handler.earlyData.add(bytes(1,2));
		HandshakeEngine cli = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);

		HandshakeController c = new HandshakeController(cli, srv);
		c.run(false, null);
		ISession session = cli.getState().getSession();
		assertEquals(1, session.getManager().getTickets(session).length);
		
		TestParameters params2 = new TestParameters();
		params2.delegatedTaskMode = DelegatedTaskMode.NONE;
		params2.namedGroups = new NamedGroup[] {
				NamedGroup.SECP384R1
				};
		cli = new HandshakeEngine(true, params, handler, handler);
		srv = new HandshakeEngine(false, params2, handler, handler);
		c = new HandshakeController(cli, srv);
		c.run(false, HandshakeType.SERVER_HELLO);
		IHandshake h = c.get(false);
		assertNotNull(h);
		List<PskContext> psks = ((EngineState)cli.getState()).getPskContexts();
		assertEquals(1, psks.size());
		PskContext psk = psks.get(0);
		PskContext newPsk = new PskContext(psk.getKeySchedule(), psk.getTicket()) {
			int count = 3;
			
			@Override
			public KeySchedule getKeySchedule() {
				if (count-- == 0) {
					throw new NullPointerException();
				}
				return super.getKeySchedule();
			}
		};
		psks.set(0, newPsk);
		try {
			c.run(false, null);
			fail();
		}
		catch (InternalErrorAlert e) {
			assertEquals("Failed to bind PSK", e.getCause().getMessage());
		}
	}	

	@Test
	public void testUnexpectedPskExtension() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		HandshakeEngine cli = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);

		HandshakeController c = new HandshakeController(cli, srv);
		c.run(false, HandshakeType.SERVER_HELLO);
		ServerHello sh = (ServerHello) c.get(false);
		assertNotNull(sh);
		sh.getExtensions().add(new PreSharedKeyExtension(0));
		sh.prepare();
		try {
			c.run(false, null);
			fail();
		}
		catch (IllegalParameterAlert e) {
			assertEquals("Unexpected pre_shared_key extension", e.getMessage());
		}
	}
	
	@Test
	public void testInvalidSelectedIdentity() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		params.peerHost = "snf4j.org";
		params.peerPort = 80;
		handler.ticketInfos = new TicketInfo[] {new TicketInfo(100)};
		handler.earlyData.add(bytes(1,2));
		HandshakeEngine cli = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);

		HandshakeController c = new HandshakeController(cli, srv);
		c.run(false, null);
		ISession session = cli.getState().getSession();
		assertEquals(1, session.getManager().getTickets(session).length);
		
		cli = new HandshakeEngine(true, params, handler, handler);
		srv = new HandshakeEngine(false, params, handler, handler);
		c = new HandshakeController(cli, srv);
		c.run(false, HandshakeType.SERVER_HELLO);
		IHandshake h = c.get(false);
		assertNotNull(h);
		PreSharedKeyExtension psk = ExtensionsUtil.find(h, ExtensionType.PRE_SHARED_KEY);
		assertNotNull(psk);
		remove(h.getExtensions(), ExtensionType.PRE_SHARED_KEY);
		h.getExtensions().add(new PreSharedKeyExtension(psk.getSelectedIdentity()+1));
		h.prepare();
		try {
			c.run(false, null);
			fail();
		}
		catch (IllegalParameterAlert e) {
			assertEquals("Invalid selected identity", e.getMessage());
		}
	}	

	@Test
	public void testIncompatibleHashAssociatedWithPsk() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		params.peerHost = "snf4j.org";
		params.peerPort = 80;
		HandshakeEngine cli = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);

		HandshakeController c = new HandshakeController(cli, srv);
		c.run(false, null);
		ISession session = cli.getState().getSession();
		assertEquals(1, session.getManager().getTickets(session).length);
		
		cli = new HandshakeEngine(true, params, handler, handler);
		srv = new HandshakeEngine(false, params, handler, handler);
		c = new HandshakeController(cli, srv);
		c.run(false, HandshakeType.SERVER_HELLO);
		ServerHello sh = (ServerHello)c.get(false);
		assertNotNull(sh);
		ServerHello newSh = new ServerHello(
				sh.getLegacyVersion(),
				sh.getRandom(),
				sh.getLegacySessionId(),
				CipherSuite.TLS_AES_128_GCM_SHA256,
				sh.getLegacyCompressionMethod(),
				sh.getExtensions());
		c.set(false, newSh);
		try {
			c.run(false, null);
			fail();
		}
		catch (IllegalParameterAlert e) {
			assertEquals("Incompatible Hash associated with PSK", e.getMessage());
		}
	}	

	@Test
	public void testNoMatchingHashForExistingPskAndHRR() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		params.peerHost = "snf4j.org";
		params.peerPort = 80;
		params.cipherSuites = new CipherSuite[] {CipherSuite.TLS_AES_256_GCM_SHA384};
		HandshakeEngine cli = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);

		HandshakeController c = new HandshakeController(cli, srv);
		c.run(false, null);
		ISession session = cli.getState().getSession();
		assertEquals(1, session.getManager().getTickets(session).length);

		params.cipherSuites = new CipherSuite[] {CipherSuite.TLS_AES_256_GCM_SHA384, CipherSuite.TLS_AES_128_GCM_SHA256};
		TestParameters params2 = new TestParameters();
		params2.delegatedTaskMode = DelegatedTaskMode.NONE;
		params2.cipherSuites = new CipherSuite[] {CipherSuite.TLS_AES_128_GCM_SHA256};
		params2.namedGroups = new NamedGroup[] {NamedGroup.SECP384R1};
		cli = new HandshakeEngine(true, params, handler, handler);
		srv = new HandshakeEngine(false, params2, handler, handler);
		c = new HandshakeController(cli, srv);
		c.run(true, HandshakeType.CLIENT_HELLO);
		IHandshake h = c.get(true);
		assertNotNull(h);
		assertNotNull(ExtensionsUtil.find(h, ExtensionType.PRE_SHARED_KEY));
		c.run(false, HandshakeType.SERVER_HELLO);
		h = c.get(false);
		assertNotNull(h);
		c.run(true, HandshakeType.CLIENT_HELLO);
		h = c.get(true);
		assertNotNull(h);
		assertSame(HandshakeType.CLIENT_HELLO, h.getType());
		assertNull(ExtensionsUtil.find(h, ExtensionType.PRE_SHARED_KEY));
	}	
	
}
