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
package org.snf4j.core.allocator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class DefaultAllocatorMetricTest {
	
	public static void assertMetric(DefaultAllocatorMetric m, String counters, long max) {
		String[] splitted = null;
		if (counters.indexOf(';') != -1) {
			splitted = counters.split(";");
			assertEquals(8, splitted.length);
		}
		else {
			assertEquals(8, counters.length());
		}
		for (int i=0; i<8; ++i) {
			long expected = Long.parseLong(splitted == null ? ""+counters.charAt(i) : splitted[i]);
			long value = -1;
			
			switch (i) {
			case 0: value = m.getAllocatingCount(); break;
			case 1: value = m.getAllocatedCount(); break;
			case 2: value = m.getReleasingCount(); break;
			case 3: value = m.getReleasedCount(); break;
			case 4: value = m.getEnsureSomeCount(); break;
			case 5: value = m.getEnsureCount(); break;
			case 6: value = m.getReduceCount(); break;
			case 7: value = m.getExtendCount(); break;
			default: 
				fail();
			}
			assertEquals(expected, value);
			assertEquals(max, m.getMaxCapacity());
		}
	}
	
	@Test
	public void testAll() {
		DefaultAllocatorMetric m = new DefaultAllocatorMetric();
		assertMetric(m, "00000000", 0);
		m.allocating(10);
		assertMetric(m, "10000000", 0);
		m.allocated(333);
		assertMetric(m, "11000000", 333);
		m.allocated(332);
		assertMetric(m, "12000000", 333);
		m.allocated(334);
		assertMetric(m, "13000000", 334);
		m.released(11);
		assertMetric(m, "13010000", 334);
		m.releasing(1000);
		assertMetric(m, "13110000", 334);
		m.ensureSome();
		assertMetric(m, "13111000", 334);
		m.ensure();
		assertMetric(m, "13111100", 334);
		m.reduce();
		assertMetric(m, "13111110", 334);
		m.extend();
		assertMetric(m, "13111111", 334);		
	}
}
