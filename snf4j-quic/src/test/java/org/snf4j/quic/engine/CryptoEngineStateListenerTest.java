/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic.engine;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.Assume;
import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.QuicAlert;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.crypto.QuicKeySchedule;
import org.snf4j.quic.engine.crypto.CryptoEngine;
import org.snf4j.quic.engine.crypto.ProducedCrypto;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.cipher.CipherSuiteSpec;
import org.snf4j.tls.crypto.AESAead;
import org.snf4j.tls.crypto.AeadEncrypt;
import org.snf4j.tls.crypto.Hash;
import org.snf4j.tls.crypto.Hkdf;
import org.snf4j.tls.crypto.IAeadEncrypt;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.crypto.TrafficKeys;
import org.snf4j.tls.crypto.TranscriptHash;
import org.snf4j.tls.engine.DelegatedTaskMode;
import org.snf4j.tls.engine.EngineHandlerBuilder;
import org.snf4j.tls.engine.EngineParametersBuilder;
import org.snf4j.tls.engine.EngineState;
import org.snf4j.tls.engine.HandshakeEngine;
import org.snf4j.tls.engine.IEarlyDataHandler;
import org.snf4j.tls.engine.MachineState;
import org.snf4j.tls.extension.EarlyDataExtension;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.handshake.ClientHello;
import org.snf4j.tls.handshake.EndOfEarlyData;
import org.snf4j.tls.handshake.Finished;
import org.snf4j.tls.handshake.KeyUpdate;
import org.snf4j.tls.handshake.NewSessionTicket;
import org.snf4j.tls.record.RecordType;
import org.snf4j.tls.session.ISessionManager;

public class CryptoEngineStateListenerTest extends CommonTest {

	static final byte[] INITIAL_SALT_V1 = bytes("38762cf7f55934b34d179ae6a4c80cadccbb7f0a");
	
	static final byte[] DEST_CID = bytes("8394c8f03e515708");

	static final long PACKET_NUMBER = 2;
	
	static final byte[] ADDITIONAL_DATA = bytes("11223344556677889900112233445566");
	
	static final byte[] PLAINTEXT = bytes("AABBCCDDEEFF");
	
	QuicState cliState, srvState;

	CryptoEngineStateListener cliListener, srvListener;
	
	@Override
	public void before() throws Exception {
		super.before();
		cliState = new QuicState(true); 
		srvState = new QuicState(false);
		cliListener = new CryptoEngineStateListener(cliState);
		srvListener = new CryptoEngineStateListener(srvState);
	}

	void setSecret(KeySchedule ks, String name, byte[] secret) throws Exception {
		Field f = KeySchedule.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(ks, secret);
	}

	byte[][] protect(String key, String iv, String hp) throws Exception {
		SecretKey keyBytes = AESAead.AEAD_AES_128_GCM.createKey(bytes(key));
		byte[] ivBytes = bytes(iv);
		AeadEncrypt enc = new AeadEncrypt(keyBytes, AESAead.AEAD_AES_128_GCM);
		HeaderProtector protector = new HeaderProtector(AESAead.AEAD_AES_128_GCM, AESAead.AEAD_AES_128_GCM.createKey(bytes(hp)));
		Encryptor e = new Encryptor(enc, protector, ivBytes, 1000);
		return new byte[][] {e.getAead().encrypt(e.nonce(PACKET_NUMBER), ADDITIONAL_DATA, PLAINTEXT), protector.deriveMask(ADDITIONAL_DATA, 0)};
	}

	byte[][] protect(IAeadEncrypt aead, byte[] iv, byte[] hp) throws Exception {
		HeaderProtector protector = new HeaderProtector(AESAead.AEAD_AES_128_GCM, AESAead.AEAD_AES_128_GCM.createKey(hp));
		Encryptor e = new Encryptor(aead, protector, iv, 1000);
		return new byte[][] {e.getAead().encrypt(e.nonce(PACKET_NUMBER), ADDITIONAL_DATA, PLAINTEXT), protector.deriveMask(ADDITIONAL_DATA, 0)};
	}
	
	@Test
	public void testOnInit() throws Exception {
		EncryptionContext ctx = cliState.getContext(EncryptionLevel.INITIAL);
		
		assertNull(ctx.getDecryptor());
		assertNull(ctx.getEncryptor());
		cliListener.onInit(INITIAL_SALT_V1, DEST_CID);
		assertNotNull(ctx.getDecryptor());
		assertNotNull(ctx.getEncryptor());
		assertEmpty(cliState, EncryptionLevel.EARLY_DATA, EncryptionLevel.HANDSHAKE, EncryptionLevel.APPLICATION_DATA);
		assertEmpty(ctx, KeyPhase.PREVIOUS, KeyPhase.NEXT);
		
		Encryptor encryptor = ctx.getEncryptor();
		
		byte[] payload = bytes("060040f1010000ed0303ebf8fa56f12939b9584a3896472ec40bb863cfd3e868" + 
				"04fe3a47f06a2b69484c00000413011302010000c000000010000e00000b6578" + 
				"616d706c652e636f6dff01000100000a00080006001d00170018001000070005" + 
				"04616c706e000500050100000000003300260024001d00209370b2c9caa47fba" + 
				"baf4559fedba753de171fa71f50f1ce15d43e994ec74d748002b000302030400" + 
				"0d0010000e0403050306030203080408050806002d00020101001c0002400100" + 
				"3900320408ffffffffffffffff05048000ffff07048000ffff08011001048000" + 
				"75300901100f088394c8f03e51570806048000ffff");
		payload = Arrays.copyOf(payload, 1162);
		
		byte[] header = bytes("c300000001088394c8f03e5157080000449e00000002");
		byte[] nonce = encryptor.nonce(2);
		
		byte[] encrypted = encryptor.getAead().encrypt(nonce, header, payload);
		byte[] sampled = split(encrypted, 16)[0];
		assertArrayEquals(bytes("d1b1c98dd7689fb8ec11d242b123dc9b"), sampled);
		
		byte[] mask = encryptor.getProtector().deriveMask(sampled, 0);
		assertArrayEquals(bytes("437b9aec36"), split(mask,5)[0]);

		srvListener.onInit(INITIAL_SALT_V1, DEST_CID);
		ctx = srvState.getContext(EncryptionLevel.INITIAL);
		assertEmpty(srvState, EncryptionLevel.EARLY_DATA, EncryptionLevel.HANDSHAKE, EncryptionLevel.APPLICATION_DATA);
		assertEmpty(ctx, KeyPhase.PREVIOUS, KeyPhase.NEXT);
		
		Decryptor decryptor = ctx.getDecryptor();
		
		mask = decryptor.getProtector().deriveMask(sampled, 0);
		assertArrayEquals(bytes("437b9aec36"), split(mask,5)[0]);
		byte[] decrypted = decryptor.getAead().decrypt(nonce, header, encrypted);
		assertArrayEquals(payload, decrypted);
	}
	
	@Test
	public void testUnimplemented() throws Exception {
		cliListener.onNewReceivingTraficKey(null, null);
		cliListener.onNewSendingTraficKey(null, null);
		cliListener.onKeyUpdate(null, null);
		cliListener.produceChangeCipherSpec(null);
		cliListener.prepareChangeCipherSpec(null);
	}
			
	void assertEmpty(QuicState state, EncryptionLevel... levels) {
		for (EncryptionLevel level: levels) {
			EncryptionContext ctx = state.getContext(level);

			assertNull(ctx.getDecryptor(KeyPhase.PREVIOUS));
			assertNull(ctx.getEncryptor(KeyPhase.PREVIOUS));
			assertNull(ctx.getDecryptor(KeyPhase.CURRENT));
			assertNull(ctx.getEncryptor(KeyPhase.CURRENT));
			assertNull(ctx.getDecryptor(KeyPhase.NEXT));
			assertNull(ctx.getEncryptor(KeyPhase.NEXT));
		}
	}
	
