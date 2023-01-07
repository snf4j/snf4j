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
package org.snf4j.tls.handshake;

import java.security.PublicKey;
import java.security.cert.X509Certificate;

public class CertificateEntry {
	
	private final CertificateType type;
	
	private final X509Certificate certificate;
	
	private final PublicKey key;
	
	CertificateEntry(X509Certificate certificate) {
		type = CertificateType.X509;
		this.certificate = certificate;
		key = null;
	}
	
	CertificateEntry(PublicKey key) {
		type = CertificateType.RAW_PUBLIC_KEY;
		this.key = key;
		certificate = null;
	}

	public CertificateType getType() {
		return type;
	}

	public X509Certificate getCertificate() {
		return certificate;
	}

	public PublicKey getKey() {
		return key;
	}

}
