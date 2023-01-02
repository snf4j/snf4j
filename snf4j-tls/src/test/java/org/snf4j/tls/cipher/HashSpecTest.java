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
package org.snf4j.tls.cipher;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.security.MessageDigest;

import org.junit.Test;
import org.snf4j.tls.crypto.Hash;

public class HashSpecTest {
	
	@Test
	public void testSha256() throws Exception {
		assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(), HashSpec.SHA256.getEmptyHash());
		assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(), HashSpec.SHA256.getEmptyHash());
		assertEquals("SHA-256", HashSpec.SHA256.getAlgorithm());
		assertEquals(32, HashSpec.SHA256.getHashLength());
		assertSame(Hash.SHA256, HashSpec.SHA256.getHash());
		assertNotSame(HashSpec.SHA256.getEmptyHash(), HashSpec.SHA256.getEmptyHash());
	}

	@Test
	public void testSha348() throws Exception {
		assertArrayEquals(MessageDigest.getInstance("SHA-384").digest(), HashSpec.SHA384.getEmptyHash());
		assertArrayEquals(MessageDigest.getInstance("SHA-384").digest(), HashSpec.SHA384.getEmptyHash());
		assertEquals("SHA-348", HashSpec.SHA384.getAlgorithm());
		assertEquals(48, HashSpec.SHA384.getHashLength());
		assertSame(Hash.SHA384, HashSpec.SHA384.getHash());
		assertNotSame(HashSpec.SHA384.getEmptyHash(), HashSpec.SHA384.getEmptyHash());
	}
}
