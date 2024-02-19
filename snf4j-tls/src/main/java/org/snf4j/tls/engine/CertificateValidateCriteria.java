/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023-2024 SNF4J contributors
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
package org.snf4j.tls.engine;

import java.security.cert.X509Certificate;

import org.snf4j.tls.Args;
import org.snf4j.tls.extension.SignatureScheme;

public class CertificateValidateCriteria {

	private final boolean server;

	private final String hostName;

	private final SignatureScheme[] localSchemes;

	public CertificateValidateCriteria(boolean server, String hostName, SignatureScheme[] localSchemes) {
		Args.checkNull(localSchemes, "localSchemes");
		this.server = server;
		this.hostName = hostName;
		this.localSchemes = localSchemes;
	}

	public boolean isServer() {
		return server;
	}

	public String getHostName() {
		return hostName;
	}
	
	public SignatureScheme[] getLocalSchemes() {
		return localSchemes;
	}
	
	public boolean allMatch(X509Certificate[] certs, int offset, int length) {
		return CertificateCriteria.allMatch(certs, offset, length, localSchemes);
	}
	
	public boolean allMatch(X509Certificate[] certs) {
		return allMatch(certs, 0, certs.length);
	}

}
