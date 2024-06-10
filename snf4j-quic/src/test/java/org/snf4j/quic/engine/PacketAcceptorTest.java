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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.snf4j.quic.CommonTest;

public class PacketAcceptorTest extends CommonTest {

	@Test
	public void testAccept() throws Exception {
		QuicState s = new QuicState(true);
		PacketAcceptor a = new PacketAcceptor(s);
		
		assertFalse(a.accept(buffer("")));
		assertFalse(a.accept(buffer("c000000001")));
		assertFalse(a.accept(buffer("c00000000200")));
		assertFalse(a.accept(buffer("800000000100")));
		assertFalse(a.accept(buffer("c0000000010200")));
		assertFalse(a.accept(buffer("c000000001020000")));
		assertFalse(a.accept(buffer("8000000000020000")));
		
		assertFalse(a.accept(buffer("400000000000000000")));
		assertFalse(a.accept(buffer("40000000000000000000")));
		assertFalse(a.accept(buffer("3f000000000000000000")));
		
		byte[] id = s.getConnectionIdManager().getSourceId();
		byte[] len = new byte[] {(byte) id.length};
		assertTrue(a.accept(buffer("c000000001" + hex(len) + hex(id))));
		id = id.clone();
		id[0] += 1;
		assertFalse(a.accept(buffer("c000000001" + hex(len) + hex(id))));

		s = new QuicState(false);
		a = new PacketAcceptor(s);
		assertTrue(a.accept(buffer("c000000001080102030405060708")));
		s.getConnectionIdManager().setOriginalId(bytes("0102030405060708"));
		assertTrue(a.accept(buffer("c000000001080102030405060708")));
		assertFalse(a.accept(buffer("c000000001081112131415161718")));
		s.getConnectionIdManager().setRetryId(bytes("1112131415161718"));
		assertTrue(a.accept(buffer("c000000001081112131415161718")));
	}
}
