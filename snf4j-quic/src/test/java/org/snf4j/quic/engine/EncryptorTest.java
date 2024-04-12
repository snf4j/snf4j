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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import org.junit.Assume;
import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.crypto.ChaCha20HeaderProtection;
import org.snf4j.quic.crypto.QuicKeySchedule;
import org.snf4j.quic.engine.CryptorTest.TestProtector;
import org.snf4j.tls.cipher.CipherSuiteSpec;
import org.snf4j.tls.crypto.AESAead;
import org.snf4j.tls.crypto.AeadEncrypt;
import org.snf4j.tls.crypto.ChaCha20Aead;
import org.snf4j.tls.crypto.Hash;
import org.snf4j.tls.crypto.Hkdf;
import org.snf4j.tls.crypto.IAead;
import org.snf4j.tls.crypto.IAeadDecrypt;
import org.snf4j.tls.crypto.IAeadEncrypt;
import org.snf4j.tls.crypto.TrafficKeys;

public class EncryptorTest extends CommonTest {

	static final byte[] INITIAL_SALT_V1 = bytes("38762cf7f55934b34d179ae6a4c80cadccbb7f0a");
	
	static final byte[] DEST_CID = bytes("8394c8f03e515708");

	@Test
	public void testProtectPacketByClient() throws Exception {
		Hkdf h = new Hkdf(Hash.SHA256.createMac());
		QuicKeySchedule ks = new QuicKeySchedule(h, CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		
		ks.deriveInitialSecrets(INITIAL_SALT_V1, DEST_CID);
		TrafficKeys keys = ks.deriveInitialTrafficKeys();
		IAeadEncrypt aead = keys.getAeadEncrypt(true);
		HeaderProtector protector = new HeaderProtector(aead.getAead(), ks.deriveInitialHeaderProtectionKeys().getClientKey());
		Encryptor encryptor = new Encryptor(aead, protector, keys.getIv(true), 1000);
		
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
		
		header[0] ^= mask[0] & 0x0f;
		header[18] ^= mask[1];
		header[19] ^= mask[2];
		header[20] ^= mask[3];
		header[21] ^= mask[4];
		assertArrayEquals(bytes("c000000001088394c8f03e5157080000449e7b9aec34"), header);
		
		byte[] packet = bytes("c000000001088394c8f03e5157080000449e7b9aec34d1b1c98dd7689fb8ec11" + 
				"d242b123dc9bd8bab936b47d92ec356c0bab7df5976d27cd449f63300099f399" + 
				"1c260ec4c60d17b31f8429157bb35a1282a643a8d2262cad67500cadb8e7378c" + 
				"8eb7539ec4d4905fed1bee1fc8aafba17c750e2c7ace01e6005f80fcb7df6212" + 
				"30c83711b39343fa028cea7f7fb5ff89eac2308249a02252155e2347b63d58c5" + 
				"457afd84d05dfffdb20392844ae812154682e9cf012f9021a6f0be17ddd0c208" + 
				"4dce25ff9b06cde535d0f920a2db1bf362c23e596d11a4f5a6cf3948838a3aec" + 
				"4e15daf8500a6ef69ec4e3feb6b1d98e610ac8b7ec3faf6ad760b7bad1db4ba3" + 
				"485e8a94dc250ae3fdb41ed15fb6a8e5eba0fc3dd60bc8e30c5c4287e53805db" + 
				"059ae0648db2f64264ed5e39be2e20d82df566da8dd5998ccabdae053060ae6c" + 
				"7b4378e846d29f37ed7b4ea9ec5d82e7961b7f25a9323851f681d582363aa5f8" + 
				"9937f5a67258bf63ad6f1a0b1d96dbd4faddfcefc5266ba6611722395c906556" + 
				"be52afe3f565636ad1b17d508b73d8743eeb524be22b3dcbc2c7468d54119c74" + 
				"68449a13d8e3b95811a198f3491de3e7fe942b330407abf82a4ed7c1b311663a" + 
				"c69890f4157015853d91e923037c227a33cdd5ec281ca3f79c44546b9d90ca00" + 
				"f064c99e3dd97911d39fe9c5d0b23a229a234cb36186c4819e8b9c5927726632" + 
				"291d6a418211cc2962e20fe47feb3edf330f2c603a9d48c0fcb5699dbfe58964" + 
				"25c5bac4aee82e57a85aaf4e2513e4f05796b07ba2ee47d80506f8d2c25e50fd" + 
				"14de71e6c418559302f939b0e1abd576f279c4b2e0feb85c1f28ff18f58891ff" + 
				"ef132eef2fa09346aee33c28eb130ff28f5b766953334113211996d20011a198" + 
				"e3fc433f9f2541010ae17c1bf202580f6047472fb36857fe843b19f5984009dd" + 
				"c324044e847a4f4a0ab34f719595de37252d6235365e9b84392b061085349d73" + 
				"203a4a13e96f5432ec0fd4a1ee65accdd5e3904df54c1da510b0ff20dcc0c77f" + 
				"cb2c0e0eb605cb0504db87632cf3d8b4dae6e705769d1de354270123cb11450e" + 
				"fc60ac47683d7b8d0f811365565fd98c4c8eb936bcab8d069fc33bd801b03ade" + 
				"a2e1fbc5aa463d08ca19896d2bf59a071b851e6c239052172f296bfb5e724047" + 
				"90a2181014f3b94a4e97d117b438130368cc39dbb2d198065ae3986547926cd2" + 
				"162f40a29f0c3c8745c0f50fba3852e566d44575c29d39a03f0cda721984b6f4" + 
				"40591f355e12d439ff150aab7613499dbd49adabc8676eef023b15b65bfc5ca0" + 
				"6948109f23f350db82123535eb8a7433bdabcb909271a6ecbcb58b936a88cd4e" + 
				"8f2e6ff5800175f113253d8fa9ca8885c2f552e657dc603f252e1a8e308f76f0" + 
				"be79e2fb8f5d5fbbe2e30ecadd220723c8c0aea8078cdfcb3868263ff8f09400" + 
				"54da48781893a7e49ad5aff4af300cd804a6b6279ab3ff3afb64491c85194aab" + 
				"760d58a606654f9f4400e8b38591356fbf6425aca26dc85244259ff2b19c41b9" + 
				"f96f3ca9ec1dde434da7d2d392b905ddf3d1f9af93d1af5950bd493f5aa731b4" + 
				"056df31bd267b6b90a079831aaf579be0a39013137aac6d404f518cfd4684064" + 
				"7e78bfe706ca4cf5e9c5453e9f7cfd2b8b4c8d169a44e55c88d4a9a7f9474241" + 
				"e221af44860018ab0856972e194cd934");
		assertArrayEquals(packet, cat(header, encrypted));
	}
	
	@Test
	public void testProtectPacketByServer() throws Exception {
		Hkdf h = new Hkdf(Hash.SHA256.createMac());
		QuicKeySchedule ks = new QuicKeySchedule(h, CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		
		ks.deriveInitialSecrets(INITIAL_SALT_V1, DEST_CID);
		TrafficKeys keys = ks.deriveInitialTrafficKeys();
		IAeadEncrypt aead = keys.getAeadEncrypt(false);
		HeaderProtector protector = new HeaderProtector(aead.getAead(), ks.deriveInitialHeaderProtectionKeys().getServerKey());
		Encryptor encryptor = new Encryptor(aead, protector, keys.getIv(false), 1000);

		byte[] payload = bytes("02000000000600405a020000560303eefce7f7b37ba1d1632e96677825ddf739" + 
				"88cfc79825df566dc5430b9a045a1200130100002e00330024001d00209d3c94" + 
				"0d89690b84d08a60993c144eca684d1081287c834d5311bcf32bb9da1a002b00" + 
				"020304");

		byte[] header = bytes("c1000000010008f067a5502a4262b50040750001");
		byte[] nonce = encryptor.nonce(1);
		
		byte[] encrypted = encryptor.getAead().encrypt(nonce, header, payload);
		byte[] sampled = split(encrypted, 2, 16)[1];
		assertArrayEquals(bytes("2cd0991cd25b0aac406a5816b6394100"), sampled);

		byte[] mask = encryptor.getProtector().deriveMask(sampled, 0);
		assertArrayEquals(bytes("2ec0d8356a"), split(mask,5)[0]);
	
		header[0] ^= mask[0] & 0x0f;
		header[18] ^= mask[1];
		header[19] ^= mask[2];
		assertArrayEquals(bytes("cf000000010008f067a5502a4262b5004075c0d9"), header);

		byte[] packet = bytes("cf000000010008f067a5502a4262b5004075c0d95a482cd0991cd25b0aac406a" + 
				"5816b6394100f37a1c69797554780bb38cc5a99f5ede4cf73c3ec2493a1839b3" + 
				"dbcba3f6ea46c5b7684df3548e7ddeb9c3bf9c73cc3f3bded74b562bfb19fb84" + 
				"022f8ef4cdd93795d77d06edbb7aaf2f58891850abbdca3d20398c276456cbc4" + 
				"2158407dd074ee"); 
		assertArrayEquals(packet, cat(header, encrypted));
	}

	@Test
	public void testUnprotectPacketByClient() throws Exception {
		Hkdf h = new Hkdf(Hash.SHA256.createMac());
		QuicKeySchedule ks = new QuicKeySchedule(h, CipherSuiteSpec.TLS_AES_128_GCM_SHA256);
		
		ks.deriveInitialSecrets(INITIAL_SALT_V1, DEST_CID);
		TrafficKeys keys = ks.deriveInitialTrafficKeys();
		IAeadDecrypt aead = keys.getAeadDecrypt(false);
		HeaderProtector protector = new HeaderProtector(aead.getAead(), ks.deriveInitialHeaderProtectionKeys().getServerKey());
		Decryptor decryptor = new Decryptor(aead, protector, keys.getIv(false), 1000, 1000);
		
		byte[] sampled = bytes("2cd0991cd25b0aac406a5816b6394100");
		byte[] mask = decryptor.getProtector().deriveMask(sampled, 0);
		assertArrayEquals(bytes("2ec0d8356a"), split(mask,5)[0]);
		
		byte[] header = bytes("cf000000010008f067a5502a4262b5004075c0d9");
		
		header[0] ^= mask[0] & 0x0f;
		header[18] ^= mask[1];
		header[19] ^= mask[2];
		assertArrayEquals(bytes("c1000000010008f067a5502a4262b50040750001"), header);
		
		byte[] nonce = decryptor.nonce(1);
		
		byte[] encrypted = bytes("5a482cd0991cd25b0aac406a" + 
				"5816b6394100f37a1c69797554780bb38cc5a99f5ede4cf73c3ec2493a1839b3" + 
				"dbcba3f6ea46c5b7684df3548e7ddeb9c3bf9c73cc3f3bded74b562bfb19fb84" + 
				"022f8ef4cdd93795d77d06edbb7aaf2f58891850abbdca3d20398c276456cbc4" + 
				"2158407dd074ee");

		byte[] payload = bytes("02000000000600405a020000560303eefce7f7b37ba1d1632e96677825ddf739" + 
				"88cfc79825df566dc5430b9a045a1200130100002e00330024001d00209d3c94" + 
				"0d89690b84d08a60993c144eca684d1081287c834d5311bcf32bb9da1a002b00" + 
				"020304");
				
		assertArrayEquals(payload, decryptor.getAead().decrypt(nonce, header, encrypted));
	}
	
	@Test
	public void testProtectPacketWithShortHeader() throws Exception {
		Assume.assumeTrue(JAVA11);
		ChaCha20Aead chacha = ChaCha20Aead.AEAD_CHACHA20_POLY1305;
		IAeadEncrypt aead = new AeadEncrypt(chacha.createKey(
				bytes("c6d98ff3441c3fe1b2182094f69caa2ed4b716b65488960a7a984979fb23e1c8")), 
				chacha);
		ChaCha20HeaderProtection.HP_CHACHA20.createKey(bytes("25a282b9e82f06f21f488917a4fc8f1b73573685608597d0efcb076b0ab7a7a4"));
		HeaderProtector protector = new HeaderProtector(aead.getAead(), ChaCha20HeaderProtection.HP_CHACHA20.createKey(
				bytes("25a282b9e82f06f21f488917a4fc8f1b73573685608597d0efcb076b0ab7a7a4")));
		Encryptor encryptor = new Encryptor(aead, protector, bytes("e0459b3474bdd0e44a41c144"), 1000);

		byte[] payload = bytes("01");

		byte[] header = bytes("4200bff4");
		byte[] nonce = encryptor.nonce(654360564);
		assertArrayEquals(bytes("e0459b3474bdd0e46d417eb0"), nonce);

		byte[] encrypted = encryptor.getAead().encrypt(nonce, header, payload);
		assertArrayEquals(bytes("655e5cd55c41f69080575d7999c25a5bfb"), encrypted);
		byte[] sampled = split(encrypted, 1, 16)[1];
		assertArrayEquals(bytes("5e5cd55c41f69080575d7999c25a5bfb"), sampled);

		byte[] mask = encryptor.getProtector().deriveMask(sampled, 0);
		assertArrayEquals(bytes("aefefe7d03"), mask);

		header[0] ^= mask[0] & 0x1f;
		header[1] ^= mask[1];
		header[2] ^= mask[2];
		header[3] ^= mask[3];
		assertArrayEquals(bytes("4cfe4189"), header);
		
		byte[] packet = bytes("4cfe4189655e5cd55c41f69080575d7999c25a5bfb");
		assertArrayEquals(packet, cat(header, encrypted));
	}
	
	@Test
	public void testIncPackets() throws Exception {
		Cryptor c = new Encryptor(new TestEncrypt(), new TestProtector(), bytes("11"), 1000);
		
		assertFalse(c.isConfidentialityLimitReached());
		c.incPackets(999);
		assertFalse(c.isConfidentialityLimitReached());
		c.incPackets(1);
		assertFalse(c.isConfidentialityLimitReached());
		c.incPackets(1);
		assertTrue(c.isConfidentialityLimitReached());
	}
	
	@Test
	public void testErase() throws Exception {
		TestProtector p = new TestProtector();
		TestEncrypt e = new TestEncrypt();
		byte[] iv = bytes("1020304f");
		Cryptor c = new Encryptor(e, p, iv, 11);
		
		assertEquals(0, p.eraseCount);
		assertEquals(0, e.eraseCount);
		assertArrayEquals(bytes("1020304f"), iv);
		c.erase(true);
		assertEquals(0, p.eraseCount);
		assertEquals(1, e.eraseCount);
		assertArrayEquals(bytes("00000000"), iv);
		assertSame(p, c.getProtector());
		
		p = new TestProtector();
		e = new TestEncrypt();
		iv = bytes("1020304f");
		c = new Encryptor(e, p, iv, 11);
		c.erase(false);
		assertEquals(1, p.eraseCount);
		assertEquals(1, e.eraseCount);
		assertArrayEquals(bytes("00000000"), iv);
		assertSame(p, c.getProtector());
	}
	
	class TestEncrypt implements IAeadEncrypt {

		int eraseCount;
		
		@Override
		public IAead getAead() {
			return AESAead.AEAD_AES_128_GCM;
		}

		@Override
		public byte[] encrypt(byte[] nonce, byte[] additionalData, byte[] plaintext) throws GeneralSecurityException {
			return null;
		}

		@Override
		public void encrypt(byte[] nonce, byte[] additionalData, ByteBuffer plaintext, ByteBuffer ciphertext)
				throws GeneralSecurityException {
		}

		@Override
		public void encrypt(byte[] nonce, byte[] additionalData, ByteBuffer[] plaintext, ByteBuffer ciphertext)
				throws GeneralSecurityException {
		}

		@Override
		public void erase() {
			eraseCount++;
		}
		
	}
}
