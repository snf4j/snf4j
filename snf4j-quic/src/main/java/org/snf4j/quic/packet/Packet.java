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
package org.snf4j.quic.packet;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.snf4j.quic.frame.IFrame;

abstract class Packet implements IPacket {

	final byte[] destinationId;
	
	final long packetNumber;
	
	final List<IFrame> frames;
	
	Packet(byte[] destinationId, long packetNumber) {
		this(destinationId, packetNumber, new LinkedList<>());
		
	}

	Packet(byte[] destinationId, long packetNumber, List<IFrame> frames) {
		this.destinationId = destinationId;
		this.packetNumber = packetNumber;
		this.frames = frames;
	}
	
	@Override
	public byte[] getDestinationId() {
		return destinationId;
	}

	@Override
	public long getPacketNumber() {
		return packetNumber;
	}

	@Override
	public List<IFrame> getFrames() {
		return frames;
	}

	int length(int pnLength, int expansion) {
		return 1 + 1 + destinationId.length;
	}
	
	@Override
	public int getMaxLength(int expansion) {
		return length(4, expansion);
	}

	@Override
	public int getLength(long largestPn, int expansion) {
		return length(PacketUtil.encodedPacketNumberLength(packetNumber, largestPn), expansion);
	}
		
	int getFramesLength() {
		int length = 0;
		
		for (IFrame frame: frames) {
			length += frame.getLength();
		}
		return length;
	}
	
	void getFramesBytes(ByteBuffer dst) {
		for (IFrame frame: frames) {
			frame.getBytes(dst);
		}
	}
}
