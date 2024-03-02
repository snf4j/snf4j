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
package org.snf4j.quic.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.util.Arrays;

import org.junit.Assume;
import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.tls.cipher.CipherSuiteSpec;
import org.snf4j.tls.cipher.ICipherSuiteSpec;
import org.snf4j.tls.crypto.AbstractKeySchedule;
import org.snf4j.tls.crypto.Hash;
import org.snf4j.tls.crypto.Hkdf;
import org.snf4j.tls.crypto.IHkdf;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.crypto.TrafficKeys;
import org.snf4j.tls.crypto.TranscriptHash;

public class QuicKeyScheduleTest extends CommonTest {

	final static Charset ASCII = StandardCharsets.US_ASCII;
	
	static final byte[] INITIAL_SALT_V1 = bytes("38762cf7f55934b34d179ae6a4c80cadccbb7f0a");
	
	static final byte[] DEST_CID = bytes("8394c8f03e515708");
	
	Hkdf h;

	QuicKeySchedule ks;
	
	public void before() throws Exception {
		h = new Hkdf(Hash.SHA256.createMac());
		ks = new QuicKeySchedule(h, CipherSuiteSpec.TLS_AES_128_GCM_SHA256, AESHeaderProtection.HP_AES_128);
	}
	
	byte[] getSecret(KeySchedule ks, String name) throws Exception {
		Field f = KeySchedule.class.getDeclaredField(name);
		f.setAccessible(true);
		return (byte[]) f.get(ks);
	}

	void setSecret(KeySchedule ks, String name, byte[] secret) throws Exception {
		Field f = KeySchedule.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(ks, secret);
	}

	byte[] getSecret(QuicKeySchedule ks, String name) throws Exception {
		Field f = QuicKeySchedule.class.getDeclaredField(name);
		f.setAccessible(true);
		return (byte[]) f.get(ks);
	}

	void setSecret(QuicKeySchedule ks, String name, byte[] secret) throws Exception {
		Field f = QuicKeySchedule.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(ks, secret);
	}
	
	static void assertNotErased(byte[] b) {
		for (int i=0; i<b.length; ++i) {
			if (b[i] != 0) {
				return;
			}
		}
		fail();
	}

	static void assertErased(byte[] b) {
		for (int i=0; i<b.length; ++i) {
			if (b[i] != 0) {
				fail();
			}
		}
	}
	
	@Test
	public void testDeriveInitialTrafficKeys() throws Exception {
		ks.deriveInitialSecrets(INITIAL_SALT_V1, DEST_CID);
		TrafficKeys keys = ks.deriveInitialTrafficKeys();
		assertArrayEquals(bytes("1f369613dd76d5467730efcbe3b1a22d"), keys.getKey(true).getEncoded());
		assertArrayEquals(bytes("fa044b2f42a3fd3b46fb255c"), keys.getIv(true));
		assertArrayEquals(bytes("cf3a5331653c364c88f0f379b6067e37"), keys.getKey(false).getEncoded());
		assertArrayEquals(bytes("0ac1493ca1905853b0bba03e"), keys.getIv(false));
	}

	void assertIllegalState(QuicKeySchedule ks, boolean hp) throws Exception {
		try {
			if (hp) {
				ks.deriveInitialHeaderProtectionKeys();
			}
			else {
				ks.deriveInitialTrafficKeys();
			}
			fail();
		}
		catch (IllegalStateException e) {
			assertEquals("Initial Secrets not derived", e.getMessage());
		}
	}

	@Test
	public void testEraseInitialSecrets() throws Exception {
		ks.eraseInitialSecrets();
		assertIllegalState(ks, false);
		assertIllegalState(ks, true);
		ks.deriveInitialSecrets(INITIAL_SALT_V1, DEST_CID);
		ks.deriveInitialTrafficKeys();
		ks.deriveInitialHeaderProtectionKeys();
		byte[] s1 = getSecret(ks, "clientInitialSecret");
		byte[] s2 = getSecret(ks, "serverInitialSecret");
		assertNotErased(s1);
		assertNotErased(s2);
		ks.deriveInitialSecrets(INITIAL_SALT_V1, DEST_CID);
		assertErased(s1);
		assertErased(s2);
		s1 = getSecret(ks, "clientInitialSecret");
		s2 = getSecret(ks, "serverInitialSecret");
		assertNotErased(s1);
		assertNotErased(s2);
		ks.eraseInitialSecrets();
		assertErased(s1);
		assertErased(s2);
		assertIllegalState(ks, false);
		assertIllegalState(ks, true);
		assertNull(getSecret(ks, "clientInitialSecret"));
		assertNull(getSecret(ks, "serverInitialSecret"));
	}

