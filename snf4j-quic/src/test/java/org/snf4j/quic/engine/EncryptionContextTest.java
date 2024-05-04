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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.tls.crypto.AESAead;
import org.snf4j.tls.crypto.IAead;
import org.snf4j.tls.crypto.IAeadDecrypt;
import org.snf4j.tls.crypto.IAeadEncrypt;

public class EncryptionContextTest extends CommonTest {

	HeaderProtector hpe1,hpe2,hpe3,hpd1,hpd2,hpd3;
	
	byte[] iv;
	
	Encryptor e1,e2,e3;
	
	Decryptor d1,d2,d3;
	
	StringBuilder trace = new StringBuilder();
	
	void trace(String s) {
		trace.append(s).append('|');
	}
	
	String trace() {
		String s = trace.toString();
		trace.setLength(0);
		return s;
	}
	
	@Override
	public void before() throws Exception {
		super.before();
		byte[] key = bytes("11223344556677889900112233445566");
		iv = bytes("112233445566778899001122");
		hpe1 = new TestProtector(AESAead.AEAD_AES_128_GCM, AESAead.AEAD_AES_128_GCM.createKey(key),"hpe1");
		hpe2 = new TestProtector(AESAead.AEAD_AES_128_GCM, AESAead.AEAD_AES_128_GCM.createKey(key),"hpe2");
		hpe3 = new TestProtector(AESAead.AEAD_AES_128_GCM, AESAead.AEAD_AES_128_GCM.createKey(key),"hpe3");
		hpd1 = new TestProtector(AESAead.AEAD_AES_128_GCM, AESAead.AEAD_AES_128_GCM.createKey(key),"hpd1");
		hpd2 = new TestProtector(AESAead.AEAD_AES_128_GCM, AESAead.AEAD_AES_128_GCM.createKey(key),"hpd2");
		hpd3 = new TestProtector(AESAead.AEAD_AES_128_GCM, AESAead.AEAD_AES_128_GCM.createKey(key),"hpd3");
		e1 = new Encryptor(new TestEncrypt("e1"),hpe1,iv,1000);
		e2 = new Encryptor(new TestEncrypt("e2"),hpe2,iv,1000);
		e3 = new Encryptor(new TestEncrypt("e3"),hpe3,iv,1000);
		d1 = new Decryptor(new TestDecrypt("d1"),hpd1,iv,1000,1000);
		d2 = new Decryptor(new TestDecrypt("d2"),hpd2,iv,1000,1000);
		d3 = new Decryptor(new TestDecrypt("d3"),hpd3,iv,1000,1000);
		trace();
	}
	
	@Test
	public void testSetEncryptor() {
		EncryptionContext ctx = new EncryptionContext();
		
		ctx.setEncryptor(e1);
		assertSame(e1, ctx.getEncryptor());
		assertSame(e1, ctx.getEncryptor(KeyPhase.CURRENT));
		assertNull(ctx.getEncryptor(KeyPhase.PREVIOUS));
		assertNull(ctx.getEncryptor(KeyPhase.NEXT));
		
		ctx.setEncryptor(e2, KeyPhase.CURRENT);
		assertSame(e2, ctx.getEncryptor());
		assertSame(e2, ctx.getEncryptor(KeyPhase.CURRENT));
		assertNull(ctx.getEncryptor(KeyPhase.PREVIOUS));
		assertNull(ctx.getEncryptor(KeyPhase.NEXT));
		
		ctx.setEncryptor(e1, KeyPhase.PREVIOUS);
		assertSame(e2, ctx.getEncryptor());
		assertSame(e2, ctx.getEncryptor(KeyPhase.CURRENT));
		assertSame(e1,ctx.getEncryptor(KeyPhase.PREVIOUS));
		assertNull(ctx.getEncryptor(KeyPhase.NEXT));

		ctx.setEncryptor(e3, KeyPhase.NEXT);
		assertSame(e2, ctx.getEncryptor());
		assertSame(e2, ctx.getEncryptor(KeyPhase.CURRENT));
		assertSame(e1,ctx.getEncryptor(KeyPhase.PREVIOUS));
		assertSame(e3, ctx.getEncryptor(KeyPhase.NEXT));
	}

