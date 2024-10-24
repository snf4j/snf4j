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
package org.snf4j.quic.frame;

/**
 * An {@code enum} defining the core QUIC frame types as defined in RFC 9000. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public enum FrameType {
	
	/** The PADDING frame as defined in section 19.1 */
	PADDING(0x00),
	
	/** The PING frame as defined in section 19.2 */
	PING(0x01),
	
	/** The ACK frames as defined in section 19.3 */
	ACK(0x02,0x03),
	
	/** The RESET_STREAM frame as defined in section 19.4 */
	RESET_STREAM(0x04),
	
	/** The STOP_SENDING frame as defined in section 19.5 */
	STOP_SENDING(0x05),
	
	/** The CRYPTO frame as defined in section 19.6 */
	CRYPTO(0x06),

	/** The NEW_TOKEN frame as defined in section 19.7 */
	NEW_TOKEN(0x07),
	
	/** The STREAM frames as defined in section 19.8 */
	STREAM(0x08,0x0f),

	/** The MAX_DATA frame as defined in section 19.9 */
	MAX_DATA(0x10),
	
	/** The MAX_STREAM_DATA frame as defined in section 19.10 */
	MAX_STREAM_DATA(0x11),
	
	/** The MAX_STREAMS frames as defined in section 19.11 */
	MAX_STREAMS(0x12,0x13),
	
	/** The DATA_BLOCKED frame as defined in section 19.12 */
	DATA_BLOCKED(0x14),
	
	/** The STREAM_DATA_BLOCKED frame as defined in section 19.13 */
	STREAM_DATA_BLOCKED(0x15),
	
	/** The STREAMS_BLOCKED frames as defined in section 19.14 */
	STREAMS_BLOCKED(0x16,0x17),
	
	/** The NEW_CONNECTION_ID frame as defined in section 19.15 */
	NEW_CONNECTION_ID(0x18),
	
	/** The RETIRE_CONNECTION_ID frame as defined in section 19.16 */
	RETIRE_CONNECTION_ID(0x19),
	
	/** The PATH_CHALLENGE frame as defined in section 19.17 */
	PATH_CHALLENGE(0x1a),
	
	/** The PATH_RESPONSE frame as defined in section 19.18 */
	PATH_RESPONSE(0x1b),
	
	/** The CONNECTION_CLOSE frames as defined in section 19.19 */
	CONNECTION_CLOSE(0x1c,0x1d),
	
	/** The HANDSHAKE_DONE frame as defined in section 19.20 */
	HANDSHAKE_DONE(0x1e);
	
	private final int firstValue;
	
	private final int lastValue;
	
	private FrameType(int firstValue, int lastValue) {
		this.firstValue = firstValue;
		this.lastValue = lastValue;
	}

	private FrameType(int firstValue) {
		this.firstValue = firstValue;
		this.lastValue = firstValue;
	}
	
	/**
	 * Returns the first value in the range identifying the frame type.
	 * 
	 * @return the first value identifying the frame type
	 */
	public int firstValue() {
		return firstValue;
	}

	/**
	 * Returns the last value in the range identifying the frame type. For frame types with only
	 * one identifying value this value equals the first one.
	 * 
	 * @return the last value identifying the frame type
	 */
	public int lastValue() {
		return lastValue;
	}

}
