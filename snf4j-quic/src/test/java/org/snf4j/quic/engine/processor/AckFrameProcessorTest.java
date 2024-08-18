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
package org.snf4j.quic.engine.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.Version;
import org.snf4j.quic.engine.EncryptionLevel;
import org.snf4j.quic.engine.LossDetector;
import org.snf4j.quic.engine.PacketNumberSpace;
import org.snf4j.quic.engine.QuicState;
import org.snf4j.quic.engine.TestConfig;
import org.snf4j.quic.engine.TestSessionTimer;
import org.snf4j.quic.engine.TestTime;
import org.snf4j.quic.frame.AckFrame;
import org.snf4j.quic.frame.AckRange;
import org.snf4j.quic.frame.EcnAckFrame;
import org.snf4j.quic.frame.FrameType;
import org.snf4j.quic.frame.PingFrame;
import org.snf4j.quic.packet.HandshakePacket;
import org.snf4j.quic.packet.IPacket;
import org.snf4j.quic.packet.InitialPacket;
import org.snf4j.quic.packet.OneRttPacket;

public class AckFrameProcessorTest extends CommonTest {

	@Test
	public void testSending() {
		AckFrameProcessor p = new AckFrameProcessor();
		
		assertSame(FrameType.ACK, p.getType());
		p.sending(null, null, null);
	}

	@Test
	public void testProcess() throws Exception {
		QuicState s = new QuicState(true);
		QuicProcessor p = new QuicProcessor(s, null);
		AckFrameProcessor ap = new AckFrameProcessor();
		HandshakePacket packet = new HandshakePacket(bytes(), 0, bytes(), Version.V1);
		
		try {
			ap.process(p, new AckFrame(0,1000), packet);
			fail();
		}
		catch (QuicException e) {
		}
		PacketNumberSpace space = s.getSpace(EncryptionLevel.HANDSHAKE);
		space.frames().fly(new PingFrame(), 0);
		assertFalse(!space.frames().hasFlying());
		space.frames().getFlying(0).onSending(1000, 1, true, true);
		assertEquals(0, space.getAckElicitingInFlight());
		ap.process(p, new AckFrame(0,1000), packet);
		assertEquals(-1, space.getAckElicitingInFlight());
		assertTrue(!space.frames().hasFlying());
		ap.process(p, new AckFrame(0,1000), packet);
		
		AckRange[] ranges = new AckRange[] {new AckRange(9,8), new AckRange(6,4)};
		try {
			ap.process(p, new AckFrame(ranges,1000), packet);
			fail();
		}
		catch (QuicException e) {
		}
		space.frames().fly(new PingFrame(), 6);
		try {
			ap.process(p, new AckFrame(ranges,1000), packet);
			fail();
		}
		catch (QuicException e) {
		}
		space.frames().fly(new PingFrame(), 9);
		space.frames().fly(new PingFrame(), 8);
		space.frames().fly(new PingFrame(), 6);
		space.frames().fly(new PingFrame(), 5);
		space.frames().fly(new PingFrame(), 4);
		assertFalse(!space.frames().hasFlying());
		space.frames().getFlying(9).onSending(1000, 1, true, true);
		space.frames().getFlying(8).onSending(1000, 1, true, true);
		space.frames().getFlying(6).onSending(1000, 1, true, true);
		space.frames().getFlying(5).onSending(1000, 1, false, true);
		space.frames().getFlying(4).onSending(1000, 1, false, true);
		assertEquals(-1, space.getAckElicitingInFlight());
		ap.process(p, new AckFrame(ranges,1000), packet);
		assertEquals(-4, space.getAckElicitingInFlight());
		assertTrue(!space.frames().hasFlying());
		
		ranges = new AckRange[] {new AckRange(10,10), new AckRange(6,4)};
		space.frames().fly(new PingFrame(), 10);
		assertFalse(!space.frames().hasFlying());
		try {
			ap.process(p, new AckFrame(ranges,1000), packet);
			fail();
		}
		catch (QuicException e) {
			assertSame(TransportError.PROTOCOL_VIOLATION, e.getTransportError());
		}
		
	}
	
