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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.codec.DefaultCodecExecutor;
import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.codec.IEncoder;
import org.snf4j.core.session.ISession;

import com.sun.nio.sctp.MessageInfo;

public class SctpCodecExecutorAdapterTest {

	Exception decodeException;
	
	int decodeNum = 1;
	
	@Before
	public void before() {
		decodeNum = 1;
		decodeException = null;
	}
	
	DefaultCodecExecutor addEncoder(TestSctpHandler h, int streamNumber, int protoID) {
		MessageInfo msgInfo = MessageInfo.createOutgoing(new InetSocketAddress(100), 0);
		msgInfo.streamNumber(streamNumber).payloadProtocolID(protoID);
		DefaultCodecExecutor codec = new DefaultCodecExecutor();
		codec.getPipeline().add("1", new StringEncoder("" + streamNumber +"|" + protoID + "|"));
		h.addCodec(msgInfo, codec);
		return codec;
	}
	
	DefaultCodecExecutor addDecoder(TestSctpHandler h, int streamNumber, int protoID, IDecoder<?,?> decoder) {
		MessageInfo msgInfo = MessageInfo.createOutgoing(new InetSocketAddress(100), 0);
		msgInfo.streamNumber(streamNumber).payloadProtocolID(protoID);
		DefaultCodecExecutor codec = new DefaultCodecExecutor();
		codec.getPipeline().add("1", decoder);
		h.addCodec(msgInfo, codec);
		return codec;
	}
	
