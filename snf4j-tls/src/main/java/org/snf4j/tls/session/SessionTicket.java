/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.session;

import org.snf4j.tls.cipher.IHashSpec;

public class SessionTicket {

	private final IHashSpec hashSpec;
	
	private final byte[] psk;
	
	private final byte[] ticket;
	
	private final long creationTime;

	private final long expirationTime;
	
	private final long ageAdd;
	
	private final long maxEarlyDataSize;
		
	public SessionTicket(IHashSpec hashSpec, byte[] psk, byte[] ticket, long lifetime, long ageAdd, long creationTime) {
		this.hashSpec = hashSpec;
		this.psk = psk;
		this.ticket = ticket;
		this.creationTime = creationTime;
		this.ageAdd = ageAdd;
		expirationTime = creationTime + lifetime * 1000;
		maxEarlyDataSize = -1L;
	}

	public SessionTicket(IHashSpec hashSpec, byte[] psk, byte[] ticket, long lifetime, long ageAdd) {
		this(hashSpec, psk, ticket, lifetime, ageAdd, System.currentTimeMillis());
	}
	
	public IHashSpec getHashSpec() {
		return hashSpec;
	}

	public byte[] getPsk() {
		return psk;
	}

	public byte[] getTicket() {
		return ticket;
	}

	public long getCreationTime() {
		return creationTime;
	}

	public long getAgeAdd() {
		return ageAdd;
	}

	public boolean isValid(long currentTime) {
		return expirationTime > currentTime;
	}
	
	public boolean isValid() {
		return isValid(System.currentTimeMillis());
	}
	
	public long getMaxEarlyDataSize() {
		return maxEarlyDataSize;
	}
	
}
