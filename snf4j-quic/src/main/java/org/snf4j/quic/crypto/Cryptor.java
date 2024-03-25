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
package org.snf4j.quic.crypto;

import java.util.Arrays;

/**
 * The base class for QUIC encryptors and decryptors providing payload and header protection.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class Cryptor {
		
	private final byte[] iv;

	private final int expansion;

	private final HeaderProtector protector;
	
	private long confidentialityCountdown;
	
	private boolean updated;
	
	/**
	 * Constructs a base class for QUIC encryptors and decryptors with the given
	 * header protector, initialization vector, ciphertext expansion and
	 * confidentiality limit.
	 * 
	 * @param protector            the associated header protector
	 * @param iv                   the initialization vector
	 * @param expansion            the length increase of a message when it is
	 *                             encrypted
	 * @param confidentialityLimit the number of packets that can be encrypted with
	 *                             a given key
	 */
	protected Cryptor(HeaderProtector protector, byte[] iv, int expansion, long confidentialityLimit) {
		this.protector = protector;
		this.iv = iv;
		this.expansion = expansion;
		this.confidentialityCountdown = confidentialityLimit;
	}
	
	/**
	 * Calculates the nonce for the given 62-bit packet number.
	 * 
	 * @param packetNumber the packet number
	 * @return the nonce
	 */
	public byte[] nonce(long packetNumber) {
		int len = iv.length;
		byte[] nonce = iv.clone();
		int i=len-1;
		
		for (; i>=len-8; --i) {
			nonce[i] ^= (byte) packetNumber;
			packetNumber >>= 8;
		}
		return nonce;
	}
	
	/**
	 * Returns the length increase of a message when it is encrypted
	 * 
	 * @return the length increase
	 */
	public int getExpansion() {
		return expansion;
	}

	/**
	 * Increases the number of encrypted/decrypted packets by the given amount.
	 * 
	 * @param amount the amount by which it should be increased
	 */
	public void incPackets(int amount) {
		confidentialityCountdown -= amount;
	}
	
	/**
	 * Tells if the confidentiality limit has been reached.
	 * 
	 * @return {@code true} if the confidentiality limit has been reached
	 */
	public boolean isConfidentialityLimitReached() {
		return confidentialityCountdown < 0;
	}

	/**
	 * Tells if this encryptor/decryptor has been already marked for the key update.
	 * 
	 * @return {@code true} if already marked for update
	 */
	public boolean isMarkedForUpdate() {
		return updated;
	}

	/**
	 * Marks this encryptor/decryptor for the key update.
	 */
	public void markForUpdate() {
		updated = true;
	}

	/**
	 * Returns the associated header protector.
	 * 
	 * @return the associated header protector
	 */
	public HeaderProtector getProtector() {
		return protector;
	}
	
	/**
	 * Erases all secrets and keys stored by this encryptor/decryptor.
	 * 
	 * @param skipProtector instructs whether to skip erasing of the associated
	 *                      header protector
	 */
	public void erase(boolean skipProtector) {
		Arrays.fill(iv, (byte) 0);
		if (!skipProtector) {
			protector.erase();
		}
	}

}