	@Test
	public void testGetExecutor() throws Exception {
		MessageInfo msgInfo = MessageInfo.createOutgoing(new InetSocketAddress(100), 0);
		DefaultCodecExecutor codec = new DefaultCodecExecutor();
		codec.getPipeline().add("1", new StringEncoder("D|"));
		TestSctpHandler handler = new TestSctpHandler(codec);
		handler.minStreamNumber = 10;
		handler.maxStreamNumber = 20;
		handler.minProtoID = 100;
		handler.maxProtoID = 200;
		addEncoder(handler, 15, 170);
		addEncoder(handler, 10, 100);
		addEncoder(handler, 20, 200);
		addEncoder(handler, 9, 150);
		addEncoder(handler, 11, 99);
		addEncoder(handler, 21, 199);
		addEncoder(handler, 19, 201);
		addEncoder(handler, 19, 201);
		
		SctpSession s = new SctpSession("S1", handler);	
		SctpEncodeTask task = new SctpEncodeTask(s, "");
		task.msgInfo = ImmutableSctpMessageInfo.wrap(msgInfo);
		msgInfo.streamNumber(9).payloadProtocolID(99);
		List<Object> out = task.encode("...");
		assertEquals("D|...", new String((byte[])out.get(0)));
		msgInfo.streamNumber(9).payloadProtocolID(100);
		out = task.encode("...");
		assertEquals("D|...", new String((byte[])out.get(0)));
		msgInfo.streamNumber(10).payloadProtocolID(99);
		out = task.encode("...");
		assertEquals("D|...", new String((byte[])out.get(0)));
		
		msgInfo.streamNumber(21).payloadProtocolID(201);
		out = task.encode("...");
		assertEquals("D|...", new String((byte[])out.get(0)));
		msgInfo.streamNumber(20).payloadProtocolID(201);
		out = task.encode("...");
		assertEquals("D|...", new String((byte[])out.get(0)));
		msgInfo.streamNumber(21).payloadProtocolID(200);
		out = task.encode("...");
		assertEquals("D|...", new String((byte[])out.get(0)));

		msgInfo.streamNumber(20).payloadProtocolID(200);
		out = task.encode("...");
		assertEquals("20|200|...", new String((byte[])out.get(0)));
		msgInfo.streamNumber(10).payloadProtocolID(100);
		out = task.encode("...");
		assertEquals("10|100|...", new String((byte[])out.get(0)));
		msgInfo.streamNumber(15).payloadProtocolID(170);
		out = task.encode("...");
		assertEquals("15|170|...", new String((byte[])out.get(0)));
		msgInfo.streamNumber(15).payloadProtocolID(170);
		out = task.encode("...");
		assertEquals("15|170|...", new String((byte[])out.get(0)));

		msgInfo.streamNumber(18).payloadProtocolID(189);
		out = task.encode("...");
		assertNull(out);
		msgInfo.streamNumber(18).payloadProtocolID(189);
		out = task.encode("...");
		assertNull(out);
		
		codec = new DefaultCodecExecutor();
		codec.getPipeline().add("1", new StringEncoder("D|"));
		handler = new TestSctpHandler(codec);
		handler.minStreamNumber = 0;
		handler.maxStreamNumber = 65536;
		handler.minProtoID = Integer.MIN_VALUE;
		handler.maxProtoID = Integer.MAX_VALUE;
		addEncoder(handler, 0, Integer.MIN_VALUE);
		addEncoder(handler, 65536, Integer.MIN_VALUE);
		addEncoder(handler, 0, Integer.MAX_VALUE);
		addEncoder(handler, 65536, Integer.MAX_VALUE);
		
		s = new SctpSession("S1", handler);
		task = new SctpEncodeTask(s, "");
		task.msgInfo = ImmutableSctpMessageInfo.wrap(msgInfo);
		msgInfo.streamNumber(0).payloadProtocolID(Integer.MIN_VALUE);
		out = task.encode("...");
		assertEquals("0|-2147483648|...", new String((byte[])out.get(0)));
		msgInfo.streamNumber(65536).payloadProtocolID(Integer.MIN_VALUE);
		out = task.encode("...");
		assertEquals("65536|-2147483648|...", new String((byte[])out.get(0)));
		msgInfo.streamNumber(0).payloadProtocolID(Integer.MAX_VALUE);
		out = task.encode("...");
		assertEquals("0|2147483647|...", new String((byte[])out.get(0)));
		msgInfo.streamNumber(65536).payloadProtocolID(Integer.MAX_VALUE);
		out = task.encode("...");
		assertEquals("65536|2147483647|...", new String((byte[])out.get(0)));

		msgInfo.streamNumber(1).payloadProtocolID(Integer.MIN_VALUE);
		assertNull(task.encode("..."));
		msgInfo.streamNumber(0).payloadProtocolID(Integer.MIN_VALUE+1);
		assertNull(task.encode("..."));
		msgInfo.streamNumber(65536-1).payloadProtocolID(Integer.MIN_VALUE);
		assertNull(task.encode("..."));
		msgInfo.streamNumber(65536).payloadProtocolID(Integer.MIN_VALUE+1);
		assertNull(task.encode("..."));
		msgInfo.streamNumber(1).payloadProtocolID(Integer.MAX_VALUE);
		assertNull(task.encode("..."));
		msgInfo.streamNumber(0).payloadProtocolID(Integer.MAX_VALUE-1);
		assertNull(task.encode("..."));
		msgInfo.streamNumber(65536-1).payloadProtocolID(Integer.MAX_VALUE);
		assertNull(task.encode("..."));
		msgInfo.streamNumber(65536).payloadProtocolID(Integer.MAX_VALUE-1);
		assertNull(task.encode("..."));
	}
	
	@Test
	public void testEncodeBytes() throws Exception {
		ImmutableSctpMessageInfo msgInfo = ImmutableSctpMessageInfo.create(new InetSocketAddress(100), 15, 170);
		DefaultCodecExecutor codec = new DefaultCodecExecutor();
		codec.getPipeline().add("1", new StringEncoder("D|"));
		TestSctpHandler handler = new TestSctpHandler(codec);
		handler.minStreamNumber = 10;
		handler.maxStreamNumber = 20;
		handler.minProtoID = 100;
		handler.maxProtoID = 200;
		addEncoder(handler, 15, 170).getPipeline().add("2", new BytesEncoder());
		
		SctpSession s = new SctpSession("S1", handler);	
		SctpEncodeTask task = new SctpEncodeTask(s, "");
		task.msgInfo = msgInfo;
		List<Object> out = task.encode("...".getBytes());
		assertEquals("15|170|...b", new String((byte[])out.get(0)));
	}

