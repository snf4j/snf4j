package org.snf4j.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngine;

import org.junit.Test;
import org.snf4j.core.codec.DefaultCodecExecutor;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.IEngineSession;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.session.SSLEngineCreateException;
import org.snf4j.core.timer.DefaultTimer;

public class DTLSSessionTest extends DTLSTest {

	final StringBuilder clientMode = new StringBuilder();
	
	DefaultSessionConfig testConfig = new DefaultSessionConfig() {
		
		@Override
		public SSLEngine createSSLEngine(boolean clientMode) throws SSLEngineCreateException {
			DTLSSessionTest.this.clientMode.append(clientMode ? "C" : "S");
			return super.createSSLEngine(clientMode);
		}
	};
	
	
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
		s = new DTLSSession("Test1", a, h, false);
		assertEquals("CS", clientMode.toString());
		
		s = new DTLSSession(a, h, true);
		assertEquals("Test2", s.getName());
		assertTrue(h == s.getHandler());
		assertTrue(a == af.get(s));
		assertEquals("CSC", clientMode.toString());
		s = new DTLSSession(a, h, false);
		assertEquals("CSCS", clientMode.toString());
		
		s = new DTLSSession("Test1",  h, true);
		assertEquals("Test1", s.getName());
		assertTrue(h == s.getHandler());
		assertNull(af.get(s));
		assertEquals("CSCSC", clientMode.toString());
		s = new DTLSSession("Test1", h, false);
		assertEquals("CSCSCS", clientMode.toString());
	
		s = new DTLSSession(h, true);
		assertEquals("Test2", s.getName());
		assertTrue(h == s.getHandler());
		assertNull(af.get(s));
		assertEquals("CSCSCSC", clientMode.toString());
		s = new DTLSSession(h, false);
		assertEquals("CSCSCSCS", clientMode.toString());
		
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
		s.timer = new DefaultTimer();
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
		s.maxWaitTimeForReady = 1000;
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
		s.maxWaitTimeForReady = 1000;
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
		s.maxWaitTimeForReady = 1000;
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
	
	}
	
	@Test
	public void testBeginHandshake() throws Exception {
		assumeJava9();
		assumeSuccessfulHandshake();
		
		s = new DatagramHandler(PORT);
		s.useDatagramServerHandler = true;
		s.ssl = true;
		c = new DatagramHandler(PORT);
		c.ssl = true;
		s.startServer();
		c.startClient();
		assertReady(c, s);
		
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
		c.maxWaitTimeForReady = 500;
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
}
