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
package org.snf4j.websocket.extensions.compress;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.snf4j.core.codec.zip.ZlibCodec;
import org.snf4j.core.codec.zip.ZlibDecoder;
import org.snf4j.core.codec.zip.ZlibEncoder;
import org.snf4j.core.session.ISession;
import org.snf4j.websocket.TestWSSession;
import org.snf4j.websocket.frame.BinaryFrame;
import org.snf4j.websocket.frame.CloseFrame;
import org.snf4j.websocket.frame.ContinuationFrame;
import org.snf4j.websocket.frame.Frame;
import org.snf4j.websocket.frame.InvalidFrameException;
import org.snf4j.websocket.frame.PingFrame;
import org.snf4j.websocket.frame.PongFrame;
import org.snf4j.websocket.frame.TextFrame;

public class PerMessageDeflateCodecTest {
	
	PerMessageDeflateEncoder e;
	
	PerMessageDeflateDecoder d;
	
	void assertEncodeDecode(Frame f) throws Exception {
		assertEncodeDecode(f, 4);
	}
	
	void assertEncodeDecode(Frame f, int rsvMask) throws Exception {
		List<Frame> out = new ArrayList<Frame>();
		TestWSSession s = new TestWSSession();
		
		e.encode(s, f, out);
		assertEquals(1, out.size());
		Frame f2 = out.get(0);
		assertEquals(f.isFinalFragment(), f2.isFinalFragment());
		assertEquals(f.getRsvBits() | rsvMask, f2.getRsvBits());
		assertEquals(f.getOpcode(), f2.getOpcode());
		
		out.clear();
		d.decode(s, f2, out);
		assertEquals(1, out.size());
		f2 = out.get(0);
		assertEquals(f.isFinalFragment(), f2.isFinalFragment());
		assertEquals(f.getRsvBits(), f2.getRsvBits());
		assertEquals(f.getOpcode(), f2.getOpcode());
		assertArrayEquals(f.getPayload(), f2.getPayload());
	}
	
	byte[] bytes(int len) {
		byte[] bytes = new byte[len];
		
		for (int i=0; i<len; ++i) {
			bytes[i] = (byte)i;
		}
		return bytes;
	}
	
	@Test
	public void testEncodeDecode() throws Exception {
		e = new PerMessageDeflateEncoder(8, false);
		d = new PerMessageDeflateDecoder(false);
		
		assertNull(e.codec());
		assertNull(d.codec());
		
		for (int i=0; i<2; i++) {
			assertEncodeDecode(new TextFrame(true, 0, "ABCDEFG"));
			if (i == 0) {
				assertTrue(e.codec().getClass() == ZlibEncoder.class);
				assertTrue(d.codec() instanceof ZlibDecoder);
			}
			else {
				assertNull(e.codec());
				assertNull(d.codec());
			}
			assertEncodeDecode(new TextFrame(true, 3, "1"));
			assertEncodeDecode(new TextFrame(true, 1, ""));

			assertEncodeDecode(new BinaryFrame(true, 1, bytes(1024)));
			assertEncodeDecode(new PingFrame(0, bytes(10)), 0);
			assertEncodeDecode(new PongFrame(0, bytes(10)), 0);
			assertEncodeDecode(new CloseFrame(0, 1000, "XXX"), 0);
			assertEncodeDecode(new ContinuationFrame(true, 1, bytes(10)), 0);

			assertEncodeDecode(new TextFrame(false, 0, "ABCDEFG"));
			assertEncodeDecode(new ContinuationFrame(false, 0, "IJKLMNOP".getBytes()), 0);
			assertEncodeDecode(new ContinuationFrame(true, 0, "XYZ".getBytes()), 0);

			e = new PerMessageDeflateEncoder(8, true);
			d = new PerMessageDeflateDecoder(true);
		}
		
	}
	
	@Test
	public void testDecodeWithoutRsv1() throws Exception {
		d = new PerMessageDeflateDecoder(false);
		List<Frame> out = new ArrayList<Frame>();

		Frame[] frames = new Frame[] {new TextFrame(true, 0, "ABC"),new BinaryFrame(true, 0, bytes(10)),
				new ContinuationFrame(true, 0, bytes(10)),new PingFrame(0, bytes(10)),
				new PongFrame(0, bytes(10)), new CloseFrame(0, 1000)
		};
		
		for (Frame frame: frames) {
			out.clear();
			d.decode(null, frame, out);
			assertEquals(1, out.size());
			assertTrue(frame == out.get(0));
		}
	}
	
