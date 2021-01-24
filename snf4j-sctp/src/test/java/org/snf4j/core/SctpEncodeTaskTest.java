package org.snf4j.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.snf4j.core.codec.DefaultCodecExecutor;
import org.snf4j.core.codec.ICodecExecutor;
import org.snf4j.core.codec.IEncoder;
import org.snf4j.core.future.CancelledFuture;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.session.ISession;

import com.sun.nio.sctp.MessageInfo;

public class SctpEncodeTaskTest {

	void assertTask(SctpEncodeTask task, InternalSession session, String bytes, String buffer, Object msg) {
		assertTrue(task.session == session);
		if (bytes != null) {
			assertArrayEquals(bytes.getBytes(), task.bytes);
			assertEquals(task.bytes.length, task.length);
			assertNull(task.buffer);
			assertNull(task.msg);
		}
		if (buffer != null) {
			assertNull(task.bytes);
			assertNotNull(task.buffer);
			ByteBuffer dup = task.buffer.duplicate();
			byte[] data = new byte[dup.remaining()];
			dup.put(data);
			assertArrayEquals(buffer.getBytes(), data);
			assertNull(task.buffer);
			assertTrue(task.msg == msg);
		}
		if (msg != null) {
			assertNull(task.bytes);
			assertNull(task.buffer);
			assertTrue(task.msg == msg);
		}
		assertNull(task.msgInfo);
		assertNull(task.future);
		assertNull(task.remoteAddress);
	}
	
	Selector selector;
	
	SocketChannel sc;
	
	@After
	public void after() throws Exception {
		if (sc != null) {
			sc.close();
		}
		if (selector != null) {
			selector.close();
		}
	}
	
	SctpSession prepareSession(ICodecExecutor codec) throws Exception {
		SctpSession s = new SctpSession("S1", new TestSctpHandler(codec));	
		
		selector = Selector.open();
		sc = SocketChannel.open();
		sc.configureBlocking(false);
		SelectionKey key = sc.register(selector, 0);
		s.key = key;
		SelectorLoop loop = new SelectorLoop();
		s.loop = loop;
		return s;
	}
	
	@Test
	public void testConstructors() {
		SctpSession s = new SctpSession("S1", new TestSctpHandler());
		
		byte[] data = "123456".getBytes();
		SctpEncodeTask task = new SctpEncodeTask(s, data);
		assertTask(task, s, "123456", null, null);
		task = new SctpEncodeTask(s, data, 1, 3);
		assertTask(task, s, "234", null, null);
		ByteBuffer buffer = ByteBuffer.wrap("1122334455".getBytes());
		task = new SctpEncodeTask(s, buffer);
		assertTask(task, s, "1122334455", null, null);
		assertEquals(0, buffer.remaining());
		buffer = ByteBuffer.wrap("1122334455".getBytes());
		task = new SctpEncodeTask(s, buffer, 3);
		assertTask(task, s, "112", null, null);
		assertEquals(7, buffer.remaining());
		Object msg = new Integer(0);
		task = new SctpEncodeTask(s, msg);
		assertTask(task, s, null, null, msg);
	}
	
	@Test
	public void testRegister() throws Exception {
		MessageInfo msgInfo = MessageInfo.createOutgoing(new InetSocketAddress(100), 10);
		SctpSession s = prepareSession(null);	
		
		SctpEncodeTask task = new SctpEncodeTask(s, "1234".getBytes());
		task.registernf(msgInfo);
		assertNull(task.future);
		assertTrue(msgInfo == task.msgInfo);
		
		task = new SctpEncodeTask(s, "1234".getBytes());
		IFuture<Void> f = task.register(msgInfo);
		assertTrue(f == task.future);
		assertTrue(msgInfo == task.msgInfo);
	}
	
	@Test
	public void testWrite() throws Exception {
		MessageInfo msgInfo = MessageInfo.createOutgoing(new InetSocketAddress(100), 10);
		SctpSession s = prepareSession(null);	
		
		SctpEncodeTask task = new SctpEncodeTask(s, "1234".getBytes());
		task.registernf(msgInfo);
		TestWriter w = new TestWriter(s);
		task.setWriter(w);
		byte[] bytes = new byte[10];
		assertNotNull(task.write(bytes, true));
		assertTrue(msgInfo == w.msgInfo);
		assertTrue(bytes == w.bytes);
		
		w = new TestWriter(s);
		task.setWriter(w);
		assertNull(task.write(bytes, false));
		assertTrue(msgInfo == w.msgInfo);
		assertTrue(bytes == w.bytes);

		w = new TestWriter(s);
		task.setWriter(w);
		ByteBuffer buffer = ByteBuffer.allocate(10);
		assertNotNull(task.write(buffer, true));
		assertTrue(msgInfo == w.msgInfo);
		assertTrue(buffer == w.buffer);

		w = new TestWriter(s);
		task.setWriter(w);
		assertNull(task.write(buffer, false));
		assertTrue(msgInfo == w.msgInfo);
		assertTrue(buffer == w.buffer);
	}
	
