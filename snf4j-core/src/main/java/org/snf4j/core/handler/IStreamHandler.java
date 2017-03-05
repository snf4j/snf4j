/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017 SNF4J contributors
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
package org.snf4j.core.handler;

import java.nio.ByteBuffer;

import org.snf4j.core.session.IDatagramSession;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.IStreamSession;

/**
 * Extends the {@link IHandler} interface to cover stream-oriented functionalities.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IStreamHandler extends IHandler {

	/**
	 * Set the stream-oriented session that will be associated with this handler.
	 * 
	 * @param session
	 *            the stream-oriented session
	 */
	void setSession(IStreamSession session);

	/**
	 * Set the stream-oriented session that will be associated with this handler.
	 * 
	 * @param session
	 *            the stream-oriented session
	 * @throws IllegalArgumentException
	 *             when the session argument is not an instance of the
	 *             {@link IDatagramSession} interface.
	 */
	@Override
	void setSession(ISession session);
	
	/**
	 * Returns the stream-oriented session that is associated with this
	 * handler.
	 * 
	 * @return the stream-oriented session
	 */
	@Override
	IStreamSession getSession();
	
	/**
	 * Determines how many bytes can be read from the input buffer after receiving new data
	 * from the remote end. The specified number of bytes will be then passed as a byte 
	 * array to the <code>read</code> method. This method is called only if the 
	 * input buffer is not backed by an array. The type of the input buffer is specified
	 * by the implementation of the {@link org.snf4j.core.factory.ISessionStructureFactory} returned by the 
	 * <code>getFactory</code> method.
	 * <p>The inner state of the passed buffer cannot be changed by this method.   
	 * @param buffer the input buffer.
	 * @param flipped specifies the current mode of the buffer
	 * @return the number of bytes to read or 0 if the buffer has not enough data yet and the
	 * reading should skipped.
	 */
	int toRead(ByteBuffer buffer, boolean flipped);
	
	/**
	 * Determines how many bytes can be read from the input buffer after receiving new data
	 * from the remote end. The specified number of bytes will be then passed as a byte 
	 * array to the <code>read</code> method. This method is called only if the 
	 * input buffer is backed by an array. The type of the input buffer is specified
	 * by the implementation of the {@link org.snf4j.core.factory.ISessionStructureFactory} returned by the 
	 * <code>getFactory</code> method.
	 * <p>The content of the passed buffer cannot be changed by this method.   
	 * @param buffer the array that backs the input buffer.
	 * @param off the offset of the array
	 * @param len the number of the bytes in the array 
	 * @return number of bytes to read
	 */
	int toRead(byte[] buffer, int off, int len);

}
