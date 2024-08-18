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
import java.util.Arrays;

import org.junit.After;
import org.junit.Test;
import org.snf4j.core.DatagramServerHandler;
import org.snf4j.core.EngineDatagramSession;
import org.snf4j.core.SelectorLoop;
import org.snf4j.core.engine.IEngine;
import org.snf4j.core.factory.IDatagramHandlerFactory;
import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.timer.DefaultTimer;
import org.snf4j.quic.engine.QuicEngine;
import org.snf4j.tls.engine.DelegatedTaskMode;
import org.snf4j.tls.engine.EngineHandlerBuilder;
import org.snf4j.tls.engine.EngineParametersBuilder;

public class QuicSessionTest extends CommonTest {

	final static int PORT = 8081;
	
	final static String HOST = "127.0.0.1";

	final static long TIMEOUT = 2000;
	
	SelectorLoop loop;
	
	DefaultTimer timer;

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
	
	EngineDatagramSession client(TestHandler handler, long timeout) throws Exception {
		DatagramChannel channel = DatagramChannel.open();
		channel.configureBlocking(false);
		channel.connect(new InetSocketAddress(InetAddress.getByName(HOST), PORT));

		QuicEngine e = new QuicEngine(true, paramBld.build(), handlerBld.build());
		QuicSession session = new QuicSession(e, handler);
		
		loop.register(channel, session)
			.sync(TIMEOUT)
			.getSession()
			.getReadyFuture()
			.sync(timeout);
	
		return session;
	}

	EngineDatagramSession client() throws Exception {
		return client(new TestHandler(timer), TIMEOUT);
	}
	
	void server(TestHandler handler, int port) throws Exception {
		DatagramChannel channel = DatagramChannel.open();
		channel.configureBlocking(false);
		channel.socket().bind(new InetSocketAddress(port));

		loop.register(channel, new Server(new IDatagramHandlerFactory() {

			@Override
			public IDatagramHandler create(SocketAddress remoteAddress) {
				return handler;
			}			
		})).sync(TIMEOUT);
	}
	
	void server() throws Exception {
		server(new TestHandler(timer), PORT);
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
		
	@Test
	public void testClientPacketsLost() throws Exception {
		for (int i=0; i<16; ++i) {
			paramBld.delegatedTaskMode(DelegatedTaskMode.ALL);
			TestHandler s = new TestHandler(timer);
			TestHandler c = new TestHandler(timer);
			TestProxy p = new TestProxy(loop, PORT, PORT+1);
			p.srv = s;
			p.cli = c;
			p.start(TIMEOUT);
			boolean[] lost = new boolean[] {0 != (i&1), 0 != (i&2), 0 != (i&4), 0 != (i&8)};
			System.out.println("[INFO] Lost client datagrams #" + i + ": " + Arrays.toString(lost));
			p.action = p.lostAction(lost);
			s.proxyAction = true;
			server(s, PORT+1);
			client(c, 20000);
			after();
			before();
		}
	}	
	
	@Test
	public void testServerPacketLost() throws Exception {
		for (int i=0; i<16; ++i) {
			paramBld.delegatedTaskMode(DelegatedTaskMode.ALL);
			TestHandler s = new TestHandler(timer);
			TestHandler c = new TestHandler(timer);
			TestProxy p = new TestProxy(loop, PORT, PORT+1);
			p.srv = s;
			p.cli = c;
			p.start(TIMEOUT);
			boolean[] lost = new boolean[] {0 != (i&1), 0 != (i&2), 0 != (i&4), 0 != (i&8)};
			System.out.println("[INFO] Lost server datagrams #" + i + ": " + Arrays.toString(lost));
			p.action = p.lostAction(lost);
			c.proxyAction = true;
			server(s, PORT+1);
			client(c, 20000);
			after();
			before();
		}
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
