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
package org.snf4j.quic;

import org.snf4j.quic.tp.TransportParameters;

/**
 * The default implementation of QUIC configuration.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class QuicConfig implements IQuicConfig {

	@Override
	public int getMaxNumberOfAckRanges() {
		return 3;
	}

	@Override
	public int getMaxNumberOfStoredAckRanges() {
		return 10;
	}

	@Override
	public int getAckDelayExponent() {
		return TransportParameters.DEFAULT_ACK_DELAY_EXPONENT;
	}

	@Override
	public int getMaxAckDelay() {
		return TransportParameters.DEFAULT_MAX_ACK_DELAY;
	}
	
	@Override
	public int getConnectionIdLength() {
		return 8;
	}

	@Override
	public int getActiveConnectionIdLimit() {
		return TransportParameters.DEFAULT_ACTIVE_CONNECTION_ID_LIMIT;
	}

	@Override
	public int getMaxActiveConnectionIdLimit() {
		return 10;
	}

	@Override
	public int getMinNonBlockingUdpPayloadSize() {
		return 100;
	}

}
