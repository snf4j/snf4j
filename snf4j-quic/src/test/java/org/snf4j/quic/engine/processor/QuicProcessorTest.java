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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.Version;
import org.snf4j.quic.engine.CryptoEngineAdapter;
import org.snf4j.quic.engine.EncryptionLevel;
import org.snf4j.quic.engine.FlyingFrames;
import org.snf4j.quic.engine.HandshakeState;
import org.snf4j.quic.engine.PacketNumberSpace;
import org.snf4j.quic.engine.QuicState;
import org.snf4j.quic.engine.TestConfig;
import org.snf4j.quic.engine.TestTime;
import org.snf4j.quic.engine.crypto.CryptoEngine;
import org.snf4j.quic.engine.crypto.TestEngineStateListener;
import org.snf4j.quic.frame.AckFrame;
import org.snf4j.quic.frame.HandshakeDoneFrame;
import org.snf4j.quic.frame.PingFrame;
import org.snf4j.quic.packet.HandshakePacket;
import org.snf4j.quic.packet.IPacket;
import org.snf4j.quic.packet.InitialPacket;
import org.snf4j.quic.packet.OneRttPacket;
import org.snf4j.quic.packet.RetryPacket;
import org.snf4j.quic.packet.VersionNegotiationPacket;
import org.snf4j.tls.engine.DelegatedTaskMode;
import org.snf4j.tls.engine.EngineHandlerBuilder;
import org.snf4j.tls.engine.EngineParametersBuilder;
import org.snf4j.tls.engine.HandshakeEngine;

public class QuicProcessorTest extends CommonTest {

	QuicState state;
	
	QuicProcessor processor;
	
	CryptoEngineAdapter adapter;
	
	public void before() throws Exception {
		super.before();
		state = new QuicState(true);

		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder().delegatedTaskMode(DelegatedTaskMode.NONE);
		CryptoEngine engine = new CryptoEngine(new HandshakeEngine(true, epb.build(), ehb.build(), new TestEngineStateListener()));
		adapter = new CryptoEngineAdapter(engine);
		processor = new QuicProcessor(state, adapter);
	}
	
	@Test
	public void testProcess() throws Exception {
		IPacket p = new RetryPacket(bytes("00"), bytes("01"), Version.V1, bytes(), bytes(16));
		processor.process(p, false);
		assertNull(state.getConnectionIdManager().getDestinationPool().get());
		p = new VersionNegotiationPacket(bytes("00"), bytes("01"));
		processor.process(p, false);
		assertNull(state.getConnectionIdManager().getDestinationPool().get());
		p = new HandshakePacket(bytes("00"), 0, bytes("01"), Version.V1);
		processor.process(p, false);
		assertNull(state.getConnectionIdManager().getDestinationPool().get());
		p = new InitialPacket(bytes("00"), 0, bytes("01020304"), Version.V1, bytes());
		processor.process(p, false);
		assertArrayEquals(bytes("01020304"), state.getConnectionIdManager().getDestinationPool().get().getId());
		p = new InitialPacket(bytes("00"), 1, bytes("0102030405"), Version.V1, bytes());
		processor.process(p, false);
		assertArrayEquals(bytes("01020304"), state.getConnectionIdManager().getDestinationPool().get().getId());
		PacketNumberSpace space = state.getSpace(EncryptionLevel.INITIAL);
		
		state.setHandshakeState(HandshakeState.DONE_WAITING);
		p.getFrames().add(new HandshakeDoneFrame());
		processor.process(p, false);
		assertSame(HandshakeState.DONE_RECEIVED, state.getHandshakeState());
		
		space = state.getSpace(EncryptionLevel.INITIAL);
		assertNull(space.acks().build(3, 1000, 3));
		processor.process(p, true);
		AckFrame ack = space.acks().build(3, 1000, 3);
		assertNotNull(ack);
		assertEquals(1, ack.getRanges().length);
		assertEquals(1, ack.getRanges()[0].getFrom());
		assertEquals(1, ack.getRanges()[0].getTo());
	}
	
	@Test
	public void testSending() throws Exception {
		IPacket p = new RetryPacket(bytes("00"), bytes("01"), Version.V1, bytes(), bytes(16));
		processor.sending(p, false, false, 100);
		assertNull(state.getConnectionIdManager().getDestinationPool().get());
		p = new VersionNegotiationPacket(bytes("00"), bytes("01"));
		processor.sending(p, false, false, 100);
		p = new OneRttPacket(bytes("00"), 10, false, false);
		state.setHandshakeState(HandshakeState.DONE_SENDING);
		processor.sending(p, true, false, 100);
		assertSame(HandshakeState.DONE_SENDING, state.getHandshakeState());
		p.getFrames().add(new HandshakeDoneFrame());
		processor.sending(p, true, false, 100);
		assertSame(HandshakeState.DONE_SENT, state.getHandshakeState());
		
		PacketNumberSpace space = state.getSpace(EncryptionLevel.APPLICATION_DATA);
		space.frames().fly(new HandshakeDoneFrame(), 10);
		processor.sending(p, true, false, 12345);
		assertEquals(12345, space.frames().getFlying(10).getSentBytes());
		assertTrue(space.frames().getFlying(10).isAckEliciting());
		assertFalse(space.frames().getFlying(10).isInFlight());

		p = new OneRttPacket(bytes("00"), 11, false, false);
		p.getFrames().add(new HandshakeDoneFrame());
		space.frames().fly(new HandshakeDoneFrame(), 11);
		processor.sending(p, false, true, 12346);
		assertEquals(12346, space.frames().getFlying(11).getSentBytes());
		assertFalse(space.frames().getFlying(11).isAckEliciting());
		assertTrue(space.frames().getFlying(11).isInFlight());
	}
	
