package org.snf4j.core.codec.bytes;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.junit.Test;
import org.snf4j.core.ByteBufferHolder;
import org.snf4j.core.IByteBufferHolder;
import org.snf4j.core.SingleByteBufferHolder;
import org.snf4j.core.TestSession;

public class BufferHolderToBufferCodecTest {

	@Test
	public void testDecode() throws Exception {
		BufferHolderToBufferDecoder d = new BufferHolderToBufferDecoder();
		ArrayList<ByteBuffer> out = new ArrayList<ByteBuffer>();
		byte[] data = "12345".getBytes();
		byte[] bytes1 = new byte[5];
		byte[] bytes2 = new byte[7];
		ByteBuffer buf,b1,b2;
		
		assertTrue(d.getInboundType() == IByteBufferHolder.class);
		assertTrue(d.getOutboundType() == ByteBuffer.class);
		
		b1 = ByteBuffer.wrap(data);
		TestSession s = new TestSession();
		d.decode(s, new SingleByteBufferHolder(b1), out);
		assertEquals(1, out.size());
		buf = out.get(0);
		assertSame(b1, buf);
		assertEquals(5, buf.remaining());
		buf.get(bytes1);
		assertArrayEquals("12345".getBytes(), bytes1);
		assertNull(s.buffer);
		
		b1 = ByteBuffer.wrap(data);
		b2 = ByteBuffer.wrap("67".getBytes());
		ByteBufferHolder h = new ByteBufferHolder();
		h.add(b1);
		h.add(b2);
		s = new TestSession();
		buf = ByteBuffer.allocate(7);
		s.buffer = buf;
		out.clear();
		d.decode(s, h, out);
		assertEquals(1, out.size());
		assertSame(buf, out.get(0));
		assertEquals(7, buf.remaining());
		assertEquals(2, s.released.size());
		assertSame(b1, s.released.get(0));
		assertSame(b2, s.released.get(1));
		buf.get(bytes2);
		assertArrayEquals("1234567".getBytes(), bytes2);	
	}
	
	@Test
	public void testEncode() throws Exception {
		BufferHolderToBufferEncoder e = new BufferHolderToBufferEncoder();
		ArrayList<ByteBuffer> out = new ArrayList<ByteBuffer>();
		byte[] data = "12345".getBytes();
		byte[] bytes1 = new byte[5];
		byte[] bytes2 = new byte[7];
		ByteBuffer buf,b1,b2;
		
		assertTrue(e.getInboundType() == IByteBufferHolder.class);
		assertTrue(e.getOutboundType() == ByteBuffer.class);
		
		b1 = ByteBuffer.wrap(data);
		TestSession s = new TestSession();
		e.encode(s, new SingleByteBufferHolder(b1), out);
		assertEquals(1, out.size());
		buf = out.get(0);
		assertSame(b1, buf);
		assertEquals(5, buf.remaining());
		buf.get(bytes1);
		assertArrayEquals("12345".getBytes(), bytes1);
		assertNull(s.buffer);
		
		b1 = ByteBuffer.wrap(data);
		b2 = ByteBuffer.wrap("67".getBytes());
		ByteBufferHolder h = new ByteBufferHolder();
		h.add(b1);
		h.add(b2);
		s = new TestSession();
		buf = ByteBuffer.allocate(7);
		s.buffer = buf;
		out.clear();
		e.encode(s, h, out);
		assertEquals(1, out.size());
		assertSame(buf, out.get(0));
		assertEquals(7, buf.remaining());
		assertEquals(2, s.released.size());
		assertSame(b1, s.released.get(0));
		assertSame(b2, s.released.get(1));
		buf.get(bytes2);
		assertArrayEquals("1234567".getBytes(), bytes2);	
	}
	
}
