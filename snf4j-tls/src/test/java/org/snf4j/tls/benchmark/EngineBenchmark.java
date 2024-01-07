/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023-2024 SNF4J contributors
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
package org.snf4j.tls.benchmark;

import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.snf4j.benchmark.api.Bench;
import org.snf4j.benchmark.api.BenchmarkRunner;
import org.snf4j.benchmark.api.PreBench;
import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.core.session.ssl.ClientAuth;
import org.snf4j.core.session.ssl.SSLContextBuilder;
import org.snf4j.core.session.ssl.SSLEngineBuilder;
import org.snf4j.tls.CommonTest;
import org.snf4j.tls.TLSEngine;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.engine.DelegatedTaskMode;
import org.snf4j.tls.engine.EngineHandler;
import org.snf4j.tls.engine.EngineParametersBuilder;
import org.snf4j.tls.engine.TicketInfo;
import org.snf4j.tls.record.ContentType;

public class EngineBenchmark {
	
	final static char[] PASSWORD = "password".toCharArray();
	
	final static int COUNT = 100000;

	final static int HANDSHAKE_COUNT = 100;
	
	final static int DATA_SIZE = 1024;
	
	static KeyManagerFactory kmf;
	
	static X509KeyManager km;
	
	static TrustManagerFactory tmf;
	
	static X509TrustManager tm;
	
	static ByteBuffer in = ByteBuffer.allocate(32000);

	static ByteBuffer out = ByteBuffer.allocate(32000);
	
	SSLContext sslCliCtx;

	SSLContext sslSrvCtx;
	
	SSLEngine sslCli;
	
	SSLEngine sslSrv;

	TLSEngine tlsCli;
	
	TLSEngine tlsSrv;
	
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
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void clear() {
		in.clear().flip();
		out.clear();
	}

	void clear(byte[] bytes) {
		in.clear();
		in.put(bytes);
		in.flip();
		out.clear();
	}
	
	byte[] bytes(int len) {
		byte[] b = new byte[len];
		for (int i=0; i<len; ++i) {
			b[i] = (byte) i;
		}
		return b;
	}
	
	void flip() {
		ByteBuffer tmp = in;
		in = out;
		out = tmp;
		in.flip();
		out.compact();
	}

	void wrap(SSLEngine e) throws Exception {
		int p;
		
		do {
			p = out.position();
			e.wrap(in, out);
			runTasks(e);
		} 
		while (out.position() > p);
	}

	void wrap(TLSEngine e) throws Exception {
		int p;
		
		do {
			p = out.position();
			e.wrap(in, out);
			runTasks(e);
		} 
		while (out.position() > p && e.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING);
	}
	
	void unwrap(SSLEngine e) throws Exception {
		int p;
		
		do {
			p = in.position();
			e.unwrap(in, out);
			e.getHandshakeStatus();
			runTasks(e);
		} 
		while (in.position() > p);
	}

	void unwrap(TLSEngine e) throws Exception {
		int p;
		
		do {
			p = in.position();
			e.unwrap(in, out);
			e.getHandshakeStatus();
			runTasks(e);
		} 
		while (in.position() > p);
	}
	
	void runTasks(SSLEngine e) {
		Runnable t;
		
		while ((t = e.getDelegatedTask()) != null) {
			t.run();
		}
	}

	void runTasks(TLSEngine e) {
		Runnable t;
		
		while ((t = e.getDelegatedTask()) != null) {
			t.run();
		}
	}

	void prepareSSLContexts() throws Exception {
        sslCliCtx = SSLContextBuilder.forClient()
        		.protocol("TLSv1.3")
        		.ciphers("TLS_AES_128_GCM_SHA256")
				.keyManager(kmf)
				.trustManager(tmf)
				.build();
        sslSrvCtx = SSLContextBuilder.forServer(kmf)
        		.protocol("TLSv1.3")
        		.ciphers("TLS_AES_128_GCM_SHA256")
				.trustManager(tmf)
				.clientAuth(ClientAuth.NONE)
				.build();
	}
	
	void prepareSSLEngines() throws Exception {
		sslCli = SSLEngineBuilder.forClient(sslCliCtx).build();
        sslSrv = SSLEngineBuilder.forServer(sslSrvCtx).build();
	}

