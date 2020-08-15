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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class GzipEncoderTest extends EncoderTest {
	
	@Test
	public void testDeflateBound() throws Exception {
		TestGzipEncoder gzip = new TestGzipEncoder();
		
		assertEquals(ZlibEncoderTest.expectedBound(1)+8, gzip.deflateBound(1));
		assertEquals(ZlibEncoderTest.expectedBound(1000)+8, gzip.deflateBound(1000));
	}
	
	void assertEncode(String expected, int expectedCount, boolean decoderFinished) throws Exception {
		List<ByteBuffer> o = new ArrayList<ByteBuffer>();
		GzipDecoder d = new GzipDecoder();
		
		assertEquals(expectedCount, out.size());
		for (ByteBuffer bb: out) {
			byte[] b = new byte[bb.remaining()];
			ByteBuffer dup = bb.duplicate();
			
			dup.get(b);
			d.decode(null, b, o);
		}
		assertEquals(decoderFinished, d.isFinished());
		
		StringBuilder sb = new StringBuilder();
		for (ByteBuffer bb: o) {
			byte[] b = new byte[bb.remaining()];
			bb.get(b);
			sb.append(new String(b));
		}
		assertEquals(expected, sb.toString());
	}
	
	@Test
	public void testEncode() throws Exception {
		GzipEncoder e = new GzipEncoder();
		e.finish();
		e.encode(null, "abcd".getBytes(), out);
		assertEncode("abcd", 2, true);
		assertTrue(e.isFinished());
		out.clear();
		
		e = new GzipEncoder(9);
		e.encode(null, "1234567890".getBytes(), out);
		assertEncode("1234567890", 1, false);
		assertFalse(e.isFinished());
		e.encode(null, "XX".getBytes(), out);
		assertEncode("1234567890XX", 2, false);
		assertFalse(e.isFinished());
		e.finish();
		assertFalse(e.isFinished());
		e.encode(null, "YY".getBytes(), out);
		assertEncode("1234567890XXYY", 4, true);
		assertTrue(e.isFinished());
		out.clear();
		
	}
	
	static class TestGzipEncoder extends GzipEncoder {
		
		int boundOut;

		@Override
		public int deflateBound(int len) {
			boundOut = super.deflateBound(len);
			return boundOut;
		}		
	}
}
