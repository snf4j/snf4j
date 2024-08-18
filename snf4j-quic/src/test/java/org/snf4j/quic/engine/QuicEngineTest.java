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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.snf4j.core.engine.HandshakeStatus.FINISHED;
import static org.snf4j.core.engine.HandshakeStatus.NEED_TASK;
import static org.snf4j.core.engine.HandshakeStatus.NEED_TIMER;
import static org.snf4j.core.engine.HandshakeStatus.NEED_UNWRAP;
import static org.snf4j.core.engine.HandshakeStatus.NEED_UNWRAP_AGAIN;
import static org.snf4j.core.engine.HandshakeStatus.NEED_WRAP;
import static org.snf4j.core.engine.HandshakeStatus.NOT_HANDSHAKING;
import static org.snf4j.core.engine.Status.OK;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.snf4j.core.TestSession;
import org.snf4j.core.engine.EngineResult;
import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.core.engine.IEngine;
import org.snf4j.core.engine.IEngineResult;
import org.snf4j.core.engine.Status;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.Version;
import org.snf4j.quic.engine.crypto.CryptoEngine;
import org.snf4j.quic.engine.crypto.ProducedCrypto;
import org.snf4j.quic.engine.processor.QuicProcessor;
import org.snf4j.quic.frame.CryptoFrame;
import org.snf4j.quic.frame.HandshakeDoneFrame;
import org.snf4j.quic.frame.MultiPaddingFrame;
import org.snf4j.quic.frame.PingFrame;
import org.snf4j.quic.metric.QuicMetricsBuilder;
import org.snf4j.quic.metric.TestCCMetric;
import org.snf4j.quic.packet.IPacket;
import org.snf4j.quic.packet.InitialPacket;
import org.snf4j.quic.packet.OneRttPacket;
import org.snf4j.tls.engine.DelegatedTaskMode;
import org.snf4j.tls.engine.EngineHandlerBuilder;
import org.snf4j.tls.engine.EngineParametersBuilder;
import org.snf4j.tls.session.ISession;

public class QuicEngineTest extends CommonTest {

	EngineHandlerBuilder handlerBld;
	
	EngineParametersBuilder paramBld;
	
	ByteBuffer in, out;
	
	@Override
	public void before() throws Exception {
		super.before();
		handlerBld = new EngineHandlerBuilder(km(), tm());
		paramBld = new EngineParametersBuilder().delegatedTaskMode(DelegatedTaskMode.NONE);
		in = ByteBuffer.allocate(4096);
		out = ByteBuffer.allocate(4096);
	}
	
	void clear() {
		in.clear().flip();
		out.clear();
	}

	void clear(byte[] data) {
		in.clear();
		in.put(data).flip();
		out.clear();
	}
	
	void flip() {
		ByteBuffer tmp = in;
		in = out;
		out = tmp;
		in.flip();
		out.clear();
	}

	byte[] out() {
		ByteBuffer dup = out.duplicate();
		dup.flip();
		byte[] data = new byte[dup.remaining()];
		dup.get(data);
		return data;
	}

	void assertTimer(IEngine engine, IEngineResult result) throws Exception {
		assertSame(OK, result.getStatus());
		assertEquals(0, result.bytesConsumed());
		assertEquals(0, result.bytesProduced());
		assertSame(NEED_TIMER, result.getHandshakeStatus());
		assertSame(NEED_TIMER, engine.getHandshakeStatus());
		engine.timer(new TestTimer(), null);
	}
	
	@Test
	public void testLink() throws Exception {
		TestCCMetric ccm = new TestCCMetric();
		QuicState state = new QuicState(true, new QuicMetricsBuilder().congestion(ccm).build());
		QuicEngine e = new QuicEngine(state, paramBld.build(), handlerBld.build());
		TestSession session = new TestSession("");
		e.link(session);
		assertSame(session, state.getSession());
		state.getCongestion().onPacketSent(111);
		assertEquals("W12000|S111|", ccm.trace());
	}
	
	@Test
	public void testInitialHandshakeStatus() throws Exception {
		QuicEngine e = new QuicEngine(true, paramBld.build(), handlerBld.build());
		assertSame(NOT_HANDSHAKING, e.getHandshakeStatus());
		e.beginHandshake();
		assertSame(NEED_WRAP, e.getHandshakeStatus());
		e = new QuicEngine(true, paramBld.build(), handlerBld.build());
		assertTimer(e, e.wrap(in, out));
		e.wrap(in, out);
		assertSame(NEED_UNWRAP, e.getHandshakeStatus());
		e = new QuicEngine(true, paramBld.build(), handlerBld.build());
		assertTimer(e, e.unwrap(in, out));
		e.unwrap(in, out);
		assertSame(NEED_WRAP, e.getHandshakeStatus());
	}
	
