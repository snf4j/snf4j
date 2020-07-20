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
package org.snf4j.core;

import java.net.SocketAddress;

import javax.net.ssl.SSLEngine;

import org.snf4j.core.engine.IEngine;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.IDatagramHandlerFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISessionConfig;

/**
 * DTLS server handler providing functionality to handle multiple remote
 * hosts via a single datagram-orinted session.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class DTLSServerHandler extends DatagramServerHandler {
	
	/**
	 * Constructs a DTLS server handler with the
	 * {@link DefaultSessionConfig} and the
	 * {@link DefaultSessionStructureFactory}.
	 * 
	 * @param handlerFactory
	 *            the factory used to create datagram handlers the will be
	 *            associated with newly created sessions for remote hosts
	 */
	public DTLSServerHandler(IDatagramHandlerFactory handlerFactory) {
		super(handlerFactory);
	}
	
	/**
	 * Constructs a DTLS server handler with the
	 * {@link DefaultSessionConfig}.
	 * 
	 * @param handlerFactory
	 *            the factory used to create datagram handlers the will be
	 *            associated with newly created sessions for remote hosts
	 * @param config
	 *            the configuration for a session associated with this datagram
	 *            server handler
	 */
	public DTLSServerHandler(IDatagramHandlerFactory handlerFactory, ISessionConfig config) {
		super(handlerFactory, config);
	}
	
	/**
	 * Constructs a DTLS server handler.
	 * 
	 * @param handlerFactory
	 *            the factory used to create datagram handlers the will be
	 *            associated with newly created sessions for remote hosts
	 * @param config
	 *            the configuration for a session associated with this handler
	 * @param factory
	 *            the factory used to configure the internal structure of a
	 *            session associated with this handler
	 */
	public DTLSServerHandler(IDatagramHandlerFactory handlerFactory, ISessionConfig config, ISessionStructureFactory factory) {
		super(handlerFactory, config, factory);
	}
	
	/**
	 * Determines if a newly created session should be a DTLS session driven by the
	 * {@link SSLEngine} created by the {@link ISessionConfig#createSSLEngine
	 * config.createSSLEngine()} method. If the {@link ISessionConfig#createSSLEngine
	 * config.createSSLEngine()} method returns {@code null} the session being created
	 * will not be an engine-driven session.
	 */
	@Override
	protected IEngine createEngine(SocketAddress remoteAddress, ISessionConfig config) throws Exception {
		SSLEngine engine = config.createSSLEngine(false);
		
		if (engine != null) {
			return new InternalSSLEngine(engine, config);
		}
		return null;
	}

}
