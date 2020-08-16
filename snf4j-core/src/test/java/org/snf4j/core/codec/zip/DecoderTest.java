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

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.junit.Before;

public class DecoderTest {
	List<ByteBuffer> out;
	
	Deflater deflater;
	
	@Before
	public void before() {
		out = new ArrayList<ByteBuffer>();
	}

	void createDeflater(boolean nowrap) {
		deflater = new Deflater(6, nowrap);
	}
	
	void createDeflater(int level, boolean nowrap) {
		deflater = new Deflater(level, nowrap);
	}
	
	byte[] deflate(String s) {
		return deflate(s, false, "");
	}
	
	byte[] deflate(String s, boolean finish, String padding) {
		byte[] tmp = new byte[1000];
		
		deflater.setInput(s.getBytes());
		int len = deflater.deflate(tmp,0,1000, Deflater.SYNC_FLUSH);
		byte[] result = new byte[len];
		System.arraycopy(tmp, 0, result, 0, len);
		
		if (finish) {
			deflater.finish();
			len = deflater.deflate(tmp,0,1000, Deflater.SYNC_FLUSH);
			byte[] pad = padding.getBytes();
			byte[] result2 = new byte[result.length + len + pad.length];
			System.arraycopy(result, 0, result2, 0, result.length);
			int i = result.length;
			System.arraycopy(tmp, 0, result2, i, len);
			i += len;
			System.arraycopy(pad, 0, result2, i, pad.length);
			result = result2;
		}
		
		return result;
	}

	void assertInflate(String expected, int expectedCount) {
		assertEquals(expectedCount, out.size());
		StringBuilder sb = new StringBuilder();
		
		for (ByteBuffer bb: out) {
			byte[] b = new byte[bb.remaining()];
			
			bb.duplicate().get(b);
			sb.append(new String(b));
		}
		assertEquals(expected, sb.toString());
	}
	
	byte[][] split(byte[] data, int... sizes) {
		byte[][] s = new byte[sizes.length + 1][];
		int off = 0;
		int i=0;
		
		for (; i<sizes.length; ++i) {
			byte[] b = new byte[sizes[i]];
			
			System.arraycopy(data, off, b, 0, b.length);
			s[i] = b;
			off += b.length;
		}
		byte[] b = new byte[data.length - off];
		System.arraycopy(data, off, b, 0, b.length);
		s[i] = b;
		return s;
	}
	
	Inflater getInflater(ZlibDecoder e) throws Exception {
		Field f = ZlibDecoder.class.getDeclaredField("inflater");
		f.setAccessible(true);
		return (Inflater) f.get(e);
	}
	
}
