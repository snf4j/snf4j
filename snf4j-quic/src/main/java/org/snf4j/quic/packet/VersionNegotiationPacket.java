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
 * A Version Negotiation packet as defined in RFC 9000. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class VersionNegotiationPacket extends LongHeaderPacket {

	private final static PacketType TYPE = PacketType.VERSION_NEGOTIATION;
	
	private final Version[] versions;
	
	private final static IPacketParser PARSER = new IPacketParser() {

		@Override
		public PacketType getType() {
			return TYPE;
		}

		@SuppressWarnings("unchecked")
		@Override
		public VersionNegotiationPacket parse(ByteBuffer src, int remaining, ParseContext context, IFrameDecoder decoder) throws QuicException {
			if (remaining > 5) {
				int len;
				
				src.position(src.position()+5);
				len = PacketUtil.getUnsigned(src);
				remaining -= 6;
				if (remaining > len) {
					byte[] destinationId = new byte[len];

					src.get(destinationId);
					remaining -= len;
					len = PacketUtil.getUnsigned(src);
					remaining--;
					if (remaining >= len) {
						byte[] sourceId = new byte[len];
						
						src.get(sourceId);
						remaining -= len;
						if ((remaining & 0x03) == 0) {
							remaining >>= 2;
							Version[] versions = new Version[remaining];
							
							for (int i=0; i<remaining; ++i) {
								versions[i] = PacketUtil.identifyVersion(src.getInt());
							}
							return new VersionNegotiationPacket(
									destinationId,
									sourceId,
									versions);		
						}
					}
				}
			}
			throw new QuicException(TransportError.PROTOCOL_VIOLATION, "Inconsistent length of Version Negotiation packet");
		}
		
		@Override
		public HeaderInfo parseHeader(ByteBuffer src, int remaining, ParseContext context) throws QuicException {
			return null;
		}		
		
		@SuppressWarnings("unchecked")
		@Override
		public VersionNegotiationPacket parse(ByteBuffer src, HeaderInfo info, ParseContext context, IFrameDecoder decoder) throws QuicException {
			return null;
		}		
	};
	
	/**
	 * Constructs a Version Negotiation packet.
	 * 
	 * @param destinationId the destination connection id
	 * @param sourceId      the source connection id
	 * @param versions      the versions supported by server
	 */
	public VersionNegotiationPacket(byte[] destinationId, byte[] sourceId, Version... versions) {
		super(destinationId, -1, sourceId, Version.V0, null);
		this.versions = versions;
	}

	@Override
	public PacketType getType() {
		return TYPE;
	}

	/**
	 * Return the default Version Negotiation packet parser.
	 * 
	 * @return the Version Negotiation packet parser
	 */
	public static IPacketParser getParser() {
		return PARSER;
	}

	@Override
	int length(int pnLength, int expansion) {
		return super.length(pnLength, expansion) + 4*versions.length;
	}
	
	@Override
	public void getBytes(long largestPn, ByteBuffer dst) {
		getHeaderBytes(largestPn,0 , dst);
	}

	@Override
	public int getHeaderBytes(long largestPn, int expansion, ByteBuffer dst) {
		dst.put((byte) 0b11000000);
		dst.putInt(version.value());
		dst.put((byte) destinationId.length);
		dst.put(destinationId);
		dst.put((byte) sourceId.length);
		dst.put(sourceId);
		for (Version version: versions) {
			dst.putInt(version.value());
		}
		return 0;
	}
	
	@Override
	public int getPayloadLength() {
		return 0;
	}

	@Override
	public void getPayloadBytes(ByteBuffer dst) {
	}

	/**
	 * Returns versions supported by server.
	 * 
	 * @return supported versions
	 */
	public Version[] getSupportedVersions() {
		return versions;
	}

}