	@Test
	public void testSetDecryptor() {
		EncryptionContext ctx = new EncryptionContext();
		
		ctx.setDecryptor(d1);
		assertSame(d1, ctx.getDecryptor());
		assertSame(d1, ctx.getDecryptor(KeyPhase.CURRENT));
		assertNull(ctx.getDecryptor(KeyPhase.PREVIOUS));
		assertNull(ctx.getDecryptor(KeyPhase.NEXT));
		
		ctx.setDecryptor(d2, KeyPhase.CURRENT);
		assertSame(d2, ctx.getDecryptor());
		assertSame(d2, ctx.getDecryptor(KeyPhase.CURRENT));
		assertNull(ctx.getDecryptor(KeyPhase.PREVIOUS));
		assertNull(ctx.getDecryptor(KeyPhase.NEXT));
		
		ctx.setDecryptor(d1, KeyPhase.PREVIOUS);
		assertSame(d2, ctx.getDecryptor());
		assertSame(d2, ctx.getDecryptor(KeyPhase.CURRENT));
		assertSame(d1,ctx.getDecryptor(KeyPhase.PREVIOUS));
		assertNull(ctx.getDecryptor(KeyPhase.NEXT));

		ctx.setDecryptor(d3, KeyPhase.NEXT);
		assertSame(d2, ctx.getDecryptor());
		assertSame(d2, ctx.getDecryptor(KeyPhase.CURRENT));
		assertSame(d1,ctx.getDecryptor(KeyPhase.PREVIOUS));
		assertSame(d3, ctx.getDecryptor(KeyPhase.NEXT));
	}
	
	@Test
	public void testGetDecryptorWithKeyPhaseBit() {
		EncryptionContext ctx = new EncryptionContext();

		ctx.setDecryptor(d1, KeyPhase.PREVIOUS);
		ctx.setDecryptor(d2, KeyPhase.CURRENT);
		ctx.setDecryptor(d3, KeyPhase.NEXT);
		d2.updatePacketNumber(10);
		d2.updatePacketNumber(15);
		
		assertSame(d1, ctx.getDecryptor(true, 0));
		assertSame(d1, ctx.getDecryptor(true, 9));
		assertSame(d3, ctx.getDecryptor(true, 10));
		assertSame(d3, ctx.getDecryptor(true, 15));
		assertSame(d3, ctx.getDecryptor(true, 16));

		assertSame(d2, ctx.getDecryptor(false, 0));
		assertSame(d2, ctx.getDecryptor(false, 9));
		assertSame(d2, ctx.getDecryptor(false, 10));
		assertSame(d2, ctx.getDecryptor(false, 15));
		assertSame(d2, ctx.getDecryptor(false, 16));	
		
		ctx.setDecryptor(null, KeyPhase.PREVIOUS);
		assertSame(d3, ctx.getDecryptor(true, 0));
		assertSame(d3, ctx.getDecryptor(true, 9));
		
		ctx = new EncryptionContext();
		try {
			ctx.getDecryptor(false, 100);
			fail();
		}
		catch (IllegalStateException e) {			
		}
		ctx.setDecryptor(d3);
		try {
			ctx.getDecryptor(true, 100);
			fail();
		}
		catch (IllegalStateException e) {			
		}
	}

	@Test
	public void testGetDecryptorWithKeyPhaseBitAndNoPacketNumers() {
		EncryptionContext ctx = new EncryptionContext();

		ctx.setDecryptor(d1, KeyPhase.PREVIOUS);
		ctx.setDecryptor(d2, KeyPhase.CURRENT);
		ctx.setDecryptor(d3, KeyPhase.NEXT);

		assertSame(d2, ctx.getDecryptor(false, 0));
		assertSame(d2, ctx.getDecryptor(false, 1));
		assertSame(d3, ctx.getDecryptor(true, 0));
		assertSame(d3, ctx.getDecryptor(true, 1));
	}
	
	void assertEncryptors(EncryptionContext ctx, Encryptor... encryptors) {
		assertEquals(encryptors[0], ctx.getEncryptor(KeyPhase.PREVIOUS));
		assertEquals(encryptors[1], ctx.getEncryptor(KeyPhase.CURRENT));
		assertEquals(encryptors[2], ctx.getEncryptor(KeyPhase.NEXT));
	}

	void assertDecryptors(EncryptionContext ctx, Decryptor... decryptors) {
		assertEquals(decryptors[0], ctx.getDecryptor(KeyPhase.PREVIOUS));
		assertEquals(decryptors[1], ctx.getDecryptor(KeyPhase.CURRENT));
		assertEquals(decryptors[2], ctx.getDecryptor(KeyPhase.NEXT));
	}
	
