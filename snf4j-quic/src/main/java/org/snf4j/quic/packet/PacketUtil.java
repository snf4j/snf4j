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

import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.Version;
import org.snf4j.quic.frame.IFrame;
import org.snf4j.quic.frame.IFrameDecoder;

class PacketUtil {
	
	private PacketUtil() {}
	
	static final byte[] EMPTY_ARRAY = new byte[0];
	
	/**
	 * Decodes QUIC frames from the given source buffer and associates them with the
	 * QUIC packet.
	 * 
	 * @param <T>       a QUIC frame
	 * @param packet    the QUIC packet the decoded frames should associated with
	 * @param src       the source buffer
	 * @param remaining the number (greater than 0) of remaining bytes in the source
	 *                  buffer
	 * @param decoder   the decoder that should be used to decode frames
	 * @return the packet passed in the arguments
	 * @throws QuicException if an error occurred during decoding of QUIC frames, or
	 *                       the source buffer was empty
	 */
	static <T extends IPacket> T decodeFrames(T packet, ByteBuffer src, int remaining, IFrameDecoder decoder) throws QuicException {
		int left = src.remaining() - remaining;
		List<IFrame> frames = packet.getFrames();
		
		do {
			frames.add(decoder.decode(src, remaining));
			remaining = src.remaining() - left;
		} while (remaining > 0);
		return packet;
	}
	
	/**
	 * Puts the bits of the first byte merged with the length of the decoded packet
	 * number to the destination buffer.
	 * 
	 * @param bits     the first bits
	 * @param pnLength the length (1-4) of the decoded packet number
	 * @param dst      the destination buffer
	 */
	static void putFirstBits(int bits, int pnLength, ByteBuffer dst) {
		dst.put((byte) (bits | (pnLength - 1)));
	}
	
	/**
	 * Gets the unsigned byte from the source buffer.
	 * 
	 * @param src the source buffer
	 * @return the unsigned byte from the source buffer
	 */
	static int getUnsigned(ByteBuffer src) {
		return src.get() & 0xff;
	}
	
	/**
	 * Identifies the QUIC version from the given version value.
	 * 
	 * @param version the version value
	 * @return version identifier, or {@code null} if the given version value is not
	 *         supported
	 */
	static Version identifyVersion(int version) {
		switch (version) {
		case 0:	return Version.V0;
		case 1: return Version.V1;
		}
		return null;
	}
	
	/**
	 * Checks if reserved bits (0x0c) in the first byte of a QUIC packet are
	 * cleared.
	 * 
	 * @param bits the bits from the first byte
	 * @throws QuicException if any of reserved bits in the first byte of a QUIC
	 *                       packet is set.
	 */
	static void checkReservedBits(int bits) throws QuicException {
		if ((bits & 0x0c) != 0) {
			throw new QuicException(TransportError.PROTOCOL_VIOLATION, "Invalid reserved bits");
		}
	}

	/**
	 * Checks if reserved bits (0x18) in the first byte of a QUIC packet are
	 * cleared.
	 * 
	 * @param bits the bits from the first byte
	 * @throws QuicException if any of reserved bits in the first byte of a QUIC
	 *                       packet is set.
	 */
	static void checkReservedBits18(int bits) throws QuicException {
		if ((bits & 0x18) != 0) {
			throw new QuicException(TransportError.PROTOCOL_VIOLATION, "Invalid reserved bits");
		}
	}
	
	/**
	 * Returns the length of encoded variable-length integer.
	 * 
	 * @param i the value to encode
	 * @return the length of encoded variable-length integer
	 * @throws IllegalArgumentException if the given value is out of range for
	 *                                  62-bit unsigned integers
	 */
	static int encodedIntegerLength(long i) {
		if (i <= 63L) {
			if (i < 0) {
				throw new IllegalArgumentException("Negative variable-length integer");
			}
			return 1;
		}
		if (i <= 16383L) return 2;
		if (i <= 1073741823L) return 4;
		if (i <= 4611686018427387903L) return 8;
		throw new IllegalArgumentException("Invalid variable-length integer");
	}

	/**
	 * Encodes the given value as a variable-length integer.
	 * 
	 * @param i   the value to encode
	 * @param dst the destination buffer where the encoded 0 should be put in
	 * @throws IllegalArgumentException if the given value is out of range for
	 *                                  62-bit unsigned integers
	 */
	static void encodeInteger(long i, ByteBuffer dst) {
		if (i <= 63L) {
			if (i < 0) {
				throw new IllegalArgumentException("Negative variable-length integer");
			}
			dst.put((byte) i);
		}
		else if (i <= 16383L) {
			dst.putShort((short) (i | 0x4000));
		}
		else if (i <= 1073741823L) {
			dst.putInt((int) (i | 0x80000000));
		}
		else if (i <= 4611686018427387903L) {
			dst.putLong(i | 0xc000000000000000L);
		}
		else {
			throw new IllegalArgumentException("Invalid variable-length integer");
		}
	}

	/**
	 * Encodes 0 as a variable-length integer.
	 * 
	 * @param dst the destination buffer where the encoded 0 should be put in
	 */
	static void encodeZero(ByteBuffer dst) {
		dst.put((byte) 0);
	}
	
