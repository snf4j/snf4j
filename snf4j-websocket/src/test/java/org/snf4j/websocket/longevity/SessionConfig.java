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
package org.snf4j.websocket.longevity;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

import org.snf4j.core.session.SSLEngineCreateException;
import org.snf4j.websocket.DefaultWebSocketSessionConfig;
import org.snf4j.websocket.extensions.IExtension;
import org.snf4j.websocket.extensions.compress.PerMessageDeflateExtension;
import org.snf4j.websocket.extensions.compress.PerMessageDeflateExtension.NoContext;

public class SessionConfig extends DefaultWebSocketSessionConfig {
	
	static SSLContext sslContext;
	
	final IExtension[] extensions;
	static {
		try {
			KeyStore ks = KeyStore.getInstance("JKS");
			KeyStore ts = KeyStore.getInstance("JKS");
			char[] password = "password".toCharArray();

			try {
				File file = new File(SessionConfig.class.getClassLoader().getResource("keystore.jks").getFile());

				if (file != null) {
					ks.load(new FileInputStream(file), password);
					ts.load(new FileInputStream(file), password);
				}
			}
			catch (Exception e) {
				ks.load(SessionConfig.class.getClassLoader().getResourceAsStream("keystore.jks"), password);
				ts.load(SessionConfig.class.getClassLoader().getResourceAsStream("keystore.jks"), password);
			}

			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, password);
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(ts);

			SSLContext ctx = SSLContext.getInstance("TLS");
			ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			sslContext = ctx;
		}
		catch (Exception e) {
			sslContext = null;
		}
	}
	
	SessionConfig(URI requestUri) {
		super(requestUri);
		if (Utils.randomBoolean(Config.COMPRESSED_SESSION_RATIO)) {
			boolean noContext1 = Utils.randomBoolean(Config.COMPRESSED_NO_CONTEXT_TAKEOVER_RATIO);
			boolean noContext2 = Utils.randomBoolean(Config.COMPRESSED_NO_CONTEXT_TAKEOVER_RATIO);
			extensions = new IExtension[] {new PerMessageDeflateExtension(6,
					noContext1 ? NoContext.REQUIRED : NoContext.OPTIONAL,
					noContext2 ? NoContext.REQUIRED : NoContext.OPTIONAL
			)};
		}
		else {
			extensions = null;
		}
	}

	SessionConfig() {
		extensions = new IExtension[] {new PerMessageDeflateExtension()};
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
	
	@Override
	public boolean optimizeDataCopying() {
		return Utils.randomBoolean(Config.OPTIMIZE_DATA_COPING_RATIO);
	}
	
	@Override
	public IExtension[] getSupportedExtensions() {
		return extensions;
	}
	
}
