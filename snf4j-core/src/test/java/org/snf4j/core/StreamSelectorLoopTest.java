/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2019 SNF4J contributors
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
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.factory.AbstractSessionFactory;
import org.snf4j.core.factory.DefaultThreadFactory;
import org.snf4j.core.factory.IStreamSessionFactory;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.logger.TestLogger;
import org.snf4j.core.pool.DefaultSelectorLoopPool;

public class StreamSelectorLoopTest {
	final long TIMEOUT = 2000;
	final long GET_SIZE_DELAY = 200;
	final int PORT = 7781;
	final int PORT_MIN = 8888;
	final int PORT_MAX = 9999;
	
	Server s;
	Client c, c1, c2, c3, c4;
	
	@Before
	public void before() {
		s = c = c1 = c2 = c3 = c4 = null;
	}

	@After
	public void after() throws InterruptedException {
		if (c != null) c.stop(TIMEOUT);
		if (c1 != null) c1.stop(TIMEOUT);
		if (c2 != null) c2.stop(TIMEOUT);
		if (c3 != null) c3.stop(TIMEOUT);
		if (c4 != null) c4.stop(TIMEOUT);
		if (s != null) s.stop(TIMEOUT);
	}
	
	private void waitFor(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}
	
	@Test
	public void testStop() throws Exception {
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		
		s.start();
		c.start();

		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
		
		SelectorLoop loop = c.getSelectLoop(); 
		loop.stopping.set(StoppingType.DIRTY);
		
		loop.stop();
		assertFalse(loop.join(TIMEOUT));
		assertEquals("", c.getRecordedData(true));
		assertEquals("DS|", s.getRecordedData(true));
		loop.quickStop();
		assertFalse(loop.join(TIMEOUT));
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		loop.dirtyStop();
		assertFalse(loop.join(TIMEOUT));
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		
		loop.stopping.set(StoppingType.QUICK);
		loop.stop();
		assertFalse(loop.join(TIMEOUT));
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		loop.quickStop();
		assertFalse(loop.join(TIMEOUT));
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));

		loop.dirtyStop();
		assertTrue(loop.join(TIMEOUT));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));
		
		s.stop(TIMEOUT);
		
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		
		s.start();
		c.start();

		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);

		loop = c.getSelectLoop(); 
		assertFalse(loop.isStopping());
		loop.stopping.set(StoppingType.GENTLE);
		assertTrue(loop.isStopping());

		loop.stop();
		assertFalse(loop.join(TIMEOUT));
		assertEquals("", c.getRecordedData(true));
		assertEquals("DS|", s.getRecordedData(true));
		loop.quickStop();
		assertTrue(loop.join(TIMEOUT));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|DS|SCL|SEN|", s.getRecordedData(true));

		s.stop(TIMEOUT);

		s = new Server(PORT, true);
		c = new Client(PORT, true);
		
		s.start();
		c.start();

		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);

		loop = c.getSelectLoop(); 
		loop.stopping.set(StoppingType.GENTLE);

		loop.stop();
		assertFalse(loop.join(TIMEOUT));
		assertEquals("", c.getRecordedData(true));
		assertEquals("DS|", s.getRecordedData(true));
		loop.dirtyStop();
		assertTrue(loop.join(TIMEOUT));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));
		
		s.stop(TIMEOUT);
	}
	
	@Test
	public void testStopOpenSessionByClient() throws Exception {
		s = new Server(PORT);
		s.start();
		waitFor(GET_SIZE_DELAY);
		
		assertEquals(1, s.getSelectLoop().getSize());
		
		//quick stopping 
		//---------------------------------------------------------------------
		c = new Client(PORT);	c.start(); 
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		waitFor(GET_SIZE_DELAY);
		assertEquals(2, s.getSelectLoop().getSize());
		assertEquals(1, c.getSelectLoop().getSize());

		c.quickStop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		waitFor(GET_SIZE_DELAY);
		assertEquals(1, s.getSelectLoop().getSize());
		try {
			c.getSelectLoop().getSize();
			fail("size should not be returned");
		}
		catch (ClosedSelectorException e) {
		}
		
		//gentle stopping
		//---------------------------------------------------------------------
		c = new Client(PORT); c.start();	
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		
		s.quickStop(TIMEOUT);
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		waitFor(GET_SIZE_DELAY);
		try {
			s.getSelectLoop().getSize();
			fail("size should not be returned");
		}
		catch (ClosedSelectorException e) {
		}
	}

	@Test
	public void testStopOpenSessionWithSuspendedWriteByClient() throws Exception {
		s = new Server(PORT);
		s.start();
		
		//quick stopping with suspended write
		//---------------------------------------------------------------------
		c = new Client(PORT); c.start(); 
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		s.waitForSessionReady(TIMEOUT);
		c.getSession().suspendWrite();
		c.getSession().write(new Packet(PacketType.ECHO, "X").toBytes());
		waitFor(1000);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		c.quickStop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		
		//gentle stopping with suspended write
		//---------------------------------------------------------------------
		c = new Client(PORT); c.start();	
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		s.waitForSessionReady(TIMEOUT);
		c.getSession().suspendWrite();
		c.getSession().write(new Packet(PacketType.ECHO, "X").toBytes());
		waitFor(1000);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		
		s.quickStop(TIMEOUT);
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
	}

	@Test
	public void testStopOpenSessionWithSuspendedReadByClient() throws Exception {
		s = new Server(PORT);
		s.start();
		
		//quick stopping with suspended read
		//---------------------------------------------------------------------
		c = new Client(PORT); c.start(); 
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		s.waitForSessionReady(TIMEOUT);
		c.getSession().suspendRead();
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		c.quickStop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		
		//gentle stopping with suspended read
		//---------------------------------------------------------------------
		c = new Client(PORT); c.start();	
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		s.waitForSessionReady(TIMEOUT);
		c.getSession().suspendRead();
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		
		s.quickStop(TIMEOUT);
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
	}

	@Test
	public void testStopOpenSessionWithSuspendedBothByClient() throws Exception {
		s = new Server(PORT);
		s.start();
		
		//quick stopping with suspended read and write
		//---------------------------------------------------------------------
		c = new Client(PORT); c.start(); 
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		s.waitForSessionReady(TIMEOUT);
		c.getSession().suspendRead();
		c.getSession().suspendWrite();
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		c.quickStop(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		
		//gentle stopping with suspended read and write
		//---------------------------------------------------------------------
		c = new Client(PORT); c.start();	
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		s.waitForSessionReady(TIMEOUT);
		c.getSession().suspendRead();
		c.getSession().suspendWrite();
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		c.stop(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		
		s.quickStop(TIMEOUT);
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
	}
	
	@Test
	public void testStopOpenSessionByServer() throws Exception {
		s = new Server(PORT);
		s.start();
		
		//quick stopping 
		//---------------------------------------------------------------------
		c = new Client(PORT);	c.start(); 
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		s.quickStop(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		c.quickStop(TIMEOUT);
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		
		//gentle stopping
		//---------------------------------------------------------------------
		s = new Server(PORT); s.start();
		c = new Client(PORT); c.start();	
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		s.stop(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		
		try {
			c.getSession().write((byte[])null);
			fail("exception not thrown");
		}
		catch (NullPointerException e) {
		}
		
		c.quickStop(TIMEOUT);
		assertEquals("", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
	}
	
	@Test
	public void testOpenAndCloseSessionInOtherThread() throws Exception {
		s = new Server(PORT);
		DefaultSelectorLoopPool pool = new DefaultSelectorLoopPool(2);
		
		s.start();
		assertNull(s.getSelectLoop().getPool());
		assertNull(s.getSelectLoop().getParentPool());
		s.getSelectLoop().setPool(pool);
		assertTrue(pool == s.getSelectLoop().getPool());
		
		c = new Client(PORT);	c.start(); 
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		assertNull(c.getSelectLoop().getParentPool());
		assertTrue(pool.getPool()[0].getParentPool() == pool);
		
		c.getSession().write(new Packet(PacketType.GET_THREAD, "").toBytes());
		c.waitForDataRead(TIMEOUT);

		String expected = "DS|DR|GET_THREAD_RESPONSE($)|".replace("$", pool.getPool()[0].toString());
		
		assertEquals(expected, c.getRecordedData(true));
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|GET_THREAD()|DS|", s.getRecordedData(true));
		
		s.stop(TIMEOUT);
		pool.stop();
		c.waitForSessionEnding(TIMEOUT);
		c.quickStop(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
	}

	@Test
	public void testQuickCloseSessionInOtherThread() throws Exception {
		s = new Server(PORT);
		DefaultSelectorLoopPool pool = new DefaultSelectorLoopPool(2);
		
		s.start();
		s.getSelectLoop().setPool(pool);

		c = new Client(PORT);	c.start(); 
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		
		c.getSession().write(new Packet(PacketType.GET_THREAD, "").toBytes());
		c.waitForDataRead(TIMEOUT);

		String expected = "DS|DR|GET_THREAD_RESPONSE($)|".replace("$", pool.getPool()[0].toString());
		
		assertEquals(expected, c.getRecordedData(true));
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|GET_THREAD()|DS|", s.getRecordedData(true));
		
		s.quickStop(TIMEOUT);
		pool.quickStop();
		c.waitForSessionEnding(TIMEOUT);
		c.quickStop(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
	}
	
	@Test
	public void testCloseStoppedAndEmpty() throws Exception {
		SelectorLoop loop1 = new SelectorLoop();
		SelectorLoop loop2 = new SelectorLoop("Name2");
		
		assertEquals(loop1.getId()+1, loop2.getId());
		assertEquals("SelectorLoop-"+loop1.getId(), loop1.toString());
		assertEquals("SelectorLoop-Name2", loop2.toString());
		assertEquals("SelectorLoop-"+loop1.getId(), loop1.getName());
		assertEquals("Name2", loop2.getName());
		assertTrue(loop1.isStopped());
		assertTrue(loop2.isStopped());
		assertFalse(!loop1.isOpen());
		assertFalse(!loop2.isOpen());
		
		loop1.stop();
		loop1.join(TIMEOUT);
		assertTrue(loop1.isStopped());
		loop2.quickStop();
		loop2.join(TIMEOUT);
		assertTrue(loop2.isStopped());
		assertTrue(!loop1.isOpen());
		assertTrue(!loop2.isOpen());
	}
	
	@Test
	public void testCloseRunningAndEmpty() throws Exception {
		SelectorLoop loop1 = new SelectorLoop();
		assertTrue(loop1.isStopped());
		assertFalse(!loop1.isOpen());
		loop1.start();
		loop1.start();
		assertFalse(loop1.isStopped());
		assertFalse(!loop1.isOpen());
		loop1.stop();
		assertTrue(loop1.isStopping());
		loop1.join(TIMEOUT);
		assertTrue(loop1.isStopped());
		assertTrue(!loop1.isOpen());
		
		try {
			loop1.start();
			fail("Loop cannot be started when selector is closed");
		}
		catch (ClosedSelectorException e) {
		}
		loop1.stop();
	}

	@Test
	public void testCloseRunningInCurrentThreadAndEmpty() throws Exception {
		final SelectorLoop loop1 = new SelectorLoop();
		assertTrue(loop1.isStopped());
		assertFalse(!loop1.isOpen());
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					waitFor(100);
					assertFalse(loop1.isStopped());
					assertFalse(!loop1.isOpen());
					loop1.stop();
				} catch (InterruptedException e) {
				}
			}
		}).start();

		loop1.start(true);
		assertTrue(loop1.isStopped());
		assertTrue(!loop1.isOpen());
	}

	@Test
	public void testRegister() throws IOException {
		SelectorLoop loop1 = new SelectorLoop();
	
		TestHandler h = new TestHandler("");
		
		try {
			loop1.register(null, (IStreamHandler)null);
			fail ("handler cannot be null");
		}
		catch (IllegalArgumentException e) {
			assertEquals("handler is null", e.getMessage());
		}

		try {
			loop1.register(null, (StreamSession)null);
			fail ("session cannot be null");
		}
		catch (IllegalArgumentException e) {
			assertEquals("session is null", e.getMessage());
		}

		try {
			loop1.register(null, (IStreamSessionFactory)null);
			fail ("factory cannot be null");
		}
		catch (IllegalArgumentException e) {
			assertEquals("factory is null", e.getMessage());
		}

		try {
			loop1.register(null, h);
			fail ("channel cannot be null");
		}
		catch (IllegalArgumentException e) {
			assertEquals("channel is null", e.getMessage());
		}
		
		SocketChannel sc = SocketChannel.open();
		try {
			loop1.register(sc, SelectionKey.OP_ACCEPT, h);
			fail ("options have to be valid");
		}
		catch (IllegalArgumentException e) {
			assertEquals("invalid options 16", e.getMessage());
		}
		
		loop1.stopping.set(StoppingType.GENTLE);
		try {
			loop1.register(sc, h);
			fail("loop cannot be is stopping state");
		}
		catch (SelectorLoopStoppingException e) {}
		loop1.stopping.set(null);
		
		loop1.stop();
		try {
			loop1.register(sc, h);
			fail("loop have to be open");
		}
		catch (ClosedSelectorException e) {}
		
	}
	
	final StringBuilder taskResult = new StringBuilder();
	
	class Task implements Runnable {

		String text;
		boolean exception;
		SelectorLoop loop;
		
		Task(String text, boolean exception, SelectorLoop loop) {
			this.text = text;
			this.exception = exception;
			this.loop = loop;
		}

		Task(String text, boolean exception) {
			this.text = text;
			this.exception = exception;
		}
		
		@Override
		public void run() {
			if (loop != null) {
				loop.registerTask(new Task("L"+text, exception));
			}
			else {
				if (exception) {
					taskResult.append("E");
					throw new NullPointerException();
				}
				synchronized (taskResult) {
					taskResult.append(text);
				}
			}
		}
		
	}
	
	@Test
	public void testRegisterTask() throws Exception {
		
		//register task before start
		SelectorLoop loop = new SelectorLoop();
		loop.registerTask(new Task("T1", false));
		waitFor(500);
		assertEquals("", taskResult.toString());
		loop.start();
		waitFor(50);
		assertEquals("T1", taskResult.toString());
		loop.stop();
		loop.join(1000);
		
		//register 2 tasks before start
		loop = new SelectorLoop();
		loop.registerTask(new Task("T2", false));
		loop.registerTask(new Task("T0", true));
		loop.registerTask(new Task("T3", false));
		loop.start();
		waitFor(50);
		assertEquals("T1T2ET3", taskResult.toString());
		
		//register task after start
		loop.registerTask(new Task("T4", false));
		loop.registerTask(new Task("T5", false, loop));
		waitFor(50);
		assertEquals("T1T2ET3T4LT5", taskResult.toString());
		
		//register task after stop
		loop.stop();
		loop.join();
		try {
			loop.registerTask(new Task("T6", false));
			fail("exception should be thrown");
		}
		catch (SelectorLoopStoppingException e) {
		}
		
		//register task from loop
		loop = new SelectorLoop();
		
		
	}
	
	@Test
	public void testSetThreadFactory() throws Exception {
		ThreadFactory tf = new ThreadFactory() {
			public boolean executed;
			
			@Override
			public Thread newThread(Runnable r) {
				executed = true;
				return new Thread(r);
			}
			
			@Override
			public String toString() {
				return executed ? "T" : "F";
			}
		};
		SelectorLoop loop1 = new SelectorLoop();
		
		try {
			loop1.setThreadFactory(null);
			fail("Exception should be thrown");
		}
		catch (NullPointerException e) {
		}
		
		assertTrue(DefaultThreadFactory.DEFAULT == loop1.getThreadFactory());
		loop1.setThreadFactory(tf);
		assertTrue(tf == loop1.getThreadFactory());
		loop1.start();
		loop1.stop();
		assertEquals("T", tf.toString());
		loop1.join();
	}

	@Test
	public void testSetExecutor() throws Exception {
		Executor exe = new Executor() {

			@Override
			public void execute(Runnable arg0) {
			}
			
		};
		
		SelectorLoop loop1 = new SelectorLoop();
		
		try {
			loop1.setExecutor(null);
			fail("Exception should be thrown");
		}
		catch (NullPointerException e) {
		}
		
		assertTrue(DefaultExecutor.DEFAULT == loop1.getExecutor());
		loop1.setExecutor(exe);
		assertTrue(exe == loop1.getExecutor());
	}
	
	@Test
	public void testRegistrationBeforeStart() throws Exception {
		s = new Server(PORT);
		s.start(true);

		c = new Client(PORT);
		c.start(true);

		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		
		c.write(new Packet(PacketType.ECHO));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|ECHO()|DS|", s.getRecordedData(true));
		c.quickStop(TIMEOUT);
		s.quickStop(TIMEOUT);
	}
	
	@Test
	public void testRegistrationOfUsedSession() throws Exception {
		SelectorLoop loop = new SelectorLoop();
		
		loop.start();
		SocketChannel channel = SocketChannel.open();
		
		channel.configureBlocking(false);
		channel.socket().setReuseAddress(true);
		channel.socket().bind(new InetSocketAddress(PORT));
		
		StreamSession session = new StreamSession(new TestHandler("Test"));
		session.setChannel(channel);
		
		try {
			loop.register(channel, session);
			fail("Exception not thrown");
		}
		catch (IllegalArgumentException e) {
			assertEquals("session cannot be reused", e.getMessage());
		}
		
		channel.close();
		loop.stop();
		loop.join(TIMEOUT);
	}
	
	@Test
	public void testRegistrationWhileStopping() throws Exception {
		s = new Server(PORT);
		
		//register server socket
		s.setThreadFactory(new DelayedThreadFactory(100));
		s.start();
		s.stop(TIMEOUT);

		s = new Server(PORT);
		s.start();

		//register client socket (session not created)
		c = new Client(PORT);
		c.setThreadFactory(new DelayedThreadFactory(100));
		c.start();
		c.stop(TIMEOUT);
		assertEquals("",c.getRecordedData(true));
		
		s.stop(TIMEOUT);
	}
	
	@Test
	public void testRegistrationOfRegistered() throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);
		
		s.start();
		c.start();
		s.waitForSessionOpen(TIMEOUT);
		c.waitForSessionOpen(TIMEOUT);
		StreamSession session = new StreamSession(new TestHandler("Test1"));
		c.loop.register(c.getSession().channel, SelectionKey.OP_CONNECT, session);
		waitFor(GET_SIZE_DELAY);
		assertEquals(1, c.loop.getSize());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}
	
	@Test
	public void testHandleAccepting() throws Exception {
		TestSelectorLoopController slc = new TestSelectorLoopController();
		TestSelectorPool pool = new TestSelectorPool();
		s = new Server(PORT);
		s.controller = slc;
		s.start();
		assertTrue(s.getSelectLoop().getController() == slc);
		s.getSelectLoop().setPool(pool);
		c1 = new Client(PORT);
		c1.start();
		c1.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c1.getRecordedData(true));
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		
		//disable accepting
		c2 = new Client(PORT);
		slc.accept = TestSelectorLoopController.BLOCK;
		c2.start();
		assertTrue(c2.getSelectLoop().getController() == DefaultSelectorLoopController.DEFAULT);
		c2.waitForSessionEnding(TIMEOUT);
		waitFor(100);
		assertEquals("", s.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|SCL|SEN|", c2.getRecordedData(true));
		c2.stop(TIMEOUT);
		
		//exception while accepting
		c3 = new Client(PORT);
		slc.accept = TestSelectorLoopController.EXCEPTION;
		c3.start();
		c3.waitForSessionEnding(TIMEOUT);
		assertEquals("", s.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|SCL|SEN|", c3.getRecordedData(true));
		
		//exception while getting from the pool
		c4 = new Client(PORT);
		slc.accept = TestSelectorLoopController.DEFAULT;
		pool.getException = true;
		c4.start();
		s.waitForSessionEnding(TIMEOUT);
		c4.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|SEN|", s.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|SCL|SEN|", c4.getRecordedData(true));
		
		//check if the loop is not broken
		c1.write(new Packet(PacketType.ECHO));
		c1.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE()|", c1.getRecordedData(true));
		assertEquals("DR|ECHO()|DS|", s.getRecordedData(true));
		
		c1.stop(TIMEOUT);
		c1.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c1.getRecordedData(true));
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT);		
	}

	@Test
	public void testHandleConnecting() throws Exception {
		TestSelectorLoopController slc = new TestSelectorLoopController();
		s = new Server(PORT);
		s.start();
		c1 = new Client(PORT);
		c1.start();
		c1.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c1.getRecordedData(true));
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		//disable connecting
		slc.connect = TestSelectorLoopController.BLOCK;
		c2 = new Client(PORT);
		c2.controller = slc;
		c2.start();
		s.waitForSessionEnding(TIMEOUT);
		c2.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|RDY|SCL|SEN|", s.getRecordedData(true));
		assertEquals("SCR|SEN|", c2.getRecordedData(true));
		c2.stop(TIMEOUT);
		
		//exception while connecting
		slc.connect = TestSelectorLoopController.EXCEPTION;
		c3 = new Client(PORT);
		c3.controller = slc;
		c3.start();
		s.waitForSessionEnding(TIMEOUT);
		c3.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|RDY|SCL|SEN|", s.getRecordedData(true));
		assertEquals("SCR|EXC|SEN|", c3.getRecordedData(true));
		c3.stop(TIMEOUT);
		
		c1.stop(TIMEOUT);
		c1.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c1.getRecordedData(true));
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		s.stop(TIMEOUT);		
	}	

	@Test
	public void testElogWarnOrError() throws Exception {
		SelectorLoop loop = new SelectorLoop();
		TestLogger logger = new TestLogger();
		
		loop.elogWarnOrError(logger, "Message {}: {}", "X", new Exception("E"));
		assertEquals("W|Message {}: {}|[X, java.lang.Exception: E]|", logger.getLog());
		loop.elogWarnOrError(logger, "Message {}: {}", "X", new IllegalArgumentException("I"));
		assertEquals("E|Message {}: {}|[X, java.lang.IllegalArgumentException: I]|", logger.getLog());
	}
	
	@Test
	public void testTrackSizeChanges() throws Exception {
		s = new Server(PORT);
		TestSelectorPool pool = new TestSelectorPool();

		SelectorLoop loop = new SelectorLoop(null, pool, null);
		loop.start();
		pool.loop = loop;
		s.pool = pool;
		s.start();

		c1 = new Client(PORT);
		c1.start();
		c1.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		
		c1.write(new Packet(PacketType.NOP));
		s.waitForDataRead(TIMEOUT);
		assertEquals(loop.toString()+"|1|0|", pool.getUpdate());

		c1.write(new Packet(PacketType.NOP));
		s.waitForDataRead(TIMEOUT);
		assertEquals("", pool.getUpdate());

		c2 = new Client(PORT);
		c2.start();
		c2.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);

		c2.write(new Packet(PacketType.NOP));
		s.waitForDataRead(TIMEOUT);
		assertEquals(loop.toString()+"|2|1|", pool.getUpdate());
		
		c2.stop(TIMEOUT);
		c2.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		
		c1.write(new Packet(PacketType.NOP));
		s.waitForDataRead(TIMEOUT);
		assertEquals(loop.toString()+"|1|2|", pool.getUpdate());
		
		c1.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
	}
	
	@Test
	public void testConnectionFailure() throws Exception {
		c = new Client(PORT);
		c.start();
		
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
	}
	
	@Test
	public void testConnectClientToClient() throws Exception {
		//Does not work in the Travis CI environment
		Assume.assumeTrue(!"true".equalsIgnoreCase(System.getenv("TRAVIS")));
		
		int size = PORT_MAX-PORT_MIN;
		
		Random r = new Random(System.currentTimeMillis());
		
		int port1 = PORT_MIN + r.nextInt(size);
		int port2 = port1+1;
		
		c1 = new Client(port1);
		c1.localPort = port2;
		c1.reuseAddress = true;
		c2 = new Client(port2);
		c2.localPort = port1;
		c2.reuseAddress = true;
		c2.start();
		c1.start();
		
		c1.waitForSessionReady(TIMEOUT);
		c2.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c1.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", c2.getRecordedData(true));
		
		c1.write(new Packet(PacketType.ECHO));
		
		c1.waitForDataRead(TIMEOUT);
		c2.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE()|", c1.getRecordedData(true));
		assertEquals("DR|ECHO()|DS|", c2.getRecordedData(true));
		
		c1.getSession().close();
		
		c1.waitForSessionEnding(TIMEOUT);
		c2.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c1.getRecordedData(true));
		assertEquals("SCL|SEN|", c2.getRecordedData(true));
		
		c1.stop(TIMEOUT);
		c2.stop(TIMEOUT);
	}
	
	@Test
	public void testExceptionHandling() throws Exception {
		s = new Server(PORT);
		s.start();
		c = new Client(PORT);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		c.write(new Packet(PacketType.ECHO));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);

		assertEquals("DS|DR|ECHO_RESPONSE()|", c.getRecordedData(true));
		assertEquals("DR|ECHO()|DS|", s.getRecordedData(true));
		
		c.getSession().exception(new Exception());

		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);

		assertEquals("EXC|SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		c.getSession().exception(new Exception());
	}

	AbstractSessionFactory factory = new AbstractSessionFactory() {

		@Override
		protected IStreamHandler createHandler(SocketChannel channel) {
			return new TestHandler("");
		}
	};
	
	@Test
	public void testClosingAction() throws Exception {
		s = new Server(PORT);
		s.start();
		c = new Client(PORT);
		c.start();

		//default action
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);

		assertEquals("R|", s.getServerSocketLogs());
		assertTrue(s.ssc == s.registeredSsc);
		assertNull(s.closedSsc);
		
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		waitFor(100);
		assertTrue(c.loop.isOpen());
		c.stop(TIMEOUT);

		//stop with one session in the loop
		c = new Client(PORT);
		c.endingAction = EndingAction.STOP;
		c.start();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertTrue(c.loop.join(TIMEOUT));

		//stop with more sessions in the loop
		c = new Client(PORT);
		c.endingAction = EndingAction.STOP;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		StreamSession session1 = c.getSession();
		c.endingAction = EndingAction.DEFAULT;
		c.start(false, c.loop);
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		session1.close();
		waitFor(500);
		assertEquals("SCL|SEN|SCL|SEN|", c.getRecordedData(true));
		assertTrue(c.loop.join(TIMEOUT));
		
		//stop with more sessions in the loop (one listening)
		c = new Client(PORT);
		c.endingAction = EndingAction.STOP;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		ssc.socket().bind(new InetSocketAddress(PORT+1));
		c.loop.register(ssc, factory);
		waitFor(GET_SIZE_DELAY);
		assertEquals(2, c.loop.getSize());
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		waitFor(500);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertTrue(c.loop.join(TIMEOUT));

		//stop when empty with one session in the loop
		c = new Client(PORT);
		c.endingAction = EndingAction.STOP_WHEN_EMPTY;
		c.start();
		c.waitForSessionOpen(TIMEOUT);
		s.waitForSessionOpen(TIMEOUT);
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertTrue(c.loop.join(TIMEOUT));

		//stop when empty with more sessions in the loop
		c = new Client(PORT);
		c.endingAction = EndingAction.STOP_WHEN_EMPTY;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		session1 = c.getSession();
		c.endingAction = EndingAction.DEFAULT;
		c.start(false, c.loop);
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		session1.close();
		waitFor(500);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertTrue(c.loop.isOpen());
		c.getSession().close();
		waitFor(500);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertTrue(c.loop.join(TIMEOUT));

		//stop when empty with more sessions in the loop (one listening)
		c = new Client(PORT);
		c.endingAction = EndingAction.STOP_WHEN_EMPTY;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		ssc.socket().bind(new InetSocketAddress(PORT+1));
		c.loop.register(ssc, factory);
		waitFor(GET_SIZE_DELAY);
		assertEquals(2, c.loop.getSize());
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		waitFor(500);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		waitFor(GET_SIZE_DELAY);
		assertEquals(1, c.loop.getSize());
		assertTrue(c.loop.isOpen());
		ssc.close();
		c.loop.wakeup();
		waitFor(500);
		assertTrue(c.loop.join(TIMEOUT));
		s.registeredSsc = null;
		s.stop(TIMEOUT);
		
		assertEquals("C|", s.getServerSocketLogs());
		assertTrue(s.ssc == s.closedSsc);
		assertNull(s.registeredSsc);

		//stop when empty with more sessions in the loop (with pool)
		s = new Server(PORT);
		s.pool = new DefaultSelectorLoopPool(1);
		s.endingAction = EndingAction.STOP_WHEN_EMPTY;
		s.start();
		c = new Client(PORT);
		c.endingAction = EndingAction.STOP_WHEN_EMPTY;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		session1 = c.getSession();
		c.start(false, c.loop);
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		waitFor(500);
		session1.close();
		waitFor(500);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertTrue(c.loop.isOpen());
		c.getSession().close();
		waitFor(500);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertTrue(c.loop.join(TIMEOUT));
		assertTrue(((DefaultSelectorLoopPool)s.pool).getPool()[0].join(TIMEOUT));
	
	}

    @Test
	public void testInLoop() throws Exception {
		s = new Server(PORT);
		s.start();
		c = new Client(PORT);
		c.start();

		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);

		assertFalse(c.loop.inLoop());
		
		c.write(new Packet(PacketType.IN_LOOP));
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|IN_LOOP_RESPONSE(true)|", c.getRecordedData(true));
	}	
    
    @Test
    public void testRebuild() throws Exception {
		s = new Server(PORT);
		s.start();
		c = new Client(PORT);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
		
		SelectionKey key = c.getSession().key;
		assertTrue(key.isValid());
		c.loop.rebuild();
		waitFor(2000);
		assertTrue(!key.isValid());
		assertTrue(c.getSession().key != key);

		c.write(new Packet(PacketType.ECHO));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|ECHO()|DS|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE()|", c.getRecordedData(true));
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));

		waitFor(1000);
		key = s.loop.selector.keys().iterator().next();
		assertTrue(key.isValid());
		s.loop.rebuild();
		waitFor(2000);
		assertTrue(!key.isValid());
		assertTrue(s.loop.selector.keys().iterator().next() != key);
		c = new Client(PORT);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		key = s.getSession().key;
		assertTrue(key.isValid());
		s.loop.rebuild();
		waitFor(2000);
		assertTrue(!key.isValid());
		assertTrue(s.getSession().key != key);
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		
		SelectorLoop loop = new SelectorLoop();
		loop.selector.close();
		loop.selector = null;
		loop.rebuildSelector();
		assertNull(loop.selector);
		
		TestSelectorFactory f = new TestSelectorFactory();
		loop = new SelectorLoop("loop", null, f);
		Selector selector = loop.selector;
		f.throwException = true;
		loop.rebuildSelector();
		assertTrue(loop.selector == selector);
		selector.close();
    }

    @Test
    public void testRebuildWithException() throws Exception {
    	TestSelectorFactory factory = new TestSelectorFactory();
    	factory.testSelectorCounter = 2;
    	SelectorLoop loop = new SelectorLoop("loop", null, factory);
		s = new Server(PORT);
		s.start(false, loop);
		c = new Client(PORT);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
		assertEquals("R|",s.getServerSocketLogs());
		
    	factory.delegateException = true;
		s.loop.rebuild();
		waitFor(2000);
		
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertEquals("C|",s.getServerSocketLogs());
		waitFor(this.GET_SIZE_DELAY);
		assertEquals(0, s.loop.getSize());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		//with closing actions
		EndingAction[] actions = new EndingAction[] {EndingAction.STOP, EndingAction.QUICK_STOP, EndingAction.STOP_WHEN_EMPTY};
		for (EndingAction action: actions) {
			factory = new TestSelectorFactory();
			factory.testSelectorCounter = 2;
			loop = new SelectorLoop("loop", null, factory);
			s = new Server(PORT);
			s.endingAction = action;
			s.start(false, loop);
			c = new Client(PORT);
			c.start();
			c.waitForSessionReady(TIMEOUT);
			s.waitForSessionReady(TIMEOUT);
			c.getRecordedData(true);
			s.getRecordedData(true);
			assertEquals("R|",s.getServerSocketLogs());

			factory.delegateException = true;
			s.loop.rebuild();
			waitFor(2000);

			c.waitForSessionEnding(TIMEOUT);
			s.waitForSessionEnding(TIMEOUT);
			assertEquals("SCL|SEN|", c.getRecordedData(true));
			assertEquals("SCL|SEN|", s.getRecordedData(true));
			assertEquals("C|",s.getServerSocketLogs());
			assertTrue(s.loop.join(TIMEOUT));
			c.stop(TIMEOUT);
		}

    
    }    
    
    @Test
    public void testAutoRebuild() throws Exception {
    	TestSelectorFactory factory = new TestSelectorFactory();
    	factory.testSelectorCounter = 1;
    	SelectorLoop loop = new SelectorLoop("loop", null, factory);

		s = new Server(PORT);
		s.start(false, loop);
		c = new Client(PORT);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
    	
		assertTrue(s.loop.selector instanceof TestSelector);
		TestSelector selector = (TestSelector) s.loop.selector;
		
		selector.nonBlocking = true;
		selector.closeException = true;
		s.loop.wakeup();
		
		waitFor(2000);
		assertNotNull(s.loop.selector);
		assertFalse(s.loop.selector instanceof TestSelector);
		
		c.write(new Packet(PacketType.ECHO));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|ECHO()|DS|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE()|", c.getRecordedData(true));

		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));

		c = new Client(PORT);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
    	
    }
    
    @Test
    public void testAutoRebuildWithException() throws Exception {
    	TestSelectorFactory factory = new TestSelectorFactory();
    	factory.testSelectorCounter = 2;
    	SelectorLoop loop = new SelectorLoop("loop", null, factory);
		s = new Server(PORT);
		s.start(false, loop);
		c = new Client(PORT);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
		assertEquals("R|",s.getServerSocketLogs());
		
		assertTrue(s.loop.selector instanceof TestSelector);
		TestSelector selector = (TestSelector) s.loop.selector;
		
    	factory.delegateException = true;
		selector.nonBlocking = true;
		s.loop.wakeup();

		waitFor(2000);
		
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertEquals("C|",s.getServerSocketLogs());
		waitFor(this.GET_SIZE_DELAY);
		assertEquals(0, s.loop.getSize());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		//with closing actions
		EndingAction[] actions = new EndingAction[] {EndingAction.STOP, EndingAction.QUICK_STOP, EndingAction.STOP_WHEN_EMPTY};
		for (EndingAction action: actions) {
	    	factory = new TestSelectorFactory();
	    	factory.testSelectorCounter = 2;
	    	loop = new SelectorLoop("loop", null, factory);
			s = new Server(PORT);
			s.endingAction = action;
			s.start(false, loop);
			c = new Client(PORT);
			c.start();
			c.waitForSessionReady(TIMEOUT);
			s.waitForSessionReady(TIMEOUT);
			c.getRecordedData(true);
			s.getRecordedData(true);
			assertEquals("R|",s.getServerSocketLogs());
			
			assertTrue(s.loop.selector instanceof TestSelector);
			selector = (TestSelector) s.loop.selector;
			
	    	factory.delegateException = true;
			selector.nonBlocking = true;
			s.loop.wakeup();

			waitFor(2000);
			
			c.waitForSessionEnding(TIMEOUT);
			s.waitForSessionEnding(TIMEOUT);
			assertEquals("SCL|SEN|", c.getRecordedData(true));
			assertEquals("SCL|SEN|", s.getRecordedData(true));
			assertEquals("C|",s.getServerSocketLogs());
			assertTrue(s.loop.join(TIMEOUT));
			c.stop(TIMEOUT);
		}
	}    
    
    private void assertConnection(Server s, Client c) throws InterruptedException {
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);    	
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.ECHO));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE()|", c.getRecordedData(true));
		assertEquals("DR|ECHO()|DS|", s.getRecordedData(true));
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
    }
    
    @Test
    public void testRegisterChannelsInDifferentStates() throws Exception {
		s = new Server(PORT);
		s.start();
		c = new Client(PORT);
		
		//register connected channel
		c = new Client(PORT);
		c.channel = SocketChannel.open(new InetSocketAddress(InetAddress.getByName(c.ip), PORT));
		assertTrue(c.channel.isConnected());
		c.start();
		assertConnection(s,c);

		//register channel with pending connection
		c = new Client(PORT);
		c.channel = SocketChannel.open();
		c.channel.configureBlocking(false);
		c.channel.connect(new InetSocketAddress(InetAddress.getByName(c.ip), PORT));
		assertTrue(c.channel.isConnectionPending());
		c.start();
		assertConnection(s,c);
		
		//register only open channel
		c = new Client(PORT);
		c.channel = SocketChannel.open();
   	    assertTrue(!c.channel.isConnected());
		assertTrue(!c.channel.isConnectionPending());
		assertTrue(c.channel.isOpen());
		c.start();	
		waitFor(1000);
		assertEquals("", s.getRecordedData(true));
		String recordedData = c.getRecordedData(true);
		//It appears that this scenario works differently based on the NIO implementation
		//For example on Windows select will be blocked till the connect 
		if ("".equals(recordedData)) {
			assertEquals("", c.getRecordedData(true));
			c.channel.connect(new InetSocketAddress(InetAddress.getByName(c.ip), PORT));
			assertConnection(s,c);
		}
		else {
			assertEquals("SCR|EXC|SEN|", recordedData);
		}
		
		//register closed channel
		SocketChannel channel = SocketChannel.open();
		channel.close();
		TestHandler handler = new TestHandler("handler");
		handler.events = new StringBuilder();
		StreamSession session = new StreamSession(handler);
		try {
			s.loop.register(channel, session);
			fail("exception not thrown");
		}
		catch (ClosedChannelException e) {
		}
		assertEquals("", handler.getEvents());
		
		s.loop.handleRegisteredKey(null, channel, session);
		assertEquals("SESSION_CREATED|SESSION_ENDING|", handler.getEvents());
		
    }
    
    @Test
    public void testWaitAndWorkTime() throws Exception {
		s = new Server(PORT);
		s.start();
		waitFor(10500);
		long waitTime1 = s.getSelectLoop().getTotalWaitTime();
		long workTime1 = s.getSelectLoop().getTotalWorkTime();
		s.stop(TIMEOUT);
		long waitTime2;
		long workTime2;
		
		System.setProperty("org.snf4j.SelectorSelectTimeout", "0");
		try {
			s = new Server(PORT);
			s.start();
			s.getSelectLoop().wakeup();
			waitFor(10500);
			waitTime2 = s.getSelectLoop().getTotalWaitTime();
			workTime2 = s.getSelectLoop().getTotalWorkTime();
			s.stop(TIMEOUT);

		} finally {
			System.setProperty("org.snf4j.SelectorSelectTimeout", "1000");
		}
		assertTrue("" +waitTime1 +">"+ waitTime2, waitTime1 > waitTime2);
		assertTrue("" +workTime1 +">"+ workTime2, workTime1 > workTime2);
		assertTrue("" +waitTime1 +">"+ workTime1, waitTime1 > workTime1);
		
		//wait > work
		s = new Server(PORT);
		s.start();
		c = new Client(PORT);
		c.start();
		s.waitForSessionOpen(TIMEOUT);
		c.waitForSessionOpen(TIMEOUT);
		for (int i=0; i<10; ++i) {
			waitFor(200);
			c.write(new Packet(PacketType.ECHO));
		}
		waitTime1 = c.getSelectLoop().getTotalWaitTime();
		workTime1 = c.getSelectLoop().getTotalWorkTime();
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertTrue("" + waitTime1 + ">" + workTime1, waitTime1 > workTime1);
		s.stop(TIMEOUT);
		
		//work > wait
		s = new Server(PORT);
		s.start();
		c = new Client(PORT);
		c.start();
		s.waitForSessionOpen(TIMEOUT);
		c.waitForSessionOpen(TIMEOUT);
		for (int i=0; i<10; ++i) {
			s.write(new Packet(PacketType.WRITE_AND_WAIT, "200"));
			waitFor(250);
		}
		waitTime1 = c.getSelectLoop().getTotalWaitTime();
		workTime1 = c.getSelectLoop().getTotalWorkTime();
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertTrue("" + waitTime1 + "<" + workTime1, waitTime1 < workTime1);
		s.stop(TIMEOUT);
    }
 }
