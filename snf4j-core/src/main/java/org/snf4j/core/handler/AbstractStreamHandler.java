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

import java.nio.ByteBuffer;

import org.snf4j.core.session.ISession;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.session.IStreamSession;

/**
 * Base implementation of the {@link IStreamHandler} interface.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
abstract public class AbstractStreamHandler extends AbstractHandler implements IStreamHandler {

	/**
	 * Default constructor creating an unnamed stream-oriented handler.
	 */
	protected AbstractStreamHandler() {
	}
	
	/**
	 * Constructor creating an unnamed stream-oriented handler with given session
	 * configuration object.
	 * 
	 * @param config 
	 *            the session configuration object, or {@code null} to
	 *            use the default configuration
	 */	
	 protected AbstractStreamHandler(ISessionConfig config) {
		super(config);
	}
	
	/**
	 * Constructor creating a named stream-oriented handler.
	 * 
	 * @param name
	 *            the name for this handler
	 */
	protected AbstractStreamHandler(String name) {
		super(name);
	}
	
	/**
	 * Constructor creating a named stream-oriented handler with given session
	 * configuration object.
	 * 
	 * @param name
	 *            the name for this handler
	 * @param config 
	 *            the session configuration object, or {@code null} to
	 *            use the default configuration
	 */
	protected AbstractStreamHandler(String name, ISessionConfig config) {
		super(name, config);
	}
	
	
	/**
	 * Sets the stream-oriented session that will be associated with this
	 * handler.
	 * 
	 * @param session
	 *            the session
	 * @throws IllegalArgumentException
	 *             if the session argument is not an instance of the
	 *             {@link IStreamSession} interface.
	 */
	@Override
	public void setSession(ISession session) {
		if (session instanceof IStreamSession) {
			super.setSession(session);
		}
		else {
			throw new IllegalArgumentException("session is not an instance of IStreamSession");
		}
	}

	@Override
	public IStreamSession getSession() {
		return (IStreamSession) super.getSession();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation always returns total number of bytes in the buffer.
	 */
	@Override
	public int available(ByteBuffer buffer, boolean flipped) {
		return flipped ? buffer.remaining() : buffer.position();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation always returns total number of bytes in the array 
	 * (i.e. the <code>len</code> value).
	 */
	@Override
	public int available(byte[] buffer, int off, int len) {
		return len;
	}
	
}
