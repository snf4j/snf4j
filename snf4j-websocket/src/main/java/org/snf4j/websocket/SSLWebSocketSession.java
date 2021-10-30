/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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
package org.snf4j.websocket;

import java.net.SocketAddress;

import org.snf4j.core.SSLSession;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.future.TaskFuture;
import org.snf4j.core.session.SSLEngineCreateException;
import org.snf4j.websocket.handshake.IHandshaker;

/**
 * The Secure Web Socket session implementing the WebSocket Protocol described in RFC 6455.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class SSLWebSocketSession extends SSLSession implements IWebSocketSession {

	private final TaskFuture<Void> readyFuture;
	
	/**
	 * Constructs a named Secure Web Socket session associated with a handler and a
	 * remote peer.
	 * 
	 * @param name          the name for this session, or <code>null</code> if the
	 *                      handler's name should be used for this session's name
	 * @param remoteAddress the address of the remote peer
	 * @param handler       the Web Socket handler that should be associated with
	 *                      this session
	 * @param clientMode    <code>true</code> if the session should start its
	 *                      handshaking (SSL/TLS and Web Socket) in "client" mode
	 * @throws SSLEngineCreateException when the SSL engine could not be created
	 */
	public SSLWebSocketSession(String name, SocketAddress remoteAddress, IWebSocketHandler handler, boolean clientMode) 
			throws SSLEngineCreateException {
		super(name, remoteAddress, new WebSocketSessionHandler(handler, clientMode), clientMode);
		readyFuture = new TaskFuture<Void>(this);
	}

	/**
	 * Constructs a Secure Web Socket session associated with a handler and a
	 * remote peer.
	 * 
	 * @param remoteAddress the address of the remote peer
	 * @param handler       the Web Socket handler that should be associated with
	 *                      this session
	 * @param clientMode    <code>true</code> if the session should start its
	 *                      handshaking (SSL/TLS and Web Socket) in "client" mode
	 * @throws SSLEngineCreateException when the SSL engine could not be created
	 */
	public SSLWebSocketSession(SocketAddress remoteAddress, IWebSocketHandler handler, boolean clientMode)
			throws SSLEngineCreateException {
		super(remoteAddress, new WebSocketSessionHandler(handler, clientMode), clientMode);
		readyFuture = new TaskFuture<Void>(this);
	}
	
	/**
	 * Constructs a named Secure Web Socket session associated with a handler.
	 * 
	 * @param name          the name for this session, or <code>null</code> if the
	 *                      handler's name should be used for this session's name
	 * @param handler       the Web Socket handler that should be associated with
	 *                      this session
	 * @param clientMode    <code>true</code> if the session should start its
	 *                      handshaking (SSL/TLS and Web Socket) in "client" mode
	 * @throws SSLEngineCreateException when the SSL engine could not be created
	 */
	public SSLWebSocketSession(String name, IWebSocketHandler handler, boolean clientMode) 
			throws SSLEngineCreateException {
		super(name, new WebSocketSessionHandler(handler, clientMode), clientMode);
		readyFuture = new TaskFuture<Void>(this);
	}
	
	/**
	 * Constructs a Secure Web Socket session associated with a handler.
	 * 
	 * @param handler       the Web Socket handler that should be associated with
	 *                      this session
	 * @param clientMode    <code>true</code> if the session should start its
	 *                      handshaking (SSL/TLS and Web Socket) in "client" mode
	 * @throws SSLEngineCreateException when the SSL engine could not be created
	 */
	public SSLWebSocketSession(IWebSocketHandler handler, boolean clientMode)
			throws SSLEngineCreateException {
		super(new WebSocketSessionHandler(handler, clientMode), clientMode);
		readyFuture = new TaskFuture<Void>(this);
	}

	/**
	 * Gets the future that can be use to wait for the completion of the Web Socket
	 * handshake phase.
	 * 
	 * @return the future associated with the Web Socket handshake phase of this
	 *         session
	 */
	@Override
	public IFuture<Void> getReadyFuture() {
		return readyFuture;
	}	
	
	@Override
	public IWebSocketHandler getWebSocketHandler() {
		return ((WebSocketSessionHandler)getHandler()).getHandler();
	}	
	
	@Override
	public IHandshaker getHandshaker() {
		return ((WebSocketSessionHandler)getHandler()).getHandshaker();
	}
	
	@Override
	public void close(int status) {
		executenf(new WebSocketSession.CloseTask(this, status, null));
	}

	@Override
	public void close(int status, String reason) {
		executenf(new WebSocketSession.CloseTask(this, status, reason));
	}	
}
