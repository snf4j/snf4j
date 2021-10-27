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
 * Base class for all Web Socket frames.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class Frame {
	
	final static byte[] EMPTY_PAYLOAD = new byte[0];
	
	private final Opcode opcode;
	
	private final boolean finalFragment;
	
	private final int rsvBits;
	
	/** Payload data */
	protected final byte[] payload;
	
	/**
	 * Constructs a generic Web Socket frame.
	 * 
	 * @param opcode        the type of created frame
	 * @param finalFragment determines if the created frame is the final fragment in
	 *                      a message
	 * @param rsvBits		reserved bits for extensions or future versions
	 * @param payload       payload data
	 */
	protected Frame(Opcode opcode, boolean finalFragment, int rsvBits, byte[] payload) {
		this.opcode = opcode;
		this.finalFragment = finalFragment;
		this.rsvBits = rsvBits;
		this.payload = payload == null ? EMPTY_PAYLOAD : payload;
	}
	
	/**
	 * Tells if this frame is the final fragment in a message.
	 * 
	 * @return {@code true} if this frame is the final fragment
	 */
	public boolean isFinalFragment() {
		return finalFragment;
	}
	
	/**
	 * Returns the reserved bits for extensions or future versions.
	 * 
	 * @return the reserved bits
	 */
	public int getRsvBits() {
		return rsvBits;
	}

	/**
	 * Tells if the reserved bit 1 is set
	 * 
	 * @return {@code true} if the reserved bit 1 is set
	 */
	public boolean isRsvBit1() {
		return (rsvBits & 4) != 0;
	}

	/**
	 * Tells if the reserved bit 2 is set
	 * 
	 * @return {@code true} if the reserved bit 2 is set
	 */
	public boolean isRsvBit2() {
		return (rsvBits & 2) != 0;
	}

	/**
	 * Tells if the reserved bit 3 is set
	 * 
	 * @return {@code true} if the reserved bit 3 is set
	 */
	public boolean isRsvBit3() {
		return (rsvBits & 1) != 0;
	}

	/**
	 * Returns the type (opcode) of this frame.
	 * 
	 * @return the type of this frame
	 */
	public Opcode getOpcode() {
		return opcode;
	}

	/**
	 * Returns the payload data in this frame.
	 * 
	 * @return the payload data
	 */
	public byte[] getPayload() {
		return payload;
	}
	
	/**
	 * Return the length of the payload data in this frame.
	 * 
	 * @return the lenght of the payload data
	 */
	public int getPayloadLength() {
		return payload.length;
	}
	
}