	@Test
	public void testEncodeMsg() throws Exception {
		MessageInfo msgInfo = MessageInfo.createOutgoing(new InetSocketAddress(100), 10);
		DefaultCodecExecutor codec = new DefaultCodecExecutor();
		codec.getPipeline().add("1", new MsgEncoder());

		SctpSession s = new SctpSession("S1", new TestSctpHandler(codec));	
		SctpEncodeTask task = new SctpEncodeTask(s, "");
		task.msgInfo = msgInfo;
		List<Object> out = task.encode("11223344");
		assertEquals(1, out.size());
		assertArrayEquals("11223344".getBytes(), (byte[])out.get(0));
	}

	@Test
	public void testEncodeBytes() throws Exception {
		MessageInfo msgInfo = MessageInfo.createOutgoing(new InetSocketAddress(100), 10);
		DefaultCodecExecutor codec = new DefaultCodecExecutor();
		codec.getPipeline().add("1", new MsgEncoder());
		codec.getPipeline().add("2", new BytesEncoder());

		SctpSession s = new SctpSession("S1", new TestSctpHandler(codec));	
		SctpEncodeTask task = new SctpEncodeTask(s, "");
		task.msgInfo = msgInfo;
		List<Object> out = task.encode("11223399".getBytes());
		assertEquals(1, out.size());
		assertArrayEquals("11223399".getBytes(), (byte[])out.get(0));
	}
	
	@Test
	public void testEncodeBuffer() throws Exception {
		MessageInfo msgInfo = MessageInfo.createOutgoing(new InetSocketAddress(100), 10);
		DefaultCodecExecutor codec = new DefaultCodecExecutor();
		codec.getPipeline().add("1", new MsgEncoder());
		codec.getPipeline().add("2", new BufferEncoder());

		SctpSession s = new SctpSession("S1", new TestSctpHandler(codec));	
		SctpEncodeTask task = new SctpEncodeTask(s, "");
		task.msgInfo = msgInfo;
		List<Object> out = task.encode(ByteBuffer.wrap("1122339900".getBytes()));
		assertEquals(1, out.size());
		assertArrayEquals("1122339900".getBytes(), (byte[])out.get(0));
	}
	
	@Test
	public void testToString() {
		SctpSession s = new SctpSession("S1", new TestSctpHandler(null));	

		SctpEncodeTask task = new SctpEncodeTask(s, "msg1");
		String prefix = "org.snf4j.core.SctpEncodeTask[session=";
		assertEquals(prefix + "Session-S1 message]", task.toString());
		
		task = new SctpEncodeTask(s, new byte[6]);
		assertEquals(prefix + "Session-S1 length=6]", task.toString());
		task = new SctpEncodeTask(s, ByteBuffer.wrap(new byte[7]));
		assertEquals(prefix + "Session-S1 length=7]", task.toString());
		task.future = s.futuresController.getDelegatingFuture();
		assertEquals(prefix + "Session-S1 length=7 future]", task.toString());
		task.msgInfo = MessageInfo.createOutgoing(new InetSocketAddress(100), 10);
		task.msgInfo.payloadProtocolID(2);
		assertEquals(prefix + "Session-S1 length=7 future stream=10 protocol=2]", task.toString());
		
		
	}
	
	class MsgEncoder implements IEncoder<String, byte[]> {

		@Override
		public Class<String> getInboundType() {
			return String.class;
		}

		@Override
		public Class<byte[]> getOutboundType() {
			return byte[].class;
		}

		@Override
		public void encode(ISession session, String data, List<byte[]> out) throws Exception {
			out.add(data.getBytes());
		}
		
	}
	
	class BytesEncoder implements IEncoder<byte[], String> {

		@Override
		public Class<byte[]> getInboundType() {
			return byte[].class;
		}

		@Override
		public Class<String> getOutboundType() {
			return String.class;
		}

		@Override
		public void encode(ISession session, byte[] data, List<String> out) throws Exception {
			out.add(new String(data));
		}
		
	}
	
	class BufferEncoder implements IEncoder<ByteBuffer, String> {

		@Override
		public Class<ByteBuffer> getInboundType() {
			return ByteBuffer.class;
		}

		@Override
		public Class<String> getOutboundType() {
			return String.class;
		}

		@Override
		public void encode(ISession session, ByteBuffer data, List<String> out) throws Exception {
			byte[] bytes = new byte[data.remaining()];
			data.get(bytes);
			out.add(new String(bytes));
		}
		
	}
	
	class TestWriter implements ISctpEncodeTaskWriter {

		InternalSession session;
		
		ByteBuffer buffer;
		
		byte[] bytes;
		
		MessageInfo msgInfo;
		
		TestWriter(InternalSession session) {
			this.session = session;
		}
		
		@Override
		public IFuture<Void> write(SocketAddress remoteAddress, ByteBuffer buffer, boolean withFuture) {
			return null;
		}

		@Override
		public IFuture<Void> write(SocketAddress remoteAddress, byte[] bytes, boolean withFuture) {
			return null;
		}

		@Override
		public IFuture<Void> write(MessageInfo msgInfo, ByteBuffer buffer, boolean withFuture) {
			this.msgInfo = msgInfo;
			this.buffer = buffer;
			this.bytes = null;
			return withFuture ? new CancelledFuture<Void>(session) : null;
		}

		@Override
		public IFuture<Void> write(MessageInfo msgInfo, byte[] bytes, boolean withFuture) {
			this.msgInfo = msgInfo;
			this.buffer = null;
			this.bytes = bytes;
			return withFuture ? new CancelledFuture<Void>(session) : null;
		}
		
	}
}
