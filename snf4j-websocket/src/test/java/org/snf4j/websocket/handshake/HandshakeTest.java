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
package org.snf4j.websocket.handshake;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

public class HandshakeTest {
	
	byte[] bytes;
	
	protected byte[] bytes(String s, int off, int padding) {
		ByteBuffer buf = ByteBuffer.allocate(1000);
		
		for (int i=0; i<off; ++i) {
			buf.put((byte)0);
		}
		
		for (int i=0; i<s.length(); ++i) {
			char c = s.charAt(i);
			
			if (c == '|') {
				buf.put((byte) 13);
				buf.put((byte) 10);
			}
			else {
				buf.put((byte) c);
			}
		}

		for (int i=0; i<padding; ++i) {
			buf.put((byte)0);
		}
		
		buf.flip();
		bytes = new byte[buf.remaining()];
		buf.get(bytes);
		return bytes;
	}
	
	protected byte[] bytes(String s) {
		return bytes(s,0,0);
	}
	
	protected byte[] bytes() {
		return bytes;
	}
	
	protected void assertFields(HandshakeFrame f, String values) {
		StringBuilder sb = new StringBuilder();
		
		for (String n: f.getNames()) {
			sb.append(n);
			sb.append(':');
			sb.append(f.getValue(n));
			sb.append(';');
		}
		assertEquals(values, sb.toString());
	}
	

}
