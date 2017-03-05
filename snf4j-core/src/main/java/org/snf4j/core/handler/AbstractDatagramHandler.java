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

import org.snf4j.core.session.IDatagramSession;
import org.snf4j.core.session.ISession;

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
	 * Constructor creating a named datagram-oriented handler.
	 * 
	 * @param name
	 *            the name for this handler
	 */
	protected AbstractDatagramHandler(String name) {
		super(name);
	}

	@Override
	public void setSession(IDatagramSession session) {
		super.setSession(session);
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
	
}
