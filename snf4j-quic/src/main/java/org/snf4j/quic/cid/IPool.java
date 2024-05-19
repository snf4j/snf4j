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
package org.snf4j.quic.cid;

/**
 * An {@code interface} representing the base functionalities of the connection
 * ids pools.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IPool {
	
	/**
	 * The sequence number (0) for the initial connection id established during the
	 * handshake.
	 */
	public final static Integer INITIAL_SEQUENCE_NUMBER = new Integer(0);

	/**
	 * The sequence number (1) for the alternative connection id passed in the
	 * preferred_address transport parameter.
	 */
	public final static Integer ALTERNATIVE_SEQUENCE_NUMBER = new Integer(1);
	
	/**
	 * Returns one of the active connection ids stored in this pool.
	 * 
	 * @return one of the active connection ids stored in this pool
	 */
	ConnectionId get();

	/**
	 * Returns the connection id associated with the given sequence number.
	 * 
	 * @param sequenceNumber the sequence number
	 * @return the connection id, or {@code null} if no active connection id could
	 *         be found for the given sequence number
	 */
	ConnectionId get(int sequenceNumber);
	
	/**
	 * Returns the maximum number of active connection ids that can be stored in
	 * this pool.
	 * 
	 * @return the maximum number of active connection ids
	 */
	int getLimit();
	
	/**
	 * Return the number of active connection ids stored in this pool
	 * 
	 * @return the number of active connection ids
	 */
	int getSize();
	
	/**
	 * Retires the connection id associated with the given sequence number.
	 * 
	 * @param sequenceNumber the sequence number
	 * @return the retired connection id, or {@code null} if no active connection id
	 *         could be found for the given sequence number
	 */
	ConnectionId retire(int sequenceNumber);

}