	@Test
	public void testEncodeWithoutRsv1() throws Exception {
		e = new PerMessageDeflateEncoder(8, false);
		List<Frame> out = new ArrayList<Frame>();

		Frame[] frames = new Frame[] {new TextFrame(true, 4, "ABC"),new BinaryFrame(true, 7, bytes(10)),
				new ContinuationFrame(true, 0, bytes(10)),new PingFrame(0, bytes(10)),
				new PongFrame(0, bytes(10)), new CloseFrame(0, 1000)
		};
		
		for (Frame frame: frames) {
			out.clear();
			e.encode(null, frame, out);
			assertEquals(1, out.size());
			assertTrue(frame.getClass().getName(), frame == out.get(0));
		}
	}
	
	@Test
	public void testEncodeDifferentOutputs() throws Exception {
		e = new PerMessageDeflateEncoder(8, false);
		d = new PerMessageDeflateDecoder(false);
		Encoder internal = new Encoder();
		
		Field f = DeflateEncoder.class.getDeclaredField("encoder");
		f.setAccessible(true);
		f.set(e, internal);
		assertNotNull(e.codec());
		internal.split = true;
		assertEncodeDecode(new TextFrame(true, 0, "ABCDEFG"));
		internal.split = false;
		internal.empty = true;
		try {
			assertEncodeDecode(new TextFrame(true, 0, "ABCDEFG"));
			fail();
		}
		catch (IllegalStateException e) {			
			assertEquals("Deflating of input data produced no data", e.getMessage());
		}
	}
	
	@Test
	public void testDecodeDifferentOutputs() throws Exception {
		e = new PerMessageDeflateEncoder(8, false);
		d = new PerMessageDeflateDecoder(false);
		Decoder internal = new Decoder();
		
		Field f = DeflateDecoder.class.getDeclaredField("decoder");
		f.setAccessible(true);
		f.set(d, internal);
		assertNotNull(d.codec());
		internal.split = true;
		assertEncodeDecode(new TextFrame(true, 0, "ABCDEFG"));
		internal.split = false;
		internal.empty = true;
		try {
			assertEncodeDecode(new TextFrame(true, 0, "ABCDEFG"));
			fail();
		}
		catch (InvalidFrameException e) {			
			assertEquals("Inflating of input data produced no data", e.getMessage());
		}
		
		Frame frame = new TextFrame(true, 4, new byte[] {1});
		List<Frame> out = new ArrayList<Frame>();
		try {
			d.decode(new TestWSSession(), frame, out);
			fail();
		}
		catch (InvalidFrameException e) {			
			assertEquals("Inflating of input data produced no data", e.getMessage());
		}
		
		frame = new TextFrame(true, 4, new byte[2]);
		out = new ArrayList<Frame>();
		try {
			d.decode(new TestWSSession(), frame, out);
			fail();
		}
		catch (InvalidFrameException e) {			
			assertEquals("Inflating of input data produced no data", e.getMessage());
		}
	}
	
	@Test
	public void testDecodeUncompressed() throws Exception {
		d = new PerMessageDeflateDecoder(false);
		
		Frame f = new TextFrame(true, 0, "TEXT");
		List<Frame> out = new ArrayList<Frame>();
		d.decode(null, f, out);
		assertEquals(1, out.size());
		assertTrue(f == out.get(0));
		
		f = new TextFrame(false, 0, "TEXT");
		out.clear();
		d.decode(null, f, out);
		assertEquals(1, out.size());
		assertTrue(f == out.get(0));
		f = new ContinuationFrame(false, 3, "TEXT".getBytes());
		out.clear();
		d.decode(null, f, out);
		assertEquals(1, out.size());
		assertTrue(f == out.get(0));
		f = new ContinuationFrame(true, 0, "TEXT".getBytes());
		out.clear();
		d.decode(null, f, out);
		assertEquals(1, out.size());
		assertTrue(f == out.get(0));
	}

