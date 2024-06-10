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

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.Version;
import org.snf4j.quic.engine.crypto.ProducedCrypto;
import org.snf4j.quic.engine.processor.QuicProcessor;
import org.snf4j.quic.frame.AckFrame;
import org.snf4j.quic.frame.AckRange;
import org.snf4j.quic.frame.CryptoFrame;
import org.snf4j.quic.frame.IFrame;
import org.snf4j.quic.frame.MultiPaddingFrame;
import org.snf4j.quic.frame.PingFrame;
import org.snf4j.quic.packet.IPacket;
import org.snf4j.quic.packet.PacketType;

public class CryptoFragmenterTest extends CommonTest {

	static final byte[] INITIAL_SALT_V1 = bytes("38762cf7f55934b34d179ae6a4c80cadccbb7f0a");

	static final byte[] DEST_CID = bytes("8394c8f03e515708");

	QuicState state, peerState;
	
	CryptoEngineStateListener listener, peerListener;
	
	QuicProcessor processor;
	
	PacketProtection protection, peer;
	
	CryptoFragmenter fragmenter;
	
	ByteBuffer buf;
	
	@Override
	public void before() throws Exception {
		super.before();
		state = new QuicState(true);
		peerState = new QuicState(false);
		state.getConnectionIdManager().getSourceId();
		state.getConnectionIdManager().getDestinationPool().add(bytes("00010203"));
		listener = new CryptoEngineStateListener(state);
		peerListener = new CryptoEngineStateListener(peerState);
		processor = new QuicProcessor(state, null);
		protection = new PacketProtection(new TestListener(listener));
		peer = new PacketProtection(new TestListener(peerListener));
		fragmenter = new CryptoFragmenter(state, protection, new TestListener(listener), processor);
		buf = ByteBuffer.allocate(2000);
	}
	
	void assertCrypto(byte[] expected, int offset, IFrame frame) {
		CryptoFrame f = (CryptoFrame) frame;
		
		assertArrayEquals(expected, bytes(f.getData()));
		assertEquals(offset, f.getDataOffset());
	}
	
	void assertAck(IFrame frame, long... ranges) {
		AckFrame f = (AckFrame) frame;
		
		assertEquals(ranges.length, f.getRanges().length*2);
		int i = 0;
		for (AckRange range: f.getRanges()) {
			assertEquals(ranges[i++], range.getFrom());
			assertEquals(ranges[i++], range.getTo());
		}
	}
	
	ProducedCrypto[] produced(ProducedCrypto... cryptos) {
		return cryptos.clone();
	}
	
