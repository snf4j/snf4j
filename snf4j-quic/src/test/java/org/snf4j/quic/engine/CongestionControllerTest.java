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
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.snf4j.quic.frame.EcnAckFrame;
import org.snf4j.quic.frame.PingFrame;
import org.snf4j.quic.metric.TestCCMetric;

public class CongestionControllerTest {

	TestConfig config;
	
	QuicState state;
	
	int maxUdpSize;
	
	List<FlyingFrames> flying;
	
	@Before
	public void before() {
		maxUdpSize = 1200;
		config = new TestConfig();
		state = new QuicState(true, config, new TestTime(1000)) {
			@Override
			public int getMaxUdpPayloadSize() {
				return maxUdpSize;
			}
		};
		flying = new LinkedList<>();
	}
	
	void fly(long pn, long sentTime, int bytes, boolean ackEliciting, boolean inFlight) {
		FlyingFrames ff = new FlyingFrames(pn);
		ff.getFrames().add(new PingFrame());
		ff.onSending(sentTime, bytes, ackEliciting, inFlight);
		flying.add(ff);
	}
	
	@Test
	public void testName() {
		assertEquals("congestion controller", new CongestionController(state).name());
	}
	
	@Test
	public void testInitials() {
		CongestionController cc = new CongestionController(state);
		assertEquals(1200*10, cc.available());
		
		maxUdpSize = 1472;
		cc = new CongestionController(state);
		assertEquals(1472*10, cc.available());
		
		maxUdpSize = 1473;
		cc = new CongestionController(state);
		assertEquals(1472*10, cc.available());

		maxUdpSize = 14720/2;
		cc = new CongestionController(state);
		assertEquals(1472*10, cc.available());
		
		maxUdpSize = 14722/2;
		cc = new CongestionController(state);
		assertEquals(14722, cc.available());
	}
	
	@Test
	public void testAvailable() {
		CongestionController cc = new CongestionController(state);
		assertEquals(1200*10, cc.available());
		cc.onPacketSent(1);
		assertEquals(1200*10-1, cc.available());
		cc.onPacketSent(1);
		assertEquals(1200*10-2, cc.available());
	}

	@Test
	public void testIsBlocked() {
		CongestionController cc = new CongestionController(state);
		assertFalse(cc.isBlocked());
		cc.onPacketSent(12000);
		assertTrue(cc.isBlocked());
		cc.onPacketSent(1);
		assertTrue(cc.isBlocked());

		cc = new CongestionController(state);
		cc.onPacketSent(10000);
		assertEquals(2000, cc.available());
		cc.lock(2000);
		assertFalse(cc.isBlocked());
		assertTrue(cc.needUnlock());
		cc.lock(2001);
		assertTrue(cc.isBlocked());
	}

	@Test
	public void testIsAppOrFlowControlLimited() {
		CongestionController cc = new CongestionController(state);
		assertTrue(cc.isAppOrFlowControlLimited());
		assertEquals(10*1200, cc.available());
		cc.onPacketSent(10*1200-1);
		assertTrue(cc.isAppOrFlowControlLimited());
		cc.onPacketSent(1);
		assertFalse(cc.isAppOrFlowControlLimited());
	}
	
	@Test
	public void testOnCongestionEvent() {
		state = new QuicState(true, config, new TestTime(1000,2000,3000));
		CongestionController cc = new CongestionController(state);
		
		assertFalse(cc.isInCongestionRecovery(0));
		assertFalse(cc.isInCongestionRecovery(Long.MAX_VALUE));
		assertFalse(cc.isInCongestionRecovery(Long.MIN_VALUE));
		
		cc.onCongestionEvent(500);
		assertTrue(cc.isInCongestionRecovery(999));
		assertTrue(cc.isInCongestionRecovery(1000));
		assertFalse(cc.isInCongestionRecovery(1001));
		assertEquals(5*1200, cc.available());
		cc.onCongestionEvent(1000);
		assertEquals(5*1200, cc.available());

		cc.onCongestionEvent(1001);
		assertEquals(2*1200 + 600, cc.available());
		cc.onCongestionEvent(2000);
		assertEquals(2*1200 + 600, cc.available());
		
		cc.onCongestionEvent(2001);
		assertEquals(2*1200, cc.available());
		cc.onCongestionEvent(3000);
		assertEquals(2*1200, cc.available());

		assertFalse(cc.isInCongestionRecovery(3001));
		cc.onCongestionEvent(3001);
		assertEquals(2*1200, cc.available());
	}
		
