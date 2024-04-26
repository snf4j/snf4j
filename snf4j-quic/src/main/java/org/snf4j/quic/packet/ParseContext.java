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

/**
 * A parsing context providing additional information for parsing of the
 * received packets.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class ParseContext {
	
	private final long largestPn;
	
	private final int destinationIdLength;

	/**
	 * Constructs an initial parsing context.
	 */
	public ParseContext() {
		largestPn = -1;
		destinationIdLength = -1;
	}

	/**
	 * Constructs an parsing context with the given largest packet number
	 * processed/acknowledged in the current packet number space.
	 * 
	 * @param largestPn largest packet number processed in the current packet number
	 *                  space, or -1 if no packet has been successfully processed
	 *                  yet.
	 */
	public ParseContext(long largestPn) {
		this.largestPn = largestPn;
		destinationIdLength = -1;
	}

	/**
	 * Constructs an parsing context with the given largest packet number processed
	 * in the current packet number space and the fixed length of the destination
	 * connection id.
	 * 
	 * @param largestPn           largest packet number processed in the current
	 *                            packet number space, or -1 if no packet has been
	 *                            successfully processed yet.
	 * @param destinationIdLength the fixed length of the destination connection id,
	 *                            or -1 the length is not determined yet
	 */
	public ParseContext(long largestPn, int destinationIdLength) {
		this.largestPn = largestPn;
		this.destinationIdLength = destinationIdLength;
	}
	
	/**
	 * Returns the largest packet number that has been successfully processed in the
	 * current packet number space, or -1 if no packet has been successfully
	 * processed yet
	 * 
	 * @return the largest packet number
	 */
	public long getLargestPn() {
		return largestPn;
	}

	/**
	 * Returns the fixed length of the destination connection id, or -1 the length is not
	 * determined yet
	 * 
	 * @return the fixed length of the destination id
	 */
	public int getDestinationIdLength() {
		return destinationIdLength;
	}

}
