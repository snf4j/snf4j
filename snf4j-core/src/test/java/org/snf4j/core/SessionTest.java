/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2021 SNF4J contributors
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLEngine;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.allocator.DefaultAllocator;
import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.allocator.TestAllocator;
import org.snf4j.core.codec.DefaultCodecExecutor;
import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.codec.zip.GzipDecoder;
import org.snf4j.core.codec.zip.GzipEncoder;
import org.snf4j.core.codec.zip.ZlibDecoder;
import org.snf4j.core.codec.zip.ZlibEncoder;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.pool.DefaultSelectorLoopPool;
import org.snf4j.core.proxy.HttpProxyHandler;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.session.ISessionTimer;
import org.snf4j.core.session.IllegalSessionStateException;
import org.snf4j.core.session.SSLEngineCreateException;
import org.snf4j.core.session.SessionState;
import org.snf4j.core.session.UnsupportedSessionTimer;
import org.snf4j.core.timer.DefaultTimer;
import org.snf4j.core.timer.ITimer;
import org.snf4j.core.timer.ITimerTask;
import org.snf4j.core.timer.TestTimer;

public class SessionTest {
	long TIMEOUT = 2000;
	int PORT = 7777;
	TestHandler handler = new TestHandler("TestHandler");
	
	Server s;
	Client c;
	HttpProxy p;
	
	@Before
	public void before() {
		s = c = null;
	}
	
	@After
	public void after() throws InterruptedException {
		if (c != null) c.stop(TIMEOUT);
		if (s != null) s.stop(TIMEOUT);
		if (p != null) p.stop(TIMEOUT);
	}
	
	private void waitFor(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}
	
	private SelectionKey registerServerSocketChannel(Selector selector, int port) throws IOException {
		ServerSocketChannel ssc = ServerSocketChannel.open();
		
		ssc.configureBlocking(false);
		ssc.socket().bind(new InetSocketAddress(port));
		return ssc.register(selector, SelectionKey.OP_ACCEPT);
	}
	
	public static boolean isOptimized(StreamSession session) {
		return session.optimizeBuffers && session.optimizeCopying;
	}
	
