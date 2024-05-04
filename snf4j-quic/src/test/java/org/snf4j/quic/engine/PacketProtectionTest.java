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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.Version;
import org.snf4j.quic.engine.crypto.CryptoEngine;
import org.snf4j.quic.engine.crypto.ProducedCrypto;
import org.snf4j.quic.frame.CryptoFrame;
import org.snf4j.quic.frame.MultiPaddingFrame;
import org.snf4j.quic.frame.PingFrame;
import org.snf4j.quic.packet.HandshakePacket;
import org.snf4j.quic.packet.IPacket;
import org.snf4j.quic.packet.InitialPacket;
import org.snf4j.quic.packet.OneRttPacket;
import org.snf4j.quic.packet.PacketType;
import org.snf4j.quic.packet.RetryPacket;
import org.snf4j.quic.packet.VersionNegotiationPacket;
import org.snf4j.quic.packet.ZeroRttPacket;
import org.snf4j.tls.engine.DelegatedTaskMode;
import org.snf4j.tls.engine.EngineHandlerBuilder;
import org.snf4j.tls.engine.EngineParametersBuilder;
import org.snf4j.tls.engine.HandshakeEngine;
import org.snf4j.tls.engine.IEarlyDataHandler;

public class PacketProtectionTest extends CommonTest {

	static final byte[] INITIAL_SALT_V1 = bytes("38762cf7f55934b34d179ae6a4c80cadccbb7f0a");

	static final byte[] DEST_CID = bytes("8394c8f03e515708");

	static final String PROTECTED_PACKET1 = "c000000001088394c8f03e5157080000 449e7b9aec34d1b1c98dd7689fb8ec11"
			+ "d242b123dc9bd8bab936b47d92ec356c 0bab7df5976d27cd449f63300099f399"
			+ "1c260ec4c60d17b31f8429157bb35a12 82a643a8d2262cad67500cadb8e7378c"
			+ "8eb7539ec4d4905fed1bee1fc8aafba1 7c750e2c7ace01e6005f80fcb7df6212"
			+ "30c83711b39343fa028cea7f7fb5ff89 eac2308249a02252155e2347b63d58c5"
			+ "457afd84d05dfffdb20392844ae81215 4682e9cf012f9021a6f0be17ddd0c208"
			+ "4dce25ff9b06cde535d0f920a2db1bf3 62c23e596d11a4f5a6cf3948838a3aec"
			+ "4e15daf8500a6ef69ec4e3feb6b1d98e 610ac8b7ec3faf6ad760b7bad1db4ba3"
			+ "485e8a94dc250ae3fdb41ed15fb6a8e5 eba0fc3dd60bc8e30c5c4287e53805db"
			+ "059ae0648db2f64264ed5e39be2e20d8 2df566da8dd5998ccabdae053060ae6c"
			+ "7b4378e846d29f37ed7b4ea9ec5d82e7 961b7f25a9323851f681d582363aa5f8"
			+ "9937f5a67258bf63ad6f1a0b1d96dbd4 faddfcefc5266ba6611722395c906556"
			+ "be52afe3f565636ad1b17d508b73d874 3eeb524be22b3dcbc2c7468d54119c74"
			+ "68449a13d8e3b95811a198f3491de3e7 fe942b330407abf82a4ed7c1b311663a"
			+ "c69890f4157015853d91e923037c227a 33cdd5ec281ca3f79c44546b9d90ca00"
			+ "f064c99e3dd97911d39fe9c5d0b23a22 9a234cb36186c4819e8b9c5927726632"
			+ "291d6a418211cc2962e20fe47feb3edf 330f2c603a9d48c0fcb5699dbfe58964"
			+ "25c5bac4aee82e57a85aaf4e2513e4f0 5796b07ba2ee47d80506f8d2c25e50fd"
			+ "14de71e6c418559302f939b0e1abd576 f279c4b2e0feb85c1f28ff18f58891ff"
			+ "ef132eef2fa09346aee33c28eb130ff2 8f5b766953334113211996d20011a198"
			+ "e3fc433f9f2541010ae17c1bf202580f 6047472fb36857fe843b19f5984009dd"
			+ "c324044e847a4f4a0ab34f719595de37 252d6235365e9b84392b061085349d73"
			+ "203a4a13e96f5432ec0fd4a1ee65accd d5e3904df54c1da510b0ff20dcc0c77f"
			+ "cb2c0e0eb605cb0504db87632cf3d8b4 dae6e705769d1de354270123cb11450e"
			+ "fc60ac47683d7b8d0f811365565fd98c 4c8eb936bcab8d069fc33bd801b03ade"
			+ "a2e1fbc5aa463d08ca19896d2bf59a07 1b851e6c239052172f296bfb5e724047"
			+ "90a2181014f3b94a4e97d117b4381303 68cc39dbb2d198065ae3986547926cd2"
			+ "162f40a29f0c3c8745c0f50fba3852e5 66d44575c29d39a03f0cda721984b6f4"
			+ "40591f355e12d439ff150aab7613499d bd49adabc8676eef023b15b65bfc5ca0"
			+ "6948109f23f350db82123535eb8a7433 bdabcb909271a6ecbcb58b936a88cd4e"
			+ "8f2e6ff5800175f113253d8fa9ca8885 c2f552e657dc603f252e1a8e308f76f0"
			+ "be79e2fb8f5d5fbbe2e30ecadd220723 c8c0aea8078cdfcb3868263ff8f09400"
			+ "54da48781893a7e49ad5aff4af300cd8 04a6b6279ab3ff3afb64491c85194aab"
			+ "760d58a606654f9f4400e8b38591356f bf6425aca26dc85244259ff2b19c41b9"
			+ "f96f3ca9ec1dde434da7d2d392b905dd f3d1f9af93d1af5950bd493f5aa731b4"
			+ "056df31bd267b6b90a079831aaf579be 0a39013137aac6d404f518cfd4684064"
			+ "7e78bfe706ca4cf5e9c5453e9f7cfd2b 8b4c8d169a44e55c88d4a9a7f9474241" + "e221af44860018ab0856972e194cd934";

