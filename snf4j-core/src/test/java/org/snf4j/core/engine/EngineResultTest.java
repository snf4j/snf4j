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
package org.snf4j.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class EngineResultTest {
	
	@Test
	public void testConstructor() {
		EngineResult result = new EngineResult(Status.OK, HandshakeStatus.NEED_UNWRAP, 10, 100);
		
		assertEquals(Status.OK, result.getStatus());
		assertEquals(HandshakeStatus.NEED_UNWRAP, result.getHandshakeStatus());
		assertEquals(10, result.bytesConsumed());
		assertEquals(100, result.bytesProduced());

		result = new EngineResult(Status.OK, HandshakeStatus.NEED_UNWRAP, 0, 0);
		
		assertEquals(0, result.bytesConsumed());
		assertEquals(0, result.bytesProduced());
		
		try {
			new EngineResult(null, HandshakeStatus.NEED_UNWRAP, 10, 100);
			fail("exeption not thrown");
		}
		catch (IllegalArgumentException e) {
			assertEquals("status is null", e.getMessage());
		}

		try {
			new EngineResult(Status.OK, null, 10, 100);
			fail("exeption not thrown");
		}
		catch (IllegalArgumentException e) {
			assertEquals("handshakeStatus is null", e.getMessage());
		}

		try {
			new EngineResult(Status.OK, HandshakeStatus.NEED_UNWRAP, -1, 100);
			fail("exeption not thrown");
		}
		catch (IllegalArgumentException e) {
			assertEquals("bytesConsumed is negative", e.getMessage());
		}

		try {
			new EngineResult(Status.OK, HandshakeStatus.NEED_UNWRAP, 10, -1);
			fail("exeption not thrown");
		}
		catch (IllegalArgumentException e) {
			assertEquals("bytesProduced is negative", e.getMessage());
		}
		
	}
}
