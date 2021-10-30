/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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
package org.snf4j.websocket.frame;

/**
 * Web Socket binary frame.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class BinaryFrame extends DataFrame {
	
	/**
	 * Constructs a Web Socket binary frame.
	 * 
	 * @param finalFragment determines if the created frame is the final fragment in
	 *                      a message
	 * @param rsvBits       reserved bits for extensions or future versions
	 * @param payload       payload data
	 */
	public BinaryFrame(boolean finalFragment, int rsvBits, byte[] payload) {
		super(Opcode.BINARY, finalFragment, rsvBits, payload);
	}

	/**
	 * Constructs a Web Socket binary frame. The frame is created as the final
	 * fragment in a message and with all reserved bits cleared.
	 * 
	 * @param payload payload data
	 */
	public BinaryFrame(byte[] payload) {
		super(Opcode.BINARY, payload);
	}
	
	/**
	 * Constructs an empty Web Socket binary frame. The frame is created as
	 * the final fragment in a message and with all reserved bits cleared.
	 */
	public BinaryFrame() {
		this(EMPTY_PAYLOAD);
	}
	
}
