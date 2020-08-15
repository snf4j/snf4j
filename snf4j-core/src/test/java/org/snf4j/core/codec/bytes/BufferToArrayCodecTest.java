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
package org.snf4j.core.codec.bytes;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.junit.Test;

public class BufferToArrayCodecTest {

	@Test
	public void testDecode() throws Exception {
		BufferToArrayDecoder d = new BufferToArrayDecoder();
		ArrayList<byte[]> out = new ArrayList<byte[]>();
		
		assertTrue(d.getInboundType() == ByteBuffer.class);
		assertTrue(d.getOutboundType() == byte[].class);
		
		ByteBuffer in = ByteBuffer.allocateDirect(5);
		byte[] data = "12345".getBytes();
		in.put(data).flip();
		d.decode(null, in, out);
		assertEquals(1, out.size());
		assertFalse(data == out.get(0));
		assertArrayEquals(data, out.get(0));
		out.clear();
		in = ByteBuffer.wrap(data);
		d.decode(null, in, out);
		assertEquals(1, out.size());
		assertTrue(data == out.get(0));
		assertEquals("12345", new String(data));
		out.clear();
		in = ByteBuffer.wrap(data, 1, 4);
		d.decode(null, in, out);
		assertEquals(1, out.size());
		assertFalse(data == out.get(0));
		assertEquals("2345", new String(out.get(0)));
	}
	
	@Test
	public void testEncode() throws Exception {
		BufferToArrayEncoder e = new BufferToArrayEncoder();
		ArrayList<byte[]> out = new ArrayList<byte[]>();
		
		assertTrue(e.getInboundType() == ByteBuffer.class);
		assertTrue(e.getOutboundType() == byte[].class);
		
		ByteBuffer in = ByteBuffer.allocateDirect(5);
		byte[] data = "12345".getBytes();
		in.put(data).flip();
		e.encode(null, in, out);
		assertEquals(1, out.size());
		assertFalse(data == out.get(0));
		assertArrayEquals(data, out.get(0));
		out.clear();
		in = ByteBuffer.wrap(data);
		e.encode(null, in, out);
		assertEquals(1, out.size());
		assertTrue(data == out.get(0));
		assertEquals("12345", new String(data));
		out.clear();
		in = ByteBuffer.wrap(data, 1, 4);
		e.encode(null, in, out);
		assertEquals(1, out.size());
		assertFalse(data == out.get(0));
		assertEquals("2345", new String(out.get(0)));
	}	
}
