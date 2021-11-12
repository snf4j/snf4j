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
package org.snf4j.core.proxy;

import java.nio.ByteBuffer;

abstract class AbstractSocksState {
	
	final static byte VER_INDEX = 0;
	
	protected final AbstractSocksProxyHandler handler;
	
	AbstractSocksState(AbstractSocksProxyHandler handler) {
		this.handler = handler;
	}
	
	protected int length(ByteBuffer data, boolean flipped) {
		return flipped ? data.remaining() : data.position();
	}
	
	int available(ByteBuffer data, boolean flipped) {
		return available(null, 0, length(data, flipped));
	}

	int available(byte[] data, int off, int len) {
		int size = readSize();
		
		return len < size ? 0 : size;
	}
	
	abstract int readSize();
	
	abstract AbstractSocksState read(byte[] data);
	
	abstract void handleReady();
}
