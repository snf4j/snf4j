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
package org.snf4j.tls.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;

public class AbstractEngineTaskTest {

	private Exception exception;
	
	private Alert alert;
	
	private StringBuilder trace = new StringBuilder();
	
	String trace() {
		String s = trace.toString();
		trace.setLength(0);
		return s;
	}
	
	@Test
	public void testRun() throws Exception {
		Task task = new Task();
		
		assertFalse(task.isDone());
		assertTrue(task.isSuccessful());
		assertNull(task.cause());
		assertEquals("", trace());
		task.run();
		assertTrue(task.isDone());
		assertTrue(task.isSuccessful());
		assertNull(task.cause());
		assertEquals("E|", trace());
		
		try {
			task.run();
			fail();
		} catch (IllegalStateException e) {}
		
		task = new Task();
		exception = new Exception();
		task.run();
		assertTrue(task.isDone());
		assertFalse(task.isSuccessful());
		assertSame(exception, task.cause());
		assertEquals("E|", trace());
		
		EngineState state = new EngineState(
				MachineState.CLI_INIT, 
				new TestParameters(), 
				new TestHandshakeHandler(),
				new TestHandshakeHandler());

		task = new Task();
		exception = null;
		task.run(state);
		assertEquals("E|F|", trace());
		exception = new Exception();
		task = new Task();
		try {
			task.run(state);
			fail();
		} catch (InternalErrorAlert e) {}
		assertEquals("E|", trace());
		
		task = new Task();
		exception = null;
		alert = new UnexpectedMessageAlert("");
		try {
			task.run(state);
			fail();
		} catch (UnexpectedMessageAlert e) {}
		assertEquals("E|F|", trace());
	}
	
	class Task extends AbstractEngineTask {

		@Override
		public String name() {
			return null;
		}

		@Override
		public boolean isProducing() {
			return false;
		}

		@Override
		public void finish(EngineState state) throws Alert {
			trace.append("F|");
			if (alert != null) {
				throw alert;
			}
		}

		@Override
		void execute() throws Exception {
			trace.append("E|");
			if (exception != null) {
				throw exception;
			}
		}
	}
}
