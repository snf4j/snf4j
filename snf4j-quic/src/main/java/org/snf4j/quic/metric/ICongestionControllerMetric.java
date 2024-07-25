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
package org.snf4j.quic.metric;

import org.snf4j.quic.engine.QuicState;

/**
 * A metric for the congestion controller.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ICongestionControllerMetric {

	/**
	 * Signals a change of the congestion window.
	 * 
	 * @param state  the QUIC state associated with the congestion controller
	 * @param window the new size in bytes of the congestion window
	 */
	void onWnindowChange(QuicState state, long window);
	
	/**
	 * Signals a change of the slow start threshold.
	 * 
	 * @param state     the QUIC state associated with the congestion controller
	 * @param threshold the new size in bytes of the slow start threshold
	 */
	void onSlowStartThresholdChange(QuicState state, long threshold);
	
	/**
	 * Signals an occurrence of the persistent congestion.
	 * 
	 * @param state the QUIC state associated with the congestion controller
	 */
	void onPersistentCongestion(QuicState state);

	/**
	 * Provides the current number of bytes in flight after sending of a packet.
	 * 
	 * @param state    the QUIC state associated with the congestion controller
	 * @param inFlight the current number of bytes in flight
	 */
	void afterSending(QuicState state, int inFlight);
	
	/**
	 * Provides the current number of bytes in flight after acknowledgment of a
	 * packet.
	 * 
	 * @param state    the QUIC state associated with the congestion controller
	 * @param inFlight the current number of bytes in flight
	 */
	void afterAcking(QuicState state, int inFlight);
	
	/**
	 * Provides the current number of bytes in flight after losing of a packet.
	 * 
	 * @param state    the QUIC state associated with the congestion controller
	 * @param inFlight the current number of bytes in flight
	 */
	void afterLosing(QuicState state, int inFlight);
	
	/**
	 * Provides the current number of bytes in flight after discarding of a packet.
	 * 
	 * @param state    the QUIC state associated with the congestion controller
	 * @param inFlight the current number of bytes in flight
	 */
	void afterDiscarding(QuicState state, int inFlight);
	
}
