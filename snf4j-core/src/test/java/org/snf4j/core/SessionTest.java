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
package org.snf4j.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.session.IllegalSessionStateException;
import org.snf4j.core.session.SessionState;

public class SessionTest {
	long TIMEOUT = 2000;
	int PORT = 7777;
	IStreamHandler handler = new TestHandler("TestHandler");
	
	Server s;
	Client c;
	
	@Before
	public void before() {
		s = c = null;
	}
	
	@After
	public void after() throws InterruptedException {
		if (c != null) c.stop(TIMEOUT);
		if (s != null) s.stop(TIMEOUT);
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
	
	@Test
	public void testConstructor() {
		try {
			new StreamSession(null);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		StreamSession s = new StreamSession(handler);
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
	
	@Test
	public void testWrite() throws Exception {
		SelectionKey key1 = registerServerSocketChannel(Selector.open(), PORT);
		SelectionKey key2 = registerSocketChannel(key1.selector(), PORT);
		StreamSession s = new StreamSession(handler);
		
		try {
			s.write(getBytes(10,0));
			fail("exception should be thrown");
		}
		catch (IllegalSessionStateException e) {
			assertEquals(SessionState.OPENING, e.getIllegalState());
		}
		assertOutBuffers(s, "1024:0=0");
		
		s.setChannel((SocketChannel) key2.channel());
		s.setSelectionKey(key2);
		
		key2.interestOps(SelectionKey.OP_READ);
		s.write(getBytes(0, 0));
		assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, key2.interestOps());
		assertOutBuffers(s, "1024:0=0");
		
		key2.interestOps(SelectionKey.OP_READ);
		s.write(getBytes(1, 2));
		assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, key2.interestOps());
		assertOutBuffers(s, "1024:1=2");

		key2.interestOps(SelectionKey.OP_READ);
		s.write(getBytes(1023, 3));
		assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, key2.interestOps());
		assertOutBuffers(s, "1024:1=2,1023=3");

		key2.interestOps(SelectionKey.OP_READ);
		s.write(getBytes(0, 0));
		assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, key2.interestOps());
		assertOutBuffers(s, "1024:1=2,1023=3");
		
