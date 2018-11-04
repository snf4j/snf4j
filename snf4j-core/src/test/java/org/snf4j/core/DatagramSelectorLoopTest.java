/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2018 SNF4J contributors
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.session.ISession;

public class DatagramSelectorLoopTest {
	final long TIMEOUT = 2000;
	final int PORT = 7778;
	final long GET_SIZE_DELAY = 200;
	
	DatagramHandler c;
	DatagramHandler s;
	
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

	@Test
	public void testStopOpenSession() throws Exception {
		DatagramHandler c2 = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		s = new DatagramHandler(PORT);
		
		s.startServer();
		c.startClient();
		c2.start(true, s.loop);
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		c2.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c2.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		waitFor(GET_SIZE_DELAY);
		assertEquals(2, s.loop.getSize());
		assertEquals(1, c.loop.getSize());
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		waitFor(GET_SIZE_DELAY);
		try {
			c.loop.getSize();
			fail("the size should not be returned");
		}
		catch (ClosedSelectorException e) {}
		
		s.quickStop(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c2.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c2.getRecordedData(true));
		waitFor(GET_SIZE_DELAY);
		try {
			s.loop.getSize();
			fail("the size should not be returned");
		}
		catch (ClosedSelectorException e) {}

		
		//gentle close with written data
		c = new DatagramHandler(PORT);
		s = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		c.write(new Packet(PacketType.WRITE_AND_WAIT, "1000"));
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|$WRITE_AND_WAIT(1000)|DS|", s.getRecordedData(true));
		s.stop(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
	}
	
	@Test
	public void testStopOpenSessionWithSuspendedWrite() throws Exception {
		c = new DatagramHandler(PORT);
		s = new DatagramHandler(PORT);
		
		s.startServer();
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		//quick stop
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		c.getSession().suspendWrite();
		c.getSession().write(new Packet(PacketType.ECHO, "X").toBytes());
		waitFor(1000);
		c.quickStop(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		
		//gentle stop 
		c = new DatagramHandler(PORT);
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		c.getSession().suspendWrite();
		c.getSession().write(new Packet(PacketType.ECHO, "X").toBytes());
		waitFor(1000);
		c.stop(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		waitFor(1000);
		assertEquals("", s.getRecordedData(true));
		c = null;
	}

	@Test
	public void testStopOpenSessionWithSuspendedRead() throws Exception {
		c = new DatagramHandler(PORT);
		s = new DatagramHandler(PORT);
		
		s.startServer();
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		
		//quick stop
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		c.getSession().suspendRead();
		c.quickStop(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));

		//gentle stop
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		c.getSession().suspendRead();
		c.stop(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		
	}

	@Test
	public void testStopOpenSessionWithSuspendedBoth() throws Exception {
		c = new DatagramHandler(PORT);
		s = new DatagramHandler(PORT);
		
		s.startServer();
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		
		//quick stop
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		c.getSession().suspendRead();
		c.getSession().suspendWrite();
		c.quickStop(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));

		//gentle stop
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		c.getSession().suspendRead();
		c.getSession().suspendWrite();
		c.stop(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		
	}

	@Test
	public void testCloseStoppedAndEmpty() throws Exception {
		SelectorLoop loop1 = new SelectorLoop();
		SelectorLoop loop2 = new SelectorLoop("Name");
		
		assertEquals(loop1.getId()+1, loop2.getId());
		assertEquals("SelectorLoop-"+loop1.getId(), loop1.toString());
		assertEquals("SelectorLoop-Name", loop2.toString());
		assertEquals("SelectorLoop-"+loop1.getId(), loop1.getName());
		assertEquals("Name", loop2.getName());
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
		
		IDatagramHandler h = new TestDatagramHandler();
		
		try {
			loop1.register(null, (IDatagramHandler)null);
			fail ("handler cannot be null");
		}
		catch (IllegalArgumentException e) {
			assertEquals("handler is null", e.getMessage());
		}
		
		try {
			loop1.register(null, (DatagramSession)null);
			fail ("session cannot be null");
		}
		catch (IllegalArgumentException e) {
			assertEquals("session is null", e.getMessage());
		}
		
		try {
			loop1.register(null, h);
			fail ("channel cannot be null");
		}
		catch (IllegalArgumentException e) {
			assertEquals("channel is null", e.getMessage());
		}
		
		DatagramChannel dc = DatagramChannel.open();
		try {
			loop1.register(dc, SelectionKey.OP_ACCEPT, h);
			fail ("options have to be valid");
		}
		catch (IllegalArgumentException e) {
			assertEquals("invalid options 16", e.getMessage());
		}
		
		loop1.stopping = true;
		try {
			loop1.register(dc, h);
			fail("loop cannot be is stopping state");
		}
		catch (SelectorLoopStoppingException e) {}
		loop1.stopping = false;
		
		loop1.stop();
		try {
			loop1.register(dc, h);
			fail("loop have to be open");
		}
		catch (ClosedSelectorException e) {}
				
	}
	
	@Test
	public void testRegistrationBeforeStart() throws Exception {
		DatagramHandler s = new DatagramHandler(PORT);
		DatagramHandler c = new DatagramHandler(PORT);
		
		s.start(false, true, null);
		c.start(true, true, null);
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		c.write(new Packet(PacketType.ECHO));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|$ECHO()|DS|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE()|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertEquals("SCL|SEN|", c.getRecordedData(true));
	}
	
	@Test
	public void testRegistrationWhileStopping() throws Exception {
		DatagramHandler s = new DatagramHandler(PORT);

		//register not connected channel (session not created)
		s.setThreadFactory(new DelayedThreadFactory(100));
		s.startServer();
		s.stop(TIMEOUT);
		assertEquals("",s.getRecordedData(true));
		
		s = new DatagramHandler(PORT);
		s.setThreadFactory(new DelayedThreadFactory(100));
		s.registerConnectedSession = true;
		s.startServer();
		s.stop(TIMEOUT);
		assertEquals("SCR|SEN|",s.getRecordedData(true));

		//register connected channel (session not created)
		DatagramHandler c = new DatagramHandler(PORT);
		c.setThreadFactory(new DelayedThreadFactory(100));
		c.startClient();
		c.stop(TIMEOUT);
		assertEquals("",c.getRecordedData(true));

		//register connected channel (session created)
		c = new DatagramHandler(PORT);
		c.setThreadFactory(new DelayedThreadFactory(100));
		c.registerConnectedSession = true;
		c.startClient();
		c.stop(TIMEOUT);
		assertEquals("SCR|SEN|",c.getRecordedData(true));
	}
	
	@Test
	public void testRegistrationOfRegistered() throws Exception {
		DatagramHandler s = new DatagramHandler(PORT);
		
		s.startServer();
		s.waitForSessionReady(TIMEOUT);
		s.loop.register((DatagramChannel)s.getSession().channel, s.getSession());
		waitFor(GET_SIZE_DELAY);
		assertEquals(1, s.loop.getSize());
		s.stop(TIMEOUT);
	}	
	
	@Test
	public void testClosingAction() throws Exception {
		DatagramHandler s = new DatagramHandler(PORT);

		//default action
		s.startClient();
		s.waitForSessionReady(TIMEOUT);
		s.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		assertTrue(s.loop.isOpen());
		s.stop(TIMEOUT);
		
		//stop with one session in the loop
		s = new DatagramHandler(PORT);
		s.endingAction = EndingAction.STOP;
		s.startClient();
		s.waitForSessionReady(TIMEOUT);
		s.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		assertTrue(s.loop.join(TIMEOUT));
		assertFalse(s.loop.isOpen());
		
		//quick stop with one session in the loop
		s = new DatagramHandler(PORT);
		s.endingAction = EndingAction.QUICK_STOP;
		s.startClient();
		s.waitForSessionReady(TIMEOUT);
		s.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		assertTrue(s.loop.join(TIMEOUT));
		assertFalse(s.loop.isOpen());

		//stop with more sessions in the loop
		s = new DatagramHandler(PORT);
		s.endingAction = EndingAction.STOP;
		s.startClient();
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		DatagramSession session1 = s.getSession();
		s.endingAction = EndingAction.DEFAULT;
		s.port = PORT+1;
		s.start(true, s.loop);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		session1.close();
		waitFor(500);
		assertEquals("SCL|SEN|SCL|SEN|", s.getRecordedData(true));
		assertTrue(s.loop.join(TIMEOUT));
		assertFalse(s.loop.isOpen());
		
		//quick stop with more sessions in the loop
		s = new DatagramHandler(PORT);
		s.endingAction = EndingAction.QUICK_STOP;
		s.startClient();
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		session1 = s.getSession();
		s.endingAction = EndingAction.DEFAULT;
		s.port = PORT+1;
		s.start(true, s.loop);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		session1.close();
		waitFor(500);
		assertEquals("SCL|SEN|SCL|SEN|", s.getRecordedData(true));
		assertTrue(s.loop.join(TIMEOUT));
		assertFalse(s.loop.isOpen());

		//stop when empty with one session in the loop
		s = new DatagramHandler(PORT);
		s.endingAction = EndingAction.STOP_WHEN_EMPTY;
		s.startClient();
		s.waitForSessionReady(TIMEOUT);
		s.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		assertTrue(s.loop.join(TIMEOUT));
		assertFalse(s.loop.isOpen());

		//stop when empty with more sessions in the loop
		s = new DatagramHandler(PORT);
		s.endingAction = EndingAction.STOP_WHEN_EMPTY;
		s.startClient();
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		session1 = s.getSession();
		s.endingAction = EndingAction.DEFAULT;
		s.port = PORT+1;
		s.start(true, s.loop);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		session1.close();
		waitFor(500);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertTrue(s.loop.isOpen());
		s.getSession().close();
		waitFor(500);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertFalse(s.loop.isOpen());
	}
	
	void assertNotSuccessfulSessionFutures(ISession session, Throwable cause, String futuresStates) {
		@SuppressWarnings("unchecked")
		IFuture<Void>[] futures = new IFuture[5];
		
		futures[0] = session.getCreateFuture();
		futures[1] = session.getOpenFuture();
		futures[2] = session.getReadyFuture();
		futures[3] = session.getCloseFuture();
		futures[4] = session.getEndFuture();
		
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
	public void testExceptionDuringRegistration() throws Exception {
    	TestSelectorFactory factory = new TestSelectorFactory();
    	factory.testSelectorCounter = 1;
    	factory.delegateCloseSelector = true;

		//closed selector exception
    	SelectorLoop loop = new SelectorLoop("loop", null, factory);
		DatagramChannel channel = DatagramChannel.open();
		channel.configureBlocking(true);
		channel.socket().bind(new InetSocketAddress(PORT));
		TestDatagramHandler h1 = new TestDatagramHandler();
		h1.createException = new NullPointerException();
		IFuture<Void> f1 = loop.register(channel, h1);
		channel = DatagramChannel.open();
		channel.configureBlocking(true);
		channel.socket().bind(new InetSocketAddress(PORT+1));
		TestDatagramHandler h2 = new TestDatagramHandler();
		IFuture<Void> f2 = loop.register(channel, h2);
		loop.start();
		assertTrue(f1.await(TIMEOUT).isCancelled());
		assertEquals("", h1.getEventLog());
		assertNotSuccessfulSessionFutures(f1.getSession(), null, "CCCCC");
		assertTrue(f2.await(TIMEOUT).isCancelled());
		assertEquals("", h2.getEventLog());
		assertNotSuccessfulSessionFutures(f1.getSession(), null, "CCCCC");
		loop.stop();
		assertTrue(loop.join(TIMEOUT));

		//closed selector exception (for JDK1.6)
    	factory = new TestSelectorFactory();
    	factory.testSelectorCounter = 1;
    	factory.delegateCloseSelector = true;
    	factory.delegateCloseSelectorWithNullPointerException = true;
    	loop = new SelectorLoop("loop", null, factory);
		channel = DatagramChannel.open();
		channel.configureBlocking(true);
		channel.socket().bind(new InetSocketAddress(PORT));
		h1 = new TestDatagramHandler();
		h1.createException = new NullPointerException();
		f1 = loop.register(channel, h1);
		channel = DatagramChannel.open();
		channel.configureBlocking(true);
		channel.socket().bind(new InetSocketAddress(PORT+1));
		h2 = new TestDatagramHandler();
		f2 = loop.register(channel, h2);
		loop.start();
		assertTrue(f1.await(TIMEOUT).isCancelled());
		assertEquals("", h1.getEventLog());
		assertNotSuccessfulSessionFutures(f1.getSession(), null, "CCCCC");
		assertTrue(f2.await(TIMEOUT).isCancelled());
		assertEquals("", h2.getEventLog());
		assertNotSuccessfulSessionFutures(f1.getSession(), null, "CCCCC");
		loop.stop();
		assertTrue(loop.join(TIMEOUT));
		
		//thrown exception
    	factory = new TestSelectorFactory();
    	factory.testSelectorCounter = 1;
    	factory.delegateException = true;
    	factory.delegateExceptionCounter = 1;
    	loop = new SelectorLoop("loop", null, factory);
		channel = DatagramChannel.open();
		channel.configureBlocking(true);
		channel.socket().bind(new InetSocketAddress(PORT));
		h1 = new TestDatagramHandler();
		h1.createException = new NullPointerException();
		f1 = loop.register(channel, h1);
		channel = DatagramChannel.open();
		channel.configureBlocking(true);
		channel.socket().bind(new InetSocketAddress(PORT+1));
		h2 = new TestDatagramHandler();
		f2 = loop.register(channel, h2);
		loop.start();
		assertTrue(f1.await(TIMEOUT).isFailed());
		assertEquals("", h1.getEventLog());
		assertNotSuccessfulSessionFutures(f1.getSession(), null, "FFFFF");
		assertTrue(f2.await(TIMEOUT).isSuccessful());
		assertEquals("CR|OP|RD|", h2.getEventLog());
		assertNotSuccessfulSessionFutures(f2.getSession(), null, "SSSNN");
		loop.stop();
		assertTrue(loop.join(TIMEOUT));
    	
		//registration of registered channel
		loop = new SelectorLoop();
		channel = DatagramChannel.open();
		channel.configureBlocking(true);
		channel.socket().bind(new InetSocketAddress(PORT));
		f1 = loop.register(channel, new TestDatagramHandler());
		h2 = new TestDatagramHandler();
		f2 = loop.register(channel, h2);
		loop.start();
		f1.sync(TIMEOUT);
		assertTrue(f2.await(TIMEOUT).isCancelled());
		assertNotSuccessfulSessionFutures(f2.getSession(), null, "CCCCC");
		assertEquals("", h2.getEventLog());
		loop.stop();
		assertTrue(loop.join(TIMEOUT));
		
		//registration when loop is closing
		loop = new SelectorLoop();
		loop.setThreadFactory(new DelayedThreadFactory(500));
		channel = DatagramChannel.open();
		channel.configureBlocking(true);
		channel.socket().bind(new InetSocketAddress(PORT));
		h1 = new TestDatagramHandler();
		f1 = loop.register(channel, h1);
		loop.start();
		waitFor(100);
		loop.stop();
		assertTrue(f1.await(TIMEOUT).isCancelled());
		assertNotSuccessfulSessionFutures(f1.getSession(), null, "CCCCC");
		assertEquals("", h1.getEventLog());
		loop.stop();
		assertTrue(loop.join(TIMEOUT));
		
	}
}