	void assertEmpty(EncryptionContext ctx, KeyPhase... phases) {
		for (KeyPhase phase: phases) {
			assertNull(ctx.getDecryptor(phase));
			assertNull(ctx.getEncryptor(phase));
		}
	}	

	@Test
	public void testOnNewTrafficSecretsWithInitial() throws Exception {
		KeySchedule ks = new KeySchedule(new Hkdf(Hash.SHA256.createMac()), new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		EngineState tlsState = new EngineState(MachineState.CLI_INIT, null, null, null);
		tlsState.initialize(ks, CipherSuite.TLS_AES_128_GCM_SHA256);

		assertEmpty(cliState, EncryptionLevel.EARLY_DATA, EncryptionLevel.INITIAL, EncryptionLevel.HANDSHAKE, EncryptionLevel.APPLICATION_DATA);
		try {
			cliListener.onNewTrafficSecrets(tlsState, RecordType.INITIAL);
			fail();
		}
		catch (InternalErrorAlert e) {
			assertEquals("Unexpected record type: INITIAL", e.getCause().getMessage());
		}
		assertEmpty(cliState, EncryptionLevel.EARLY_DATA, EncryptionLevel.INITIAL, EncryptionLevel.HANDSHAKE, EncryptionLevel.APPLICATION_DATA);
	}
	
	QuicKeySchedule getKeySchedule(CryptoEngineStateListener listener) throws Exception {
		Field f = CryptoEngineStateListener.class.getDeclaredField("keySchedule");
		f.setAccessible(true);
		return (QuicKeySchedule) f.get(listener);
	}
	
	@Test
	public void testOnCleanup() throws Exception {
		KeySchedule ks = new KeySchedule(new Hkdf(Hash.SHA256.createMac()), new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		setSecret(ks, "clientApplicationTrafficSecret", bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea"));
		setSecret(ks, "serverApplicationTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));
		EngineState tlsState = new EngineState(MachineState.CLI_INIT, null, null, null);
		tlsState.initialize(ks, CipherSuite.TLS_AES_128_GCM_SHA256);

		cliListener.onNewTrafficSecrets(tlsState, RecordType.APPLICATION);
		QuicKeySchedule qks = getKeySchedule(cliListener);
		assertNotNull(qks);
		qks.deriveNextGenerationTrafficKeys();
		cliListener.onCleanup(tlsState);
		assertNull(getKeySchedule(cliListener));
		try {
			qks.deriveNextGenerationTrafficKeys();
			fail();
		}
		catch (IllegalStateException e) {
			assertEquals("Next Generation Secrets not derived", e.getMessage());
		}
	}
	
	@Test
	public void testOnNewTrafficSecretsWithEearlyData() throws Exception {
		KeySchedule ks = new KeySchedule(new Hkdf(Hash.SHA256.createMac()), new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		setSecret(ks, "earlyTrafficSecret", bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea"));
		EngineState tlsState = new EngineState(MachineState.CLI_INIT, null, null, null);
		tlsState.initialize(ks, CipherSuite.TLS_AES_128_GCM_SHA256);
		EncryptionContext ctx = cliState.getContext(EncryptionLevel.EARLY_DATA);

		assertEmpty(ctx, KeyPhase.PREVIOUS, KeyPhase.CURRENT, KeyPhase.NEXT);
		cliListener.onNewTrafficSecrets(tlsState, RecordType.ZERO_RTT);
		assertNull(ctx.getDecryptor());
		assertNotNull(ctx.getEncryptor());
		assertEmpty(cliState, EncryptionLevel.INITIAL, EncryptionLevel.HANDSHAKE, EncryptionLevel.APPLICATION_DATA);
		assertEmpty(ctx, KeyPhase.PREVIOUS, KeyPhase.NEXT);
		
		byte[][] expected = protect(
				"1f369613dd76d5467730efcbe3b1a22d",
				"fa044b2f42a3fd3b46fb255c",
				"9f50449e04a0e810283a1e9933adedd2");
		byte[] encrypted = ctx.getEncryptor().getAead().encrypt(ctx.getEncryptor().nonce(PACKET_NUMBER), ADDITIONAL_DATA, PLAINTEXT);
		assertArrayEquals(expected[0], encrypted);
		byte[] mask = ctx.getEncryptor().getProtector().deriveMask(ADDITIONAL_DATA, 0);
		assertArrayEquals(expected[1], mask);
		
		ks = new KeySchedule(new Hkdf(Hash.SHA256.createMac()), new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		setSecret(ks, "earlyTrafficSecret", bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea"));
		tlsState = new EngineState(MachineState.SRV_INIT, null, null, null);
		tlsState.initialize(ks, CipherSuite.TLS_AES_128_GCM_SHA256);
		ctx = srvState.getContext(EncryptionLevel.EARLY_DATA);

		assertNull(ctx.getDecryptor());
		assertNull(ctx.getEncryptor());
		srvListener.onNewTrafficSecrets(tlsState, RecordType.ZERO_RTT);
		assertNotNull(ctx.getDecryptor());
		assertNull(ctx.getEncryptor());
		assertEmpty(srvState, EncryptionLevel.INITIAL, EncryptionLevel.HANDSHAKE, EncryptionLevel.APPLICATION_DATA);
		assertEmpty(ctx, KeyPhase.PREVIOUS, KeyPhase.NEXT);
		
		assertArrayEquals(mask, ctx.getDecryptor().getProtector().deriveMask(ADDITIONAL_DATA, 0));
		assertArrayEquals(PLAINTEXT, ctx.getDecryptor().getAead().decrypt(ctx.getDecryptor().nonce(PACKET_NUMBER), ADDITIONAL_DATA, encrypted));
	}
	
	@Test
	public void testOnNewTrafficSecretsWithHandshakeByClient() throws Exception {
		KeySchedule ks = new KeySchedule(new Hkdf(Hash.SHA256.createMac()), new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		setSecret(ks, "clientHandshakeTrafficSecret", bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea"));
		setSecret(ks, "serverHandshakeTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));
		EngineState tlsState = new EngineState(MachineState.CLI_INIT, null, null, null);
		tlsState.initialize(ks, CipherSuite.TLS_AES_128_GCM_SHA256);
		EncryptionContext ctx = cliState.getContext(EncryptionLevel.HANDSHAKE);
		
		assertEmpty(ctx, KeyPhase.PREVIOUS, KeyPhase.CURRENT, KeyPhase.NEXT);
		cliListener.onNewTrafficSecrets(tlsState, RecordType.HANDSHAKE);
		assertNotNull(ctx.getDecryptor());
		assertNotNull(ctx.getEncryptor());
		assertEmpty(cliState, EncryptionLevel.INITIAL, EncryptionLevel.EARLY_DATA, EncryptionLevel.APPLICATION_DATA);
		assertEmpty(ctx, KeyPhase.PREVIOUS, KeyPhase.NEXT);
		
		byte[][] expected = protect(
				"1f369613dd76d5467730efcbe3b1a22d",
				"fa044b2f42a3fd3b46fb255c",
				"9f50449e04a0e810283a1e9933adedd2");
		byte[] encrypted = ctx.getEncryptor().getAead().encrypt(ctx.getEncryptor().nonce(PACKET_NUMBER), ADDITIONAL_DATA, PLAINTEXT);
		assertArrayEquals(expected[0], encrypted);
		byte[] mask = ctx.getEncryptor().getProtector().deriveMask(ADDITIONAL_DATA, 0);
		assertArrayEquals(expected[1], mask);

		ks = new KeySchedule(new Hkdf(Hash.SHA256.createMac()), new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		setSecret(ks, "clientHandshakeTrafficSecret", bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea"));
		setSecret(ks, "serverHandshakeTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));
		tlsState = new EngineState(MachineState.SRV_INIT, null, null, null);
		tlsState.initialize(ks, CipherSuite.TLS_AES_128_GCM_SHA256);
		ctx = srvState.getContext(EncryptionLevel.HANDSHAKE);

		assertEmpty(ctx, KeyPhase.PREVIOUS, KeyPhase.CURRENT, KeyPhase.NEXT);
		srvListener.onNewTrafficSecrets(tlsState, RecordType.HANDSHAKE);
		assertNotNull(ctx.getDecryptor());
		assertNotNull(ctx.getEncryptor());
		assertEmpty(srvState, EncryptionLevel.INITIAL, EncryptionLevel.EARLY_DATA, EncryptionLevel.APPLICATION_DATA);
		assertEmpty(ctx, KeyPhase.PREVIOUS, KeyPhase.NEXT);
		
		assertArrayEquals(mask, ctx.getDecryptor().getProtector().deriveMask(ADDITIONAL_DATA, 0));
		assertArrayEquals(PLAINTEXT, ctx.getDecryptor().getAead().decrypt(ctx.getDecryptor().nonce(PACKET_NUMBER), ADDITIONAL_DATA, encrypted));
	}	

	@Test
	public void testOnNewTrafficSecretsWithHandshakeByServer() throws Exception {
		KeySchedule ks = new KeySchedule(new Hkdf(Hash.SHA256.createMac()), new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		setSecret(ks, "clientHandshakeTrafficSecret", bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea"));
		setSecret(ks, "serverHandshakeTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));
		EngineState tlsState = new EngineState(MachineState.CLI_INIT, null, null, null);
		tlsState.initialize(ks, CipherSuite.TLS_AES_128_GCM_SHA256);
		EncryptionContext ctx = srvState.getContext(EncryptionLevel.HANDSHAKE);
		
		assertEmpty(ctx, KeyPhase.PREVIOUS, KeyPhase.CURRENT, KeyPhase.NEXT);
		srvListener.onNewTrafficSecrets(tlsState, RecordType.HANDSHAKE);
		assertNotNull(ctx.getDecryptor());
		assertNotNull(ctx.getEncryptor());
		assertEmpty(srvState, EncryptionLevel.INITIAL, EncryptionLevel.EARLY_DATA, EncryptionLevel.APPLICATION_DATA);
		assertEmpty(ctx, KeyPhase.PREVIOUS, KeyPhase.NEXT);
		
		byte[][] expected = protect(
				"cf3a5331653c364c88f0f379b6067e37",
				"0ac1493ca1905853b0bba03e",
				"c206b8d9b9f0f37644430b490eeaa314");
		byte[] encrypted = ctx.getEncryptor().getAead().encrypt(ctx.getEncryptor().nonce(PACKET_NUMBER), ADDITIONAL_DATA, PLAINTEXT);
		assertArrayEquals(expected[0], encrypted);
		byte[] mask = ctx.getEncryptor().getProtector().deriveMask(ADDITIONAL_DATA, 0);
		assertArrayEquals(expected[1], mask);

		ks = new KeySchedule(new Hkdf(Hash.SHA256.createMac()), new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		setSecret(ks, "clientHandshakeTrafficSecret", bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea"));
		setSecret(ks, "serverHandshakeTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));
		tlsState = new EngineState(MachineState.SRV_INIT, null, null, null);
		tlsState.initialize(ks, CipherSuite.TLS_AES_128_GCM_SHA256);
		ctx = cliState.getContext(EncryptionLevel.HANDSHAKE);

		assertEmpty(ctx, KeyPhase.PREVIOUS, KeyPhase.CURRENT, KeyPhase.NEXT);
		cliListener.onNewTrafficSecrets(tlsState, RecordType.HANDSHAKE);
		assertNotNull(ctx.getDecryptor());
		assertNotNull(ctx.getEncryptor());
		assertEmpty(cliState, EncryptionLevel.INITIAL, EncryptionLevel.EARLY_DATA, EncryptionLevel.APPLICATION_DATA);
		assertEmpty(ctx, KeyPhase.PREVIOUS, KeyPhase.NEXT);
		
		assertArrayEquals(mask, ctx.getDecryptor().getProtector().deriveMask(ADDITIONAL_DATA, 0));
		assertArrayEquals(PLAINTEXT, ctx.getDecryptor().getAead().decrypt(ctx.getDecryptor().nonce(PACKET_NUMBER), ADDITIONAL_DATA, encrypted));
	}	

	@Test
	public void testOnNewTrafficSecretsWithApplicationByClient() throws Exception {
		KeySchedule ks = new KeySchedule(new Hkdf(Hash.SHA256.createMac()), new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		setSecret(ks, "clientHandshakeTrafficSecret", bytes("1122334455667788990011223344556677889900112233445566778899001122"));
		setSecret(ks, "serverHandshakeTrafficSecret", bytes("1122334455667788990011223344556677889900112233445566778899001122"));
		setSecret(ks, "clientApplicationTrafficSecret", bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea"));
		setSecret(ks, "serverApplicationTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));
		EngineState tlsState = new EngineState(MachineState.CLI_INIT, null, null, null);
		tlsState.initialize(ks, CipherSuite.TLS_AES_128_GCM_SHA256);
		EncryptionContext ctx = cliState.getContext(EncryptionLevel.APPLICATION_DATA);
		
		assertEmpty(ctx, KeyPhase.PREVIOUS, KeyPhase.CURRENT, KeyPhase.NEXT);
		cliListener.onNewTrafficSecrets(tlsState, RecordType.HANDSHAKE);
		cliListener.onNewTrafficSecrets(tlsState, RecordType.APPLICATION);
		assertNotNull(ctx.getDecryptor());
		assertNotNull(ctx.getEncryptor());
		assertNotNull(ctx.getDecryptor(KeyPhase.NEXT));
		assertNotNull(ctx.getEncryptor(KeyPhase.NEXT));
		assertEmpty(cliState, EncryptionLevel.INITIAL, EncryptionLevel.EARLY_DATA);
		assertEmpty(ctx, KeyPhase.PREVIOUS);
		
		byte[][] expected = protect(
				"1f369613dd76d5467730efcbe3b1a22d",
				"fa044b2f42a3fd3b46fb255c",
				"9f50449e04a0e810283a1e9933adedd2");
		byte[] encrypted = ctx.getEncryptor().getAead().encrypt(ctx.getEncryptor().nonce(PACKET_NUMBER), ADDITIONAL_DATA, PLAINTEXT);
		assertArrayEquals(expected[0], encrypted);
		byte[] mask = ctx.getEncryptor().getProtector().deriveMask(ADDITIONAL_DATA, 0);
		assertArrayEquals(expected[1], mask);

		ks = new KeySchedule(new Hkdf(Hash.SHA256.createMac()), new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		setSecret(ks, "clientApplicationTrafficSecret", bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea"));
		setSecret(ks, "serverApplicationTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));
		tlsState = new EngineState(MachineState.SRV_INIT, null, null, null);
		tlsState.initialize(ks, CipherSuite.TLS_AES_128_GCM_SHA256);
		ctx = srvState.getContext(EncryptionLevel.APPLICATION_DATA);

		assertEmpty(ctx, KeyPhase.PREVIOUS, KeyPhase.CURRENT, KeyPhase.NEXT);
		srvListener.onNewTrafficSecrets(tlsState, RecordType.APPLICATION);
		assertNotNull(ctx.getDecryptor());
		assertNotNull(ctx.getEncryptor());
		assertNotNull(ctx.getDecryptor(KeyPhase.NEXT));
		assertNotNull(ctx.getEncryptor(KeyPhase.NEXT));
		assertEmpty(srvState, EncryptionLevel.INITIAL, EncryptionLevel.EARLY_DATA, EncryptionLevel.HANDSHAKE);
		assertEmpty(ctx, KeyPhase.PREVIOUS);
		
		assertArrayEquals(mask, ctx.getDecryptor().getProtector().deriveMask(ADDITIONAL_DATA, 0));
		assertArrayEquals(PLAINTEXT, ctx.getDecryptor().getAead().decrypt(ctx.getDecryptor().nonce(PACKET_NUMBER), ADDITIONAL_DATA, encrypted));
	}	

	@Test
	public void testOnNewTrafficSecretsWithApplicationByServer() throws Exception {
		KeySchedule ks = new KeySchedule(new Hkdf(Hash.SHA256.createMac()), new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		setSecret(ks, "clientApplicationTrafficSecret", bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea"));
		setSecret(ks, "serverApplicationTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));
		EngineState tlsState = new EngineState(MachineState.CLI_INIT, null, null, null);
		tlsState.initialize(ks, CipherSuite.TLS_AES_128_GCM_SHA256);
		EncryptionContext ctx = srvState.getContext(EncryptionLevel.APPLICATION_DATA);
		
		assertEmpty(ctx, KeyPhase.PREVIOUS, KeyPhase.CURRENT, KeyPhase.NEXT);
		srvListener.onNewTrafficSecrets(tlsState, RecordType.APPLICATION);
		assertNotNull(ctx.getDecryptor());
		assertNotNull(ctx.getEncryptor());
		assertNotNull(ctx.getDecryptor(KeyPhase.NEXT));
		assertNotNull(ctx.getEncryptor(KeyPhase.NEXT));
		assertEmpty(srvState, EncryptionLevel.INITIAL, EncryptionLevel.EARLY_DATA, EncryptionLevel.HANDSHAKE);
		assertEmpty(ctx, KeyPhase.PREVIOUS);
		
		byte[][] expected = protect(
				"cf3a5331653c364c88f0f379b6067e37",
				"0ac1493ca1905853b0bba03e",
				"c206b8d9b9f0f37644430b490eeaa314");
		byte[] encrypted = ctx.getEncryptor().getAead().encrypt(ctx.getEncryptor().nonce(PACKET_NUMBER), ADDITIONAL_DATA, PLAINTEXT);
		assertArrayEquals(expected[0], encrypted);
		byte[] mask = ctx.getEncryptor().getProtector().deriveMask(ADDITIONAL_DATA, 0);
		assertArrayEquals(expected[1], mask);

		ks = new KeySchedule(new Hkdf(Hash.SHA256.createMac()), new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		setSecret(ks, "clientApplicationTrafficSecret", bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea"));
		setSecret(ks, "serverApplicationTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));
		tlsState = new EngineState(MachineState.SRV_INIT, null, null, null);
		tlsState.initialize(ks, CipherSuite.TLS_AES_128_GCM_SHA256);
		ctx = cliState.getContext(EncryptionLevel.APPLICATION_DATA);

		assertEmpty(ctx, KeyPhase.PREVIOUS, KeyPhase.CURRENT, KeyPhase.NEXT);
		cliListener.onNewTrafficSecrets(tlsState, RecordType.APPLICATION);
		assertNotNull(ctx.getDecryptor());
		assertNotNull(ctx.getEncryptor());
		assertNotNull(ctx.getDecryptor(KeyPhase.NEXT));
		assertNotNull(ctx.getEncryptor(KeyPhase.NEXT));
		assertEmpty(cliState, EncryptionLevel.INITIAL, EncryptionLevel.EARLY_DATA, EncryptionLevel.HANDSHAKE);
		assertEmpty(ctx, KeyPhase.PREVIOUS);
		
		assertArrayEquals(mask, ctx.getDecryptor().getProtector().deriveMask(ADDITIONAL_DATA, 0));
		assertArrayEquals(PLAINTEXT, ctx.getDecryptor().getAead().decrypt(ctx.getDecryptor().nonce(PACKET_NUMBER), ADDITIONAL_DATA, encrypted));
	}	
	
	@Test
	public void testOnNewTrafficSecretsNextKeysByClient() throws Exception {
		Assume.assumeTrue(JAVA11);
		KeySchedule ks = new KeySchedule(new Hkdf(Hash.SHA256.createMac()), new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_CHACHA20_POLY1305_SHA256);
		setSecret(ks, "clientApplicationTrafficSecret", bytes("9ac312a7f877468ebe69422748ad00a15443f18203a07d6060f688f30f21632b"));
		setSecret(ks, "serverApplicationTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));
		EngineState tlsState = new EngineState(MachineState.CLI_INIT, null, null, null);
		tlsState.initialize(ks, CipherSuite.TLS_CHACHA20_POLY1305_SHA256);
		EncryptionContext ctx = cliState.getContext(EncryptionLevel.APPLICATION_DATA);
		cliListener.onNewTrafficSecrets(tlsState, RecordType.APPLICATION);
		assertArrayEquals(
				ctx.getDecryptor().getProtector().deriveMask(ADDITIONAL_DATA, 0),
				ctx.getDecryptor(KeyPhase.NEXT).getProtector().deriveMask(ADDITIONAL_DATA, 0));
		assertArrayEquals(
				ctx.getEncryptor().getProtector().deriveMask(ADDITIONAL_DATA, 0),
				ctx.getEncryptor(KeyPhase.NEXT).getProtector().deriveMask(ADDITIONAL_DATA, 0));
		
		//generate keys KU secret
		QuicState cliState2 = new QuicState(true); 
		CryptoEngineStateListener cliListener2 = new CryptoEngineStateListener(cliState2);
		ks = new KeySchedule(new Hkdf(Hash.SHA256.createMac()), new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_CHACHA20_POLY1305_SHA256);
		setSecret(ks, "clientApplicationTrafficSecret", bytes("1223504755036d556342ee9361d253421a826c9ecdf3c7148684b36b714881f9"));
		setSecret(ks, "serverApplicationTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));
		EngineState tlsState2 = new EngineState(MachineState.CLI_INIT, null, null, null);
		tlsState2.initialize(ks, CipherSuite.TLS_CHACHA20_POLY1305_SHA256);
		EncryptionContext ctx2 = cliState2.getContext(EncryptionLevel.APPLICATION_DATA);
		cliListener2.onNewTrafficSecrets(tlsState2, RecordType.APPLICATION);
		
		byte[] encrypted = ctx.getEncryptor(KeyPhase.NEXT).getAead().encrypt(ctx.getEncryptor(KeyPhase.NEXT).nonce(PACKET_NUMBER), ADDITIONAL_DATA, PLAINTEXT);
		byte[] encrypted2 = ctx2.getEncryptor().getAead().encrypt(ctx2.getEncryptor().nonce(PACKET_NUMBER), ADDITIONAL_DATA, PLAINTEXT);
		assertArrayEquals(encrypted, encrypted2);
	}

	@Test
	public void testOnNewTrafficSecretsNextKeysByServer() throws Exception {
		Assume.assumeTrue(JAVA11);
		KeySchedule ks = new KeySchedule(new Hkdf(Hash.SHA256.createMac()), new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_CHACHA20_POLY1305_SHA256);
		setSecret(ks, "serverApplicationTrafficSecret", bytes("9ac312a7f877468ebe69422748ad00a15443f18203a07d6060f688f30f21632b"));
		setSecret(ks, "clientApplicationTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));
		EngineState tlsState = new EngineState(MachineState.SRV_INIT, null, null, null);
		tlsState.initialize(ks, CipherSuite.TLS_CHACHA20_POLY1305_SHA256);
		EncryptionContext ctx = srvState.getContext(EncryptionLevel.APPLICATION_DATA);
		srvListener.onNewTrafficSecrets(tlsState, RecordType.APPLICATION);
		assertArrayEquals(
				ctx.getDecryptor().getProtector().deriveMask(ADDITIONAL_DATA, 0),
				ctx.getDecryptor(KeyPhase.NEXT).getProtector().deriveMask(ADDITIONAL_DATA, 0));
		assertArrayEquals(
				ctx.getEncryptor().getProtector().deriveMask(ADDITIONAL_DATA, 0),
				ctx.getEncryptor(KeyPhase.NEXT).getProtector().deriveMask(ADDITIONAL_DATA, 0));

		//generate keys KU secret
		QuicState srvState2 = new QuicState(false); 
		CryptoEngineStateListener cliListener2 = new CryptoEngineStateListener(srvState2);
		ks = new KeySchedule(new Hkdf(Hash.SHA256.createMac()), new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_CHACHA20_POLY1305_SHA256);
		setSecret(ks, "serverApplicationTrafficSecret", bytes("1223504755036d556342ee9361d253421a826c9ecdf3c7148684b36b714881f9"));
		setSecret(ks, "clientApplicationTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));
		EngineState tlsState2 = new EngineState(MachineState.SRV_INIT, null, null, null);
		tlsState2.initialize(ks, CipherSuite.TLS_CHACHA20_POLY1305_SHA256);
		EncryptionContext ctx2 = srvState2.getContext(EncryptionLevel.APPLICATION_DATA);
		cliListener2.onNewTrafficSecrets(tlsState2, RecordType.APPLICATION);
		
		byte[] encrypted = ctx.getEncryptor(KeyPhase.NEXT).getAead().encrypt(ctx.getEncryptor(KeyPhase.NEXT).nonce(PACKET_NUMBER), ADDITIONAL_DATA, PLAINTEXT);
		byte[] encrypted2 = ctx2.getEncryptor().getAead().encrypt(ctx2.getEncryptor().nonce(PACKET_NUMBER), ADDITIONAL_DATA, PLAINTEXT);
		assertArrayEquals(encrypted, encrypted2);
	}
	
	@Test
	public void testOnNextKeysByClient() throws Exception {
		KeySchedule ks = new KeySchedule(new Hkdf(Hash.SHA256.createMac()), new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		setSecret(ks, "clientApplicationTrafficSecret", bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea"));
		setSecret(ks, "serverApplicationTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));
		EngineState tlsState = new EngineState(MachineState.CLI_INIT, null, null, null);
		tlsState.initialize(ks, CipherSuite.TLS_AES_128_GCM_SHA256);
		EncryptionContext ctx = cliState.getContext(EncryptionLevel.APPLICATION_DATA);
		
		cliListener.onNewTrafficSecrets(tlsState, RecordType.APPLICATION);
		Decryptor d1 = ctx.getDecryptor();
		Decryptor d2 = ctx.getDecryptor(KeyPhase.NEXT);
		Encryptor e1 = ctx.getEncryptor();
		Encryptor e2 = ctx.getEncryptor(KeyPhase.NEXT);

		cliListener.onNextKeys();
		assertSame(d1, ctx.getDecryptor());
		assertSame(d2, ctx.getDecryptor(KeyPhase.NEXT));
		assertSame(e1, ctx.getEncryptor());
		assertSame(e2, ctx.getEncryptor(KeyPhase.NEXT));
		
		ctx.rotateKeys();
		assertSame(d1, ctx.getDecryptor(KeyPhase.PREVIOUS));
		assertSame(d2, ctx.getDecryptor());
		assertNull(ctx.getDecryptor(KeyPhase.NEXT));
		assertNull(ctx.getEncryptor(KeyPhase.PREVIOUS));
		assertSame(e2, ctx.getEncryptor());
		assertNull(ctx.getEncryptor(KeyPhase.NEXT));
		
		cliListener.onNextKeys();
		assertSame(d1, ctx.getDecryptor(KeyPhase.PREVIOUS));
		assertSame(d2, ctx.getDecryptor());
		assertNotNull(ctx.getDecryptor(KeyPhase.NEXT));
		assertNull(ctx.getEncryptor(KeyPhase.PREVIOUS));
		assertSame(e2, ctx.getEncryptor());
		assertNotNull(ctx.getEncryptor(KeyPhase.NEXT));
		assertSame(ctx.getEncryptor().getProtector(), ctx.getEncryptor(KeyPhase.NEXT).getProtector());
		assertSame(ctx.getDecryptor().getProtector(), ctx.getDecryptor(KeyPhase.NEXT).getProtector());
		
		ks = new KeySchedule(new Hkdf(Hash.SHA256.createMac()), new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		setSecret(ks, "clientApplicationTrafficSecret", bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea"));
		setSecret(ks, "serverApplicationTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));
		QuicKeySchedule keySchedule = new QuicKeySchedule(
				new Hkdf(CipherSuite.TLS_AES_128_GCM_SHA256.spec().getHashSpec().getHash().createMac()),
				CipherSuite.TLS_AES_128_GCM_SHA256.spec());		

		keySchedule.deriveNextGenerationSecrets(ks);
		TrafficKeys keys = keySchedule.deriveNextGenerationTrafficKeys();
		byte[][] expected = protect(keys.getAeadEncrypt(true), keys.getIv(true), new byte[16]);
		byte[] encrypted = ctx.getEncryptor().getAead().encrypt(ctx.getEncryptor().nonce(PACKET_NUMBER), ADDITIONAL_DATA, PLAINTEXT);
		assertArrayEquals(expected[0], encrypted);
		byte[][] encryptedByPeer = protect(keys.getAeadEncrypt(false), keys.getIv(false), new byte[16]);
		assertArrayEquals(
				PLAINTEXT,
				ctx.getDecryptor().getAead().decrypt(ctx.getDecryptor().nonce(PACKET_NUMBER), ADDITIONAL_DATA, encryptedByPeer[0]));
		
		keys = keySchedule.deriveNextGenerationTrafficKeys();
		expected = protect(keys.getAeadEncrypt(true), keys.getIv(true), new byte[16]);
		encrypted = ctx.getEncryptor(KeyPhase.NEXT).getAead().encrypt(ctx.getEncryptor(KeyPhase.NEXT).nonce(PACKET_NUMBER), ADDITIONAL_DATA, PLAINTEXT);
		assertArrayEquals(expected[0], encrypted);
		encryptedByPeer = protect(keys.getAeadEncrypt(false), keys.getIv(false), new byte[16]);
		assertArrayEquals(
				PLAINTEXT,
				ctx.getDecryptor(KeyPhase.NEXT).getAead().decrypt(ctx.getDecryptor(KeyPhase.NEXT).nonce(PACKET_NUMBER), ADDITIONAL_DATA, encryptedByPeer[0]));
	}

	@Test
	public void testOnNextKeysByServer() throws Exception {
		KeySchedule ks = new KeySchedule(new Hkdf(Hash.SHA256.createMac()), new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		setSecret(ks, "clientApplicationTrafficSecret", bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea"));
		setSecret(ks, "serverApplicationTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));
		EngineState tlsState = new EngineState(MachineState.SRV_INIT, null, null, null);
		tlsState.initialize(ks, CipherSuite.TLS_AES_128_GCM_SHA256);
		EncryptionContext ctx = srvState.getContext(EncryptionLevel.APPLICATION_DATA);
		
		srvListener.onNewTrafficSecrets(tlsState, RecordType.APPLICATION);
		Decryptor d1 = ctx.getDecryptor();
		Decryptor d2 = ctx.getDecryptor(KeyPhase.NEXT);
		Encryptor e1 = ctx.getEncryptor();
		Encryptor e2 = ctx.getEncryptor(KeyPhase.NEXT);

		srvListener.onNextKeys();
		assertSame(d1, ctx.getDecryptor());
		assertSame(d2, ctx.getDecryptor(KeyPhase.NEXT));
		assertSame(e1, ctx.getEncryptor());
		assertSame(e2, ctx.getEncryptor(KeyPhase.NEXT));
		
		ctx.rotateKeys();
		assertSame(d1, ctx.getDecryptor(KeyPhase.PREVIOUS));
		assertSame(d2, ctx.getDecryptor());
		assertNull(ctx.getDecryptor(KeyPhase.NEXT));
		assertNull(ctx.getEncryptor(KeyPhase.PREVIOUS));
		assertSame(e2, ctx.getEncryptor());
		assertNull(ctx.getEncryptor(KeyPhase.NEXT));
		
		srvListener.onNextKeys();
		assertSame(d1, ctx.getDecryptor(KeyPhase.PREVIOUS));
		assertSame(d2, ctx.getDecryptor());
		assertNotNull(ctx.getDecryptor(KeyPhase.NEXT));
		assertNull(ctx.getEncryptor(KeyPhase.PREVIOUS));
		assertSame(e2, ctx.getEncryptor());
		assertNotNull(ctx.getEncryptor(KeyPhase.NEXT));
		assertSame(ctx.getEncryptor().getProtector(), ctx.getEncryptor(KeyPhase.NEXT).getProtector());
		assertSame(ctx.getDecryptor().getProtector(), ctx.getDecryptor(KeyPhase.NEXT).getProtector());
		
		ks = new KeySchedule(new Hkdf(Hash.SHA256.createMac()), new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		setSecret(ks, "clientApplicationTrafficSecret", bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea"));
		setSecret(ks, "serverApplicationTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));
		QuicKeySchedule keySchedule = new QuicKeySchedule(
				new Hkdf(CipherSuite.TLS_AES_128_GCM_SHA256.spec().getHashSpec().getHash().createMac()),
				CipherSuite.TLS_AES_128_GCM_SHA256.spec());		

		keySchedule.deriveNextGenerationSecrets(ks);
		TrafficKeys keys = keySchedule.deriveNextGenerationTrafficKeys();
		byte[][] expected = protect(keys.getAeadEncrypt(false), keys.getIv(false), new byte[16]);
		byte[] encrypted = ctx.getEncryptor().getAead().encrypt(ctx.getEncryptor().nonce(PACKET_NUMBER), ADDITIONAL_DATA, PLAINTEXT);
		assertArrayEquals(expected[0], encrypted);
		byte[][] encryptedByPeer = protect(keys.getAeadEncrypt(true), keys.getIv(true), new byte[16]);
		assertArrayEquals(
				PLAINTEXT,
				ctx.getDecryptor().getAead().decrypt(ctx.getDecryptor().nonce(PACKET_NUMBER), ADDITIONAL_DATA, encryptedByPeer[0]));
		
		keys = keySchedule.deriveNextGenerationTrafficKeys();
		expected = protect(keys.getAeadEncrypt(false), keys.getIv(false), new byte[16]);
		encrypted = ctx.getEncryptor(KeyPhase.NEXT).getAead().encrypt(ctx.getEncryptor(KeyPhase.NEXT).nonce(PACKET_NUMBER), ADDITIONAL_DATA, PLAINTEXT);
		assertArrayEquals(expected[0], encrypted);
		encryptedByPeer = protect(keys.getAeadEncrypt(true), keys.getIv(true), new byte[16]);
		assertArrayEquals(
				PLAINTEXT,
				ctx.getDecryptor(KeyPhase.NEXT).getAead().decrypt(ctx.getDecryptor(KeyPhase.NEXT).nonce(PACKET_NUMBER), ADDITIONAL_DATA, encryptedByPeer[0]));
	}
	
	@Test
	public void testOnHandshake() throws Exception {
		try {
			cliListener.onHandshake(null, new EndOfEarlyData());
			fail();
		}
		catch (QuicAlert e) {
			assertSame(TransportError.PROTOCOL_VIOLATION, e.getTransportError());
		}
		try {
			cliListener.onHandshake(null, new KeyUpdate(true));
			fail();
		}
		catch (UnexpectedMessageAlert e) {
			assertEquals(0xa, e.getDescription().value());
		}
		try {
			cliListener.onHandshake(null, new ClientHello(
					0x0303, 
					new byte[32], 
					new byte[1], 
					new CipherSuite[] {CipherSuite.TLS_AES_128_GCM_SHA256}, 
					new byte[1],
					new ArrayList<IExtension>()));
			fail();
		}
		catch (QuicAlert e) {
			assertSame(TransportError.PROTOCOL_VIOLATION, e.getTransportError());
		}
		
		cliListener.onHandshake(null, new ClientHello(
				0x0303, 
				new byte[32], 
				new byte[0], 
				new CipherSuite[] {CipherSuite.TLS_AES_128_GCM_SHA256}, 
				new byte[1],
				new ArrayList<IExtension>()));
		cliListener.onHandshake(null, new Finished(new byte[32]));
		
		List<IExtension> extensions = new ArrayList<IExtension>();
		NewSessionTicket ticket = new NewSessionTicket(bytes("00"), bytes("") , 0, 0, extensions);
		cliListener.onHandshake(null, ticket);
		extensions.add(new EarlyDataExtension(0xffffffffL));
		cliListener.onHandshake(null, ticket);
		extensions.clear();
		extensions.add(new EarlyDataExtension(0xffffffffL-1));
		try {
			cliListener.onHandshake(null, ticket);
			fail();
		}
		catch (QuicAlert e) {
			assertSame(TransportError.PROTOCOL_VIOLATION, e.getTransportError());
		}
	}

	void assertContexts(QuicState state, boolean... exists) {
		int i = 0;
		for (EncryptionLevel level: EncryptionLevel.values()) {
			EncryptionContext ctx = state.getContext(level);
			if (exists[i++]) {
				if (level == EncryptionLevel.EARLY_DATA) {
					if (state.isClientMode()) {
						assertNotNull(ctx.getEncryptor());
						assertNull(ctx.getDecryptor());
					}
					else {
						assertNull(ctx.getEncryptor());
						assertNotNull(ctx.getDecryptor());						
					}
					continue;
				}
				else {
					assertNotNull(ctx.getEncryptor());
					assertNotNull(ctx.getDecryptor());
				}
				if (level == EncryptionLevel.APPLICATION_DATA) {
					assertNotNull(ctx.getEncryptor(KeyPhase.NEXT));
					assertNotNull(ctx.getDecryptor(KeyPhase.NEXT));
				}
				else {
					assertNull(ctx.getEncryptor(KeyPhase.NEXT));
					assertNull(ctx.getDecryptor(KeyPhase.NEXT));
				}
			}
			else {
				assertNull(ctx.getEncryptor());
				assertNull(ctx.getDecryptor());
			}
		}
	}
	
	@Test
	public void testHandshakeWithPSK() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder sepb = new EngineParametersBuilder()
				.skipEndOfEarlyData(true)
				.delegatedTaskMode(DelegatedTaskMode.NONE);
		EngineParametersBuilder cepb = new EngineParametersBuilder()
				.skipEndOfEarlyData(true)
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost("quic1.snf4j.org")
				.peerPort(8001);
		CryptoEngine cliEngine = new CryptoEngine(new HandshakeEngine(true, cepb.build(), ehb.build(), cliListener));
		CryptoEngine srvEngine = new CryptoEngine(new HandshakeEngine(false, sepb.build(), ehb.build(), srvListener));
		CryptoEngineAdapter cliAdapter = new CryptoEngineAdapter(cliEngine);
		CryptoEngineAdapter srvAdapter = new CryptoEngineAdapter(srvEngine);
		assertContexts(cliState, false, false, false, false);
		assertContexts(srvState, false, false, false, false);
		cliListener.onInit(INITIAL_SALT_V1, DEST_CID);
		assertContexts(cliState, true, false, false, false);
		assertContexts(srvState, false, false, false, false);
		srvListener.onInit(INITIAL_SALT_V1, DEST_CID);
		assertContexts(cliState, true, false, false, false);
		assertContexts(srvState, true, false, false, false);
		cliEngine.start();
		srvEngine.start();
		
		ProducedCrypto[] produced = cliAdapter.produce();
		assertContexts(cliState, true, false, false, false);
		assertEquals(1, produced.length);
		ByteBuffer data = produced[0].getData();
		srvAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		assertContexts(srvState, true, false, true, true);
		produced = srvAdapter.produce();
		assertEquals(2, produced.length);
		data = produced[0].getData();
		cliAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		assertContexts(cliState, true, false, true, false);
		data = produced[1].getData();
		int handshakeLength = data.remaining();
		cliAdapter.consume(data, produced[1].getOffset(), data.remaining(), produced[1].getEncryptionLevel());
		assertContexts(cliState, true, false, true, true);
		produced = cliAdapter.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		srvAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = srvAdapter.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		ISessionManager mgr = cliEngine.getSession().getManager();
		assertEquals(0, mgr.getTickets(cliEngine.getSession()).length);
		cliAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = cliAdapter.produce();
		assertEquals(0, produced.length);
		assertEquals(1, mgr.getTickets(cliEngine.getSession()).length);
		
		//With PSK
		cliState = new QuicState(true); 
		srvState = new QuicState(false);
		cliListener = new CryptoEngineStateListener(cliState);
		srvListener = new CryptoEngineStateListener(srvState);
		cliEngine = new CryptoEngine(new HandshakeEngine(true, cepb.build(), ehb.build(), cliListener));
		srvEngine = new CryptoEngine(new HandshakeEngine(false, sepb.build(), ehb.build(), srvListener));
		cliAdapter = new CryptoEngineAdapter(cliEngine);
		srvAdapter = new CryptoEngineAdapter(srvEngine);
		cliListener.onInit(INITIAL_SALT_V1, DEST_CID);
		srvListener.onInit(INITIAL_SALT_V1, DEST_CID);
		cliEngine.start();
		srvEngine.start();

		produced = cliAdapter.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		srvAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = srvAdapter.produce();
		assertEquals(2, produced.length);
		data = produced[0].getData();
		cliAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		data = produced[1].getData();
		assertTrue(handshakeLength/3 > data.remaining());
		cliAdapter.consume(data, produced[1].getOffset(), data.remaining(), produced[1].getEncryptionLevel());
		produced = cliAdapter.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		srvAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = srvAdapter.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		assertEquals(0, mgr.getTickets(cliEngine.getSession()).length);
		cliAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = cliAdapter.produce();
		assertEquals(0, produced.length);
		assertEquals(1, mgr.getTickets(cliEngine.getSession()).length);
	}
	
	@Test
	public void testHandshakeWithEarlyData() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm())
				.ticketInfos(0xffffffffL);
		EngineParametersBuilder sepb = new EngineParametersBuilder()
				.skipEndOfEarlyData(true)
				.delegatedTaskMode(DelegatedTaskMode.NONE);
		EngineParametersBuilder cepb = new EngineParametersBuilder()
				.skipEndOfEarlyData(true)
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost("quic2.snf4j.org")
				.peerPort(8001);
		CryptoEngine cliEngine = new CryptoEngine(new HandshakeEngine(true, cepb.build(), ehb.build(), cliListener));
		CryptoEngine srvEngine = new CryptoEngine(new HandshakeEngine(false, sepb.build(), ehb.build(), srvListener));
		CryptoEngineAdapter cliAdapter = new CryptoEngineAdapter(cliEngine);
		CryptoEngineAdapter srvAdapter = new CryptoEngineAdapter(srvEngine);
		cliListener.onInit(INITIAL_SALT_V1, DEST_CID);
		srvListener.onInit(INITIAL_SALT_V1, DEST_CID);
		cliEngine.start();
		srvEngine.start();
		
		ProducedCrypto[] produced = cliAdapter.produce();
		assertEquals(1, produced.length);
		ByteBuffer data = produced[0].getData();
		srvAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = srvAdapter.produce();
		assertEquals(2, produced.length);
		data = produced[0].getData();
		cliAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		data = produced[1].getData();
		int handshakeLength = data.remaining();
		cliAdapter.consume(data, produced[1].getOffset(), data.remaining(), produced[1].getEncryptionLevel());
		produced = cliAdapter.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		srvAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = srvAdapter.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		ISessionManager mgr = cliEngine.getSession().getManager();
		assertEquals(0, mgr.getTickets(cliEngine.getSession()).length);
		cliAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = cliAdapter.produce();
		assertEquals(0, produced.length);
		assertEquals(1, mgr.getTickets(cliEngine.getSession()).length);
		
		//With early data
		TestEDHandler edh = new TestEDHandler(bytes("ac"));
		cliState = new QuicState(true); 
		srvState = new QuicState(false);
		cliListener = new CryptoEngineStateListener(cliState);
		srvListener = new CryptoEngineStateListener(srvState);
		cliEngine = new CryptoEngine(new HandshakeEngine(true, cepb.build(), ehb.build(edh), cliListener));
		srvEngine = new CryptoEngine(new HandshakeEngine(false, sepb.build(), ehb.build(), srvListener));
		cliAdapter = new CryptoEngineAdapter(cliEngine);
		srvAdapter = new CryptoEngineAdapter(srvEngine);
		cliListener.onInit(INITIAL_SALT_V1, DEST_CID);
		srvListener.onInit(INITIAL_SALT_V1, DEST_CID);
		cliEngine.start();
		srvEngine.start();

		produced = cliAdapter.produce();
		assertEquals(2, produced.length);		
		assertContexts(cliState, true, true, false, false);
		data = produced[0].getData();
		srvAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		assertContexts(srvState, true, true, true, true);
		assertSame(EncryptionLevel.EARLY_DATA, produced[1].getEncryptionLevel());
		data = produced[1].getData();
		assertEquals(1, data.remaining());
		assertEquals(0xac, (int)data.get() & 0xff);
		produced = srvAdapter.produce();
		assertEquals(2, produced.length);
		data = produced[0].getData();
		cliAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		assertContexts(cliState, true, true, true, false);
		data = produced[1].getData();
		assertTrue(handshakeLength/3 > data.remaining());
		cliAdapter.consume(data, produced[1].getOffset(), data.remaining(), produced[1].getEncryptionLevel());
		assertContexts(cliState, true, true, true, true);
		produced = cliAdapter.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		srvAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = srvAdapter.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		assertEquals(0, mgr.getTickets(cliEngine.getSession()).length);
		cliAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = cliAdapter.produce();
		assertEquals(0, produced.length);
		assertEquals(1, mgr.getTickets(cliEngine.getSession()).length);
		assertEquals("ACC", edh.status.toString());
	}
	
	@Test
	public void testHandshakeWithEarlyDataRejected() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm())
				.ticketInfos(0xffffffffL);
		EngineParametersBuilder sepb = new EngineParametersBuilder()
				.skipEndOfEarlyData(true)
				.namedGroups(NamedGroup.SECP256R1, NamedGroup.SECP521R1)
				.delegatedTaskMode(DelegatedTaskMode.NONE);
		EngineParametersBuilder cepb = new EngineParametersBuilder()
				.skipEndOfEarlyData(true)
				.namedGroups(NamedGroup.SECP256R1, NamedGroup.SECP521R1)
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost("quic4.snf4j.org")
				.peerPort(8001);
		CryptoEngine cliEngine = new CryptoEngine(new HandshakeEngine(true, cepb.build(), ehb.build(), cliListener));
		CryptoEngine srvEngine = new CryptoEngine(new HandshakeEngine(false, sepb.build(), ehb.build(), srvListener));
		CryptoEngineAdapter cliAdapter = new CryptoEngineAdapter(cliEngine);
		CryptoEngineAdapter srvAdapter = new CryptoEngineAdapter(srvEngine);
		cliListener.onInit(INITIAL_SALT_V1, DEST_CID);
		srvListener.onInit(INITIAL_SALT_V1, DEST_CID);
		cliEngine.start();
		srvEngine.start();
		
		ProducedCrypto[] produced = cliAdapter.produce();
		assertEquals(1, produced.length);
		ByteBuffer data = produced[0].getData();
		srvAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = srvAdapter.produce();
		assertEquals(2, produced.length);
		data = produced[0].getData();
		cliAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		data = produced[1].getData();
		int handshakeLength = data.remaining();
		cliAdapter.consume(data, produced[1].getOffset(), data.remaining(), produced[1].getEncryptionLevel());
		produced = cliAdapter.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		srvAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = srvAdapter.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		ISessionManager mgr = cliEngine.getSession().getManager();
		assertEquals(0, mgr.getTickets(cliEngine.getSession()).length);
		cliAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = cliAdapter.produce();
		assertEquals(0, produced.length);
		assertEquals(1, mgr.getTickets(cliEngine.getSession()).length);
		
		//With early data
		TestEDHandler edh = new TestEDHandler(bytes("ac"));
		sepb.namedGroups(NamedGroup.SECP521R1);
		cliState = new QuicState(true); 
		srvState = new QuicState(false);
		cliListener = new CryptoEngineStateListener(cliState);
		srvListener = new CryptoEngineStateListener(srvState);
		cliEngine = new CryptoEngine(new HandshakeEngine(true, cepb.build(), ehb.build(edh), cliListener));
		srvEngine = new CryptoEngine(new HandshakeEngine(false, sepb.build(), ehb.build(), srvListener));
		cliAdapter = new CryptoEngineAdapter(cliEngine);
		srvAdapter = new CryptoEngineAdapter(srvEngine);
		cliListener.onInit(INITIAL_SALT_V1, DEST_CID);
		srvListener.onInit(INITIAL_SALT_V1, DEST_CID);
		cliEngine.start();
		srvEngine.start();

		produced = cliAdapter.produce();
		assertEquals(2, produced.length);		
		assertContexts(cliState, true, true, false, false);
		data = produced[0].getData();
		srvAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		assertContexts(srvState, true, false, false, false);
		assertSame(EncryptionLevel.EARLY_DATA, produced[1].getEncryptionLevel());
		data = produced[1].getData();
		assertEquals(1, data.remaining());
		assertEquals(0xac, (int)data.get() & 0xff);
		produced = srvAdapter.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		cliAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = cliAdapter.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		srvAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = srvAdapter.produce();
		assertEquals(2, produced.length);
		data = produced[0].getData();
		cliAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		assertContexts(cliState, true, true, true, false);
		data = produced[1].getData();
		assertTrue(handshakeLength/3 > data.remaining());
		cliAdapter.consume(data, produced[1].getOffset(), data.remaining(), produced[1].getEncryptionLevel());
		assertContexts(cliState, true, true, true, true);
		produced = cliAdapter.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		srvAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = srvAdapter.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		assertEquals(0, mgr.getTickets(cliEngine.getSession()).length);
		cliAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = cliAdapter.produce();
		assertEquals(0, produced.length);
		assertEquals(1, mgr.getTickets(cliEngine.getSession()).length);
		assertEquals("REJ", edh.status.toString());
	}
	
	@Test
	public void testHandshakeWithHRR() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm())
				.ticketInfos(0xffffffffL);
		EngineParametersBuilder sepb = new EngineParametersBuilder()
				.skipEndOfEarlyData(true)
				.namedGroups(NamedGroup.SECP521R1)
				.delegatedTaskMode(DelegatedTaskMode.NONE);
		EngineParametersBuilder cepb = new EngineParametersBuilder()
				.skipEndOfEarlyData(true)
				.delegatedTaskMode(DelegatedTaskMode.NONE)
				.peerHost("quic3.snf4j.org")
				.namedGroups(NamedGroup.SECP256R1, NamedGroup.SECP521R1)
				.peerPort(8001);
		CryptoEngine cliEngine = new CryptoEngine(new HandshakeEngine(true, cepb.build(), ehb.build(), cliListener));
		CryptoEngine srvEngine = new CryptoEngine(new HandshakeEngine(false, sepb.build(), ehb.build(), srvListener));
		CryptoEngineAdapter cliAdapter = new CryptoEngineAdapter(cliEngine);
		CryptoEngineAdapter srvAdapter = new CryptoEngineAdapter(srvEngine);
		cliListener.onInit(INITIAL_SALT_V1, DEST_CID);
		srvListener.onInit(INITIAL_SALT_V1, DEST_CID);
		cliEngine.start();
		srvEngine.start();
		
		ProducedCrypto[] produced = cliAdapter.produce();
		assertEquals(1, produced.length);
		ByteBuffer data = produced[0].getData();
		assertSame(EncryptionLevel.INITIAL, produced[0].getEncryptionLevel());
		srvAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = srvAdapter.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		assertSame(EncryptionLevel.INITIAL, produced[0].getEncryptionLevel());
		cliAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = cliAdapter.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		assertSame(EncryptionLevel.INITIAL, produced[0].getEncryptionLevel());
		srvAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = srvAdapter.produce();
		assertEquals(2, produced.length);
		data = produced[0].getData();
		cliAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		data = produced[1].getData();
		cliAdapter.consume(data, produced[1].getOffset(), data.remaining(), produced[1].getEncryptionLevel());
		produced = cliAdapter.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		srvAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = srvAdapter.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		ISessionManager mgr = cliEngine.getSession().getManager();
		assertEquals(0, mgr.getTickets(cliEngine.getSession()).length);
		cliAdapter.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = cliAdapter.produce();
		assertEquals(0, produced.length);
		assertEquals(1, mgr.getTickets(cliEngine.getSession()).length);	
	}
	
	class TestEDHandler implements IEarlyDataHandler {
		
		byte[] data;
		
		StringBuilder status = new StringBuilder();
		
		TestEDHandler(byte[] data) {
			this.data = data;
		}
		
		@Override
		public boolean hasEarlyData() {
			return true;
		}

		@Override
		public byte[] nextEarlyData(String protocol) {
			byte[] data = this.data;
			this.data = null;
			return data;
		}

		@Override
		public void acceptedEarlyData() {
			status.append("ACC");
		}

		@Override
		public void rejectedEarlyData() {
			status.append("REJ");
		}
		
	}
}
