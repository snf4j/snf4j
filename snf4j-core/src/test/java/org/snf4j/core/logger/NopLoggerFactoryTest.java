/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017 SNF4J contributors
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
package org.snf4j.core.logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NopLoggerFactoryTest {
	
	@Test
	public void testGetLogger() {
		ILogger l = new NopLoggerFactory().getLogger("");
		
		assertTrue(l instanceof NopLogger);
		assertFalse(l.isDebugEnabled());
		assertFalse(l.isTraceEnabled());
		
		l.trace(null);
		l.trace(null,1);
		l.trace(null,1,2);
		l.trace(null,1,2,3);
		l.debug(null);
		l.debug(null,1);
		l.debug(null,1,2);
		l.debug(null,1,2,3);
		l.info(null);
		l.info(null,1);
		l.info(null,1,2);
		l.info(null,1,2,3);
		l.warn(null);
		l.warn(null,1);
		l.warn(null,1,2);
		l.warn(null,1,2,3);
		l.error(null);
		l.error(null,1);
		l.error(null,1,2);
		l.error(null,1,2,3);
	}
}
