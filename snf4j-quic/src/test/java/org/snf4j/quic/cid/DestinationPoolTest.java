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
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;

public class DestinationPoolTest extends CommonTest {

	@Test
	public void testConstants() {
		assertEquals(0, Pool.INITIAL_SEQUENCE_NUMBER.intValue());
		assertEquals(1, Pool.ALTERNATIVE_SEQUENCE_NUMBER.intValue());
	}
	
	@Test
	public void testLimit() {
		assertEquals(2, new DestinationPool(2).getLimit());
		assertEquals(10, new DestinationPool(10).getLimit());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidLimit() {
		new DestinationPool(1);
	}
	
	@Test
	public void testUpdateResetToken() throws Exception {
		DestinationPool p = new DestinationPool(2);
		
		assertEquals(0, p.getSize());
		assertNull(p.updateResetToken(bytes(16)));
		assertEquals(0, p.getSize());
		assertNull(p.get());
		ConnectionId cid = p.add(bytes("01020304"));
		assertEquals(1, p.getSize());
		assertEquals(0, cid.getSequenceNumber());
		assertArrayEquals(bytes("01020304"), cid.getId());
		assertNull(cid.getResetToken());
		
		cid = p.updateResetToken(bytes(16));
		assertArrayEquals(bytes(16), cid.getResetToken());
		assertEquals(1, p.getSize());
		assertSame(cid, p.get());
		assertSame(cid, p.get(0));
	}
	
	@Test
	public void testAdd() throws Exception {
		DestinationPool p = new DestinationPool(3);
		ConnectionId cid;
		
		assertEquals(0, p.getSize());
		cid = p.add(bytes("01020304"));
		assertEquals(1, p.getSize());
		assertEquals(0, cid.getSequenceNumber());
		assertArrayEquals(bytes("01020304"), cid.getId());
		assertNull(cid.getResetToken());
		assertNull(p.add(bytes("01020304")));
		assertSame(cid, p.retire(0));
		assertEquals(0, p.getSize());
		assertNull(p.add(bytes("01020304")));
		
		cid = p.add(1, bytes("02030405"), bytes(16));
		assertEquals(1, p.getSize());
		assertEquals(1, cid.getSequenceNumber());
		assertArrayEquals(bytes("02030405"), cid.getId());
		assertArrayEquals(bytes(16), cid.getResetToken());
		assertNotNull(p.add(2, bytes("02030406"), bytes(16)));
		assertEquals(2, p.getSize());
		assertNotNull(p.add(3, bytes("02030407"), bytes(16)));
		assertEquals(3, p.getSize());
		assertNull(p.add(3, bytes("02030407"), bytes(16)));
		try {
			p.add(4, bytes("02030408"), bytes(16));
			fail();
		}
		catch (QuicException e) {
			assertSame(TransportError.CONNECTION_ID_LIMIT_ERROR, e.getTransportError());
		}
		
		cid = p.retire(2);
		assertEquals(2, p.getSize());
		assertEquals(2, cid.getSequenceNumber());
		assertNull(p.add(2, bytes("02030406"), bytes(16)));
		assertEquals(2, p.getSize());
		p.add(4, bytes("02030408"), bytes(16));
		assertEquals(3, p.getSize());
	}
}
