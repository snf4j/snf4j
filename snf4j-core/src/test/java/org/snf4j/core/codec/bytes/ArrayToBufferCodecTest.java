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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.junit.Test;

public class ArrayToBufferCodecTest {

	@Test
	public void testDecode() throws Exception {
		ArrayToBufferDecoder d = new ArrayToBufferDecoder();
		ArrayList<ByteBuffer> out = new ArrayList<ByteBuffer>();
		
		assertTrue(d.getInboundType() == byte[].class);
		assertTrue(d.getOutboundType() == ByteBuffer.class);
		byte[] data = "12345".getBytes();
		
		d.decode(null, data, out);
		assertEquals(1, out.size());
		ByteBuffer b = out.get(0);
		assertTrue(data == b.array());
		assertEquals(data.length, b.remaining());
		assertEquals(0, b.position());
		assertEquals(data.length, b.limit());
	}

	@Test
	public void testEncode() throws Exception {
		ArrayToBufferEncoder e = new ArrayToBufferEncoder();
		ArrayList<ByteBuffer> out = new ArrayList<ByteBuffer>();
		
		assertTrue(e.getInboundType() == byte[].class);
		assertTrue(e.getOutboundType() == ByteBuffer.class);
		byte[] data = "12345".getBytes();
		
		e.encode(null, data, out);
		assertEquals(1, out.size());
		ByteBuffer b = out.get(0);
		assertTrue(data == b.array());
		assertEquals(data.length, b.remaining());
		assertEquals(0, b.position());
		assertEquals(data.length, b.limit());
	}
}
