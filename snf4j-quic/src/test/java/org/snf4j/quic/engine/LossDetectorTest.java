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
import static org.snf4j.quic.engine.EncryptionLevel.APPLICATION_DATA;
import static org.snf4j.quic.engine.EncryptionLevel.HANDSHAKE;
import static org.snf4j.quic.engine.EncryptionLevel.INITIAL;

import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.snf4j.core.timer.DefaultTimer;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.engine.LossDetector.TimeAndSpace;
import org.snf4j.quic.engine.processor.QuicProcessor;
import org.snf4j.quic.frame.FrameType;
import org.snf4j.quic.frame.HandshakeDoneFrame;
import org.snf4j.quic.frame.IFrame;
import org.snf4j.quic.frame.PingFrame;

public class LossDetectorTest extends CommonTest {

	DefaultTimer timer;
	
	TestSessionTimer stimer;

	QuicState cli, srv;
	
	PacketNumberSpace cliSpace0, cliSpace1, cliSpace2;

	PacketNumberSpace srvSpace0, srvSpace1, srvSpace2;
	
	@Override
	public void before() throws Exception {
		super.before();
		cli = new QuicState(true);
		srv = new QuicState(false);
		cliSpace0 = cli.getSpace(INITIAL);
		cliSpace1 = cli.getSpace(HANDSHAKE);
		cliSpace2 = cli.getSpace(APPLICATION_DATA);
		srvSpace0 = srv.getSpace(INITIAL);
		srvSpace1 = srv.getSpace(HANDSHAKE);
		srvSpace2 = srv.getSpace(APPLICATION_DATA);
		timer = new DefaultTimer();
		stimer = new TestSessionTimer(timer);

	}
	
	@After
	public void after() {
		timer.cancel();
	}

	@Test
	public void testgetLossTimeAndSpace() {
		TimeAndSpace tas = cli.getLossDetector().getLossTimeAndSpace();
		assertSame(cliSpace0, tas.space);
		assertEquals(0, tas.time.longValue());
		
		cliSpace0.setLossTime(1);
		tas = cli.getLossDetector().getLossTimeAndSpace();
		assertSame(cliSpace0, tas.space);
		assertEquals(1, tas.time.longValue());
		
		cliSpace0.setLossTime(100);
		cliSpace1.setLossTime(100);
		tas = cli.getLossDetector().getLossTimeAndSpace();
		assertSame(cliSpace0, tas.space);
		assertEquals(100, tas.time.longValue());
	
		cliSpace1.setLossTime(99);
		tas = cli.getLossDetector().getLossTimeAndSpace();
		assertSame(cliSpace1, tas.space);
		assertEquals(99, tas.time.longValue());
		
		cliSpace1.setLossTime(101);
		tas = cli.getLossDetector().getLossTimeAndSpace();
		assertSame(cliSpace0, tas.space);
		assertEquals(100, tas.time.longValue());
		
		cliSpace0.setLossTime(0);
		cliSpace1.setLossTime(1);
		tas = cli.getLossDetector().getLossTimeAndSpace();
		assertSame(cliSpace1, tas.space);
		assertEquals(1, tas.time.longValue());

		cliSpace0.setLossTime(1);
		cliSpace1.setLossTime(2);
		cliSpace2.setLossTime(3);
		assertSame(cliSpace0, cli.getLossDetector().getLossTimeAndSpace().space);

		cliSpace0.setLossTime(3);
		cliSpace1.setLossTime(2);
		cliSpace2.setLossTime(1);
		assertSame(cliSpace2, cli.getLossDetector().getLossTimeAndSpace().space);
		
		cliSpace0.setLossTime(Long.MAX_VALUE);
		cliSpace1.setLossTime(Long.MAX_VALUE+1);
		cliSpace2.setLossTime(Long.MAX_VALUE-1);
		assertSame(cliSpace2, cli.getLossDetector().getLossTimeAndSpace().space);
		
		cliSpace0.setLossTime(Long.MAX_VALUE);
		cliSpace1.setLossTime(Long.MAX_VALUE-1);
		cliSpace2.setLossTime(Long.MAX_VALUE+1);
		assertSame(cliSpace1, cli.getLossDetector().getLossTimeAndSpace().space);
	}
	
