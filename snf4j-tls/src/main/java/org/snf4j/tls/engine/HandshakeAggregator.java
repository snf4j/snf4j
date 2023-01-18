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
package org.snf4j.tls.engine;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.tls.alert.AlertException;

public class HandshakeAggregator {
	
	private final List<ByteBuffer> fragments = new LinkedList<ByteBuffer>();
	
	private final IHandshakeEngine handshaker;
	
	private int expectedBytes;
	
	private int neededBytes;
	
	private ByteBuffer pending;
	
	HandshakeAggregator(IHandshakeEngine handshaker) {
		this.handshaker = handshaker;
	}
	
	HandshakeStatus unwrapPending() throws AlertException {
		HandshakeStatus status = HandshakeStatus.NEED_UNWRAP;
		
		if (pending != null) {
			status = unwrap(pending, pending.remaining());
			if (!pending.hasRemaining()) {
				pending = null;
			}
		}
		return status;
	}
	
	HandshakeStatus unwrap(ByteBuffer src, int remaining) throws AlertException {
		HandshakeStatus status = HandshakeStatus.NEED_UNWRAP;
		ByteBuffer[] srcs;
		int aggregated;
		
		if (neededBytes > 0) {
			if (remaining < neededBytes) {
				byte[] fragment = new byte[remaining];
				
				src.get(fragment);
				fragments.add(ByteBuffer.wrap(fragment));
				neededBytes -= remaining;
				return status;
			}
			else {
				srcs = fragments.toArray(new ByteBuffer[fragments.size()+1]);
				fragments.clear();
				remaining -= neededBytes;
				aggregated = expectedBytes;
				neededBytes = 0;
				srcs[srcs.length-1] = src;
			}
		}
		else {
			int len = 4 + (src.getInt(src.position()) & 0xffffff);
			
			if (len > remaining) {
				byte[] fragment = new byte[remaining];
				
				src.get(fragment);
				fragments.add(ByteBuffer.wrap(fragment));
				expectedBytes = len;
				neededBytes = len - remaining;
				return status;
			}
			else {
				srcs = new ByteBuffer[] {src};
				remaining -= len;
				aggregated = len;
			}
		}
		
		for(;;) {
			handshaker.consume(srcs, aggregated);
			
			if (handshaker.hasTask()) {
				if (handshaker.hasProducingTask()) {
					status = HandshakeStatus.NEED_WRAP;
				}
				break;
			}
			
			if (remaining >= 4) {
				int len = 4 + (src.getInt(src.position()) & 0xffffff);

				if (len > remaining) {
					expectedBytes = len;
					neededBytes = len - remaining;
					len = srcs.length-1;
					for (int i=0; i<len; ++i) {
						ByteBuffer b = srcs[i];

						if (b.hasRemaining()) {
							fragments.add(b);
						}
					}

					byte[] fragment = new byte[remaining];

					srcs[len].get(fragment);
					remaining = 0;
					fragments.add(ByteBuffer.wrap(fragment));
					break;
				}
				remaining -= len;
				aggregated = len;
			}
			else {
				break;
			}
		}
		
		if (remaining > 0 && pending != src) {
			byte[] pending = new byte[remaining];
			
			src.get(pending);
			this.pending = ByteBuffer.wrap(pending);
		}
		
		return status;
	}

}
