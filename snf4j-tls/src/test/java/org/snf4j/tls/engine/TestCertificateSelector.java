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
import java.util.ArrayList;

import org.snf4j.tls.CommonTest;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.SignatureScheme;
import org.snf4j.tls.handshake.CertificateEntry;

public class TestCertificateSelector implements ICertificateSelector {

	volatile String keyAlgorithm = "RSA"; 
	
	volatile String keyName = "rsa";
	
	volatile String[] certNames = new String[] {"rsasha256"};
	
	volatile SignatureScheme signatureScheme = SignatureScheme.RSA_PKCS1_SHA256;
	
	@Override
	public SelectedCertificates selectCertificates(CertificateCriteria criteria)  throws Exception {
		PrivateKey key = CommonTest.key(keyAlgorithm, keyName);
		CertificateEntry[] certs = new CertificateEntry[certNames.length];
		
		for (int i=0; i<certNames.length; ++i) {
			certs[i] = new CertificateEntry(CommonTest.cert(certNames[i]).getEncoded(), new ArrayList<IExtension>(0));
		}
		return new SelectedCertificates(signatureScheme, certs, key);
	}

}
