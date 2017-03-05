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
import org.snf4j.core.handler.IStreamHandler;

/**
 * Base implementation of the {@link IStreamSessionFactory} interface.
 *
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public abstract class AbstractSessionFactory implements IStreamSessionFactory {

	/** Constructs the factory */
	protected AbstractSessionFactory() {
	}
	
	/**
	 * Creates a stream-oriented session for a newly accepted connection. The
	 * returned session will be associated with the handler returned by the
	 * <code>createHandler</code> method.
	 * 
	 * @param channel
	 *            the socket channel associated with the accepted connection.
	 * @return a stream-oriented session that will be associated with the
	 *         accepted connection
	 */
	@Override
	public StreamSession create(SocketChannel channel) {
		return new StreamSession(createHandler(channel));
	}
	
	/**
	 * Creates a stream-oriented handler for a newly accepted connection. This method can be also used to
	 * configure the newly accepted channel.
	 * 
	 * @param channel
	 *            the socket channel associated with the accepted connection.
	 * @return a stream-oriented handler that will be associated with the
	 *         session returned by the <code>create</code> method 
	 */
	protected abstract IStreamHandler createHandler(SocketChannel channel);

	@Override
	public void registered(ServerSocketChannel channel) {
	}

	@Override
	public void closed(ServerSocketChannel channel) {
	}
	
}
