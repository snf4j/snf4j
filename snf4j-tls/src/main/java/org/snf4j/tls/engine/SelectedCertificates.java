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
package org.snf4j.tls.engine;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;

import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.SignatureScheme;
import org.snf4j.tls.handshake.CertificateEntry;

public class SelectedCertificates {
	
	private final SignatureScheme algorithm;
	
	private final CertificateEntry[] entries;
	
	private final PrivateKey privateKey;

	private final Certificate[] certs;
	
	public SelectedCertificates(SignatureScheme algorithm, CertificateEntry[] entries, PrivateKey privateKey, Certificate[] certs) {
		this.algorithm = algorithm;
		this.entries = entries;
		this.privateKey = privateKey;
		this.certs = certs;
	}

	public SelectedCertificates(SignatureScheme algorithm, PrivateKey privateKey, Certificate[] certs) throws CertificateEncodingException {
		this(algorithm, entries(certs), privateKey, certs);
	}
	
	private static CertificateEntry[] entries(Certificate[] certs) throws CertificateEncodingException {
		CertificateEntry[] entries = new CertificateEntry[certs.length];

		for (int i=0; i<certs.length; ++i) {
			entries[i] = new CertificateEntry(certs[i].getEncoded(), new ArrayList<IExtension>(0));
		}
		return entries;
	}
	
	public SignatureScheme getAlgorithm() {
		return algorithm;
	}

	public CertificateEntry[] getEntries() {
		return entries;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public Certificate[] getCertificates() {
		return certs;
	}
	
	
}
