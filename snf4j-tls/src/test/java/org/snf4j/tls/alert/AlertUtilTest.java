/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.alert;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AlertUtilTest {

	@Test
	public void testPut() {
		AlertUtil.put("xxx", new AlertDescription("xxx", 999999));
	}
	
	@Test
	public void testOf() {
		int count = 0;
		
		for (int i=0; i<256; ++i) {
			AlertDescription desc = AlertDescription.of(i);
			Alert alert = AlertUtil.of(AlertLevel.FATAL, desc);
			
			if (desc.isKnown()) {
				if (!"Alert".equals(alert.getClass().getSimpleName())) {
					++count;
				}
			}
			else {
				assertEquals("Alert", alert.getClass().getSimpleName());
				assertEquals("Received 'unknown' error alert", alert.getMessage());
			}
		}
		assertEquals(27, count);
	}
}
