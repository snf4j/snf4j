/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.engine;

public interface IEarlyDataHandler {
	
	/**
	 * Determines the max size of incoming early data that was rejected by
	 * server. This size should also include possible padding as without
	 * cryptographic material servers will not be able to differentiate padding from
	 * content.
	 * 
	 * @return the max size of incoming early data
	 */
	long getMaxEarlyDataSize();
	
	/**
	 * Tells if client has an early data to send.
	 * 
	 * @return {@code true} if the client has an early data to send
	 */
	boolean hasEarlyData();
	
	/**
	 * Called only when client is ready for sending an early data. It will not be
	 * called when sending of an early data is not possible (e.g. lack of a proper
	 * session ticket).
	 * 
	 * @param protocol the application protocol that was chosen for the early data.
	 * @return the early data or {@code null} if no more early data is available
	 */
	byte[] nextEarlyData(String protocol);
	
	void acceptedEarlyData();
	
	void rejectedEarlyData();

}