	@Test
	public void testGetPtoPeriod() {
		assertEquals(333000000L + 4 * 166500000L, cli.getLossDetector().getPtoPeriod());
		cli.setHandshakeState(HandshakeState.DONE);
		cli.setPeerMaxAckDelay(24);
		assertEquals(333000000L + 4 * 166500000L + 24000000L, cli.getLossDetector().getPtoPeriod());
	}
	
	@Test
	public void testGetPtoTimeAndSpaceDuration() {
		assertEquals(333000000L, cli.getEstimator().getSmoothedRtt());
		assertEquals(166500000L, cli.getEstimator().getRttVar());
		
		TimeAndSpace tas = cli.getLossDetector().getPtoTimeAndSpace(111);
		long duration = 111L + (333000000L + 4 * 166500000L) * 1;
		assertEquals(duration, tas.time.longValue());
		
		cli.getLossDetector().onLossDetectionTimeout();
		tas = cli.getLossDetector().getPtoTimeAndSpace(112);
		duration = 112L + (333000000L + 4 * 166500000L) * 2;
		assertEquals(duration, tas.time.longValue());
		
		cli.getLossDetector().onLossDetectionTimeout();
		tas = cli.getLossDetector().getPtoTimeAndSpace(113);
		duration = 113L + (333000000L + 4 * 166500000L) * 4;
		assertEquals(duration, tas.time.longValue());

		cli.getLossDetector().resetPtoCount();
		cli.getEstimator().addSample(100, 0, 0, INITIAL);
		assertEquals(100L, cli.getEstimator().getSmoothedRtt());
		assertEquals(50L, cli.getEstimator().getRttVar());
		tas = cli.getLossDetector().getPtoTimeAndSpace(112);
		duration = 112L + (100L + 1000000L) * 1;
		assertEquals(duration, tas.time.longValue());
		
		cli.getLossDetector().onLossDetectionTimeout();
		tas = cli.getLossDetector().getPtoTimeAndSpace(113);
		duration = 113L + (100L + 1000000L) * 2;
		assertEquals(duration, tas.time.longValue());
	}
	
	@Test
	public void testGetPtoTimeAndSpaceAntiDeadlock() throws Exception {
		assertEquals(333000000L, cli.getEstimator().getSmoothedRtt());
		assertEquals(166500000L, cli.getEstimator().getRttVar());
		assertFalse(cli.isAddressValidatedByPeer());
		
		TimeAndSpace tas = cli.getLossDetector().getPtoTimeAndSpace(111);
		long duration = 111L + (333000000L + 4 * 166500000L) * 1;
		assertEquals(duration, tas.time.longValue());
		assertSame(cliSpace0, tas.space);
		
		CryptoEngineStateListener l = new CryptoEngineStateListener(cli);
		l.onInit(bytes(16), bytes(8));
		tas = cli.getLossDetector().getPtoTimeAndSpace(111);
		duration = 111L + (333000000L + 4 * 166500000L) * 1;
		assertEquals(duration, tas.time.longValue());
		assertSame(cliSpace0, tas.space);
		
		EncryptionContext ctx1 = cli.getContext(INITIAL);
		EncryptionContext ctx2 = cli.getContext(HANDSHAKE);
		ctx2.setEncryptor(ctx1.getEncryptor());
		tas = cli.getLossDetector().getPtoTimeAndSpace(111);
		duration = 111L + (333000000L + 4 * 166500000L) * 1;
		assertEquals(duration, tas.time.longValue());
		assertSame(cliSpace1, tas.space);
	}
	
