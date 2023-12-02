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
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.X509KeyManager;

import org.snf4j.tls.Args;
import org.snf4j.tls.IntConstant;
import org.snf4j.tls.extension.SignatureScheme;

public class X509KeyManagerCertificateSelector implements ICertificateSelector {

	private final X509KeyManager keyManager;
	
	private final String alias;
		
	public X509KeyManagerCertificateSelector(X509KeyManager keyManager, String alias) {
		Args.checkNull(keyManager, "keyManager");
		this.keyManager = keyManager;
		this.alias = alias;
	}

	public X509KeyManagerCertificateSelector(X509KeyManager keyManager) {
		this(keyManager, null);
	}
	
	SelectedCertificates selectCertificates(CertificateCriteria criteria, String alias) throws CertificateSelectorException, Exception {
		X509Certificate[] certs = keyManager.getCertificateChain(alias);
		
		if (certs == null) {
			throw new CertificateSelectorException("No certificate chain found for " + alias + " alias");
		}

		SignatureScheme algorithm = criteria.matchByKey(certs[0]);

		if (algorithm == null) {
			throw new CertificateSelectorException("Key algorithm not in both local and offered signature schemes");
		}
		
		if (!criteria.allMatch(certs)) {
			throw new CertificateSelectorException("Chain has certificate(s) with sign algorithm not in offered signature schemes");
		}
		
		PrivateKey key = keyManager.getPrivateKey(alias);
		if (key == null) {
			throw new CertificateSelectorException("No key found for " + alias + " alias");
		}
						
		return new SelectedCertificates(algorithm, key, certs);
	}
	
	@Override
	public SelectedCertificates selectCertificates(CertificateCriteria criteria) throws Exception {
		if (alias != null) {
			return selectCertificates(criteria, alias);
		}
		
		SignatureScheme[] schemes = criteria.getSchemes();
		Set<String> processed = new HashSet<String>();
		
		for (SignatureScheme scheme: criteria.getLocalSchemes()) {
			if (IntConstant.find(schemes, scheme) != null) {
				String keyType = scheme.spec().getSignature().keyAlgorithm();
				
				if (!processed.contains(keyType)) {
					processed.add(keyType);
					
					String[] aliases = criteria.isServer() 
						? keyManager.getServerAliases(keyType, null)
						: keyManager.getClientAliases(keyType, null);
					
					if (aliases != null) {
						for (String alias: aliases) {
							try {
								return selectCertificates(criteria, alias);
							}
							catch (CertificateSelectorException e) {
								//Ignore
							}
						}
					}
				}
			}
		}
		throw new CertificateSelectorException("No certificate chain found");
	}

}
