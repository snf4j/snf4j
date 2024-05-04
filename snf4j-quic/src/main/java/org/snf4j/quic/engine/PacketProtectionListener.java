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

import org.snf4j.quic.QuicException;
import org.snf4j.quic.Version;

/**
 * A listener for the QUIC packet protection functionalities.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface PacketProtectionListener {

	/**
	 * Called when there is a need for derivation of the initial keys. It is
	 * expected that the initial keys will be ready after returning from this
	 * method.
	 * 
	 * @param state         the state of the associated QUIC engine
	 * @param destinationId the original destination connection id provided by
	 *                      client
	 * @param version       the version provided by client
	 * @throws QuicException if an error occurred during the keys derivation. To close the connection
	 *                       the INTERNAL_ERROR should be signaled
	 */
	void onInitialKeys(QuicState state, byte[] destinationId, Version version) throws QuicException;
	
	/**
	 * Called after rotation of the 1-RTT keys that was initiated by other endpoint.
	 * It is not expected that the next 1-RTT keys will be ready after returning
	 * from this method. For example this method can schedule a task that will derive
	 * the next keys in the future.
	 * 
	 * @param state the state of the associated QUIC engine
	 * @throws QuicException if an error occurred. To close the connection
	 *                       the INTERNAL_ERROR should be signaled
	 */
	void onKeysRotation(QuicState state) throws QuicException;
}
