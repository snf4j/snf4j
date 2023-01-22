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
	
	boolean isEmpty() {
		return neededBytes == 0 && pending == null;
	}
	
	boolean isPending() {
		return pending != null;
	}
	
	/**
	 * Tries to unwrap (aggregate) complete handshake messages from existing pending
	 * data. And then, if available, consumes them by the associated handshake
	 * engine.
	 * 
	 * @return {@code true} if unwrapping should be continued, or {@code false} if
	 *         new handshake messages are ready to be wrapped
	 * @throws AlertException if an alert has occurred
	 */
	boolean unwrapPending() throws AlertException {
		boolean statusChanged = true;
		
		if (pending != null) {
			statusChanged = unwrap(pending, pending.remaining());
			if (!pending.hasRemaining()) {
				pending = null;
			}
		}
		return statusChanged;
	}
	
	/**
	 * Tries to unwrap (aggregate) complete handshake messages from incoming data.
	 * And then, if available, consumes them by the associated handshake engine.
	 * 
	 * @param src the unprotected incoming data
	 * @param remaining the length of incoming data (the content from one record)
	 * @return {@code true} if unwrapping should be continued, or {@code false} if
	 *         new handshake messages are ready to be wrapped
	 * @throws AlertException if an alert has occurred
	 */
	boolean unwrap(ByteBuffer src, int remaining) throws AlertException {
		ByteBuffer[] srcs;
		int aggregated;
		
		if (neededBytes > 0) {
			if (remaining < neededBytes) {
				byte[] fragment = new byte[remaining];
				
				src.get(fragment);
				fragments.add(ByteBuffer.wrap(fragment));
				neededBytes -= remaining;
				return true;
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
				return true;
			}
			else {
				srcs = new ByteBuffer[] {src};
				remaining -= len;
				aggregated = len;
			}
		}
		
		boolean continueUnwrap = true;

		for(;;) {
			handshaker.consume(srcs, aggregated);
			
			if (handshaker.hasTask()) {
				if (handshaker.hasProducingTask()) {
					continueUnwrap = false;
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

		if (handshaker.needProduce()) {
			continueUnwrap = false;
		}
		
		if (remaining > 0 && pending != src) {
			byte[] pending = new byte[remaining];
			
			src.get(pending);
			this.pending = ByteBuffer.wrap(pending);
		}
		
		return continueUnwrap;
	}

}
