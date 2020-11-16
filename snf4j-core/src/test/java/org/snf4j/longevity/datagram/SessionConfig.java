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
package org.snf4j.longevity.datagram;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import org.snf4j.core.codec.DefaultCodecExecutor;
import org.snf4j.core.codec.ICodecExecutor;
import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.SSLEngineCreateException;
import org.snf4j.longevity.ByteToByteDecoder;
import org.snf4j.longevity.ByteToByteEncoder;
import org.snf4j.longevity.ByteToPacketDecoder;
import org.snf4j.longevity.Config;
import org.snf4j.longevity.ObjectToVoidDecoder;
import org.snf4j.longevity.ObjectToVoidEncoder;
import org.snf4j.longevity.PacketToByteEncoder;
import org.snf4j.longevity.Utils;

public class SessionConfig extends DefaultSessionConfig {
	
	static SSLContext sslContext;
	
	final boolean parent;
	
	public SessionConfig() {
		parent = false;
	}
	
	public SessionConfig(boolean parent) {
		this.parent = parent;
	}
	
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

			SSLContext ctx = SSLContext.getInstance("DTLS");
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
		
		SSLParameters params = engine.getSSLParameters();
		
		try {
			Method method = SSLParameters.class.getMethod("setMaximumPacketSize", int.class);
			method.invoke(params, 1200);
			engine.setSSLParameters(params);
		}
		catch (Exception e) {
			throw new SSLEngineCreateException(e);
		}
		
		return engine;
	}

	@Override
	public boolean optimizeDataCopying() {
		return Config.DATAGRAM_OPTIMIZE_DATA_COPING;
	}
	
	public long getDatagramServerSessionNoReopenPeriod() {
		return 30000;
	}
	
	@Override
	public ICodecExecutor createCodecExecutor() {
		if (parent) return super.createCodecExecutor();
		
		if (Utils.randomBoolean(Config.CODEC_EXECUTOR_RATIO)) {
			ICodecExecutor e = new DefaultCodecExecutor();
			ICodecPipeline p = e.getPipeline();

			p.add("v1", new ObjectToVoidDecoder());
			p.add("BASE", new ByteToByteDecoder(2));
			p.add("v2", new ObjectToVoidDecoder());
			p.add("d2", new ByteToByteDecoder(-1));
			p.add("d3", new ByteToByteDecoder(-1));
			p.add("d4", new ByteToPacketDecoder());
			p.add("v3", new ObjectToVoidDecoder());

			p.add("v4", new ObjectToVoidEncoder());
			p.add("e1", new ByteToByteEncoder(3));
			p.add("v5", new ObjectToVoidEncoder());
			p.add("e2", new ByteToByteEncoder(-2));
			p.add("e3", new ByteToByteEncoder(-1));
			p.add("e4", new PacketToByteEncoder());
			p.add("v6", new ObjectToVoidEncoder());

			return e;
		}
		return null;
	}
	
}
