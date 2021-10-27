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
package org.snf4j.websocket.extensions;

import java.util.List;

import org.snf4j.core.codec.IEncoder;
import org.snf4j.core.session.ISession;
import org.snf4j.websocket.frame.Frame;
import org.snf4j.websocket.frame.Opcode;
import org.snf4j.websocket.frame.TextFrame;

public class TestEncoder implements IEncoder<Frame,Frame> {

	private final char mask;
	
	private final int rsv;
	
	private final boolean skip;
	
	TestEncoder(char mask, int rsv, boolean skip) {
		this.mask = mask;
		this.rsv = rsv;
		this.skip = skip;
	}
	
	@Override
	public Class<Frame> getInboundType() {
		return Frame.class;
	}

	@Override
	public Class<Frame> getOutboundType() {
		return Frame.class;
	}

	@Override
	public void encode(ISession session, Frame data, List<Frame> out) throws Exception {
		if (!skip) {
			if (data.getOpcode() == Opcode.TEXT) {
				byte[] payload = data.getPayload();

				for (int i=0; i<payload.length; ++i) {
					payload[i] ^= (byte)mask;
				}
				data = new TextFrame(data.isFinalFragment(), data.getRsvBits() | rsv, payload);
			}
		}
		out.add(data);
	}

}
