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
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import org.junit.Test;

public class GzipDecoderTest extends DecoderTest {
	
	final static byte[] GZIP_ABCD = new byte[] { 31, -117, 8, 8, 41, 23, 51, 95, 0, 3, 116, 101, 115, 116, 46, 116, 120, 116, 0, 75,
			76, 74, 78, 1, 0, 17, -51, -126, -19, 4, 0, 0, 0 };
	
	private void assertException(GzipDecoder d, byte[] data, String expectedMsg) throws Exception {
		try {
			d.decode(null, data, out); fail();
		}
		catch (DecompressionException e) {
			assertEquals(expectedMsg, e.getMessage());
		}
	}
	
	void put(ByteBuffer out, int value, int size) {
		out.put((byte) value);
		out.put((byte) (value >>> 8));
		if (size > 2) {
			out.put((byte) (value >>> 16));
			out.put((byte) (value >>> 24));
		}
	}
	
	byte[] deflate(String data, int xlen, int fname, int fcomment, boolean hcrc) {
		ByteBuffer out = ByteBuffer.allocate(data.length()*120/100 + 10 + xlen + fname + fcomment + 8 + 10);
		byte[] b = out.array();
		
		out.put(GzipEncoder.HEADER);
		if (xlen > 0) {
			b[3] |= 1 << 2;   /* FEXTRA */
			
			put(out, xlen, 2);
			out.put(new byte[xlen]);
		}
		
		if (fname > 0) {
			b[3] |= 1 << 3;  /* FNAME */
			
			byte[] n = new byte[fname];
			Arrays.fill(n, (byte)'a');
			out.put(n);
			out.put((byte) 0);
		}
		
		if (fcomment > 0) {
			b[3] |= 1 << 4;  /* FCOMMENT */
			byte[] c = new byte[fcomment];
			Arrays.fill(c, (byte)'b');
			out.put(c);
			out.put((byte) 0);
		}
		
		if (hcrc) {
			b[3] |= 1 << 1;  /* FHCRC */
			
			CRC32 crc = new CRC32();
			crc.update(b, 0, out.position());
			put(out, (int) crc.getValue(), 2);
		}
		
		Deflater deflater = new Deflater(6, true);
		byte[] bdata = data.getBytes();
		
		deflater.setInput(bdata);
		deflater.finish();
		int len = deflater.deflate(b, out.position(), out.remaining());
		out.position(out.position() + len);

		CRC32 crc = new CRC32();
		crc.update(bdata);
		put(out, (int) crc.getValue(), 4);
		
		int isize = bdata.length;
		
		put(out, isize, 4);
		
		out.flip();
		b = new byte[out.remaining()];
		out.get(b);
		return b;
	} 
	
	@Test
	public void testDecode() throws Exception {
		GzipDecoder d = new GzipDecoder();
		d.decode(null, GZIP_ABCD, out);
		assertInflate("abcd", 1);
		assertTrue(d.isFinished());
		out.clear();
		
		//incorrect isize in footer
		d = new GzipDecoder();
		byte[] b = GZIP_ABCD.clone();
		b[b.length-4] = 3;
		assertException(d, b, "number of bytes mismatch");
		
		//incorrect crc in footer
		d = new GzipDecoder();
		b = GZIP_ABCD.clone();
		b[b.length-8] = 3;
		assertException(d, b, "crc value mismatch");
		
		//incorrect id1 in header
		d = new GzipDecoder();
		b = GZIP_ABCD.clone();
		b[0] += 1;
		assertException(d, b, "input data is not in gzip format");

		//incorrect id2 in header
		d = new GzipDecoder();
		b = GZIP_ABCD.clone();
		b[1] += 1;
		assertException(d, b, "input data is not in gzip format");

		//incorrect cm in header
		d = new GzipDecoder();
		b = GZIP_ABCD.clone();
		b[2] += 1;
		assertException(d, b, "unssuported compression method 9 in gzip header");

		//reserved flags set in header
		d = new GzipDecoder();
		b = GZIP_ABCD.clone();
		b[3] |= 0x80;
		assertException(d, b, "reserved flags set in gzip header");
		d = new GzipDecoder();
		b = GZIP_ABCD.clone();
		b[3] |= 0x40;
		assertException(d, b, "reserved flags set in gzip header");
		d = new GzipDecoder();
		b = GZIP_ABCD.clone();
		b[3] |= 0x20;
		assertException(d, b, "reserved flags set in gzip header");
		
		//incorrect crc in header
		d = new GzipDecoder();
		b = deflate("abcd", 0, 0, 0, true);
		b[11] += 1;
		assertException(d, b, "crc value mismatch for gzip header");
		
		//test xlen
		out.clear();
		int lenWithoutXlen = b.length;
		d = new GzipDecoder();
		b = deflate("abcd", 255, 0, 0, true);
		assertEquals(257, b.length - lenWithoutXlen);
		d.decode(null, b, out);
		assertInflate("abcd", 1);
		assertTrue(d.isFinished());
		out.clear();		
		d = new GzipDecoder();
		b = deflate("abcd", 256, 0, 0, true);
		assertEquals(258, b.length - lenWithoutXlen);
		d.decode(null, b, out);
		assertInflate("abcd", 1);
		assertTrue(d.isFinished());
		
	}

	@Test
	public void testDecodeAllFlagCombinations() throws Exception {
		for (int i=0; i<16; ++i) {
			int a1 = (i & 1) == 0 ? 0 : 10;
			int a2 = (i & 2) == 0 ? 0 : 11;
			int a3 = (i & 4) == 0 ? 0 : 12;
			boolean a4 = (i & 8) == 0 ? false : true;
			
			byte[] b = deflate("123456", a1, a2, a3, a4);
			
			GzipDecoder d = new GzipDecoder();
			d.decode(null, b, out);
			assertInflate("123456", 1);		
			assertTrue(d.isFinished());
			out.clear();		
			
			for (int j=1; j<b.length-1; ++j) {
				byte[][] split = split(b, j);
				
				d = new GzipDecoder();
				d.decode(null, split[0], out);
				assertFalse(d.isFinished());
				d.decode(null, split[1], out);
				assertInflate("123456", out.size());		
				assertTrue(d.isFinished());
				out.clear();			
			}
		}
	}
	
}
