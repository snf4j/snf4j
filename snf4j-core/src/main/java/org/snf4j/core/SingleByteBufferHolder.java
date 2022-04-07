/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022 SNF4J contributors
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
package org.snf4j.core;

import java.nio.ByteBuffer;

/**
 * A byte buffer holder that stores only one byte buffer.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class SingleByteBufferHolder implements IByteBufferHolder {
	
	private final ByteBuffer buffer;

	/**
	 * Construct a byte buffer holder storing one specified byte buffer.
	 * 
	 * @param buffer the byte buffer to be stored by this holder
	 * @throws IllegalArgumentException if the {@code buffer} is null
	 */
	public SingleByteBufferHolder(ByteBuffer buffer) {
		if (buffer == null) throw new IllegalArgumentException("buffer is null");
		this.buffer = buffer;
	}
	
	@Override
	public boolean hasRemaining() {
		return buffer.hasRemaining();
	}

	@Override
	public int remaining() {
		return buffer.remaining();
	}

	@Override
	public ByteBuffer[] toArray() {
		return new ByteBuffer[] {buffer};
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @return {@code false}
	 */
	@Override
	public boolean isMessage() {
		return false;
	}

}
