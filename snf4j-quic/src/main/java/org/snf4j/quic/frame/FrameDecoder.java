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

import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;

/**
 * The default QUIC frame decoder. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class FrameDecoder implements IFrameDecoder {

	private final static int PARSERS_LENGTH = FrameType.HANDSHAKE_DONE.lastValue()+1;
	
	private final static IFrameParser[] PARSERS = new IFrameParser[PARSERS_LENGTH];
	
	/**
	 * A stateless instance of the default QUIC frame decoder.
	 */
	public final static IFrameDecoder INSTANCE = new FrameDecoder();
	
	static {
		add(PaddingFrame.getParser());
		add(PingFrame.getParser());
		add(AckFrame.getParser());
		add(CryptoFrame.getParser());
	}
	
	private static void add(IFrameParser parser) {
		int i = parser.getType().firstValue();
		int last = parser.getType().lastValue();
		
		for (; i<=last; ++i) {
			PARSERS[i] = parser;
		}
	}

	/**
	 * Constructs the default QUIC frame decoder.
	 */
	public FrameDecoder() {
	}
	
	@Override
	public <T extends IFrame> T decode(ByteBuffer src, int remaining) throws QuicException {
		if (remaining > 0) {
			int type = (int)src.get() & 0xff;
			
			if (type < PARSERS_LENGTH) {
				return PARSERS[type].parse(src, remaining-1, type);
			}
			throw new QuicException(TransportError.FRAME_ENCODING_ERROR, "Unknown frame type: " + type);
		}
		throw new QuicException(TransportError.FRAME_ENCODING_ERROR, "No data to decode");
	}
}
