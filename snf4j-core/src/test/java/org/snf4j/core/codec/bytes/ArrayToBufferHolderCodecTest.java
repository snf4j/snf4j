package org.snf4j.core.codec.bytes;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.junit.Test;
import org.snf4j.core.IByteBufferHolder;
import org.snf4j.core.TestSession;

public class ArrayToBufferHolderCodecTest {
	
	@Test
	public void testDecode() throws Exception {
		ArrayToBufferHolderDecoder d = new ArrayToBufferHolderDecoder();
		ArrayList<IByteBufferHolder> out = new ArrayList<IByteBufferHolder>();
		
		assertTrue(d.getInboundType() == byte[].class);
		assertTrue(d.getOutboundType() == IByteBufferHolder.class);
		
		byte[] data = "12345".getBytes();
		byte[] bytes = new byte[5];
		
		d.decode(null, data, out);
		assertEquals(1, out.size());
		IByteBufferHolder h = out.get(0);
		assertEquals(1, h.toArray().length);
		ByteBuffer b = h.toArray()[0];
		assertTrue(data == b.array());
		assertEquals(data.length, b.remaining());
		assertEquals(0, b.position());
		assertEquals(data.length, b.limit());
		b.get(bytes);
		assertArrayEquals("12345".getBytes(), bytes);
		
		TestSession s = new TestSession();
		d = new ArrayToBufferHolderDecoder(true);
		out.clear();
		s.buffer = ByteBuffer.allocate(100);
		d.decode(s, data, out);
		assertEquals(1, out.size());
		h = out.get(0);
		assertEquals(1, h.toArray().length);
		b = h.toArray()[0];
		assertTrue(s.buffer == b);
		assertEquals(5, b.remaining());
		b.get(bytes);
		assertArrayEquals(bytes, data);
		
		d = new ArrayToBufferHolderDecoder(false);
		out.clear();
		d.decode(s, data, out);
		assertEquals(1, out.size());
		h = out.get(0);
		assertEquals(1, h.toArray().length);
		b = h.toArray()[0];
		assertTrue(data == b.array());
		assertEquals(data.length, b.remaining());
		b.get(bytes);
		assertArrayEquals("12345".getBytes(), bytes);
	}

	@Test
	public void testEncode() throws Exception {
		ArrayToBufferHolderEncoder e = new ArrayToBufferHolderEncoder();
		ArrayList<IByteBufferHolder> out = new ArrayList<IByteBufferHolder>();
		
		assertTrue(e.getInboundType() == byte[].class);
		assertTrue(e.getOutboundType() == IByteBufferHolder.class);
		
		byte[] data = "12345".getBytes();
		byte[] bytes = new byte[5];
		
		e.encode(null, data, out);
		assertEquals(1, out.size());
		IByteBufferHolder h = out.get(0);
		assertEquals(1, h.toArray().length);
		ByteBuffer b = h.toArray()[0];
		assertTrue(data == b.array());
		assertEquals(data.length, b.remaining());
		assertEquals(0, b.position());
		assertEquals(data.length, b.limit());
		b.get(bytes);
		assertArrayEquals("12345".getBytes(), bytes);
		
		TestSession s = new TestSession();
		e = new ArrayToBufferHolderEncoder(true);
		out.clear();
		s.buffer = ByteBuffer.allocate(100);
		e.encode(s, data, out);
		assertEquals(1, out.size());
		h = out.get(0);
		assertEquals(1, h.toArray().length);
		b = h.toArray()[0];
		assertTrue(s.buffer == b);
		assertEquals(5, b.remaining());
		b.get(bytes);
		assertArrayEquals(bytes, data);
		
		e = new ArrayToBufferHolderEncoder(false);
		out.clear();
		e.encode(s, data, out);
		assertEquals(1, out.size());
		h = out.get(0);
		assertEquals(1, h.toArray().length);
		b = h.toArray()[0];
		assertTrue(data == b.array());
		assertEquals(data.length, b.remaining());
		b.get(bytes);
		assertArrayEquals("12345".getBytes(), bytes);
	}

}
