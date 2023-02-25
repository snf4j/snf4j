/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.session;

import java.util.concurrent.atomic.AtomicLong;

import org.snf4j.tls.cipher.CipherSuite;

public class Session {

	private final static AtomicLong ID = new AtomicLong();
	
	private final long id;
	
	private final long creationTime;
	
	private final ISessionManager manager;

	private final String host;
	
	private final int port;
	
	private final CipherSuite cipherSuite;
	
	public Session(ISessionManager manager, CipherSuite cipherSuite, String host, int port) {
		this.id = ID.incrementAndGet();
		creationTime = System.currentTimeMillis();
		this.manager = manager;
		this.cipherSuite = cipherSuite;
		this.host = host;
		this.port = port;
		this.manager.storeSession(this);
	}

	public long getId() {
		return id;
	}

	public long getCreationTime() {
		return creationTime;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}
	
	public ISessionManager getManager() {
		return manager;
	}

	public CipherSuite getCipherSuite() {
		return cipherSuite;
	}
	
	public void storeTicket(SessionTicket ticket) {
		manager.storeTicket(this, ticket);
	}
}