	@Test
	public void testEncodeCompressed() throws Exception {
		e = new PerMessageDeflateEncoder(8, false);
		
		Frame f = new TextFrame(true, 4, "TEXT");
		List<Frame> out = new ArrayList<Frame>();
		e.encode(null, f, out);
		assertEquals(1, out.size());
		assertTrue(f == out.get(0));
		
		f = new TextFrame(false, 4, "TEXT");
		out.clear();
		e.encode(null, f, out);
		assertEquals(1, out.size());
		assertTrue(f == out.get(0));
		f = new ContinuationFrame(false, 3, "TEXT".getBytes());
		out.clear();
		e.encode(null, f, out);
		assertEquals(1, out.size());
		assertTrue(f == out.get(0));
		f = new ContinuationFrame(true, 0, "TEXT".getBytes());
		out.clear();
		e.encode(null, f, out);
		assertEquals(1, out.size());
		assertTrue(f == out.get(0));
	}
	
	@Test
	public void testMinInflateBound() throws Exception {
		e = new PerMessageDeflateEncoder(8, false);
		d = new PerMessageDeflateDecoder(false);
		
		Decoder internal = new Decoder();
		
		Field f = DeflateDecoder.class.getDeclaredField("decoder");
		f.setAccessible(true);
		f.set(d, internal);
		assertEncodeDecode(new BinaryFrame(true, 0, new byte[2024]));
		assertTrue(internal.outSize > 10);
		
		e = new PerMessageDeflateEncoder(8, false);
		d = new PerMessageDeflateDecoder(false, 2024);
		assertEncodeDecode(new BinaryFrame(true, 0, new byte[2024]));
		internal.wrapped = (ZlibDecoder) f.get(d);
		f.set(d, internal);
		assertEncodeDecode(new BinaryFrame(true, 0, new byte[2024]));
		assertEquals(1, internal.outSize);
		
	}
	
	@Test
	public void testDecompressionFailure() throws Exception {
		e = new PerMessageDeflateEncoder(8, false);
		d = new PerMessageDeflateDecoder(false);

		assertEncodeDecode(new BinaryFrame(true, 0, bytes(2024)));
		d = new PerMessageDeflateDecoder(false);
		try {
			assertEncodeDecode(new BinaryFrame(true, 0, bytes(2024)));
			fail();
		}
		catch (InvalidFrameException e) {
		}
	}
	
	class Decoder extends ZlibDecoder {
		
		boolean split;
		
		boolean empty;
		
		int outSize;
		
		ZlibDecoder wrapped;
		
		Decoder() {
			super(ZlibCodec.Mode.RAW);
		}
		
		@Override
		public void decode(ISession session, byte[] data, List<ByteBuffer> out) throws Exception {
			if (wrapped != null) {
				wrapped.decode(session, data, out);
			}
			else {
				super.decode(session, data, out);
			}
			
			outSize = out.size();
			if (out.size() == 1 && split) {
				ByteBuffer b = out.get(0);
				byte[] b1 = new byte[b.remaining()/2];
				byte[] b2 = new byte[b.remaining() - b1.length];
				
				b.get(b1);
				b.get(b2);
				out.clear();
				out.add(ByteBuffer.wrap(b1));
				out.add(ByteBuffer.wrap(b2));
			}
			if (empty) {
				out.clear();
			}
		}
	}
	
	class Encoder extends ZlibEncoder {
		
		boolean split;
		
		boolean empty;
		
		Encoder() {
			super(8, ZlibCodec.Mode.RAW);
		}
		
		@Override
		public void encode(ISession session, byte[] data, List<ByteBuffer> out) throws Exception {
			super.encode(session, data, out);
			
			if (out.size() == 1 && split) {
				ByteBuffer b = out.get(0);
				byte[] b1 = new byte[b.remaining()/2];
				byte[] b2 = new byte[b.remaining() - b1.length];
				
				b.get(b1);
				b.get(b2);
				out.clear();
				out.add(ByteBuffer.wrap(b1));
				out.add(ByteBuffer.wrap(b2));
			}
			if (empty) {
				out.clear();
			}
		}
	}
}