	@Test
	public void testGetPtoTimeAndSpace() throws Exception {
		assertEquals(333000000L, srv.getEstimator().getSmoothedRtt());
		assertEquals(166500000L, srv.getEstimator().getRttVar());
		assertTrue(srv.isAddressValidatedByPeer());

		srv.getSpace(INITIAL).updateAckElicitingInFlight(1);
		srv.getSpace(INITIAL).setLastAckElicitingTime(223);;
		TimeAndSpace tas = srv.getLossDetector().getPtoTimeAndSpace(111);
		long duration = 223L + (333000000L + 4 * 166500000L) * 1;
		assertEquals(duration, tas.time.longValue());
		assertSame(srvSpace0, tas.space);

		srv.getSpace(INITIAL).updateAckElicitingInFlight(-1);
		srv.getSpace(HANDSHAKE).updateAckElicitingInFlight(1);
		srv.getSpace(HANDSHAKE).setLastAckElicitingTime(224);;
		tas = srv.getLossDetector().getPtoTimeAndSpace(111);
		duration = 224L + (333000000L + 4 * 166500000L) * 1;
		assertEquals(duration, tas.time.longValue());
		assertSame(srvSpace1, tas.space);

		//handshake not confirmed
		assertFalse(srv.isHandshakeConfirmed());
		srv.getSpace(HANDSHAKE).updateAckElicitingInFlight(-1);
		srv.getSpace(APPLICATION_DATA).updateAckElicitingInFlight(1);
		srv.getSpace(APPLICATION_DATA).setLastAckElicitingTime(225);;
		tas = srv.getLossDetector().getPtoTimeAndSpace(111);
		assertNull(tas.time);
		assertSame(srvSpace0, tas.space);

		//handshake confirmed
		srv.setPeerMaxAckDelay(20);
		assertEquals(20, srv.getPeerMaxAckDelay());
		srv.setHandshakeState(HandshakeState.DONE);
		assertTrue(srv.isHandshakeConfirmed());
		tas = srv.getLossDetector().getPtoTimeAndSpace(111);
		duration = 225L + (333000000L + 4 * 166500000L) * 1;
		duration += srv.getPeerMaxAckDelay()*1000000 * 1;
		assertEquals(duration, tas.time.longValue());
		assertSame(srvSpace2, tas.space);
		
		//pto count = 1
		srv.getLossDetector().onLossDetectionTimeout();
		tas = srv.getLossDetector().getPtoTimeAndSpace(111);
		duration = 225L + (333000000L + 4 * 166500000L) * 2;
		duration += srv.getPeerMaxAckDelay()*1000000 * 2;
		assertEquals(duration, tas.time.longValue());
		assertSame(srvSpace2, tas.space);
		
		srv.getSpace(INITIAL).updateAckElicitingInFlight(1);
		srv.getSpace(HANDSHAKE).updateAckElicitingInFlight(1);
		srv.getSpace(APPLICATION_DATA).updateAckElicitingInFlight(-1);
		srv.getSpace(INITIAL).setLastAckElicitingTime(224);;
		srv.getSpace(HANDSHAKE).setLastAckElicitingTime(224);;
		tas = srv.getLossDetector().getPtoTimeAndSpace(111);
		duration = 224L + (333000000L + 4 * 166500000L) * 2;
		assertEquals(duration, tas.time.longValue());
		assertSame(srvSpace0, tas.space);

		srv.getSpace(INITIAL).setLastAckElicitingTime(224);;
		srv.getSpace(HANDSHAKE).setLastAckElicitingTime(225);;
		tas = srv.getLossDetector().getPtoTimeAndSpace(111);
		duration = 224L + (333000000L + 4 * 166500000L) * 2;
		assertEquals(duration, tas.time.longValue());
		assertSame(srvSpace0, tas.space);

		srv.getSpace(INITIAL).setLastAckElicitingTime(226);;
		srv.getSpace(HANDSHAKE).setLastAckElicitingTime(225);;
		tas = srv.getLossDetector().getPtoTimeAndSpace(111);
		duration = 225L + (333000000L + 4 * 166500000L) * 2;
		assertEquals(duration, tas.time.longValue());
		assertSame(srvSpace1, tas.space);	
	}
	
