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

import org.snf4j.quic.frame.IFrame;
import org.snf4j.quic.frame.MultiPaddingFrame;
import org.snf4j.quic.packet.HandshakePacket;
import org.snf4j.quic.packet.IPacket;
import org.snf4j.quic.packet.InitialPacket;
import org.snf4j.quic.packet.OneRttPacket;
import org.snf4j.quic.packet.PacketUtil;
import org.snf4j.quic.packet.ZeroRttPacket;

class CryptoFragmenterContext {
	
	private final QuicState state;
	
	final byte[] dcid;
	
	final byte[] scid;
	
	final byte[] token;
	
	int remaining;
	
	private int committed;
	
	private int packetLength;
	
	private int headerLength;
	
	EncryptionLevel level;
	
	EncryptionContext enc;
	
	PacketNumberSpace space;

	CryptoFragmenterContext(QuicState state) {
		this.state = state;
		dcid = state.getConnectionIdManager().getDestinationId();
		scid = state.getConnectionIdManager().getSourceId();
		committed = remaining = state.getMaxUdpPayloadSize();
		token = new byte[0];
	}
	
	/**
	 * Initializes the context for the given encryption level.
	 * 
	 * @param level the encryption level
	 * @return {@code true} if the encryption context for the given level is ready
	 *         for the packet protection process (i.e. has keys)
	 */
	boolean init(EncryptionLevel level) {
		this.level = level;
		if (state.getContext(level).getEncryptor() == null) {
			return false;
		}
		else {
			enc = state.getContext(level);
			space = state.getSpace(level);			

			switch(level) {
			case INITIAL:
				headerLength = 1 + 4 + 2 + dcid.length + scid.length + 4 + 4
					+ PacketUtil.encodedIntegerLength(token.length)
					+ token.length;
				break;

			case EARLY_DATA:
			case HANDSHAKE:
				headerLength = 1 + 4 + 2 + dcid.length + scid.length + 4 + 4;
				break;

			default:
				headerLength = 1 + dcid.length + 4;
			}
			headerLength += enc.getEncryptor().getExpansion();
			
			return true;
		}
	}

	/**
	 * Tries to create a new packet with the first frame to carry. If there is no
	 * enough space for it in the remaining UDP payload then no packet is created
	 * and no packet number is wasted.
	 * 
	 * @param frame the first frame to be carried by the packet.
	 * @return the created packet, or {@code null} if there is no enough space in
	 *         the remaining UDP payload
	 */
	IPacket packet(IFrame frame) {
		int frameLength = frame.getLength();
		IPacket packet = packet(frameLength);
		
		if (packet != null) {
			packet.getFrames().add(frame);
			packetLength = headerLength + frameLength;
		}
		return packet;
	}
	
	/**
	 * Tries to add the given frame to the packet that was previously created by
	 * {@link #packet(IFrame)}. If there is no enough space for the frame in the
	 * remaining UDP payload then the frame is not added to the packet.
	 * 
	 * @param packet the packet
	 * @param frame  the frame to add
	 * @return {@code true} if the frame has been added to the packet
	 */
	boolean add(IPacket packet, IFrame frame) {
		int frameLength = frame.getLength();
		
		if (remaining >= packetLength + frameLength) {
			packet.getFrames().add(frame);
			packetLength += frameLength;
			return true;
		}
		return false;
	}
	
	/**
	 * Tries to create a new packet for the given payload length. If there is no
	 * enough space for it in the remaining UDP payload then no packet is created
	 * and no packet number is wasted.
	 * 
	 * @param payloadLength the payload length
	 * @return the created packet, or {@code null} if there is no enough space in
	 *         the remaining UDP payload
	 */
	IPacket packet(int payloadLength) {
		
		if (remaining < headerLength + payloadLength) {
			return null;
		}
		
		long pn = state.getSpace(level).next();
		
		switch (level) {
		case INITIAL:
			return new InitialPacket(dcid, pn, scid, state.getVersion(), new byte[0]);
			
		case EARLY_DATA:
			return new ZeroRttPacket(dcid, pn, scid, state.getVersion());
			
		case HANDSHAKE:
			return new HandshakePacket(dcid, pn, scid, state.getVersion());
			
		default:
			return new OneRttPacket(dcid, pn, false, enc.getKeyPhaseBit());
		}
	}
	
	/**
	 * Returns the length of the given packet.
	 * 
	 * @param packet the packet
	 * @return the length of the packet
	 */
	int length(IPacket packet) {
		return packet.getLength(space.getLargestAcked(), enc.getEncryptor().getExpansion());
	}
	
	/**
	 * Adds padding to the given packet so it will fully fill the remaining UDP
	 * payload.
	 * 
	 * @param packet the packet
	 */
	void padding(IPacket packet) {
		if (remaining > 0) {
			int length = PacketUtil.encodedPacketNumberLength(
					packet.getPacketNumber(), 
					space.getLargestAcked()
					) 
					+ packet.getPayloadLength()
					+ enc.getEncryptor().getExpansion();
			
			
			int size = PacketUtil.encodedIntegerLength(length + remaining);
			int delta = size - PacketUtil.encodedIntegerLength(length);
			
			if (delta > 0) {
				remaining -= delta;
				if (size > PacketUtil.encodedIntegerLength(length + remaining)) {
					if (state.getMaxUdpPayloadSize() == PacketUtil.MIN_MAX_UDP_PAYLOAD_SIZE) {
						remaining += delta;
					}
				}
			}
			
			packet.getFrames().add(new MultiPaddingFrame(remaining));
		}
	}
	
	/**
	 * Tells if there is no space in the remaining UDP payload for a new packet.
	 * 
	 * @return {@code true} if there is no space in the remaining UDP payload for a
	 *         new packet
	 */
	boolean noRemaining() {
		return remaining < headerLength + 1;
	}
	
	/**
	 * Commits the current remaining of UDP payload. It should be called after the
	 * remaining space was reduced by the length of fully created packet.
	 */
	void commitRemaining() {
		committed = remaining;
	}
	
	/**
	 * Rolls back the current remaining of UDP payload to the previously committed
	 * value.
	 */
	void rollbackRemaining() {
		remaining = committed;;
	}
}
