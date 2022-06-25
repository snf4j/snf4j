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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * A byte buffer array wrapper providing absolute and relative get methods that
 * read data from an array of the {@link ByteBuffer} objects.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class ByteBufferArray {
	
	private final ByteBuffer[] buffers;

	private int index;
	
	private ByteBuffer buffer;
	
	public ByteBufferArray(Collection<ByteBuffer> buffers) {
		this(buffers.toArray(new ByteBuffer[buffers.size()]));
	}

	public ByteBufferArray(ByteBuffer[] buffers) {
		this.buffers = buffers;
		if (this.buffers.length > 0) {
			buffer = this.buffers[0];
		}
	}
	
	public final boolean hasRemaining() {
		for (int i=index; i<buffers.length; ++i) {
			if (buffers[i].hasRemaining()) {
				return true;
			}
		}
		return false;
	}
	
	public ByteBufferArray duplicate() {
		ByteBuffer[] dup = new ByteBuffer[buffers.length];
		
		for (int i=0; i<dup.length; ++i) {
			dup[i] = buffers[i].duplicate();
		}
		ByteBufferArray dupArray = new ByteBufferArray(dup);
		dupArray.index = index;
		dupArray.buffer = dup[index];
		return dupArray;
	}
	
	public long remaining() {
		long remaining = 0;
		for (int i=index; i<buffers.length; ++i) {
			remaining += buffers[i].remaining();
		}
		return remaining;
	}
	
	private ByteBuffer buffer() {
		if (buffer == null) {
			throw new BufferUnderflowException();
		}
		while (!buffer.hasRemaining()) {
			++index;
			if (index >= buffers.length) {
				throw new BufferUnderflowException();
			}
			buffer = buffers[index];
		}
		return buffer;
	}

	private ByteBuffer buffer(int size) {
		ByteBuffer b = buffer();
		
		if (b.remaining() >= size) {
			return b;
		}
		byte[] data = new byte[size];
		get(data);
		return ByteBuffer.wrap(data).order(b.order());
	}
	
	private ByteBuffer buffer(int size, int[] index) {
		int i = index[0];
		
		if (i < 0) {
			throw new IndexOutOfBoundsException();
		}
		for (int b=0; b<buffers.length; ++b) {
			ByteBuffer buf = buffers[b]; 
			int l = buf.limit();
			
			if (i >= l) {
				i -= l;
			}
			else if (i <= l-size) {
				index[0] = i;
				return buf;
			}
			else {
				byte[] data = new byte[size];

				for (int j=0; j<size; ++j) {
					if (i < l) {
						data[j] = buf.get(i++);
					}
					else if (++b < buffers.length) {
						buf = buffers[b];
						l = buf.limit();
						i = 0;
						--j;
					}
					else {
						throw new IndexOutOfBoundsException();
					}
				}
				index[0] = 0;
				return ByteBuffer.wrap(data).order(buf.order());
			}
		}
		throw new IndexOutOfBoundsException();
	}
	
	public ByteBufferArray get(byte[] dst) {
		return get(dst, 0, dst.length);
	}
	
	public ByteBufferArray get(byte[] dst, int off, int len) {
		InternalSession.checkBounds(off, len, dst.length);
		if (len == 0) {
			return this;
		}
		
		ByteBuffer buffer = buffer();
		
		int remaining = buffer.remaining();
		if (len <= remaining) {
			buffer.get(dst, off, len);
			return this;
		}
		
		for (int i=index+1; i<buffers.length; ++i) {
			remaining += buffers[i].remaining();
			if (remaining >= len) {
				while (true) {
					int size = Math.min(len, buffer.remaining());

					buffer.get(dst, off, size);
					off += size;
					len -= size;
					if (len > 0) {
						buffer = buffer();
					}
					else {
						return this;		
					}
				}				
			}
		}
		throw new BufferUnderflowException();
	}
	
	/**
	 * Relative get method. Reads the byte at this buffer array wrapper's current
	 * position, and then increments the position.
	 * 
	 * @return The byte at the buffer array wrapper's current position
	 */
	public byte get() {
		return buffer().get(); 
	}

	public byte get(int index) {
		int[] indexArray = new int[] {index};
		return buffer(1, indexArray).get(indexArray[0]);
	}
	
	public char getChar() {
		return buffer(2).getChar();
	}

	public char getChar(int index) {
		int[] indexArray = new int[] {index};
		return buffer(2, indexArray).getChar(indexArray[0]);
	}
	
	public short getShort() {
		return buffer(2).getShort();
	}

	public short getShort(int index) {
		int[] indexArray = new int[] {index};
		return buffer(2, indexArray).getShort(indexArray[0]);
	}
	
	public int getInt() {
		return buffer(4).getInt();
	}

	public int getInt(int index) {
		int[] indexArray = new int[] {index};
		return buffer(4, indexArray).getInt(indexArray[0]);
	}
	
	public long getLong() {
		return buffer(8).getLong();
	}

	public long getLong(int index) {
		int[] indexArray = new int[] {index};
		return buffer(8, indexArray).getLong(indexArray[0]);
	}

	public float getFloat() {
		return buffer(4).getFloat();
	}

	public float getFloat(int index) {
		int[] indexArray = new int[] {index};
		return buffer(4, indexArray).getFloat(indexArray[0]);
	}
	
	public double getDouble() {
		return buffer(8).getDouble();
	}

	public double getDouble(int index) {
		int[] indexArray = new int[] {index};
		return buffer(8, indexArray).getDouble(indexArray[0]);
	}
	
}