	/**
	 * Decodes variable-length integer from the source buffer.
	 * 
	 * @param src       the source buffer
	 * @param remaining a one-byte array with the number (greater than 0) of
	 *                  remaining bytes in the source buffer. The first element will
	 *                  be decreased by the number of bytes consumed from the source
	 *                  buffer.
	 * @return the decoded variable-length integer
	 * @throws QuicException if the are not enough data in the buffer to decode a
	 *                       variable-length integer
	 */
	static long decodeInteger(ByteBuffer src, int[] remaining) throws QuicException {
		int i = (src.get(src.position()) >> 6) & 0x03;
		
		switch (i) {
		case 0:
			remaining[0]--;
			return src.get();
			
		case 1:
			if (remaining[0] >= 2) {
				remaining[0] -= 2;
				return src.getShort() & 0x3fff;
			}
			break;
			
		case 2:
			if (remaining[0] >= 4) {
				remaining[0] -= 4;
				return src.getInt() & 0x3fffffff;
			}
			break;

		default:
			if (remaining[0] >= 8) {
				remaining[0] -= 8;
				return src.getLong() & 0x3fffffffffffffffL;
			}
			break;
		}
		throw new QuicException(TransportError.PROTOCOL_VIOLATION, "Invalid variable-length integer");
	}
		
	/**
	 * Returns the length of encoded packet number.
	 * 
	 * @param pn        the full packet number to encode
	 * @param largestPn the largest packet number that has been acknowledged by the
	 *                  peer in the current packet number space, or -1 if no packet
	 *                  has been acknowledged yet.
	 * @return the length of encoded packet number
	 */
	static int encodedPacketNumberLength(long pn, long largestPn) {
		int minBits = 64 - Long.numberOfLeadingZeros(largestPn == -1 
				? pn + 1
				: pn - largestPn);
		return (minBits + 7) >> 3;
	}
	
	/**
	 * Truncates the full packet number to the given length.
	 * 
	 * @param pn        the full packet number to truncate
	 * @param largestPn the length of the packet number after truncation
	 * @param dst       the destination buffer where the truncated packet number
	 *                  should be put in
	 */
	static void truncatePacketNumber(long pn, int length, ByteBuffer dst) {
		switch (length) {
		case 1:
			dst.put((byte) pn);
			break;
			
		case 2:
			dst.putShort((short) pn);
			break;
			
		case 3:
			dst.put((byte) (pn >> 16));
			dst.putShort((short) pn);
			break;
			
		case 4:
			dst.putInt((int) pn);
			break;
			
		default:
			throw new IllegalArgumentException("Illegal packet numbers to encode");
		}
	}
	
	/**
	 * Encodes the given packet number.
	 * 
	 * @param pn        the full packet number of the packet being sent
	 * @param largestPn the largest packet number that has been acknowledged by the
	 *                  peer in the current packet number space, or -1 if no packet
	 *                  has been acknowledged yet.
	 * @param dst       the destination buffer where the encoded packet number
	 *                  should be put in
	 * @return the length in bytes of encoded packet number
	 */
	static int encodePacketNumber(long pn, long largestPn, ByteBuffer dst) {
		int length = encodedPacketNumberLength(pn, largestPn);
		
		truncatePacketNumber(pn, length, dst);
		return length;
	}
	
	/**
	 * Decodes the packet number from the given byte buffer.
	 * 
	 * @param src       the source buffer with the encoded packet number
	 * @param length    the length in bytes of the encoded packet number
	 * @param largestPn the largest packet number that has been successfully
	 *                  processed in the current packet number space, or -1 if no
	 *                  packet has been successfully processed yet
	 * @return the decoded packet number
	 */
	static long decodePacketNumber(ByteBuffer src, int length, long largestPn) {
		long truncatedPn;
		long pnWin;
		
		switch (length) {
		case 1:
			pnWin = 1L << 8;
			truncatedPn = (long)src.get() & 0xffL; 
			break;
			
		case 2:
			pnWin = 1L << 16;
			truncatedPn = (long)src.getShort() & 0xffffL; 
			break;
			
		case 3:
			pnWin = 1L << 24;
			truncatedPn = ((long)src.get() & 0xff) << 16;
			truncatedPn |= (long)src.getShort() & 0xffffL;
			break;

		case 4:
			pnWin = 1L << 32;
			truncatedPn = (long)src.getInt() & 0xffffffffL; 
			break;
			
		default:
			throw new IllegalArgumentException("Illegal length of packet number to decode");
		}
		
		if (largestPn == -1) {
			return truncatedPn;
		}
		
		long expectedPn = largestPn + 1;
		long pnHWin = pnWin >> 1;
		long pnMask = pnWin - 1;
		long candidatePn = (expectedPn & ~pnMask) | truncatedPn;
		
		if (candidatePn <= expectedPn - pnHWin && candidatePn < (1L << 62) - pnWin) {
			return candidatePn + pnWin;
		}
		if (candidatePn > expectedPn + pnHWin && candidatePn >= pnWin) {
			return candidatePn - pnWin;
		}
		return candidatePn;
	}
}
