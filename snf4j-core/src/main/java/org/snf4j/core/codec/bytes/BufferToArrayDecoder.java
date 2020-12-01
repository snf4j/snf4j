/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020 SNF4J contributors
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

import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.session.ISession;

/**
 * A decoder transforming a {@link ByteBuffer} into an array of bytes.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class BufferToArrayDecoder extends BufferToArrayCodec implements IDecoder<ByteBuffer,byte[]> {

	private final boolean release;
	
	/**
	 * Constructs a decoder with a specified buffer releasing mode.
	 * 
	 * @param release the releasing mode determining if the input buffer should be
	 *                released by the session's allocator
	 */
	public BufferToArrayDecoder(boolean release) {
		this.release = release;
	}
	
	/**
	 * Constructs a decoder with no buffer releasing.
	 */
	public BufferToArrayDecoder() {
		release = false;
	}
	
	@Override
	public void decode(ISession session, ByteBuffer data, List<byte[]> out) throws Exception {
		if (release) {
			byte[] array = new byte[data.remaining()];
			
			data.get(array);
			out.add(array);
			session.release(data);
		}
		else {
			out.add(toArray(data));
		}
	}

}
