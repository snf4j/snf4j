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
package org.snf4j.core.session;

import org.snf4j.core.concurrent.IFuture;
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
	 * Writes bytes.
	 * <p>
	 * After returning from this method the passed byte array can be safely
	 * modified by the caller. The content of <code>data</code> is not 
	 * changed by this method.
	 * 
	 * @param data
	 *            bytes to be written
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(byte[] data);

}
