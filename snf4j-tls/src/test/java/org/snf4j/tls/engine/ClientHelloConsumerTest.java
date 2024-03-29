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

import static org.snf4j.tls.cipher.CipherSuite.TLS_AES_128_GCM_SHA256;
import static org.snf4j.tls.cipher.CipherSuite.TLS_AES_256_GCM_SHA384;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.snf4j.tls.cipher.CipherSuite.TLS_AES_128_CCM_8_SHA256;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.snf4j.tls.alert.DecryptErrorAlert;
import org.snf4j.tls.alert.HandshakeFailureAlert;
import org.snf4j.tls.alert.IllegalParameterAlert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.alert.MissingExtensionAlert;
import org.snf4j.tls.alert.ProtocolVersionAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.alert.UnrecognizedNameAlert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.EarlyDataExtension;
import org.snf4j.tls.extension.ExtensionDecoder;
import org.snf4j.tls.extension.ExtensionType;
import org.snf4j.tls.extension.ExtensionsUtil;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.KeyShareExtension;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.OfferedPsk;
import org.snf4j.tls.extension.PreSharedKeyExtension;
import org.snf4j.tls.extension.PskIdentity;
import org.snf4j.tls.extension.PskKeyExchangeMode;
import org.snf4j.tls.extension.PskKeyExchangeModesExtension;
import org.snf4j.tls.extension.ServerNameExtension;
import org.snf4j.tls.extension.SignatureScheme;
import org.snf4j.tls.extension.SupportedGroupsExtension;
import org.snf4j.tls.extension.SupportedVersionsExtension;
import org.snf4j.tls.handshake.CertificateType;
import org.snf4j.tls.handshake.ClientHello;
import org.snf4j.tls.handshake.EncryptedExtensions;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.ICertificate;
import org.snf4j.tls.handshake.ICertificateEntry;
import org.snf4j.tls.handshake.ICertificateVerify;
import org.snf4j.tls.handshake.IFinished;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.handshake.ServerHello;
import org.snf4j.tls.handshake.ServerHelloRandom;
import org.snf4j.tls.record.RecordType;
import org.snf4j.tls.session.SessionTicket;

public class ClientHelloConsumerTest extends EngineTest {

	ClientHelloConsumer consumer;
	
	List<IExtension> extensions;
	
	EngineState state;
	
	@Override
	public void before() throws Exception {
		super.before();
		consumer = new ClientHelloConsumer();
		
		extensions = new ArrayList<IExtension>();
		extensions.add(serverName("snf4j.org"));
		extensions.add(versions(0x0304));
		extensions.add(keyShare(NamedGroup.SECP256R1));
		extensions.add(groups(NamedGroup.SECP256R1, NamedGroup.SECP384R1));
		extensions.add(schemes(SignatureScheme.RSA_PKCS1_SHA256, SignatureScheme.RSA_PSS_RSAE_SHA256));
		state = new EngineState(MachineState.SRV_WAIT_1_CH,params, handler, handler);
	}
	
	ClientHello clientHello() {
		return new ClientHello(0x0303, random, legacySessionId, suites(TLS_AES_128_GCM_SHA256), new byte[1], extensions);
	}
	
	ClientHello clientHello(int legacyVersion) {
		return new ClientHello(legacyVersion, random, legacySessionId, suites(TLS_AES_128_GCM_SHA256), new byte[1], extensions);
	}

	ClientHello clientHelloCompress(byte[] compressions) {
		return new ClientHello(0x0303, random, legacySessionId, suites(TLS_AES_128_GCM_SHA256), compressions, extensions);
	}

	ClientHello clientHelloEx(IExtension... extensions) {
		return new ClientHello(0x0303, random, legacySessionId, suites(TLS_AES_128_GCM_SHA256), new byte[1], Arrays.asList(extensions));
	}

	ClientHello clientHelloCS(CipherSuite... suites) {
		return new ClientHello(0x0303, random, legacySessionId, suites, new byte[1], extensions);
	}
	
