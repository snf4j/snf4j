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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.handshake.HandshakeType;

public class SignatureAlgorithmsExtensionTest extends ExtensionTest {

	@Test
	public void testGetBytes() {
		SignatureAlgorithmsExtension e = new SignatureAlgorithmsExtension(SignatureScheme.ED25519);
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,13,0,4,0,2,8,7), buffer());
		assertEquals(4, e.getDataLength());
		assertEquals(1, e.getSchemes().length);
		assertEquals(ExtensionType.SIGNATURE_ALGORITHMS, e.getType());
		assertEquals(SignatureScheme.ED25519, e.getSchemes()[0]);
		buffer.clear();
		
		e = new SignatureAlgorithmsExtension(SignatureScheme.ED25519, SignatureScheme.ED448);
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,13,0,6,0,4,8,7,8,8), buffer());
		assertEquals(6, e.getDataLength());
		assertEquals(2, e.getSchemes().length);
		assertEquals(SignatureScheme.ED25519, e.getSchemes()[0]);
		assertEquals(SignatureScheme.ED448, e.getSchemes()[1]);
	}	

	@Test(expected = IllegalArgumentException.class) 
	public void testNullArgument() {
		new SignatureAlgorithmsExtension((SignatureScheme[])null);
	}

	@Test(expected = IllegalArgumentException.class) 
	public void testEmptyArgument() {
		new SignatureAlgorithmsExtension(new SignatureScheme[0]);
	}
	
	@Test
	public void testParser() {
		assertSame(ExtensionType.SIGNATURE_ALGORITHMS, SignatureAlgorithmsExtension.getParser().getType());
		assertSame(SignatureAlgorithmsExtension.getParser().getType(), SignatureAlgorithmsExtension.getParser().getType());
	}
	
	@Test
	public void testParseRealData() throws AlertException {
		byte[] data = new byte[] {
				0x00, 0x0d, 0x00, 0x1e, 0x00, 0x1c, 0x04, 0x03, 0x05, 0x03, 0x06, 0x03, 0x08, 0x07, 0x08, 
				0x08, 0x08, 0x09, 0x08, 0x0a, 0x08, 0x0b, 0x08, 0x04, 0x08, 0x05, 0x08, 0x06, 0x04, 0x01,
				0x05, 0x01, 0x06, 0x01};
		
		SignatureAlgorithmsExtension e = (SignatureAlgorithmsExtension) SignatureAlgorithmsExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array(data, 4), data.length-4);
		assertSame(SignatureAlgorithmsExtension.class, e.getClass());
		assertSame(ExtensionType.SIGNATURE_ALGORITHMS, e.getType());
		e.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(14, e.getSchemes().length);
		assertSame(SignatureScheme.ECDSA_SECP256R1_SHA256, e.getSchemes()[0]);
		assertSame(SignatureScheme.ECDSA_SECP384R1_SHA384, e.getSchemes()[1]);
		assertSame(SignatureScheme.ECDSA_SECP521R1_SHA512, e.getSchemes()[2]);
		assertSame(SignatureScheme.ED25519, e.getSchemes()[3]);
		assertSame(SignatureScheme.ED448, e.getSchemes()[4]);
		assertSame(SignatureScheme.RSA_PSS_PSS_SHA256, e.getSchemes()[5]);
		assertSame(SignatureScheme.RSA_PSS_PSS_SHA384, e.getSchemes()[6]);
		assertSame(SignatureScheme.RSA_PSS_PSS_SHA512, e.getSchemes()[7]);
		assertSame(SignatureScheme.RSA_PSS_RSAE_SHA256, e.getSchemes()[8]);
		assertSame(SignatureScheme.RSA_PSS_RSAE_SHA384, e.getSchemes()[9]);
		assertSame(SignatureScheme.RSA_PSS_RSAE_SHA512, e.getSchemes()[10]);
		assertSame(SignatureScheme.RSA_PKCS1_SHA256, e.getSchemes()[11]);
		assertSame(SignatureScheme.RSA_PKCS1_SHA384, e.getSchemes()[12]);
		assertSame(SignatureScheme.RSA_PKCS1_SHA512, e.getSchemes()[13]);
	}
	
	@Test
	public void testConsumeBytes() throws AlertException {
		ByteBufferArray array = ByteBufferArray.wrap(array(bytes(0,4,4,1,8,5),0));
		
		assertEquals(6, array.remaining());
		IExtension e = SignatureAlgorithmsExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array, 6);
		assertNotNull(e);
		assertEquals(0, array.remaining());
	}

	void assertFailure(ByteBuffer[] array, int remaining, String message) {
		try {
			SignatureAlgorithmsExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array, remaining);
			fail();
		}
		catch (AlertException e) {
			assertEquals(message, e.getMessage());
		}
	}

	@Test
	public void testParseFailures() {
		String inconsistentLength = "Extension 'signature_algorithms' parsing failure: Inconsistent length";
		String incorrectLength = "Extension 'signature_algorithms' parsing failure: Incorrect length";
		
		assertFailure(array(bytes(0,4,4,1,8,5), 0), 0, inconsistentLength);
		assertFailure(array(bytes(0,4,4,1,8,5), 0), 3, inconsistentLength);
		assertFailure(array(bytes(0,0), 0), 2, inconsistentLength);
		assertFailure(array(bytes(0,0,0,0), 0), 4, incorrectLength);
		assertFailure(array(bytes(0,3,4,1,8), 0), 5, incorrectLength);
		assertFailure(array(bytes(0,4,4,1,8,5), 0), 5, inconsistentLength);
		assertFailure(array(bytes(0,4,4,1,8,5), 0), 7, inconsistentLength);
	}	

}
