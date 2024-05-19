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
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.snf4j.quic.CommonTest;

public class ConnectionIdTest extends CommonTest {

	@Test
	public void testConstrutors() {
		ConnectionId id = new ConnectionId(0, bytes(""), null);
		assertEquals(0, id.getSequenceNumber());
		assertEquals(0L, id.getCode());
		assertArrayEquals(bytes(""), id.getId());
		assertNull(id.getResetToken());
		
		id = new ConnectionId(1, bytes("ef"), bytes(16));
		assertEquals(1, id.getSequenceNumber());
		assertEquals(0xefL, id.getCode());
		assertArrayEquals(bytes("ef"), id.getId());
		assertArrayEquals(bytes(16), id.getResetToken());
		
		id = new ConnectionId(1, bytes("eeef"), null);
		assertEquals(0xeeefL, id.getCode());
		assertArrayEquals(bytes("eeef"), id.getId());

		id = new ConnectionId(1, bytes("edeeef"), null);
		assertEquals(0xedeeefL, id.getCode());
		assertArrayEquals(bytes("edeeef"), id.getId());

		id = new ConnectionId(1, bytes("ecedeeef"), null);
		assertEquals(0xecedeeefL, id.getCode());
		assertArrayEquals(bytes("ecedeeef"), id.getId());

		id = new ConnectionId(1, bytes("ebecedeeef"), null);
		assertEquals(0xebecedeeefL, id.getCode());
		assertArrayEquals(bytes("ebecedeeef"), id.getId());

		id = new ConnectionId(1, bytes("eaebecedeeef"), null);
		assertEquals(0xeaebecedeeefL, id.getCode());
		assertArrayEquals(bytes("eaebecedeeef"), id.getId());

		id = new ConnectionId(1, bytes("e9eaebecedeeef"), null);
		assertEquals(0xe9eaebecedeeefL, id.getCode());
		assertArrayEquals(bytes("e9eaebecedeeef"), id.getId());

		id = new ConnectionId(1, bytes("e8e9eaebecedeeef"), null);
		assertEquals(0xe8e9eaebecedeeefL, id.getCode());
		assertArrayEquals(bytes("e8e9eaebecedeeef"), id.getId());

		id = new ConnectionId(1, bytes("e7e8e9eaebecedeeef"), null);
		assertEquals(0xe8e9eaebecedeeefL, id.getCode());
		assertArrayEquals(bytes("e7e8e9eaebecedeeef"), id.getId());
	}
	
	@Test
	public void testCloneWith() {
		ConnectionId id = new ConnectionId(4, bytes("0102030405"), null);
		
		ConnectionId cloned = id.cloneWith(bytes(16));
		assertEquals(id.getSequenceNumber(), cloned.getSequenceNumber());
		assertEquals(id.getCode(), cloned.getCode());
		assertArrayEquals(id.getId(), cloned.getId());
		assertArrayEquals(bytes(16), cloned.getResetToken());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidResetToken() {
		new ConnectionId(4, bytes("0102030405"), bytes(15));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testInvalidResetToken2() {
		new ConnectionId(4, bytes("0102030405"), bytes(17));
	}
}