	@Test
	public void testAddSample() throws Exception {
		TestConfig config = new TestConfig();
		TestTime time = new TestTime(1000_000_000L);
		QuicState s = new QuicState(true, config, time);
		QuicProcessor p = new QuicProcessor(s, null);
		AckFrameProcessor ap = new AckFrameProcessor();
		PacketNumberSpace space = s.getSpace(EncryptionLevel.APPLICATION_DATA);
		
		//largest not newly acked
		assertEquals(333_000_000L, s.getEstimator().getSmoothedRtt());
		AckFrame ack = new AckFrame(new AckRange[] {new AckRange(1,0)}, 1000);
		PingFrame ping = new PingFrame();
		IPacket packet = new OneRttPacket(bytes("0001"), 0, false, false);
		packet.getFrames().add(ping);
		packet.getFrames().add(ack);
		p.preProcess();
		space.frames().fly(ping, 1);
		space.frames().ack(1);
		space.frames().fly(ping, 0);
		space.frames().getFlying(0).onSending(0, 100, false, true);
		ap.process(p, ack, packet);
		assertEquals(333_000_000L, s.getEstimator().getSmoothedRtt());
		
		//largest newly acked but not ack-eliciting
		packet = new OneRttPacket(bytes("0001"), 1, false, false);
		ack = new AckFrame(1, 1000);
		packet.getFrames().add(ack);
		space.frames().fly(ack, 1);
		space.frames().getFlying(1).onSending(0, 100, false, true);
		ap.process(p, ack, packet);
		assertEquals(333_000_000L, s.getEstimator().getSmoothedRtt());
		
		//first sample
		packet = new OneRttPacket(bytes("0001"), 2, false, false);
		ack = new AckFrame(2, 1000);
		packet.getFrames().add(ping);
		packet.getFrames().add(ack);
		space.frames().fly(ack, 2);
		space.frames().fly(ping, 2);
		space.frames().getFlying(2).onSending(980_000_000L, 100, true, true);
		ap.process(p, ack, packet);
		assertEquals(20_000_000L, s.getEstimator().getSmoothedRtt());
		
		//second sample
		s.setPeerAckDelayExponent(1);
		s.setPeerMaxAckDelay(10000);
		packet = new OneRttPacket(bytes("0001"), 3, false, false);
		ack = new AckFrame(3, 1000);
		packet.getFrames().add(ping);
		packet.getFrames().add(ack);
		space.frames().fly(ack, 3);
		space.frames().fly(ping, 3);
		space.frames().getFlying(3).onSending(960_000_000L, 100, true, true);
		ap.process(p, ack, packet);
		assertEquals(22250000L, s.getEstimator().getSmoothedRtt());
		
		//third sample
		space = s.getSpace(EncryptionLevel.HANDSHAKE);
		packet = new HandshakePacket(bytes("0001"), 4, bytes("00"), Version.V1);
		ack = new AckFrame(new AckRange[] {new AckRange(4,2)}, 1000);
		packet.getFrames().add(ack);
		space.frames().fly(ping, 3);
		space.frames().ack(3);
		space.frames().fly(ping, 2);
		space.frames().fly(ack, 4);
		space.frames().getFlying(2).onSending(960_000_000L, 100, true, true);
		space.frames().getFlying(4).onSending(960_000_000L, 100, true, true);
		ap.process(p, ack, packet);
		assertEquals(23468750L, s.getEstimator().getSmoothedRtt());
	}
	
	@Test
	public void testSetAddressValidatedByPeer() throws Exception {
		QuicState s = new QuicState(true);
		QuicProcessor p = new QuicProcessor(s, null);
		AckFrameProcessor ap = new AckFrameProcessor();
		
		assertFalse(s.isAddressValidatedByPeer());
		PacketNumberSpace space = s.getSpace(EncryptionLevel.INITIAL);
		IPacket packet = new InitialPacket(bytes("00"), 0, bytes("01"), Version.V1, bytes(""));
		AckFrame ack = new AckFrame(0, 1000);
		packet.getFrames().add(ack);
		space.frames().fly(ack, 0);
		space.frames().getFlying(0).onSending(1000, 100, true, true);
		ap.process(p, ack, packet);
		assertFalse(s.isAddressValidatedByPeer());

		space = s.getSpace(EncryptionLevel.HANDSHAKE);
		packet = new HandshakePacket(bytes("00"), 0, bytes("01"), Version.V1);
		packet.getFrames().add(ack);
		space.frames().fly(ack, 0);
		space.frames().getFlying(0).onSending(1000, 100, true, true);
		ap.process(p, ack, packet);
		assertTrue(s.isAddressValidatedByPeer());
	}
	
	@Test
	public void testProcessEcn() throws Exception {
		QuicState s = new QuicState(true);
		QuicProcessor p = new QuicProcessor(s, null);
		AckFrameProcessor ap = new AckFrameProcessor();
		PacketNumberSpace space = s.getSpace(EncryptionLevel.INITIAL);
		
		IPacket packet = new InitialPacket(bytes("00"), 0, bytes("01"), Version.V1, bytes(""));
		AckFrame ack = new EcnAckFrame(0, 1000, 0, 0, 111);
		packet.getFrames().add(ack);
		space.frames().fly(ack, 0);
		space.frames().getFlying(0).onSending(1000, 100, true, true);
		assertEquals(0, space.getEcnCeCount());
		ap.process(p, ack, packet);
		assertEquals(111, space.getEcnCeCount());

		space.frames().fly(ack, 1);
		space.frames().fly(ack, 2);
		space.frames().getFlying(1).onSending(1000, 100, true, true);
		space.frames().getFlying(2).onSending(1000, 100, true, true);
		space.updateAcked(2);
		ack = new EcnAckFrame(new AckRange[] {new AckRange(2,1)}, 1000, 0, 0, 222);
		ap.process(p, ack, packet);
		assertEquals(111, space.getEcnCeCount());		
	}
	
