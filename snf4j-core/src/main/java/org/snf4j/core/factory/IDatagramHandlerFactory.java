/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020 SNF4J contributors
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
package org.snf4j.core.factory;

import java.net.SocketAddress;

import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.DatagramServerHandler;

/**
 * Factory used to create a datagram handler that will be associated with 
 * a datagram-oriented session created by {@link DatagramServerHandler}.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IDatagramHandlerFactory {
	
	/**
	 * Creates a datagram handler that will be associated with a
	 * datagram-oriented session created by {@link DatagramServerHandler} after
	 * receiving first data from a remote host.
	 * 
	 * @param remoteAddress
	 *            the address of the remote host.
	 * @return the handler or {@code null} if the creation of the session should be
	 *         silently skipped.
	 */
	IDatagramHandler create(SocketAddress remoteAddress);
}
