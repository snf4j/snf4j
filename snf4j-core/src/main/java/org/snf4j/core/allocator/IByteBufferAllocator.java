/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2019 SNF4J contributors
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
package org.snf4j.core.allocator;

import java.nio.ByteBuffer;

/**
 * Allocates and manages {@link ByteBuffer}s used by the sessions for I/O
 * operations.
 * <p>
 * The implementation should be thread safe.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IByteBufferAllocator {

	/**
	 * Tells whether or not this allocator allocates buffers that are backed by
	 * an accessible byte array.
	 * 
	 * @return true if the buffers allocated by this allocator are backed by an
	 *         accessible byte array
	 */
	boolean usesArray();
	
	/**
	 * Allocates a new byte buffer.
	 * 
	 * @param capacity
	 *            The new buffer's capacity, in bytes
	 * @return The new byte buffer
	 */
	ByteBuffer allocate(int capacity);
	
	/**
	 * Tells if an implementation of the allocator supports releasing of no
	 * longer used buffers.
	 * 
	 * @return <code>true</code> if the releasing is supported
	 */
	boolean isReleasable();
	
	/**
	 * Informs that given buffer that was allocated by this allocator is no
	 * longer used and can be released.
	 * 
	 * @param buffer
	 *            buffer to be released
	 */
	void release(ByteBuffer buffer);
	
	/**
	 * Ensures that returned buffer will contain some room for new data. The
	 * content of the original buffer must be preserved in the returned buffer.
	 * If the original buffer is full and its capacity reached the max capacity
	 * the method should throw an {@link IndexOutOfBoundsException}.
	 * <p>
	 * It is only used to manage the capacity of the input buffer associated
	 * with the stream-oriented sessions.
	 * 
	 * @param buffer
	 *            buffer in the write mode (i.e. not flipped yet)
	 * @param minCapacity
	 *            min capacity
	 * @param maxCapacity
	 *            max capacity
	 * @return the original buffer or new one in the write mode.
	 * @throws IndexOutOfBoundsException
	 *             if the buffer is full and its capacity reached the max
	 *             capacity
	 */
	ByteBuffer ensureSome(ByteBuffer buffer, int minCapacity, int maxCapacity);

	/**
	 * Assures that returned buffer will contain room for new data. The
	 * content of the original buffer must be preserved in the returned buffer.
	 * If there is no room in the buffer for the new data the method should throw 
	 * an {@link IndexOutOfBoundsException}.
	 * <p>
	 * It is only used to manage the capacity of the input buffer associated
	 * with the engine driven stream-oriented sessions.
	 * 
	 * @param buffer
	 *            buffer in the write mode (i.e. not flipped yet)
	 * @param size
	 *            size of data to be put to the buffer 
	 * @param minCapacity
	 *            min capacity
	 * @param maxCapacity
	 *            max capacity
	 * @return the original buffer or new one in the write mode.
	 * @throws IndexOutOfBoundsException
	 *             if there is no room in the buffer for the new data
	 */
	ByteBuffer ensure(ByteBuffer buffer, int size, int minCapacity, int maxCapacity);	
	
	/**
	 * Tries to reduce the capacity of the buffer based on its content. The
	 * content of the original buffer must be preserved in the returned buffer
	 * <p>
	 * It is only used to manage the capacity of the output buffers associated
	 * with the stream-oriented sessions.
	 * 
	 * @param buffer
	 *            buffer in the write mode (i.e. not flipped yet)
	 * @param minCapacity
	 *            min capacity
	 * @return the original buffer or new one in the write mode.
	 */
	ByteBuffer reduce(ByteBuffer buffer, int minCapacity);
	
	/**
	 * Extends the capacity of the buffer based on its current capacity. The
	 * content of the original buffer must be preserved in the returned buffer.
	 * <p>
	 * It is only used to manage the capacity of the input buffer associated
	 * with the datagram-oriented sessions. The method is called after the 
	 * received datagram fully filled up the input buffer.
	 * 
	 * @param buffer
	 *            buffer in the write mode (i.e. not flipped yet)
	 * @param maxCapacity
	 *            max capacity
	 * @return the new buffer
	 */
	ByteBuffer extend(ByteBuffer buffer, int maxCapacity);
}
