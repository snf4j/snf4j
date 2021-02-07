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
package org.snf4j.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.junit.Test;

public class ChannelContextTest {

	@Test
	public void testChannelContext() throws Exception {
		TestChannelContext ctx = new TestChannelContext(55);
		
		ServerSocketChannel channel = ServerSocketChannel.open();
		channel.bind(new InetSocketAddress(7777));
		assertTrue(channel.isOpen());
		ctx.postClose(channel);
		ctx.postRegistration(channel);
		assertEquals(channel.toString(), ctx.toString(channel));
		assertNull(ctx.toString(null));
		assertTrue(ctx.completeRegistration(null, null, channel));
		ctx.handle(null, null);
		ctx.exception(null, null);
		assertTrue(channel.isOpen());
		ctx.close(channel);
		assertFalse(channel.isOpen());
		assertTrue(ctx.finishConnect(null));
	}
	
	@Test
	public void testServerChannelContext() {
		TestServerChannelContext ctx = new TestServerChannelContext(45);
		
		assertTrue(ctx.isServer());
		assertFalse(ctx.isSession());
		assertNull(ctx.getSession());
		ctx.shutdown(null);
		assertTrue(ctx.exceptionOnDecodingFailure());
	}
	
	@Test
	public void testSessionChannelContext() {
		StreamSession session = new StreamSession(new TestHandler(""));
		TestSessionChannelContext ctx = new TestSessionChannelContext(session);
		
		assertFalse(ctx.isServer());
		assertTrue(ctx.isSession());
		assertTrue(session == ctx.getSession());
		try {
			ctx.accept(null);
			fail();
		}
		catch (Exception e) {
			assertTrue(e.getClass() == UnsupportedOperationException.class);
		}
		try {
			ctx.create(null);
			fail();
		}
		catch (Exception e) {
			assertTrue(e.getClass() == UnsupportedOperationException.class);
		}
		
	}
	
	@Test
	public void testSocketChannelContext() throws Exception {
		StreamSession session = new StreamSession(new TestHandler(""));
		SocketChannelContext ctx = new SocketChannelContext(session);
		ChannelContext<StreamSession> ctx2 = ctx.wrap(session);
		
		assertTrue(ctx2.getClass() == SocketChannelContext.class);
		assertTrue(session == ctx2.getSession());
		assertTrue(ctx.exceptionOnDecodingFailure());
		
		SocketChannel sc = SocketChannel.open();
		sc.configureBlocking(false);
		Selector s = Selector.open();
		SelectionKey k = sc.register(s, 0);
		ctx.handle(null, k);
		sc.close();
		s.close();
	}
	
	@Test
	public void testDatagramChannelContext() throws Exception {
		DatagramSession session = new DatagramSession(new TestDatagramHandler());
		DatagramChannelContext ctx = new DatagramChannelContext(session);
		ChannelContext<DatagramSession> ctx2 = ctx.wrap(session);
		
		assertTrue(ctx2.getClass() == DatagramChannelContext.class);
		assertTrue(session == ctx2.getSession());
		assertFalse(ctx.exceptionOnDecodingFailure());
		
		ChannelContext<?> ctx3 = new SocketChannelContext(null);
		SocketChannel sc = SocketChannel.open();
		assertEquals(sc.toString(), ctx3.toString(sc));
		sc.close();
		ctx = new DatagramChannelContext(null);
		ctx.shutdown(null);
		DatagramChannel dc = DatagramChannel.open();
		assertEquals("sun.nio.ch.DatagramChannelImpl[local=unknown]", ctx.toString(dc));
		dc.socket().bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 7788));
		assertEquals("sun.nio.ch.DatagramChannelImpl[local=/127.0.0.1:7788]", ctx.toString(dc));
		dc.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.2"), 7789));
		assertEquals("sun.nio.ch.DatagramChannelImpl[local=/127.0.0.1:7788,remote=/127.0.0.2:7789]", ctx.toString(dc));
		assertNull(ctx.toString(null));
		dc.close();
		
		dc = DatagramChannel.open();
		dc.configureBlocking(false);
		Selector s = Selector.open();
		SelectionKey k = dc.register(s, 0);
		ctx.handle(null, k);
		dc.close();
		s.close();
	}
	
	static class TestSessionChannelContext extends SessionChannelContext<StreamSession> {

		TestSessionChannelContext(StreamSession context) {
			super(context);
		}

		@Override
		ChannelContext<StreamSession> wrap(InternalSession session) {
			return null;
		}

		@Override
		boolean finishConnect(SelectableChannel channel) throws Exception {
			return false;
		}

		@Override
		void shutdown(SelectableChannel channel) throws Exception {
		}

		@Override
		boolean exceptionOnDecodingFailure() {
			return false;
		}
		
	}
	
	static class TestServerChannelContext extends ServerChannelContext<Integer> {

		TestServerChannelContext(Integer context) {
			super(context);
		}

		@Override
		ChannelContext<?> wrap(InternalSession session) {
			return null;
		}

		@Override
		SelectableChannel accept(SelectableChannel channel) throws Exception {
			return null;
		}

		@Override
		InternalSession create(SelectableChannel channel) throws Exception {
			return null;
		}

	}
	
	static class TestChannelContext extends ChannelContext<Integer> {

		TestChannelContext(Integer context) {
			super(context);
		}

		@Override
		boolean isServer() {
			return false;
		}

		@Override
		boolean isSession() {
			return false;
		}

		@Override
		InternalSession getSession() {
			return null;
		}

		@Override
		ChannelContext<?> wrap(InternalSession session) {
			return null;
		}

		@Override
		SelectableChannel accept(SelectableChannel channel) throws Exception {
			return null;
		}

		@Override
		InternalSession create(SelectableChannel channel) throws Exception {
			return null;
		}

		@Override
		void shutdown(SelectableChannel channel) throws Exception {
		}

		@Override
		boolean exceptionOnDecodingFailure() {
			return false;
		}

	}
}
