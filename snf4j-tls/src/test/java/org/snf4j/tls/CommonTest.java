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
import java.util.Arrays;
import java.util.Random;

import org.junit.Before;

public class CommonTest {

	protected final ByteBuffer buffer = ByteBuffer.allocate(0x20010);

	private static Random RANDOM = new Random(); 
	
	public static final boolean JAVA11;

	public static final boolean JAVA8;
	
	public static final boolean JAVA15;

	static {
		double version = Double.parseDouble(System.getProperty("java.specification.version"));
		
		JAVA8 = version < 9.0;
		JAVA11 = version >= 11.0;
		JAVA15 = version >= 15.0;
	}
	
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
		
	protected byte[] bytes(String hexString) {
		byte[] bytes = new byte[hexString.length()/2];
		
		for (int i=0; i<bytes.length; ++i) {
			bytes[i] = (byte)Integer.parseInt(hexString.substring(i*2, i*2+2), 16);
		}
		return bytes;
	}
	
	protected byte[] bytes(byte[] array, int off, int len) {
		byte[] bytes = new byte[len];
		
		System.arraycopy(array, off, bytes, 0, len);
		return bytes;
	}
	
	protected byte[] bytes(int len, byte first, byte mid, byte last) {
		byte[] bytes = new byte[len];
		
		Arrays.fill(bytes, mid);
		bytes[0] = first;
		bytes[len-1] = last;
		return bytes;
	}
	
	protected byte[] cat(byte[]... arrays) {
		int len = 0;
		for (byte[] array: arrays) {
			len += array.length;
		}
		byte[] bytes = new byte[len];
		len = 0;
		for (byte[] array: arrays) {
			System.arraycopy(array, 0, bytes, len, array.length);
			len += array.length;
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
	
	protected byte[] random(int len) {
		byte[] data = new byte[len];
		
		RANDOM.nextBytes(data);
		return data;
	}

	@Before
	public void before() {
		buffer.clear();
	}

}
