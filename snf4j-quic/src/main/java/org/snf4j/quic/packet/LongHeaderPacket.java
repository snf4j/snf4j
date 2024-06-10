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

import java.util.List;

import org.snf4j.quic.Version;
import org.snf4j.quic.frame.IFrame;

abstract class LongHeaderPacket extends Packet implements ILongHeaderPacket {

	final byte[] sourceId;
	
	final Version version;
	
	LongHeaderPacket(byte[] destinationId, long packetNumber, byte[] sourceId, Version version) {
		super(destinationId, packetNumber);
		this.sourceId = sourceId;
		this.version = version;
	}

	LongHeaderPacket(byte[] destinationId, long packetNumber, byte[] sourceId, Version version, List<IFrame> frames) {
		super(destinationId, packetNumber, frames);
		this.sourceId = sourceId;
		this.version = version;
	}
	
	@Override
	int length(int pnLength, int expansion) {
		return super.length(pnLength, expansion) + 4 + 1 + sourceId.length;
	}
	
	@Override
	public byte[] getSourceId() {
		return sourceId;
	}
	
	@Override
	public Version getVersion() {
		return version;
	}
}
