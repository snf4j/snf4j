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
 * A range of packet numbers being acknowledged by the ACK frame. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class AckRange {

	private final long from;
	
	private final long to;

	/**
	 * Constructs a range of packet numbers.
	 * 
	 * @param from the maximum packet number in the range
	 * @param to   the minimum packet number in the range
	 * @throws IllegalArgumentException if {@code to} parameter is greater than
	 *                                  {@code from}
	 */
	public AckRange(long from, long to) {
		if (to > from) {
			throw new IllegalArgumentException("to is greater than from");
		}
		this.from = from;
		this.to = to;
	}

	/**
	 * Constructs a range containing only one packet number.
	 * 
	 * @param pn the packet number
	 */
	public AckRange(long pn) {
		this.from = pn;
		this.to = pn;
	}
	
	/**
	 * Returns the maximum packet number in the range
	 * 
	 * @return the maximum packet number in the range
	 */
	public long getFrom() {
		return from;
	}

	/**
	 * Returns the minimum packet number in the range
	 * 
	 * @return the minimum packet number in the range
	 */
	public long getTo() {
		return to;
	}
	
}
