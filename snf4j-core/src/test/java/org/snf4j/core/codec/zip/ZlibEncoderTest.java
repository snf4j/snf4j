/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020 SNF4J contributors
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
package org.snf4j.core.codec.zip;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.zip.Deflater;

import org.junit.Test;
import org.snf4j.core.session.ISession;

public class ZlibEncoderTest extends EncoderTest {
	
	@Test
	public void testConstructor() throws Exception {
		ZlibEncoder e = new ZlibEncoder();
		assertTrue(e.getInboundType() == byte[].class);
		assertTrue(e.getOutboundType() == ByteBuffer.class);
		
		byte[] data = "1234567890".getBytes();
		Deflater deflater = getDeflater(e);
		Deflater expected = new Deflater(6);
		byte[] dbuf = new byte[100];
		byte[] ebuf = new byte[100];
		deflater.setInput(data);
		expected.setInput(data);
		int elen = expected.deflate(ebuf,0, 2, Deflater.SYNC_FLUSH);
		int dlen = deflater.deflate(dbuf,0, 2, Deflater.SYNC_FLUSH);
		assertEquals(elen, dlen);
		assertArrayEquals(ebuf, dbuf);
		
		e = new ZlibEncoder(1);
		deflater = getDeflater(e);
		expected = new Deflater(1);
		deflater.setInput(data);
		expected.setInput(data);
		elen = expected.deflate(ebuf,0, 2, Deflater.SYNC_FLUSH);
		dlen = deflater.deflate(dbuf,0, 2, Deflater.SYNC_FLUSH);
		assertEquals(elen, dlen);
		assertArrayEquals(ebuf, dbuf);
		
		e = new ZlibEncoder(1, ZlibCodec.Mode.ZLIB);
		deflater = getDeflater(e);
		deflater.setInput(data);
		dlen = deflater.deflate(dbuf,0, 2, Deflater.SYNC_FLUSH);
		assertEquals(elen, dlen);
		assertArrayEquals(ebuf, dbuf);

		e = new ZlibEncoder(1, ZlibCodec.Mode.AUTO);
		deflater = getDeflater(e);
		deflater.setInput(data);
		dlen = deflater.deflate(dbuf,0, 2, Deflater.SYNC_FLUSH);
		assertEquals(elen, dlen);
		assertArrayEquals(ebuf, dbuf);

		e = new ZlibEncoder(1, ZlibCodec.Mode.RAW);
		deflater = getDeflater(e);
		expected = new Deflater(1,true);
		deflater.setInput(data);
		expected.setInput(data);
		elen = expected.deflate(ebuf,0, 2, Deflater.SYNC_FLUSH);
		dlen = deflater.deflate(dbuf,0, 2, Deflater.SYNC_FLUSH);
		assertEquals(elen, dlen);
		assertArrayEquals(ebuf, dbuf);
		
		byte[] dict = new byte[1];
		e = new ZlibEncoder(1, dict);
		deflater = getDeflater(e);
		expected = new Deflater(1);
		expected.setDictionary(dict);
		deflater.setInput(data);
		expected.setInput(data);
		elen = expected.deflate(ebuf,0, 2, Deflater.SYNC_FLUSH);
		dlen = deflater.deflate(dbuf,0, 2, Deflater.SYNC_FLUSH);
		assertEquals(elen, dlen);
		assertArrayEquals(ebuf, dbuf);

		e = new ZlibEncoder(dict);
		deflater = getDeflater(e);
		expected = new Deflater(6);
		expected.setDictionary(dict);
		deflater.setInput(data);
		expected.setInput(data);
		elen = expected.deflate(ebuf,0, 2, Deflater.SYNC_FLUSH);
		dlen = deflater.deflate(dbuf,0, 2, Deflater.SYNC_FLUSH);
		assertEquals(elen, dlen);
		assertArrayEquals(ebuf, dbuf);
		
		e = new ZlibEncoder(1, (byte[])null);
		deflater = getDeflater(e);
		expected = new Deflater(1);
		deflater.setInput(data);
		expected.setInput(data);
		elen = expected.deflate(ebuf,0, 2, Deflater.SYNC_FLUSH);
		dlen = deflater.deflate(dbuf,0, 2, Deflater.SYNC_FLUSH);
		assertEquals(elen, dlen);
		assertArrayEquals(ebuf, dbuf);
		
		e = new ZlibEncoder((byte[])null);
		deflater = getDeflater(e);
		expected = new Deflater(6);
		deflater.setInput(data);
		expected.setInput(data);
		elen = expected.deflate(ebuf,0, 2, Deflater.SYNC_FLUSH);
		dlen = deflater.deflate(dbuf,0, 2, Deflater.SYNC_FLUSH);
		assertEquals(elen, dlen);
		assertArrayEquals(ebuf, dbuf);
		
		e = new ZlibEncoder(1, dict, ZlibCodec.Mode.RAW);
		deflater = getDeflater(e);
		expected = new Deflater(1,true);
		expected.setDictionary(dict);
		deflater.setInput(data);
		expected.setInput(data);
		elen = expected.deflate(ebuf,0, 2, Deflater.SYNC_FLUSH);
		dlen = deflater.deflate(dbuf,0, 2, Deflater.SYNC_FLUSH);
		assertEquals(elen, dlen);
		assertArrayEquals(ebuf, dbuf);
	
		for (int i=0; i<10; i++) {
			e = new ZlibEncoder(i);
		}
		try {
			e = new ZlibEncoder(-1); fail();
		}
		catch (IllegalArgumentException ex) {
		}
		try {
			e = new ZlibEncoder(10); fail();
		}
		catch (IllegalArgumentException ex) {
		}
		try {
			e = new ZlibEncoder(6, new byte[0], null); fail();
		}
		catch (NullPointerException ex) {
		}
	}
	
