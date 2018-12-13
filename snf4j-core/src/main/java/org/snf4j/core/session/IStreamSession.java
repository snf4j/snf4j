/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2018 SNF4J contributors
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
package org.snf4j.core.session;

import java.nio.ByteBuffer;

import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.IStreamHandler;

/**
 * Extends the {@link ISession} interface to cover stream-oriented functionalities.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IStreamSession extends ISession {

	/**
	 * Gets the stream-oriented handler associated with this session
	 * 
	 * @return the stream-oriented handler
	 */
	@Override
	IStreamHandler getHandler();

	/**
	 * Writes <code>data.length</code> bytes from the specified byte array
	 * to the stream-oriented channel associated with this session. 
	 * <p>
	 * The operation is asynchronous.
	 * 
	 * @param data
	 *            bytes to be written
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(byte[] data);

	/**
	 * Writes <code>data.length</code> bytes from the specified byte array
	 * to the stream-oriented channel associated with this session. 
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to 
	 * synchronize on a future object. This will save some resources and 
	 * may improve performance.
	 * 
	 * @param data
	 *            bytes to be written
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 */
	void writenf(byte[] data);
	
	/**
	 * Writes <code>length</code> bytes from the specified byte array
	 * to the stream-oriented channel associated with this session. 
	 * <p>
	 * The operation is asynchronous.
	 * 
	 * @param data
	 *            bytes to be written
	 * @param offset
	 *            offset within the array of the first byte to be written
	 * @param length
	 *            number of bytes to be written
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(byte[] data, int offset, int length);

	/**
	 * Writes <code>length</code> bytes from the specified byte array
	 * to the stream-oriented channel associated with this session. 
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to 
	 * synchronize on a future object. This will save some resources and 
	 * may improve performance.
	 * 
	 * @param data
	 *            bytes to be written
	 * @param offset
	 *            offset within the array of the first byte to be written
	 * @param length
	 *            number of bytes to be written
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 */
	void writenf(byte[] data, int offset, int length);
	
	/**
	 * Writes <code>data.remaining()</code> bytes from the specified 
	 * byte buffer to the stream-oriented channel associated with 
	 * this session. 
	 * <p>
	 * The operation is asynchronous.
	 * 
	 * @param data
	 *            bytes to be written
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(ByteBuffer data);

	/**
	 * Writes <code>data.remaining()</code> bytes from the specified 
	 * byte buffer to the stream-oriented channel associated with 
	 * this session. 
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to 
	 * synchronize on a future object. This will save some resources and 
	 * may improve performance.
	 * 
	 * @param data
	 *            bytes to be written
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 */
	void writenf(ByteBuffer data);
	
	/**
	 * Writes <code>length</code> bytes from the specified byte buffer
	 * to the stream-oriented channel associated with this session. 
	 * <p>
	 * The operation is asynchronous.
	 * 
	 * @param data
	 *            bytes to be written
	 * @param length
	 *            number of bytes to be written
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(ByteBuffer data, int length);	

	/**
	 * Writes <code>length</code> bytes from the specified byte buffer
	 * to the stream-oriented channel associated with this session. 
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to 
	 * synchronize on a future object. This will save some resources and 
	 * may improve performance.
	 * 
	 * @param data
	 *            bytes to be written
	 * @param length
	 *            number of bytes to be written
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 */
	void writenf(ByteBuffer data, int length);	
	
}