	@Test
	public void testSetLossDetectionTimer() throws Exception {
		stimer = new TestSessionTimer(null);
		cli.getTimer().init(stimer, () -> {});
		srv.getTimer().init(stimer, () -> {});
		
		cli.getLossDetector().setLossDetectionTimer(100, false);
		assertEquals("TimerTask;999;true|", stimer.trace());
		cli.getLossDetector().setLossDetectionTimer(100, false);
		assertEquals("CANCEL|TimerTask;999;true|", stimer.trace());
		
		cli.getSpace(HANDSHAKE).setLossTime(11000000);
		cli.getLossDetector().setLossDetectionTimer(1000000, false);
		assertEquals("CANCEL|TimerTask;10;true|", stimer.trace());
		
		assertTrue(srv.getAntiAmplificator().isBlocked());
		srv.getLossDetector().setLossDetectionTimer(100, false);
		assertEquals("", stimer.trace());
		srv.getLossDetector().setLossDetectionTimer(100, false);
		assertEquals("", stimer.trace());

		srv.getAntiAmplificator().incReceived(100);
		assertFalse(srv.getAntiAmplificator().isBlocked());
		assertFalse(srv.isAckElicitingInFlight());
		assertTrue(srv.isAddressValidatedByPeer());
		srv.getLossDetector().setLossDetectionTimer(100, false);
		assertEquals("", stimer.trace());
		
		srv.getSpace(HANDSHAKE).updateAckElicitingInFlight(1);
		assertTrue(srv.isAckElicitingInFlight());
		srv.getLossDetector().setLossDetectionTimer(100, false);
		assertEquals("TimerTask;998;true|", stimer.trace());
		
		srv.getSpace(HANDSHAKE).updateAckElicitingInFlight(-1);
		srv.getSpace(APPLICATION_DATA).updateAckElicitingInFlight(1);
		srv.getLossDetector().setLossDetectionTimer(100, false);
		assertEquals("CANCEL|", stimer.trace());
	
		srv.getSpace(APPLICATION_DATA).updateAckElicitingInFlight(-1);
		srv.getSpace(HANDSHAKE).setLossTime(11000000);
		srv.getLossDetector().setLossDetectionTimer(11000000, false);
		assertEquals("TimerTask;0;true|", stimer.trace());
		srv.getLossDetector().setLossDetectionTimer(11000000+1, false);
		assertEquals("CANCEL|TimerTask;0;true|", stimer.trace());
		srv.getLossDetector().setLossDetectionTimer(11000000*2, false);
		assertEquals("CANCEL|TimerTask;0;true|", stimer.trace());
		
		srv.getLossDetector().setLossDetectionTimer(11000000*2, true);
		assertEquals("CANCEL|", stimer.trace());
		srv.getLossDetector().setLossDetectionTimer(11000000*2, true);
		assertEquals("", stimer.trace());
		
		srv.setHandshakeState(HandshakeState.DONE);
		srv.getSpace(APPLICATION_DATA).updateAckElicitingInFlight(1);
		srv.getLossDetector().setLossDetectionTimer(11000000+1, false);
		assertEquals("TimerTask;1012;true|", stimer.trace());
		srv.getLossDetector().disable();
		srv.getLossDetector().setLossDetectionTimer(11000000+1, false);
		assertEquals("CANCEL|", stimer.trace());
	}
	
	long m2n(long millis) {
		return millis * 1000 * 1000;
	}
	
