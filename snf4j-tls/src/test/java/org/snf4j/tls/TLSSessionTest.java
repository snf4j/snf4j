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
package org.snf4j.tls;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.junit.After;
import org.junit.Assume;
import org.junit.Test;
import org.snf4j.core.EngineStreamSession;
import org.snf4j.core.SSLSession;
import org.snf4j.core.SelectorLoop;
import org.snf4j.core.StreamSession;
import org.snf4j.core.factory.AbstractSessionFactory;
import org.snf4j.core.factory.IStreamSessionFactory;
import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.session.SSLEngineCreateException;
import org.snf4j.core.session.ssl.ClientAuth;
import org.snf4j.core.session.ssl.SSLContextBuilder;
import org.snf4j.core.session.ssl.SSLEngineBuilder;
import org.snf4j.tls.engine.DelegatedTaskMode;
import org.snf4j.tls.engine.EngineHandler;
import org.snf4j.tls.engine.EngineParametersBuilder;
import org.snf4j.tls.session.SessionManager;

public class TLSSessionTest extends CommonTest {

	final static char[] PASSWORD = "password".toCharArray();

	final static int PORT = 8081;
	
	final static String HOST = "127.0.0.1";
	
	final static String HOST_NAME = "host.org";
	
	final static long TIMEOUT = 2000;
	
	SelectorLoop loop;
	
	KeyManagerFactory kmf;
	
	X509KeyManager km;

	TrustManagerFactory tmf;
	
	X509TrustManager tm;
	
	SSLEngineBuilder cliJdk, srvJdk;
	
	final StringBuilder trace = new StringBuilder();
	
	final AtomicReference<EngineStreamSession> cli = new AtomicReference<EngineStreamSession>();
	
	final AtomicReference<EngineStreamSession> srv = new AtomicReference<EngineStreamSession>();
	
	void trace(String s) {
		synchronized (trace) {
			trace.append(s);
		}
	}
	
	String trace() {
		String s;
		
		synchronized (trace) {
			s = trace.toString();
			trace.setLength(0);
		}
		return s;
	}
	