	QuicState cliState, srvState;

	CryptoEngineStateListener cliListener, srvListener;

	final ByteBuffer dst = ByteBuffer.allocate(10000);

	PacketProtection protection;

	TestListener listener;
	
	@Override
	public void before() throws Exception {
		super.before();
		cliState = new QuicState(true);
		srvState = new QuicState(false);
		cliListener = new CryptoEngineStateListener(cliState);
		srvListener = new CryptoEngineStateListener(srvState);
		dst.clear();
		listener = new TestListener();
		protection = new PacketProtection(listener);
	}

	@Test
	public void testProtect() throws Exception {
		cliListener.onInit(INITIAL_SALT_V1, DEST_CID);
		InitialPacket p = new InitialPacket(DEST_CID, 2, bytes(), Version.V1, bytes()) {
			@Override
			public int getHeaderBytes(long largestPn, int expansion, ByteBuffer dst) {
				ByteBuffer buf = ByteBuffer.allocate(100);

				super.getHeaderBytes(largestPn, expansion, buf);
				buf.flip();
				assertArrayEquals(bytes("c0 00000001 08 8394c8f03e515708 00 00 449b02"), bytes(buf));
				dst.put(bytes("c3 00000001 08 8394c8f03e515708 00 00 449e00000002"));
				return 0x49e;
			}
		};
		buffer("0040f1010000ed0303ebf8fa56f129 39b9584a3896472ec40bb863cfd3e868"
				+ "04fe3a47f06a2b69484c000004130113 02010000c000000010000e00000b6578"
				+ "616d706c652e636f6dff01000100000a 00080006001d00170018001000070005"
				+ "04616c706e0005000501000000000033 00260024001d00209370b2c9caa47fba"
				+ "baf4559fedba753de171fa71f50f1ce1 5d43e994ec74d748002b000302030400"
				+ "0d0010000e0403050306030203080408 050806002d00020101001c0002400100"
				+ "3900320408ffffffffffffffff050480 00ffff07048000ffff08011001048000"
				+ "75300901100f088394c8f03e51570806 048000ffff");
		CryptoFrame f = CryptoFrame.getParser().parse(buffer, buffer.remaining(), 6);
		p.getFrames().add(f);
		p.getFrames().add(new MultiPaddingFrame(1162 - f.getLength()));
		assertEquals(917, p.getFrames().get(1).getLength());

		protection.protect(cliState, p, dst);

		dst.flip();
		assertArrayEquals(bytes(PROTECTED_PACKET1), bytes(dst));

		p = new InitialPacket(DEST_CID, 2, bytes(), Version.V1, bytes());
		p.getFrames().add(f);
		p.getFrames().add(new MultiPaddingFrame(1162 - f.getLength()));

		// Performance test
		int count = 0;
		buffer(PROTECTED_PACKET1);
		for (int i = 0; i < count; ++i) {
			p = new InitialPacket(DEST_CID, count, bytes(), Version.V1, bytes());
			p.getFrames().add(f);
			p.getFrames().add(new MultiPaddingFrame(1162 - f.getLength()));
			dst.clear();
			protection.protect(cliState, p, dst);
		}
	}

	@Test
	public void testProtectRetry() throws Exception {
		RetryPacket p = new RetryPacket(DEST_CID, bytes("0001"), Version.V1, bytes(), bytes(16));
		
		protection.protect(cliState, p, dst);
		dst.putInt(0);
		dst.flip();
		p = (RetryPacket) protection.unprotect(srvState, dst);
		assertEquals(0, dst.remaining());
		assertArrayEquals(DEST_CID, p.getDestinationId());
		assertArrayEquals(bytes("0001"), p.getSourceId());
		assertSame(Version.V1, p.getVersion());
		assertArrayEquals(bytes("00010203"), p.getToken());
		assertEquals(16, p.getIntegrityTag().length);
	}

