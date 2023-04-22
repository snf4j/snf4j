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

import java.security.cert.Certificate;

import org.snf4j.tls.cipher.CipherSuite;

public class TestSession implements ISession {

	private long id;
	
	public byte[] data;
	
	TestSession(long id, int size) {
		this.id = id;
		data = new byte[size];
	}
	
	@Override
	public long getId() {
		return id;
	}

	@Override
	public long getCreationTime() {
		return 0;
	}

	@Override
	public CipherSuite getCipherSuite() {
		return null;
	}

	@Override
	public String getPeerHost() {
		return null;
	}

	@Override
	public int getPeerPort() {
		return 0;
	}

	@Override
	public ISessionManager getManager() {
		return null;
	}

	@Override
	public void invalidate() {
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public Certificate[] getPeerCertificates() {
		return null;
	}

	@Override
	public Certificate[] getLocalCertificates() {
		return null;
	}

}
