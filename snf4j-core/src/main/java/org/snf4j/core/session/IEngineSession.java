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
package org.snf4j.core.session;

import java.util.concurrent.Executor;

import org.snf4j.core.engine.IEngine;

/**
 * A engine-driven session which represents connection between end-points
 * regardless of transport type. Sessions implementing this interface handle
 * protocols driven by customized protocol engines implementing the
 * {@link IEngine} interface.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IEngineSession extends ISession {
	
	/**
	 * Sets the executor that will be used to execute delegated tasks required
	 * by this session to complete operations that block, or may take an
	 * extended period of time to complete.
	 * 
	 * @param executor
	 *            the new executor, or <code>null</code> to use the executor
	 *            configured in the selector loop that handles this session.
	 */
	void setExecutor(Executor executor);
	
	/**
	 * Returns the executor that is used to execute delegated tasks
	 * required by this session to complete operations that block, or may take
	 * an extended period of time to complete.
	 * <p>
	 * By default, this method returns the executor configured in the selector 
	 * loop that handles this session, or <code>null</code>.
	 * 
	 * @return the current executor, or <code>null</code> if the executor is
	 *         undefined (i.e. the session is not associated with the selector 
	 *         loop and the executor is not set)
	 */
	Executor getExecutor();
	
	/**
	 * Initiates handshaking (initial or renegotiation) on the protocol engine
	 * driving this session. After calling this method the handshake will start
	 * immediately.
	 * <p>
	 * This method is not needed for the initial handshake, as the <code>wrap</code> and 
	 * <code>unwrap</code> methods of the protocol engine should initiate it if the 
	 * handshaking has not already begun.
	 * <p>
	 * The operation is asynchronous.
	 */
	void beginHandshake();
	
	/**
	 * Initiates lazy handshaking (initial or renegotiation) on the protocol
	 * engine driving this session. After calling this method the handshake will
	 * not start immediately. It will start when new data is received from a
	 * remote peer or following methods are called: <code>write</code>,
	 * <code>writenf</code>, <code>beginHandshake</code>.
	 * <p>
	 * This method is not needed for the initial handshake, as the
	 * <code>wrap</code> and <code>unwrap</code> methods of the protocol engine
	 * should initiate it if the handshaking has not already begun.
	 * <p>
	 * The operation is asynchronous.
	 */
	void beginLazyHandshake();
	
	/**
	 * Returns an object representing a session in the protocol engine driving this
	 * session.
	 * 
	 * @return an object representing a session.
	 * @throws UnsupportedOperationException if the protocol engine driving this
	 *                                       session does not support a session.
	 */
	Object getEngineSession();
	
}
