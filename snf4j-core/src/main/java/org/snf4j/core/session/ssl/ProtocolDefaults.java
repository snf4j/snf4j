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
package org.snf4j.core.session.ssl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

class ProtocolDefaults {
	
	final static String TLS = "TLS";

	final static String DTLS = "DTLS";
	
	static volatile ProtocolDefaults tlsDefaults;
	
	static volatile ProtocolDefaults dtlsDefaults;
	
	final static String[] PROTOCOLS = new String[] {
			"TLSv1.3", 
			"TLSv1.2", 
			"TLSv1.1", 
			"TLSv1",
			"DTLSv1.2", 
			"DTLSv1.0"
			};
	
	final static String[] CIPHERS = new String[] {
			"TLS_AES_128_GCM_SHA256",
			"TLS_AES_256_GCM_SHA384",
			"TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
			"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
			"TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
			"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
			"TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384",
			"TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384",
			"TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
			"TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256"
			};

	private final Set<String> supportedCiphers;
	
	private final String[] defaultCiphers;

	private final Set<String> supportedProtocols;
	
	private final String[] defaultProtocols;
	
	ProtocolDefaults(String protocol) {
		SSLEngine engine = ProtocolDefaults.defaultEngine(protocol);
		
		supportedCiphers = ProtocolDefaults.supportedCiphers(engine);
		defaultCiphers = ProtocolDefaults.defaultCiphers(engine, supportedCiphers);
		supportedProtocols = ProtocolDefaults.supportedProtocols(engine);
		defaultProtocols = ProtocolDefaults.defaultProtocols(engine, supportedProtocols);
	}
	
	Set<String> supportedCiphers() {
		return supportedCiphers;
	}

	String[] defaultCiphers() {
		return defaultCiphers;
	}

	Set<String> supportedProtocols() {
		return supportedProtocols;
	}

	String[] defaultProtocols() {
		return defaultProtocols;
	}
	
	static ProtocolDefaults instance(SSLEngine engine) {
		return instance(isDtls(engine));
	}

	static ProtocolDefaults instance(boolean dtls) {
		if (dtls) {
			if (dtlsDefaults == null) {
				synchronized (ProtocolDefaults.class) {
					if (dtlsDefaults == null) {
						dtlsDefaults = new ProtocolDefaults(DTLS);
					}
				}
			}
			return dtlsDefaults;
		}
		if (tlsDefaults == null) {
			synchronized (ProtocolDefaults.class) {
				if (tlsDefaults == null) {
					tlsDefaults = new ProtocolDefaults(TLS);
				}
			}
		}
		return tlsDefaults;
	}
	
	static boolean isDtls(SSLEngine engine) {
		String[] protocols = engine.getSupportedProtocols();
		
		for (String protocol: protocols) {
			if (protocol.startsWith(DTLS)) {
				return true;
			}
		}
		return false;
	}
	
	static SSLEngine defaultEngine(String protocol) throws Error {
		SSLContext context;
		
		try {
			context = SSLContext.getInstance(protocol);
			context.init(null, null, null);
		}
		catch (Exception e) {
			throw new Error("Initialization of SSL context for protocol " + protocol + " failed", e);
		}
		return context.createSSLEngine();
	}
	
	static Set<String> supportedCiphers(SSLEngine engine) {
		String[] supported = engine.getSupportedCipherSuites();
		Set<String> ciphers = new HashSet<String>(supported.length);
		
		for (String cipher: supported) {
			ciphers.add(cipher);
		}
		return ciphers;
	}
	
	static String[] defaultCiphers(SSLEngine engine, Set<String> supportedCiphers) {
		List<String> defaultList = new ArrayList<String>(CIPHERS.length);
		
		for (String cipher: CIPHERS) {
			if (supportedCiphers.contains(cipher)) {
				defaultList.add(cipher);
			}
		}
		if (defaultList.isEmpty()) {
			String[] ciphers = engine.getEnabledCipherSuites();
			for (String cipher: ciphers) {
				defaultList.add(cipher);
			}
		}
		return defaultList.toArray(new String[defaultList.size()]);
	}

	static Set<String> supportedProtocols(SSLEngine engine) {
		String[] supported = engine.getSupportedProtocols();
		Set<String> protocols = new HashSet<String>(supported.length);
		
		for (String protocol: supported) {
			protocols.add(protocol);
		}
		return protocols;
	}
	
	static String[] defaultProtocols(SSLEngine engine, Set<String> supportedPtotocols) {
		List<String> defaultList = new ArrayList<String>(PROTOCOLS.length);
		
		for (String protocol: PROTOCOLS) {
			if (supportedPtotocols.contains(protocol)) {
				defaultList.add(protocol);
			}
		}
		if (defaultList.isEmpty()) {
			String[] protocols = engine.getEnabledProtocols();
			for (String protocol: protocols) {
				defaultList.add(protocol);
			}
		}
		return defaultList.toArray(new String[defaultList.size()]);
	}
}