	@Test
	public void testFinish() throws Exception {
		
		//no data deflated
		TestZlibEncoder e = new TestZlibEncoder();
		e.finish();
		assertFalse(e.isFinished());
		e.encode(null, new byte[0], out);
		assertTrue(e.isFinished());
		assertDeflate("", 1, true, false);
		e.encode(null, "ew".getBytes(), out);
		assertEquals(2, out.size());
		assertEquals("ew", new String(out.get(1).array()));
		out.clear();
		e = new TestZlibEncoder(ZlibCodec.Mode.RAW);
		e.finish();
		assertFalse(e.isFinished());
		e.encode(null, new byte[0], out);
		assertTrue(e.isFinished());
		assertDeflate("", 1, true, true);
		e.encode(null, "ew".getBytes(), out);
		assertEquals(2, out.size());
		assertEquals("ew", new String(out.get(1).array()));
		out.clear();
		
		//data deflated
		e = new TestZlibEncoder();
		e.encode(null, "1234".getBytes(), out);
		assertDeflate("1234", 1, false, false);
		e.finish();
		assertFalse(e.isFinished());
		e.encode(null, "567".getBytes(), out);
		assertDeflate("1234", 3, true, false);
		assertTrue(e.isFinished());
		assertEquals("567", new String(out.get(2).array()));
		out.clear();
		e = new TestZlibEncoder(ZlibCodec.Mode.RAW);
		e.encode(null, "1234".getBytes(), out);
		assertDeflate("1234", 1, false, true);
		e.finish();
		assertFalse(e.isFinished());
		e.encode(null, "567".getBytes(), out);
		assertDeflate("1234", 3, true, true);
		assertTrue(e.isFinished());
		assertEquals("567", new String(out.get(2).array()));
		out.clear();
		
		//bound to short
		e = new TestZlibEncoder();
		e.deflateBound = 2;
		e.finish();
		assertFalse(e.isFinished());
		e.encode(null, new byte[0], out);
		assertTrue(e.isFinished());
		assertDeflate("", 2, true, false);
		e.encode(null, "ew".getBytes(), out);
		assertEquals(3, out.size());
		assertEquals("ew", new String(out.get(2).array()));
		out.clear();	
	}
	
