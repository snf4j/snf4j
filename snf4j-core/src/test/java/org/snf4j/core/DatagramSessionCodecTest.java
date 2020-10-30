/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019-2020 SNF4J contributors
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

import java.lang.reflect.Field;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.TestCodec.BBDEv;
import org.snf4j.core.allocator.TestAllocator;
import org.snf4j.core.codec.DefaultCodecExecutor;
import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.codec.IEncoder;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.IllegalSessionStateException;

public class DatagramSessionCodecTest {

	long TIMEOUT = 2000;
	int PORT = 7779;
	
	DatagramHandler c;
	DatagramHandler s;

	boolean directAllocator;
	TestAllocator allocator;
	boolean optimizeDataCopying;
	TestCodec codec;

	static ByteBuffer getInBuffer(DatagramSession session) throws Exception {
		Field f = DatagramSession.class.getDeclaredField("inBuffer");
		
		f.setAccessible(true);
		return (ByteBuffer) f.get(session);
	}
	
	@Before
	public void before() {
		s = c = null;
		codec = new TestCodec();
	}
	
	@After
	public void after() throws InterruptedException {
		if (c != null) c.stop(TIMEOUT);
		if (s != null) s.stop(TIMEOUT);
	}
	
	private void waitFor(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}
	
	private void startWithCodec(boolean bytesEncoder) throws Exception {
		startWithCodec(true, bytesEncoder);
	}

