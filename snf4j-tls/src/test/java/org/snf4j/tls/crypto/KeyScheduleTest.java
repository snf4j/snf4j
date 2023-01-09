/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022-2023 SNF4J contributors
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
package org.snf4j.tls.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;

import org.junit.Test;
import org.snf4j.tls.CommonTest;
import org.snf4j.tls.cipher.CipherSuiteSpec;
import org.snf4j.tls.cipher.ICipherSuiteSpec;
import org.snf4j.tls.handshake.HandshakeType;

public class KeyScheduleTest extends CommonTest {
	
	final static Charset ASCII = StandardCharsets.US_ASCII;

	Hkdf h;
	
	TranscriptHash th;
	
	KeySchedule ks;

	MessageDigest md;
	
	int hashLen;
	
	byte[] emptyHash;
	
	@Override
	public void before() throws Exception {
		super.before();
		try {
			h = new Hkdf(Mac.getInstance("HmacSHA256"));
			th = new TranscriptHash(MessageDigest.getInstance("SHA-256"));
			ks = new KeySchedule(h, th, CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
			emptyHash = MessageDigest.getInstance("SHA-256").digest();
			hashLen = 32;
			md = MessageDigest.getInstance("SHA-256");
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Test
	public void testHkdfExpandLabel() throws Exception {
		byte[] secret = bytes("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
		byte[] context = bytes(1,2,3,4,5,6,7,8);
		
		byte[] data = ks.hkdfExpandLabel(secret, "xxx", context, 32);
		buffer.putShort((short) 32);
		String s = "tls13 xxx";
		buffer.put((byte) s.length());
		buffer.put(s.getBytes());
		buffer.put((byte) context.length);
		buffer.put(context);
		assertEquals(32, data.length);
		assertArrayEquals(h.expand(secret, buffer(), 32), data);
		
		buffer.clear();
		context = bytes();
		data = ks.hkdfExpandLabel(secret, "xxx".getBytes(), context, 32);
		buffer.putShort((short) 32);
		s = "xxx";
		buffer.put((byte) s.length());
		buffer.put(s.getBytes());
		buffer.put((byte) context.length);
		buffer.put(context);
		assertEquals(32, data.length);
		assertArrayEquals(h.expand(secret, buffer(), 32), data);
		
		buffer.clear();
		context = bytes();
		data = ks.hkdfExpandLabel(secret, "xxx".getBytes(), context, 255);
		buffer.putShort((short) 255);
		s = "xxx";
		buffer.put((byte) s.length());
		buffer.put(s.getBytes());
		buffer.put((byte) context.length);
		buffer.put(context);
		assertEquals(255, data.length);
		assertArrayEquals(h.expand(secret, buffer(), 255), data);

		buffer.clear();
		context = bytes();
		data = ks.hkdfExpandLabel(secret, "xxx".getBytes(), context, 0x1ff);
		buffer.putShort((short) 0x1ff);
		s = "xxx";
		buffer.put((byte) s.length());
		buffer.put(s.getBytes());
		buffer.put((byte) context.length);
		buffer.put(context);
		assertEquals(0x1ff, data.length);
		assertArrayEquals(h.expand(secret, buffer(), 0x1ff), data);
	}
	
	byte[] earlySecret(byte[] psk) throws InvalidKeyException {
		if (psk == null) {
			psk = new byte[hashLen];
		}
		return h.extract(new byte[hashLen], psk);
	}
	
	byte[] binderKey(byte[] earlySecret, boolean external) throws InvalidKeyException {
		String label = external ? "tls13 ext binder" : "tls13 res binder";
		
		return ks.hkdfExpandLabel(earlySecret, label.getBytes(ASCII), emptyHash, hashLen);
	}
	
	byte[] pskBinder(byte[] binderKey, byte[] context) throws Exception {
		byte[] fk = ks.hkdfExpandLabel(binderKey, "tls13 finished".getBytes(ASCII), new byte[0], hashLen);
		
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(fk, mac.getAlgorithm()));
		return mac.doFinal(context);
	}
	
	void assertKeyNotEquals(byte[] k1, byte[] k2) {
		assertEquals(k1.length, k2.length);
		for (int i=0; i<k1.length; i++) {
			if (k1[i] != k2[i]) {
				return;
			}
		}
		fail();
	}
	
	@Test
	public void testDeriveEarlySecret() throws Exception {
		ks.deriveEarlySecret();
		ks.deriveBinderKey();
		byte[] key = ks.computePskBinder("xxx".getBytes());
		byte[] es = earlySecret(null);
		byte[] bk = binderKey(es, false);
		assertArrayEquals(pskBinder(bk, md.digest("xxx".getBytes())), key);
		assertEquals(32, key.length);
		
		byte[] psk = new byte[hashLen];
		Arrays.fill(psk, (byte)0xab);
		
		ks.deriveEarlySecret(psk, false);
		ks.deriveBinderKey();
		byte[] key2 = ks.computePskBinder("xxx".getBytes());
		es = earlySecret(psk);
		bk = binderKey(es, false);
		assertArrayEquals(pskBinder(bk, md.digest("xxx".getBytes())), key2);
		assertEquals(32, key2.length);

		ks.deriveEarlySecret(psk, true);
		ks.deriveBinderKey();
		byte[] key3 = ks.computePskBinder("xxx".getBytes());
		es = earlySecret(psk);
		bk = binderKey(es, true);
		assertArrayEquals(pskBinder(bk, md.digest("xxx".getBytes())), key3);
		assertEquals(32, key3.length);
		
		assertKeyNotEquals(key, key2);
		assertKeyNotEquals(key, key3);
		assertKeyNotEquals(key2, key3);
		
		th.update(HandshakeType.CLIENT_HELLO, "CH".getBytes());
		th.updateHelloRetryRequest("HRR".getBytes());
		ks.deriveEarlySecret(psk, false);
		ks.deriveBinderKey();
		byte[] key4 = ks.computePskBinder("xxx".getBytes());
		es = earlySecret(psk);
		bk = binderKey(es, false);
		md.reset();
		byte[] ch1 = md.digest("CH".getBytes());
		md.reset();
		md.update(new byte[] {(byte)254,0,0,(byte)hashLen});
		md.update(ch1);
		md.update("HRR".getBytes());
		assertArrayEquals(pskBinder(bk, md.digest("xxx".getBytes())), key4);
		assertEquals(32, key4.length);
	}
	
	byte[] getSecret(KeySchedule ks, String name) throws Exception {
		Field f = KeySchedule.class.getDeclaredField(name);
		f.setAccessible(true);
		return (byte[]) f.get(ks);
	}

	SecretKeySpec getSecretKey(KeySchedule ks, String name) throws Exception {
		Field f = KeySchedule.class.getDeclaredField(name);
		f.setAccessible(true);
		return (SecretKeySpec) f.get(ks);
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
	public void testEraseEarlySecret() throws Exception {
		assertNull(getSecret(ks, "earlySecret"));
		ks.deriveEarlySecret();
		byte[] b = getSecret(ks, "earlySecret");
		assertNotErased(b);
		ks.eraseEarlySecret();
		assertErased(b);
		assertNull(getSecret(ks, "earlySecret"));
		ks.deriveEarlySecret();
		b = getSecret(ks, "earlySecret");
		assertNotErased(b);
		ks.deriveEarlySecret();
		assertErased(b);
		assertNotNull(getSecret(ks, "earlySecret"));			
	}
	
	@Test
	public void testEraseBinderKey() throws Exception {
		ks.deriveEarlySecret();
		assertNull(getSecret(ks, "binderKey"));
		ks.deriveBinderKey();
		byte[] b = getSecret(ks, "binderKey");
		assertNotErased(b);
		ks.eraseBinderKey();
		assertErased(b);
		assertNull(getSecret(ks, "binderKey"));	
		ks.deriveBinderKey();
		b = getSecret(ks, "binderKey");
		assertNotErased(b);
		ks.deriveBinderKey();
		assertErased(b);
		assertNotNull(getSecret(ks, "binderKey"));			
	}
	
	@Test
	public void testDeriveEarlyTrafficSecret() throws Exception {
		ks.deriveEarlySecret();
		ks.deriveEarlyTrafficSecret();
		byte[] es = getSecret(ks, "earlySecret");
		byte[] ets = ks.hkdfExpandLabel(es, "tls13 c e traffic".getBytes(ASCII), md.digest(), hashLen);
		assertArrayEquals(ets, getSecret(ks, "earlyTrafficSecret"));
		th.update(HandshakeType.CLIENT_HELLO, "CH".getBytes());
		ks.deriveEarlyTrafficSecret();
		ets = ks.hkdfExpandLabel(es, "tls13 c e traffic".getBytes(ASCII), md.digest("CH".getBytes()), hashLen);
		assertArrayEquals(ets, getSecret(ks, "earlyTrafficSecret"));
	}
	
	@Test
	public void testEraseEarlyTrafficSecret() throws Exception {
		ks.deriveEarlySecret();
		assertNull(getSecret(ks, "earlyTrafficSecret"));
		ks.deriveEarlyTrafficSecret();
		byte[] b = getSecret(ks, "earlyTrafficSecret");
		assertNotErased(b);
		ks.eraseEarlyTrafficSecret();
		assertErased(b);
		assertNull(getSecret(ks, "earlyTrafficSecret"));	
		ks.deriveEarlyTrafficSecret();
		b = getSecret(ks, "earlyTrafficSecret");
		assertNotErased(b);
		ks.deriveEarlyTrafficSecret();
		assertErased(b);
		assertNotNull(getSecret(ks, "earlyTrafficSecret"));			
	}
	
	@Test
	public void testDeriveEarlyTrafficKeys() throws Exception {
		ks.deriveEarlySecret();
		ks.deriveEarlyTrafficSecret();
		ks.deriveEarlyTrafficKeys();
		byte[] ets = getSecret(ks, "earlyTrafficSecret");
		byte[] iv = ks.hkdfExpandLabel(ets, "tls13 iv".getBytes(ASCII), new byte[0], 12);
		assertArrayEquals(iv, getSecret(ks, "clientIv"));
		byte[] k = ks.hkdfExpandLabel(ets, "tls13 key".getBytes(ASCII), new byte[0], 16);
		SecretKeySpec key = getSecretKey(ks, "clientKey");
		assertArrayEquals(k, key.getEncoded());
		assertEquals("AES", key.getAlgorithm());
		assertNull(getSecret(ks, "serverIv"));
		assertNull(getSecretKey(ks, "serverKey"));
		iv = getSecret(ks, "clientIv");
		assertNotErased(iv);
		ks.deriveEarlyTrafficKeys();
		assertErased(iv);
		assertNotNull(getSecret(ks, "clientIv"));
		assertNotSame(iv, getSecret(ks, "clientIv"));
	}
	
	@Test
	public void testHandshakeTrafficSecrets() throws Exception {
		byte[] secret = bytes("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
		ks.deriveEarlySecret();
		byte[] es = getSecret(ks, "earlySecret");
		ks.deriveHandshakeSecret(secret);
		byte[] d = ks.hkdfExpandLabel(es, "tls13 derived".getBytes(ASCII), emptyHash, hashLen);
		byte[] hs = h.extract(d, secret);
		assertArrayEquals(hs, getSecret(ks,"handshakeSecret"));
		
		th.update(HandshakeType.CLIENT_HELLO, "CH".getBytes());
		th.update(HandshakeType.SERVER_HELLO, "SH".getBytes());
		th.update(HandshakeType.ENCRYPTED_EXTENSIONS, "EE".getBytes());
		ks.deriveHandshakeTrafficSecrets();
		byte[] traffic = ks.hkdfExpandLabel(hs, "tls13 c hs traffic".getBytes(ASCII), md.digest("CHSH".getBytes()), hashLen);
		assertArrayEquals(traffic, getSecret(ks,"clientHandshakeTrafficSecret"));
		traffic = ks.hkdfExpandLabel(hs, "tls13 s hs traffic".getBytes(ASCII), md.digest("CHSH".getBytes()), hashLen);
		assertArrayEquals(traffic, getSecret(ks,"serverHandshakeTrafficSecret"));
	}
	
	@Test
	public void testEraseHandshakeSecret() throws Exception {
		byte[] secret = bytes("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
		ks.deriveEarlySecret();
		assertNull(getSecret(ks, "handshakeSecret"));
		ks.deriveHandshakeSecret(secret);
		byte[] b = getSecret(ks, "handshakeSecret");
		assertNotErased(b);
		ks.eraseHandshakeSecret();
		assertErased(b);
		assertNull(getSecret(ks, "handshakeSecret"));	
		ks.deriveHandshakeSecret(secret);
		b = getSecret(ks, "handshakeSecret");
		assertNotErased(b);
		ks.deriveHandshakeSecret(secret);
		assertErased(b);
		assertNotNull(getSecret(ks, "handshakeSecret"));			
	}
	
	@Test
	public void testEraseHandshakeTrafficSecrets() throws Exception {
		byte[] secret = bytes("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
		ks.deriveEarlySecret();
		ks.deriveHandshakeSecret(secret);
		assertNull(getSecret(ks, "clientHandshakeTrafficSecret"));
		assertNull(getSecret(ks, "serverHandshakeTrafficSecret"));
		ks.deriveHandshakeTrafficSecrets();
		byte[] cb = getSecret(ks, "clientHandshakeTrafficSecret");
		byte[] sb = getSecret(ks, "serverHandshakeTrafficSecret");
		assertNotErased(cb);
		assertNotErased(sb);
		ks.eraseHandshakeTrafficSecrets();
		assertErased(cb);
		assertErased(sb);
		assertNull(getSecret(ks, "clientHandshakeTrafficSecret"));	
		assertNull(getSecret(ks, "serverHandshakeTrafficSecret"));	
		ks.deriveHandshakeTrafficSecrets();
		cb = getSecret(ks, "clientHandshakeTrafficSecret");
		sb = getSecret(ks, "serverHandshakeTrafficSecret");
		assertNotErased(cb);
		assertNotErased(sb);
		ks.deriveHandshakeTrafficSecrets();
		assertErased(cb);
		assertErased(sb);
		assertNotNull(getSecret(ks, "clientHandshakeTrafficSecret"));	
		assertNotNull(getSecret(ks, "serverHandshakeTrafficSecret"));	
	}
	
	@Test
	public void testDeriveHandshakeTrafficKeys() throws Exception {
		byte[] secret = bytes("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
		ks.deriveEarlySecret();
		ks.deriveHandshakeSecret(secret);
		ks.deriveHandshakeTrafficSecrets();
		ks.deriveHandshakeTrafficKeys();		
		byte[] hts = getSecret(ks, "clientHandshakeTrafficSecret");
		byte[] iv = ks.hkdfExpandLabel(hts, "tls13 iv".getBytes(ASCII), new byte[0], 12);
		assertArrayEquals(iv, getSecret(ks, "clientIv"));
		byte[] k = ks.hkdfExpandLabel(hts, "tls13 key".getBytes(ASCII), new byte[0], 16);
		SecretKeySpec key = getSecretKey(ks, "clientKey");
		assertArrayEquals(k, key.getEncoded());
		
		hts = getSecret(ks, "serverHandshakeTrafficSecret");
		iv = ks.hkdfExpandLabel(hts, "tls13 iv".getBytes(ASCII), new byte[0], 12);
		assertArrayEquals(iv, getSecret(ks, "serverIv"));
		k = ks.hkdfExpandLabel(hts, "tls13 key".getBytes(ASCII), new byte[0], 16);
		key = getSecretKey(ks, "serverKey");
		assertArrayEquals(k, key.getEncoded());
		assertEquals("AES", key.getAlgorithm());
		
		iv = getSecret(ks, "clientIv");
		byte[] iv2 = getSecret(ks, "serverIv");
		assertNotErased(iv);
		assertNotErased(iv2);
		ks.deriveHandshakeTrafficKeys();
		assertErased(iv);
		assertErased(iv2);
		assertNotNull(getSecret(ks, "clientIv"));
		assertNotNull(getSecret(ks, "serverIv"));
		assertNotSame(iv, getSecret(ks, "clientIv"));
		assertNotSame(iv2, getSecret(ks, "serverIv"));
	}
	
	@Test
	public void testComputeServerVerifyData() throws Exception {
		byte[] secret = bytes("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
		ks.deriveEarlySecret();
		ks.deriveHandshakeSecret(secret);
		th.update(HandshakeType.CLIENT_HELLO, "CH".getBytes(ASCII));
		th.update(HandshakeType.SERVER_HELLO, "SH".getBytes(ASCII));
		ks.deriveHandshakeTrafficSecrets();

		byte[] hts = getSecret(ks, "serverHandshakeTrafficSecret");
		
		th.update(HandshakeType.ENCRYPTED_EXTENSIONS, "EE".getBytes(ASCII));
		th.update(HandshakeType.CERTIFICATE, "CT".getBytes(ASCII));
		th.update(HandshakeType.CERTIFICATE_VERIFY, "CV".getBytes(ASCII));
		th.update(HandshakeType.FINISHED, "F".getBytes(ASCII));
		byte[] vd = ks.computeServerVerifyData();
		
		byte[] key = ks.hkdfExpandLabel(hts, "tls13 finished".getBytes(ASCII), new byte[0], hashLen);
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(key, mac.getAlgorithm()));
		mac.update(md.digest("CHSHEECTCV".getBytes(ASCII)));
		assertArrayEquals(mac.doFinal(), vd);
	}
	
	@Test
	public void testComputeClientVerifyData() throws Exception {
		byte[] secret = bytes("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
		ks.deriveEarlySecret();
		ks.deriveHandshakeSecret(secret);
		th.update(HandshakeType.CLIENT_HELLO, "CH".getBytes(ASCII));
		th.update(HandshakeType.SERVER_HELLO, "SH".getBytes(ASCII));
		ks.deriveHandshakeTrafficSecrets();

		byte[] hts = getSecret(ks, "clientHandshakeTrafficSecret");
		
		th.update(HandshakeType.ENCRYPTED_EXTENSIONS, "EE".getBytes(ASCII));
		th.update(HandshakeType.CERTIFICATE, "CT".getBytes(ASCII));
		th.update(HandshakeType.CERTIFICATE_VERIFY, "CV".getBytes(ASCII));
		th.update(HandshakeType.FINISHED, "F".getBytes(ASCII));
		th.update(HandshakeType.CERTIFICATE, "ct".getBytes(ASCII));
		th.update(HandshakeType.CERTIFICATE_VERIFY, "cv".getBytes(ASCII));
		th.update(HandshakeType.FINISHED, "f".getBytes(ASCII));
		byte[] vd = ks.computeClientVerifyData();
		
		byte[] key = ks.hkdfExpandLabel(hts, "tls13 finished".getBytes(ASCII), new byte[0], hashLen);
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(key, mac.getAlgorithm()));
		mac.update(md.digest("CHSHEECTCVFctcv".getBytes(ASCII)));
		assertArrayEquals(mac.doFinal(), vd);
	}
	
	@Test
	public void testApplicationTrafficSecrets() throws Exception {
		byte[] secret = bytes("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
		ks.deriveEarlySecret();
		ks.deriveHandshakeSecret(secret);
		th.update(HandshakeType.CLIENT_HELLO, "CH".getBytes(ASCII));
		th.update(HandshakeType.SERVER_HELLO, "SH".getBytes(ASCII));
		byte[] hs = getSecret(ks, "handshakeSecret");
		byte[] d = ks.hkdfExpandLabel(hs, "tls13 derived".getBytes(ASCII), emptyHash, hashLen);
		byte[] ms = h.extract(d, new byte[hashLen]);
		ks.deriveMasterSecret();
		assertArrayEquals(ms, getSecret(ks,"masterSecret"));

		th.update(HandshakeType.ENCRYPTED_EXTENSIONS, "EE".getBytes(ASCII));
		th.update(HandshakeType.CERTIFICATE, "CT".getBytes(ASCII));
		th.update(HandshakeType.CERTIFICATE_VERIFY, "CV".getBytes(ASCII));
		th.update(HandshakeType.FINISHED, "F".getBytes(ASCII));
		th.update(HandshakeType.CERTIFICATE, "ct".getBytes(ASCII));
		ks.deriveApplicationTrafficSecrets();
		byte[] traffic = ks.hkdfExpandLabel(ms, "tls13 c ap traffic".getBytes(ASCII), md.digest("CHSHEECTCVF".getBytes()), hashLen);
		assertArrayEquals(traffic, getSecret(ks,"clientApplicationTrafficSecret"));
		traffic = ks.hkdfExpandLabel(ms, "tls13 s ap traffic".getBytes(ASCII), md.digest("CHSHEECTCVF".getBytes()), hashLen);
		assertArrayEquals(traffic, getSecret(ks,"serverApplicationTrafficSecret"));
	}
	
	@Test
	public void testEraseMasterSecret() throws Exception {
		byte[] secret = bytes("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
		ks.deriveEarlySecret();
		ks.deriveHandshakeSecret(secret);
		assertNull(getSecret(ks, "masterSecret"));
		ks.deriveMasterSecret();
		byte[] b = getSecret(ks, "masterSecret");
		assertNotErased(b);
		ks.eraseMasterSecret();
		assertErased(b);
		assertNull(getSecret(ks, "masterSecret"));	
		ks.deriveMasterSecret();
		b = getSecret(ks, "masterSecret");
		assertNotErased(b);
		ks.deriveMasterSecret();
		assertErased(b);
		assertNotNull(getSecret(ks, "masterSecret"));			
	}
	
	@Test
	public void testEraseApplicationTrafficSecrets() throws Exception {
		byte[] secret = bytes("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
		ks.deriveEarlySecret();
		ks.deriveHandshakeSecret(secret);
		ks.deriveMasterSecret();
		assertNull(getSecret(ks, "clientApplicationTrafficSecret"));
		assertNull(getSecret(ks, "serverApplicationTrafficSecret"));
		ks.deriveApplicationTrafficSecrets();
		byte[] cb = getSecret(ks, "clientApplicationTrafficSecret");
		byte[] sb = getSecret(ks, "serverApplicationTrafficSecret");
		assertNotErased(cb);
		assertNotErased(sb);
		ks.eraseApplicationTrafficSecrets();
		assertErased(cb);
		assertErased(sb);
		assertNull(getSecret(ks, "clientApplicationTrafficSecret"));	
		assertNull(getSecret(ks, "serverApplicationTrafficSecret"));	
		ks.deriveApplicationTrafficSecrets();
		cb = getSecret(ks, "clientApplicationTrafficSecret");
		sb = getSecret(ks, "serverApplicationTrafficSecret");
		assertNotErased(cb);
		assertNotErased(sb);
		ks.deriveApplicationTrafficSecrets();
		assertErased(cb);
		assertErased(sb);
		assertNotNull(getSecret(ks, "clientApplicationTrafficSecret"));	
		assertNotNull(getSecret(ks, "serverApplicationTrafficSecret"));	
	}
	
	DestroyFailedException exception;
	int exceptionCount;
	int destroyCount;
	
	@Test
	public void testEraseTrafficKeys() throws Exception {
		byte[] secret = bytes("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
		ks = new TestKeySchedule(h, th, CipherSuiteSpec.TLS_AES_256_GCM_SHA384);
		ks.deriveEarlySecret();
		ks.deriveHandshakeSecret(secret);
		ks.deriveHandshakeTrafficSecrets();

		exception = null;
		exceptionCount = 0;
		destroyCount = 0;
		ks.eraseTrafficKeys();
		assertEquals(0, exceptionCount);
		assertEquals(0, destroyCount);
		ks.deriveHandshakeTrafficKeys();
		byte[] iv1 = getSecret(ks, "clientIv");
		byte[] iv2 = getSecret(ks, "serverIv");
		assertNotErased(iv1);
		assertNotErased(iv2);
		ks.eraseTrafficKeys();
		assertEquals(0, exceptionCount);
		assertEquals(2, destroyCount);
		assertErased(iv1);
		assertErased(iv2);
		assertNull(getSecret(ks, "clientIv"));
		assertNull(getSecret(ks, "serverIv"));
		assertNull(getSecretKey(ks, "clientKey"));
		assertNull(getSecretKey(ks, "serverKey"));

		exception = new DestroyFailedException();
		exceptionCount = 0;
		destroyCount = 0;
		ks.deriveHandshakeTrafficKeys();
		iv1 = getSecret(ks, "clientIv");
		iv2 = getSecret(ks, "serverIv");
		assertNotErased(iv1);
		assertNotErased(iv2);
		ks.eraseTrafficKeys();
		assertEquals(2, exceptionCount);
		assertEquals(2, destroyCount);
		assertErased(iv1);
		assertErased(iv2);
		assertNull(getSecret(ks, "clientIv"));
		assertNull(getSecret(ks, "serverIv"));
		assertNull(getSecretKey(ks, "clientKey"));
		assertNull(getSecretKey(ks, "serverKey"));
	}
	
	@Test
	public void testCheckDerived() throws Exception {
		try {
			ks.deriveBinderKey();
			fail();
		} catch (IllegalStateException e) {
			assertEquals("Early Secret not derived", e.getMessage());
		}
		try {
			ks.deriveEarlyTrafficSecret();
			fail();
		} catch (IllegalStateException e) {
			assertEquals("Early Secret not derived", e.getMessage());
		}
		try {
			ks.deriveHandshakeSecret(new byte[32]);
			fail();
		} catch (IllegalStateException e) {
			assertEquals("Early Secret not derived", e.getMessage());
		}
		
		ks.deriveEarlySecret();
		try {
			ks.computePskBinder("CH".getBytes());
			fail();
		} catch (IllegalStateException e) {
			assertEquals("Binder Key not derived", e.getMessage());
		}
		ks.deriveBinderKey();
		ks.computePskBinder("CH".getBytes());

		try {
			ks.deriveEarlyTrafficKeys();
			fail();
		} catch (IllegalStateException e) {
			assertEquals("Early Traffic Secret not derived", e.getMessage());
		}
		ks.deriveEarlyTrafficSecret();
		ks.deriveEarlyTrafficKeys();

		try {
			ks.deriveHandshakeTrafficSecrets();
			fail();
		} catch (IllegalStateException e) {
			assertEquals("Handshake Secret not derived", e.getMessage());
		}
		ks.deriveHandshakeSecret(new byte[32]);
		try {
			ks.deriveHandshakeTrafficKeys();
			fail();
		} catch (IllegalStateException e) {
			assertEquals("Handshake Traffic Secrets not derived", e.getMessage());
		}
		try {
			ks.computeClientVerifyData();
			fail();
		} catch (IllegalStateException e) {
			assertEquals("Handshake Traffic Secrets not derived", e.getMessage());
		}
		try {
			ks.computeServerVerifyData();
			fail();
		} catch (IllegalStateException e) {
			assertEquals("Handshake Traffic Secrets not derived", e.getMessage());
		}
		ks.deriveHandshakeTrafficSecrets();
		ks.computeClientVerifyData();
		ks.computeServerVerifyData();
		ks.deriveHandshakeTrafficKeys();
	}
	
	class TestSecretKey extends SecretKeySpec {

		private static final long serialVersionUID = 1L;

		public TestSecretKey(byte[] key, String algorithm) {
			super(key, algorithm);
		}

		@Override
		public void	destroy() throws DestroyFailedException {
			destroyCount++;
			if (exception != null) {
				exceptionCount++;
				throw exception;
			}
		}
	}
	
	class TestKeySchedule extends KeySchedule {

		public TestKeySchedule(IHkdf hkdf, ITranscriptHash transcriptHash, ICipherSuiteSpec cipherSuiteSpec) {
			super(hkdf, transcriptHash, cipherSuiteSpec);
		}

		@Test
		protected SecretKey createKey(byte[] key, ICipherSuiteSpec cipherSuiteSpec) {
			return new TestSecretKey(key, "AES");
		}
	}
}