	@Test
	public void testIsInPersistentCongestion() {
		CongestionController cc = new CongestionController(state);
		assertFalse(cc.isInPersistentCongestion(flying));
		
		state.setPeerMaxAckDelay(24);
		fly(0, 1000, 100, true, true);
		assertFalse(cc.isInPersistentCongestion(flying));
		long duration = (333000000L + 666000000L + 24000000L) * 3;
		flying.clear();
		fly(1, duration + 1000, 100, true, true);
		assertFalse(cc.isInPersistentCongestion(flying));
		flying.clear();
		fly(2, duration + 1000 + 1, 100, true, true);
		assertTrue(cc.isInPersistentCongestion(flying));
		flying.clear();
		assertFalse(cc.isInPersistentCongestion(flying));
		
		state.getEstimator().addSample(10, 0, 0, EncryptionLevel.INITIAL);
		assertEquals(10, state.getEstimator().getSmoothedRtt());
		assertEquals(5, state.getEstimator().getRttVar());
		duration = (10L + 1000000L + 24000000L) * 3;
		fly(0, 1000, 100, true, true);
		assertFalse(cc.isInPersistentCongestion(flying));
		flying.clear();
		fly(0, duration + 1000, 100, true, true);
		assertFalse(cc.isInPersistentCongestion(flying));
		flying.clear();
		fly(0, duration + 1000 + 1, 100, true, true);
		assertTrue(cc.isInPersistentCongestion(flying));
	}
	
	@Test
	public void testRemoveFromBytesInFlight() {
		CongestionController cc = new CongestionController(state);
		cc.onPacketSent(1000);
		assertEquals(1200*10 - 1000, cc.available());
		
		fly(0, 10, 500, false, false);
		fly(0, 10, 400, false, true);
		cc.removeFromBytesInFlight(flying);
		assertEquals(1200*10 - 600, cc.available());
	}
	
	@Test
	public void testProcessEcn() {
		state = new QuicState(true, config, new TestTime(1001,2001,3001));
		CongestionController cc = new CongestionController(state);
		PacketNumberSpace space = state.getSpace(EncryptionLevel.HANDSHAKE);
		assertEquals(0, space.getEcnCeCount());
		fly(0, 500, 100, true, true);
		EcnAckFrame ack = new EcnAckFrame(0, 1000, 0, 0, 600);		
		cc.processEcn(ack, space, flying.get(0));
		assertEquals(1200*5, cc.available());
		assertTrue(cc.isInCongestionRecovery(1001));
		assertFalse(cc.isInCongestionRecovery(1002));
		assertEquals(600, space.getEcnCeCount());
		cc.processEcn(ack, space, flying.get(0));
		assertEquals(1200*5, cc.available());
		ack = new EcnAckFrame(0, 1000, 0, 0, 500);		
		cc.processEcn(ack, space, flying.get(0));
		assertEquals(1200*5, cc.available());
		assertEquals(600, space.getEcnCeCount());
		
		ack = new EcnAckFrame(0, 1000, 0, 0, 601);		
		flying.clear();
		fly(0, 1000, 100, true, true);
		cc.processEcn(ack, space, flying.get(0));
		assertEquals(1200*5, cc.available());
		assertEquals(601, space.getEcnCeCount());
		
		ack = new EcnAckFrame(0, 1000, 0, 0, 602);		
		flying.clear();
		fly(0, 1001, 100, true, true);
		cc.processEcn(ack, space, flying.get(0));
		assertEquals(1200*5, cc.available());
		assertEquals(602, space.getEcnCeCount());
		
		ack = new EcnAckFrame(0, 1000, 0, 0, 603);		
		flying.clear();
		fly(0, 1002, 100, true, true);
		cc.processEcn(ack, space, flying.get(0));
		assertEquals(1200*2 + 600, cc.available());
		assertEquals(603, space.getEcnCeCount());
	}
	
