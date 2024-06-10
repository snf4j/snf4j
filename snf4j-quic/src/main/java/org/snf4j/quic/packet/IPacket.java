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
	 * Returns the maximum length of this packet. The maximum length assumes the
	 * maximum length of the packet number (i.e. 4 bytes)
	 * 
	 * @param expansion the length increase of a message when it is encrypted. It is
	 *                  ignored by packets not supporting protection.
	 * @return the maximum length of this packet
	 */
	int getMaxLength(int expansion);
	
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
	 * @param expansion the length increase of a message when it is encrypted. It is
	 *                  ignored by packets not supporting protection.
	 * @return the length of this packet
	 */
	int getLength(long largestPn, int expansion);
	
	/**
	 * Gets the bytes representing this QUIC packet and put them into the given
	 * destination byte buffer.
	 * <p>
	 * This method is only for packets that do not require the header and payload
	 * protection.
	 * 
	 * @param largestPn the largest processed/acknowledged packet number in the
	 *                  current packet number space
	 * @param dst       the destination byte buffer
	 */
	void getBytes(long largestPn, ByteBuffer dst);
	
	/**
	 * Gets the bytes representing the header of this QUIC packet up to the Packet
	 * Number field (inclusive) and put them into the given destination byte buffer.
	 * <p>
	 * This method is only for packets that require the header and payload
	 * protection.
	 * 
	 * @param largestPn the largest processed/acknowledged packet number in the
	 *                  current packet number space
	 * @param expansion the number of bytes the length of ciphertext is greater than
	 *                  the length of plaintext
	 * @param dst       the destination buffer
	 * @return the length of expected encrypted payload with the encoded packet
	 *         number, or 0 for packets not carrying frames.
	 */
	int getHeaderBytes(long largestPn, int expansion, ByteBuffer dst);
	
	/**
	 * Returns the length of the unprotected payload (i.e. the length of frames) of
	 * this packet.
	 * 
	 * @return the length of the unprotected payload, or 0 for packets not carrying
	 *         frames
	 */
	int getPayloadLength();
	
	/**
	 * Gets the bytes representing the unprotected payload (i.e. frames) of this
	 * QUIC packet and put them into the given destination byte buffer.
	 * 
	 * @param dst the destination buffer
	 */
	void getPayloadBytes(ByteBuffer dst);
	
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