	@Test
	public void testInitial() throws Exception {
		ProducedCrypto pc1,pc2;
		
		pc1 = new ProducedCrypto(buffer(bytes("010203040506")), EncryptionLevel.INITIAL, 0);
		fragmenter.protect(produced(pc1), buf);
		buf.flip();
		assertEquals(1200, buf.remaining());
		IPacket packet = peer.unprotect(peerState, buf);
		assertEquals(0, packet.getPacketNumber());
		assertNotNull(packet);
		assertEquals(0, buf.remaining());
		assertEquals(2, packet.getFrames().size());
		assertCrypto(bytes("010203040506"), 0, packet.getFrames().get(0));
		
		buf.clear();
		pc1 = new ProducedCrypto(buffer(bytes("010203040506")), EncryptionLevel.INITIAL, 6);
		pc2 = new ProducedCrypto(buffer(bytes("0708090a")), EncryptionLevel.INITIAL, 12);
		fragmenter.protect(produced(pc1,pc2), buf);
		buf.flip();
		assertEquals(1200, buf.remaining());
		packet = peer.unprotect(peerState, buf);
		assertEquals(1, packet.getPacketNumber());
		assertNotNull(packet);
		assertEquals(0, buf.remaining());
		assertEquals(3, packet.getFrames().size());
		assertCrypto(bytes("010203040506"), 6, packet.getFrames().get(0));
		assertCrypto(bytes("0708090a"), 12, packet.getFrames().get(1));
		
		buf.clear();
		PacketNumberSpace space = state.getSpace(EncryptionLevel.INITIAL);
		space.acks().add(0);
		space.acks().add(1);
		assertNotNull(space.acks().build(3));
		fragmenter.protect(produced(pc1), buf);
		assertNull(space.acks().build(3));
		buf.flip();
		assertEquals(1200, buf.remaining());
		packet = peer.unprotect(peerState, buf);
		assertNotNull(packet);
		assertEquals(2, packet.getPacketNumber());
		assertEquals(3, packet.getFrames().size());
		assertAck(packet.getFrames().get(0), 1, 0);
		assertCrypto(bytes("010203040506"), 6, packet.getFrames().get(1));
		
		buf.clear();
		space.frames().add(new PingFrame());
		space.frames().add(new PingFrame());
		assertNotNull(space.frames().peek());
		fragmenter.protect(produced(pc1), buf);
		assertNull(space.frames().peek());
		buf.flip();
		assertEquals(1200, buf.remaining());
		packet = peer.unprotect(peerState, buf);
		assertNotNull(packet);
		assertEquals(3, packet.getPacketNumber());
		assertEquals(5, packet.getFrames().size());
		MultiPaddingFrame padding = (MultiPaddingFrame)packet.getFrames().get(0);
		assertEquals(2, padding.getLength());
		assertTrue(packet.getFrames().get(1) instanceof PingFrame);
		assertTrue(packet.getFrames().get(2) instanceof PingFrame);
		assertCrypto(bytes("010203040506"), 6, packet.getFrames().get(3));
		
		buf.clear();
		space.frames().add(new PingFrame());
		space.acks().add(0);
		fragmenter.protect(produced(pc1), buf);
		assertNull(space.frames().peek());
		assertNull(space.acks().build(3));
		buf.flip();
		assertEquals(1200, buf.remaining());
		packet = peer.unprotect(peerState, buf);
		assertEquals(4, packet.getPacketNumber());
		assertEquals(4, packet.getFrames().size());
		assertAck(packet.getFrames().get(0), 0, 0);
		assertTrue(packet.getFrames().get(1) instanceof PingFrame);
		assertCrypto(bytes("010203040506"), 6, packet.getFrames().get(2));
		
		buf.clear();
		space.frames().add(new PingFrame());
		space.acks().add(0);
		fragmenter.protect(produced(pc1,pc2), buf);
		assertNull(space.frames().peek());
		assertNull(space.acks().build(3));
		buf.flip();
		assertEquals(1200, buf.remaining());
		packet = peer.unprotect(peerState, buf);
		assertEquals(5, packet.getPacketNumber());
		assertEquals(5, packet.getFrames().size());
		assertAck(packet.getFrames().get(0), 0, 0);
		assertTrue(packet.getFrames().get(1) instanceof PingFrame);
		assertCrypto(bytes("010203040506"), 6, packet.getFrames().get(2));
		assertCrypto(bytes("0708090a"), 12, packet.getFrames().get(3));

	}
	