	@Test
	public void testProtectVersionNegotiation() throws Exception {
		VersionNegotiationPacket p = new VersionNegotiationPacket(DEST_CID, bytes("0001"), Version.V1);
		
		protection.protect(cliState, p, dst);
		dst.putInt(0);
		dst.flip();
		p = (VersionNegotiationPacket) protection.unprotect(srvState, dst);
		assertEquals(0, dst.remaining());
		assertArrayEquals(DEST_CID, p.getDestinationId());
		assertArrayEquals(bytes("0001"), p.getSourceId());
		assertSame(Version.V0, p.getVersion());
		assertEquals(2, p.getSupportedVersions().length);
		assertSame(Version.V1, p.getSupportedVersions()[0]);
		assertSame(Version.V0, p.getSupportedVersions()[1]);
	}
	
	@Test
	public void testProtectWithoutKeys() throws Exception {
		IPacket p = new InitialPacket(DEST_CID, 2, bytes(), Version.V1, bytes());
		p.getFrames().add(new MultiPaddingFrame(10));
		try {
			protection.protect(srvState, p, dst);
			fail();
		}
		catch (QuicException e) {
			assertSame(TransportError.INTERNAL_ERROR, e.getTransportError());
		}
		assertEquals("", listener.status.toString());
		protection.protect(cliState, p, dst);
		dst.flip();
		assertEquals("INI-V1", listener.status.toString());
		cliState.getContext(EncryptionLevel.INITIAL).erase();
		try {
			protection.protect(cliState, p, dst);
			fail();
		}
		catch (QuicException e) {
			assertSame(TransportError.INTERNAL_ERROR, e.getTransportError());
		}
		srvListener.onInit(INITIAL_SALT_V1, DEST_CID);
		assertNotNull(protection.unprotect(srvState, dst));
		
		p = new HandshakePacket(DEST_CID, 2, bytes(), Version.V1);
		p.getFrames().add(new MultiPaddingFrame(10));

		try {
			protection.protect(cliState, p, dst);
			fail();
		}
		catch (QuicException e) {
			assertSame(TransportError.INTERNAL_ERROR, e.getTransportError());
		}
		
	}

	@Test
	public void testProtectTooShortPacket() throws Exception {
		cliListener.onInit(INITIAL_SALT_V1, DEST_CID);
		InitialPacket p = new InitialPacket(DEST_CID, 2, bytes(), Version.V1, bytes());
		p.getFrames().add(new MultiPaddingFrame(2));
		try {
			protection.protect(cliState, p, dst);
			fail();
		}
		catch (QuicException e) {
			assertSame(TransportError.INTERNAL_ERROR, e.getTransportError());
			assertTrue(e.getCause() instanceof BufferUnderflowException);
		}
	}
	
	@Test
	public void testUnprotect() throws Exception {
		srvListener.onInit(INITIAL_SALT_V1, DEST_CID);
		buffer(PROTECTED_PACKET1);
		IPacket p = protection.unprotect(srvState, buffer);
		assertEquals(917, p.getFrames().get(1).getLength());
		assertEquals(0, buffer.remaining());

		buffer(PROTECTED_PACKET1 + "00");
		p = protection.unprotect(srvState, buffer);
		assertEquals(917, p.getFrames().get(1).getLength());
		assertEquals(1, buffer.remaining());

		// Performance test
		int count = 0;
		buffer(PROTECTED_PACKET1);
		for (int i = 0; i < count; ++i) {
			ByteBuffer buffer = this.buffer.duplicate();
			p = protection.unprotect(srvState, buffer);
			p.getFrames();
		}
	}

