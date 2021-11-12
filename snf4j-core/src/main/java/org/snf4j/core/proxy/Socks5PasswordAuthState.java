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
import java.nio.charset.StandardCharsets;

class Socks5PasswordAuthState extends AbstractSocksState {
	
	private final static byte VERSION = 1;	
	
	private final static byte SUCCESS = 0;
	
	private final static int READ_SIZE = 2;

	final static int STATUS_INDEX = 1;
	
	private final String username;
	
	private final String password;
	
	private final AbstractSocksState nextState;

	Socks5PasswordAuthState(Socks5ProxyHandler handler, String username, String password, AbstractSocksState nextState) {
		super(handler);
		this.username = check(username, "username");
		this.password = check(password, "password");
		this.nextState = nextState;
	}
	
	private String check(String value, String name) {
		value = value == null || value.isEmpty() ? null : value;
		if (value != null) {
			if (value.getBytes(StandardCharsets.US_ASCII).length > 255) {
				throw new IllegalArgumentException(name + " length is too long (expected less than 256)");
			}
		}
		return value;
	}

	boolean isConfigured() {
		return username != null || password != null;
	}
	
	@Override
	int readSize() {
		return READ_SIZE;
	}
	
	@Override
	AbstractSocksState read(byte[] data) {
		if (data[VER_INDEX] != VERSION) {
			throw new ProxyConnectionException("Unsupported SOCKS5 subnegotiation reply version: " + data[0]);
		}
		
		byte statusCode = data[STATUS_INDEX];
		
		if (statusCode != SUCCESS) {
			throw new ProxyConnectionException("Username/Password authentication response status code: " + statusCode);
		}
		return nextState;
	}

	@Override
	void handleReady() {
		byte[] user = username == null ? new byte[0] : username.getBytes(StandardCharsets.US_ASCII);
		byte[] pass = password == null ? new byte[0] : password.getBytes(StandardCharsets.US_ASCII);
		ByteBuffer buf = handler.getSession().allocate(3 + user.length + pass.length);

		buf.put(VERSION);
		buf.put((byte)user.length);
		buf.put(user);
		buf.put((byte)pass.length);
		buf.put(pass);
		handler.flipAndWrite(buf);
	}

}