	@Test
	public void testNegotiateCipherSuite() throws Exception {
		replace(extensions, keyShare());
		ClientHello ch = clientHelloCS(TLS_AES_128_GCM_SHA256);
		consumer.consume(state, ch, data(ch), false);
		ServerHello sh = (ServerHello) state.getProduced()[0].getHandshake();
		assertTrue(ServerHelloRandom.isHelloRetryRequest(sh));
		assertSame(TLS_AES_128_GCM_SHA256, sh.getCipherSuite());

		state = new EngineState(MachineState.SRV_WAIT_1_CH,params, handler, handler);
		ch = clientHelloCS(TLS_AES_256_GCM_SHA384);
		consumer.consume(state, ch, data(ch), false);
		sh = (ServerHello) state.getProduced()[0].getHandshake();
		assertTrue(ServerHelloRandom.isHelloRetryRequest(sh));
		assertSame(TLS_AES_256_GCM_SHA384, sh.getCipherSuite());

		state = new EngineState(MachineState.SRV_WAIT_1_CH,params, handler, handler);
		ch = clientHelloCS(TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384);
		consumer.consume(state, ch, data(ch), false);
		sh = (ServerHello) state.getProduced()[0].getHandshake();
		assertTrue(ServerHelloRandom.isHelloRetryRequest(sh));
		assertSame(TLS_AES_256_GCM_SHA384, sh.getCipherSuite());
	}

	@Test
	public void testProducedServerHello() throws Exception {
		ClientHello ch = clientHelloCS(TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384);
		consumer.consume(state, ch, data(ch), false);
		assertEquals(0, state.getProduced().length);
		Runnable task1 = state.getTask();
		Runnable task2 = state.getTask();
		assertEquals("Key exchange", ((IEngineTask)task1).name());
		assertEquals("Certificate", ((IEngineTask)task2).name());
		assertNotNull(task1);
		assertNotNull(task2);
		assertNull(state.getTask());
		assertEquals(0, state.getProduced().length);
		task1.run();
		assertEquals(0, state.getProduced().length);
		task2.run();
		ProducedHandshake[] produced = state.getProduced();
		assertEquals(5, produced.length);
		ServerHello sh = (ServerHello) produced[0].getHandshake();
		assertSame(RecordType.INITIAL, produced[0].getRecordType());
		assertNotNull(sh);
		assertEquals(0x0303, sh.getLegacyVersion());
		assertEquals(TLS_AES_256_GCM_SHA384, sh.getCipherSuite());
		assertEquals(0, sh.getLegacyCompressionMethod());
		assertArrayEquals(ch.getLegacySessionId(), sh.getLegacySessionId());
		assertFalse(ServerHelloRandom.isHelloRetryRequest(sh.getRandom()));
		assertEquals(2, sh.getExtensions().size());
		KeyShareExtension kse = ExtensionsUtil.find(sh, ExtensionType.KEY_SHARE);
		assertEquals(1, kse.getEntries().length);
		assertSame(NamedGroup.SECP256R1, kse.getEntries()[0].getNamedGroup());
		SupportedVersionsExtension sve = ExtensionsUtil.find(sh, ExtensionType.SUPPORTED_VERSIONS);
		assertEquals(1, sve.getVersions().length);
		assertEquals(0x0304, sve.getVersions()[0]);
		
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		state = serverState();
		consumer.consume(state, ch, data(ch), false);
		produced = state.getProduced();
		assertEquals(5, produced.length);
		sh = (ServerHello) produced[0].getHandshake();
		assertNotNull(sh);
	}

	@Test
	public void testProducedEncryptedExtensions() throws Exception {
		params.namedGroups = new NamedGroup[] {NamedGroup.SECP256R1, NamedGroup.SECP521R1};
		replace(extensions, groups(NamedGroup.SECP256R1));
		ClientHello ch = clientHelloCS(TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384);
		consumer.consume(state, ch, data(ch), false);
		state.getTask().run();
		state.getTask().run();
		ProducedHandshake[] produced = state.getProduced();
		assertEquals(5, produced.length);
		EncryptedExtensions ee = (EncryptedExtensions) produced[1].getHandshake();
		assertEquals(2, ee.getExtensions().size());
		ServerNameExtension sne = ExtensionsUtil.find(ee, ExtensionType.SERVER_NAME);
		assertEquals("", sne.getHostName());
		SupportedGroupsExtension sge = ExtensionsUtil.find(ee, ExtensionType.SUPPORTED_GROUPS);
		assertEquals(2, sge.getGroups().length);
		assertSame(NamedGroup.SECP256R1, sge.getGroups()[0]);
		assertSame(NamedGroup.SECP521R1, sge.getGroups()[1]);
		
		params.namedGroups = new NamedGroup[] {NamedGroup.SECP256R1};
		remove(extensions, ExtensionType.SERVER_NAME);
		ch = clientHelloCS(TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384);
		state = serverState();
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		consumer.consume(state, ch, data(ch), false);
		produced = state.getProduced();
		assertEquals(5, produced.length);
		ee = (EncryptedExtensions) produced[1].getHandshake();
		assertEquals(1, ee.getExtensions().size());
		sge = ExtensionsUtil.find(ee, ExtensionType.SUPPORTED_GROUPS);
		assertEquals(1, sge.getGroups().length);
		assertSame(NamedGroup.SECP256R1, sge.getGroups()[0]);
	}