	@Test
	public void testUnprotectEncodedPacketNumber() throws Exception {
		cliListener.onInit(INITIAL_SALT_V1, DEST_CID);
		srvListener.onInit(INITIAL_SALT_V1, DEST_CID);
		InitialPacket p = new InitialPacket(DEST_CID, 0, bytes(), Version.V1, bytes());
		p.getFrames().add(new MultiPaddingFrame(10));
		protection.protect(cliState, p, dst);
		dst.flip();
		IPacket p2 = protection.unprotect(srvState, dst);
		assertEquals(0, p2.getPacketNumber());

		p = new InitialPacket(DEST_CID, 0xff, bytes(), Version.V1, bytes());
		p.getFrames().add(new MultiPaddingFrame(10));
		dst.clear();
		protection.protect(cliState, p, dst);
		dst.flip();
		p2 = protection.unprotect(srvState, dst);
		assertEquals(0xff, p2.getPacketNumber());

		p = new InitialPacket(DEST_CID, 0xffff, bytes(), Version.V1, bytes());
		p.getFrames().add(new MultiPaddingFrame(10));
		dst.clear();
		protection.protect(cliState, p, dst);
		dst.flip();
		p2 = protection.unprotect(srvState, dst);
		assertEquals(0xffff, p2.getPacketNumber());

		p = new InitialPacket(DEST_CID, 0xffffff, bytes(), Version.V1, bytes());
		p.getFrames().add(new MultiPaddingFrame(10));
		dst.clear();
		protection.protect(cliState, p, dst);
		dst.flip();
		p2 = protection.unprotect(srvState, dst);
		assertEquals(0xffffff, p2.getPacketNumber());

		p = new InitialPacket(DEST_CID, 0xfffffffeL, bytes(), Version.V1, bytes());
		p.getFrames().add(new MultiPaddingFrame(10));
		dst.clear();
		protection.protect(cliState, p, dst);
		dst.flip();
		p2 = protection.unprotect(srvState, dst);
		assertEquals(0xfffffffeL, p2.getPacketNumber());

		srvState.getSpace(EncryptionLevel.INITIAL).updateProcessed(0xffffffffL - 0x8001);
		cliState.getSpace(EncryptionLevel.INITIAL).updateAcked(0xffffffffL - 0x8001);
		p = new InitialPacket(DEST_CID, 0xffffffffL, bytes(), Version.V1, bytes());
		p.getFrames().add(new MultiPaddingFrame(10));
		dst.clear();
		protection.protect(cliState, p, dst);
		dst.flip();
		p2 = protection.unprotect(srvState, dst);
		assertEquals(0xffffffffL, p2.getPacketNumber());

		srvState.getSpace(EncryptionLevel.INITIAL).updateProcessed(0x1fffffffffL - 0x800001);
		cliState.getSpace(EncryptionLevel.INITIAL).updateAcked(0x1fffffffffL - 0x800001);
		p = new InitialPacket(DEST_CID, 0x1fffffffffL, bytes(), Version.V1, bytes());
		p.getFrames().add(new MultiPaddingFrame(10));
		dst.clear();
		protection.protect(cliState, p, dst);
		dst.flip();
		p2 = protection.unprotect(srvState, dst);
		assertEquals(0x1fffffffffL, p2.getPacketNumber());

		srvState.getSpace(EncryptionLevel.INITIAL).updateProcessed(0x3fffffffffffffffL - 0x40000002);
		cliState.getSpace(EncryptionLevel.INITIAL).updateAcked(0x3fffffffffffffffL - 0x40000002);
		p = new InitialPacket(DEST_CID, 0x3fffffffffffffffL, bytes(), Version.V1, bytes());
		p.getFrames().add(new MultiPaddingFrame(10));
		dst.clear();
		protection.protect(cliState, p, dst);
		dst.flip();
		p2 = protection.unprotect(srvState, dst);
		assertEquals(0x3fffffffffffffffL, p2.getPacketNumber());
		
	}
	
	@Test
	public void testUnprotectTooShortPacket() throws Exception {
		srvListener.onInit(INITIAL_SALT_V1, DEST_CID);
		assertNull(protection.unprotect(srvState, buffer("c0 00000001 01 01 01 02 00 13 00 " + hex(bytes(18)) + "00")));
		assertEquals(1, buffer.remaining());
			
		assertNull(protection.unprotect(cliState, buffer("")));
	}

	@Test
	public void testUprotectCorrupted() throws Exception {
		cliListener.onInit(INITIAL_SALT_V1, DEST_CID);
		srvListener.onInit(INITIAL_SALT_V1, DEST_CID);
		InitialPacket p = new InitialPacket(DEST_CID, 0, bytes(), Version.V1, bytes());
		p.getFrames().add(new MultiPaddingFrame(10));
		protection.protect(cliState, p, dst);
		dst.put((byte) 0);
		dst.flip();
		dst.put(dst.limit()-2, (byte) (dst.get(dst.limit()-2) + 1));
		assertNull(protection.unprotect(srvState, dst));
		assertEquals(1, dst.remaining());
	}
	
