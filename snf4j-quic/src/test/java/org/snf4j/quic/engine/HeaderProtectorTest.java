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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

import org.junit.Test;
import org.snf4j.quic.crypto.AESHeaderProtection;
import org.snf4j.tls.crypto.AESAead;
import org.snf4j.tls.crypto.AeadId;

public class HeaderProtectorTest {

	@Test
	public void testGetProtection() throws Exception {
		HeaderProtector p = new HeaderProtector(AeadId.AES_128_GCM, new TestKey());
		assertSame(AESHeaderProtection.HP_AES_128, p.getProtection());
		p = new HeaderProtector(AESAead.AEAD_AES_128_GCM, new TestKey());
		assertSame(AESHeaderProtection.HP_AES_128, p.getProtection());
		p = new HeaderProtector(AeadId.AES_256_GCM, new TestKey());
		assertSame(AESHeaderProtection.HP_AES_256, p.getProtection());
		p = new HeaderProtector(AESAead.AEAD_AES_256_GCM, new TestKey());
		assertSame(AESHeaderProtection.HP_AES_256, p.getProtection());
	}
	
	@Test
	public void testDestroy() throws Exception {
		TestKey k = new TestKey();
		HeaderProtector p = new HeaderProtector(AeadId.AES_128_GCM, k);
		assertEquals(0, k.destroyCount);
		p.erase();
		assertEquals(1, k.destroyCount);
		k.exception = new DestroyFailedException();
		p.erase();
		assertEquals(1, k.destroyCount);
	}
	
	class TestKey implements SecretKey {

		private static final long serialVersionUID = 1L;

		DestroyFailedException exception;
		
		int destroyCount;
		
		@Override
		public String getAlgorithm() {
			return null;
		}

		@Override
		public String getFormat() {
			return null;
		}

		@Override
		public byte[] getEncoded() {
			return null;
		}
		
		@Override
	    public void destroy() throws DestroyFailedException {
	    	if (exception != null) {
	    		throw exception;
	    	}
			destroyCount++;
	    }
	}
}
