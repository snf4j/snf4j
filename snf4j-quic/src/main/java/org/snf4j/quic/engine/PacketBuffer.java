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

import java.util.LinkedList;
import java.util.Queue;

/**
 * A buffer for packet data. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class PacketBuffer {
	
	private final Queue<byte[]> packets = new LinkedList<>();
	
	private final int maxSize;
	
	/**
	 * Constructs a buffer with the given maximum size.
	 * 
	 * @param maxSize the maximum size of the buffer (i.e. maximum number of
	 *                packets)
	 */
	public PacketBuffer(int maxSize) {
		this.maxSize = maxSize;
	}

	/**
	 * Puts the given packet data into this buffer. If the maximum size of this
	 * buffer is reached no other packet data is put (i.e. is discarded)
	 * 
	 * @param packet the packet data
	 */
	public void put(byte[] packet) {
		if (packets.size() < maxSize) {
			packets.add(packet);
		}
	}
	
	/**
	 * Gets and removes the oldest packet data from this buffer.
	 * 
	 * @return the oldest packet data, or {@code null} if the buffer is empty
	 */
	public byte[] get() {
		return packets.poll();
	}
	
	/**
	 * Tells if this buffer is empty.
	 * 
	 * @return {@code true} if the buffer is empty
	 */
	public boolean isEmpty() {
		return packets.isEmpty();
	}
}
