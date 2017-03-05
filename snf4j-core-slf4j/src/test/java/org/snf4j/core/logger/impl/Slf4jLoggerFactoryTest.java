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
package org.snf4j.core.logger.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;

public class Slf4jLoggerFactoryTest {
	
	@Before
	public void before() {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "TRACE");
		System.setProperty("org.slf4j.simpleLogger.logFile", "System.err");
		System.setErr(TestPrintStream.getInstance().getStream());
	}
	
	@Test
	public void testGetLogger() throws Exception {
		Slf4jLoggerFactory factory = new Slf4jLoggerFactory();
		
		ILogger l1 = factory.getLogger("LOG1");
		ILogger l2 = factory.getLogger("LOG2");
		
		assertTrue(l1 instanceof Slf4jLogger);
		assertTrue(l1 == factory.getLogger("LOG1"));
		assertFalse(l1 == l2);
		
		Field field = l1.getClass().getDeclaredField("logger");
		
		field.setAccessible(true);
		assertTrue(field.get(l1) instanceof Logger);
		
		
		ILogger l3 = LoggerFactory.getLogger(this.getClass());
		assertTrue(l3 instanceof Slf4jLogger);
		assertTrue(l3 == LoggerFactory.getLogger(this.getClass()));
	}
}
