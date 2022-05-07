/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019-2022 SNF4J contributors
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
import org.snf4j.core.codec.IBaseDecoder;
import org.snf4j.core.codec.ICodec;
import org.snf4j.core.codec.ICodecExecutor;
import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.codec.IEncoder;
import org.snf4j.core.codec.IEventDrivenCodec;
import org.snf4j.core.codec.bytes.ArrayToBufferDecoder;
import org.snf4j.core.codec.bytes.ArrayToBufferEncoder;
import org.snf4j.core.codec.bytes.BufferToArrayDecoder;
import org.snf4j.core.codec.bytes.BufferToArrayEncoder;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.SessionEvent;
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
	public void testCodecAdapterProtectedConstructor() throws Exception {
		DefaultCodecExecutor executor = new DefaultCodecExecutor();
		TestCodecAdapter adapter = new TestCodecAdapter(executor);
		
		assertTrue(executor == adapter.executor);
		assertNull(adapter.session);
		Field f = CodecExecutorAdapter.class.getDeclaredField("datagram");
		f.setAccessible(true);
		assertFalse(f.getBoolean(adapter));
	}
	
	static class TestCodecAdapter extends CodecExecutorAdapter { 
		protected TestCodecAdapter(ICodecExecutor executor) {
			super(executor);
		}
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
		c.incident = true;
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
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|ECHO_NF(ABCd)|ENCODING_PIPELINE_FAILURE|EXC|EXC|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DS|SCL|SEN|", s.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		startWithCodec(true);
		codec.encodeException = new Exception("E1");
		c.throwInIncident = false;
		c.incident = true;
		session = s.getSession();		
		session.write(packet.toBytes());
		waitFor(100);
		assertEquals("DR|ECHO_NF(ABCd)|ENCODING_PIPELINE_FAILURE|", c.getRecordedData(true));
		assertEquals("DS|", s.getRecordedData(true));
		c.incident = false;
		session.write(packet.toBytes());
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|ECHO_NF(ABCd)|ENCODING_PIPELINE_FAILURE|EXC|SCL|SEN|", c.getRecordedData(true));
		assertEquals("DS|SCL|SEN|", s.getRecordedData(true));

	}
	
	@Test
	public void testEncodeException() throws Exception {
		startWithCodec(true);
		c.incidentRecordException = true;
		c.incident = true;
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
	public void testWriteByteBufferHolder() throws Exception {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("0", new BHVE());
		p.getPipeline().add("1", codec.BHBHE());
		p.getPipeline().add("2", new BHVE());
		optimizeDataCopying = true;
		directAllocator = true;
		allocator = new TestAllocator(true, true); 
		startWithCodec(p);
		Packet packet = new Packet(PacketType.ECHO, "ABCDEFGHIJKL");
		StreamSession session = c.getSession();

		assertEquals(0, allocator.getSize());
		codec.changeInLastBuffer = 0;
		session.write(SessionTest.createHolder(session, packet.toBytes(), 2,3)).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|BUF|ECHO_RESPONSE(ABDDEFGHIJKL)|", c.getRecordedData(true));
		assertEquals("BHVE|BHVE|", getTrace());
		assertEquals(1, allocator.getSize());
		assertTrue(c.bufferRead == allocator.get().get(0));
		session.release(c.bufferRead);
		assertEquals(4, allocator.getAllocatedCount());
		assertEquals(4, allocator.getReleasedCount());
		assertEquals(0, allocator.getSize());

		codec.changeInLastBuffer = 3;
		ByteBuffer bb = session.allocate(100);
		bb.put(packet.toBytes()).flip();
		session.writenf(bb);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|BUF|ECHO_RESPONSE(BBCDEFGHIJKL)|", c.getRecordedData(true));
		assertEquals("BHVE|BHVE|", getTrace());
		assertEquals(1, allocator.getSize());
		assertTrue(c.bufferRead == allocator.get().get(0));
		session.release(c.bufferRead);
		assertEquals(6, allocator.getAllocatedCount());
		assertEquals(6, allocator.getReleasedCount());
		assertEquals(0, allocator.getSize());

		codec.changeInLastBuffer = 4;
		session.write(packet.toBytes()).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|BUF|ECHO_RESPONSE(ACCDEFGHIJKL)|", c.getRecordedData(true));
		assertEquals("BHVE|BHVE|", getTrace());
		assertEquals(1, allocator.getSize());
		assertTrue(c.bufferRead == allocator.get().get(0));
		session.release(c.bufferRead);
		assertEquals(7, allocator.getAllocatedCount());
		assertEquals(8, allocator.getReleasedCount());
		assertEquals(0, allocator.getSize());
		
		session.getCodecPipeline().remove("0");
		session.getCodecPipeline().remove("2");
		session.getCodecPipeline().remove("1");
		session.getCodecPipeline().add("0", new BVE());
		session.getCodecPipeline().add("1", codec.PBBE());
		session.getCodecPipeline().add("2", codec.BPE());
		session.getCodecPipeline().add("3", new BVE());
		session.getCodecPipeline().add("4", new BVE());
		session.write(SessionTest.createHolder(session, packet.toBytes(), 2,3)).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|BUF|ECHO_RESPONSE(ABCDEFGHIJKLe2)|", c.getRecordedData(true));
		assertEquals("BVE|BVE|BVE|", getTrace());
		assertEquals(1, allocator.getSize());
		assertTrue(c.bufferRead == allocator.get().get(0));
		session.release(c.bufferRead);
		assertEquals(11, allocator.getAllocatedCount());
		assertEquals(13, allocator.getReleasedCount());
		assertEquals(0, allocator.getSize());

		session.getCodecPipeline().remove("0");
		session.getCodecPipeline().remove("3");
		session.getCodecPipeline().remove("4");
		session.getCodecPipeline().remove("2");
		session.getCodecPipeline().remove("1");
		session.getCodecPipeline().add("0", new BBVE());
		session.getCodecPipeline().add("1", codec.BBBBE());
		session.getCodecPipeline().add("3", new BBVE());
		session.getCodecPipeline().add("4", new BBVE());
		session.writenf(SessionTest.createHolder(session, packet.toBytes(), 2,3));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|BUF|ECHO_RESPONSE(ABCDEFGHIJKL)|", c.getRecordedData(true));
		assertEquals("BBVE|BBVE|BBVE|", getTrace());
		assertEquals(1, allocator.getSize());
		assertTrue(c.bufferRead == allocator.get().get(0));
		session.release(c.bufferRead);
		assertEquals(18, allocator.getAllocatedCount());
		assertEquals(20, allocator.getReleasedCount());
		assertEquals(0, allocator.getSize());

		session.write(SessionTest.createHolder(session, packet.toBytes())).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|BUF|ECHO_RESPONSE(ABCDEFGHIJKL)|", c.getRecordedData(true));
		assertEquals("BBVE|BBVE|BBVE|", getTrace());
		assertEquals(1, allocator.getSize());
		assertTrue(c.bufferRead == allocator.get().get(0));
		session.release(c.bufferRead);
		assertEquals(20, allocator.getAllocatedCount());
		assertEquals(22, allocator.getReleasedCount());
		assertEquals(0, allocator.getSize());
		
		session.getCodecPipeline().remove("0");
		session.getCodecPipeline().remove("3");
		session.getCodecPipeline().remove("4");
		session.getCodecPipeline().remove("1");
		assertTrue(session.getCodecPipeline().encoderKeys().isEmpty());
		session.write(SessionTest.createHolder(session, packet.toBytes(), 2,3)).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|BUF|ECHO_RESPONSE(ABCDEFGHIJKL)|", c.getRecordedData(true));
		assertEquals("", getTrace());
		assertEquals(1, allocator.getSize());
		assertTrue(c.bufferRead == allocator.get().get(0));
		session.release(c.bufferRead);
		assertEquals(24, allocator.getAllocatedCount());
		assertEquals(26, allocator.getReleasedCount());
		assertEquals(0, allocator.getSize());
	}

	@Test
	public void testWriteByteBufferHolderAsMessage() throws Exception {
		codec.changeInLastBuffer = 0;
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BHBHE());
		p.getPipeline().add("2", new BBHBHE());
		optimizeDataCopying = true;
		directAllocator = true;
		allocator = new TestAllocator(true, true); 
		startWithCodec(p);
		Packet packet = new Packet(PacketType.ECHO, "ABCDEFGHIJKL");
		StreamSession session = c.getSession();
		
		session.write((Object)SessionTest.createHolder(session, packet.toBytes(), 2,3)).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|BUF|ECHO_RESPONSE(ABDDEFGHIJKL)|", c.getRecordedData(true));
		assertEquals("", getTrace());

		session.write(SessionTest.createHolder(session, packet.toBytes(), 2,3)).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|BUF|ECHO_RESPONSE(ABDDEFGHIJKL)|", c.getRecordedData(true));
		assertEquals("", getTrace());

		ByteBufferHolder holder = new ByteBufferHolder() {
			@Override
			public boolean isMessage() {
				return true;
			}
		};
		ByteBuffer[] bufs = SessionTest.createHolder(session, packet.toBytes(), 2,3).toArray();
		for (ByteBuffer buf: bufs) {
			holder.add(buf);
		}
		session.write(holder).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|BUF|ECHO_RESPONSE(ABDDEFGHIJKL)|", c.getRecordedData(true));
		assertEquals("BBHBHE|", getTrace());
		
		holder.clear();
		bufs = SessionTest.createHolder(session, packet.toBytes(), 2,3).toArray();
		for (ByteBuffer buf: bufs) {
			holder.add(buf);
		}
		session.write((Object)holder).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|BUF|ECHO_RESPONSE(ABDDEFGHIJKL)|", c.getRecordedData(true));
		assertEquals("BBHBHE|", getTrace());
		
		codec.changeInLastBuffer = 3;
		session.write((Object)new SingleByteBufferHolder(ByteBuffer.wrap(packet.toBytes()))).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|BUF|ECHO_RESPONSE(BBCDEFGHIJKL)|", c.getRecordedData(true));
		assertEquals("", getTrace());
		
		codec.changeInLastBuffer = 0;
		session.getCodecPipeline().remove("2");
		session.write((Object)SessionTest.createHolder(session, packet.toBytes(), 2,3)).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|BUF|ECHO_RESPONSE(ABDDEFGHIJKL)|", c.getRecordedData(true));
		assertEquals("", getTrace());
		
		holder.clear();
		bufs = SessionTest.createHolder(session, packet.toBytes(), 2,3).toArray();
		for (ByteBuffer buf: bufs) {
			holder.add(buf);
		}
		session.write(holder).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|BUF|ECHO_RESPONSE(ABDDEFGHIJKL)|", c.getRecordedData(true));
		assertEquals("", getTrace());
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
		assertEquals(0, SessionTest.getOutBuffers(session).length);
		session.write(b);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP2()|", s.getRecordedData(true));
		assertEquals(0, SessionTest.getOutBuffers(session).length);
		assertEquals(1, allocator.getReleasedCount());
		assertEquals(1, allocator.getAllocatedCount());
		assertTrue(allocator.getReleased().get(0) == allocator.getAllocated().get(0));
		assertTrue(b == allocator.getReleased().get(0));
		assertEquals(0, allocator.getSize());
		//b.compact();
		
		session.getCodecPipeline().remove("1");
		session.write(new Packet(PacketType.NOP,"1").toBytes());
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(1)|", s.getRecordedData(true));
		assertEquals(0, SessionTest.getOutBuffers(session).length);
		assertEquals(2, allocator.getReleasedCount());
		assertEquals(0, allocator.getSize());
		//b.compact();
		
		ByteBuffer b0 = session.allocate(100);
		b0.put(new Packet(PacketType.NOP,"2").toBytes());
		b0.put((byte)0);
		b0.flip();
		session.write(b0,b0.remaining()-1);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(2)|", s.getRecordedData(true));
		assertEquals(0, SessionTest.getOutBuffers(session).length);
		assertEquals(3, allocator.getReleasedCount());
		assertEquals(1, allocator.getSize());
		session.release(b0);
		assertEquals(0, allocator.getSize());
		
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
		
		session.getCodecPipeline().replace("1", "1", new ArrayToBufferEncoder());
		b0 = session.allocate(100);
		b0.put(new Packet(PacketType.NOP,"2").toBytes());
		b0.put((byte)0);
		b0.flip();
		int relCount = allocator.getReleasedCount();
		int allCount = allocator.getAllocatedCount();
		session.write(b0);
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals("DR|NOP(2)|", s.getRecordedData(true));
		assertEquals(2, allocator.getReleasedCount()-relCount);
		assertEquals(0, allocator.getAllocatedCount()-allCount);
		assertEquals(0, allocator.getSize());
		
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
		
		assertEquals(0, allocator.getAllocatedCount());
		ByteBuffer b = SessionTest.getInBuffer(c.getSession());
		assertNull(b);
		s.getSession().write(new Packet(PacketType.NOP).toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		waitFor(50);
		assertEquals("DR|BUF|NOP()|", c.getRecordedData(true));
		assertEquals(1, allocator.getReleasedCount());
		assertEquals(1, allocator.getAllocatedCount());
		assertTrue(allocator.getAllocated().get(0) == allocator.getReleased().get(0));
		assertNull(SessionTest.getInBuffer(c.getSession()));
		
		c.getSession().getCodecPipeline().remove("1");
		s.getSession().write(new Packet(PacketType.NOP,"1").toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|BUF|NOP(1)|", c.getRecordedData(true));
		assertEquals(1, allocator.getReleasedCount());
		assertEquals(2, allocator.getAllocatedCount());
		assertTrue(c.bufferRead == allocator.getAllocated().get(1));
		
		c.getSession().getCodecPipeline().add("1", new BVD());
		s.getSession().write(new Packet(PacketType.NOP,"2").toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|BUF|NOP(2)|", c.getRecordedData(true));
		assertEquals("BVD|", getTrace());
		assertEquals(1, allocator.getReleasedCount());
		assertEquals(3, allocator.getAllocatedCount());
		assertTrue(c.bufferRead == allocator.getAllocated().get(2));

		c.getSession().getCodecPipeline().add("2", new BBVD());
		s.getSession().write(new Packet(PacketType.NOP,"3").toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DR|BUF|NOP(3)|", c.getRecordedData(true));
		assertEquals("BVD|BBVD|", getTrace());
		assertEquals(1, allocator.getReleasedCount());
		assertEquals(4, allocator.getAllocatedCount());
		assertTrue(c.bufferRead == allocator.getAllocated().get(3));
		
		c.exceptionRecordException = true;
		c.getSession().getCodecPipeline().add("3", new ExeD());
		s.getSession().write(new Packet(PacketType.NOP,"3").toBytes());
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		assertEquals("DR|EXC|(E)|SCL|SEN|", c.getRecordedData(true));
		assertEquals("BVD|BBVD|", getTrace());
		waitFor(50);
		assertEquals(2, allocator.getReleasedCount());
		assertEquals(5, allocator.getAllocatedCount());
		assertEquals(3, allocator.getSize());
		
	}

	byte[] bytes(PacketType type) {
		return new Packet(type,"000").toBytes();
	}
	
	void assertWrite(String expected) throws Exception {
		c.waitForDataSent(TIMEOUT);
		s.waitForDataRead(TIMEOUT);
		assertEquals(expected, s.getRecordedData(true));
	}
	
	void assertWrite2(String expected) throws Exception {
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals(expected, c.getRecordedData(true));
	}
	
	@Test
	public void testReplaceInEventDrivenCodec() throws Exception {
		DefaultCodecExecutor e = new DefaultCodecExecutor();
		ICodecPipeline p = e.getPipeline();
		ReplaceEncoder EA = new ReplaceEncoder('A');
		ReplaceEncoder EB = new ReplaceEncoder('B', EA, "E");
		ReplaceDecoder DC = new ReplaceDecoder('C');
		ReplaceDecoder DD = new ReplaceDecoder('D', DC, "D");
		
		p.add(1, new ArrayToBufferEncoder());
		p.add("E", EA);
		p.add(2, new BufferToArrayEncoder());
		startWithCodec(e);
		
		StreamSession session = c.getSession();
		session.write(bytes(PacketType.NOP));
		assertWrite("DR|NOP(00A)|");
		session.getCodecPipeline().replace("E", "E", EB);
		session.write(bytes(PacketType.NOP));
		assertWrite("DR|NOP(00B)|");
		session.write(bytes(PacketType.NOP));
		assertWrite("DR|NOP(00A)|");
		session.getCodecPipeline().replace("E", "E", EB);
		session.suspendWrite();
		session.write(bytes(PacketType.NOP));
		session.write(bytes(PacketType.NOP));
		session.resumeWrite();
		waitFor(100);
		assertWrite("DR|NOP(00B)|NOP(00A)|");
		
		p.add(3, new BaseDecoder());
		p.add(4, new BufferToArrayDecoder());
		p.add("D", DC);
		p.add(5, new ArrayToBufferDecoder());
		p.remove("E");
		
		c.getRecordedData(true);
		session = s.getSession();
		session.write(bytes(PacketType.NOP));
		assertWrite2("DR|BUF|NOP(00C)|");
		p.replace("D", "D", DD);
		session.write(bytes(PacketType.NOP));
		assertWrite2("DR|BUF|NOP(00D)|");
		session.write(bytes(PacketType.NOP));
		assertWrite2("DR|BUF|NOP(00C)|");
		p.replace("D", "D", DD);
		session.suspendWrite();
		session.write(bytes(PacketType.NOP));
		session.write(bytes(PacketType.NOP));
		session.resumeWrite();
		waitFor(100);
		assertWrite2("DR|BUF|NOP(00D)|BUF|NOP(00C)|");
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
	}
	
	@Test
	public void testGenericBaseBuffer() throws Exception {
		DefaultCodecExecutor e = new DefaultCodecExecutor();
		ICodecPipeline p = e.getPipeline();
		
		p.add("B", codec.BasePD2());
		startWithCodec(e);
		byte[] data = new Packet(PacketType.NOP, "12345678").toBytes();
		
		s.getSession().write(data, 0, 5);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataReceived(TIMEOUT);
		waitFor(100);
		assertEquals("DS|",s.getRecordedData(true));
		assertEquals("DR|",c.getRecordedData(true));
		s.getSession().write(data, 5, data.length-5);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|",s.getRecordedData(true));
		assertEquals("DR|M(NOP[12345678])|",c.getRecordedData(true));
	}
	
	static class BaseDecoder extends ArrayToBufferDecoder implements IBaseDecoder<byte[],ByteBuffer> {

		@Override
		public int available(ISession session, ByteBuffer buffer, boolean flipped) {
			if (flipped) {
				return Packet.available(buffer.array(), buffer.position(), buffer.limit());
			}
			return Packet.available(buffer.array(), 0, buffer.position());
		}

		@Override
		public int available(ISession session, byte[] buffer, int off, int len) {
			return Packet.available(buffer, off, len);
		}
	}
	
	static class ReplaceDecoder extends ReplaceCodec implements IDecoder<byte[],byte[]> {

		ReplaceDecoder(char last) {
			super((byte) last);
		}
		
		ReplaceDecoder(char last, ReplaceDecoder replace, Object replaceKey) {
			super((byte) last, replace, replaceKey);
		}

		@Override
		public void decode(ISession session, byte[] data, List<byte[]> out) throws Exception {
			code(data, out);
		}
	}
	
	static class ReplaceEncoder extends ReplaceCodec implements IEncoder<byte[],byte[]> {

		ReplaceEncoder(char last) {
			super((byte) last);
		}
		
		ReplaceEncoder(char last, ReplaceEncoder replace, Object replaceKey) {
			super((byte) last, replace, replaceKey);
		}

		@Override
		public void encode(ISession session, byte[] data, List<byte[]> out) throws Exception {
			code(data, out);
		}
	}

	static class ReplaceCodec implements ICodec<byte[],byte[]>, IEventDrivenCodec {

		byte lastByte;
		
		ICodecPipeline pipeline;
		
		ReplaceCodec replace;
		
		Object replaceKey;
		
		ReplaceCodec(byte lastByte) {
			this.lastByte = lastByte;
		}
		
		ReplaceCodec(byte lastByte, ReplaceCodec replace, Object replaceKey) {
			this.lastByte = lastByte;
			this.replace = replace;
			this.replaceKey = replaceKey;
		}
		
		@Override
		public Class<byte[]> getInboundType() {
			return byte[].class;
		}

		@Override
		public Class<byte[]> getOutboundType() {
			return byte[].class;
		}

		public void code(byte[] data, List<byte[]> out) throws Exception {
			data[data.length-1] = lastByte;
			out.add(data);
			if (pipeline != null && replaceKey != null) {
				if (replace instanceof IDecoder) {
				pipeline.replace(replaceKey, replaceKey, (IDecoder<?, ?>)replace);
				}
				else {
					pipeline.replace(replaceKey, replaceKey, (IEncoder<?, ?>)replace);
				}
			}
		}

		@Override
		public void added(ISession session, ICodecPipeline pipeline) {
			this.pipeline = pipeline;
		}

		@Override
		public void event(ISession session, SessionEvent event) {
		}

		@Override
		public void removed(ISession session, ICodecPipeline pipeline) {
			pipeline = null;
		}
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

	class BBVE implements IEncoder<ByteBuffer,Void> {
		@Override public Class<ByteBuffer> getInboundType() {return ByteBuffer.class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void encode(ISession session, ByteBuffer data, List<Void> out)	throws Exception {trace("BBVE");}
	}

	class BHVE implements IEncoder<IByteBufferHolder,Void> {
		@Override public Class<IByteBufferHolder> getInboundType() {return IByteBufferHolder.class;}
		@Override public Class<Void> getOutboundType() {return Void.class;}
		@Override public void encode(ISession session, IByteBufferHolder data, List<Void> out)	throws Exception {trace("BHVE");}
	}

	class BBHBHE implements IEncoder<ByteBufferHolder,IByteBufferHolder> {
		@Override public Class<ByteBufferHolder> getInboundType() {return ByteBufferHolder.class;}
		@Override public Class<IByteBufferHolder> getOutboundType() {return IByteBufferHolder.class;}
		@Override public void encode(ISession session, ByteBufferHolder data, List<IByteBufferHolder> out)	throws Exception {
			trace("BBHBHE");
			out.add(data);
		}
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
