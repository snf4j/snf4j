/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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
package org.snf4j.websocket.frame;

import java.nio.charset.StandardCharsets;

/**
 * Web Socket connection close frame.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class CloseFrame extends ControlFrame {
	
	/**
	 * Indicates (value 1000) a normal closure, meaning that the purpose for which
	 * the connection was established has been fulfilled.
	 */
	public static final int NORMAL = 1000;
	
	/**
	 * Indicates (value 1001) that an endpoint is "going away", such as a server
	 * going down or a browser having navigated away from a page.
	 */
	public static final int GOING_AWAY = 1001;

	/**
	 * Indicates (value 1002) that an endpoint is terminating the connection due to
	 * a protocol error.
	 */
	public final static int PROTOCOL_ERROR = 1002;

	/**
	 * Indicates (value 1003) that an endpoint is terminating the connection because
	 * it has received a type of data it cannot accept (e.g., an endpoint that
	 * understands only text data MAY send this if it receives a binary message).
	 */
	public static final int NOT_ACCEPTED = 1003;

	/**
	 * Reserved (value 1005) and MUST NOT be set as a status code in a Close control
	 * frame by an endpoint. It is designated for use in applications expecting a
	 * status code to indicate that no status code was actually present.
	 */
	public static final int NO_CODE = 1005;
	
	/**
	 * Reserved (value 1006) and MUST NOT be set as a status code in a Close control
	 * frame by an endpoint. It is designated for use in applications expecting a
	 * status code to indicate that the connection was closed abnormally, e.g.,
	 * without sending or receiving a Close control frame.
	 */
	public static final int ABNORMAL = 1006;
	
	/**
	 * Indicates (value 1007) that an endpoint is terminating the connection because
	 * it has received data within a message that was not consistent with the type
	 * of the message (e.g., non-UTF-8 [RFC3629] data within a text message).
	 */
	public final static int NON_UTF8 = 1007;
	
	/**
	 * Indicates (value 1008) that an endpoint is terminating the connection because
	 * it has received a message that violates its policy. This is a generic status
	 * code that can be returned when there is no other more suitable status code
	 * (e.g., 1003 or 1009) or if there is a need to hide specific details about the
	 * policy.
	 */
	public static final int POLICY_VALIDATION = 1008;

	/**
	 * Indicates (1009) that an endpoint is terminating the connection because it
	 * has received a message that is too big for it to process.
	 */
	public final static int TOO_BIG = 1009;

	/**
	 * Indicates (value 1010) that an endpoint (client) is terminating the
	 * connection because it has expected the server to negotiate one or more
	 * extension, but the server didn't return them in the response message of the
	 * WebSocket handshake. The list of extensions that are needed SHOULD appear in
	 * the /reason/ part of the Close frame. Note that this status code is not used
	 * by the server, because it can fail the WebSocket handshake instead.
	 */
	public static final int MISSED_EXTENSION = 1010;

	/**
	 * Indicates (value 1011) that a server is terminating the connection because it
	 * encountered an unexpected condition that prevented it from fulfilling the
	 * request.
	 */
	public static final int UNEXPECTED_CONDITION = 1011;

	/**
	 * Indicates (value 1012) that the service is restarted. A client may reconnect,
	 * and if it choses to do, should reconnect using a randomized delay of 5 - 30s.
	 * See https://www.ietf.org/mail-archive/web/hybi/current/msg09670.html for more
	 * information.
	 **/
	public static final int SERVICE_RESTART = 1012;
	
	/**
	 * Indicates (value 1013) that the service is experiencing overload. A client
	 * should only connect to a different IP (when there are multiple for the
	 * target) or reconnect to the same IP upon user action. See
	 * https://www.ietf.org/mail-archive/web/hybi/current/msg09670.html for more
	 * information.
	 **/
	public static final int SERVICE_OVERLOAD = 1013;
	
	/**
	 * Indicates (value 1014) that the server was acting as a gateway or proxy and
	 * received an invalid response from the upstream server. This is similar to 502
	 * HTTP Status Code See
	 * https://www.ietf.org/mail-archive/web/hybi/current/msg10748.html fore more
	 * information.
	 **/
	public static final int BAD_GATEWAY = 1014;
	
	/**
	 * Reserved (value 1015) and MUST NOT be set as a status code in a Close control
	 * frame by an endpoint. It is designated for use in applications expecting a
	 * status code to indicate that the connection was closed due to a failure to
	 * perform a TLS handshake (e.g., the server certificate can't be verified).
	 */
	public static final int TLS_FAILURE = 1015;

	/**
	 * Constructs a Web Socket connection close frame.
	 * 
	 * @param rsvBits reserved bits for extensions or future versions
	 * @param payload payload data
	 * @throws IllegalArgumentException if the length of the payload data equals 1
	 *                                  or is greater than 125
	 */
	public CloseFrame(int rsvBits, byte[] payload) {
		super(Opcode.CLOSE, rsvBits, payload);
		if (payload.length == 1) {
			throw new IllegalArgumentException("illegal data length (1)");
		}
	}

	/**
	 * Constructs a Web Socket connection close frame with all reserved bits cleared.
	 * 
	 * @param payload payload data
	 * @throws IllegalArgumentException if the length of the payload data equals 1
	 *                                  or is greater than 125
	 */
	public CloseFrame(byte[] payload) {
		this(0, payload);
	}
	
	/**
	 * Constructs an empty Web Socket connection close frame with all reserved bits
	 * cleared.
	 */
	public CloseFrame() {
		this(EMPTY_PAYLOAD);
	}

	
	/**
	 * Constructs a Web Socket connection close frame with the specified status
	 * code.
	 * 
	 * @param rsvBits reserved bits for extensions or future versions
	 * @param status  the status code
	 */
	public CloseFrame(int rsvBits, int status) {
		this(rsvBits, payload(status, null));
	}

	/**
	 * Constructs a Web Socket connection close frame with the specified status code
	 * and with all reserved bits cleared.
	 * 
	 * @param status the status code
	 */
	public CloseFrame(int status) {
		this(0, status);
	}

	/**
	 * Constructs a Web Socket connection close frame with the specified status code
	 * and reason text.
	 * 
	 * @param rsvBits reserved bits for extensions or future versions
	 * @param status  the status code
	 * @param reason  the reason text
	 */
	public CloseFrame(int rsvBits, int status, String reason) {
		this(rsvBits, payload(status, reason));
	}
	
	/**
	 * Constructs a Web Socket connection close frame with the specified status code,
	 * reason text and with all reserved bit cleared.
	 * 
	 * @param status  the status code
	 * @param reason  the reason text
	 */
	public CloseFrame(int status, String reason) {
		this(0, status, reason);
	}
	
	/**
	 * Returns the closing status code in this frame.
	 * 
	 * @return the status code, or {@code -1} if this frame is empty (no payload)
	 */
	public int getStatus() {
		if (payload.length > 0) {
			return ((int)payload[0] << 8) & 0xff00 | (int)payload[1] & 0xff;
		}
		return -1;
	}
	
	/**
	 * Returns the closing reason text in this frame.
	 * 
	 * @return the reason text or an empty text if the reason is not present
	 */
	public String getReason() {
		if (payload.length <= 2) {
			return "";
		}
		return new String(payload, 2, payload.length-2, StandardCharsets.UTF_8);
	}
	
	private static byte[] payload(int status, String reason) {
		byte[] payload;
		
		if (reason != null) {
			byte[] bytes = reason.getBytes(StandardCharsets.UTF_8);
			
			payload = new byte[2+bytes.length];
			if (bytes.length > 0) {
				System.arraycopy(bytes, 0, payload, 2, bytes.length);
			}
		}
		else {
			payload = new byte[2];
		}
		
		payload[0] = (byte) ((status >> 8) & 0xff);
		payload[1] = (byte) (status & 0xff);
		return payload;
	}
}
