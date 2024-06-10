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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.Version;
import org.snf4j.quic.engine.CryptoEngineAdapter;
import org.snf4j.quic.engine.EncryptionLevel;
import org.snf4j.quic.engine.HandshakeState;
import org.snf4j.quic.engine.PacketNumberSpace;
import org.snf4j.quic.engine.QuicState;
import org.snf4j.quic.engine.crypto.CryptoEngine;
import org.snf4j.quic.engine.crypto.TestEngineStateListener;
import org.snf4j.quic.frame.AckFrame;
import org.snf4j.quic.frame.HandshakeDoneFrame;
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
		assertNull(space.acks().build(3));
		processor.process(p, true);
		AckFrame ack = space.acks().build(3);
		assertNotNull(ack);
		assertEquals(1, ack.getRanges().length);
		assertEquals(1, ack.getRanges()[0].getFrom());
		assertEquals(1, ack.getRanges()[0].getTo());
	}
	
	@Test
	public void testSending() throws Exception {
		IPacket p = new RetryPacket(bytes("00"), bytes("01"), Version.V1, bytes(), bytes(16));
		processor.sending(p);
		assertNull(state.getConnectionIdManager().getDestinationPool().get());
		p = new VersionNegotiationPacket(bytes("00"), bytes("01"));
		processor.sending(p);
		p = new OneRttPacket(bytes("00"), 10, false, false);
		state.setHandshakeState(HandshakeState.DONE_SENDING);
		processor.sending(p);
		assertSame(HandshakeState.DONE_SENDING, state.getHandshakeState());
		p.getFrames().add(new HandshakeDoneFrame());
		processor.sending(p);
		assertSame(HandshakeState.DONE_SENT, state.getHandshakeState());
	}
}
