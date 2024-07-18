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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.junit.Test;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.frame.IFrame;
import org.snf4j.quic.frame.PingFrame;

public class FrameManagerTest {

	@Test
	public void testAll() throws Exception {
		FrameManager m = new FrameManager();
		
		assertTrue(!m.hasFlying());
		assertNull(m.peek());
		PingFrame f1 = new PingFrame();
		PingFrame f2 = new PingFrame();
		PingFrame f3 = new PingFrame();
		m.add(f1);
		assertTrue(!m.hasFlying());
		assertSame(f1, m.peek());
		assertSame(f1, m.peek());
		m.add(f2);
		assertTrue(!m.hasFlying());
		assertSame(f1, m.peek());
		assertFalse(m.isFlying(0));
		assertNull(m.getFlying(0));
		m.fly(f1, 0);
		assertTrue(m.isFlying(0));
		assertEquals(1, m.getFlying(0).getFrames().size());
		assertSame(f1, m.getFlying(0).getFrames().get(0));
		assertFalse(!m.hasFlying());
		assertSame(f2, m.peek());
		m.ack(0);
		assertFalse(m.isFlying(0));
		assertNull(m.getFlying(0));
		assertTrue(!m.hasFlying());
		assertSame(f2, m.peek());
		
		m.fly(f3, 1);
		assertFalse(!m.hasFlying());
		assertSame(f2, m.peek());
		m.ack(1);
		assertTrue(!m.hasFlying());
		assertSame(f2, m.peek());
		m.add(f1);
		m.add(f3);
		
		m.fly(f1, 2);
		m.fly(f2, 2);
		m.fly(f3, 2);
		assertFalse(!m.hasFlying());
		assertNull(m.peek());
		m.ack(2);
		assertTrue(!m.hasFlying());
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
		assertFalse(!m.hasFlying());
		m.ack(0);
		assertTrue(!m.hasFlying());
	}
	
	@Test
	public void testLost() {
		FrameManager m = new FrameManager();
		PingFrame f1 = new PingFrame();
		PingFrame f2 = new PingFrame();
		PingFrame f3 = new PingFrame();

		
		assertEquals(0, m.getLost().size());
		m.fly(f1, 0);
		m.fly(f2, 0);
		m.fly(f3, 1);
		assertTrue(m.hasFlying());
		assertEquals(0, m.getLost().size());
		FlyingFrames lost = m.lost(0);
		assertEquals(0, lost.getPacketNumber());
		assertEquals(2, lost.getFrames().size());
		assertNull(m.getFlying(0));
		assertNotNull(m.getFlying(1));
		assertTrue(lost.getFrames().contains(f1));
		assertTrue(lost.getFrames().contains(f2));
		assertTrue(m.hasFlying());
		assertEquals(2, m.getLost().size());
		assertTrue(m.getLost().contains(f1));
		assertTrue(m.getLost().contains(f2));
		
		assertNull(m.lost(2));
		
		lost = m.lost(1);
		assertEquals(1, lost.getPacketNumber());
		assertEquals(1, lost.getFrames().size());
		assertFalse(m.hasFlying());
		assertEquals(3, m.getLost().size());
		
		Iterator<IFrame> i = m.getLost().iterator();
		i.next();
		i.remove();
		assertEquals(2, m.getLost().size());
	}
}
