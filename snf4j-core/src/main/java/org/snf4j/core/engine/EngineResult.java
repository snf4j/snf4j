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
 * The default implementation of the {@link IEngineResult} interface.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class EngineResult implements IEngineResult {

	private final Status status;
	
	private final HandshakeStatus handshakeStatus;
	
	private final int bytesConsumed;

	private final int bytesProduced;
	
	/**
	 * Initializes a new instance of this class.
	 * 
	 * @param status
	 *            the return value of the operation
	 * @param handshakeStatus
	 *            the current handshaking status
	 * @param bytesConsumed
	 *            the number of bytes consumed from the input buffer
	 * @param bytesProduced
	 *            the number of bytes placed into the output buffer
	 * @throws IllegalArgumentException
	 *             if the <code>status</code> or <code>handshakeStatus</code>
	 *             arguments are null, or if <code>bytesConsumed</code> or <code>bytesProduced</code> is
	 *             negative.
	 */
	public EngineResult(Status status, HandshakeStatus handshakeStatus,
            int bytesConsumed,
            int bytesProduced) {
		if (status == null) {
			throw new IllegalArgumentException("status is null");
		}
		if (handshakeStatus == null) {
			throw new IllegalArgumentException("handshakeStatus is null");
		}
		if (bytesConsumed < 0) {
			throw new IllegalArgumentException("bytesConsumed is negative");
		}
		if (bytesProduced < 0) {
			throw new IllegalArgumentException("bytesProduced is negative");
		}
		this.status = status;
		this.handshakeStatus = handshakeStatus;
		this.bytesConsumed = bytesConsumed;
		this.bytesProduced = bytesProduced;
	}
	
	@Override
	public final int bytesConsumed() {
		return bytesConsumed;
	}

	@Override
	public final int bytesProduced() {
		return bytesProduced;
	}

	@Override
	public final Status getStatus() {
		return status;
	}

	@Override
	public final HandshakeStatus getHandshakeStatus() {
		return handshakeStatus;
	}

}
