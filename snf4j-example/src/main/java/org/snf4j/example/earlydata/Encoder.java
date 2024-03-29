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

import org.snf4j.core.codec.IEncoder;
import org.snf4j.core.session.ISession;

public class Encoder implements IEncoder<String, ByteBuffer> {

	private final int shift;
	
	Encoder(int shift) {
		this.shift = shift;
	}
	
	@Override
	public void encode(ISession session, String data, List<ByteBuffer> out) throws Exception {
		byte[] bytes = data.getBytes(StandardCharsets.US_ASCII);
		
		for (int i=0; i<bytes.length; ++i) {
			bytes[i] += shift;
		}
		ByteBuffer buffer = session.allocate(bytes.length + 2);
		buffer.putShort((short) bytes.length);
		buffer.put(bytes).flip();
		out.add(buffer);
	}
	
	@Override
	public Class<String> getInboundType() {
		return String.class;
	}

	@Override
	public Class<ByteBuffer> getOutboundType() {
		return ByteBuffer.class;
	}

}
