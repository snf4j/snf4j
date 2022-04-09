/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022 SNF4J contributors
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
package org.snf4j.core.codec.bytes;

import java.nio.ByteBuffer;
import java.util.List;

import org.snf4j.core.IByteBufferHolder;
import org.snf4j.core.SingleByteBufferHolder;
import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.session.ISession;

/**
 * A decoder transforming an array of bytes into a {@link IByteBufferHolder}.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class ArrayToBufferHolderDecoder extends ArrayToBufferHolderCodec implements IDecoder<byte[], IByteBufferHolder> {

	private final boolean allocate;
	
	/**
	 * Constructs a decoder with a specified allocation mode.
	 * 
	 * @param allocate the allocation mode determining if the buffer in the output
	 *                 buffer holder should be allocated by the session's allocator
	 *                 or the buffer should wrap the input array.
	 */
	public ArrayToBufferHolderDecoder(boolean allocate) {
		this.allocate = allocate;
	}
	
	/**
	 * Constructs a decoder with no buffer allocation (only by wrapping the input array)
	 */
	public ArrayToBufferHolderDecoder() {
		allocate = false;
	}
	
	@Override
	public void decode(ISession session, byte[] data, List<IByteBufferHolder> out) throws Exception {
		if (allocate) {
			ByteBuffer b = session.allocate(data.length);
			
			b.put(data).flip();
			out.add(new SingleByteBufferHolder(b));
		}
		else {
			out.add(new SingleByteBufferHolder(ByteBuffer.wrap(data)));
		}
	}

}
