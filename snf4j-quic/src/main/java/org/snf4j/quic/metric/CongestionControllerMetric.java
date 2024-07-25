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
import org.snf4j.quic.metric.ICongestionControllerMetric;

/**
 * The default metric for the congestion controller, which does not collect any
 * metrics.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class CongestionControllerMetric implements ICongestionControllerMetric {

	/** A stateless instance of the default metric. */
	public final static ICongestionControllerMetric INSTANCE = new CongestionControllerMetric();
	
	@Override
	public void onWnindowChange(QuicState state, long window) {
	}

	@Override
	public void onSlowStartThresholdChange(QuicState state, long threshold) {
	}

	@Override
	public void onPersistentCongestion(QuicState state) {
	}

	@Override
	public void afterSending(QuicState state, int inFlight) {
	}

	@Override
	public void afterAcking(QuicState state, int inFlight) {
	}

	@Override
	public void afterLosing(QuicState state, int inFlight) {
	}

	@Override
	public void afterDiscarding(QuicState state, int inFlight) {
	}
}
