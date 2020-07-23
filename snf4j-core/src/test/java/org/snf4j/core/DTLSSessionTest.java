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
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.List;

import javax.net.ssl.SSLEngine;

import org.junit.Test;
import org.snf4j.core.codec.DefaultCodecExecutor;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.HandshakeTimeoutException;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.IEngineSession;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.session.SSLEngineCreateException;
import org.snf4j.core.timer.DefaultTimer;
import org.snf4j.core.timer.TestTimer;

public class DTLSSessionTest extends DTLSTest {

	final StringBuilder clientMode = new StringBuilder();
	
	SocketAddress remoteAddress;
	
	DefaultSessionConfig testConfig = new DefaultSessionConfig() {
		
		@Override
		public SSLEngine createSSLEngine(boolean clientMode) throws SSLEngineCreateException {
			DTLSSessionTest.this.clientMode.append(clientMode ? "C" : "S");
			remoteAddress = null;
			return super.createSSLEngine(clientMode);
		}
		
		@Override
		public SSLEngine createSSLEngine(SocketAddress remoteAddress, boolean clientMode) throws SSLEngineCreateException {
			DTLSSessionTest.this.clientMode.append(clientMode ? "C" : "S");
			DTLSSessionTest.this.remoteAddress = remoteAddress;
			return super.createSSLEngine(clientMode);
		}
	};
	
	@Test
	public void testNoSessionTimerException() throws Exception {
		s = new DatagramHandler(PORT);
		s.ssl = true;
		s.useDatagramServerHandler = true;
		s.startServer();
		
		System.setProperty(Constants.IGNORE_NO_SESSION_TIMER_EXCEPTION, "0");
		c = new DatagramHandler(PORT);
		c.ssl = true;
		try {
			c.startClient(); fail();
		}
		catch (IllegalStateException e) {
		}
		c.stop(TIMEOUT);
		
		System.setProperty(Constants.IGNORE_NO_SESSION_TIMER_EXCEPTION, "");
		c = new DatagramHandler(PORT);
		c.ssl = true;
		try {
			c.startClient(); fail();
		}
		catch (IllegalStateException e) {
		}
		c.stop(TIMEOUT);
		
		System.setProperty(Constants.IGNORE_NO_SESSION_TIMER_EXCEPTION, "1");
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		c.stop(TIMEOUT);

		System.clearProperty(Constants.IGNORE_NO_SESSION_TIMER_EXCEPTION);
		c = new DatagramHandler(PORT);
		c.ssl = true;
		try {
			c.startClient(); fail();
		}
		catch (IllegalStateException e) {
		}
		c.stop(TIMEOUT);
		
		s.getRecordedData(true);
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.timer = new DefaultTimer();
		c.startClient();
		c.waitForSessionOpen(TIMEOUT);
		waitFor(100);
		assertEquals("", s.getRecordedData(true));
		c.stop(TIMEOUT);
		
		s.timer = new DefaultTimer();
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.timer = new DefaultTimer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		
		System.setProperty(Constants.IGNORE_NO_SESSION_TIMER_EXCEPTION, "1");
		s.timer = null;
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
	}
	
	@Test
	public void testConstructor() throws Exception {
		SocketAddress a = address(101);
		TestDatagramHandler h = new TestDatagramHandler("Test2") {
			@Override
			public ISessionConfig getConfig() {
				return testConfig;
			}
		};
		Field af = EngineDatagramSession.class.getDeclaredField("remoteAddress");
		Field wf = EngineDatagramSession.class.getDeclaredField("wrapper");
		
		af.setAccessible(true);
		wf.setAccessible(true);
		
		DTLSSession s = new DTLSSession("Test1", a, h, true);
		assertEquals("Test1", s.getName());
		assertTrue(h == s.getHandler());
		assertTrue(a == af.get(s));
		assertEquals("C", clientMode.toString());
		assertTrue(a == remoteAddress);
		remoteAddress = null;
		s = new DTLSSession("Test1", a, h, false);
		assertEquals("CS", clientMode.toString());
		assertTrue(a == remoteAddress);
		remoteAddress = null;
		
		s = new DTLSSession(a, h, true);
		assertEquals("Test2", s.getName());
		assertTrue(h == s.getHandler());
		assertTrue(a == af.get(s));
		assertEquals("CSC", clientMode.toString());
		assertTrue(a == remoteAddress);
		remoteAddress = null;
		s = new DTLSSession(a, h, false);
		assertEquals("CSCS", clientMode.toString());
		assertTrue(a == remoteAddress);
		
		s = new DTLSSession("Test1",  h, true);
		assertEquals("Test1", s.getName());
		assertTrue(h == s.getHandler());
		assertNull(af.get(s));
		assertEquals("CSCSC", clientMode.toString());
		assertNull(remoteAddress);
		remoteAddress = a;
		s = new DTLSSession("Test1", h, false);
		assertEquals("CSCSCS", clientMode.toString());
		assertNull(remoteAddress);
		remoteAddress = a;
	
		s = new DTLSSession(h, true);
		assertEquals("Test2", s.getName());
		assertTrue(h == s.getHandler());
		assertNull(af.get(s));
		assertEquals("CSCSCSC", clientMode.toString());
		assertNull(remoteAddress);
		remoteAddress = a;
		s = new DTLSSession(h, false);
		assertEquals("CSCSCSCS", clientMode.toString());
		assertNull(remoteAddress);
		remoteAddress = a;

	}
	
