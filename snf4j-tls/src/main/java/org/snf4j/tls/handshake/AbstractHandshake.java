/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022-2023 SNF4J contributors
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
package org.snf4j.tls.handshake;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public abstract class AbstractHandshake implements IHandshake {

	private final HandshakeType type;
	
	private byte[] prepared;
	
	protected AbstractHandshake(HandshakeType type) {
		this.type = type;
	}
	
	@Override
	public HandshakeType getType() {
		return type;
	}

	@Override
	public int getLength() {
		return 4 + getDataLength();
	}
	
	@Override
	public void getBytes(ByteBuffer buffer) {
		if (prepared != null) {
			buffer.put(prepared);
		}
		else {
			int len = getDataLength();

			if (buffer.remaining() >= len + 4) {
				buffer.put((byte) getType().value());
				buffer.put((byte) (len >> 16));
				buffer.putShort((short) (len & 0xffff));
				getData(buffer);
				return;
			}
			throw new BufferOverflowException();
		}
	}

	@Override
	public boolean isPrepared() {
		return prepared != null;
	}
	
	@Override
	public byte[] prepare() {
		byte[] prepared = new byte[getLength()];
		
		this.prepared = null;
		getBytes(ByteBuffer.wrap(prepared));
		this.prepared = prepared;
		return prepared;
	}

	protected abstract void getData(ByteBuffer buffer);
}
