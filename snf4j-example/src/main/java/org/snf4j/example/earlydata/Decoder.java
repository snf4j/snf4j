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
package org.snf4j.example.earlydata;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.snf4j.core.codec.IBaseDecoder;
import org.snf4j.core.session.ISession;

public class Decoder implements IBaseDecoder<ByteBuffer, String> {
	
	private final int shift;
	
	Decoder(int shift) {
		this.shift = shift;
	}

	@Override
	public void decode(ISession session, ByteBuffer data, List<String> out) throws Exception {
		byte[] bytes = new byte[data.remaining()-2];
		
		data.position(2);
		data.get(bytes);
		session.release(data);
		for (int i=0; i<bytes.length; ++i) {
			bytes[i] -= shift;
		}
		out.add(new String(bytes, StandardCharsets.US_ASCII));
	}

	@Override
	public Class<ByteBuffer> getInboundType() {
		return ByteBuffer.class;
	}

	@Override
	public Class<String> getOutboundType() {
		return String.class;
	}

	@Override
	public int available(ISession session, ByteBuffer buffer, boolean flipped) {
		int remaining, position;
		
		if (flipped) {
			remaining = buffer.remaining();
			position = buffer.position();
		}
		else {
			remaining = buffer.position();
			position = 0;
		}
		
		if (remaining >= 2) {
			int length = buffer.getShort(position) + 2;
			
			if (remaining >= length) {
				return length;
			}
		}
		return 0;
	}

	@Override
	public int available(ISession session, byte[] buffer, int off, int len) {
		if (buffer.length >= 2) {
			int length = ByteBuffer.wrap(buffer).getShort() + 2;
					
			if (buffer.length >= length) {
				return length;
			}
		}
		return 0;
	}
}
