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
 * A CRYPTO frame as defined in RFC 9000. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class CryptoFrame implements IFrame {

	private final static FrameType TYPE = FrameType.CRYPTO;
	
	private final long offset;
	
	private final ByteBuffer data;
	
	private final static IFrameParser PARSER = new IFrameParser() {

		@Override
		public FrameType getType() {
			return TYPE;
		}

		@SuppressWarnings("unchecked")
		@Override
		public CryptoFrame parse(ByteBuffer src, int remaining, int type) throws QuicException {
			if (remaining > 1) {
				int[] remainings = new int[] {remaining};
				long offset = FrameUtil.decodeInteger(src, remainings);
				
				if (remainings[0] > 1) {
					long length = FrameUtil.decodeInteger(src, remainings);
					
					if (remainings[0] >= length) {
						byte[] data = new byte[(int) length];
						
						src.get(data);
						return new CryptoFrame(offset, ByteBuffer.wrap(data));
					}
				}
			}
			throw new QuicException(TransportError.FRAME_ENCODING_ERROR, "Inconsistent length of Crypto frame");
		}
		
	};
	
	/**
	 * Constructs a CRYPTO frame with the given offset and cryptographic data.
	 * 
	 * @param offset the byte offset in the stream for the cryptographic data in
	 *               this CRYPTO frame
	 * @param data the cryptographic data
	 */
	public CryptoFrame(long offset, ByteBuffer data) {
		this.offset = offset;
		this.data = data;
	}

	/**
	 * Return the default CRYPTO frame parser.
	 * 
	 * @return the CRYPTO frame parser
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
		return 0x06;
	}
	
	@Override
	public int getLength() {
		return 1 
			+ FrameUtil.encodedIntegerLength(offset)
			+ FrameUtil.encodedIntegerLength(data.remaining())
			+ data.remaining();
	}

	@Override
	public void getBytes(ByteBuffer dst) {
		dst.put((byte) getTypeValue());
		FrameUtil.encodeInteger(offset, dst);
		FrameUtil.encodeInteger(data.remaining(), dst);
		dst.put(data.duplicate());
	}

	/**
	 * Returns the byte offset in the stream for the cryptographic data in this
	 * CRYPTO frame.
	 * 
	 * @return the byte offset in the stream for the cryptographic data
	 */
	public long getDataOffset() {
		return offset;
	}

	/**
	 * Returns the length of the cryptographic data in this CRYPTO frame.
	 * 
	 * @return the length of the cryptographic data
	 */
	public int getDataLength() {
		return data.remaining();
	}

	/**
	 * Returns the cryptographic data in this CRYPTO frame.
	 * 
	 * @return the cryptographic data
	 */
	public ByteBuffer getData() {
		return data;
	}

}