	@Test
	public void testProducedFinished() throws Exception {
		params.namedGroups = new NamedGroup[] {NamedGroup.SECP256R1, NamedGroup.SECP521R1};
		replace(extensions, groups(NamedGroup.SECP256R1));
		ClientHello ch = clientHelloCS(TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384);
		consumer.consume(state, ch, data(ch), false);
		state.getTask().run();
		state.getTask().run();
		ProducedHandshake[] produced = state.getProduced();
		assertEquals("ALPN(null)|PN(null)|VSN(snf4j.org)|CS|HTS|RTS(HANDSHAKE)|ATS|", handler.trace());
		CertificateCriteria criteria = handler.certificateSelector.criteria;
		assertEquals("snf4j.org", criteria.getHostName());
		assertTrue(criteria.isServer());
		assertSame(CertificateType.X509, criteria.getType());
		assertNull(criteria.getCertSchemes());
		assertEquals(2, criteria.getSchemes().length);
		assertSame(SignatureScheme.RSA_PKCS1_SHA256, criteria.getSchemes()[0]);
		assertSame(SignatureScheme.RSA_PSS_RSAE_SHA256, criteria.getSchemes()[1]);
		
		ICertificate cert = (ICertificate) produced[2].getHandshake();
		assertEquals(0, cert.getRequestContext().length);
		assertEquals(1, cert.getEntries().length);
		ICertificateEntry entry = cert.getEntries()[0];
		assertEquals(entry.getDataLength(), entry.getData().length);
		assertEquals(0, entry.getExtensions().size());
		assertArrayEquals(cert("rsasha256").getEncoded(), entry.getData());
		
		ICertificateVerify certv = (ICertificateVerify) produced[3].getHandshake();
		assertSame(SignatureScheme.RSA_PKCS1_SHA256, certv.getAlgorithm());
		byte[] signature = ConsumerUtil.sign(state.getTranscriptHash().getHash(HandshakeType.CERTIFICATE, false), 
				SignatureScheme.RSA_PKCS1_SHA256, 
				key("RSA", "rsa"), 
				false,
				RANDOM);
		assertArrayEquals(signature, certv.getSignature());
		assertNull(certv.getExtensions());
		
		IFinished finished = (IFinished) produced[4].getHandshake();
		assertArrayEquals(state.getKeySchedule().computeServerVerifyData(), finished.getVerifyData());

		extensions.add(certSchemes(SignatureScheme.ECDSA_SHA1));
		ch = clientHelloCS(TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384);
		handler.certificateSelector.certNames = new String[] {"rsasha256","rsasha384"};
		state = serverState();
		consumer.consume(state, ch, data(ch), false);
		state.getTask().run();
		state.getTask().run();
		produced = state.getProduced();
		criteria = handler.certificateSelector.criteria;
		assertEquals(2, criteria.getSchemes().length);
		assertSame(SignatureScheme.RSA_PKCS1_SHA256, criteria.getSchemes()[0]);
		assertSame(SignatureScheme.RSA_PSS_RSAE_SHA256, criteria.getSchemes()[1]);
		assertEquals(1, criteria.getCertSchemes().length);
		assertSame(SignatureScheme.ECDSA_SHA1, criteria.getCertSchemes()[0]);
		cert = (ICertificate) produced[2].getHandshake();
		assertEquals(2, cert.getEntries().length);
		assertArrayEquals(cert("rsasha256").getEncoded(), cert.getEntries()[0].getData());
		assertArrayEquals(cert("rsasha384").getEncoded(), cert.getEntries()[1].getData());

		state = serverState();
		consumer.consume(state, ch, data(ch), false);
		handler.onATSException = new InternalErrorAlert("");
		state.getTask().run();
		state.getTask().run();
		try {
			state.getProduced();
			fail();
		} catch (InternalErrorAlert e) {}
	}
	
