/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020 SNF4J contributors
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
package org.snf4j.example.dtls;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.zip.CRC32;

class Packet {
	
	static final int CRC_SIZE = 8;
	
	static final Random RANDOM = new Random(System.currentTimeMillis());
	
	final private long sequence;
	
	final private byte[] bytes;
	
	Packet(long sequence, int size) {
		this.sequence = sequence;
		bytes = new byte[size];
		RANDOM.nextBytes(bytes);
		
		ByteBuffer b = ByteBuffer.wrap(bytes);
		CRC32 crc = new CRC32();
		
		b.clear();
		b.putInt(bytes.length);
		b.putLong(sequence);
		crc.update(bytes, 0, bytes.length - CRC_SIZE);
		b.position(bytes.length - CRC_SIZE);
		b.putLong(crc.getValue());
	}
	
	Packet(byte[] bytes) {
		this.bytes = bytes;
		
		ByteBuffer b = ByteBuffer.wrap(bytes);
		CRC32 crc = new CRC32();
		long crcValue;
		
		int size = b.getInt();
		if (size != bytes.length) {
			throw new IllegalArgumentException();
		}
		sequence = b.getLong();
		b.position(bytes.length - CRC_SIZE);
		crcValue = b.getLong();
		crc.update(bytes, 0, bytes.length - CRC_SIZE);
		if (crcValue != crc.getValue()) {
			throw new IllegalArgumentException();
		}
	}
	
	long getSequence() {
		return sequence;
	}
	
	byte[] toBytes() {
		return bytes;
	}
}
