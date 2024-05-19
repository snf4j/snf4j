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

import org.snf4j.quic.QuicException;

/**
 * An {@code interface} representing the functionalities of the destination
 * connection ids pools.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IDestinationPool extends IPool {
	
	/**
	 * Adds a new connection id with the given id and initial (0) sequence number.
	 * 
	 * @param id the id
	 * @return the newly added connection id, or {@code null} if the initial
	 *         sequence number has been retired or already exists in this pool
	 * @throws QuicException if the connection id limit has been exceeded
	 */
	ConnectionId add(byte[] id) throws QuicException;

	/**
	 * Adds a new connection id with the given sequence number, id and stateless
	 * reset token.
	 * 
	 * @param sequenceNumber the sequence number that should be associated with the
	 *                       id
	 * @param id             the id
	 * @param resetToken     the stateless reset token, or {@code null} if not token
	 *                       is defined
	 * @return the newly added connection id, or {@code null} if the sequence number
	 *         has been retired or already exists in this pool
	 * @throws QuicException if the connection id limit has been exceeded
	 */
	ConnectionId add(int sequenceNumber, byte[] id, byte[] resetToken) throws QuicException;

	/**
	 * Updates the stateless reset token for the initial (i.e. associated with 0
	 * sequence number) connection id.
	 * 
	 * @param resetToken the stateless reset token
	 * @return the updated connection id, or {@code null} no initial connection id
	 *         is not present in this pool
	 */
	ConnectionId updateResetToken(byte[] resetToken);
}
