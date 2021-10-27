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

import java.nio.charset.StandardCharsets;

/**
 * Web Socket text frame.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class TextFrame extends DataFrame {

	/**
	 * Constructs a Web Socket text frame containing binary data.
	 * <p>
	 * NOTE: The binary data should be a valid UTF-8 encoding.
	 * 
	 * @param finalFragment determines if the created frame is the final fragment in
	 *                      a message
	 * @param rsvBits       reserved bits for extensions or future versions
	 * @param payload       payload data
	 */
	public TextFrame(boolean finalFragment, int rsvBits, byte[] payload) {
		super(Opcode.TEXT, finalFragment, rsvBits, payload);
	}
	
	/**
	 * Constructs a Web Socket text frame containing text data.
	 * 
	 * @param finalFragment determines if the created frame is the final fragment in
	 *                      a message
	 * @param rsvBits       reserved bits for extensions or future versions
	 * @param text          text data
	 */
	public TextFrame(boolean finalFragment, int rsvBits, String text) {
		super(Opcode.TEXT, finalFragment, rsvBits, toBytes(text));
	}

	/**
	 * Constructs a Web Socket text frame containing binary data. The frame
	 * is created as the final fragment in a message and with all reserved bits
	 * cleared.
	 * <p>
	 * NOTE: The binary data should be a valid UTF-8 encoding.
	 * 
	 * @param payload payload data
	 */
	public TextFrame(byte[] payload) {
		super(Opcode.TEXT, payload);
	}

	/**
	 * Constructs a Web Socket text frame containing text data. The frame is
	 * created as the final fragment in a message and with all reserved bits
	 * cleared.
	 * 
	 * @param text text data
	 */
	public TextFrame(String text) {
		super(Opcode.TEXT, toBytes(text));
	}
	
	/**
	 * Constructs an empty Web Socket text frame. The frame is created as
	 * the final fragment in a message and with all reserved bits cleared.
	 */
	public TextFrame() {
		this(EMPTY_PAYLOAD);
	}
	
	/**
	 * Returns the text data in this frame.
	 * 
	 * @return the text data
	 */
	public String getText() {
		return fromBytes(getPayload());
	}
	
	static String fromBytes(byte[] bytes) {
		return new String(bytes, StandardCharsets.UTF_8);
	}
	
	static byte[] toBytes(String text) {
		if (text == null || text.isEmpty()) {
			return EMPTY_PAYLOAD;
		}
		return text.getBytes(StandardCharsets.UTF_8);
	}
}
