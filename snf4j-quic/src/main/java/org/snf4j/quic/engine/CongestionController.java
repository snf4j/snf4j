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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.snf4j.quic.frame.EcnAckFrame;
import org.snf4j.quic.metric.CongestionControllerMetric;
import org.snf4j.quic.metric.ICongestionControllerMetric;

/**
 * The default congestion controller as defined in RFC 9002.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class CongestionController extends AbstractDataBlockable {
	
	private final static int K_LOSS_REDUCTION_FACTOR_NUM = 1;

	private final static int K_LOSS_REDUCTION_FACTOR_DEN = 2;
	
	private final static int K_PERSISTENT_CONGESTION_THRESHOLD = 3;
	
	private final static long K_GLANURALITY = 1000000;

	private final QuicState state;
	
	private final PersistentCongestion persistent = new PersistentCongestion();
	
	private final ICongestionControllerMetric metric;
	
	private long window;
	
	private int bytesInFlight;
	
	private long recoveryStartTime;
	
	private long ssthresh = Long.MAX_VALUE;
	
	private boolean sent;
	
	/**
	 * Constructs a congestion controller associated with the give QUIC state and
	 * congestion controller metric.
	 * 
	 * @param state  the QUIC state
	 * @param metric the congestion controller metric
	 */
	public CongestionController(QuicState state, ICongestionControllerMetric metric) {
		this.state = state;		
		int minWindow = 2 * state.getMaxUdpPayloadSize();
		window = Math.min(minWindow * 5, Math.max(14720, minWindow));	
		this.metric = metric;
	}
	
	/**
	 * Constructs a congestion controller associated with the give QUIC state.
	 * 
	 * @param state  the QUIC state
	 */
	public CongestionController(QuicState state) {
		this(state, CongestionControllerMetric.INSTANCE);
	}
	
	/**
	 * Returns the maximum number of bytes that can be sent without exceeding the
	 * current congestion window.
	 * 
	 * @return the maximum number of bytes that can be sent without exceeding the
	 *         current congestion window
	 */
	public int available() {
		return (int) (window - bytesInFlight);
	}
	
	/**
	 * Called when a packet is sent and it contains non-ACK frames (increases
	 * bytes in flight).
	 * 
	 * @param sentBytes the length of packet being sent
	 */
	public void onPacketSent(int sentBytes) {
		bytesInFlight += sentBytes;
		if (!sent) {
			sent = true;
			metric.onWnindowChange(state, window);
		}
		metric.afterSending(state, bytesInFlight);
	}
	
	boolean isInCongestionRecovery(long sentTime) {
		return recoveryStartTime != 0 && sentTime - recoveryStartTime <= 0;
	}
	
	boolean isAppOrFlowControlLimited() {
		return window > bytesInFlight;
	}
	
	/**
	 * Called when packets carrying frames marked as put in flight are newly
	 * acknowledged.
	 * 
	 * @param fframes the packets carrying frames marked as put in flight
	 */
	public void onPacketAcked(List<FlyingFrames> fframes) {
		for (FlyingFrames ff: fframes) {
			if (ff.isInFlight()) {
				onPacketInFlightAcked(ff);
			}
			persistent.acked(ff.getSentTime());
		}
	}

	void onPacketInFlightAcked(FlyingFrames fframes) {
		bytesInFlight -= fframes.getSentBytes();
		metric.afterAcking(state, bytesInFlight);
		if (isAppOrFlowControlLimited()) {
			return;
		}
		if (isInCongestionRecovery(fframes.getSentTime())) {
			return;
		}
		if (window < ssthresh) {
			window += fframes.getSentBytes();
		}
		else {
			window += state.getMaxUdpPayloadSize()
				* fframes.getSentBytes()
				/ window;
		}
		metric.onWnindowChange(state, window);
	}
	
	void sendOnePacket() {
		//TODO: maybe send a packet to speed up loss recovery
	}
	
	void onCongestionEvent(long sentTime) {
		if (!isInCongestionRecovery(sentTime)) {
			recoveryStartTime = state.getTime().nanoTime();
			ssthresh = K_LOSS_REDUCTION_FACTOR_NUM * window / K_LOSS_REDUCTION_FACTOR_DEN;
			window = Math.max(ssthresh, 2 * state.getMaxUdpPayloadSize());
			metric.onSlowStartThresholdChange(state, ssthresh);
			metric.onWnindowChange(state, window);
			sendOnePacket();
		}
	}
	
	/**
	 * Called when packets carrying frames marked as put in flight are detected as
	 * lost.
	 * 
	 * @param fframes the packets carrying frames marked as put in flight
	 */
	public void onPacketsLost(List<FlyingFrames> fframes) {	
		long lastLossTime = 0;
		
		for (FlyingFrames ff: fframes) {
			if (ff.isInFlight()) {
				bytesInFlight -= ff.getSentBytes();
				metric.afterLosing(state, bytesInFlight);
				if (lastLossTime == 0 || ff.getSentTime() - lastLossTime > 0) {
					lastLossTime = ff.getSentTime();
				}
			}
		}
		if (lastLossTime != 0) {
			onCongestionEvent(lastLossTime);
		}
		
		if (state.getEstimator().isSampled()) {
			List<FlyingFrames> lost = new ArrayList<>(fframes.size());
			long firstRttTime = state.getEstimator().getFirstSampleTime();
			
			for (FlyingFrames ff: fframes) {
				if (ff.getSentTime() - firstRttTime > 0 && ff.isAckEliciting()) {
					lost.add(ff);
				}
			}
			if (isInPersistentCongestion(lost)) {
				window = 2 * state.getMaxUdpPayloadSize();
				recoveryStartTime = 0;
				metric.onPersistentCongestion(state);
				metric.onWnindowChange(state, window);
			}
		}
	}
	
	boolean isInPersistentCongestion(List<FlyingFrames> lost) {
		for (FlyingFrames ff: lost) {
			persistent.lost(ff.getSentTime());
		}
		
		if (persistent.isDetectable()) {
			RttEstimator rtt = state.getEstimator();
			long duration = (rtt.getSmoothedRtt() 
					+ Math.max(4*rtt.getRttVar(), K_GLANURALITY)
					+ state.getPeerMaxAckDelay() * 1000000L) 
					* K_PERSISTENT_CONGESTION_THRESHOLD;

			return persistent.detect(duration);
		}
		return false;
	}
	
	/**
	 * Called when an ACK frame with an ECN section is received.
	 * 
	 * @param ack          the ACK frame
	 * @param space        the packet number space
	 * @param largestAcked the frames carried by packet with the largest packet
	 *                     number being acknowledged by the ACK frame
	 */
	public void processEcn(EcnAckFrame ack, PacketNumberSpace space, FlyingFrames largestAcked) {
		if (ack.getEcnCeCount() > space.getEcnCeCount()) {		
			space.setEcnCeCount(ack.getEcnCeCount());
			onCongestionEvent(largestAcked.getSentTime());
		}
	}
	
	/**
	 * Called when Initial or Handshake keys are discarded.
	 * 
	 * @param discarded the frames being still in flight when the keys were
	 *                  discarded
	 */
	public void removeFromBytesInFlight(Collection<FlyingFrames> discarded) {
		for (FlyingFrames ff: discarded) {
			if (ff.isInFlight()) {
				bytesInFlight -= ff.getSentBytes();
				metric.afterDiscarding(state, bytesInFlight);
			}
		}
	}

	@Override
	public boolean isBlocked() {
		int available = available();
		
		return available == 0 || available < locked;
	}

	@Override
	public String name() {
		return "congestion controller";
	}
}
