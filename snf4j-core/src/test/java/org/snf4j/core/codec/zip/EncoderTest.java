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
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.junit.Before;

public class EncoderTest {
	List<ByteBuffer> out;
	
	byte[] dictionary;
	
	@Before
	public void before() {
		out = new ArrayList<ByteBuffer>();
		dictionary = null;
	}
	
	void assertDeflate(String expected, int expectedCount) throws DataFormatException {
		assertDeflate(expected, expectedCount, false, false);
	}
	
	void assertDeflate(String expected, int expectedCount, boolean finished, boolean raw) throws DataFormatException {
		assertEquals(expectedCount, out.size());
		
		StringBuilder sb = new StringBuilder();
		Inflater i = new Inflater(raw);
		byte[] buf = new byte[1000];
		int len;
		
		for (ByteBuffer bb: out) {
			byte[] b = new byte[bb.remaining()];
			
			
			bb.duplicate().get(b);
			i.setInput(b);
			len = i.inflate(buf);
			if (len == 0 && i.needsDictionary()) {
				if (dictionary == null) {
					throw new IllegalArgumentException();
				}
				i.setDictionary(dictionary);
				len = i.inflate(buf);
			}
			if (!i.finished()) {
				assertTrue(i.needsInput());
			}
			sb.append(new String(buf, 0, len));
		}
		assertEquals(expected, sb.toString());
		assertEquals(finished, i.finished());
	}
	
	Deflater getDeflater(ZlibEncoder e) throws Exception {
		Field f = ZlibEncoder.class.getDeclaredField("deflater");
		f.setAccessible(true);
		return (Deflater) f.get(e);
	}

}
