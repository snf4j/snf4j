/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.record;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.snf4j.tls.CommonTest;
import org.snf4j.tls.alert.DecryptErrorAlert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.crypto.AESAead;
import org.snf4j.tls.crypto.AeadDecrypt;
import org.snf4j.tls.crypto.AeadEncrypt;

public class RecordTest extends CommonTest {
	
	final byte[] IV = bytes(1,2,3,4,5,6,7,8,9,10,11,12);

	final byte[] KEY = bytes(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16);
	
	final AESAead AEAD = AESAead.AEAD_AES_128_GCM;
	
	Encryptor encryptor;
	Decryptor decryptor;
	
	public void before() throws Exception {
		super.before();
		encryptor = new Encryptor(new AeadEncrypt(AEAD.createKey(KEY),AEAD), IV, 14);
		decryptor = new Decryptor(new AeadDecrypt(AEAD.createKey(KEY),AEAD), IV, 10);
	}
	
	@Test
	public void testCheckForAlert() {
		ByteBuffer dst = ByteBuffer.allocate(100);
		
		dst.limit(7);
		assertTrue(Record.checkForAlert(dst));
		dst.limit(8);
		assertTrue(Record.checkForAlert(dst));
		dst.limit(6);
		assertFalse(Record.checkForAlert(dst));
		dst.limit(5);
		assertFalse(Record.checkForAlert(dst));
		
		dst.limit(5+2+1+0+16);
		assertTrue(Record.checkForAlert(dst, 0, encryptor));
		dst.limit(5+2+1+0+17);
		assertTrue(Record.checkForAlert(dst, 0, encryptor));
		dst.limit(5+2+1+0+15);
		assertFalse(Record.checkForAlert(dst, 0, encryptor));
		dst.limit(5+2+1+1+16);
		assertTrue(Record.checkForAlert(dst, 1, encryptor));
		dst.limit(5+2+1+1+17);
		assertTrue(Record.checkForAlert(dst, 1, encryptor));
		dst.limit(5+2+1+1+15);
		assertFalse(Record.checkForAlert(dst, 1, encryptor));
	}
	
	@Test
	public void testAlert() throws Exception {
		ByteBuffer dst = ByteBuffer.allocate(100);
		
		assertEquals(7, Record.alert(new InternalErrorAlert("x"), dst));
		dst.flip();
		assertEquals(7, dst.remaining());
		byte[] bytes = new byte[7];
		dst.get(bytes);
		assertArrayEquals(bytes(21,3,3,0,2,2,80), bytes);
		
		dst.clear();
		int produced = Record.alert(new InternalErrorAlert("x"), 6, encryptor, dst);
		assertEquals(5,2+1+6+16, produced);
		dst.flip();
		assertEquals(produced, dst.remaining());
	}
	
	@Test
	public void testProtect() throws Exception {
		ByteBuffer content = ByteBuffer.allocate(10);
		ByteBuffer dst = ByteBuffer.allocate(10);
		dst.position(1);
		assertFalse(encryptor.isKeyLimitReached());
		try {
			Record.protect(content, encryptor, dst);
			fail();
		}
		catch (InternalErrorAlert e) {
			assertEquals(0, encryptor.getSequence());
			assertEquals(1, dst.position());
		}
		
		encryptor.nextNonce();
		content.clear();
		dst = ByteBuffer.allocate(100);
		assertFalse(encryptor.isKeyLimitReached());
		int produced = Record.protect(content, encryptor, dst);
		assertFalse(encryptor.isKeyLimitReached());
		assertEquals(5+10+16, produced);
		dst.flip();
		assertEquals(produced, dst.remaining());
		content.clear();
		content.putInt(10).flip();
		Record.protect(content, encryptor, dst);
		assertFalse(encryptor.isKeyLimitReached());
		encryptor.incProcessedBytes(1);
		assertTrue(encryptor.isKeyLimitReached());		
	}
	
	@Test
	public void testProtectArray() throws Exception {
		ByteBuffer content = ByteBuffer.allocate(10);
		ByteBuffer dst = ByteBuffer.allocate(10);
		dst.position(1);
		assertFalse(encryptor.isKeyLimitReached());
		try {
			Record.protect(new ByteBuffer[] {content}, 10, encryptor, dst);
			fail();
		}
		catch (InternalErrorAlert e) {
			assertEquals(0, encryptor.getSequence());
			assertEquals(1, dst.position());
		}
		
		encryptor.nextNonce();
		content.clear();
		dst = ByteBuffer.allocate(100);
		assertFalse(encryptor.isKeyLimitReached());
		int produced = Record.protect(new ByteBuffer[] {content}, 10, encryptor, dst);
		assertFalse(encryptor.isKeyLimitReached());
		assertEquals(5+10+16, produced);
		dst.flip();
		assertEquals(produced, dst.remaining());
		content.clear();
		content.putInt(10).flip();
		Record.protect(new ByteBuffer[] {content}, 4, encryptor, dst);
		assertFalse(encryptor.isKeyLimitReached());
		encryptor.incProcessedBytes(1);
		assertTrue(encryptor.isKeyLimitReached());		
	}

	@Test
	public void testUnprotect() throws Exception {
		ByteBuffer content = ByteBuffer.allocate(10);
		ByteBuffer dst = ByteBuffer.allocate(100);
		Record.protect(content, encryptor, dst);
		dst.flip();
		
		ByteBuffer dup = dst.duplicate();
		content.clear();
		assertFalse(decryptor.isKeyLimitReached());
		assertEquals(10, Record.unprotect(dst, dst.remaining()-5, decryptor, content));
		assertFalse(decryptor.isKeyLimitReached());
		content.flip();
		assertEquals(10, content.remaining());
		decryptor.incProcessedBytes(1);
		assertTrue(decryptor.isKeyLimitReached());
		
		try {
			Record.unprotect(dup, dup.remaining()-5, decryptor, content);
			fail();
		}
		catch (DecryptErrorAlert e) {
			assertEquals(0, content.position());
		}
	}
	
}
