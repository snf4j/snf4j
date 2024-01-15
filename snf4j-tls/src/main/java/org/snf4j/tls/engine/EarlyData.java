/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.engine;

import java.nio.ByteBuffer;
import java.util.List;

import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.IHandshake;

public class EarlyData implements IHandshake {

	private final byte[] data;
	
	public EarlyData(byte[] data) {
		this.data = data;
	}
	
	@Override
	public HandshakeType getType() {
		return null;
	}

	@Override
	public void getBytes(ByteBuffer buffer) {
		buffer.put(data);
	}

	@Override
	public int getLength() {
		return data.length;
	}

	@Override
	public int getDataLength() {
		return data.length;
	}

	@Override
	public boolean isKnown() {
		return true;
	}

	@Override
	public boolean isPrepared() {
		return true;
	}

	@Override
	public byte[] prepare() {
		return data;
	}

	@Override
	public byte[] getPrepared() {
		return prepare();
	}

	@Override
	public List<IExtension> getExtensions() {
		return null;
	}

}
