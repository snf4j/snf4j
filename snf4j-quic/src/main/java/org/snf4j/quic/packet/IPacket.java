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
import java.util.List;

import org.snf4j.quic.frame.IFrame;

/**
 * A generic interface for all QUIC packets. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IPacket {
	
	/**
	 * Returns the maximum length of this packet. The maximum length assumes the maximum length
	 * of the packet number (i.e. 4 bytes)
	 * 
	 * @return the maximum length of this packet
	 */
	int getMaxLength();
	
	/**
	 * Returns the type identifying this QUIC packet.
	 * 
	 * @return the QUIC packet type
	 */
	PacketType getType();
	
	/**
	 * Returns the length of this packet for the given largest packet number that
	 * has been successfully processed/acknowledged in the current packet number
	 * space, or -1 if no packet has been successfully processed/acknowledged yet.
	 * 
	 * @param largestPn the largest processed/acknowledged packet number in the
	 *                  current packet number space
	 * @return the length of this packet
	 */
	int getLength(long largestPn);
	
	/**
	 * Gets the bytes representing this QUIC packet and put them into the given
	 * destination byte buffer.
	 * 
	 * @param largestPn the largest processed/acknowledged packet number in the
	 *                  current packet number space
	 * @param dst       the destination byte buffer
	 */
	void getBytes(long largestPn, ByteBuffer dst);
	
	/**
	 * Returns the destination connection id.
	 * 
	 * @return the destination connection id
	 */
	byte[] getDestinationId();
	
	/**
	 * Returns the full (i.e. not truncated) packet number identifying this packet.
	 * 
	 * @return the full packet number, or -1 for packets without the packet number
	 */
	long getPacketNumber();
	
	/**
	 * Returns a list of the QUIC frames currently associated with this packet.
	 * 
	 * @return a list of the QUIC frames, or {@code null} by packets not
	 *         carrying frames in their payload
	 */
	List<IFrame> getFrames();
	
}
