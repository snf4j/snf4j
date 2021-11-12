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

class Socks5InitState extends AbstractSocksState implements ISocks5 {
	
	final static byte METHOD_INDEX = 1;

	private final static int READ_SIZE = 2;

	private final Socks5AuthMethod[] authMethods;
	
	private final AbstractSocksState[] nextStates;
	
	Socks5InitState(Socks5ProxyHandler handler, Socks5AuthMethod[] authMethods, AbstractSocksState[] nextStates) {
		super(handler);
		this.authMethods = authMethods;
		this.nextStates = nextStates;
	}

	@Override
	int readSize() {
		return READ_SIZE;
	}
	
	@Override
	AbstractSocksState read(byte[] data) {
		if (data[VER_INDEX] != VERSION) {
			throw new ProxyConnectionException("Unsupported SOCKS5 reply version: " + data[0]);
		}
		
		byte methodCode = data[METHOD_INDEX];
		
		if (methodCode == Socks5AuthMethod.UNACCEPTED.code()) {
			throw new ProxyConnectionException("No acceptable authentication method");
		}
		
		Socks5AuthMethod method = null;
		AbstractSocksState next = null;
		
		for (int i=0; i<authMethods.length; ++i) {
			if (authMethods[i].code() == methodCode) {
				method = authMethods[i];
				next = nextStates[i];
				break;
			}
		}
		if (method == null) {
			throw new ProxyConnectionException("Unexpected authentication method: " + methodCode);
		}
		return next;
	}

	@Override
	void handleReady() {
		ByteBuffer buf = handler.getSession().allocate(2 + authMethods.length);
		
		buf.put(VERSION);
		buf.put((byte)authMethods.length);
		for (int i=0; i<authMethods.length; ++i) {
			buf.put(authMethods[i].code());
		}
		handler.flipAndWrite(buf);
	}

}
