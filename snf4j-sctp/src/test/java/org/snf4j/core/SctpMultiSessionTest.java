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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.snf4j.core.future.CancelledFuture;
import org.snf4j.core.future.FailedFuture;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.future.SuccessfulFuture;
import org.snf4j.core.future.TaskFuture;
import org.snf4j.core.handler.SctpSendingFailureException;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.logger.LoggerRecorder;
import org.snf4j.core.session.ISctpSession;

import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

public class SctpMultiSessionTest extends SctpTest {

	SctpClient ms;
	
	SctpClient mc;
	
	@After
	public void after2() throws Exception {
		if (ms != null) {
			if (ms.session != null) {
				ms.session.dirtyClose();
			}
			ms.stop(TIMEOUT);
		}
		if (mc != null) {
			if (mc.session != null) {
				mc.session.dirtyClose();
			}
			mc.stop(TIMEOUT);
		}
	}
	
	void assertFinished(SctpServer... servers) {
		for (SctpServer server: servers) {
			assertEquals(ClosingState.FINISHED, server.session.closing);
		}
	}
	
	@Test
	public void testGetParent() throws Exception {
		assumeSupported();
		ms = new SctpClient(PORT);
		ms.traceNotification = true;
		ms.startMulti();
		ms.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", ms.getTrace());		
		assertNull(ms.session.getParent());
	}
	
	@Test
	public void testCloseWithNoAssociations() throws Exception {
		assumeSupported();
		ms = new SctpClient(PORT);
		ms.traceNotification = true;
		ms.startMulti();
		ms.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", ms.getTrace());		
		ms.session.close();
		ms.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", ms.getTrace());
		assertFinished(ms);
		ms.stop(TIMEOUT);

		ms = new SctpClient(PORT);
		ms.traceNotification = true;
		ms.startMulti();
		ms.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", ms.getTrace());		
		ms.session.quickClose();
		ms.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", ms.getTrace());
		assertFinished(ms);
		ms.stop(TIMEOUT);

		ms = new SctpClient(PORT);
		ms.traceNotification = true;
		ms.startMulti();
		ms.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", ms.getTrace());		
		ms.session.dirtyClose();
		ms.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", ms.getTrace());
		assertFinished(ms);
		ms.stop(TIMEOUT);
	}

	public void testCloseWithAssociations(StoppingType type) throws Exception {
		ms = new SctpClient(PORT);
		ms.traceNotification = true;
		ms.startMulti();
		ms.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", ms.getTrace());		
	
		c = new SctpClient(PORT);
		c.traceNotification = true;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		c.waitForNotification("ASC", TIMEOUT);
		assertEquals("SCR|SOP|RDY|ASC|", c.getTrace());
		ms.waitForNotification("ASC", TIMEOUT);
		assertEquals("ASC|", ms.getTrace());
		Association a1 = ms.association(0);
		
		s = new SctpClient(PORT);
		s.traceNotification = true;
		s.start();
		s.waitForSessionReady(TIMEOUT);
		s.waitForNotification("ASC", TIMEOUT);
		assertEquals("SCR|SOP|RDY|ASC|", s.getTrace());
		ms.waitForNotification("ASC", TIMEOUT);
		assertEquals("ASC|", ms.getTrace());
		Association a2 = ms.association(1);
		
		assertEquals(2, ms.smc.associations().size());
		
		MessageInfo msgInfo1 = MessageInfo.createOutgoing(a1, null, 1);
		MessageInfo msgInfo2 = MessageInfo.createOutgoing(a2, null, 1);
		sleepLoop(ms.loop, 100);
		ms.session.writenf(nopb("123456"), ImmutableSctpMessageInfo.create(msgInfo1));
		ms.session.writenf(nopb("567"), ImmutableSctpMessageInfo.create(msgInfo2));
		switch (type) {
		case GENTLE:
			ms.session.close();
			ms.session.writenf(nopb("78"), ImmutableSctpMessageInfo.create(msgInfo1));
			ms.session.writenf(nopb("11"), ImmutableSctpMessageInfo.create(msgInfo2));
			break;
			
		case QUICK:
			ms.session.quickClose();
			break;
			
		case DIRTY:
			ms.session.dirtyClose();
			break;
			
		}
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		ms.waitForSessionEnding(TIMEOUT);
		assertFinished(s,c,ms);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		ms.stop(TIMEOUT);
	}
	
	@Test
	public void testCloseWithAssociations() throws Exception {
		assumeSupported();
		testCloseWithAssociations(StoppingType.GENTLE);
		assertEquals("DR|NOP(123456)[1]|SHT|ASC|SCL|SEN|", c.getTrace());
		assertEquals("DR|NOP(567)[1]|SHT|ASC|SCL|SEN|", s.getTrace());
		assertEquals("DS|ASC|ASC|SCL|SEN|", ms.getTrace());
		testCloseWithAssociations(StoppingType.QUICK);
		assertEquals("SHT|ASC|SCL|SEN|", c.getTrace());
		assertEquals("SHT|ASC|SCL|SEN|", s.getTrace());
		assertEquals("ASC|ASC|SCL|SEN|", ms.getTrace());
		testCloseWithAssociations(StoppingType.DIRTY);
		assertEquals("SHT|ASC|SCL|SEN|", c.getTrace());
		assertEquals("SHT|ASC|SCL|SEN|", s.getTrace());
		assertEquals("SCL|SEN|", ms.getTrace());
	}
	