	@Test
	public void testConsumeServerName() throws Exception {
		params.serverNameRequired = true;
		replace(extensions, keyShare());
		ClientHello ch = clientHello();
		consumer.consume(state, ch, data(ch), false);
		assertEquals("ALPN(null)|PN(null)|VSN(snf4j.org)|", handler.trace());
		
		state = serverState();
		remove(extensions, ExtensionType.SERVER_NAME);
		ch = clientHello();
		try {
			consumer.consume(state, ch, data(ch), false);
			fail();
		} catch (MissingExtensionAlert e) {}
		assertEquals("ALPN(null)|PN(null)|", handler.trace());
		
		state = serverState();
		params.serverNameRequired = false;
		ch = clientHello();
		consumer.consume(state, ch, data(ch), false);
		assertEquals("ALPN(null)|PN(null)|", handler.trace());
		
		extensions.add(serverName("snf4j.org"));
		handler.verifyServerName = false;
		state = serverState();
		params.serverNameRequired = true;
		ch = clientHello();
		try {
			consumer.consume(state, ch, data(ch), false);
			fail();
		} catch (UnrecognizedNameAlert e) {}
		assertEquals("ALPN(null)|PN(null)|VSN(snf4j.org)|", handler.trace());
	}
	
	@Test
	public void testConsumeOnceAgainWithCipherSuiteMismatch() throws Exception {
		replace(extensions, keyShare());
		ClientHello ch = clientHelloCS(TLS_AES_128_GCM_SHA256, TLS_AES_256_GCM_SHA384);
		consumer.consume(state, ch, data(ch), false);
		ServerHello sh = (ServerHello) state.getProduced()[0].getHandshake();
		assertTrue(ServerHelloRandom.isHelloRetryRequest(sh));
		KeyShareExtension kse = ExtensionsUtil.find(sh, ExtensionType.KEY_SHARE);
		assertSame(NamedGroup.SECP256R1, kse.getNamedGroup());
		assertSame(TLS_AES_256_GCM_SHA384, sh.getCipherSuite());
		
		replace(extensions, keyShare(NamedGroup.SECP256R1));
		ch = clientHelloCS(TLS_AES_128_GCM_SHA256);
		try {
			consumer.consume(state, ch, data(ch), false);
			fail();
		} catch (IllegalParameterAlert e) {}

		ch = clientHelloCS(TLS_AES_256_GCM_SHA384);
		consumer.consume(state, ch, data(ch), false);
	}
	
	@Test
	public void testConsumeOnceAgainWithNamedGroupMismatch() throws Exception {
		replace(extensions, keyShare());
		ClientHello ch = clientHelloCS(TLS_AES_128_GCM_SHA256);
		consumer.consume(state, ch, data(ch), false);
		ServerHello sh = (ServerHello) state.getProduced()[0].getHandshake();
		assertTrue(ServerHelloRandom.isHelloRetryRequest(sh));
		KeyShareExtension kse = ExtensionsUtil.find(sh, ExtensionType.KEY_SHARE);
		assertSame(NamedGroup.SECP256R1, kse.getNamedGroup());
		
		ch = clientHelloCS(TLS_AES_128_GCM_SHA256);
		try {
			consumer.consume(state, ch, data(ch), false);
			fail();
		} catch (IllegalParameterAlert e) {}

		replace(extensions, keyShare(NamedGroup.SECP384R1));
		ch = clientHelloCS(TLS_AES_128_GCM_SHA256);
		try {
			consumer.consume(state, ch, data(ch), false);
			fail();
		} catch (IllegalParameterAlert e) {}

		replace(extensions, keyShare(NamedGroup.SECP256R1, NamedGroup.SECP384R1));
		ch = clientHelloCS(TLS_AES_128_GCM_SHA256);
		try {
			consumer.consume(state, ch, data(ch), false);
			fail();
		} catch (IllegalParameterAlert e) {}

		replace(extensions, keyShare(NamedGroup.SECP256R1));
		ch = clientHelloCS(TLS_AES_128_GCM_SHA256);
		consumer.consume(state, ch, data(ch), false);
	}

	@Test(expected=InternalErrorAlert.class)
	public void testSecondHelloRetryRequest() throws Exception {
		replace(extensions, keyShare());
		ClientHello ch = clientHelloCS(TLS_AES_128_GCM_SHA256);
		state.changeState(MachineState.SRV_WAIT_2_CH);
		consumer.consume(state, ch, data(ch), false);
	}
	
