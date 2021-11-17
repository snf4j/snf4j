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

import java.net.InetSocketAddress;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.session.ISessionConfig;

/**
 * Handles client proxy connections via the SOCKS Protocol Version 4.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class Socks4ProxyHandler extends AbstractSocksProxyHandler {

	/**
	 * Constructs a SOCKS4 proxy connection handler with the specified destination
	 * address, the default ({@link Socks4Command#CONNECT CONNECT}) command, user's
	 * name and the default (10 seconds) connection timeout.
	 * <p>
	 * NOTE: The connection timeout will have no effect if the associated session
	 * does not support a session timer.
	 * 
	 * @param address           the destination address to the host to which the
	 *                          connection should be proxied
	 * @param username          the user's name, or {@code null} for an empty name
	 * @throws IllegalArgumentException if the address is null
	 */
	public Socks4ProxyHandler(InetSocketAddress address, String username) {
		this(address, Socks4Command.CONNECT, username, null, null);
	}

	/**
	 * Constructs a SOCKS4 proxy connection handler with the specified destination
	 * address, the default ({@link Socks4Command#CONNECT CONNECT}) command, user's
	 * name, the default (10 seconds) connection timeout and configuration.
	 * <p>
	 * NOTE: The connection timeout will have no effect if the associated session
	 * does not support a session timer.
	 * 
	 * @param address           the destination address to the host to which the
	 *                          connection should be proxied
	 * @param username          the user's name, or {@code null} for an empty name
	 * @param config            the session configuration object, or {@code null} to
	 *                          use the default configuration
	 * @throws IllegalArgumentException if the address is null
	 */
	public Socks4ProxyHandler(InetSocketAddress address, String username, ISessionConfig config) {
		this(address, Socks4Command.CONNECT, username, config, null);
	}

	/**
	 * Constructs a SOCKS4 proxy connection handler with the specified destination
	 * address, the default ({@link Socks4Command#CONNECT CONNECT}) command, user's
	 * name, the default (10 seconds) connection timeout, configuration and factory.
	 * <p>
	 * NOTE: The connection timeout will have no effect if the associated session
	 * does not support a session timer.
	 * 
	 * @param address           the destination address to the host to which the
	 *                          connection should be proxied
	 * @param username          the user's name, or {@code null} for an empty name
	 * @param config            the session configuration object, or {@code null} to
	 *                          use the default configuration
	 * @param factory           the factory that will be used to configure the
	 *                          internal structure of the associated session, or
	 *                          {@code null} to use the default factory
	 * @throws IllegalArgumentException if the address is null
	 */
	public Socks4ProxyHandler(InetSocketAddress address, String username, ISessionConfig config, ISessionStructureFactory factory) {
		this(address, Socks4Command.CONNECT, username, config, factory);
	}
	
	/**
	 * Constructs a SOCKS4 proxy connection handler with the specified destination
	 * address, SOCKS4 command, user's name and the default (10 seconds) connection
	 * timeout.
	 * <p>
	 * NOTE: The connection timeout will have no effect if the associated session
	 * does not support a session timer.
	 * 
	 * @param address  the destination address to the host to which the connection
	 *                 should be proxied
	 * @param command  the SCOCS4 command
	 * @param username the user's name, or {@code null} for an empty name
	 * @throws IllegalArgumentException if the address or the command is null
	 */
	public Socks4ProxyHandler(InetSocketAddress address, Socks4Command command, String username) {
		this(address, command, username, null, null);
	}

	/**
	 * Constructs a SOCKS4 proxy connection handler with the specified destination
	 * address, SOCKS4 command, user's name, the default (10 seconds) connection
	 * timeout and configuration.
	 * <p>
	 * NOTE: The connection timeout will have no effect if the associated session
	 * does not support a session timer.
	 * 
	 * @param address  the destination address to the host to which the connection
	 *                 should be proxied
	 * @param command  the SCOCS4 command
	 * @param username the user's name, or {@code null} for an empty name
	 * @param config   the session configuration object, or {@code null} to use the
	 *                 default configuration
	 * @throws IllegalArgumentException if the address or the command is null
	 */
	public Socks4ProxyHandler(InetSocketAddress address, Socks4Command command, String username, ISessionConfig config) {
		this(address, command, username, config, null);
	}
	
	/**
	 * Constructs a SOCKS4 proxy connection handler with the specified destination
	 * address, SOCKS4 command, user's name, the default (10 seconds) connection
	 * timeout, configuration and factory.
	 * <p>
	 * NOTE: The connection timeout will have no effect if the associated session
	 * does not support a session timer.
	 * 
	 * @param address  the destination address to the host to which the connection
	 *                 should be proxied
	 * @param command  the SCOCS4 command
	 * @param username the user's name, or {@code null} for an empty name
	 * @param config   the session configuration object, or {@code null} to use the
	 *                 default configuration
	 * @param factory  the factory that will be used to configure the internal
	 *                 structure of the associated session, or {@code null} to use
	 *                 the default factory
	 * @throws IllegalArgumentException if the address or the command is null
	 */
	public Socks4ProxyHandler(InetSocketAddress address, Socks4Command command, String username, ISessionConfig config, ISessionStructureFactory factory) {
		super(address, config, factory);
		checkNull(command, "command");
		state = new Socks4CommandState(this, command, username);
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalArgumentException if the connection timeout is negative 
	 */
	@Override
	public Socks4ProxyHandler connectionTimeout(long connectionTimeout) {
		super.connectionTimeout(connectionTimeout);
		return this;
	}

	@Override
	String protocol() {
		return ISocks4.PROTOCOL;
	}

}
