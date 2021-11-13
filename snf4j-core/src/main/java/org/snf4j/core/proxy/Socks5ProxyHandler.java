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
 * Handles client proxy connections via the SOCKS Protocol Version 5. For more
 * details about the protocol refer to RFC 1928.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class Socks5ProxyHandler extends AbstractSocksProxyHandler {
	
	private final Socks5PasswordAuthState passwordAuthState;

	private final Socks5CommandState commandState;
	
	public Socks5ProxyHandler(InetSocketAddress address) {
		this(address, Socks5Command.CONNECT, null, null, null, null);
	}

	public Socks5ProxyHandler(InetSocketAddress address, ISessionConfig config) {
		this(address, Socks5Command.CONNECT, null, null, config, null);
	}

	public Socks5ProxyHandler(InetSocketAddress address, ISessionConfig config, ISessionStructureFactory factory) {
		this(address, Socks5Command.CONNECT, null, null, config, factory);
	}
	
	public Socks5ProxyHandler(InetSocketAddress address, String username, String password) {
		this(address, Socks5Command.CONNECT, username, password, null, null);
	}

	public Socks5ProxyHandler(InetSocketAddress address, String username, String password, ISessionConfig config) {
		this(address, Socks5Command.CONNECT, username, password, config, null);
	}

	public Socks5ProxyHandler(InetSocketAddress address, String username, String password, ISessionConfig config, ISessionStructureFactory factory) {
		this(address, Socks5Command.CONNECT, username, password, config, factory);
	}
	
	/**
	 * Constructs a SOCKS5 proxy connection handler with the specified destination
	 * address, SOCKS5 command and the default (10 seconds) connection timeout.
	 * <p>
	 * NOTE: The connection timeout will have no effect if the associated session
	 * does not support a session timer.
	 * 
	 * @param address the destination address to the host to which the connection
	 *                should be proxied
	 * @param command the SCOCS5 command
	 * @throws IllegalArgumentException if the address or the command is null
	 */
	public Socks5ProxyHandler(InetSocketAddress address, Socks5Command command) {
		this(address, command, null, null, null, null);
	}

	/**
	 * Constructs a SOCKS5 proxy connection handler with the specified destination
	 * address, SOCKS5 command, the default (10 seconds) connection timeout and
	 * configuration.
	 * <p>
	 * NOTE: The connection timeout will have no effect if the associated session
	 * does not support a session timer.
	 * 
	 * @param address the destination address to the host to which the connection
	 *                should be proxied
	 * @param command the SCOCS5 command
	 * @param config  the session configuration object, or {@code null} to use the
	 *                default configuration
	 * @throws IllegalArgumentException if the address or the command is null
	 */
	public Socks5ProxyHandler(InetSocketAddress address, Socks5Command command, ISessionConfig config) {
		this(address, command, null, null, config, null);
	}

	/**
	 * Constructs a SOCKS5 proxy connection handler with the specified destination
	 * address, SOCKS5 command, the default (10 seconds) connection timeout,
	 * configuration and factory.
	 * <p>
	 * NOTE: The connection timeout will have no effect if the associated session
	 * does not support a session timer.
	 * 
	 * @param address the destination address to the host to which the connection
	 *                should be proxied
	 * @param command the SCOCS5 command
	 * @param config  the session configuration object, or {@code null} to use the
	 *                default configuration
	 * @param factory the factory that will be used to configure the internal
	 *                structure of the associated session, or {@code null} to use
	 *                the default factory
	 * @throws IllegalArgumentException if the address or the command is null
	 */
	public Socks5ProxyHandler(InetSocketAddress address, Socks5Command command, ISessionConfig config, ISessionStructureFactory factory) {
		this(address, command, null, null, config, factory);
	}
	
	/**
	 * Constructs a SOCKS5 proxy connection handler with the specified destination
	 * address, SOCKS5 command, user's name/password and the default (10 seconds)
	 * connection timeout.
	 * <p>
	 * NOTE: The connection timeout will have no effect if the associated session
	 * does not support a session timer.
	 * 
	 * @param address  the destination address to the host to which the connection
	 *                 should be proxied
	 * @param command  the SCOCS5 command
	 * @param username the user's name, or {@code null} for an empty name
	 * @param password the user's password, or {@code null} for an empty password
	 * @throws IllegalArgumentException if the address or the command is null
	 */
	public Socks5ProxyHandler(InetSocketAddress address, Socks5Command command, String username, String password) {
		this(address, command, username, password, null, null);
	}

	/**
	 * Constructs a SOCKS5 proxy connection handler with the specified destination
	 * address, SOCKS5 command, user's name/password, the default (10 seconds)
	 * connection timeout and configuration.
	 * <p>
	 * NOTE: The connection timeout will have no effect if the associated session
	 * does not support a session timer.
	 * 
	 * @param address  the destination address to the host to which the connection
	 *                 should be proxied
	 * @param command  the SCOCS5 command
	 * @param username the user's name, or {@code null} for an empty name
	 * @param password the user's password, or {@code null} for an empty password
	 * @param config   the session configuration object, or {@code null} to use the
	 *                 default configuration
	 * @throws IllegalArgumentException if the address or the command is null
	 */
	public Socks5ProxyHandler(InetSocketAddress address, Socks5Command command, String username, String password, ISessionConfig config) {
		this(address, command, username, password, config, null);
	}
	
	public Socks5ProxyHandler(InetSocketAddress address, long connectionTimeout) {
		this(address, Socks5Command.CONNECT, null, null, connectionTimeout, null, null);
	}
	
	public Socks5ProxyHandler(InetSocketAddress address, long connectionTimeout, ISessionConfig config) {
		this(address, Socks5Command.CONNECT, null, null, connectionTimeout, config, null);
	}

	public Socks5ProxyHandler(InetSocketAddress address, long connectionTimeout, ISessionConfig config, ISessionStructureFactory factory) {
		this(address, Socks5Command.CONNECT, null, null, connectionTimeout, config, factory);
	}

	public Socks5ProxyHandler(InetSocketAddress address, String username, String password, long connectionTimeout) {
		this(address, Socks5Command.CONNECT, username, password, connectionTimeout, null, null);
	}

	public Socks5ProxyHandler(InetSocketAddress address, String username, String password, long connectionTimeout, ISessionConfig config) {
		this(address, Socks5Command.CONNECT, username, password, connectionTimeout, config, null);
	}

	public Socks5ProxyHandler(InetSocketAddress address, String username, String password, long connectionTimeout, ISessionConfig config, ISessionStructureFactory factory) {
		this(address, Socks5Command.CONNECT, username, password, connectionTimeout, config, factory);
	}
	
	/**
	 * Constructs a SOCKS5 proxy connection handler with the specified destination
	 * address, SOCKS5 command and connection timeout.
	 * <p>
	 * NOTE: The connection timeout will have no effect if the associated session
	 * does not support a session timer.
	 * 
	 * @param address           the destination address to the host to which the
	 *                          connection should be proxied
	 * @param command           the SCOCS5 command
	 * @param connectionTimeout the SOCKS5 proxy connection timeout in milliseconds,
	 *                          or {@code 0} to wait an infinite amount of time for
	 *                          establishing of the SOCKS5 connection.
	 * @throws IllegalArgumentException if the address or the command is null
	 */
	public Socks5ProxyHandler(InetSocketAddress address, Socks5Command command, long connectionTimeout) {
		this(address, command, null, null, connectionTimeout, null, null);
	}

	/**
	 * Constructs a SOCKS5 proxy connection handler with the specified destination
	 * address, SOCKS5 command, connection timeout and configuration.
	 * <p>
	 * NOTE: The connection timeout will have no effect if the associated session
	 * does not support a session timer.
	 * 
	 * @param address           the destination address to the host to which the
	 *                          connection should be proxied
	 * @param command           the SCOCS5 command
	 * @param connectionTimeout the SOCKS5 proxy connection timeout in milliseconds,
	 *                          or {@code 0} to wait an infinite amount of time for
	 *                          establishing of the SOCKS5 connection.
	 * @param config            the session configuration object, or {@code null} to
	 *                          use the default configuration
	 * @throws IllegalArgumentException if the address or the command is null
	 */
	public Socks5ProxyHandler(InetSocketAddress address, Socks5Command command, long connectionTimeout, ISessionConfig config) {
		this(address, command, null, null, connectionTimeout, config, null);
	}

	/**
	 * Constructs a SOCKS5 proxy connection handler with the specified destination
	 * address, SOCKS5 command, connection timeout, configuration and factory.
	 * <p>
	 * NOTE: The connection timeout will have no effect if the associated session
	 * does not support a session timer.
	 * 
	 * @param address           the destination address to the host to which the
	 *                          connection should be proxied
	 * @param command           the SCOCS5 command
	 * @param connectionTimeout the SOCKS5 proxy connection timeout in milliseconds,
	 *                          or {@code 0} to wait an infinite amount of time for
	 *                          establishing of the SOCKS5 connection.
	 * @param config            the session configuration object, or {@code null} to
	 *                          use the default configuration
	 * @param factory           the factory that will be used to configure the
	 *                          internal structure of the associated session, or
	 *                          {@code null} to use the default factory
	 * @throws IllegalArgumentException if the address or the command is null
	 */
	public Socks5ProxyHandler(InetSocketAddress address, Socks5Command command, long connectionTimeout, ISessionConfig config, ISessionStructureFactory factory) {
		this(address, command, null, null, connectionTimeout, config, factory);
	}

	/**
	 * Constructs a SOCKS5 proxy connection handler with the specified destination
	 * address, SOCKS5 command, user's name/password and connection timeout.
	 * <p>
	 * NOTE: The connection timeout will have no effect if the associated session
	 * does not support a session timer.
	 * 
	 * @param address           the destination address to the host to which the
	 *                          connection should be proxied
	 * @param command           the SCOCS5 command
	 * @param username			the user's name, or {@code null} for an empty name
	 * @param password			the user's password, or {@code null} for an empty password
	 * @param connectionTimeout the SOCKS5 proxy connection timeout in milliseconds,
	 *                          or {@code 0} to wait an infinite amount of time for
	 *                          establishing of the SOCKS5 connection.
	 * @throws IllegalArgumentException if the address or the command is null
	 */
	public Socks5ProxyHandler(InetSocketAddress address, Socks5Command command, String username, String password, long connectionTimeout) {
		this(address, command, username, password, connectionTimeout, null, null);
	}
	
	/**
	 * Constructs a SOCKS5 proxy connection handler with the specified destination
	 * address, SOCKS5 command, user's name/password, connection timeout and 
	 * configuration.
	 * <p>
	 * NOTE: The connection timeout will have no effect if the associated session
	 * does not support a session timer.
	 * 
	 * @param address           the destination address to the host to which the
	 *                          connection should be proxied
	 * @param command           the SCOCS5 command
	 * @param username			the user's name, or {@code null} for an empty name
	 * @param password			the user's password, or {@code null} for an empty password
	 * @param connectionTimeout the SOCKS5 proxy connection timeout in milliseconds,
	 *                          or {@code 0} to wait an infinite amount of time for
	 *                          establishing of the SOCKS5 connection.
	 * @param config            the session configuration object, or {@code null} to
	 *                          use the default configuration
	 * @throws IllegalArgumentException if the address or the command is null
	 */
	public Socks5ProxyHandler(InetSocketAddress address, Socks5Command command, String username, String password, long connectionTimeout, ISessionConfig config) {
		this(address, command, username, password, connectionTimeout, config, null);
	}

	/**
	 * Constructs a SOCKS5 proxy connection handler with the specified destination
	 * address, SOCKS5 command, user's name/password, the default (10 seconds)
	 * connection timeout, configuration and factory.
	 * <p>
	 * NOTE: The connection timeout will have no effect if the associated session
	 * does not support a session timer.
	 * 
	 * @param address  the destination address to the host to which the connection
	 *                 should be proxied
	 * @param command  the SCOCS5 command
	 * @param username the user's name, or {@code null} for an empty name
	 * @param password the user's password, or {@code null} for an empty password
	 * @param config   the session configuration object, or {@code null} to use the
	 *                 default configuration
	 * @param factory  the factory that will be used to configure the internal
	 *                 structure of the associated session, or {@code null} to use
	 *                 the default factory
	 * @throws IllegalArgumentException if the address or the command is null
	 */
	public Socks5ProxyHandler(InetSocketAddress address, Socks5Command command, String username, String password, ISessionConfig config, ISessionStructureFactory factory) {
		super(address, config, factory);
		checkNull(command, "command");
		commandState = new Socks5CommandState(this, command);
		passwordAuthState = new Socks5PasswordAuthState(this, username, password, commandState);
		state = state();
	}
	
	/**
	 * Constructs a SOCKS5 proxy connection handler with the specified destination
	 * address, SOCKS5 command, user's name/password, connection timeout, configuration and
	 * factory.
	 * <p>
	 * NOTE: The connection timeout will have no effect if the associated session
	 * does not support a session timer.
	 * 
	 * @param address           the destination address to the host to which the
	 *                          connection should be proxied
	 * @param command           the SCOCS5 command
	 * @param username			the user's name, or {@code null} for an empty name
	 * @param password			the user's password, or {@code null} for an empty password
	 * @param connectionTimeout the SOCKS5 proxy connection timeout in milliseconds,
	 *                          or {@code 0} to wait an infinite amount of time for
	 *                          establishing of the SOCKS5 connection.
	 * @param config            the session configuration object, or {@code null} to
	 *                          use the default configuration
	 * @param factory           the factory that will be used to configure the
	 *                          internal structure of the associated session, or
	 *                          {@code null} to use the default factory
	 * @throws IllegalArgumentException if the address or the command is null
	 */
	public Socks5ProxyHandler(InetSocketAddress address, Socks5Command command, String username, String password, long connectionTimeout, ISessionConfig config, ISessionStructureFactory factory) {
		super(address, connectionTimeout, config, factory);
		checkNull(command, "command");
		commandState = new Socks5CommandState(this, command);
		passwordAuthState = new Socks5PasswordAuthState(this, username, password, commandState);
		state = state();
	}

	private Socks5InitState state() {
		AbstractSocksState[] nextStates;
		Socks5AuthMethod[] authMethods;
		
		if (passwordAuthState.isConfigured()) {
			authMethods = new Socks5AuthMethod[] { Socks5AuthMethod.NO_AUTH, Socks5AuthMethod.PASSWORD };
			nextStates = new AbstractSocksState[] { commandState, passwordAuthState };
		}
		else {
			authMethods = new Socks5AuthMethod[] { Socks5AuthMethod.NO_AUTH };
			nextStates = new AbstractSocksState[] { commandState };
		}
		return new Socks5InitState(this, authMethods, nextStates);
	}
	
	@Override
	String protocol() {
		return ISocks5.PROTOCOL;
	}

}
