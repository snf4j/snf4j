/*
T * -------------------------------- MIT License --------------------------------
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

import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.core.session.SSLEngineCreateException;

/**
 * The stream-oriented session that handles SSL/TLS connections.
 * <p>
 * It uses {@link javax.net.ssl.SSLEngine SSLEngine} to handle secure protocols 
 * such as the Secure Sockets Layer (SSL) or IETF RFC 2246 "Transport Layer 
 * Security" (TLS) protocols.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */

public class SSLSession extends EngineStreamSession {

	private final static ILogger LOGGER = LoggerFactory.getLogger(SSLSession.class);

	/**
	 * Constructs the named SSL/TLS session associated with a handler and a 
	 * remote peer.
	 * 
	 * @param name
	 *            the name for this session, or <code>null</code> if the
	 *            handler's name should be used for this session's name
	 * @param remoteAddress
	 *            the address of the remote peer
	 * @param handler
	 *            the handler that should be associated with this session
	 * @param clientMode
	 *            <code>true</code> if the engine should start its handshaking
	 *            in "client" mode
	 * @throws SSLEngineCreateException
	 *             when the SSL engine could not be created
	 */
	public SSLSession(String name, SocketAddress remoteAddress, IStreamHandler handler, boolean clientMode) throws SSLEngineCreateException {
		super(name, new InternalSSLEngine(remoteAddress, handler.getConfig(), clientMode), handler , LOGGER);
	}
	
	/**
	 * Constructs the SSL/TLS session associated with a handler and a 
	 * remote peer.
	 * 
	 * @param remoteAddress
	 *            the address of the remote peer
	 * @param handler
	 *            the handler that should be associated with this session
	 * @param clientMode
	 *            <code>true</code> if the engine should start its handshaking
	 *            in "client" mode
	 * @throws SSLEngineCreateException
	 *             when the SSL engine could not be created
	 */
	public SSLSession(SocketAddress remoteAddress, IStreamHandler handler, boolean clientMode) throws SSLEngineCreateException {
		super(new InternalSSLEngine(remoteAddress, handler.getConfig(), clientMode), handler , LOGGER);
	}
	
	/**
	 * Constructs the named SSL/TLS session associated with a handler.
	 * 
	 * @param name
	 *            the name for this session, or <code>null</code> if the
	 *            handler's name should be used for this session's name
	 * @param handler
	 *            the handler that should be associated with this session
	 * @param clientMode
	 *            <code>true</code> if the engine should start its handshaking
	 *            in "client" mode
	 * @throws SSLEngineCreateException
	 *             when the SSL engine could not be created
	 */
	public SSLSession(String name, IStreamHandler handler, boolean clientMode) throws SSLEngineCreateException {
		super(name, new InternalSSLEngine(null, handler.getConfig(), clientMode), handler , LOGGER);
	}

	/**
	 * Constructs the SSL/TLS session associated with a handler.
	 * 
	 * @param handler
	 *            the handler that should be associated with this session
	 * @param clientMode
	 *            <code>true</code> if the engine should start its handshaking
	 *            in "client" mode
	 * @throws SSLEngineCreateException
	 *             when the SSL engine could not be created
	 */
	public SSLSession(IStreamHandler handler, boolean clientMode) throws SSLEngineCreateException {
		super(new InternalSSLEngine(null, handler.getConfig(), clientMode), handler , LOGGER);
	}
	
}
