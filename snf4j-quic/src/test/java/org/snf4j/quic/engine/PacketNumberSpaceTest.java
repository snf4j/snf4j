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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.frame.PingFrame;

public class PacketNumberSpaceTest {

	@Test
	public void testNext() {
		PacketNumberSpace s = new PacketNumberSpace(PacketNumberSpace.Type.APPLICATION_DATA);
		
		assertEquals(0, s.next());
		assertEquals(1, s.next());
		assertSame(PacketNumberSpace.Type.APPLICATION_DATA, s.getType());
	}
	
	@Test
	public void testUpdate() throws Exception {
		PacketNumberSpace s = new PacketNumberSpace(PacketNumberSpace.Type.APPLICATION_DATA);
		
		assertEquals(-1, s.getLargestAcked());
		assertEquals(-1, s.getLargestProcessed());
		s.frames().fly(new PingFrame(), 100);
		s.updateAcked(100);
		assertEquals(100, s.getLargestAcked());
		assertEquals(-1, s.getLargestProcessed());
		s.updateAcked(99);
		assertEquals(100, s.getLargestAcked());
		assertEquals(-1, s.getLargestProcessed());
		s.frames().fly(new PingFrame(), 101);
		s.updateAcked(101);
		assertEquals(101, s.getLargestAcked());
		assertEquals(-1, s.getLargestProcessed());
		
		s.updateProcessed(50);
		assertEquals(101, s.getLargestAcked());
		assertEquals(50, s.getLargestProcessed());
		s.updateProcessed(49);
		assertEquals(101, s.getLargestAcked());
		assertEquals(50, s.getLargestProcessed());
		s.updateProcessed(51);
		assertEquals(101, s.getLargestAcked());
		assertEquals(51, s.getLargestProcessed());
		
		try {
			s.updateAcked(102);
			fail();
		}
		catch (QuicException e) {
		}
		s.frames().fly(new PingFrame(), 102);
		s.updateAcked(102);
	}
}
