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

import org.snf4j.core.IByteBufferHolder;
import org.snf4j.core.codec.ICodec;

/**
 * A base {@code class} for encoders and decoders transforming an {@link IByteBufferHolder}
 * into an array of bytes.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class BufferHolderToArrayCodec implements ICodec<IByteBufferHolder,byte[]> {

	private final static byte[] EMPTY = new byte[0];
	
	/**
	 * Converts an {@link IByteBufferHolder} into an array of bytes. If the passed
	 * buffer holder stores only one buffer and this buffer is backed by an
	 * accessible byte array and the number of remaining bytes in the buffer equals
	 * the length of the backing array then it simply returns the backing array. In
	 * other cases it returns a newly created array filled with the bytes remaining
	 * in the buffer holder.
	 * <p>
	 * The passed buffer holder should be no longer used after calling this method.
	 * 
	 * @param holder the buffer holder to convert
	 * @return an array of bytes
	 */
	public static byte[] toArray(IByteBufferHolder holder) {
		ByteBuffer[] bufs = holder.toArray();

		if (bufs.length == 1) {
			return BufferToArrayCodec.toArray(bufs[0]);
		}
		
		int remaining = holder.remaining();
		
		if (remaining == 0) {
			return EMPTY;
		}
		
		byte[] bytes = new byte[remaining];
		int off = 0;
		
		for (ByteBuffer buf: bufs) {
			remaining = buf.remaining();
			buf.get(bytes, off, remaining);
			off += remaining;
		}
		return bytes;
	}
	
	@Override
	public Class<IByteBufferHolder> getInboundType() {
		return IByteBufferHolder.class;
	}

	@Override
	public Class<byte[]> getOutboundType() {
		return byte[].class;
	}

}
