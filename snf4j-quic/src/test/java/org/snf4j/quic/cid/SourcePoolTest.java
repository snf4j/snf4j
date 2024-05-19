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
package org.snf4j.quic.cid;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.TestSecureRandom;

public class SourcePoolTest extends CommonTest {

	TestSecureRandom random;
	
	@Override
	public void before() throws Exception {
		super.before();
		random = new TestSecureRandom();
	}
	
	@Test
	public void testGenerate() {
		SourcePool p = new SourcePool(3, false, random);
		assertArrayEquals(bytes("00010203"), p.generate(4));
		assertArrayEquals(bytes("0405"), p.generate(2));
	}
	
	@Test
	public void testIssue() {
		SourcePool p = new SourcePool(4, false, random);
		
		assertEquals(0, p.getSize());
		ConnectionId c1 = p.issue();
		assertEquals(1, p.getSize());
		assertEquals(0, c1.getSequenceNumber());
		assertArrayEquals(bytes("00010203"), c1.getId());
		assertNull(c1.getResetToken());
		
		ConnectionId c2 = p.issue();
		assertEquals(2, p.getSize());
		assertEquals(1, c2.getSequenceNumber());
		assertArrayEquals(bytes("04050607"), c2.getId());
		assertArrayEquals(bytes("08090a0b0c0d0e0f1011121314151617"), c2.getResetToken());
		
		assertNull(p.issue());
		assertEquals(2, p.getSize());

		p.retire(0);
		assertEquals(1, p.getSize());
		c1 = p.issue();
		assertEquals(2, p.getSize());
		assertEquals(2, c1.getSequenceNumber());
		
		random = new TestSecureRandom();
		p = new SourcePool(2, true, random);
		c1 = p.issue();
		assertEquals(0, c1.getSequenceNumber());
		assertArrayEquals(bytes("0001"), c1.getId()); 
		assertArrayEquals(bytes("02030405060708090a0b0c0d0e0f1011"), c1.getResetToken());
	}
	
	@Test
	public void testGet() {
		SourcePool p = new SourcePool(4, false, random);
		
		ConnectionId c1 = p.get(0);
		assertEquals(1, p.getSize());
		assertEquals(0, c1.getSequenceNumber());
		assertArrayEquals(bytes("00010203"), c1.getId());
		assertNull(c1.getResetToken());
		
		assertSame(c1, p.get(0));
		assertNull(p.get(1));
		
		ConnectionId c2 = p.issue();
		assertSame(c2, p.get(1));
		
		random = new TestSecureRandom();
		p = new SourcePool(4, true, random);
		
		c1 = p.get();
		assertEquals(0, c1.getSequenceNumber());
		assertArrayEquals(bytes("00010203"), c1.getId());
		assertArrayEquals(bytes("0405060708090a0b0c0d0e0f10111213"), c1.getResetToken());
		
		assertSame(c1, p.get());
		assertEquals(1, p.getSize());
		p.retire(0);
		assertNull(p.get());
		assertNull(p.get(0));
		c1 = p.issue();
		assertEquals(1, c1.getSequenceNumber());
		assertSame(c1, p.get());
		assertNull(p.get(0));
		assertSame(c1, p.get(1));
	}	
	
	@Test
	public void testLimit() {
		SourcePool p = new SourcePool(4, false, random);
		
		assertEquals(2, p.getLimit());
		assertNotNull(p.issue());
		assertNotNull(p.issue());
		assertNull(p.issue());
		p.setLimit(3);
		assertNotNull(p.issue());
		assertNull(p.issue());
	}
}