	@Test
	public void testEncode() throws Exception {
		TestZlibEncoder e = new TestZlibEncoder();
		
		e.encode(null, new byte[0], out);
		assertEquals(0, out.size());
		assertEquals(0, e.preDeflateCount);
		
		e.encode(null, "a".getBytes(), out);
		assertDeflate("a", 1);
		out.clear();
		
		e = new TestZlibEncoder();
		e.encode(null, "abcdefghij".getBytes(), out);
		assertDeflate("abcdefghij", 1);
		out.clear();
		
		e = new TestZlibEncoder();
		e.encode(null, "123456789".getBytes(), out);
		e.encode(null, "abcdefghi".getBytes(), out);
		assertDeflate("123456789abcdefghi", 2);
		out.clear();
		
		String inData = "1234567890abcdefghijklmnop";
		e = new TestZlibEncoder();
		e.encode(null, inData.getBytes(), out);
		assertDeflate(inData, 1);
		int limit = out.get(0).limit();
		out.clear();
		
		e = new TestZlibEncoder();
		e.deflateBound = limit-2;
		e.encode(null, inData.getBytes(), out);
		assertDeflate(inData, 2);
		out.clear();
	
		e = new TestZlibEncoder();
		e.deflateBound = limit;
		e.encode(null, inData.getBytes(), out);
		assertDeflate(inData, 2);
		out.clear();
		
		e = new TestZlibEncoder();
		e.deflateBound = 0;
		e.encode(null, inData.getBytes(), out);
		assertDeflate(inData, out.size());
		
	}

	static int expectedBound(int len) {
		return len + ((len + 7) >> 3) + ((len + 63) >> 6) + 5 + 10;
	}
	
	@Test
	public void testDeflateBound() throws Exception {
		TestZlibEncoder e = new TestZlibEncoder();
		e.encode(null, "1".getBytes(), out);
		assertTrue(expectedBound(1) == e.boundOut);
		
		e = new TestZlibEncoder();
		e.encode(null, new byte[1000], out);
		assertTrue(expectedBound(1000) == e.boundOut);
		
	}
	
	@Test
	public void testDeflateWithDictionary() throws Exception {
		byte[] dic = "abcd".getBytes();
		ZlibEncoder e = new ZlibEncoder(6, dic);
		e.encode(null, "abcdabcd".getBytes(), out);
		dictionary = dic;
		assertDeflate("abcdabcd", 1);
		out.clear();
		
		e = new ZlibEncoder(6, dic, ZlibCodec.Mode.ZLIB);
		e.encode(null, "abcdabcd".getBytes(), out);
		dictionary = dic;
		assertDeflate("abcdabcd", 1);
	}
	
	static class TestZlibEncoder extends ZlibEncoder {
		
		int preDeflateCount;
		int preFinishCount;
		int postFinishCount;
		int boundIn;
		int boundOut;
		int deflateBound = -1;
		
		TestZlibEncoder() {
			super(6, ZlibCodec.Mode.ZLIB);
		}
		
		TestZlibEncoder(Mode mode) {
			super(6, mode);
		}
		
		protected void preDeflate(ISession session, byte[] in, ByteBuffer out) throws Exception {
			++preDeflateCount;
			super.preDeflate(session, in, out);
		}
		
		protected void preFinish(ISession session, ByteBuffer out) throws Exception {
			++preFinishCount;
			super.preFinish(session, out);
		}
		
		protected void postFinish(ISession session, ByteBuffer out) throws Exception {
			++postFinishCount;
			super.postFinish(session, out);
		}
		
		protected int deflateBound(int len) {
			boundOut = deflateBound == -1 ? super.deflateBound(len) : deflateBound;
			boundIn = len;
			deflateBound = -1;
			return boundOut;
		}
	}
	
}
