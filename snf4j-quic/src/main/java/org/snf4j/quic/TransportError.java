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
package org.snf4j.quic;

/**
 * An {@code enum} defining transport errors detected by an QUIC subsystem. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public enum TransportError {

	/**
	 * An endpoint uses this with CONNECTION_CLOSE to signal that the connection is
	 * being closed abruptly in the absence of any error.
	 */
	NO_ERROR(0x00),
	
	/**
	 * The endpoint encountered an internal error and cannot continue with the
	 * connection.
	 */
	INTERNAL_ERROR (0x01),
	
	/** The server refused to accept a new connection. */
	CONNECTION_REFUSED (0x02),
	
	/**
	 * An endpoint received more data than it permitted in its advertised data
	 * limits.
	 */
	FLOW_CONTROL_ERROR (0x03),
	
	/**
	 * An endpoint received a frame for a stream identifier that exceeded its
	 * advertised stream limit for the corresponding stream type.
	 */
	STREAM_LIMIT_ERROR (0x04),

	/**
	 * An endpoint received a frame for a stream that was not in a state that
	 * permitted that frame.
	 */
	STREAM_STATE_ERROR (0x05),
	
	/**
	 * (1) An endpoint received a STREAM frame containing data that exceeded the
	 * previously established final size, <br> 
	 * (2) an endpoint received a STREAM frame or
	 * a RESET_STREAM frame containing a final size that was lower than the size of
	 * stream data that was already received, or (3) an endpoint received a STREAM
	 * frame or a RESET_STREAM frame containing a different final size to the one
	 * already established.
	 */
	FINAL_SIZE_ERROR (0x06),
	
	/**
	 * An endpoint received a frame that was badly formatted -- for instance, a
	 * frame of an unknown type or an ACK frame that has more acknowledgment ranges
	 * than the remainder of the packet could carry.
	 */
	FRAME_ENCODING_ERROR (0x07),

	/**
	 * An endpoint received transport parameters that were badly formatted, included
	 * an invalid value, omitted a mandatory transport parameter, included a
	 * forbidden transport parameter, or were otherwise in error.
	 */
	TRANSPORT_PARAMETER_ERROR (0x08),
	
	/**
	 * The number of connection IDs provided by the peer exceeds the advertised
	 * active_connection_id_limit.
	 */
	CONNECTION_ID_LIMIT_ERROR (0x09),

	/**
	 * An endpoint detected an error with protocol compliance that was not covered
	 * by more specific error codes.
	 */
	PROTOCOL_VIOLATION (0x0a),

	/** A server received a client Initial that contained an invalid Token field. */
	INVALID_TOKEN (0x0b),

	/**
	 * The application or application protocol caused the connection to be closed.
	 */
	APPLICATION_ERROR (0x0c),

	/** An endpoint has received more data in CRYPTO frames than it can buffer. */
	CRYPTO_BUFFER_EXCEEDED (0x0d),

	/** An endpoint detected errors in performing key updates. */
	KEY_UPDATE_ERROR (0x0e),
	
	/**
	 * An endpoint has reached the confidentiality or integrity limit for the AEAD
	 * algorithm used by the given connection.
	 */
	AEAD_LIMIT_REACHED (0x0f),
	
	/**
	 * An endpoint has determined that the network path is incapable of supporting
	 * QUIC. An endpoint is unlikely to receive a CONNECTION_CLOSE frame carrying
	 * this code except when the path does not support a large enough MTU.
	 */
	NO_VIABLE_PATH (0x10),

	/** The cryptographic handshake failed. */
	CRYPTO_ERROR (0x0100);
	
	private int code;
	
	private TransportError(int code) {
		this.code = code;
	}
	
	/**
	 * Returns the error code that is defined for this transport error.
	 * <p>
	 * NOTE: For the {@link #CRYPTO_ERROR} transport error it returns the first value from the reserved
	 * range of 256 values.
	 * 
	 * @return the error code
	 */
	public int code() {
		return code;
	}
	
	/**
	 * Returns an instance of the transport error for the given error code.
	 * 
	 * @param code the error code
	 * @return a transport error, or {@code null} if no transport error could by
	 *         found
	 */
	public static TransportError of(long code) {
		if (code >= 0 && code <= 0x10) {
			return TransportError.values()[(int) code];
		}
		if (code >= 0x0100 && code <= 0x01ff) {
			return TransportError.CRYPTO_ERROR;
		}
		return null;
	}
}
