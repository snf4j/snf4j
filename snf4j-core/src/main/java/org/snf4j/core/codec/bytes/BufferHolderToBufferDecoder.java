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
 * A decoder transforming an {@link IByteBufferHolder} into a {@link ByteBuffer}.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class BufferHolderToBufferDecoder extends BufferHolderToBufferCodec implements IDecoder<IByteBufferHolder, ByteBuffer> {

	@Override
	public void decode(ISession session, IByteBufferHolder data, List<ByteBuffer> out) throws Exception {
		ByteBuffer[] bufs = data.toArray();
		
		if (bufs.length == 1) {
			out.add(bufs[0]);
		}
		else {
			ByteBuffer newBuf = session.allocate(data.remaining());
			
			for (ByteBuffer buf: bufs) {
				newBuf.put(buf);
				session.release(buf);
			}
			newBuf.flip();
			out.add(newBuf);
		}
	}

}
