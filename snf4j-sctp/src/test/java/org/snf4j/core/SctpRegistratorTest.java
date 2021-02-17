/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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
package org.snf4j.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.core.handler.ISctpHandler;

public class SctpRegistratorTest extends SctpTest {
	
	@Test
	public void testRegister() throws Exception {
		SelectorLoop loop = new SelectorLoop();
		
		loop.start();
		try {
			SctpRegistrator.register(loop,new TestSctpChannel(), (SctpSession)null);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("session is null", e.getMessage());
		}
		
		try {
			SctpRegistrator.register(loop,new TestSctpChannel(), (ISctpHandler)null);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("handler is null", e.getMessage());
		}
		
		try {
			SctpRegistrator.register(loop,new TestSctpServerChannel(), null);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("factory is null", e.getMessage());
		}
		loop.stop();
		
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		c.registerHandler = true;
		s.start();
		c.start();
		waitForReady(TIMEOUT);
	}

}
