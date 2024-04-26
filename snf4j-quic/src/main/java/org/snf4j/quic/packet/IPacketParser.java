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
package org.snf4j.quic.packet;

import java.nio.ByteBuffer;

import org.snf4j.quic.QuicException;
import org.snf4j.quic.frame.IFrameDecoder;

/**
 * A generic interface for QUIC packet parsers.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IPacketParser {
	
	/**
	 * Returns the type of the packet resulting from this parser.
	 * 
	 * @return the type of the packet
	 */
	PacketType getType();
	
	/**
	 * Parses a QUIC packet of the given type.
	 * 
	 * @param <T>       a QUIC packet
	 * @param src       the source buffer positioned on the first byte of a QUIC
	 *                  packet
	 * @param remaining the number (greater than 0) of remaining bytes in the source
	 *                  buffer
	 * @param context   the parsing context
	 * @param decoder   a QUIC frame decoder
	 * @return the parsed QUIC packet
	 * @throws QuicException if an error occurred during parsing of QUIC packet
	 */
	<T extends IPacket> T parse(ByteBuffer src, int remaining, ParseContext context, IFrameDecoder decoder) throws QuicException;
}