	@Test
	public void testCloseMultiBySingle() throws Exception {
		assumeSupported();
		ms = new SctpClient(PORT);
		ms.traceNotification = true;
		ms.startMulti();
		ms.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", ms.getTrace());
		
		c = new SctpClient(PORT);
		c.traceNotification = true;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		c.waitForNotification("ASC", TIMEOUT);
		assertEquals("SCR|SOP|RDY|ASC|", c.getTrace());
		ms.waitForNotification("ASC", TIMEOUT);
		assertEquals("ASC|", ms.getTrace());
		Association a = ms.association(0);
		assertEquals("COMM_UP", ms.ASC(0).event().name());
		assertEquals("COMM_UP", c.ASC(0).event().name());
		
		c.session.writenf(nopb("1"));
		c.waitForDataSent(TIMEOUT);
		ms.waitForDataRead(TIMEOUT);
		assertEquals("DS|", c.getTrace());
		assertEquals("DR|NOP(1)|", ms.getTrace());
		
		MessageInfo msgInfo = MessageInfo.createOutgoing(a, null, 1);
		ms.session.writenf(nopb("2"), ImmutableSctpMessageInfo.create(msgInfo));
		ms.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|", ms.getTrace());
		assertEquals("DR|NOP(2)[1]|", c.getTrace());
		
		ms.clearNotifications();
		c.clearNotifications();
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("ASC|SCL|SEN|", c.getTrace());
		ms.waitForNotification("ASC", TIMEOUT);
		assertEquals("SHT|ASC|", ms.getTrace());
		assertEquals("SHUTDOWN", ms.ASC(1).event().name());
		assertFinished(c);
		c.stop(TIMEOUT);
		
		c = new SctpClient(PORT);
		c.traceNotification = true;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		c.waitForNotification("ASC", TIMEOUT);
		assertEquals("SCR|SOP|RDY|ASC|", c.getTrace());
		ms.waitForNotification("ASC", TIMEOUT);
		assertEquals("ASC|", ms.getTrace());

		c.session.writenf(nopb("1"));
		ms.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|", c.getTrace());
		assertEquals("DR|NOP(1)|", ms.getTrace());
		
		ms.clearNotifications();
		c.clearNotifications();
		c.session.quickClose();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("ASC|SCL|SEN|", c.getTrace());
		ms.waitForNotification("ASC", TIMEOUT);
		assertEquals("SHT|ASC|", ms.getTrace());
		assertEquals("SHUTDOWN", ms.ASC(1).event().name());
		assertFinished(c);
		c.stop(TIMEOUT);

		c = new SctpClient(PORT);
		c.traceNotification = true;
		c.start();
		c.waitForSessionReady(TIMEOUT);
		c.waitForNotification("ASC", TIMEOUT);
		assertEquals("SCR|SOP|RDY|ASC|", c.getTrace());
		ms.waitForNotification("ASC", TIMEOUT);
		assertEquals("ASC|", ms.getTrace());

		c.session.writenf(nopb("1"));
		ms.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|", c.getTrace());
		assertEquals("DR|NOP(1)|", ms.getTrace());
		
		ms.clearNotifications();
		c.clearNotifications();
		c.session.dirtyClose();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getTrace());
		ms.waitForNotification("ASC", TIMEOUT);
		assertEquals("SHT|ASC|", ms.getTrace());
		assertEquals("SHUTDOWN", ms.ASC(1).event().name());
		assertFinished(c);
		c.stop(TIMEOUT);
		
	}
	
	@Test
	public void testCloseMultiByMulti() throws Exception {
		assumeSupported();
		ms = new SctpClient(PORT);
		ms.traceNotification = true;
		ms.startMulti();
		ms.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", ms.getTrace());
		
		mc = new SctpClient(PORT+1);
		mc.traceNotification = true;
		mc.startMulti();
		mc.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", mc.getTrace());
		
		MessageInfo msgInfo = MessageInfo.createOutgoing(address(PORT), 1);
		mc.session.write(nopb("1234"), ImmutableSctpMessageInfo.create(msgInfo));
		mc.waitForDataSent(TIMEOUT);
		mc.waitForNotification("ASC", TIMEOUT);
		ms.waitForDataRead(TIMEOUT);
		assertEquals("DS|ASC|", mc.getTrace());
		assertEquals("ASC|DR|NOP(1234)[1]|", ms.getTrace());
		
		sleepLoop(mc.loop, 100);
		mc.session.write(nopb("456"), ImmutableSctpMessageInfo.create(msgInfo));
		mc.session.close();
		mc.session.write(nopb("7"), ImmutableSctpMessageInfo.create(msgInfo));
		mc.waitForSessionEnding(TIMEOUT);
		ms.waitForDataRead(TIMEOUT);
		assertEquals("DS|ASC|SCL|SEN|", mc.getTrace());
		assertEquals("DR|NOP(456)[1]|SHT|ASC|", ms.getTrace());
		assertFinished(mc);
		mc.stop(TIMEOUT);
		
		mc = new SctpClient(PORT+1);
		mc.traceNotification = true;
		mc.startMulti();
		mc.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", mc.getTrace());
		
		mc.session.write(nopb("1234"), ImmutableSctpMessageInfo.create(msgInfo));
		mc.waitForDataSent(TIMEOUT);
		mc.waitForNotification("ASC", TIMEOUT);
		ms.waitForDataRead(TIMEOUT);
		assertEquals("DS|ASC|", mc.getTrace());
		assertEquals("ASC|DR|NOP(1234)[1]|", ms.getTrace());
		
		sleepLoop(mc.loop, 100);
		mc.session.write(nopb("456"), ImmutableSctpMessageInfo.create(msgInfo));
		mc.session.quickClose();
		mc.session.write(nopb("7"), ImmutableSctpMessageInfo.create(msgInfo));
		mc.waitForSessionEnding(TIMEOUT);
		ms.waitForNotification("ASC", TIMEOUT);
		assertEquals("ASC|SCL|SEN|", mc.getTrace());
		assertEquals("SHT|ASC|", ms.getTrace());
		assertFinished(mc);
		mc.stop(TIMEOUT);

		mc = new SctpClient(PORT+1);
		mc.traceNotification = true;
		mc.startMulti();
		mc.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", mc.getTrace());
		
		mc.session.write(nopb("1234"), ImmutableSctpMessageInfo.create(msgInfo));
		mc.waitForDataSent(TIMEOUT);
		mc.waitForNotification("ASC", TIMEOUT);
		ms.waitForDataRead(TIMEOUT);
		assertEquals("DS|ASC|", mc.getTrace());
		assertEquals("ASC|DR|NOP(1234)[1]|", ms.getTrace());
		
		sleepLoop(mc.loop, 100);
		mc.session.write(nopb("456"), ImmutableSctpMessageInfo.create(msgInfo));
		mc.session.dirtyClose();
		mc.waitForSessionEnding(TIMEOUT);
		ms.waitForNotification("ASC", TIMEOUT);
		assertEquals("SCL|SEN|", mc.getTrace());
		assertEquals("SHT|ASC|", ms.getTrace());
		assertFinished(mc);
		mc.stop(TIMEOUT);
	}

	SctpClient startMulti(int port) throws Exception {
		SctpClient ms = new SctpClient(port);
		ms.traceNotification = true;
		ms.traceIncident = true;
		ms.startMulti();
		ms.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", ms.getTrace());
		return ms;
	}
	
	long time;
	
	void setTime() {
		time = System.currentTimeMillis();
	}
	
	void assertTime(long max) {
		assertTrue(System.currentTimeMillis() - time < max);
		setTime();
	}
	
	@Test
	public void testIncident() throws Exception {
		assumeSupported();
		ImmutableSctpMessageInfo msgInfo = ImmutableSctpMessageInfo.create(0);
		
		mc = startMulti(PORT+1);
		
		LoggerRecorder.enableRecording();
		IFuture<Void> f = mc.session.write(nopb("1"), msgInfo);
		f.await(TIMEOUT);
		assertTrue(f.isFailed());
		waitFor(50);
		assertEquals("SCTP_SENDING_FAILURE|", mc.getTrace());
		List<String> recording = LoggerRecorder.disableRecording();
		String warnMsg = "[ERROR] " + SessionIncident.SCTP_SENDING_FAILURE.defaultMessage();
		assertTrue(recording.contains(warnMsg));
		
		mc.incidentResult = true;
		LoggerRecorder.enableRecording();
		f = mc.session.write(nopb("1"), msgInfo);
		f.await(TIMEOUT);
		assertTrue(f.isFailed());
		waitFor(50);
		assertEquals("SCTP_SENDING_FAILURE|", mc.getTrace());
		recording = LoggerRecorder.disableRecording();
		assertFalse(recording.contains(warnMsg));
	}
	
	@Test
	public void testSendFailure() throws Exception {
		assumeSupported();
		mc = startMulti(PORT+1);
		mc.session.write(nopb("1"), ImmutableSctpMessageInfo.create(address(PORT), 1)).sync(TIMEOUT);
		mc.waitForNotification("SFL", TIMEOUT);
		assertEquals("DS|ASC|SFL|", mc.getTrace());	
		assertEquals(0, ((SctpMultiSession)mc.session).getAssociations().size());
		
		ms = startMulti(PORT);
		mc.session.write(nopb("1"), ImmutableSctpMessageInfo.create(address(PORT), 1)).sync(TIMEOUT);
		mc.waitForDataSent(TIMEOUT);
		mc.waitForNotification("ASC", TIMEOUT);
		ms.waitForNotification("ASC", TIMEOUT);
		ms.waitForDataRead(TIMEOUT);
		assertEquals("DS|ASC|", mc.getTrace());
		assertEquals("ASC|DR|NOP(1)[1]|", ms.getTrace());
		
		ms.clearNotifications();
		mc.session.key.attach(new TestSctpMultiChannelContext((SctpMultiSession) mc.session));
		mc.session.write(nopb("1"), ImmutableSctpMessageInfo.create(address(PORT), 1));//.sync(TIMEOUT);
		mc.waitForSessionEnding(TIMEOUT);
		ms.waitForNotification("ASC", TIMEOUT);
		assertEquals("EXC|SCL|SEN|", mc.getTrace());
		assertEquals("SHT|ASC|", ms.getTrace());
	}
	
	static class TestSctpMultiChannelContext extends SctpMultiChannelContext {

		TestSctpMultiChannelContext(SctpMultiSession session) {
			super(session);
		}
		
		@Override
		int send(SelectionKey key, ByteBuffer msg, MessageInfo msgInfo) throws Exception {
			throw new IOException();
		}
	}
	
	@Test
	public void testWriteFutureWithCodec() throws Exception {
		assumeSupported();
		IFuture<Void> f;
		
		TestCodec codec = new TestCodec();
		ms = startMulti(PORT);
		mc = new SctpClient(PORT+1);
		mc.traceNotification = true;
		mc.traceIncident = true;
		addCodecs(mc,1,0,codec.PBE(), codec.BPE());
		mc.startMulti();
		mc.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", mc.getTrace());
	
		ImmutableSctpMessageInfo msgInfo1 = ImmutableSctpMessageInfo.create(address(PORT), 1);
		ImmutableSctpMessageInfo msgInfo2 = ImmutableSctpMessageInfo.create(1);
		
		mc.session.write(nopb("1"), msgInfo1).sync(TIMEOUT);
		mc.waitForDataSent(TIMEOUT);
		mc.waitForNotification("ASC", TIMEOUT);
		ms.waitForDataRead(TIMEOUT);
		assertEquals("DS|ASC|", mc.getTrace());
		assertEquals("ASC|DR|NOP(1e)[1]|", ms.getTrace());
		assertEquals(5, mc.session.getWrittenBytes());
		
		f = mc.session.write(nopb("1"), msgInfo2);
		setTime();
		f.await(TIMEOUT);
		assertTime(100);
		waitFor(100);
		assertTrue(f.isDone());
		assertTrue(f.isFailed());
		assertEquals("SCTP_SENDING_FAILURE|", mc.getTrace());
		assertEquals("", ms.getTrace());
		assertEquals(5, mc.session.getWrittenBytes());
		
		mc.session.write(nopb("12"), msgInfo1).sync(TIMEOUT);
		mc.waitForDataSent(TIMEOUT);
		ms.waitForDataRead(TIMEOUT);
		assertEquals("DS|", mc.getTrace());
		assertEquals("DR|NOP(12e)[1]|", ms.getTrace());
		assertEquals(11, mc.session.getWrittenBytes());
	}
	
	@Test
	public void testWriteFuture() throws Exception {
		assumeSupported();
		IFuture<Void> f;
		
		ms = startMulti(PORT);
		mc = startMulti(PORT+1);
		
		ImmutableSctpMessageInfo msgInfo1 = ImmutableSctpMessageInfo.create(address(PORT), 1);
		ImmutableSctpMessageInfo msgInfo2 = ImmutableSctpMessageInfo.create(0);
		
		mc.session.write(nopb("1"), msgInfo1).sync(TIMEOUT);
		mc.waitForDataSent(TIMEOUT);
		mc.waitForNotification("ASC", TIMEOUT);
		ms.waitForDataRead(TIMEOUT);
		assertEquals("DS|ASC|", mc.getTrace());
		assertEquals("ASC|DR|NOP(1)[1]|", ms.getTrace());
		assertEquals(4, mc.session.getWrittenBytes());
		
		f = mc.session.write(nopb("1"), msgInfo2);
		setTime();
		f.await(TIMEOUT);
		assertTime(100);
		waitFor(100);
		assertTrue(f.isDone());
		assertTrue(f.isFailed());
		assertEquals("SCTP_SENDING_FAILURE|", mc.getTrace());
		assertEquals("", ms.getTrace());
		assertEquals(4, mc.session.getWrittenBytes());
		
		mc.session.write(nopb("12"), msgInfo1).sync(TIMEOUT);
		mc.waitForDataSent(TIMEOUT);
		ms.waitForDataRead(TIMEOUT);
		assertEquals("DS|", mc.getTrace());
		assertEquals("DR|NOP(12)[1]|", ms.getTrace());
		assertEquals(9, mc.session.getWrittenBytes());
		
		mc.session.suspendWrite();
		IFuture<Void> f1 = mc.session.write(nopb("12"), msgInfo1);
		IFuture<Void> f2 = mc.session.write(nopb("123"), msgInfo2);		
		IFuture<Void> f3 = mc.session.write(nopb("1234"), msgInfo1);
		waitFor(100);
		assertFalse(f1.isDone());
		assertFalse(f2.isDone());
		assertFalse(f3.isDone());
		mc.session.resumeWrite();
		f1.sync(TIMEOUT);
		f3.sync(TIMEOUT);
		assertTrue(f2.isFailed());
		mc.waitForDataSent(TIMEOUT);
		ms.waitForDataRead(TIMEOUT);
		waitFor(100);
		assertEquals("SCTP_SENDING_FAILURE|DS|", mc.getTrace());
		assertEquals("DR|NOP(12)[1]|DR|NOP(1234)[1]|", ms.getTrace());
		assertEquals(9+5+7, mc.session.getWrittenBytes());
	}
	
	@Test
	public void testWriteShutdownAssociation() throws Exception {
		assumeSupported();
		ms = startMulti(PORT);
		mc = startMulti(PORT+1);
		
		mc.session.write(nopb("1234"), ImmutableSctpMessageInfo.create(address(PORT), 1));
		mc.waitForDataSent(TIMEOUT);
		mc.waitForNotification("ASC", TIMEOUT);
		ms.waitForDataRead(TIMEOUT);
		assertEquals("DS|ASC|", mc.getTrace());
		assertEquals("ASC|DR|NOP(1234)[1]|", ms.getTrace());

		SctpMultiSession session = (SctpMultiSession) ms.session;
		assertEquals(1, session.getAssociations().size());
		Association a = session.getAssociation();
		
		session.shutdown(a);
		ms.waitForNotification("ASC", TIMEOUT);
		mc.waitForNotification("ASC", TIMEOUT);
		assertEquals("ASC|", ms.getTrace());
		assertEquals("SHT|ASC|", mc.getTrace());
		IFuture<Void> f = session.write(nopb("12"), ImmutableSctpMessageInfo.create(a, 1));
		f.await(TIMEOUT);
		assertTrue(f.isFailed());
		assertNotNull(f.cause());
		waitFor(50);
		assertEquals("SCTP_SENDING_FAILURE|", ms.getTrace());
		assertEquals("", mc.getTrace());
		assertTrue(ms.incidentThrowable instanceof SctpSendingFailureException);
		assertTrue(a == ((SctpSendingFailureException)ms.incidentThrowable).getMessageInfo().association());	
		
		session.writenf(nopb("12"), ImmutableSctpMessageInfo.create(a, 1));
		waitFor(100);
		assertEquals("SCTP_SENDING_FAILURE|", ms.getTrace());
		assertEquals("", mc.getTrace());
		assertTrue(ms.incidentThrowable instanceof SctpSendingFailureException);
		assertTrue(a == ((SctpSendingFailureException)ms.incidentThrowable).getMessageInfo().association());	
	}
	
	@Test
	public void testWriteWithoutAssociationAndAddress() throws Exception {
		assumeSupported();
		ms = startMulti(PORT);
		mc = startMulti(PORT+1);
		
		IFuture<Void> f = mc.session.write(nopb("1"), ImmutableSctpMessageInfo.create(0));
		f.await(TIMEOUT);
		assertTrue(f.isFailed());
		assertNotNull(f.cause());
		waitFor(50);
		assertEquals("SCTP_SENDING_FAILURE|", mc.getTrace());
		assertEquals("", ms.getTrace());
		
		mc.session.write(nopb("1234"), ImmutableSctpMessageInfo.create(address(PORT), 1));
		mc.waitForDataSent(TIMEOUT);
		mc.waitForNotification("ASC", TIMEOUT);
		ms.waitForDataRead(TIMEOUT);
		assertEquals("DS|ASC|", mc.getTrace());
		assertEquals("ASC|DR|NOP(1234)[1]|", ms.getTrace());
		
		f = mc.session.write(nopb("1"), ImmutableSctpMessageInfo.create(0));
		f.await(TIMEOUT);
		assertTrue(f.isFailed());
		assertNotNull(f.cause());
		waitFor(50);
		assertEquals("SCTP_SENDING_FAILURE|", mc.getTrace());
		assertEquals("", ms.getTrace());
		
		mc.session.write(nopb("1234"), ImmutableSctpMessageInfo.create(address(PORT), 1));
		mc.waitForDataSent(TIMEOUT);
		ms.waitForDataRead(TIMEOUT);
		assertEquals("DS|", mc.getTrace());
		assertEquals("DR|NOP(1234)[1]|", ms.getTrace());		
	}	
	
	@Test
	public void testBindUnbindAddress() throws Exception {
		assumeSupported();
		startClientServer();
		InternalSctpSession session = c.session;
		InetAddress addr0 = address(0).getAddress();
		InetAddress addr1 = null;
		
		for (InetAddress a: addresses(session)) {
			if (!a.equals(addr0)) {
				addr1 = a;
				break;
			}
		}
		assertNotNull(addr1);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		ms = new SctpClient(PORT);
		ms.traceNotification = true;
		ms.startMulti();
		ms.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", ms.getTrace());
		
		assertEquals(1, ms.session.getLocalAddresses().size());
		ms.session.bindAddress(addr1).sync(TIMEOUT);
		assertEquals(2, ms.session.getLocalAddresses().size());
		ms.session.unbindAddress(addr1).sync(TIMEOUT);
		assertEquals(1, ms.session.getLocalAddresses().size());		
	}
	
	@Test
	public void testAddresses() throws Exception {
		assumeSupported();
		startClientServer();
		InetAddress addr0 = address(0).getAddress();
		InetAddress addr1 = null;
		
		for (InetAddress a: addresses(c.session)) {
			if (!a.equals(addr0)) {
				addr1 = a;
				break;
			}
		}
		assertNotNull(addr1);
		Association a = c.session.getAssociation();
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		ms = new SctpClient(PORT);
		ms.traceNotification = true;
		ms.startMulti();
		ms.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", ms.getTrace());
		SctpMultiSession session = (SctpMultiSession) ms.session;

		Set<SocketAddress> addrs = session.getLocalAddresses();
		assertEquals(1, addrs.size());
		assertNotNull(session.getLocalAddress());
		assertEquals(session.getLocalAddress(), session.getLocalAddresses().iterator().next());
		session.bindAddress(addr1).sync(TIMEOUT);
		addrs = session.getLocalAddresses();
		assertEquals(2, addrs.size());
		assertNotNull(session.getLocalAddress());
		
		assertNull(session.getRemoteAddress());
		assertEquals(0, session.getRemoteAddresses(a).size());
		assertEquals(0, session.getRemoteAddresses().size());
		assertNull(session.getRemoteAddress());
		
		try {
			session.getRemoteAddresses(null);
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		assertEquals(0, session.getAssociations().size());
		assertNull(session.getAssociation());
		
		c = new SctpClient(PORT);
		c.traceNotification = true;
		c.localAddresses.add(address(0));
		c.start();
		c.waitForSessionReady(TIMEOUT);
		c.waitForNotification("ASC", TIMEOUT);
		assertEquals("SCR|SOP|RDY|ASC|", c.getTrace());
		ms.waitForNotification("ASC", TIMEOUT);
		assertEquals("ASC|", ms.getTrace());
			
		assertEquals(1, session.getRemoteAddresses(ms.association(0)).size());
		assertEquals(1, session.getRemoteAddresses().size());
		assertNotNull(session.getRemoteAddress());
		assertEquals(session.getRemoteAddress(), session.getRemoteAddresses().iterator().next());
		assertEquals(1, c.session.getLocalAddresses().size());
		assertEquals(c.session.getLocalAddress(), session.getRemoteAddresses(ms.association(0)).iterator().next());
		assertEquals(1, session.getAssociations().size());
		assertNotNull(session.getAssociation());
		assertEquals(session.getAssociation(), session.getAssociations().iterator().next());
		
		SctpClient c2 = new SctpClient(PORT);
		c2.traceNotification = true;
		c2.localAddresses.add(address(0));
		c2.start();
		c2.waitForSessionReady(TIMEOUT);
		c2.waitForNotification("ASC", TIMEOUT);
		assertEquals("SCR|SOP|RDY|ASC|", c2.getTrace());
		ms.waitForNotification("ASC", TIMEOUT);
		assertEquals("ASC|", ms.getTrace());

		assertEquals(1, session.getRemoteAddresses(ms.association(1)).size());
		assertEquals(2, session.getRemoteAddresses().size());
		assertEquals(1, c2.session.getLocalAddresses().size());
		assertEquals(c2.session.getLocalAddress(), session.getRemoteAddresses(ms.association(1)).iterator().next());
		assertEquals(2, session.getAssociations().size());
		a = session.getAssociation();
		assertNotNull(a);
		Iterator<Association> i = session.getAssociations().iterator();
		if (!a.equals(i.next())) {
			assertEquals(a, i.next());
		}
		SocketAddress addr = session.getRemoteAddress();
		assertNotNull(addr);
		Iterator<SocketAddress> i2 = session.getRemoteAddresses().iterator();
		if (!addr.equals(i2.next())) {
			assertEquals(addr, i2.next());
		}
		
		session.close();
		ms.waitForSessionEnding(TIMEOUT);
		ms.waitForNotification("ASC", TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		c.waitForNotification("ASC", TIMEOUT);
		c2.waitForSessionEnding(TIMEOUT);
		c2.waitForNotification("ASC", TIMEOUT);
		assertEquals("ASC|ASC|SCL|SEN|", ms.getTrace());
		assertEquals("SHT|ASC|SCL|SEN|", c.getTrace());
		assertEquals("SHT|ASC|SCL|SEN|", c2.getTrace());
		c2.stop(TIMEOUT);
		assertEquals(0, session.getRemoteAddresses(ms.association(0)).size());
		assertEquals(0, session.getAssociations().size());
		session.channel = null;
		assertEquals(0, session.getRemoteAddresses(ms.association(0)).size());
		assertEquals(0, session.getAssociations().size());
		TestSctpMultiChannel tsmc = new TestSctpMultiChannel();
		tsmc.associationsException = new IOException();
		session.channel = tsmc;
		assertEquals(0, session.getAssociations().size());	
		tsmc.close();
	}
	
	@Test
	public void testCloseWithPendingShutowns() throws Exception {
		assumeSupported();
		ms = new SctpClient(PORT);
		ms.traceNotification = true;
		ms.startMulti();
		ms.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", ms.getTrace());
		SctpMultiSession session = (SctpMultiSession) ms.session;
		
		c = new SctpClient(PORT);
		c.traceNotification = true;
		c.localAddresses.add(address(0));
		c.start();
		c.waitForSessionReady(TIMEOUT);
		c.waitForNotification("ASC", TIMEOUT);
		assertEquals("SCR|SOP|RDY|ASC|", c.getTrace());
		ms.waitForNotification("ASC", TIMEOUT);
		assertEquals("ASC|", ms.getTrace());
		
		sleepLoop(ms.loop, 100);
		session.close();
	
		SctpClient c2 = new SctpClient(PORT);
		c2.traceNotification = true;
		c2.localAddresses.add(address(0));
		c2.start();

		SctpClient c3 = new SctpClient(PORT);
		c3.traceNotification = true;
		c3.localAddresses.add(address(0));
		c3.start();
		
		c.waitForSessionEnding(TIMEOUT);
		c2.waitForSessionEnding(TIMEOUT);
		c3.waitForSessionEnding(TIMEOUT);
		ms.waitForSessionEnding(TIMEOUT);
		c2.stop(TIMEOUT);
		c3.stop(TIMEOUT);
		assertEquals("SHT|ASC|SCL|SEN|", c.getTrace());
		assertEquals("SCR|SOP|RDY|ASC|SHT|ASC|SCL|SEN|", c2.getTrace());
		assertEquals("SCR|SOP|RDY|ASC|SHT|ASC|SCL|SEN|", c3.getTrace());
		assertEquals("ASC|ASC|ASC|ASC|ASC|SCL|SEN|", ms.getTrace());
		assertFinished(c,c2,c3,ms);
	}
	
	@Test
	public void testCloseWithoutLoopAndExceptions() throws Exception {
		assumeSupported();
		ms = new SctpClient(PORT);
		ms.traceNotification = true;
		ms.startMulti();
		ms.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", ms.getTrace());
		SctpMultiSession session = (SctpMultiSession) ms.session;
		
		session.loop = null;
		session.quickClose();
		assertFinished(ms);
		session.loop = ms.loop;
		
		TestSctpMultiChannel tsmc = new TestSctpMultiChannel();
		tsmc.associationsException = new IOException();
		session.shutdown((Set<Association>)null);
		tsmc.close();
	}
	
	void assertIllegalStateException(String name, Class<?>... args) throws Exception {
		Method m = SctpMultiSession.class.getDeclaredMethod(name, args);
		
		Object[] vals = new Object[args.length];
		for (int i=0; i<vals.length; ++i) {
			Class<?> clazz = args[i];
			
			if (clazz == int.class) {
				vals[i] = 1;
			}
		}
		
		try {
			m.invoke(ms.session, vals);
			fail();
		}
		catch (InvocationTargetException e) {
			assertTrue(e.getCause().getClass() == IllegalStateException.class);
			assertEquals("default peer address is not configured", e.getCause().getMessage());
		}
	}
	
	private void assertWrite(String expected) throws Exception {
		ms.waitForDataSent(TIMEOUT);
		mc.waitForDataRead(TIMEOUT);
		assertEquals(expected, mc.getTrace());
	}
	
	@Test
	public void testWriteMethods() throws Exception {
		assumeSupported();

		ms = new SctpClient(PORT);
		ms.startMulti();
		ms.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", ms.getTrace());
		
		assertIllegalStateException("write",byte[].class);
		assertIllegalStateException("write",byte[].class, int.class, int.class);
		assertIllegalStateException("write",ByteBuffer.class);
		assertIllegalStateException("write",ByteBuffer.class,int.class);
		assertIllegalStateException("write",Object.class);
		assertIllegalStateException("writenf",byte[].class);
		assertIllegalStateException("writenf",byte[].class, int.class, int.class);
		assertIllegalStateException("writenf",ByteBuffer.class);
		assertIllegalStateException("writenf",ByteBuffer.class,int.class);
		assertIllegalStateException("writenf",Object.class);
		ms.session.close();
		ms.waitForSessionEnding(TIMEOUT);
		
		ms = new SctpClient(PORT);
		ms.defaultSctpPeerAddress = address(PORT+1);
		ms.startMulti();
		ms.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", ms.getTrace());

		mc = new SctpClient(PORT+1);
		mc.startMulti();
		mc.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", mc.getTrace());
		
		ISctpSession session = ms.session;
		
		session.write(nopb("1")).sync(TIMEOUT);
		assertWrite("DR|NOP(1)|");
		session.writenf(nopb("1"));
		assertWrite("DR|NOP(1)|");
		
		session.write(nopb("1",4,7),4,4).sync(TIMEOUT);
		assertWrite("DR|NOP(1)|");
		session.writenf(nopb("1",4,7),4,4);
		assertWrite("DR|NOP(1)|");
		
		session.write(nopbb("1")).sync(TIMEOUT);
		assertWrite("DR|NOP(1)|");
		session.writenf(nopbb("1"));
		assertWrite("DR|NOP(1)|");
		
		session.write(nopbb("123", 7),6).sync(TIMEOUT);
		assertWrite("DR|NOP(123)|");
		session.writenf(nopbb("123", 7),6);
		assertWrite("DR|NOP(123)|");

		session.write((Object)nopb("1")).sync(TIMEOUT);
		assertWrite("DR|NOP(1)|");
		session.writenf((Object)nopb("1"));
		assertWrite("DR|NOP(1)|");
		
	}
	
	@Test
	public void testShutdown() throws Exception {
		assumeSupported();
		ms = new SctpClient(PORT);
		ms.traceNotification = true;
		ms.startMulti();
		ms.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", ms.getTrace());
		SctpMultiSession session = (SctpMultiSession) ms.session;
		
		c = new SctpClient(PORT);
		c.traceNotification = true;
		c.localAddresses.add(address(0));
		c.start();
		c.waitForSessionReady(TIMEOUT);
		c.waitForNotification("ASC", TIMEOUT);
		assertEquals("SCR|SOP|RDY|ASC|", c.getTrace());
		ms.waitForNotification("ASC", TIMEOUT);
		assertEquals("ASC|", ms.getTrace());
		
		session.shutdown(ms.association(0)).sync(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		ms.waitForNotification("ASC", TIMEOUT);
		assertEquals("SHT|ASC|SCL|SEN|", c.getTrace());
		assertEquals("ASC|", ms.getTrace());
		c.stop(TIMEOUT);

		IFuture<Void> f = session.shutdown(ms.association(0));
		f.await(TIMEOUT);
		assertTrue(f.isFailed());
		assertNotNull(f.cause());
		assertTrue(f.getClass() == TaskFuture.class);
		ms.clearNotifications();
		
		c = new SctpClient(PORT);
		c.traceNotification = true;
		c.localAddresses.add(address(0));
		c.start();
		c.waitForSessionReady(TIMEOUT);
		c.waitForNotification("ASC", TIMEOUT);
		assertEquals("SCR|SOP|RDY|ASC|", c.getTrace());
		ms.waitForNotification("ASC", TIMEOUT);
		assertEquals("ASC|", ms.getTrace());
		
		ShutdownTask task = new ShutdownTask(session, ms.association(0));
		ms.loop.execute(task).sync(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		ms.waitForNotification("ASC", TIMEOUT);
		assertEquals("SHT|ASC|SCL|SEN|", c.getTrace());
		assertEquals("ASC|", ms.getTrace());
		c.stop(TIMEOUT);
		assertTrue(task.f.getClass() == SuccessfulFuture.class);
		
		task = new ShutdownTask(session, ms.association(0));		
		ms.loop.execute(task).sync(TIMEOUT);
		task.f.await(TIMEOUT);
		assertTrue(task.f.isFailed());
		assertNotNull(task.f.cause());
		assertTrue(task.f.getClass() == FailedFuture.class);
		
		session.close();
		ms.waitForSessionEnding(TIMEOUT);
		ms.stop(TIMEOUT);
		assertEquals("SCL|SEN|", ms.getTrace());
		session.loop = null;
		f = session.shutdown(ms.association(0));
		assertTrue(f.getClass() == CancelledFuture.class);
		session.loop = ms.loop;
		session.channel = null;
		f = session.shutdown(ms.association(0));
		assertTrue(f.getClass() == CancelledFuture.class);
		ms = null;
	}
	
	class ShutdownTask implements Runnable {
		
		IFuture<Void> f;
		
		final SctpMultiSession s;
		
		final Association a;
		
		ShutdownTask(SctpMultiSession s, Association a) {
			this.s = s;
			this.a = a;
		}
		
		@Override
		public void run() {
			f = s.shutdown(a);
		}
	}
	
	@Test
	public void testBranch() throws Exception {
		assumeSupported();
		ms = new SctpClient(PORT);
		ms.traceNotification = true;
		ms.startMulti();
		ms.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", ms.getTrace());
		SctpMultiSession session = (SctpMultiSession) ms.session;

		c = new SctpClient(PORT);
		c.traceNotification = true;
		c.localAddresses.add(address(0));
		c.start();
		c.waitForSessionReady(TIMEOUT);
		c.waitForNotification("ASC", TIMEOUT);
		assertEquals("SCR|SOP|RDY|ASC|", c.getTrace());
		ms.waitForNotification("ASC", TIMEOUT);
		assertEquals("ASC|", ms.getTrace());
		
		assertEquals(1, session.getAssociations().size());
		SctpChannel channel = ms.smc.branch(ms.association(0));
		assertEquals(0, session.getAssociations().size());
		
		ms.session.close();
		ms.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", ms.getTrace());
		
		SctpClient c2 = new SctpClient(PORT);
		c2.traceNotification = true;
		c2.start(channel);
		c2.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c2.getTrace());
		c.session.writenf(nopb("1"));
		c.waitForDataSent(TIMEOUT);
		c2.waitForDataRead(TIMEOUT);
		assertEquals("DS|", c.getTrace());
		assertEquals("DR|NOP(1)|", c2.getTrace());

		c2.session.writenf(nopb("12"));
		c2.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|", c2.getTrace());
		assertEquals("DR|NOP(12)|", c.getTrace());
		
		c.session.close();
		c2.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("ASC|SCL|SEN|", c.getTrace());
		assertEquals("SHT|ASC|SCL|SEN|", c2.getTrace());
		
		c2.stop(TIMEOUT);
	}
}
