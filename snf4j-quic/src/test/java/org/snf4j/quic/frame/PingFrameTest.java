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

import org.junit.Test;
import org.snf4j.quic.CommonTest;

public class PingFrameTest extends CommonTest {
	
	@Test
	public void testGetType() {
		assertEquals(0x01, FrameType.PING.firstValue());
		assertEquals(0x01, FrameType.PING.lastValue());
		assertSame(FrameType.PING, new PingFrame().getType());
		assertSame(FrameType.PING, PingFrame.getParser().getType());
	}

	@Test
	public void testGetLength() {
		assertEquals(1, new PingFrame().getLength());
	}
	
	@Test
	public void testParse() throws Exception {
		PingFrame f = PingFrame.getParser().parse(buffer(""), 0, 1);
		assertEquals(1, f.getLength());
		assertEquals(0, buffer.remaining());
		f = PingFrame.getParser().parse(buffer("00"), 1, 1);
		assertEquals(1, f.getLength());
		assertEquals(1, buffer.remaining());
		f = PingFrame.getParser().parse(buffer("01"), 1, 1);
		assertEquals(1, f.getLength());
		assertEquals(1, buffer.remaining());
	}
	
	@Test
	public void testGetBytes() {
		assertArrayEquals(bytes(""), bytes());
		new PingFrame().getBytes(buffer);
		assertArrayEquals(bytes("01"), bytes());
	}
	
}