	@Test
	public void testProducedHelloRetryRequest() throws Exception {
		replace(extensions, keyShare());
		ClientHello ch = clientHelloCS(TLS_AES_128_GCM_SHA256);
		consumer.consume(state, ch, data(ch), false);
		ServerHello sh = (ServerHello) state.getProduced()[0].getHandshake();
		assertTrue(ServerHelloRandom.isHelloRetryRequest(sh));
		assertSame(TLS_AES_128_GCM_SHA256, sh.getCipherSuite());
		assertEquals(2,sh.getExtensions().size());
		SupportedVersionsExtension sve = ExtensionsUtil.find(sh, ExtensionType.SUPPORTED_VERSIONS);
		assertEquals(1, sve.getVersions().length);
		assertEquals(0x0304, sve.getVersions()[0]);
		KeyShareExtension kse = ExtensionsUtil.find(sh, ExtensionType.KEY_SHARE);
		assertSame(NamedGroup.SECP256R1, kse.getNamedGroup());
		assertArrayEquals(ch.getLegacySessionId(), sh.getLegacySessionId());
		assertEquals(0, ch.getLegacySessionId().length);
		assertEquals(0, sh.getLegacyCompressionMethod());
		
		replace(extensions, versions(0x0303,0x0304,0x0305));
		legacySessionId = random(32);
		state = new EngineState(MachineState.SRV_WAIT_1_CH,params, handler, handler);
		ch = clientHelloCS(TLS_AES_256_GCM_SHA384);
		consumer.consume(state, ch, data(ch), false);
		sh = (ServerHello) state.getProduced()[0].getHandshake();
		assertTrue(ServerHelloRandom.isHelloRetryRequest(sh));
		assertSame(TLS_AES_256_GCM_SHA384, sh.getCipherSuite());
		assertEquals(2,sh.getExtensions().size());
		sve = ExtensionsUtil.find(sh, ExtensionType.SUPPORTED_VERSIONS);
		assertEquals(1, sve.getVersions().length);
		assertEquals(0x0304, sve.getVersions()[0]);
		kse = ExtensionsUtil.find(sh, ExtensionType.KEY_SHARE);
		assertSame(NamedGroup.SECP256R1, kse.getNamedGroup());
		assertArrayEquals(ch.getLegacySessionId(), sh.getLegacySessionId());
		assertEquals(32, ch.getLegacySessionId().length);

		replace(extensions, versions(0x0304));
		replace(extensions, groups(NamedGroup.SECP384R1));
		state = new EngineState(MachineState.SRV_WAIT_1_CH,params, handler, handler);
		ch = clientHelloCS(TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384);
		consumer.consume(state, ch, data(ch), false);
		sh = (ServerHello) state.getProduced()[0].getHandshake();
		assertTrue(ServerHelloRandom.isHelloRetryRequest(sh));
		assertSame(TLS_AES_256_GCM_SHA384, sh.getCipherSuite());
		assertEquals(2,sh.getExtensions().size());
		sve = ExtensionsUtil.find(sh, ExtensionType.SUPPORTED_VERSIONS);
		assertEquals(1, sve.getVersions().length);
		assertEquals(0x0304, sve.getVersions()[0]);
		kse = ExtensionsUtil.find(sh, ExtensionType.KEY_SHARE);
		assertSame(NamedGroup.SECP384R1, kse.getNamedGroup());
	}
	
	@Test
	public void testGetType() {
		assertSame(HandshakeType.CLIENT_HELLO, new ClientHelloConsumer().getType());
	}
	
	@Test
	public void testDeriveHandshakeSecretFailure() throws Exception {
		ClientHello ch = clientHelloCS(TLS_AES_128_GCM_SHA256);
		consumer.consume(state, ch, data(ch), false);
		Runnable task = state.getTask();
		task.run();
		state.getTask().run();
		Field f = EngineState.class.getDeclaredField("keySchedule");
		f.setAccessible(true);
		f.set(state, null);
		try {
			state.getProduced();
			fail();
		} catch (InternalErrorAlert e) {
			assertEquals("Failed to derive handshake secret", e.getMessage());
		}
	}
	
	@Test(expected=UnexpectedMessageAlert.class)
	public void testInvalidMachineSate() throws Exception {
		ClientHello ch = clientHello(0x0303);
		consumer.consume(new EngineState(MachineState.SRV_WAIT_FINISHED,params, handler, handler), ch, data(ch), false);
	}
	
	@Test(expected=ProtocolVersionAlert.class)
	public void testInvalidLegacyVersion() throws Exception {
		ClientHello ch = clientHello(0x0302);
		consumer.consume(state, ch, data(ch), false);
	}

	@Test(expected=ProtocolVersionAlert.class)
	public void testConsumeNoSupportedVersions1() throws Exception {
		ClientHello ch = clientHelloEx();
		consumer.consume(state, ch, data(ch), false);
	}

	@Test(expected=ProtocolVersionAlert.class)
	public void testConsumeNoSupportedVersions2() throws Exception {
		ClientHello ch = clientHelloEx(versions(0x0303));
		consumer.consume(state, ch, data(ch), false);
	}

	@Test(expected=ProtocolVersionAlert.class)
	public void testConsumeNoSupportedVersions3() throws Exception {
		ClientHello ch = clientHelloEx(versions(0x0303,0x0305));
		consumer.consume(state, ch, data(ch), false);
	}

