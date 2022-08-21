/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022 SNF4J contributors
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
package org.snf4j.tls;

import java.nio.ByteBuffer;

import org.junit.Before;

public class CommonTest {

	protected final ByteBuffer buffer = ByteBuffer.allocate(0x20010);
	
	protected byte[] buffer() {
		ByteBuffer dup = buffer.duplicate();
		dup.flip();
		byte[] bytes = new byte[dup.remaining()];
		dup.get(bytes);
		return bytes;
	}
	
	protected byte[] buffer(int off, int len) {
		byte[] subbytes = new byte[len];
		System.arraycopy(buffer(), off, subbytes, 0, len);
		return subbytes;
	}

	protected byte[] bytes(int... values) {
		byte[] bytes = new byte[values.length];
		int i = 0;
		
		for (int value: values) {
			bytes[i++] = (byte)value;
		}
		return bytes;
	}
		
	protected ByteBuffer[] array(byte[] bytes, int off, int... sizes) {
		ByteBuffer[] array = new ByteBuffer[sizes.length + 1];
		int len = bytes.length;
		int i=0;
		
		for (; i<sizes.length; ++i) {
			byte[] a = new byte[sizes[i]];
			System.arraycopy(bytes, off, a, 0, a.length);
			array[i] = ByteBuffer.wrap(a);
			off += sizes[i];
		}
		byte[] a = new byte[len-off];
		System.arraycopy(bytes, off, a, 0, a.length);
		array[i] = ByteBuffer.wrap(a);
		return array;
	}
	
	@Before
	public void before() {
		buffer.clear();
	}

}
