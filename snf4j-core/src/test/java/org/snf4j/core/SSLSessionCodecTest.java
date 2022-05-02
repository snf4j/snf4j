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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.TestCodec.BBDEv;
import org.snf4j.core.allocator.TestAllocator;
import org.snf4j.core.codec.DefaultCodecExecutor;
import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.session.IllegalSessionStateException;

public class SSLSessionCodecTest {

	long TIMEOUT = 2000;
	int PORT = 7777;

	Server s;
	Client c;
	
	static final String CLIENT_RDY_TAIL = SSLSessionTest.CLIENT_RDY_TAIL;
	
	boolean directAllocator;
	TestAllocator allocator;
	boolean optimizeDataCopying;
	
	TestCodec codec;

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

	private void startWithCodec(DefaultCodecExecutor pipeline)  throws Exception {
		s = new Server(PORT, true);
		c = new Client(PORT, true);
		
		c.directAllocator = directAllocator;
		c.allocator = allocator;
		c.optimizeDataCopying = optimizeDataCopying;
		c.codecPipeline = pipeline;
		
		s.start();
		c.start();

		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		c.getRecordedData(true);
		s.getRecordedData("RDY|", true);
	}
	
	private void startWithCodec(boolean bytesEncoder) throws Exception {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BasePD());
		p.getPipeline().add("2", codec.PBD());
		p.getPipeline().add("3", bytesEncoder ? codec.PBE() : codec.PBBE());
		p.getPipeline().add("4", codec.BPE());
		startWithCodec(p);
	}

	private void stop() throws Exception {
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}

	@Test
	public void testWriteByteBufferHolder() throws Exception {
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BHBHE());
		optimizeDataCopying = true;
		directAllocator = true;
		allocator = new TestAllocator(true, true); 
		startWithCodec(p);
		Packet packet = new Packet(PacketType.ECHO, "ABCDEFGHIJKL");
		StreamSession session = c.getSession();
		waitFor(50);
		c.getRecordedData(true);
		
		assertEquals(0, allocator.getSize());
		codec.changeInLastBuffer = 0;
		session.write(SessionTest.createHolder(session, packet.toBytes(), 2,3)).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|BUF|ECHO_RESPONSE(ABDDEFGHIJKL)|", c.getRecordedData(true));
		waitFor(50);
		assertEquals(1, allocator.getSize());
		assertTrue(c.bufferRead == allocator.get().get(0));
		session.release(c.bufferRead);
		assertEquals(0, allocator.getSize());
		
		codec.changeInLastBuffer = 3;
		session.writenf(SessionTest.createHolder(session, packet.toBytes()));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|BUF|ECHO_RESPONSE(BBCDEFGHIJKL)|", c.getRecordedData(true));
		waitFor(50);
		assertEquals(1, allocator.getSize());
		assertTrue(c.bufferRead == allocator.get().get(0));
		session.release(c.bufferRead);
		assertEquals(0, allocator.getSize());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		
		p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BHBHE());
		optimizeDataCopying = true;
		directAllocator = false;
		allocator = new TestAllocator(false, false); 
		startWithCodec(p);
		session = c.getSession();
		waitFor(50);
		c.getRecordedData(true);
		
		codec.changeInLastBuffer = 0;
		session.write(SessionTest.createHolder(session, packet.toBytes(), 2,3)).sync(TIMEOUT);
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABDDEFGHIJKL)|", c.getRecordedData(true));
		codec.changeInLastBuffer = 3;
		session.writenf(SessionTest.createHolder(session, packet.toBytes()));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(BBCDEFGHIJKL)|", c.getRecordedData(true));
		
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
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.trimRecordedData(CLIENT_RDY_TAIL));
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
		session.write(packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCe2d)|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertTrue(future.isSuccessful());
		session.writenf(ByteBuffer.wrap(packet.toBytes()));
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCe2d)|", c.getRecordedData(true));

		stop();
	}
	
	@Test
	public void testEncodeException() throws Exception {
		startWithCodec(true);
		waitFor(100);
		c.incidentRecordException = true;
		c.incident = true;
		Packet packet = new Packet(PacketType.ECHO, "ABC");
		StreamSession session = c.getSession();
		codec.encodeException = new Exception("E1");
		IFuture<Void> future = session.write(packet.toBytes());
		future.await(TIMEOUT);
		waitFor(100);
		assertEquals("ENCODING_PIPELINE_FAILURE(E1)|", c.trimRecordedData(CLIENT_RDY_TAIL));
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
		assertEquals("ENCODING_PIPELINE_FAILURE(E2)|DS|SCL|SEN|", c.getRecordedData(true));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		//quick close on incident
		startWithCodec(true);
		waitFor(100);
		c.incidentRecordException = true;
		c.incidentQuickClose = true;
		codec.encodeException = new Exception("E3");
		session = c.getSession();
		future = session.write(packet.toBytes());
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("ENCODING_PIPELINE_FAILURE(E3)|DS|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		//dirty close on incident
		startWithCodec(true);
		waitFor(100);
		c.incidentRecordException = true;
		c.incidentDirtyClose = true;
		codec.encodeException = new Exception("E4");
		session = c.getSession();
		future = session.write(packet.toBytes());
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("ENCODING_PIPELINE_FAILURE(E4)|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
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
		assertEquals("DS|DR|EXC|(E1)|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
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
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCe])|", c.trimRecordedData(CLIENT_RDY_TAIL));
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
		waitFor(50);
		assertEquals("DS|DR|M(ECHO_RESPONSE[ABCe])|M(ECHO_RESPONSE[ABCe])|", c.trimRecordedData(CLIENT_RDY_TAIL));
		stop();

		startWithCodec(new DefaultCodecExecutor());
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		session.write(packet.toBytes()).sync(TIMEOUT);
		assertTrue(session.write(new Integer(0)).await(TIMEOUT).isCancelled());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.trimRecordedData(CLIENT_RDY_TAIL));
		assertEquals(1, c.availableCounter);
		stop();
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
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.trimRecordedData(CLIENT_RDY_TAIL));
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
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.trimRecordedData(CLIENT_RDY_TAIL));
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
		assertEquals("DS|DR|", c.trimRecordedData(CLIENT_RDY_TAIL));
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
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|ECHO_RESPONSE(ABCed)|ECHO_RESPONSE(ABCed)|", c.trimRecordedData(CLIENT_RDY_TAIL));
		stop();
		codec.duplicatingDecode = false;
		
		//direct buffer
		directAllocator = true;
		startWithCodec(true);
		session = c.getSession();
		session.write(packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.trimRecordedData(CLIENT_RDY_TAIL));
		stop();

		//direct buffer without decoder
		startWithCodec(new DefaultCodecExecutor());
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		session.write(packet.toBytes());
		s.waitForDataSent(TIMEOUT);
		c.waitForDataRead(TIMEOUT);
		assertEquals("DS|DR|ECHO_RESPONSE(ABC)|", c.trimRecordedData(CLIENT_RDY_TAIL));
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
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|", c.trimRecordedData(CLIENT_RDY_TAIL));
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
	public void testCloseInsideDecoder() throws Exception {
		codec.decodeClose = true;
		startWithCodec(true);
		Packet packet = new Packet(PacketType.ECHO, "ABC");
		StreamSession session = c.getSession();
		session.write(packet);
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		SSLSessionTest.assertTLSVariants("DS|DR|ECHO(ABCe)|DS|DR|?{DS|}SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|DS|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
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
		SSLSessionTest.assertTLSVariants("DS|DR|ECHO(ABCe)|DS|DR|?{DS|}SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|DS|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
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
		assertEquals("DS|DR|ECHO(ABCe)|DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|DR|ECHO_RESPONSE(ABCed)|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
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
		SSLSessionTest.assertTLSVariants("DS|DR|?{DS|}SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		codec.encodeClose = false;
		
		codec.encodeQuickClose = true;
		startWithCodec(true);
		packet = new Packet(PacketType.ECHO, "ABC");
		session = c.getSession();
		IFuture<Void> future = session.write(packet);
		assertTrue(future.await(TIMEOUT).isCancelled());
		s.waitForSessionEnding(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		SSLSessionTest.assertTLSVariants("DS|DR|?{DS|}SCL|SEN|", s.getRecordedData(true));
		assertEquals("DS|SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
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
		assertEquals("DS|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|SCL|SEN|", s.getRecordedData(true));
		assertEquals("SCL|SEN|", c.trimRecordedData(CLIENT_RDY_TAIL));
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
		waitFor(50);
	
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
	public void testGenericBaseBuffer() throws Exception {
		DefaultCodecExecutor e = new DefaultCodecExecutor();
		ICodecPipeline p = e.getPipeline();
		
		p.add("B", codec.BasePD2());
		startWithCodec(e);
		waitFor(50);
		s.getRecordedData(true);
		c.getRecordedData(true);
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
}
