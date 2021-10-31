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

import java.net.URI;

import org.snf4j.core.handler.AbstractStreamHandler;

/**
 * Base implementation of the {@link IWebSocketHandler} interface.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
abstract public class AbstractWebSocketHandler extends AbstractStreamHandler implements IWebSocketHandler {
	
	/**
	 * Constructs an unnamed Web Socket handler for server sessions with the default
	 * configuration.
	 * 
	 * @see DefaultWebSocketSessionConfig
	 */
	protected AbstractWebSocketHandler() {
		super(new DefaultWebSocketSessionConfig());
	}

	/**
	 * Constructs an unnamed Web Socket handler for client sessions with the default
	 * configuration.
	 * 
	 * @param requestUri request URI that will be used to set the Host header field
	 *                   and the "Request-URI" of the GET method
	 * @see DefaultWebSocketSessionConfig
	 */
	protected AbstractWebSocketHandler(URI requestUri) {
		super(new DefaultWebSocketSessionConfig(requestUri));
	}

	/**
	 * Constructs a named Web Socket handler for server sessions with the default
	 * configuration.
	 * 
	 * @param name the name for this handler
	 * @see DefaultWebSocketSessionConfig
	 */
	protected AbstractWebSocketHandler(String name) {
		super(name, new DefaultWebSocketSessionConfig());
	}

	/**
	 * Constructs a named Web Socket handler for client sessions with the default
	 * configuration.
	 * 
	 * @param name       the name for this handler
	 * @param requestUri request URI that will be used to set the Host header field
	 *                   and the "Request-URI" of the GET method
	 * @see DefaultWebSocketSessionConfig
	 */
	protected AbstractWebSocketHandler(String name, URI requestUri) {
		super(name, new DefaultWebSocketSessionConfig(requestUri));
	}
	
	/**
	 * Constructs an unnamed Web Socket handler with the specified configuration.
	 * 
	 * @param config the Web Socket session configuration
	 */
	protected AbstractWebSocketHandler(IWebSocketSessionConfig config) {
		super(config);
	}

	/**
	 * Constructs a named Web Socket handler with the specified configuration.
	 * 
	 * @param name   the name for this handler
	 * @param config the Web Socket session configuration
	 */
	protected AbstractWebSocketHandler(String name, IWebSocketSessionConfig config) {
		super(name, config);
	}
	
	/**
	 * Returns the Web Socket configuration object that will be used to configure
	 * the behavior of the associated session.
	 * 
	 * @return the Web Socket configuration object
	 */
	@Override
	public IWebSocketSessionConfig getConfig() {
		return (IWebSocketSessionConfig) super.getConfig();
	}

}
