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
package org.snf4j.quic.engine;

import org.snf4j.quic.Version;

/**
 * A {@code class} providing the initial salt.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class InitialSalt {

	private final static byte[] SALT = new byte[] {
			0x38, 0x76, 0x2c, (byte)0xf7, (byte)0xf5, 0x59, 0x34, (byte)0xb3, 0x4d, 
			0x17, (byte)0x9a, (byte)0xe6, (byte)0xa4, (byte)0xc8, 0x0c, (byte)0xad, 
			(byte)0xcc, (byte)0xbb, (byte)0x7f, (byte)0x0a};
	
	private InitialSalt() {}
	
	/**
	 * Returns a copy of the initial salt for the given QUIC version.
	 * 
	 * @param version the QUIC version
	 * @return a copy of the initial salt
	 */
	public static byte[] of(Version version) {
		return SALT.clone();
	}
}
