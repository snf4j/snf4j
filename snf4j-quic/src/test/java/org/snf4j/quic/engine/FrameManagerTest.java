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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.frame.PingFrame;

public class FrameManagerTest {

	@Test
	public void testAll() throws Exception {
		FrameManager m = new FrameManager();
		
		assertTrue(m.allAcked());
		assertNull(m.peek());
		PingFrame f1 = new PingFrame();
		PingFrame f2 = new PingFrame();
		PingFrame f3 = new PingFrame();
		m.add(f1);
		assertTrue(m.allAcked());
		assertSame(f1, m.peek());
		assertSame(f1, m.peek());
		m.add(f2);
		assertTrue(m.allAcked());
		assertSame(f1, m.peek());
		assertFalse(m.isFlying(0));
		assertNull(m.getFlying(0));
		m.fly(f1, 0);
		assertTrue(m.isFlying(0));
		assertEquals(1, m.getFlying(0).getFrames().size());
		assertSame(f1, m.getFlying(0).getFrames().get(0));
		assertFalse(m.allAcked());
		assertSame(f2, m.peek());
		m.ack(0);
		assertFalse(m.isFlying(0));
		assertNull(m.getFlying(0));
		assertTrue(m.allAcked());
		assertSame(f2, m.peek());
		
		m.fly(f3, 1);
		assertFalse(m.allAcked());
		assertSame(f2, m.peek());
		m.ack(1);
		assertTrue(m.allAcked());
		assertSame(f2, m.peek());
		m.add(f1);
		m.add(f3);
		
		m.fly(f1, 2);
		m.fly(f2, 2);
		m.fly(f3, 2);
		assertFalse(m.allAcked());
		assertNull(m.peek());
		m.ack(2);
		assertTrue(m.allAcked());
		assertNull(m.peek());
		
		try {
			m.ack(3);
			fail();
		}
		catch (QuicException e) {
		}
		
		m = new FrameManager();
		try {
			m.ack(0);
			fail();
		}
		catch (QuicException e) {
		}
		m.fly(f1, 0);
		assertFalse(m.allAcked());
		m.ack(0);
		assertTrue(m.allAcked());
	}
}