	@Test
	public void testUprotectWithoutKeys() throws Exception {
		cliListener.onInit(INITIAL_SALT_V1, DEST_CID);
		srvListener.onInit(INITIAL_SALT_V1, DEST_CID);
		EncryptionContext ctx1 = cliState.getContext(EncryptionLevel.INITIAL);
		EncryptionContext ctx2 = cliState.getContext(EncryptionLevel.HANDSHAKE);
		ctx2.setEncryptor(ctx1.getEncryptor());
		IPacket p = new HandshakePacket(DEST_CID, 0, bytes(), Version.V1);
		p.getFrames().add(new MultiPaddingFrame(10));
		protection.protect(cliState, p, dst);
		dst.put((byte) 0);
		dst.flip();
		PacketBuffer buf = srvState.getContext(EncryptionLevel.HANDSHAKE).getBuffer();
		assertTrue(buf.isEmpty());
		assertNull(protection.unprotect(srvState, dst));
		assertEquals(1, dst.remaining());
		assertFalse(buf.isEmpty());
		ctx1 = srvState.getContext(EncryptionLevel.INITIAL);
		ctx2 = srvState.getContext(EncryptionLevel.HANDSHAKE);
		ctx2.setDecryptor(ctx1.getDecryptor());
		byte[] buffered = buf.get();
		dst.clear();
		dst.put(buffered).flip();
		p = protection.unprotect(srvState, dst);
		assertNotNull(p);
		assertTrue(buf.isEmpty());
		
		//with erased keys
		p = new InitialPacket(DEST_CID, 1, bytes(), Version.V1, bytes());
		p.getFrames().add(new MultiPaddingFrame(10));
		dst.clear();
		protection.protect(cliState, p, dst);
		dst.put((byte) 0);
		dst.flip();
		ctx1.erase();
		buf = srvState.getContext(EncryptionLevel.INITIAL).getBuffer();
		assertNull(protection.unprotect(srvState, dst));
		assertEquals(1, dst.remaining());
		assertTrue(buf.isEmpty());
	}
	
	@Test
	public void testUprotectWithoutInitialKeys() throws Exception {
		cliListener.onInit(INITIAL_SALT_V1, DEST_CID);
		IPacket p = new InitialPacket(DEST_CID, 0, bytes(), Version.V1, bytes());
		p.getFrames().add(new MultiPaddingFrame(10));
		dst.put((byte) 0);
		protection.protect(cliState, p, dst);
		dst.put((byte) 0);
		dst.flip();
		dst.get();
		assertEquals("", listener.status.toString());
		p = protection.unprotect(srvState, dst);
		assertEquals(1, dst.remaining());
		assertNotNull(p);
		assertEquals("INI-V1", listener.status.toString());
		
		before();
		srvListener.onInit(INITIAL_SALT_V1, DEST_CID);
		p = new InitialPacket(bytes(), 0, bytes("0011"), Version.V1, bytes());
		p.getFrames().add(new MultiPaddingFrame(10));
		dst.put((byte) 0);
		protection.protect(srvState, p, dst);
		dst.put((byte) 0);
		dst.flip();
		dst.get();
		assertEquals("", listener.status.toString());
		p = protection.unprotect(cliState, dst);
		assertEquals(1, dst.remaining());
		assertNull(p);
		assertEquals("", listener.status.toString());
	}
	
	@Test
	public void testDetectType() throws Exception {
		assertSame(PacketType.ONE_RTT, protection.detectType(buffer("40")));
		assertSame(PacketType.INITIAL, protection.detectType(buffer("c000000001")));
		assertSame(PacketType.ZERO_RTT, protection.detectType(buffer("d000000001")));
		assertSame(PacketType.HANDSHAKE, protection.detectType(buffer("e000000001")));
		assertSame(PacketType.RETRY, protection.detectType(buffer("f000000001")));
		assertSame(PacketType.VERSION_NEGOTIATION, protection.detectType(buffer("f000000000")));
		try {
			protection.detectType(buffer("f0000000"));
		}
		catch (QuicException e) {
			assertSame(TransportError.PROTOCOL_VIOLATION, e.getTransportError());
		}
	}
	
	void assertReservedBits(PacketType type, int bits) {
		try {
			protection.checkReservedBits(type, bits);
			fail();
		}
		catch (QuicException e) {
			assertSame(TransportError.PROTOCOL_VIOLATION, e.getTransportError());
			assertEquals("Non-zero reserved bits", e.getMessage());
		}
	}
	
