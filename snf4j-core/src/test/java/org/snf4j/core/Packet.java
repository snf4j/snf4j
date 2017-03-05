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

public class Packet {
	PacketType type;
	String payload;

	public Packet(PacketType type, String payload) {
		this.type = type;
		this.payload = payload;
	}
	
	public Packet(PacketType type) {
		this(type, "");
	}
	
	static int toRead(byte[] buffer, int off, int len) {
		if (len >= 3) {
			int expected = (((int)buffer[0] << 8) & 0xff00) | ((int)buffer[1] & 0xff);
			
			if (expected <= len) {
				return expected;
			}
		}
		return 0;
	}
	
	static Packet fromBytes(byte[] data) {
		byte t = data[2];
		
		return new Packet(PacketType.values()[t], new String(data, 3, data.length - 3));
	}
	
	byte[] toBytes() {
		byte[] payload = this.payload.getBytes();
		byte[] data = new byte[3 + payload.length];
		int len = 3 + payload.length;
	
		data[0] = (byte) (len >>> 8);
		data[1] = (byte) len;
		data[2] = (byte) type.ordinal();
		System.arraycopy(payload, 0, data, 3, payload.length);
		return data;
	}
}
