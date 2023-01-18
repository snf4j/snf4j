/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022-2023 SNF4J contributors
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
package org.snf4j.tls.engine;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.handshake.ServerHello;
import org.snf4j.tls.handshake.ServerHelloRandom;

public class ServerHelloRandomTest {

	@Test
	public void testIsHelloRetryRequest() throws Exception {
		byte[] random = MessageDigest.getInstance("SHA-256").digest("HelloRetryRequest".getBytes());
		
		assertTrue(ServerHelloRandom.isHelloRetryRequest(random));
		assertFalse(ServerHelloRandom.isHelloRetryRequest(Arrays.copyOf(random, 31)));
		assertFalse(ServerHelloRandom.isHelloRetryRequest(Arrays.copyOf(random, 33)));
		
		for (int i=0; i<32; ++i) {
			random[i]++;
			assertFalse(ServerHelloRandom.isHelloRetryRequest(random));
			random[i]--;
		}
		assertTrue(ServerHelloRandom.isHelloRetryRequest(random));
		
		ServerHello sh = new ServerHello(0x0303, random, new byte[32], CipherSuite.TLS_AES_128_CCM_8_SHA256, (byte)0, new ArrayList<IExtension>());
		assertTrue(ServerHelloRandom.isHelloRetryRequest(sh));
		random[3]++;
		assertFalse(ServerHelloRandom.isHelloRetryRequest(sh));		
	}
	
	@Test
	public void testGetHelloRetryRequestRandom() throws Exception {
		byte[] random = MessageDigest.getInstance("SHA-256").digest("HelloRetryRequest".getBytes());
		
		assertArrayEquals(random, ServerHelloRandom.getHelloRetryRequestRandom());
		assertNotSame(ServerHelloRandom.getHelloRetryRequestRandom(), ServerHelloRandom.getHelloRetryRequestRandom());
	}
}
