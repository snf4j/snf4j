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
package org.snf4j.quic.cid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;

import org.junit.Test;

public class RetirePoolTest {

	void assertRetired(RetirePool p, boolean queueExists, String s) throws Exception {
		String[] parts = s.split(",");
		
		for (String part: parts) {
			int i;
			boolean retired;
			
			if (part.startsWith("-")) {
				i = Integer.parseInt(part.substring(1));
				retired = true;
			}
			else {
				i = Integer.parseInt(part);
				retired = false;
			}
			assertEquals(retired, p.isRetired(i));
		}
		assertQueue(p, queueExists);
	}
	
	void assertQueue(RetirePool p, boolean exists) throws Exception {
		Field f = RetirePool.class.getDeclaredField("queue");
		f.setAccessible(true);
		if (!exists) {
			assertNull(f.get(p));
		}
		else {
			assertNotNull(f.get(p));
		}
	}
	
	@Test
	public void testRetire() throws Exception {
		RetirePool p = new RetirePool();
		
		assertFalse(p.isRetired(0));
		assertRetired(p, false, "0,1,2,3");
		p.retire(0);
		assertRetired(p, false,  "-0,1,2,3");
		p.retire(2);
		assertRetired(p, true, "-0,1,-2,3");
		p.retire(1);
		assertRetired(p, false, "-0,-1,-2,3,4,5,6,7,8");
		p.retire(5);
		assertRetired(p, true, "-0,-1,-2,3,4,-5,6,7,8");
		p.retire(4);
		assertRetired(p, true, "-0,-1,-2,3,-4,-5,6,7,8");
		p.retire(3);
		assertRetired(p, false, "-0,-1,-2,-3,-4,-5,6,7,8,9,10,11,12");
		p.retire(8);
		assertRetired(p, true, "-5,6,7,-8,9,10,11,12");
		p.retire(10);
		assertRetired(p, true, "-5,6,7,-8,9,-10,11,12");
		p.retire(6);
		assertRetired(p, true, "-5,-6,7,-8,9,-10,11,12");
		p.retire(7);
		assertRetired(p, true, "-5,-6,-7,-8,9,-10,11,12");
		p.retire(9);
		assertRetired(p, false, "-5,-6,-7,-8,-9,-10,11,12");
		
		p.isRetired(10);
		assertRetired(p, false, "-5,-6,-7,-8,-9,-10,11,12");
		p.isRetired(9);
		assertRetired(p, false, "-5,-6,-7,-8,-9,-10,11,12");
	}
}