	void handshakeSSLEngines() throws Exception {
        sslCli.beginHandshake();
        sslSrv.beginHandshake();
        
        clear();
        wrap(sslCli);
        flip();
        unwrap(sslSrv);
        wrap(sslSrv);
        flip();
        unwrap(sslCli);
        wrap(sslCli);
        unwrap(sslCli);
        wrap(sslCli);
        flip();
        unwrap(sslSrv);
        wrap(sslSrv);
        flip();
        unwrap(sslCli);
        sslSrv.getHandshakeStatus();
        sslCli.getHandshakeStatus();
	}
	
	void prepareTLSEngines() throws Exception {
		tlsCli = new TLSEngine(true, 
				new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.ALL)
				.cipherSuites(CipherSuite.TLS_AES_128_GCM_SHA256)
				.compatibilityMode(true)
				.build(),
				new EngineHandler(km, null, tm, null, null, new TicketInfo[] {TicketInfo.NO_MAX_EARLY_DATA_SIZE}, 1, 0, null, null, null) {
			
			@Override
			public int calculatePadding(ContentType type, int contentLength) {
				return 16;
			}
		});

		tlsSrv = new TLSEngine(false, 
				new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.ALL)
				.cipherSuites(CipherSuite.TLS_AES_128_GCM_SHA256)
				.compatibilityMode(true)
				.clientAuth(ClientAuth.NONE)
				.build(),
				new EngineHandler(km, null, tm, null, null, new TicketInfo[] {TicketInfo.NO_MAX_EARLY_DATA_SIZE}, 1, 0, null, null, null) {
			
			@Override
			public int calculatePadding(ContentType type, int contentLength) {
				return 16;
			}
		});
	}

	void handshakeTLSEngines() throws Exception {
        tlsCli.beginHandshake();
        tlsSrv.beginHandshake();
        
        clear();
        wrap(tlsCli);
        flip();
        unwrap(tlsSrv);
        wrap(tlsSrv);
        flip();
        unwrap(tlsCli);
        wrap(tlsCli);
        unwrap(tlsCli);
        wrap(tlsCli);
        flip();
        unwrap(tlsSrv);
        wrap(tlsSrv);
        flip();
        unwrap(tlsCli);
        wrap(tlsCli);
        flip();
        unwrap(tlsSrv);
        wrap(tlsSrv);
        flip();
        unwrap(tlsCli);
        tlsSrv.getHandshakeStatus();
        tlsCli.getHandshakeStatus();
	}
	
	@PreBench(name="SSLEngine")
	public void preBench1() throws Exception {
		prepareSSLContexts();
		prepareSSLEngines();
		handshakeSSLEngines();
        clear(bytes(1024));
	}
	
	@Bench(name="SSLEngine")
	public void bench1() throws Exception {
		for (int i=0; i<COUNT; ++i) {
			sslCli.wrap(in, out);
			flip();
			sslSrv.unwrap(in, out);
			flip();
			sslSrv.wrap(in, out);
			flip();
			sslCli.unwrap(in, out);
			flip();
		}
	}

	@PreBench(name="SSLEngine (Handshake)")
	public void preBench1_2() throws Exception {
		prepareSSLContexts();
	}
	
	@Bench(name="SSLEngine (Handshake)")
	public void bench1_2() throws Exception {
		for (int i=0; i<HANDSHAKE_COUNT; ++i) {
			prepareSSLEngines();
			handshakeSSLEngines();
		}
	}
	
	@PreBench(name="TLSEngine")
	public void preBench2() throws Exception {
		prepareTLSEngines();
		handshakeTLSEngines();
        clear(bytes(1024));
	}

	@Bench(name="TLSEngine")
	public void bench2() throws Exception {
		for (int i=0; i<COUNT; ++i) {
			tlsCli.wrap(in, out);
			flip();
			tlsSrv.unwrap(in, out);
			flip();
			tlsSrv.wrap(in, out);
			flip();
			tlsCli.unwrap(in, out);
			flip();
		}
	}

	@Bench(name="TLSEngine (Handshake)")
	public void bench2_2() throws Exception {
		for (int i=0; i<HANDSHAKE_COUNT; ++i) {
			prepareTLSEngines();
			handshakeTLSEngines();
		}
	}
	
	public static void main(String[] args) {
		BenchmarkRunner.run(new EngineBenchmark());
	}

}
