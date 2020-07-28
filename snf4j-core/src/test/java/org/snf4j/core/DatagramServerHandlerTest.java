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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.codec.DefaultCodecExecutor;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.IDatagramHandlerFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.AbstractDatagramHandler;
import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.logger.LoggerRecorder;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.SessionState;
import org.snf4j.core.timer.DefaultTimer;
import org.snf4j.core.timer.ITimeoutModel;
import org.snf4j.core.timer.ITimer;
import org.snf4j.core.timer.ITimerTask;

public class DatagramServerHandlerTest {
	
	long TIMEOUT = 2000;
	int PORT = 7779;
	
	DatagramHandler c, c2;
	DatagramHandler s;
	
	TestCodec codec;
	TestCodec codec2;
	
	@Before
	public void before() {
		s = c = c2 = null;
	}
	
	@After
	public void after() throws InterruptedException {
		if (c != null) c.stop(TIMEOUT);
		if (c2 != null) c2.stop(TIMEOUT);
		if (s != null) s.stop(TIMEOUT);
	}
	
	private void waitFor(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}

	DatagramSession getDelegate(DatagramSession session) throws Exception {
		Field f = DatagramServerSession.class.getDeclaredField("delegate");
		f.setAccessible(true);
		
		return (DatagramSession) f.get(session);
	}
	
	SocketAddress getAddress(DatagramSession session) throws Exception {
		Field f = DatagramServerSession.class.getDeclaredField("remoteAddress");
		f.setAccessible(true);
		
		return (SocketAddress) f.get(session);
		
	}
	
	int getSize(IDatagramHandler handler) throws Exception {
		Field f = DatagramServerHandler.class.getDeclaredField("sessions");
		f.setAccessible(true);
		Map<?,? >map = (Map<?, ?>) f.get(handler);
		return map.size();
	}
	
	@SuppressWarnings("unchecked")
	Map<SocketAddress, ITimerTask> getTimers(IDatagramHandler handler) throws Exception {
		Field f = DatagramServerHandler.class.getDeclaredField("timers");
		f.setAccessible(true);
		Map<?,? >map = (Map<?, ?>) f.get(handler);
		return (Map<SocketAddress, ITimerTask>) map;
	}
	
	byte[] nop(String s) {
		return new Packet(PacketType.NOP, s).toBytes();
	}
	
	byte[] nop() {
		return nop("");
	}
	
	@Test(expected = NullPointerException.class)
	public void testConstructorWithNull() {
		new DatagramServerHandler(null);
	}

	@Test(expected = NullPointerException.class)
	public void testConstructorWithNull2() {
		new DatagramServerHandler(null, new DefaultSessionConfig());
	}

	@Test(expected = NullPointerException.class)
	public void testConstructorWithNull3() {
		new DatagramServerHandler(null, new DefaultSessionConfig(), DefaultSessionStructureFactory.DEFAULT);
	}
	
	@Test
	public void testConstructor() {
		IDatagramHandlerFactory hf = new IDatagramHandlerFactory() {

			@Override
			public IDatagramHandler create(SocketAddress remoteAddress) {
				return null;
			}
			
		};
		ISessionStructureFactory f = new ISessionStructureFactory() {

			@Override
			public IByteBufferAllocator getAllocator() {
				return null;
			}

			@Override
			public ConcurrentMap<Object, Object> getAttributes() {
				return null;
			}

			@Override
			public Executor getExecutor() {
				return null;
			}
			
			@Override
			public ITimer getTimer() {
				return null;
			}

			@Override
			public ITimeoutModel getTimeoutModel() {
				return null;
			}
			
		};
		DefaultSessionConfig c = new DefaultSessionConfig();
		
		DatagramServerHandler h = new DatagramServerHandler(hf);
		assertTrue(h.getFactory() == DefaultSessionStructureFactory.DEFAULT);
		assertTrue(h.getConfig().getClass() == DefaultSessionConfig.class);
		assertFalse(h.getConfig() == c);
		
		h = new DatagramServerHandler(hf, c);
		assertTrue(h.getFactory() == DefaultSessionStructureFactory.DEFAULT);
		assertTrue(h.getConfig() == c);

		h = new DatagramServerHandler(hf, c, f);
		assertTrue(h.getFactory() == f);
		assertTrue(h.getConfig() == c);
	
	}

