/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019-2022 SNF4J contributors
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
package org.snf4j.example.engine;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

import org.snf4j.core.ByteBufferArray;

public class Packet {
	
	public final static int MAX_SIZE = 1024;

	public final static int HEADER_SIZE = 4; 

	public final static int CHECKSUM_SIZE = 8; 

	public final static int MIN_SIZE = HEADER_SIZE + CHECKSUM_SIZE; 
	
	public final static int MAX_DATA = MAX_SIZE - MIN_SIZE; 
	
	private static byte[] byteArray = new byte[MAX_SIZE]; 

	private static ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray); 
	
	private final static byte[] CLOSE_DATA = "Bye!".getBytes();

	public static int calculateMaxData(int remaining) {
		return remaining - MIN_SIZE;
	}
	
	public static boolean isClose(byte[] data) {
		return Arrays.equals(CLOSE_DATA, data);
	}
	
	public static byte[] getCloseData() {
		return CLOSE_DATA;
	}
	
	public static byte[] getBytes(ByteBuffer[] srcs, int maxSize) {
		ByteBufferArray array = ByteBufferArray.wrap(srcs);
		byte[] bytes = new byte[Math.min((int)array.remaining(), maxSize)];
		array.get(bytes);
		return bytes;
	}	

	public static ByteBuffer encode(int offset, ByteBuffer[] data, int maxDataSize) {
		byte[] bytes = getBytes(data, maxDataSize);
		
		if (bytes.length > 0) {
			return encode(offset, bytes);
		}
		return null;
	}
	
	public static ByteBuffer encode(int offset, byte[] data) {
		CRC32 crc = new CRC32();
		
		byteBuffer.clear();
		byteBuffer.putInt(data.length + MIN_SIZE);
		byteBuffer.put(data);
		crc.update(byteArray, 0, byteBuffer.position());
		byteBuffer.putLong(crc.getValue());
		byteBuffer.flip();
		
		//encrypt
		int size = byteBuffer.limit();
		for (int i=0; i<size; ++i) {
			byteArray[i] += offset;
		}
		return byteBuffer;
	}

	public static int decodeSize(int offset, ByteBuffer data) {
		if (data.remaining() < MIN_SIZE) {
			return -1;
		}
		
		byteBuffer.clear();
		byteBuffer.putInt(data.duplicate().getInt());

		//decrypt size
		for (int i=0; i<4; ++i) {
			byteArray[i] -= offset;
		}

		//check size
		byteBuffer.flip();
		int size = byteBuffer.getInt();
		if (size > MAX_SIZE) {
			throw new IllegalArgumentException();
		}
		if (size > data.remaining()) {
			return -1;
		}
		return size;
	}
	
	public static byte[] decode(int offset, ByteBuffer data, int size) {
		ByteBuffer dataDuplicate = data.duplicate();
		dataDuplicate.limit(dataDuplicate.position()+size);
		byteBuffer.clear();
		byteBuffer.put(dataDuplicate);
		byteBuffer.flip();
		data.position(dataDuplicate.position());

		//decrypt
		for (int i=0; i<size; ++i) {
			byteArray[i] -= offset;
		}
		
		//validate checksum
		CRC32 crc = new CRC32();
		crc.update(byteArray, 0, size - CHECKSUM_SIZE);
		if (crc.getValue() != byteBuffer.getLong(size - CHECKSUM_SIZE)) {
			throw new IllegalArgumentException();
		}
		
		byte[] decoded = new byte[size - MIN_SIZE];
		System.arraycopy(byteArray, HEADER_SIZE, decoded, 0, decoded.length);
		return decoded;
	}
}
