/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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
package org.snf4j.core.engine;

/**
 * An <code>interface</code> encapsulating the result state produced by the 
 * {@link IEngine} calls.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */

public interface IEngineResult {
	
	/**
	 * Returns the number of bytes consumed from the input buffer.
	 *
	 * @return the number of bytes consumed.
	 */
	int bytesConsumed();
	
	/**
	 * Returns the number of bytes written to the output buffer.
	 *
	 * @return the number of bytes produced
	 */
	int bytesProduced();
	
	/**
	 * Gets the overall result of the {@link IEngine} calls.
	 * 
	 * @return the return value
	 */
	Status getStatus();
	
	/**
	 * Gets the current handshake status produced by the {@link IEngine} call.
	 * It should match the value returned by the
	 * {@link IEngine#getHandshakeStatus()} method. The only exception to this
	 * rule is when the call has just finished the ongoing handshake. In such
	 * situation this method should return the {@link HandshakeStatus#FINISHED }
	 * status which is forbidden for the {@link IEngine#getHandshakeStatus()}
	 * method.
	 * 
	 * @return the handshake status
	 */
	HandshakeStatus getHandshakeStatus();
}