	@Test
	public void testHasPending() throws Exception {
		ProducedCrypto pc1,pc2;
		
		assertFalse(fragmenter.hasPending());
		pc1 = new ProducedCrypto(buffer(bytes(1200-42)), EncryptionLevel.INITIAL, 0);
		fragmenter.protect(produced(pc1), buf);
		buf.flip();
		assertEquals(1200, buf.remaining());
		assertTrue(fragmenter.hasPending());
		IPacket packet1 = peer.unprotect(peerState, buf);
		buf.clear();
		fragmenter.protect(produced(), buf);
		buf.flip();
		assertEquals(1200, buf.remaining());
		assertFalse(fragmenter.hasPending());
		IPacket packet2 = peer.unprotect(peerState, buf);
		assertEquals(1200-42, 
				((CryptoFrame)packet1.getFrames().get(0)).getDataLength()+
				((CryptoFrame)packet2.getFrames().get(0)).getDataLength());
		
		pc1 = new ProducedCrypto(buffer(bytes(1200-43)), EncryptionLevel.INITIAL, 0);
		pc2 = new ProducedCrypto(buffer(bytes(100)), EncryptionLevel.INITIAL, 0);
		fragmenter.protect(produced(pc1,pc2), buf);
		buf.flip();
		assertEquals(1200, buf.remaining());
		assertTrue(fragmenter.hasPending());
		buf.clear();
		fragmenter.protect(produced(), buf);
		buf.flip();
		assertEquals(1200, buf.remaining());
		assertFalse(fragmenter.hasPending());
		packet2 = peer.unprotect(peerState, buf);
		assertEquals(100, 
				((CryptoFrame)packet2.getFrames().get(0)).getDataLength());
	}
	
	@Test
	public void testNoCrypto() throws Exception {
		fragmenter.protect(produced(), buf);
		buf.flip();
		assertEquals(0, buf.remaining());
		
		buf.clear();
		PacketNumberSpace space = state.getSpace(EncryptionLevel.INITIAL);
		space.acks().add(0);
		space.acks().add(1);
		fragmenter.protect(produced(), buf);
		buf.flip();
		assertEquals(1200, buf.remaining());
		IPacket packet = peer.unprotect(peerState, buf);
		assertEquals(2, packet.getFrames().size());
		assertAck(packet.getFrames().get(0), 1, 0);
		
		buf.clear();
		space.frames().add(new PingFrame());
		fragmenter.protect(produced(), buf);
		buf.flip();
		assertEquals(1200, buf.remaining());
		packet = peer.unprotect(peerState, buf);
		assertEquals(3, packet.getFrames().size());
		MultiPaddingFrame padding = (MultiPaddingFrame)packet.getFrames().get(0);
		assertEquals(2, padding.getLength());
		assertTrue(packet.getFrames().get(1) instanceof PingFrame);
		
		buf.clear();
		space.frames().add(new PingFrame());
		space.acks().add(2);
		fragmenter.protect(produced(), buf);
		buf.flip();
		assertEquals(1200, buf.remaining());
		packet = peer.unprotect(peerState, buf);
		assertEquals(3, packet.getFrames().size());
		assertAck(packet.getFrames().get(0), 2, 2);
		assertTrue(packet.getFrames().get(1) instanceof PingFrame);
		
		buf.clear();
		space.acks().add(3);
		space.acks().add(5);
		space.acks().add(7);
		space.acks().add(8);
		space.acks().add(10);
		fragmenter.protect(produced(), buf);
		buf.flip();
		assertEquals(1200, buf.remaining());
		packet = peer.unprotect(peerState, buf);
		assertEquals(2, packet.getFrames().size());
		assertAck(packet.getFrames().get(0), 10,10, 8,7, 5,5);
		buf.clear();
		fragmenter.protect(produced(), buf);
		buf.flip();
		assertEquals(1200, buf.remaining());
		packet = peer.unprotect(peerState, buf);
		assertEquals(2, packet.getFrames().size());
		assertAck(packet.getFrames().get(0), 3,3);
		assertNull(space.acks().build(3));
	}
	
	void init(EncryptionLevel level) throws Exception {
		QuicState state = new QuicState(true);
		QuicState peerState = new QuicState(false);
		CryptoEngineStateListener listener = new CryptoEngineStateListener(state);
		CryptoEngineStateListener peerListener = new CryptoEngineStateListener(peerState);
		listener.onInit(INITIAL_SALT_V1, DEST_CID);
		peerListener.onInit(INITIAL_SALT_V1, DEST_CID);
		
		EncryptionContext ctx = state.getContext(EncryptionLevel.INITIAL);
		Encryptor e = ctx.getEncryptor();
		this.state.getContext(level).setEncryptor(e);
		ctx = peerState.getContext(EncryptionLevel.INITIAL);
		Decryptor d = ctx.getDecryptor();
		this.peerState.getContext(level).setDecryptor(d);
	}
	
