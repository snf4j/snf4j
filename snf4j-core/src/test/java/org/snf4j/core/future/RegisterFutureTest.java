package org.snf4j.core.future;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedSelectorException;
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
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.IStreamSession;

public class RegisterFutureTest {

	static final int PORT = 7777;
	static final long TIMEOUT = 2000;
	
	private void waitFor(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}
	
	class SessionFactory extends AbstractSessionFactory {

		@Override
		protected IStreamHandler createHandler(SocketChannel channel) {
			return new TestHandler();
		}
		
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
	
	void assertNotSuccessfulSessionFutures(ISession session, Throwable cause, String futuresStates) {
		@SuppressWarnings("unchecked")
		IFuture<Void>[] futures = new IFuture[4];
		
		futures[0] = session.getCreateFuture();
		futures[1] = session.getOpenFuture();
		futures[2] = session.getCloseFuture();
		futures[3] = session.getEndFuture();
		
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < futures.length; ++i) {
			if (futures[i].isCancelled()) {
				sb.append('C');
			} else if (futures[i].isSuccessful()) {
				sb.append('S');
			} else if (futures[i].isFailed()) {
				sb.append('F');
			} else {
				sb.append('N');
			}
		}
		assertEquals(futuresStates, sb.toString());
	}
	
	@Test
	public void testAbortRegister() throws Exception {
		SelectorLoop loop = new SelectorLoop();
		
		//exception thrown
		DatagramChannel channel = DatagramChannel.open();
		channel.configureBlocking(true);
		channel.socket().bind(new InetSocketAddress(PORT));
		TestDatagramHandler h1 = new TestDatagramHandler();
		h1.createException = new NullPointerException();
		IFuture<Void> f1 = loop.register(channel, h1);
		channel = DatagramChannel.open();
		channel.configureBlocking(true);
		channel.socket().bind(new InetSocketAddress(PORT+1));
		IFuture<Void> f2 = loop.register(channel, new TestDatagramHandler());
		loop.start();
		assertTrue(f1.await(TIMEOUT).isFailed());
		//assertNotSuccessfulSessionFutures(f1.getSession(), h1.createException, "SCCS");
		f2.sync(TIMEOUT);
		loop.stop();
		assertTrue(loop.join(TIMEOUT));
		
		//registration of registered channel
		loop = new SelectorLoop();
		channel = DatagramChannel.open();
		channel.configureBlocking(true);
		channel.socket().bind(new InetSocketAddress(PORT));
		f1 = loop.register(channel, new TestDatagramHandler());
		f2 = loop.register(channel, new TestDatagramHandler());
		loop.start();
		f1.sync(TIMEOUT);
		assertTrue(f2.await(TIMEOUT).isCancelled());
		assertNotSuccessfulSessionFutures(f2.getSession(), null, "CCCC");
		loop.stop();
		assertTrue(loop.join(TIMEOUT));
		
		//registration with closed selector exception
		loop = new SelectorLoop();
		channel = DatagramChannel.open();
		channel.configureBlocking(true);
		channel.socket().bind(new InetSocketAddress(PORT));
		h1 = new TestDatagramHandler();
		h1.createException = new ClosedSelectorException();
		f1 = loop.register(channel, h1);
		channel = DatagramChannel.open();
		channel.configureBlocking(true);
		channel.socket().bind(new InetSocketAddress(PORT+1));
		f2 = loop.register(channel, new TestDatagramHandler());
		loop.start();
		assertTrue(f1.await(TIMEOUT).isCancelled());
		//assertNotSuccessfulSessionFutures(f1.getSession(), null, "CCCC");
		assertTrue(f2.await(TIMEOUT).isCancelled());
		assertNotSuccessfulSessionFutures(f2.getSession(), null, "CCCC");
		waitFor(500);
		assertTrue(loop.isStopped());
		loop.stop();
		assertTrue(loop.join(TIMEOUT));
	}
}