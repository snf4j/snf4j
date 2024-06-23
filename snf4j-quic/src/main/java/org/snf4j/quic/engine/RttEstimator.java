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

/**
 * The Round-Trip Time (RTT) estimator as defined in RFC 9002.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class RttEstimator {

	private final static long INITIAL_RTT = 333000000;
	
	private final QuicState state;
	
	private long latestRtt;
	
	private long minRtt;
	
	private long smoothedRtt;
	
	private long rttVar;
	
	private boolean sampled;
	
	public RttEstimator(QuicState state) {
		this.state = state;
		smoothedRtt = INITIAL_RTT;
		rttVar = INITIAL_RTT / 2;
	}
	
	/**
	 * Called on receiving an ACK frame that meets all conditions for the RTT sample
	 * generation.
	 * 
	 * @param ackTime  the time in nanoseconds the ACK frame was received
	 * @param sentTime the time in nanoseconds the largest acknowledged packet
	 *                 number newly acknowledged was sent
	 * @param ackDelay the decoded ACK delay in microseconds from the ACK frame
	 * @param level    the keys being used for protecting the packet carrying the
	 *                 ACK frame
	 */
	public void addSample(long ackTime, long sentTime, long ackDelay, EncryptionLevel level) {	
		if (ackTime < sentTime) {
			return;
		}
		
		latestRtt = ackTime - sentTime;
				
		if (sampled) {
			if (latestRtt < minRtt) {
				minRtt = latestRtt;
			}

			switch (level) {
			case INITIAL:
				ackDelay = 0;
				break;
				
			case APPLICATION_DATA:
				ackDelay = Math.min(ackDelay, state.getPeerMaxAckDelay()*1000)*1000;
				break;
				
			default:
				ackDelay *= 1000;
			}

			long adjustedRtt = latestRtt;
			if (adjustedRtt - ackDelay >= minRtt) {
				adjustedRtt -= ackDelay;
			}
			
			rttVar = (3 * rttVar + Math.abs(smoothedRtt - adjustedRtt)) / 4;
			smoothedRtt = (7 * smoothedRtt + adjustedRtt) / 8;
		}
		else {
			minRtt = latestRtt;
			smoothedRtt = latestRtt;
			rttVar = latestRtt / 2;
			sampled = true;
		}
	}

	/**
	 * Returns the latest RTT sample in nanoseconds.
	 * 
	 * @return the latest RTT sample
	 */
	public long getLatestRtt() {
		return latestRtt;
	}

	
	/**
	 * Returns the minimum RTT sample in nanoseconds.
	 * 
	 * @return the minimum RTT sample
	 */
	public long getMinRtt() {
		return minRtt;
	}

	/**
	 * Returns the exponentially weighted moving average in nanoseconds of RTT
	 * samples.
	 * 
	 * @return the exponentially weighted moving average of RTT samples
	 */
	public long getSmoothedRtt() {
		return smoothedRtt;
	}

	/**
	 * Returns the estimated variation in nanoseconds in the RTT samples.
	 * 
	 * @return the estimated variation in nanoseconds in the RTT samples
	 */
	public long getRttVar() {
		return rttVar;
	}	
	
}
