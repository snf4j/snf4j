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
package org.snf4j.tls.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.snf4j.tls.CommonTest;

public class DerivedSecretsTest extends CommonTest {

	@Test
	public void testAll() {
		byte[] s1 = bytes("1122334455");
		byte[] s2 = bytes("6677889900");
		byte[] zeros = new byte[5];
		
		DerivedSecrets s = new DerivedSecrets(s1,s2);
		assertSame(s1, s.getSecret(true));
		assertSame(s1, s.getClientSecret());
		assertSame(s2, s.getSecret(false));
		assertSame(s2, s.getServerSecret());
		s.clear();
		assertArrayEquals(zeros, s1);
		assertArrayEquals(zeros, s2);

		s1 = bytes("1122334455");
		s = new DerivedSecrets(s1);
		assertSame(s1, s.getSecret(true));
		assertSame(s1, s.getClientSecret());
		assertNull(s.getSecret(false));
		assertNull(s.getServerSecret());
		s.clear();
		assertArrayEquals(zeros, s1);
	}
}