	@Test
	public void testEncodeBuffer() throws Exception {
		ImmutableSctpMessageInfo msgInfo = ImmutableSctpMessageInfo.create(new InetSocketAddress(100), 15, 170);
		DefaultCodecExecutor codec = new DefaultCodecExecutor();
		codec.getPipeline().add("1", new StringEncoder("D|"));
		TestSctpHandler handler = new TestSctpHandler(codec);
		handler.minStreamNumber = 10;
		handler.maxStreamNumber = 20;
		handler.minProtoID = 100;
		handler.maxProtoID = 200;
		addEncoder(handler, 15, 170).getPipeline().add("2", new BufferEncoder());
		
		SctpSession s = new SctpSession("S1", handler);	
		SctpEncodeTask task = new SctpEncodeTask(s, "");
		task.msgInfo = msgInfo;
		List<Object> out = task.encode(ByteBuffer.wrap("...".getBytes()));
		assertEquals("15|170|...bb", new String((byte[])out.get(0)));
	}
	
	@Test
	public void testRead() throws Exception {
		MessageInfo msgInfo = MessageInfo.createOutgoing(new InetSocketAddress(100), 0);
		DefaultCodecExecutor codec = new DefaultCodecExecutor();
		codec.getPipeline().add("1", new BytesDecoder('A'));
		TestSctpHandler handler = new TestSctpHandler(codec);
		addDecoder(handler, 15, 170, new BytesDecoder('B'));
		handler.minStreamNumber = 10;
		handler.maxStreamNumber = 20;
		handler.minProtoID = 100;
		handler.maxProtoID = 200;
		SctpSession s = new SctpSession("S1", handler);	
		
		SctpCodecExecutorAdapter adapter = (SctpCodecExecutorAdapter) s.codec;
		
		msgInfo.streamNumber(5).payloadProtocolID(170);
		adapter.read("...".getBytes(), msgInfo);
		assertEquals("B|A..|5|170|", handler.getTrace());
		adapter.read(ByteBuffer.wrap("...".getBytes()), msgInfo);
		assertEquals("B|A..|5|170|", handler.getTrace());
		
		msgInfo.streamNumber(15).payloadProtocolID(170);
		adapter.read(ByteBuffer.wrap("...".getBytes()), msgInfo);
		assertEquals("B|B..|15|170|", handler.getTrace());
		adapter.read("...".getBytes(), msgInfo);
		assertEquals("B|B..|15|170|", handler.getTrace());
		
		msgInfo.streamNumber(16).payloadProtocolID(170);
		adapter.read("...".getBytes(), msgInfo);
		assertEquals("B|...|16|170|", handler.getTrace());
		adapter.read(ByteBuffer.wrap("...".getBytes()), msgInfo);
		assertEquals("BB|...|16|170|", handler.getTrace());
		
		decodeException = new Exception("E");
		msgInfo.streamNumber(15).payloadProtocolID(170);
		try {
			adapter.read("...".getBytes(), msgInfo);
			fail();
		}
		catch (PipelineDecodeException e) {
			assertTrue(((PipelineDecodeException)e).getCause() == decodeException);
		}
		try {
			adapter.read(ByteBuffer.wrap("...".getBytes()), msgInfo);
			fail();
		}
		catch (PipelineDecodeException e) {
			assertTrue(((PipelineDecodeException)e).getCause() == decodeException);
		}
	}
	
	@Test
	public void testReadBytesOutput() throws Exception {
		MessageInfo msgInfo = MessageInfo.createOutgoing(new InetSocketAddress(100), 0);
		DefaultCodecExecutor codec = new DefaultCodecExecutor();
		codec.getPipeline().add("1", new BytesDecoder('A'));
		TestSctpHandler handler = new TestSctpHandler(codec);
		handler.minStreamNumber = 10;
		handler.maxStreamNumber = 20;
		handler.minProtoID = 100;
		handler.maxProtoID = 200;
		SctpSession s = new SctpSession("S1", handler);	
		
		SctpCodecExecutorAdapter adapter = (SctpCodecExecutorAdapter) s.codec;
		
		adapter.read("...".getBytes(), msgInfo);
		assertEquals("B|A..|0|0|", handler.getTrace());
		decodeNum = 0;
		adapter.read("...".getBytes(), msgInfo);
		assertEquals("", handler.getTrace());
		decodeNum = 2;
		adapter.read("...".getBytes(), msgInfo);
		assertEquals("B|A..|0|0|B|AA.|0|0|", handler.getTrace());
	}
	