	private DefaultCodecExecutor codec() {
		codec = new TestCodec();
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BasePD());
		p.getPipeline().add("2", codec.PBD());
		p.getPipeline().add("3", codec.PBE());
		p.getPipeline().add("4", codec.BPE());
		return p;
	}
	
	private DefaultCodecExecutor codec2() {
		codec2 = new TestCodec();
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec2.BasePD());
		p.getPipeline().add("2", codec2.PBD_D());
		p.getPipeline().add("3", codec2.PBE());
		p.getPipeline().add("4", codec2.BPE());
		return p;
	}	
	
	@Test
	public void testSessionWithConnectedChannel() throws Exception {
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		c.useDatagramServerHandler = true;
		c.codecPipeline = codec();
		c.codecPipeline2 = codec2();
		s.startServer();
		c.startClient();
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		waitFor(100);
		DatagramSession superSession = c.getSession();
		superSession.write(nop());
		s.waitForDataRead(TIMEOUT);
		waitFor(100);
		assertEquals("DR|$NOP(e)|", s.getRecordedData(true));
		assertEquals("", c.getRecordedData(true));
		s.getSession().send(c.getSession().getLocalAddress(), nop());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals(superSession.getRemoteAddress(), c.handlerFactoryRemoteAddress);
		assertEquals("SCR|SOP|RDY|DR|NOP(Dd)|", c.getRecordedData(true));
		assertEquals("DS|", s.getRecordedData(true));
		DatagramServerSession session = (DatagramServerSession) c.getSession();
		assertEquals(3, session.getReadBytes());
		assertEquals(3, superSession.getReadBytes());
		assertEquals(0, session.getWrittenBytes());
		assertEquals(4, superSession.getWrittenBytes());
		
		codec.sessionId = true;
		codec2.sessionId = true;
		long idD = c.getSession().getParent().getId();
		long idd = c.getSession().getId();
		SocketAddress addr = c.getSession().getLocalAddress();
		assertEquals(c.getSession().getRemoteAddress(), superSession.getRemoteAddress());
		s.getSession().send(addr, nop());
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|NOP(D["+idD+"]d["+idd+"])|", c.getRecordedData(true));
		assertEquals("DS|", s.getRecordedData(true));
		assertTrue(superSession == getDelegate(session));
		assertEquals(6, session.getReadBytes());
		assertEquals(6, superSession.getReadBytes());
		assertNull(getAddress(session));
		assertTrue(superSession.getHandler() instanceof DatagramServerHandler);
		assertEquals("org.snf4j.core.DatagramHandler$Handler", session.getHandler().getClass().getName());
		codec.sessionId = false;
		codec2.sessionId = false;
		
		s.getSession().send(addr, nop("1"));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|NOP(1Dd)|", c.getRecordedData(true));
		assertEquals("DS|", s.getRecordedData(true));
		assertTrue(session == c.getSession());
		assertEquals(10, session.getReadBytes());
		assertEquals(10, superSession.getReadBytes());
		assertEquals(0, session.getWrittenBytes());
		assertEquals(4, superSession.getWrittenBytes());
		
		session.write(nop("12")).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DR|$NOP(12ee)|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals(10, session.getReadBytes());
		assertEquals(10, superSession.getReadBytes());
		assertEquals(7, session.getWrittenBytes());
		assertEquals(11, superSession.getWrittenBytes());
		
		superSession.write(nop("123")).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DR|$NOP(123e)|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals(10, session.getReadBytes());
		assertEquals(10, superSession.getReadBytes());
		assertEquals(14, session.getWrittenBytes());
		assertEquals(18, superSession.getWrittenBytes());
		
		assertEquals(SessionState.OPEN, session.getState());
		session.close();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals(SessionState.CLOSING, session.getState());
		assertEquals(SessionState.OPEN, superSession.getState());
		
		s.getSession().send(addr, nop());
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP(Dd)|", c.getRecordedData(true));
		assertEquals("DS|", s.getRecordedData(true));
		assertFalse(session == c.getSession());
		session = (DatagramServerSession) c.getSession();
		assertEquals(3, session.getReadBytes());
		assertEquals(13, superSession.getReadBytes());
		assertEquals(0, session.getWrittenBytes());
		assertEquals(18, superSession.getWrittenBytes());
		
		superSession.getCodecPipeline().remove("2");
		s.getSession().send(addr, new Packet(PacketType.NOP, "5").toBytes()).sync(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DR|M(NOP[5])|", c.getRecordedData(true));
		assertEquals("DS|", s.getRecordedData(true));
		assertEquals(7, session.getReadBytes());
		assertEquals(17, superSession.getReadBytes());
		assertEquals(0, session.getWrittenBytes());
		assertEquals(18, superSession.getWrittenBytes());
		
		assertEquals(SessionState.OPEN, session.getState());
		assertEquals(SessionState.OPEN, superSession.getState());
		superSession.close();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals(SessionState.CLOSING, session.getState());
		assertEquals(SessionState.CLOSING, superSession.getState());
		c = null;

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
	public void testReadWithDecoders() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.codecPipeline = codec();
		s.codecPipeline2 = codec2();
		c = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP(Dd)|", s.getRecordedData(true));
		s.stop(TIMEOUT);
		
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.codecPipeline = codec();
		s.startServer();
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP(d)|", s.getRecordedData(true));
		codec.decodeException = new Exception();
		LoggerRecorder.enableRecording();
		s.incident = false;
		c.getSession().write(nop());
		c.waitForDataSent(TIMEOUT);
		waitFor(50);
		List<String> recording = getErrors(LoggerRecorder.disableRecording());		
		assertEquals(1, recording.size());
		assertTrue(recording.get(0).startsWith("[ERROR] " + SessionIncident.DECODING_PIPELINE_FAILURE.defaultMessage()));
		assertEquals("DR|DECODING_PIPELINE_FAILURE|", s.getRecordedData(true));
		codec.decodeException = null;
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(d)|", s.getRecordedData(true));
		codec.decodeException = new Exception();
		LoggerRecorder.enableRecording();
		s.incident = true;
		c.getSession().write(nop());
		c.waitForDataSent(TIMEOUT);
		waitFor(50);
		recording = getErrors(LoggerRecorder.disableRecording());		
		assertEquals(0, recording.size());
		assertEquals("DR|DECODING_PIPELINE_FAILURE|", s.getRecordedData(true));
		codec.decodeException = null;
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(d)|", s.getRecordedData(true));
		
		s.stop(TIMEOUT);

		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.codecPipeline2 = codec2();
		s.startServer();
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP(D)|", s.getRecordedData(true));
		codec2.decodeException = new Exception();
		c.getSession().write(nop());
		c.waitForDataSent(TIMEOUT);
		waitFor(100);
		assertEquals("DR|DECODING_PIPELINE_FAILURE|", s.getRecordedData(true));
		codec2.decodeException = null;
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(D)|", s.getRecordedData(true));
		
	}
	
	@Test
	public void testSessionWithNotConnectedChannel() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.codecPipeline = codec();
		s.codecPipeline2 = codec2();
		c = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		DatagramSession superSession = s.getSession();
		
		codec.sessionId = true;
		codec2.sessionId = true;
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		long idD = s.getSession().getParent().getId();
		long idd = s.getSession().getId();
		assertEquals("SCR|SOP|RDY|DR|NOP(D["+idD+"]d["+idd+"])|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		waitFor(10);
		DatagramServerSession session = (DatagramServerSession) s.getSession();
		assertTrue(superSession == getDelegate(session));
		assertEquals(3, superSession.getReadBytes());
		assertEquals(3, session.getReadBytes());
		assertEquals(0, superSession.getWrittenBytes());
		assertEquals(0, session.getWrittenBytes());
		assertEquals(c.getSession().getLocalAddress(), getAddress(session));
		assertEquals(session.getRemoteAddress(), c.getSession().getLocalAddress());
		assertEquals(c.getSession().getLocalAddress(), s.handlerFactoryRemoteAddress);
		codec.sessionId = false;
		codec2.sessionId = false;
		
		c.getSession().write(nop("1"));
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DR|NOP(1Dd)|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		waitFor(10);
		assertEquals(7, superSession.getReadBytes());
		assertEquals(7, session.getReadBytes());
		assertEquals(0, superSession.getWrittenBytes());
		assertEquals(0, session.getWrittenBytes());
		
		session.write(nop("12"));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|", s.getRecordedData(true));
		assertEquals("DR|NOP(12ee)|", c.getRecordedData(true));
		waitFor(10);
		assertEquals(7, superSession.getReadBytes());
		assertEquals(7, session.getReadBytes());
		assertEquals(7, superSession.getWrittenBytes());
		assertEquals(7, session.getWrittenBytes());
		
		superSession.send(c.getSession().getLocalAddress(), nop("1"));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("DS|", s.getRecordedData(true));
		assertEquals("DR|NOP(1e)|", c.getRecordedData(true));
		waitFor(10);
		assertEquals(7, superSession.getReadBytes());
		assertEquals(7, session.getReadBytes());
		assertEquals(12, superSession.getWrittenBytes());
		assertEquals(12, session.getWrittenBytes());
		
		IFuture<Void> f = superSession.write(nop()).await(TIMEOUT);
		assertTrue(f.isFailed());
		assertTrue(f.cause().getClass() == NotYetConnectedException.class);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals(SessionState.CLOSING, session.getState());
		assertEquals(SessionState.CLOSING, superSession.getState());
		assertEquals("EXC|SCL|SEN|", s.getRecordedData(true));
		
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.codecPipeline = codec();
		s.codecPipeline2 = codec2();
		s.startServer();
		superSession = s.getSession();
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP(Dd)|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		session = (DatagramServerSession) s.getSession();
		assertEquals(3, session.getReadBytes());
		assertEquals(3, superSession.getReadBytes());
		assertEquals(0, session.getWrittenBytes());
		assertEquals(0, superSession.getWrittenBytes());
	
		assertEquals(SessionState.OPEN, session.getState());
		session.close();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertEquals(SessionState.CLOSING, session.getState());
		assertEquals(SessionState.OPEN, superSession.getState());
		
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP(Dd)|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		assertFalse(session == s.getSession());
		waitFor(10);
		session = (DatagramServerSession) s.getSession();
		assertEquals(3, session.getReadBytes());
		assertEquals(6, superSession.getReadBytes());
		assertEquals(0, session.getWrittenBytes());
		assertEquals(0, superSession.getWrittenBytes());

		superSession.getCodecPipeline().remove("2");
		c.getSession().write(new Packet(PacketType.NOP, "5").toBytes()).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DR|M(NOP[5])|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals(7, session.getReadBytes());
		assertEquals(10, superSession.getReadBytes());
		assertEquals(0, session.getWrittenBytes());
		assertEquals(0, superSession.getWrittenBytes());
		
		assertEquals(SessionState.OPEN, session.getState());
		assertEquals(SessionState.OPEN, superSession.getState());
		superSession.close();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertEquals(SessionState.CLOSING, session.getState());
		assertEquals(SessionState.CLOSING, superSession.getState());
		s = null;
		
	}
	
	void assertSessionBytes(DatagramSession session, int bytesSuperRead, int bytesRead, int bytesSuperWritten, int bytesWritten) throws Exception {
		assertEquals(bytesSuperRead, getDelegate(session).getReadBytes());
		assertEquals(bytesRead, session.getReadBytes());
		assertEquals(bytesSuperWritten, getDelegate(session).getWrittenBytes());
		assertEquals(bytesWritten, session.getWrittenBytes());
	}
	
	@Test
	public void testMultiClients() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		c = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);

		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|DS|", c.getRecordedData(true));
		DatagramSession s1 = s.getSession();
		assertSessionBytes(s1, 3, 3, 0, 0);
		assertEquals(1, getSize(getDelegate(s1).getHandler()));
		
		DatagramHandler c2 = new DatagramHandler(PORT);
		c2.startClient();
		c2.waitForSessionReady(TIMEOUT);
		c2.getSession().write(nop("1"));
		s.waitForDataRead(TIMEOUT);
		c2.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP(1)|", s.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|DS|", c2.getRecordedData(true));
		DatagramSession s2 = s.getSession();
		assertSessionBytes(s1, 7, 3, 0, 0);
		assertSessionBytes(s2, 7, 4, 0, 0);
		assertEquals(2, getSize(getDelegate(s2).getHandler()));
		
		s1.write(nop("12"));
		c.waitForDataRead(TIMEOUT);
		waitFor(100);
		assertEquals("DS|", s.getRecordedData(true));
		assertEquals("DR|NOP(12)|", c.getRecordedData(true));
		assertEquals("", c2.getRecordedData(true));
		assertSessionBytes(s1, 7, 3, 5, 5);
		assertSessionBytes(s2, 7, 4, 5, 0);
		
		s2.write(nop("123"));
		c2.waitForDataRead(TIMEOUT);
		waitFor(100);
		assertEquals("DS|", s.getRecordedData(true));
		assertEquals("DR|NOP(123)|", c2.getRecordedData(true));
		assertEquals("", c.getRecordedData(true));
		assertSessionBytes(s1, 7, 3, 11, 5);
		assertSessionBytes(s2, 7, 4, 11, 6);
		
		s2.send(c.getSession().getLocalAddress(), nop("4"));
		c.waitForDataRead(TIMEOUT);
		waitFor(100);
		assertEquals("DS|", s.getRecordedData(true));
		assertEquals("DR|NOP(4)|", c.getRecordedData(true));
		assertSessionBytes(s1, 7, 3, 15, 9);
		assertSessionBytes(s2, 7, 4, 15, 6);
		
		s2.close();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertEquals(SessionState.CLOSING, s2.getState());
		assertEquals(SessionState.OPEN, s1.getState());
		assertEquals(SessionState.OPEN, getDelegate(s1).getState());
		assertEquals(1, getSize(getDelegate(s1).getHandler()));
		
		s1.close();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertEquals(SessionState.CLOSING, s2.getState());
		assertEquals(SessionState.CLOSING, s1.getState());
		assertEquals(SessionState.OPEN, getDelegate(s1).getState());
		assertEquals(0, getSize(getDelegate(s1).getHandler()));
	
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		assertEquals(1, getSize(getDelegate(s.getSession()).getHandler()));
		s1 = s.getSession();
		c2.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		assertEquals(2, getSize(getDelegate(s.getSession()).getHandler()));
		s2 = s.getSession();
		assertEquals(2, getSize(getDelegate(s1).getHandler()));
		
		getDelegate(s.getSession()).close();
		waitFor(500);
		assertEquals("SCL|SEN|SCL|SEN|", s.getRecordedData(true));
		assertEquals(SessionState.CLOSING, s2.getState());
		assertEquals(SessionState.CLOSING, s1.getState());
		assertEquals(SessionState.CLOSING, getDelegate(s1).getState());
		assertEquals(0, getSize(getDelegate(s1).getHandler()));
		s = null;
		c2.stop(TIMEOUT);
		
	}
	
	@Test
	public void testCreateNullSession() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.createNullHandler = true;
		c = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		
		c.getSession().write(nop());
		waitFor(100);
		assertEquals("", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));

		s.createNullHandler = false;
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		
		s.stop(TIMEOUT);
		
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.createNullHandler = true;
		s.codecPipeline = codec();
		s.codecPipeline2 = s.codecPipeline;
		s.startServer();
		s.getSession().getCodecPipeline().remove("2");
	
		c.getSession().write(nop());
		waitFor(100);
		assertEquals("", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));

		s.createNullHandler = false;
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|M(NOP[])|", s.getRecordedData(true));
		
	}
	
	@Test
	public void testCloseUnknownSession() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		c = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		DatagramServerHandler h = (DatagramServerHandler) s.getSession().getHandler();
		
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		
		h.closeSession(c.getSession().getRemoteAddress());
	}
	
	@Test
	public void testReadException() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		c = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		
		c.getSession().write(nop());
		
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		waitFor(100);
		
		s.throwInRead = true;
		c.getSession().write(nop());
		s.waitForSessionEnding(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DR|NOP()|EXC|SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals(1, s.throwInReadCount.get());
		assertTrue(getDelegate(s.getSession()).isOpen());
		assertFalse(s.getSession().isOpen());
		s.stop(TIMEOUT);
		
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.codecPipeline = codec();
		s.codecPipeline.getPipeline().remove("2");
		s.startServer();
		
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|M(NOP[])|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		waitFor(100);
		
		s.throwInRead = true;
		c.getSession().write(nop());
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|M(NOP[])|EXC|SCL|SEN|", s.getRecordedData(true));
		assertEquals(1, s.throwInReadCount.get());
		assertTrue(getDelegate(s.getSession()).isOpen());
		assertFalse(s.getSession().isOpen());
		s.stop(TIMEOUT);
		
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.codecPipeline2 = codec();
		s.codecPipeline2.getPipeline().remove("2");
		s.startServer();
		s.throwInRead = true;
		c.getSession().write(nop());
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|M(NOP[])|EXC|SCL|SEN|", s.getRecordedData(true));
		assertEquals(1, s.throwInReadCount.get());
		assertTrue(getDelegate(s.getSession()).isOpen());
		assertFalse(s.getSession().isOpen());
		s.stop(TIMEOUT);
			
	}
	
	@Test
	public void testSuperReadException() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		c = new DatagramHandler(PORT);
		c2 = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		c2.startClient();
		c.waitForSessionReady(TIMEOUT);
		c2.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", c2.getRecordedData(true));
		
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		DatagramSession session1 = s.getSession();
		
		c2.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		c2.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		assertEquals("DS|", c2.getRecordedData(true));
		DatagramSession session2 = s.getSession();
		
		s.throwInSuperRead = true;
		c.getSession().write(nop());
		s.waitForSessionEnding(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(100);
		assertFalse(s.getSession().isOpen());
		assertFalse(getDelegate(s.getSession()).isOpen());
		assertEquals("DR|EXC|SCL|SEN|EXC|SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals(1, s.throwInSuperReadCount.get());
		
		assertFalse(session1 == session2);
		assertFalse(session1.isOpen());
		assertFalse(session2.isOpen());
		s.stop(TIMEOUT);
		
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.codecPipeline = codec();
		s.codecPipeline2 = codec2();
		s.startServer();

		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP(Dd)|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		session1 = s.getSession();

		c2.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		c2.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP(Dd)|", s.getRecordedData(true));
		assertEquals("DS|", c2.getRecordedData(true));
		session2 = s.getSession();

		s.throwInSuperRead = true;
		s.codecPipeline2.getPipeline().remove("2");
		c.getSession().write(nop());
		s.waitForSessionEnding(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(100);
		assertFalse(s.getSession().isOpen());
		assertFalse(getDelegate(s.getSession()).isOpen());
		assertEquals("DR|EXC|SCL|SEN|EXC|SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals(100, s.throwInSuperReadCount.get());
		
		assertFalse(session1 == session2);
		assertFalse(session1.isOpen());
		assertFalse(session2.isOpen());
	}
	
	@Test
	public void testCloseInException() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		c = new DatagramHandler(PORT);
		c2 = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		c2.startClient();
		c.waitForSessionReady(TIMEOUT);
		c2.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", c2.getRecordedData(true));
		
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		DatagramSession session1 = s.getSession();
		
		c2.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		c2.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		assertEquals("DS|", c2.getRecordedData(true));
		DatagramSession session2 = s.getSession();
		
		s.exceptionClose = true;
		s.throwInSuperRead = true;
		c.getSession().write(nop());
		s.waitForSessionEnding(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(100);
		assertFalse(s.getSession().isOpen());
		assertFalse(getDelegate(s.getSession()).isOpen());
		assertEquals("DR|EXC|close|SCL|SEN|EXC|close|SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals(1, s.throwInSuperReadCount.get());
		
		assertFalse(session1 == session2);
		assertFalse(session1.isOpen());
		assertFalse(session2.isOpen());
		
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.startServer();

		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		session1 = s.getSession();
		
		c2.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		c2.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		assertEquals("DS|", c2.getRecordedData(true));
		session2 = s.getSession();
		waitFor(100);
		
		s.exceptionClose = true;
		s.throwInRead = true;
		c.getSession().write(nop());
		s.waitForSessionEnding(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(100);
		assertFalse(session1 == session2);
		assertFalse(session1.isOpen());
		assertEquals("DR|NOP()|EXC|close|SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		assertTrue(getDelegate(session2).isOpen());
		assertTrue(session2.isOpen());
		
	}	
	
	@Test
	public void testIncident() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.codecPipeline = codec();
		s.codecPipeline2 = codec2();
		c = new DatagramHandler(PORT);
		c2 = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		c2.startClient();
		c.waitForSessionReady(TIMEOUT);
		c2.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", c2.getRecordedData(true));

		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP(Dd)|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		DatagramSession session1 = s.getSession();
		
		c2.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		c2.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP(Dd)|", s.getRecordedData(true));
		assertEquals("DS|", c2.getRecordedData(true));
	
		codec2.decodeException = new Exception("E1");
		c.getSession().write(nop());
		s.waitForDataReceived(TIMEOUT);
		waitFor(100);
		assertEquals("DR|DECODING_PIPELINE_FAILURE|DECODING_PIPELINE_FAILURE|", s.getRecordedData(true));
		assertTrue(session1.isOpen());
		assertTrue(session1.isOpen());
		assertTrue(getDelegate(session1).isOpen());
		
		codec2.decodeException = null;
		codec2.encodeException = new Exception("E2");
		session1.write(nop());
		waitFor(100);
		assertEquals("ENCODING_PIPELINE_FAILURE|ENCODING_PIPELINE_FAILURE|", s.getRecordedData(true));
		assertTrue(session1.isOpen());
		assertTrue(session1.isOpen());
		assertTrue(getDelegate(session1).isOpen());
	}
	
	@Test
	public void testIncidentReturnedValue() throws Exception {
		s = new DatagramHandler(PORT);
		s.startServer();
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));

		DatagramServerHandler h = new DatagramServerHandler(new IDatagramHandlerFactory() {

			@Override
			public IDatagramHandler create(SocketAddress remoteAddress) {
				return null;
			}
		});
		
		Field f = DatagramServerHandler.class.getDeclaredField("sessions");
		f.setAccessible(true);
		@SuppressWarnings("unchecked")
		Map<SocketAddress, DatagramServerSession> sessions = (Map<SocketAddress, DatagramServerSession>) f.get(h);	
		SocketAddress a1 = new InetSocketAddress(1001);
		SocketAddress a2 = new InetSocketAddress(1002);
		SocketAddress a3 = new InetSocketAddress(1003);
		
		DatagramServerSession s1 = new DatagramServerSession(s.getSession(), a1, new Handler(false));
		DatagramServerSession s2 = new DatagramServerSession(s.getSession(), a2, new Handler(false));
		DatagramServerSession s3 = new DatagramServerSession(s.getSession(), a3, new Handler(true));
		
		assertFalse(h.incident(null, null));

		sessions.put(a1, s1);
		assertFalse(h.incident(null, null));

		sessions.put(a3, s3);
		assertTrue(h.incident(null, null));
		
		sessions.put(a2, s2);
		assertTrue(h.incident(null, null));
		
		sessions.remove(a3);
		assertFalse(h.incident(null, null));
		
	}
	
	@Test
	public void testCloseInSessionCreatedEvent() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.closeInEvent = EventType.SESSION_CREATED;
		c = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		c.getSession().write(nop());
		s.waitForSessionEnding(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("SCR|SEN|", s.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, s.getSession().closing);
	}

	@Test
	public void testCloseInSessionOpenedEvent() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.closeInEvent = EventType.SESSION_OPENED;
		c = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		c.getSession().write(nop());
		s.waitForSessionEnding(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("SCR|SOP|SCL|SEN|", s.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, s.getSession().closing);
	}

	@Test
	public void testCloseInSessionReadyEvent() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.closeInEvent = EventType.SESSION_READY;
		c = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		c.getSession().write(nop());
		s.waitForSessionEnding(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|SCL|SEN|", s.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, s.getSession().closing);
	}
	
	@Test
	public void testCloseInSessionCloseEvent() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.closeInEvent = EventType.SESSION_CLOSED;
		c = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		s.getSession().close();
		waitFor(100);
		s.waitForSessionEnding(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|DR|NOP()|SCL|SEN|", s.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, s.getSession().closing);
	}
	
	@Test
	public void testCloseInSessionEndingEvent() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.closeInEvent = EventType.SESSION_ENDING;
		c = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		s.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|DR|NOP()|SCL|SEN|", s.getRecordedData(true));
		assertEquals(ClosingState.FINISHED, s.getSession().closing);
	}
	
	class TestTimerTask implements ITimerTask {
		boolean canceled;
		
		@Override
		public void cancelTask() {
			canceled = true;
		}
	}
	
	@Test
	public void testCloseTimeWaitWithConnectedChannel() throws Exception {
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		c.useDatagramServerHandler = true;
		c.timer = new DefaultTimer();
		c.reopenBlockedInterval = 500;
		s.startServer();
		c.startClient();
		Map<SocketAddress, ITimerTask> timers = getTimers(c.getSession().getHandler());
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		waitFor(100);
		DatagramSession superSession = c.getSession();
		superSession.write(nop());
		s.waitForDataRead(TIMEOUT);
		waitFor(100);
		assertEquals("DR|$NOP()|", s.getRecordedData(true));
		assertEquals("", c.getRecordedData(true));
		SocketAddress address = c.getSession().getLocalAddress();
		s.getSession().send(address, nop());
		c.waitForSessionReady(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|SCL|SEN|", c.getRecordedData(true));
		assertEquals(1, timers.size());
		s.getSession().send(address, nop());
		waitFor(300);
		assertEquals("", c.getRecordedData(true));
		s.getSession().send(address, nop());
		waitFor(150);
		assertEquals("", c.getRecordedData(true));
		waitFor(60);
		assertEquals(0, timers.size());
		s.getSession().send(address, nop());
		waitFor(10);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", c.getRecordedData(true));
	}
	
	@Test
	public void testCloseTimeWait() throws Exception {
		//test wait time
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.reopenBlockedInterval = 500;
		s.timer = new DefaultTimer();
		c = new DatagramHandler(PORT);
		s.startServer();
		DatagramSession superSession = s.getSession();
		Map<SocketAddress, ITimerTask> timers = getTimers(s.getSession().getHandler());
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals(0, timers.size());
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		s.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals(1, timers.size());
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c.getSession().write(nop());
		waitFor(300);
		c.getSession().write(nop());
		waitFor(150);
		assertEquals("", s.getRecordedData(true));
		waitFor(60);
		assertEquals(0, timers.size());
		c.getSession().write(nop());
		waitFor(10);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		TestTimerTask task = new TestTimerTask();
		timers.put(s.getSession().getRemoteAddress(), task);
		assertEquals(1, timers.size());
		s.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals(1, timers.size());
		assertTrue(task.canceled);
		assertFalse(task == timers.values().iterator().next());
		s.stop(TIMEOUT);
		DatagramServerHandler h = (DatagramServerHandler) superSession.getHandler();
		h.setReopenBlockedTimer(superSession);
		
		//test with wait time = 0
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.reopenBlockedInterval = 0;
		s.timer = new DefaultTimer();
		s.startServer();
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		s.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c.getSession().write(nop());
		waitFor(20);		
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		s.stop(TIMEOUT);
		
		//test with no timer
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.reopenBlockedInterval = 500;
		s.startServer();
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		s.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c.getSession().write(nop());
		waitFor(20);		
		assertEquals("SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		s.stop(TIMEOUT);
		
	}
	
	@Test
	public void testSendToOtherClient() throws Exception {
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.timer = new DefaultTimer();
		s.startServer();
		c.startClient();
		
		c.waitForSessionReady(TIMEOUT);
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		
		c2 = new DatagramHandler(PORT+1);
		c2.startServer();
		c2.waitForSessionReady(TIMEOUT);
		SocketAddress a = new InetSocketAddress("127.0.0.1", PORT+1);
		
		s.getRecordedData(true);
		c.getRecordedData(true);
		c2.getRecordedData(true);
		s.getSession().write(nop("1"));
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(1)|", c.getRecordedData(true));
		s.getSession().send(a,  nop("2"));
		c2.waitForDataRead(TIMEOUT);
		assertEquals("DR|$NOP(2)|", c2.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.timer = new DefaultTimer();
		s.codecPipeline = codec();
		c.codecPipeline = codec();
		s.startServer();
		c.startClient();
		
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|NOP(ed)|", s.getRecordedData(true));
		c.waitForDataSent(TIMEOUT);
		assertEquals("DS|", c.getRecordedData(true));
		
		s.getSession().write(nop("3"));
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(3ed)|", c.getRecordedData(true));
		s.getSession().send(a,  nop("4"));
		c2.waitForDataRead(TIMEOUT);
		assertEquals("DR|$NOP(4e)|", c2.getRecordedData(true));
	}
	
	class Handler extends AbstractDatagramHandler {
		boolean incidentResult;
		
		Handler (boolean incidentResult) {
			this.incidentResult = incidentResult;
		}

		@Override
		public void read(byte[] data) {
		}

		@Override
		public void read(SocketAddress remoteAddress, byte[] datagram) {
		}
		
		@Override
		public boolean incident(SessionIncident incident, Throwable t) {
			return incidentResult;

		}
	}
		
}
