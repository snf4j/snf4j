/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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
import org.apache.logging.log4j.Logger;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;

public class Log4j2LoggerFactoryTest {

	@Before
	public void before() {
		System.setErr(TestPrintStream.getInstance().getStream());
		System.setProperty("log4j.configurationFile", getClass().getClassLoader().getResource("log4j2.xml").getFile());
	}
	
	@Test
	public void testGetLogger() throws Exception {
		Log4j2LoggerFactory factory = new Log4j2LoggerFactory();
		
		ILogger l1 = factory.getLogger("LOG1");
		ILogger l2 = factory.getLogger("LOG2");
		
		assertTrue(l1 instanceof Log4j2Logger);
		assertTrue(l1 == factory.getLogger("LOG1"));
		assertFalse(l1 == l2);
		
		Field field = l1.getClass().getDeclaredField("logger");
		
		field.setAccessible(true);
		assertTrue(field.get(l1) instanceof Logger);
		
		
		ILogger l3 = LoggerFactory.getLogger(this.getClass());
		assertTrue(l3 instanceof Log4j2Logger);
		assertTrue(l3 == LoggerFactory.getLogger(this.getClass()));
	}
}
