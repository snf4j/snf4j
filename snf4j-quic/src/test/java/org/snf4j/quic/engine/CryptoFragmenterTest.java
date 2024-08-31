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

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.List;

import org.junit.After;
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
import org.snf4j.quic.frame.PaddingFrame;
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
	
	long receiveTime;
	
	long nanoTime;

    IAntiAmplificator antiAmplificator;
    
	ITimeProvider time = new ITimeProvider() {

		@Override
		public long nanoTime() {
			return nanoTime;
		}
	};
	
	TestConfig config = new TestConfig();
	
	@Override
	public void before() throws Exception {
		super.before();
		receiveTime = 1000000000;
		nanoTime = 1000000000 + (1000000 << 3); // for ack delay = 1000
		antiAmplificator = null;
		state = new QuicState(true, config, time) {
			public IAntiAmplificator getAntiAmplificator() {
				if (antiAmplificator != null) {
					return antiAmplificator;					
				}
				return super.getAntiAmplificator();
			}
			
			public void eraseKeys(EncryptionLevel level, long currentTime) {
			}
		};
		TestConfig peerConfig = new TestConfig();
		peerConfig.connectionIdLength = 4;
		peerState = new QuicState(false, peerConfig, time);
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
	
	@After
	public void after() {
		config = new TestConfig();
		nanoTime = 0;
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
		space.acks().add(0, 1000);
		space.acks().add(1, 1000);
		assertNotNull(space.acks().build(3, 1000, 3));
		fragmenter.protect(produced(pc1), buf);
		assertNull(space.acks().build(3, 1000, 3));
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
		space.acks().add(0, 1000);
		fragmenter.protect(produced(pc1), buf);
		assertNull(space.frames().peek());
		assertNull(space.acks().build(3, 1000, 3));
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
		space.acks().add(0, 1000);
		fragmenter.protect(produced(pc1,pc2), buf);
		assertNull(space.frames().peek());
		assertNull(space.acks().build(3, 1000, 3));
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
	public void testLockingServerCongestionController() throws Exception {
		ProducedCrypto pc;
		
		listener.onInit(INITIAL_SALT_V1, DEST_CID);
		peerListener.onInit(INITIAL_SALT_V1, DEST_CID);
		fragmenter = new CryptoFragmenter(peerState, peer, new TestListener(peerListener), new QuicProcessor(peerState, null));
		peerState.getConnectionIdManager().getSourceId();
		peerState.getConnectionIdManager().getDestinationPool().add(bytes("00010203"));

		pc = new ProducedCrypto(buffer(bytes(100)), EncryptionLevel.INITIAL, 0);
		CongestionController cc = peerState.getCongestion();
		peerState.getAntiAmplificator().incReceived(10000);
		cc.onPacketSent(12000-1199);
		assertEquals(1199, cc.available());
		assertEquals(0, fragmenter.protect(produced(pc), buf));
		assertEquals(1199, cc.available());
		assertTrue(cc.needUnlock());
		assertTrue(cc.isBlocked());
		cc.onPacketSent(-1);
		assertEquals(1200, cc.available());
		assertEquals(1200, fragmenter.protect(produced(pc), buf));
		assertEquals(0, cc.available());
		assertFalse(cc.needUnlock());
		assertTrue(cc.isBlocked());
		
		initPeer(EncryptionLevel.HANDSHAKE);
		pc = new ProducedCrypto(buffer(bytes(200)), EncryptionLevel.HANDSHAKE, 0);
		cc.onPacketSent(-99);
		assertEquals(99, cc.available());
		assertEquals(0, fragmenter.protect(produced(pc), buf));
		assertEquals(99, cc.available());
		assertTrue(cc.needUnlock());
		assertTrue(cc.isBlocked());
		cc.onPacketSent(-1);
		assertEquals(100, cc.available());
		assertEquals(99, fragmenter.protect(produced(), buf));
		assertTrue(fragmenter.hasPending());
		assertEquals(1, cc.available());
		cc.onPacketSent(-199);
		assertEquals(200, cc.available());
		assertEquals(176, fragmenter.protect(produced(), buf));
		assertFalse(fragmenter.hasPending());
		assertEquals(24, cc.available());
		
		PacketNumberSpace space = peerState.getSpace(EncryptionLevel.INITIAL);
		cc.onPacketSent(-1175);
		assertEquals(1199, cc.available());
		space.frames().add(PaddingFrame.INSTANCE);
		assertEquals(37, fragmenter.protect(produced(), buf));
		assertFalse(cc.needUnlock());
		space.frames().clear();
		space.acks().add(0, 100);
		assertEquals(1162, cc.available());
		assertEquals(42, fragmenter.protect(produced(), buf));
		assertEquals(1162, cc.available());
		assertFalse(cc.needUnlock());
		assertFalse(cc.isBlocked());
		space.acks().clear();

		space.frames().add(PingFrame.INSTANCE);
		assertEquals(0, fragmenter.protect(produced(), buf));
		assertTrue(cc.needUnlock());
		cc.unlock();
		assertFalse(cc.isBlocked());
		
		space = peerState.getSpace(EncryptionLevel.HANDSHAKE);
		space.frames().add(PingFrame.INSTANCE);
		cc.onPacketSent(-37);
		assertEquals(1199, cc.available());
		assertEquals(0, fragmenter.protect(produced(), buf));
		assertTrue(cc.needUnlock());
		cc.unlock();
		assertFalse(cc.isBlocked());
		
		peerState.getContext(EncryptionLevel.INITIAL).erase();
		assertEquals(36, fragmenter.protect(produced(), buf));		
	}
	
	@Test
	public void testLockingClientCongestionController() throws Exception {
		ProducedCrypto pc;
		
		pc = new ProducedCrypto(buffer(bytes(100)), EncryptionLevel.INITIAL, 0);
		CongestionController cc = state.getCongestion();
		cc.onPacketSent(12000-1199);
		assertEquals(1199, cc.available());
		assertEquals(0, fragmenter.protect(produced(pc), buf));
		assertEquals(1199, cc.available());
		assertTrue(cc.needUnlock());
		assertTrue(cc.isBlocked());
		cc.onPacketSent(-1);
		assertEquals(1200, cc.available());
		assertEquals(1200, fragmenter.protect(produced(pc), buf));
		assertEquals(0, cc.available());
		assertFalse(cc.needUnlock());
		assertTrue(cc.isBlocked());
		
		init(EncryptionLevel.HANDSHAKE);
		pc = new ProducedCrypto(buffer(bytes(200)), EncryptionLevel.HANDSHAKE, 0);
		cc.onPacketSent(-99);
		assertEquals(99, cc.available());
		assertEquals(0, fragmenter.protect(produced(pc), buf));
		assertEquals(99, cc.available());
		assertTrue(cc.needUnlock());
		assertTrue(cc.isBlocked());
		cc.onPacketSent(-1);
		assertEquals(100, cc.available());
		assertEquals(99, fragmenter.protect(produced(), buf));
		assertTrue(fragmenter.hasPending());
		assertEquals(1, cc.available());
		cc.onPacketSent(-199);
		assertEquals(200, cc.available());
		assertEquals(184, fragmenter.protect(produced(), buf));
		assertFalse(fragmenter.hasPending());
		assertEquals(16, cc.available());
		
		PacketNumberSpace space = state.getSpace(EncryptionLevel.INITIAL);
		cc.onPacketSent(-1183);
		assertEquals(1199, cc.available());
		space.frames().add(PaddingFrame.INSTANCE);
		assertEquals(0, fragmenter.protect(produced(), buf));
		assertTrue(cc.needUnlock());
		cc.unlock();
		assertFalse(cc.isBlocked());
		space.frames().clear();
		space.acks().add(0, 100);
		assertEquals(0, fragmenter.protect(produced(), buf));
		assertTrue(cc.needUnlock());
		cc.unlock();
		assertFalse(cc.isBlocked());
		space.acks().clear();

		space.frames().add(PingFrame.INSTANCE);
		assertEquals(0, fragmenter.protect(produced(), buf));
		assertTrue(cc.needUnlock());
		cc.unlock();
		assertFalse(cc.isBlocked());
		
		space = state.getSpace(EncryptionLevel.HANDSHAKE);
		space.frames().add(PingFrame.INSTANCE);
		assertEquals(0, fragmenter.protect(produced(), buf));
		assertTrue(cc.needUnlock());
		cc.unlock();
		assertFalse(cc.isBlocked());
		
		state.getContext(EncryptionLevel.INITIAL).erase();
		assertEquals(40, fragmenter.protect(produced(), buf));
		
		cc.onPacketSent(1059);
		assertEquals(100, cc.available());
		assertEquals(0, fragmenter.protect(produced(), buf));
		assertFalse(cc.isBlocked());
		config.minNonBlockingUdpPayloadSize = 101;
		assertEquals(0, fragmenter.protect(produced(), buf));
		assertTrue(cc.isBlocked());
		cc.onPacketSent(-1);
		assertFalse(cc.isBlocked());
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
		
		buf.clear();
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
	
	String pending(CryptoFragmenter f) throws Exception {
		Field f1 = CryptoFragmenter.class.getDeclaredField("pending");
		Field f2 = CryptoFragmenter.class.getDeclaredField("current");
		f1.setAccessible(true);
		f2.setAccessible(true);
		
		@SuppressWarnings("unchecked")
		List<ProducedCrypto> l = (List<ProducedCrypto>)f1.get(f);
		ProducedCrypto c = (ProducedCrypto) f2.get(f);
		StringBuilder sb = new StringBuilder();
		
		if (c == null) {
			sb.append("-|");
		}
		else {
			sb.append(c.getData().remaining()).append('|');
		}
		for (ProducedCrypto p: l) {
			sb.append(p.getData().remaining()).append('|');
		}
		return sb.toString();
	}
	
	@Test
	public void testAddPending() throws Exception {
		ProducedCrypto pc1,pc2,pc3,pc4,pc5,pc6;
		
		assertFalse(fragmenter.hasPending());
		pc1 = new ProducedCrypto(buffer(bytes(200)), EncryptionLevel.INITIAL, 0);
		fragmenter.addPending(pc1);
		assertTrue(fragmenter.hasPending());
		pc2 = new ProducedCrypto(buffer(bytes(100)), EncryptionLevel.INITIAL, 0);
		fragmenter.addPending(pc2);
		assertTrue(fragmenter.hasPending());
		fragmenter.protect(produced(pc1), buf);
		buf.flip();
		assertEquals(1200, buf.remaining());
		assertFalse(fragmenter.hasPending());
		IPacket packet1 = peer.unprotect(peerState, buf);
		assertEquals(200, ((CryptoFrame)packet1.getFrames().get(0)).getDataLength());
		assertEquals(100, ((CryptoFrame)packet1.getFrames().get(1)).getDataLength());
		
		pc3 = new ProducedCrypto(buffer(bytes(201)), EncryptionLevel.EARLY_DATA, 0);
		pc4 = new ProducedCrypto(buffer(bytes(101)), EncryptionLevel.EARLY_DATA, 0);
		pc5 = new ProducedCrypto(buffer(bytes(202)), EncryptionLevel.HANDSHAKE, 0);
		pc6 = new ProducedCrypto(buffer(bytes(102)), EncryptionLevel.HANDSHAKE, 0);
		assertEquals("-|", pending(fragmenter));
		fragmenter.addPending(pc3);
		assertEquals("201|", pending(fragmenter));
		fragmenter.addPending(pc1);
		assertEquals("200|201|", pending(fragmenter));
		fragmenter.addPending(pc2);
		assertEquals("200|100|201|", pending(fragmenter));
		fragmenter.addPending(pc5);
		assertEquals("200|100|201|202|", pending(fragmenter));
		fragmenter.addPending(pc6);
		assertEquals("200|100|201|202|102|", pending(fragmenter));
		fragmenter.addPending(pc4);
		assertEquals("200|100|201|101|202|102|", pending(fragmenter));
		
		fragmenter = new CryptoFragmenter(state, protection, new TestListener(listener), processor);
		assertEquals("-|", pending(fragmenter));
		fragmenter.addPending(pc3);
		assertEquals("201|", pending(fragmenter));
		fragmenter.addPending(pc5);
		assertEquals("201|202|", pending(fragmenter));
		fragmenter.addPending(pc1);
		assertEquals("200|201|202|", pending(fragmenter));
		fragmenter.addPending(pc2);
		assertEquals("200|100|201|202|", pending(fragmenter));
	}
	
	@Test
	public void testNoCrypto() throws Exception {
		fragmenter.protect(produced(), buf);
		buf.flip();
		assertEquals(0, buf.remaining());
		
		buf.clear();
		PacketNumberSpace space = state.getSpace(EncryptionLevel.INITIAL);
		space.acks().add(0, 1000);
		space.acks().add(1, 1000);
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
		space.acks().add(2, 1000);
		fragmenter.protect(produced(), buf);
		buf.flip();
		assertEquals(1200, buf.remaining());
		packet = peer.unprotect(peerState, buf);
		assertEquals(3, packet.getFrames().size());
		assertAck(packet.getFrames().get(0), 2, 2);
		assertTrue(packet.getFrames().get(1) instanceof PingFrame);
		
		buf.clear();
		space.acks().add(3, 1000);
		space.acks().add(5, 1000);
		space.acks().add(7, 1000);
		space.acks().add(8, 1000);
		space.acks().add(10, 1000);
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
		assertNull(space.acks().build(3, 1000, 3));
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

	void initPeer(EncryptionLevel level) throws Exception {
		QuicState state = new QuicState(true);
		QuicState peerState = new QuicState(false);
		CryptoEngineStateListener listener = new CryptoEngineStateListener(state);
		CryptoEngineStateListener peerListener = new CryptoEngineStateListener(peerState);
		listener.onInit(INITIAL_SALT_V1, DEST_CID);
		peerListener.onInit(INITIAL_SALT_V1, DEST_CID);
		
		EncryptionContext ctx = state.getContext(EncryptionLevel.INITIAL);
		Encryptor e = ctx.getEncryptor();
		this.peerState.getContext(level).setEncryptor(e);
		ctx = peerState.getContext(EncryptionLevel.INITIAL);
		Decryptor d = ctx.getDecryptor();
		this.state.getContext(level).setDecryptor(d);
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
		space.acks().add(0, 1000000000);
		fragmenter.protect(produced(pc1), buf);
		assertEquals(1200, buf.position());
		assertNotNull(space.acks().build(3, nanoTime, 3));
		int len = space.acks().build(3, nanoTime, 3).getLength();
		pc1 = new ProducedCrypto(buffer(bytes(1200-43-43-len+1)), EncryptionLevel.INITIAL, 6);
		buf.clear();
		fragmenter.protect(produced(pc1), buf);
		assertEquals(1200, buf.position());
		assertNotNull(space.acks().build(3, nanoTime, 3));
		pc1 = new ProducedCrypto(buffer(bytes(1200-43-43-len)), EncryptionLevel.INITIAL, 6);
		buf.clear();
		fragmenter.protect(produced(pc1), buf);
		assertEquals(1200, buf.position());
		assertNull(space.acks().build(3, nanoTime, 3));
		
		//no remaining for frame
		state.getCongestion().onPacketSent(-10000);
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
	
	@Test
	public void testBuildingAcks() throws Exception {
		config.maxNumberOfStoredAckRanges = 10;
		config.maxNumberOfAckRanges = 2;
		config.ackDelayExponent = 1;
		before();
		
		listener.onInit(INITIAL_SALT_V1, DEST_CID);
		peerListener.onInit(INITIAL_SALT_V1, DEST_CID);
		init(EncryptionLevel.HANDSHAKE);
		init(EncryptionLevel.APPLICATION_DATA);

		PacketNumberSpace space = state.getSpace(EncryptionLevel.INITIAL);
		space.acks().add(0, receiveTime);
		space.acks().add(2, receiveTime);
		space.acks().add(4, receiveTime);
		buf.clear();
		fragmenter.protect(produced(), buf);
		buf.flip();
		IPacket packet = peer.unprotect(peerState, buf);
		AckFrame ack = (AckFrame) packet.getFrames().get(0);
		assertAck(ack, 4,4,2,2);
		assertEquals(((nanoTime-receiveTime)/1000) >> 3, ack.getDelay());

		space = state.getSpace(EncryptionLevel.HANDSHAKE);
		space.acks().add(0, receiveTime);
		space.acks().add(3, receiveTime);
		space.acks().add(6, receiveTime);
		buf.clear();
		fragmenter.protect(produced(), buf);
		buf.flip();
		packet = peer.unprotect(peerState, buf);
		packet = peer.unprotect(peerState, buf);
		ack = (AckFrame) packet.getFrames().get(0);
		assertAck(ack, 6,6,3,3);
		assertEquals(((nanoTime-receiveTime)/1000) >> 3, ack.getDelay());

		space = state.getSpace(EncryptionLevel.APPLICATION_DATA);
		space.acks().add(0, receiveTime);
		space.acks().add(4, receiveTime);
		space.acks().add(8, receiveTime);
		buf.clear();
		fragmenter.protect(produced(), buf);
		buf.flip();
		packet = peer.unprotect(peerState, buf);
		packet = peer.unprotect(peerState, buf);
		ack = (AckFrame) packet.getFrames().get(0);
		assertAck(ack, 8,8,4,4);
		assertEquals(((nanoTime-receiveTime)/1000) >> 3, ack.getDelay());
		
		state.setHandshakeState(HandshakeState.DONE);
		buf.clear();
		fragmenter.protect(produced(), buf);
		buf.flip();
		packet = peer.unprotect(peerState, buf);
		ack = (AckFrame) packet.getFrames().get(0);
		assertAck(ack, 0,0);
		assertEquals(((nanoTime-receiveTime)/1000) >> 1, ack.getDelay());
	}
	
	@Test
	public void testCallingPreSending() throws Exception {
		listener.onInit(INITIAL_SALT_V1, DEST_CID);
		peerListener.onInit(INITIAL_SALT_V1, DEST_CID);
		PacketNumberSpace space = state.getSpace(EncryptionLevel.INITIAL);
		space.frames().add(new PingFrame());
		buf.clear();
		nanoTime = 12345678;
		fragmenter.protect(produced(), buf);
		FlyingFrames flying = space.frames().getFlying(0);
		assertNotNull(flying);
		assertEquals(12345678, flying.getSentTime());
		buf.flip();
		assertTrue(buf.hasRemaining());
		assertEquals(buf.remaining(), flying.getSentBytes());
	}
	
	@Test
	public void testSettingAckEliciting() throws Exception {
		listener.onInit(INITIAL_SALT_V1, DEST_CID);
		peerListener.onInit(INITIAL_SALT_V1, DEST_CID);
		PacketNumberSpace space = state.getSpace(EncryptionLevel.INITIAL);
		space.frames().add(new PingFrame());
		buf.clear();
		fragmenter.protect(produced(), buf);
		FlyingFrames flying = space.frames().getFlying(0);
		assertTrue(flying.isAckEliciting());

		space.frames().add(new PaddingFrame());
		buf.clear();
		fragmenter.protect(produced(), buf);
		flying = space.frames().getFlying(1);
		assertFalse(flying.isAckEliciting());
	}

	@Test
	public void testSettingInFlight() throws Exception {
		listener.onInit(INITIAL_SALT_V1, DEST_CID);
		peerListener.onInit(INITIAL_SALT_V1, DEST_CID);
		init(EncryptionLevel.APPLICATION_DATA);
		PacketNumberSpace space = state.getSpace(EncryptionLevel.APPLICATION_DATA);
		space.frames().add(new PingFrame());
		buf.clear();
		fragmenter.protect(produced(), buf);
		FlyingFrames flying = space.frames().getFlying(0);
		assertTrue(flying.isInFlight());

		space.frames().add(new AckFrame(0,1000));
		buf.clear();
		fragmenter.protect(produced(), buf);
		flying = space.frames().getFlying(1);
		assertFalse(flying.isInFlight());
	}
	
	IAntiAmplificator antiAmplificator(CryptoFragmenter cf) throws Exception {
		Field f = CryptoFragmenter.class.getDeclaredField("antiAmplificator");
		f.setAccessible(true);
		return (IAntiAmplificator) f.get(cf);
	}
	
	@Test
	public void testAntiAmplification() throws Exception {
		antiAmplificator = new AntiAmplificator(state);
		IAntiAmplificator aa = antiAmplificator;
		fragmenter = new CryptoFragmenter(state, protection, new TestListener(listener), processor);
		listener.onInit(INITIAL_SALT_V1, DEST_CID);
		peerListener.onInit(INITIAL_SALT_V1, DEST_CID);
		init(EncryptionLevel.HANDSHAKE);
		
		ProducedCrypto pc1 = new ProducedCrypto(buffer(bytes(200)), EncryptionLevel.INITIAL, 6);
		ProducedCrypto pc2 = new ProducedCrypto(buffer(bytes(108)), EncryptionLevel.HANDSHAKE, 0);
		buf.clear();
		assertTrue(aa.isBlocked());
		assertEquals(0, aa.available());
		assertEquals(0, fragmenter.protect(produced(pc1), buf));
		assertTrue(fragmenter.hasPending());
		assertTrue(aa.isBlocked());
		assertEquals(0, buf.position());
		assertEquals(0, aa.available());
		assertEquals(0, fragmenter.protect(produced(pc2), buf));
		assertTrue(aa.isBlocked());
		assertEquals(0, buf.position());
		aa.incReceived(400-1);
		assertEquals(399*3, aa.available());
		assertEquals(0, fragmenter.protect(produced(), buf));
		assertTrue(aa.isBlocked());
		aa.incReceived(1);
		assertFalse(aa.isBlocked());
		assertEquals(0, buf.position());
		assertEquals(400*3, aa.available());
		assertEquals(1200, fragmenter.protect(produced(), buf));
		assertFalse(fragmenter.hasPending());
		
		buf.flip();
		assertSame(PacketType.INITIAL, peer.unprotect(peerState, buf).getType());
		assertTrue(aa.isBlocked());
		assertFalse(aa.needUnlock());
		
		buf.clear();
		assertEquals(0, aa.available());
		assertEquals(0, fragmenter.protect(produced(), buf));
		assertTrue(aa.isBlocked());
		assertEquals(0, aa.available());
		PacketNumberSpace space = state.getSpace(EncryptionLevel.INITIAL);
		space.frames().add(PaddingFrame.INSTANCE);
		assertEquals(0, fragmenter.protect(produced(), buf));
		aa.incReceived(33);
		assertEquals(99, aa.available());
		assertEquals(0, fragmenter.protect(produced(), buf));
		aa.incReceived(1);
		assertEquals(102, aa.available());
		assertEquals(0, fragmenter.protect(produced(), buf));
		assertEquals(102, aa.available());
		assertTrue(aa.isBlocked());
		aa.incReceived(365);
		assertTrue(aa.isBlocked());
		assertEquals(1197, aa.available());
		assertEquals(0, fragmenter.protect(produced(), buf));
		aa.incReceived(1);
		assertFalse(aa.isBlocked());
		assertEquals(1200, aa.available());
		assertEquals(1200, fragmenter.protect(produced(), buf));
		assertTrue(aa.isBlocked());
		assertEquals(0, aa.available());
		
		space = state.getSpace(EncryptionLevel.HANDSHAKE);
		space.frames().add(PaddingFrame.INSTANCE);
		assertEquals(0, fragmenter.protect(produced(), buf));
		assertTrue(aa.isBlocked());
		aa.incReceived(33);
		assertEquals(99, aa.available());
		assertEquals(0, fragmenter.protect(produced(), buf));
		aa.incReceived(1);
		assertEquals(102, aa.available());
		assertEquals(40, fragmenter.protect(produced(), buf));
		assertEquals(62, aa.available());
		assertFalse(aa.isBlocked());
	}

	@Test
	public void testAntiAmplificationWithDisarmAndUnblockedData() throws Exception {
		antiAmplificator = new AntiAmplificator(state);
		IAntiAmplificator aa = antiAmplificator;
		fragmenter = new CryptoFragmenter(state, protection, new TestListener(listener), processor);
		listener.onInit(INITIAL_SALT_V1, DEST_CID);
		peerListener.onInit(INITIAL_SALT_V1, DEST_CID);
		
		ProducedCrypto pc1 = new ProducedCrypto(buffer(bytes(200)), EncryptionLevel.INITIAL, 6);
		buf.clear();
		assertEquals(0, fragmenter.protect(produced(pc1), buf));
		assertTrue(aa.isBlocked());
		assertFalse(aa.needUnlock());
		state.setAddressValidated();
		assertFalse(aa.isBlocked());
		assertFalse(aa.needUnlock());
		assertEquals(1200, fragmenter.protect(produced(), buf));
		assertFalse(aa.isArmed());
		
		buf.clear();
		assertSame(PacketType.INITIAL, peer.unprotect(peerState, buf).getType());
	}

	@Test
	public void testAntiAmplificationWithDisarm() throws Exception {
		antiAmplificator = new AntiAmplificator(state);
		IAntiAmplificator aa = antiAmplificator;
		fragmenter = new CryptoFragmenter(state, protection, new TestListener(listener), processor);
		listener.onInit(INITIAL_SALT_V1, DEST_CID);
		peerListener.onInit(INITIAL_SALT_V1, DEST_CID);
		
		ProducedCrypto pc1 = new ProducedCrypto(buffer(bytes(200)), EncryptionLevel.INITIAL, 6);
		state.setAddressValidated();
		buf.clear();
		assertEquals(0, aa.available());
		assertEquals(1200, fragmenter.protect(produced(pc1), buf));
		assertFalse(aa.isArmed());
		
		buf.clear();
		assertSame(PacketType.INITIAL, peer.unprotect(peerState, buf).getType());
	}

	@Test
	public void testCongestionController() throws Exception {
		listener.onInit(INITIAL_SALT_V1, DEST_CID);
		peerListener.onInit(INITIAL_SALT_V1, DEST_CID);
		CongestionController cc = state.getCongestion();
		
		ProducedCrypto pc1 = new ProducedCrypto(buffer(bytes(200)), EncryptionLevel.INITIAL, 6);
		ProducedCrypto pc2 = new ProducedCrypto(buffer(bytes(400)), EncryptionLevel.INITIAL, 206);
		cc.onPacketSent(12000-1199);
		assertEquals(1199, cc.available());
		assertFalse(cc.isBlocked());
		assertEquals(0, fragmenter.protect(produced(pc1), buf));
		assertTrue(cc.isBlocked());
		assertTrue(cc.needUnlock());
		assertEquals(0, fragmenter.protect(produced(pc2), buf));
		cc.onPacketSent(-1);
		assertEquals(1200, fragmenter.protect(produced(), buf));
		
		buf.clear();
		IPacket packet = peer.unprotect(peerState, buf);
		assertSame(PacketType.INITIAL, packet.getType());
		assertEquals(200, ((CryptoFrame)packet.getFrames().get(0)).getDataLength());
		assertEquals(400, ((CryptoFrame)packet.getFrames().get(1)).getDataLength());
		assertEquals(0, cc.available());
	}
	
	@Test
	public void testPadding() throws Exception {
		listener.onInit(INITIAL_SALT_V1, DEST_CID);
		peerListener.onInit(INITIAL_SALT_V1, DEST_CID);
		
		PacketNumberSpace space = state.getSpace(EncryptionLevel.INITIAL);
		space.frames().add(PingFrame.INSTANCE);
		buf.clear();
		fragmenter.protect(produced(), buf);
		assertEquals(1200, buf.position());
		assertTrue(space.frames().isEmpty());
		
		space.acks().add(0, 1000);
		buf.clear();
		fragmenter.protect(produced(), buf);
		assertEquals(1200, buf.position());
		assertTrue(space.acks().isEmpty());
		
		peerState.getConnectionIdManager().getSourceId();
		peerState.getConnectionIdManager().getDestinationPool().add(bytes("00010203"));
		peerState.getAntiAmplificator().incReceived(1200);
		processor = new QuicProcessor(peerState, null);
		fragmenter = new CryptoFragmenter(peerState, protection, new TestListener(peerListener), processor);
		space = peerState.getSpace(EncryptionLevel.INITIAL);
		space.frames().add(PingFrame.INSTANCE);
		buf.clear();
		
		fragmenter.protect(produced(), buf);
		assertEquals(1200, buf.position());
		assertTrue(space.frames().isEmpty());
		
		space.acks().add(0, 1000);
		buf.clear();
		fragmenter.protect(produced(), buf);
		assertEquals(42, buf.position());
		assertTrue(space.acks().isEmpty());
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
