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

import org.snf4j.quic.IQuicConfig;

public class TestConfig implements IQuicConfig {

	public int maxNumberOfAckRanges = 3;
	
	public int maxNumberOfStoredAckRanges = 10;
	
	public int ackDelayExponent = 3;
	
	public int maxAckDelay = 25;
	
	public int connectionIdLength = 8;
	
	public int activeConnectionIdLimit = 2;
	
	public int maxActiveConnectionIdLimit = 10;
	
	@Override
	public int getMaxNumberOfAckRanges() {
		return maxNumberOfAckRanges;
	}

	@Override
	public int getMaxNumberOfStoredAckRanges() {
		return maxNumberOfStoredAckRanges;
	}

	@Override
	public int getAckDelayExponent() {
		return ackDelayExponent;
	}
	@Override
	public int getMaxAckDelay() {
		return maxAckDelay;
	}
	
	@Override
	public int getConnectionIdLength() {
		return connectionIdLength;
	}

	@Override
	public int getActiveConnectionIdLimit() {
		return activeConnectionIdLimit;
	}

	@Override
	public int getMaxActiveConnectionIdLimit() {
		return maxActiveConnectionIdLimit;
	}

}
