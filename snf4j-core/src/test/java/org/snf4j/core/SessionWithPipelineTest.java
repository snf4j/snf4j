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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.allocator.TestAllocator;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISessionPipeline;
import org.snf4j.core.session.IStreamSession;
import org.snf4j.core.session.IllegalSessionStateException;

public class SessionWithPipelineTest {

	long TIMEOUT = 2000;
	
	int PORT = 7777;

	Server s;

	Client c;
	
	boolean directAllocator;
	
	boolean releasableAllocator;
	
	boolean optimizeDataCopying;
	
	boolean useTestSession;
	
	@Before
	public void before() {
		s = c = null;
	}
	
	@After
	public void after() throws InterruptedException {
		if (c != null) c.stop(TIMEOUT);
		if (s != null) s.stop(TIMEOUT);
	}
	
	void waitFor(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}
	
	String replaceDSDR(String s) {
		return s.replace("DS|", "").replace("DR|", "");
	}
	
	void start(boolean... ssls) throws Exception {
		s = new Server(PORT, ssls[ssls.length-1]);
		s.waitForCloseMessage = true;
		s.allocator = new TestAllocator(directAllocator, releasableAllocator);
		s.ignoreAvailableException = true;
		s.optimizeDataCopying = optimizeDataCopying;
		s.useTestSession = useTestSession;
		c = new Client(PORT, ssls[ssls.length-1]);
		c.waitForCloseMessage = true;
		c.allocator = new TestAllocator(directAllocator, releasableAllocator);
		c.ignoreAvailableException = true;
		c.optimizeDataCopying = optimizeDataCopying;
		c.useTestSession = useTestSession;
		
		for (int i=0; i<ssls.length-1; ++i) {
			s.addPreSession("S" + i, ssls[i], null);
			c.addPreSession("C" + i, ssls[i], null);
		}

		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", replaceDSDR(c.getRecordedData(true)));
		assertEquals("SCR|SOP|RDY|", replaceDSDR(s.getRecordedData(true)));
	}
	
	void stop() throws Exception {
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}
	