	@Test
	public void testNeedWrap() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE);

		QuicState state = new QuicState(true);
		QuicEngine ce = new QuicEngine(state, epb.build(), ehb.build());
		assertFalse(ce.needWrap());
		Field f = QuicEngine.class.getDeclaredField("cryptoEngine");
		f.setAccessible(true);
		((CryptoEngine) f.get(ce)).start();
		assertTrue(ce.needWrap());
		
		state = new QuicState(true);
		ce = new QuicEngine(state, epb.build(), ehb.build());
		assertFalse(ce.needWrap());
		f = QuicEngine.class.getDeclaredField("cryptoFragmenter");
		f.setAccessible(true);
		((CryptoFragmenter) f.get(ce)).addPending(new ProducedCrypto(ByteBuffer.allocate(10), EncryptionLevel.INITIAL, 0));
		assertTrue(ce.needWrap());		
	}
	
	@Test
	public void testHandshake() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE);

		QuicEngine ce = new QuicEngine(true, epb.build(), ehb.build());
		QuicEngine se = new QuicEngine(false, epb.build(), ehb.build());
		
		clear();
		assertTimer(ce, ce.wrap(in, out));
		IEngineResult r = ce.wrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertEquals(1200, r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
		
		flip();
		assertTimer(se, se.unwrap(in, out));
		r = se.unwrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_WRAP, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(1200, r.bytesConsumed());
		
		clear();
		r = se.wrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertEquals(1200, r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
		
		flip();
		r = ce.unwrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_WRAP, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(1200, r.bytesConsumed());
		
		clear();
		r = ce.wrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertEquals(1200, r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
		
		flip();
		r = se.unwrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_WRAP, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(1200, r.bytesConsumed());
		
		//delaying wrapping of handshake done
		QuicState state = state(se);
		state.getSpace(EncryptionLevel.HANDSHAKE).frames().add(new MultiPaddingFrame(1140));
		clear();
		r = se.wrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_WRAP, r.getHandshakeStatus());
		assertSame(NEED_WRAP, se.getHandshakeStatus());
		flip();
		r = ce.unwrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertSame(NEED_UNWRAP, ce.getHandshakeStatus());
		
		clear();
		r = se.wrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(FINISHED, r.getHandshakeStatus());
		assertSame(NOT_HANDSHAKING, se.getHandshakeStatus());
		assertEquals(out.position(), r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
		
		//broken handshake done
		flip();
		byte[] data = bytes(in);
		data[data.length-1]++;
		r = ce.unwrap(ByteBuffer.wrap(data), out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertSame(NEED_UNWRAP, ce.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(data.length, r.bytesConsumed());
		
		//split crypto data with session ticket
		state = state(ce);
		PacketProtection protection = protection(ce);
		IPacket packet = protection.unprotect(state, in);
		CryptoFrame frame = (CryptoFrame) packet.getFrames().get(2);
		data = bytes(frame.getData());
		int offset = (int) frame.getDataOffset();
		frame.getData().limit(data.length-1);
		packet = new OneRttPacket(packet.getDestinationId(), packet.getPacketNumber()+1, false, false);
		packet.getFrames().add(HandshakeDoneFrame.INSTANCE);
		packet.getFrames().add(frame);
		clear();
		protection(se).protect(state(se), packet, out);
		flip();

		r = ce.unwrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(FINISHED, r.getHandshakeStatus());
		assertSame(NEED_WRAP, ce.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(in.position(), r.bytesConsumed());
		
		clear();
		r = ce.wrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertSame(NEED_UNWRAP, ce.getHandshakeStatus());
		assertEquals(out.position(), r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
				
		//crypto data with final part
		packet = new OneRttPacket(packet.getDestinationId(), packet.getPacketNumber()+1, false, false);
		packet.getFrames().add(new MultiPaddingFrame(100));
		byte[] lastData = new byte[1];
		lastData[0] = data[data.length-1];
		frame = new CryptoFrame(offset+data.length-1, ByteBuffer.wrap(lastData));
		packet.getFrames().add(frame);
		clear();
		protection(se).protect(state(se), packet, out);
		flip();
		
		r = ce.unwrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NOT_HANDSHAKING, r.getHandshakeStatus());
		assertSame(NOT_HANDSHAKING, ce.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(in.position(), r.bytesConsumed());
	}

	@Test
	public void testHandshakeWithCertificateTasks() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES);

		QuicEngine ce = new QuicEngine(true, epb.build(), ehb.build());
		QuicEngine se = new QuicEngine(false, epb.build(), ehb.build());
		
		clear();
		assertTimer(ce, ce.wrap(in, out));
		IEngineResult r = ce.wrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertEquals(1200, r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
		
		flip();
		assertTimer(se, se.unwrap(in, out));
		r = se.unwrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_TASK, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(1200, r.bytesConsumed());
		assertSame(NEED_TASK, se.getHandshakeStatus());
		Runnable task = se.getDelegatedTask();
		assertNotNull(task);
		assertSame(NEED_TASK, se.getHandshakeStatus());
		task.run();
		assertSame(NEED_WRAP, se.getHandshakeStatus());
		
		clear();
		r = se.wrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertEquals(1200, r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
		
		flip();
		r = ce.unwrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_TASK, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(1200, r.bytesConsumed());
		assertSame(NEED_TASK, ce.getHandshakeStatus());
		task = ce.getDelegatedTask();
		assertSame(NEED_TASK, ce.getHandshakeStatus());
		task.run();		
		assertSame(NEED_UNWRAP_AGAIN, ce.getHandshakeStatus());
		
		clear();
		r = ce.unwrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_WRAP, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
		
		clear();
		r = ce.wrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertEquals(1200, r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
		
		flip();
		r = se.unwrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_WRAP, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(1200, r.bytesConsumed());
		
		clear();
		r = se.wrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(FINISHED, r.getHandshakeStatus());
		assertSame(NOT_HANDSHAKING, se.getHandshakeStatus());
		assertEquals(out.position(), r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
		
		flip();
		r = ce.unwrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(FINISHED, r.getHandshakeStatus());
		assertSame(NOT_HANDSHAKING, ce.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(in.position(), r.bytesConsumed());
	}
	
	@Test
	public void testHandshakeWithAllTasks() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.ALL);

		QuicEngine ce = new QuicEngine(true, epb.build(), ehb.build());
		QuicEngine se = new QuicEngine(false, epb.build(), ehb.build());
		
		assertSame(NOT_HANDSHAKING, ce.getHandshakeStatus());
		assertSame(NOT_HANDSHAKING, se.getHandshakeStatus());
		ce.beginHandshake();
		assertSame(NEED_WRAP, ce.getHandshakeStatus());
		se.beginHandshake();
		assertSame(NEED_UNWRAP, se.getHandshakeStatus());
		ce.beginHandshake();
		assertSame(NEED_WRAP, ce.getHandshakeStatus());
		se.beginHandshake();
		assertSame(NEED_UNWRAP, se.getHandshakeStatus());
		
		clear();
		assertTimer(ce, ce.wrap(in, out));
		IEngineResult r = ce.wrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_TASK, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
		assertSame(NEED_TASK, ce.getHandshakeStatus());
		Runnable task = ce.getDelegatedTask();
		assertNotNull(task);
		assertSame(NEED_TASK, ce.getHandshakeStatus());
		task.run();
		assertNull(ce.getDelegatedTask());
		assertSame(NEED_WRAP, ce.getHandshakeStatus());
		
		r = ce.wrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertEquals(1200, r.bytesProduced());
		assertEquals(0, r.bytesConsumed());		
		
		flip();
		assertTimer(se, se.unwrap(in, out));
		r = se.unwrap(in, out);
		assertSame(Status.OK, r.getStatus());
		assertSame(NEED_TASK, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(1200, r.bytesConsumed());
		assertSame(NEED_TASK, se.getHandshakeStatus());
		task = se.getDelegatedTask();
		assertNotNull(task);
		assertSame(NEED_TASK, se.getHandshakeStatus());
		task.run();
		assertSame(NEED_TASK, se.getHandshakeStatus());
		task = se.getDelegatedTask();
		assertNotNull(task);
		assertSame(NEED_TASK, se.getHandshakeStatus());
		task.run();
		assertSame(NEED_WRAP, se.getHandshakeStatus());
				
		clear();
		r = se.wrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertEquals(1200, r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
		
		flip();
		r = ce.unwrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_TASK, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(1200, r.bytesConsumed());
		assertSame(NEED_TASK, ce.getHandshakeStatus());
		task = ce.getDelegatedTask();
		assertNotNull(task);
		assertSame(NEED_TASK, ce.getHandshakeStatus());
		task.run();
		assertSame(NEED_UNWRAP_AGAIN, ce.getHandshakeStatus());
		
		clear();
		r = ce.unwrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_WRAP, r.getHandshakeStatus());
		QuicState state = state(ce);
		state.getCongestion().onPacketSent(50000);
		assertSame(NEED_UNWRAP, ce.getHandshakeStatus());
		state.getCongestion().onPacketSent(-50000);
		assertSame(NEED_WRAP, ce.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
		
		clear();
		r = ce.wrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertEquals(1200, r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
		
		flip();
		r = se.unwrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_WRAP, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(1200, r.bytesConsumed());
		
		clear();
		r = se.wrap(in, out);
		assertSame(Status.OK, r.getStatus());
		assertSame(HandshakeStatus.FINISHED, r.getHandshakeStatus());
		assertSame(HandshakeStatus.NOT_HANDSHAKING, se.getHandshakeStatus());
		assertEquals(out.position(), r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
		
		flip();
		r = ce.unwrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(FINISHED, r.getHandshakeStatus());
		assertSame(NOT_HANDSHAKING, ce.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(in.position(), r.bytesConsumed());
		
		Object session = ce.getSession();
		assertNotNull(session);
		assertTrue(session instanceof ISession);
		
		ce.closeOutbound();
		assertFalse(ce.isOutboundDone());
		assertFalse(ce.isInboundDone());
		assertSame(NEED_WRAP, ce.getHandshakeStatus());
		clear();
		r = ce.wrap(in, out);
		assertSame(Status.CLOSED, r.getStatus());
		assertSame(NOT_HANDSHAKING, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
		assertSame(NOT_HANDSHAKING, ce.getHandshakeStatus());
		assertTrue(ce.isOutboundDone());
		assertTrue(ce.isInboundDone());
		ce.closeOutbound();
		assertSame(NOT_HANDSHAKING, ce.getHandshakeStatus());
		
		r = ce.unwrap(in, out);
		assertSame(Status.CLOSED, r.getStatus());
		assertSame(NOT_HANDSHAKING, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
		
		r = ce.wrap(in, out);
		assertSame(Status.CLOSED, r.getStatus());
		assertSame(NOT_HANDSHAKING, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
	}

	@Test
	public void testBufferSizes() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.ALL);

		QuicEngine ce = new QuicEngine(true, epb.build(), ehb.build());
		assertEquals(1200, ce.getMinNetworkBufferSize());
		assertEquals(4096, ce.getMinApplicationBufferSize());
		assertEquals(1200, ce.getMaxNetworkBufferSize());
		assertEquals(4096, ce.getMaxApplicationBufferSize());
	}
	
	QuicState state(QuicEngine e) throws Exception {
		Field f = QuicEngine.class.getDeclaredField("state");
		f.setAccessible(true);
		return (QuicState) f.get(e);
	}

	PacketProtection protection(QuicEngine e) throws Exception {
		Field f = QuicEngine.class.getDeclaredField("protection");
		f.setAccessible(true);
		return (PacketProtection) f.get(e);
	}
	
	@Test
	public void testUnwrapInvalidPacketsByServer() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE);

		QuicEngine ce = new QuicEngine(true, epb.build(), ehb.build());
		QuicEngine se = new QuicEngine(false, epb.build(), ehb.build());
		
		clear();
		assertTimer(ce, ce.wrap(in, out));
		ce.wrap(in, out);
		flip();
		in.put(100, (byte) (in.get(100)+1));
		assertTimer(se, se.unwrap(in, out));
		IEngineResult r = se.unwrap(in, out);
		assertSame(Status.OK, r.getStatus());
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(1200, r.bytesConsumed());
		
		QuicState state = state(ce);
		PacketProtection protection = protection(ce);
		PacketNumberSpace space = state.getSpace(EncryptionLevel.INITIAL);
		byte[] dcid = state.getConnectionIdManager().getOriginalId();
		byte[] scid = state.getConnectionIdManager().getSourceId();
		
		//initial no ack-eliciting packet less than 1200
		InitialPacket p = new InitialPacket(dcid, space.next(), scid, Version.V1, bytes());
		p.getFrames().add(new MultiPaddingFrame(1000));
		int len = p.getLength(-1, 16);
		p.getFrames().add(new MultiPaddingFrame(1200-len-1));
		clear();
		protection.protect(state, p, out);
		flip();
		r = se.unwrap(in, out);
		assertSame(Status.OK, r.getStatus());
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(1199, r.bytesConsumed());
	
		//initial ack-eliciting packet less than 1200
		p = new InitialPacket(dcid, space.next(), scid, Version.V1, bytes());
		p.getFrames().add(new MultiPaddingFrame(1000));
		p.getFrames().add(PingFrame.INSTANCE);
		len = p.getLength(-1, 16);
		p.getFrames().add(new MultiPaddingFrame(1200-len-1));
		clear();
		protection.protect(state, p, out);
		flip();
		r = se.unwrap(in, out);
		assertSame(Status.OK, r.getStatus());
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(1199, r.bytesConsumed());
		assertNull(space.acks().build(1, 1000, 3));
		
		//packet with not allowed frame
		p = new InitialPacket(dcid, space.next(), scid, Version.V1, bytes());
		p.getFrames().add(HandshakeDoneFrame.INSTANCE);
		p.getFrames().add(new MultiPaddingFrame(4));
		clear();
		protection.protect(state, p, out);
		flip();
		try {
			se.unwrap(in, out);
			fail();
		}
		catch (QuicException e) {}
	}
	
	@Test
	public void testUnwrapInvalidPacketsByClient() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE);

		QuicEngine ce = new QuicEngine(true, epb.build(), ehb.build());
		QuicEngine se = new QuicEngine(false, epb.build(), ehb.build());
		
		clear();
		assertTimer(ce, ce.wrap(in, out));
		ce.wrap(in, out);
		flip();
		assertTimer(se, se.unwrap(in, out));
		se.unwrap(in, out);
		clear();
		se.wrap(in, out);
		flip();
		in.put(100, (byte) (in.get(100)+1));
		IEngineResult r = ce.unwrap(in, out);
		assertSame(Status.OK, r.getStatus());
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(1200, r.bytesConsumed());
		
		QuicState state = state(se);
		PacketProtection protection = protection(se);
		PacketNumberSpace space = state.getSpace(EncryptionLevel.INITIAL);
		byte[] dcid = state.getConnectionIdManager().getDestinationId();
		byte[] scid = state.getConnectionIdManager().getSourceId();

		//initial ack-eliciting packet less than 1200
		InitialPacket p = new InitialPacket(dcid, space.next(), scid, Version.V1, bytes());
		p.getFrames().add(new MultiPaddingFrame(1000));
		p.getFrames().add(PingFrame.INSTANCE);
		int len = p.getLength(-1, 16);
		p.getFrames().add(new MultiPaddingFrame(1200-len-1));
		clear();
		protection.protect(state, p, out);
		flip();
		r = ce.unwrap(in, out);
		assertSame(Status.OK, r.getStatus());
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(1199, r.bytesConsumed());
		assertNull(space.acks().build(1, 1000, 3));
		
		//initial no ack-eliciting packet less than 1200
		p = new InitialPacket(dcid, space.next(), scid, Version.V1, bytes());
		p.getFrames().add(new MultiPaddingFrame(1000));
		len = p.getLength(-1, 16);
		p.getFrames().add(new MultiPaddingFrame(1200-len-1));
		clear();
		protection.protect(state, p, out);
		flip();
		r = ce.unwrap(in, out);
		assertSame(Status.OK, r.getStatus());
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(1199, r.bytesConsumed());
		assertNull(space.acks().build(1, 1000, 3));
		
	}
	
	@Test
	public void testWrapException() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE);

		QuicEngine ce = new QuicEngine(true, epb.build(), ehb.build());
		QuicEngine se = new QuicEngine(false, epb.build(), ehb.build());

		clear();
		assertTimer(ce, ce.wrap(in, out));
		ce.wrap(in, out);
		flip();
		assertTimer(se, se.unwrap(in, out));
		se.unwrap(in, out);

		QuicState state = state(se);
		EncryptionContext ctx = state.getContext(EncryptionLevel.INITIAL);
		Encryptor enc = ctx.getEncryptor();
		ctx.setEncryptor(new Encryptor(enc.getAead(), enc.getProtector(), new byte[0], 1000));
		
		clear();
		try {
			se.wrap(in, out);
			fail();
		}
		catch (QuicException e) {
			assertSame(TransportError.INTERNAL_ERROR, e.getTransportError());
		}

		ce = new QuicEngine(true, epb.build(), ehb.build());
		se = new QuicEngine(false, epb.build(), ehb.build()) {
			
			@Override
			IEngineResult exception(Exception e, ByteBuffer src, ByteBuffer dst, boolean wrapping) throws QuicException {
				return new EngineResult(
						OK, 
						HandshakeStatus.NOT_HANDSHAKING, 
						wrapping ? 33 : 22, 
						44);
			}
		};
	
		clear();
		assertTimer(ce, ce.wrap(in, out));
		ce.wrap(in, out);
		flip();
		assertTimer(se, se.wrap(in, out));
		se.unwrap(in, out);

		state = state(se);
		ctx = state.getContext(EncryptionLevel.INITIAL);
		enc = ctx.getEncryptor();
		ctx.setEncryptor(new Encryptor(enc.getAead(), enc.getProtector(), new byte[0], 1000));

		clear();
		IEngineResult r = se.wrap(in, out);
		assertEquals(44, r.bytesProduced());
		assertEquals(33, r.bytesConsumed());
	}
	
	@Test
	public void testUnwrapException() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE);

		QuicEngine ce = new QuicEngine(true, epb.build(), ehb.build());
		QuicEngine se = new QuicEngine(false, epb.build(), ehb.build());
		
		clear();
		assertTimer(ce, ce.wrap(in, out));
		ce.wrap(in, out);
		flip();
		assertTimer(se, se.unwrap(in, out));
		se.unwrap(in, out);
		clear();
		se.wrap(in, out);

		QuicState state = state(ce);
		EncryptionContext ctx = state.getContext(EncryptionLevel.INITIAL);
		Decryptor enc = ctx.getDecryptor();
		ctx.setDecryptor(new Decryptor(enc.getAead(), enc.getProtector(), new byte[0], 1000, 1000));

		flip();
		try {
			ce.unwrap(in, out);
			fail();
		}
		catch (QuicException e) {
			assertSame(TransportError.INTERNAL_ERROR, e.getTransportError());
		}
		
		ce = new QuicEngine(true, epb.build(), ehb.build()) {
			
			@Override
			IEngineResult exception(Exception e, ByteBuffer src, ByteBuffer dst, boolean wrapping) throws QuicException {
				return new EngineResult(
						OK, 
						HandshakeStatus.NOT_HANDSHAKING, 
						wrapping ? 33 : 22, 
						44);
			}
		};
		se = new QuicEngine(false, epb.build(), ehb.build());

		clear();
		assertTimer(ce, ce.wrap(in, out));
		ce.wrap(in, out);
		flip();
		assertTimer(se, se.unwrap(in, out));
		se.unwrap(in, out);
		clear();
		se.wrap(in, out);

		state = state(ce);
		ctx = state.getContext(EncryptionLevel.INITIAL);
		enc = ctx.getDecryptor();
		ctx.setDecryptor(new Decryptor(enc.getAead(), enc.getProtector(), new byte[0], 1000, 1000));
	
		flip();
		IEngineResult r = ce.unwrap(in, out);
		assertEquals(44, r.bytesProduced());
		assertEquals(22, r.bytesConsumed());
	}
	
	@Test
	public void testHandlingOfUpdateTasks() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE);
		final AtomicReference<Boolean> ref = new AtomicReference<Boolean>(true);
		
		QuicEngine ce = new QuicEngine(true, epb.build(), ehb.build()) {
			
			@Override
			boolean updateTasks() throws QuicException {
				if (!ref.get()) {
					return super.updateTasks();
				}
				return true;
			}
		};
		
		QuicEngine se = new QuicEngine(false, epb.build(), ehb.build()) {

			@Override
			boolean updateTasks() throws QuicException {
				if (!ref.get()) {
					return super.updateTasks();
				}
				return true;
			}
		};

		clear();
		assertTimer(ce, ce.wrap(in, out));
		IEngineResult r = ce.wrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_WRAP, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
		
		ref.set(false);
		r = ce.wrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertEquals(1200, r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
		
		flip();
		ref.set(true);
		assertTimer(se, se.unwrap(in, out));
		r = se.unwrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(0, r.bytesConsumed());
	}
	
	@Test
	public void testKeyRotation() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE);

		QuicEngine ce = new QuicEngine(true, epb.build(), ehb.build());
		QuicEngine se = new QuicEngine(false, epb.build(), ehb.build());

		clear();
		assertTimer(ce, ce.wrap(in, out));
		IEngineResult r = ce.wrap(in, out);
		flip();
		assertTimer(se, se.unwrap(in, out));
		r = se.unwrap(in, out);
		clear();
		r = se.wrap(in, out);
		flip();
		r = ce.unwrap(in, out);
		clear();
		r = ce.wrap(in, out);
		flip();
		r = se.unwrap(in, out);
		
		clear();
		r = se.wrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(FINISHED, r.getHandshakeStatus());
		assertSame(NOT_HANDSHAKING, se.getHandshakeStatus());
		assertEquals(out.position(), r.bytesProduced());
		assertEquals(0, r.bytesConsumed());

		flip();
		r = ce.unwrap(in, out);
		assertSame(OK, r.getStatus());
		assertSame(FINISHED, r.getHandshakeStatus());
		assertSame(NOT_HANDSHAKING, ce.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(in.position(), r.bytesConsumed());
		
		QuicState state = state(ce);
		EncryptionContext ctx = state.getContext(EncryptionLevel.APPLICATION_DATA);
		ctx.rotateKeys();
		PacketNumberSpace space = state.getSpace(EncryptionLevel.APPLICATION_DATA);
		OneRttPacket packet = new OneRttPacket(
				state.getConnectionIdManager().getDestinationId(),
				space.next(),
				false,
				ctx.getKeyPhaseBit());
		packet.getFrames().add(new MultiPaddingFrame(10));
		packet.getFrames().add(new PingFrame());
		clear();
		protection(ce).protect(state, packet, out);

		flip();
		state = state(se);
		ctx = state.getContext(EncryptionLevel.APPLICATION_DATA);
		assertFalse(ctx.getKeyPhaseBit());
		se.unwrap(in, out);
		assertTrue(ctx.getKeyPhaseBit());
	}
	
	@Test
	public void testInitialKeysException() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE);

		QuicEngine ce = new QuicEngine(true, epb.build(), ehb.build()) {
			
			@Override
			void initialKeys(byte[] salt, byte[] connectionId) throws Exception {
				throw new Exception("xxx");
			}
		};
		clear();
		assertTimer(ce, ce.wrap(in, out));
		try {
			ce.wrap(in, out);
			fail();
		}
		catch (QuicException e) {
			assertSame(TransportError.INTERNAL_ERROR, e.getTransportError());
		}
	}
	
	@Test
	public void testWrapArray() throws Exception{
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE);

		QuicEngine ce = new QuicEngine(true, epb.build(), ehb.build());
		assertNull(ce.wrap(new ByteBuffer[2], out));
	}
	
	@Test
	public void testCallingPreprocess() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE);
		TestTime time = new TestTime(1234567, 1234568);
		QuicState state = new QuicState(false, new TestConfig(), time);
		
		QuicEngine se = new QuicEngine(state, epb.build(), ehb.build());
		assertTimer(se, se.unwrap(buffer("00030405"), out));
		se.unwrap(buffer("00030405"), out);
		Field f = QuicEngine.class.getDeclaredField("processor");
		f.setAccessible(true);
		QuicProcessor processor = (QuicProcessor) f.get(se);
		f = QuicProcessor.class.getDeclaredField("currentTime");
		f.setAccessible(true);
		assertEquals(1234567, f.getLong(processor));
		se.unwrap(buffer("00030405"), out);
		assertEquals(1234568, f.getLong(processor));
	}
	
	@Test
	public void testAntiAmplification() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.NONE);

		QuicEngine ce = new QuicEngine(true, epb.build(), ehb.build());
		QuicEngine se = new QuicEngine(false, epb.build(), ehb.build());
		
		IAntiAmplificator aa = state(ce).getAntiAmplificator();
		assertFalse(aa.isArmed());
		aa = state(se).getAntiAmplificator();
		assertTrue(aa.isArmed());
		
		clear();
		assertTimer(ce, ce.wrap(in, out));
		IEngineResult r = ce.wrap(in, out);
		flip();
		assertTimer(se, se.unwrap(in, out));
		r = se.unwrap(in, out);
		assertSame(NEED_WRAP, r.getHandshakeStatus());
		assertSame(NEED_WRAP, se.getHandshakeStatus());
		
		assertFalse(aa.accept(3*1200+1));
		assertTrue(aa.accept(2*1200+1));
		assertSame(NEED_WRAP, se.getHandshakeStatus());
		clear();
		r = se.wrap(in, out);
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertSame(NEED_UNWRAP, se.getHandshakeStatus());
		assertEquals(0, r.bytesProduced());
		assertEquals(0, out.position());
		
		aa.incReceived(1);
		assertSame(NEED_WRAP, se.getHandshakeStatus());
		r = se.wrap(in, out);
		assertSame(NEED_UNWRAP, r.getHandshakeStatus());
		assertSame(NEED_UNWRAP, se.getHandshakeStatus());
		assertEquals(1200, r.bytesProduced());
		assertEquals(1200, out.position());
		
		flip();
		r = ce.unwrap(in, out);
		assertSame(NEED_WRAP, r.getHandshakeStatus());
		assertSame(NEED_WRAP, ce.getHandshakeStatus());
		
		assertTrue(aa.isArmed());
		clear();
		ce.wrap(in, out);
		flip();
		r = se.unwrap(in, out);
		assertTrue(aa.isArmed());
		assertSame(NEED_WRAP, r.getHandshakeStatus());
		assertSame(NEED_WRAP, se.getHandshakeStatus());
		clear();
		r = se.wrap(in, out);
		assertFalse(aa.isArmed());
	}
	
	@Test
	public void testSetLossDetectionTimerAfterUnblockingAntiAmplificator() throws Exception {
			EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
			EngineParametersBuilder epb = new EngineParametersBuilder()
					.delegatedTaskMode(DelegatedTaskMode.NONE);

			//Unblocked after blocked
			QuicEngine ce = new QuicEngine(true, epb.build(), ehb.build());
			QuicEngine se = new QuicEngine(false, epb.build(), ehb.build());
			
			TestSessionTimer ctimer = new TestSessionTimer(null);
			state(ce).getTimer().init(ctimer, () -> {});
			TestSessionTimer stimer = new TestSessionTimer(null);
			state(se).getTimer().init(stimer, () -> {});
			IAntiAmplificator aa = state(ce).getAntiAmplificator();
			assertFalse(aa.isArmed());
			aa = state(se).getAntiAmplificator();
			assertTrue(aa.isArmed());
			
			clear();
			ce.wrap(in, out);
			flip();
			assertEquals("", stimer.trace());
			state(se).getSpace(EncryptionLevel.INITIAL).updateAckElicitingInFlight(1);
			state(se).getSpace(EncryptionLevel.INITIAL).setLastAckElicitingTime(System.nanoTime());
			se.unwrap(in, out);
			state(se).getSpace(EncryptionLevel.INITIAL).updateAckElicitingInFlight(-1);
			assertTrue(stimer.trace().startsWith("TimerTask;"));
			
			//Blocked after blocked
			ce = new QuicEngine(true, epb.build(), ehb.build());
			se = new QuicEngine(false, epb.build(), ehb.build());
			
			ctimer = new TestSessionTimer(null);
			state(ce).getTimer().init(ctimer, () -> {});
			stimer = new TestSessionTimer(null);
			state(se).getTimer().init(stimer, () -> {});
			aa = state(ce).getAntiAmplificator();
			assertFalse(aa.isArmed());
			aa = state(se).getAntiAmplificator();
			assertTrue(aa.isArmed());
			
			clear();
			ce.wrap(in, out);
			flip();
			assertEquals("", stimer.trace());
			state(se).getAntiAmplificator().incReceived(-1200);
			state(se).getSpace(EncryptionLevel.INITIAL).updateAckElicitingInFlight(1);
			state(se).getSpace(EncryptionLevel.INITIAL).setLastAckElicitingTime(System.nanoTime());
			se.unwrap(in, out);
			state(se).getSpace(EncryptionLevel.INITIAL).updateAckElicitingInFlight(-1);
			assertEquals("", stimer.trace());	
			
			//Unblocked after unblocked
			ce = new QuicEngine(true, epb.build(), ehb.build());
			se = new QuicEngine(false, epb.build(), ehb.build());
			
			ctimer = new TestSessionTimer(null);
			state(ce).getTimer().init(ctimer, () -> {});
			stimer = new TestSessionTimer(null);
			state(se).getTimer().init(stimer, () -> {});
			aa = state(ce).getAntiAmplificator();
			assertFalse(aa.isArmed());
			aa = state(se).getAntiAmplificator();
			assertTrue(aa.isArmed());
			
			clear();
			ce.wrap(in, out);
			flip();
			assertEquals("", stimer.trace());
			state(se).getAntiAmplificator().disarm();
			state(se).getSpace(EncryptionLevel.INITIAL).updateAckElicitingInFlight(1);
			state(se).getSpace(EncryptionLevel.INITIAL).setLastAckElicitingTime(System.nanoTime());
			se.unwrap(in, out);
			state(se).getSpace(EncryptionLevel.INITIAL).updateAckElicitingInFlight(-1);
			assertEquals("", stimer.trace());	
	}
}
