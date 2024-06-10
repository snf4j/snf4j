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
 * An Initial packet as defined in RFC 9000. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class InitialPacket extends LongHeaderPacket {

	private final static PacketType TYPE = PacketType.INITIAL;
	
	private final byte[] token;
	
	private final static IPacketParser PARSER = new IPacketParser() {

		@Override
		public PacketType getType() {
			return TYPE;
		}

		@SuppressWarnings("unchecked")
		@Override
		public InitialPacket parse(ByteBuffer src, int remaining, ParseContext context, IFrameDecoder decoder) throws QuicException {
			return parse(src, parseHeader(src, remaining, context), context, decoder);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public InitialPacket parse(ByteBuffer src, HeaderInfo info, ParseContext context, IFrameDecoder decoder) throws QuicException {
			int pnLength = (info.getBits() & 0x03) + 1;
			
			if (info.getLength() > pnLength) {
				long pn = parsePacketNumber(src, pnLength, context);
				
				return PacketUtil.decodeFrames(new InitialPacket(
						info.getDestinationId(),
						pn,
						info.getSourceId(),
						info.getVersion(),
						info.getToken()), 
							src, 
							info.getLength() - pnLength, 
							decoder);
			}
			throw new QuicException(TransportError.PROTOCOL_VIOLATION, "Inconsistent length of Initial packet");
		}

		@Override
		public HeaderInfo parseHeader(ByteBuffer src, int remaining, ParseContext context) throws QuicException {
			int bits = src.get();
			
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
						
						if (remainings[0] > llen) {
							byte[] token;

							if (llen == 0) {
								token = PacketUtil.EMPTY_ARRAY;
							}
							else {
								token = new byte[(int) llen];
								src.get(token);
								remainings[0] -= llen;
							}
							llen = PacketUtil.decodeInteger(src, remainings);
							if (remainings[0] >= llen) {
								return new HeaderInfo(
										bits,
										version,
										destinationId,
										sourceId,
										token,
										(int) llen);
							}
						}
					}
				}
			}
			throw new QuicException(TransportError.PROTOCOL_VIOLATION, "Inconsistent length of Initial packet");
		}
	};
	
	/**
	 * Constructs an Initial packet.
	 * 
	 * @param destinationId the destination connection id
	 * @param packetNumber  the full packet number identifying this packet
	 * @param sourceId      the source connection id
	 * @param version       the version of the packet
	 * @param token         the token that was previously provided in a Retry packet
	 *                      or NEW_TOKEN frame
	 */
	public InitialPacket(byte[] destinationId, long packetNumber, byte[] sourceId, Version version, byte[] token) {
		super(destinationId, packetNumber, sourceId, version);
		this.token = token;
	}

	@Override
	public PacketType getType() {
		return TYPE;
	}
	
	/**
	 * Return the default Initial packet parser.
	 * 
	 * @return the Initial packet parser
	 */
	public static IPacketParser getParser() {
		return PARSER;
	}
	
	@Override
	int length(int pnLength, int expansion) {
		int length = getFramesLength() + pnLength + expansion;
		
		return super.length(pnLength, expansion)
			+ PacketUtil.encodedIntegerLength(token.length)
			+ token.length
			+ PacketUtil.encodedIntegerLength(length)
			+ length;
	}
		
	@Override
	public void getBytes(long largestPn, ByteBuffer dst) {
		getHeaderBytes(largestPn, 0, dst);
		getPayloadBytes(dst);
	}
	
	@Override
	public int getHeaderBytes(long largestPn, int expansion, ByteBuffer dst) {
		int pnLength = PacketUtil.encodedPacketNumberLength(packetNumber, largestPn);
		int length = getFramesLength() + pnLength + expansion;
		
		PacketUtil.putFirstBits(0b11000000, pnLength, dst);
		dst.putInt(version.value());
		dst.put((byte) destinationId.length);
		dst.put(destinationId);
		dst.put((byte) sourceId.length);
		dst.put(sourceId);
		if (token.length > 0) {
			PacketUtil.encodeInteger(token.length, dst);
			dst.put(token);
		}
		else {
			PacketUtil.encodeZero(dst);
		}
		PacketUtil.encodeInteger(length, dst);
		PacketUtil.truncatePacketNumber(packetNumber, pnLength, dst);
		return length;
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
	 * Returns the token that was previously provided in a Retry packet or NEW_TOKEN
	 * frame
	 * 
	 * @return the token, or an empty array if no token is available
	 */
	public byte[] getToken() {
		return token;
	}
}
