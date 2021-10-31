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
package org.snf4j.websocket.longevity;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;

public class Packet {
	
	static final int LEN_SIZE = 4;
	
	static final int ID_SIZE = 8;
	
	static final int TYPE_SIZE = 1;
	
	static final int RESPOND_SIZE = 1;
	
	static final int CRC_SIZE = 8;
	
	static final int HEADER_SIZE = LEN_SIZE + ID_SIZE + TYPE_SIZE + RESPOND_SIZE + CRC_SIZE;
	
	static final int RESPOND_POS = LEN_SIZE + ID_SIZE + TYPE_SIZE;
	
	final static AtomicLong nextId = new AtomicLong(0);
	
	final static Random random = new Random(System.currentTimeMillis());

	byte[] data;

	long id;
	
	PacketType type;
	
	boolean response;
	
	public Packet(PacketType type, int length) {
		this.type = type;
		if (length < HEADER_SIZE) {
			length = HEADER_SIZE - LEN_SIZE;
		}
		id = nextId.incrementAndGet();
		data = new byte[length + LEN_SIZE];

		ByteBuffer b = ByteBuffer.wrap(data);
		b.clear();
		b.putInt(length);
		b.putLong(id);
		b.put((byte)type.ordinal());
		b.put(response ? (byte)1 : (byte)0);
		byte[] payload = new byte[data.length - HEADER_SIZE];
		random.nextBytes(payload);
		b.put(payload);
		CRC32 crc = new CRC32();
		crc.update(data, 0, data.length - CRC_SIZE);
		b.putLong(crc.getValue());
	}
	
	public Packet(byte[] data) {
		this.data = data.clone();
		ByteBuffer b = ByteBuffer.wrap(data);
		int length = b.getInt();
		if (length != b.remaining()) {
			throw new IllegalArgumentException("Packet: length != b.remaining() " + length + "!=" + b.remaining());
		}
		long c = b.getLong(data.length - CRC_SIZE);
		CRC32 crc = new CRC32();
		crc.update(data, 0, data.length - CRC_SIZE);
		if (c != crc.getValue()) {
			throw new IllegalArgumentException("Packet: c != crc.getValue()");
		}
		id = b.getLong();
		byte t = b.get();
		if (t < 0 || t >= PacketType.values().length) {
			throw new IllegalArgumentException("Packet: t < 0 || t >= PacketType.values().length");
		}
		type = PacketType.values()[t];
		t = b.get();
		if (t != 0 && t != 1) {
			throw new IllegalArgumentException("Packet: t != 0 && t != 1");
		}
		response = t == 1;
	}

	long crc() {
		CRC32 crc = new CRC32();
		crc.update(data, 0, data.length - CRC_SIZE);
		return crc.getValue();
	}
	
	void updateCrc() {
		ByteBuffer b = ByteBuffer.wrap(data);
		b.putLong(data.length - CRC_SIZE, crc());
	}
	
	public byte[] getBytes() {
		return data;
	}
	
	public void setResponse(boolean response) {
		this.response = response;
		data[RESPOND_POS] = response ? (byte)1 : 0;
		updateCrc();
	}
	
	public boolean isResponse() {
		return response;
	}
	
	public PacketType getType() {
		return type;
	}
	
	long getId() {
		return id;
	}
	
}