	@Override
	public void before() throws Exception {
		super.before();
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null, null);
		ks.setKeyEntry("key", key("EC", "secp256r1"), PASSWORD, new X509Certificate[] {cert("secp256r1")});
        kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks,PASSWORD);
        km =  (X509KeyManager) kmf.getKeyManagers()[0];
		
        ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null, null);
		ks.setCertificateEntry("ca", cert("secp256r1"));
        tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        tm = (X509TrustManager) tmf.getTrustManagers()[0];
		        
		loop = new SelectorLoop();
		loop.start();
	}
	
	@After
	public void after() throws Exception {
		if (loop != null) {
			loop.stop();
			loop.join(TIMEOUT);
		}
	}
	
	void initJdk11() throws Exception {
		Assume.assumeTrue(JAVA11);
        cliJdk = SSLContextBuilder.forClient()
        		.protocol("TLSv1.3")
				.keyManager(kmf)
				.trustManager(tmf)
				.engineBuilder();        
        srvJdk = SSLContextBuilder.forServer(kmf)
        		.protocol("TLSv1.3")
				.trustManager(tmf)
				.clientAuth(ClientAuth.NONE)
				.engineBuilder();        
 	}

	EngineStreamSession client(boolean jdk, DelegatedTaskMode mode) throws Exception {
		return client(jdk, mode, null);
	}
	
	EngineStreamSession client(boolean jdk, DelegatedTaskMode mode, String name) throws Exception {
		SocketChannel channel = SocketChannel.open();
		channel.configureBlocking(false);
		channel.connect(new InetSocketAddress(InetAddress.getByName(HOST), PORT));
		
		EngineStreamSession session;

		if (jdk) {
			session = new SSLSession(
					channel.getRemoteAddress(), 
					new ClientHandler(cliJdk), true);
		}
		else {
			EngineParametersBuilder builder = new EngineParametersBuilder()
				.delegatedTaskMode(mode)
				.compatibilityMode(true)
				.peerHost(HOST_NAME)
				.peerPort(PORT);
			if (name != null) {
				session = new TLSSession(
						name,
						builder.build(), 
						new EngineHandler(km,tm), 
						new ClientHandler(null), 
						true);
			}
			else {
				session = new TLSSession(
						builder.build(), 
						new EngineHandler(km,tm), 
						new ClientHandler(null), 
						true);
			}
		}
		loop.register(channel, session)
			.sync(TIMEOUT)
			.getSession()
			.getReadyFuture()
			.sync(TIMEOUT);
		
		return session;
	}

	void server(boolean jdk, DelegatedTaskMode mode) throws Exception {
		ServerSocketChannel schannel = ServerSocketChannel.open();
		schannel.configureBlocking(false);
		schannel.socket().bind(new InetSocketAddress(PORT));

		IStreamSessionFactory factory;
		
		if (jdk) {
			factory = new AbstractSessionFactory(true) {

				@Override
				protected IStreamHandler createHandler(SocketChannel channel) {
					return new ServerHandler(srvJdk);
				}
			};
		}
		else {
			EngineParametersBuilder builder = new EngineParametersBuilder()
					.delegatedTaskMode(mode)
					.compatibilityMode(true);
			factory = new IStreamSessionFactory() {

				@Override
				public StreamSession create(SocketChannel channel)
						throws Exception {
					return new TLSSession(
							builder.build(),
							new EngineHandler(km,tm,new SessionManager()),
							new ServerHandler(null),
							false
							);
				}

				@Override
				public void registered(ServerSocketChannel channel) {
				}

				@Override
				public void closed(ServerSocketChannel channel) {
				}

				@Override
				public void exception(ServerSocketChannel channel, Throwable exception) {
				}
			};
		}
		loop.register(schannel, factory)
			.sync(TIMEOUT);
	}
	
	@Test
	public void testJdkToJdk() throws Exception {
		initJdk11();
		server(true, DelegatedTaskMode.ALL);
		EngineStreamSession session = client(true, DelegatedTaskMode.ALL);

		session.write("Text1".getBytes()).sync();
		session.close();
		session.getCloseFuture().sync(TIMEOUT);
		assertEquals("Text1", trace());

		session = client(true, DelegatedTaskMode.ALL);
		session.write("Text2".getBytes()).sync();
		session.close();
		session.getEndFuture().sync(TIMEOUT);
		assertEquals("Text2", trace());
	}

	@Test
	public void testTlsToJdk() throws Exception {
		initJdk11();
		server(true, DelegatedTaskMode.ALL);
		EngineStreamSession session = client(false, DelegatedTaskMode.ALL);

		session.write("Text1".getBytes()).sync();
		Object o = session.getEngineSession();
		session.close();
		session.getCloseFuture().sync(TIMEOUT);
		assertEquals("Text1", trace());

		session = client(false, DelegatedTaskMode.ALL);
		session.write("Text2".getBytes()).sync();
		assertSame(o, session.getEngineSession());
		session.close();
		session.getEndFuture().sync(TIMEOUT);
		assertEquals("Text2", trace());
	}

	@Test
	public void testJdkToTls() throws Exception {
		initJdk11();
		server(false, DelegatedTaskMode.ALL);
		EngineStreamSession session = client(true, DelegatedTaskMode.ALL);

		session.write("Text1".getBytes()).sync();
		session.close();
		session.getCloseFuture().sync(TIMEOUT);
		assertEquals("Text1", trace());

		session = client(true, DelegatedTaskMode.ALL);
		session.write("Text2".getBytes()).sync();
		session.close();
		session.getEndFuture().sync(TIMEOUT);
		assertEquals("Text2", trace());
	}
	
	@Test
	public void testTlsToTlsAllTasks() throws Exception {
		server(false, DelegatedTaskMode.ALL);
		EngineStreamSession session = client(false, DelegatedTaskMode.ALL,"Name1");

		assertEquals("Name1", session.getName());
		session.write("Text1".getBytes()).sync();
		Object o = session.getEngineSession();
		session.close();
		session.getCloseFuture().sync(TIMEOUT);
		assertEquals("Text1", trace());
		
		session = client(false, DelegatedTaskMode.ALL);
		assertEquals("Session-" + session.getId(), session.getName());
		session.write("Text2".getBytes()).sync();
		assertSame(o, session.getEngineSession());
		session.close();
		session.getCloseFuture().sync(TIMEOUT);
		assertEquals("Text2", trace());
	}

	@Test
	public void testTlsToTlsCertificateTasks() throws Exception {
		server(false, DelegatedTaskMode.CERTIFICATES);
		EngineStreamSession session = client(false, DelegatedTaskMode.CERTIFICATES);

		session.write("Text1".getBytes()).sync();
		Object o = session.getEngineSession();
		session.close();
		session.getCloseFuture().sync(TIMEOUT);
		assertEquals("Text1", trace());
		
		session = client(false, DelegatedTaskMode.CERTIFICATES);
		session.write("Text2".getBytes()).sync();
		assertSame(o, session.getEngineSession());
		session.close();
		session.getCloseFuture().sync(TIMEOUT);
		assertEquals("Text2", trace());
	}

	@Test
	public void testTlsToTlsNoneTasks() throws Exception {
		server(false, DelegatedTaskMode.NONE);
		EngineStreamSession session = client(false, DelegatedTaskMode.NONE);

		session.write("Text1".getBytes()).sync();
		Object o = session.getEngineSession();
		session.close();
		session.getCloseFuture().sync(TIMEOUT);
		assertEquals("Text1", trace());
		
		session = client(false, DelegatedTaskMode.NONE);
		session.write("Text2".getBytes()).sync();
		assertSame(o, session.getEngineSession());
		session.close();
		session.getCloseFuture().sync(TIMEOUT);
		assertEquals("Text2", trace());
	}
	
	class Handler extends AbstractStreamHandler {

		final DefaultSessionConfig config;
		
		final boolean clientMode;
		
		Handler(boolean clientMode, DefaultSessionConfig config, SSLEngineBuilder builder) {
			this.clientMode = clientMode;
			this.config = config;
			if (builder != null) {
				config.addSSLEngineBuilder(builder);
			}
			config.setWaitForInboundCloseMessage(true);
		}
		
		@Override
		public void read(Object msg) {
			if (clientMode) {
				clientRead((byte[]) msg);
			}
			else {
				serverRead((byte[]) msg);
			}
		}

		@Override
		public void event(SessionEvent event) {
			if (event == SessionEvent.READY) {
				if (clientMode) {
					cli.set((EngineStreamSession) getSession());
				}
				else {
					srv.set((EngineStreamSession) getSession());
				}
			}
		}
		
		public void serverRead(byte[] msg) {
			getSession().write(msg);
		}

		public void clientRead(byte[] msg) {
			trace(new String(msg));
		}
		
		@Override
		public ISessionConfig getConfig() {
			return config;
		}
		
		@Override
		public void exception(Throwable t) {
			t.printStackTrace();
		}
	}
	
	class ClientHandler extends Handler {
				
		ClientHandler(SSLEngineBuilder builder) {
			super(true, new DefaultSessionConfig() {
				public SSLEngine createSSLEngine(SocketAddress remoteAddress, boolean clientMode) throws SSLEngineCreateException {
					return getSSLEngineBuilder(clientMode).build(HOST_NAME, PORT);
				}
			}, 
			builder);
		}
	}

	class ServerHandler extends Handler {

		ServerHandler(SSLEngineBuilder builder) {
			super(false, new DefaultSessionConfig(), builder);
		}
	}
	
}