	@Test
	public void testPreProcess() {
		TestTime time = new TestTime(100,200,300);
		state = new QuicState(true, new TestConfig(), time);
		processor = new QuicProcessor(state, adapter);
		
		processor.preProcess();
		assertEquals(100, processor.currentTime);
		processor.preProcess();
		assertEquals(200, processor.currentTime);
	}
	
	@Test
	public void testPreSending() {
		TestTime time = new TestTime(100,200,300);
		state = new QuicState(true, new TestConfig(), time);
		processor = new QuicProcessor(state, adapter);
		
		processor.preSending();
		assertEquals(100, processor.currentTime);
		processor.preSending();
		assertEquals(200, processor.currentTime);
	}

	@Test
	public void testSetSendTime() {
		TestTime time = new TestTime(100,200,300);
		state = new QuicState(true, new TestConfig(), time);
		processor = new QuicProcessor(state, adapter);
		PacketNumberSpace space = state.getSpace(EncryptionLevel.APPLICATION_DATA);
		
		processor.preSending();
		IPacket packet = new OneRttPacket(bytes("00"), 0, false, false);
		PingFrame frame = new PingFrame();
		packet.getFrames().add(frame);
		space.frames().fly(frame, 0);
		FlyingFrames flying = space.frames().getFlying(0);
		assertEquals(0, flying.getSentTime());
		processor.sending(packet, false, false, 101);
		assertEquals(100, flying.getSentTime());
		assertEquals(101, flying.getSentBytes());
	}
	
	@Test
	public void testSetLastAckElicitingTime() {
		TestTime time = new TestTime(111,200,300);
		state = new QuicState(true, new TestConfig(), time);
		processor = new QuicProcessor(state, adapter);
		PacketNumberSpace space = state.getSpace(EncryptionLevel.APPLICATION_DATA);

		processor.preSending();
		IPacket packet = new OneRttPacket(bytes("00"), 0, false, false);
		PingFrame frame = new PingFrame();
		packet.getFrames().add(frame);
		space.frames().fly(frame, 0);
		processor.sending(packet, false, false, 101);
		assertEquals(0, space.getLastAckElicitingTime());
		processor.sending(packet, true, false, 101);
		assertEquals(0, space.getLastAckElicitingTime());
		processor.sending(packet, false, true, 101);
		assertEquals(0, space.getLastAckElicitingTime());
		processor.sending(packet, true, true, 101);
		assertEquals(111, space.getLastAckElicitingTime());
	}
	
	@Test
	public void testSetReceiveTime() throws Exception {
		TestTime time = new TestTime(100000000,200,300);
		state = new QuicState(true, new TestConfig(), time);
		processor = new QuicProcessor(state, adapter);
		PacketNumberSpace space = state.getSpace(EncryptionLevel.APPLICATION_DATA);
		
		processor.preProcess();
		IPacket packet = new OneRttPacket(bytes("00"), 0, false, false);
		PingFrame frame = new PingFrame();
		packet.getFrames().add(frame);
		processor.process(packet, true);
		AckFrame ack = space.acks().build(3, 400000000, 1);
		assertEquals(150000, ack.getDelay());
	}
	
	@Test
	public void testSetAddressValidated() throws Exception {
		state = new QuicState(true);
		processor = new QuicProcessor(state, adapter);

		IPacket p = new InitialPacket(bytes("00"), 0, bytes("01"), Version.V1, bytes(""));
		PingFrame ping = new PingFrame();
		p.getFrames().add(ping);
		assertFalse(state.isAddressValidated());
		processor.process(p, true);
		assertTrue(state.isAddressValidated());
		
		state = new QuicState(false);
		processor = new QuicProcessor(state, adapter);
		assertFalse(state.isAddressValidated());
		processor.process(p, true);
		assertFalse(state.isAddressValidated());
		
		p = new HandshakePacket(bytes("00"), 0, bytes("01"), Version.V1);
		p.getFrames().add(ping);
		assertFalse(state.isAddressValidated());
		processor.process(p, true);
		assertTrue(state.isAddressValidated());
		p = new HandshakePacket(bytes("00"), 1, bytes("01"), Version.V1);
		p.getFrames().add(ping);
		assertTrue(state.isAddressValidated());
		processor.process(p, true);
		assertTrue(state.isAddressValidated());
	}
}
