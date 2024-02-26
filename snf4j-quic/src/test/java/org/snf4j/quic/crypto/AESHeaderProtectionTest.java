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
package org.snf4j.quic.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.junit.Test;
import org.snf4j.quic.CommonTest;

public class AESHeaderProtectionTest extends CommonTest {

	@Test
	public void testIsImplemented() {
		assertTrue(AESHeaderProtection.HP_AES_128.isImplemented());
		assertTrue(AESHeaderProtection.HP_AES_256.isImplemented());
	}
	
	@Test
	public void testGetKeyLength() {
		assertEquals(16, AESHeaderProtection.HP_AES_128.getKeyLength());
		assertEquals(32, AESHeaderProtection.HP_AES_256.getKeyLength());
	}
	
	@Test
	public void testDeriveMask() throws Exception {
		AESHeaderProtection hp = AESHeaderProtection.HP_AES_128;
		Cipher c = hp.createCipher();
		SecretKey k = hp.createKey(bytes("9f50449e04a0e810283a1e9933adedd2"));
		
		byte[] mask = hp.deriveMask(c, k, bytes("d1b1c98dd7689fb8ec11d242b123dc9b"), 0);
		assertArrayEquals(bytes("437b9aec36"), Arrays.copyOf(mask, 5));
		mask = hp.deriveMask(c, k, bytes("d1b1c98dd7689fb8ec11d242b123dc9b"), 0);
		assertArrayEquals(bytes("437b9aec36"), Arrays.copyOf(mask, 5));
		mask = hp.deriveMask(c, k, bytes("d1b1c98dd7689fb8ec11d242b123dc9b00"), 0);
		assertArrayEquals(bytes("437b9aec36"), Arrays.copyOf(mask, 5));
		mask = hp.deriveMask(c, k, bytes("00d1b1c98dd7689fb8ec11d242b123dc9b00"), 1);
		assertArrayEquals(bytes("437b9aec36"), Arrays.copyOf(mask, 5));
	}
}