	@Test
	public void testDetectAndRemoveLostPacketsLossDelay() throws Exception {
		assertEquals(333000000L, cli.getEstimator().getSmoothedRtt());
		assertEquals(0L, cli.getEstimator().getLatestRtt());
		PacketNumberSpace space = cli.getSpace(HANDSHAKE);
		PingFrame f = new PingFrame();
		
		space.frames().fly(f, 1);
		space.updateAcked(1);
		space.frames().fly(f, 0);
		space.frames().getFlying(0).onSending(m2n(100), 10, true, true);
		assertEquals(0, space.getLossTime());
		assertEquals(0, cli.getLossDetector().detectAndRemoveLostPackets(space, m2n(200)).size());
		long lossTime = 9 * 333000000L / 8 + m2n(100) + 1;
		assertEquals(lossTime, space.getLossTime());
		
		cli.getEstimator().addSample(m2n(100), 0, 0, HANDSHAKE);
		assertEquals(100000000L, cli.getEstimator().getSmoothedRtt());
		assertEquals(100000000L, cli.getEstimator().getLatestRtt());
		space.setLossTime(0);
		assertEquals(0, cli.getLossDetector().detectAndRemoveLostPackets(space, m2n(200)).size());
		lossTime = 9 * 100000000L / 8 + m2n(100) + 1;
		assertEquals(lossTime, space.getLossTime());
		
		cli.getEstimator().addSample(m2n(200), 0, 0, HANDSHAKE);
		assertEquals(112500000L, cli.getEstimator().getSmoothedRtt());
		assertEquals(200000000L, cli.getEstimator().getLatestRtt());
		space.setLossTime(0);
		assertEquals(0, cli.getLossDetector().detectAndRemoveLostPackets(space, m2n(200)).size());
		lossTime = 9 * 200000000L / 8 + m2n(100) + 1;
		assertEquals(lossTime, space.getLossTime());
		
		for (int i=0; i<1000; ++i) {
			cli.getEstimator().addSample(1, 0, 0, HANDSHAKE);
		}
		assertEquals(1L, cli.getEstimator().getSmoothedRtt());
		assertEquals(1L, cli.getEstimator().getLatestRtt());
		space.setLossTime(0);
		assertEquals(0, cli.getLossDetector().detectAndRemoveLostPackets(space, 1).size());
		lossTime = 1000000L + m2n(100) + 1;
		assertEquals(lossTime, space.getLossTime());
	}
	
	@Test
	public void testDetectAndRemoveLostPacketsSkipPackets() throws Exception {
		assertEquals(333000000L, cli.getEstimator().getSmoothedRtt());
		assertEquals(0L, cli.getEstimator().getLatestRtt());
		PacketNumberSpace space = cli.getSpace(HANDSHAKE);
		PingFrame f = new PingFrame();
		
		//not sent yet
		space.frames().fly(f, 1);
		space.updateAcked(1);
		space.frames().fly(f, 0);
		assertEquals(0, space.getLossTime());
		assertEquals(0, cli.getLossDetector().detectAndRemoveLostPackets(space, m2n(200)).size());
		assertEquals(0, space.getLossTime());

		//sent but 
		space.frames().fly(f, 2);
		space.frames().getFlying(2).onSending(m2n(100), 10, true, true);
		assertEquals(0, space.getLossTime());
		assertEquals(0, cli.getLossDetector().detectAndRemoveLostPackets(space, m2n(200)).size());
		assertEquals(0, space.getLossTime());
	}

