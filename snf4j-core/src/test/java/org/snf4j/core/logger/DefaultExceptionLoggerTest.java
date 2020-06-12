/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2020 SNF4J contributors
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class DefaultExceptionLoggerTest {
	
	@Test
	public void testLoad() {
		LoggerFactory.getLogger(DefaultExceptionLoggerTest.class);

		String property = "org.snf4j.ExceptionLogger";
		IExceptionLogger l = ExceptionLogger.load();
		
		assertTrue(l instanceof DefaultExceptionLogger);
		assertTrue(ExceptionLogger.getInstance() instanceof DefaultExceptionLogger);
		
		System.setProperty(property, "org.snf4j.XXX");
		l = ExceptionLogger.load();
		assertTrue(l instanceof DefaultExceptionLogger);
		
		System.setProperty(property, this.getClass().getName());
		l = ExceptionLogger.load();
		assertTrue(l instanceof DefaultExceptionLogger);
	
		System.setProperty(property, "org.snf4j.core.logger.TestExceptionLogger");
		l = ExceptionLogger.load();
		assertTrue(l instanceof TestExceptionLogger);
		
	}
	
	@Test
	public void testPrepareArguments() {
		DefaultExceptionLogger l = new DefaultExceptionLogger();
		
		assertNull(l.prepareArguments(null));
		Object[] args = new Object[0];
		assertTrue(args == l.prepareArguments(args));
		args = new Object[] {1};
		assertTrue(args == l.prepareArguments(args));
		args = new Object[] {new Exception("E1")};
		Object[] args2 = l.prepareArguments(args);
		assertTrue(args != args2);
		assertEquals(Arrays.toString(new Object[] {"java.lang.Exception: E1"}), Arrays.toString(args2));
		args = new Object[] {new Integer(1), new Exception("E2")};
		args2 = l.prepareArguments(args);
		assertTrue(args != args2);
		assertEquals(Arrays.toString(new Object[] {new Integer(1), "java.lang.Exception: E2"}), Arrays.toString(args2));
	}
	
	@Test
	public void testLogging() {
		TestLogger log = new TestLogger();
		IExceptionLogger elog = new DefaultExceptionLogger();
		Object[] args = new Object[] {1, new Exception("E1")};
		Object[] expa = new Object[] {1, "java.lang.Exception: E1"};
		String exps = Arrays.toString(expa);
		
		elog.trace(log, "Message", args);
		assertEquals("T|Message|" + exps +"|", log.getLog());
		elog.debug(log, "Message", args);
		assertEquals("D|Message|" + exps +"|", log.getLog());
		elog.info(log, "Message", args);
		assertEquals("I|Message|" + exps +"|", log.getLog());
		elog.warn(log, "Message", args);
		assertEquals("W|Message|" + exps +"|", log.getLog());
		elog.error(log, "Message", args);
		assertEquals("E|Message|" + exps +"|", log.getLog());
	}
}
