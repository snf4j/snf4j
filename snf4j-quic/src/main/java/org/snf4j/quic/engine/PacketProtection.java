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
package org.snf4j.quic.engine;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.Version;
import org.snf4j.quic.frame.FrameDecoder;
import org.snf4j.quic.packet.HandshakePacket;
import org.snf4j.quic.packet.HeaderInfo;
import org.snf4j.quic.packet.ILongHeaderPacket;
import org.snf4j.quic.packet.IPacket;
import org.snf4j.quic.packet.IPacketParser;
import org.snf4j.quic.packet.InitialPacket;
import org.snf4j.quic.packet.OneRttPacket;
import org.snf4j.quic.packet.PacketType;
import org.snf4j.quic.packet.ParseContext;
import org.snf4j.quic.packet.RetryPacket;
import org.snf4j.quic.packet.VersionNegotiationPacket;
import org.snf4j.quic.packet.ZeroRttPacket;
import org.snf4j.tls.Args;

/**
 * A class providing the QUIC packet protection functionalities.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class PacketProtection {

	private final static IPacketParser[] PARSERS = new IPacketParser[PacketType.values().length];
	
	static {
		add(InitialPacket.getParser());
		add(ZeroRttPacket.getParser());
		add(HandshakePacket.getParser());
		add(OneRttPacket.getParser());
		add(RetryPacket.getParser());
		add(VersionNegotiationPacket.getParser());
	}
	
	private static void add(IPacketParser parser) {
		PARSERS[parser.getType().ordinal()] = parser;
	}

	private final PacketProtectionListener listener;
	
	/**
	 * Constructs a packet protection with the given packet protection listener.
	 * 
	 * @param listener the packet protection listener
	 */
	public PacketProtection(PacketProtectionListener listener) {
		Args.checkNull(listener, "listener");
		this.listener = listener;
	}

	/**
	 * Protects a packet and puts it into the given destination buffer. It works
	 * with all packet types for which the header and payload protection is required
	 * or not. For packets not requiring the protection it simply put into the
	 * buffer their plaintext form.
	 * 
	 * @param state  the state of the associated QUIC engine
	 * @param packet the packet to protect
	 * @param dst    the destination buffer
	 * @throws QuicException if an internal error occurred
	 */
	public void protect(QuicState state, IPacket packet, ByteBuffer dst) throws QuicException {
		EncryptionLevel level = packet.getType().encryptionLevel();

		if (level == null) {
			packet.getBytes(-1, dst);
			return;
		}
		
		EncryptionContext encCtx = state.getContext(level);
		Encryptor encryptor = encCtx.getEncryptor();
		if (encryptor == null) {
			if (level == EncryptionLevel.INITIAL && !encCtx.isErased() && state.isClientMode()) {
				listener.onInitialKeys(
						state, 
						packet.getDestinationId(), 
						((ILongHeaderPacket)packet).getVersion());
				encryptor = encCtx.getEncryptor();
			}
			if (encryptor == null) {
				throw new QuicException(TransportError.INTERNAL_ERROR, "No keys for packet protection");
			}
		}
		
		//Prepare unprotected header
		ByteBuffer dup = dst.duplicate();		
		int pos0 = dst.position();
		int length = packet.getHeaderBytes(state.getSpace(level).getLargestAcked(), encryptor.getExpansion(), dst);
		dup.limit(dst.position());
		byte[] header = new byte[dup.remaining()];
		dup.get(header);
		
		//Prepare plaintext
		ByteBuffer plaintext = ByteBuffer.allocate(length - encryptor.getExpansion() - 1);
		packet.getPayloadBytes(plaintext);
		plaintext.flip();
		
		byte[] nonce = encryptor.nonce(packet.getPacketNumber());
		int pnLen = (header[0] & 0x03) + 1;
		
		try {
			
			//Encrypt
			dup = dst.duplicate();
			encryptor.getAead().encrypt(nonce, header, plaintext, dst);

			//Retrieve sample
			byte[] sample = new byte[16];
			dup.position(dup.position() + 4 - pnLen);
			dup.limit(dst.position());
			dup.get(sample);
			
			//Protect bits
			byte[] mask = encryptor.getProtector().deriveMask(sample, 0);
			if (packet.getType().hasLongHeader()) {
				header[0] ^= mask[0] & 0x0f;
			}
			else {
				header[0] ^= mask[0] & 0x1f;
			}
			dst.put(pos0, header[0]);
			
			//Protect packet number
			for (int i=1, j=header.length-pnLen; i<=pnLen; ++i,++j) {
				dst.put(pos0+j, (byte) (header[j] ^ mask[i]));
			}
			
		} catch (Exception e) {
			throw new QuicException(TransportError.INTERNAL_ERROR, "Packet protection failed", e);
		}
	}
	
	PacketType decectLongHeaderType(int type) {
		switch (type) {
		case 0: return PacketType.INITIAL;
		case 1: return PacketType.ZERO_RTT;
		case 2: return PacketType.HANDSHAKE;
		default: return PacketType.RETRY;
		}
	}
	
	PacketType detectType(ByteBuffer src) throws QuicException {
		int pos = src.position();
		int firstByte = src.get(pos);
		
		if ((firstByte & 0x80) == 0) {
			return PacketType.ONE_RTT;
		}
		if (src.remaining() > 4) {
			if (src.getInt(pos+1) == Version.V0.value()) {
				return PacketType.VERSION_NEGOTIATION;
			}
			return decectLongHeaderType((firstByte & 0x30) >> 4);
		}
		throw new QuicException(TransportError.PROTOCOL_VIOLATION, "Unknown packet type");
	}
	
	void checkReservedBits(PacketType type, int bits) throws QuicException {
		if (type.hasLongHeader()) {
			bits &= 0x0c;
		}
		else {
			bits &= 0x18;
		}
		if (bits != 0) {
			throw new QuicException(TransportError.PROTOCOL_VIOLATION, "Non-zero reserved bits");
		}
	}
	
	/**
	 * Unprotects a packet from the given source buffer. It works for all packet
	 * types with the header and payload protected or not. For packets that are not
	 * protected it simply parses their plaintext form.
	 * 
	 * @param state the state of the associated QUIC engine
	 * @param src   the destination buffer
	 * @return a packet being unprotected, or {@code null} if the was consumed and
	 *         dropped
	 * @throws QuicException if an error occurred that does not allow to process the
	 *                       rest of the data that left in the source buffer
	 */
	public IPacket unprotect(QuicState state, ByteBuffer src) throws QuicException {
		if (src.remaining() > 1) {
			PacketType type = detectType(src);
			IPacketParser parser = PARSERS[type.ordinal()];
			EncryptionLevel level = type.encryptionLevel();

			if (level == null) {
				return parser.parse(src, src.remaining(), ParseContext.INITIAL, FrameDecoder.INSTANCE);
			}

			int pos0 = src.position();
			PacketNumberSpace space = state.getSpace(level);
			EncryptionContext encCtx = state.getContext(level);
			Decryptor decryptor = encCtx.getDecryptor();
			
			//Set parse context
			ParseContext ctx;
			if (type.hasLongHeader()) {
				ctx = new ParseContext(space.getLargestProcessed());
			}
			else {
				ctx = new ParseContext(space.getLargestProcessed(), state.getSourceId().length);
			}
			
			HeaderInfo info = parser.parseHeader(src, src.remaining(), ctx);

			if (decryptor == null) {
				int len = info.getLength() + src.position() - pos0;
				
				if (encCtx.isErased()) {
					src.position(pos0 + len);
					return null;
				}
				else if (level == EncryptionLevel.INITIAL) {
					if (state.isClientMode()) {
						src.position(pos0 + len);
						return null;						
					}
					listener.onInitialKeys(
							state, 
							info.getDestinationId(), 
							info.getVersion());
					decryptor = encCtx.getDecryptor();
				}
				else {
					byte[] data = new byte[len];

					src.position(pos0);
					src.get(data);
					encCtx.getBuffer().put(data);
					return null;
				}
			}

			try {

				//Retrieve sample
				ByteBuffer dup = src.duplicate();
				byte[] sample = new byte[16];
				dup.limit(dup.position() + info.getLength());
				dup.position(dup.position() + 4);
				if (dup.remaining() < 16) {
					src.position(src.position() + info.getLength());
					return null;
				}
				dup.get(sample);

				//Consume full packet
				dup = src.duplicate();
				src.position(src.position() + info.getLength());

				//Unprotect bits
				byte[] mask = decryptor.getProtector().deriveMask(sample, 0);
				if (type.hasLongHeader()) {
					info.unprotect(mask[0] & 0x0f, decryptor.getExpansion());
				}
				else {
					info.unprotect(mask[0] & 0x1f, decryptor.getExpansion());
				}

				//Unprotect packet number
				int pnLen = (info.getBits() & 0x03) + 1;
				ByteBuffer plaintext = ByteBuffer.allocate(info.getLength());
				for (int i=1; i<=pnLen; ++i) {
					plaintext.put((byte) (dup.get() ^ mask[i]));
				}
				plaintext.position(0);
				long pn = parser.parsePacketNumber(plaintext, pnLen, ctx);

				//Check key phase bit
				if (!type.hasLongHeader()) {
					boolean keyPhase = (info.getBits() & 0x04) != 0;

					if (encCtx.getKeyPhaseBit() != keyPhase) {
						decryptor = encCtx.getDecryptor(keyPhase, pn);
						if (decryptor == encCtx.getDecryptor(KeyPhase.NEXT)) {
							encCtx.rotateKeys();
							listener.onKeysRotation(state);
						}
					}
				}

				byte[] nonce = decryptor.nonce(pn);
				byte[] header;

				//Prepare unprotected header
				dup.flip();
				dup.position(pos0);
				header = new byte[dup.remaining()];
				dup.get(header);
				header[0] = (byte) info.getBits();
				plaintext.position(0);
				for (int i=0, j=header.length-pnLen; i<pnLen; ++i, ++j) {
					header[j] = plaintext.get();
				}

				//Decrypt
				dup.limit(dup.position() + info.getLength() - pnLen + decryptor.getExpansion());
				decryptor.getAead().decrypt(nonce, header, dup, plaintext);
				decryptor.updatePacketNumber(pn);

				checkReservedBits(type, info.getBits());

				plaintext.position(0);
				return parser.parse(plaintext, info, ctx, FrameDecoder.INSTANCE);

			} catch (GeneralSecurityException e) {
				//Discarding packet
			}
		}
		return null;
	}
}
