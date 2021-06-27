/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020-2021 SNF4J contributors
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import org.junit.Test;
import org.snf4j.core.codec.zip.ZlibCodec.Mode;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISession;

public class ZlibDecoderTest extends DecoderTest {
	
	
	
	@Test
	public void testNowrap() throws Exception {
		Method nowrap = ZlibDecoder.class.getDeclaredMethod("nowrap", short.class);
		nowrap.setAccessible(true);

		short val = 0;
		assertTrue((Boolean)nowrap.invoke(null, val));
		val = 0x7800;
		assertTrue((Boolean)nowrap.invoke(null, val));
		val = 0x7800 + (31 - 0x7800 % 31);
		assertFalse((Boolean)nowrap.invoke(null, val));
		val = (short) (0xFF00 + (31 - 0xFF00 % 31));
		assertTrue((Boolean)nowrap.invoke(null, val));
	}
	
	@Test
	public void testConstructor() throws Exception {
		ZlibDecoder d = new ZlibDecoder();
		assertNotNull(getInflater(d));
		createDeflater(false);
		byte b[] = deflate("abcd");
		d.decode(null, b, out);
		assertInflate("abcd", 1);
		out.clear();

		d = new ZlibDecoder((byte[])null);
		assertNotNull(getInflater(d));
		createDeflater(false);
		b = deflate("abcd");
		d.decode(null, b, out);
		assertInflate("abcd", 1);
		out.clear();		
		
		byte[] dic = "abcd".getBytes();
		d = new ZlibDecoder(dic);
		assertNotNull(getInflater(d));
		createDeflater(false);
		deflater.setDictionary(dic);
		b = deflate("abcd");
		d.decode(null, b, out);
		assertInflate("abcd", 1);
		out.clear();
		
		d = new ZlibDecoder(ZlibCodec.Mode.RAW);
		assertNotNull(getInflater(d));
		createDeflater(true);
		b = deflate("abcd");
		d.decode(null, b, out);
		assertInflate("abcd", 1);
		out.clear();

		d = new ZlibDecoder(ZlibCodec.Mode.ZLIB);
		assertNotNull(getInflater(d));
		createDeflater(false);
		b = deflate("abcd");
		d.decode(null, b, out);
		assertInflate("abcd", 1);
		out.clear();
		
		d = new ZlibDecoder(ZlibCodec.Mode.AUTO);
		assertNull(getInflater(d));
		
		d = new ZlibDecoder(dic, ZlibCodec.Mode.ZLIB);
		assertNotNull(getInflater(d));
		createDeflater(false);
		deflater.setDictionary(dic);
		b = deflate("abcd");
		d.decode(null, b, out);
		assertInflate("abcd", 1);
		out.clear();
		
		d = new ZlibDecoder(null, ZlibCodec.Mode.ZLIB);
		assertNotNull(getInflater(d));
		createDeflater(false);
		b = deflate("abcd");
		d.decode(null, b, out);
		assertInflate("abcd", 1);
		out.clear();

		try {
			new ZlibDecoder(dic, null); fail();
		}
		catch (NullPointerException e) {
		}
		try {
			new ZlibDecoder((ZlibCodec.Mode)null); fail();
		}
		catch (NullPointerException e) {
		}
	}
	
