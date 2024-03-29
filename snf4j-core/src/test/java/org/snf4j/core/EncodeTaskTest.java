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

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.codec.DefaultCodecExecutor;
import org.snf4j.core.codec.IEncoder;
import org.snf4j.core.future.IDelegatingFuture;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.future.SuccessfulFuture;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.logger.LoggerRecorder;
import org.snf4j.core.session.ISession;

public class EncodeTaskTest {
	
	long TIMEOUT = 2000;
	int PORT = 7777;

	Server s;
	Client c;
	
	AtomicBoolean traceLock = new AtomicBoolean(false);
	
	StringBuilder trace = new StringBuilder();
	
	boolean discarding;

	int duplicating;
	
	RuntimeException writerException;

	Exception encoderException;
	
	private void trace(String s) {
		trace.append(s);
	}
	
	private String getTrace() {
		String s = trace.toString();
		trace.setLength(0);
		return s;
	}
	
	@Before
	public void before() {
		s = c = null;
		discarding = false;
		duplicating = 0;
		writerException = null;
		encoderException = null;
	}
	
	@After
	public void after() throws InterruptedException {
		if (c != null) c.stop(TIMEOUT);
		if (s != null) s.stop(TIMEOUT);
	}
	
	private void setOptimizeCopying(InternalSession session, boolean canOwn) throws Exception {
		Field f = InternalSession.class.getDeclaredField("optimizeCopying");
		
		f.setAccessible(true);
		f.setBoolean(session, canOwn);
	}
	
	private void setWriter(StreamSession session, IEncodeTaskWriter writer) throws Exception {
		Field f = StreamSession.class.getDeclaredField("encodeTaskWriter");
		
		f.setAccessible(true);
		f.set(session, writer);
	}

	private void setCodec(StreamSession session, DefaultCodecExecutor pipeline) throws Exception {
		Field f = InternalSession.class.getDeclaredField("codec");
		
		f.setAccessible(true);
		f.set(session, new CodecExecutorAdapter(pipeline, session));
	}
	
	private String getString(ByteBuffer buffer) {
		byte[] b = new byte[buffer.remaining()];
		buffer.duplicate().get(b);
		return new String(b);
	}
	
