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
package org.snf4j.core;

import java.net.SocketAddress;

/**
 * A reader that reads data directly from a datagram channel.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IDatagramReader {
	
	/**
	 * Called when a new datagram was received from the remote end. As the
	 * remote end is not specified, the method is only called for sessions
	 * created with a connected datagram channel.
	 * <p>
	 * The passed array can be safely stored or modified by this method as it
	 * will not be used by the caller.
	 * 
	 * @param datagram
	 *            the datagram received from the remote end.
	 */
	void read(byte[] datagram);
	
	/**
	 * Called when a new datagram was received from the remote end that is
	 * specified by the given remote address. The method is only called for
	 * sessions created with a disconnected datagram channel.
	 * <p>
	 * The passed array can be safely stored or modified by this method as it
	 * will not be used by the caller.
	 * 
	 * @param remoteAddress
	 *            address of the remote host that is the source of the datagram.
	 * @param datagram
	 *            the datagram received from the remote end.
	 */
	void read(SocketAddress remoteAddress, byte[] datagram);
}
