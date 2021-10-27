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
 * Base class for all Web Socket data frames.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class DataFrame extends Frame {

	/**
	 * Constructs a generic Web Socket data frame.
	 * 
	 * @param opcode        the type of created frame
	 * @param finalFragment determines if the created frame is the final fragment in
	 *                      a message
	 * @param rsvBits       reserved bits for extensions or future versions
	 * @param payload       payload data
	 */
	protected DataFrame(Opcode opcode, boolean finalFragment, int rsvBits, byte[] payload) {
		super(opcode, finalFragment, rsvBits, payload);
	}
	
	/**
	 * Constructs a generic Web Socket data frame being the final fragment in a
	 * message and with all reserved bits cleared.
	 * 
	 * @param opcode  the type of created frame
	 * @param payload payload data
	 */
	protected DataFrame(Opcode opcode, byte[] payload) {
		super(opcode, true, 0, payload);
	}

}
