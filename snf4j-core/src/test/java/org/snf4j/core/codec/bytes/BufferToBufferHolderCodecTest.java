package org.snf4j.core.codec.bytes;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.junit.Test;
import org.snf4j.core.IByteBufferHolder;

public class BufferToBufferHolderCodecTest {
	
	@Test
	public void testDecode() throws Exception {
		BufferToBufferHolderDecoder d = new BufferToBufferHolderDecoder();
		ArrayList<IByteBufferHolder> out = new ArrayList<IByteBufferHolder>();
		
		assertTrue(d.getInboundType() == ByteBuffer.class);
		assertTrue(d.getOutboundType() == IByteBufferHolder.class);
		
		byte[] data = "12345".getBytes();
		byte[] bytes = new byte[5];
		ByteBuffer buf = ByteBuffer.wrap(data);
		
		d.decode(null, buf, out);
		assertEquals(1, out.size());
		IByteBufferHolder h = out.get(0);
		assertEquals(5, h.remaining());
		assertEquals(1, h.toArray().length);
		ByteBuffer b = h.toArray()[0];
		assertSame(buf, b);
		assertEquals(5, b.remaining());
		b.get(bytes);
		assertArrayEquals(data, bytes);
	}

	@Test
	public void testEncode() throws Exception {
		BufferToBufferHolderEncoder e = new BufferToBufferHolderEncoder();
		ArrayList<IByteBufferHolder> out = new ArrayList<IByteBufferHolder>();
		
		assertTrue(e.getInboundType() == ByteBuffer.class);
		assertTrue(e.getOutboundType() == IByteBufferHolder.class);
		
		byte[] data = "12345".getBytes();
		byte[] bytes = new byte[5];
		ByteBuffer buf = ByteBuffer.wrap(data);
		
		e.encode(null, buf, out);
		assertEquals(1, out.size());
		IByteBufferHolder h = out.get(0);
		assertEquals(5, h.remaining());
		assertEquals(1, h.toArray().length);
		ByteBuffer b = h.toArray()[0];
		assertSame(buf, b);
		assertEquals(5, b.remaining());
		b.get(bytes);
		assertArrayEquals(data, bytes);
	}
}