	@Test
	public void testCheckReservedBits() throws Exception {
		cliListener.onInit(INITIAL_SALT_V1, DEST_CID);
		srvListener.onInit(INITIAL_SALT_V1, DEST_CID);
	
		InitialPacket p = new InitialPacket(DEST_CID, 2, bytes(), Version.V1, bytes()) {
			@Override
			public int getHeaderBytes(long largestPn, int expansion, ByteBuffer dst) {
				int len = super.getHeaderBytes(largestPn, expansion, dst);
				dst.put(0, (byte) (dst.get(0) | 0x0c));
				return len;
			}
		};
		p.getFrames().add(new MultiPaddingFrame(100));
		protection.protect(cliState, p, dst);
		dst.flip();
		try {
			protection.unprotect(srvState, dst);
			fail();
		}
		catch (QuicException e) {
			assertSame(TransportError.PROTOCOL_VIOLATION, e.getTransportError());
			assertEquals("Non-zero reserved bits", e.getMessage());
		}
		assertReservedBits(PacketType.INITIAL, 0b00001100);
		assertReservedBits(PacketType.INITIAL, 0b00001000);
		assertReservedBits(PacketType.INITIAL, 0b00000100);
		assertReservedBits(PacketType.ZERO_RTT, 0b00001100);
		assertReservedBits(PacketType.ZERO_RTT, 0b00001000);
		assertReservedBits(PacketType.ZERO_RTT, 0b00000100);
		assertReservedBits(PacketType.HANDSHAKE, 0b00001100);
		assertReservedBits(PacketType.HANDSHAKE, 0b00001000);
		assertReservedBits(PacketType.HANDSHAKE, 0b00000100);
		assertReservedBits(PacketType.ONE_RTT, 0b00011000);
		assertReservedBits(PacketType.ONE_RTT, 0b00010000);
		assertReservedBits(PacketType.ONE_RTT, 0b00001000);
	}
	
	@Test
	public void testHandshake() throws Exception {
		cliListener.onInit(INITIAL_SALT_V1, DEST_CID);
		srvListener.onInit(INITIAL_SALT_V1, DEST_CID);
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm()).ticketInfos(0xffffffffL);
		EngineParametersBuilder epb = new EngineParametersBuilder().skipEndOfEarlyData(true)
				.delegatedTaskMode(DelegatedTaskMode.NONE).peerHost("xxx1.snf4j.org").peerPort(8888);
		CryptoEngine cliE = new CryptoEngine(new HandshakeEngine(true, epb.build(), ehb.build(), cliListener));
		CryptoEngine srvE = new CryptoEngine(new HandshakeEngine(false, epb.build(), ehb.build(), srvListener));
		CryptoEngineAdapter cliA = new CryptoEngineAdapter(cliE);
		CryptoEngineAdapter srvA = new CryptoEngineAdapter(srvE);

		srvA.getEngine().start();
		cliA.getEngine().start();
		ProducedCrypto[] produced = cliA.produce();
		assertEquals(1, produced.length);
		assertSame(EncryptionLevel.INITIAL, produced[0].getEncryptionLevel());
		IPacket p = new InitialPacket(DEST_CID, 0, bytes(), Version.V1, bytes());
		p.getFrames().add(new CryptoFrame(produced[0].getOffset(), produced[0].getData()));
		protection.protect(cliState, p, dst);

		dst.flip();
		p = protection.unprotect(srvState, dst);
		assertEquals(0, dst.remaining());
		assertSame(PacketType.INITIAL, p.getType());
		assertEquals(1, p.getFrames().size());
		CryptoFrame f = (CryptoFrame) p.getFrames().get(0);
		srvA.consume(f.getData(), f.getDataOffset(), f.getDataLength(), p.getType().encryptionLevel());
		produced = srvA.produce();
		assertEquals(2, produced.length);
		assertSame(EncryptionLevel.INITIAL, produced[0].getEncryptionLevel());
		assertSame(EncryptionLevel.HANDSHAKE, produced[1].getEncryptionLevel());
		dst.clear();
		p = new InitialPacket(DEST_CID, 0, bytes(), Version.V1, bytes());
		p.getFrames().add(new CryptoFrame(produced[0].getOffset(), produced[0].getData()));
		protection.protect(srvState, p, dst);
		p = new HandshakePacket(DEST_CID, 0, bytes(), Version.V1);
		p.getFrames().add(new CryptoFrame(produced[1].getOffset(), produced[1].getData()));
		protection.protect(srvState, p, dst);

		dst.flip();
		p = protection.unprotect(cliState, dst);
		assertSame(PacketType.INITIAL, p.getType());
		assertEquals(1, p.getFrames().size());
		f = (CryptoFrame) p.getFrames().get(0);
		cliA.consume(f.getData(), f.getDataOffset(), f.getDataLength(), p.getType().encryptionLevel());
		produced = cliA.produce();
		assertEquals(0, produced.length);
		p = protection.unprotect(cliState, dst);
		assertEquals(0, dst.remaining());
		assertSame(PacketType.HANDSHAKE, p.getType());
		assertEquals(1, p.getFrames().size());
		f = (CryptoFrame) p.getFrames().get(0);
		cliA.consume(f.getData(), f.getDataOffset(), f.getDataLength(), p.getType().encryptionLevel());
		produced = cliA.produce();
		assertEquals(1, produced.length);
		assertSame(EncryptionLevel.HANDSHAKE, produced[0].getEncryptionLevel());
		dst.clear();
		p = new HandshakePacket(DEST_CID, 0, bytes(), Version.V1);
		p.getFrames().add(new CryptoFrame(produced[0].getOffset(), produced[0].getData()));
		protection.protect(cliState, p, dst);