	@Test
	public void testDetectAndRemoveLostPacketsLossTime() throws Exception {
		assertEquals(333000000L, cli.getEstimator().getSmoothedRtt());
		assertEquals(0L, cli.getEstimator().getLatestRtt());
		PacketNumberSpace space = cli.getSpace(HANDSHAKE);
		PingFrame f = new PingFrame();

		space.frames().fly(f, 2);
		space.updateAcked(2);
		space.frames().fly(f, 1);
		space.frames().fly(f, 0);
		space.frames().getFlying(1).onSending(m2n(100), 10, true, true);
		space.frames().getFlying(0).onSending(m2n(101), 10, true, true);
		assertEquals(0, space.getLossTime());
		assertEquals(0, cli.getLossDetector().detectAndRemoveLostPackets(space, m2n(200)).size());
		long lossTime = 9 * 333000000L / 8 + m2n(100) + 1;
		assertEquals(lossTime, space.getLossTime());

		space.frames().getFlying(1).onSending(m2n(100), 10, true, true);
		space.frames().getFlying(0).onSending(m2n(99), 10, true, true);
		assertEquals(0, cli.getLossDetector().detectAndRemoveLostPackets(space, m2n(200)).size());
		lossTime = 9 * 333000000L / 8 + m2n(99) + 1;
		assertEquals(lossTime, space.getLossTime());
	}
	
	@Test
	public void testDetectAndRemoveLostPackets() throws Exception {
		assertEquals(333000000L, cli.getEstimator().getSmoothedRtt());
		assertEquals(0L, cli.getEstimator().getLatestRtt());
		PacketNumberSpace space = cli.getSpace(HANDSHAKE);
		PingFrame f = new PingFrame();
		
		space.frames().fly(f, 2);
		space.updateAcked(2);
		space.frames().fly(f, 1);
		space.frames().fly(f, 0);
		long lossDelay = 9 * 333000000L / 8;
		space.frames().getFlying(1).onSending(m2n(1000)-lossDelay, 10, true, true);
		space.frames().getFlying(0).onSending(m2n(1000)-lossDelay+1, 10, true, true);
		assertEquals(0, space.getAckElicitingInFlight());
		List<FlyingFrames> lost = cli.getLossDetector().detectAndRemoveLostPackets(space, m2n(1000));
		assertEquals(-1, space.getAckElicitingInFlight());
		assertEquals(1, lost.size());
		assertEquals(1, lost.get(0).getPacketNumber());
		assertEquals(1, space.frames().getFlying().size());
		assertTrue(space.frames().isFlying(0));
		assertEquals(1, space.frames().getLost().size());
		space.frames().getFlying(0).onSending(m2n(1000)-lossDelay-1, 10, false, true);
		assertEquals(-1, space.getAckElicitingInFlight());
		lost = cli.getLossDetector().detectAndRemoveLostPackets(space, m2n(1000));
		assertEquals(-1, space.getAckElicitingInFlight());
		assertEquals(1, lost.size());
		assertEquals(0, lost.get(0).getPacketNumber());
		assertEquals(0, space.frames().getFlying().size());
		assertEquals(2, space.frames().getLost().size());
		
		space.frames().getLost().clear();
		space.frames().fly(f, 10);
		space.updateAcked(10);
		space.frames().fly(f, 9);
		space.frames().fly(f, 8);
		space.frames().fly(f, 7);
		space.frames().fly(f, 6);		
		space.frames().getFlying(9).onSending(m2n(1000)-lossDelay/2, 10, true, true);
		space.frames().getFlying(8).onSending(m2n(1000)-lossDelay/2, 10, true, true);
		space.frames().getFlying(7).onSending(m2n(1000)-lossDelay/2, 10, true, true);
		space.frames().getFlying(6).onSending(m2n(1000)-lossDelay/2, 10, true, true);
		assertEquals(0, space.frames().getLost().size());
		lost = cli.getLossDetector().detectAndRemoveLostPackets(space, m2n(1000));
		assertEquals(2, lost.size());
		assertEquals(2, space.frames().getFlying().size());
		assertEquals(2, space.frames().getLost().size());
		assertTrue(space.frames().isFlying(9));
		assertTrue(space.frames().isFlying(8));
	}
	
