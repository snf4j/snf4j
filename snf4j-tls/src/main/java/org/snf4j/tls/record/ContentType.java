/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.record;

import org.snf4j.tls.IntConstant;

public class ContentType extends IntConstant {
	
	public static final ContentType INVALID = new ContentType("invalid", 0);
	
	public static final ContentType CHANGE_CIPHER_SPEC = new ContentType("change_cipher_spec", 20);

	public static final ContentType ALERT = new ContentType("alert", 21);

	public static final ContentType HANDSHAKE = new ContentType("handshake", 22);

	public static final ContentType APPLICATION_DATA = new ContentType("application_data", 23);
     
	private final static ContentType[] KNOWN = new ContentType[APPLICATION_DATA.value()+1];
	
	private static void known(ContentType... knowns) {
		for (ContentType known: knowns) {
			KNOWN[known.value()] = known;
		}
	}
	
	static {
		known(
				INVALID,
				CHANGE_CIPHER_SPEC,
				ALERT,
				HANDSHAKE,
				APPLICATION_DATA
				);
	}
	
	protected ContentType(String name, int value) {
		super(name, value);
	}

	protected ContentType(int value) {
		super(value);
	}

	public static ContentType of(int value) {
		if (value >= 0 && value < KNOWN.length) {
			ContentType known = KNOWN[value];
			
			if (known != null) {
				return known;
			}
		}
		return new ContentType(value);
	}
	
}