	@Test(expected=HandshakeFailureAlert.class)
	public void testConsumeNoCipherSuite1() throws Exception {
		ClientHello ch = clientHelloCS();
		consumer.consume(state, ch, data(ch), false);
	}

	@Test(expected=HandshakeFailureAlert.class)
	public void testConsumeNoCipherSuite2() throws Exception {
		ClientHello ch = clientHelloCS(TLS_AES_128_CCM_8_SHA256);
		consumer.consume(state, ch, data(ch), false);
	}

	@Test(expected=MissingExtensionAlert.class)
	public void testConsumeNoKeyShare() throws Exception {
		ClientHello ch = clientHelloEx(versions(0x0304));
		consumer.consume(state, ch, data(ch), false);
	}

	@Test(expected=MissingExtensionAlert.class)
	public void testConsumeNoSupportedGroups() throws Exception {
		ClientHello ch = clientHelloEx(versions(0x0304), keyShare(NamedGroup.SECP256R1));
		consumer.consume(state, ch, data(ch), false);
	}

	@Test(expected=IllegalParameterAlert.class)
	public void testConsumeKeyShareFromNoSupportedGroup() throws Exception {
		ClientHello ch = clientHelloEx(
				versions(0x0304), 
				keyShare(NamedGroup.SECP256R1),
				groups(NamedGroup.FFDHE6144),
				schemes(SignatureScheme.RSA_PKCS1_SHA256));
		consumer.consume(state, ch, data(ch), false);
	}

	@Test(expected=HandshakeFailureAlert.class)
	public void testConsumeEmptyKeyShareAndNoSupportedGroup() throws Exception {
		ClientHello ch = clientHelloEx(
				versions(0x0304), 
				keyShare(),
				groups(NamedGroup.FFDHE6144),
				schemes(SignatureScheme.RSA_PKCS1_SHA256));
		consumer.consume(state, ch, data(ch), false);
	}

	@Test(expected=MissingExtensionAlert.class)
	public void testConsumeEmptyKeyShareAndNoSignatureAlgos() throws Exception {
		ClientHello ch = clientHelloEx(
				versions(0x0304), 
				keyShare(),
				groups(NamedGroup.FFDHE6144));
		consumer.consume(state, ch, data(ch), false);
	}
	
	@Test(expected=IllegalParameterAlert.class)
	public void testConsumeNoCompressions() throws Exception {
		ClientHello ch = clientHelloCompress(new byte[0]);
		consumer.consume(state, ch, data(ch), false);
	}

	@Test(expected=IllegalParameterAlert.class)
	public void testConsumeToManyCompressions() throws Exception {
		ClientHello ch = clientHelloCompress(new byte[2]);
		consumer.consume(state, ch, data(ch), false);
	}

	@Test(expected=IllegalParameterAlert.class)
	public void testConsumeWrongCompressions() throws Exception {
		ClientHello ch = clientHelloCompress(new byte[] {1});
		consumer.consume(state, ch, data(ch), false);
	}
	
	@Test(expected=InternalErrorAlert.class)
	public void testConsumeKeyScheduleFailure() throws Exception {
		params.cipherSuites[0] = new CipherSuite("xxx", 6666, null) {};
		ClientHello ch = clientHelloCS(new CipherSuite("xxx", 6666, null) {});
		consumer.consume(state, ch, data(ch), false);
	}
	
	@Test(expected=IllegalParameterAlert.class)
	public void testEarlyDataInSecondClientHello() throws Exception {
		extensions.add(new EarlyDataExtension());
		state.changeState(MachineState.SRV_WAIT_2_CH);
		ClientHello ch = clientHello(0x0303);
		consumer.consume(state, ch, data(ch), false);
	}

	@Test(expected=IllegalParameterAlert.class)
	public void testPskExtensionNotTheLastOne() throws Exception {
		OfferedPsk psk = new OfferedPsk(new PskIdentity(bytes(1,2,3,4),100), bytes(1,2,3,4));
		extensions.add(new PreSharedKeyExtension(psk));
		extensions.add(new PskKeyExchangeModesExtension(PskKeyExchangeMode.PSK_DHE_KE));
		ClientHello ch = clientHello(0x0303);
		consumer.consume(state, ch, data(ch), false);
	}

	@Test(expected=MissingExtensionAlert.class)
	public void testPskExtensionWithoutKeyExchangeModes() throws Exception {
		OfferedPsk psk = new OfferedPsk(new PskIdentity(bytes(1,2,3,4),100), bytes(1,2,3,4));
		extensions.add(new PreSharedKeyExtension(psk));
		ClientHello ch = clientHello(0x0303);
		consumer.consume(state, ch, data(ch), false);
	}

