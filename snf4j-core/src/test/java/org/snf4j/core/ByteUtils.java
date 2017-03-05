/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017 SNF4J contributors
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
package org.snf4j.core;

import java.util.ArrayList;
import java.util.Arrays;

public class ByteUtils {

	/**
	 * Creates byte array with expected content.
	 * @param expectedContent contains expected content that is formatted in the following way:
	 * "bytes_count=bytes_value[,bytes_count...]"
	 */
	public static byte[] getBytes(String expectedContent) {
		ArrayList<Byte> expectedBytes = new ArrayList<Byte>();

		for (String byteCount: expectedContent.split(",")) {
			String[] split3 = byteCount.split("=");
			int count = Integer.parseInt(split3[0]);
			int value = Integer.parseInt(split3[1]);
			
			for (int i=0; i<count; ++i) {
				expectedBytes.add((byte)value);
			}
		}
		byte[] data2 = new byte[expectedBytes.size()];
		for (int i=0; i<data2.length; ++i) {
			data2[i] = expectedBytes.get(i);
		}
		return data2;
	}
	
	public static byte[] getBytes(int size, int value) {
		byte[] bytes = new byte[size];
		
		Arrays.fill(bytes, (byte)value);
		return bytes;
	}
	

}
