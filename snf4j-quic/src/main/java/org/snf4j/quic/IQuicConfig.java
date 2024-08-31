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

/**
 * A QUIC configuration.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IQuicConfig {
	
	/**
	 * Determines the maximum number of ranges in sent ACK frames.
	 * <p>
	 * This parameter allows to control the maximum size of sent ACK frames. The ACK
	 * ranges that are stored and that were not put into an ACK frame can be put
	 * into a next ACK frame.
	 * 
	 * @return the maximum number of ranges in sent ACK frames
	 */
	int getMaxNumberOfAckRanges();

	/**
	 * Determines the maximum number of stored ACK ranges.
	 * <p>
	 * This parameter allows to control the maximum size of resources used to store
	 * ACK ranges in each packet number space.
	 * 
	 * @return the maximum number of stored ACK ranges
	 */
	int getMaxNumberOfStoredAckRanges();
	
	/**
	 * Determines the acknowledgment delay exponent that will be used to encode the
	 * ACK delay after the handshake confirmation.
	 * 
	 * @return the acknowledgment delay exponent
	 */
	int getAckDelayExponent();
	
	/**
	 * Determines the maximum acknowledgment delay in milliseconds.
	 * 
	 * @return the maximum acknowledgment delay
	 */
	int getMaxAckDelay();
	
	/**
	 * Determines the length in bytes of locally generated connection IDs
	 * 
	 * @return the length of locally generated connection IDs
	 */
	int getConnectionIdLength();
	
	/**
	 * Determines the maximum maximum number of active connection IDs from the peer
	 * that an endpoint is willing to store.
	 * 
	 * @return the maximum number of active connection IDs to store
	 */
	int getActiveConnectionIdLimit();
	
	/**
	 * Determines the maximum number of active connection IDs an endpoint is willing
	 * to send to the peer.
	 * 
	 * @return the maximum number of active connection IDs to send
	 */
	int getMaxActiveConnectionIdLimit();

	/**
	 * Determines the minimal non-blocking UDP payload size which defines the
	 * minimal number of bytes that must be available in the congestion or
	 * anti-amplification buffer to not block the sending.
	 * 
	 * @return the minimal non-blocking UDP payload size
	 */
	int getMinNonBlockingUdpPayloadSize();
}
