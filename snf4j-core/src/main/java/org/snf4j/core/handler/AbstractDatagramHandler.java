/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2021 SNF4J contributors
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
import java.nio.ByteBuffer;

import org.snf4j.core.session.IDatagramSession;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.ISessionConfig;

/**
 * Base implementation of the {@link IDatagramHandler} interface.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public abstract class AbstractDatagramHandler extends AbstractHandler implements IDatagramHandler {
	
	/**
	 * Default constructor creating an unnamed datagram-oriented handler.
	 */
	protected AbstractDatagramHandler() {
	}
	
	/**
	 * Constructor creating an unnamed datagram-oriented handler with given session
	 * configuration object.
	 * 
	 * @param config 
	 *            the session configuration object
	 */	
	 protected AbstractDatagramHandler(ISessionConfig config) {
		super(config);
	}
	
	/**
	 * Constructor creating a named datagram-oriented handler.
	 * 
	 * @param name
	 *            the name for this handler
	 */
	protected AbstractDatagramHandler(String name) {
		super(name);
	}
	
	/**
	 * Constructor creating a named datagram-oriented handler with given session
	 * configuration object.
	 * 
	 * @param name
	 *            the name for this handler
	 * @param config 
	 *            the session configuration object
	 */
	protected AbstractDatagramHandler(String name, ISessionConfig config) {
		super(name, config);
	}

	/**
	 * Sets the datagram-oriented session that will be associated with this
	 * handler.
	 * 
	 * @param session
	 *            the session
	 * @throws IllegalArgumentException
	 *             if the session argument is not an instance of the
	 *             {@link IDatagramSession} interface.
	 */
	@Override
	public void setSession(ISession session) {
		if (session instanceof IDatagramSession) {
			super.setSession(session);
		}
		else {
			throw new IllegalArgumentException("session is not an instance of IDatagramSession");
		}
	}

	@Override
	public IDatagramSession getSession() {
		return (IDatagramSession) super.getSession();
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * By default it simply passes the {@code remoteAddress} and {@code data} values
	 * to the {@link IDatagramHandler#read(SocketAddress,Object)} method.
	 */
	@Override 
	public void read(SocketAddress remoteAddress, byte[] datagram) {
		read(remoteAddress, (Object)datagram);
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * By default it simply passes the {@code remoteAddress} and {@code data} values
	 * to the {@link IDatagramHandler#read(SocketAddress,Object)} method.
	 */
	@Override 
	public void read(SocketAddress remoteAddress, ByteBuffer datagram) {
		read(remoteAddress, (Object)datagram);
	}

	@Override
	public void event(SocketAddress remoteAddress, DataEvent event, long length) {
	}
}
