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
package org.snf4j.websocket.frame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Random;

import org.snf4j.core.codec.IEncoder;
import org.snf4j.core.session.ISession;

/**
 * Encodes a Web Socket frame into bytes in the protocol version 13 format.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class FrameEncoder implements IEncoder<Frame,ByteBuffer> {

	private final static Random RANDOM = new Random();
	
	private final boolean clientMode;
	
	private boolean closed;

	/**
	 * Constructs a Web Socket encoder.
	 * 
	 * @param clientMode determines the mode (client/server) in which the encoder
	 *                   should work
	 */
	public FrameEncoder(boolean clientMode) {
		this.clientMode = clientMode;
	}
	
	@Override
	public Class<Frame> getInboundType() {
		return Frame.class;
	}

	@Override
	public Class<ByteBuffer> getOutboundType() {
		return ByteBuffer.class;
	}

	@Override
	public void encode(ISession session, Frame frame, List<ByteBuffer> out) throws Exception {
		if (closed) {
			return;
		}
		else if (frame.getOpcode() == Opcode.CLOSE) {
			closed = true;
		}
		
		ByteBuffer buffer = session.allocate(length(frame));
		
		buffer.order(ByteOrder.BIG_ENDIAN);
		byte b = (byte) ((frame.getRsvBits() << 4) & 0x70);
		
		if (frame.isFinalFragment()) {
			b |= 0x80;
		}
		b |= frame.getOpcode().value();
		buffer.put(b);
		
		byte[] payload = frame.getPayload();
		int len = payload.length;
		
		b = (byte) (clientMode ? 0x80 : 0);
		if (len > 0xffff) {
			b |= 127;
			buffer.put(b);
			buffer.putLong(len);
		}
		else if (len > 125) {
			b |= 126;
			buffer.put(b);
			buffer.putShort((short) len);
		}
		else {
			b |= len;
			buffer.put(b);
		}
		if (clientMode) {
			byte[] mask = new byte[4];
			byte[] masked = new byte[len];
			
			RANDOM.nextBytes(mask);
			for (int i=0; i<len; ++i) {
				masked[i] = (byte) (payload[i] ^ mask[i % 4]);
			}
			buffer.put(mask);
			payload = masked;
		}
		buffer.put(payload).flip();
		out.add(buffer);
	}

	protected int length(Frame frame) {
		int len = frame.getPayloadLength();
		
		if (len > 0xffff) {
			len += 8;
		}
		else if (len > 125) {
			len += 2;
		}
		if (clientMode) {
			len += 4;
		}
		return len + 2;
	}
}
