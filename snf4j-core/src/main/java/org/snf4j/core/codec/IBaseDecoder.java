/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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
package org.snf4j.core.codec;

import java.nio.ByteBuffer;

import org.snf4j.core.session.ISession;

/**
 * A special decoder that can determine how many bytes should be read from the
 * input buffer to create the byte array of the inbound data that can be decoded
 * by this decoder.
 * <p>
 * The special functionality (calling of the <code>available</code> methods) of
 * this decoder will only work if this decoder is the first decoder in the
 * pipeline. Otherwise, it will work as a regular decoder.
 * 
 * @param <O>
 *            the type of the produced outbound objects
 *            
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IBaseDecoder<O> extends IDecoder<byte[],O> {

	/**
	 * Determines how many bytes should be read from the input buffer to create 
	 * the byte array that will be then passed to the {@link IDecoder#decode} method. 
	 * 
	 * <p>This method is called only if the input buffer is not backed by an array
	 * (i.e. a direct byte buffer allocator is used to create session's internal
	 * buffers).
	 * 
	 * <p>The inner state of the passed buffer cannot be changed by this method.   
	 * 
	 * @param session
	 *            the {@link ISession} which the buffer belongs to
	 * @param buffer
	 *            the input buffer.
	 * @param flipped
	 *            specifies the current mode of the buffer
	 * @return the number of bytes to read or 0 if the buffer has not enough
	 *         data yet and the reading should skipped now.
	 */
	int available(ISession session, ByteBuffer buffer, boolean flipped);

	/**
	 * Determines how many bytes should be read from the input buffer to create
	 * the byte array that will be then passed to the {@link IDecoder#decode}
	 * method.
	 * 
	 * <p>This method is called only if the input buffer is backed by an array. 
	 * (i.e. a heap byte buffer allocator is used to create session's internal
	 * buffers).
	 *  
	 * <p>The content of the passed buffer cannot be changed by this method.
	 * 
	 * @param session
	 *            the {@link ISession} which the buffer belongs to
	 * @param buffer
	 *            the array that backs the input buffer.
	 * @param off
	 *            the offset of the array
	 * @param len
	 *            the number of the bytes in the array
	 * @return number of bytes to read or 0 if the buffer has not enough data
	 *         yet and the reading should skipped now.
	 */
	int available(ISession session, byte[] buffer, int off, int len);

}