	void assertIllegalStateNG(QuicKeySchedule ks, boolean client) throws Exception {
		try {
			ks.deriveNextGenerationTrafficKey(client);
			fail();
		}
		catch (IllegalStateException e) {
			assertEquals("Next Generation Secrets not derived", e.getMessage());
		}
	}
	
	@Test
	public void testEraseNextGenerationSecrets() throws Exception {
		KeySchedule ks0 = new KeySchedule(h, new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		setSecret(ks0, "clientApplicationTrafficSecret", bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea"));
		setSecret(ks0, "serverApplicationTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));

		ks.eraseNextGenerationSecrets();
		assertIllegalStateNG(ks,true);
		assertIllegalStateNG(ks,false);
		ks.deriveNextGenerationSecrets(ks0);
		byte[] s1 = getSecret(ks, "clientNextGenerationSecret");
		byte[] s2 = getSecret(ks, "serverNextGenerationSecret");
		assertNotErased(s1);
		assertNotErased(s2);
		ks.deriveNextGenerationTrafficKey(true);
		assertErased(s1);
		assertNotErased(s2);
		s1 = getSecret(ks, "clientNextGenerationSecret");
		assertNotErased(s1);
		assertNotErased(s2);
		ks.deriveNextGenerationTrafficKey(false);
		assertNotErased(s1);
		assertErased(s2);
		s2 = getSecret(ks, "serverNextGenerationSecret");
		assertNotErased(s1);
		assertNotErased(s2);
		ks.eraseNextGenerationSecrets();
		assertErased(s1);
		assertErased(s2);
		assertNull(getSecret(ks, "clientInitialSecret"));
		assertNull(getSecret(ks, "serverInitialSecret"));
		assertNull(getSecret(ks, "clientNextGenerationSecret"));
		assertNull(getSecret(ks, "serverNextGenerationSecret"));
	}
	
	@Test
	public void testEraseAll() throws Exception {
		KeySchedule ks0 = new KeySchedule(h, new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		setSecret(ks0, "clientApplicationTrafficSecret", bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea"));
		setSecret(ks0, "serverApplicationTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));

		ks.deriveInitialSecrets(INITIAL_SALT_V1, DEST_CID);
		ks.deriveNextGenerationSecrets(ks0);
		byte[] s1 = getSecret(ks, "clientInitialSecret");
		byte[] s2 = getSecret(ks, "serverInitialSecret");
		byte[] s3 = getSecret(ks, "clientNextGenerationSecret");
		byte[] s4 = getSecret(ks, "serverNextGenerationSecret");
		assertNotErased(s1);
		assertNotErased(s2);
		assertNotErased(s3);
		assertNotErased(s4);
		ks.eraseAll();
		assertErased(s1);
		assertErased(s2);
		assertErased(s3);
		assertErased(s4);
		assertNull(getSecret(ks, "clientNextGenerationSecret"));
		assertNull(getSecret(ks, "serverNextGenerationSecret"));
		
	}
	
	@Test
	public void testDeriveInitialHeaderProtectionKeys() throws Exception {
		ks.deriveInitialSecrets(INITIAL_SALT_V1, DEST_CID);
		SecretKeys keys = ks.deriveInitialHeaderProtectionKeys();
		assertArrayEquals(bytes("9f50449e04a0e810283a1e9933adedd2"), keys.getKey(true).getEncoded());
		assertArrayEquals(bytes("c206b8d9b9f0f37644430b490eeaa314"), keys.getKey(false).getEncoded());
	}

	@Test
	public void testDeriveEarlyKey() throws Exception {
		KeySchedule ks0 = new KeySchedule(h, new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		setSecret(ks0, "earlyTrafficSecret", bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea"));
		TrafficKeys keys = ks.deriveEarlyTrafficKey(ks0);
		assertArrayEquals(bytes("1f369613dd76d5467730efcbe3b1a22d"), keys.getKey(true).getEncoded());
		assertArrayEquals(bytes("fa044b2f42a3fd3b46fb255c"), keys.getIv(true));
		assertNull(keys.getKey(false));
		assertNull(keys.getIv(false));
		
		SecretKeys hpKeys = ks.deriveEarlyHeaderProtectionKey(ks0);
		assertArrayEquals(bytes("9f50449e04a0e810283a1e9933adedd2"), hpKeys.getKey(true).getEncoded());
		assertNull(hpKeys.getKey(false));
	}
	
	@Test
	public void testDeriveEarlyKeyChaCha20() throws Exception {
		Assume.assumeTrue(JAVA11);
		KeySchedule ks = new KeySchedule(h, new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_CHACHA20_POLY1305_SHA256);
		setSecret(ks, "earlyTrafficSecret", bytes("9ac312a7f877468ebe69422748ad00a15443f18203a07d6060f688f30f21632b"));
		QuicKeySchedule qks = new QuicKeySchedule(h, CipherSuiteSpec.TLS_CHACHA20_POLY1305_SHA256, ChaCha20HeaderProtection.HP_CHACHA20);
		TrafficKeys keys = qks.deriveEarlyTrafficKey(ks);
		assertArrayEquals(bytes("c6d98ff3441c3fe1b2182094f69caa2ed4b716b65488960a7a984979fb23e1c8"), keys.getKey(true).getEncoded());
		assertArrayEquals(bytes("e0459b3474bdd0e44a41c144"), keys.getIv(true));
		assertNull(keys.getKey(false));
		assertNull(keys.getIv(false));
		
		SecretKeys hpKeys = qks.deriveEarlyHeaderProtectionKey(ks);
		assertArrayEquals(bytes("25a282b9e82f06f21f488917a4fc8f1b73573685608597d0efcb076b0ab7a7a4"), hpKeys.getKey(true).getEncoded());
		assertNull(hpKeys.getKey(false));
	}

	@Test
	public void testDeriveHandshakeKeys() throws Exception {
		KeySchedule ks0 = new KeySchedule(h, new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		setSecret(ks0, "clientHandshakeTrafficSecret", bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea"));
		setSecret(ks0, "serverHandshakeTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));
		
		TrafficKeys keys = ks.deriveHandshakeTrafficKeys(ks0);
		assertArrayEquals(bytes("1f369613dd76d5467730efcbe3b1a22d"), keys.getKey(true).getEncoded());
		assertArrayEquals(bytes("fa044b2f42a3fd3b46fb255c"), keys.getIv(true));
		assertArrayEquals(bytes("cf3a5331653c364c88f0f379b6067e37"), keys.getKey(false).getEncoded());
		assertArrayEquals(bytes("0ac1493ca1905853b0bba03e"), keys.getIv(false));
		
		SecretKeys hpKeys = ks.deriveHandshakeHeaderProtectionKeys(ks0);
		assertArrayEquals(bytes("9f50449e04a0e810283a1e9933adedd2"), hpKeys.getKey(true).getEncoded());
		assertArrayEquals(bytes("c206b8d9b9f0f37644430b490eeaa314"), hpKeys.getKey(false).getEncoded());
	}
	
	@Test
	public void testDeriveHandshakeKeysChaCha20() throws Exception {
		Assume.assumeTrue(JAVA11);
		
		//client
		KeySchedule ks = new KeySchedule(h, new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_CHACHA20_POLY1305_SHA256);
		setSecret(ks, "clientHandshakeTrafficSecret", bytes("9ac312a7f877468ebe69422748ad00a15443f18203a07d6060f688f30f21632b"));
		setSecret(ks, "serverHandshakeTrafficSecret", bytes("0000000000000000000000000000000000000000000000000000000000000000"));
		QuicKeySchedule qks = new QuicKeySchedule(h, CipherSuiteSpec.TLS_CHACHA20_POLY1305_SHA256, ChaCha20HeaderProtection.HP_CHACHA20);
		TrafficKeys keys = qks.deriveHandshakeTrafficKeys(ks);
		assertArrayEquals(bytes("c6d98ff3441c3fe1b2182094f69caa2ed4b716b65488960a7a984979fb23e1c8"), keys.getKey(true).getEncoded());
		assertArrayEquals(bytes("e0459b3474bdd0e44a41c144"), keys.getIv(true));
		assertNotNull(keys.getKey(false));
		assertNotNull(keys.getIv(false));
		assertNotEquals(keys.getKey(true), keys.getKey(false));
		
		SecretKeys hpKeys = qks.deriveHandshakeHeaderProtectionKeys(ks);
		assertArrayEquals(bytes("25a282b9e82f06f21f488917a4fc8f1b73573685608597d0efcb076b0ab7a7a4"), hpKeys.getKey(true).getEncoded());
		assertNotNull(hpKeys.getKey(false));
		assertNotEquals(hpKeys.getKey(true), hpKeys.getKey(false));

		//server
		ks = new KeySchedule(h, new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_CHACHA20_POLY1305_SHA256);
		setSecret(ks, "serverHandshakeTrafficSecret", bytes("9ac312a7f877468ebe69422748ad00a15443f18203a07d6060f688f30f21632b"));
		setSecret(ks, "clientHandshakeTrafficSecret", bytes("0000000000000000000000000000000000000000000000000000000000000000"));
		qks = new QuicKeySchedule(h, CipherSuiteSpec.TLS_CHACHA20_POLY1305_SHA256, ChaCha20HeaderProtection.HP_CHACHA20);
		keys = qks.deriveHandshakeTrafficKeys(ks);
		assertArrayEquals(bytes("c6d98ff3441c3fe1b2182094f69caa2ed4b716b65488960a7a984979fb23e1c8"), keys.getKey(false).getEncoded());
		assertArrayEquals(bytes("e0459b3474bdd0e44a41c144"), keys.getIv(false));
		assertNotNull(keys.getKey(true));
		assertNotNull(keys.getIv(true));
		assertNotEquals(keys.getKey(true), keys.getKey(false));
		
		hpKeys = qks.deriveHandshakeHeaderProtectionKeys(ks);
		assertArrayEquals(bytes("25a282b9e82f06f21f488917a4fc8f1b73573685608597d0efcb076b0ab7a7a4"), hpKeys.getKey(false).getEncoded());
		assertNotEquals(hpKeys.getKey(true), hpKeys.getKey(false));	
	}

	@Test
	public void testDeriveApplicationKeys() throws Exception {
		KeySchedule ks0 = new KeySchedule(h, new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		setSecret(ks0, "clientApplicationTrafficSecret", bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea"));
		setSecret(ks0, "serverApplicationTrafficSecret", bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b"));
		
		TrafficKeys keys = ks.deriveApplicationTrafficKeys(ks0);
		assertArrayEquals(bytes("1f369613dd76d5467730efcbe3b1a22d"), keys.getKey(true).getEncoded());
		assertArrayEquals(bytes("fa044b2f42a3fd3b46fb255c"), keys.getIv(true));
		assertArrayEquals(bytes("cf3a5331653c364c88f0f379b6067e37"), keys.getKey(false).getEncoded());
		assertArrayEquals(bytes("0ac1493ca1905853b0bba03e"), keys.getIv(false));
		
		SecretKeys hpKeys = ks.deriveApplicationHeaderProtectionKeys(ks0);
		assertArrayEquals(bytes("9f50449e04a0e810283a1e9933adedd2"), hpKeys.getKey(true).getEncoded());
		assertArrayEquals(bytes("c206b8d9b9f0f37644430b490eeaa314"), hpKeys.getKey(false).getEncoded());
	}
	
	@Test
	public void testDeriveApplicationKeysChaCha20() throws Exception {
		Assume.assumeTrue(JAVA11);
		
		//client
		KeySchedule ks = new KeySchedule(h, new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_CHACHA20_POLY1305_SHA256);
		setSecret(ks, "clientApplicationTrafficSecret", bytes("9ac312a7f877468ebe69422748ad00a15443f18203a07d6060f688f30f21632b"));
		setSecret(ks, "serverApplicationTrafficSecret", bytes("0000000000000000000000000000000000000000000000000000000000000000"));
		QuicKeySchedule qks = new QuicKeySchedule(h, CipherSuiteSpec.TLS_CHACHA20_POLY1305_SHA256, ChaCha20HeaderProtection.HP_CHACHA20);
		TrafficKeys keys = qks.deriveApplicationTrafficKeys(ks);
		assertArrayEquals(bytes("c6d98ff3441c3fe1b2182094f69caa2ed4b716b65488960a7a984979fb23e1c8"), keys.getKey(true).getEncoded());
		assertArrayEquals(bytes("e0459b3474bdd0e44a41c144"), keys.getIv(true));
		assertNotNull(keys.getKey(false));
		assertNotNull(keys.getIv(false));
		assertNotEquals(keys.getKey(true), keys.getKey(false));
		
		SecretKeys hpKeys = qks.deriveApplicationHeaderProtectionKeys(ks);
		assertArrayEquals(bytes("25a282b9e82f06f21f488917a4fc8f1b73573685608597d0efcb076b0ab7a7a4"), hpKeys.getKey(true).getEncoded());
		assertNotNull(hpKeys.getKey(false));
		assertNotEquals(hpKeys.getKey(true), hpKeys.getKey(false));

		//server
		ks = new KeySchedule(h, new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_CHACHA20_POLY1305_SHA256);
		setSecret(ks, "serverHandshakeTrafficSecret", bytes("9ac312a7f877468ebe69422748ad00a15443f18203a07d6060f688f30f21632b"));
		setSecret(ks, "clientHandshakeTrafficSecret", bytes("0000000000000000000000000000000000000000000000000000000000000000"));
		qks = new QuicKeySchedule(h, CipherSuiteSpec.TLS_CHACHA20_POLY1305_SHA256, ChaCha20HeaderProtection.HP_CHACHA20);
		keys = qks.deriveHandshakeTrafficKeys(ks);
		assertArrayEquals(bytes("c6d98ff3441c3fe1b2182094f69caa2ed4b716b65488960a7a984979fb23e1c8"), keys.getKey(false).getEncoded());
		assertArrayEquals(bytes("e0459b3474bdd0e44a41c144"), keys.getIv(false));
		assertNotNull(keys.getKey(true));
		assertNotNull(keys.getIv(true));
		assertNotEquals(keys.getKey(true), keys.getKey(false));
		
		hpKeys = qks.deriveHandshakeHeaderProtectionKeys(ks);
		assertArrayEquals(bytes("25a282b9e82f06f21f488917a4fc8f1b73573685608597d0efcb076b0ab7a7a4"), hpKeys.getKey(false).getEncoded());
		assertNotEquals(hpKeys.getKey(true), hpKeys.getKey(false));	
	}

	@Test
	public void testDeriveNextGenerationSecrets() throws Exception {
		KeySchedule ks0 = new KeySchedule(h, new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		byte[] csecret = bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea");
		byte[] ssecret = bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b");
		setSecret(ks0, "clientApplicationTrafficSecret", csecret);
		setSecret(ks0, "serverApplicationTrafficSecret", ssecret);
		ks.deriveNextGenerationSecrets(ks0);
		
		TestKeySchedule tks = new TestKeySchedule(h, CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		byte[] cexpected = tks.hkdfExpandLabel(csecret, "quic ku", new byte[0], 32);
		byte[] sexpected = tks.hkdfExpandLabel(ssecret, "quic ku", new byte[0], 32);
		
		assertArrayEquals(cexpected, getSecret(ks, "clientNextGenerationSecret"));
		assertArrayEquals(sexpected, getSecret(ks, "serverNextGenerationSecret"));
	}
	
	@Test
	public void testDeriveNextGenerationSecretsChaCha20() throws Exception {
		Assume.assumeTrue(JAVA11);

		KeySchedule ks = new KeySchedule(h, new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_CHACHA20_POLY1305_SHA256);
		setSecret(ks, "clientApplicationTrafficSecret", bytes("9ac312a7f877468ebe69422748ad00a15443f18203a07d6060f688f30f21632b"));
		setSecret(ks, "serverApplicationTrafficSecret", bytes("0000000000000000000000000000000000000000000000000000000000000000"));
		QuicKeySchedule qks = new QuicKeySchedule(h, CipherSuiteSpec.TLS_CHACHA20_POLY1305_SHA256, ChaCha20HeaderProtection.HP_CHACHA20);
		qks.deriveNextGenerationSecrets(ks);
		byte[] csecret = getSecret(qks, "clientNextGenerationSecret");
		byte[] ssecret = getSecret(qks, "serverNextGenerationSecret");
		assertArrayEquals(bytes("1223504755036d556342ee9361d253421a826c9ecdf3c7148684b36b714881f9"), csecret);
		assertFalse(Arrays.equals(csecret, ssecret));

		ks = new KeySchedule(h, new TranscriptHash(MessageDigest.getInstance("SHA-256")), CipherSuiteSpec.TLS_CHACHA20_POLY1305_SHA256);
		setSecret(ks, "serverApplicationTrafficSecret", bytes("9ac312a7f877468ebe69422748ad00a15443f18203a07d6060f688f30f21632b"));
		setSecret(ks, "clientApplicationTrafficSecret", bytes("0000000000000000000000000000000000000000000000000000000000000000"));
		qks = new QuicKeySchedule(h, CipherSuiteSpec.TLS_CHACHA20_POLY1305_SHA256, ChaCha20HeaderProtection.HP_CHACHA20);
		qks.deriveNextGenerationSecrets(ks);
		csecret = getSecret(qks, "clientNextGenerationSecret");
		ssecret = getSecret(qks, "serverNextGenerationSecret");
		assertArrayEquals(bytes("1223504755036d556342ee9361d253421a826c9ecdf3c7148684b36b714881f9"), ssecret);
		assertFalse(Arrays.equals(csecret, ssecret));
	}
	
	@Test
	public void testDeriveNextGenerationTrafficKey() throws Exception {
		byte[] csecret = bytes("c00cf151ca5be075ed0ebfb5c80323c42d6b7db67881289af4008f1f6c357aea");
		byte[] ssecret = bytes("3c199828fd139efd216c155ad844cc81fb82fa8d7446fa7d78be803acdda951b");
		setSecret(ks, "clientNextGenerationSecret", csecret.clone());
		setSecret(ks, "serverNextGenerationSecret", ssecret.clone());

		TrafficKeys keys = ks.deriveNextGenerationTrafficKey(true);
		assertArrayEquals(bytes("1f369613dd76d5467730efcbe3b1a22d"), keys.getKey(true).getEncoded());
		assertArrayEquals(bytes("fa044b2f42a3fd3b46fb255c"), keys.getIv(true));
		assertNull(keys.getKey(false));
		assertNull(keys.getIv(false));

		TestKeySchedule tks = new TestKeySchedule(h, CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		byte[] expected = tks.hkdfExpandLabel(csecret, "quic ku", new byte[0], 32);
		assertArrayEquals(expected, getSecret(ks, "clientNextGenerationSecret"));
		
		keys = ks.deriveNextGenerationTrafficKey(false);
		assertArrayEquals(bytes("cf3a5331653c364c88f0f379b6067e37"), keys.getKey(false).getEncoded());
		assertArrayEquals(bytes("0ac1493ca1905853b0bba03e"), keys.getIv(false));
		assertNull(keys.getKey(true));
		assertNull(keys.getIv(true));

		expected = tks.hkdfExpandLabel(ssecret, "quic ku", new byte[0], 32);
		assertArrayEquals(expected, getSecret(ks, "serverNextGenerationSecret"));
	}
	
	class TestKeySchedule extends AbstractKeySchedule {

		protected TestKeySchedule(IHkdf hkdf, ICipherSuiteSpec cipherSuiteSpec) {
			super(hkdf, cipherSuiteSpec);
		}

		@Override
		public void eraseAll() {
		}
		
		@Override
		public byte[] hkdfExpandLabel(byte[] secret, String label, byte[] context, int length) throws InvalidKeyException {
			return super.hkdfExpandLabel(secret, label, context, length);
		}
		
	}
}
