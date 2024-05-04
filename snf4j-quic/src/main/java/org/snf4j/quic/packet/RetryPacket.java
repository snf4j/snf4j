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
import org.snf4j.tls.Args;

/**
 * A Retry packet as defined in RFC 9000. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class RetryPacket extends LongHeaderPacket {

	private final static PacketType TYPE = PacketType.RETRY;
	
	private final static int INTEGRITY_TAG_LENGTH = 128/8;
	
	private final byte[] token;
	
	private final byte[] integrityTag;
	
	private final static IPacketParser PARSER = new IPacketParser() {

		@Override
		public PacketType getType() {
			return TYPE;
		}

		@SuppressWarnings("unchecked")
		@Override
		public RetryPacket parse(ByteBuffer src, int remaining, ParseContext context, IFrameDecoder decoder) throws QuicException {
			src.get();
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
						if (remaining >= INTEGRITY_TAG_LENGTH) {
							len = remaining - INTEGRITY_TAG_LENGTH;
							byte[] token, tag;
							
							if (len == 0) {
								token = PacketUtil.EMPTY_ARRAY;
							}
							else {
								token = new byte[len];
								src.get(token);
							}
							
							tag = new byte[INTEGRITY_TAG_LENGTH];
							src.get(tag);
							return new RetryPacket(
									destinationId, 
									sourceId,
									version,
									token,
									tag);
						}
					}
				}
			}
			throw new QuicException(TransportError.PROTOCOL_VIOLATION, "Inconsistent length of Initial packet");
		}
		
		@Override
		public HeaderInfo parseHeader(ByteBuffer src, int remaining, ParseContext context) throws QuicException {
			return null;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public RetryPacket parse(ByteBuffer src, HeaderInfo info, ParseContext context, IFrameDecoder decoder) throws QuicException {
			return null;
		}
	};
	
	/**
	 * Constructs a Retry packet.
	 * 
	 * @param destinationId the destination connection id
	 * @param sourceId      the source connection id
	 * @param version       the version of the packet
	 * @param token         the opaque token that the server can use to validate the
	 *                      client's address
	 * @param integrityTag  the retry integrity tag
	 */
	public RetryPacket(byte[] destinationId, byte[] sourceId, Version version, byte[] token, byte[] integrityTag) {
		super(destinationId, -1, sourceId, version, null);
		Args.checkFixed(integrityTag, INTEGRITY_TAG_LENGTH, "integrityTag");
		this.token = token;
		this.integrityTag = integrityTag;
	}

	@Override
	public PacketType getType() {
		return TYPE;
	}
	
	/**
	 * Return the default Retry packet parser.
	 * 
	 * @return the Retry packet parser
	 */
	public static IPacketParser getParser() {
		return PARSER;
	}
	
	@Override
	int length(int pnLength) {
		return super.length(pnLength)
			+ token.length
			+ INTEGRITY_TAG_LENGTH;
	}
	
	@Override
	public void getBytes(long largestPn, ByteBuffer dst) {
		getHeaderBytes(largestPn, 0, dst);
	}

	@Override
	public int getHeaderBytes(long largestPn, int expansion, ByteBuffer dst) {
		dst.put((byte) 0b11110000);
		dst.putInt(version.value());
		dst.put((byte) destinationId.length);
		dst.put(destinationId);
		dst.put((byte) sourceId.length);
		dst.put(sourceId);
		if (token.length > 0) {
			dst.put(token);
		}
		dst.put(integrityTag);
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
	 * Returns the opaque token that the server can use to validate the client's
	 * address.
	 * 
	 * @return the opaque token, or an empty array if no token is used
	 */
	public byte[] getToken() {
		return token;
	}

	/**
	 * Returns the retry integrity tag.
	 * 
	 * @return the retry integrity tag
	 */
	public byte[] getIntegrityTag() {
		return integrityTag;
	}
	
}