	@Test(expected=MissingExtensionAlert.class)
	public void testEarlyDataWithoutPsk() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		extensions.add(new EarlyDataExtension());
		extensions.add(new PskKeyExchangeModesExtension(PskKeyExchangeMode.PSK_DHE_KE));
		ClientHello ch = clientHello(0x0303);
		consumer.consume(state, ch, data(ch), false);
	}
	
	@Test
	public void testUnsupportedKeyExchangeModeWithEarlyData() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		OfferedPsk psk = new OfferedPsk(new PskIdentity(bytes(1,2,3,4),100), bytes(1,2,3,4));
		extensions.add(new EarlyDataExtension());
		extensions.add(new PskKeyExchangeModesExtension(PskKeyExchangeMode.PSK_KE));
		extensions.add(new PreSharedKeyExtension(psk));
		ClientHello ch = clientHello(0x0303);
		consumer.consume(state, ch, data(ch), false);
		assertSame(EarlyDataState.REJECTING, state.getEarlyDataContext().getState());
	}

	@Test
	public void testUnsupportedKeyExchangeModeWithoutEarlyData() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		OfferedPsk psk = new OfferedPsk(new PskIdentity(bytes(1,2,3,4),100), bytes(1,2,3,4));
		extensions.add(new PskKeyExchangeModesExtension(PskKeyExchangeMode.PSK_KE));
		extensions.add(new PreSharedKeyExtension(psk));
		ClientHello ch = clientHello(0x0303);
		consumer.consume(state, ch, data(ch), false);
		assertSame(EarlyDataState.NONE, state.getEarlyDataContext().getState());
	}

	@Test
	public void testInvalidPskBinder() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		params.peerHost = "snf4j.org";
		params.peerPort = 99;
		HandshakeEngine cli = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);
		
		HandshakeController c = new HandshakeController(cli, srv);
		
		c.run(true, null);
		assertTrue(cli.getState().isConnected());
		assertTrue(srv.getState().isConnected());

		cli = new HandshakeEngine(true, params, handler, handler);
		srv = new HandshakeEngine(false, params, handler, handler);
		
		c = new HandshakeController(cli, srv);
		c.run(true, HandshakeType.CLIENT_HELLO);
		IHandshake h = c.get(true);
		assertNotNull(h);
		PreSharedKeyExtension psk = ExtensionsUtil.find(h, ExtensionType.PRE_SHARED_KEY);
		assertNotNull(psk);
		psk.getOfferedPsks()[0].getBinder()[0] ^= 0xaa;
		h.prepare();
		try {
			c.run(true, null);
			fail();
		}
		catch (DecryptErrorAlert e) {
			assertEquals("Invalid PSK binder", e.getMessage());
		}
	}

	@Test
	public void testFailedComputationOfServerVerifyData() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.CERTIFICATES;
		params.peerHost = "snf4j.org";
		params.peerPort = 99;
		HandshakeEngine cli = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);
		
		HandshakeController c = new HandshakeController(cli, srv);
		
		c.run(true, null);
		IEngineTask task = (IEngineTask) srv.getTask();
		task.run();
		EngineState state = (EngineState) srv.getState();
		Field f = EngineState.class.getDeclaredField("keySchedule");
		f.setAccessible(true);
		f.set(state, null);
		try {
			task.finish((EngineState) srv.getState());
			fail();
		}
		catch (InternalErrorAlert e) {
			assertEquals("Failed to compute server verify data", e.getMessage());
		}
	}
	
	@Test
	public void testUnacceptedResumedSessionDueSelectedIdentity() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		params.peerHost = "snf4j.org";
		params.peerPort = 99;
		handler.earlyData.add(bytes(1,2,3,4));
		handler.ticketInfos = new TicketInfo[] {new TicketInfo(100)};
		handler.sessionManager.selectedIdentity = 1;
		handler.sessionManager.selectedIdentityCounter = 2;
		HandshakeEngine cli = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);
		
		HandshakeController c = new HandshakeController(cli, srv);
		
		c.run(true, null);
		assertTrue(cli.getState().isConnected());
		assertTrue(srv.getState().isConnected());

		cli = new HandshakeEngine(true, params, handler, handler);
		srv = new HandshakeEngine(false, params, handler, handler);
		
		c = new HandshakeController(cli, srv);
		c.run(true, HandshakeType.CLIENT_HELLO);
		IHandshake h = c.get(true);
		assertNotNull(h);
		PreSharedKeyExtension psk = ExtensionsUtil.find(h, ExtensionType.PRE_SHARED_KEY);
		assertNotNull(psk);
		c.run(true, null);
		assertSame(MachineState.CLI_CONNECTED, cli.getState().getState());
		assertSame(MachineState.SRV_CONNECTED, srv.getState().getState());
		assertSame(EarlyDataState.REJECTED, ((EngineState)cli.getState()).getEarlyDataContext().getState());
	}

	@Test
	public void testUnacceptedResumedSessionDueCipherSuite() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		params.peerHost = "snf4j.org";
		params.peerPort = 99;
		params.cipherSuites = new CipherSuite[] {CipherSuite.TLS_AES_128_GCM_SHA256};
		handler.earlyData.add(bytes(1,2,3,4));
		handler.ticketInfos = new TicketInfo[] {new TicketInfo(100)};
		handler.sessionManager.sessionTicket = new SessionTicket(CipherSuite.TLS_AES_256_GCM_SHA384, null, bytes(1), bytes(1), 1000, 1000, 1000);
		handler.sessionManager.sessionTicketCounter = 2;
		HandshakeEngine cli = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);
		
		HandshakeController c = new HandshakeController(cli, srv);
		
		c.run(true, null);
		assertTrue(cli.getState().isConnected());
		assertTrue(srv.getState().isConnected());

		cli = new HandshakeEngine(true, params, handler, handler);
		srv = new HandshakeEngine(false, params, handler, handler);
		
		c = new HandshakeController(cli, srv);
		c.run(true, HandshakeType.CLIENT_HELLO);
		IHandshake h = c.get(true);
		assertNotNull(h);
		PreSharedKeyExtension psk = ExtensionsUtil.find(h, ExtensionType.PRE_SHARED_KEY);
		assertNotNull(psk);
		params.delegatedTaskMode = DelegatedTaskMode.CERTIFICATES;
		c.run(true, null);
		assertSame(MachineState.CLI_CONNECTED, cli.getState().getState());
		assertSame(MachineState.SRV_CONNECTED, srv.getState().getState());
		assertSame(EarlyDataState.REJECTED, ((EngineState)cli.getState()).getEarlyDataContext().getState());
	}

	@Test
	public void testUnacceptedResumedSessionDueMaxDataSize() throws Exception {
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
		params.peerHost = "snf4j.org";
		params.peerPort = 99;
		params.cipherSuites = new CipherSuite[] {CipherSuite.TLS_AES_128_GCM_SHA256};
		handler.earlyData.add(bytes(1,2,3,4));
		handler.ticketInfos = new TicketInfo[] {new TicketInfo(100)};
		handler.sessionManager.sessionTicket = new SessionTicket(CipherSuite.TLS_AES_128_GCM_SHA256, null, bytes(1), bytes(1), 1000, 1000, 0);
		handler.sessionManager.sessionTicketCounter = 2;
		HandshakeEngine cli = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);
		
		HandshakeController c = new HandshakeController(cli, srv);
		
		c.run(true, null);
		assertTrue(cli.getState().isConnected());
		assertTrue(srv.getState().isConnected());

		cli = new HandshakeEngine(true, params, handler, handler);
		srv = new HandshakeEngine(false, params, handler, handler);
		
		c = new HandshakeController(cli, srv);
		c.run(true, HandshakeType.CLIENT_HELLO);
		IHandshake h = c.get(true);
		assertNotNull(h);
		PreSharedKeyExtension psk = ExtensionsUtil.find(h, ExtensionType.PRE_SHARED_KEY);
		assertNotNull(psk);
		params.delegatedTaskMode = DelegatedTaskMode.ALL;
		c.start(false);
		buffer.clear();
		h.getBytes(buffer);
		buffer.flip();
		buffer.position(4);
		h = ClientHello.getParser().parse(new ByteBuffer[] {buffer}, buffer.remaining(), ExtensionDecoder.DEFAULT);
		new ClientHelloConsumer().consume((EngineState)srv.getState(), h, new ByteBuffer[] {ByteBuffer.wrap(h.getPrepared().clone())}, false);
		srv.getTask().run();
		srv.getTask().run();
		srv.updateTasks();
		c.remove(true);
		c.run(true, null);
		assertSame(MachineState.CLI_CONNECTED, cli.getState().getState());
		assertSame(MachineState.SRV_CONNECTED, srv.getState().getState());
		assertSame(EarlyDataState.REJECTED, ((EngineState)cli.getState()).getEarlyDataContext().getState());
	}
	
}
