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

public class TestCCMetric implements ICongestionControllerMetric {
	
	StringBuilder trace = new StringBuilder();
	
	public void trace(String s) {
		trace.append(s).append('|');
	}
	
	public String trace() {
		String s = trace.toString();
		trace.setLength(0);
		return s;
	}
	
	@Override
	public void onWnindowChange(QuicState state, long window) {
		trace("W" + window);
	}

	@Override
	public void onSlowStartThresholdChange(QuicState state, long threshold) {
		trace("T" + threshold);
	}

	@Override
	public void onPersistentCongestion(QuicState state) {
		trace("P");
	}

	@Override
	public void afterSending(QuicState state, int inFlight) {
		trace("S" + inFlight);
	}

	@Override
	public void afterAcking(QuicState state, int inFlight) {
		trace("A" + inFlight);
	}

	@Override
	public void afterLosing(QuicState state, int inFlight) {
		trace("L" + inFlight);
	}

	@Override
	public void afterDiscarding(QuicState state, int inFlight) {
		trace("D" + inFlight);
	}
}