	@Test
	public void testReadBufferOutput() throws Exception {
		MessageInfo msgInfo = MessageInfo.createOutgoing(new InetSocketAddress(100), 0);
		DefaultCodecExecutor codec = new DefaultCodecExecutor();
		codec.getPipeline().add("1", new BufferDecoder());
		TestSctpHandler handler = new TestSctpHandler(codec);
		handler.minStreamNumber = 10;
		handler.maxStreamNumber = 20;
		handler.minProtoID = 100;
		handler.maxProtoID = 200;
		SctpSession s = new SctpSession("S1", handler);	
		
		SctpCodecExecutorAdapter adapter = (SctpCodecExecutorAdapter) s.codec;
		
		adapter.read("...".getBytes(), msgInfo);
		assertEquals("BB|X..|0|0|", handler.getTrace());
		decodeNum = 2;
		adapter.read("...".getBytes(), msgInfo);
		assertEquals("BB|X..|0|0|BB|XX.|0|0|", handler.getTrace());
		
	}
	
	@Test
	public void testReadStringOutput() throws Exception {
		MessageInfo msgInfo = MessageInfo.createOutgoing(new InetSocketAddress(100), 7);
		DefaultCodecExecutor codec = new DefaultCodecExecutor();
		codec.getPipeline().add("1", new StringDecoder());
		TestSctpHandler handler = new TestSctpHandler(codec);
		handler.minStreamNumber = 10;
		handler.maxStreamNumber = 20;
		handler.minProtoID = 100;
		handler.maxProtoID = 200;
		SctpSession s = new SctpSession("S1", handler);	
		
		SctpCodecExecutorAdapter adapter = (SctpCodecExecutorAdapter) s.codec;
		
		adapter.read("...".getBytes(), msgInfo);
		assertEquals("M|X..|7|0|", handler.getTrace());
		decodeNum = 3;
		adapter.read("....".getBytes(), msgInfo);
		assertEquals("M|X...|7|0|M|XX..|7|0|M|XXX.|7|0|", handler.getTrace());
		
	}
	
	class StringDecoder implements IDecoder<byte[], String> {

		@Override
		public Class<byte[]> getInboundType() {
			return byte[].class;
		}

		@Override
		public Class<String> getOutboundType() {
			return String.class;
		}

		@Override
		public void decode(ISession session, byte[] data, List<String> out) throws Exception {
			data[0] = (byte) 'X';
			if (decodeNum > 0) {
				for (int i=0; i<decodeNum; ++i) {
					out.add(new String(data));
					data = data.clone();
					data[i+1] = (byte) 'X';
				}
			}
		}
		
	}
	
	class BufferDecoder implements IDecoder<byte[], ByteBuffer> {

		@Override
		public Class<byte[]> getInboundType() {
			return byte[].class;
		}

		@Override
		public Class<ByteBuffer> getOutboundType() {
			return ByteBuffer.class;
		}

		@Override
		public void decode(ISession session, byte[] data, List<ByteBuffer> out) throws Exception {
			data[0] = (byte) 'X';
			if (decodeNum > 0) {
				for (int i=0; i<decodeNum; ++i) {
					out.add(ByteBuffer.wrap(data));
					data = data.clone();
					data[i+1] = (byte) 'X';
				}
			}
		}
	}
	
	class BytesDecoder implements IDecoder<byte[], byte[]> {

		char character;
		
		BytesDecoder(char character) {
			this.character = character;
		}
		
		@Override
		public Class<byte[]> getInboundType() {
			return byte[].class;
		}

		@Override
		public Class<byte[]> getOutboundType() {
			return byte[].class;
		}

		@Override
		public void decode(ISession session, byte[] data, List<byte[]> out) throws Exception {
			if (decodeException != null) {
				throw decodeException;
			}
			data[0] = (byte) character;
			if (decodeNum > 0) {
				for (int i=0; i<decodeNum; ++i) {
					out.add(data);
					data = data.clone();
					data[i+1] = (byte) character;
				}
			}
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
			out.add(new String(bytes)+"bb");
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
			out.add(new String(data)+"b");
		}		
	}
	
	class StringEncoder implements IEncoder<String, byte[]> {

		String prefix;
		
		StringEncoder(String prefix) {
			this.prefix = prefix;
		}
		
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
			out.add((prefix + data).getBytes());
		}
	}
}