	@Test
	public void testOnPacketsLost() throws Exception {
		QuicState s = new QuicState(true, new TestConfig(), new TestTime(100000000L));
		QuicProcessor p = new QuicProcessor(s, null);
		AckFrameProcessor ap = new AckFrameProcessor();
		PacketNumberSpace space = s.getSpace(EncryptionLevel.INITIAL);
		
		IPacket packet = new InitialPacket(bytes("00"), 0, bytes("01"), Version.V1, bytes(""));
		AckFrame ack = new AckFrame(0, 1000);
		packet.getFrames().add(ack);
		space.frames().fly(ack, 0);
		space.frames().fly(ack, 1);
		space.frames().fly(ack, 2);
		space.updateAcked(2);
		space.frames().getFlying(0).onSending(1000, 100, true, true);
		space.frames().getFlying(1).onSending(-100000000000L, 600, true, true);
		assertEquals(0, space.getEcnCeCount());
		s.getCongestion().onPacketSent(2000);
		p.preProcess();
		assertEquals(0, space.frames().getLost().size());
		ap.process(p, ack, packet);
		assertEquals(1, space.frames().getLost().size());
		assertTrue(s.getCongestion().accept(1200*5 - 1400));
		assertTrue(s.getCongestion().accept(1200*5 - 1400 + 1));
	}
	
	@Test
	public void testResetPtoCount() throws Exception {
		QuicState s = new QuicState(true, new TestConfig(), new TestTime(100000000L));
		QuicProcessor p = new QuicProcessor(s, null);
		AckFrameProcessor ap = new AckFrameProcessor();
		PacketNumberSpace space = s.getSpace(EncryptionLevel.INITIAL);
		
		IPacket packet = new InitialPacket(bytes("00"), 0, bytes("01"), Version.V1, bytes(""));
		AckFrame ack = new AckFrame(0, 1000);
		packet.getFrames().add(ack);
		space.frames().fly(ack, 0);
		space.frames().getFlying(0).onSending(1000, 100, true, true);
		TestSessionTimer timer = new TestSessionTimer(null);
		s.getTimer().init(timer, () -> {});
		Field f = LossDetector.class.getDeclaredField("ptoCount");
		f.setAccessible(true);
		f.setInt(s.getLossDetector(), 4);
		ap.process(p, ack, packet);
		assertEquals(4, f.get(s.getLossDetector()));
		assertEquals("TimerTask;15984;true|", timer.trace());

		packet = new InitialPacket(bytes("00"), 1, bytes("01"), Version.V1, bytes(""));
		ack = new AckFrame(1, 1000);
		packet.getFrames().add(ack);
		space.frames().fly(ack, 1);
		space.frames().getFlying(1).onSending(1000, 100, true, true);
		assertEquals(4, f.get(s.getLossDetector()));
		s.setAddressValidatedByPeer();
		assertEquals(4, f.get(s.getLossDetector()));		
		ap.process(p, ack, packet);
		assertEquals(0, f.get(s.getLossDetector()));		
	}
	
	@Test
	public void testOnPacketAcked() throws Exception {
		QuicState s = new QuicState(true, new TestConfig(), new TestTime(100000000L));
		QuicProcessor p = new QuicProcessor(s, null);
		AckFrameProcessor ap = new AckFrameProcessor();
		PacketNumberSpace space = s.getSpace(EncryptionLevel.INITIAL);
		
		IPacket packet = new InitialPacket(bytes("00"), 0, bytes("01"), Version.V1, bytes(""));
		AckFrame ack = new AckFrame(0, 1000);
		packet.getFrames().add(ack);
		space.frames().fly(ack, 0);
		space.frames().getFlying(0).onSending(1000, 100, true, true);
		s.getCongestion().onPacketSent(200);
		assertTrue(s.getCongestion().accept(12000-200));
		assertFalse(s.getCongestion().accept(12000-200+1));
		ap.process(p, ack, packet);
		assertTrue(s.getCongestion().accept(12000-100));
		assertFalse(s.getCongestion().accept(12000-100+1));
	}
	
	@Test
	public void testRecover() {
		AckFrameProcessor p = new AckFrameProcessor();
		p.recover(null, null, null);
	}
}