	@Test
	public void testDecode() throws Exception {
		TestZlibDecoder d = new TestZlibDecoder(ZlibCodec.Mode.ZLIB);
		
		createDeflater(false);
		byte b[] = deflate("");
		d.decode(null, b, out);
		assertInflate("", 0);
		b = deflate("aaa");
		d.decode(null, b, out);
		assertInflate("aaa", 1);
		d.decode(null, new byte[0], out);
		assertInflate("aaa", 1);
		b = deflate("bbb", true, "");
		d.decode(null, b, out);
		assertInflate("aaabbb", 2);
		assertTrue(d.isFinished());
		d.decode(null, "c".getBytes(), out);
		assertInflate("aaabbbc", 3);
		d.decode(null, new byte[0], out);
		assertInflate("aaabbbc", 3);
		out.clear();
		
		//split 1,...
		d = new TestZlibDecoder(ZlibCodec.Mode.AUTO);
		createDeflater(false);
		b = deflate("1234");
		byte[][] split = split(b, 1);
		d.decode(null, split[0], out);
		assertInflate("", 0);
		d.decode(null, split[1], out);
		assertInflate("1234", 1);
		out.clear();

		//split 1,1,...
		d = new TestZlibDecoder(ZlibCodec.Mode.AUTO);
		createDeflater(false);
		b = deflate("1234");
		split = split(b, 1,1);
		d.decode(null, split[0], out);
		assertInflate("", 0);
		d.decode(null, split[1], out);
		assertInflate("", 0);
		d.decode(null, split[2], out);
		assertInflate("1234", 1);
		out.clear();
		
		//split 2,1,...
		d = new TestZlibDecoder(ZlibCodec.Mode.AUTO);
		createDeflater(false);
		b = deflate("1234");
		split = split(b, 2,1);
		d.decode(null, split[0], out);
		assertInflate("", 0);
		d.decode(null, split[1], out);
		assertInflate("", 0);
		d.decode(null, split[2], out);
		assertInflate("1234", 1);
		out.clear();
		
		//split 2,1,1,...
		d = new TestZlibDecoder(ZlibCodec.Mode.AUTO);
		createDeflater(false);
		b = deflate("1234");
		split = split(b, 2,1,1);
		d.decode(null, split[0], out);
		assertInflate("", 0);
		d.decode(null, split[1], out);
		assertInflate("", 0);
		d.decode(null, split[2], out);
		d.decode(null, split[3], out);
		assertInflate("1234", out.size());
		out.clear();
		
		//to small bound
		d = new TestZlibDecoder(ZlibCodec.Mode.ZLIB);
		createDeflater(false);
		b = deflate("1234");
		split = split(b, 2);
		d.decode(null, split[0], out);
		assertInflate("", 0);
		d.inflateBound = 2;
		d.decode(null, split[1], out);
		assertInflate("1234", 2);
		out.clear();
		
		//preInflate consume all data
		d = new TestZlibDecoder(ZlibCodec.Mode.ZLIB);
		createDeflater(false);
		b = deflate("1234");
		split = split(b, 2);
		d.decode(null, split[0], out);
		assertInflate("", 0);
		d.preInflateConsume = true;
		d.decode(null, split[1], out);
		assertInflate("", 0);
		d.decode(null, split[1], out);
		assertInflate("1234", 1);
		
		//dictionary not specified
		d = new TestZlibDecoder(ZlibCodec.Mode.ZLIB);
		createDeflater(false);
		deflater.setDictionary("abcd".getBytes());
		b = deflate("abcd");
		try {
			d.decode(null, b, out); fail();
		}
		catch (DecompressionException e) {
			assertTrue(e.getMessage().indexOf("dictionary") != -1);
		}
		out.clear();
		
		//invalid format data
		d = new TestZlibDecoder(ZlibCodec.Mode.ZLIB);
		createDeflater(false);
		b = deflate("");
		d.decode(null, b, out);
		assertInflate("", 0);
		b = deflate("abcdefghijklmn");
		for (int i=0; i<b.length; ++i) {
			b[i] *= 45;
		}
		try {
			d.decode(null, b, out); fail();
		}
		catch (DecompressionException e) {
			assertTrue(e.getMessage().indexOf("invalid") != -1);
		}
		out.clear();
		
		//delayed finish
		d = new TestZlibDecoder(ZlibCodec.Mode.ZLIB);
		createDeflater(false);
		b = deflate("", true, "ee");
		d.postFinish = false;
		d.decode(null, b, out);
		assertInflate("", 0);
		assertFalse(d.isFinished());
		d.decode(null, "bb".getBytes(), out);
		assertTrue(d.isFinished());
		assertNull(getInflater(d));
		assertInflate("eebb", 1);
	}
	
