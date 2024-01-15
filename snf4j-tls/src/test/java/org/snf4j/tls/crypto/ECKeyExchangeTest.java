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
package org.snf4j.tls.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;

import org.junit.Test;
import org.snf4j.tls.CommonTest;

public class ECKeyExchangeTest extends CommonTest {

	void assertKeyExchange(ECKeyExchange dh, String algo, int len) throws Exception {
		KeyPair kp1 = dh.generateKeyPair(RANDOM);
		KeyPair kp2 = dh.generateKeyPair(RANDOM);
		
		assertEquals(((ECPublicKey)kp1.getPublic()).getW().getAffineX(), dh.getX(kp1.getPublic()));
		assertEquals(((ECPublicKey)kp1.getPublic()).getW().getAffineY(), dh.getY(kp1.getPublic()));
		assertTrue(dh.isImplemented());
		assertEquals(algo.toLowerCase(), dh.getAlgorithm());
		
		assertEquals(kp1.getPublic(), dh.generatePublicKey(((ECPublicKey)kp1.getPublic()).getW().getAffineX(),((ECPublicKey)kp1.getPublic()).getW().getAffineY()));
		
		byte[] s1 = dh.generateSecret(kp1.getPrivate(), kp2.getPublic(), RANDOM);
		byte[] s2 = dh.generateSecret(kp2.getPrivate(), kp1.getPublic(), RANDOM);
		assertArrayEquals(s1,s2);
		assertEquals(len,s1.length);
	}

	@Test
	public void testAll() throws Exception {
		assertKeyExchange(ECKeyExchange.SECP256R1, "SECP256R1", 32);
		assertKeyExchange(ECKeyExchange.SECP384R1, "SECP384R1", 48);
		assertKeyExchange(ECKeyExchange.SECP521R1, "SECP521R1", 66);
	}

}
