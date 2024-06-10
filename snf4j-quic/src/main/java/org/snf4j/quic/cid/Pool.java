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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

abstract class Pool implements IPool {
	
	final Map<Integer, ConnectionId> cids = new HashMap<>();
	
	final RetirePool retired = new RetirePool();
	
	@Override
	public ConnectionId get() {
		return cids.values().stream().findAny().orElse(null);
	}
	
	@Override
	public ConnectionId get(int sequenceNumber) {
		return cids.get(sequenceNumber);
	}
	
	@Override
	public ConnectionId get(byte[] id) {
		for (ConnectionId cid: cids.values()) {
			if (Arrays.equals(cid.getId(), id)) {
				return cid;
			}
		}
		return null;
	}
	
	@Override
	public int getSize() {
		return cids.size();
	}
	
	@Override
	public ConnectionId retire(int sequenceNumber) {
		ConnectionId cid = cids.remove(sequenceNumber);
		
		retired.retire(sequenceNumber);
		return cid;
	}
}
