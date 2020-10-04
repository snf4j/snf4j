/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020 SNF4J contributors
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
package org.snf4j.example.echo;

import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

import org.snf4j.core.codec.DefaultCodecExecutor;
import org.snf4j.core.codec.ICodecExecutor;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.SSLEngineCreateException;

public class SessionConfig extends DefaultSessionConfig {
	
	static volatile SSLContext sslContext = null; 
	
	private final int pipelineSize;
	
	SessionConfig(int pipelineSize) {
		this.pipelineSize = pipelineSize;
	}
	
	void load(KeyStore ks, String fileName, char[] password) throws Exception {
		InputStream in = getClass().getResourceAsStream(fileName);
		
		try {
			ks.load(in, password);
		}
		finally {
			in.close();
		}
	}
	
	SSLContext getSSLContext() throws SSLEngineCreateException {
		if (sslContext == null) {
			try {
				synchronized (SessionConfig.class) {
					if (sslContext == null) {
						KeyStore ks = KeyStore.getInstance("JKS");
						KeyStore ts = KeyStore.getInstance("JKS");
						char[] password = "password".toCharArray();

						load(ks, "/keystore.jks", password);
						load(ts, "/keystore.jks", password);

						KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
						kmf.init(ks, password);
						TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
						tmf.init(ts);

						SSLContext ctx = SSLContext.getInstance("TLS");
						ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
						sslContext = ctx;
					}
				}
			}
			catch (Exception e) {
				throw new SSLEngineCreateException(e);
			}
		}
		return sslContext;
	}
	
	@Override
	public SSLEngine createSSLEngine(boolean clientMode) throws SSLEngineCreateException {
		SSLEngine engine = getSSLContext().createSSLEngine();
		
		engine.setUseClientMode(clientMode);
		if (!clientMode) {
			engine.setNeedClientAuth(true);
		}
		return engine;
	}
	
	@Override
	public ICodecExecutor createCodecExecutor() {
		if (pipelineSize <= 0) {
			return null;
		}
		
		DefaultCodecExecutor executor = new DefaultCodecExecutor();

		for (int i=0; i<pipelineSize; ++i) {
			executor.getPipeline().add("DECODER"+i, new Decoder());
			executor.getPipeline().add("ENCODER"+i, new Encoder());
		}
		return executor;
	}

	
}
