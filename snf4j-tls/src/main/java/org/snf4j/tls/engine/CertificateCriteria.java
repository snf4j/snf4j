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

import java.security.cert.X509Certificate;

import org.snf4j.tls.Args;
import org.snf4j.tls.IntConstant;
import org.snf4j.tls.extension.SignatureScheme;
import org.snf4j.tls.handshake.CertificateType;

public class CertificateCriteria {
	
	private final boolean server;
	
	private final CertificateType type;
	
	private final String hostName;
	
	private final SignatureScheme[] schemes;
	
	private final SignatureScheme[] certSchemes;

	private final SignatureScheme[] localSchemes;
	
	public CertificateCriteria(boolean server, CertificateType type, String hostName, SignatureScheme[] schemes,
			SignatureScheme[] certSchemes, SignatureScheme[] localSchemes) {
		Args.checkNull(type, "type");
		Args.checkNull(schemes, "schemes");
		Args.checkNull(localSchemes, "localSchemes");
		this.server = server;
		this.type = type;
		this.hostName = hostName;
		this.schemes = schemes;
		this.certSchemes = certSchemes;
		this.localSchemes = localSchemes;
	}

	public boolean isServer() {
		return server;
	}
	
	public CertificateType getType() {
		return type;
	}

	public String getHostName() {
		return hostName;
	}

	public SignatureScheme[] getSchemes() {
		return schemes;
	}

	public SignatureScheme[] getCertSchemes() {
		return certSchemes;
	}
	
	public SignatureScheme[] getLocalSchemes() {
		return localSchemes;
	}
		
	public static SignatureScheme match(X509Certificate cert, SignatureScheme[] schemes, SignatureScheme[] secondarySchemes) {
		Args.checkNull(cert, "cert");
		Args.checkNull(schemes, "schemes");
		boolean secondary = secondarySchemes != null;
		
		for (SignatureScheme scheme: schemes) {
			if (scheme.spec().getSignature().matches(cert)) {
				if (secondary) {
					if (IntConstant.find(secondarySchemes, scheme) == null) {
						continue;
					}
				}
				return scheme;
			}
		}		
		return null;
	}

	public static SignatureScheme matchByKey(X509Certificate cert, SignatureScheme[] schemes, SignatureScheme[] secondarySchemes) {
		Args.checkNull(cert, "cert");
		Args.checkNull(schemes, "schemes");
		boolean secondary = secondarySchemes != null;
		
		for (SignatureScheme scheme: schemes) {
			if (scheme.spec().getSignature().matchesByKey(cert)) {
				if (secondary) {
					if (IntConstant.find(secondarySchemes, scheme) == null) {
						continue;
					}
				}
				return scheme;
			}
		}		
		return null;
	}

	public static boolean allMatch(X509Certificate[] certs, int offset, int length, SignatureScheme[] schemes) {
		Args.checkNull(certs, "certs");
		Args.checkNull(schemes, "schemes");
		Args.checkBounds(offset, length, certs.length);
		
		length += offset;
		for (int i=offset; i<length; ++i) {
			if (match(certs[i], schemes, null) == null) {
				return false;
			}
		}		
		return true;
	}
	
	public SignatureScheme match(X509Certificate cert) {
		return match(cert, localSchemes, schemes);
	}

	public SignatureScheme matchByKey(X509Certificate cert) {
		return matchByKey(cert, localSchemes, schemes);
	}

	public boolean allMatch(X509Certificate[] certs, int offset, int length) {
		return allMatch(certs, offset, length, certSchemes == null ? this.schemes : certSchemes);
	}
	
	public boolean allMatch(X509Certificate[] certs) {
		return allMatch(certs, 0, certs.length);
	}
	
}
