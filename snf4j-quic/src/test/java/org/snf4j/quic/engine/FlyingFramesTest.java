/*
* -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FlyingFramesTest {

	@Test
	public void testAll() {
		FlyingFrames ff = new FlyingFrames(112);
		
		assertEquals(112, ff.getPacketNumber());
		assertEquals(0, ff.getFrames().size());
		assertEquals(0, ff.getSentTime());
		assertEquals(0, ff.getSentBytes());
		assertFalse(ff.isAckEliciting());
		assertFalse(ff.isInFlight());
		ff.onSending(12345,111,true,false);
		assertEquals(12345, ff.getSentTime());
		assertEquals(111, ff.getSentBytes());
		assertTrue(ff.isAckEliciting());
		assertFalse(ff.isInFlight());
		ff.onSending(12345,111,false,true);
		assertFalse(ff.isAckEliciting());
		assertTrue(ff.isInFlight());
	}
}