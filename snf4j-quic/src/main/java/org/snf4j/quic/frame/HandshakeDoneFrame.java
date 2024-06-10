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

/**
 * A HANDSHAKE_DONE frame as defined in RFC 9000. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class HandshakeDoneFrame implements IFrame {

	private final static FrameType TYPE = FrameType.HANDSHAKE_DONE;
	
	/**
	 * A stateless instance of the HANDSHAKE_DONE frame.
	 */
	public static final HandshakeDoneFrame INSTANCE = new HandshakeDoneFrame();
	
	private final static IFrameParser PARSER = new IFrameParser() {

		@Override
		public FrameType getType() {
			return TYPE;
		}

		@SuppressWarnings("unchecked")
		@Override
		public HandshakeDoneFrame parse(ByteBuffer src, int remaining, int type) {
			return INSTANCE;
		}
	};
	
	/**
	 * Constructs a HANDSHAKE_DONE frame.
	 */
	public HandshakeDoneFrame() {
	}
	
	/**
	 * Return the default HANDSHAKE_DONE frame parser.
	 * 
	 * @return the PING frame parser
	 */
	public static IFrameParser getParser() {
		return PARSER;
	}
	
	@Override
	public FrameType getType() {
		return TYPE;
	}

	@Override
	public int getTypeValue() {
		return 0x1e;
	}

	@Override
	public int getLength() {
		return 1;
	}

	@Override
	public void getBytes(ByteBuffer dst) {
		dst.put((byte) getTypeValue());
	}
}