	@Test
	public void testHandshake() throws Exception {
		ProducedCrypto pc1,pc2;

		listener.onInit(INITIAL_SALT_V1, DEST_CID);
		peerListener.onInit(INITIAL_SALT_V1, DEST_CID);
		init(EncryptionLevel.HANDSHAKE);
		
		pc1 = new ProducedCrypto(buffer(bytes("010203040506")), EncryptionLevel.INITIAL, 6);
		pc2 = new ProducedCrypto(buffer(bytes("0708090a")), EncryptionLevel.HANDSHAKE, 12);
		fragmenter.protect(produced(pc1,pc2), buf);
		buf.flip();
		assertEquals(1200, buf.remaining());
		IPacket packet1 = peer.unprotect(peerState, buf);
		assertSame(PacketType.INITIAL, packet1.getType());
		assertEquals(1, packet1.getFrames().size());
		IPacket packet2 = peer.unprotect(peerState, buf);
		assertSame(PacketType.HANDSHAKE, packet2.getType());
		assertEquals(2, packet2.getFrames().size());
		assertEquals(0, buf.remaining());
		assertCrypto(bytes("010203040506"), 6, packet1.getFrames().get(0));
		assertCrypto(bytes("0708090a"), 12, packet2.getFrames().get(0));
		
		//no remaining
		buf.clear();
		PacketNumberSpace space = state.getSpace(EncryptionLevel.HANDSHAKE);
		pc1 = new ProducedCrypto(buffer(bytes(1200-43-43)), EncryptionLevel.INITIAL, 6);
		space.frames().add(new PingFrame());
		fragmenter.protect(produced(pc1), buf);
		buf.flip();
		assertEquals(1200, buf.remaining());
		assertNotNull(space.frames().peek());
		buf.clear();
		fragmenter.protect(produced(), buf);
		buf.flip();
		assertEquals(40, buf.remaining());
		packet2 = peer.unprotect(peerState, buf);
		assertNull(space.frames().peek());
		assertEquals(2, packet2.getFrames().size());
		assertEquals(2, packet2.getFrames().get(0).getLength());
		assertTrue(packet2.getFrames().get(1) instanceof PingFrame);

		//no remaining for ack
		buf.clear();
		pc1 = new ProducedCrypto(buffer(bytes(1200-43-44)), EncryptionLevel.INITIAL, 6);
		space.acks().add(0);
		fragmenter.protect(produced(pc1), buf);
		assertEquals(1200, buf.position());
		assertNotNull(space.acks().build(3));
		int len = space.acks().build(3).getLength();
		pc1 = new ProducedCrypto(buffer(bytes(1200-43-43-len+1)), EncryptionLevel.INITIAL, 6);
		buf.clear();
		fragmenter.protect(produced(pc1), buf);
		assertEquals(1200, buf.position());
		assertNotNull(space.acks().build(3));
		pc1 = new ProducedCrypto(buffer(bytes(1200-43-43-len)), EncryptionLevel.INITIAL, 6);
		buf.clear();
		fragmenter.protect(produced(pc1), buf);
		assertEquals(1200, buf.position());
		assertNull(space.acks().build(3));
		
		//no remaining for frame
		buf.clear();
		pc1 = new ProducedCrypto(buffer(bytes(1200-43-44)), EncryptionLevel.INITIAL, 6);
		space.frames().add(new PingFrame());
		fragmenter.protect(produced(pc1), buf);
		assertEquals(1200, buf.position());
		assertNotNull(space.frames().peek());
		buf.clear();
		pc1 = new ProducedCrypto(buffer(bytes(1200-43-45)), EncryptionLevel.INITIAL, 6);
		fragmenter.protect(produced(pc1), buf);
		assertEquals(1200, buf.position());
		assertNotNull(space.frames().peek());
		buf.clear();
		pc1 = new ProducedCrypto(buffer(bytes(1200-43-46)), EncryptionLevel.INITIAL, 6);
		fragmenter.protect(produced(pc1), buf);
		assertEquals(1200, buf.position());
		assertNull(space.frames().peek());
		buf.clear();
		pc1 = new ProducedCrypto(buffer(bytes(1200-43-45)), EncryptionLevel.INITIAL, 6);
		space.frames().add(new MultiPaddingFrame(3));
		fragmenter.protect(produced(pc1), buf);
		assertEquals(1200, buf.position());
		assertNotNull(space.frames().peek());
		buf.clear();
		pc1 = new ProducedCrypto(buffer(bytes(1200-43-46)), EncryptionLevel.INITIAL, 6);
		fragmenter.protect(produced(pc1), buf);
		assertEquals(1200, buf.position());
		assertNull(space.frames().peek());
		
		//no remaining for another frame
		buf.clear();
		space.frames().add(new MultiPaddingFrame(3));
		space.frames().add(new PingFrame());
		pc1 = new ProducedCrypto(buffer(bytes(1200-43-46)), EncryptionLevel.INITIAL, 6);
		fragmenter.protect(produced(pc1), buf);
		assertEquals(1200, buf.position());
		assertTrue(space.frames().peek() instanceof PingFrame);
		buf.clear();
		space.frames().add(new MultiPaddingFrame(2));
		space.frames().add(new PingFrame());
		pc1 = new ProducedCrypto(buffer(bytes(1200-43-47)), EncryptionLevel.INITIAL, 6);
		fragmenter.protect(produced(pc1), buf);
		assertEquals(1200, buf.position());
		assertNull(space.frames().peek());
		
		//no remaining for crypto frame
		buf.clear();
		pc1 = new ProducedCrypto(buffer(bytes(1200-43-43-19)), EncryptionLevel.INITIAL, 6);
		pc2 = new ProducedCrypto(buffer(bytes(100)), EncryptionLevel.HANDSHAKE, 6);
		fragmenter.protect(produced(pc1,pc2), buf);
		assertEquals(1200, buf.position());
		assertTrue(fragmenter.hasPending());
		buf.clear();
		fragmenter.protect(produced(), buf);
		assertFalse(fragmenter.hasPending());
		buf.clear();
		pc1 = new ProducedCrypto(buffer(bytes(1200-43-43-20)), EncryptionLevel.INITIAL, 6);
		pc2 = new ProducedCrypto(buffer(bytes(100)), EncryptionLevel.HANDSHAKE, 0);
		fragmenter.protect(produced(pc1,pc2), buf);
		assertEquals(1200, buf.position());
		assertTrue(fragmenter.hasPending());
		buf.flip();
		peer.unprotect(peerState, buf);
		assertTrue(buf.hasRemaining());
		packet1 = peer.unprotect(peerState, buf);
		assertEquals(0, buf.remaining());
		buf.clear();
		fragmenter.protect(produced(), buf);
		buf.flip();
		packet2 = peer.unprotect(peerState, buf);
		assertEquals(0, buf.remaining());
		byte[] data = cat(bytes(((CryptoFrame)packet1.getFrames().get(0)).getData()),
		bytes(((CryptoFrame)packet2.getFrames().get(0)).getData()));
		assertArrayEquals(bytes(100), data);
	}
	
	class TestListener implements IPacketProtectionListener {

		final CryptoEngineStateListener listener;
		
		public TestListener(CryptoEngineStateListener listener) {
			this.listener = listener;
		}

		@Override
		public void onInitialKeys(QuicState state, byte[] destinationId, Version version) throws QuicException {
			try {
				listener.onInit(InitialSalt.of(version), destinationId);
			} catch (GeneralSecurityException e) {
				throw new QuicException(TransportError.INTERNAL_ERROR, "", e);
			}
		}

		@Override
		public void onKeysRotation(QuicState state) throws QuicException {
		}
		
	}
}
