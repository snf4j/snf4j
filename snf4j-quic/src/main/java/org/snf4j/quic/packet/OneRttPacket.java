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
import org.snf4j.quic.frame.IFrameDecoder;

/**
 * An 1-RTT packet as defined in RFC 9000. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class OneRttPacket extends Packet {

	private final static PacketType TYPE = PacketType.ONE_RTT;
	
	private final static int SPIN_BIT = 0b00100000;
	
	private final static int KEY_PHASE = 0b00000100;
	
	private final boolean spinBit;
	
	private final boolean keyPhase;
	
	private final static IPacketParser PARSER = new IPacketParser() {

		@Override
		public PacketType getType() {
			return TYPE;
		}

		@SuppressWarnings("unchecked")
		@Override
		public OneRttPacket parse(ByteBuffer src, int remaining, ParseContext context, IFrameDecoder decoder) throws QuicException {
			return parse(src, parseHeader(src, remaining, context), context, decoder);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public OneRttPacket parse(ByteBuffer src, HeaderInfo info, ParseContext context, IFrameDecoder decoder) throws QuicException {
			int bits = info.getBits();
			int pnLength = (bits & 0x03) + 1;
			
			if (info.getLength() > pnLength) {
				long pn = parsePacketNumber(src, pnLength, context);
				
				return PacketUtil.decodeFrames(new OneRttPacket(
						info.getDestinationId(),
						pn,
						(bits & SPIN_BIT) != 0,
						(bits & KEY_PHASE) != 0), 
							src, 
							info.getLength() - pnLength, 
							decoder);
			}
			throw new QuicException(TransportError.PROTOCOL_VIOLATION, "Inconsistent length of 1-RTT packet");
		}
		
		@Override
		public HeaderInfo parseHeader(ByteBuffer src, int remaining, ParseContext context) throws QuicException {
			int bits = src.get();
			int len = context.getDestinationIdLength();
			
			--remaining;
			if (remaining > len) {
				byte[] destinationId = new byte[len];
				
				src.get(destinationId);
				remaining -= len;
				return new HeaderInfo(
						bits,
						destinationId,
						remaining);
			}
			throw new QuicException(TransportError.PROTOCOL_VIOLATION, "Inconsistent length of 1-RTT packet");
		}
	};
	
	/**
	 * Constructs an 1-RTT packet.
	 * 
	 * @param destinationId the destination connection id
	 * @param packetNumber  the full packet number identifying this packet
	 * @param spinBit       the latency spin bit
	 * @param keyPhase      the key phase, which allows a recipient of a packet to
	 *                      identify the packet protection keys that are used to
	 *                      protect the packet
	 */
	public OneRttPacket(byte[] destinationId, long packetNumber, boolean spinBit, boolean keyPhase) {
		super(destinationId, packetNumber);
		this.spinBit = spinBit;
		this.keyPhase = keyPhase;
	}

	@Override
	public PacketType getType() {
		return TYPE;
	}
	
	/**
	 * Return the default 1-RTT packet parser.
	 * 
	 * @return the 1-RTT packet parser
	 */
	public static IPacketParser getParser() {
		return PARSER;
	}
	
	@Override
	int length(int pnLength, int expansion) {
		return 1 + destinationId.length + pnLength + getFramesLength() + expansion;
	}

	@Override
	public void getBytes(long largestPn, ByteBuffer dst) {
		getHeaderBytes(largestPn, 0, dst);
		getPayloadBytes(dst);
	}

	@Override
	public int getHeaderBytes(long largestPn, int expansion, ByteBuffer dst) {
		int pnLength = PacketUtil.encodedPacketNumberLength(packetNumber, largestPn);
		int firstByte = 0b01000000;
		
		if (spinBit) {
			firstByte |= SPIN_BIT;
		}
		if (keyPhase) {
			firstByte |= KEY_PHASE;
		}
		
		PacketUtil.putFirstBits(firstByte, pnLength, dst);
		dst.put(destinationId);
		PacketUtil.truncatePacketNumber(packetNumber, pnLength, dst);
		return getFramesLength() + pnLength + expansion;
	}
	
	@Override
	public int getPayloadLength() {
		return getFramesLength();
	}

	@Override
	public void getPayloadBytes(ByteBuffer dst) {
		getFramesBytes(dst);
	}
	
	/**
	 * Returns the latency spin bit.
	 * 
	 * @return {@code true} if the latency spin bit is set to 1
	 */
	public boolean getSpinBit() {
		return spinBit;
	}

	/**
	 * Returns the key phase bit, which allows a recipient of a packet to identify
	 * the packet protection keys that are used to protect the packet.
	 * 
	 * @return {@code true} if the key phase bit is set to 1
	 */
	public boolean getKeyPhase() {
		return keyPhase;
	}
	
}
