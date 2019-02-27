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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Level;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;

public class Log4j2LoggerTest {
	
	@Before
	public void before() {
		System.setErr(TestPrintStream.getInstance().getStream());
		System.setProperty("log4j.configurationFile", getClass().getClassLoader().getResource("log4j2.xml").getFile());
	}

	private void assertLog(String expectedLevel, String expectedMsg, String msg) {
		msg = msg.replace("\n", "").replace("\r", "");
		String s1 = "] " + expectedLevel + " org.snf4j.core.logger.impl.Log4j2LoggerTest - ";
		
		int i = msg.indexOf(s1);
		assertTrue(i != -1);
		assertEquals(expectedMsg, msg.substring(i+s1.length()));
	}
	
	@Test
	public void testLog() {
		ILogger l = LoggerFactory.getLogger(this.getClass());
		
		assertTrue(l.isDebugEnabled());
		assertTrue(l.isTraceEnabled());
	
		TestPrintStream s = TestPrintStream.getInstance();
		
		String level = "DEBUG"; 
		l.debug("Message");
		assertLog(level, "Message", s.getString());
		l.debug("Message {}", 1);
		assertLog(level, "Message 1", s.getString());
		l.debug("Message {},{}", 1, 2);
		assertLog(level, "Message 1,2", s.getString());
		l.debug("Message {},{},{}", 1, 2, 3);
		assertLog(level, "Message 1,2,3", s.getString());
		l.debug("Message {},{},{},{}", 1, 2, 3, 4);
		assertLog(level, "Message 1,2,3,4", s.getString());
		l.debug("Message {},{},{},{},{}", 1, 2, 3, 4, 5);
		assertLog(level, "Message 1,2,3,4,5", s.getString());

		level = "TRACE";
		l.trace("Message");
		assertLog(level, "Message", s.getString());
		l.trace("Message {}", 1);
		assertLog(level, "Message 1", s.getString());
		l.trace("Message {},{}", 1, 2);
		assertLog(level, "Message 1,2", s.getString());
		l.trace("Message {},{},{}", 1, 2, 3);
		assertLog(level, "Message 1,2,3", s.getString());
		l.trace("Message {},{},{},{}", 1, 2, 3, 4);
		assertLog(level, "Message 1,2,3,4", s.getString());
		l.trace("Message {},{},{},{},{}", 1, 2, 3, 4, 5);
		assertLog(level, "Message 1,2,3,4,5", s.getString());

		level = "INFO ";
		l.info("Message");
		assertLog(level, "Message", s.getString());
		l.info("Message {}", 1);
		assertLog(level, "Message 1", s.getString());
		l.info("Message {},{}", 1, 2);
		assertLog(level, "Message 1,2", s.getString());
		l.info("Message {},{},{}", 1, 2, 3);
		assertLog(level, "Message 1,2,3", s.getString());
		l.info("Message {},{},{},{}", 1, 2, 3, 4);
		assertLog(level, "Message 1,2,3,4", s.getString());
		l.info("Message {},{},{},{},{}", 1, 2, 3, 4, 5);
		assertLog(level, "Message 1,2,3,4,5", s.getString());

		level = "WARN ";
		l.warn("Message");
		assertLog(level, "Message", s.getString());
		l.warn("Message {}", 1);
		assertLog(level, "Message 1", s.getString());
		l.warn("Message {},{}", 1, 2);
		assertLog(level, "Message 1,2", s.getString());
		l.warn("Message {},{},{}", 1, 2, 3);
		assertLog(level, "Message 1,2,3", s.getString());
		l.warn("Message {},{},{},{}", 1, 2, 3, 4);
		assertLog(level, "Message 1,2,3,4", s.getString());
		l.warn("Message {},{},{},{},{}", 1, 2, 3, 4, 5);
		assertLog(level, "Message 1,2,3,4,5", s.getString());
		
		level = "ERROR";
		l.error("Message");
		assertLog(level, "Message", s.getString());
		l.error("Message {}", 1);
		assertLog(level, "Message 1", s.getString());
		l.error("Message {},{}", 1, 2);
		assertLog(level, "Message 1,2", s.getString());
		l.error("Message {},{},{}", 1, 2, 3);
		assertLog(level, "Message 1,2,3", s.getString());
		l.error("Message {},{},{},{}", 1, 2, 3, 4);
		assertLog(level, "Message 1,2,3,4", s.getString());
		l.error("Message {},{},{},{},{}", 1, 2, 3, 4, 5);
		assertLog(level, "Message 1,2,3,4,5", s.getString());
	}

}
