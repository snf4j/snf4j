/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022 SNF4J contributors
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
package org.snf4j.tls.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.KeyPair;

import javax.crypto.interfaces.DHPublicKey;

import org.junit.Test;

public class DHKeyExchangeTest {

	void assertKeyExchange(DHKeyExchange dh, String algo, int len) throws Exception {
		KeyPair kp1 = dh.generateKeyPair();
		KeyPair kp2 = dh.generateKeyPair();
		
		assertEquals(((DHPublicKey)kp1.getPublic()).getY(), dh.getY(kp1.getPublic()));
		assertTrue(dh.isImplemented());
		assertEquals(len, dh.getPLength());
		assertEquals(algo.toLowerCase(), dh.getAlgorithm());
		
		assertEquals(kp1.getPublic(), dh.generatePublicKey(((DHPublicKey)kp1.getPublic()).getY()));
		
		byte[] s1 = dh.generateSecret(kp1.getPrivate(), kp2.getPublic());
		byte[] s2 = dh.generateSecret(kp2.getPrivate(), kp1.getPublic());
		assertArrayEquals(s1,s2);
		assertEquals(len,s1.length);
	}
	
	@Test
	public void testAll() throws Exception {
		assertKeyExchange(DHKeyExchange.FFDHE2048, "FFDHE2048", 256);
		assertKeyExchange(DHKeyExchange.FFDHE3072, "FFDHE3072", 384);
		assertKeyExchange(DHKeyExchange.FFDHE4096, "FFDHE4096", 512);
		assertKeyExchange(DHKeyExchange.FFDHE6144, "FFDHE6144", 768);
		assertKeyExchange(DHKeyExchange.FFDHE8192, "FFDHE8192", 1024);
	}
}
