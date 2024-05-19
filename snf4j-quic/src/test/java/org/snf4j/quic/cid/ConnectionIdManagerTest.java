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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.SecureRandom;
import java.util.Arrays;

import org.junit.Test;
import org.snf4j.quic.CommonTest;

public class ConnectionIdManagerTest extends CommonTest {

	final SecureRandom RANDOM = new SecureRandom();
	
	void assertArrayNotEquals(byte[] a1, byte[] a2) {
		assertFalse(Arrays.equals(a1, a2));
	}
	
	@Test
	public void testConnectionIdsInHandshake() throws Exception {
		ConnectionIdManager cli = new ConnectionIdManager(true, 4, 2, RANDOM);
		ConnectionIdManager srv = new ConnectionIdManager(false, 8, 2, RANDOM);
		byte[] d,s;
		
		//client
		byte[] s1 = cli.getDestinationId();
		byte[] c1 = cli.getSourceId();
		assertEquals(8, s1.length);
		assertEquals(4, c1.length);
		d = s1;
		s = c1;
		
		//server
		srv.setOriginalId(d);
		srv.getDestinationPool().add(0, s, null);
		d = srv.getDestinationId();
		byte[] s3 = srv.getSourceId();
		s = s3;
		assertArrayEquals(c1, d);
		assertArrayEquals(s1, srv.getOriginalId());
		assertArrayNotEquals(s3, s1);

		//client
		cli.getDestinationPool().add(0, s, null);
		d = cli.getDestinationId();
		s = cli.getSourceId();
		assertArrayEquals(d, s3);
		assertArrayEquals(s, c1);

		//server
		d = srv.getDestinationId();
		s = srv.getSourceId();
		assertArrayEquals(d, c1);
		assertArrayEquals(s, s3);
	}
	
	@Test
	public void testConnectionIdsInHandshakeWithRetry() throws Exception {
		ConnectionIdManager cli = new ConnectionIdManager(true, 0, 2, RANDOM);
		ConnectionIdManager srv = new ConnectionIdManager(false, 8, 2, RANDOM);
		byte[] d,s;
		
		//client
		assertFalse(cli.hasOriginalId());
		byte[] s1 = cli.getDestinationId();
		assertTrue(cli.hasOriginalId());
		byte[] c1 = cli.getSourceId();
		assertEquals(8, s1.length);
		assertEquals(0, c1.length);
		d = s1;
		s = c1;
		
		//server
		srv.setOriginalId(d);
		assertFalse(srv.hasRetryId());
		byte[] s2 = srv.getRetryId();
		assertTrue(srv.hasRetryId());
		srv.getDestinationPool().add(0, s, null);
		d = srv.getDestinationId();
		s = srv.getRetryId();
		assertArrayEquals(c1, d);
		assertArrayEquals(s1, srv.getOriginalId());
		assertArrayEquals(s, s2);
		assertArrayNotEquals(s2, s1);
		
		//client
		assertFalse(cli.hasRetryId());
		cli.setRetryId(s);
		assertTrue(cli.hasRetryId());
		d = cli.getDestinationId();
		s = cli.getSourceId();
		assertArrayEquals(c1, s);
		assertArrayEquals(s2, d);
		assertArrayEquals(s2, cli.getRetryId());
		
		//server
		srv.getDestinationPool().add(0, s, null);
		d = srv.getDestinationId();
		byte[] s3 = srv.getSourceId();
		s = s3;
		assertArrayEquals(c1, d);
		assertArrayEquals(s1, srv.getOriginalId());
		assertArrayNotEquals(s3, s1);

		//client
		cli.getDestinationPool().add(0, s, null);
		d = cli.getDestinationId();
		s = cli.getSourceId();
		assertArrayEquals(d, s3);
		assertArrayEquals(s, c1);

		//server
		d = srv.getDestinationId();
		s = srv.getSourceId();
		assertArrayEquals(d, c1);
		assertArrayEquals(s, s3);
	}	
}