		dst.flip();
		p = protection.unprotect(srvState, dst);
		assertEquals(0, dst.remaining());
		assertSame(PacketType.HANDSHAKE, p.getType());
		assertEquals(1, p.getFrames().size());
		f = (CryptoFrame) p.getFrames().get(0);
		srvA.consume(f.getData(), f.getDataOffset(), f.getDataLength(), p.getType().encryptionLevel());
		produced = srvA.produce();
		assertEquals(1, produced.length);
		assertSame(EncryptionLevel.APPLICATION_DATA, produced[0].getEncryptionLevel());
		dst.clear();
		p = new OneRttPacket(bytes("001122334455"), 0, false, false);
		p.getFrames().add(new CryptoFrame(produced[0].getOffset(), produced[0].getData()));
		protection.protect(srvState, p, dst);

		dst.flip();
		cliState.setSourceId(bytes("001122334455"));
		srvState.setDestinationId(DEST_CID);
		p = protection.unprotect(cliState, dst);
		assertEquals(0, dst.remaining());
		assertSame(PacketType.ONE_RTT, p.getType());
		assertEquals(1, p.getFrames().size());
		f = (CryptoFrame) p.getFrames().get(0);
		cliA.consume(f.getData(), f.getDataOffset(), f.getDataLength(), p.getType().encryptionLevel());
		produced = cliA.produce();
		assertEquals(0, produced.length);
		assertEquals(1, cliE.getSession().getManager().getTickets(cliE.getSession()).length);

		// With early data
		TestEDHandler edh = new TestEDHandler(bytes("ac"));
		cliState = new QuicState(true);
		srvState = new QuicState(false);
		cliListener = new CryptoEngineStateListener(cliState);
		srvListener = new CryptoEngineStateListener(srvState);
		cliE = new CryptoEngine(new HandshakeEngine(true, epb.build(), ehb.build(edh), cliListener));
		srvE = new CryptoEngine(new HandshakeEngine(false, epb.build(), ehb.build(), srvListener));
		cliA = new CryptoEngineAdapter(cliE);
		srvA = new CryptoEngineAdapter(srvE);
		cliListener.onInit(INITIAL_SALT_V1, DEST_CID);
		srvListener.onInit(INITIAL_SALT_V1, DEST_CID);

		srvA.getEngine().start();
		cliA.getEngine().start();
		produced = cliA.produce();
		assertEquals(2, produced.length);
		assertSame(EncryptionLevel.INITIAL, produced[0].getEncryptionLevel());
		assertSame(EncryptionLevel.EARLY_DATA, produced[1].getEncryptionLevel());
		dst.clear();
		p = new InitialPacket(DEST_CID, 0, bytes(), Version.V1, bytes());
		p.getFrames().add(new CryptoFrame(produced[0].getOffset(), produced[0].getData()));
		protection.protect(cliState, p, dst);
		p = new ZeroRttPacket(DEST_CID, 0, bytes(), Version.V1);
		p.getFrames().add(new PingFrame());
		p.getFrames().add(new MultiPaddingFrame(100));
		protection.protect(cliState, p, dst);

		dst.flip();
		p = protection.unprotect(srvState, dst);
		assertSame(PacketType.INITIAL, p.getType());
		assertEquals(1, p.getFrames().size());
		f = (CryptoFrame) p.getFrames().get(0);
		srvA.consume(f.getData(), f.getDataOffset(), f.getDataLength(), p.getType().encryptionLevel());
		p = protection.unprotect(srvState, dst);
		assertEquals(0, dst.remaining());
		assertSame(PacketType.ZERO_RTT, p.getType());
		assertEquals(2, p.getFrames().size());
		produced = srvA.produce();
		assertEquals(2, produced.length);
		assertSame(EncryptionLevel.INITIAL, produced[0].getEncryptionLevel());
		assertSame(EncryptionLevel.HANDSHAKE, produced[1].getEncryptionLevel());
		dst.clear();
		p = new InitialPacket(DEST_CID, 0, bytes(), Version.V1, bytes());
		p.getFrames().add(new CryptoFrame(produced[0].getOffset(), produced[0].getData()));
		protection.protect(srvState, p, dst);
		p = new HandshakePacket(DEST_CID, 0, bytes(), Version.V1);
		p.getFrames().add(new CryptoFrame(produced[1].getOffset(), produced[1].getData()));
		protection.protect(srvState, p, dst);

