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
package org.snf4j.core.factory;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.snf4j.core.StreamSession;

/**
 * Factory used to create a stream-oriented session for a newly accepted
 * connection. This interface is associated with the
 * {@link java.nio.channels.ServerSocketChannel} being registered with the
 * selector loop.
 * 
 * @see org.snf4j.core.SelectorLoop SelectorLoop
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IStreamSessionFactory {
	
	/**
	 * Creates a stream-oriented session for a newly accepted connection. This
	 * method can be also used to configure the newly accepted channel.
	 * 
	 * @param channel
	 *            the socket channel associated with the accepted connection.
	 * @return a stream-oriented session that will be associated with the
	 *         accepted connection
	 */
	StreamSession create(SocketChannel channel);
	
	/**
	 * Notifies about registration of an listening channel. Once it is called
	 * the associated selector loop is ready to accept new connections form the
	 * channel.
	 * 
	 * @param channel
	 *            an listening channel that has been registered
	 */
	void registered(ServerSocketChannel channel);
	
	/**
	 * Notifies about closing and unregistering of an listening channel. It is
	 * only called when the channel is closed by the associated selector loop.
	 * 
	 * @param channel
	 *            an listening channel that has been closed
	 */
	void closed(ServerSocketChannel channel);
}
