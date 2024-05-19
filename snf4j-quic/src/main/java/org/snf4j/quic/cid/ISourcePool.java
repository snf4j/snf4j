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
 * An {@code interface} representing the functionalities of the source
 * connection ids pools.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ISourcePool extends IPool {

	/**
	 * Issues a new connection id and adds it to this pool.
	 * 
	 * @return the newly issued connection id, or {@code null} if there was no room
	 *         for issuing a new connection id in this pool
	 */
	ConnectionId issue();
	
	/**
	 * Sets the maximum number of active connection ids that can be stored in this
	 * pool.
	 * 
	 * @param limit the maximum number of active connection ids
	 */
	void setLimit(int limit);
	
	/**
	 * Returns the length of connection ids issued by this pool.
	 * 
	 * @return the length of connection ids
	 */
	int getIdLength();
}
