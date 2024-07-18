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

import java.util.LinkedList;
import java.util.List;

import org.snf4j.core.timer.ITimerTask;
import org.snf4j.quic.frame.PingFrame;

/**
 * The default packet loss detector as defined in RFC 9002.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class LossDetector {

	private final static int SPACES_LEN = 3;

	private final static int INITIAL = 0;

	private final static int HANDSHAKE = 1;

	private final static int APPLICATION_DATA = 2;

	private final static long K_TIME_THRESHOLD_NUM = 9;

	private final static long K_TIME_THRESHOLD_DEN = 8;

	private final static int K_PACKET_THRESHOLD = 3;

	private final static long K_GLANURALITY = 1000000;

	private final static ITimerTask CANCELED = new ITimerTask() {

		@Override
		public void cancelTask() {
		}
	};

	private final QuicState state;

	private final PacketNumberSpace[] spaces = new PacketNumberSpace[SPACES_LEN];

	private int ptoCount;

	private ITimerTask lossDetectionTimer = CANCELED;

	private Runnable onLossDetectionTimeout = new Runnable() {

		@Override
		public void run() {
			TimeAndSpace timeSpace = getLossTimeAndSpace();
			long currentTime = state.getTime().nanoTime();

			if (timeSpace.time != 0) {
				List<FlyingFrames> lost = detectAndRemoveLostPackets(timeSpace.space, currentTime);
				
				state.getCongestion().onPacketsLost(lost);
			} else {
				if (!state.isAckElicitingInFlight()) {
					if (hasHandshakeKeys()) {
						spaces[1].frames().add(PingFrame.INSTANCE);
					} else {
						spaces[0].frames().add(PingFrame.INSTANCE);
					}
				} else {
					PacketNumberSpace space = getPtoTimeAndSpace(currentTime).space;

					space.frames().add(PingFrame.INSTANCE);
				}
				++ptoCount;
			}
			lossDetectionTimer = CANCELED;
			setLossDetectionTimer(currentTime, false);
		}
	};

	/**
	 * Constructs a packet loss detector associated with the give QUIC state.
	 * 
	 * @param state the QUIC state
	 */
	public LossDetector(QuicState state) {
		this.state = state;
		spaces[INITIAL] = state.getSpace(EncryptionLevel.INITIAL);
		spaces[HANDSHAKE] = state.getSpace(EncryptionLevel.HANDSHAKE);
		spaces[APPLICATION_DATA] = state.getSpace(EncryptionLevel.APPLICATION_DATA);
	}

	boolean hasHandshakeKeys() {
		return state.getContext(EncryptionLevel.HANDSHAKE).getEncryptor() != null;
	}

	/**
	 * Returns the earliest loss time in nanoseconds and packet number space.
	 * <p>
	 * NOTE: The time field in the returned object is always different from
	 * {@code null} and 0 indicates that loss time is not set in all packet number
	 * spaces.
	 * 
	 * @return the earliest loss time and packet number space
	 */
	TimeAndSpace getLossTimeAndSpace() {
		PacketNumberSpace space = spaces[INITIAL];
		long time = space.getLossTime();

		for (int i = HANDSHAKE; i < SPACES_LEN; ++i) {
			PacketNumberSpace innerSpace = spaces[i];
			long lossTime = innerSpace.getLossTime();

			if (lossTime != 0) {
				if (time == 0 || lossTime - time < 0) {
					time = lossTime;
					space = innerSpace;
				}
			}
		}
		return new TimeAndSpace(time, space);
	}

	/**
	 * Returns the probe timeout (PTO) in nanoseconds.
	 * <p>
	 * NOTE: The time field in the returned object can be {@code null} what
	 * indicates an infinite probe timeout otherwise it points the time the timeout
	 * will expire. To calculate the delay for a timer use following formula:
	 * {@code delay = time - currentTime}.
	 * 
	 * @param currentTime the current time in nanoseconds
	 * @return the probe timeout (PTO)
	 */
	TimeAndSpace getPtoTimeAndSpace(long currentTime) {
		RttEstimator rtt = state.getEstimator();
		long duration = (rtt.getSmoothedRtt() + Math.max(4 * rtt.getRttVar(), K_GLANURALITY)) * (1 << ptoCount);

		// If server might be blocked by the anti-amplification limit
		// client need to arm anti-deadlock PTO that starts from the current 
		// time.
		if (!state.isAckElicitingInFlight()) {
			if (hasHandshakeKeys()) {
				return new TimeAndSpace(currentTime + duration, spaces[HANDSHAKE]);
			}
			return new TimeAndSpace(currentTime + duration, spaces[INITIAL]);
		}

		PacketNumberSpace space = null;
		long time = 0;

		for (int i = 0; i < SPACES_LEN; ++i) {
			PacketNumberSpace innerSpace = spaces[i];

			if (innerSpace.getAckElicitingInFlight() == 0) {
				continue;
			}

			if (i == APPLICATION_DATA) {
				if (!state.isHandshakeConfirmed()) {
					break;
				}
				duration += state.getConfig().getMaxAckDelay() * 1000000 * (1 << ptoCount);
			}

			long t = innerSpace.getLastAckElicitingTime() + duration;

			if (space == null || t - time < 0) {
				space = innerSpace;
				time = t;
			}
		}
		if (space == null) {
			return new TimeAndSpace(spaces[INITIAL]);
		}
		return new TimeAndSpace(time, space);
	}

	void onLossDetectionTimeout() {
		onLossDetectionTimeout.run();
	}

	/**
	 * Sets the loss detection timer. 
	 * <p>
	 * NOTE: The timer set in the past fires immediately.
	 * 
	 * @param currentTime   the current time in nanoseconds
	 * @param callIfExpired determines whether the loss detection task should be
	 *                      scheduled or called in current thread when the timer is
	 *                      set in the past.
	 */
	public void setLossDetectionTimer(long currentTime, boolean callIfExpired) {
		TimeAndSpace timeSpace = getLossTimeAndSpace();
		boolean schedule;

		if (timeSpace.time != 0) {
			schedule = true;
		} else if (state.getAntiAmplificator().isBlocked()) {
			schedule = false;
		} else if (!state.isAckElicitingInFlight() && state.isAddressValidatedByPeer()) {
			schedule = false;
		} else {
			timeSpace = getPtoTimeAndSpace(currentTime);
			schedule = timeSpace.time != null;
		}
		
		lossDetectionTimer.cancelTask();
		if (schedule) {
			long delay = timeSpace.time - currentTime;

			if (delay < 0) {
				if (callIfExpired) {
					onLossDetectionTimeout.run();
					return;
				}
				delay = 0;
			}
			lossDetectionTimer = state.getTimer().scheduleTask(onLossDetectionTimeout, delay);
		}
	}

	/**
	 * Detects lost packet in the given packet number space. The frames in detected
	 * packets are moved from frames marked as put in a flight to the lost frames.
	 * 
	 * @param space       the packet number space
	 * @param currentTime the current time in nanoseconds
	 * @return a list frames that were detected as lost
	 */
	public List<FlyingFrames> detectAndRemoveLostPackets(PacketNumberSpace space, long currentTime) {
		List<FlyingFrames> lost = new LinkedList<>();
		long lossDelay = K_TIME_THRESHOLD_NUM
				* Math.max(state.getEstimator().getLatestRtt(), state.getEstimator().getSmoothedRtt())
				/ K_TIME_THRESHOLD_DEN;
		long largestAcked = space.getLargestAcked();
		
		if (lossDelay < K_GLANURALITY) {
			lossDelay = K_GLANURALITY;
		}
		space.setLossTime(0);
		for (FlyingFrames fframes : space.frames().getFlying()) {
			long pn = fframes.getPacketNumber();

			if (pn > largestAcked || fframes.getSentBytes() == 0) {
				continue;
			}

			if (currentTime - fframes.getSentTime() >= lossDelay || largestAcked >= pn + K_PACKET_THRESHOLD) {
				lost.add(fframes);
			} else {
				long lossTime = (fframes.getSentTime() + lossDelay) | 1L;

				if (space.getLossTime() == 0) {
					space.setLossTime(lossTime);
				}
				else {
					space.setLossTime(Math.min(space.getLossTime(), lossTime));
				}
			}
		}

		for (FlyingFrames fframes : lost) {
			space.frames().lost(fframes.getPacketNumber());
			if (fframes.isAckEliciting()) {
				space.updateAckElicitingInFlight(-1);
			}
		}
		
		return lost;
	}

	/**
	 * Resets the number of times a PTO has been sent without receiving an
	 * acknowledgment.
	 */
	public void resetPtoCount() {
		ptoCount = 0;
	}

	/**
	 * Called when Initial or Handshake keys are discarded.
	 * 
	 * @param space       the packet number space
	 * @param currentTime the current time in nanoseconds
	 */
	public void onPacketNumberSpaceDiscarded(PacketNumberSpace space, long currentTime) {
		state.getCongestion().removeFromBytesInFlight(space.frames().getFlying());
		space.frames().clearFlying();
		space.setLastAckElicitingTime(0);
		space.setLossTime(0);
		ptoCount = 0;
		setLossDetectionTimer(currentTime, false);
	}
	
	static class TimeAndSpace {

		final Long time;

		final PacketNumberSpace space;

		TimeAndSpace(long time, PacketNumberSpace space) {
			this.time = time;
			this.space = space;
		}

		TimeAndSpace(PacketNumberSpace space) {
			time = null;
			this.space = space;
		}
	}
}
