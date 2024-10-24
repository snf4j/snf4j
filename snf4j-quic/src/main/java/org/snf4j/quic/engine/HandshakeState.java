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
 * <p>
 * Closing phase:
 * <pre>
 *              +---------------+   sent    .....................
 *   close() -&gt; | CLOSE_SENDING | --------&gt; . CLOSE_PRE_WAITING .
 *              +---------------+           .....................
 *                      |                             .
 *                      |                             . outbound_done
 *                      |                             .
 *                      |                             V
 *                      |                   +-------------------+
 *                      |                   |   CLOSE_WAITING   |
 *                      |                   +-------------------+
 *                      |                             |
 *                      |                             | rcvd
 *                      |                             |
 *                      |                             V
 *                      |                   ......................
 *                      |                   . CLOSE_PRE_DRAINING .
 *                      |                   ......................
 *                      |                             .
 *                      |                             . inbound_done
 *                      | rcvd                        .
 *                      |                             V
 *                      |                   +--------------------+
 *                      |                   |   CLOSE_DRAINING   |
 *                      |                   +--------------------+
 *                      |                             ^
 *                      |                             .
 *                      |                             . outbound_done
 *                      |                             .
 *                      |                  ........................
 *                      |                  . CLOSE_PRE_DRAINING_2 .
 *                      |                  ........................
 *                      |                             ^
 *                      |                             |
 *                      |                             | sent
 *                      V        inbound_done         |
 *          .......................    \    +--------------------+
 *   rcvd -&gt;. CLOSE_PRE_SENDING_2 .........&gt;|   CLOSE_SENDING_2  |
 *          .......................         +--------------------+
 * </pre>
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public enum HandshakeState {
	
	/** 
	 * A state indicating that a QUIC engine has been just created.
	 */
	INIT(true),
	
	/**
	 * A state indicating that the QUIC handshake will be started soon.
	 */
	STARTING(true),
	
	/**
	 * A state indicating that the QUIC handshake has started and is not completed
	 * yet.
	 */
	STARTED,
	
	/**
	 * A state indicating that a server is about to complete the QUIC handshake
	 * but still need to send the HANDSHAKE_DONE frame.
	 */
	DONE_SENDING,
		
	/**
	 * A transitory state indicating that a server is about to complete the QUIC
	 * handshake and the HANDSHAKE_DONE frame has been just sent.
	 */
	DONE_SENT(false, false, true),
		
	/**
	 * A state indicating that a client is about to complete the QUIC handshake
	 * but still waiting for receiving of the HANDSHAKE_DONE frame.
	 */
	DONE_WAITING,

	/**
	 * A transitory state indicating that a client is about to complete the QUIC
	 * handshake and the HANDSHAKE_DONE frame has been just received.
	 */
	DONE_RECEIVED(false, false, true),
	
	/**
	 * A state indicating the completion of the QUIC handshake.
	 */
	DONE,
	
	/**
	 * A state indicating that the connection closing has been initiated and not
	 * all pending CONNECTION_CLOSE frames have been sent yet.
	 */
	CLOSE_SENDING(false, true),

	/**
	 * A transitory state indicating that CONNECTION_CLOSE frame has been just
	 * received and the QUIC engine should immediately perform transition to
	 * {@link #CLOSE_SENDING_2} state.
	 */
	CLOSE_PRE_SENDING_2(false, true, true),
	
	/**
	 * A state indicating that the connection closing has been initiated by peer
	 * and not all pending CONNECTION_CLOSE frames have been sent yet.
	 */
	CLOSE_SENDING_2(false, true),
	
	/**
	 * A transitory state indicating that all CONNECTION_CLOSE frames have been just
	 * sent and the QUIC engine should immediately perform transition to
	 * {@link #CLOSE_WAITING} state.
	 */
	CLOSE_PRE_WAITING(false, true, true),
		
	/**
	 * A state indicating that all CONNECTION_CLOSE frames have been sent and an
	 * endpoint is currently waiting for receiving of the CONNECTION_CLOSE frame.
	 */
	CLOSE_WAITING(false, true),
		
	/**
	 * A state indicating that expected CONNECTION_CLOSE was not received within
	 * closing interval. 
	 */
	CLOSE_TIMEOUT(false, true),
	
	/**
	 * A transitory state indicating that CONNECTION_CLOSE frame have been just
	 * received and the QUIC engine should immediately perform transition to
	 * {@link #CLOSE_DRAINING} state.
	 */
	CLOSE_PRE_DRAINING(false, true, true),

	/**
	 * A transitory state indicating that all CONNECTION_CLOSE frames have been just
	 * sent and the QUIC engine should immediately perform transition to
	 * {@link #CLOSE_DRAINING} state.
	 */
	CLOSE_PRE_DRAINING_2(false, true, true),
	
	/**
	 * The state indicating that the QUIC connection has been fully closed and is currently
	 * in the draining state.
	 */
	CLOSE_DRAINING(false, true);
	
	private final boolean notStarted;
	
	private final boolean closing;
	
	private final boolean transitory;
	
	private HandshakeState() {
		notStarted = false;
		closing = false;
		transitory = false;
	}

	private HandshakeState(boolean notStarted) {
		this.notStarted = notStarted;
		closing = false;
		transitory = false;
	}

	private HandshakeState(boolean notStarted, boolean closing) {
		this.notStarted = notStarted;
		this.closing = closing;
		transitory = false;
	}

	private HandshakeState(boolean notStarted, boolean closing, boolean transitory) {
		this.notStarted = notStarted;
		this.closing = closing;
		this.transitory = transitory;
	}
	
	/**
	 * Tells if QUIC engine in this state has started the QUIC handshake.
	 * 
	 * @return {@code true} if the QUIC handshake has started
	 */
	public boolean notStarted() {
		return notStarted;
	}

	/**
	 * Tells if QUIC engine in this state is in the closing phase.
	 * 
	 * @return {@code true} if the closing phase is in progress
	 */
	public boolean isClosing() {
		return closing;
	}

	/**
	 * Tells if this state is a transitory state which is used only to pass
	 * information from QUIC processor to QUIC engine. After detecting such state
	 * engine should immediately perform transition to proper non-transitory state.
	 * 
	 * @return {@code true} if it is a transitory state
	 */
	public boolean isTransitory() {
		return transitory;
	}
	
}
