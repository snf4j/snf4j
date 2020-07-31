/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2020 SNF4J contributors
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

import java.net.SocketAddress;

import org.snf4j.core.IDatagramReader;
import org.snf4j.core.session.IDatagramSession;
import org.snf4j.core.session.ISession;

/**
 * Extends the {@link IHandler} interface to cover datagram-oriented functionalities.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IDatagramHandler extends IHandler, IDatagramReader {

	/**
	 * Sets the datagram-oriented session that will be associated with this handler.
	 * 
	 * @param session
	 *            the datagram-oriented session
	 * @throws IllegalArgumentException
	 *             when the session argument is not an instance of the
	 *             {@link IDatagramSession} interface.
	 */
	@Override
	void setSession(ISession session);
	
	/**
	 * Returns the datagram-oriented session that is associated with this
	 * handler.
	 * 
	 * @return the datagram-oriented session
	 */
	@Override
	IDatagramSession getSession();
	
	/**
	 * Called when a new message was received and decoded from a remote end
	 * that is identified by the given remote address. This method is called when
	 * the associated session is configured with a codec pipeline in which the
	 * last decoder produces outbound object(s) of type different than the
	 * {@code byte[]}.
	 * <p>
	 * The method is only called for sessions created with a disconnected
	 * datagram channel.
	 * 
	 * @param remoteAddress
	 *            address of the remote end.
	 * @param msg
	 *            the message received from the remote end.
	 */
	void read(SocketAddress remoteAddress, Object msg);
	
	/**
	 * Called to notify about an I/O operation related to a specified remote host.
	 * 
	 * @param remoteAddress
	 * 			  the address of the remote host
	 * @param event
	 *            an event related with the type of I/O operation
	 * @param length
	 *            the number of bytes related with the I/O operation
	 * @see DataEvent
	 */
	void event(SocketAddress remoteAddress, DataEvent event, long length);

}