	@Test
	public void testDecodeOtherModes() throws Exception {
		TestZlibDecoder d = new TestZlibDecoder(ZlibCodec.Mode.RAW);
		
		createDeflater(true);
		byte b[] = deflate("abcd");
		d.decode(null, b, out);
		assertInflate("abcd", 1);
		b = deflate("1234", true, "56");
		d.decode(null, b, out);
		assertInflate("abcd123456", 3);
		assertTrue(d.isFinished());
		d.decode(null, "c".getBytes(), out);
		assertInflate("abcd123456c", 4);
		out.clear();
		
		byte[] dictionary = "abcd".getBytes();
		
		for (int i=0; i<10; ++i) {

			d = new TestZlibDecoder(ZlibCodec.Mode.AUTO);	
			createDeflater(i, false);
			b = deflate("abcd");
			d.decode(null, b, out);
			assertInflate("abcd", 1);
			b = deflate("", true, "");
			d.decode(null, b, out);
			assertInflate("abcd", 1);
			assertTrue(d.isFinished());
			out.clear();
			
			d = new TestZlibDecoder(dictionary, ZlibCodec.Mode.AUTO);	
			createDeflater(i, false);
			deflater.setDictionary(dictionary);
			b = deflate("abcd");
			d.decode(null, b, out);
			assertInflate("abcd", 1);
			b = deflate("", true, "");
			d.decode(null, b, out);
			assertInflate("abcd", 1);
			assertTrue(d.isFinished());
			out.clear();
			
			d = new TestZlibDecoder(ZlibCodec.Mode.AUTO);	
			createDeflater(i, true);
			b = deflate("abcd");
			d.decode(null, b, out);
			assertInflate("abcd", 1);
			b = deflate("", true, "");
			d.decode(null, b, out);
			assertInflate("abcd", 1);
			assertTrue(d.isFinished());
			out.clear();

		}
		
	}
	
	@Test
	public void testEvents() throws Exception {
		ZlibDecoder d = new ZlibDecoder();
		
		assertFalse(d.isFinished());
		d.added(null, null);
		d.removed(null, null);
		d.event(null, SessionEvent.CREATED);
		assertFalse(d.isFinished());
		d.event(null, SessionEvent.OPENED);
		assertFalse(d.isFinished());
		d.event(null, SessionEvent.READY);
		assertFalse(d.isFinished());
		d.event(null, SessionEvent.CLOSED);
		assertFalse(d.isFinished());
		assertNotNull(getInflater(d));
		d.event(null, SessionEvent.ENDING);
		assertTrue(d.isFinished());
		assertNull(getInflater(d));
		
		d.event(null, SessionEvent.CREATED);
		d.event(null, SessionEvent.OPENED);
		d.event(null, SessionEvent.READY);
		d.event(null, SessionEvent.CLOSED);
		d.event(null, SessionEvent.ENDING);
	}
	
	@Test
	public void testDecodeHighlyCompressed() throws Exception {
		ZlibEncoder e = new ZlibEncoder(9, Mode.RAW);
		ZlibDecoder d = new ZlibDecoder(Mode.RAW);
		
		e.encode(null, new byte[1000], out);
		assertEquals(1, out.size());
		byte[] b = new byte[out.get(0).remaining()];
		out.get(0).get(b);
		out.clear();
		d.decode(null, b, out);
		int exp = 1000/(b.length*2);
		if (exp*b.length*2 < 1000) {
			exp++;
		}
		assertEquals(exp, out.size());
	}
	
	static class TestZlibDecoder extends ZlibDecoder {
		
		int inflateBound = -1;
		boolean preInflateConsume;
		boolean postFinish = true;
		
		TestZlibDecoder(Mode mode) {
			super(mode);
		}
		
		TestZlibDecoder(byte[] dictionary, Mode mode) {
			super(dictionary, mode);
		}
		
		@Override
		protected int inflateBound(int len) {
			int bound = inflateBound == -1 ? super.inflateBound(len) : inflateBound;
			inflateBound = -1;
			return bound;
		}
		
		@Override
		protected void preInflate(ISession session, ByteBuffer in) throws Exception {
			if (preInflateConsume) {
				preInflateConsume = false;
				byte[] b = new byte[in.remaining()];
				in.get(b);
			}
			super.preInflate(session, in);
		}
		
		@Override
		protected boolean postFinish(ISession session, ByteBuffer out) throws Exception {
			if (!postFinish) {
				postFinish = true;
				return false;
			}
			return super.postFinish(session, out);
		}

	}
}
