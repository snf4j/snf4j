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

/**
 * A generic interface for QUIC frame decoders. The decoders are responsible for
 * identifying the frame type by the first byte and use the right parser to
 * parse the frame.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IFrameDecoder {
	
	/**
	 * Encodes a QUIC frame from the given source buffer.
	 * 
	 * @param <T>       a QUIC frame
	 * @param src       the source buffer positioned on the first byte of a QUIC
	 *                  frame
	 * @param remaining the number (greater than 0) of remaining bytes in the source
	 *                  buffer
	 * @return the decoded QUIC frame
	 * @throws QuicException if an error occurred during decoding of QUIC frame, or
	 *                       the source buffer was empty
	 */
	<T extends IFrame> T decode(ByteBuffer src, int remaining) throws QuicException;
}
