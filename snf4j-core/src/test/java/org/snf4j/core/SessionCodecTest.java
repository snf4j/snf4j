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

import java.nio.ByteBuffer;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.TestCodec.BBDEv;
import org.snf4j.core.allocator.TestAllocator;
import org.snf4j.core.codec.CompoundDecoder;
import org.snf4j.core.codec.CompoundEncoder;
import org.snf4j.core.codec.DefaultCodecExecutor;
import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.codec.IEncoder;
import org.snf4j.core.codec.bytes.ArrayToBufferDecoder;
import org.snf4j.core.codec.bytes.BufferToArrayDecoder;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.IllegalSessionStateException;

public class SessionCodecTest {

	long TIMEOUT = 2000;
	int PORT = 7777;

	Server s;
	Client c;
	
	boolean directAllocator;
	TestAllocator allocator;
	boolean optimizeDataCopying;
	
	TestCodec codec;

	StringBuilder trace = new StringBuilder();
	
	synchronized void trace(String s) {
		trace.append(s);
		trace.append('|');
	}
	
	synchronized String getTrace() {
		String s = trace.toString();
		trace.setLength(0);
		return s;
	}
	
	@Before
	public void before() {
		s = c = null;
		directAllocator = false;
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
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BasePD());
		p.getPipeline().add("2", codec.PBD());
		p.getPipeline().add("3", bytesEncoder ? codec.PBE() : codec.PBBE());
		p.getPipeline().add("4", codec.BPE());
		startWithCodec(p);
	}
	
	private void startWithCodec(DefaultCodecExecutor pipeline)  throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);
		
		c.directAllocator = directAllocator;
		c.allocator = allocator;
		c.optimizeDataCopying = optimizeDataCopying;
		c.codecPipeline = pipeline;
		
		s.start();
		c.start();

		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData(true);
	}
	
	private void stop() throws Exception {
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}
	@Test
	public void testGetCodecPipeline() throws Exception {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BPD());
		p.getPipeline().add("2", codec.PBD());
		p.getPipeline().add("3", codec.PBE());
		p.getPipeline().add("4", codec.BPE());
		startWithCodec(p);
		assertNotNull(c.getSession().getCodecPipeline());
		assertTrue(p.getPipeline() == c.getSession().getCodecPipeline());
		assertNull(s.getSession().getCodecPipeline());
		stop();
	}
	
	@Test
	public void testWrite() throws Exception {
		startWithCodec(true);

		Packet packet = new Packet(PacketType.ECHO, "ABC");
		StreamSession session = c.getSession();
		int packetLen = packet.toBytes().length;
		session.suspendWrite();
		IFuture<Void> future = session.write(packet.toBytes());
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
		
		session.suspendWrite();
		future = session.write(packet.toBytes(4, 6),4,packetLen);
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
		
		session.suspendWrite();
		future = session.write(ByteBuffer.wrap(packet.toBytes()));
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

		session.suspendWrite();
		future = session.write(ByteBuffer.wrap(packet.toBytes(0, 3)), packetLen);
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

		session.suspendWrite();
		future = session.write(packet);
		assertFalse(future.isSuccessful());
		session.resumeWrite();
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		assertTrue(future.isSuccessful());
		session.writenf(packet);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCeed)|", c.getRecordedData(true));
		
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
		
		packet = new Packet(PacketType.ECHO, "ABC");
		startWithCodec(false);
		session = c.getSession();
		future = session.write(packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCe2d)|", c.getRecordedData(true));
		assertTrue(future.isSuccessful());
		session.writenf(ByteBuffer.wrap(packet.toBytes()));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCe2d)|", c.getRecordedData(true));

		stop();
	}
	
	@Test
	public void testEncodeInRead() throws Exception {
		startWithCodec(true);
		Packet packet = new Packet(PacketType.ECHO_NF, "ABC");
		StreamSession session = s.getSession();

		session.write(packet.toBytes());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DR|ECHO_NF(ABCd)|DS|", c.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE(ABCde)|", s.getRecordedData(true));
		
		packet = new Packet(PacketType.ECHO, "ABC");
		session.write(packet.toBytes());
		s.waitForDataRead(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		assertEquals("DR|ECHO(ABCd)|DS|", c.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE(ABCde)|", s.getRecordedData(true));
		
		codec.encodeException = new Exception("E1");
		session.write(packet.toBytes());
		waitFor(100);
		assertEquals("DR|ECHO(ABCd)|ENCODING_PIPELINE_FAILURE|", c.getRecordedData(true));
		assertEquals("DS|", s.getRecordedData(true));
		
		packet = new Packet(PacketType.ECHO_NF, "ABC");
		session.write(packet.toBytes());
		waitFor(100);
		assertEquals("DR|ECHO_NF(ABCd)|ENCODING_PIPELINE_FAILURE|", c.getRecordedData(true));
		assertEquals("DS|", s.getRecordedData(true));
		
		c.incidentClose = true;
		session.write(packet.toBytes());
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|ECHO_NF(ABCd)|ENCODING_PIPELINE_FAILURE|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DS|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		startWithCodec(true);
		codec.encodeException = new Exception("E1");
		c.throwInIncident = true;
		session = s.getSession();		
		session.write(packet.toBytes());
		waitFor(100);
		assertEquals("DR|ECHO_NF(ABCd)|ENCODING_PIPELINE_FAILURE|", c.getRecordedData(true));
		assertEquals("DS|", s.getRecordedData(true));
	}
	
	@Test
	public void testEncodeException() throws Exception {
		startWithCodec(true);
		c.incidentRecordException = true;
		Packet packet = new Packet(PacketType.ECHO, "ABC");
		StreamSession session = c.getSession();
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
		s.waitForSessionEnding(TIMEOUT);
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
		s.waitForSessionEnding(TIMEOUT);
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
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("ENCODING_PIPELINE_FAILURE(E4)|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}
	
	@Test
	public void testDecodeException() throws Exception {
		startWithCodec(true);
		c.exceptionRecordException = true;
		codec.decodeException = new Exception("E1");
		Packet packet = new Packet(PacketType.ECHO, "ABC");
		StreamSession session = c.getSession();
		session.write(packet.toBytes());
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		assertEquals("DS|DR|EXC|(E1)|SCL|SEN|", c.getRecordedData(true));
	}
	
	@Test
	public void testDecode() throws Exception {
		
		//pipeline without decoder
		startWithCodec(new DefaultCodecExecutor());
		Packet packet = new Packet(PacketType.ECHO, "ABC");
		StreamSession session = c.getSession();
		session.write(packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));
		assertEquals(1, c.availableCounter);
		stop();
		
		//pipeline without base decoder
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BPD());
		p.getPipeline().add("2", codec.PBD());
		p.getPipeline().add("3", codec.PBE());
		p.getPipeline().add("4", codec.BPE());
		startWithCodec(p);
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		session.write(packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		assertEquals(0, c.availableCounter);
		stop();
		
		
		//pipeline with discarding decoder
		codec.discardingDecode = true;
		startWithCodec(true);
		session = c.getSession();
		session.write(packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataReceived(TIMEOUT);
		waitFor(100);
		assertEquals("DS|DR|", c.getRecordedData(true));
		stop();
		codec.discardingDecode = false;
		
		//pipeline with duplicating decoder
		codec.duplicatingDecode = true;
		startWithCodec(true);
		session = c.getSession();
		session.write(packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		waitFor(100);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|ECHO_RESPONSE(ABCed)|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		stop();
		codec.duplicatingDecode = false;
		
		//direct buffer
		directAllocator = true;
		startWithCodec(true);
		session = c.getSession();
		session.write(packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		stop();

		//direct buffer without decoder
		startWithCodec(new DefaultCodecExecutor());
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		session.write(packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));
		assertEquals(1, c.availableCounter);
		stop();
		
		//direct buffer without base decoder
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BPD());
		p.getPipeline().add("2", codec.PBD());
		p.getPipeline().add("3", codec.PBE());
		p.getPipeline().add("4", codec.BPE());
		startWithCodec(p);
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		session.write(packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.getRecordedData(true));
		assertEquals(0, c.availableCounter);
		CodecExecutorAdapter adapter = new CodecExecutorAdapter(p, session); 
		ByteBuffer buffer = ByteBuffer.allocate(10);
		assertEquals(0, adapter.available(buffer, false));
		buffer.put((byte) 5);
		assertEquals(1, adapter.available(buffer, false));
		buffer.put((byte) 3).flip();
		assertEquals(2, adapter.available(buffer, true));
		assertTrue(p == adapter.getExecutor());
		stop();
	}

	@Test
	public void testWithCompoundCodec() throws Exception {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new CompoundBPD(codec.BPD(),codec.PBD(),codec.BPD()));
		p.getPipeline().add("2", new CompoundPBE(codec.PBE(),codec.BPE(),codec.PBE()));
		startWithCodec(p);
		Packet packet = new Packet(PacketType.ECHO, "ABC");
		StreamSession session = c.getSession();
		session.write(packet);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCeed])|", c.getRecordedData(true));
		stop();

		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new CompoundBPD(new BVD(),codec.BPD(),new PVD(),codec.PBD(),new BVD(),codec.BPD(),new PVD()));
		p.getPipeline().add("2", new CompoundPBE(new PVE(),codec.PBE(),new BVE(),codec.BPE(),new PVE(),codec.PBE(),new BVE()));
		startWithCodec(p);
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		session.write(packet);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCeed])|", c.getRecordedData(true));
		assertEquals("PVE|BVE|PVE|BVE|BVD|PVD|BVD|PVD|", getTrace());
		stop();
	
	}
	
	@Test
	public void testWriteReadMessage() throws Exception {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BasePD());
		p.getPipeline().add("2", codec.PBE());
		startWithCodec(p);
		Packet packet = new Packet(PacketType.ECHO, "ABC");
		StreamSession session = c.getSession();
		session.write(packet).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCe])|", c.getRecordedData(true));
		stop();

		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BasePD());
		p.getPipeline().add("2", codec.PBE());
		p.getPipeline().add("3", codec.PPD());
		startWithCodec(p);
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		session.write(packet).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		waitFor(100);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCe])|M(ECHO_RESPONSE[ABCe])|", c.getRecordedData(true));
		stop();

		startWithCodec(new DefaultCodecExecutor());
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		session.write(packet.toBytes()).sync(TIMEOUT);
		assertTrue(session.write(new Integer(0)).await(TIMEOUT).isCancelled());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));
		assertEquals(1, c.availableCounter);
		stop();

	}
	
	@Test
	public void testWriteMessage() throws Exception {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BasePD());
		p.getPipeline().add("2", codec.PBE());
		startWithCodec(p);
		Packet packet = new Packet(PacketType.ECHO, "ABC");
		StreamSession session = c.getSession();

		session.write(packet).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCe])|", c.getRecordedData(true));
		session.writenf(packet);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCee])|", c.getRecordedData(true));
	
		session.write((Object)packet.toBytes()).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCee])|", c.getRecordedData(true));
		session.writenf((Object)packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCee])|", c.getRecordedData(true));
		
		session.write((Object)ByteBuffer.wrap(packet.toBytes())).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCee])|", c.getRecordedData(true));
		session.writenf((Object)ByteBuffer.wrap(packet.toBytes()));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCee])|", c.getRecordedData(true));
		
		stop();

		//write without pipeline
		startWithCodec(null);
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		
		try {
			session.write(packet); fail();
		}
		catch (IllegalArgumentException e) {}
		try {
			session.writenf(packet);
		}
		catch (IllegalArgumentException e) {}
		
		session.write((Object)packet.toBytes()).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));
		session.writenf((Object)packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));

		session.write((Object)ByteBuffer.wrap(packet.toBytes())).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));
		session.writenf((Object)ByteBuffer.wrap(packet.toBytes()));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));
		
		stop();
	
		//write with empty pipeline
		startWithCodec(new DefaultCodecExecutor());
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		
		IFuture<Void> future = session.write(packet);
		assertTrue(future.await().isCancelled());
		session.writenf(packet);
		
		session.write((Object)packet.toBytes()).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));
		session.writenf((Object)packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));

		session.write((Object)ByteBuffer.wrap(packet.toBytes())).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));
		session.writenf((Object)ByteBuffer.wrap(packet.toBytes()));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.getRecordedData(true));
		
	}
	
	@Test
	public void testCloseInsideDecoder() throws Exception {
		codec.decodeClose = true;
		startWithCodec(true);
		Packet packet = new Packet(PacketType.ECHO, "ABC");
		StreamSession session = c.getSession();
		session.write(packet);
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|ECHO(ABCe)|DS|SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		codec.decodeClose = false;

		codec.decodeQuickClose = true;
		startWithCodec(true);
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		session.write(packet);
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|ECHO(ABCe)|DS|SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		codec.decodeQuickClose = false;

		codec.decodeDirtyClose = true;
		startWithCodec(true);
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		session.write(packet);
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|ECHO(ABCe)|DS|SCL|SEN|", s.getRecordedData(true));
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
		StreamSession session = c.getSession();
		assertTrue(session.write(packet).await(TIMEOUT).isCancelled());
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
		assertEquals("SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		codec.encodeClose = false;
		
		codec.encodeQuickClose = true;
		startWithCodec(true);
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		IFuture<Void> future = session.write(packet);
		assertTrue(future.await(TIMEOUT).isFailed());
		assertTrue(future.cause().getClass() == IllegalSessionStateException.class);
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
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
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("SCL|SEN|", s.getRecordedData(true));
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
		startWithCodec(p);
		
		long id = c.getSession().getId();
		assertEquals("A("+id+")|CREATED("+id+")|OPENED("+id+")|READY("+id+")|", ((BBDEv)d).getTrace());
		p.getPipeline().remove("3");
		p.getPipeline().add("3", d2);
		c.getRecordedData(true);
		s.getRecordedData(true);
		c.getSession().write(new Packet(PacketType.NOP));
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(e)|", s.getRecordedData(true));
		c.getSession().close();
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("R("+id+")|", ((BBDEv)d).getTrace());	
		assertEquals("A("+id+")|CLOSED("+id+")|ENDING("+id+")|", ((BBDEv)d2).getTrace());	
	}
	
	@Test
	public void testOptimizedDataCopyingWrite() throws Exception {
		codec.nopToNop2 = true;
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BBBBE());
		optimizeDataCopying = true;
		allocator = new TestAllocator(false,true);
		startWithCodec(p);
		
		StreamSession session = c.getSession();
		ByteBuffer b = session.allocate(100);
		
		b.put(new Packet(PacketType.NOP).toBytes());
		b.flip();
		session.write(b);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP2()|", s.getRecordedData(true));
		assertEquals(1, session.getOutBuffers().length);
		assertTrue(b == session.getOutBuffers()[0]);
		assertEquals(1, allocator.getReleasedCount());
		assertEquals(1024, allocator.getReleased().get(0).capacity());
		b.compact();
		
		session.getCodecPipeline().remove("1");
		session.write(new Packet(PacketType.NOP,"1").toBytes());
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(1)|", s.getRecordedData(true));
		assertEquals(1, session.getOutBuffers().length);
		assertTrue(b == session.getOutBuffers()[0]);
		assertEquals(1, allocator.getReleasedCount());
		b.compact();
		
		ByteBuffer b0 = session.allocate(100);
		b0.put(new Packet(PacketType.NOP,"2").toBytes());
		b0.put((byte)0);
		b0.flip();
		session.write(b0,b0.remaining()-1);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(2)|", s.getRecordedData(true));
		assertEquals(1, session.getOutBuffers().length);
		assertTrue(b == session.getOutBuffers()[0]);
		assertEquals(1, allocator.getReleasedCount());
		b.compact();
		
		//split packet
		codec.nopToNop2 = false;
		session.getCodecPipeline().add("1", codec.BBBBE());
		byte[] bytes = new Packet(PacketType.NOP , "1234567890").toBytes();
		session.write(bytes, 0, 5).sync(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		waitFor(50);
		assertEquals("DR|", s.getRecordedData(true));
		session.write(bytes, 5, bytes.length-5).sync(TIMEOUT);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(1234567890)|", s.getRecordedData(true));
		
	}
	
	@Test
	public void testOptimizedDataCopyingRead() throws Exception {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new BufferToArrayDecoder());
		p.getPipeline().add("2", new ArrayToBufferDecoder());
		optimizeDataCopying = true;
		allocator = new TestAllocator(false,true);
		directAllocator = true;
		startWithCodec(p);
		
		s.getSession().write(new Packet(PacketType.NOP).toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|BUF|NOP()|", c.getRecordedData(true));
		
		c.getSession().getCodecPipeline().add("3", new BufferToArrayDecoder());
		s.getSession().write(new Packet(PacketType.NOP,"1").toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(1)|", c.getRecordedData(true));
		
		c.getSession().getCodecPipeline().add("4", new ArrayToBufferDecoder());
		s.getSession().write(new Packet(PacketType.NOP,"2").toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|BUF|NOP(2)|", c.getRecordedData(true));
		
		c.getSession().getCodecPipeline().add("5", new DupD());
		s.getSession().write(new Packet(PacketType.NOP,"3").toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		waitFor(50);
		assertEquals("DR|BUF|NOP(3)|BUF|NOP(3)|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", new ArrayToBufferDecoder());
		optimizeDataCopying = true;
		allocator = new TestAllocator(false,true);
		startWithCodec(p);
		
		ByteBuffer b = c.getSession().getInBuffer();
		s.getSession().write(new Packet(PacketType.NOP).toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|BUF|NOP()|", c.getRecordedData(true));
		assertEquals(1, allocator.getReleasedCount());
		assertTrue(b == allocator.getReleased().get(0));
		assertFalse(b == c.getSession().getInBuffer());
		assertEquals(3, allocator.getAllocatedCount());
		
		c.getSession().getCodecPipeline().remove("1");
		s.getSession().write(new Packet(PacketType.NOP,"1").toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|BUF|NOP(1)|", c.getRecordedData(true));
		assertEquals(1, allocator.getReleasedCount());
		assertEquals(4, allocator.getAllocatedCount());
		
		c.getSession().getCodecPipeline().add("1", new BVD());
		s.getSession().write(new Packet(PacketType.NOP,"2").toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|BUF|NOP(2)|", c.getRecordedData(true));
		assertEquals("BVD|", getTrace());
		assertEquals(1, allocator.getReleasedCount());
		assertEquals(5, allocator.getAllocatedCount());

		c.getSession().getCodecPipeline().add("2", new BBVD());
		s.getSession().write(new Packet(PacketType.NOP,"3").toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|BUF|NOP(3)|", c.getRecordedData(true));
		assertEquals("BVD|BBVD|", getTrace());
		assertEquals(1, allocator.getReleasedCount());
		assertEquals(6, allocator.getAllocatedCount());
		
		c.exceptionRecordException = true;
		c.getSession().getCodecPipeline().add("3", new ExeD());
		s.getSession().write(new Packet(PacketType.NOP,"3").toBytes());
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|EXC|(E)|SCL|SEN|", c.getRecordedData(true));
		assertEquals("BVD|BBVD|", getTrace());
		assertEquals(3, allocator.getReleasedCount());
		assertEquals(6, allocator.getAllocatedCount());
		
	}

	class DupD implements IDecoder<ByteBuffer,ByteBuffer> {
		@Override public Class<ByteBuffer> getInboundType() {return ByteBuffer.class;}
		@Override public Class<ByteBuffer> getOutboundType() {return ByteBuffer.class;}
		@Override
		public void decode(ISession session, ByteBuffer data, List<ByteBuffer> out) throws Exception {
			out.add(data);
			out.add(data.duplicate());
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
	
	class BVD implements IDecoder<byte[],Void> {
		@Override public Class<byte[]> getInboundType() {return byte[].class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void decode(ISession session, byte[] data, List<Void> out)	throws Exception {trace("BVD");}
	}
	
	class BBVD implements IDecoder<ByteBuffer,Void> {
		@Override public Class<ByteBuffer> getInboundType() {return ByteBuffer.class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void decode(ISession session, ByteBuffer data, List<Void> out)	throws Exception {trace("BBVD");}
	}

	class PVD implements IDecoder<Packet,Void> {
		@Override public Class<Packet> getInboundType() {return Packet.class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void decode(ISession session, Packet data, List<Void> out)	throws Exception {trace("PVD");}
	}

	class BVE implements IEncoder<byte[],Void> {
		@Override public Class<byte[]> getInboundType() {return byte[].class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void encode(ISession session, byte[] data, List<Void> out)	throws Exception {trace("BVE");}
	}

	class PVE implements IEncoder<Packet,Void> {
		@Override public Class<Packet> getInboundType() {return Packet.class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void encode(ISession session, Packet data, List<Void> out)	throws Exception {trace("PVE");}
	}
	
	class CompoundBPD extends CompoundDecoder<byte[],Packet> {

		CompoundBPD(IDecoder<?,?>... decoders) {
			super(decoders);
		}
		
		@Override
		public Class<byte[]> getInboundType() {
			return byte[].class;
		}

		@Override
		public Class<Packet> getOutboundType() {
			return Packet.class;
		}
		
	}
	
	class CompoundPBE extends CompoundEncoder<Packet,byte[]> {

		CompoundPBE(IEncoder<?,?>... encoders) {
			super(encoders);
		}
		
		@Override
		public Class<Packet> getInboundType() {
			return Packet.class;
		}

		@Override
		public Class<byte[]> getOutboundType() {
			return byte[].class;
		}
		
	}
	
}
