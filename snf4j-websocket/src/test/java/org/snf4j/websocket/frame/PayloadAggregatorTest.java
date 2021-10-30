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

public class PayloadAggregatorTest {

	@Test
	public void testAdd() {
		byte[] data1 = "ABCDEF".getBytes();
		byte[] data2 = "GHIJ".getBytes();
		PayloadAggregator pa = new PayloadAggregator(data1);
		
		assertEquals(1, pa.getFragments().size());
		assertTrue(pa.getFragments().get(0) == data1);
		assertEquals(data1.length, pa.getLength());
		assertTrue(pa.get() == data1);
		
		pa.add(data2);
		assertEquals(2, pa.getFragments().size());
		assertTrue(pa.getFragments().get(0) == data1);
		assertTrue(pa.getFragments().get(1) == data2);
		assertEquals(data1.length+data2.length, pa.getLength());
		byte[] data3 = pa.get();
		assertEquals("ABCDEFGHIJ", new String(data3));
		assertEquals(1, pa.getFragments().size());
		assertTrue(data3 == pa.get());
		assertTrue(data3 == pa.getFragments().get(0));
		assertEquals(data3.length, pa.getLength());
		
		data1 = "XY".getBytes();
		data2 = "Z".getBytes();
		pa.add(data1);
		pa.add(data2);
		assertEquals(3, pa.getFragments().size());
		assertEquals("ABCDEFGHIJXYZ", new String(pa.get()));
	}
}
