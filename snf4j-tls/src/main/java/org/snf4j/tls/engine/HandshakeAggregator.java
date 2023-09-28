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
import java.util.Queue;

import org.snf4j.tls.alert.Alert;

public class HandshakeAggregator {
	
	private final List<ByteBuffer> fragments = new LinkedList<ByteBuffer>();
	
	private final Queue<ByteBuffer> remaining = new LinkedList<ByteBuffer>();

	private final IHandshakeEngine handshaker;
	
	private int expectedBytes;
	
	private int neededBytes;
	
	private ByteBuffer pendingHeader;
		
	HandshakeAggregator(IHandshakeEngine handshaker) {
		this.handshaker = handshaker;
	}
	
	boolean isEmpty() {
		return neededBytes == 0 && pendingHeader == null && remaining.isEmpty();
	}
		
	boolean hasRemaining() {
		return !remaining.isEmpty();
	}
	
	/**
	 * Tries to unwrap (aggregate) complete handshake messages from existing remaining
	 * data. And then, if available, consumes them by the associated handshake
	 * engine.
	 * 
	 * @return {@code true} if unwrapping should be continued, or {@code false} if
	 *         new handshake messages are ready to be wrapped
	 * @throws Alert if an alert has occurred
	 */
	boolean unwrapRemaining() throws Alert {
		boolean continueUnwrap = true;
		ByteBuffer src;
		
		while (continueUnwrap && (src = remaining.peek()) != null) {
			continueUnwrap = unwrap(src, src.remaining(), true);
			if (!src.hasRemaining()) {
				remaining.poll();
			}
		}
		return continueUnwrap;
	}	
	
	/**
	 * Tries to unwrap (aggregate) complete handshake messages from incoming data.
	 * And then, if available, consumes them by the associated handshake engine.
	 * 
	 * @param src the unprotected incoming data
	 * @param remaining the length of incoming data (the content from one record)
	 * @return {@code true} if unwrapping should be continued, or {@code false} if
	 *         new handshake messages are ready to be wrapped
	 * @throws Alert if an alert has occurred
	 */
	boolean unwrap(ByteBuffer src, int remaining) throws Alert {
		return unwrap(src, remaining, false);
	}	

	private boolean unwrap(ByteBuffer src, int remaining, boolean stored) throws Alert {
		ByteBuffer[] srcs;
		int aggregated;

		if (neededBytes == 0) {
			int len;

			if (pendingHeader == null) {
				if (remaining < 4) {
					if (remaining > 0) {
						pendingHeader = ByteBuffer.allocate(4);
						while (remaining-- > 0) {
							pendingHeader.put(src.get());
						}
					}
					return true;
				}
				len = 4 + (src.getInt(src.position()) & 0xffffff);
			}
			else if (pendingHeader.remaining() > remaining) {
				while (remaining-- > 0) {
					pendingHeader.put(src.get());
				}
				return true;
			}
			else {
				while (pendingHeader.hasRemaining()) {
					pendingHeader.put(src.get());
					--remaining;
				}
				pendingHeader.flip();
				len = pendingHeader.getInt(0) & 0xffffff;
			}

			if (len > remaining) {
				byte[] fragment = new byte[remaining];

				src.get(fragment);
				if (pendingHeader != null) {
					fragments.add(pendingHeader);
					pendingHeader = null;
					expectedBytes = len + 4;
				}
				else {
					expectedBytes = len;
				}
				neededBytes = len - remaining;
				fragments.add(ByteBuffer.wrap(fragment));
				return true;
			}
			else if (pendingHeader != null) {
				srcs = new ByteBuffer[] {pendingHeader, src};
				pendingHeader = null;
				aggregated = len + 4;
				remaining -= len;
			}
			else {
				srcs = new ByteBuffer[] {src};
				aggregated = len;
				remaining -= len;
			}
		}
		else if (remaining < neededBytes) {
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

		handshaker.consume(srcs, aggregated);

		boolean continueUnwrap = true;
		
		if (handshaker.hasTask()) {
			if (handshaker.hasProducingTask()) {
				continueUnwrap = false;
			}
		}
		else if (remaining > 0) {
			return unwrap(src, remaining);
		}

		if (continueUnwrap) {
			if (handshaker.needProduce()) {
				continueUnwrap = false;
			}
			else {
				handshaker.updateTasks();
				if (handshaker.needProduce()) {
					continueUnwrap = false;
				}
			}
		}
		
		if (!stored && remaining > 0) {
			byte[] pending = new byte[remaining];

			src.get(pending);
			this.remaining.add(ByteBuffer.wrap(pending));
		}
		
		return continueUnwrap;
	}	
	
}
