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
package org.snf4j.core.proxy;

import java.nio.ByteBuffer;
import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISessionTimer;
import org.snf4j.core.timer.ITimerTask;

/**
 * Base implementation for pre-handlers handling client connections via proxy
 * protocols.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
abstract public class AbstractProxyHandler extends AbstractStreamHandler {
	
	private final static long DEFAULT_CONNECTION_TIMEOUT = 10000;
	
	private final static Object CONNECTION_TIMER_EVENT = new Object();
	
	private final long connectionTimeout;

	private ITimerTask connectionTimer;
	
	/**
	 * Constructs a proxy connection pre-handler with the specified connection
	 * timeout.
	 * <p>
	 * NOTE: The connection timeout will have no effect if the associated session
	 * does not support a session timer.
	 * 
	 * @param connectionTimeout the proxy connection timeout in milliseconds, or
	 *                          {@code 0} to wait an infinite amount of time for the
	 *                          proxy connection.
	 */
	protected AbstractProxyHandler(long connectionTimeout) {
		if (connectionTimeout < 0) {
			throw new IllegalArgumentException("connectionTimeout is negative");
		}
		this.connectionTimeout = connectionTimeout;
	}
	
	/**
	 * Constructs a proxy connection pre-handler with the default (10 seconds)
	 * connection timeout.
	 * <p>
	 * NOTE: The connection timeout will have no effect if the associated session
	 * does not support a session timer.
	 */
	protected AbstractProxyHandler() {
		this.connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
	}
	
	@Override
	public int available(ByteBuffer data, boolean flipped) {
		if (data.hasArray()) {
			if (flipped) {
				return available(data.array(), data.arrayOffset() + data.position(), data.remaining());
			}
			return available(data.array(), data.arrayOffset(), data.position());
		}
		
		ByteBuffer dup = data.duplicate();
		byte[] array;
		
		if (!flipped) {
			dup.flip();
		}
		array = new byte[dup.remaining()];
		dup.get(array);
		return available(array, 0, array.length);
	}
	
	/**
	 * Called to initiate the connection process via a proxy protocol
	 * 
	 * @throws Exception if a failure occurred
	 */
	abstract protected void handleReady() throws Exception ;

	@SuppressWarnings("incomplete-switch")
	@Override
	public void event(SessionEvent event) {
		switch (event) {
		case OPENED:
			if (connectionTimeout > 0) {
				ISessionTimer timer = getSession().getTimer();

				if (timer.isSupported()) {
					connectionTimer = timer.scheduleEvent(CONNECTION_TIMER_EVENT, connectionTimeout);
				}
			}
			break;
			
		case READY:
			try {
				handleReady();
			}
			catch (Exception e) {
				getSession().getPipeline().markClosed(e);
				getSession().quickClose();
			}
			break;
			
		case ENDING:
			if (connectionTimer != null) {
				connectionTimer.cancelTask();
				connectionTimer = null;
			}
			break;
		}
	}
	
	@Override
	public void timer(Object event) {
		if (event == CONNECTION_TIMER_EVENT) {
			connectionTimer = null;
			getSession().getPipeline().markClosed(new ProxyConnectionTimeoutException("Proxy connection timed out"));
			getSession().quickClose();
		}
	}
	
}
