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

class Socks5CommandState extends AbstractSocksState implements ISocks5 {
	
	final static int STATUS_INDEX = 1;

	final static int ATYP_INDEX = 3;

	final static int ADDR_INDEX = 4;
	
	private final Socks5Command command;
	
	Socks5CommandState(Socks5ProxyHandler handler, Socks5Command command) {
		super(handler);
		this.command = command;
	}

	private int expectedLength(byte atyp, byte a0) {
		int alen;
		
		switch (atyp) {
		case 1:
			alen = 4;
			break;
			
		case 3:
			alen = 1 + ((int)a0 & 0xff);
			break;
			
		case 4:
			alen = 16;
			break;
			
		default:
			//unexpected address type, set to 0 and fail during parsing
			alen = 0;
		}
		return alen + 6;
	}

	@Override
	int available(ByteBuffer data, boolean flipped) {
		int len = length(data, flipped);

		if (len >= 6) {
			ByteBuffer buf = flipped ? data : (ByteBuffer)data.duplicate().flip();
			int expLen = expectedLength(buf.get(3), buf.get(4));
			
			if (len >= expLen) {
				return expLen;
			}
		}
		return 0;
	}

	@Override
	int available(byte[] data, int off, int len) {
		if (len >= 6) {
			int expLen = expectedLength(data[off+3], data[off+4]);
			
			if (len >= expLen) {
				return expLen;
			}
		}
		return 0;
	}

	@Override
	int readSize() {
		//Ignored as we override available methods
		return 0;
	}

	@Override
	AbstractSocksState read(byte[] data) {
		if (data[VER_INDEX] != VERSION) {
			throw new ProxyConnectionException("Unsupported SOCKS5 reply version: " + data[VER_INDEX]);
		}
		
		int statusCode = (int)data[STATUS_INDEX] & 0xff;
		Socks5Status status = Socks5Status.valueOf(statusCode);
		SocksAddressType addrType = SocksAddressType.valueOf(data[ATYP_INDEX]);
		String addr;
		int addrLen;
		
		if (addrType == null) {
			throw new ProxyConnectionException("Unexpected address type: " + data[ATYP_INDEX]);
		}
		
		switch (addrType) {
		case IPV4:
			addrLen = 4;
			addr = NetworkUtil.ipv4ToString(data, ADDR_INDEX);
			break;
			
		case IPV6:
			addrLen = 16;
			addr = NetworkUtil.ipv6ToString(data, ADDR_INDEX, false);
			break;
			
		default:
			addrLen = (int)data[ADDR_INDEX] & 0xff;
			addr = new String(data, ADDR_INDEX+1, addrLen++, StandardCharsets.US_ASCII);
			break;
		}
		
		int replyCount = handler.reply(new Socks5Reply(statusCode, addrType, addr, NetworkUtil.toPort(data,ADDR_INDEX+addrLen)));
		
		if (status != Socks5Status.SUCCESS) {
			throw new ProxyConnectionException("SOCKS5 proxy response status code: " + statusCode);
		}
		if (command != Socks5Command.BIND || replyCount == 2) {
			return null;
		}
		return this;
	}

	@Override
	void handleReady() {
		InetSocketAddress address = handler.getAddress();
		SocksAddressType addrType = null;
		int addrLen = 0;
		String host;
		byte[] addrBytes;
		
		if (address.isUnresolved()) {
			host = address.getHostString();
		}
		else {
			host = address.getAddress().getHostAddress();
		}
		addrBytes = NetworkUtil.ipv4ToBytes(host);
		if (addrBytes == null) {
			addrBytes = NetworkUtil.ipv6ToBytes(host);
			if (addrBytes != null) {
				addrType = SocksAddressType.IPV6;
				addrLen = 16;
			}
		}
		else {
			addrType = SocksAddressType.IPV4;
			addrLen = 4;
		}
		if (addrBytes == null) {
			addrBytes = host.getBytes(StandardCharsets.US_ASCII);
			if (addrBytes.length > 255) {
				throw new ProxyConnectionException("Destination domain name length too long");
			}
			addrType = SocksAddressType.DOMAIN;
			addrLen = addrBytes.length + 1;
		}
		
		ByteBuffer buf = handler.getSession().allocate(6 + addrLen);
		
		buf.put(VERSION);
		buf.put(command.code());
		buf.put((byte)0);
		buf.put(addrType.code());
		if (addrType == SocksAddressType.DOMAIN) {
			buf.put((byte)addrBytes.length);
		}
		buf.put(addrBytes);
		buf.putShort((short)address.getPort());
		handler.flipAndWrite(buf);
	}

}
