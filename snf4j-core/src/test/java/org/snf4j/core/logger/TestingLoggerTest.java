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

import java.io.PrintStream;
import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestingLoggerTest {

	PrintStream original;
	
	@Before
	public void before() {
		original = System.err;
		System.setErr(TestPrintStream.getInstance().getStream());
		TestPrintStream.getInstance().getString();
	}
	
	@After
	public void after() {
		System.setErr(original);
	}
	
	private void assertLog(String expectedLevel, String expectedMsg, String msg) {
		msg = msg.replace("\n", "").replace("\r", "");
		String s1 = "] " + expectedLevel + " org.snf4j.core.logger.TestingLoggerTest - ";
		
		int i = msg.indexOf(s1);
		assertTrue(i != -1);
		assertEquals(expectedMsg, msg.substring(i+s1.length()));
	}

	private void assertLog(String expectedMsg, String msg) {
		msg = msg.replace("\n", "").replace("\r", "");
		assertEquals(expectedMsg, msg);
	}

	@Test
	public void testRecording() {
		TestingLogger.disableRecording();
		assertNull(TestingLogger.getRecording());
		TestingLogger.enableRecording();
		
		ILogger l = new TestingLogger(this.getClass().getName());
		l.trace("Message");
		Object[] recording = TestingLogger.getRecording();
		assertEquals(1, recording.length);
		assertEquals("[TRACE] Message", recording[0]);
		TestingLogger.disableRecording();
		assertNull(TestingLogger.getRecording());
	}
	
	@Test
	public void testLog() throws Exception {
		ILogger l = new TestingLogger(this.getClass().getName());
		
		Field f = l.getClass().getDeclaredField("skipLogging");
		f.setAccessible(true);
		f.set(l, false);
		
		TestPrintStream s = TestPrintStream.getInstance();

		assertTrue(l.isDebugEnabled());
		assertTrue(l.isTraceEnabled());
		
		String level = "TRACE";
		l.trace("Message");
		assertLog(level, "Message", s.getString());
		l.trace("Message {}", 1);
		assertLog(level, "Message {} [1]", s.getString());
		l.trace("Message {} {}", 1, 2);
		assertLog(level, "Message {} {} [1] [2]", s.getString());
		l.trace("Message {} {} {}", 1, null, 3);
		assertLog(level, "Message {} {} {} [1] [null] [3]", s.getString());

		level = "DEBUG";
		l.debug("Message");
		assertLog(level, "Message", s.getString());
		l.debug("Message {}", 1);
		assertLog(level, "Message {} [1]", s.getString());
		l.debug("Message {} {}", 1, 2);
		assertLog(level, "Message {} {} [1] [2]", s.getString());
		l.debug("Message {} {} {}", 1, null, 3);
		assertLog(level, "Message {} {} {} [1] [null] [3]", s.getString());

		level = " INFO";
		l.info("Message");
		assertLog(level, "Message", s.getString());
		l.info("Message {}", 1);
		assertLog(level, "Message {} [1]", s.getString());
		l.info("Message {} {}", 1, 2);
		assertLog(level, "Message {} {} [1] [2]", s.getString());
		l.info("Message {} {} {}", 1, null, 3);
		assertLog(level, "Message {} {} {} [1] [null] [3]", s.getString());

		level = " WARN";
		l.warn("Message");
		assertLog(level, "Message", s.getString());
		l.warn("Message {}", 1);
		assertLog(level, "Message {} [1]", s.getString());
		l.warn("Message {} {}", 1, 2);
		assertLog(level, "Message {} {} [1] [2]", s.getString());
		l.warn("Message {} {} {}", 1, null, 3);
		assertLog(level, "Message {} {} {} [1] [null] [3]", s.getString());

		level = "ERROR";
		l.error("Message");
		assertLog(level, "Message", s.getString());
		l.error("Message {}", 1);
		assertLog(level, "Message {} [1]", s.getString());
		l.error("Message {} {}", 1, 2);
		assertLog(level, "Message {} {} [1] [2]", s.getString());
		l.error("Message {} {} {}", 1, null, 3);
		assertLog(level, "Message {} {} {} [1] [null] [3]", s.getString());

		f.set(l, true);
		l.error("Message");
		assertEquals("", s.getString());
		l.error("Message {}", 1);
		assertEquals("", s.getString());
		l.error("Message {} {}", 1, 2);
		assertEquals("", s.getString());
		l.error("Message {} {} {}", 1, null, 3);
		assertEquals("", s.getString());

		l.error("Message {}");
		assertLog("SNF4J: Wrong number of arguments for log message: [Message {}]", s.getString());
		l.error("Message {}", 1, 2);
		assertLog("SNF4J: Wrong number of arguments for log message: [Message {}]", s.getString());
		l.error("Message {} {}", 1);
		assertLog("SNF4J: Wrong number of arguments for log message: [Message {} {}]", s.getString());
		l.error("Message {} {}", 1, 2, 3);
		assertLog("SNF4J: Wrong number of arguments for log message: [Message {} {}]", s.getString());
		l.error("Message {} {} {}", 1, 2);
		assertLog("SNF4J: Wrong number of arguments for log message: [Message {} {} {}]", s.getString());
		l.error("Message {} {} {}", 1, 2, 3, 4);
		assertLog("SNF4J: Wrong number of arguments for log message: [Message {} {} {}]", s.getString());
		
	}
}
