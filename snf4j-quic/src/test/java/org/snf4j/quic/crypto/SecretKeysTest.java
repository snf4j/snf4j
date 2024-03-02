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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import javax.crypto.SecretKey;

import org.junit.Test;

public class SecretKeysTest {

	@Test
	public void testAll() {
		SecretKey k1 = AESHeaderProtection.HP_AES_128.createKey(new byte[16]);
		SecretKey k2 = AESHeaderProtection.HP_AES_128.createKey(new byte[16]);
		
		SecretKeys hpk = new SecretKeys(k1,k2);
		assertSame(k1, hpk.getKey(true));
		assertSame(k2, hpk.getKey(false));
		assertSame(k1, hpk.getClientKey());
		assertSame(k2, hpk.getServerKey());
		hpk.clear();
		assertNull(hpk.getKey(true));
		assertNull(hpk.getKey(false));
		assertNull(hpk.getClientKey());
		assertNull(hpk.getServerKey());
		hpk = new SecretKeys(k1);
		assertSame(k1, hpk.getKey(true));
		assertNull(hpk.getKey(false));
		assertSame(k1, hpk.getClientKey());
		assertNull(hpk.getServerKey());
		hpk.clear();
		assertNull(hpk.getKey(true));
		assertNull(hpk.getKey(false));
		assertNull(hpk.getClientKey());
		assertNull(hpk.getServerKey());
	}
}
