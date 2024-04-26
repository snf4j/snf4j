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

class FrameUtil {

	private FrameUtil() {}
	
	/**
	 * Returns the length of encoded variable-length integer.
	 * 
	 * @param i the value to encode
	 * @return the length of encoded variable-length integer
	 * @throws IllegalArgumentException if the given value is out of range for
	 *                                  62-bit unsigned integers
	 */
	static int encodedIntegerLength(long i) {
		if (i <= 63L) {
			if (i < 0) {
				throw new IllegalArgumentException("Negative variable-length integer");
			}
			return 1;
		}
		if (i <= 16383L) return 2;
		if (i <= 1073741823L) return 4;
		if (i <= 4611686018427387903L) return 8;
		throw new IllegalArgumentException("Invalid variable-length integer");
	}
	
	/**
	 * Encodes the given value as a variable-length integer.
	 * 
	 * @param i   the value to encode
	 * @param dst the destination buffer where the encoded 0 should be put in
	 * @throws IllegalArgumentException if the given value is out of range for
	 *                                  62-bit unsigned integers
	 */
	static void encodeInteger(long i, ByteBuffer dst) {
		if (i <= 63L) {
			if (i < 0) {
				throw new IllegalArgumentException("Negative variable-length integer");
			}
			dst.put((byte) i);
		}
		else if (i <= 16383L) {
			dst.putShort((short) (i | 0x4000));
		}
		else if (i <= 1073741823L) {
			dst.putInt((int) (i | 0x80000000));
		}
		else if (i <= 4611686018427387903L) {
			dst.putLong(i | 0xc000000000000000L);
		}
		else {
			throw new IllegalArgumentException("Invalid variable-length integer");
		}
	}
	
	/**
	 * Decodes variable-length integer from the source buffer.
	 * 
	 * @param src       the source buffer
	 * @param remaining a one-byte array with the number (greater than 0) of
	 *                  remaining bytes in the source buffer. The first element will
	 *                  be decreased by the number of bytes consumed from the source
	 *                  buffer.
	 * @return the decoded variable-length integer
	 * @throws QuicException if the are not enough data in the buffer to decode a
	 *                       variable-length integer
	 */
	static long decodeInteger(ByteBuffer src, int[] remaining) throws QuicException {
		int i = (src.get(src.position()) >> 6) & 0x03;
		
		switch (i) {
		case 0:
			remaining[0]--;
			return src.get();
			
		case 1:
			if (remaining[0] >= 2) {
				remaining[0] -= 2;
				return src.getShort() & 0x3fff;
			}
			break;
			
		case 2:
			if (remaining[0] >= 4) {
				remaining[0] -= 4;
				return src.getInt() & 0x3fffffff;
			}
			break;

		default:
			if (remaining[0] >= 8) {
				remaining[0] -= 8;
				return src.getLong() & 0x3fffffffffffffffL;
			}
			break;
		}
		throw new QuicException(TransportError.FRAME_ENCODING_ERROR, "Invalid variable-length integer");
	}


}
