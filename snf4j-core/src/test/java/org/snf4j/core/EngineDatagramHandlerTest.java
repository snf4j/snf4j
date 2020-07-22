/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020 SNF4J contributors
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;

import org.junit.Test;
import org.snf4j.core.allocator.TestAllocator;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.logger.LoggerRecorder;
import org.snf4j.core.timer.DefaultTimeoutModel;
import org.snf4j.core.timer.DefaultTimer;
import org.snf4j.core.timer.ITimerTask;
import org.snf4j.core.timer.TestTimer;

public class EngineDatagramHandlerTest extends DTLSTest {

	String getReleased(int base, TestAllocator allocator) {
		StringBuffer sb = new StringBuffer();
		
		List<ByteBuffer> r = allocator.getReleased();
		for (int i=base; i<r.size(); ++i) {
			ByteBuffer b = r.get(i);
			
			sb.append(b.limit());
			sb.append(",");
			sb.append(b.capacity());
			sb.append("|");
		}
		return sb.toString();
	}
	
	String getReleasedIds(int base, TestAllocator allocator) {
		StringBuffer sb = new StringBuffer();
		
		List<ByteBuffer> r = allocator.getReleased();
		for (int i=base; i<r.size(); ++i) {
			sb.append(allocator.getBufferId(r.get(i)));
			sb.append("|");
		}
		return sb.toString();
	}
	