	void assertSwitch(Server s) throws Exception {
		s.waitForSessionEnding(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertTrue(s.session.isOpen());
	}

	void assertSwitch(Server s, Client c) throws Exception {
		assertSwitch(s);
		assertSwitch(c);
	}

	void assertEnding(Server s, Client c) throws Exception {
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
	}
	
	void write(Server s, Packet... packets) {
		int len = 0;
		
		for (Packet p: packets) {
			len += p.toBytes().length;
		}
		
		byte[] bytes = new byte[len];
		int off = 0;
		
		for (Packet p: packets) {
			byte[] d = p.toBytes();
			
			System.arraycopy(d, 0, bytes, off, d.length);
			off += d.length;
		}
		s.session.write(bytes);
	}
	
	@Test
	public void testNoSwitching() throws Exception {
		s = new Server(PORT);
		s.getPipeline = true;
		s.start();
		c = new Client(PORT);
		c.getPipeline = true;
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
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertFalse(c.session.isOpen());
		assertFalse(s.session.isOpen());
		stop();
		
		start(false, false);
		ISessionPipeline<IStreamSession> p = c.session.getPipeline();
		TestStreamSession testSession = new TestStreamSession(c.createHandler());
		p.add("x", testSession);
		testSession.copyInBufferException = true;
		c.session.close();
		waitFor(100);
		assertEquals("SCL|SEN|SCR|SOP|RDY|EXC|SCL|SEN|SCR|EXC|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|SCR|SEN|", s.getRecordedData(true));
		stop();
		
	}

	@Test
	public void testNoSwitchingSsl() throws Exception {
		s = new Server(PORT,true);
		s.start();
		c = new Client(PORT,true);
		StreamSession session = c.createSession();
		session.getPipeline();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", replaceDSDR(c.getRecordedData(true)));
		assertEquals("SCR|SOP|RDY|", replaceDSDR(s.getRecordedData(true)));
		c.write(new Packet(PacketType.ECHO));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertEquals("ECHO_RESPONSE()|", replaceDSDR(c.getRecordedData(true)));
		assertEquals("ECHO()|", replaceDSDR(s.getRecordedData(true)));
		c.session.close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|DS|SCL|SEN|", s.getRecordedData(true));
		assertFalse(c.session.isOpen());
		assertFalse(s.session.isOpen());
		stop();
		
		start(true, true);
		ISessionPipeline<IStreamSession> p = c.session.getPipeline();
		TestStreamSession testSession = new TestStreamSession(c.createHandler());
		p.add("x", testSession);
		testSession.copyInBufferException = true;
		c.session.close();
		waitFor(100);
		assertEquals("SCL|SEN|SCR|SOP|RDY|EXC|SCL|SEN|SCR|EXC|SEN|", replaceDSDR(c.getRecordedData(true)));
		assertEquals("SCL|SEN|SCR|SOP|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", replaceDSDR(s.getRecordedData(true)));
		stop();

		start(true, true);
		p = c.session.getPipeline();
		TestOwnSSLSession testSSLSession = new TestOwnSSLSession(c.createHandler(), true);
		p.add("x", testSSLSession);
		testSSLSession.copyInBufferException = true;
		c.session.close();
		waitFor(100);
		assertEquals("SCL|SEN|SCR|SOP|EXC|SCL|SEN|SCR|EXC|SEN|", replaceDSDR(c.getRecordedData(true)));
		assertEquals("SCL|SEN|SCR|SOP|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", replaceDSDR(s.getRecordedData(true)));
		stop();
	}
	
	@Test
	public void testSwitching() throws Exception {
		start(false, false);
		write(c, new Packet(PacketType.WRITE_CLOSE_AND_CLOSE), new Packet(PacketType.NOP));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertSwitch(s,c);
		waitFor(100);
		assertEquals("DS|DR|CLOSE()|SCL|SEN|SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("DR|WRITE_CLOSE_AND_CLOSE()|DS|SCL|SEN|SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		c.resetDataLocks();
		s.resetDataLocks();
		c.write(new Packet(PacketType.ECHO));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		c.session.close();
		assertEnding(s,c);
		assertEquals("DS|DR|ECHO_RESPONSE()|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|ECHO()|DS|SCL|SEN|", s.getRecordedData(true));
		stop();

		start(false, false);
		write(c, new Packet(PacketType.WRITE_CLOSE_AND_QUICK_CLOSE), new Packet(PacketType.NOP));
		c.waitForDataSent(TIMEOUT);
		assertSwitch(s);
		waitFor(100);
		assertEquals("DS|", c.getRecordedData(true));
		assertEquals("DR|WRITE_CLOSE_AND_QUICK_CLOSE()|SCL|SEN|SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		c.resetDataLocks();
		s.resetDataLocks();
		c.write(new Packet(PacketType.ECHO));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		waitFor(100);
		assertEquals("DS|DR|ECHO_RESPONSE()|", c.getRecordedData(true));
		assertEquals("DR|ECHO()|DS|", s.getRecordedData(true));
		s.session.close();
		waitFor(100);
		assertEquals("SCL|SEN|SCR|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		stop();
		
		directAllocator = true; 
		start(false, false);
		write(c, new Packet(PacketType.WRITE_CLOSE_AND_CLOSE), new Packet(PacketType.NOP));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertSwitch(s,c);
		waitFor(100);
		assertEquals("DS|DR|CLOSE()|SCL|SEN|SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("DR|WRITE_CLOSE_AND_CLOSE()|DS|SCL|SEN|SCR|SOP|RDY|DR|NOP()|", s.getRecordedData(true));
		c.session.close();
		assertEnding(s,c);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		stop();
		
		releasableAllocator = true;
		optimizeDataCopying = true;
		start(false, false);
		write(c, new Packet(PacketType.WRITE_CLOSE_AND_CLOSE), new Packet(PacketType.NOP));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertSwitch(s,c);
		waitFor(100);
		assertEquals("DS|DR|BUF|CLOSE()|SCL|SEN|SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("DR|BUF|WRITE_CLOSE_AND_CLOSE()|DS|SCL|SEN|SCR|SOP|RDY|DR|BUF|NOP()|", s.getRecordedData(true));
		c.session.close();
		assertEnding(s,c);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		stop();
	}

	@Test
	public void testSwitchingSsl() throws Exception {
		start(true, true);
		write(c, new Packet(PacketType.WRITE_AND_CLOSE));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertSwitch(s,c);
		waitFor(100);
		assertEquals("WRITE_AND_CLOSE_RESPONSE()|SCL|SEN|SCR|SOP|RDY|", replaceDSDR(c.getRecordedData(true)));
		assertEquals("WRITE_AND_CLOSE()|SCL|SEN|SCR|SOP|RDY|", replaceDSDR(s.getRecordedData(true)));
		c.resetDataLocks();
		s.resetDataLocks();
		c.write(new Packet(PacketType.ECHO));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		c.session.close();
		assertEnding(s,c);
		assertEquals("DS|DR|ECHO_RESPONSE()|DS|DR|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|ECHO()|DS|DR|DS|SCL|SEN|", s.getRecordedData(true));
		stop();

		start(true, true);
		write(c, new Packet(PacketType.WRITE_AND_QUICK_CLOSE));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertSwitch(s,c);
		waitFor(100);
		assertEquals("NOP()|SCL|SEN|SCR|SOP|RDY|", replaceDSDR(c.getRecordedData(true)));
		assertEquals("WRITE_AND_QUICK_CLOSE()|SCL|SEN|SCR|SOP|RDY|", replaceDSDR(s.getRecordedData(true)));
		c.resetDataLocks();
		s.resetDataLocks();
		c.write(new Packet(PacketType.ECHO));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		c.session.close();
		assertEnding(s,c);
		assertEquals("DS|DR|ECHO_RESPONSE()|DS|DR|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|ECHO()|DS|DR|DS|SCL|SEN|", s.getRecordedData(true));
		stop();
		
		start(true, false);
		write(c, new Packet(PacketType.WRITE_AND_CLOSE));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertSwitch(s,c);
		assertEquals("WRITE_AND_CLOSE_RESPONSE()|SCL|SEN|SCR|SOP|RDY|", replaceDSDR(c.getRecordedData(true)));
		assertEquals("WRITE_AND_CLOSE()|SCL|SEN|SCR|SOP|RDY|", replaceDSDR(s.getRecordedData(true)));
		c.resetDataLocks();
		s.resetDataLocks();
		c.write(new Packet(PacketType.ECHO));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		c.session.close();
		assertEnding(s,c);
		assertEquals("DS|DR|ECHO_RESPONSE()|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DR|ECHO()|DS|SCL|SEN|", s.getRecordedData(true));
		stop();

		start(false, true);
		write(c, new Packet(PacketType.WRITE_CLOSE_AND_CLOSE));
		c.waitForDataRead(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		assertSwitch(s,c);
		assertEquals("CLOSE()|SCL|SEN|SCR|SOP|RDY|", replaceDSDR(c.getRecordedData(true)));
		assertEquals("WRITE_CLOSE_AND_CLOSE()|SCL|SEN|SCR|SOP|RDY|", replaceDSDR(s.getRecordedData(true)));
		c.resetDataLocks();
		s.resetDataLocks();
		c.write(new Packet(PacketType.ECHO));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		c.session.close();
		assertEnding(s,c);
		assertEquals("ECHO_RESPONSE()|SCL|SEN|", replaceDSDR(c.getRecordedData(true)));
		assertEquals("ECHO()|SCL|SEN|", replaceDSDR(s.getRecordedData(true)));
		stop();
	}

	@Test
	public void testNoConnection() throws Exception {
		c = new Client(PORT, false);
		c.waitForCloseMessage = true;
		c.addPreSession("C", false, null);

		c.start();
		c.waitForSessionEnding(TIMEOUT*5);
		waitFor(100);
		assertEquals("SCR|EXC|SEN|SCR|EXC|SEN|", replaceDSDR(c.getRecordedData(true)));
		c.stop(TIMEOUT);
		
		c = new Client(PORT, false);
		c.waitForCloseMessage = true;
		c.addPreSession("C1", false, null);
		c.addPreSession("C2", false, null);

		c.start();
		c.waitForSessionEnding(TIMEOUT*5);
		waitFor(100);
		assertEquals("SCR|EXC|SEN|SCR|EXC|SEN|", replaceDSDR(c.getRecordedData(true)));
		assertTrue(c.preSessions.get(0).isCreated());
		assertFalse(c.preSessions.get(1).isCreated());
		c.stop(TIMEOUT);
	}

	@Test
	public void testNoConnectionSsl() throws Exception {
		c = new Client(PORT, true);
		c.waitForCloseMessage = true;
		c.addPreSession("C", true, null);

		c.start();
		c.waitForSessionEnding(TIMEOUT*5);
		waitFor(100);
		assertEquals("SCR|EXC|SEN|SCR|EXC|SEN|", replaceDSDR(c.getRecordedData(true)));
		c.stop(TIMEOUT);
		
		c = new Client(PORT, true);
		c.waitForCloseMessage = true;
		c.addPreSession("C1", true, null);
		c.addPreSession("C2", true, null);

		c.start();
		c.waitForSessionEnding(TIMEOUT*5);
		waitFor(100);
		assertEquals("SCR|EXC|SEN|SCR|EXC|SEN|", replaceDSDR(c.getRecordedData(true)));
		assertTrue(c.preSessions.get(0).isCreated());
		assertFalse(c.preSessions.get(1).isCreated());
		c.stop(TIMEOUT);
	}
	
	@Test
	public void testClosing() throws Exception {
		start(false, false);
		s.session.channel.close();
		waitFor(500);
		assertEquals("SCL|SEN|SCR|SEN|", c.getRecordedData(true));
		stop();
		
		start(false, false);
		c.registeredSession.close();
		waitFor(500);
		assertEquals("", c.getRecordedData(true));
		c.session.close();
		waitFor(500);
		assertEquals("SCL|SEN|SCR|SEN|", c.getRecordedData(true));
		stop();

		start(false, false);
		c.closeInEvent = EventType.SESSION_OPENED;
		c.closeType = StoppingType.GENTLE;
		c.session.close();
		waitFor(500);
		assertEquals("SCL|SEN|SCR|SOP|SCL|SEN|", c.getRecordedData(true));
		stop();
		
		start(false, false);
		c.throwInRead = true;
		s.write(new Packet(PacketType.NOP));
		waitFor(500);
		assertEquals("DR|NOP()|EXC|SCL|SEN|SCR|EXC|SEN|", c.getRecordedData(true));
		stop();
		
	}

	@Test
	public void testClosingSsl() throws Exception {
		start(true, true);
		s.session.channel.close();
		waitFor(500);
		assertEquals("SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|SCR|SEN|", replaceDSDR(c.getRecordedData(true)));
		stop();

		start(true, true);
		c.registeredSession.close();
		waitFor(500);
		assertEquals("", replaceDSDR(c.getRecordedData(true)));
		c.session.close();
		waitFor(500);
		assertEquals("DS|DR|SCL|SEN|SCR|SEN|", c.getRecordedData(true));
		stop();

		start(true, true);
		c.closeInEvent = EventType.SESSION_OPENED;
		c.closeType = StoppingType.GENTLE;
		c.session.close();
		waitFor(500);
		assertEquals("SCL|SEN|SCR|SOP|SCL|SEN|", replaceDSDR(c.getRecordedData(true)));
		stop();

		start(true, true);
		c.throwInRead = true;
		s.write(new Packet(PacketType.NOP));
		waitFor(500);
		assertEquals("NOP()|EXC|SCL|SEN|SCR|EXC|SEN|", replaceDSDR(c.getRecordedData(true)));
		stop();
	}
	
	@Test
	public void testClosingNotSwitched() throws Exception {
		start(false, false);
		stop();
		assertEquals("SCL|SEN|SCR|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|SCR|SEN|", s.getRecordedData(true));

		s = new Server(PORT);
		c = new Client(PORT);
		s.addPreSession("1", false, new TestSessionPipelineHandler(100).closePipeline(null));
		s.addPreSession("2", false, new TestSessionPipelineHandler(100));
		c.addPreSession("1", false, new TestSessionPipelineHandler(3,100));
		c.addPreSession("2", false, new TestSessionPipelineHandler(2,100));
		s.start();
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SEN|", s.getRecordedData(true));
		assertEquals("SCR|SEN|", c.getRecordedData(true));
		stop();

		s = new Server(PORT);
		c = new Client(PORT);
		s.addPreSession("1", false, new TestSessionPipelineHandler(100).closePipeline(new Exception("E")));
		s.addPreSession("2", false, new TestSessionPipelineHandler(100));
		c.addPreSession("1", false, new TestSessionPipelineHandler(3,100));
		c.addPreSession("2", false, new TestSessionPipelineHandler(2,100));
		s.start();
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|SEN|", s.getRecordedData(true));
		assertEquals("SCR|SEN|", c.getRecordedData(true));
		stop();
		
		s = new Server(PORT);
		c = new Client(PORT);
		s.addPreSession("1", false, new TestSessionPipelineHandler(100).closePipeline(2,null));
		s.addPreSession("2", false, new TestSessionPipelineHandler(100));
		c.addPreSession("1", false, new TestSessionPipelineHandler(3,100));
		c.addPreSession("2", false, new TestSessionPipelineHandler(2,100));
		s.start();
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SEN|", s.getRecordedData(true));
		assertEquals("SCR|SEN|", c.getRecordedData(true));
		stop();

		s = new Server(PORT);
		c = new Client(PORT);
		s.addPreSession("1", false, new TestSessionPipelineHandler(100).closePipeline(2,new Exception("E")));
		s.addPreSession("2", false, new TestSessionPipelineHandler(100));
		c.addPreSession("1", false, new TestSessionPipelineHandler(3,100));
		c.addPreSession("2", false, new TestSessionPipelineHandler(2,100));
		s.start();
		c.start();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|EXC|SEN|", s.getRecordedData(true));
		assertEquals("SCR|SEN|", c.getRecordedData(true));
		stop();
	}
	
	String getTrace(Server s, int i) {
		return ((TestSessionPipelineHandler)s.preSessions.get(i).getHandler()).getTrace();
	}
	
	@Test
	public void testMultiSwitching() throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);
		s.addPreSession("1", false, new TestSessionPipelineHandler(100));
		s.addPreSession("2", false, new TestSessionPipelineHandler(100));
		c.addPreSession("1", false, new TestSessionPipelineHandler(3,100));
		c.addPreSession("2", false, new TestSessionPipelineHandler(2,100));
		s.start();
		c.start();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("R3|R2|R1|R0|C|", getTrace(s,0));
		assertEquals("R3|R2|R1|R0|C|", getTrace(c,0));
		assertEquals("R2|R1|R0|C|", getTrace(s,1));
		assertEquals("R2|R1|R0|C|", getTrace(c,1));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		
		RuntimeException e = new RuntimeException();
		s = new Server(PORT);
		c = new Client(PORT);
		s.addPreSession("1", false, new TestSessionPipelineHandler(100));
		s.addPreSession("2", false, new TestSessionPipelineHandler(100));
		c.addPreSession("1", false, new TestSessionPipelineHandler(3,100).exception(2, e));
		c.addPreSession("2", false, new TestSessionPipelineHandler(2,100));
		s.start();
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SEN|", s.getRecordedData(true));
		assertEquals("SCR|EXC|SEN|", c.getRecordedData(true));
		assertEquals("R3|R2|", getTrace(s,0));
		assertEquals("R3|R2|E|", getTrace(c,0));
		assertEquals("", getTrace(s,1));
		assertEquals("", getTrace(c,1));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		e = new SessionTest.CloseControllingException("E1", ICloseControllingException.CloseType.GENTLE, new NullPointerException());
		s = new Server(PORT);
		c = new Client(PORT);
		s.addPreSession("1", false, new TestSessionPipelineHandler(100));
		s.addPreSession("2", false, new TestSessionPipelineHandler(100));
		c.addPreSession("1", false, new TestSessionPipelineHandler(3,100).exception(2, e));
		c.addPreSession("2", false, new TestSessionPipelineHandler(2,100));
		s.start();
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SEN|", s.getRecordedData(true).replace("EXC|SEN|", "SEN|"));
		assertEquals("SCR|EXC|SEN|", c.getRecordedData(true));
		assertEquals("R3|R2|R2|", getTrace(s,0).replace("R2|E|", "R2|"));
		assertEquals("R3|R2|E|", getTrace(c,0));
		assertEquals("", getTrace(s,1));
		assertEquals("", getTrace(c,1));
		assertFutures(c.preSessions.get(0), true);
		assertFutures(c.preSessions.get(1), false);
		assertFutures(s.preSessions.get(0), true);
		assertFutures(s.preSessions.get(1), false);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		s = new Server(PORT);
		c = new Client(PORT);
		TestSessionPipelineHandler h1 = new TestSessionPipelineHandler(100);
		TestSessionPipelineHandler h2 = new TestSessionPipelineHandler(100);
		TestSessionPipelineHandler h3 = new TestSessionPipelineHandler(3,100).exception(2, e);
		TestSessionPipelineHandler h4 = new TestSessionPipelineHandler(2,100);
		
		s.addPreSession("1", false, h1);
		s.addPreSession("2", false, h2);
		c.addPreSession("1", false, h3);
		c.addPreSession("2", false, h4);
		s.start();
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SEN|", s.getRecordedData(true).replace("EXC|SEN|", "SEN|"));
		assertEquals("SCR|EXC|SEN|", c.getRecordedData(true));
		assertEquals("R3|R2|R2|", getTrace(s,0).replace("R2|E|", "R2|"));
		assertEquals("R3|R2|E|", getTrace(c,0));
		assertEquals("", getTrace(s,1));
		assertEquals("", getTrace(c,1));
		assertFutures(c.preSessions.get(0), true);
		assertFutures(c.preSessions.get(1), false);
		assertFutures(s.preSessions.get(0), true);
		assertFutures(s.preSessions.get(1), false);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}
	
	void assertFutures(StreamSession s, boolean done) {
		assertEquals(done, s.getCreateFuture().isDone());
		assertEquals(done, s.getOpenFuture().isDone());
		assertEquals(done, s.getReadyFuture().isDone());
		assertEquals(done, s.getCloseFuture().isDone());
		assertEquals(done, s.getEndFuture().isDone());
	}

	@Test
	public void testSessionNotification() throws Exception {
		RuntimeException e = new RuntimeException();

		s = new Server(PORT);
		c = new Client(PORT);
		TestSessionPipelineHandler h1 = new TestSessionPipelineHandler(100);
		TestSessionPipelineHandler h2 = new TestSessionPipelineHandler(100);
		TestSessionPipelineHandler h3 = new TestSessionPipelineHandler(3,100).exception(2, e);
		TestSessionPipelineHandler h4 = new TestSessionPipelineHandler(2,100);
		TestSessionPipelineHandler h5 = new TestSessionPipelineHandler(2,100);
		TestSessionPipelineHandler h6 = new TestSessionPipelineHandler(2,100);
		
		((DefaultSessionConfig)h6.getConfig()).setAlwaysNotifiedBeingInPipeline(true);
		
		s.addPreSession("1", false, h1);
		s.addPreSession("2", false, h2);
		s.addPreSession("3", false, h5);
		c.addPreSession("1", false, h3);
		c.addPreSession("2", false, h4);
		c.addPreSession("3", false, h6);
		s.start();
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SEN|", s.getRecordedData(true).replace("EXC|SEN|", "SEN|"));
		assertEquals("SCR|EXC|SEN|", c.getRecordedData(true));
		assertEquals("R3|R2|", getTrace(s,0).replace("R2|E|", "R2|"));
		assertEquals("R3|R2|E|", getTrace(c,0));
		assertEquals("", getTrace(s,1));
		assertEquals("", getTrace(c,1));
		assertEquals("", getTrace(s,2));
		assertEquals("E|", getTrace(c,2));
		assertFutures(c.preSessions.get(0), true);
		assertFutures(c.preSessions.get(1), false);
		assertFutures(c.preSessions.get(2), true);
		assertFutures(s.preSessions.get(0), true);
		assertFutures(s.preSessions.get(1), false);
		assertFutures(s.preSessions.get(2), false);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		s = new Server(PORT);
		c = new Client(PORT);
		h1 = new TestSessionPipelineHandler(100);
		h2 = new TestSessionPipelineHandler(100);
		h3 = new TestSessionPipelineHandler(3,100).exception(2, e);
		h4 = new TestSessionPipelineHandler(2,100);
		h5 = new TestSessionPipelineHandler(2,100);
		h6 = new TestSessionPipelineHandler(2,100);
		
		((DefaultSessionConfig)h4.getConfig()).setAlwaysNotifiedBeingInPipeline(true);
		
		s.addPreSession("1", false, h1);
		s.addPreSession("2", false, h2);
		s.addPreSession("3", false, h5);
		c.addPreSession("1", false, h3);
		c.addPreSession("2", false, h4);
		c.addPreSession("3", false, h6);
		s.start();
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SEN|", s.getRecordedData(true).replace("EXC|SEN|", "SEN|"));
		assertEquals("SCR|EXC|SEN|", c.getRecordedData(true));
		assertEquals("R3|R2|", getTrace(s,0).replace("R2|E|", "R2|"));
		assertEquals("R3|R2|E|", getTrace(c,0));
		assertEquals("", getTrace(s,1));
		assertEquals("E|", getTrace(c,1));
		assertEquals("", getTrace(s,2));
		assertEquals("", getTrace(c,2));
		assertFutures(c.preSessions.get(0), true);
		assertFutures(c.preSessions.get(1), true);
		assertFutures(c.preSessions.get(2), false);
		assertFutures(s.preSessions.get(0), true);
		assertFutures(s.preSessions.get(1), false);
		assertFutures(s.preSessions.get(2), false);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		s = new Server(PORT);
		c = new Client(PORT);
		h1 = new TestSessionPipelineHandler(100);
		h2 = new TestSessionPipelineHandler(100);
		h3 = new TestSessionPipelineHandler(3,100).exception(2, e);
		h4 = new TestSessionPipelineHandler(2,100);
		h5 = new TestSessionPipelineHandler(2,100);
		h6 = new TestSessionPipelineHandler(2,100);
		
		((DefaultSessionConfig)h4.getConfig()).setAlwaysNotifiedBeingInPipeline(true);
		((DefaultSessionConfig)h6.getConfig()).setAlwaysNotifiedBeingInPipeline(true);
		
		s.addPreSession("1", false, h1);
		s.addPreSession("2", false, h2);
		s.addPreSession("3", false, h5);
		c.addPreSession("1", false, h3);
		c.addPreSession("2", false, h4);
		c.addPreSession("3", false, h6);
		s.start();
		c.start();
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCR|SEN|", s.getRecordedData(true).replace("EXC|SEN|", "SEN|"));
		assertEquals("SCR|EXC|SEN|", c.getRecordedData(true));
		assertEquals("R3|R2|", getTrace(s,0).replace("R2|E|", "R2|"));
		assertEquals("R3|R2|E|", getTrace(c,0));
		assertEquals("", getTrace(s,1));
		assertEquals("E|", getTrace(c,1));
		assertEquals("", getTrace(s,2));
		assertEquals("E|", getTrace(c,2));
		assertFutures(c.preSessions.get(0), true);
		assertFutures(c.preSessions.get(1), true);
		assertFutures(c.preSessions.get(2), true);
		assertFutures(s.preSessions.get(0), true);
		assertFutures(s.preSessions.get(1), false);
		assertFutures(s.preSessions.get(2), false);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}

	void assertClosed(Server s, int i) throws Exception {
		if (i == -1) {
			assertClosed(s.registeredSession);
		}
		else {
			assertClosed(s.preSessions.get(i));
		}
	}
	
	void assertClosed(StreamSession s) throws Exception {
		assertFalse(s.isOpen());
		try {
			s.write(new byte[1]);
			fail();
		}
		catch (IllegalSessionStateException e) {
		}
	}

	void assertClosed(StreamSession s, int i, boolean expected) {
		if (i >= 0) {
			Object key = s.getPipeline().getKeys().get(i);
			
			s = (StreamSession) s.getPipeline().get(key);
		}
		assertEquals(expected, s.closeCalled.get());
	}
	
	void assertClosing(Server s, int i) throws Exception {
		if (i == -1) {
			assertClosing(s.registeredSession);
		}
		else {
			assertClosing(s.preSessions.get(i));
		}
	}
	
	void assertClosing(StreamSession s) throws Exception {
		assertFalse(s.isOpen());
		assertTrue(s.write(new byte[1]).await(TIMEOUT).isCancelled());
	}
	
	@Test
	public void testPipelineClose() throws Exception {
		start(false);
		assertEquals(0, c.registeredSession.getPipeline().getKeys().size());
		c.registeredSession.getPipeline().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertClosed(c, -1);
		stop();
		
		start(false, false);
		assertEquals(1, c.registeredSession.getPipeline().getKeys().size());
		c.registeredSession.getPipeline().close();
		waitFor(100);
		assertEquals("SCL|SEN|SCR|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|SCR|SEN|", s.getRecordedData(true));
		assertClosed(c, -1);
		assertClosed(c, 0);
		stop();

		start(false, false, false);
		assertEquals(2, c.registeredSession.getPipeline().getKeys().size());
		c.registeredSession.getPipeline().close();
		waitFor(100);
		assertEquals("SCL|SEN|SCR|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|SCR|SEN|", s.getRecordedData(true));
		assertFutures(c.preSessions.get(0), true);
		assertFutures(c.preSessions.get(1), false);
		assertFutures(s.preSessions.get(0), true);
		assertFutures(s.preSessions.get(1), false);
		assertClosed(c, -1);
		assertClosed(c, 0);
		assertClosed(c, 1);
		assertClosed(c.registeredSession, 1, true);
		stop();

		start(false, false, false);
		assertEquals(2, c.registeredSession.getPipeline().getKeys().size());
		c.preSessions.get(0).close();
		waitFor(100);
		assertClosing(c, 0);
		assertTrue(c.preSessions.get(0).write(new byte[1]).await(TIMEOUT).isCancelled());
		assertFalse(c.preSessions.get(0).isOpen());
		assertEquals("SCL|SEN|SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		c.registeredSession.getPipeline().close();
		waitFor(100);
		assertEquals("SCL|SEN|SCR|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|SCR|SEN|", s.getRecordedData(true));
		assertFutures(c.preSessions.get(0), true);
		assertFutures(c.preSessions.get(1), true);
		assertFutures(s.preSessions.get(0), true);
		assertFutures(s.preSessions.get(1), false);
		assertClosed(c, -1);
		assertClosed(c, 0);
		assertClosed(c, 1);
		stop();
		
		start(false, false, false);
		assertEquals(2, c.registeredSession.getPipeline().getKeys().size());
		c.registeredSession.getPipeline().addFirst("1", new StreamSession(c.createHandler()));
		c.preSessions.get(0).close();
		waitFor(100);
		assertClosing(c, 0);
		assertEquals("SCL|SEN|SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("", s.getRecordedData(true));
		c.registeredSession.getPipeline().close();
		waitFor(100);
		assertEquals("SCL|SEN|SCR|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|SCR|SEN|", s.getRecordedData(true));
		assertFutures(c.preSessions.get(0), true);
		assertFutures(c.preSessions.get(1), true);
		assertFutures(s.preSessions.get(0), true);
		assertFutures(s.preSessions.get(1), false);
		assertClosed(c, -1);
		assertClosed(c, 0);
		assertClosed(c, 1);
		assertClosed(c.registeredSession, 0, false);
		stop();
		
		s = new Server(PORT);
		s.start();
		c = new Client(PORT);
		StreamSession session = c.createSession();
		session.getPipeline().close();
		assertClosed(session, -1, true);
		c.start();
		waitFor(100);
		assertEquals("SCR|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		
		c = new Client(PORT);
		session = c.createSession();
		session.getPipeline().add("1", new StreamSession(c.createHandler()));
		session.getPipeline().close();
		assertClosed(session, -1, true);
		assertClosed(session, 0, true);
		c.start();
		waitFor(100);
		assertEquals("SCR|SEN|SCR|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);

		c = new Client(PORT);
		session = c.createSession();
		session.getPipeline().add("1", new StreamSession(c.createHandler()));
		session.getPipeline().add("2", new StreamSession(c.createHandler()));
		session.getPipeline().close();
		assertClosed(session, -1, true);
		assertClosed(session, 0, true);
		assertClosed(session, 1, true);
		c.start();
		waitFor(100);
		assertEquals("SCR|SEN|SCR|SEN|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
	}
	
	@Test
	public void testPipelineCloseTypes() throws Exception {
		s = new Server(PORT);
		s.start();
		c = new Client(PORT);
		c.useTestSession = true;
		TestStreamSession session = (TestStreamSession)c.createSession();
		TestStreamSession session1 = new TestStreamSession(c.createHandler());
		TestStreamSession session2 = new TestStreamSession(c.createHandler());
		session.getPipeline().add("1", session1);
		session.getPipeline().add("2", session2);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		session.getPipeline().close();
		waitFor(100);
		assertEquals("SCL|SEN|SCR|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertEquals(StoppingType.GENTLE, session1.closeType);
		assertEquals(StoppingType.GENTLE, session2.closeType);
		assertNull(session.closeType);
		c.stop(TIMEOUT);
		
		c = new Client(PORT);
		c.useTestSession = true;
		session = (TestStreamSession)c.createSession();
		session1 = new TestStreamSession(c.createHandler());
		session2 = new TestStreamSession(c.createHandler());
		session.getPipeline().add("1", session1);
		session.getPipeline().add("2", session2);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		session.getPipeline().quickClose();
		waitFor(100);
		assertEquals("SCL|SEN|SCR|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertEquals(StoppingType.QUICK, session1.closeType);
		assertEquals(StoppingType.GENTLE, session2.closeType);
		assertNull(session.closeType);
		c.stop(TIMEOUT);
		
		c = new Client(PORT);
		c.useTestSession = true;
		session = (TestStreamSession)c.createSession();
		session1 = new TestStreamSession(c.createHandler());
		session2 = new TestStreamSession(c.createHandler());
		session.getPipeline().add("1", session1);
		session.getPipeline().add("2", session2);
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
		session.getPipeline().dirtyClose();
		waitFor(100);
		assertEquals("SCL|SEN|SCR|SEN|", c.getRecordedData(true));
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertEquals(StoppingType.DIRTY, session1.closeType);
		assertEquals(StoppingType.GENTLE, session2.closeType);
		assertNull(session.closeType);
		c.stop(TIMEOUT);

		session = new TestStreamSession(c.createHandler());
		session1 = new TestStreamSession(c.createHandler());
		session2 = new TestStreamSession(c.createHandler());
		session.getPipeline().add("1", session1);
		session.getPipeline().add("2", session2);
		session.getPipeline().close();
		assertEquals(StoppingType.GENTLE, session1.closeType);
		assertEquals(StoppingType.GENTLE, session2.closeType);
		assertEquals(StoppingType.GENTLE, session.closeType);
		
		session = new TestStreamSession(c.createHandler());
		session1 = new TestStreamSession(c.createHandler());
		session2 = new TestStreamSession(c.createHandler());
		session.getPipeline().add("1", session1);
		session.getPipeline().add("2", session2);
		session.getPipeline().quickClose();
		assertEquals(StoppingType.QUICK, session1.closeType);
		assertEquals(StoppingType.QUICK, session2.closeType);
		assertEquals(StoppingType.QUICK, session.closeType);

		session = new TestStreamSession(c.createHandler());
		session1 = new TestStreamSession(c.createHandler());
		session2 = new TestStreamSession(c.createHandler());
		session.getPipeline().add("1", session1);
		session.getPipeline().add("2", session2);
		session.getPipeline().dirtyClose();
		assertEquals(StoppingType.DIRTY, session1.closeType);
		assertEquals(StoppingType.DIRTY, session2.closeType);
		assertEquals(StoppingType.DIRTY, session.closeType);
	}
}
