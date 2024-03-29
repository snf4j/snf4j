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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.snf4j.tls.CommonTest;
import org.snf4j.tls.cipher.CipherSuite;

public class SessionTicketTest extends CommonTest {

	@Test
	public void testAll() throws Exception {
		byte[] psk = new byte[10];
		byte[] ticket = new byte[5];
		
		SessionTicket st = new SessionTicket(CipherSuite.TLS_AES_128_GCM_SHA256, null, psk, ticket, 100, 111, 666, 1000);
		assertSame(CipherSuite.TLS_AES_128_GCM_SHA256, st.getCipherSuite());
		assertArrayEquals(psk, st.getPsk());
		assertArrayEquals(ticket, st.getTicket());
		assertEquals(111, st.getAgeAdd());
		assertEquals(1000L, st.getCreationTime());
		assertEquals(666, st.getMaxEarlyDataSize());
		assertTrue(st.isValid(1000-1+100000));
		assertFalse(st.isValid(1000+100000));
		assertNull(st.getProtocol());
		
		long time = System.currentTimeMillis();
		st = new SessionTicket(CipherSuite.TLS_AES_256_GCM_SHA384, "proto", psk, ticket, 1, 111111, -1);
		assertSame(CipherSuite.TLS_AES_256_GCM_SHA384, st.getCipherSuite());
		assertTrue(st.getCreationTime() >= time);
		assertEquals(111111, st.getAgeAdd());
		assertTrue(st.isValid());
		waitFor(500);
		assertTrue(st.isValid());
		waitFor(600);
		assertFalse(st.isValid());
		assertEquals(-1, st.getMaxEarlyDataSize());
		assertEquals("proto", st.getProtocol());
	}
	
	@Test
	public void testForEarlyData() {
			SessionTicket t = new SessionTicket(CipherSuite.TLS_AES_128_GCM_SHA256, null, null, null, 1, 1, 1, 1);
			
			assertTrue(t.forEarlyData(null));
			assertFalse(t.forEarlyData("proto"));
			assertTrue(t.forEarlyData());
			
			t = new SessionTicket(CipherSuite.TLS_AES_128_GCM_SHA256, "proto", null, null, 1, 1, 1, 1);
			assertFalse(t.forEarlyData(null));
			assertTrue(t.forEarlyData("proto"));

			t = new SessionTicket(CipherSuite.TLS_AES_128_GCM_SHA256, null, null, null, 1, 1, 0, 1);
			assertFalse(t.forEarlyData(null));
			assertFalse(t.forEarlyData("proto"));
			assertFalse(t.forEarlyData());
			
			t = new SessionTicket(CipherSuite.TLS_AES_128_GCM_SHA256, "proto", null, null, 1, 1, 0, 1);
			assertFalse(t.forEarlyData(null));
			assertFalse(t.forEarlyData("proto"));

			t = new SessionTicket(CipherSuite.TLS_AES_128_GCM_SHA256, null, null, null, 1, 1, -1, 1);
			assertFalse(t.forEarlyData(null));
			assertFalse(t.forEarlyData("proto"));
			assertFalse(t.forEarlyData());
			
			t = new SessionTicket(CipherSuite.TLS_AES_128_GCM_SHA256, "proto", null, null, 1, 1, -1, 1);
			assertFalse(t.forEarlyData(null));
			assertFalse(t.forEarlyData("proto"));
		
	}
}