	@Test
	public void testConstructors()throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);
		
		s.start();
		c.start();

		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		
		StreamSession session = c.getSession();
		String prefix = "org.snf4j.core.EncodeTask[session=" + session;
		
		//constructor 1
		setOptimizeCopying(session, false);
		byte[] bytes = "ABC".getBytes();
		EncodeTask task = new EncodeTask(session, bytes);
		assertNotNull(task.bytes);
		assertNull(task.holder);
		assertNull(task.buffer);
		assertNull(task.msg);
		assertTrue(task.bytes != bytes);
		assertTrue(task.session == session);
		bytes[0] = 'X';
		assertEquals("ABC", new String(task.bytes));
		setOptimizeCopying(session, true);
		task = new EncodeTask(session, bytes);
		assertNotNull(task.bytes);
		assertNull(task.holder);
		assertNull(task.buffer);
		assertNull(task.msg);
		assertTrue(task.bytes == bytes);
		assertTrue(task.session == session);
		assertEquals("XBC", new String(task.bytes));
		bytes[0] = 'Y';
		assertEquals("YBC", new String(task.bytes));
		assertEquals(prefix + " length=3]", task.toString());
		
		//constructor 2
		setOptimizeCopying(session, false);
		bytes = "1ABC2".getBytes();
		task = new EncodeTask(session, bytes, 1, 3);
		assertNotNull(task.bytes);
		assertNull(task.holder);
		assertNull(task.buffer);
		assertNull(task.msg);
		assertTrue(task.bytes != bytes);
		assertTrue(task.session == session);
		bytes[1] = 'X';
		assertEquals("ABC", new String(task.bytes));
		task = new EncodeTask(session, bytes, 0, 5);
		assertNotNull(task.bytes);
		assertNull(task.holder);
		assertNull(task.buffer);
		assertTrue(task.bytes != bytes);
		setOptimizeCopying(session, true);
		task = new EncodeTask(session, bytes, 1, 3);
		assertNotNull(task.bytes);
		assertNull(task.buffer);
		assertNull(task.msg);
		assertTrue(task.bytes != bytes);
		assertTrue(task.session == session);
		assertEquals("XBC", new String(task.bytes));
		bytes[1] = 'Y';
		assertEquals("XBC", new String(task.bytes));
		assertEquals(prefix + " length=3]", task.toString());
		task = new EncodeTask(session, bytes, 0, 4);
		assertNotNull(task.bytes);
		assertEquals("1YBC", new String(task.bytes));
		task = new EncodeTask(session, bytes, 1, 4);
		assertNotNull(task.bytes);
		assertEquals("YBC2", new String(task.bytes));
		task = new EncodeTask(session, bytes, 0, 5);
		assertNotNull(task.bytes);
		assertEquals("1YBC2", new String(task.bytes));
		assertTrue(task.bytes == bytes);
		
		//constructor 3
		setOptimizeCopying(session, false);
		bytes = "ABC".getBytes();
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		task = new EncodeTask(session, buffer);
		assertNotNull(task.bytes);
		assertNull(task.holder);
		assertNull(task.buffer);
		assertNull(task.msg);
		assertTrue(task.bytes != bytes);
		assertTrue(task.session == session);
		bytes[0] = 'X';
		assertEquals("ABC", new String(task.bytes));
		setOptimizeCopying(session, true);
		buffer = ByteBuffer.wrap(bytes);
		task = new EncodeTask(session, buffer);
		assertNull(task.bytes);
		assertNull(task.holder);
		assertNotNull(task.buffer);
		assertNull(task.msg);
		assertTrue(task.buffer == buffer);
		assertTrue(task.session == session);
		assertEquals("XBC", getString(task.buffer));
		buffer.get();
		assertEquals("BC", getString(task.buffer));
		assertEquals(prefix + " length=3]", task.toString());
		
		//constructor 4
		setOptimizeCopying(session, false);
		bytes = "ABCD".getBytes();
		buffer = ByteBuffer.wrap(bytes);
		task = new EncodeTask(session, buffer, 3);
		assertNotNull(task.bytes);
		assertNull(task.holder);
		assertNull(task.buffer);
		assertNull(task.msg);
		assertTrue(task.bytes != bytes);
		assertTrue(task.session == session);
		bytes[0] = 'X';
		assertEquals("ABC", new String(task.bytes));
		setOptimizeCopying(session, true);
		buffer = ByteBuffer.wrap(bytes);
		task = new EncodeTask(session, buffer, 3);
		assertNull(task.holder);
		assertNotNull(task.bytes);
		assertNull(task.buffer);
		assertNull(task.msg);
		assertTrue(task.bytes != bytes);
		assertTrue(task.session == session);
		bytes[0] = 'Y';
		assertEquals("XBC", new String(task.bytes));
		buffer = ByteBuffer.wrap(bytes);
		task = new EncodeTask(session, buffer, 4);
		assertNull(task.bytes);
		assertNotNull(task.buffer);
		assertNull(task.msg);
		assertTrue(task.buffer == buffer);
		assertTrue(task.session == session);
		assertEquals("YBCD", getString(task.buffer));
		buffer.get();
		assertEquals("BCD", getString(task.buffer));
		assertEquals(prefix + " length=4]", task.toString());

		//constructor 5
		setOptimizeCopying(session, false);
		bytes = "ABCDEFGHIJKL".getBytes();
		ByteBufferHolder holder = SessionTest.createHolder(session, bytes);
		task = new EncodeTask(session, holder);
		assertNull(task.holder);
		assertArrayEquals(bytes, task.bytes);
		assertNull(task.buffer);
		assertNull(task.msg);
		assertTrue(task.session == session);
		holder = SessionTest.createHolder(session, bytes, 1, 0, 3);
		task = new EncodeTask(session, holder);
		assertArrayEquals(bytes, task.bytes);
		task = new EncodeTask(session, new ByteBufferHolder());
		assertEquals(0, task.bytes.length);
		setOptimizeCopying(session, true);
		holder = SessionTest.createHolder(session, bytes, 1);
		task = new EncodeTask(session, holder);
		assertSame(holder, task.holder);
		assertNull(task.bytes);
		assertNull(task.buffer);
		assertNull(task.msg);
		assertTrue(task.session == session);
		
		//constructor 6
		String text = "ABCD";
		task = new EncodeTask(session, text);
		assertNull(task.holder);
		assertNull(task.bytes);
		assertNull(task.buffer);
		assertNotNull(task.msg);
		assertTrue(task.msg == text);
		assertTrue(task.session == session);
		task = new EncodeTask(session, (Object)text.getBytes());
		assertNotNull(task.bytes);
		assertNull(task.holder);
		assertNull(task.buffer);
		assertNull(task.msg);
		assertEquals("ABCD", new String(task.bytes));
		assertTrue(task.session == session);
		task = new EncodeTask(session, (Object)ByteBuffer.wrap(text.getBytes()));
		assertNull(task.bytes);
		assertNotNull(task.buffer);
		assertNull(task.msg);
		assertEquals("ABCD", getString(task.buffer));
		assertTrue(task.session == session);
		assertEquals(prefix + " message]", task.toString());

		
		
		task.future = new Future();
		assertEquals(prefix + " message future]", task.toString());
		task.remoteAddress = session.getRemoteAddress();
		assertEquals(prefix + " message future remote=" + session.getRemoteAddress()+"]", task.toString());
		task.future = null;
		assertEquals(prefix + " message remote=" + session.getRemoteAddress()+"]", task.toString());
		Field f = EncodeTask.class.getDeclaredField("session");
		f.setAccessible(true);
		f.set(task, null);
		assertEquals("org.snf4j.core.EncodeTask[session=null message remote="+session.getRemoteAddress()+"]", task.toString());
		
		byte[] b = new byte[11];
		task = EncodeTask.simple(session, b);
		assertTrue(session == task.session);
		assertTrue(b == task.bytes);
		assertEquals(11, task.length);
		assertNull(task.msg);
		assertNull(task.buffer);
		assertNull(task.holder);

		ByteBuffer bb = ByteBuffer.wrap(b);
		task = EncodeTask.simple(session, bb);
		assertTrue(session == task.session);
		assertTrue(bb == task.buffer);
		assertEquals(11, task.length);
		assertNull(task.msg);
		assertNull(task.bytes);
		assertNull(task.holder);

		holder = SessionTest.createHolder(session, bytes, 1, 0, 3);
		task = EncodeTask.simple(session, holder);
		assertTrue(session == task.session);
		assertNull(task.buffer);
		assertTrue(holder == task.holder);
		assertEquals(12, task.length);
		assertNull(task.msg);
		assertNull(task.bytes);

		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
	}
	
	static class Future extends SuccessfulFuture<Void> implements IDelegatingFuture<Void> {

		public Future() {
			super(null);
		}

		@Override
		public void setDelegate(IFuture<Void> delegate) {
		}
		
	}
	
	@Test
	public void testRegister() throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);
		
		s.start();
		c.start();

		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		
		StreamSession session = c.getSession();
		EncodeTask task = new Task(session, "T1|");
		IFuture<Void> future = task.register();
		LockUtils.waitFor(traceLock, TIMEOUT);
		assertEquals("T1|", getTrace());
		assertNull(task.remoteAddress);
		assertNotNull(task.future);
		assertTrue(task.future == future);

		task = new Task(session, "T2|");
		task.registernf();
		LockUtils.waitFor(traceLock, TIMEOUT);
		assertEquals("T2|", getTrace());
		assertNull(task.remoteAddress);
		assertNull(task.future);

		SocketAddress address = session.getLocalAddress();
		task = new Task(session, "T3|");
		future = task.register(address);
		LockUtils.waitFor(traceLock, TIMEOUT);
		assertEquals("T3|", getTrace());
		assertNotNull(task.remoteAddress);
		assertTrue(task.remoteAddress == address);
		assertTrue(task.future == future);

		task = new Task(session, "T4|");
		task.registernf(address);
		LockUtils.waitFor(traceLock, TIMEOUT);
		assertEquals("T4|", getTrace());
		assertNotNull(task.remoteAddress);
		assertTrue(task.remoteAddress == address);
		assertNull(task.future);
		
		c.stop(TIMEOUT);
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		
		task = new Task(session, "T5|");
		try {
			task.register();
			fail("not thrown");
		}
		catch (IllegalStateException e) {}
		try {
			task.registernf();
			fail("not thrown");
		}
		catch (IllegalStateException e) {}
		try {
			task.register(address);
			fail("not thrown");
		}
		catch (IllegalStateException e) {}
		try {
			task.registernf(address);
			fail("not thrown");
		}
		catch (IllegalStateException e) {}
	}
	
	DefaultCodecExecutor createPipeline(String encoders) {
		DefaultCodecExecutor pipeline = new DefaultCodecExecutor();
		IEncoder<?,?> enc;
		int i = 1;
		
		for (String s: encoders.split(";")) {
			if (s.equals("SB")) {
				enc = new SB();
			}
			else if (s.equals("BS")) {
				enc = new BS();
			}
			else if (s.equals("BBS")) {
				enc = new BBS();
			}
			else if (s.equals("SBB")) {
				enc = new SBB();
			}
			else if (s.equals("BHS")) {
				enc = new BHS();
			}
			else if (s.equals("SBH")) {
				enc = new SBH();
			}
			else {
				throw new IllegalArgumentException("Unknown " + s);
			}
			pipeline.getPipeline().add("" + (i++), enc);
		}
		return pipeline;
	}
	
	private ByteBuffer getBuffer(String s) {
		return ByteBuffer.wrap(s.getBytes());
	}
	
	@Test
	public void testRun() throws Exception {
		s = new Server(PORT);
		c = new Client(PORT);
		
		c.incidentRecordException = true;
		s.start();
		c.start();

		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);

		StreamSession session = c.getSession();
		setOptimizeCopying(session, true);
		setWriter(session, new Writer());
		
		//inbound message
		setCodec(session, createPipeline("SB"));
		EncodeTask task = new EncodeTask(session, "ABC");
		task.future = session.futuresController.getDelegatingFuture();
		task.run();
		assertEquals("nB(ABC)f", getTrace());
		assertTrue(task.future.isSuccessful());
		task = new EncodeTask(session, "ABC");
		task.run();
		assertEquals("nB(ABC)n", getTrace());
		task = new EncodeTask(session, new Integer(1));
		task.future = session.futuresController.getDelegatingFuture();
		task.run();
		assertEquals("", getTrace());
		assertTrue(task.future.isCancelled());
		task = new EncodeTask(session, new Integer(1));
		task.run();
		assertEquals("", getTrace());
		
		//inbound bytes
		setCodec(session, createPipeline("SB;BS"));
		task = new EncodeTask(session, "ABC".getBytes());
		task.future = session.futuresController.getDelegatingFuture();
		task.run();
		assertEquals("nB(ABC)f", getTrace());
		assertTrue(task.future.isSuccessful());
		task = new EncodeTask(session, "ABC".getBytes());
		task.run();
		assertEquals("nB(ABC)n", getTrace());
		setCodec(session, createPipeline("SB"));
		task = new EncodeTask(session, "ABC".getBytes());
		task.future = session.futuresController.getDelegatingFuture();
		task.run();
		assertEquals("nB(ABC)f", getTrace());
		assertTrue(task.future.isSuccessful());
		task = new EncodeTask(session, "ABC".getBytes());
		task.run();
		assertEquals("nB(ABC)n", getTrace());

		//inbound byte buffer
		setCodec(session, createPipeline("SBB;BBS"));
		task = new EncodeTask(session, getBuffer("ABC"));
		task.future = session.futuresController.getDelegatingFuture();
		task.run();
		assertEquals("nBB(ABC)f", getTrace());
		assertTrue(task.future.isSuccessful());
		task = new EncodeTask(session, getBuffer("ABC"));
		task.run();
		assertEquals("nBB(ABC)n", getTrace());
		setCodec(session, createPipeline("SBB"));
		task = new EncodeTask(session, getBuffer("ABC"));
		task.future = session.futuresController.getDelegatingFuture();
		task.run();
		assertEquals("nBB(ABC)f", getTrace());
		assertTrue(task.future.isSuccessful());
		task = new EncodeTask(session, getBuffer("ABC"));
		task.run();
		assertEquals("nBB(ABC)n", getTrace());
		
		//inbound byte buffer holder
		setCodec(session, createPipeline("SBH;BHS"));
		ByteBufferHolder holder = SessionTest.createHolder(session, "ABCDEF".getBytes(), 3);
		task = new EncodeTask(session, holder);
		task.future = session.futuresController.getDelegatingFuture();
		task.run();
		assertEquals("nBH(ABC+DEF+)f", getTrace());
		assertTrue(task.future.isSuccessful());
		holder = SessionTest.createHolder(session, "ABCDEF".getBytes(), 3);
		task = new EncodeTask(session, holder);
		task.run();
		assertEquals("nBH(ABC+DEF+)n", getTrace());
		setCodec(session, createPipeline("SBH"));
		holder = SessionTest.createHolder(session, "ABCDEF".getBytes(), 3);
		task = new EncodeTask(session, holder);
		task.future = session.futuresController.getDelegatingFuture();
		task.run();
		assertEquals("nBH(ABC+DEF+)f", getTrace());
		assertTrue(task.future.isSuccessful());
		holder = SessionTest.createHolder(session, "ABCDEF".getBytes(), 3);
		task = new EncodeTask(session, holder);
		task.run();
		assertEquals("nBH(ABC+DEF+)n", getTrace());
		
		//discarding pipeline
		discarding = true;
		setCodec(session, createPipeline("SB"));
		task = new EncodeTask(session, "ABC");
		task.future = session.futuresController.getDelegatingFuture();
		task.run();
		assertEquals("", getTrace());
		assertTrue(task.future.isSuccessful());
		task = new EncodeTask(session, "ABC");
		task.run();
		assertEquals("", getTrace());
		discarding = false;
		
		//writer throwing exception
		writerException = new IllegalStateException();
		setCodec(session, createPipeline("SB"));
		task = new EncodeTask(session, "ABC");
		task.future = session.futuresController.getDelegatingFuture();
		task.run();
		assertEquals("", getTrace());
		assertTrue(task.future.isFailed());
		assertTrue(task.future.cause() == writerException);
		task = new EncodeTask(session, "ABC");
		task.run();
		assertEquals("", getTrace());
		writerException = null;
		
		//duplicating encoders
		duplicating = 2;
		setCodec(session, createPipeline("SB"));
		task = new EncodeTask(session, "ABC");
		task.future = session.futuresController.getDelegatingFuture();
		task.run();
		assertEquals("nB(ABC)nnB(ABC)nnB(ABC)f", getTrace());
		assertTrue(task.future.isSuccessful());
		task = new EncodeTask(session, "ABC");
		task.run();
		assertEquals("nB(ABC)nnB(ABC)nnB(ABC)n", getTrace());

		setCodec(session, createPipeline("SBB"));
		task = new EncodeTask(session, "ABC");
		task.future = session.futuresController.getDelegatingFuture();
		task.run();
		assertEquals("nBB(ABC)nnBB(ABC)nnBB(ABC)f", getTrace());
		assertTrue(task.future.isSuccessful());
		task = new EncodeTask(session, "ABC");
		task.run();
		assertEquals("nBB(ABC)nnBB(ABC)nnBB(ABC)n", getTrace());

		setCodec(session, createPipeline("SBH"));
		task = new EncodeTask(session, "ABCDEF");
		task.future = session.futuresController.getDelegatingFuture();
		task.run();
		assertEquals("nBH(ABC+DEF+)nnBH(ABC+DEF+)nnBH(ABC+DEF+)f", getTrace());
		assertTrue(task.future.isSuccessful());
		task = new EncodeTask(session, "ABCDEF");
		task.run();
		assertEquals("nBH(ABC+DEF+)nnBH(ABC+DEF+)nnBH(ABC+DEF+)n", getTrace());
		duplicating = 0;
		
		//encoder throwing exception
		encoderException = new Exception("E1");
		setCodec(session, createPipeline("SB"));
		task = new EncodeTask(session, "ABC");
		task.future = session.futuresController.getDelegatingFuture();
		c.getRecordedData(true);
		LoggerRecorder.enableRecording();
		task.run();
		List<String> recording = LoggerRecorder.disableRecording();
		assertTrue(recording.size() > 0);
		assertTrue(recording.get(0).startsWith("[ERROR] " + SessionIncident.ENCODING_PIPELINE_FAILURE.defaultMessage()));
		c.waitForSessionEnding(TIMEOUT);
		s.waitForSessionEnding(TIMEOUT);
		assertEquals("ENCODING_PIPELINE_FAILURE(E1)|EXC|SCL|SEN|", c.getRecordedData(true));
		assertEquals("", getTrace());
		assertTrue(task.future.isFailed());
		assertTrue(task.future.cause() == encoderException);
		setCodec(session, createPipeline("SB"));
		task = new EncodeTask(session, "ABC");
		task.run();
		assertEquals("ENCODING_PIPELINE_FAILURE(E1)|", c.getRecordedData(true));
		assertEquals("", getTrace());
		task = new EncodeTask(session, "ABC");
		c.incident = true;
		LoggerRecorder.enableRecording();
		task.run();
		recording = LoggerRecorder.disableRecording();
		assertEquals(0, recording.size());
		assertEquals("ENCODING_PIPELINE_FAILURE(E1)|", c.getRecordedData(true));
		assertEquals("", getTrace());
	}
	
	class SB implements IEncoder<String,byte[]> {
		@Override public Class<String> getInboundType() {return String.class;}
		@Override public Class<byte[]> getOutboundType() {return byte[].class;}
		@Override public void encode(ISession session, String data, List<byte[]> out) throws Exception {
			if (encoderException != null) {
				throw encoderException;
			}
			if (!discarding) {
				out.add(data.getBytes());
			}
			for (int i=0; i<duplicating; ++i) {
				out.add(data.getBytes());
			}
		}
	}

	class SBB implements IEncoder<String,ByteBuffer> {
		@Override public Class<String> getInboundType() {return String.class;}
		@Override public Class<ByteBuffer> getOutboundType() {return ByteBuffer.class;}
		@Override public void encode(ISession session, String data, List<ByteBuffer> out) {
			if (!discarding) {
				out.add(ByteBuffer.wrap(data.getBytes()));
			}
			for (int i=0; i<duplicating; ++i) {
				out.add(ByteBuffer.wrap(data.getBytes()));
			}
		}
	}

	class SBH implements IEncoder<String,IByteBufferHolder> {
		@Override public Class<String> getInboundType() {return String.class;}
		@Override public Class<IByteBufferHolder> getOutboundType() {return IByteBufferHolder.class;}
		@Override public void encode(ISession session, String data, List<IByteBufferHolder> out) {
			byte[] bytes = data.getBytes();
			if (!discarding) {
				ByteBufferHolder holder = new ByteBufferHolder();
				holder.add(ByteBuffer.wrap(bytes, 0, 3));
				holder.add(ByteBuffer.wrap(bytes, 3, bytes.length-3));
				out.add(holder);
			}
			for (int i=0; i<duplicating; ++i) {
				ByteBufferHolder holder = new ByteBufferHolder();
				holder.add(ByteBuffer.wrap(bytes, 0, 3));
				holder.add(ByteBuffer.wrap(bytes, 3, bytes.length-3));
				out.add(holder);
			}
		}
	}
	
	class BS implements IEncoder<byte[],String> {
		@Override public Class<byte[]> getInboundType() {return byte[].class;}
		@Override public Class<String> getOutboundType() {return String.class;}
		@Override public void encode(ISession session, byte[] data, List<String> out) {
			if (!discarding) {
				out.add(new String(data));
			}
			for (int i=0; i<duplicating; ++i) {
				out.add(new String(data));
			}
		}
	}

	class BBS implements IEncoder<ByteBuffer,String> {
		@Override public Class<ByteBuffer> getInboundType() {return ByteBuffer.class;}
		@Override public Class<String> getOutboundType() {return String.class;}
		@Override public void encode(ISession session, ByteBuffer data, List<String> out) {
			if (!discarding) {
				byte[] b = new byte[data.remaining()];
				data.get(b);
				out.add(new String(b));
			}
			for (int i=0; i<duplicating; ++i) {
				byte[] b = new byte[data.remaining()];
				data.get(b);
				out.add(new String(b));
			}
		}
	}
	
	class BHS implements IEncoder<IByteBufferHolder,String> {
		@Override public Class<IByteBufferHolder> getInboundType() {return IByteBufferHolder.class;}
		@Override public Class<String> getOutboundType() {return String.class;}
		@Override public void encode(ISession session, IByteBufferHolder data, List<String> out) {
			byte[] b = new byte[data.remaining()];
			int off=0,len;
			for (ByteBuffer buffer: data.toArray()) {
				len = buffer.remaining();
				buffer.get(b, off, len);
				off += len;
			}
			if (!discarding) {
				out.add(new String(b));
			}
			for (int i=0; i<duplicating; ++i) {
				out.add(new String(b));
			}
		}
	}
	
	class Writer implements IEncodeTaskWriter {

		@Override
		public IFuture<Void> write(SocketAddress remoteAddress, ByteBuffer buffer, boolean withFuture) {
			if (writerException != null) {
				throw writerException;
			}

			StringBuilder sb = new StringBuilder();
			
			sb.append(remoteAddress == null ? "n" : "a");
			sb.append("BB(");
			byte[] b = new byte[buffer.remaining()];
			buffer.get(b);
			sb.append(new String(b));
			sb.append(")");
			sb.append(withFuture ? "f" : "n");
			trace(sb.toString());
			return withFuture ? new SuccessfulFuture<Void>(null) : null;
		}

		@Override
		public IFuture<Void> write(SocketAddress remoteAddress, byte[] bytes, boolean withFuture) {
			if (writerException != null) {
				throw writerException;
			}
			
			StringBuilder sb = new StringBuilder();
			
			sb.append(remoteAddress == null ? "n" : "a");
			sb.append("B(");
			sb.append(new String(bytes));
			sb.append(")");
			sb.append(withFuture ? "f" : "n");
			trace(sb.toString());
			return withFuture ? new SuccessfulFuture<Void>(null) : null;
		}

		@Override
		public IFuture<Void> write(SocketAddress remoteAddress, IByteBufferHolder holder, boolean withFuture) {
			if (writerException != null) {
				throw writerException;
			}
			
			StringBuilder sb = new StringBuilder();
			
			sb.append(remoteAddress == null ? "n" : "a");
			sb.append("BH(");
			for (ByteBuffer buffer: holder.toArray()) {
				byte[] bytes = new byte[buffer.remaining()];
				buffer.get(bytes);
				sb.append(new String(bytes));
				sb.append('+');
			}
			sb.append(")");
			sb.append(withFuture ? "f" : "n");
			trace(sb.toString());
			return withFuture ? new SuccessfulFuture<Void>(null) : null;
		}
	}
	
	class Task extends EncodeTask {

		Task(InternalSession session, Object msg) {
			super(session, msg);
		}

		@Override
		public void run() {
			trace(msg.toString());
			LockUtils.notify(traceLock);
		}
		
	}
}