	@Test
	public void testonPacketNumberSpaceDiscarded() {
		stimer = new TestSessionTimer(null);
		cli.getTimer().init(stimer, () -> {});
		cli.getLossDetector().onLossDetectionTimeout();
		cli.getCongestion().onPacketSent(3000);
		PacketNumberSpace space = cli.getSpace(HANDSHAKE);
		space.frames().fly(new PingFrame(), 0);
		space.frames().fly(new PingFrame(), 1);
		space.frames().getFlying(0).onSending(1000, 400, true, true);
		space.frames().getFlying(1).onSending(200, 700, true, true);
		space.setLastAckElicitingTime(456);
		space.setLossTime(5664);
		assertEquals("TimerTask;1998;true|", stimer.trace());
		assertEquals(5664, space.getLossTime());
		assertEquals(456, space.getLastAckElicitingTime());

		cli.getLossDetector().onPacketNumberSpaceDiscarded(space, 2000000);
		assertEquals("CANCEL|TimerTask;999;true|", stimer.trace());
		assertEquals(0, space.getLossTime());
		assertEquals(0, space.getLastAckElicitingTime());
		assertEquals(1200*10 - 1900, cli.getCongestion().available());
		assertFalse(space.frames().hasFlying());
		TimeAndSpace tas = cli.getLossDetector().getPtoTimeAndSpace(111);
		long duration = 111L + (333000000L + 4 * 166500000L) * 1;
		assertEquals(duration, tas.time.longValue());
	}
	
	@Test
	public void testOnLossDetectionTimeout() throws Exception {
		cli = new QuicState(true, new TestConfig(), new TestTime(10000000));
		stimer = new TestSessionTimer(null);
		cli.getTimer().init(stimer, () -> {});
		cli.getLossDetector().onLossDetectionTimeout();
		assertEquals("TimerTask;1998;true|", stimer.trace());
		IFrame frame = cli.getSpace(INITIAL).frames().peek();
		assertNotNull(frame);
		assertSame(FrameType.PING, frame.getType());
		assertNull(cli.getSpace(HANDSHAKE).frames().peek());
		cli.getSpace(INITIAL).frames().fly(frame, 0);
		assertNull(cli.getSpace(INITIAL).frames().peek());
		
		EncryptionContext ctx = cli.getContext(HANDSHAKE);
		new CryptoEngineStateListener(cli).onInit(bytes(16), bytes(8));
		ctx.setEncryptor(cli.getContext(INITIAL).getEncryptor());
		cli.getLossDetector().onLossDetectionTimeout();
		assertEquals("TimerTask;3996;true|", stimer.trace());
		assertNull(cli.getSpace(INITIAL).frames().peek());
		frame = cli.getSpace(HANDSHAKE).frames().peek();
		assertNotNull(frame);
		assertSame(FrameType.PING, frame.getType());
		
		PacketNumberSpace space = cli.getSpace(INITIAL);
		space.updateAckElicitingInFlight(1);
		cli.getLossDetector().onLossDetectionTimeout();
		assertEquals("TimerTask;7982;true|", stimer.trace());
		frame = cli.getSpace(INITIAL).frames().peek();
		assertNotNull(frame);
		assertNotNull(cli.getSpace(HANDSHAKE).frames().peek());
		assertSame(FrameType.PING, frame.getType());

		space.setLossTime(500);
		space.frames().getFlying(0).getFrames().add(HandshakeDoneFrame.INSTANCE);
		space.frames().getFlying(0).onSending(-1000000000L, 100, true, true);
		space.frames().fly(new PingFrame(), 1);
		space.updateAcked(1);
		cli.getCongestion().onPacketSent(2000);
		assertEquals(0, space.frames().getLost().size());
		QuicProcessor processor = new QuicProcessor(cli, null);
		cli.getLossDetector().setProcessor(processor);
		cli.getLossDetector().onLossDetectionTimeout();
		assertEquals(2, space.frames().getLost().size());
		frame = space.frames().peek();
		space.frames().fly(frame, 3);
		assertSame(FrameType.HANDSHAKE_DONE, space.frames().peek().getType());
		assertEquals(1200*5 - 1900, cli.getCongestion().available());
	}
}
