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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.tls.Args;

/**
 * A CONNECTION_CLOSE frame as defined in RFC 9000. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class ConnectionCloseFrame implements IFrame {
	
	private final static FrameType TYPE = FrameType.CONNECTION_CLOSE;
	
	private final static byte[] NO_REASON = new byte[0];
	
	private final static int MAX_FRAME_TYPE = 0x1e;
	
	public final static int QUIC_TYPE = 0x1c;

	public final static int APPLICATION_TYPE = 0x1d;
		
	private final long error;
	
	private final int frameType;
	
	private final byte[] reason;

	private final static IFrameParser PARSER = new IFrameParser() {

		@Override
		public FrameType getType() {
			return TYPE;
		}

		@SuppressWarnings("unchecked")
		@Override
		public ConnectionCloseFrame parse(ByteBuffer src, int remaining, int type) throws QuicException {
			if (remaining > 1) {
				int[] remainings = new int[] {remaining};
				long error = FrameUtil.decodeInteger(src, remainings);
				
				if (remainings[0] >= 1) {
					long frameType;
					
					if (type == QUIC_TYPE) {
						frameType = FrameUtil.decodeInteger(src, remainings);
						if (frameType > MAX_FRAME_TYPE) {
							throw new QuicException(TransportError.FRAME_ENCODING_ERROR, "Unknown frame type in ConnectionClose frame");
						}
					}
					else {
						frameType = -1;
					}
					if (remainings[0] >= 1) {
						long len = FrameUtil.decodeInteger(src, remainings);
						
						if (len <= remainings[0]) {
							byte[] reason;
							
							if (len == 0) {
								reason = NO_REASON;
							}
							else {
								reason = new byte[(int) len];							
								src.get(reason);
							}
							if (frameType == -1) {
								return new ConnectionCloseFrame(error, reason);
							}
							return new ConnectionCloseFrame(error, (int) frameType, reason);
						}
					}
				}
			}
			throw new QuicException(TransportError.FRAME_ENCODING_ERROR, "Inconsistent length of ConnectionClose frame");
		}
	};

	private ConnectionCloseFrame(long error, int frameType, byte[] reason) {
		Args.checkRange(frameType, 0, MAX_FRAME_TYPE, "frameType");
		this.error = error;
		this.frameType = frameType;
		this.reason = reason;
	}

	private ConnectionCloseFrame(long error, byte[] reason) {
		this.error = error;
		this.frameType = -1;
		this.reason = reason;
	}
	
	/**
	 * Constructs a CONNECTION_CLOSE frame signaling errors at only the QUIC layer.
	 * 
	 * @param error     an error code that indicates the reason for closing the
	 *                  connection
	 * @param frameType an value encoding the type of frame that triggered the
	 *                  error, or 0 if the type is unknown
	 * @param reason    an additional diagnostic information for the closure
	 */
	public ConnectionCloseFrame(long error, int frameType, String reason) {
		this(error, frameType, reason == null ? NO_REASON : utf8(reason));
	}
	
	/**
	 * Constructs a CONNECTION_CLOSE frame signaling an error with the application
	 * that uses QUIC.
	 * 
	 * @param error  an error code that indicates the reason for closing the
	 *               connection
	 * @param reason an additional diagnostic information for the closure
	 */
	public ConnectionCloseFrame(long error, String reason) {
		this(error, reason == null ? NO_REASON : utf8(reason));
	}

	/**
	 * Return the default CONNECTION_CLOSE frame parser.
	 * 
	 * @return the CONNECTION_CLOSE frame parser
	 */
	public static IFrameParser getParser() {
		return PARSER;
	}
	
	private static byte[] utf8(String s) {
		return s.getBytes(StandardCharsets.UTF_8);
	}
	
	private static String utf8(byte[] a) {
		return new String(a, StandardCharsets.UTF_8);
	}
	
	@Override
	public FrameType getType() {
		return TYPE;
	}

	@Override
	public int getTypeValue() {
		return frameType == -1 ? APPLICATION_TYPE : QUIC_TYPE;
	}

	@Override
	public int getLength() {
		if (frameType == -1) {
			return 1
				+ FrameUtil.encodedIntegerLength(error)
				+ FrameUtil.encodedIntegerLength(reason.length)
				+ reason.length;
		}
		return 1
			+ FrameUtil.encodedIntegerLength(error)
			+ 1
			+ FrameUtil.encodedIntegerLength(reason.length)
			+ reason.length;
	}

	@Override
	public void getBytes(ByteBuffer dst) {
		dst.put((byte) getTypeValue());
		FrameUtil.encodeInteger(error, dst);
		if (frameType != -1) {
			FrameUtil.encodeInteger(frameType, dst);
		}
		if (reason.length == 0) {
			FrameUtil.encodeInteger(0, dst);
		}
		else {
			FrameUtil.encodeInteger(reason.length, dst);
			dst.put(reason);
		}
	}

	/**
	 * Returns the error code that indicates the reason for closing the connection.
	 * 
	 * @return the error code
	 */
	public long getError() {
		return error;
	}

	/**
	 * Returns the value encoding the type of frame that triggered the error
	 * 
	 * @return the value encoding the type of frame, or -1 if the type is not
	 *         applicable
	 */
	public int getFrameType() {
		return frameType;
	}

	/**
	 * Returns an additional diagnostic information for the closure.
	 * 
	 * @return an additional diagnostic information, or an empty string if the
	 *         reason is not present
	 */
	public String getReason() {
		return utf8(reason);
	}

}
