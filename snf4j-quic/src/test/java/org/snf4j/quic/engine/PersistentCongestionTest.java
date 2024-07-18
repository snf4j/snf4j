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
package org.snf4j.quic.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.snf4j.quic.engine.PersistentCongestion.Item;

public class PersistentCongestionTest {

	@Test
	public void testIsDetectable() {
		PersistentCongestion pc = new PersistentCongestion();
		
		assertFalse(pc.isDetectable());
		pc.lost(10);
		assertFalse(pc.isDetectable());
		pc.lost(10);
		assertFalse(pc.isDetectable());
		pc.lost(11);
		assertTrue(pc.isDetectable());
		assertFalse(pc.detect(1));
		assertTrue(pc.isDetectable());
		pc.lost(12);
		assertTrue(pc.isDetectable());
		assertTrue(pc.detect(1));
		assertFalse(pc.isDetectable());
		pc.lost(10);
		assertFalse(pc.isDetectable());
		pc.lost(11);
		assertTrue(pc.isDetectable());
	}
	
	@Test
	public void testDetect() {
		PersistentCongestion pc = new PersistentCongestion();
		
		assertFalse(pc.detect(0));
		pc.lost(10);
		assertFalse(pc.detect(0));
		pc.lost(11);
		assertFalse(pc.detect(1));
		assertTrue(pc.detect(0));
		assertFalse(pc.detect(0));
	}

	long[] items(PersistentCongestion pc) throws Exception {
		Field f = PersistentCongestion.class.getDeclaredField("first");
		f.setAccessible(true);
		PersistentCongestion.Item i = (Item) f.get(pc);
		List<Long> items = new ArrayList<>();
		
		while (i != null) {
			items.add(i.sentTime);
			i = i.next;
		}
		long[] array = new long[items.size()];
		for (int j=0; j<items.size(); ++j) {
			array[j] = items.get(j);
		}
		return array;
	}
	
	void assertItems(String expected, PersistentCongestion pc) throws Exception {
		long[] items = items(pc);
		StringBuilder sb = new StringBuilder();
		
		for (long item: items) {
			sb.append(item).append('|');
		}
		assertEquals(expected, sb.toString());
	}
	
	@Test
	public void testLost() throws Exception {
		PersistentCongestion pc = new PersistentCongestion();
		
		assertItems("", pc);
		pc.lost(10);
		assertItems("10|", pc);
		pc.lost(10);
		assertItems("10|", pc);
		pc.lost(9);
		assertItems("9|10|", pc);
		pc.lost(11);
		assertItems("9|10|11|", pc);
		pc.lost(0);
		assertItems("0|9|10|11|", pc);
		pc.lost(1);
		assertItems("0|1|9|10|11|", pc);
		pc.lost(8);
		assertItems("0|1|8|9|10|11|", pc);
		pc.lost(6);
		assertItems("0|1|6|8|9|10|11|", pc);
		pc.lost(20);
		assertItems("0|1|6|8|9|10|11|20|", pc);
		pc.lost(10);
		assertItems("0|1|6|8|9|10|11|20|", pc);		
		
		pc = new PersistentCongestion();
		pc.lost(Long.MAX_VALUE);
		assertItems("9223372036854775807|", pc);
		pc.lost(Long.MAX_VALUE+1);
		assertItems("9223372036854775807|-9223372036854775808|", pc);
		assertTrue(pc.detect(0));

		pc = new PersistentCongestion();
		pc.lost(Long.MAX_VALUE+1);
		assertItems("-9223372036854775808|", pc);
		pc.lost(Long.MAX_VALUE);
		assertItems("9223372036854775807|-9223372036854775808|", pc);
		assertTrue(pc.detect(0));

		pc = new PersistentCongestion();
		pc.lost(Long.MAX_VALUE-1);
		assertItems("9223372036854775806|", pc);
		pc.lost(Long.MAX_VALUE+1);
		assertItems("9223372036854775806|-9223372036854775808|", pc);
		pc.lost(Long.MAX_VALUE);
		assertItems("9223372036854775806|9223372036854775807|-9223372036854775808|", pc);
		assertTrue(pc.detect(1));
	}
	
	@Test
	public void testAcked() throws Exception {
		PersistentCongestion pc = new PersistentCongestion();
		
		pc.acked(0);
		pc.lost(1);
		pc.lost(2);
		pc.lost(3);
		assertItems("1|2|3|", pc);
		pc.acked(1);
		assertItems("2|3|", pc);
		pc.acked(3);
		assertItems("", pc);
		assertFalse(pc.isDetectable());
		pc.lost(1);
		pc.lost(2);
		pc.lost(3);
		assertItems("1|2|3|", pc);
		pc.acked(2);
		assertItems("3|", pc);

		pc.lost(5);
		pc.lost(7);
		assertItems("3|5|7|", pc);
		pc.acked(2);
		assertItems("3|5|7|", pc);
		pc.acked(4);
		assertItems("5|7|", pc);
		pc.lost(9);
		assertItems("5|7|9|", pc);
		pc.acked(8);
		assertItems("9|", pc);
		pc.lost(11);
		pc.lost(13);
		assertItems("9|11|13|", pc);
		pc.acked(14);
		assertItems("", pc);
		assertFalse(pc.isDetectable());

		pc.lost(Long.MAX_VALUE);
		assertItems("9223372036854775807|", pc);
		pc.acked(Long.MAX_VALUE+1);
		assertItems("", pc);

		pc.lost(Long.MAX_VALUE);
		pc.lost(Long.MAX_VALUE+1);
		pc.lost(Long.MAX_VALUE+3);
		assertItems("9223372036854775807|-9223372036854775808|-9223372036854775806|", pc);
		pc.acked(Long.MAX_VALUE+1);
		assertItems("-9223372036854775806|", pc);
	}
	
	@Test
	public void testRfc9002Example() throws Exception {
		PersistentCongestion pc = new PersistentCongestion();
		
		pc.lost(1);
		pc.lost(2);
		pc.lost(3);
		pc.lost(4);
		pc.lost(5);
		pc.lost(6);
		pc.lost(8);
		assertFalse(pc.detect(7));
		assertTrue(pc.detect(6));
		pc.acked(12);
		assertFalse(pc.isDetectable());
	}
}