	void assertReleased(int base, TestAllocator a, int... expectedIds) {
		String ids = getReleasedIds(base, a);
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i<expectedIds.length; ++i) {
			sb.append(expectedIds[i]);
			sb.append("|");
		}
		assertEquals(sb.toString(), ids);
	}
	
	void prepareServerClient(boolean start) throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.timer = new DefaultTimer();
		s.ssl = true;
		s.testEngine = new TestDTLSEngine();
		s.allocator = new TestAllocator(false, true);
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.testEngine = new TestDTLSEngine();
		c.allocator = new TestAllocator(false, true);
		if (start) {
			s.startServer();
			c.startClient();
			assertReady(c, s);
		}
	}
	
	void stopServerClient() throws Exception {
		if (c != null) {
			c.stop(TIMEOUT);
			c = null;
		}
		if (s != null) {
			s.stop(TIMEOUT);
			s = null;
		}
	}
	
	@Test
	public void testWrap() throws Exception {
		prepareServerClient(true);
		
		int base = c.allocator.getReleasedCount();
		int id = c.allocator.getAllocatedCount();
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		assertEquals("3,3|103,1024|", getReleased(base, c.allocator));
		assertReleased(base, c.allocator, id, id+1);
		base += 2;
		
		c.testEngine.wrapException = new SSLException("");
		assertEquals("", getReleased(base, c.allocator));
		c.getSession().write(nop());
		c.waitForSessionEnding(TIMEOUT);
		waitFor(100);
		assertEquals(0, c.allocator.getSize());
		assertReleased(base, c.allocator, id+3, id+2, 1, 0);

		s.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		
		stopServerClient();
		prepareServerClient(false);
		c.allocator = new TestAllocator(false, false);
		s.startServer();
		c.startClient();
		assertReady(c, s);

		base = c.allocator.getReleasedCount();
		id = c.allocator.getAllocatedCount();
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		
		c.testEngine.wrapException = new SSLException("");
		assertEquals("", getReleased(base, c.allocator));
		c.getSession().write(nop());
		c.waitForSessionEnding(TIMEOUT);
		waitFor(100);
		assertEquals(0, c.allocator.getReleasedCount());

		s.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
	
		stopServerClient();
		prepareServerClient(true);
		
		c.testEngine.wrapException = new SSLException("");
		base = c.allocator.getReleasedCount();
		id = c.allocator.getAllocatedCount();
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertEquals(0, c.allocator.getSize());
		assertReleased(base, c.allocator, id, 1, 0);
	}

	@Test
	public void testWrapTwoPacketsInOneWrite() throws Exception {
		prepareServerClient(true);
		
		c.testEngine.wrapConsumed = 3;
		ByteBuffer bb = ByteBuffer.allocate(1024);
		bb.put(nop());
		bb.put(nop("1")).flip();
		int base = c.allocator.getReleasedCount();
		int id = c.allocator.getAllocatedCount();
		c.getSession().write(bb).sync(TIMEOUT);
		waitFor(50);
		assertEquals("DR|NOP()|DR|NOP(1)|", s.getRecordedData(true));
		assertEquals("7,7|103,1024|104,1024|", getReleased(base, c.allocator));
		assertReleased(base, c.allocator, id, id+1, id+2);
		
		c.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertEquals(0, c.allocator.getSize());
		
		stopServerClient();
		prepareServerClient(false);
		c.canOwnPasseData = true;
		s.startServer();
		c.startClient();
		assertReady(c, s);
		
		c.testEngine.wrapConsumed = 3;
		bb.clear();
		bb.put(nop());
		bb.put(nop("1")).flip();
		base = c.allocator.getReleasedCount();
		id = c.allocator.getAllocatedCount();
		c.getSession().write(bb).sync(TIMEOUT);
		waitFor(50);
		assertEquals("DR|NOP()|DR|NOP(1)|", s.getRecordedData(true));
		assertEquals("103,1024|104,1024|", getReleased(base, c.allocator));
		assertReleased(base, c.allocator, id, id+1);
		
		c.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertEquals(0, c.allocator.getSize());

		stopServerClient();
		prepareServerClient(false);
		c.allocator = new TestAllocator(false, false);
		s.startServer();
		c.startClient();
		assertReady(c, s);
		
		c.testEngine.wrapConsumed = 3;
		bb.clear();
		bb.put(nop());
		bb.put(nop("1")).flip();
		base = c.allocator.getReleasedCount();
		id = c.allocator.getAllocatedCount();
		c.getSession().write(bb).sync(TIMEOUT);
		waitFor(50);
		assertEquals("DR|NOP()|DR|NOP(1)|", s.getRecordedData(true));
		assertEquals("", getReleased(base, c.allocator));
		
		c.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertEquals(0, c.allocator.getReleasedCount());
		
	}
	
	@Test
	public void testWrapWithBufferOverflow() throws Exception {
		prepareServerClient(true);
		
		int base = c.allocator.getReleasedCount();
		int id = c.allocator.getAllocatedCount();
		c.testEngine.wrapResult = new SSLEngineResult(Status.BUFFER_OVERFLOW, HandshakeStatus.NOT_HANDSHAKING, 0, 0);
		c.testEngine.session.netBufferSize += 10;
		c.getSession().write(nop());
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP()|", s.getRecordedData(true));
		assertEquals("1024,1024|3,3|103,1034|", getReleased(base, c.allocator));
		assertReleased(base, c.allocator, id+1, id, id+2);
		base += 3;
		id += 3;
		c.testEngine.wrapResult = new SSLEngineResult(Status.BUFFER_OVERFLOW, HandshakeStatus.NOT_HANDSHAKING, 0, 0);
		c.getSession().write(nop());
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertReleased(base, c.allocator, id+1, id, 1, 0);
		assertEquals(0, c.allocator.getSize());

		stopServerClient();
		prepareServerClient(false);
		c.canOwnPasseData = true;
		s.startServer();
		c.startClient();
		assertReady(c, s);
		c.testEngine.wrapResult = new SSLEngineResult(Status.BUFFER_OVERFLOW, HandshakeStatus.NOT_HANDSHAKING, 0, 0);
		c.getSession().write(nop());
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertEquals(0, c.allocator.getSize());
		
		stopServerClient();
		prepareServerClient(false);
		c.allocator = new TestAllocator(false, false);
		s.startServer();
		c.startClient();
		assertReady(c, s);
		
		c.testEngine.wrapResult = new SSLEngineResult(Status.BUFFER_OVERFLOW, HandshakeStatus.NOT_HANDSHAKING, 0, 0);
		c.testEngine.session.netBufferSize += 10;
		c.getSession().write(nop());
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP()|", s.getRecordedData(true));
		c.testEngine.wrapResult = new SSLEngineResult(Status.BUFFER_OVERFLOW, HandshakeStatus.NOT_HANDSHAKING, 0, 0);
		c.getSession().write(nop());
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertEquals(0, c.allocator.getReleasedCount());
		
	}
	
	@Test
	public void testWrapClosed() throws Exception {
		prepareServerClient(false);
		c.waitForCloseMessage = true;
		c.testEngine = null;
		s.testEngine = null;
		s.startServer();
		c.startClient();
		assertReady(c, s);
		
		int base = c.allocator.getReleasedCount();
		int id = c.allocator.getAllocatedCount();
		c.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		if (c.engine instanceof TestDTLSEngine) {
			assertReleased(base, c.allocator, id, 1, 0);
		}
		else {
			assertReleased(base, c.allocator, id, id+1, 1, 0);
		}
		assertEquals(0, c.allocator.getSize());
		
		stopServerClient();
		prepareServerClient(false);
		c.waitForCloseMessage = true;
		c.testEngine = null;
		s.testEngine = null;
		s.startServer();
		c.startClient();
		assertReady(c, s);

		s.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertEquals(0, c.allocator.getSize());
		
		stopServerClient();
		prepareServerClient(false);
		c.waitForCloseMessage = true;
		c.timer = new TestTimer();
		c.handshakeTimeout = 3333;
		TestTimer t = (TestTimer)c.timer;
		s.startServer();
		c.startClient();
		assertReady(c, s);
		EngineDatagramHandler h = getHandler(c.getSession());
		t.getTrace(true);
		
		c.testEngine.addRecord("W|NU|-|-|C|-|");
		h.run(new org.snf4j.core.engine.HandshakeStatus[] {org.snf4j.core.engine.HandshakeStatus.NEED_WRAP});
		assertEquals("3333|", t.getTrace(true));

		c.testEngine.addRecord("W|NH|-|-|C|-|");
		h.run(new org.snf4j.core.engine.HandshakeStatus[] {org.snf4j.core.engine.HandshakeStatus.NEED_WRAP});
		assertEquals("", t.getTrace(true));

		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("c3333|", t.getTrace(true));
		assertEquals(0, t.getSize());
		
	}
	
	@Test
	public void testUnwrap() throws Exception {
		prepareServerClient(true);
		
		int base = c.allocator.getReleasedCount();
		s.getSession().write(nop());
		c.waitForDataRead(TIMEOUT);
		waitFor(50);
		assertEquals("", getReleased(base, c.allocator));
		
		c.testEngine.unwrapException = new SSLException("");
		s.getSession().write(nop());
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertEquals("1024,1024|103,1024|", getReleased(base, c.allocator));
		assertReleased(base, c.allocator, 1,0);
		assertEquals(0, c.allocator.getSize());
		
		stopServerClient();
		prepareServerClient(false);
		c.allocator = new TestAllocator(false, false);
		s.startServer();
		c.startClient();
		assertReady(c, s);
		
		s.getSession().write(nop());
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP()|", c.getRecordedData(true));
		c.testEngine.unwrapException = new SSLException("");
		s.getSession().write(nop());
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertEquals(0, c.allocator.getReleasedCount());
	}
	
	@Test
	public void testUnwrapTwoPacketsInOneRead() throws Exception {
		prepareServerClient(true);

		c.testEngine.unwrapConsumed = 3;
		ByteBuffer bb = ByteBuffer.allocate(1024);
		bb.put(nop());
		bb.put(TestDTLSEngine.PREAMBLE);
		bb.put(nop("1")).flip();
		int base = c.allocator.getReleasedCount();
		s.getSession().write(bb).sync(TIMEOUT);
		waitFor(50);
		assertEquals("DR|NOP()|NOP(1)|", c.getRecordedData(true));
		assertEquals("", getReleased(base, c.allocator));
	
		c.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertEquals(0, c.allocator.getSize());
	}
	
	List<String> getErrors(List<String> recording) {
		List<String> result = new ArrayList<String>();
		
		for (String s: recording) {
			if (s.startsWith("[ERROR]")) {
				result.add(s);
			}
		}
		return result;
	}
	
	@Test
	public void testUnwrapRead() throws Exception {
		prepareServerClient(false);
		c.codecPipeline = codec();
		c.incidentRecordException = true;
		s.startServer();
		c.startClient();
		assertReady(c, s);

		codec.decodeException = new Exception("E1");
		LoggerRecorder.enableRecording();
		s.getSession().write(nop()).sync(TIMEOUT);
		waitFor(50);
		List<String> recording = getErrors(LoggerRecorder.disableRecording());
		assertEquals(1, recording.size());
		assertTrue(recording.get(0).startsWith("[ERROR] " + SessionIncident.DECODING_PIPELINE_FAILURE.defaultMessage()));
		assertEquals("DR|DECODING_PIPELINE_FAILURE(E1)|", c.getRecordedData(true));
		assertEquals("DS|", s.getRecordedData(true));
		codec.decodeException = null;
		s.getSession().write(nop()).sync(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		waitFor(50);
		assertEquals("DR|NOP(d)|", c.getRecordedData(true));
		assertEquals("DS|", s.getRecordedData(true));
		
		c.incident = true;
		codec.decodeException = new Exception("E1");
		LoggerRecorder.enableRecording();
		s.getSession().write(nop()).sync(TIMEOUT);
		waitFor(50);
		recording = getErrors(LoggerRecorder.disableRecording());
		assertEquals(0, recording.size());
		assertEquals("DR|DECODING_PIPELINE_FAILURE(E1)|", c.getRecordedData(true));
		assertEquals("DS|", s.getRecordedData(true));
		codec.decodeException = null;
		s.getSession().write(nop()).sync(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		waitFor(50);
		assertEquals("DR|NOP(d)|", c.getRecordedData(true));
		assertEquals("DS|", s.getRecordedData(true));
		
		c.throwInRead = true;
		s.getSession().write(nop()).sync(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|NOP(d)|EXC|SCL|SEN|", c.getRecordedData(true));
		waitFor(50);
		assertEquals("DS|", s.getRecordedData(true));
		assertEquals(0, c.allocator.getSize());
		
	}
	
	@Test
	public void testRead() throws Exception {
		prepareServerClient(false);
		s.waitForCloseMessage = true;
		s.startServer();
		c.startClient();
		assertReady(c, s);
		
		s.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);

		EngineDatagramHandler h = getHandler(c.getSession());
		assertTrue(c.getSession() == h.getSession());
		h.preCreated();
		h.read(new byte[1]);
		Field f = EngineDatagramHandler.class.getDeclaredField("inNetBuffers");
		f.setAccessible(true);
		@SuppressWarnings("unchecked")
		Queue<ByteBuffer> queue = (Queue<ByteBuffer>) f.get(h); 
		assertEquals(0, queue.size());
		h.read(null, (Object)null);
		h.event(null, null, 100);
	}
	
	@Test
	public void testWrite() throws Exception {
		prepareServerClient(true);
		EngineDatagramHandler h = getHandler(c.getSession());
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertNull(h.write(null, false));
		assertTrue(h.write(null, true).isCancelled());
	}
	
	@Test
	public void testUnwrapWithBufferOverflow() throws Exception {
		prepareServerClient(true);
		
		int base = c.allocator.getReleasedCount();
		int id = c.allocator.getAllocatedCount();
		c.testEngine.unwrapResult = new SSLEngineResult(Status.BUFFER_OVERFLOW, HandshakeStatus.NOT_HANDSHAKING, 0, 0);
		c.testEngine.session.appBufferSize += 10;
		s.getSession().write(nop());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP()|", c.getRecordedData(true));
		assertEquals("1024,1024|", getReleased(base, c.allocator));
		assertReleased(base, c.allocator, 1);
		base++;
		c.testEngine.unwrapResult = new SSLEngineResult(Status.BUFFER_OVERFLOW, HandshakeStatus.NOT_HANDSHAKING, 0, 0);
		s.getSession().write(nop());
		s.waitForDataSent(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertReleased(base, c.allocator, id, 0);
		assertEquals(0, c.allocator.getSize());
		
		stopServerClient();
		prepareServerClient(false);
		c.allocator = new TestAllocator(false, false);
		s.startServer();
		c.startClient();
		assertReady(c, s);
		
		c.testEngine.unwrapResult = new SSLEngineResult(Status.BUFFER_OVERFLOW, HandshakeStatus.NOT_HANDSHAKING, 0, 0);
		c.testEngine.session.appBufferSize += 10;
		s.getSession().write(nop());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP()|", c.getRecordedData(true));
		c.testEngine.unwrapResult = new SSLEngineResult(Status.BUFFER_OVERFLOW, HandshakeStatus.NOT_HANDSHAKING, 0, 0);
		s.getSession().write(nop());
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertEquals(0, c.allocator.getReleasedCount());
	}	
	
	EngineDatagramHandler getHandler(DatagramSession session) throws Exception {
		Field wrapper = EngineDatagramSession.class.getDeclaredField("wrapper");
		Field internal = EngineDatagramWrapper.class.getDeclaredField("internal");
		
		wrapper.setAccessible(true);
		internal.setAccessible(true);
		return (EngineDatagramHandler) internal.get(wrapper.get(session));
	}
	
	@Test
	public void testUnwrapWithBufferUnderflow() throws Exception {
		prepareServerClient(true);
		
		c.testEngine.unwrapResult = new SSLEngineResult(Status.BUFFER_UNDERFLOW, HandshakeStatus.NOT_HANDSHAKING, 0, 0);
		s.getSession().write(nop());
		waitFor(50);
		assertEquals("DR|", c.getRecordedData(true));
		s.getSession().write(nop("2"));
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(2)|", c.getRecordedData(true));
		
		c.testEngine.unwrapResult = new SSLEngineResult(Status.BUFFER_UNDERFLOW, HandshakeStatus.NEED_UNWRAP, 0, 0);
		s.getSession().write(nop());
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|EXC|SCL|SEN|", c.getRecordedData(true));
		waitFor(50);
		assertEquals(0, c.allocator.getSize());
		
		stopServerClient();
		prepareServerClient(true);
		
		EngineDatagramHandler h = getHandler(c.getSession());
		org.snf4j.core.engine.HandshakeStatus[] status = new org.snf4j.core.engine.HandshakeStatus[1];
		status[0] = org.snf4j.core.engine.HandshakeStatus.NEED_UNWRAP_AGAIN;
		
		c.testEngine.unwrapResult = new SSLEngineResult(Status.BUFFER_UNDERFLOW, HandshakeStatus.NOT_HANDSHAKING, 0, 0);
		int base = c.allocator.getReleasedCount();
		h.unwrap(status);
		assertEquals("", getReleased(base, c.allocator));
		s.getSession().write(nop());
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP()|", c.getRecordedData(true));
		
	}	
	
	@Test
	public void testTimer() throws Exception {
		prepareServerClient(false);
		s.waitForCloseMessage = true;
		c.timer = new DefaultTimer();
		s.startServer();
		c.startClient();
		assertReady(c, s);
		
		c.getSession().getTimer().scheduleEvent("ff", 50);
		waitFor(10);
		assertEquals("", c.getRecordedData(true));
		waitFor(50);
		assertEquals("TIM;ff|", c.getRecordedData(true));
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.startServer();
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.timer = new DefaultTimer();
		c.handshakeTimeout = 100;
		c.startClient();
		c.waitForSessionEnding(TIMEOUT);
	}
	
	@Test
	public void testScheduleAlreadyRunningTimer() throws Exception {
		prepareServerClient(false);
		c.timer = new TestTimer();
		c.timeoutModel = new DefaultTimeoutModel(444,50000);
		c.handshakeTimeout = 4998;
		c.waitForCloseMessage = true;
		c.testEngine.handshakingAfterClose = true;
		TestTimer t = (TestTimer)c.timer;
		s.startServer();
		c.startClient();
		assertReady(c, s);
		t.getTrace(true);
		c.getRecordedData(true);
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|DR|SCL|SEN|", c.getRecordedData(true));
		assertEquals("444|4998|c444|c4998|", t.getTrace(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		prepareServerClient(false);
		c.timer = new TestTimer();
		c.handshakeTimeout = 4998;
		c.waitForCloseMessage = true;
		c.testEngine.handshakingAfterClose = true;
		t = (TestTimer)c.timer;
		s.startServer();
		c.startClient();
		assertReady(c, s);
		t.getTrace(true);
		c.getRecordedData(true);
		
		Runnable r = new Runnable() {

			@Override
			public void run() {
			}
		};
		
		ITimerTask task = new DefaultTimer().schedule(r, 1000);
		task.cancelTask();

		EngineDatagramHandler h = getHandler(c.getSession());
		Field f = EngineDatagramHandler.class.getDeclaredField("handshakeTimer");
		f.setAccessible(true);
		f.set(h, task);
		f = EngineDatagramHandler.class.getDeclaredField("retransmissionTimer");
		f.setAccessible(true);
		f.set(h, task);		
		
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|DR|SCL|SEN|", c.getRecordedData(true));
		assertEquals("", t.getTrace(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}
	
	@Test
	public void testRetransmission() throws Exception {
		prepareServerClient(false);
		c.handshakeTimeout = 2000;
		c.waitForCloseMessage = true;
		c.testEngine.handshakingAfterClose = true;
		c.timer = new TestTimer();
		
		TestTimer t = (TestTimer)c.timer;
		s.startServer();
		c.startClient();
		assertReady(c, s);
		s.getSession().dirtyClose();
		s.waitForSessionEnding(TIMEOUT);
		t.getTrace(true);
		c.testEngine.addRecord("U|NU|-|-|OK|-|");
		c.testEngine.addRecord("U|NH|-|-|OK|F|");
		EngineDatagramHandler h = getHandler(c.getSession());
		h.handshaking = true;
		h.run(new org.snf4j.core.engine.HandshakeStatus[] {org.snf4j.core.engine.HandshakeStatus.NEED_UNWRAP_AGAIN});
		waitFor(100);
		assertEquals("1000|", t.getTrace(true));
		waitFor(1000);
		assertEquals("1000|", t.getExpired());
		c.getSession().dirtyClose();
		c.waitForSessionEnding(TIMEOUT);
	}	
}
