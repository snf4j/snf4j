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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.security.GeneralSecurityException;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.engine.HeaderProtector;
import org.snf4j.tls.crypto.AeadId;

public class CryptorTest extends CommonTest {

	@Test
	public void testConstructor() throws Exception {
		TestProtector p = new TestProtector();
		byte[] iv = new byte[12];
		Cryptor c = new Cryptor(p, iv, 11, 1000);
		assertSame(p, c.getProtector());
		assertEquals(11, c.getExpansion());
	}
	
	@Test
	public void testErase() throws Exception {
		TestProtector p = new TestProtector();
		byte[] iv = bytes("1020304f");
		Cryptor c = new Cryptor(p, iv, 11, 1000);
		
		assertEquals(0, p.eraseCount);
		assertArrayEquals(bytes("1020304f"), iv);
		c.erase(true);
		assertEquals(0, p.eraseCount);
		assertArrayEquals(bytes("00000000"), iv);
		c.erase(true);
		assertEquals(0, p.eraseCount);
		c.erase(false);
		assertEquals(1, p.eraseCount);
		assertSame(p, c.getProtector());
		
		p = new TestProtector();
		iv = bytes("1020304f");
		c = new Cryptor(p, iv, 11, 1000);
		
		c.erase(false);
		assertEquals(1, p.eraseCount);
		assertArrayEquals(bytes("00000000"), iv);
		c.erase(false);
		assertEquals(2, p.eraseCount);
		assertArrayEquals(bytes("00000000"), iv);
		assertSame(p, c.getProtector());
	}

	@Test
	public void testMarkForUpdate() throws Exception {
		Cryptor c = new Cryptor(new TestProtector(), bytes("1020304f"), 11, 1000);
		
		assertFalse(c.isMarkedForUpdate());
		c.markForUpdate();
		assertTrue(c.isMarkedForUpdate());
	}
	
	@Test
	public void testIncPackets() throws Exception {
		Cryptor c = new Cryptor(new TestProtector(), bytes("1020304f"), 11, 1000);
		
		assertFalse(c.isConfidentialityLimitReached());
		c.incPackets(999);
		assertFalse(c.isConfidentialityLimitReached());
		c.incPackets(1);
		assertFalse(c.isConfidentialityLimitReached());
		c.incPackets(1);
		assertTrue(c.isConfidentialityLimitReached());
	}
	
	@Test
	public void testNonce() throws Exception {
		byte[] iv = bytes("cccccccccccccccccccc");
		Cryptor c = new Cryptor(new TestProtector(), iv, 11, 1000);

		assertArrayEquals(bytes("cccccccccccccccccccc"), c.nonce(0));
		assertArrayEquals(bytes("cccccccccccccccccccd"), c.nonce(1));
		assertArrayEquals(bytes("ccccccccccccccccccdd"), c.nonce(0x11));
		assertArrayEquals(bytes("ccccccccccccccccdddd"), c.nonce(0x1111));
		assertArrayEquals(bytes("ccccccccccccccdddddd"), c.nonce(0x111111));
		assertArrayEquals(bytes("ccccccccccccdddddddd"), c.nonce(0x11111111));
		assertArrayEquals(bytes("ccccccccccdddddddddd"), c.nonce(0x1111111111L));
		assertArrayEquals(bytes("ccccccccdddddddddddd"), c.nonce(0x111111111111L));
		assertArrayEquals(bytes("ccccccdddddddddddddd"), c.nonce(0x11111111111111L));
		assertArrayEquals(bytes("ccccdddddddddddddddd"), c.nonce(0x1111111111111111L));
	}
	
	static class TestProtector extends HeaderProtector {

		public TestProtector() throws GeneralSecurityException {
			super(AeadId.AES_128_GCM, new SecretKeySpec(new byte[16], "AES"));
		}

		int eraseCount;
		
		@Override
		public void erase() {
			++eraseCount;
		}
		
	}
	
}
