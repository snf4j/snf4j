package org.snf4j.core.codec.bytes;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.junit.Test;
import org.snf4j.core.ByteBufferHolder;
import org.snf4j.core.IByteBufferHolder;
import org.snf4j.core.SingleByteBufferHolder;
import org.snf4j.core.TestSession;

public class BufferHolderToArrayCodecTest {

	@Test
	public void testToArray() {
		byte[] data = "12345".getBytes();
		byte[] bytes;
		
		bytes = BufferHolderToArrayCodec.toArray(new SingleByteBufferHolder(ByteBuffer.wrap(data)));
		assertSame(data, bytes);
		assertArrayEquals("12345".getBytes(), bytes);
		
		bytes = BufferHolderToArrayCodec.toArray(new ByteBufferHolder());
		assertEquals(0, bytes.length);
		
		byte[] data2 = "567".getBytes();
		ByteBufferHolder holder = new ByteBufferHolder();
		holder.add(ByteBuffer.wrap(data));
		holder.add(ByteBuffer.wrap(data2));
		bytes = BufferHolderToArrayCodec.toArray(holder);
		assertArrayEquals("12345567".getBytes(), bytes);
	}
	
	@Test
	public void testDecode() throws Exception {
		BufferHolderToArrayDecoder d = new BufferHolderToArrayDecoder();
		byte[] data = "12345".getBytes();
		byte[] bytes;
		ArrayList<byte[]> out = new ArrayList<byte[]>();
		ByteBuffer b1,b2;
		
		assertTrue(d.getInboundType() == IByteBufferHolder.class);
		assertTrue(d.getOutboundType() == byte[].class);
		
		for (int i=0; i<2; ++i) {
			d.decode(null, new SingleByteBufferHolder(ByteBuffer.wrap(data)), out);
			assertEquals(1, out.size());
			assertSame(data, out.get(0));
			assertArrayEquals("12345".getBytes(), out.get(0));
			out.clear();

			ByteBufferHolder holder = new ByteBufferHolder();
			b1 = ByteBuffer.wrap(data);
			b2 = ByteBuffer.wrap("67".getBytes());
			holder.add(b1);
			holder.add(b2);
			d.decode(null, holder, out);
			assertEquals(1, out.size());
			assertArrayEquals("1234567".getBytes(), out.get(0));
			out.clear();
			
			d = new BufferHolderToArrayDecoder(false);
		}
		
		TestSession s = new TestSession();
		d = new BufferHolderToArrayDecoder(true);
		b1 = ByteBuffer.wrap(data);
		d.decode(s, new SingleByteBufferHolder(b1), out);
		assertEquals(1, out.size());
		bytes = out.get(0);
		assertNotSame(data, bytes);
		assertArrayEquals("12345".getBytes(), bytes);
		assertEquals(1, s.released.size());
		assertSame(b1, s.released.get(0));
		
		out.clear();
		s = new TestSession();
		ByteBufferHolder holder = new ByteBufferHolder();
		b1 = ByteBuffer.wrap(data);
		b2 = ByteBuffer.wrap("67".getBytes());
		holder.add(b1);
		holder.add(b2);
		d.decode(s, holder, out);
		assertEquals(1, out.size());
		bytes = out.get(0);
		assertNotSame(data, bytes);
		assertArrayEquals("1234567".getBytes(), bytes);
		assertEquals(2, s.released.size());
		assertSame(b1, s.released.get(0));
		assertSame(b2, s.released.get(1));

	}

	@Test
	public void testEncode() throws Exception {
		BufferHolderToArrayEncoder e = new BufferHolderToArrayEncoder();
		byte[] data = "12345".getBytes();
		byte[] bytes;
		ArrayList<byte[]> out = new ArrayList<byte[]>();
		ByteBuffer b1,b2;
		
		assertTrue(e.getInboundType() == IByteBufferHolder.class);
		assertTrue(e.getOutboundType() == byte[].class);
		
		for (int i=0; i<2; ++i) {
			e.encode(null, new SingleByteBufferHolder(ByteBuffer.wrap(data)), out);
			assertEquals(1, out.size());
			assertSame(data, out.get(0));
			assertArrayEquals("12345".getBytes(), out.get(0));
			out.clear();

			ByteBufferHolder holder = new ByteBufferHolder();
			b1 = ByteBuffer.wrap(data);
			b2 = ByteBuffer.wrap("67".getBytes());
			holder.add(b1);
			holder.add(b2);
			e.encode(null, holder, out);
			assertEquals(1, out.size());
			assertArrayEquals("1234567".getBytes(), out.get(0));
			out.clear();
			
			e = new BufferHolderToArrayEncoder(false);
		}
		
		TestSession s = new TestSession();
		e = new BufferHolderToArrayEncoder(true);
		b1 = ByteBuffer.wrap(data);
		e.encode(s, new SingleByteBufferHolder(b1), out);
		assertEquals(1, out.size());
		bytes = out.get(0);
		assertNotSame(data, bytes);
		assertArrayEquals("12345".getBytes(), bytes);
		assertEquals(1, s.released.size());
		assertSame(b1, s.released.get(0));
		
		out.clear();
		s = new TestSession();
		ByteBufferHolder holder = new ByteBufferHolder();
		b1 = ByteBuffer.wrap(data);
		b2 = ByteBuffer.wrap("67".getBytes());
		holder.add(b1);
		holder.add(b2);
		e.encode(s, holder, out);
		assertEquals(1, out.size());
		bytes = out.get(0);
		assertNotSame(data, bytes);
		assertArrayEquals("1234567".getBytes(), bytes);
		assertEquals(2, s.released.size());
		assertSame(b1, s.released.get(0));
		assertSame(b2, s.released.get(1));

	}

}