	private void startWithCodec(boolean client, boolean bytesEncoder) throws Exception {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BasePD());
		p.getPipeline().add("2", codec.PBD());
		p.getPipeline().add("3", bytesEncoder ? codec.PBE() : codec.PBBE());
		p.getPipeline().add("4", codec.BPE());
		startWithCodec(client, p);
	}

	private void startWithCodec(boolean client, DefaultCodecExecutor pipeline) throws Exception {
		s = new DatagramHandler(PORT);
		c = new DatagramHandler(PORT);
		if (client) {
			c.directAllocator = directAllocator;
			c.allocator = allocator;
			c.codecPipeline = pipeline;
			c.optimizeDataCopying = optimizeDataCopying;
		}
		else {
			s.directAllocator = directAllocator;
			s.allocator = allocator;
			s.codecPipeline = pipeline;
			s.optimizeDataCopying = optimizeDataCopying;
		}
		s.startServer();
		c.startClient();
		s.waitForSessionReady(TIMEOUT);
		c.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getRecordedData(true));
		assertEquals("SCR|SOP|RDY|", s.getRecordedData(true));
	}

	private void stop() throws Exception {
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
	}

	@Test
	public void testGetCodecPipeline() throws Exception {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BasePD());
		p.getPipeline().add("2", codec.PBE());
		p.getPipeline().add("3", codec.PPD());
		startWithCodec(true, p);
		assertNotNull(c.getSession().getCodecPipeline());
		assertTrue(p.getPipeline() == c.getSession().getCodecPipeline());
		assertNull(s.getSession().getCodecPipeline());
		stop();
	}
	
	@Test
	public void testWrite() throws Exception {
		startWithCodec(true);
		SocketAddress address = s.getSession().getLocalAddress();
		
		Packet packet = new Packet(PacketType.ECHO, "ABC");
		DatagramSession session = c.getSession();
		int packetLen = packet.toBytes().length;
		session.suspendWrite();
		IFuture<Void> future = session.write(packet.toBytes());
		assertFalse(future.isSuccessful());
		session.resumeWrite();
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		assertTrue(future.isSuccessful());
		session.suspendWrite();
		future = session.send(address, packet.toBytes());
		assertFalse(future.isSuccessful());
		session.resumeWrite();
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		assertTrue(future.isSuccessful());
		session.writenf(packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		session.sendnf(address, packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		
		session.suspendWrite();
		future = session.write(packet.toBytes(4, 6),4,packetLen);
		assertFalse(future.isSuccessful());
		session.resumeWrite();
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		assertTrue(future.isSuccessful());
		session.suspendWrite();
		future = session.send(address, packet.toBytes(4, 6),4,packetLen);
		assertFalse(future.isSuccessful());
		session.resumeWrite();
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		assertTrue(future.isSuccessful());
		session.writenf(packet.toBytes(4, 6),4,packetLen);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		session.sendnf(address, packet.toBytes(4, 6),4,packetLen);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));

		session.suspendWrite();
		future = session.write(ByteBuffer.wrap(packet.toBytes()));
		assertFalse(future.isSuccessful());
		session.resumeWrite();
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		assertTrue(future.isSuccessful());
		session.suspendWrite();
		future = session.send(address, ByteBuffer.wrap(packet.toBytes()));
		assertFalse(future.isSuccessful());
		session.resumeWrite();
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		assertTrue(future.isSuccessful());
		session.writenf(ByteBuffer.wrap(packet.toBytes()));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		session.sendnf(address, ByteBuffer.wrap(packet.toBytes()));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));

		session.suspendWrite();
		future = session.write(ByteBuffer.wrap(packet.toBytes(0, 3)), packetLen);
		assertFalse(future.isSuccessful());
		session.resumeWrite();
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		assertTrue(future.isSuccessful());
		session.suspendWrite();
		future = session.send(address, ByteBuffer.wrap(packet.toBytes(0, 3)), packetLen);
		assertFalse(future.isSuccessful());
		session.resumeWrite();
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		assertTrue(future.isSuccessful());
		session.writenf(ByteBuffer.wrap(packet.toBytes(0, 3)), packetLen);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		session.sendnf(address, ByteBuffer.wrap(packet.toBytes(0, 3)), packetLen);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));

		session.suspendWrite();
		future = session.write(packet);
		assertFalse(future.isSuccessful());
		session.resumeWrite();
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		assertTrue(future.isSuccessful());
		session.suspendWrite();
		future = session.send(address, packet);
		assertFalse(future.isSuccessful());
		session.resumeWrite();
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCeed)|", c.getRecordedData(true));
		assertTrue(future.isSuccessful());
		session.writenf(packet);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCeeed)|", c.getRecordedData(true));
		session.sendnf(address, packet);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCeeeed)|", c.getRecordedData(true));
		
		stop();
		
		try {
			session.write(packet.toBytes()); fail(); 
		}
		catch (IllegalStateException e) {}
		try {
			session.writenf(packet.toBytes()); fail(); 
		}
		catch (IllegalStateException e) {}
		try {
			session.write(packet.toBytes(), 0, packetLen); fail(); 
		}
		catch (IllegalStateException e) {}
		try {
			session.writenf(packet.toBytes(), 0, packetLen); fail(); 
		}
		catch (IllegalStateException e) {}
		try {
			session.write(ByteBuffer.wrap(packet.toBytes())); fail(); 
		}
		catch (IllegalStateException e) {}
		try {
			session.writenf(ByteBuffer.wrap(packet.toBytes())); fail(); 
		}
		catch (IllegalStateException e) {}
		try {
			session.write(ByteBuffer.wrap(packet.toBytes()), 3); fail(); 
		}
		catch (IllegalStateException e) {}
		try {
			session.writenf(ByteBuffer.wrap(packet.toBytes()), 3); fail(); 
		}
		catch (IllegalStateException e) {}
		try {
			session.write(packet); fail(); 
		}
		catch (IllegalStateException e) {}
		try {
			session.writenf(packet); fail(); 
		}
		catch (IllegalStateException e) {}

		try {
			session.send(address,packet.toBytes()); fail(); 
		}
		catch (IllegalStateException e) {}
		try {
			session.sendnf(address,packet.toBytes()); fail(); 
		}
		catch (IllegalStateException e) {}
		try {
			session.send(address,packet.toBytes(), 0, packetLen); fail(); 
		}
		catch (IllegalStateException e) {}
		try {
			session.sendnf(address,packet.toBytes(), 0, packetLen); fail(); 
		}
		catch (IllegalStateException e) {}
		try {
			session.send(address,ByteBuffer.wrap(packet.toBytes())); fail(); 
		}
		catch (IllegalStateException e) {}
		try {
			session.sendnf(address,ByteBuffer.wrap(packet.toBytes())); fail(); 
		}
		catch (IllegalStateException e) {}
		try {
			session.send(address,ByteBuffer.wrap(packet.toBytes()), 3); fail(); 
		}
		catch (IllegalStateException e) {}
		try {
			session.sendnf(address,ByteBuffer.wrap(packet.toBytes()), 3); fail(); 
		}
		catch (IllegalStateException e) {}
		try {
			session.send(address,packet); fail(); 
		}
		catch (IllegalStateException e) {}
		try {
			session.sendnf(address,packet); fail(); 
		}
		catch (IllegalStateException e) {}
		
		packet = new Packet(PacketType.ECHO, "ABC");
		startWithCodec(false);
		session = c.getSession();
		future = session.write(packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCe2d)|", c.getRecordedData(true));
		assertTrue(future.isSuccessful());
		future = session.send(address, packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCe2d)|", c.getRecordedData(true));
		assertTrue(future.isSuccessful());
		session.writenf(ByteBuffer.wrap(packet.toBytes()));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCe2d)|", c.getRecordedData(true));
		session.sendnf(address,ByteBuffer.wrap(packet.toBytes()));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCe2d)|", c.getRecordedData(true));

		stop();
	}

	@Test
	public void testEncodeException() throws Exception {
		startWithCodec(true);
		c.incidentRecordException = true;
		Packet packet = new Packet(PacketType.ECHO, "ABC");
		DatagramSession session = c.getSession();
		codec.encodeException = new Exception("E1");
		IFuture<Void> future = session.write(packet.toBytes());
		future.await(TIMEOUT);
		waitFor(100);
		assertEquals("ENCODING_PIPELINE_FAILURE(E1)|", c.getRecordedData(true));
		assertTrue(future.isFailed());
		assertTrue(future.cause() == codec.encodeException);
		codec.encodeException = null;
		future = session.write(packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		
		//close on incident
		c.incidentClose = true;
		codec.encodeException = new Exception("E2");
		future = session.write(packet.toBytes());
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("ENCODING_PIPELINE_FAILURE(E2)|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		//quick close on incident
		startWithCodec(true);
		c.incidentRecordException = true;
		c.incidentQuickClose = true;
		codec.encodeException = new Exception("E3");
		session = c.getSession();
		future = session.write(packet.toBytes());
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("ENCODING_PIPELINE_FAILURE(E3)|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		//dirty close on incident
		startWithCodec(true);
		c.incidentRecordException = true;
		c.incidentDirtyClose = true;
		codec.encodeException = new Exception("E4");
		session = c.getSession();
		future = session.write(packet.toBytes());
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("ENCODING_PIPELINE_FAILURE(E4)|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}
	
	@Test
	public void testDecodeException() throws Exception {
		startWithCodec(true);
		c.incidentRecordException = true;
		codec.decodeException = new Exception("E1");
		Packet packet = new Packet(PacketType.ECHO, "ABC");
		DatagramSession session = c.getSession();
		session.write(packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		waitFor(100);
		assertEquals("DS|DR|DECODING_PIPELINE_FAILURE(E1)|", c.getRecordedData(true));
		codec.decodeException = null;
		session.write(packet);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		
		//close on incident
		c.incidentClose = true;
		codec.decodeException = new Exception("E2");
		session.write(packet.toBytes());
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|DR|DECODING_PIPELINE_FAILURE(E2)|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		//quick close on incident
		startWithCodec(true);
		c.incidentRecordException = true;
		c.incidentQuickClose = true;
		codec.decodeException = new Exception("E3");
		session = c.getSession();
		session.write(packet.toBytes());
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|DR|DECODING_PIPELINE_FAILURE(E3)|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		//dirty close on incident
		startWithCodec(true);
		c.incidentRecordException = true;
		c.incidentDirtyClose = true;
		codec.decodeException = new Exception("E3");
		session = c.getSession();
		session.write(packet.toBytes());
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|DR|DECODING_PIPELINE_FAILURE(E3)|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		startWithCodec(false, true);
		s.incidentRecordException = true;
		codec.decodeException = new Exception("E1");
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		session.write(packet.toBytes());
		s.waitForDataReceived(TIMEOUT);
		waitFor(100);
		assertEquals("DR|DECODING_PIPELINE_FAILURE(E1)|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		codec.decodeException = null;
		session.write(packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|$ECHO(ABCd)|DS|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE(ABCde)|", c.getRecordedData(true));
		
	}

	@Test
	public void testDecode() throws Exception {
		
		//pipeline without decoder
		startWithCodec(false, new DefaultCodecExecutor());
		SocketAddress address = s.getSession().getLocalAddress();
		Packet packet = new Packet(PacketType.ECHO, "ABC");
		DatagramSession session = c.getSession();
		session.send(address, packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|$ECHO(ABC)|DS|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));
		stop();

		//pipeline without base decoder
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BPD());
		p.getPipeline().add("2", codec.PBD());
		p.getPipeline().add("3", codec.PBE());
		p.getPipeline().add("4", codec.BPE());
		startWithCodec(false, p);
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		session.send(address, packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|$ECHO(ABCd)|DS|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE(ABCde)|", c.getRecordedData(true));
		stop();
		
		//pipeline with discarding decoder
		codec.discardingDecode = true;
		startWithCodec(false, true);
		session = c.getSession();
		session.write(packet.toBytes());
		s.waitForDataReceived(TIMEOUT);
		waitFor(100);
		assertEquals("DR|", s.getRecordedData(true));
		assertEquals("DS|", c.getRecordedData(true));
		stop();
		codec.discardingDecode = false;
		
		//pipeline with duplicating decoder
		codec.duplicatingDecode = true;
		startWithCodec(false, true);
		session = c.getSession();
		session.write(packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		waitFor(100);
		assertEquals("DR|$ECHO(ABCd)|$ECHO(ABCd)|$ECHO(ABCd)|DS|DS|DS|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE(ABCde)|DR|ECHO_RESPONSE(ABCde)|DR|ECHO_RESPONSE(ABCde)|", c.getRecordedData(true));
		stop();
		codec.duplicatingDecode = false;
		
		//direct buffer
		directAllocator = true;
		startWithCodec(false, true);
		session = c.getSession();
		session.write(packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|$ECHO(ABCd)|DS|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE(ABCde)|", c.getRecordedData(true));
		stop();
		
		//direct buffer without decoder
		startWithCodec(false, new DefaultCodecExecutor());
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		session.write(packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|$ECHO(ABC)|DS|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));
		stop();
		
		//direct buffer without base decoder
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BPD());
		p.getPipeline().add("2", codec.PBD());
		p.getPipeline().add("3", codec.PBE());
		p.getPipeline().add("4", codec.BPE());
		startWithCodec(false, p);
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		session.write(packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|$ECHO(ABCd)|DS|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE(ABCde)|", c.getRecordedData(true));
		stop();
	}
	
	@Test
	public void testSendMessage() throws Exception {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BasePD());
		p.getPipeline().add("2", codec.PBE());
		startWithCodec(true, p);
		SocketAddress address = s.getSession().getLocalAddress();
		Packet packet = new Packet(PacketType.ECHO, "ABC");
		DatagramSession session = c.getSession();

		session.send(null, packet).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCe])|", c.getRecordedData(true));
		session.send(address, packet).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCee])|", c.getRecordedData(true));
		
		session.send(null, (Object)packet.toBytes()).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCee])|", c.getRecordedData(true));
		session.send(address, (Object)packet.toBytes()).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCee])|", c.getRecordedData(true));
		
		session.send(null, (Object)ByteBuffer.wrap(packet.toBytes())).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCee])|", c.getRecordedData(true));
		session.send(address, (Object)ByteBuffer.wrap(packet.toBytes())).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCee])|", c.getRecordedData(true));
		
		packet = new Packet(PacketType.ECHO, "ABC");
		session.sendnf(null, packet);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCe])|", c.getRecordedData(true));
		session.sendnf(address, packet);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCee])|", c.getRecordedData(true));
		
		session.sendnf(null, (Object)packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCee])|", c.getRecordedData(true));
		session.sendnf(address, (Object)packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCee])|", c.getRecordedData(true));
		
		session.sendnf(null, (Object)ByteBuffer.wrap(packet.toBytes()));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCee])|", c.getRecordedData(true));
		session.sendnf(address, (Object)ByteBuffer.wrap(packet.toBytes()));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCee])|", c.getRecordedData(true));
		
		stop();
		
		//send without pipeline
		startWithCodec(true, null);
		address = s.getSession().getLocalAddress();
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		
		try {
			session.send(address, packet); fail();
		}
		catch (IllegalArgumentException e) {}
		try {
			session.sendnf(address, packet);
		}
		catch (IllegalArgumentException e) {}
		
		session.send(address, (Object)packet.toBytes()).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));
		session.sendnf(address, (Object)packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));

		session.send(address, (Object)ByteBuffer.wrap(packet.toBytes())).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));
		session.sendnf(address, (Object)ByteBuffer.wrap(packet.toBytes()));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));
		
		stop();
		
		//send with empty pipeline
		startWithCodec(true, new DefaultCodecExecutor());
		address = s.getSession().getLocalAddress();
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		
		IFuture<Void> future = session.send(address, packet);
		assertTrue(future.await().isCancelled());
		session.sendnf(address, packet);
		
		session.send(address, (Object)packet.toBytes()).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));
		session.sendnf(address, (Object)packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));

		session.send(address, (Object)ByteBuffer.wrap(packet.toBytes())).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));
		session.sendnf(address, (Object)ByteBuffer.wrap(packet.toBytes()));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));

	}
	
	@Test
	public void testWriteFailures() throws Exception {
		startWithCodec(true);
		Packet packet = new Packet(PacketType.ECHO, "ABC");
		DatagramSession session = c.getSession();

		codec.encodeFakeClosing = true;
		assertTrue(session.write(packet).await(TIMEOUT).isCancelled());
		codec.encodeFakeClosing = false;
		session.closing = ClosingState.NONE;
	
		stop();

		startWithCodec(true);
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();

		codec.encodeDirtyClose = true;
		IFuture<Void> future = session.write(packet);
		assertTrue(future.await(TIMEOUT).isFailed());
		assertTrue(future.cause().getClass() == IllegalSessionStateException.class);
		codec.encodeDirtyClose = false;
		session.closing = ClosingState.NONE;
		
		stop();
	}
	
	@Test
	public void testWriteReadMessage() throws Exception {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BasePD());
		p.getPipeline().add("2", codec.PBE());
		startWithCodec(false, p);
		Packet packet = new Packet(PacketType.ECHO, "ABC");
		DatagramSession session = c.getSession();
		session.write(packet.toBytes()).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|$M(ECHO[ABC])|DS|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE(ABCe)|", c.getRecordedData(true));
		stop();
		
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BasePD());
		p.getPipeline().add("2", codec.PBE());
		p.getPipeline().add("3", codec.PPD());
		startWithCodec(false, p);
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		session.write(packet.toBytes()).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		waitFor(100);
		assertEquals("DR|$M(ECHO[ABC])|$M(ECHO[ABC])|DS|DS|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE(ABCe)|DR|ECHO_RESPONSE(ABCe)|", c.getRecordedData(true));
		stop();
		
		startWithCodec(true, new DefaultCodecExecutor());
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		session.write(packet.toBytes()).sync(TIMEOUT);
		assertTrue(session.write(new Integer(0)).await(TIMEOUT).isCancelled());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));
		stop();
		
	}	
	
	@Test
	public void testCloseInsideDecoder() throws Exception {
		codec.decodeClose = true;
		startWithCodec(true);
		Packet packet = new Packet(PacketType.ECHO, "ABC");
		DatagramSession session = c.getSession();
		session.write(packet);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		codec.decodeClose = false;

		codec.decodeQuickClose = true;
		startWithCodec(true);
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		session.write(packet);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		codec.decodeQuickClose = false;

		codec.decodeDirtyClose = true;
		startWithCodec(true);
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		session.write(packet);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		codec.decodeDirtyClose = false;
	}
	
	@Test
	public void testCloseInsideEncoder() throws Exception {
		codec.encodeClose = true;
		startWithCodec(true);
		Packet packet = new Packet(PacketType.ECHO, "ABC");
		DatagramSession session = c.getSession();
		IFuture<Void> future = session.write(packet);
		assertTrue(future.await(TIMEOUT).isFailed());
		assertTrue(future.cause().getClass() == IllegalSessionStateException.class);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		codec.encodeClose = false;

		codec.encodeQuickClose = true;
		startWithCodec(true);
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		future = session.write(packet);
		assertTrue(future.await(TIMEOUT).isFailed());
		assertTrue(future.cause().getClass() == IllegalSessionStateException.class);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		codec.encodeQuickClose = false;

		codec.encodeDirtyClose = true;
		startWithCodec(true);
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		future = session.write(packet);
		assertTrue(future.await(TIMEOUT).isFailed());
		assertTrue(future.cause().getClass() == IllegalSessionStateException.class);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		codec.encodeDirtyClose = false;
		
	}	
	
	@Test
	public void testEventDrivenCodec() throws Exception {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		IDecoder<?, ?> d = codec.BBDEv();
		IDecoder<?, ?> d2 = codec.BBDEv();
		p.getPipeline().add("1", codec.BasePD());
		p.getPipeline().add("2", codec.PBD());
		p.getPipeline().add("3", d);
		p.getPipeline().add("4", codec.PBE());
		
		startWithCodec(true, p);
		long id = c.getSession().getId();
		assertEquals("A("+id+")|CREATED("+id+")|OPENED("+id+")|READY("+id+")|", ((BBDEv)d).getTrace());
		p.getPipeline().remove("3");
		p.getPipeline().add("3", d2);
		c.getRecordedData(true);
		s.getRecordedData(true);
		c.getSession().write(new Packet(PacketType.NOP));
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|$NOP(e)|", s.getRecordedData(true));
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("R("+id+")|", ((BBDEv)d).getTrace());	
		assertEquals("A("+id+")|CLOSED("+id+")|ENDING("+id+")|", ((BBDEv)d2).getTrace());	
	}
	
	@Test
	public void testOptimizedDataCopyingWrite() throws Exception {
		codec.nopToNop2 = true;
		
		//optimization with release
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BBBBE());
		allocator = new TestAllocator(false,true);
		optimizeDataCopying = true;
		startWithCodec(true, p);
		
		DatagramSession session = c.getSession();
		ByteBuffer b = session.allocate(100);
		
		b.put(new Packet(PacketType.NOP).toBytes());
		b.flip();
		session.write(b);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|$NOP2()|", s.getRecordedData(true));
		assertEquals(1, allocator.getReleasedCount());
		assertTrue(b == allocator.getReleased().get(0));
		
		byte[] array = new Packet(PacketType.NOP,"1").toBytes();
		session.write(array);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|$NOP2(1)|", s.getRecordedData(true));
		assertEquals(2, allocator.getReleasedCount());
		assertTrue(allocator.getReleased().get(1).hasArray());
		assertTrue(allocator.getReleased().get(1).array() == array);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		//optimization with no release
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BBBBE());
		allocator = new TestAllocator(false,false);
		optimizeDataCopying = true;
		startWithCodec(true, p);
		
		session = c.getSession();
		b = session.allocate(100);
		
		b.put(new Packet(PacketType.NOP).toBytes());
		b.flip();
		session.write(b);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|$NOP2()|", s.getRecordedData(true));
		assertEquals(0, allocator.getReleasedCount());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		//optimization with release (server side)
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BBBBE());
		allocator = new TestAllocator(false,true);
		optimizeDataCopying = true;
		startWithCodec(false, p);
		SocketAddress a = c.getSession().getLocalAddress();
		
		session = s.getSession();
		b = session.allocate(100);
		
		b.put(new Packet(PacketType.NOP).toBytes());
		b.flip();
		session.send(a, b);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP2()|", c.getRecordedData(true));
		assertEquals(1, allocator.getReleasedCount());
		assertTrue(b == allocator.getReleased().get(0));

		array = new Packet(PacketType.NOP,"1").toBytes();
		session.send(a, array);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP2(1)|", c.getRecordedData(true));
		assertEquals(2, allocator.getReleasedCount());
		assertTrue(allocator.getReleased().get(1).hasArray());
		assertTrue(allocator.getReleased().get(1).array() == array);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);	
	}

	@Test
	public void testOptimizedDataCopyingRead() throws Exception {
		codec.nopToNop2 = true;
		
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BBBBD());
		allocator = new TestAllocator(false,true);
		optimizeDataCopying = true;
		startWithCodec(true, p);
		SocketAddress a = c.getSession().getLocalAddress();
		
		DatagramSession session = s.getSession();
		ByteBuffer b = getInBuffer(c.getSession());
		
		assertEquals(1, allocator.getAllocatedCount());
		session.send(a, new Packet(PacketType.NOP).toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|BUF|NOP2()|", c.getRecordedData(true));
		assertEquals(2, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		assertTrue(b == c.bufferRead);
		assertFalse(b == getInBuffer(c.getSession()));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BBBBD());
		allocator = new TestAllocator(false,true);
		optimizeDataCopying = true;
		startWithCodec(false, p);
		
		session = c.getSession();
		b = getInBuffer(s.getSession());
		assertEquals(1, allocator.getAllocatedCount());
		session.write(new Packet(PacketType.NOP).toBytes());
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|BUF|$NOP2()|", s.getRecordedData(true));
		assertEquals(2, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		assertTrue(b == s.bufferRead);
		assertFalse(b == getInBuffer(s.getSession()));
		
		p.getPipeline().remove("1");
		b = getInBuffer(s.getSession());
		session.write(new Packet(PacketType.NOP).toBytes());
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|BUF|$NOP()|", s.getRecordedData(true));
		assertEquals(3, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		assertTrue(b == s.bufferRead);
		assertFalse(b == getInBuffer(s.getSession()));
		
		p.getPipeline().add("1", new DupD());
		b = getInBuffer(s.getSession());
		session.write(new Packet(PacketType.NOP).toBytes());
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		waitFor(50);
		assertEquals("DR|BUF|$NOP()|BUF|$NOP()|", s.getRecordedData(true));
		assertEquals(4, allocator.getAllocatedCount());
		assertEquals(0, allocator.getReleasedCount());
		assertTrue(b == s.bufferRead);
		assertFalse(b == getInBuffer(s.getSession()));
		
		s.incidentRecordException = true;
		p.getPipeline().add("2", new ExeD());
		b = getInBuffer(s.getSession());
		session.write(new Packet(PacketType.NOP).toBytes());
		waitFor(50);
		assertEquals("DR|DECODING_PIPELINE_FAILURE(E)|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BBBBD());
		allocator = new TestAllocator(false,false);
		optimizeDataCopying = true;
		startWithCodec(true, p);

		b = getInBuffer(s.getSession());
		c.write(new Packet(PacketType.NOP));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|$NOP()|", s.getRecordedData(true));
		assertTrue(b == getInBuffer(s.getSession()));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BBBBD());
		allocator = new TestAllocator(false,true);
		optimizeDataCopying = false;
		startWithCodec(true, p);

		b = getInBuffer(s.getSession());
		c.write(new Packet(PacketType.NOP));
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|$NOP()|", s.getRecordedData(true));
		assertTrue(b == getInBuffer(s.getSession()));
	}
	
	class DupD implements IDecoder<ByteBuffer,ByteBuffer> {
		@Override public Class<ByteBuffer> getInboundType() {return ByteBuffer.class;}
		@Override public Class<ByteBuffer> getOutboundType() {return ByteBuffer.class;}
		@Override
		public void decode(ISession session, ByteBuffer data, List<ByteBuffer> out) throws Exception {
			out.add(data.duplicate());
			out.add(data);
		}
	}

	class ExeD implements IDecoder<ByteBuffer,ByteBuffer> {
		@Override public Class<ByteBuffer> getInboundType() {return ByteBuffer.class;}
		@Override public Class<ByteBuffer> getOutboundType() {return ByteBuffer.class;}
		@Override
		public void decode(ISession session, ByteBuffer data, List<ByteBuffer> out) throws Exception {
			throw new Exception("E");
		}
	}
	
}
