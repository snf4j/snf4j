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

import java.net.SocketAddress;

import org.snf4j.core.concurrent.IFuture;
import org.snf4j.core.handler.IDatagramHandler;

/**
 * Extends the {@link ISession} interface to cover datagram-oriented functionalities.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IDatagramSession extends ISession {
	
	/**
	 * Gets the datagram-oriented handler associated with this session
	 * 
	 * @return the datagram-oriented handler
	 */
	@Override
	IDatagramHandler getHandler();

	/**
	 * Writes a datagram to the associated datagram-oriented channel that needs
	 * to be connected.
	 * <p>
	 * After returning from this method the passed byte array should not be
	 * modified by the caller.
	 * 
	 * @param datagram
	 *            the datagram to be sent
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(byte[] datagram);
	
	/**
	 * Writes a datagram to the associated datagram-oriented channel that is not
	 * connected.
	 * <p>
	 * After returning from this method the passed byte array should not be
	 * modified by the caller.
	 * 
	 * @param remoteAddress
	 *            the address of the remote end where the datagram should be sent
	 * @param datagram
	 *            the datagram to be sent
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(SocketAddress remoteAddress, byte[] datagram);
}
