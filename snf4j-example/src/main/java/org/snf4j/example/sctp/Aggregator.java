/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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
package org.snf4j.example.sctp;

import java.nio.ByteBuffer;
import java.util.List;

import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.session.ISession;

public class Aggregator implements IDecoder<ByteBuffer,ByteBuffer> {
	
	private final int size;
	
	private ByteBuffer buffer;
	
	Aggregator(int size) {
		this.size = size;
	}
	
	@Override
	public Class<ByteBuffer> getInboundType() {
		return ByteBuffer.class;
	}

	@Override
	public Class<ByteBuffer> getOutboundType() {
		return ByteBuffer.class;
	}

	@Override
	public void decode(ISession session, ByteBuffer data, List<ByteBuffer> out) throws Exception {
		if (buffer == null) {
			if (data.remaining() == size) {
				out.add(data);
				return;
			}
			buffer = session.allocate(size);
		}
		
		if (data.remaining() <= buffer.remaining()) {
			buffer.put(data);
		}
		else {
			ByteBuffer dup = data.duplicate();
			dup.limit(dup.position()+buffer.remaining());
			buffer.put(dup);
			data.position(dup.position());
		}
		
		if (!buffer.hasRemaining()) {
			buffer.flip();
			out.add(buffer);
			buffer = null;
			decode(session, data, out);
		}
	}

}
