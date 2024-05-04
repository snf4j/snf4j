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

import org.snf4j.quic.engine.EncryptionLevel;

/**
 * An {@code enum} defining the QUIC packet types as defined in RFC 9000. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public enum PacketType {
	
	/** The Version Negotiation packet as defined in section 17.2.1 */
	VERSION_NEGOTIATION(true),
	
	/** The Initial packet as defined in section 17.2.2 */
	INITIAL(true, EncryptionLevel.INITIAL),
	
	/** The 0-RTT packet as defined in section 17.2.3 */
	ZERO_RTT(true, EncryptionLevel.EARLY_DATA),
	
	/** The Handshake packet as defined in section 17.2.4 */
	HANDSHAKE(true, EncryptionLevel.HANDSHAKE),
	
	/** The Retry packet as defined in section 17.2.5 */
	RETRY(true),
	
	/** The 1-RTT packet as defined in section 17.3.1 */
	ONE_RTT(false, EncryptionLevel.APPLICATION_DATA);
	
	private final boolean longHeader;
	
	private final EncryptionLevel encryptionLevel;
	
	PacketType(boolean longHeader) {
		this(longHeader, null);
	}

	PacketType(boolean longHeader, EncryptionLevel encryptionLevel) {
		this.longHeader = longHeader;
		this.encryptionLevel = encryptionLevel;
	}
	
	/**
	 * Tells if a packet of this type has the long header.
	 * 
	 * @return {@code true} if a packet of this type has the long header
	 */
	public boolean hasLongHeader() {
		return longHeader;
	}
	
	/**
	 * Returns the encryption level associated with this packet type.
	 * 
	 * @return the encryption level, or {@code null} if the packet is not protected
	 */
	public EncryptionLevel encryptionLevel() {
		return encryptionLevel;
	}
}