	@Test
	public void testRotateKeys() {
		EncryptionContext ctx = new EncryptionContext();
		
		ctx.setEncryptor(e1, KeyPhase.PREVIOUS);
		ctx.setDecryptor(d1, KeyPhase.PREVIOUS);
		ctx.setEncryptor(e2, KeyPhase.CURRENT);
		ctx.setDecryptor(d2, KeyPhase.CURRENT);
		ctx.setEncryptor(e3, KeyPhase.NEXT);
		ctx.setDecryptor(d3, KeyPhase.NEXT);
		assertFalse(ctx.getKeyPhaseBit());
		assertEquals("", trace());
		ctx.rotateKeys();
		assertTrue(ctx.getKeyPhaseBit());
		assertEquals("d1|e1|e2|", trace());
		assertEncryptors(ctx, null,e3,null);
		assertDecryptors(ctx, d2,d3,null);
		
		ctx = new EncryptionContext();
		ctx.setEncryptor(e2, KeyPhase.CURRENT);
		ctx.setDecryptor(d2, KeyPhase.CURRENT);
		ctx.setEncryptor(e3, KeyPhase.NEXT);
		ctx.setDecryptor(d3, KeyPhase.NEXT);
		assertFalse(ctx.getKeyPhaseBit());
		assertEquals("", trace());
		ctx.rotateKeys();
		assertTrue(ctx.getKeyPhaseBit());
		assertEquals("e2|", trace());
		assertEncryptors(ctx, null,e3,null);
		assertDecryptors(ctx, d2,d3,null);

		ctx.setEncryptor(e1, KeyPhase.NEXT);
		ctx.setDecryptor(d1, KeyPhase.NEXT);
		ctx.rotateKeys();
		assertFalse(ctx.getKeyPhaseBit());
		assertEquals("d2|e3|", trace());
		assertNull(ctx.getEncryptor(KeyPhase.PREVIOUS));
		assertSame(d3, ctx.getDecryptor(KeyPhase.PREVIOUS));
		assertSame(e1, ctx.getEncryptor(KeyPhase.CURRENT));
		assertSame(d1, ctx.getDecryptor(KeyPhase.CURRENT));
		assertNull(ctx.getEncryptor(KeyPhase.NEXT));
		assertNull(ctx.getDecryptor(KeyPhase.NEXT));

		ctx = new EncryptionContext();
		ctx.setEncryptor(e3, KeyPhase.NEXT);
		ctx.setDecryptor(d3, KeyPhase.NEXT);
		assertEquals("", trace());
		ctx.rotateKeys();
		assertTrue(ctx.getKeyPhaseBit());
		assertEquals("", trace());
		assertNull(ctx.getEncryptor(KeyPhase.PREVIOUS));
		assertNull(ctx.getDecryptor(KeyPhase.PREVIOUS));
		assertSame(e3, ctx.getEncryptor(KeyPhase.CURRENT));
		assertSame(d3, ctx.getDecryptor(KeyPhase.CURRENT));
		assertNull(ctx.getEncryptor(KeyPhase.NEXT));
		assertNull(ctx.getDecryptor(KeyPhase.NEXT));

		ctx = new EncryptionContext();
		assertEquals("", trace());
		ctx.rotateKeys();
		assertTrue(ctx.getKeyPhaseBit());
		assertEquals("", trace());
		assertNull(ctx.getEncryptor(KeyPhase.PREVIOUS));
		assertNull(ctx.getDecryptor(KeyPhase.PREVIOUS));
		assertNull(ctx.getEncryptor(KeyPhase.CURRENT));
		assertNull(ctx.getDecryptor(KeyPhase.CURRENT));
		assertNull(ctx.getEncryptor(KeyPhase.NEXT));
		assertNull(ctx.getDecryptor(KeyPhase.NEXT));
	}
	
	@Test
	public void testRotateKeysRealProcess() {
		EncryptionContext ctx = new EncryptionContext();
		
		//phase 0
		assertFalse(ctx.getKeyPhaseBit());
		ctx.setEncryptor(e1, KeyPhase.CURRENT);
		ctx.setDecryptor(d1, KeyPhase.CURRENT);
		assertFalse(ctx.hasNextKeys());
		ctx.setEncryptor(e2, KeyPhase.NEXT);
		assertFalse(ctx.hasNextKeys());
		ctx.setDecryptor(d2, KeyPhase.NEXT);
		assertTrue(ctx.hasNextKeys());
		
		//phase 1
		assertEquals("", trace());
		ctx.rotateKeys();
		assertTrue(ctx.getKeyPhaseBit());
		assertEquals("e1|", trace());
		assertEncryptors(ctx, null, e2, null);
		assertDecryptors(ctx, d1, d2, null);
		assertFalse(ctx.hasNextKeys());
		ctx.setDecryptor(d3, KeyPhase.NEXT);
		assertFalse(ctx.hasNextKeys());
		ctx.setEncryptor(e3, KeyPhase.NEXT);
		assertTrue(ctx.hasNextKeys());
		
		//phase 0
		assertEquals("", trace());
		ctx.rotateKeys();
		assertFalse(ctx.getKeyPhaseBit());
		assertEquals("d1|e2|", trace());
		assertEncryptors(ctx, null, e3, null);
		assertDecryptors(ctx, d2, d3, null);
		ctx.setDecryptor(d1, KeyPhase.NEXT);
		ctx.setEncryptor(e1, KeyPhase.NEXT);
		assertEncryptors(ctx, null, e3, e1);
		assertDecryptors(ctx, d2, d3, d1);
	}
	
