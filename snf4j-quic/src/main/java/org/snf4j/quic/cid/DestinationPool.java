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

import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.tls.Args;

class DestinationPool extends Pool implements IDestinationPool {
	
	private final int limit;
	
	DestinationPool(int limit) {
		Args.checkMin(limit, 2, "limit");
		this.limit = limit;
	}
	
	@Override
	public ConnectionId updateResetToken(byte[] resetToken) {
		ConnectionId cid = cids.get(INITIAL_SEQUENCE_NUMBER);
		
		if (cid != null) {
			cid = cid.cloneWith(resetToken);
			cids.put(cid.getSequenceNumber(), cid);
		}
		return cid;
	}

	@Override
	public int getLimit() {
		return limit;
	}
	
	@Override
	public ConnectionId add(byte[] id) throws QuicException {
		return add(INITIAL_SEQUENCE_NUMBER, id, null);
	}
	
	@Override
	public ConnectionId add(int sequenceNumber, byte[] id, byte[] resetToken) throws QuicException {
		if (retired.isRetired(sequenceNumber) || cids.containsKey(sequenceNumber)) {
			return null;
		}
		if (cids.size() < limit) {
			ConnectionId cid = new ConnectionId(sequenceNumber, id, resetToken);
		
			cids.put(sequenceNumber, cid);
			return cid;
		}
		throw new QuicException(TransportError.CONNECTION_ID_LIMIT_ERROR, "Connection id limit exceeded");
	}

}
