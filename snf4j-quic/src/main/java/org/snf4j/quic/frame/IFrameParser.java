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
 * A generic interface for QUIC frame parsers.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IFrameParser {

	/**
	 * Returns the type of the frame resulting from this parser.
	 * 
	 * @return the type of the frame
	 */
	FrameType getType();
	
	/**
	 * Parses a QUIC frame of the given type.
	 * 
	 * @param <T>       a QUIC frame
	 * @param src       the source buffer positioned on the byte right after the
	 *                  type byte (i.e. the first byte) of a QUIC frame
	 * @param remaining the number of remaining bytes in the source buffer
	 * @param type      the value of the type byte of a QUIC frame to be parsed
	 * @return the parsed QUIC frame
	 * @throws QuicException if an error occurred during parsing of QUIC frame
	 */
	<T extends IFrame> T parse(ByteBuffer src, int remaining, int type) throws QuicException;
}