	@Test
	public void testOnPacketInFlightAcked() {
		state = new QuicState(true, config, new TestTime(1000,2000,3000));
		CongestionController cc = new CongestionController(state) {
			@Override
			public boolean isAppOrFlowControlLimited() {
				return false;
			}
		};
		fly(0, 1000, 500, true, true);		
		cc.onPacketSent(2000);
		assertEquals(1200*10 - 2000, cc.available());
		cc.onPacketInFlightAcked(flying.get(0));
		assertEquals(1200*10 + 500 - 1500, cc.available());
		
		cc.onCongestionEvent(500);
		assertEquals(1200*5 + 250 - 1500, cc.available());
		cc.onCongestionEvent(1500);
		assertEquals(3125 - 1500, cc.available());
		cc.onCongestionEvent(2500);
		assertEquals(2400 - 1500, cc.available());
		flying.clear();
		fly(0, 3500, 500, true, true);		
		cc.onPacketInFlightAcked(flying.get(0));
		int cwnd = 2400 + 1200 * 500 / 2400;
		assertEquals(cwnd - 1000, cc.available());
		
		state = new QuicState(true, config, new TestTime(1000,2000,3000));
		cc = new CongestionController(state) {
			@Override
			public boolean isAppOrFlowControlLimited() {
				return false;
			}
		};
		flying.clear();
		fly(0, 999, 500, true, true);		
		cc.onPacketSent(2000);
		assertEquals(1200*10 - 2000, cc.available());
		cc.onCongestionEvent(500);
		cc.onPacketInFlightAcked(flying.get(0));
		assertEquals(1200*5 - 1500, cc.available());
		flying.clear();
		fly(0, 1000, 100, true, true);		
		cc.onPacketInFlightAcked(flying.get(0));
		assertEquals(1200*5 - 1400, cc.available());
		flying.clear();
		fly(0, 1001, 100, true, true);		
		cc.onPacketInFlightAcked(flying.get(0));
		cwnd = 6000 + 1200 * 100 / 6000;
		assertEquals(cwnd - 1300, cc.available());

		state = new QuicState(true, config, new TestTime(1000,2000,3000));
		cc = new CongestionController(state) {
			@Override
			public boolean isAppOrFlowControlLimited() {
				return true;
			}
		};
		flying.clear();
		fly(0, 1000, 500, true, true);		
		cc.onPacketSent(2000);
		cc.onPacketInFlightAcked(flying.get(0));
		assertEquals(1200*10 - 1500, cc.available());
	}
	
	@Test
	public void testOnPacketAcked() throws Exception {
		state = new QuicState(true, config, new TestTime(500));
		CongestionController cc = new CongestionController(state) {
			@Override
			public boolean isAppOrFlowControlLimited() {
				return false;
			}
		};
		cc.onPacketSent(2000);
		assertEquals(1200*10 - 2000, cc.available());
		
		Field f = CongestionController.class.getDeclaredField("persistent");
		f.setAccessible(true);
		PersistentCongestion p = (PersistentCongestion) f.get(cc);
		fly(0, 1000, 500, true, true);		
		fly(1, 1100, 400, true, false);	
		state.getEstimator().addSample(500, 0, 0, EncryptionLevel.HANDSHAKE);
		cc.onPacketsLost(flying);
		assertTrue(p.isDetectable());
		cc.onPacketAcked(flying);
		assertFalse(p.isDetectable());
		assertEquals(1200*5+100 - 1000, cc.available());
		p.lost(5000);
		assertFalse(p.isDetectable());
		p.lost(5001);
		assertTrue(p.isDetectable());
	}
	
	@Test
	public void testOnPacketsLostLastLossTime() {
		StringBuilder sb = new StringBuilder();
		CongestionController cc = new CongestionController(state) {
			@Override
			void onCongestionEvent(long sentTime) {
				sb.append(sentTime).append('|');
			}
		};
		cc.onPacketSent(2000);
		fly(0, 1000, 500, true, false);		
		fly(1, 2000, 300, true, false);		
		assertEquals(1200*10 - 2000, cc.available());
		cc.onPacketsLost(flying);
		assertEquals(1200*10 - 2000, cc.available());
		assertEquals("", sb.toString());
		
		flying.clear();
		fly(0, 1000, 500, true, false);		
		fly(1, 2000, 300, true, true);		
		cc.onPacketsLost(flying);
		assertEquals(1200*10 - 1700, cc.available());
		assertEquals("2000|", sb.toString());

		flying.clear();
		fly(0, 1000, 500, true, true);		
		fly(1, 1001, 300, true, true);		
		cc.onPacketsLost(flying);
		assertEquals(1200*10 - 900, cc.available());
		assertEquals("2000|1001|", sb.toString());
	
		flying.clear();
		fly(0, 801, 500, true, true);		
		fly(1, 800, 300, true, true);		
		cc.onPacketsLost(flying);
		assertEquals(1200*10 - 100, cc.available());
		assertEquals("2000|1001|801|", sb.toString());

		flying.clear();
		fly(0, 700, 10, true, true);		
		fly(1, 700, 20, true, true);		
		cc.onPacketsLost(flying);
		assertEquals(1200*10 - 70, cc.available());
		assertEquals("2000|1001|801|700|", sb.toString());
	}
	
