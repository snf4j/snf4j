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
import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.session.ISession;

/**
 * A decoder transforming an {@link IByteBufferHolder} into an array of bytes.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class BufferHolderToArrayDecoder extends BufferHolderToArrayCodec implements IDecoder<IByteBufferHolder,byte[]> {

	private final boolean release;
	
	/**
	 * Constructs a decoder with a specified buffer releasing mode.
	 * 
	 * @param release the releasing mode determining if buffers in the input buffer
	 *                holder should be released by the session's allocator
	 */
	public BufferHolderToArrayDecoder(boolean release) {
		this.release = release;
	}
	
	/**
	 * Constructs a decoder with no buffer releasing.
	 */
	public BufferHolderToArrayDecoder() {
		release = false;
	}

	@Override
	public void decode(ISession session, IByteBufferHolder data, List<byte[]> out) throws Exception {
		if (release) {
			byte[] array = new byte[data.remaining()];
			int remaining, off = 0;
			
			for (ByteBuffer buf: data.toArray()) {
				remaining = buf.remaining();
				buf.get(array, off, remaining);
				off += remaining;
				session.release(buf);
			}
			out.add(array);
		}
		else {
			out.add(toArray(data));
		}
	}

}
