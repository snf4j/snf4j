/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic.frame;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.snf4j.quic.CommonTest;

public class PaddingFrameTest extends CommonTest {

	@Test
	public void testGetType() {
		assertEquals(0, FrameType.PADDING.firstValue());
		assertEquals(0, FrameType.PADDING.lastValue());
		assertSame(FrameType.PADDING, new PaddingFrame().getType());
		assertSame(FrameType.PADDING, PaddingFrame.getParser().getType());
	}
	
	@Test
	public void testGetLength() {
		assertEquals(1, new PaddingFrame().getLength());
	}
	
	@Test
	public void testParse() throws Exception {
		PaddingFrame f = PaddingFrame.getParser().parse(buffer(""), 0, 0);
		assertEquals(1, f.getLength());
		assertEquals(0, buffer.remaining());
		f = PaddingFrame.getParser().parse(buffer("00"), 0, 0);
		assertEquals(1, f.getLength());
		assertEquals(1, buffer.remaining());
		f = PaddingFrame.getParser().parse(buffer("00"), 1, 0);
		assertEquals(2, f.getLength());
		assertEquals(0, buffer.remaining());
		f = PaddingFrame.getParser().parse(buffer("00000001"), 4, 0);
		assertEquals(4, f.getLength());
		assertEquals(1, buffer.remaining());
		
		buffer("00000001");
		buffer.position(2);
		ByteBuffer slice = buffer.slice();
		f = PaddingFrame.getParser().parse(slice, 2, 0);
		assertEquals(2, f.getLength());
		assertEquals(1, slice.remaining());
		assertEquals(1, slice.position());
	}

	@Test
	public void testParseFromDirectBuffer() throws Exception {
		PaddingFrame f = PaddingFrame.getParser().parse(directBuffer(""), 0, 0);
		assertEquals(1, f.getLength());
		assertEquals(0, directBuffer.remaining());
		f = PaddingFrame.getParser().parse(directBuffer("00"), 0, 0);
		assertEquals(1, f.getLength());
		assertEquals(1, directBuffer.remaining());
		f = PaddingFrame.getParser().parse(directBuffer("00"), 1, 0);
		assertEquals(2, f.getLength());
		assertEquals(0, directBuffer.remaining());
		f = PaddingFrame.getParser().parse(directBuffer("00000001"), 4, 0);
		assertEquals(4, f.getLength());
		assertEquals(1, directBuffer.remaining());
	}
	
	@Test
	public void testGetBytes() {
		assertArrayEquals(bytes(""), bytes());
		new PaddingFrame().getBytes(buffer);
		assertArrayEquals(bytes("00"), bytes());
	}
}
