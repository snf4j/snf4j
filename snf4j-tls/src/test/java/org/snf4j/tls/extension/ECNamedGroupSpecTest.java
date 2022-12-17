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
import static org.snf4j.tls.extension.NamedGroup.SECP256R1;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;

import org.junit.Test;
import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.alert.AlertDescription;
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.crypto.ECKeyExchange;

public class ECNamedGroupSpecTest extends ExtensionTest {

	KeyPair ecKey(String name) throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec(name));
        return keyPairGenerator.genKeyPair();
	}

	@Test
	public void testGetData() {
		ECNamedGroupSpec spec = ECNamedGroupSpec.SECP256R1;
		byte[] x = bytes(1,2,3);
		byte[] y = bytes(4,5,6);
		spec.getData(buffer, x, y);
		assertArrayEquals(cat(
				bytes(30,(byte)4,(byte)0,(byte)0),x,
				bytes(29,(byte)0,(byte)0,(byte)0),y
				), buffer());
		buffer.clear();

		x = bytes(32,(byte)1,(byte)2,(byte)3);
		y = bytes(32,(byte)4,(byte)5,(byte)6);
		spec.getData(buffer, x, y);
		assertArrayEquals(cat(bytes(4),x,y), buffer());
		buffer.clear();
		
		x = cat(bytes(0), bytes(32,(byte)1,(byte)2,(byte)3));
		y = cat(bytes(0), bytes(32,(byte)4,(byte)5,(byte)6));
		spec.getData(buffer, x, y);
		assertArrayEquals(cat(bytes(4),bytes(x,1,32),bytes(y,1,32)), buffer());
		buffer.clear();
		
		x = cat(bytes(0,0,0), bytes(32,(byte)1,(byte)2,(byte)3));
		y = cat(bytes(0,0,0), bytes(32,(byte)4,(byte)5,(byte)6));
		spec.getData(buffer, x, y);
		assertArrayEquals(cat(bytes(4),bytes(x,3,32),bytes(y,3,32)), buffer());
		buffer.clear();

		x = cat(bytes(0,0,1), bytes(32,(byte)1,(byte)2,(byte)3));
		y = cat(bytes(0,0,0), bytes(32,(byte)4,(byte)5,(byte)6));
		try {
			spec.getData(buffer, x, y);
			fail();
		} catch (IllegalArgumentException e) {};

		x = cat(bytes(0,0,0), bytes(32,(byte)1,(byte)2,(byte)3));
		y = cat(bytes(0,0,1), bytes(32,(byte)4,(byte)5,(byte)6));
		try {
			spec.getData(buffer, x, y);
			fail();
		} catch (IllegalArgumentException e) {};

		x = cat(bytes(1,0,0), bytes(32,(byte)1,(byte)2,(byte)3));
		y = cat(bytes(0,0,0), bytes(32,(byte)4,(byte)5,(byte)6));
		try {
			spec.getData(buffer, x, y);
			fail();
		} catch (IllegalArgumentException e) {};

		x = cat(bytes(0,0,0), bytes(32,(byte)1,(byte)2,(byte)3));
		y = cat(bytes(1,0,0), bytes(32,(byte)4,(byte)5,(byte)6));
		try {
			spec.getData(buffer, x, y);
			fail();
		} catch (IllegalArgumentException e) {};
	}
	
	@Test
	public void testGetDataLength() {
		ECNamedGroupSpec spec = new ECNamedGroupSpec(ECKeyExchange.SECP256R1, 32);
		assertEquals(65, spec.getDataLength());
	}
	
	@Test
	public void testParse() throws Exception {
		KeyPair kp1 = ecKey(SECP256R1.name());
		ECNamedGroupSpec spec = ECNamedGroupSpec.SECP256R1;
		
		spec.getData(buffer, kp1.getPublic());
		byte[] data = buffer();
		ParsedKey pk = spec.parse(ByteBufferArray.wrap(array(data,0)), data.length);
		assertEquals(kp1.getPublic(), spec.generateKey(pk));
		assertTrue(spec.isImplemented());
		
	}

	void assertFailure(ECNamedGroupSpec spec, byte[] data, int remaining, AlertDescription desc, String message) {
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
		KeyPair kp1 = ecKey(SECP256R1.name());
		ECNamedGroupSpec spec = ECNamedGroupSpec.SECP256R1;
		spec.getData(buffer, kp1.getPublic());
		byte[] data = buffer();
		
		assertFailure(spec, data, data.length-1, AlertDescription.DECODE_ERROR, 
				"Extension 'key_share' parsing failure: EC key exchange unexpected size");
		data[0] = 3;
		assertFailure(spec, data, data.length, AlertDescription.DECODE_ERROR,
				"Extension 'key_share' parsing failure: EC key exchange unexpected legacy form");
		data[0] = 4;
		assertFailure(new TestECSpec(new NoSuchAlgorithmException()), data, data.length, 
				AlertDescription.INTERNAL_ERROR, "Extension 'key_share' internal failure: No EC algorithm");
		assertFailure(new TestECSpec(new InvalidKeySpecException()), data, data.length, 
				AlertDescription.INTERNAL_ERROR, "Extension 'key_share' internal failure: Invalid EC key specification");
		assertFailure(new TestECSpec(new InvalidParameterSpecException()), data, data.length, 
				AlertDescription.INTERNAL_ERROR, "Extension 'key_share' internal failure: Invalid EC parameter specification");
		assertFailure(new TestECSpec(new Exception()), data, data.length, 
				AlertDescription.INTERNAL_ERROR, "Extension 'key_share' internal failure: EC key generation failure");
	}
	
	static class TestECSpec extends ECNamedGroupSpec {

		public TestECSpec(Exception e) {
			super(new TestECKeyExchange(e), 32);
		}
	}
	
	static class TestECKeyExchange extends ECKeyExchange {

		Exception e;
		
		public TestECKeyExchange(Exception e) {
			super("secp256r1");
			this.e = e;
		}

		@Override
		public PublicKey generatePublicKey(BigInteger x, BigInteger y) throws NoSuchAlgorithmException, InvalidParameterSpecException, InvalidKeySpecException {
			if (e instanceof NoSuchAlgorithmException) {
				throw (NoSuchAlgorithmException)e;
			} else if (e instanceof InvalidParameterSpecException) {
				throw (InvalidParameterSpecException)e;
			} else if (e instanceof InvalidKeySpecException) {
				throw (InvalidKeySpecException)e;
			}
			throw new RuntimeException(e);
		}
	}
}
