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
package org.snf4j.websocket.frame;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AggregatedFrameTest {

	@Test
	public void testBinaryFrame() {
		byte[] data1 = "ABCD".getBytes();
		AggregatedBinaryFrame f = new AggregatedBinaryFrame(true, 0, data1);
		
		assertTrue(Opcode.BINARY == f.getOpcode());
		assertTrue(f.isFinalFragment());
		assertEquals(0, f.getRsvBits());
		assertTrue(data1 == f.getPayload());
		assertEquals(data1.length, f.getPayloadLength());
		assertEquals(1, f.getFragments().size());
		assertTrue(data1 == f.getFragments().get(0));
		
		byte[] data2 = "EF".getBytes();
		f.addFragment(data2);
		assertEquals(data1.length+data2.length, f.getPayloadLength());
		assertEquals(2, f.getFragments().size());
		assertTrue(data1 == f.getFragments().get(0));
		assertTrue(data2 == f.getFragments().get(1));
		assertEquals("ABCDEF", new String(f.getPayload()));
		assertEquals("ABCDEF", new String(f.getPayload()));
		assertEquals(1, f.getFragments().size());
		assertTrue(f.getPayload() == f.getFragments().get(0));
	}

	@Test
	public void testTextFrame() {
		byte[] data1 = "ABCD".getBytes();
		AggregatedTextFrame f = new AggregatedTextFrame(true, 0, data1);
		
		assertTrue(Opcode.TEXT == f.getOpcode());
		assertTrue(f.isFinalFragment());
		assertEquals(0, f.getRsvBits());
		assertTrue(data1 == f.getPayload());
		assertEquals(data1.length, f.getPayloadLength());
		assertEquals(1, f.getFragments().size());
		assertTrue(data1 == f.getFragments().get(0));
		
		byte[] data2 = "EF".getBytes();
		f.addFragment(data2);
		assertEquals(data1.length+data2.length, f.getPayloadLength());
		assertEquals(2, f.getFragments().size());
		assertTrue(data1 == f.getFragments().get(0));
		assertTrue(data2 == f.getFragments().get(1));
		assertEquals("ABCDEF", new String(f.getPayload()));
		assertEquals("ABCDEF", new String(f.getPayload()));
		assertEquals(1, f.getFragments().size());
		assertTrue(f.getPayload() == f.getFragments().get(0));
		
		assertEquals(FrameTest.UTF8_TEXT, AggregatedTextFrame.toText(FrameTest.UTF8_BYTES));
	}
}