	@Test
	public void testErase1() throws Exception {
		EncryptionContext ctx = new EncryptionContext();
		
		ctx.setEncryptor(e1, KeyPhase.PREVIOUS);
		ctx.setEncryptor(e2, KeyPhase.CURRENT);
		ctx.setEncryptor(e3, KeyPhase.NEXT);
		ctx.setDecryptor(d1, KeyPhase.PREVIOUS);
		ctx.setDecryptor(d2, KeyPhase.CURRENT);
		ctx.setDecryptor(d3, KeyPhase.NEXT);
		assertFalse(ctx.isErased());
		ctx.erase();
		assertTrue(ctx.isErased());
		assertEquals("hpe1|e1|hpd1|d1|e2|d2|e3|d3|", trace());
		assertEncryptors(ctx, null, null, null);
		assertDecryptors(ctx, null, null, null);
	}

	@Test
	public void testErase2() throws Exception {
		EncryptionContext ctx = new EncryptionContext();
		
		ctx.setEncryptor(e2, KeyPhase.CURRENT);
		ctx.setEncryptor(e3, KeyPhase.NEXT);
		ctx.setDecryptor(d2, KeyPhase.CURRENT);
		ctx.setDecryptor(d3, KeyPhase.NEXT);
		ctx.erase();
		assertEquals("hpe2|e2|hpd2|d2|e3|d3|", trace());
		assertEncryptors(ctx, null, null, null);
		assertDecryptors(ctx, null, null, null);
	}

	@Test
	public void testErase3() throws Exception {
		EncryptionContext ctx = new EncryptionContext();
		
		ctx.setEncryptor(e3, KeyPhase.NEXT);
		ctx.setDecryptor(d3, KeyPhase.NEXT);
		ctx.erase();
		assertEquals("hpe3|e3|hpd3|d3|", trace());
		assertEncryptors(ctx, null, null, null);
		assertDecryptors(ctx, null, null, null);
		ctx.erase();
		assertEquals("", trace());
	}
	
	@Test
	public void testBuffer() throws Exception {
		EncryptionContext ctx = new EncryptionContext();
		
		for (int i=0; i<11; ++i) {
			ctx.getBuffer().put(bytes(i));
		}
		for (int i=0; i<10; ++i) {
			assertEquals(i, ctx.getBuffer().get().length);
		}
		assertNull(ctx.getBuffer().get());
		
		ctx = new EncryptionContext(2);
		ctx.getBuffer().put(bytes(0));
		ctx.getBuffer().put(bytes(1));
		ctx.getBuffer().put(bytes(2));
		assertEquals(0, ctx.getBuffer().get().length);
		assertEquals(1, ctx.getBuffer().get().length);
		assertNull(ctx.getBuffer().get());
		
		PacketBuffer buf = ctx.getBuffer();
		ctx.erase();
		assertNotSame(buf, ctx.getBuffer());
	}
	
	class TestEncrypt implements IAeadEncrypt {

		String id;
		
		TestEncrypt(String id) {
			this.id = id;
		}
		
		@Override
		public IAead getAead() {
			return AESAead.AEAD_AES_128_GCM;
		}

		@Override
		public byte[] encrypt(byte[] nonce, byte[] additionalData, byte[] plaintext) throws GeneralSecurityException {
			return null;
		}

		@Override
		public void encrypt(byte[] nonce, byte[] additionalData, ByteBuffer plaintext, ByteBuffer ciphertext)
				throws GeneralSecurityException {
		}

		@Override
		public void encrypt(byte[] nonce, byte[] additionalData, ByteBuffer[] plaintext, ByteBuffer ciphertext)
				throws GeneralSecurityException {
		}

		@Override
		public void erase() {
			trace(id);
		}	
	}

	class TestDecrypt implements IAeadDecrypt {

		String id;
		
		TestDecrypt(String id) {
			this.id = id;
		}
		
		@Override
		public IAead getAead() {
			return AESAead.AEAD_AES_128_GCM;
		}

		@Override
		public byte[] decrypt(byte[] nonce, byte[] additionalData, byte[] ciphertext) throws GeneralSecurityException {
			return null;
		}

		@Override
		public void decrypt(byte[] nonce, byte[] additionalData, ByteBuffer ciphertext, ByteBuffer plaintext)
				throws GeneralSecurityException {
		}

		@Override
		public void erase() {
			trace(id);
		}
	}
	
	class TestProtector extends HeaderProtector {

		String id;

		public TestProtector(IAead aead, SecretKey key, String id) throws GeneralSecurityException {
			super(aead, key);
			this.id = id;
		}
		
		@Override
		public void erase() {
			super.erase();
			trace(id);
		}
	}
}
