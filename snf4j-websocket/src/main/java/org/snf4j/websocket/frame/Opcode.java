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
 * An <code>enum</code> that represents types of the Web Socket frames.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public enum Opcode {

	/** Denotes a continuation frame */
	CONTINUATION(0), 
	
	/** Denotes a text frame */
	TEXT(1), 
	
	/** Denotes a binary frame */
	BINARY(2), 
	
	/** Denotes a connection close frame */
	CLOSE(8),
	
	/** Denotes a ping frame */
	PING(9), 
	
	/** Denotes a pong frame */
	PONG(10);

	final static int VALUE_MASK = 0x0f;

	private int value;
	
	private final static Opcode[] MAP = new Opcode[VALUE_MASK + 1];
	
	static {
		for (Opcode opcode: Opcode.values()) {
			MAP[opcode.value()] = opcode;
		}
	}
	
	/**
	 * Finds an Opcode by its numeric value.
	 * 
	 * @param value the numeric value of the Opcode to find
	 * @return {@link Opcode} or {@code null} if no Opcode is defined for given
	 *         value
	 * @throws ArrayIndexOutOfBoundsException if the {@code value} is too big or negative
	 */
	public static Opcode findByValue(int value) {
		return MAP[value];
	}
	
	Opcode(int value) {
		this.value = value;
	}
	
	/**
	 * Returns numeric value of this Opcode.
	 * 
	 * @return the numeric value
	 */
	public int value() {
		return value;
	}
	
	/**
	 * Tells if this Opcode is for a control frame.
	 * 
	 * @return {@code true} if the Opcode is for a control frame
	 */
	public boolean isControl() {
		return value >= 8;
	}
}
