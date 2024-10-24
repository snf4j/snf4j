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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Test;
import org.snf4j.core.DatagramServerHandler;
import org.snf4j.core.EngineDatagramSession;
import org.snf4j.core.SelectorLoop;
import org.snf4j.core.engine.IEngine;
import org.snf4j.core.factory.IDatagramHandlerFactory;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.session.DefaultSessionConfig;
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

	DefaultSessionConfig srvConfig;
	
	DefaultSessionConfig cliConfig;
	
	EngineHandlerBuilder handlerBld;
	
	EngineParametersBuilder paramBld;
	
	public void before() throws Exception {
		super.before();
		loop = new SelectorLoop();
		loop.start();
		handlerBld = new EngineHandlerBuilder(km(), tm());
		paramBld = new EngineParametersBuilder().delegatedTaskMode(DelegatedTaskMode.NONE);
		timer = new DefaultTimer(true);
		srvConfig = new DefaultSessionConfig();
		cliConfig = new DefaultSessionConfig();
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

	IFuture<Void> client(TestHandler handler) throws Exception {
		DatagramChannel channel = DatagramChannel.open();
		channel.configureBlocking(false);
		channel.connect(new InetSocketAddress(InetAddress.getByName(HOST), PORT));

		QuicEngine e = new QuicEngine(true, paramBld.build(), handlerBld.build());
		QuicSession session = new QuicSession(e, handler);
		return loop.register(channel, session);
	}
	
	EngineDatagramSession client(TestHandler handler, long timeout) throws Exception {
		DatagramChannel channel = DatagramChannel.open();
		channel.configureBlocking(false);
		channel.connect(new InetSocketAddress(InetAddress.getByName(HOST), PORT));

		QuicEngine e = new QuicEngine(true, paramBld.build(), handlerBld.build());
		QuicSession session = new QuicSession(e, handler);
		
		loop.register(channel, session)
			.sync(TIMEOUT);
		
		if (timeout >= 0) {
			session.getReadyFuture().sync(timeout);
		}
		
		return session;
	}

	EngineDatagramSession client() throws Exception {
		return client(new TestHandler(timer, cliConfig), TIMEOUT);
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
	
	TestHandler server() throws Exception {
		TestHandler handler = new TestHandler(timer, srvConfig);
		server(handler, PORT);
		return handler;
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
	public void testCloseNoWaitForCloseMessage() throws Exception {
		srvConfig.setWaitForInboundCloseMessage(false);
		cliConfig.setWaitForInboundCloseMessage(false);
		paramBld.delegatedTaskMode(DelegatedTaskMode.ALL);
		TestHandler srv = server();
		EngineDatagramSession cli = client();
		cli.close();
		cli.getCloseFuture().sync(TIMEOUT);
		srv.getSession().getCloseFuture().sync(TIMEOUT);
	}

	@Test
	public void testClose() throws Exception {
		srvConfig.setWaitForInboundCloseMessage(true);
		cliConfig.setWaitForInboundCloseMessage(true);
		paramBld.delegatedTaskMode(DelegatedTaskMode.ALL);
		TestHandler srv = server();
		EngineDatagramSession cli = client();
		cli.close();
		cli.getCloseFuture().sync(TIMEOUT);
		srv.getSession().getCloseFuture().sync(TIMEOUT);
	}

	@Test
	public void testQuicClose() throws Exception {
		srvConfig.setWaitForInboundCloseMessage(true);
		cliConfig.setWaitForInboundCloseMessage(true);
		paramBld.delegatedTaskMode(DelegatedTaskMode.ALL);
		TestHandler srv = server();
		EngineDatagramSession cli = client();
		cli.quickClose();
		cli.getCloseFuture().sync(TIMEOUT);
		srv.getSession().getCloseFuture().sync(TIMEOUT);
	}

	@Test
	public void testDirtyClose() throws Exception {
		srvConfig.setWaitForInboundCloseMessage(true);
		cliConfig.setWaitForInboundCloseMessage(true);
		paramBld.delegatedTaskMode(DelegatedTaskMode.ALL);
		TestHandler srv = server();
		EngineDatagramSession cli = client();
		waitFor(100);
		cli.dirtyClose();
		cli.getCloseFuture().sync(TIMEOUT);
		waitFor(TIMEOUT);
		assertFalse(srv.getSession().getCloseFuture().isDone());
	}
	
	@Test
	public void closeInClientCreatedEvent() throws Exception {
		srvConfig.setWaitForInboundCloseMessage(true);
		cliConfig.setWaitForInboundCloseMessage(true);
		paramBld.delegatedTaskMode(DelegatedTaskMode.ALL);
		TestHandler srvh = server();
		TestHandler clih = new TestHandler(timer, cliConfig) {
			@Override
			protected void created() {
				getSession().close();
			}
		};
		
		EngineDatagramSession cli = (EngineDatagramSession) client(clih).getSession();
		cli.getCreateFuture().sync(TIMEOUT);
		cli.getEndFuture().sync(TIMEOUT);
		assertTrue(cli.getOpenFuture().isCancelled());
		assertTrue(cli.getReadyFuture().isCancelled());
		assertTrue(cli.getCloseFuture().isCancelled());
		waitFor(100);
		assertEquals("CR|EN|", clih.trace());
		assertEquals("", srvh.trace());		
	}

	@Test
	public void closeInServerCreatedEvent() throws Exception {
		srvConfig.setWaitForInboundCloseMessage(true);
		cliConfig.setWaitForInboundCloseMessage(true);
		cliConfig.setEngineHandshakeTimeout(3000);
		paramBld.delegatedTaskMode(DelegatedTaskMode.ALL);
		TestHandler srvh = new TestHandler(timer, srvConfig) {
			@Override
			protected void created() {
				getSession().close();
			}
		};
		TestHandler clih = new TestHandler(timer, cliConfig);
		server(srvh, PORT);
		
		EngineDatagramSession cli = (EngineDatagramSession) client(clih).getSession();
		cli.getCreateFuture().sync(TIMEOUT);
		cli.getOpenFuture().sync(TIMEOUT);
		try {
			cli.getCloseFuture().sync(4000);
		}
		catch (ExecutionException e) {
		}
		waitFor(100);
        assertEquals("CR|OP|CL|EN|", clih.trace());
        //Two sessions as PING from loss detector creates the second one
		assertEquals("CR|EN|CR|EN|", srvh.trace());		
	}
	
	@Test
	public void closeInClientOpenedEvent() throws Exception {
		srvConfig.setWaitForInboundCloseMessage(true);
		cliConfig.setWaitForInboundCloseMessage(true);
		paramBld.delegatedTaskMode(DelegatedTaskMode.ALL);
		TestHandler srvh = server();
		TestHandler clih = new TestHandler(timer, cliConfig) {
			@Override
			protected void opened() {
				getSession().close();
			}
		};
		
		EngineDatagramSession cli = (EngineDatagramSession) client(clih).getSession();
		cli.getCreateFuture().sync(TIMEOUT);
		cli.getOpenFuture().sync(TIMEOUT);
		cli.getCloseFuture().sync(TIMEOUT);
		cli.getEndFuture().sync(TIMEOUT);
		assertTrue(cli.getReadyFuture().isCancelled());
		waitFor(100);
		assertEquals("CR|OP|CL|EN|", clih.trace());
		assertEquals("", srvh.trace());		
	}

	@Test
	public void closeInServerOpenEvent() throws Exception {
		srvConfig.setWaitForInboundCloseMessage(true);
		cliConfig.setWaitForInboundCloseMessage(true);
		cliConfig.setEngineHandshakeTimeout(3000);
		paramBld.delegatedTaskMode(DelegatedTaskMode.ALL);
		TestHandler srvh = new TestHandler(timer, srvConfig) {
			@Override
			protected void opened() {
				getSession().close();
			}
		};
		TestHandler clih = new TestHandler(timer, cliConfig);
		server(srvh, PORT);
		
		EngineDatagramSession cli = (EngineDatagramSession) client(clih).getSession();
		cli.getCreateFuture().sync(TIMEOUT);
		cli.getOpenFuture().sync(TIMEOUT);
		try {
			cli.getCloseFuture().sync(4000);
		}
		catch (ExecutionException e) {
		}
		waitFor(100);
        assertEquals("CR|OP|CL|EN|", clih.trace());
        //Two sessions as PING from loss detector creates the second one
		assertEquals("CR|OP|CL|EN|CR|OP|CL|EN|", srvh.trace());		
	}
		
	@Test
	public void closeInClientReadyEvent() throws Exception {
		srvConfig.setWaitForInboundCloseMessage(true);
		cliConfig.setWaitForInboundCloseMessage(true);
		paramBld.delegatedTaskMode(DelegatedTaskMode.ALL);
		TestHandler srvh = server();
		TestHandler clih = new TestHandler(timer, cliConfig) {
			@Override
			protected void ready() {
				getSession().close();
			}
		};
		
		EngineDatagramSession cli = (EngineDatagramSession) client(clih).getSession();
		cli.getCreateFuture().sync(TIMEOUT);
		cli.getOpenFuture().sync(TIMEOUT);
		cli.getReadyFuture().sync(TIMEOUT);
		cli.getCloseFuture().sync(TIMEOUT);
		cli.getEndFuture().sync(TIMEOUT);
		waitFor(100);
		assertEquals("CR|OP|RE|CL|EN|", clih.trace());
		assertEquals("CR|OP|RE|CL|EN|", srvh.trace());		
	}

	@Test
	public void closeInServerReadyEvent() throws Exception {
		srvConfig.setWaitForInboundCloseMessage(true);
		cliConfig.setWaitForInboundCloseMessage(true);
		cliConfig.setEngineHandshakeTimeout(3000);
		paramBld.delegatedTaskMode(DelegatedTaskMode.ALL);
		TestHandler srvh = new TestHandler(timer, srvConfig) {
			@Override
			protected void ready() {
				getSession().close();
			}
		};
		TestHandler clih = new TestHandler(timer, cliConfig);
		server(srvh, PORT);
		
		EngineDatagramSession cli = (EngineDatagramSession) client(clih).getSession();
		cli.getCreateFuture().sync(TIMEOUT);
		cli.getOpenFuture().sync(TIMEOUT);
		cli.getReadyFuture().sync(TIMEOUT);
		cli.getCloseFuture().sync(TIMEOUT);
		cli.getEndFuture().sync(TIMEOUT);
		waitFor(100);
        assertEquals("CR|OP|RE|CL|EN|", clih.trace());
		assertEquals("CR|OP|RE|CL|EN|", srvh.trace());		
	}
	
	@Test
	public void closeInClientClosedEvent() throws Exception {
		srvConfig.setWaitForInboundCloseMessage(true);
		cliConfig.setWaitForInboundCloseMessage(true);
		paramBld.delegatedTaskMode(DelegatedTaskMode.ALL);
		TestHandler srvh = server();
		TestHandler clih = new TestHandler(timer, cliConfig) {
			@Override
			protected void closed() {
				getSession().close();
			}
		};
		
		EngineDatagramSession cli = (EngineDatagramSession) client(clih).getSession();
		cli.getCreateFuture().sync(TIMEOUT);
		cli.getOpenFuture().sync(TIMEOUT);
		cli.getReadyFuture().sync(TIMEOUT);
		cli.close();
		cli.getCloseFuture().sync(TIMEOUT);
		cli.getEndFuture().sync(TIMEOUT);
		waitFor(100);
		assertEquals("CR|OP|RE|CL|EN|", clih.trace());
		assertEquals("CR|OP|RE|CL|EN|", srvh.trace());		
	}
	
	@Test
	public void testLostConnectionClose() throws Exception {
		srvConfig.setWaitForInboundCloseMessage(true);
		cliConfig.setWaitForInboundCloseMessage(true);
		paramBld.delegatedTaskMode(DelegatedTaskMode.ALL);
		TestHandler srvh = new TestHandler(timer, srvConfig);
		TestHandler clih = new TestHandler(timer, cliConfig);
		server(srvh, PORT);
		EngineDatagramSession cli = (EngineDatagramSession) client(clih).getSession();
		waitFor(1000);
		srvh.getSession().dirtyClose();
		srvh.getSession().getEndFuture().sync(TIMEOUT);
		cli.close();
		cli.getCloseFuture().sync(100000);
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
