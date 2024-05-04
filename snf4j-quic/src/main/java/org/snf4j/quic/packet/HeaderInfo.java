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
 * Header information of a parsed QUIC packet.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class HeaderInfo {
	
	private int bits;
	
	private Version version;
	
	private byte[] destinationId;
	
	private byte[] sourceId;
	
	private byte[] token;
	
	private int length;
	
	private boolean unprotected;
	
	/**
	 * Constructs header information with values of the fields from the Initial
	 * packet.
	 * 
	 * @param bits          all bits in the first byte
	 * @param version       the version
	 * @param destinationId the destination connection id
	 * @param sourceId      the source connection id
	 * @param token         the token
	 * @param length        the value in the length field
	 */
	public HeaderInfo(int bits, Version version, byte[] destinationId, byte[] sourceId, byte[] token, int length) {
		this.bits = bits;
		this.version = version;
		this.destinationId = destinationId;
		this.sourceId = sourceId;
		this.token = token;
		this.length = length;
	}

	/**
	 * Constructs header information with values of the fields from the Handshake or
	 * 0-RTT packet.
	 * 
	 * @param bits          all bits in the first byte
	 * @param version       the version
	 * @param destinationId the destination connection id
	 * @param sourceId      the source connection id
	 * @param length        the value in the length field
	 */
	public HeaderInfo(int bits, Version version, byte[] destinationId, byte[] sourceId, int length) {
		this.bits = bits;
		this.version = version;
		this.destinationId = destinationId;
		this.sourceId = sourceId;
		this.length = length;
	}

	/**
	 * Constructs header information with values of the fields from the 1-RTT
	 * packet.
	 * 
	 * @param bits          all bits in the first byte
	 * @param destinationId the destination connection id
	 * @param length        the value in the length field
	 */
	public HeaderInfo(int bits, byte[] destinationId, int length) {
		this.bits = bits;
		this.destinationId = destinationId;
		this.length = length;
	}
	
	/**
	 * Unprotects the protected bits in the first byte and updates the length to
	 * match the length in the packet with the unprotected payload.
	 * <p>
	 * NOTE: This method does nothing if this header information is already
	 * unprotected.
	 * 
	 * @param bitsMask  masked bits from the first byte of the header protection
	 *                  mask
	 * @param expansion the number of bytes the length of ciphertext is greater than
	 *                  the length of plaintext
	 */
	public void unprotect(int bitsMask, int expansion) {
		if (!unprotected) {
			bits ^= bitsMask;
			length -= expansion;
			unprotected = true;
		}
	}
	
	/**
	 * Tells if this header information has been unprotected.
	 * 
	 * @return {@code true} if this header information has been unprotected
	 */
	public boolean isUnprotected() {
		return unprotected;
	}
	
	/**
	 * Returns all bits in the first byte. If this header information is unprotected
	 * the protected bits in returned bits are also unprotected.
	 * 
	 * @return all bits in the first byte
	 */
	public int getBits() {
		return bits;
	}
	
	/**
	 * Returns the version.
	 * 
	 * @return the version, or {@code null} if there is no version field in the
	 *         packet header
	 */
	public Version getVersion() {
		return version;
	}

	/**
	 * Returns the destination connection id.
	 * 
	 * @return the destination connection id
	 */
	public byte[] getDestinationId() {
		return destinationId;
	}

	/**
	 * Returns the source connection id.
	 * 
	 * @return the source connection id, or {@code null} if there is no source
	 *         connection id field in the packet header
	 */
	public byte[] getSourceId() {
		return sourceId;
	}

	/**
	 * Returns the token.
	 * 
	 * @return the token, or {@code null} if there is no token field in the packet
	 *         header
	 */
	public byte[] getToken() {
		return token;
	}

	/**
	 * Returns the value in the length field. If this header information is
	 * unprotected the returned length is corrected to match the length in the
	 * packet with the unprotected payload.
	 * 
	 * @return the value in the length field
	 */
	public int getLength() {
		return length;
	}
	
}
