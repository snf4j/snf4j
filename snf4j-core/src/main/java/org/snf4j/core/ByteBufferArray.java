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
import java.nio.InvalidMarkException;
import java.util.Collection;

/**
 * A byte buffer array wrapper providing absolute and relative get methods that
 * read data from an array of the {@link ByteBuffer} objects.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class ByteBufferArray {

	private final ByteBuffer[] array;

	private final int off;
	
	private final int size;
	
	private final int end;
	
	private int index;

	private long mark = -1;

	private ByteBuffer buffer;

	/**
	 * Constructs a byte buffer array wrapper that will be backed by an array
	 * containing {@link ByteBuffer} objects from the given collection, in the order
	 * they are returned by the collection's iterator.
	 * 
	 * @param collection the collection with {@link ByteBuffer} objects
	 */
	public ByteBufferArray(Collection<ByteBuffer> collection) {
		this(collection.toArray(new ByteBuffer[collection.size()]));
	}

	/**
	 * Constructs a byte buffer array wrapper that will be backed by the given
	 * {@link ByteBuffer} array.
	 * 
	 * @param array The array that will back this buffer array wrapper
	 */
	public ByteBufferArray(ByteBuffer[] array) {
		this.array = array;
		size = end = array.length; 
		off = 0;
		if (size > 0) {
			buffer = this.array[off];
		}
	}

	/**
	 * Constructs a byte buffer array wrapper that will be backed by the given
	 * {@link ByteBuffer} array.
	 * 
	 * @param array The array that will back this buffer array wrapper
	 * @param off   The offset of the subarray to be used
	 * @param len   The length of the subarray to be used
	 * @throws IndexOutOfBoundsException If the preconditions on the offset and
	 *                                   length parameters do not hold
	 */
	public ByteBufferArray(ByteBuffer[] array, int off, int len) {
		InternalSession.checkBounds(off, len, array.length);
		this.array = array;
		if (len > 0) {
			buffer = this.array[off];
		}
		this.off = off;
		this.size = len;
		end = off + len;
		index = off;
	}
	
	/**
	 * Returns the {@link ByteBuffer} array that backs this buffer array wrapper.
	 * 
	 * @return The array that backs this buffer
	 */
	public ByteBuffer[] array() {
		return array;
	}
	
	/**
	 * Returns the offset within this buffer array wrapper's backing array of the
	 * first buffer.
	 * 
	 * @return The offset within this buffer array wrapper's array of the first
	 *         buffer
	 */
	public int arrayOffset() {
		return off;
	}
	
	/**
	 * Returns the index after the last buffer in the array backing this buffer
	 * array wrapper
	 * 
	 * @return the index after the last buffer
	 */
	public int arrayEnd() {
		return end;
	}
	
	/**
	 * Returns the index of the buffer in the backing array that is pointed by this
	 * buffer array wrapper's current position. If this buffer array wrapper has no
	 * remaining bytes the return value will equals the {@code arrayEnd()}.
	 * 
	 * @return The index of the current buffer, or the size if this buffer array
	 *         wrapper has no remaining bytes
	 */
	public int arrayIndex() {
		int i = index;
		
		for (; i<end; ++i) {
			if (array[i].hasRemaining()) {
				return i;
			}
		}
		return i;
	}
	
	/**
	 * Returns the number of buffers in the the backing array.
	 * 
	 * @return The number of buffers
	 */
	public int size() {
		return size;
	}
	
	/**
	 * Tells whether there are any bytes between the current position and the limit.
	 * 
	 * @return {@code true} if, and only if, there is at least one byte remaining in
	 *         this buffer array wrapper
	 */
	public boolean hasRemaining() {
		for (int i = index; i < end; ++i) {
			if (array[i].hasRemaining()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the number of bytes between the current position and the limit.
	 * 
	 * @return The number of bytes remaining in this buffer array wrapper
	 */
	public long remaining() {
		long remaining = 0;

		for (int i = index; i < end; ++i) {
			remaining += array[i].remaining();
		}
		return remaining;
	}

	/**
	 * Returns this buffer array wrapper's limit.
	 * 
	 * @return The limit of this buffer array wrapper
	 */
	public long limit() {
		long limit = 0;

		for (int i = off; i < end; ++i) {
			limit += array[i].limit();
		}
		return limit;
	}

	/**
	 * Returns this buffer array wrapper's position.
	 * 
	 * @return The position of this buffer array wrapper
	 */
	public long position() {
		if (buffer != null) {
			long position = 0;

			for (int i=off; i < index; ++i) {
				position += array[i].limit();
			}
			return buffer.position() + position;
		}
		return 0;
	}

	/**
	 * Sets this buffer array wrapper's position. If the mark is defined and larger
	 * than the new position then it is discarded.
	 * 
	 * @param newPosition The new position value; must be non-negative and no larger
	 *                    than the current limit
	 * @return This buffer array wrapper
	 * @throws IllegalArgumentException If the preconditions on newPosition do not
	 *                                  hold
	 */
	public ByteBufferArray position(long newPosition) {
		return position(newPosition, false);
	}

	private ByteBufferArray position(long newPosition, boolean reset) {
		if (newPosition >= 0) {
			int limit = -1, i = off, len = end;
			long position = newPosition;
			ByteBuffer buffer = null;
			boolean positioned = false;
			
			for (; i < len; ++i) {
				buffer = array[i];
				limit = buffer.limit();
				if (position < limit) {
					buffer.position((int) position);
					index = i;
					this.buffer = buffer;
					if (mark > newPosition) {
						mark = -1;
					}
					positioned = true;
					break;
				} else {
					position -= limit;
				}
			}
			if (i == len && position == 0) {
				if (limit != -1) {
					buffer.position(limit);
					index = --i;
					this.buffer = buffer;
				}
				positioned = true;
			}
			if (positioned) {
				int j = off;
				for (; j<i; ++j) {
					array[j].position(array[j].limit());
				}
				for (++j; j<len; ++j) {
					array[j].position(0);
				}
				return this;
			}
		}
		throw reset ? new InvalidMarkException() : new IllegalArgumentException();
	}
	
	/**
	 * Sets this buffer array wrapper's mark at its position.
	 * 
	 * @return This buffer array wrapper
	 */
	public ByteBufferArray mark() {
		mark = position();
		return this;
	}

	/**
	 * Resets this buffer array wrapper's position to the previously-marked
	 * position.
	 * <p>
	 * Invoking this method neither changes nor discards the mark's value.
	 * 
	 * @return This buffer array wrapper
	 * @throws InvalidMarkException If the mark has not been set or its value is
	 *                              invalid
	 */
	public ByteBufferArray reset() {
		long m = mark;
		if (m < 0)
			throw new InvalidMarkException();
		return position(m, true);
	}

	/**
	 * Rewinds this buffer array wrapper. The position is set to zero and the mark
	 * is discarded.
	 * 
	 * @return This buffer array wrapper
	 */
	public ByteBufferArray rewind() {
		for (int i = off; i < end; ++i) {
			array[i].rewind();
		}
		if (size > 0) {
			buffer = this.array[off];
		}
		index = off;
		mark = -1;
		return this;
	}
	
	/**
	 * Creates a new byte buffer array wrapper that shares this buffer array
	 * wrapper's content.
	 * <p>
	 * The new buffer array wrapper's limit, position, and mark values will be
	 * identical to those of this buffer array wrapper and they values will be
	 * changed independently.
	 * <p>
	 * 
	 * @return The new byte buffer array wrapper
	 */
	public ByteBufferArray duplicate() {
		ByteBuffer[] dup = new ByteBuffer[array.length];

		for (int i = off; i < end; ++i) {
			dup[i] = array[i].duplicate();
		}
		ByteBufferArray dupArray = new ByteBufferArray(dup, off, size);
		dupArray.index = index;
		dupArray.mark = mark;
		if (buffer != null) {
			dupArray.buffer = dup[index];
		}
		return dupArray;
	}

	private ByteBuffer buffer() {
		if (buffer == null) {
			throw new BufferUnderflowException();
		}
		while (!buffer.hasRemaining()) {
			++index;
			if (index >= end) {
				throw new BufferUnderflowException();
			}
			buffer = array[index];
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
		for (int b = off; b < end; ++b) {
			ByteBuffer buf = array[b];
			int l = buf.limit();

			if (i >= l) {
				i -= l;
			} else if (i <= l - size) {
				index[0] = i;
				return buf;
			} else {
				byte[] data = new byte[size];

				for (int j = 0; j < size; ++j) {
					if (i < l) {
						data[j] = buf.get(i++);
					} else if (++b < end) {
						buf = array[b];
						l = buf.limit();
						i = 0;
						--j;
					} else {
						throw new IndexOutOfBoundsException();
					}
				}
				index[0] = 0;
				return ByteBuffer.wrap(data).order(buf.order());
			}
		}
		throw new IndexOutOfBoundsException();
	}

	/**
	 * Relative get method that transfers bytes from this buffer array wrapper into
	 * the given destination byte array.
	 * <p>
	 * If there are fewer bytes remaining in the buffer than are required to satisfy
	 * the request then no bytes are transferred and a BufferUnderflowException is
	 * thrown. Otherwise, the bytes are copied into the given array and the position
	 * of this buffer array wrapper is incremented by number of copied bytes.
	 * 
	 * @param dst the destination byte array
	 * @return This buffer array wrapper
	 * @throws BufferUnderflowException If there are fewer than length bytes
	 *                                  remaining in this buffer buffer wrapper
	 */
	public ByteBufferArray get(byte[] dst) {
		return get(dst, 0, dst.length);
	}

	/**
	 * Relative get method that transfers bytes from this buffer array wrapper into
	 * the given destination byte array.
	 * <p>
	 * If there are fewer bytes remaining in the buffer than are required to satisfy
	 * the request then no bytes are transferred and a
	 * {@link BufferUnderflowException} is thrown. Otherwise, the bytes are copied
	 * into the given array and the position of this buffer array wrapper is
	 * incremented by number of copied bytes.
	 * 
	 * @param dst the destination byte array
	 * @param off the offset within the array of the first byte to be copied
	 * @param len the number of bytes to be copied into the given array
	 * @return This buffer array wrapper
	 * @throws BufferUnderflowException  If there are fewer than length bytes
	 *                                   remaining in this buffer buffer wrapper
	 * @throws IndexOutOfBoundsException If the preconditions on the offset and
	 *                                   length parameters do not hold
	 */
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

		for (int i = index + 1; i < end; ++i) {
			remaining += array[i].remaining();
			if (remaining >= len) {
				while (true) {
					int size = Math.min(len, buffer.remaining());

					buffer.get(dst, off, size);
					off += size;
					len -= size;
					if (len > 0) {
						buffer = buffer();
					} else {
						return this;
					}
				}
			}
		}
		throw new BufferUnderflowException();
	}

	/**
	 * Relative get method that reads the byte at this buffer array wrapper's
	 * current position, and then increments the position.
	 * 
	 * @return The byte at the buffer array wrapper's current position
	 * @throws BufferUnderflowException If the buffer array wrapper's current
	 *                                  position is not smaller than its limit
	 */
	public byte get() {
		return buffer().get();
	}

	/**
	 * Absolute get method that reads the byte at the given position.
	 * 
	 * @param position The position from which the byte will be read
	 * @return The byte at the given position
	 * @throws IndexOutOfBoundsException If position is negative or not smaller than
	 *                                   the buffer array wrapper's limit
	 */
	public byte get(int position) {
		int[] indexArray = new int[] { position };
		return buffer(1, indexArray).get(indexArray[0]);
	}

	/**
	 * Relative get method for reading a char value.
	 * <p>
	 * It reads the next two bytes at this buffer array wrapper's current position,
	 * composing them into a char value according to the current byte order, and
	 * then increments the position by two.
	 * <p>
	 * NOTE: The current byte order is determined by the current byte order of the
	 * buffer pointed by the current position of this buffer array wrapper.
	 * 
	 * @return The char value at the buffer array wrapper's current position
	 * @throws BufferUnderflowException  If there are fewer than two bytes remaining
	 *                                   in this buffer array wrapper
	 */
	public char getChar() {
		return buffer(2).getChar();
	}

	/**
	 * Absolute get method for reading a char value.
	 * <p>
	 * It reads two bytes at the given position, composing them into a char value
	 * according to the current byte order.
	 * <p>
	 * NOTE: The current byte order is determined by the current byte order of the
	 * buffer pointed by the given position.
	 * 
	 * @param position The position from which the bytes will be read
	 * @return The char value at the given position
	 * @throws IndexOutOfBoundsException If position is negative or not smaller than
	 *                                   the buffer array wrapper's limit, minus one
	 */
	public char getChar(int position) {
		int[] indexArray = new int[] { position };
		return buffer(2, indexArray).getChar(indexArray[0]);
	}

	/**
	 * Relative get method for reading a short value.
	 * <p>
	 * It reads the next two bytes at this buffer array wrapper's current position,
	 * composing them into a short value according to the current byte order, and
	 * then increments the position by two.
	 * <p>
	 * NOTE: The current byte order is determined by the current byte order of the
	 * buffer pointed by the current position of this buffer array wrapper.
	 * 
	 * @return The short value at the buffer array wrapper's current position
	 * @throws BufferUnderflowException  If there are fewer than two bytes remaining
	 *                                   in this buffer array wrapper
	 */
	public short getShort() {
		return buffer(2).getShort();
	}

	/**
	 * Absolute get method for reading a short value.
	 * <p>
	 * It reads two bytes at the given position, composing them into a short value
	 * according to the current byte order.
	 * <p>
	 * NOTE: The current byte order is determined by the current byte order of the
	 * buffer pointed by the given position.
	 * 
	 * @param position The position from which the bytes will be read
	 * @return The short value at the given position
	 * @throws IndexOutOfBoundsException If position is negative or not smaller than
	 *                                   the buffer array wrapper's limit, minus one
	 */
	public short getShort(int position) {
		int[] indexArray = new int[] { position };
		return buffer(2, indexArray).getShort(indexArray[0]);
	}

	/**
	 * Relative get method for reading an int value.
	 * <p>
	 * It reads the next four bytes at this buffer array wrapper's current position,
	 * composing them into an int value according to the current byte order, and
	 * then increments the position by four.
	 * <p>
	 * NOTE: The current byte order is determined by the current byte order of the
	 * buffer pointed by the current position of this buffer array wrapper.
	 * 
	 * @return The int value at the buffer array wrapper's current position
	 * @throws BufferUnderflowException If there are fewer than four bytes remaining
	 *                                  in this buffer array wrapper
	 */
	public int getInt() {
		return buffer(4).getInt();
	}

	/**
	 * Absolute get method for reading an int value.
	 * <p>
	 * It reads four bytes at the given position, composing them into an int value
	 * according to the current byte order.
	 * <p>
	 * NOTE: The current byte order is determined by the current byte order of the
	 * buffer pointed by the given position.
	 * 
	 * @param position The position from which the bytes will be read
	 * @return The int value at the given position
	 * @throws IndexOutOfBoundsException If position is negative or not smaller than
	 *                                   the buffer array wrapper's limit, minus
	 *                                   three
	 */
	public int getInt(int position) {
		int[] indexArray = new int[] { position };
		return buffer(4, indexArray).getInt(indexArray[0]);
	}

	/**
	 * Relative get method for reading a long value.
	 * <p>
	 * It reads the next eight bytes at this buffer array wrapper's current position,
	 * composing them into a long value according to the current byte order, and
	 * then increments the position by eight.
	 * <p>
	 * NOTE: The current byte order is determined by the current byte order of the
	 * buffer pointed by the current position of this buffer array wrapper.
	 * 
	 * @return The long value at the buffer array wrapper's current position
	 * @throws BufferUnderflowException If there are fewer than eight bytes remaining
	 *                                  in this buffer array wrapper
	 */
	public long getLong() {
		return buffer(8).getLong();
	}

	/**
	 * Absolute get method for reading a long value.
	 * <p>
	 * It reads eight bytes at the given position, composing them into a long value
	 * according to the current byte order.
	 * <p>
	 * NOTE: The current byte order is determined by the current byte order of the
	 * buffer pointed by the given position.
	 * 
	 * @param position The position from which the bytes will be read
	 * @return The long value at the given position
	 * @throws IndexOutOfBoundsException If position is negative or not smaller than
	 *                                   the buffer array wrapper's limit, minus
	 *                                   seven
	 */
	public long getLong(int position) {
		int[] indexArray = new int[] { position };
		return buffer(8, indexArray).getLong(indexArray[0]);
	}

	/**
	 * Relative get method for reading a float value.
	 * <p>
	 * It reads the next four bytes at this buffer array wrapper's current position,
	 * composing them into a float value according to the current byte order, and
	 * then increments the position by four.
	 * <p>
	 * NOTE: The current byte order is determined by the current byte order of the
	 * buffer pointed by the current position of this buffer array wrapper.
	 * 
	 * @return The float value at the buffer array wrapper's current position
	 * @throws BufferUnderflowException If there are fewer than four bytes remaining
	 *                                  in this buffer array wrapper
	 */
	public float getFloat() {
		return buffer(4).getFloat();
	}

	/**
	 * Absolute get method for reading a float value.
	 * <p>
	 * It reads four bytes at the given position, composing them into a float value
	 * according to the current byte order.
	 * <p>
	 * NOTE: The current byte order is determined by the current byte order of the
	 * buffer pointed by the given position.
	 * 
	 * @param position The position from which the bytes will be read
	 * @return The float value at the given position
	 * @throws IndexOutOfBoundsException If position is negative or not smaller than
	 *                                   the buffer array wrapper's limit, minus
	 *                                   three
	 */
	public float getFloat(int position) {
		int[] indexArray = new int[] { position };
		return buffer(4, indexArray).getFloat(indexArray[0]);
	}

	/**
	 * Relative get method for reading a double value.
	 * <p>
	 * It reads the next eight bytes at this buffer array wrapper's current position,
	 * composing them into a double value according to the current byte order, and
	 * then increments the position by eight.
	 * <p>
	 * NOTE: The current byte order is determined by the current byte order of the
	 * buffer pointed by the current position of this buffer array wrapper.
	 * 
	 * @return The double value at the buffer array wrapper's current position
	 * @throws BufferUnderflowException If there are fewer than eight bytes remaining
	 *                                  in this buffer array wrapper
	 */
	public double getDouble() {
		return buffer(8).getDouble();
	}

	/**
	 * Absolute get method for reading a double value.
	 * <p>
	 * It reads eight bytes at the given position, composing them into a double value
	 * according to the current byte order.
	 * <p>
	 * NOTE: The current byte order is determined by the current byte order of the
	 * buffer pointed by the given position.
	 * 
	 * @param position The position from which the bytes will be read
	 * @return The double value at the given position
	 * @throws IndexOutOfBoundsException If position is negative or not smaller than
	 *                                   the buffer array wrapper's limit, minus
	 *                                   seven
	 */
	public double getDouble(int position) {
		int[] indexArray = new int[] { position };
		return buffer(8, indexArray).getDouble(indexArray[0]);
	}

}
