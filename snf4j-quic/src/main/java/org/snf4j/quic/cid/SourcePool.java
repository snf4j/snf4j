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
package org.snf4j.quic.cid;

import java.security.SecureRandom;

class SourcePool extends Pool implements ISourcePool {

	private final IResetTokenCalculator calculator;
	
	private final int idLength;
	
	private final SecureRandom random;
	
	private final boolean iniResetToken;
	
	private int next;
	
	private int limit = 2;
	
	SourcePool(int idLength, boolean iniResetToken, SecureRandom random) {
		this.idLength = idLength;
		this.random = random;
		this.iniResetToken = iniResetToken;
		calculator = RandomResetTokenCalculator.INSTANCE;
	}
	
	byte[] generate(int length) {
		byte[] id = new byte[length];
		
		random.nextBytes(id);
		return id;
	}
	
	private ConnectionId add(int sequenceNumber, byte[] id, byte[] resetToken) {
		ConnectionId cid = new ConnectionId(sequenceNumber, id, resetToken);
		
		cids.put(sequenceNumber, cid);
		return cid;
	}
	
	@Override
	public ConnectionId issue() {
		if (cids.size() < limit) {
			int sequenceNumber = next++;
			boolean resetToken = sequenceNumber == 0 ? iniResetToken : true;
			byte[] id = generate(idLength);

			return add(sequenceNumber, id, resetToken ? calculator.calculate(
					sequenceNumber, 
					id,
					random) : null);
		}
		return null;
	}
	
	@Override
	public ConnectionId get() {
		if (next == 0) {
			return issue();
		}
		return super.get();
	}

	@Override
	public ConnectionId get(int sequenceNumber) {
		if (next == 0) {
			return issue();
		}
		return super.get(sequenceNumber);
	}
	
	@Override
	public int getLimit() {
		return limit;
	}

	@Override
	public void setLimit(int limit) {
		this.limit = limit;
	}

	@Override
	public int getIdLength() {
		return idLength;
	}
	
}
