/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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
package org.snf4j.core;

import java.security.Principal;
import java.security.cert.Certificate;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;

public class TestSSLSession implements SSLSession {

	int appBufferSize = 1024;
	int netBufferSize = 1024;
	
	@Override
	public int getApplicationBufferSize() {
		return appBufferSize;
	}

	@Override
	public String getCipherSuite() {
		return null;
	}

	@Override
	public long getCreationTime() {
		return 0;
	}

	@Override
	public byte[] getId() {
		return null;
	}

	@Override
	public long getLastAccessedTime() {
		return 0;
	}

	@Override
	public Certificate[] getLocalCertificates() {
		return null;
	}

	@Override
	public Principal getLocalPrincipal() {
		return null;
	}

	@Override
	public int getPacketBufferSize() {
		return netBufferSize;
	}

	@Override
	public X509Certificate[] getPeerCertificateChain()
			throws SSLPeerUnverifiedException {
		return null;
	}

	@Override
	public Certificate[] getPeerCertificates()
			throws SSLPeerUnverifiedException {
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
	public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
		return null;
	}

	@Override
	public String getProtocol() {
		return null;
	}

	@Override
	public SSLSessionContext getSessionContext() {
		return null;
	}

	@Override
	public Object getValue(String arg0) {
		return null;
	}

	@Override
	public String[] getValueNames() {
		return null;
	}

	@Override
	public void invalidate() {
	}

	@Override
	public boolean isValid() {
		return false;
	}

	@Override
	public void putValue(String arg0, Object arg1) {
	}

	@Override
	public void removeValue(String arg0) {
	}

}
