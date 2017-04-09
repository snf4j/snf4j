/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017 SNF4J contributors
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
package org.snf4j.core.future;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.junit.Test;
import org.snf4j.core.DatagramSession;
import org.snf4j.core.SelectorLoop;
import org.snf4j.core.Server;
import org.snf4j.core.StreamSession;
import org.snf4j.core.TestDatagramHandler;
import org.snf4j.core.factory.AbstractSessionFactory;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.handler.TestHandler;
import org.snf4j.core.session.IDatagramSession;
import org.snf4j.core.session.IStreamSession;

public class RegisterFutureTest {

	static final int PORT = 7777;
	static final long TIMEOUT = 2000;
	
	class SessionFactory extends AbstractSessionFactory {

		@Override
		protected IStreamHandler createHandler(SocketChannel channel) {
			return new TestHandler();
		}
		
	}
	
	@Test
	public void testSuccess() {
		RegisterFuture<Void> f = new RegisterFuture<Void>(null);
		
		assertNull(f.getSession());
		assertFalse(f.isDone());
		f.success();
		assertTrue(f.isDone());
		assertTrue(f.isSuccessful());
		f.success();
		assertTrue(f.isDone());
		assertTrue(f.isSuccessful());
		
		f = new RegisterFuture<Void>(null);
		f.abort(null);
		assertTrue(f.isDone());
		assertTrue(f.isCancelled());
		f.success();
		assertTrue(f.isDone());
		assertFalse(f.isSuccessful());
	}
	
	@Test
	public void testAbort() {
		RegisterFuture<Void> f = new RegisterFuture<Void>(null);
		Exception cause = new Exception();
		
		f.abort(null);
		assertTrue(f.isDone());
		assertTrue(f.isCancelled());
		f.abort(cause);
		assertTrue(f.isDone());
		assertTrue(f.isCancelled());
		assertNull(f.cause());
		
		f = new RegisterFuture<Void>(null);
		f.success();
		f.abort(null);
		assertTrue(f.isDone());
		assertFalse(f.isCancelled());

		f = new RegisterFuture<Void>(null);
		f.abort(cause);
		assertTrue(f.isDone());
		assertTrue(f.isFailed());
		assertTrue(cause == f.cause());
		f.abort(null);
		assertTrue(f.isDone());
		assertTrue(f.isFailed());
		assertTrue(cause == f.cause());

	}
	
	@Test
	public void testRegister() throws Exception {
		Server s = new Server(PORT);
		s.start();
		
		//register stream handler
		SelectorLoop loop = new SelectorLoop();
		SocketChannel channel = SocketChannel.open();
		channel.configureBlocking(false);
		channel.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT));
		IFuture<Void> future = loop.register(channel, new TestHandler());
		assertFalse(future.await(300).isDone());
		loop.start();
		assertTrue(future.await(TIMEOUT).isDone());
		assertTrue(future.isSuccessful());
		assertTrue(future.getSession() instanceof IStreamSession);
		future.getSession().getOpenFuture().sync(TIMEOUT);
		future.getSession().close();
		s.waitForSessionEnding(TIMEOUT);

		//register stream session
		loop = new SelectorLoop();
		channel = SocketChannel.open();
		channel.configureBlocking(false);
		channel.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT));
		StreamSession session = new StreamSession(new TestHandler());
		future = loop.register(channel, session);
		assertFalse(future.await(300).isDone());
		loop.start();
		assertTrue(future.await(TIMEOUT).isDone());
		assertTrue(future.isSuccessful());
		assertTrue(future.getSession() == session);
		future.getSession().getOpenFuture().sync(TIMEOUT);
		future.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		
		s.stop(TIMEOUT);
		loop.stop();
		assertTrue(loop.join(TIMEOUT));
		
		//register server channel
		loop = new SelectorLoop();

		ServerSocketChannel schannel = ServerSocketChannel.open();
		
		schannel.configureBlocking(false);
		schannel.socket().bind(new InetSocketAddress(PORT));
		future = loop.register(schannel, new SessionFactory());
		assertFalse(future.await(300).isDone());
		loop.start();
		assertTrue(future.await(TIMEOUT).isDone());
		assertTrue(future.isSuccessful());
		assertNull(future.getSession());
		loop.stop();
		assertTrue(loop.join(TIMEOUT));

		//register datagram handler
		loop = new SelectorLoop();
		DatagramChannel dchannel = DatagramChannel.open();
		dchannel.configureBlocking(true);
		dchannel.socket().bind(new InetSocketAddress(PORT));
		future = loop.register(dchannel, new TestDatagramHandler());
		assertFalse(future.await(300).isDone());
		loop.start();
		assertTrue(future.await(TIMEOUT).isDone());
		assertTrue(future.isSuccessful());
		assertTrue(future.getSession() instanceof IDatagramSession);
		future.getSession().close();
		future.getSession().getEndFuture().sync(TIMEOUT);
		loop.stop();
		assertTrue(loop.join(TIMEOUT));

		//register datagram session
		loop = new SelectorLoop();
		dchannel = DatagramChannel.open();
		dchannel.configureBlocking(true);
		dchannel.socket().bind(new InetSocketAddress(PORT));
		DatagramSession dsession = new DatagramSession(new TestDatagramHandler());
		future = loop.register(dchannel, dsession);
		assertFalse(future.await(300).isDone());
		loop.start();
		assertTrue(future.await(TIMEOUT).isDone());
		assertTrue(future.isSuccessful());
		assertTrue(future.getSession() == dsession);
		future.getSession().close();
		future.getSession().getEndFuture().sync(TIMEOUT);
		loop.stop();
		assertTrue(loop.join(TIMEOUT));
		
	}
	
}