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
package org.snf4j.tls.longevity;

import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.SSLEngineCreateException;
import org.snf4j.tls.CommonTest;

public class SessionConfig extends DefaultSessionConfig {
	
	final static char[] PASSWORD = "password".toCharArray();

	static SSLContext sslContext;
	
	static KeyManagerFactory kmf;
	
	static X509KeyManager km;
	
	static TrustManagerFactory tmf;
	
	static X509TrustManager tm;
	
	static {
		try {
			
			KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			ks.load(null, null);
			ks.setKeyEntry("key", CommonTest.key("EC", "secp256r1"), PASSWORD, new X509Certificate[] {CommonTest.cert("secp256r1")});
	        kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
	        kmf.init(ks,PASSWORD);
	        km = (X509KeyManager) kmf.getKeyManagers()[0];
			
	        ks = KeyStore.getInstance(KeyStore.getDefaultType());
			ks.load(null, null);
			ks.setCertificateEntry("ca", CommonTest.cert("secp256r1"));
	        tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
	        tmf.init(ks);
	        tm = (X509TrustManager) tmf.getTrustManagers()[0];

			SSLContext ctx = SSLContext.getInstance("TLSv1.3");
			ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			sslContext = ctx;
		}
		catch (Exception e) {
			sslContext = null;
		}
	}
	
	@Override
	public SSLEngine createSSLEngine(boolean clientMode) throws SSLEngineCreateException {
		SSLEngine engine = sslContext.createSSLEngine();
		engine.setUseClientMode(clientMode);
		if (!clientMode) {
			engine.setNeedClientAuth(true);
		}
		return engine;
	}
	
}
