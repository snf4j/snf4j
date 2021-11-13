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

class Socks5Reply implements ISocksReply {
	
	private final int status;
	
	private final String address;
	
	private final SocksAddressType addressType;
	
	private final int port;
	
	Socks5Reply(int status, SocksAddressType addressType, String address, int port) {
		this.status = status;
		this.addressType = addressType;
		this.address = address;
		this.port = port;
	}
	
	@Override
	public boolean isSuccessful() {
		return status == Socks5Status.SUCCESS.code();
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public String getStatusDescription() {
		return Socks5Status.valueOf(status).description();
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public String getAddress() {
		return address;
	}

	@Override
	public SocksAddressType getAddressType() {
		return addressType;
	}

}
