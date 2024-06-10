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

import org.snf4j.quic.Version;

/**
 * A {@code class} providing additional information about QUIC packets.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class PacketInfo {

	private final static PacketInfo PACKET_INFO = new PacketInfo();
	
	private PacketInfo() {}
	
	/**
	 * Returns an instance of the packet information object for the given QUIC
	 * version.
	 * 
	 * @param version the QUIC version
	 * @return the instance of the packet information object
	 */
	public static PacketInfo of(Version version) {
		return PACKET_INFO;
	}
	
	/**
	 * Detects the long header packet type based on the first byte of a packet.
	 * 
	 * @param bits the first byte
	 * @return the detected long header packet type
	 */
	public PacketType longHeaderType(int bits) {
		switch ((bits & 0x30) >> 4) {
		case 0: return PacketType.INITIAL;
		case 1: return PacketType.ZERO_RTT;
		case 2: return PacketType.HANDSHAKE;
		default: return PacketType.RETRY;
		}
	}
}
