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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

public class AckFrameBuilderTest {
	
	private AckFrameBuilder gen;
	
	@Before
	public void before() {
		gen = new AckFrameBuilder(10);
	}
	
	void assertRanges(AckFrame ack, long... vals) {
		int size = vals.length/2;
		
		assertEquals(size, ack.getRanges().length);
		for (int i=0; i<size; ++i) {
			assertEquals(vals[i*2], ack.getRanges()[i].getFrom());
			assertEquals(vals[i*2+1], ack.getRanges()[i].getTo());
		}
	}
	
	@Test
	public void testAddAtEnd() {
		assertNull(gen.build(4));
		gen.add(0);
		assertRanges(gen.build(9), 0,0);
		gen.add(0);
		assertRanges(gen.build(9), 0,0);
		
		gen.add(1);
		assertRanges(gen.build(9), 1,0);
		gen.add(1);
		assertRanges(gen.build(9), 1,0);
		
		gen.add(3);
		assertRanges(gen.build(9), 3,3,1,0);
		gen.add(3);
		assertRanges(gen.build(9), 3,3,1,0);
		
		gen.add(4);
		assertRanges(gen.build(9), 4,3,1,0);
		gen.add(4);
		assertRanges(gen.build(9), 4,3,1,0);

		gen.add(6);
		assertRanges(gen.build(9), 6,6,4,3,1,0);
		gen.add(6);
		assertRanges(gen.build(9), 6,6,4,3,1,0);
	}
	
	@Test
	public void testAddAtBegining() {
		gen.add(9);
		assertRanges(gen.build(9), 9,9);
		gen.add(9);
		assertRanges(gen.build(9), 9,9);
		
		gen.add(8);
		assertRanges(gen.build(9), 9,8);
		gen.add(9);
		assertRanges(gen.build(9), 9,8);

		gen.add(6);
		assertRanges(gen.build(9), 9,8,6,6);
		gen.add(6);
		assertRanges(gen.build(9), 9,8,6,6);

		gen.add(5);
		assertRanges(gen.build(9), 9,8,6,5);
		gen.add(5);
		assertRanges(gen.build(9), 9,8,6,5);

		gen.add(3);
		assertRanges(gen.build(9), 9,8,6,5,3,3);
		gen.add(3);
		assertRanges(gen.build(9), 9,8,6,5,3,3);		
	}	
	
	
	@Test
	public void testAddInMiddle() {
		gen.add(0);
		gen.add(20);
		assertRanges(gen.build(9), 20,20,0,0);
		
		gen.add(1);
		assertRanges(gen.build(9), 20,20,1,0);
		gen.add(1);
		assertRanges(gen.build(9), 20,20,1,0);

		gen.add(19);
		assertRanges(gen.build(9), 20,19,1,0);
		gen.add(19);
		assertRanges(gen.build(9), 20,19,1,0);

		gen.add(3);
		assertRanges(gen.build(9), 20,19,3,3,1,0);
		gen.add(3);
		assertRanges(gen.build(9), 20,19,3,3,1,0);

		gen.add(17);
		assertRanges(gen.build(9), 20,19,17,17,3,3,1,0);
		gen.add(17);
		assertRanges(gen.build(9), 20,19,17,17,3,3,1,0);
		
		gen.add(4);
		assertRanges(gen.build(9), 20,19,17,17,4,3,1,0);
		gen.add(4);
		assertRanges(gen.build(9), 20,19,17,17,4,3,1,0);

		gen.add(16);
		assertRanges(gen.build(9), 20,19,17,16,4,3,1,0);
		gen.add(16);
		assertRanges(gen.build(9), 20,19,17,16,4,3,1,0);

		gen.add(2);
		assertRanges(gen.build(9), 20,19,17,16,4,0);
		gen.add(2);
		assertRanges(gen.build(9), 20,19,17,16,4,0);

		gen.add(18);
		assertRanges(gen.build(9), 20,16,4,0);
		gen.add(18);
		assertRanges(gen.build(9), 20,16,4,0);		
	}
	
	@Test
	public void testAddWithLimit() {
		gen = new AckFrameBuilder(3);
		
		gen.add(10);
		assertRanges(gen.build(9), 10,10);
		gen.add(12);
		assertRanges(gen.build(9), 12,12,10,10);
		gen.add(14);
		assertRanges(gen.build(9), 14,14,12,12,10,10);
		gen.add(15);
		assertRanges(gen.build(9), 15,14,12,12,10,10);
		gen.add(17);
		assertRanges(gen.build(9), 17,17,15,14,12,12);
		gen.add(11);
		assertRanges(gen.build(9), 17,17,15,14,12,11);
		gen.add(9);
		assertRanges(gen.build(9), 17,17,15,14,12,11);
		assertRanges(gen.build(1), 17,17);
		assertRanges(gen.build(2), 17,17,15,14);
		assertRanges(gen.build(3), 17,17,15,14,12,11);
	}
	
	@Test
	public void testKeepPriorTo() {
		gen.add(0);
		gen.add(1);
		gen.add(2);
		gen.add(6);
		gen.add(7);
		gen.add(10);
		assertRanges(gen.build(9), 10,10,7,6,2,0);
		
		gen.keepPriorTo(11);
		assertRanges(gen.build(9), 10,10,7,6,2,0);
		gen.keepPriorTo(10);
		assertRanges(gen.build(9), 7,6,2,0);
		gen.keepPriorTo(7);
		assertRanges(gen.build(9), 6,6,2,0);
		gen.keepPriorTo(2);
		assertRanges(gen.build(9), 1,0);
		gen.keepPriorTo(0);
		assertNull(gen.build(9));
	}
}