	private void testConnectedToConnected(DefaultCodecExecutor codec) throws Exception {
		DatagramHandler s2 = new DatagramHandler(PORT);
		s2.startServer();
		s2.waitForSessionReady(TIMEOUT);
		
		String ed = codec != null ? "ed" : "";
		
		s = new DatagramHandler(PORT);
		s.ssl = true;
		s.connected = true;
		s.sslClientMode = false;
		s.codecPipeline = codec;
		s.startServer();
		s.waitForSessionOpen(TIMEOUT);
		s2.stop(TIMEOUT);
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.localAddress = s.getSession().getRemoteAddress();
		c.remoteAddress = s.getSession().getLocalAddress();
		c.codecPipeline = codec;
		
		c.startClient();
		assertReady(c, s, "SCR|SOP|DR+|DS+|RDY|DR+|", "SCR|SOP|DR+|DS+|RDY|DS|");
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DR|NOP("+ed+")|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		s.getSession().write(nop());
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		waitFor(50);
		assertEquals("DR|NOP("+ed+")|", c.getRecordedData(true));
		assertEquals("DS|", s.getRecordedData(true));
		
		//send to other remote
		s2 = new DatagramHandler(PORT+1);
		s2.startServer();
		s2.waitForSessionReady(TIMEOUT);
		c.getSession().send(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT+1), nop());//.sync(TIMEOUT);
		s.waitForDataReceived(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(50);
		assertEquals("SCR|SOP|RDY|", s2.getRecordedData(true));
		assertEquals("DR|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		
		//send to null remote
		c.getSession().send(null, nop("2")).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DR|NOP(2"+ed+")|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		
		//send to remote (no remote address in the wrapper)
		c.getSession().send(s.getSession().getLocalAddress(), nop("3")).sync(TIMEOUT);
		s.waitForDataReceived(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(50);
		assertEquals("DR|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		s2.stop(TIMEOUT);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		

		//send to remote (remote address in the wrapper)
		s2 = new DatagramHandler(PORT);
		s2.startServer();
		s2.waitForSessionReady(TIMEOUT);
		
		s = new DatagramHandler(PORT);
		s.ssl = true;
		s.connected = true;
		s.sslClientMode = false;
		s.codecPipeline = codec;
		s.startServer();
		s.waitForSessionOpen(TIMEOUT);
		s2.stop(TIMEOUT);
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.sslRemoteAddress = true;
		c.localAddress = s.getSession().getRemoteAddress();
		c.remoteAddress = s.getSession().getLocalAddress();
		c.codecPipeline = codec;
		c.startClient();
		assertReady(c, s, "SCR|SOP|DR+|DS+|RDY|DR+|", "SCR|SOP|DR+|DS+|RDY|DS|");
		c.getSession().send(s.getSession().getLocalAddress(), nop("3")).sync(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(3"+ed+")|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
	}
	
	@Test
	public void testConnectedToConnected() throws Exception {
		testConnectedToConnected(codec());
		testConnectedToConnected(null);
	}
	
	private void testNotConnectedToNotConnected(DefaultCodecExecutor codec) throws Exception {
		String ed = codec != null ? "ed" : "";
		String de = codec != null ? "de" : "";
		String d = codec != null ? "d" : "";
		
		s = new DatagramHandler(PORT);
		s.ssl = true;
		s.sslClient = true;
		s.sslClientMode = false;
		s.sslRemoteAddress = true;
		s.localAddress = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT);
		s.remoteAddress = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT+1);
		s.codecPipeline = codec;
		s.startServer();
		c = new DatagramHandler(PORT+1);
		c.ssl = true;
		c.sslClient = true;
		c.sslClientMode = true;
		c.sslRemoteAddress = true;
		c.localAddress = s.remoteAddress;
		c.remoteAddress = s.localAddress;
		c.codecPipeline = codec;
		c.startServer();
		assertReady(c, s);
		c.getSession().write(nop()).sync(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP("+ed+")|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		s.getSession().write(nop()).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP("+ed+")|", c.getRecordedData(true));
		assertEquals("DS|", s.getRecordedData(true));
		//send to other remote
		s2 = new DatagramHandler(PORT+2);
		s2.codecPipeline = codec;
		s2.startServer();
		s2.waitForSessionReady(TIMEOUT);
		s2.getRecordedData(true);
		c.getSession().send(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT+2), nop("4")).sync(TIMEOUT);
		s2.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(50);
		assertEquals("DR|$NOP(4"+ed+")|", s2.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		s2.stop(TIMEOUT);
		//send to null remote
		c.getSession().send(null, nop("2")).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DR|NOP(2"+ed+")|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		//send to remote
		c.getSession().send(s.getSession().getLocalAddress(), nop("3")).sync(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DR|NOP(3"+ed+")|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		//receive from other remote
		s2 = new DatagramHandler(PORT);
		s2.startClient();
		s2.waitForSessionReady(TIMEOUT);
		s2.getRecordedData(true);
		s2.getSession().write(new Packet(PacketType.ECHO).toBytes());
		s2.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE("+de+")|", s2.getRecordedData(true));
		assertEquals("DR|$ECHO("+d+")|DS|",s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		s2.stop(TIMEOUT);
	}
	
	@Test
	public void testNotConnectedToNotConnected() throws Exception {
		testNotConnectedToNotConnected(codec());
		testNotConnectedToNotConnected(null);
	}
	
	@Test
	public void testClose() throws Exception {
		assumeJava9();

		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.timer = new TestTimer();
		s.reopenBlockedInterval = 0;
		s.ssl = true;
		c = new DatagramHandler(PORT);
		c.ssl = true;
		
		//closed by client
		s.startServer();
		c.startClient();
		assertReady(c, s);
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		c.getSession().close();
		c.getSession().key = null;
		c.getSession().close();

		//quickly closed by client
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.startClient();
		assertReady(c, s);
		c.getSession().quickClose();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		c.getSession().quickClose();
		c.getSession().key = null;
		c.getSession().quickClose();

		//dirty closed by client
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.startClient();
		assertReady(c, s);
		c.getSession().dirtyClose();
		c.waitForSessionEnding(TIMEOUT);
		waitFor(500);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		s.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		c.getSession().dirtyClose();
		c.getSession().key = null;
		c.getSession().dirtyClose();
		
		//closed by server
		s.handshakeTimeout = 1000;
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.startClient();
		assertReady(c, s);
		s.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		waitFor(960);
		assertEquals("DR|DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|SCR|SOP|DR|", s.getRecordedData(true));
		waitFor(50);
		assertEquals("EXC|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		
		//quickly closed by server
		s.handshakeTimeout = 1000;
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.startClient();
		assertReady(c, s);
		s.getSession().quickClose();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		waitFor(960);
		assertEquals("DR|DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|SCR|SOP|DR|", s.getRecordedData(true));
		waitFor(50);
		assertEquals("EXC|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);

		//dirty closed by server
		s.handshakeTimeout = 1000;
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.startClient();
		assertReady(c, s);
		s.getSession().dirtyClose();
		s.waitForSessionEnding(TIMEOUT);
		waitFor(100);
		assertEquals("", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		assertEquals(0, ((TestTimer)s.timer).getSize());
	
	}
	
	@Test
	public void testBeginHandshake() throws Exception {
		assumeJava9();
		assumeSuccessfulRehandshake();
		
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.ssl = true;
		c = new DatagramHandler(PORT);
		c.ssl = true;
		s.startServer();
		c.startClient();
		assertReady(c, s);
		assertEquals("" + c.getSession().getLocalAddress() + "|false", s.engineArguments);
		
		c.getSession().write(new Packet(PacketType.ECHO).toBytes());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|ECHO()|DS|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE()|", c.getRecordedData(true));
		
		((IEngineSession)c.getSession()).beginHandshake();
		waitFor(100);
		
		assertEquals(TLS1_3 ? "DR|DS|" : "DR+|DS+|", getRecordedData(s));
		assertEquals(TLS1_3 ? "DR|DS|" : "DR+|DS+|", getRecordedData(c));
		
		c.getSession().write(new Packet(PacketType.ECHO).toBytes());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|ECHO()|DS|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE()|", c.getRecordedData(true));
		
		((IEngineSession)c.getSession()).beginLazyHandshake();
		waitFor(100);
		assertEquals("", getRecordedData(s));
		assertEquals("", getRecordedData(c));

		c.getSession().write(new Packet(PacketType.ECHO).toBytes());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR+|DS+|ECHO()|DS|", getRecordedData(s));
		assertEquals("DR+|DS+|ECHO_RESPONSE()|", getRecordedData(c));
		
		((IEngineSession)s.getSession()).beginHandshake();
		waitFor(100);
		assertEquals(TLS1_3 ? "DR|DS|" : "DR+|DS+|", getRecordedData(s));
		assertEquals(TLS1_3 ? "DR|DS|" : "DR+|DS+|", getRecordedData(c));

		c.getSession().write(new Packet(PacketType.ECHO).toBytes());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|ECHO()|DS|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE()|", c.getRecordedData(true));
		
		((IEngineSession)s.getSession()).beginLazyHandshake();
		waitFor(100);
		assertEquals("", getRecordedData(s));
		assertEquals("", getRecordedData(c));

		s.getSession().write(new Packet(PacketType.ECHO).toBytes());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR+|DS+|ECHO()|DS|", getRecordedData(c));
		assertEquals("DR+|DS+|ECHO_RESPONSE()|", getRecordedData(s));
		
		c.getSession().dirtyClose();
		s.getSession().dirtyClose();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		
	}
	
	@Test
	public void testReconnectFromOtherSession() throws Exception {
		assumeJava9();
		
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.ssl = true;
		c = new DatagramHandler(PORT);
		c.ssl = true;

		s.startServer();
		c.startClient();
		assertReady(c, s);
		DatagramSession origSession = c.getSession();
		DatagramHandler origC = c;
		
		c.getSession().write(new Packet(PacketType.ECHO).toBytes());
		c.waitForDataRead(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
		
		//try to connect with new ssl engine to the same server session
		c = new DatagramHandler(PORT);
		c.timer = new DefaultTimer();
		c.handshakeTimeout = 500;
		c.ssl = true;
		c.localAddress = origSession.getLocalAddress();
		c.startClient();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForDataReceived(TIMEOUT);
		assertEquals("SCR|SOP|DS|EXC|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|", s.getRecordedData(true));
		c.stop(TIMEOUT);

		//check the original session
		c = origC;
		origSession.write(new Packet(PacketType.ECHO).toBytes());
		s.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		waitFor(50);
		assertEquals("DR|ECHO()|DS|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE()|", c.getRecordedData(true));
		origSession.close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
	}

	void assertWrite(DatagramHandler read, DatagramHandler sent, String expRead, String expSent) throws InterruptedException {
		read.waitForDataRead(TIMEOUT);
		sent.waitForDataSent(TIMEOUT);
		assertEquals(expRead, read.getRecordedData(true));
		assertEquals(expSent, sent.getRecordedData(true));
	}
	
	void assertWrite(DatagramHandler read, String expRead) throws InterruptedException {
		read.waitForDataRead(TIMEOUT);
		assertEquals(expRead, read.getRecordedData(true));
	}

	private void testWrite(DefaultCodecExecutor codec) throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.ssl = true;
		s.codecPipeline = codec;
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.sslRemoteAddress = true;
		c.sslClient = true;
		c.sslClientMode = true;
		c.remoteAddress = address(PORT);
		c.localAddress = address(PORT+1);
		c.codecPipeline = codec;
		
		String ed = codec == null ? "" : "ed";

		s.startServer();
		c.startServer();
		assertReady(c, s);
		DatagramSession cs = c.getSession();
		DatagramSession ss = s.getSession();
		
		cs.write(nop()).sync(TIMEOUT);
		assertWrite(s,c,"DR|NOP("+ed+")|", "DS|");
		cs.writenf(nop("1"));
		assertWrite(s,c,"DR|NOP(1"+ed+")|", "DS|");
		ss.write(nop("2")).sync(TIMEOUT);
		assertWrite(c,s,"DR|NOP(2"+ed+")|", "DS|");
		ss.writenf(nop("3"));
		assertWrite(c,s,"DR|NOP(3"+ed+")|", "DS|");
		
		cs.write(nopp("10").toBytes(3, 10), 3, 5).sync(TIMEOUT);
		assertWrite(s,c,"DR|NOP(10"+ed+")|", "DS|");
		cs.writenf(nopp("11").toBytes(3, 10), 3, 5);
		assertWrite(s,c,"DR|NOP(11"+ed+")|", "DS|");
		ss.write(nopp("12").toBytes(3, 10), 3, 5).sync(TIMEOUT);
		assertWrite(c,s,"DR|NOP(12"+ed+")|", "DS|");
		ss.writenf(nopp("13").toBytes(3, 10), 3, 5);
		assertWrite(c,s,"DR|NOP(13"+ed+")|", "DS|");

		cs.write(ByteBuffer.wrap(nop())).sync(TIMEOUT);
		assertWrite(s,c,"DR|NOP("+ed+")|", "DS|");
		cs.writenf(ByteBuffer.wrap(nop("1")));
		assertWrite(s,c,"DR|NOP(1"+ed+")|", "DS|");
		ss.write(ByteBuffer.wrap(nop("2"))).sync(TIMEOUT);
		assertWrite(c,s,"DR|NOP(2"+ed+")|", "DS|");
		ss.writenf(ByteBuffer.wrap(nop("3")));
		assertWrite(c,s,"DR|NOP(3"+ed+")|", "DS|");

		cs.write(ByteBuffer.wrap(nopp("00").toBytes(0, 10)), 5).sync(TIMEOUT);
		assertWrite(s,c,"DR|NOP(00"+ed+")|", "DS|");
		cs.writenf(ByteBuffer.wrap(nopp("01").toBytes(0, 10)), 5);
		assertWrite(s,c,"DR|NOP(01"+ed+")|", "DS|");
		ss.write(ByteBuffer.wrap(nopp("02").toBytes(0, 10)), 5).sync(TIMEOUT);
		assertWrite(c,s,"DR|NOP(02"+ed+")|", "DS|");
		ss.writenf(ByteBuffer.wrap(nopp("03").toBytes(0, 10)), 5);
		assertWrite(c,s,"DR|NOP(03"+ed+")|", "DS|");

		cs.write((Object)nop()).sync(TIMEOUT);
		assertWrite(s,c,"DR|NOP("+ed+")|", "DS|");
		cs.writenf((Object)nop("1"));
		assertWrite(s,c,"DR|NOP(1"+ed+")|", "DS|");
		ss.write((Object)nop("2")).sync(TIMEOUT);
		assertWrite(c,s,"DR|NOP(2"+ed+")|", "DS|");
		ss.writenf((Object)nop("3"));
		assertWrite(c,s,"DR|NOP(3"+ed+")|", "DS|");

		cs.write((Object)ByteBuffer.wrap(nop())).sync(TIMEOUT);
		assertWrite(s,c,"DR|NOP("+ed+")|", "DS|");
		cs.writenf((Object)ByteBuffer.wrap(nop("1")));
		assertWrite(s,c,"DR|NOP(1"+ed+")|", "DS|");
		ss.write((Object)ByteBuffer.wrap(nop("2"))).sync(TIMEOUT);
		assertWrite(c,s,"DR|NOP(2"+ed+")|", "DS|");
		ss.writenf((Object)ByteBuffer.wrap(nop("3")));
		assertWrite(c,s,"DR|NOP(3"+ed+")|", "DS|");
		
		SocketAddress a1 = null;
		SocketAddress a2 = null;
		for (int i=0; i<2; ++i) {
			cs.send(a1, nop()).sync(TIMEOUT);
			assertWrite(s,c,"DR|NOP("+ed+")|", "DS|");
			cs.sendnf(a1, nop("1"));
			assertWrite(s,c,"DR|NOP(1"+ed+")|", "DS|");
			ss.send(a2, nop("2")).sync(TIMEOUT);
			assertWrite(c,s,"DR|NOP(2"+ed+")|", "DS|");
			ss.sendnf(a2, nop("3"));
			assertWrite(c,s,"DR|NOP(3"+ed+")|", "DS|");
			
			cs.send(a1,nopp("10").toBytes(3, 10), 3, 5).sync(TIMEOUT);
			assertWrite(s,c,"DR|NOP(10"+ed+")|", "DS|");
			cs.sendnf(a1,nopp("11").toBytes(3, 10), 3, 5);
			assertWrite(s,c,"DR|NOP(11"+ed+")|", "DS|");
			ss.send(a2,nopp("12").toBytes(3, 10), 3, 5).sync(TIMEOUT);
			assertWrite(c,s,"DR|NOP(12"+ed+")|", "DS|");
			ss.sendnf(a2,nopp("13").toBytes(3, 10), 3, 5);
			assertWrite(c,s,"DR|NOP(13"+ed+")|", "DS|");

			cs.send(a1,ByteBuffer.wrap(nop())).sync(TIMEOUT);
			assertWrite(s,c,"DR|NOP("+ed+")|", "DS|");
			cs.sendnf(a1,ByteBuffer.wrap(nop("1")));
			assertWrite(s,c,"DR|NOP(1"+ed+")|", "DS|");
			ss.send(a2,ByteBuffer.wrap(nop("2"))).sync(TIMEOUT);
			assertWrite(c,s,"DR|NOP(2"+ed+")|", "DS|");
			ss.sendnf(a2,ByteBuffer.wrap(nop("3")));
			assertWrite(c,s,"DR|NOP(3"+ed+")|", "DS|");

			cs.send(a1,ByteBuffer.wrap(nopp("00").toBytes(0, 10)), 5).sync(TIMEOUT);
			assertWrite(s,c,"DR|NOP(00"+ed+")|", "DS|");
			cs.sendnf(a1,ByteBuffer.wrap(nopp("01").toBytes(0, 10)), 5);
			assertWrite(s,c,"DR|NOP(01"+ed+")|", "DS|");
			ss.send(a2,ByteBuffer.wrap(nopp("02").toBytes(0, 10)), 5).sync(TIMEOUT);
			assertWrite(c,s,"DR|NOP(02"+ed+")|", "DS|");
			ss.sendnf(a2,ByteBuffer.wrap(nopp("03").toBytes(0, 10)), 5);
			assertWrite(c,s,"DR|NOP(03"+ed+")|", "DS|");

			cs.send(a1,(Object)nop()).sync(TIMEOUT);
			assertWrite(s,c,"DR|NOP("+ed+")|", "DS|");
			cs.sendnf(a1,(Object)nop("1"));
			assertWrite(s,c,"DR|NOP(1"+ed+")|", "DS|");
			ss.send(a2,(Object)nop("2")).sync(TIMEOUT);
			assertWrite(c,s,"DR|NOP(2"+ed+")|", "DS|");
			ss.sendnf(a2,(Object)nop("3"));
			assertWrite(c,s,"DR|NOP(3"+ed+")|", "DS|");

			cs.send(a1,(Object)ByteBuffer.wrap(nop())).sync(TIMEOUT);
			assertWrite(s,c,"DR|NOP("+ed+")|", "DS|");
			cs.sendnf(a1,(Object)ByteBuffer.wrap(nop("1")));
			assertWrite(s,c,"DR|NOP(1"+ed+")|", "DS|");
			ss.send(a2,(Object)ByteBuffer.wrap(nop("2"))).sync(TIMEOUT);
			assertWrite(c,s,"DR|NOP(2"+ed+")|", "DS|");
			ss.sendnf(a2,(Object)ByteBuffer.wrap(nop("3")));
			assertWrite(c,s,"DR|NOP(3"+ed+")|", "DS|");
			
			a1 = address(PORT);
			a2 = address(PORT+1);
		}
		
		s2 = new DatagramHandler(PORT+2);
		s2.codecPipeline = codec;
		s2.startServer();
		s2.waitForSessionReady(TIMEOUT);
		s2.getRecordedData(true);
		a1 = address(PORT+2);
		
		cs.send(a1, nop()).sync(TIMEOUT);
		assertWrite(s2,c,"DR|$NOP("+ed+")|", "DS|");
		cs.sendnf(a1, nop("1"));
		assertWrite(s2,c,"DR|$NOP(1"+ed+")|", "DS|");
		ss.send(a1, nop("2")).sync(TIMEOUT);
		assertWrite(s2,"DR|$NOP(2"+ed+")|");
		ss.sendnf(a1, nop("3"));
		assertWrite(s2,"DR|$NOP(3"+ed+")|");
		
		cs.send(a1,nopp("10").toBytes(3, 10), 3, 5).sync(TIMEOUT);
		assertWrite(s2,c,"DR|$NOP(10"+ed+")|", "DS|");
		cs.sendnf(a1,nopp("11").toBytes(3, 10), 3, 5);
		assertWrite(s2,c,"DR|$NOP(11"+ed+")|", "DS|");
		ss.send(a1,nopp("12").toBytes(3, 10), 3, 5).sync(TIMEOUT);
		assertWrite(s2,"DR|$NOP(12"+ed+")|");
		ss.sendnf(a1,nopp("13").toBytes(3, 10), 3, 5);
		assertWrite(s2,"DR|$NOP(13"+ed+")|");

		cs.send(a1,ByteBuffer.wrap(nop())).sync(TIMEOUT);
		assertWrite(s2,c,"DR|$NOP("+ed+")|", "DS|");
		cs.sendnf(a1,ByteBuffer.wrap(nop("1")));
		assertWrite(s2,c,"DR|$NOP(1"+ed+")|", "DS|");
		ss.send(a1,ByteBuffer.wrap(nop("2"))).sync(TIMEOUT);
		assertWrite(s2,"DR|$NOP(2"+ed+")|");
		ss.sendnf(a1,ByteBuffer.wrap(nop("3")));
		assertWrite(s2,"DR|$NOP(3"+ed+")|");

		cs.send(a1,ByteBuffer.wrap(nopp("00").toBytes(0, 10)), 5).sync(TIMEOUT);
		assertWrite(s2,c,"DR|$NOP(00"+ed+")|", "DS|");
		cs.sendnf(a1,ByteBuffer.wrap(nopp("01").toBytes(0, 10)), 5);
		assertWrite(s2,c,"DR|$NOP(01"+ed+")|", "DS|");
		ss.send(a1,ByteBuffer.wrap(nopp("02").toBytes(0, 10)), 5).sync(TIMEOUT);
		assertWrite(s2,"DR|$NOP(02"+ed+")|");
		ss.sendnf(a1,ByteBuffer.wrap(nopp("03").toBytes(0, 10)), 5);
		assertWrite(s2,"DR|$NOP(03"+ed+")|");

		cs.send(a1,(Object)nop()).sync(TIMEOUT);
		assertWrite(s2,c,"DR|$NOP("+ed+")|", "DS|");
		cs.sendnf(a1,(Object)nop("1"));
		assertWrite(s2,c,"DR|$NOP(1"+ed+")|", "DS|");
		ss.send(a1,(Object)nop("2")).sync(TIMEOUT);
		assertWrite(s2,"DR|$NOP(2"+ed+")|");
		ss.sendnf(a1,(Object)nop("3"));
		assertWrite(s2,"DR|$NOP(3"+ed+")|");

		cs.send(a1,(Object)ByteBuffer.wrap(nop())).sync(TIMEOUT);
		assertWrite(s2,c,"DR|$NOP("+ed+")|", "DS|");
		cs.sendnf(a1,(Object)ByteBuffer.wrap(nop("1")));
		assertWrite(s2,c,"DR|$NOP(1"+ed+")|", "DS|");
		ss.send(a1,(Object)ByteBuffer.wrap(nop("2"))).sync(TIMEOUT);
		assertWrite(s2,"DR|$NOP(2"+ed+")|");
		ss.sendnf(a1,(Object)ByteBuffer.wrap(nop("3")));
		assertWrite(s2,"DR|$NOP(3"+ed+")|");
		s2.stop(TIMEOUT);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

	}
	
	@Test
	public void testWrite() throws Exception {
		testWrite(null);
		testWrite(codec());
	}
	
	@Test
	public void testEngineExceptionAndNull() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.ssl = true;
		s.nullEngine = true;
		c = new DatagramHandler(PORT);
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		c.getSession().write(new Packet(PacketType.ECHO).toBytes());
		s.waitForSessionReady(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("SCR|SOP|RDY|DR|ECHO()|DS|", s.getRecordedData(true));
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE()|", c.getRecordedData(true));
		c.getSession().close();
		s.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		
		s.nullEngine = false;
		s.engineException = true;
		s.getRecordedData(true);
		c = new DatagramHandler(PORT);
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		c.getSession().write(nop());
		waitFor(100);
		assertEquals("", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}
	
	@Test
	public void testExceptionInException() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.ssl = true;
		c = new DatagramHandler(PORT);
		c.ssl = true;
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		c.getSession().write(nop());
		s.waitForSessionReady(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
		c.throwInRead = true;
		c.throwInException = true;
		s.getSession().write(nop());
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|NOP()|EXC|SCL|SEN|", c.getRecordedData(true));
		assertEquals(1, c.throwInExceptionCount.get());
		c.getSession().exception(new Exception());
		waitFor(100);
		assertEquals("", c.getRecordedData(true));
	}

	@Test
	public void testCloseInSessionCreatedEvent() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.ssl = true;
		s.timer = new TestTimer();
		s.closeInEvent = EventType.SESSION_CREATED;
		c = new DatagramHandler(PORT);
		c.ssl = true;
		s.startServer();
		c.startClient();
		c.waitForSessionOpen(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SEN|", s.getRecordedData(true));
		assertEquals("SCR|SOP|DS|", c.getRecordedData(true));
		assertEquals("", ((TestTimer)s.timer).getTrace(true));
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);

		s.closeInEvent = null;
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.closeInEvent = EventType.SESSION_CREATED;
		c.startClient();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SEN|", c.getRecordedData(true));
	}

	@Test
	public void testCloseInSessionOpenedEvent() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.ssl = true;
		s.timer = new TestTimer();
		s.closeInEvent = EventType.SESSION_OPENED;
		c = new DatagramHandler(PORT);
		c.ssl = true;
		s.startServer();
		c.startClient();
		c.waitForSessionOpen(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|SCL|SEN|", s.getRecordedData(true));
		assertEquals("SCR|SOP|DS|", c.getRecordedData(true));
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		c.stop(TIMEOUT);

		s.closeInEvent = null;
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.closeInEvent = EventType.SESSION_OPENED;
		c.startClient();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|SCL|SEN|", c.getRecordedData(true));
		s.stop(TIMEOUT);
		assertEquals(0, ((TestTimer)s.timer).getSize());
	}
	
	@Test
	public void testCloseInSessionReadyEvent() throws Exception {
		assumeJava9();
		
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.ssl = true;
		s.timer = new TestTimer();
		s.closeInEvent = EventType.SESSION_READY;
		c = new DatagramHandler(PORT);
		c.ssl = true;
		s.startServer();
		c.startClient();
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|DR+|DS+|RDY|SCL|SEN|", getRecordedData(s));
		waitFor(100);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|DR+|DS+|RDY|DR+|DS|SCL|SEN|", getRecordedData(c));
		c.stop(TIMEOUT);
		
		s.closeInEvent = null;
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.closeInEvent = EventType.SESSION_READY;
		c.startClient();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SOP|DR+|DS+|RDY|DR|DS|SCL|SEN|", getRecordedData(c));
		s.stop(TIMEOUT);
		assertEquals(0, ((TestTimer)s.timer).getSize());
	}
	
	@Test
	public void testCloseInSessionCloseEvent() throws Exception {
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.ssl = true;
		s.timer = new TestTimer();
		s.closeInEvent = EventType.SESSION_CLOSED;
		c = new DatagramHandler(PORT);
		c.ssl = true;
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		waitFor(50);
		s.getRecordedData(true);
		c.getRecordedData(true);
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		
		s.closeInEvent = null;
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.closeInEvent = EventType.SESSION_CLOSED;
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		waitFor(50);
		s.getRecordedData(true);
		c.getRecordedData(true);
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		s.stop(TIMEOUT);
		assertEquals(0, ((TestTimer)s.timer).getSize());
		
	}
	
	void prepareProxy(boolean serverProxyAction, boolean clientProxyAction) throws Exception {
		p = new DatagramProxy(PORT);
		p.start(TIMEOUT);

		s = new DatagramHandler(PORT+1);
		s.useDatagramServerHandler = true;
		s.localAddress = address(PORT+1);
		s.ssl = true;
		s.timer = new TestTimer();
		s.handshakeTimeout = 120000;
		s.proxyAction = serverProxyAction;
		c = new DatagramHandler(PORT);
		c.ssl = true;
		c.timer = new TestTimer();
		c.handshakeTimeout = 120000;
		c.proxyAction = clientProxyAction;
		p.peer1 = s;
		p.peer2 = c;
	}
	
	@Test
	public void testRetransmissionDoubleEachPacket() throws Exception {
		assumeJava9();
		
		prepareProxy(true, true);
		p.action = p.DUPLICATE_ACTION;
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		
		s.getRecordedData(true);
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		waitFor(50);
		assertEquals("DR|NOP()|DR|NOP()|", s.getRecordedData(true));
		
		s.getRecordedData(true);
		c.getRecordedData(true);
		c.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertEquals("DR|SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		
		TestTimer t = (TestTimer)s.timer;
		assertEquals(1, t.getSize());
		assertEquals("4999|", t.get());
		assertEquals("1000|4999|120000|", t.getDelays());
		t = (TestTimer)c.timer;
		assertEquals(0, t.getSize());
		assertEquals("1000|120000|", t.getDelays());
	}
	
	@Test
	public void testRetransmissionWithOnePreviousPacket() throws Exception {
		assumeJava9();
		
		prepareProxy(true, true);
		p.action = p.WITH_1PPREVIOUS_PACKET_ACTION;
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		
		s.getRecordedData(true);
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		waitFor(50);
		assertEquals("DR|NOP()|DR|", s.getRecordedData(true));
		
		s.getRecordedData(true);
		c.getRecordedData(true);
		c.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertEquals("DR|SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		
		TestTimer t = (TestTimer)s.timer;
		assertEquals(1, t.getSize());
		assertEquals("4999|", t.get());
		assertEquals("1000|4999|120000|", t.getDelays());
		t = (TestTimer)c.timer;
		assertEquals(0, t.getSize());
		assertEquals("1000|120000|", t.getDelays());
	}
	
	@Test
	public void testRetransmissionLostEveryPacketOnce() throws Exception {
		assumeJava9();
		
		prepareProxy(true, true);
		p.action = p.LOST_EVERY_PACKET_ONCE_ACTION;
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT*10);
		s.waitForSessionReady(TIMEOUT*10);
		
		p.action = p.DEFAULT_ACTION;
		s.getRecordedData(true);
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		waitFor(50);
		assertEquals("DR|NOP()|", s.getRecordedData(true));
		
		s.getRecordedData(true);
		c.getRecordedData(true);
		c.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertEquals("DR|SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));

		TestTimer t = (TestTimer)s.timer;
		assertEquals(1, t.getSize());
		assertEquals("4999|", t.get());
		assertEquals("1000|2000|4999|120000|", t.getDelays());
		t = (TestTimer)c.timer;
		assertEquals(0, t.getSize());
		assertEquals("1000|2000|120000|", t.getDelays());
	
	}
	
	@Test
	public void testWriteBeforeHandshakeCompletes() throws Exception {
		assumeJava9();
		
		prepareProxy(true, false);
		p.action = p.LOST_FIRST_1PACKET_ACTION;
		s.startServer();
		c.startClient();
		c.waitForSessionOpen(TIMEOUT);
		c.getSession().write(nop());
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertTrue(s.getRecordedData(true).endsWith("DR|NOP()|"));
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		
		TestTimer t = (TestTimer)s.timer;
		assertEquals(1, t.getSize());
		assertEquals("4999|", t.get());
		assertEquals("1000|4999|120000|", t.getDelays());
		t = (TestTimer)c.timer;
		assertEquals(0, t.getSize());
		assertEquals("1000|2000|120000|", t.getDelays());
	}
	
	@Test
	public void testWriteBeforeSecondHandshakeCompletes() throws Exception {
		assumeSuccessfulRehandshake();
		
		prepareProxy(true, false);
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getSession().write(new Packet(PacketType.ECHO).toBytes());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		s.getRecordedData(true);
		
		p.action = p.LOST_FIRST_1PACKET_ACTION;
		((IEngineSession)c.getSession()).beginHandshake();
		waitFor(100);
		IFuture<Void> f = c.getSession().write(nop("1"));
		waitFor(800);
		assertFalse(f.isDone());
		s.waitForDataRead(TIMEOUT);
		assertTrue(s.getRecordedData(true).endsWith("DR|NOP(1)|"));
		assertTrue(f.isSuccessful());	
	}
	
	@Test
	public void testHandshakeTimeout() throws Exception {
		assumeJava9();
		
		//initial handshake
		prepareProxy(true, false);
		TestTimer t = (TestTimer)c.timer;
		p.action = p.LOST_EVERY_PACKET_ACTION;
		c.handshakeTimeout = 500;
		s.startServer();
		c.startClient();
		c.waitForSessionOpen(TIMEOUT);
		waitFor(100);
		assertEquals("1000|500|", t.getTrace(true));
		c.getRecordedData(true);
		waitFor(300);
		assertEquals("", c.getRecordedData(true));
		waitFor(200);
		assertEquals("EXC|SCL|SEN|", c.getRecordedData(true));
		assertTrue(c.getSession().getReadyFuture().cause() instanceof HandshakeTimeoutException);
		assertEquals("c1000|", t.getTrace(true));
		assertEquals(1, t.getExpiredSize());
		assertEquals("500|", t.getExpired());
		assertEquals(0, t.getSize());
		assertEquals("500|1000|", t.getDelays());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		p.stop(TIMEOUT);
		
		//second handshake
		prepareProxy(true, false);
		t = (TestTimer)c.timer;
		c.handshakeTimeout = 500;
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		((TestTimer)c.timer).getTrace(true);
		c.getSession().write(new Packet(PacketType.ECHO).toBytes());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
		assertEquals("", ((TestTimer)c.timer).getTrace(true));
		p.action = p.LOST_EVERY_PACKET_ACTION;
		((IEngineSession)c.getSession()).beginHandshake();
		waitFor(100);
		assertEquals("500|1000|", t.getTrace(true));
		c.getRecordedData(true);
		waitFor(300);
		assertEquals("", c.getRecordedData(true));
		waitFor(200);
		assertEquals("EXC|SCL|SEN|", c.getRecordedData(true));
		assertTrue(c.getSession().getReadyFuture().isSuccessful());
		assertTrue(c.getSession().getCloseFuture().cause() instanceof HandshakeTimeoutException);
		assertEquals("c1000|", t.getTrace(true));
		assertEquals("500|", t.getExpired());
		assertEquals(0, t.getSize());
		assertEquals("500|1000|", t.getDelays());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		p.stop(TIMEOUT);
		
		//handshake after close
		if (HANDSHAKING_AFTER_CLOSE) {
			prepareProxy(true, false);
			t = (TestTimer)c.timer;
			c.handshakeTimeout = 500;
			c.waitForCloseMessage = true;
			s.startServer();
			c.startClient();
			c.waitForSessionReady(TIMEOUT);
			s.waitForSessionReady(TIMEOUT);
			t.getTrace(true);
			p.action = p.LOST_EVERY_PACKET_ACTION;
			c.getSession().close();
			waitFor(100);
			assertEquals("1000|500|", t.getTrace(true));
			c.getRecordedData(true);
			waitFor(300);
			assertEquals("", c.getRecordedData(true));
			waitFor(200);
			assertEquals("EXC|SCL|SEN|", c.getRecordedData(true));
			c.waitForSessionEnding(TIMEOUT);
			assertTrue(c.getSession().getCloseFuture().cause() instanceof HandshakeTimeoutException);
			assertEquals("c1000|", t.getTrace(true));
			assertEquals("500|", t.getExpired());
			assertEquals(0, t.getSize());
			assertEquals("500|1000|", t.getDelays());
		}
	}
	
	@Test
	public void testRetransmissionTimeout() throws Exception {
		assumeJava9();
		
		prepareProxy(true, false);
		TestTimer t = (TestTimer)c.timer;
		p.action = p.LOST_FIRST_3PACKETS_ACTION;
		s.startServer();
		c.startClient();
		c.waitForDataSent(TIMEOUT);
		
		List<byte[]> cp = p.get(c.getSession().getLocalAddress());
		List<byte[]> sp = p.get(c.localAddress);
		
		waitFor(100);
		assertEquals(1, cp.size());
		assertEquals("1000|120000|", t.getTrace(true));
		waitFor(800);
		assertEquals(1, cp.size());
		assertEquals("", t.getTrace(true));
		waitFor(200);
		assertEquals(2, cp.size());
		assertEquals("2000|", t.getTrace(true));
		waitFor(1800);
		assertEquals(2, cp.size());
		assertEquals("", t.getTrace(true));
		waitFor(200);
		assertEquals(3, cp.size());
		assertEquals("4000|", t.getTrace(true));
		waitFor(3800);
		assertEquals(3, cp.size());
		assertEquals("", t.getTrace(true));
		assertEquals(0, sp.size());
		waitFor(200);
		assertTrue(cp.size() >= 4);
		
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("1000|2000|4000|8000|120000|", t.getDelays());

		t.getTrace(true);
		s.getRecordedData(true);
		c.getSession().write(nop());
		s.waitForDataRead(TIMEOUT);
		waitFor(50);
		assertEquals("DR|NOP()|", s.getRecordedData(true));
		assertEquals("", t.getTrace(true));
		
		s.getRecordedData(true);
		c.getRecordedData(true);
		c.getSession().close();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		waitFor(50);
		assertEquals("DR|SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("1000|2000|4000|", t.getExpired());
		assertEquals(0, t.getSize());
		assertEquals("1000|2000|4000|8000|120000|", t.getDelays());
		
	}
	
	@Test
	public void testRetransmissionAfterClose() throws Exception {
		assumeHandshakingAfterClose();
		
		prepareProxy(true, false);
		TestTimer t = (TestTimer)c.timer;
		c.waitForCloseMessage = true;
		c.handshakeTimeout = 3000;
		s.handshakeTimeout = 3000;
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		waitFor(50);
		
		p.action = p.LOST_FIRST_1PACKET_ACTION;
		t.getTrace(true);
		c.getRecordedData(true);
		s.getRecordedData(true);
		c.getSession().close();
		waitFor(100);
		assertEquals("1000|3000|", t.getTrace(true));
		waitFor(800);
		assertEquals("", t.getTrace(true));
		waitFor(200);
		assertEquals("", t.getTrace(true));
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|EXC|SCL|SEN|", c.getRecordedData(true));
		assertTrue(c.getSession().getCloseFuture().cause() instanceof HandshakeTimeoutException);
		assertEquals("", t.getTrace(true));
		assertEquals("1000|3000|", t.getExpired());
		assertEquals(0, t.getSize());
		waitFor(100);
		assertEquals("", s.getRecordedData(true));
		
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		p.stop(TIMEOUT);

		prepareProxy(true, false);
		t = (TestTimer)c.timer;
		c.handshakeTimeout = 3000;
		s.handshakeTimeout = 3000;
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);

		p.action = p.LOST_FIRST_1PACKET_ACTION;
		t.getTrace(true);
		c.getRecordedData(true);
		s.getRecordedData(true);
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("3000|c3000|", t.getTrace(true));
		assertEquals("", t.getExpired());
		assertEquals(0, t.getSize());
		
	}
	
	@Test
	public void testRetransmissionAfterCloseNoHandshaking() throws Exception {
		assumeNoHandshakingAfterClose();

		prepareProxy(true, false);
		TestTimer t = (TestTimer)c.timer;
		c.waitForCloseMessage = true;
		c.handshakeTimeout = 3000;
		s.handshakeTimeout = 3000;
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		waitFor(50);
		
		p.action = p.LOST_FIRST_1PACKET_ACTION;
		t.getTrace(true);
		c.getRecordedData(true);
		s.getRecordedData(true);
		c.getSession().close();
		waitFor(500);
		assertEquals("", t.getTrace(true));
		assertEquals("DS|", c.getRecordedData(true));
		c.getSession().dirtyClose();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("", t.getExpired());
		assertEquals(0, t.getSize());

		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		p.stop(TIMEOUT);

		prepareProxy(true, false);
		t = (TestTimer)c.timer;
		c.handshakeTimeout = 3000;
		s.handshakeTimeout = 3000;
		s.startServer();
		c.startClient();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);

		p.action = p.LOST_FIRST_1PACKET_ACTION;
		t.getTrace(true);
		c.getRecordedData(true);
		s.getRecordedData(true);
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("", t.getTrace(true));
		assertEquals("", t.getExpired());
		assertEquals(0, t.getSize());
		
	}
	
}
