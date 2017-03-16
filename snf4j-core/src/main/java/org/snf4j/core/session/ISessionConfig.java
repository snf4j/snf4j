/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017 SNF4J contributors
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
package org.snf4j.core.session;

import org.snf4j.core.EndingAction;

/**
 * A configuration for associated session.
 *  
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ISessionConfig {

	/**
	 * Gets the minimum capacity for the session's input buffer. The current
	 * capacity is adjusted automatically and will never be decreased below
	 * this value.
	 * <p>
	 * This value also determines the initial capacity of the input buffer.
	 * <p>
	 * Changing it once an associated session is created has no effect for 
	 * the session.
	 * 
	 * @return the minimum capacity in bytes.
	 */
	int getMinInBufferCapacity();
	
	/**
	 * Gets the maximum capacity for the session's input buffer. The current
	 * capacity is adjusted automatically and will never be increased above
	 * this value. 
	 * <p>
	 * Changing it once an associated session is created has no effect for 
	 * the session.
	 * 
	 * @return the maximum capacity in bytes.
	 */
	int getMaxInBufferCapacity();
	
	/**
	 * Gets the minimum capacity for the session's output buffer. The current
	 * capacity is adjusted automatically and will never be decreased below
	 * this value.
	 * <p>
	 * This configuration parameter is only valid for stream-oriented 
	 * transmissions.
	 * <p>
	 * Changing it once an associated session is created has no effect for 
	 * the session.
	 *  
	 * @return the minimum capacity in bytes.
	 */
	int getMinOutBufferCapacity();
	
	/**
	 * Gets the interval in milliseconds between each throughput calculation.
	 * 
	 * @return the interval in milliseconds.
	 */
	long getThroughputCalculationInterval();
	
	/**
	 * Determines if possibly incomplete datagrams should be ignored.
	 * <p>
	 * If the size of received datagram is bigger than the size of session's
	 * input buffer the remainder of the datagram is silently discarded by the
	 * NIO API. In some protocols (e.g. TFTP) such situations would lead to
	 * erroneous processing of the received datagram.
	 * <p>
	 * To solve such situations we can ignore each datagram that fully filled up
	 * the session's input buffer. Considering that after detecting such
	 * datagram the size of the input buffer will be extended up to the max
	 * limit there is a big chance that there will be enough room in the buffer
	 * when the ignored (i.e. lost) datagram will be retransmitted by the remote
	 * end-point.
	 * <p>
	 * Changing it once an associated session is created has no effect for 
	 * the session.
	 * 
	 * @return <code>true</code> if possibly incomplete datagrams should be
	 *         ignored
	 * @see java.nio.channels.DatagramChannel#read
	 */
	boolean ignorePossiblyIncompleteDatagrams();
	
	/**
	 * Gets the action that should be performed by the selector
	 * loop after ending of the associated session.
	 * 
	 * @return the ending action
	 */
	EndingAction getEndingAction();
}
