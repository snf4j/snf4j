/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic.tp;

import java.net.Inet4Address;
import java.net.Inet6Address;

import org.snf4j.tls.Args;

/**
 * A preferred address that can be transfered in the QUIC transport parameters. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class PreferredAddress {

	private final Inet4Address ip4;
	
	private final int ip4Port;
	
	private final Inet6Address ip6;
	
	private final int ip6Port;
	
	private final byte[] connectionId;
	
	private final byte[] resetToken;

	/**
	 * Constructs a preferred address with given parameters.
	 * 
	 * @param ip4          the IPv4 address, or {@code null} if not available
	 * @param ip4Port      the port number for the IPv4 address, ignored when
	 *                     {@code ip4} is null
	 * @param ip6          the IPv6 address, or {@code null} if not available
	 * @param ip6Port      the port number for the IPv6 address, ignored when
	 *                     {@code ip6} is null
	 * @param connectionId the connection id
	 * @param resetToken   the stateless reset token associated with the connection
	 *                     id
	 * @throws IllegalArgumentException if the connection id is {@code null} or if
	 *                                  the stateless reset token is null or its
	 *                                  length is different than 16
	 */
	public PreferredAddress(Inet4Address ip4, int ip4Port, Inet6Address ip6, int ip6Port, byte[] connectionId,
			byte[] resetToken) {
		Args.checkNull(connectionId, "connectionId");
		Args.checkFixed(resetToken, 16, "resetToken");
		this.ip4 = ip4;
		this.ip4Port = ip4Port;
		this.ip6 = ip6;
		this.ip6Port = ip6Port;
		this.connectionId = connectionId;
		this.resetToken = resetToken;
	}

	/**
	 * Returns the IPv4 address.
	 * 
	 * @return the IPv4 address, or {@code null} if not available
	 */
	public Inet4Address getIp4() {
		return ip4;
	}

	/**
	 * Returns the port number for the IPv4 address.
	 * 
	 * @return the port number for the IPv4 address
	 */
	public int getIp4Port() {
		return ip4Port;
	}

	/**
	 * Returns the IPv6 address.
	 * 
	 * @return the IPv6 address, or {@code null} if not available
	 */
	public Inet6Address getIp6() {
		return ip6;
	}

	/**
	 * Returns the port number for the IPv6 address.
	 * 
	 * @return the port number for the IPv6 address
	 */
	public int getIp6Port() {
		return ip6Port;
	}

	/**
	 * Returns the connection id.
	 * 
	 * @return the connection id
	 */
	public byte[] getConnectionId() {
		return connectionId;
	}

	/**
	 * Returns the stateless reset token associated with the connection id.
	 * 
	 * @return the stateless reset token
	 */
	public byte[] getResetToken() {
		return resetToken;
	}

}
