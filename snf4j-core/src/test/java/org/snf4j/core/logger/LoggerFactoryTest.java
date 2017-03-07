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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LoggerFactoryTest {
	
	@Test
	public void testLoad() {
		LoggerFactory.getLogger(LoggerFactoryTest.class);
		
		String property = "org.snf4j.LoggerFactory";
		ILoggerFactory l = LoggerFactory.load();
		
		assertTrue(l instanceof TestingLoggerFactory);

		System.setProperty(property, "org.snf4j.XXX");
		l = LoggerFactory.load();
		assertTrue(l instanceof NopLoggerFactory);
		
		System.setProperty(property, this.getClass().getName());
		l = LoggerFactory.load();
		assertTrue(l instanceof NopLoggerFactory);

		System.setProperty(property, "org.snf4j.core.logger.TestLoggerFactory");
		l = LoggerFactory.load();
		assertTrue(l instanceof TestLoggerFactory);

	}
}