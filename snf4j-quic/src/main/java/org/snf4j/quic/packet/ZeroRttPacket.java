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

import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.Version;
import org.snf4j.quic.frame.IFrameDecoder;

/**
 * A 0-RTT packet as defined in RFC 9000. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class ZeroRttPacket extends LongHeaderPacket {
	
	private final static PacketType TYPE = PacketType.ZERO_RTT;
	
	private final static IPacketParser PARSER = new IPacketParser() {

		@Override
		public PacketType getType() {
			return TYPE;
		}

		@SuppressWarnings("unchecked")
		@Override
		public ZeroRttPacket parse(ByteBuffer src, int remaining, ParseContext context, IFrameDecoder decoder) throws QuicException {
			int firstByte = src.get();
			int pnLength;
			
			PacketUtil.checkReservedBits(firstByte);
			pnLength = (firstByte & 0x03) + 1;
			--remaining;
			if (remaining > 4) {
				Version version = PacketUtil.identifyVersion(src.getInt());
				int len = PacketUtil.getUnsigned(src);
				
				remaining -= 5;
				if (remaining > len) {
					byte[] destinationId = new byte[len];
					
					src.get(destinationId);
					remaining -= len;
					len = PacketUtil.getUnsigned(src);
					--remaining;
					if (remaining > len) {
						byte[] sourceId = new byte[len];
						
						src.get(sourceId);
						remaining -= len;
						
						int[] remainings = new int[] {remaining};
						long llen = PacketUtil.decodeInteger(src, remainings);

						remaining = remainings[0];
						if (remaining >= llen && llen > pnLength) {
							long pn = PacketUtil.decodePacketNumber(src, pnLength, context.getLargestPn());
							
							remaining -= pnLength;
							return PacketUtil.decodeFrames(new ZeroRttPacket(
									destinationId,
									pn,
									sourceId,
									version), src, remaining, decoder);
						}
					}
				}
			}
			throw new QuicException(TransportError.PROTOCOL_VIOLATION, "Inconsistent length of 0-RTT packet");
		}
	};
	
	/**
	 * Constructs a 0-RTT packet.
	 * 
	 * @param destinationId the destination connection id
	 * @param packetNumber  the full packet number identifying this packet
	 * @param sourceId      the source connection id
	 * @param version       the version of the packet
	 */
	public ZeroRttPacket(byte[] destinationId, long packetNumber, byte[] sourceId, Version version) {
		super(destinationId, packetNumber, sourceId, version);
	}

	@Override
	public PacketType getType() {
		return TYPE;
	}
	
	/**
	 * Return the default 0-RTT packet parser.
	 * 
	 * @return the Handshake packet parser
	 */
	public static IPacketParser getParser() {
		return PARSER;
	}
	
	@Override
	int length(int pnLength) {
		int length = getFramesLength() + pnLength;
		
		return super.length(pnLength)
			+ PacketUtil.encodedIntegerLength(length)
			+ length;
	}
	
	@Override
	public void getBytes(long largestPn, ByteBuffer dst) {
		int pnLength = PacketUtil.encodedPacketNumberLength(packetNumber, largestPn);
		int length = getFramesLength() + pnLength;
		
		PacketUtil.putFirstBits(0b11010000, pnLength, dst);
		dst.putInt(version.value());
		dst.put((byte) destinationId.length);
		dst.put(destinationId);
		dst.put((byte) sourceId.length);
		dst.put(sourceId);
		PacketUtil.encodeInteger(length, dst);
		PacketUtil.truncatePacketNumber(packetNumber, pnLength, dst);
		getFramesBytes(dst);
	}

}
