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
import org.snf4j.tls.handshake.ClientHello;
import org.snf4j.tls.handshake.ServerHello;
import org.snf4j.tls.handshake.ServerHelloRandom;
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
		handler.onETSException = new NullPointerException();
		new ServerHelloConsumer().consume(state, sh, data(sh), false);
	}

	@Test(expected = InternalErrorAlert.class)
	public void testConsumeEx2() throws Exception {
		ServerHello sh = serverHello(0x0303);
		handler.onHTSException = new NullPointerException();
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
		
		SessionTicket st = new SessionTicket(HashSpec.SHA256, bytes(1), bytes(2), 0, 0);
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
}
