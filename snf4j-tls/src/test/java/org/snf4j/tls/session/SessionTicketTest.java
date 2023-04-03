/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.session;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.snf4j.tls.CommonTest;
import org.snf4j.tls.cipher.HashSpec;

public class SessionTicketTest extends CommonTest {

	@Test
	public void testAll() throws Exception {
		byte[] psk = new byte[10];
		byte[] ticket = new byte[5];
		
		SessionTicket st = new SessionTicket(HashSpec.SHA256, psk, ticket, 100, 111, 1000);
		assertSame(HashSpec.SHA256, st.getHashSpec());
		assertArrayEquals(psk, st.getPsk());
		assertArrayEquals(ticket, st.getTicket());
		assertEquals(111, st.getAgeAdd());
		assertEquals(1000L, st.getCreationTime());
		assertEquals(-1, st.getMaxEarlyDataSize());
		assertTrue(st.isValid(1000-1+100000));
		assertFalse(st.isValid(1000+100000));
		
		long time = System.currentTimeMillis();
		st = new SessionTicket(HashSpec.SHA384, psk, ticket, 1, 111111);
		assertSame(HashSpec.SHA384, st.getHashSpec());
		assertTrue(st.getCreationTime() >= time);
		assertEquals(111111, st.getAgeAdd());
		assertTrue(st.isValid());
		waitFor(500);
		assertTrue(st.isValid());
		waitFor(600);
		assertFalse(st.isValid());
	}
}