		key2.interestOps(SelectionKey.OP_READ);
		s.write(getBytes(1, 4));
		assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, key2.interestOps());
		assertOutBuffers(s, "1024:1=2,1023=3;1024:1=4");
		
		key2.interestOps(SelectionKey.OP_READ);
		s.write(getBytes(1023, 5));
		assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, key2.interestOps());
		assertOutBuffers(s, "1024:1=2,1023=3;1024:1=4,1023=5");
		
		key2.interestOps(SelectionKey.OP_READ);
		s.write(getBytes(1000,6));
		assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, key2.interestOps());
		assertOutBuffers(s, "1024:1=2,1023=3;1024:1=4,1023=5;1024:1000=6");
		
		key2.interestOps(SelectionKey.OP_READ);
		s.write(getBytes(1049,7));
		assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, key2.interestOps());
		assertOutBuffers(s, "1024:1=2,1023=3;1024:1=4,1023=5;1024:1000=6,24=7;1025:1025=7");
		
		ByteBuffer[] buffers = s.getOutBuffers();
		buffers[0].get();
		s.compactOutBuffers(0);
		assertOutBuffers(s, "1024:1023=3;1024:1=4,1023=5;1024:1000=6,24=7;1025:1025=7");
		
		key2.interestOps(SelectionKey.OP_READ);
		s.write(getBytes(1, 8));
		assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, key2.interestOps());
		assertOutBuffers(s, "1024:1023=3;1024:1=4,1023=5;1024:1000=6,24=7;1025:1025=7;1024:1=8");
		
		s.closing = ClosingState.SENDING;
		s.write(getBytes(1, 9));
		assertOutBuffers(s, "1024:1023=3;1024:1=4,1023=5;1024:1000=6,24=7;1025:1025=7;1024:1=8");
		s.closing = ClosingState.FINISHING;
		s.write(getBytes(1, 9));
		assertOutBuffers(s, "1024:1023=3;1024:1=4,1023=5;1024:1000=6,24=7;1025:1025=7;1024:1=8");
		s.closing = ClosingState.FINISHED;
		s.write(getBytes(1, 9));
		assertOutBuffers(s, "1024:1023=3;1024:1=4,1023=5;1024:1000=6,24=7;1025:1025=7;1024:1=8");
		s.closing = ClosingState.NONE;
		
		key1.channel().close();
		key2.channel().close();
		key1.selector().close();

		try {
			s.write(getBytes(20, 0));
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
	
	@Test
	public void testCompactOutBuffers() throws Exception {
		SelectionKey key1 = registerServerSocketChannel(Selector.open(), PORT);
		SelectionKey key2 = registerSocketChannel(key1.selector(), PORT);
		StreamSession s = new StreamSession(handler);
		
		s.setChannel((SocketChannel) key2.channel());
		s.setSelectionKey(key2);
		
		//empty buffer
		ByteBuffer[] bufs = s.getOutBuffers();
		assertTrue(s.compactOutBuffers(0));
		assertOutBuffers(s, "1024:0=0");
		
		//get 1 from 1
		s.write(getBytes(1,1));
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1=1");
		assertTrue(s.compactOutBuffers(0));
		assertOutBuffers(s, "1024:0=0");
		
		//get 1 from 2
		s.write(getBytes("1=2,1=3"));
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1=2");
		assertFalse(s.compactOutBuffers(0));
		assertOutBuffers(s, "1024:1=3");

		//get 1023 from 1024
		s.write(getBytes(1023,4));
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1=3,1022=4");
		assertFalse(s.compactOutBuffers(0));
		assertOutBuffers(s, "1024:1=4");

		//get 1024 from 1024
		s.write(getBytes(1023,5));
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1=4,1023=5");
		assertTrue(s.compactOutBuffers(0));
		assertOutBuffers(s, "1024:0=0");
		
		//get 1 from 1024;1
		s.write(getBytes("1=6,1=7,1023=8"));
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1=6");
		assertFalse(s.compactOutBuffers(0));
		assertOutBuffers(s, "1024:1=7,1022=8;1024:1=8");
		
		//get 1023 from 1023;1
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1=7,1022=8");
		assertFalse(s.compactOutBuffers(0));
		assertOutBuffers(s, "1024:1=8");
		
		//get 1024 from 1024;1025
		s.write(getBytes("2048=9"));
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1=8,1023=9");
		assertFalse(s.compactOutBuffers(0));
		assertOutBuffers(s, "1025:1025=9");
		
		//get 1025 from 1025
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1025=9");
		assertTrue(s.compactOutBuffers(0));
		assertOutBuffers(s, "1024:0=0");
		
		//get 1024 from 1025;1
		s.write(getBytes("2049=1"));
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1024=1");
		assertFalse(s.compactOutBuffers(0));
		assertOutBuffers(s, "1025:1025=1");
		s.write(getBytes("1=2"));
		assertOutBuffers(s, "1025:1025=1;1024:1=2");
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1024=1");
		assertFalse(s.compactOutBuffers(0));
		assertOutBuffers(s, "1025:1=1;1024:1=2");
		
		//get 1 from 1/1025;1
		bufs = s.getOutBuffers();
		assertBufferGet(bufs[0], "1=1");
		s.compactOutBuffers(0);
		assertOutBuffers(s, "1024:1=2");
		
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
		
		key1.channel().close();
		key2.channel().close();
		key1.selector().close();
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
	
	@Test
	public void testSuspendAndResume() throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);

		s.start();
		c.start();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
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
		
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
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
		assertEquals("SCR|SOP|DS|", c.getRecordedData(true));
		assertEquals("SCR|SOP|DR|NOP(1234)|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
		c.write(new Packet(PacketType.CLOSE));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|CLOSE()|SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
		
		//close outside the loop
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
		c.write(new Packet(PacketType.WRITE_AND_CLOSE));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|DR|WRITE_AND_CLOSE_RESPONSE()|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|WRITE_AND_CLOSE()|DS|SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
		
		//close outside the loop with data to send (closed on other side)
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
		c.write(new Packet(PacketType.QUICK_CLOSE));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|QUICK_CLOSE()|SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);

		//quick close outside the loop
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
		c.write(new Packet(PacketType.WRITE_AND_QUICK_CLOSE));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|WRITE_AND_QUICK_CLOSE()|SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);
		
		//quick close outside the loop with data to send (closed on other side)
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
		c.write(new Packet(PacketType.WRITE_AND_WAIT, "1000"));
		waitFor(500);
		c.getSession().quickClose();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|WRITE_AND_WAIT(1000)|DS|SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT); c.stop(TIMEOUT);

		//quick close outside the loop with data to send (closed on the same side)
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
		if (suspendS) s.getSession().suspendWrite();
		if (suspendC) c.getSession().suspendWrite();
		if (writeS) s.write(new Packet(PacketType.NOP));
		if (writeC) c.write(new Packet(PacketType.NOP));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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
	}  
	
	@Test
	public void testGetAddress() throws Exception {
		StreamSession session = new StreamSession(handler);
		
		assertNull(session.getLocalAddress());
		assertNull(session.getRemoteAddress());
		
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
		
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
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		assertEquals("SCR|SOP|", c.getRecordedData(true));
		assertEquals("SCR|SOP|", s.getRecordedData(true));
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

			fireEvent(s, t);
			assertEquals(t.toString()+"|", h.getEvents());
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

		assertEvents("SCR|SOP", "EXC|DS|DR", "SCL|SEN");
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
}
