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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.snf4j.core.util.NetworkUtil;

class Socks4CommandState extends AbstractSocksState implements ISocks4 {

	private final static byte REPLY_VERSION = 0;

	private final static int RESPONSE_SIZE = 8;
	
	private final static byte[] DOMAIN_MARKER = new byte[] {0,0,0,1};
	
	final static int STATUS_INDEX = 1;

	final static int IP_INDEX = 4;

	final static int PORT_INDEX = 2;
	
	private final Socks4Command command;
	
	private final String username;
	
	Socks4CommandState(Socks4ProxyHandler handler, Socks4Command command, String username) {
		super(handler);
		this.command = command;
		this.username = username == null ? "" : username;
	}

	@Override
	int responseSize() {
		return RESPONSE_SIZE;
	}

	@Override
	AbstractSocksState read(byte[] data) {
		if (data[VER_INDEX] != REPLY_VERSION) {
			throw new ProxyConnectionException("Unsupported SOCKS4 reply version: " + data[0] + " (expected: 0)");
		}
		
		int statusCode = (int)data[STATUS_INDEX] & 0xff;
		Socks4Status status = Socks4Status.valueOf(statusCode);
		int replyCount = handler.reply(new Socks4Reply(
				statusCode, NetworkUtil.ipv4ToString(data, IP_INDEX), NetworkUtil.toPort(data,PORT_INDEX)));
		
		if (status != Socks4Status.SUCCESS) {
			throw new ProxyConnectionException("SOCKS4 proxy response status code: " + statusCode);
		}
		if (command == Socks4Command.CONNECT || replyCount == 2) {
			return null;
		}
		return this;
	}

	@Override
	void handleReady() {
		InetSocketAddress address = handler.getAddress();
		byte[] usernameBytes = username.getBytes(StandardCharsets.US_ASCII);
		String host;
		byte[] ipv4, hostBytes;
		int len;
		
		if (address.isUnresolved()) {
			host = address.getHostString();
		}
		else {
			host = address.getAddress().getHostAddress();
		}
		
		ipv4 = NetworkUtil.ipv4ToBytes(host);
		len = 1+1+2+4+usernameBytes.length+1;
		if (ipv4 != null) {
			hostBytes = null;
		}
		else {
			hostBytes = host.getBytes(StandardCharsets.US_ASCII);
			ipv4 = DOMAIN_MARKER;
			len += hostBytes.length + 1;
		}
		
		ByteBuffer buf = handler.getSession().allocate(len);
		
		buf.put(VERSION);
		buf.put(command.code());
		buf.putShort((short)address.getPort());
		buf.put(ipv4);
		buf.put(usernameBytes);
		buf.put((byte)0);
		if (hostBytes != null) {
			buf.put(hostBytes);
			buf.put((byte)0);
		}
		handler.flipAndWrite(buf);
	}

}
