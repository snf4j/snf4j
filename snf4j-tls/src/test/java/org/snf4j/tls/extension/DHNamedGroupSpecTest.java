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
package org.snf4j.tls.extension;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import org.junit.Test;
import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.alert.AlertDescription;
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.crypto.DHKeyExchange;

public class DHNamedGroupSpecTest extends ExtensionTest {

	@Test
	public void testGetData() throws Exception {
		DHNamedGroupSpec spec = DHNamedGroupSpec.FFDHE2048;
		byte[] y = bytes(1,2,3);
		spec.getData(buffer, y);
		assertArrayEquals(cat(
				bytes(253,(byte)0,(byte)0,(byte)0),y
				), buffer());
		buffer.clear();
		
		y = bytes(256,(byte)1,(byte)2,(byte)3);
		spec.getData(buffer, y);
		assertArrayEquals(y, buffer());
		buffer.clear();
		
		spec.getData(buffer, cat(bytes(0),y));
		assertArrayEquals(y, buffer());
		buffer.clear();

		spec.getData(buffer, cat(bytes(0,0,0),y));
		assertArrayEquals(y, buffer());
		buffer.clear();

		KeyPair kp1 = DHKeyExchange.FFDHE2048.generateKeyPair();
		spec.getData(buffer, kp1.getPublic());
		byte[] data = buffer();
		ParsedKey pk = spec.parse(ByteBufferArray.wrap(array(data, 0)), data.length);
		buffer.clear();
		spec.getData(buffer, pk);
		assertArrayEquals(data, buffer());
		
		try {
			spec.getData(buffer, cat(bytes(0,0,1),y));
			fail();
		} catch (IllegalArgumentException e) {};

		try {
			spec.getData(buffer, cat(bytes(1,0,0),y));
			fail();
		} catch (IllegalArgumentException e) {};
		
	}
	
	@Test
	public void testGetDataLength() {
		DHNamedGroupSpec spec = new DHNamedGroupSpec(DHKeyExchange.FFDHE2048);
		assertEquals(256, spec.getDataLength());
	}
	
	@Test
	public void testParse() throws Exception {
		KeyPair kp1 = DHKeyExchange.FFDHE2048.generateKeyPair(); 
		DHNamedGroupSpec spec = DHNamedGroupSpec.FFDHE2048;
		
		spec.getData(buffer, kp1.getPublic());
		byte[] data = buffer();
		ParsedKey pk = spec.parse(ByteBufferArray.wrap(array(data,0)), data.length);
		assertEquals(kp1.getPublic(), spec.generateKey(pk));
		assertTrue(spec.isImplemented());
	}

	void assertFailure(DHNamedGroupSpec spec, byte[] data, int remaining, AlertDescription desc, String message) {
		try {
			ParsedKey key = spec.parse(ByteBufferArray.wrap(array(data,0)), remaining);
			spec.generateKey(key);
			fail();
		} catch (AlertException e) {
			assertSame(desc, e.getDescription());
			assertEquals(message, e.getMessage());
		}
	}

	@Test
	public void testParseFailures() throws Exception {
		KeyPair kp1 = DHKeyExchange.FFDHE2048.generateKeyPair();
		DHNamedGroupSpec spec = DHNamedGroupSpec.FFDHE2048;
		spec.getData(buffer, kp1.getPublic());
		byte[] data = buffer();

		assertFailure(spec, data, data.length-1, AlertDescription.DECODE_ERROR, 
				"Extension 'key_share' parsing failure: DH key exchange unexpected size");
		assertFailure(new TestDHSpec(new NoSuchAlgorithmException()), data, data.length, 
				AlertDescription.INTERNAL_ERROR, "Extension 'key_share' internal failure: No DH algorithm");
		assertFailure(new TestDHSpec(new InvalidKeySpecException()), data, data.length, 
				AlertDescription.INTERNAL_ERROR, "Extension 'key_share' internal failure: Invalid DH key specification");
		assertFailure(new TestDHSpec(new Exception()), data, data.length, 
				AlertDescription.INTERNAL_ERROR, "Extension 'key_share' internal failure: DH key generation failure");

	}	
	
	static class TestDHSpec extends DHNamedGroupSpec {

		public TestDHSpec(Exception e) {
			super(new TestDHKeyExchange(e));
		}
	}
	
	static class TestDHKeyExchange extends DHKeyExchange {

		Exception e;
		
		public TestDHKeyExchange(Exception e) {
			super("ffdhe2048", BigInteger.valueOf(1000), BigInteger.valueOf(1000), 256);
			this.e = e;
		}

		@Override
		public PublicKey generatePublicKey(BigInteger y) throws NoSuchAlgorithmException, InvalidKeySpecException {
			if (e instanceof NoSuchAlgorithmException) {
				throw (NoSuchAlgorithmException)e;
			} else if (e instanceof InvalidKeySpecException) {
				throw (InvalidKeySpecException)e;
			}
			throw new RuntimeException(e);
		}
	}

}
