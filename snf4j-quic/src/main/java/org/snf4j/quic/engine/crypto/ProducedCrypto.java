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
package org.snf4j.quic.engine.crypto;

import java.nio.ByteBuffer;

import org.snf4j.tls.Args;

/**
 * The holder for cryptographic handshake data produced by an {@link ICryptoEngine} implementation.
 *  
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class ProducedCrypto {

	private final EncryptionLevel encryptionLevel;
	
	private final ByteBuffer data;
	
	private final int offset;
	
	/**
	 * Constructs the holder for produced cryptographic handshake data with the
	 * associated encryption level and offset.
	 * 
	 * @param data            the produced cryptographic handshake data
	 * @param encryptionLevel the associated encryption level
	 * @param offset          the offset within the cryptographic handshake
	 *                        stream of the produced data 
	 */
	public ProducedCrypto(ByteBuffer data, EncryptionLevel encryptionLevel, int offset) {
		Args.checkNull(data, "data");
		Args.checkNull(encryptionLevel, "encryptionLevel");
		this.data = data;
		this.encryptionLevel = encryptionLevel;
		this.offset = offset;
	}
	
	/**
	 * Returns the associated cryptographic handshake data.
	 * 
	 * @return the cryptographic handshake data
	 */
	public ByteBuffer getData() {
		return data;
	}
	
	/**
	 * Returns the encryption level identifying the packet protection keys that
	 * should be used to protect the associated cryptographic handshake data.
	 * 
	 * @return the encryption level
	 */
	public EncryptionLevel getEncryptionLevel() {
		return encryptionLevel;
	}

	/**
	 * Returns the offset within the cryptographic handshake stream of the
	 * associated cryptographic handshake data.
	 * 
	 * @return the offset within the cryptographic handshake stream
	 */
	public int getOffset() {
		return offset;
	}
}
