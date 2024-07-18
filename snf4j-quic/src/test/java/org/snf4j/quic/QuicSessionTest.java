/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import org.junit.After;
import org.junit.Test;
import org.snf4j.core.DatagramServerHandler;
import org.snf4j.core.EngineDatagramSession;
import org.snf4j.core.SelectorLoop;
import org.snf4j.core.engine.IEngine;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.IDatagramHandlerFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.AbstractDatagramHandler;
import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.timer.DefaultTimer;
import org.snf4j.core.timer.ITimeoutModel;
import org.snf4j.core.timer.ITimer;
import org.snf4j.quic.engine.DisabledTimeoutModel;
import org.snf4j.quic.engine.QuicEngine;
import org.snf4j.tls.engine.DelegatedTaskMode;
import org.snf4j.tls.engine.EngineHandlerBuilder;
import org.snf4j.tls.engine.EngineParametersBuilder;

public class QuicSessionTest extends CommonTest {

	final static int PORT = 8081;
	
	final static String HOST = "127.0.0.1";

	final static long TIMEOUT = 2000;
	
	SelectorLoop loop;
	
	DefaultTimer timer = new DefaultTimer(true);

	EngineHandlerBuilder handlerBld;
	
	EngineParametersBuilder paramBld;
	
	public void before() throws Exception {
		super.before();
		loop = new SelectorLoop();
		loop.start();
		handlerBld = new EngineHandlerBuilder(km(), tm());
		paramBld = new EngineParametersBuilder().delegatedTaskMode(DelegatedTaskMode.NONE);
		timer = new DefaultTimer(true);
	}
	
	@After
	public void after() throws Exception {
		if (loop != null) {
			loop.stop();
			loop.join(TIMEOUT);
		}
		timer.cancel();
	}

	static void waitFor(long millis) throws Exception {
		Thread.sleep(millis);
	}
	
	EngineDatagramSession client() throws Exception {
		DatagramChannel channel = DatagramChannel.open();
		channel.configureBlocking(false);
		channel.connect(new InetSocketAddress(InetAddress.getByName(HOST), PORT));

		QuicEngine e = new QuicEngine(true, paramBld.build(), handlerBld.build());
		QuicSession session = new QuicSession(e, new ClientHandler());
		
		loop.register(channel, session)
			.sync(TIMEOUT)
			.getSession()
			.getReadyFuture()
			.sync(TIMEOUT);
	
		return session;
	}
	
	void server() throws Exception {
		DatagramChannel channel = DatagramChannel.open();
		channel.configureBlocking(false);
		channel.socket().bind(new InetSocketAddress(PORT));

		loop.register(channel, new Server(new IDatagramHandlerFactory() {

			@Override
			public IDatagramHandler create(SocketAddress remoteAddress) {
				return new ServerHandler();
			}			
		})).sync(TIMEOUT);
	}
	
	@Test
	public void testHandshake() throws Exception {
		server();
		EngineDatagramSession c1 = client();
		client();
		
		c1.close();
		c1.getCloseFuture().sync(TIMEOUT);
		c1.close();
		c1.getEndFuture().sync(TIMEOUT);
	}
	
	@Test
	public void testHandshakeWithAllTasks() throws Exception {
		paramBld.delegatedTaskMode(DelegatedTaskMode.ALL);
		server();
		client();
	}

	@Test
	public void testHandshakeWithCertificateTasks() throws Exception {
		paramBld.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES);
		server();
		client();
	}
	
	class Handler extends AbstractDatagramHandler {
		
		final DefaultSessionConfig config = new DefaultSessionConfig();

		@Override
		public void read(SocketAddress remoteAddress, Object msg) {
		}

		@Override
		public void read(Object msg) {
		}
		
		@Override
		public ISessionConfig getConfig() {
			return config;
		}

		@Override
		public ISessionStructureFactory getFactory() {
			return new DefaultSessionStructureFactory() {
				
				@Override
				public ITimer getTimer() {
					return timer;
				}
				
				@Override
				public ITimeoutModel getTimeoutModel() {
					return DisabledTimeoutModel.INSTANCE;
				}
			};
		}
	}
	
	class ServerHandler extends Handler {
	}
	
	class ClientHandler extends Handler {
	}
	
	class Server extends DatagramServerHandler {

		public Server(IDatagramHandlerFactory handlerFactory) {
			super(handlerFactory);
		}
		
		@Override
		protected IEngine createEngine(SocketAddress remoteAddress, ISessionConfig config) throws Exception {
			return new QuicEngine(false, paramBld.build(), handlerBld.build());
		}
	}
}
