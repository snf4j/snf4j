/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019-2020 SNF4J contributors
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
package org.snf4j.longevity;

import java.nio.ByteBuffer;
import java.util.List;

import org.snf4j.core.codec.IBaseDecoder;
import org.snf4j.core.session.ISession;

public class ByteToByteDecoder implements IBaseDecoder<byte[]> {

	final int increment; 
	
	public ByteToByteDecoder(int increment) {
		this.increment = increment;
	}
	
	@Override
	public void decode(ISession session, byte[] data, List<byte[]> out)	throws Exception {
		if (increment != 0) {
			for (int i=0; i<data.length; ++i) {
				data[i] += increment;
			}
		}
		out.add(data);
	}

	@Override
	public Class<byte[]> getInboundType() {
		return byte[].class;
	}

	@Override
	public Class<byte[]> getOutboundType() {
		return byte[].class;
	}

	@Override
	public int available(ISession session, ByteBuffer buffer, boolean flipped) {
		return Packet.available(buffer, flipped);
	}

	@Override
	public int available(ISession session, byte[] buffer, int off, int len) {
		return Packet.available(buffer, off, len);
	}

}