		dst.flip();
		p = protection.unprotect(cliState, dst);
		assertSame(PacketType.INITIAL, p.getType());
		assertEquals(1, p.getFrames().size());
		f = (CryptoFrame) p.getFrames().get(0);
		cliA.consume(f.getData(), f.getDataOffset(), f.getDataLength(), p.getType().encryptionLevel());
		produced = cliA.produce();
		assertEquals(0, produced.length);
		p = protection.unprotect(cliState, dst);
		assertEquals(0, dst.remaining());
		assertSame(PacketType.HANDSHAKE, p.getType());
		assertEquals(1, p.getFrames().size());
		f = (CryptoFrame) p.getFrames().get(0);
		cliA.consume(f.getData(), f.getDataOffset(), f.getDataLength(), p.getType().encryptionLevel());
		produced = cliA.produce();
		assertEquals(1, produced.length);
		assertEquals("ACC", edh.status.toString());
	}

	@Test
	public void testKeyRotation() throws Exception {
		cliListener.onInit(INITIAL_SALT_V1, DEST_CID);
		srvListener.onInit(INITIAL_SALT_V1, DEST_CID);
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder().skipEndOfEarlyData(true)
				.delegatedTaskMode(DelegatedTaskMode.NONE);
		CryptoEngine cliE = new CryptoEngine(new HandshakeEngine(true, epb.build(), ehb.build(), cliListener));
		CryptoEngine srvE = new CryptoEngine(new HandshakeEngine(false, epb.build(), ehb.build(), srvListener));
		CryptoEngineAdapter cliA = new CryptoEngineAdapter(cliE);
		CryptoEngineAdapter srvA = new CryptoEngineAdapter(srvE);

		srvA.getEngine().start();
		cliA.getEngine().start();
		ProducedCrypto[] produced = cliA.produce();
		assertEquals(1, produced.length);
		
		ByteBuffer data = produced[0].getData();
		srvA.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = srvA.produce();
		assertEquals(2, produced.length);

		data = produced[0].getData();
		cliA.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		data = produced[1].getData();
		cliA.consume(data, produced[1].getOffset(), data.remaining(), produced[1].getEncryptionLevel());
		produced = cliA.produce();
		assertEquals(1, produced.length);

		data = produced[0].getData();
		srvA.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = srvA.produce();
		assertEquals(1, produced.length);

		data = produced[0].getData();
		cliA.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = cliA.produce();
		assertEquals(0, produced.length);
		
		cliState.setDestinationId(DEST_CID);
		srvState.setSourceId(DEST_CID);
		IPacket p = new OneRttPacket(DEST_CID, 0, false, false);
		p.getFrames().add(new MultiPaddingFrame(1200));
		ByteBuffer buf0 = ByteBuffer.allocate(2000);
		protection.protect(cliState, p, buf0);
		buf0.flip();
		p = new OneRttPacket(DEST_CID, 1, false, false);
		p.getFrames().add(new MultiPaddingFrame(1200));
		ByteBuffer buf1 = ByteBuffer.allocate(2000);
		protection.protect(cliState, p, buf1);
		buf1.flip();
		
		p = protection.unprotect(srvState, buf0);
		assertNotNull(p);
		
		cliState.getContext(EncryptionLevel.APPLICATION_DATA).rotateKeys();
		cliListener.onNextKeys();
		
		p = new OneRttPacket(DEST_CID, 2, false, true);
		p.getFrames().add(new MultiPaddingFrame(1200));
		ByteBuffer buf2 = ByteBuffer.allocate(2000);
		protection.protect(cliState, p, buf2);
		buf2.flip();
		
		assertEquals("", listener.status.toString());
		p = protection.unprotect(srvState, buf2);
		assertNotNull(p);
		assertEquals("ROT", listener.status.toString());
		
		p = protection.unprotect(srvState, buf1);
		assertNotNull(p);
		assertEquals("ROT", listener.status.toString());
		
		cliState.getContext(EncryptionLevel.APPLICATION_DATA).rotateKeys();
		cliListener.onNextKeys();
		
		p = new OneRttPacket(DEST_CID, 3, false, false);
		p.getFrames().add(new MultiPaddingFrame(1200));
		ByteBuffer buf3 = ByteBuffer.allocate(2000);
		protection.protect(cliState, p, buf3);
		buf3.flip();
		
		p = protection.unprotect(srvState, buf3);
		assertNotNull(p);
		assertEquals("ROTROT", listener.status.toString());
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

	class TestListener implements PacketProtectionListener {
		
		StringBuilder status = new StringBuilder();

		@Override
		public void onKeysRotation(QuicState state) {
			status.append("ROT");
			try {
				if (state.isClientMode()) {
					cliListener.onNextKeys();
				}
				else {
					srvListener.onNextKeys();
				}
			}
			catch (Exception e) {
			}
		}

		@Override
		public void onInitialKeys(QuicState state, byte[] destinationId, Version version) {
			status.append("INI-" + version);
			try {
				if (state.isClientMode()) {
					cliListener.onInit(INITIAL_SALT_V1, destinationId);
				}
				else {
					srvListener.onInit(INITIAL_SALT_V1, destinationId);
				}
			}
			catch (Exception e) {
			}
		}	
	}
}
