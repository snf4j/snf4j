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

/**
 * An {@code enum} defining states of QUIC handshake.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public enum HandshakeState {
	
	/** 
	 * The state indicating that a QUIC engine has been just created.
	 */
	INIT(true),
	
	/**
	 * The state indicating that the QUIC handshake will be started soon.
	 */
	STARTING(true),
	
	/**
	 * The state indicating that the QUIC handshake has started and is not completed
	 * yet.
	 */
	STARTED,
	
	/**
	 * The state indicating that a server is about to complete the QUIC handshake
	 * but still need to send the HANDSHAKE_DONE frame.
	 */
	DONE_SENDING,
		
	/**
	 * The state indicating that a server is about to complete the QUIC handshake
	 * and the HANDSHAKE_DONE frame has been just sent.
	 */
	DONE_SENT,
		
	/**
	 * The state indicating that a client is about to complete the QUIC handshake
	 * but still waiting for receiving of the HANDSHAKE_DONE frame.
	 */
	DONE_WAITING,

	/**
	 * The state indicating that a client is about to complete the QUIC handshake
	 * and the HANDSHAKE_DONE frame has been just received.
	 */
	DONE_RECEIVED,
	
	/**
	 * The state indicating the completion of the QUIC handshake.
	 */
	DONE,
	
	/**
	 * The state indicating that the process of closing QUIC connection has been
	 * initiated but is not completed yet. 
	 */
	CLOSING,
	
	/**
	 * The state indicating that the QUIC connection has been fully closed.
	 */
	CLOSED;
	
	private final boolean notStarted;
	
	private HandshakeState() {
		notStarted = false;
	}

	private HandshakeState(boolean notStarted) {
		this.notStarted = notStarted;
	}

	/**
	 * Tells if a engine in this state has started the QUIC handshake.
	 * 
	 * @return {@code true} if the QUIC handshake has started
	 */
	public boolean notStarted() {
		return notStarted;
	}
	
}