	@Test
	public void testOnPacketsLost() throws Exception {
		AtomicBoolean block = new AtomicBoolean(true);
		state = new QuicState(true, config, new TestTime(1200,2000,3000));
		CongestionController cc = new CongestionController(state) {
			@Override
			void onCongestionEvent(long sentTime) {
				if (!block.get()) {
					super.onCongestionEvent(sentTime);
				}
			}
		};
		state.getEstimator().addSample(1000, 0, 0, EncryptionLevel.HANDSHAKE);
		Field f = CongestionController.class.getDeclaredField("persistent");
		f.setAccessible(true);
		PersistentCongestion p = (PersistentCongestion) f.get(cc);

		state.setPeerMaxAckDelay(24);
		cc.onPacketSent(2000);
		fly(0, 1199, 100, true, true);		
		fly(0, 1200, 100, true, true);		
		cc.onPacketsLost(flying);
		assertFalse(p.isDetectable());
		p.lost(1);
		assertFalse(p.isDetectable());
		p.lost(2);
		assertTrue(p.isDetectable());
		p.acked(10000);;
		assertFalse(p.isDetectable());
		
		flying.clear();
		fly(0, 1201, 100, false, true);		
		fly(0, 1202, 100, false, true);		
		cc.onPacketsLost(flying);
		assertFalse(p.isDetectable());
		p.lost(1);
		assertFalse(p.isDetectable());
		p.lost(2);
		assertTrue(p.isDetectable());
		p.acked(10000);;
		assertFalse(p.isDetectable());

		flying.clear();
		fly(0, 1201, 100, true, true);		
		fly(0, 1202, 100, false, true);		
		cc.onPacketsLost(flying);
		assertFalse(p.isDetectable());
		p.lost(1);
		assertTrue(p.isDetectable());
		p.acked(10000);;
		assertFalse(p.isDetectable());
	
		flying.clear();
		fly(0, 1201, 100, true, true);		
		fly(0, 1202, 100, true, true);		
		cc.onPacketsLost(flying);
		assertTrue(p.isDetectable());
		p.acked(10000);;
		assertFalse(p.isDetectable());
		assertEquals(1200*10 - 1200, cc.available());
		p.acked(10000);;
		assertFalse(p.isDetectable());
		
		flying.clear();
		long duration = (1000 + 1000000 + 25000000) * 3;
		fly(0, 1201, 100, true, true);		
		fly(0, 1201 + duration + 1, 100, true, true);	
		block.set(false);
		cc.onCongestionEvent(100);
		assertTrue(cc.isInCongestionRecovery(0));
		cc.onPacketsLost(flying);
		assertFalse(p.isDetectable());
		assertEquals(1200*2 - 1000, cc.available());
		assertFalse(cc.isInCongestionRecovery(0));
	}
	
	@Test
	public void testMetric() {
		TestCCMetric m = new TestCCMetric();
		CongestionController cc = new CongestionController(state, m);
		state.getEstimator().addSample(2000, 1000, 0, EncryptionLevel.HANDSHAKE);
		assertEquals("", m.trace());
		
		//onPacketSent
		cc.onPacketSent(112);
		assertEquals("W12000|S112|", m.trace());
		cc.onPacketSent(88);
		assertEquals("S200|", m.trace());
		
		//onPacketInFlightAcked
		fly(0, 1201, 50, true, true);
		cc.onPacketInFlightAcked(flying.get(0));
		assertEquals("A150|", m.trace());
		cc.onPacketSent(12000);
		assertEquals("S12150|", m.trace());
		cc.onPacketInFlightAcked(flying.get(0));
		assertEquals("A12100|W12050|", m.trace());

		//onCongestionEvent
		//onPacketsLost
		cc.onPacketsLost(flying);
		assertEquals("L12050|T6025|W6025|", m.trace());
		cc.onPacketsLost(flying);
		assertEquals("L12000|T3012|W3012|", m.trace());
		cc.onPacketsLost(flying);
		assertEquals("L11950|T1506|W2400|", m.trace());
		
		//removeFromBytesInFlight
		cc.removeFromBytesInFlight(flying);
		assertEquals("D11900|", m.trace());
		
		cc = new CongestionController(state, m);
		cc.onPacketSent(1000);
		assertEquals("W12000|S1000|", m.trace());
		flying.clear();
		fly(0, 10000, 50, true, true);
		fly(0, 2000000000, 50, true, true);
		cc.onPacketsLost(flying);
		assertEquals("L950|L900|T6000|W6000|P|W2400|", m.trace());
	}
	
}