	private SelectionKey registerSocketChannel(Selector selector, int port) throws IOException {
		SocketChannel sc = SocketChannel.open();
		sc.configureBlocking(false);
		sc.register(selector, SelectionKey.OP_CONNECT);
		sc.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port));
		int state = 0;
		SelectionKey key = null;
		while (state != 3) {
			int i = selector.select();
			if (i > 0) {
				for (SelectionKey k: selector.selectedKeys()) {
					if (k.isAcceptable()) {
						((ServerSocketChannel)k.channel()).accept();
						state |= 1;
					}
					if (k.isConnectable()) {
						if (((SocketChannel)k.channel()).finishConnect()) {
							state |= 2;
							key = k;
						}
					}
				}
			}
		}
		if (key == null) {
			throw new IOException();
		}
		return key;
	}
	
	private byte[] getBytes(int size, int value) {
		return ByteUtils.getBytes(size, value);
	}
	
	private ByteBuffer getBuffer(int size, int value) {
		return ByteBuffer.wrap(getBytes(size, value));
	}
	
	static ByteBuffer getInBuffer(StreamSession s) throws Exception {
		Field f = StreamSession.class.getDeclaredField("inBuffer");
		f.setAccessible(true);
		return (ByteBuffer) f.get(s);
	}
	
	static ByteBuffer[] getOutBuffers(StreamSession s) throws Exception {
		Field f = StreamSession.class.getDeclaredField("outBuffers");
		f.setAccessible(true);
		return (ByteBuffer[]) f.get(s);
	}
	
	static void assertVaraints(String expected, String actual, boolean useVariant) {
		String variant = expected;
		int i,j;
		
		while ((i = expected.indexOf("?{")) != -1) {
			j = expected.indexOf("}", i);
			if (j != -1) {
				String s1 = expected.substring(i, j+1);
				String s2 = expected.substring(i+2,j);
				
				expected = expected.replace(s1, s2);
				variant = variant.replace(s1, "");
			}
			else {
				break;
			}
		}
		if (useVariant) {
			if (!expected.equals(actual)) {
				assertEquals(variant, actual);
			}
		}
		else {
			assertEquals(expected, actual);
		}
	}
	
	@Test
	public void testConstructor() {
		try {
			new StreamSession(null);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		StreamSession s = new StreamSession(handler);
		assertNull(s.getParent());
		s.preCreated();
		assertTrue(handler.getSession() == s);
		assertTrue(s.getHandler() == handler);
		ByteBuffer buf = s.getInBuffer();
		ByteBuffer[] bufs = s.getOutBuffers();
		assertNotNull(buf);
		assertEquals(1024, buf.capacity());
		assertNotNull(bufs);
		assertEquals(1, bufs.length);
		assertEquals(1024, bufs[0].capacity());
		assertEquals("TestHandler", s.getName());
		assertEquals("Session-TestHandler", s.toString());
		
		s = new StreamSession("Name1", handler);
		assertEquals("Name1", s.getName());
		assertEquals("Session-Name1", s.toString());
		
		s = new StreamSession(new TestHandler(null));
		long id1 = s.getId();
		assertEquals("Session-"+id1, s.getName());
		assertEquals("Session-"+id1, s.toString());
		s = new StreamSession(new TestHandler(null));
		long id2 = s.getId();
		assertEquals(id1+1, id2);
		
		ISessionConfig c = s.getConfig();
		assertEquals(1024, c.getMinInBufferCapacity());
		
		Map<Object,Object> a = s.getAttributes();
		assertNotNull(a);
		assertTrue(a == s.getAttributes());
		
		handler = new TestHandler("Test1");
		assertEquals(0, handler.allocatorCount);
		s = new StreamSession(handler);
		assertEquals(1, handler.allocatorCount);
		
		CodecExecutorAdapter codec = new CodecExecutorAdapter(null, null);
		TestInternalSession session = new TestInternalSession("Test2", handler, codec);
		assertTrue(codec == session.codec);
	}
	
	@Test
	public void testGetId() {
		StreamSession s1 = new StreamSession(handler); 
		StreamSession s2 = new StreamSession(handler);
		
		assertTrue(s2.getId() == s1.getId()+1);
	}
	
	@Test
	public void testGetState() throws Exception {
		SelectionKey key = registerServerSocketChannel(Selector.open(), PORT);
		
		StreamSession s = new StreamSession(handler);
		assertEquals(SessionState.OPENING, s.getState());
		assertFalse(s.isOpen());
		s.setSelectionKey(key);
		assertNull(key.attachment());
		assertEquals(SessionState.CLOSING, s.getState());
		key.attach(new SocketChannelContext(new StreamSession(new TestHandler(""))));
		assertEquals(SessionState.CLOSING, s.getState());
		key.attach(new SocketChannelContext(s));
		assertEquals(SessionState.OPEN, s.getState());
		assertTrue(s.isOpen());
		key.channel().close();
		key.selector().close();
		assertEquals(SessionState.CLOSING, s.getState());
		assertFalse(s.isOpen());
	}

	/**
	 * Creates byte array with expected content.
	 * @param expectedContent contains expected content that is formatted in the following way:
	 * "bytes_count=bytes_value[,bytes_count...]"
	 */
	private byte[] getBytes(String expectedContent) {
		return ByteUtils.getBytes(expectedContent);
	}
	
	/**
	 * Asserts content of out buffers.
	 * @param expectedContent contains expected content that is formatted in the following way:
	 * "buffer_capacity:bytes_count=bytes_value[,bytes_count...][;buffer_capacity...]"
	 */
	private void assertOutBuffers(StreamSession session, String expectedContent, boolean compact) {
		ByteBuffer[] buffers = session.getOutBuffers();
		String[] bufferContents = expectedContent.split(";");
		ArrayList<Byte> expectedBytes = new ArrayList<Byte>();
		
		assertEquals("buffer count for "+expectedContent, bufferContents.length, buffers.length);
		int bufferPos = 0;
		
		for (String expectedSize: bufferContents) {
			String[] split1 = expectedSize.split(":");
			String[] split2 = split1[1].split(",");
			int expectedCapacity = Integer.parseInt(split1[0]);
			
			ByteBuffer b = buffers[bufferPos++];
			assertEquals("capacity for "+expectedContent, expectedCapacity, b.capacity());
			expectedBytes.clear();
			for (String byteCount: split2) {
				String[] split3 = byteCount.split("=");
				int count = Integer.parseInt(split3[0]);
				int value = Integer.parseInt(split3[1]);
				
				for (int i=0; i<count; ++i) {
					expectedBytes.add((byte)value);
				}
			}
			assertEquals("remaining for "+expectedContent, expectedBytes.size(), b.remaining());
			byte[] data2 = new byte[expectedBytes.size()];
			for (int i=0; i<data2.length; ++i) {
				data2[i] = expectedBytes.get(i);
			}
			assertEquals("content for "+expectedContent, Arrays.toString(data2), Arrays.toString(Arrays.copyOfRange(b.array(), b.position(), b.position()+b.remaining())));
		}
		if (compact) {
			session.compactOutBuffers(0);
		}
	}

	private void assertOutBuffers(StreamSession session, String expectedContent) {
		assertOutBuffers(session, expectedContent, true);
	}

	private void write(StreamSession session, int size, int value, boolean buffer, int padding, boolean nofuture) {
		if (buffer) {
			if (padding > 0) {
				byte[] d = getBytes(size+padding,value);
				Arrays.fill(d, size, size+padding, (byte)-1);
				if (nofuture) {
					session.writenf(ByteBuffer.wrap(d), size);
				}
				else {
					session.write(ByteBuffer.wrap(d), size);
				}
			}
			else {
				if (nofuture) {
					session.writenf(ByteBuffer.wrap(getBytes(size,value)));
				}
				else {
					session.write(ByteBuffer.wrap(getBytes(size,value)));
				}
			}
		}
		else {
			if (padding > 0) {
				byte[] d = getBytes(size+padding,value);
				int off = 1;
				
				d[0] = (byte)-1;
				Arrays.fill(d, size+1, size+padding, (byte)-1);
				if (nofuture) {
					session.writenf(d, off, size);
				}
				else {
					session.write(d, off, size);
				}
			}
			else {
				if (nofuture) {
					session.writenf(getBytes(size,value));
				}
				else {
					session.write(getBytes(size,value));
				}
			}
		}
	}

	@Test
	public void testWriteWithArray() throws Exception {
		testWrite(false, 0, true);
		testWrite(false, 10, true);
		testWrite(false, 0, false);
		testWrite(false, 10, false);
	}

	@Test
	public void testWriteWithBuffer() throws Exception {
		testWrite(true, 0, true);
		testWrite(true, 10, true);
		testWrite(true, 0, false);
		testWrite(true, 10, false);
	}

	
	private void testWrite(boolean buffer, int padding, boolean nofuture) throws Exception {
		SelectionKey key1 = registerServerSocketChannel(Selector.open(), PORT);
		SelectionKey key2 = registerSocketChannel(key1.selector(), PORT);
		StreamSession s = new StreamSession(handler);
		s.preCreated();
		
		try {
			write(s, 10,0, buffer, padding, nofuture);
			fail("exception should be thrown");
		}
		catch (IllegalSessionStateException e) {
			assertEquals(SessionState.OPENING, e.getIllegalState());
		}
		assertOutBuffers(s, "1024:0=0");
		
		s.setChannel((SocketChannel) key2.channel());
		s.setSelectionKey(key2);
		
		key2.interestOps(SelectionKey.OP_READ);
		write(s, 0, 0, buffer, padding, nofuture);
		assertEquals(SelectionKey.OP_READ, key2.interestOps());
		assertOutBuffers(s, "1024:0=0");
		
		key2.interestOps(SelectionKey.OP_READ);
		write(s, 1, 2, buffer, padding, nofuture);
		assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, key2.interestOps());
		assertOutBuffers(s, "1024:1=2");

		key2.interestOps(SelectionKey.OP_READ);
		write(s, 1023, 3, buffer, padding, nofuture);
		assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, key2.interestOps());
		assertOutBuffers(s, "1024:1=2,1023=3");

		key2.interestOps(SelectionKey.OP_READ);
		write(s, 0, 0, buffer, padding, nofuture);
		assertEquals(SelectionKey.OP_READ, key2.interestOps());
		assertOutBuffers(s, "1024:1=2,1023=3");
		
		key2.interestOps(SelectionKey.OP_READ);
		write(s, 1, 4, buffer, padding, nofuture);
		assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, key2.interestOps());
		assertOutBuffers(s, "1024:1=2,1023=3;1024:1=4");
		
		key2.interestOps(SelectionKey.OP_READ);
		write(s, 1023, 5, buffer, padding, nofuture);
		assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, key2.interestOps());
		assertOutBuffers(s, "1024:1=2,1023=3;1024:1=4,1023=5");
		
		key2.interestOps(SelectionKey.OP_READ);
		write(s, 1000,6, buffer, padding, nofuture);
		assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, key2.interestOps());
		assertOutBuffers(s, "1024:1=2,1023=3;1024:1=4,1023=5;1024:1000=6");
		
		key2.interestOps(SelectionKey.OP_READ);
		write(s, 1049,7, buffer, padding, nofuture);
		assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, key2.interestOps());
		assertOutBuffers(s, "1024:1=2,1023=3;1024:1=4,1023=5;1024:1000=6,24=7;1025:1025=7");
		
		ByteBuffer[] buffers = s.getOutBuffers();
		buffers[0].get();
		s.compactOutBuffers(0);
		assertOutBuffers(s, "1024:1023=3;1024:1=4,1023=5;1024:1000=6,24=7;1025:1025=7");
		
		key2.interestOps(SelectionKey.OP_READ);
		write(s, 1, 8, buffer, padding, nofuture);
		assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, key2.interestOps());
		assertOutBuffers(s, "1024:1023=3;1024:1=4,1023=5;1024:1000=6,24=7;1025:1025=7;1024:1=8");
		
		s.closing = ClosingState.SENDING;
		write(s, 1, 9, buffer, padding, nofuture);
		assertOutBuffers(s, "1024:1023=3;1024:1=4,1023=5;1024:1000=6,24=7;1025:1025=7;1024:1=8");
		s.closing = ClosingState.FINISHING;
		write(s, 1, 9, buffer, padding, nofuture);
		assertOutBuffers(s, "1024:1023=3;1024:1=4,1023=5;1024:1000=6,24=7;1025:1025=7;1024:1=8");
		s.closing = ClosingState.FINISHED;
		write(s, 1, 9, buffer, padding, nofuture);
		assertOutBuffers(s, "1024:1023=3;1024:1=4,1023=5;1024:1000=6,24=7;1025:1025=7;1024:1=8");
		s.closing = ClosingState.NONE;
		
		key1.channel().close();
		key2.channel().close();
		key1.selector().close();

		try {
			write(s, 20, 0, buffer, padding, nofuture);
			fail("exception should be thrown");
		}
		catch (IllegalSessionStateException e) {
			assertEquals(SessionState.CLOSING, e.getIllegalState());
		}
		assertOutBuffers(s, "1024:1023=3;1024:1=4,1023=5;1024:1000=6,24=7;1025:1025=7;1024:1=8");
		
	}
	
	private void assertBufferGet( ByteBuffer buffer, String expectedContent) {
		byte[] expected = getBytes(expectedContent);
		byte[] data = new byte[expected.length];
		
		buffer.get(data);
		assertEquals(Arrays.toString(expected), Arrays.toString(data));
	}

	private boolean contains(List<ByteBuffer> buffers, ByteBuffer buffer) {
		for (ByteBuffer b: buffers) {
			if (b == buffer) {
				return true;
			}
		}
		return false;
	}
	
	private void assertReleasedBuffers(int expectedSize, List<ByteBuffer> buffers, ByteBuffer[] oldBuffers) {
		assertEquals(expectedSize, buffers.size());
		if (!buffers.isEmpty()) {
			for (ByteBuffer b: buffers) {
				assertFalse(b.hasRemaining());
			}
			
			int i=0;
			for (; i<expectedSize; ++i) {
				assertTrue(contains(buffers, oldBuffers[i]));
			}

			for (; i<oldBuffers.length; ++i) {
				assertFalse(contains(buffers, oldBuffers[i]));
			}
			
			buffers.clear();
		}
	}
	
	@Test
	public void testCompactOutBuffers() throws Exception {
		SelectionKey key1 = registerServerSocketChannel(Selector.open(), PORT);
		SelectionKey key2 = registerSocketChannel(key1.selector(), PORT);
		StreamSession s = new StreamSession(handler);
		s.preCreated();
		
		s.setChannel((SocketChannel) key2.channel());
		s.setSelectionKey(key2);
		
		//empty buffer
		ByteBuffer[] bufs = s.getOutBuffers();
		assertTrue(s.compactOutBuffers(0));
		assertOutBuffers(s, "1024:0=0");
		assertReleasedBuffers(0, handler.getReleasedBuffers(), bufs);
		
		//get 1 from 1
		s.write(getBytes(1,1));
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1=1");
		assertTrue(s.compactOutBuffers(0));
		assertOutBuffers(s, "1024:0=0");
		assertReleasedBuffers(0, handler.getReleasedBuffers(), bufs);
		
		//get 1 from 2
		s.write(getBytes("1=2,1=3"));
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1=2");
		assertFalse(s.compactOutBuffers(0));
		assertOutBuffers(s, "1024:1=3");
		assertReleasedBuffers(0, handler.getReleasedBuffers(), bufs);

		//get 1023 from 1024
		s.write(getBytes(1023,4));
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1=3,1022=4");
		assertFalse(s.compactOutBuffers(0));
		assertOutBuffers(s, "1024:1=4");
		assertReleasedBuffers(0, handler.getReleasedBuffers(), bufs);

		//get 1024 from 1024
		s.write(getBytes(1023,5));
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1=4,1023=5");
		assertTrue(s.compactOutBuffers(0));
		assertOutBuffers(s, "1024:0=0");
		assertReleasedBuffers(0, handler.getReleasedBuffers(), bufs);
		
		//get 1 from 1024;1
		s.write(getBytes("1=6,1=7,1023=8"));
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1=6");
		assertFalse(s.compactOutBuffers(0));
		assertOutBuffers(s, "1024:1=7,1022=8;1024:1=8");
		assertReleasedBuffers(0, handler.getReleasedBuffers(), bufs);
		
		//get 1023 from 1023;1
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1=7,1022=8");
		assertFalse(s.compactOutBuffers(0));
		assertOutBuffers(s, "1024:1=8");
		assertReleasedBuffers(1, handler.getReleasedBuffers(), bufs);
		
		//get 1024 from 1024;1025
		s.write(getBytes("2048=9"));
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1=8,1023=9");
		assertFalse(s.compactOutBuffers(0));
		assertOutBuffers(s, "1025:1025=9");
		assertReleasedBuffers(1, handler.getReleasedBuffers(), bufs);
		
		//get 1025 from 1025
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1025=9");
		assertTrue(s.compactOutBuffers(0));
		assertOutBuffers(s, "1024:0=0");
		assertReleasedBuffers(0, handler.getReleasedBuffers(), bufs);
		
		//get 1024 from 1025;1
		s.write(getBytes("2049=1"));
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1024=1");
		assertFalse(s.compactOutBuffers(0));
		assertOutBuffers(s, "1025:1025=1");
		assertReleasedBuffers(1, handler.getReleasedBuffers(), bufs);
		s.write(getBytes("1=2"));
		assertOutBuffers(s, "1025:1025=1;1024:1=2");
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1024=1");
		assertFalse(s.compactOutBuffers(0));
		assertOutBuffers(s, "1025:1=1;1024:1=2");
		assertReleasedBuffers(0, handler.getReleasedBuffers(), bufs);
		
		//get 1 from 1/1025;1
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1=1");
		s.compactOutBuffers(0);
		assertOutBuffers(s, "1024:1=2");
		assertReleasedBuffers(1, handler.getReleasedBuffers(), bufs);
		
		//get 1025;1026;1 from 1025;1026;2
		s.write(getBytes("2048=3"));
		s.write(getBytes("1026=4"));
		s.write(getBytes("2=5"));
		assertOutBuffers(s, "1024:1=2,1023=3;1025:1025=3;1026:1026=4;1024:2=5");
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1=2,1023=3");
		assertBufferGet(bufs[1], "1025=3");
		assertBufferGet(bufs[2], "1026=4");
		assertBufferGet(bufs[3], "1=5");
		assertFalse(s.compactOutBuffers(0));
		assertOutBuffers(s, "1024:1=5");
		assertReleasedBuffers(3, handler.getReleasedBuffers(), bufs);
		
		key1.channel().close();
		key2.channel().close();
		key1.selector().close();
	}
	
	@Test
	public void testAttributes() throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);
		
		s.start();
		c.start();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertNotNull(c.getSession().getAttributes());
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);

		ConcurrentHashMap<Object, Object> attributes = new ConcurrentHashMap<Object, Object>();
		c = new Client(PORT);
		c.attributes = attributes;
		c.start();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertTrue(c.getSession().getAttributes() == attributes);
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
	}
	
	@Test
	public void testSetWriteInterestOps() throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);
		
		s.start();
		c.start();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		
		StreamSession session = c.getSession();
		SelectionKey key = session.key;
		
		assertEquals(SelectionKey.OP_READ, key.interestOps());
		session.setWriteInterestOps(key);
		assertEquals(SelectionKey.OP_READ|SelectionKey.OP_WRITE, key.interestOps());
		session.setWriteInterestOps(key);
		assertEquals(SelectionKey.OP_READ|SelectionKey.OP_WRITE, key.interestOps());
		session.clearWriteInterestOps(key);
		assertEquals(SelectionKey.OP_READ, key.interestOps());
		session.clearWriteInterestOps(key);
		assertEquals(SelectionKey.OP_READ, key.interestOps());
		assertFalse(session.isWriteSuspended());
		
		session.suspendWrite();
		assertTrue(session.isWriteSuspended());
		session.setWriteInterestOps(key);
		assertEquals(SelectionKey.OP_READ, key.interestOps());
		session.resumeWrite();
		assertFalse(session.isWriteSuspended());
		session.setWriteInterestOps(key);
		assertEquals(SelectionKey.OP_READ|SelectionKey.OP_WRITE, key.interestOps());
		session.suspendWrite();
		session.clearWriteInterestOps(key);
		assertEquals(SelectionKey.OP_READ, key.interestOps());
		session.resumeWrite();
		session.clearWriteInterestOps(key);
		assertEquals(SelectionKey.OP_READ, key.interestOps());
	}
	
	private void assertOutOfBoundException(StreamSession session, byte[] data, int off, int len) {
		try {
			session.write(data, off, len);
			fail("Exception not thrown");
		}
		catch (IndexOutOfBoundsException e) {}
		try {
			session.writenf(data, off, len);
			fail("Exception not thrown");
		}
		catch (IndexOutOfBoundsException e) {}
	}

	private void assertIllegalStateException(StreamSession session, byte[] data, int off, int len) {
		try {
			session.write(data, off, len);
			fail("Exception not thrown");
		}
		catch (IllegalStateException e) {}
		try {
			session.writenf(data, off, len);
			fail("Exception not thrown");
		}
		catch (IllegalStateException e) {}
	}
	
	@Test
	public void testWriteArguments() throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);

		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		
		StreamSession session = c.getSession();
		session.write(new Packet(PacketType.ECHO, "1234").toBytes());
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(1234)|", c.getRecordedData(true));
		session.writenf(new Packet(PacketType.ECHO, "12").toBytes());
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(12)|", c.getRecordedData(true));
		
		byte[] data = new Packet(PacketType.ECHO, "567").toBytes(0, 4);
		session.write(data, 0, data.length-4);
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(567)|", c.getRecordedData(true));
		data = new Packet(PacketType.ECHO, "67").toBytes(0, 4);
		session.writenf(data, 0, data.length-4);
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(67)|", c.getRecordedData(true));

		data = new Packet(PacketType.ECHO, "89").toBytes(3, 0);
		session.write(data, 3, data.length-3);
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(89)|", c.getRecordedData(true));
		data = new Packet(PacketType.ECHO, "891").toBytes(3, 0);
		session.writenf(data, 3, data.length-3);
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(891)|", c.getRecordedData(true));

		data = new Packet(PacketType.ECHO, "0").toBytes(7, 10);
		session.write(data, 7, data.length-17);
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(0)|", c.getRecordedData(true));
		data = new Packet(PacketType.ECHO, "01").toBytes(7, 10);
		session.writenf(data, 7, data.length-17);
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(01)|", c.getRecordedData(true));
		
		data = new Packet(PacketType.ECHO, "0").toBytes();
		session.write((Object)data).sync(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(0)|", c.getRecordedData(true));
		session.writenf((Object)data);
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(0)|", c.getRecordedData(true));
		
		session.write((Object)ByteBuffer.wrap(data)).sync(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(0)|", c.getRecordedData(true));
		session.writenf((Object)ByteBuffer.wrap(data));
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(0)|", c.getRecordedData(true));
		
		
		session.closing = ClosingState.SENDING;
		assertFalse(session.write(new byte[3], 0, 1).isSuccessful());
		assertFalse(session.write(new byte[3]).isSuccessful());
		assertFalse(session.write(getBuffer(10,0)).isSuccessful());
		assertFalse(session.write(getBuffer(10,0), 5).isSuccessful());
		session.closing = ClosingState.NONE;

		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		try {
			session.write((byte[])null);
			fail("Exception not thrown");
		}
		catch (NullPointerException e) {}
		try {
			session.write((byte[])null, 0, 0);
			fail("Exception not thrown");
		}
		catch (NullPointerException e) {}
		try {
			session.write((ByteBuffer)null);
			fail("Exception not thrown");
		}
		catch (NullPointerException e) {}
		try {
			session.write((ByteBuffer)null, 0);
			fail("Exception not thrown");
		}
		catch (NullPointerException e) {}
		try {
			session.write((Object)null);
			fail("Exception not thrown");
		}
		catch (NullPointerException e) {}
		try {
			session.writenf((byte[])null);
			fail("Exception not thrown");
		}
		catch (NullPointerException e) {}
		try {
			session.writenf((byte[])null, 0, 0);
			fail("Exception not thrown");
		}
		catch (NullPointerException e) {}
		try {
			session.writenf((ByteBuffer)null);
			fail("Exception not thrown");
		}
		catch (NullPointerException e) {}
		try {
			session.writenf((ByteBuffer)null, 0);
			fail("Exception not thrown");
		}
		catch (NullPointerException e) {}
		try {
			session.writenf((Object)null);
			fail("Exception not thrown");
		}
		catch (NullPointerException e) {}
		
		assertTrue(session.write(new byte[0]).isSuccessful());
		assertTrue(session.write(new byte[3], 0, 0).isSuccessful());
		assertTrue(session.write(getBuffer(0,0)).isSuccessful());
		assertTrue(session.write(getBuffer(10,0), 0).isSuccessful());
		assertTrue(session.write(new byte[3], 1, 0).isSuccessful());
		session.writenf(new byte[0]);
		session.writenf(new byte[3], 0, 0);
		session.writenf(getBuffer(0,0));
		session.writenf(getBuffer(10,0), 0);
		session.writenf(new byte[3], 1, 0);
		
		assertOutOfBoundException(session, new byte[10], -1, 4);
		assertOutOfBoundException(session, new byte[10], 10, 1);
		assertOutOfBoundException(session, new byte[10], 0, -1);
		assertOutOfBoundException(session, new byte[10], 5, 6);
		assertOutOfBoundException(session, new byte[10], 0x7fffffff, 1);
		try {
			session.write(getBuffer(0,90), 11);
			fail("Exception not thrown");
		}
		catch (IndexOutOfBoundsException e) {}
		try {
			session.writenf(getBuffer(0,90), 11);
			fail("Exception not thrown");
		}
		catch (IndexOutOfBoundsException e) {}
		try {
			session.write(getBuffer(0,90), -1);
			fail("Exception not thrown");
		}
		catch (IndexOutOfBoundsException e) {}
		try {
			session.writenf(getBuffer(0,90), -1);
			fail("Exception not thrown");
		}
		catch (IndexOutOfBoundsException e) {}
		
		assertIllegalStateException(session, new byte[10], 0, 10);
		assertIllegalStateException(session, new byte[10], 1, 9);
		assertIllegalStateException(session, new byte[10], 0, 1);
		
		try {
			session.write(new Integer(1)); fail();
		}
		catch (IllegalArgumentException e) {}
		try {
			session.writenf(new Integer(1)); fail();
		}
		catch (IllegalArgumentException e) {}
	}
	
	@Test
	public void testSuspendAndResume() throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);

		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		c.write(new Packet(PacketType.ECHO, "1"));
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(1)|", c.getRecordedData(true));
		s.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);


		//write suspend&resume
		StreamSession session = c.getSession();
		assertFalse(session.isWriteSuspended());
		session.suspendWrite();
		assertTrue(session.isWriteSuspended());
		c.write(new Packet(PacketType.ECHO, "2"));
		c.write(new Packet(PacketType.ECHO, "3"));
		waitFor(2000);
		assertEquals("", c.getRecordedData(true));
		assertFalse(session.suspend(SelectionKey.OP_WRITE));
		session.suspendWrite();
		session.resumeWrite();
		assertFalse(session.isWriteSuspended());
		waitFor(500);
		c.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(2)|ECHO_RESPONSE(3)|", c.getRecordedData(true));
		s.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertFalse(session.resume(SelectionKey.OP_WRITE));
		session.resumeWrite();


		//read suspend&resume
		s.getRecordedData(true);
		assertFalse(session.isReadSuspended());
		session.suspendRead();
		assertTrue(session.isReadSuspended());
		c.write(new Packet(PacketType.ECHO, "4"));
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|", c.getRecordedData(true));
		s.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|ECHO(4)|DS|", s.getRecordedData(true));
		waitFor(2000);
		assertFalse(session.suspend(SelectionKey.OP_READ));
		session.suspendRead();
		assertEquals("", c.getRecordedData(true));
		session.resumeRead();
		assertFalse(session.isReadSuspended());
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|ECHO_RESPONSE(4)|", c.getRecordedData(true));
		assertFalse(session.resume(SelectionKey.OP_READ));
		session.resumeRead();
		assertFalse(session.isReadSuspended());

		//when key is invalid
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertResumeSuspendException(session, RSType.SUSPEND_WRITE, SessionState.CLOSING);
		assertFalse(session.isWriteSuspended());
		assertResumeSuspendException(session, RSType.RESUME_WRITE, SessionState.CLOSING);
		assertResumeSuspendException(session, RSType.SUSPEND_READ, SessionState.CLOSING);
		assertFalse(session.isReadSuspended());
		assertResumeSuspendException(session, RSType.RESUME_READ, SessionState.CLOSING);
		assertFalse(session.suspend(SelectionKey.OP_READ));
		assertFalse(session.resume(SelectionKey.OP_READ));

		//when key is null
		session = new StreamSession(handler);
		assertResumeSuspendException(session, RSType.SUSPEND_WRITE, SessionState.OPENING);
		assertFalse(session.isWriteSuspended());
		assertResumeSuspendException(session, RSType.RESUME_WRITE, SessionState.OPENING);
		assertResumeSuspendException(session, RSType.SUSPEND_READ, SessionState.OPENING);
		assertFalse(session.isReadSuspended());
		assertResumeSuspendException(session, RSType.RESUME_READ, SessionState.OPENING);

		c = null;
	}

	public static enum RSType {SUSPEND_READ, SUSPEND_WRITE, RESUME_READ, RESUME_WRITE};
	public static void assertResumeSuspendException(ISession session, RSType type, SessionState illState) {
		try {
			switch (type) {
			case SUSPEND_READ:
				session.suspendRead();
				break;

			case SUSPEND_WRITE:
				session.suspendWrite();
				break;

			case RESUME_READ:
				session.resumeRead();
				break;

			case RESUME_WRITE:
				session.resumeWrite();
				break;
			}
			fail("no exception");
		} catch (IllegalSessionStateException e) {
			assertEquals(illState, e.getIllegalState());
		}
	}
	
	@Test
	public void testStatistics() throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);
		
		long t0 = System.currentTimeMillis();
		s.start();
		c.start();
		
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		long t1 = System.currentTimeMillis();
		
		StreamSession cs = c.getSession();
		StreamSession ss = s.getSession();
		
		assertTrue(cs.getCreationTime() <= ss.getCreationTime());
		assertTrue(cs.getCreationTime() >= t0 && cs.getCreationTime() <= t1);
		assertEquals(cs.getCreationTime(), cs.getLastIoTime());
		assertEquals(cs.getCreationTime(), cs.getLastReadTime());
		assertEquals(cs.getCreationTime(), cs.getLastWriteTime());
		
		assertEquals(0, cs.getReadBytes());
		assertEquals(0, cs.getWrittenBytes());
		assertEquals(0, ss.getReadBytes());
		assertEquals(0, ss.getWrittenBytes());
		
		waitFor(10);
		t0 = System.currentTimeMillis();
		c.write(new Packet(PacketType.NOP, "1234"));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		t1 = System.currentTimeMillis();
		assertEquals("SCR|SOP|RDY|DS|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|DR|NOP(1234)|", s.getRecordedData(true));
		assertEquals(0, cs.getReadBytes());
		assertEquals(7, cs.getWrittenBytes());
		assertEquals(7, ss.getReadBytes());
		assertEquals(0, ss.getWrittenBytes());
		assertEquals(cs.getLastWriteTime(), cs.getLastIoTime());
		assertEquals(cs.getCreationTime(), cs.getLastReadTime());
		assertTrue(cs.getLastWriteTime() >= t0 && cs.getLastWriteTime() <= t1);
		assertEquals(ss.getLastReadTime(), ss.getLastIoTime());
		assertEquals(ss.getCreationTime(), ss.getLastWriteTime());
		assertTrue(ss.getLastReadTime() >= t0 && ss.getLastReadTime() <= t1);
		
		s.write(new Packet(PacketType.NOP, "12345"));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|", s.getRecordedData(true));
		assertEquals("DR|NOP(12345)|", c.getRecordedData(true));
		assertEquals(8, cs.getReadBytes());
		assertEquals(7, cs.getWrittenBytes());
		assertEquals(7, ss.getReadBytes());
		assertEquals(8, ss.getWrittenBytes());
		
		s.write(new Packet(PacketType.ECHO, ""));
		s.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals(11, cs.getReadBytes());
		assertEquals(10, cs.getWrittenBytes());
		assertEquals(10, ss.getReadBytes());
		assertEquals(11, ss.getWrittenBytes());
	}
	
	@Test
	public void testCalculateThroughput() throws Exception {
		s = new Server(PORT);
		s.throughputCalcInterval = 1000;
		c = new Client(PORT);
		c.throughputCalcInterval = 1000;
		
		s.start();
		c.start();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		StreamSession cs = c.getSession();
		StreamSession ss = s.getSession();

		long t0 = System.currentTimeMillis();
		c.write(new Packet(PacketType.NOP, new String(new byte[1000])));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		waitFor(1010 - (System.currentTimeMillis() - t0));
		c.write(new Packet(PacketType.ECHO));
		s.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		
		double vCR = cs.getReadBytesThroughput();
		double vSR = ss.getReadBytesThroughput();
		double vCW = cs.getWrittenBytesThroughput();
		double vSW = ss.getWrittenBytesThroughput();
		
		assertTrue(vCR < 0.00000001);
		assertTrue(vSW < 0.00000001);
		assertTrue(Double.toString(vSR), vSR > 950.0 && vSR < 1010.0);
		assertTrue(Double.toString(vCW), vCW > 950.0 && vCW < 1010.0);
	}

	@Test
	public void testDisabledCalculateThroughput() throws Exception {
		s = new Server(PORT);
		s.throughputCalcInterval = 0;
		c = new Client(PORT);
		c.throughputCalcInterval = 0;
		
		s.start();
		c.start();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		StreamSession cs = c.getSession();
		StreamSession ss = s.getSession();

		c.write(new Packet(PacketType.NOP, new String(new byte[1000])));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		waitFor(2000);
		c.write(new Packet(PacketType.ECHO));
		s.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		
		double vCR = cs.getReadBytesThroughput();
		double vSR = ss.getReadBytesThroughput();
		double vCW = cs.getWrittenBytesThroughput();
		double vSW = ss.getWrittenBytesThroughput();
		
		assertTrue(vCR < 0.00000001);
		assertTrue(vSW < 0.00000001);
		assertTrue(vSR < 0.00000001);
		assertTrue(vCW < 0.00000001);
	}
	
	@Test
	public void testClose() throws Exception {

		StreamSession session = new StreamSession(handler);

		//close when key == null
		session.close();
		session.quickClose();
		
		//close inside the loop
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.CLOSE));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|CLOSE()|SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
		
		//close outside the loop
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c.getSession().close();
		waitFor(500);
		assertEquals("", c.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);

		//close inside the loop with data to send
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.WRITE_AND_CLOSE));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|DR|WRITE_AND_CLOSE_RESPONSE()|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|WRITE_AND_CLOSE()|DS|SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
		
		//close outside the loop with data to send (closed on other side)
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.WRITE_AND_WAIT, "1000"));
		waitFor(500);
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|DR|WRITE_AND_WAIT_RESPONSE(1000)|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|WRITE_AND_WAIT(1000)|DS|SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);

		//close outside the loop with data to send (closed on the same side)
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.WRITE_AND_WAIT, "1000"));
		waitFor(500);
		s.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|DR|WRITE_AND_WAIT_RESPONSE(1000)|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|WRITE_AND_WAIT(1000)|DS|SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
		
		//quick close inside the loop
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.QUICK_CLOSE));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|QUICK_CLOSE()|SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);

		//quick close outside the loop
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.getSession().quickClose();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c.getSession().quickClose();
		assertEquals("", c.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);

		//quick close inside the loop with data to send
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.WRITE_AND_QUICK_CLOSE));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|WRITE_AND_QUICK_CLOSE()|SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
		
		//quick close outside the loop with data to send (closed on other side)
		s = new Server(PORT); s.start();
		s.dontReplaceException = true;
		c = new Client(PORT); c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.WRITE_AND_WAIT, "1000"));
		waitFor(500);
		c.getSession().quickClose();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		String r = s.getRecordedData(true);
		if (!r.equals("DR|WRITE_AND_WAIT(1000)|DS|SCL|SEN|")) {
			assertEquals("DR|WRITE_AND_WAIT(1000)|DS|EXC|SCL|SEN|", r);
		}
		s.stop(TIMEOUT); c.stop(TIMEOUT);

		//quick close outside the loop with data to send (closed on the same side)
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.WRITE_AND_WAIT, "1000"));
		waitFor(500);
		s.getSession().quickClose();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|WRITE_AND_WAIT(1000)|SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
	}

	private void testCloseOutsideSuspendWrite(boolean suspendS, boolean suspendC, boolean writeS, boolean writeC, boolean quickClose) throws Exception {
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		if (suspendS) s.getSession().suspendWrite();
		if (suspendC) c.getSession().suspendWrite();
		if (writeS) s.write(new Packet(PacketType.NOP));
		if (writeC) c.write(new Packet(PacketType.NOP));
		waitFor(100);
		if (quickClose) {
			c.getSession().quickClose();
		}
		else {
			c.getSession().close();
		}
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
	}

	private void testCloseInsideSuspendWrite(boolean suspendS, boolean suspendC, boolean writeS, boolean writeC, boolean quickClose) throws Exception {
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		StringBuilder sb = new StringBuilder();
		if (suspendS) s.getSession().suspendWrite();
		if (suspendC) sb.append("S");
		if (writeS) s.write(new Packet(PacketType.NOP));
		if (writeC) sb.append("W");
		if (quickClose) {
			if (sb.length() == 0) {
				c.write(new Packet(PacketType.QUICK_CLOSE));
			}
			else {
				sb.append("Q");
				c.write(new Packet(PacketType.SUSPEND_WRITE_CLOSE, sb.toString()));
			}
		}
		else {
			if (sb.length() == 0) {
				c.write(new Packet(PacketType.CLOSE));
			}
			else {
				c.write(new Packet(PacketType.SUSPEND_WRITE_CLOSE, sb.toString()));
			}
		}
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		if (sb.length() == 0) {
			assertEquals("DR|CLOSE|SCL|SEN|".replace("CLOSE", quickClose ? "QUICK_CLOSE()" : "CLOSE()"), s.getRecordedData(true));
		}
		else {
			assertEquals("DR|SUSPEND_WRITE_CLOSE($)|SCL|SEN|".replace("$", sb.toString()), s.getRecordedData(true));
		}
		s.stop(TIMEOUT); c.stop(TIMEOUT);
	}
	
	@Test
	public void testCloseWhenSuspendedWrite() throws Exception {
		//closed outside the loop (suspended on the same side)
		testCloseOutsideSuspendWrite(false, true, false, false, false);
		testCloseOutsideSuspendWrite(false, true, false, false, true);

		//closed outside the loop (suspended on the same side)
		testCloseOutsideSuspendWrite(true, false, false, false, false);
		testCloseOutsideSuspendWrite(true, false, false, false, true);

		//closed outside the loop (suspended on both sides)
		testCloseOutsideSuspendWrite(true, true, false, false, false);
		testCloseOutsideSuspendWrite(true, true, false, false, true);

		//closed with written data outside the loop (suspended on the same side)
		testCloseOutsideSuspendWrite(false, true, false, true, false);
		testCloseOutsideSuspendWrite(false, true, false, true, true);

		//closed with written data outside the loop (suspended on the same side)
		testCloseOutsideSuspendWrite(true, false, true, false, false);
		testCloseOutsideSuspendWrite(true, false, true, false, true);

		//closed with written data outside the loop (suspended on both sides)
		testCloseOutsideSuspendWrite(true, true, true, true, false);
		testCloseOutsideSuspendWrite(true, true, true, true, true);

		testCloseInsideSuspendWrite(false, true, false, false, false);
		testCloseInsideSuspendWrite(false, true, false, false, true);
		testCloseInsideSuspendWrite(true, false, false, false, false);
		testCloseInsideSuspendWrite(true, false, false, false, true);
		testCloseInsideSuspendWrite(true, true, false, false, false);
		testCloseInsideSuspendWrite(true, true, false, false, true);
		testCloseInsideSuspendWrite(false, true, false, true, false);
		testCloseInsideSuspendWrite(false, true, false, true, true);
		testCloseInsideSuspendWrite(true, false, true, false, false);
		testCloseInsideSuspendWrite(true, false, true, false, true);
		testCloseInsideSuspendWrite(true, true, true, true, false);
		testCloseInsideSuspendWrite(true, true, true, true, true);
	}

	private void testCloseOutsideSuspendRead(boolean suspendS, boolean suspendC, boolean quickClose) throws Exception {
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		if (suspendS) s.getSession().suspendRead();
		if (suspendC) c.getSession().suspendRead();
		if (quickClose) {
			c.getSession().quickClose();
		}
		else {
			c.getSession().close();
		}
		
		if (suspendS) {
			if (quickClose) {
				c.waitForSessionEnding(TIMEOUT);
				assertEquals("SCL|SEN|", c.getRecordedData(true));
				waitFor(2000);
				assertEquals("", s.getRecordedData(true));
			}
			else {
				waitFor(2000);
				assertEquals("", c.getRecordedData(true));
				assertEquals("", s.getRecordedData(true));
			}
			s.getSession().resumeRead();
		}

		if (!suspendS || !quickClose) {
			c.waitForSessionEnding(TIMEOUT);
			assertEquals("SCL|SEN|", c.getRecordedData(true));
		}
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
	}
	
	@Test
	public void testCloseWhenSuspendedRead() throws Exception {
		testCloseOutsideSuspendRead(false, true, false);
		testCloseOutsideSuspendRead(true, false, false);
		testCloseOutsideSuspendRead(true, true, false);
		testCloseOutsideSuspendRead(false, true, true);
		testCloseOutsideSuspendRead(true, false, true);
		testCloseOutsideSuspendRead(true, true, true);
		
		//close with written data (data length==0 but OP_WRITE set)
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		StreamSession session = c.getSession();
		session.suspendRead();
		session.key.interestOps(session.key.interestOps() | SelectionKey.OP_WRITE);
		session.close();
		session.loop.selector.wakeup(); //need to wake up as we set OP_WRITE in illegal way
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT); s.stop(TIMEOUT);

		//close with written data
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		session = c.getSession();
		session.suspendRead();
		session.suspendWrite();
		c.write(new Packet(PacketType.NOP));
		session.key.interestOps(session.key.interestOps() | SelectionKey.OP_WRITE);
		session.close();
		session.loop.selector.wakeup(); //need to wake up as we set OP_WRITE in illegal way
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|NOP()|SCL|SEN|", s.getRecordedData(true));
	}
	
	@Test
	public void testCloseOtherScenarios() throws Exception {
		
		//quick close after blocked gentle close (server closed by resume)
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		s.getSession().suspendRead();
		c.getSession().close();
		waitFor(2000);
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		c.getSession().quickClose();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		s.getSession().resumeRead();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT); s.stop(TIMEOUT);
		
		//quick close after blocked gentle close (server closed by close)
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		s.getSession().suspendRead();
		c.getSession().close();
		waitFor(2000);
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		c.getSession().quickClose();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		s.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT); s.stop(TIMEOUT);
	
		//quick close after blocked gentle close (server closed by quick close)
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		s.getSession().suspendRead();
		c.getSession().close();
		waitFor(2000);
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		c.getSession().quickClose();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		s.getSession().quickClose();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT); s.stop(TIMEOUT);
		
		//gentle close with skipped sending
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		
		final AtomicBoolean lock = new AtomicBoolean();
		
		c.loop.execute(new Runnable() {

			@Override
			public void run() {
				LockUtils.notify(lock);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
		});
		LockUtils.waitFor(lock, TIMEOUT);
		c.session.write(new Packet(PacketType.NOP).toBytes());
		c.session.close(false, false);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT); s.stop(TIMEOUT);
	}  
	
	@Test
	public void testGetAddress() throws Exception {
		StreamSession session = new StreamSession(handler);
		
		assertNull(session.getLocalAddress());
		assertNull(session.getRemoteAddress());
		
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		
		assertNotNull(c.getSession().getLocalAddress());
		assertNotNull(s.getSession().getLocalAddress());
		
		assertEquals("/127.0.0.1:" + PORT, c.getSession().getRemoteAddress().toString());
		assertFalse(("/127.0.0.1:" + PORT).equals(c.getSession().getLocalAddress().toString()));
		
		assertEquals(c.getSession().getLocalAddress().toString(), s.getSession().getRemoteAddress().toString());
		assertEquals(s.getSession().getLocalAddress().toString(), c.getSession().getRemoteAddress().toString());
		
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		waitFor(2000);
		
		assertNull(c.getSession().getRemoteAddress());
		assertNull(s.getSession().getRemoteAddress());
		assertNull(c.getSession().getLocalAddress());
		assertNull(s.getSession().getLocalAddress());
	}
	
	@Test
	public void testDirectAllocator() throws Exception {
		s = new Server(PORT);
		s.directAllocator = true;
		c = new Client(PORT);
		
		//write one packet
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.ECHO));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE()|", c.getRecordedData(true));
		assertEquals("DR|ECHO()|DS|", s.getRecordedData(true));
		
		//write three packets at once
		c.getSession().suspendWrite();
		c.write(new Packet(PacketType.ECHO));
		c.write(new Packet(PacketType.ECHO));
		c.write(new Packet(PacketType.ECHO));
		c.getSession().resumeWrite();
		waitFor(500);
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE()|ECHO_RESPONSE()|ECHO_RESPONSE()|", c.getRecordedData(true));
		assertEquals("DR|ECHO()|ECHO()|ECHO()|DS|", s.getRecordedData(true));
		
		//partial read of second packet
		c.getSession().suspendWrite();
		c.write(new Packet(PacketType.ECHO));
		byte[] d = new Packet(PacketType.ECHO, "1234567890").toBytes();
		byte[] d1 = Arrays.copyOf(d, 4);
		byte[] d2 = Arrays.copyOfRange(d, 4, d.length);
		c.getSession().write(d1);
		c.getSession().resumeWrite();
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE()|", c.getRecordedData(true));
		assertEquals("DR|ECHO()|DS|", s.getRecordedData(true));
		c.getSession().write(d2);
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(1234567890)|", c.getRecordedData(true));
		assertEquals("DR|ECHO(1234567890)|DS|", s.getRecordedData(true));

		//partial read of first packet
		c.getSession().write(d1);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataReceived(TIMEOUT);
		waitFor(2000);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|", s.getRecordedData(true));
		c.getSession().write(d2);
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(1234567890)|", c.getRecordedData(true));
		assertEquals("DR|ECHO(1234567890)|DS|", s.getRecordedData(true));
		
	}
	
	private EventType getEventType(String s) {
		Iterator<EventType> i = Server.eventMapping.keySet().iterator();
		
		while(i.hasNext()) {
			EventType t = i.next();
			
			if (s.equals(Server.eventMapping.get(t))) {
				return t;
			}
		}
		throw new IllegalStateException();
	}
	
	private void fireEvent(StreamSession s, EventType type) {
		switch (type) {
		case DATA_RECEIVED:
			s.event(DataEvent.RECEIVED, 0);
			break;
			
		case DATA_SENT:
			s.event(DataEvent.SENT, 0);
			break;
			
		case EXCEPTION_CAUGHT:
			s.exception(new Exception());
			break;
			
		case SESSION_CLOSED:
			s.event(SessionEvent.CLOSED);
			break;
			
		case SESSION_CREATED:
			s.event(SessionEvent.CREATED);
			break;
			
		case SESSION_ENDING:
			s.event(SessionEvent.ENDING);
			break;
			
		case SESSION_OPENED:
			s.event(SessionEvent.OPENED);
			break;

		case SESSION_READY:
			s.event(SessionEvent.READY);
			break;
			
		default:
			break;
		
		}
	}
	
	private StreamSession getSession(String init) {
		TestHandler h = new TestHandler("");
		StreamSession s = new StreamSession(h);
		h.events = new StringBuilder();

		String[] splitted = init.split("\\|");
		
		for (int i=0; i<splitted.length; ++i) {
			if (splitted[i].isEmpty()) continue;
			
			EventType t = getEventType(splitted[i]);

			s.closeCalled.set(false);
			fireEvent(s, t);
			if (t == EventType.SESSION_OPENED) {
				assertEquals(t.toString()+"|"+EventType.SESSION_READY+"|", h.getEvents());
			}
			else {
				assertEquals(t.toString()+"|", h.getEvents());
			}
		}
		return s;
	}
	
	private void assertEvents(String init, String allowed, String ignored) {
		String[] asplitted = allowed.split("\\|");
		String[] isplitted = ignored.split("\\|");

		EventType[] disallowed = EventType.values().clone();
		
		for (int i=0; i<asplitted.length; ++i) {
			if (asplitted[i].isEmpty()) continue;

			EventType t = getEventType(asplitted[i]);
			disallowed[t.ordinal()] = null;
		}
		
		for (int i=0; i<isplitted.length; ++i) {
			if (isplitted[i].isEmpty()) continue;

			EventType t = getEventType(isplitted[i]);
			disallowed[t.ordinal()] = null;
		}
		
		for (int i=0; i<asplitted.length; ++i) {
			if (asplitted[i].isEmpty()) continue;

			StreamSession s = getSession(init);
			EventType t = getEventType(asplitted[i]);
			
			fireEvent(s, t);
			assertEquals(t.toString()+"|", ((TestHandler)s.handler).getEvents());
			
			for (int j=0; j<disallowed.length; ++j) {
				t = disallowed[j];
				if (t != null) {
					fireEvent(s, t);
					assertEquals("", ((TestHandler)s.handler).getEvents());
				}
			}
		}
	}
	
	@Test
	public void testEvent() {
		assertEvents("", "", "");
		assertEvents("SCR", "SEN|EXC", "SOP");
		assertEvents("SCR|EXC", "SEN|EXC", "SOP");
		assertEvents("SCR|SEN", "", "");
		assertEvents("SCR|EXC|SEN", "", "");

		assertEvents("SCR|SOP", "EXC|DS|DR|", "SCL|SEN");
		assertEvents("SCR|SOP|DS", "EXC|DS|DR", "SCL|SEN");
		assertEvents("SCR|SOP|DR", "EXC|DS|DR", "SCL|SEN");
		assertEvents("SCR|SOP|EXC", "EXC|DS|DR", "SCL|SEN");
		assertEvents("SCR|SOP|EXC|DR|DS", "EXC|DS|DR", "SCL|SEN");
		assertEvents("SCR|EXC|SOP", "EXC|DS|DR", "SCL|SEN");
		assertEvents("SCR|EXC|SOP|DS", "EXC|DS|DR", "SCL|SEN");
		assertEvents("SCR|EXC|SOP|DR", "EXC|DS|DR", "SCL|SEN");
		assertEvents("SCR|EXC|SOP|EXC", "EXC|DS|DR", "SCL|SEN");
		assertEvents("SCR|EXC|SOP|EXC|DR|DS", "EXC|DS|DR", "SCL|SEN");
		
		assertEvents("SCR|SOP|SCL", "SEN|EXC", "");
		assertEvents("SCR|EXC|SOP|SCL", "SEN|EXC", "");
		assertEvents("SCR|EXC|SOP|EXC|DS|DR|SCL", "SEN|EXC", "");
		
		assertEvents("SCR|SOP|SCL|SEN", "", "");
		assertEvents("SCR|EXC|SOP|SCL|SEN", "", "");
		assertEvents("SCR|EXC|SOP|EXC|DS|DR|SCL|EXC|SEN", "", "");
	}
	
	@Test
	public void testState() throws Exception {
		StreamSession session = new StreamSession(handler);
		
		assertEquals(SessionState.OPENING, session.getState());

		s = new Server(PORT);
		c = new Client(PORT);
		s.start();
		c.start();
		
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals(SessionState.OPEN, c.getSession().getState());
		assertEquals(SessionState.OPEN, s.getSession().getState());
		
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals(SessionState.CLOSING, c.getSession().getState());
		assertEquals(SessionState.CLOSING, s.getSession().getState());
		s.stop(TIMEOUT);
		c.stop(TIMEOUT);
	}
	
	@Test
	public void testDetectRebuild() throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);
		s.start();
		c.start();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		
		StreamSession session = c.getSession();
		SelectionKey key = session.key;
		
		assertTrue(key == session.detectRebuild(key));
		assertTrue(key == session.detectRebuild(s.getSession().key));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		try {
			session.detectRebuild(s.getSession().key);
			fail("detecting rebuild should fail");
		}
		catch (IllegalSessionStateException e) {
		}
	}
	
	@Test
	public void testEventHandlerWithException() throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);
		c.throwInEvent = true;
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		c.write(new Packet(PacketType.ECHO));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE()|", c.getRecordedData(true));
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		waitFor(100);
		assertEquals(7, c.throwInEventCount.get());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		
		c = new Client(PORT);
		c.throwInException = true;
		c.start();
		c.waitForSessionEnding(TIMEOUT*2);
		assertEquals("SCR|EXC|SEN|", c.getRecordedData(true));
		assertEquals(1,c.throwInExceptionCount.get());
		c.stop(TIMEOUT);
	}
	
	@Test
	public void testReadHandlerWithException() throws Exception {
		s = new Server(PORT);
		s.throwInRead = true;
		c = new Client(PORT);
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.ECHO));
		waitFor(500);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|ECHO()|EXC|SCL|SEN|", s.getRecordedData(true));
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		Exception e = new Exception("Ex2");
		s = new Server(PORT);
		s.throwInRead = true;
		s.throwIn = new CloseControllingException("Ex1", ICloseControllingException.CloseType.NONE, e);
		s.exceptionRecordException = true;
		c = new Client(PORT);
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.ECHO));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE()|", c.getRecordedData(true));
		assertEquals("DR|ECHO()|EXC|(Ex2)|DS|", s.getRecordedData(true));
		assertFalse(s.session.getCloseFuture().isDone());
		s.throwIn = new CloseControllingException("Ex1", ICloseControllingException.CloseType.GENTLE, e);
		c.write(new Packet(PacketType.ECHO));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertTrue(s.session.getReadyFuture().isSuccessful());
		assertTrue(s.session.getCloseFuture().isSuccessful());
		assertTrue(s.session.getEndFuture().isSuccessful());
		assertEquals("DS|DR|ECHO_RESPONSE()|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|ECHO()|EXC|(Ex2)|DS|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	
		s = new Server(PORT);
		s.throwInRead = true;
		s.throwIn = new CloseControllingException("Ex1", ICloseControllingException.CloseType.DEFAULT, e);
		s.exceptionRecordException = true;
		c = new Client(PORT);
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.ECHO));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|ECHO()|EXC|(Ex2)|SCL|SEN|", s.getRecordedData(true));	
	}
	
	@Test
	public void testReleaseOfAllocatedBuffers() throws Exception {
		TestHandler handler = new TestHandler("Test");
		TestAllocator allocator = handler.allocator;
		
		assertEquals(0, allocator.getSize());
		StreamSession session = new StreamSession(handler);
		assertEquals(0, allocator.getSize());
		try {
			session.write(new byte[10]);
			fail("Exception not thrown");
		}
		catch (IllegalSessionStateException e) {}
		assertEquals(0, allocator.getSize());
		assertEquals(0, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		
		allocator = new TestAllocator(false, true);
		s = new Server(PORT);
		c = new Client(PORT);
		c.allocator = allocator;
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals(2, allocator.getSize());
		assertEquals(2, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals(0, allocator.getSize());
		assertEquals(2, allocator.getAllocatedCount());
		assertEquals(2, allocator.getReleasedCount());

		allocator = new TestAllocator(false, true);
		c = new Client(PORT);
		c.allocator = allocator;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		c.getSession().suspendWrite();
		byte[] data = new byte[2000];
		Arrays.fill(data, (byte)'A');
		c.getSession().write(new Packet(PacketType.NOP, new String(data)).toBytes());
		assertEquals(3, allocator.getSize());
		assertEquals(3, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		c.getSession().resumeWrite();
		c.waitForDataSent(TIMEOUT);
		assertEquals(2, allocator.getSize());
		assertEquals(3, allocator.getAllocatedCount());
		assertEquals(1, allocator.getReleasedCount());
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals(0, allocator.getSize());
		assertEquals(3, allocator.getAllocatedCount());
		assertEquals(3, allocator.getReleasedCount());

		allocator = new TestAllocator(false, false);
		c = new Client(PORT);
		c.allocator = allocator;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		c.getSession().suspendWrite();
		data = new byte[2000];
		Arrays.fill(data, (byte)'A');
		c.getSession().write(new Packet(PacketType.NOP, new String(data)).toBytes());
		assertEquals(3, allocator.getSize());
		assertEquals(3, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		c.getSession().resumeWrite();
		c.waitForDataSent(TIMEOUT);
		assertEquals(3, allocator.getSize());
		assertEquals(3, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals(3, allocator.getSize());
		assertEquals(3, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		
		s.stop(TIMEOUT);
	}
	
	@Test
	public void testExceptionInHandleWriting() throws Exception {
		s = new Server(PORT);
		s.useTestSession = true;
		c = new Client(PORT);
		c.useTestSession = true;
		
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		((TestStreamSession)c.getSession()).getOutBuffersException = true;
		c.write(new Packet(PacketType.NOP));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("EXC|SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
	}

	@Test
	public void testExceptionInHandleReadding() throws Exception {
		s = new Server(PORT);
		s.useTestSession = true;
		c = new Client(PORT);
		c.useTestSession = true;
		c.dontReplaceException = true;
		
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		waitFor(100);
		
		((TestStreamSession)s.getSession()).getInBufferException = true;
		c.write(new Packet(PacketType.NOP));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		String recordedData = c.getRecordedData(true);
		if (recordedData.indexOf("EXC") == -1) {
			assertEquals("DS|SCL|SEN|", recordedData);
		}
		else {
			assertEquals("DS|EXC|SCL|SEN|", recordedData);
		}
		assertEquals("EXC|SCL|SEN|", s.getRecordedData(true));
	}
	
	private SocketChannel connect() throws IOException {
		SocketChannel channel = SocketChannel.open();
		channel.configureBlocking(true);
		channel.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT)); 
		return channel;
	}
	
	Object getFromIntSession(InternalSession session, String name) throws Exception {
		Field f = InternalSession.class.getDeclaredField(name);
		
		f.setAccessible(true);
		return f.get(session);
	}

	Object getFromSession(StreamSession session, String name) throws Exception {
		Field f = StreamSession.class.getDeclaredField(name);
		
		f.setAccessible(true);
		return f.get(session);
	}
	
	@Test
	public void testGentleCloseWithDifferentPeerInteraction() throws Exception {
		s = new Server(PORT);
		s.start();
		SocketChannel channel;

		//peer only shutdownOutput
		channel = connect();
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		channel.shutdownOutput();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		channel.close();
		
		//peer only shutdownOutput, host with data to sent but write suspended
 	    channel = connect();
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		s.getSession().suspendWrite();
		s.getSession().write(new byte[2]);
		channel.shutdownOutput();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		channel.close();

		//peer only shutdownOutput, host with data to sent
 	    channel = connect();
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		
		Object lock = getFromIntSession(s.getSession(), "writeLock");
		SelectionKey key = (SelectionKey) getFromIntSession(s.getSession(), "key");
		ByteBuffer[] bufs = (ByteBuffer[]) getFromSession((StreamSession)s.getSession(), "outBuffers");
		
		synchronized (lock) {
			bufs[0].put(new byte[2]);
			key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
			channel.shutdownOutput();
		}
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", s.getRecordedData(true));
		channel.close();
		
		//host gently closes but peer delays shutdown
 	    channel = connect();
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		s.getSession().close();
		waitFor(1000);
		assertEquals("", s.getRecordedData(true));
		channel.shutdownOutput();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		channel.close();

		//host gently closes but peer doesn't shutdown
 	    channel = connect();
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		s.getSession().close();
		waitFor(TIMEOUT);
		assertEquals("", s.getRecordedData(true));
		s.getSession().quickClose();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		channel.close();

		//host gently closes but peer doesn't shutdown, stop loop
 	    channel = connect();
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		s.getSession().close();
		s.loop.stop();
		assertFalse(s.loop.join(TIMEOUT));
		s.loop.quickStop();
		assertTrue(s.loop.join(TIMEOUT));
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		channel.close();
		
	}	
	
	public static int countDS(String s) {
		int off = 0;
		while (s.startsWith("DS|", off)) {
			off += 3;
		}
		return off/3;
	}
	
	public static int countRDNOP(String s, byte[] payload) {
		int off = 0;
		String rdnop = "DR|NOP(" + new String(payload) + ")|";
		int i;
		int count = 0;
		
		while ((i = s.indexOf(rdnop, off)) != -1) {
			off = i + rdnop.length();
			count++;
		}
		return count;
	}
	
	@Test
	public void testWriteSpinCount() throws Exception {
		s = new Server(PORT);
		s.start();
		
		StreamSession session;
		byte[] payload = new byte[2000];
		Arrays.fill(payload, (byte)'1');
		byte[] data = new Packet(PacketType.NOP, new String(payload)).toBytes();
		int writeCount = 2000;
		boolean unix = "true".equalsIgnoreCase(System.getenv("SNF4J_UNIX_TEST")) || true;
		int maxTries = unix ? 10 : 1;
		StringBuilder sb = new StringBuilder();
		
		//On unix we are trying 10 times as it is difficult to predict the size
		//of data consumed by single execution of channel's write method.
		for (int t=0; t<maxTries; ++t) {
			c = new Client(PORT);
			c.start();
			c.waitForSessionReady(TIMEOUT);
			s.waitForSessionReady(TIMEOUT);
			assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
			assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));

			session = c.getSession();
			session.suspendWrite();
			for (int i=0; i<writeCount; i++) {
				session.write(data);
			}
			session.write(new Packet(PacketType.CLOSE).toBytes());
			session.resumeWrite();
			s.waitForSessionEnding(TIMEOUT);
			c.waitForSessionEnding(TIMEOUT);
			String text = c.getRecordedData(true);
			assertEquals(writeCount, countRDNOP(s.getRecordedData(true), payload));
			int count = countDS(text);
			assertEquals("SCL|SEN|", text.substring(count*3));
			c.stop(TIMEOUT);

			c = new Client(PORT);
			c.maxWriteSpinCount = 1;
			c.start();
			c.waitForSessionReady(TIMEOUT);
			s.waitForSessionReady(TIMEOUT);
			c.getRecordedData(true);
			s.getRecordedData(true);

			session = c.getSession();
			session.suspendWrite();
			for (int i=0; i<writeCount; i++) {
				session.write(data);
			}
			session.write(new Packet(PacketType.CLOSE).toBytes());
			session.resumeWrite();
			s.waitForSessionEnding(TIMEOUT);
			c.waitForSessionEnding(TIMEOUT);
			text = c.getRecordedData(true);
			assertEquals(writeCount, countRDNOP(s.getRecordedData(true), payload));
			int count2 = countDS(text);
			assertEquals("SCL|SEN|", text.substring(count2*3));
			
			boolean countsOk;
			if (unix) {
				countsOk = count2 > count;
			}
			else {
				countsOk = count2 > count*4;
			}
			c.stop(TIMEOUT);
			
			if (countsOk) {
				sb.setLength(0);
				break;
			}
			else {
				sb.append(count2);
				sb.append(">");
				sb.append(count);
				sb.append("; ");
			}
		}
		if (sb.length() > 0) {
			fail("count2 > count: " + sb.toString());
		}
		
		c = new Client(PORT);
		c.useTestSession = true;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
		
		session = c.getSession();
		((TestStreamSession)session).getOutBuffersException = true;
		session = c.getSession();
		session.suspendWrite();
		for (int i=0; i<writeCount; i++) {
			session.write(data);
		}
		session.write(new Packet(PacketType.CLOSE).toBytes());
		session.resumeWrite();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("EXC|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		
		c = new Client(PORT);
		c.useTestSession = true;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
		session = c.getSession();
		((TestStreamSession)session).getOutBuffersException = true;
		((TestStreamSession)session).getOutBuffersExceptionDelay = 1;
		session = c.getSession();
		session.suspendWrite();
		for (int i=0; i<writeCount; i++) {
			session.write(data);
		}
		session.write(new Packet(PacketType.CLOSE).toBytes());
		session.resumeWrite();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|EXC|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		
		c = new Client(PORT);
		c.maxWriteSpinCount = 1;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
		
		session = c.getSession();
		session.suspendWrite();
		session.write(new Packet(PacketType.NOP, "123456").toBytes());
		TestSelectionKey key = new TestSelectionKey(new TestSocketChannel());
		Method m = SelectorLoop.class.getDeclaredMethod("handleWriting", StreamSession.class, SelectionKey.class, int.class);
		m.setAccessible(true);
		m.invoke(c.loop, session, key, 1);
		session.write(new Packet(PacketType.CLOSE).toBytes());
		session.resumeWrite();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|NOP(123456)|CLOSE()|SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
	
		TestHandler h = new TestHandler("") {
			@Override
			public ISessionConfig getConfig() {
				return new DefaultSessionConfig().setMaxWriteSpinCount(0);
			}			
		};
		try {
			new StreamSession(h);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		
		h = new TestHandler("") {
			@Override
			public ISessionConfig getConfig() {
				return new DefaultSessionConfig().setMaxWriteSpinCount(-1);
			}			
		};		
		try {
			new StreamSession(h);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testDataEventDetails() throws Exception {
		s = new Server(PORT);
		s.recordDataEventDetails = true;
		c = new Client(PORT);
		c.recordDataEventDetails = true;
		
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		
		Packet p = new Packet(PacketType.NOP);
		int pLen = p.toBytes().length;
		
		c.getSession().write(p.toBytes());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		
		assertEquals("DS|"+pLen+"|", c.getRecordedData(true));
		assertEquals("DR|"+pLen+"|NOP()|", s.getRecordedData(true));
		
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));

	}

	final AtomicBoolean expired = new AtomicBoolean(false);

	static class Task implements Runnable {
		String name;
		
		Task(String name) {
			this.name = name;
		}

		@Override
		public void run() {
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
	
	@Test
	public void testTimerExpirationAfterStopping() throws Exception {
		DefaultTimer timer = new DefaultTimer();
		s = new Server(PORT);
		s.timer = timer;
		c = new Client(PORT);
		
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		
		Runnable expiredTask = new Runnable() {

			@Override
			public void run() {
				expired.set(true);		
			}
		};
		
		ITimerTask t = s.getSession().getTimer().scheduleTask(expiredTask, 1000, true);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		waitFor(1100);
		assertFalse(((TimerTask)t).cancel());
		assertFalse(expired.get());
		
		timer.schedule(expiredTask, 10);
		waitFor(50);
		assertTrue(expired.get());
	}
	
	@Test
	public void testTimer() throws Exception {
		DefaultTimer timer = new DefaultTimer();
		s = new Server(PORT);
		s.timer = timer;
		c = new Client(PORT);
	
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
		
		ISessionTimer stimer = c.getSession().getTimer();
		assertTrue(stimer == UnsupportedSessionTimer.INSTANCE);
		assertFalse(stimer.isSupported());
		try {
			stimer.scheduleEvent("1", 10);
			fail();
		}
		catch (UnsupportedOperationException e) {}
		try {
			stimer.scheduleTask((Runnable)null, 10, false);
			fail();
		}
		catch (UnsupportedOperationException e) {}
		try {
			stimer.scheduleEvent("1", 10, 10);
			fail();
		}
		catch (UnsupportedOperationException e) {}
		try {
			stimer.scheduleTask((Runnable)null, 10, 10, false);
			fail();
		}
		catch (UnsupportedOperationException e) {}
		
		
		stimer = s.getSession().getTimer();
		assertTrue(stimer instanceof InternalSessionTimer);
		assertTrue(stimer.isSupported());
		
		stimer.scheduleEvent("t1", 100);
		waitFor(80);
		assertEquals("", s.getRecordedData(true));
		waitFor(40);
		assertEquals("TIM;t1|", s.getRecordedData(true));
		stimer.scheduleEvent("t2", 10).cancelTask();
		waitFor(20);
		assertEquals("", s.getRecordedData(true));
		ITimerTask task = stimer.scheduleEvent("t3", 10, 10);
		waitFor(8);
		assertEquals("", s.getRecordedData(true));
		waitFor(4);
		assertEquals("TIM;t3|", s.getRecordedData(true));
		waitFor(25);
		assertEquals("TIM;t3|TIM;t3|", s.getRecordedData(true));
		task.cancelTask();
		waitFor(100);
		assertEquals("", s.getRecordedData(true));

		stimer.scheduleTask(new Task("task1"), 100, true);
		waitFor(80);
		assertEquals("", s.getRecordedData(true));
		waitFor(25);
		assertEquals("TIM;task1|", s.getRecordedData(true));
		stimer.scheduleTask(new Task("task2"), 10, true).cancelTask();
		waitFor(20);
		assertEquals("", s.getRecordedData(true));
		task = stimer.scheduleTask(new Task("task3"), 10, 10, true);
		waitFor(8);
		assertEquals("", s.getRecordedData(true));
		waitFor(4);
		assertEquals("TIM;task3|", s.getRecordedData(true));
		waitFor(25);
		assertEquals("TIM;task3|TIM;task3|", s.getRecordedData(true));
		task.cancelTask();
		waitFor(100);
		assertEquals("", s.getRecordedData(true));
		
		Runnable expiredTask = new Runnable() {

			@Override
			public void run() {
				expired.set(true);		
			}
		};
		
		expired.set(false);
		stimer.scheduleTask(expiredTask, 100, false);
		waitFor(80);
		assertFalse(expired.get());
		waitFor(25);
		assertTrue(expired.get());
		assertEquals("", s.getRecordedData(true));
		expired.set(false);
		stimer.scheduleTask(expiredTask, 10, false).cancelTask();
		waitFor(20);
		assertFalse(expired.get());
		task = stimer.scheduleTask(expiredTask, 10, 10, false);
		waitFor(8);
		assertFalse(expired.get());
		waitFor(4);
		assertTrue(expired.get());
		expired.set(false);
		waitFor(10);
		assertTrue(expired.get());
		task.cancelTask();
		expired.set(false);
		waitFor(115);
		assertFalse(expired.get());
		
		s.throwInTimer = true;
		stimer.scheduleEvent("e1", 10);
		waitFor(50);
		assertEquals(1, s.throwInTimerCount.get());
		assertEquals("TIM;e1|", s.getRecordedData(true));
		stimer.scheduleTask(new Task("t1"), 10, true);
		waitFor(50);
		assertEquals(2, s.throwInTimerCount.get());
		assertEquals("TIM;t1|", s.getRecordedData(true));
		
		c.getSession().write(new Packet(PacketType.NOP, "123").toBytes());
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(123)|", s.getRecordedData(true));
		
		timer.cancel();
		
		stimer = new InternalSessionTimer(c.getSession(), null);
		assertFalse(stimer.isSupported());
	}
	
	private void testCloseInSessionCreatedEvent(StoppingType type) throws Exception{
		s = new Server(PORT);
		c = new Client(PORT);
		c.closeInEvent = EventType.SESSION_CREATED;
		c.closeType = type;
		s.start();
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|SCL|SEN|", s.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, c.getSession().closing);
		s.stop(TIMEOUT);

		s = new Server(PORT);
		c = new Client(PORT);
		s.closeInEvent = EventType.SESSION_CREATED;
		s.closeType = type;
		s.start();
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|RDY|SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SEN|", s.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, s.getSession().closing);
		s.stop(TIMEOUT);

		s = new Server(PORT);
		s.closeInEvent = EventType.SESSION_CREATED;
		s.closeType = type;
		TestSelectorPool pool = new TestSelectorPool();
		s.start();
		s.getSelectLoop().setPool(pool);
		pool.getException = true;

		c = new Client(PORT);
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|RDY|SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCR|EXC|SEN|", s.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, s.getSession().closing);
		s.stop(TIMEOUT);
	}
	
	@Test
	public void testCloseInSessionCreatedEvent() throws Exception{
		testCloseInSessionCreatedEvent(StoppingType.GENTLE);
		testCloseInSessionCreatedEvent(StoppingType.QUICK);
		testCloseInSessionCreatedEvent(StoppingType.DIRTY);
	}

	private void testCloseInSessionOpenedEvent(StoppingType type)
			throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);
		c.closeInEvent = EventType.SESSION_OPENED;
		c.closeType = type;
		s.start();
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|SCL|SEN|", s.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, c.getSession().closing);
		s.stop(TIMEOUT);

		s = new Server(PORT);
		c = new Client(PORT);
		s.closeInEvent = EventType.SESSION_OPENED;
		s.closeType = type;
		s.start();
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|RDY|SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SOP|SCL|SEN|", s.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, s.getSession().closing);
		s.stop(TIMEOUT);

		s = new Server(PORT);
		c = new Client(PORT);
		s.closeInEvent = EventType.SESSION_OPENED;
		s.closeType = type;
		s.start();
		s.getSelectLoop().setPool(new DefaultSelectorLoopPool(2));
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|RDY|SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SOP|SCL|SEN|", s.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, s.getSession().closing);
		s.stop(TIMEOUT);
	}
	
	@Test
	public void testCloseInSessionOpenedEvent() throws Exception {
		testCloseInSessionOpenedEvent(StoppingType.GENTLE);
		testCloseInSessionOpenedEvent(StoppingType.QUICK);
		testCloseInSessionOpenedEvent(StoppingType.DIRTY);
	}

	private void testCloseInSessionClosedOrEndingEvent(StoppingType type, EventType event) throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);
		c.closeInEvent = event;
		c.closeType = type;
		s.start();
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		c.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|RDY|SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|SCL|SEN|", s.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, c.getSession().closing);
		s.stop(TIMEOUT);
		c.stop(TIMEOUT);
		
		s = new Server(PORT);
		c = new Client(PORT);
		c.closeInEvent = event;
		c.closeType = type;
		s.start();
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		s.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|RDY|SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|SCL|SEN|", s.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, c.getSession().closing);
		s.stop(TIMEOUT);
		c.stop(TIMEOUT);
		
	}
	
	@Test
	public void testCloseInSessionClosedEvent() throws Exception {
		testCloseInSessionClosedOrEndingEvent(StoppingType.GENTLE, EventType.SESSION_CLOSED);
		testCloseInSessionClosedOrEndingEvent(StoppingType.QUICK, EventType.SESSION_CLOSED);
		testCloseInSessionClosedOrEndingEvent(StoppingType.DIRTY, EventType.SESSION_CLOSED);
	}
	
	@Test
	public void testCloseInSessionEndingEvent() throws Exception {
		testCloseInSessionClosedOrEndingEvent(StoppingType.GENTLE, EventType.SESSION_ENDING);
		testCloseInSessionClosedOrEndingEvent(StoppingType.QUICK, EventType.SESSION_ENDING);
		testCloseInSessionClosedOrEndingEvent(StoppingType.DIRTY, EventType.SESSION_ENDING);
	}
	
	@Test
	public void testCompressionCodec() throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);
		
		DefaultCodecExecutor codec = new DefaultCodecExecutor();
		
		codec.getPipeline().add("DECODER", new ZlibDecoder());
		codec.getPipeline().add("PACKET", new PacketDecoder());
		codec.getPipeline().add("ENCODER", new ZlibEncoder());
		s.codecPipeline = codec;
		
		codec = new DefaultCodecExecutor();
		codec.getPipeline().add("DECODER", new ZlibDecoder());
		codec.getPipeline().add("PACKET", new PacketDecoder());
		codec.getPipeline().add("ENCODER", new ZlibEncoder());
		c.codecPipeline = codec;
		
		s.start();
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		
		byte[] big = new byte[10000];
		Arrays.fill(big, (byte)'1');
		String bigStr = new String(big);
		
		s.getRecordedData(true);
		c.getRecordedData(true);
		c.getSession().write(new Packet(PacketType.NOP, bigStr).toBytes()).sync();
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DR|M(NOP["+bigStr+"])|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		s.getSession().write(new Packet(PacketType.NOP, bigStr).toBytes()).sync();
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|M(NOP["+bigStr+"])|", c.getRecordedData(true));
		assertEquals("DS|", s.getRecordedData(true));
		assertTrue(c.getSession().getWrittenBytes() < 100);
		assertTrue(s.getSession().getWrittenBytes() < 100);
		assertTrue(c.getSession().getReadBytes() < 100);
		assertTrue(s.getSession().getReadBytes() < 100);
		
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertTrue(((ZlibDecoder)c.codecPipeline.getPipeline().get("DECODER")).isFinished());
		assertTrue(((ZlibEncoder)c.codecPipeline.getPipeline().get("ENCODER")).isFinished());
		assertTrue(((ZlibDecoder)s.codecPipeline.getPipeline().get("DECODER")).isFinished());
		assertTrue(((ZlibEncoder)s.codecPipeline.getPipeline().get("ENCODER")).isFinished());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		s = new Server(PORT);
		c = new Client(PORT);
		
		codec = new DefaultCodecExecutor();
		codec.getPipeline().add("DECODER", new GzipDecoder());
		codec.getPipeline().add("PACKET", new PacketDecoder());
		codec.getPipeline().add("ENCODER", new GzipEncoder());
		s.codecPipeline = codec;
		
		codec = new DefaultCodecExecutor();
		codec.getPipeline().add("DECODER", new GzipDecoder());
		codec.getPipeline().add("PACKET", new PacketDecoder());
		codec.getPipeline().add("ENCODER", new GzipEncoder());
		c.codecPipeline = codec;
		
		s.start();
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);

		s.getRecordedData(true);
		c.getRecordedData(true);
		c.getSession().write(new Packet(PacketType.NOP, bigStr).toBytes()).sync();
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DR|M(NOP["+bigStr+"])|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		s.getSession().write(new Packet(PacketType.NOP, bigStr).toBytes()).sync();
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|M(NOP["+bigStr+"])|", c.getRecordedData(true));
		assertEquals("DS|", s.getRecordedData(true));
		assertTrue(c.getSession().getWrittenBytes() < 100);
		assertTrue(s.getSession().getWrittenBytes() < 100);
		assertTrue(c.getSession().getReadBytes() < 100);
		assertTrue(s.getSession().getReadBytes() < 100);
		
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertTrue(((GzipDecoder)c.codecPipeline.getPipeline().get("DECODER")).isFinished());
		assertTrue(((GzipEncoder)c.codecPipeline.getPipeline().get("ENCODER")).isFinished());
		assertTrue(((GzipDecoder)s.codecPipeline.getPipeline().get("DECODER")).isFinished());
		assertTrue(((GzipEncoder)s.codecPipeline.getPipeline().get("ENCODER")).isFinished());
	}
	
	@Test
	public void testAllocateAndRelease() throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);
		c.allocator = new TestAllocator(false, false);
		s.allocator = new TestAllocator(true, true);
		
		s.start();
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		
		ByteBuffer b = c.getSession().allocate(16);
		assertFalse(b.isDirect());
		assertEquals(16, b.capacity());
		assertEquals(0, c.allocator.getReleased().size());
		c.getSession().release(b);
		assertEquals(0, c.allocator.getReleased().size());
		
		b = s.getSession().allocate(32);
		assertTrue(b.isDirect());
		assertEquals(32, b.capacity());
		assertEquals(0, s.allocator.getReleased().size());
		s.getSession().release(b);
		assertEquals(1, s.allocator.getReleased().size());
		assertTrue(b == s.allocator.getReleased().get(0));
		
	}
	
	@Test
	public void testOptimizedDataCopyingRead() throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);
		s.allocator = new TestAllocator(false, true);
		s.optimizeDataCopying = true;
		s.ignoreAvailableException = true;
		
		s.start();
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		StreamSession session = s.getSession();
		assertNull(getInBuffer(session));
		assertEquals(0, getOutBuffers(session).length);
		
		ByteBuffer b = null;
		c.getSession().write(new Packet(PacketType.NOP).toBytes());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(50);
		assertEquals(1, s.allocator.getAllocatedCount());
		assertEquals(1, s.allocator.getSize());
		session.release(s.bufferRead);
		assertEquals(0, s.allocator.getSize());
		assertNull(getInBuffer(session));
		assertEquals(0, getOutBuffers(session).length);
		assertEquals("SCR|SOP|RDY|DS|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|DR|BUF|NOP()|", s.getRecordedData(true));
		
		c.getSession().write(new Packet(PacketType.NOP,"1").toBytes());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(50);
		assertEquals(2, s.allocator.getAllocatedCount());
		assertEquals(1, s.allocator.getSize());
		session.release(s.bufferRead);
		assertEquals(0, s.allocator.getSize());
		assertNull(getInBuffer(session));
		assertEquals(0, getOutBuffers(session).length);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|BUF|NOP(1)|", s.getRecordedData(true));
		
		int acount = s.allocator.getAllocatedCount();
		byte[] bytes = new Packet(PacketType.NOP,"10").toBytes();
		c.getSession().write(bytes, 0, 2);
		c.waitForDataSent(TIMEOUT);
		waitFor(50);
		assertEquals(acount+1, s.allocator.getAllocatedCount());
		assertEquals(1, s.allocator.getSize());
		b = getInBuffer(session);
		assertNotNull(b);
		assertEquals(2, b.position());
		assertEquals(0, getOutBuffers(session).length);
		c.getSession().write(bytes, 2, bytes.length-2);
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(50);
		assertEquals(acount+1, s.allocator.getAllocatedCount());
		assertNull(getInBuffer(session));
		assertEquals(0, getOutBuffers(session).length);
		assertEquals(1, s.allocator.getSize());
		s.allocator.release(s.bufferRead);
		assertEquals(0, s.allocator.getSize());
		assertEquals("DS|DS|", c.getRecordedData(true));
		assertEquals("DR|DR|BUF|NOP(10)|", s.getRecordedData(true));
		
		byte[] bytes2 = new byte[bytes.length*2];
		System.arraycopy(bytes, 0, bytes2, 0, bytes.length);
		System.arraycopy(bytes, 0, bytes2, bytes.length, bytes.length);
		bytes2[bytes2.length-1]++;
		c.getSession().write(bytes2);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		waitFor(50);
		assertEquals(acount+3, s.allocator.getAllocatedCount());
		assertNull(getInBuffer(session));
		assertEquals(0, getOutBuffers(session).length);
		assertEquals(2, s.allocator.getSize());
		s.allocator.release(s.bufferRead);
		assertEquals(1, s.allocator.getSize());
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|BUF|NOP(10)|BUF|NOP(11)|", s.getRecordedData(true));
		
		c.getSession().write(bytes2, 0, 6);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		waitFor(50);
		assertEquals(acount+5, s.allocator.getAllocatedCount());
		assertEquals(3, s.allocator.getSize());
		assertNotNull(getInBuffer(session));
		assertEquals(0, getOutBuffers(session).length);
		s.allocator.release(s.bufferRead);
		assertEquals(2, s.allocator.getSize());
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|BUF|NOP(10)|", s.getRecordedData(true));
		c.getSession().write(bytes2, 6, 4);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		waitFor(50);
		assertEquals(acount+5, s.allocator.getAllocatedCount());
		assertEquals(2, s.allocator.getSize());
		s.allocator.release(s.bufferRead);
		assertEquals(1, s.allocator.getSize());
		assertNull(getInBuffer(session));
		assertEquals(0, getOutBuffers(session).length);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|BUF|NOP(11)|", s.getRecordedData(true));
		assertTrue(b != session.getInBuffer());
		assertEquals(b.capacity(), session.getInBuffer().capacity());

		byte[] bytes3 = new byte[bytes.length*3];
		b = session.getInBuffer();
		System.arraycopy(bytes, 0, bytes3, 0, bytes.length);
		System.arraycopy(bytes, 0, bytes3, bytes.length, bytes.length);
		System.arraycopy(bytes, 0, bytes3, bytes.length*2, bytes.length);
		c.getSession().write(bytes3);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		waitFor(50);
		assertEquals(acount+8, s.allocator.getAllocatedCount());
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|BUF|NOP(10)|BUF|NOP(10)|BUF|NOP(10)|", s.getRecordedData(true));
		assertNull(getInBuffer(session));
		assertEquals(0, getOutBuffers(session).length);
		assertEquals(4, s.allocator.getSize());
		s.allocator.release(s.bufferRead);
		assertEquals(3, s.allocator.getSize());
		
		TestSelectionKey key = new TestSelectionKey(new TestSocketChannel());
		Method m = SelectorLoop.class.getDeclaredMethod("handleReading", StreamSession.class, SelectionKey.class);
		m.setAccessible(true);
		m.invoke(s.loop, session, key);
		assertEquals(3, s.allocator.getSize());
		assertNull(getInBuffer(session));
		assertEquals(acount+9, s.allocator.getAllocatedCount());
		bytes = new Packet(PacketType.NOP, "123455").toBytes();
		c.getSession().write(bytes, 0, 4);
		waitFor(50);
		assertEquals("DR|", s.getRecordedData(true));
		assertEquals(4, s.allocator.getSize());
		b = getInBuffer(session);
		assertNotNull(b);
		m.invoke(s.loop, session, key);
		assertTrue(b == getInBuffer(session));
		assertEquals(4, s.allocator.getSize());
		c.getSession().write(bytes, 4, bytes.length-4);
		waitFor(50);
		assertEquals("DR|BUF|NOP(123455)|", s.getRecordedData(true));
		
		session = c.getSession();
		b = getInBuffer(session);
		assertNotNull(b);
		m.invoke(c.loop, session, key);
		assertTrue(b == getInBuffer(session));
		
		
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		s = new Server(PORT);
		c = new Client(PORT);
		s.allocator = new TestAllocator(true, true);
		s.optimizeDataCopying = true;
		s.ignoreAvailableException = true;
		
		s.start();
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
		
		c.getSession().write(bytes2);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		waitFor(50);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|BUF|NOP(10)|BUF|NOP(11)|", s.getRecordedData(true));

	}
	
	@Test
	public void testOptimizedDataCopyingWrite() throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);
		c.allocator = new TestAllocator(false, true);
		c.optimizeDataCopying = true;
		
		s.start();
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		
		StreamSession session = c.getSession();
		assertNull(getInBuffer(session));
		assertEquals(0, getOutBuffers(session).length);

		ByteBuffer b = session.allocate(100);
		ByteBuffer b0 = null;
		b.put(new Packet(PacketType.NOP).toBytes());
		b.flip();
		session.write(b);
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals(1, c.allocator.getAllocatedCount());
		assertEquals(1, c.allocator.getReleasedCount());
		assertEquals(0, c.allocator.getSize());
		assertNull(getInBuffer(session));
		assertEquals(0, getOutBuffers(session).length);
		List<ByteBuffer> released = c.allocator.getReleased();
		assertTrue(b == released.get(0));
		assertEquals("SCR|SOP|RDY|DS|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		
		session.suspendWrite();
		session.write(new Packet(PacketType.NOP,"1").toBytes());
		waitFor(50);
		assertEquals(2, c.allocator.getAllocatedCount());
		assertEquals(1, c.allocator.getReleasedCount());
		assertTrue(c.allocator.getAllocated().get(1) == getOutBuffers(session)[0]);
		
		b0 = session.allocate(100);		
		b0.put(new Packet(PacketType.NOP).toBytes());
		b0.flip();
		session.write(b0);
		session.resumeWrite();
		waitFor(50);
		released = c.allocator.getReleased();
		assertEquals(3, released.size());
		assertTrue(b0 == released.get(2));
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|NOP(1)|NOP()|", s.getRecordedData(true));
		assertNull(getInBuffer(session));
		assertEquals(0, getOutBuffers(session).length);
		assertEquals(0, c.allocator.getSize());
		
		session.write(new Packet(PacketType.NOP,"2").toBytes());
		waitFor(50);
		assertEquals(4, released.size());
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|NOP(2)|", s.getRecordedData(true));
		assertNull(getInBuffer(session));
		assertEquals(0, getOutBuffers(session).length);
		assertEquals(0, c.allocator.getSize());

		b = session.allocate(100);		
		b.put(new Packet(PacketType.NOP,"3").toBytes());
		b.flip();
		session.write(b);
		waitFor(50);
		assertEquals(5, released.size());
		assertTrue(b == released.get(4));
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|NOP(3)|", s.getRecordedData(true));	
		assertNull(getInBuffer(session));
		assertEquals(0, getOutBuffers(session).length);
		assertEquals(0, c.allocator.getSize());
		
		b0 = session.allocate(100);
		b0.put(new Packet(PacketType.NOP,"4").toBytes());
		b0.put((byte)0);
		b0.flip();
		session.write(b0, b0.remaining()-1);
		waitFor(50);
		assertEquals(6, released.size());
		assertEquals(1, c.allocator.getSize());
		c.allocator.release(b0);
		assertEquals(0, c.allocator.getSize());
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|NOP(4)|", s.getRecordedData(true));	
		assertNull(getInBuffer(session));
		assertEquals(0, getOutBuffers(session).length);
		assertEquals(0, c.allocator.getSize());
		assertEquals(7, c.allocator.getAllocatedCount());
		
		byte[] bytes = new Packet(PacketType.NOP , "1234567890").toBytes();
		session.write(bytes, 0, 5).sync(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(50);
		assertEquals("DR|", s.getRecordedData(true));
		assertEquals(8, c.allocator.getAllocatedCount());
		assertEquals(0, getOutBuffers(session).length);
		assertEquals(0, c.allocator.getSize());
		session.write(bytes, 5, bytes.length-5).sync(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(50);
		assertEquals("DR|NOP(1234567890)|", s.getRecordedData(true));
		assertEquals(9, c.allocator.getAllocatedCount());
		assertNull(getInBuffer(session));
		assertEquals(0, getOutBuffers(session).length);
		assertEquals(0, c.allocator.getSize());		
		
		assertEquals(0, session.getOutBuffers().length);
		session.suspendWrite();
		session.write(bytes);
		assertEquals(1, c.allocator.getSize());
		ByteBuffer[] bb = getOutBuffers(session);
		assertEquals(1, bb.length);
		bb[0] = null;
		session.dirtyClose();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals(1, c.allocator.getSize());
		
	}
	
	@Test
	public void testUpdateOutBuffersForOptimization() {
		IByteBufferAllocator a = DefaultAllocator.DEFAULT;
		
		//remaining data exists but last empty
		ByteBuffer[] b = new ByteBuffer[] {ByteBuffer.wrap(new byte[10]), ByteBuffer.allocate(10)};
		b[1].flip();
		ByteBuffer[] b2 = StreamSession.compactBuffers(b, a, 100, true);
		assertEquals(2, b2.length);
		assertEquals(10, b2[0].remaining());
		assertEquals(0, b2[1].position());
		
		//no data exist
		b[0].clear().flip();
		b[1].clear().flip();
		b2 = StreamSession.compactBuffers(b, a, 100, true);
		assertEquals(0, b2.length);	
		b[0].clear().flip();
		b[1].clear().flip();
		b2 = StreamSession.compactBuffers(b, a, 5, false);
		assertEquals(1, b2.length);
		assertEquals(5, b2[0].capacity());
		
		//last buffer is empty
		b = new ByteBuffer[] {ByteBuffer.allocate(10)};
		ByteBuffer data = ByteBuffer.wrap(new byte[10]);
		b[0].flip();
		TestAllocator a2 = new TestAllocator(false, true);
		data.flip();
		b2 = StreamSession.putToBuffers(b, a2, data);
		assertEquals(1, a2.getReleasedCount());
		assertTrue(data == b2[0]);
		
		//last buffer is null
		b[0] = null;
		b2 = StreamSession.putToBuffers(b, a2, data);
		assertEquals(1, a2.getReleasedCount());
		assertTrue(data == b2[0]);
		
		
	}
	
	@Test
	public void testExecute() throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);
		final TestStreamSession tss = new TestStreamSession(new TestHandler("test"));
		c.initSession = tss;
		s.start();
		c.start();
		s.waitForSessionReady(TIMEOUT);
		waitFor(100);
		
		ExecuteTask task = new ExecuteTask(0);
		tss.execute(task).sync(TIMEOUT);
		assertNotNull(task.thread);
		assertFalse(task.thread == Thread.currentThread());
		assertTrue(task.thread.getName().startsWith("selector-loop-"));
		Thread thread = task.thread;
		
		task = new ExecuteTask(200);
		IFuture<Void> f = tss.execute(task);
		waitFor(100);
		assertFalse(f.isDone());
		f.sync(200);
		
		task = new ExecuteTask(200);
		tss.executenf(task);
		waitFor(100);
		assertNull(task.thread);
		waitFor(200);
		assertTrue(thread == task.thread);
		
		final ExecuteTask task2 = new ExecuteTask(200);
		final StringBuilder sb = new StringBuilder();
		c.loop.execute(new Runnable() {
			@Override
			public void run() {
				sb.append(tss.execute(task2));
			}
		});
		waitFor(100);
		assertNull(task2.thread);
		assertEquals("", sb.toString());
		waitFor(200);
		assertTrue(thread == task2.thread);
		assertTrue(sb.toString().endsWith("SuccessfulFuture[successful]"));
		
		final ExecuteTask task3 = new ExecuteTask(200);
		sb.setLength(0);
		c.loop.execute(new Runnable() {
			@Override
			public void run() {
				tss.executenf(task3);
			}
		});
		waitFor(100);
		assertNull(task3.thread);
		waitFor(200);
		assertTrue(thread == task3.thread);
	
		
		TestStreamSession tss2 = new TestStreamSession(new TestHandler("test"));		
		try {
			tss2.execute(task);
			fail();
		}
		catch (IllegalStateException e) {
		}
		try {
			tss2.executenf(task);
			fail();
		}
		catch (IllegalStateException e) {
		}
		try {
			tss2.execute(null);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		try {
			tss2.executenf(null);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		
	}
	
	static void assertBuffer(ByteBuffer b, int size, int capacity) {
		assertEquals(size, b.position());
		assertEquals(capacity, b.capacity());
		assertEquals(capacity, b.limit());
		b.flip();
		assertEquals(size, b.remaining());
		byte[] bytes = new byte[b.remaining()];
		b.get(bytes);
		assertArrayEquals(bytes(bytes.length), bytes);
	}
	
	static byte[] bytes(int size) {
		byte[] bytes = new byte[size];
		for (int i=0; i<size; ++i) {
			bytes[i] = (byte) i;
		}
		return bytes;
	}
	
	@Test
	public void testCopyInBuffer() throws Exception {
		Client c = new Client(PORT);
		c.minInBufferCapacity = 16;
		c.maxInBufferCapacity = 64;
		StreamSession s1 = new StreamSession(c.createHandler());
		c.minInBufferCapacity = 128;
		c.maxInBufferCapacity = 128;
		StreamSession s2 = new StreamSession(c.createHandler());
		
		assertEquals(0, s2.getInBuffersForCopying().length);
		ByteBuffer b1 = s1.getInBuffer();
		ByteBuffer b2 = s2.getInBuffer();
		assertEquals(0, s2.getInBuffersForCopying().length);
		
		assertEquals(16, b1.capacity());
		assertEquals(128, b2.capacity());
		assertEquals(0, s1.copyInBuffer(s2));
		assertBuffer(b1, 0, 16);
		b1.clear();
		b2.clear();
		b2.put(bytes(10));
		assertEquals(1, s2.getInBuffersForCopying().length);
		assertEquals(10, s1.copyInBuffer(s2));
		assertBuffer(b1, 10, 16);
		b1.clear();
		b2.clear();
		b2.put(bytes(16));
		assertEquals(16, s1.copyInBuffer(s2));
		assertBuffer(b1, 16, 16);
		b1.clear();
		b2.clear();
		b2.put(bytes(17));
		assertEquals(17, s1.copyInBuffer(s2));
		b1 = s1.getInBuffer();
		assertBuffer(b1, 17, 32);
		b1.clear();
		b2.clear();
		b2.put(bytes(64));
		assertEquals(64, s1.copyInBuffer(s2));
		b1 = getInBuffer(s1);
		assertBuffer(b1, 64, 64);
		
		c.minInBufferCapacity = 16;
		c.maxInBufferCapacity = 64;
		s1 = new StreamSession(c.createHandler());
		b1 = s1.getInBuffer();
		assertEquals(16, b1.capacity());
		b1.clear();
		b2.clear();
		b2.put(bytes(63));
		assertEquals(63, s1.copyInBuffer(s2));
		b1 = getInBuffer(s1);
		assertBuffer(b1, 63, 64);
		
		c.minInBufferCapacity = 16;
		c.maxInBufferCapacity = 64;
		s1 = new StreamSession(c.createHandler());
		b1 = s1.getInBuffer();
		assertEquals(16, b1.capacity());
		b1.clear();
		b2.clear();
		b2.put(bytes(100));
		assertEquals(100, s1.copyInBuffer(s2));
		b1 = getInBuffer(s1);
		assertBuffer(b1, 100, 100);
		
		TestInternalSession session = new TestInternalSession("1", new TestHandler(""), null);
		assertEquals(0, session.copyInBuffer(null));
		session.consumeInBuffer();
	}

	int countPipes(String s) {
		return s.length() - s.replace("|", "").length();
	}

	@Test
	public void testConnectByProxy() throws Exception {
		//successful connection
		p = new HttpProxy(PORT+1);
		p.start(TIMEOUT);
		s = new Server(PORT);
		s.start();
		c = new Client(PORT+1);
		c.addPreSession("C", false, new HttpProxyHandler(new URI("http://127.0.0.1:" + PORT)) {
			@Override
			public void event(DataEvent event, long length) {
				c.record(event.name() + "(" + length + ")");
			}
		});
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SENT(57)|RECEIVED(39)|SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		String w = "CONNECT 127.0.0.1:7777 HTTP/1.1|Host: 127.0.0.1:7777||";
		String r = "HTTP/1.1 200 Connection established||";
		assertEquals(w+r, p.getTrace());
		assertEquals(r.length() + countPipes(r), c.preSessions.get(0).getReadBytes());
		assertEquals(w.length() + countPipes(w), c.preSessions.get(0).getWrittenBytes());
		c.write(new Packet(PacketType.ECHO, "123"));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|ECHO(123)|DS|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE(123)|", c.getRecordedData(true));
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);

		//successful connection with appended packet
		p.appendPacket = new Packet(PacketType.NOP, "123");
		c = new Client(PORT+1);
		c.addPreSession("C", false, new HttpProxyHandler(new URI("http://127.0.0.1:" + PORT)));
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		waitFor(50);
		assertEquals("SCR|SOP|RDY|DR|NOP(123)|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		p.appendPacket = null;
		p.getTrace();
		
		TestTimer timer = new TestTimer();
		TestAllocator allocator = new TestAllocator(true,true);
		DefaultSessionStructureFactory factory = new DefaultSessionStructureFactory() {
			@Override
			public IByteBufferAllocator getAllocator() {
				return allocator;
			}
			
			@Override
			public ITimer getTimer() {
				return timer;
			}
		};
		
		//successful connection with buffer optimization
		c = new Client(PORT+1);
		c.optimizeDataCopying = true;
		c.allocator = new TestAllocator(true,true);
		c.addPreSession("C", false, new HttpProxyHandler(new URI("http://127.0.0.1:" + PORT), 200) {
			@Override
			public void event(DataEvent event, long length) {
				throw new RuntimeException();
			}
			
			@Override
			public ISessionStructureFactory getFactory() {
				return factory;
			}
		});
		((DefaultSessionConfig)c.preSessions.get(0).getConfig()).setOptimizeDataCopying(true);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		assertEquals(w+r, p.getTrace());
		waitFor(300);
		assertEquals("", c.getRecordedData(true));
		assertEquals("200|c200|", timer.getTrace(true));
		c.stop(TIMEOUT);

		//not reachable URI
		c = new Client(PORT+1);
		c.addPreSession("C", false, new HttpProxyHandler(new URI("http://127.0.0.1:" + (PORT+2))));
		c.start();
		c.waitForSessionEnding(TIMEOUT*5);
		assertEquals("SCR|SEN|", c.getRecordedData(true));
		w = "CONNECT 127.0.0.1:7779 HTTP/1.1|Host: 127.0.0.1:7779||";
		assertEquals(w, p.getTrace());
		assertEquals(0, c.session.getReadBytes());
		assertEquals(w.length() + countPipes(w), c.preSessions.get(0).getWrittenBytes());
		c.stop(TIMEOUT);

		//timed out connection
		c = new Client(PORT+1);
		c.exceptionRecordException = true;
		c.addPreSession("C", false, new HttpProxyHandler(new URI("http://127.0.0.1:" + (PORT)), 200) {
			@Override
			public ISessionStructureFactory getFactory() {
				return factory;
			}
		});
		p.skipConnection = true;
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|(Proxy connection timed out)|SEN|", c.getRecordedData(true));
		assertEquals("200|", timer.getTrace(true));
		c.stop(TIMEOUT);
		
		//timed out connection with 0 delay
		c = new Client(PORT+1);
		c.timer = new TestTimer();
		c.exceptionRecordException = true;
		c.addPreSession("C", false, new HttpProxyHandler(new URI("http://127.0.0.1:" + (PORT)), 0) {
			@Override
			public ISessionStructureFactory getFactory() {
				return factory;
			}
		});
		p.skipConnection = true;
		c.start();
		waitFor(200);
		assertEquals("", c.getRecordedData(true));
		c.session.getPipeline().markClosed();
		c.preSessions.get(0).close();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SEN|", c.getRecordedData(true));
		assertEquals("", timer.getTrace(true));
		c.stop(TIMEOUT);

		//timed out connection with other timer
		c = new Client(PORT+1);
		c.timer = new TestTimer();
		c.exceptionRecordException = true;
		c.addPreSession("C", false, new HttpProxyHandler(new URI("http://127.0.0.1:" + (PORT)), 300) {
			@Override
			public ISessionStructureFactory getFactory() {
				return factory;
			}
		});
		p.skipConnection = true;
		c.start();
		waitFor(100);
		c.preSessions.get(0).getTimer().scheduleEvent("TEST", 100);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|(Proxy connection timed out)|SEN|", c.getRecordedData(true));
		assertEquals("300|100|", timer.getTrace(true));
		c.stop(TIMEOUT);
		
		//URI without host
		c = new Client(PORT+1);
		c.exceptionRecordException = true;
		c.addPreSession("C", false, new HttpProxyHandler(new URI(null, null, "", "")));
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|(Undefined host)|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		
		//201 status code
		p.skipConnection = false;
		p.status = "201 Error";
		c = new Client(PORT+1);
		c.exceptionRecordException = true;
		c.addPreSession("C", false, new HttpProxyHandler(new URI("http://127.0.0.1:" + PORT)));
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|(HTTP proxy response status code: 201)|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
	}

	@Test
	public void testConnectByProxySsl() throws Exception {
		p = new HttpProxy(PORT+1);
		p.start(TIMEOUT, true);
		s = new Server(PORT);
		s.start();
		c = new Client(PORT+1);
		c.addPreSession("C", true, new HttpProxyHandler(new URI("http://127.0.0.1:" + PORT)) {
			@Override
			public ISessionConfig getConfig() {
				DefaultSessionConfig config = new DefaultSessionConfig() {
				
					@Override
					public SSLEngine createSSLEngine(boolean clientMode) throws SSLEngineCreateException {
						SSLEngine engine;
						
						try {
							engine = Server.getSSLContext().createSSLEngine();
						} catch (Exception e) {
							throw new SSLEngineCreateException(e);
						}
						engine.setUseClientMode(clientMode);
						if (!clientMode) {
							engine.setNeedClientAuth(true);
						}
						return engine;
					}
				};
				config.setWaitForInboundCloseMessage(true);
				return config;
			}
		});
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|RDY|SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|SCL|SEN|", s.getRecordedData(true));
	}
	
	@Test
	public void testHttpProxy() throws Exception {
		s = new Server(PORT);
		s.start();
		
		HttpProxy proxy = new HttpProxy(PORT+1);
		proxy.start(TIMEOUT);
		
		Socket socket = new Socket(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT+1)));
	
		try {
			socket.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT));

			byte[] buf = new byte[100];
			int i;

			socket.getOutputStream().write(new Packet(PacketType.ECHO, "123").toBytes());
			i = socket.getInputStream().read(buf);
			assertTrue(i > 0);
			Packet p = Packet.fromBytes(buf, 0, i);
			assertEquals(PacketType.ECHO_RESPONSE, p.type);
			assertEquals("123", p.payload);

			socket.getOutputStream().write(new Packet(PacketType.ECHO, "4567890").toBytes());
			i = socket.getInputStream().read(buf);
			assertTrue(i > 0);
			p = Packet.fromBytes(buf, 0, i);
			assertEquals(PacketType.ECHO_RESPONSE, p.type);
			assertEquals("4567890", p.payload);
		}
		finally {
			socket.close();
			proxy.stop(TIMEOUT);
		}
		
		proxy = new HttpProxy(PORT+1);
		try {
			proxy.status = "400 Bad Request";
			proxy.start(TIMEOUT);
			socket = new Socket(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT+1)));
			try {
				socket.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT));
				fail();
			}
			catch (Throwable e) {
			}
		}
		finally {
			proxy.stop(TIMEOUT);
		}
		
	}
	
	static class ExecuteTask implements Runnable {

		volatile Thread thread;
		
		int sleep;
		
		ExecuteTask(int sleep) {
			this.sleep = sleep;
		}
		
		@Override
		public void run() {
			if (sleep > 0) {
				try {
					Thread.sleep(sleep);
				} catch (InterruptedException e) {
				}
			}
			thread = Thread.currentThread();
		}
	}
	
	public static class CloseControllingException extends RuntimeException implements ICloseControllingException {

		private static final long serialVersionUID = 1L;

		ICloseControllingException.CloseType type;
		
		Throwable cause;
		
		CloseControllingException(String message, ICloseControllingException.CloseType type, Throwable cause) {
			super(message);
			this.type = type;
			this.cause = cause;
		}
		
		@Override
		public CloseType getCloseType() {
			return type;
		}

		@Override
		public Throwable getClosingCause() {
			return cause != null ? cause : this;
		}
		
	}
	
	static class PacketDecoder implements IDecoder<ByteBuffer,Packet> {
		
		IByteBufferAllocator allocator = DefaultAllocator.DEFAULT;
		
		ByteBuffer buffer = allocator.allocate(1000);
		
		@Override
		public void decode(ISession session, ByteBuffer data, List<Packet> out) throws Exception {
			buffer = allocator.ensure(buffer, data.remaining(), 1000, 64000);
			buffer.put(data);
			buffer.flip();
			
			byte[] b = buffer.array();
			int len;
			
			while ((len = Packet.available(b, buffer.position(), buffer.remaining())) > 0) {
				Packet packet = Packet.fromBytes(b, buffer.position(), len);
				
				buffer.position(buffer.position() + len);
				out.add(packet);
			}
			buffer.compact();
		}

		@Override
		public Class<ByteBuffer> getInboundType() {
			return ByteBuffer.class;
		}

		@Override
		public Class<Packet> getOutboundType() {
			return Packet.class;
		}
	}
}
