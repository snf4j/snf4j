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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.snf4j.tls.extension.KeyShareExtension.Mode.CLIENT_HELLO;
import static org.snf4j.tls.extension.KeyShareExtension.Mode.HELLO_RETRY_REQUEST;
import static org.snf4j.tls.extension.KeyShareExtension.Mode.SERVER_HELLO;
import static org.snf4j.tls.extension.NamedGroup.SECP256R1;
import static org.snf4j.tls.extension.NamedGroup.SECP384R1;
import static org.snf4j.tls.extension.NamedGroup.SECP521R1;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;

import org.junit.Test;
import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.handshake.HandshakeType;

public class KeyShareExtensionTest extends ExtensionTest {
	
	
	KeyPair ecKey(String name) throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec(name));
        return keyPairGenerator.genKeyPair();
	}

	byte[] pad(byte[] data, int len) {
		if (data.length == len) {
			return data;
		}
		byte[] pad = new byte[len];
		if (data.length > len) {
			System.arraycopy(data, data.length-len, pad, 0, len);
		}
		else {
			System.arraycopy(data, 0, pad, len-data.length, data.length);
		}
		return pad;
	}
	
	@Test
	public void testGetBytesForClientHello() throws Exception {
		KeyShareExtension e = new KeyShareExtension(CLIENT_HELLO);
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertEquals(0, e.getEntries().length);
		assertEquals(2, e.getDataLength());
		assertArrayEquals(bytes(0,51,0,2,0,0), buffer());
		buffer.clear();
		
		KeyPair kp1 = ecKey(SECP256R1.name());
		e = new KeyShareExtension(CLIENT_HELLO, new KeyShareEntry(SECP256R1, kp1.getPublic()));
		e.getBytes(buffer);
		assertEquals(2+2+2+1+2*32, e.getDataLength());
		assertNull(e.getNamedGroup());
		assertEquals(1, e.getEntries().length);
		assertSame(kp1.getPublic(), e.getEntries()[0].getKey());
		assertArrayEquals(cat(
				bytes(0,51,0,71,0,69),
				bytes(0,0x17,0,65,4),
				pad(((ECPublicKey)kp1.getPublic()).getW().getAffineX().toByteArray(), 32), 
				pad(((ECPublicKey)kp1.getPublic()).getW().getAffineY().toByteArray(), 32)), buffer());
		buffer.clear();
		
		KeyPair kp2 = ecKey(SECP384R1.name());
		e = new KeyShareExtension(CLIENT_HELLO, new KeyShareEntry(SECP256R1, kp1.getPublic()), new KeyShareEntry(SECP384R1, kp2.getPublic()));
		e.getBytes(buffer);
		assertEquals(2+2+2+1+2*32+2+2+1+2*48, e.getDataLength());
		assertEquals(2, e.getEntries().length);
		assertSame(kp1.getPublic(), e.getEntries()[0].getKey());
		assertSame(kp2.getPublic(), e.getEntries()[1].getKey());
		assertArrayEquals(cat(
				bytes(0,51,0,172,0,170),
				bytes(0,0x17,0,65,4),
				pad(((ECPublicKey)kp1.getPublic()).getW().getAffineX().toByteArray(), 32), 
				pad(((ECPublicKey)kp1.getPublic()).getW().getAffineY().toByteArray(), 32),
				bytes(0,0x18,0,97,4),
				pad(((ECPublicKey)kp2.getPublic()).getW().getAffineX().toByteArray(), 48), 
				pad(((ECPublicKey)kp2.getPublic()).getW().getAffineY().toByteArray(), 48)), buffer());
		buffer.clear();

		KeyPair kp3 = ecKey(SECP521R1.name());
		e = new KeyShareExtension(CLIENT_HELLO, new KeyShareEntry(SECP521R1, kp3.getPublic()));
		e.getBytes(buffer);
		assertEquals(2+2+2+1+2*66, e.getDataLength());
		assertNull(e.getNamedGroup());
		assertEquals(1, e.getEntries().length);
		assertSame(kp3.getPublic(), e.getEntries()[0].getKey());
		assertArrayEquals(cat(
				bytes(0,51,0,139,0,137),
				bytes(0,0x19,0,133,4),
				pad(((ECPublicKey)kp3.getPublic()).getW().getAffineX().toByteArray(), 66), 
				pad(((ECPublicKey)kp3.getPublic()).getW().getAffineY().toByteArray(), 66)), buffer());
		buffer.clear();
	}
	
	@Test
	public void testGetBytesForHelloRetryRequest() throws Exception {
		KeyShareExtension e = new KeyShareExtension(NamedGroup.SECP256R1);
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertNull(e.getEntries());
		assertEquals(2, e.getDataLength());
		assertArrayEquals(bytes(0,51,0,2,0,0x17), buffer());
		buffer.clear();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testGetBytesForHelloRetryRequestEx() {
		new KeyShareExtension(HELLO_RETRY_REQUEST);
	}
	
	@Test
	public void testGetBytesForServerHello() throws Exception {
		KeyPair kp1 = ecKey(SECP256R1.name());
		KeyShareExtension e = new KeyShareExtension(SERVER_HELLO, new KeyShareEntry(SECP256R1, kp1.getPublic()));
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertEquals(1, e.getEntries().length);
		assertEquals(2+2+1+2*32, e.getDataLength());
		assertArrayEquals(cat(
				bytes(0,51,0,69),
				bytes(0,0x17,0,65,4),
				pad(((ECPublicKey)kp1.getPublic()).getW().getAffineX().toByteArray(), 32), 
				pad(((ECPublicKey)kp1.getPublic()).getW().getAffineY().toByteArray(), 32)), buffer());
		buffer.clear();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetBytesForServerHelloEx1() throws Exception {
		KeyPair kp1 = ecKey(SECP256R1.name());
		new KeyShareExtension(SERVER_HELLO, new KeyShareEntry(SECP256R1, kp1.getPublic()),new KeyShareEntry(SECP256R1, kp1.getPublic()));
	}	

	@Test(expected = IllegalArgumentException.class)
	public void testGetBytesForServerHelloEx2() throws Exception {
		new KeyShareExtension(SERVER_HELLO);
	}	
	
	@Test
	public void testParseRealData() throws AlertException {
		byte[] data = new byte[] {
				0x00, 0x33, 0x00, 0x26, 0x00, 0x24, 0x00, 0x1d, 0x00, 0x20, 0x35, (byte)0x80, 0x72, (byte)0xd6, 
				0x36, 0x58, (byte)0x80, (byte)0xd1, (byte)0xae, (byte)0xea, 0x32, (byte)0x9a, (byte)0xdf, (byte)0x91, 
				0x21, 0x38, 0x38, 0x51, (byte)0xed,	0x21, (byte)0xa2, (byte)0x8e, 0x3b, 0x75, (byte)0xe9, 0x65, 
				(byte)0xd0, (byte)0xd2, (byte)0xcd, 0x16, 0x62, 0x54};
		
		KeyShareExtension e = (KeyShareExtension) KeyShareExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array(data, 4), 0x26);
		assertSame(KeyShareExtension.class, e.getClass());
		assertSame(ExtensionType.KEY_SHARE, e.getType());
		assertEquals(1, e.getEntries().length);
		assertSame(KeyShareExtension.Mode.CLIENT_HELLO, e.getMode());
		assertNull(e.getNamedGroup());
		KeyShareEntry entry = e.getEntries()[0];
		assertSame(NamedGroup.X25519, entry.getNamedGroup());
		if (TLS1_3) {
			assertNotNull(entry.getParsedKey());
			assertNull(entry.getKey());
			assertNull(entry.getRawKey());
		}
		else {
			assertNull(entry.getKey());
			assertNull(entry.getParsedKey());
			assertEquals(32, entry.getRawKey().length);
			assertArrayEquals(bytes(data, data.length-32, 32), entry.getRawKey());
		}
		e.getBytes(buffer);
		assertArrayEquals(data, buffer());
		buffer.clear();
		
		data = new byte[] {
				0x00, 0x33, 0x00, 0x24, 0x00, 0x1d, 0x00, 0x20, (byte)0x9f, (byte)0xd7, (byte)0xad, 0x6d, (byte)0xcf, 
				(byte)0xf4, 0x29, (byte)0x8d, (byte)0xd3, (byte)0xf9, 0x6d, 0x5b, 0x1b, 0x2a, (byte)0xf9, 0x10, 
				(byte)0xa0, 0x53, 0x5b, 0x14, (byte)0x88, (byte)0xd7, (byte)0xf8, (byte)0xfa, (byte)0xbb, 0x34, 
				(byte)0x9a, (byte)0x98, 0x28, (byte)0x80, (byte)0xb6, 0x15};

		e = (KeyShareExtension) KeyShareExtension.getParser().parse(HandshakeType.SERVER_HELLO, array(data, 4), 0x24);
		assertSame(ExtensionType.KEY_SHARE, e.getType());
		assertEquals(1, e.getEntries().length);
		assertSame(KeyShareExtension.Mode.SERVER_HELLO, e.getMode());
		assertNull(e.getNamedGroup());
		entry = e.getEntries()[0];
		assertSame(NamedGroup.X25519, entry.getNamedGroup());
		if (TLS1_3) {
			assertNotNull(entry.getParsedKey());
			assertNull(entry.getKey());
			assertNull(entry.getRawKey());
		}
		else {
			assertNull(entry.getParsedKey());
			assertNull(entry.getKey());
			assertEquals(32, entry.getRawKey().length);
			assertArrayEquals(bytes(data, data.length-32, 32), entry.getRawKey());
		}
		e.getBytes(buffer);
		assertArrayEquals(data, buffer());
		buffer.clear();
		
		data = new byte[] {0x00, 0x33, 0x00, 0x02, 0x00, 0x1d};
		e = (KeyShareExtension) KeyShareExtension.getParser().parse(HandshakeType.SERVER_HELLO, array(data, 4), 0x02);
		assertSame(ExtensionType.KEY_SHARE, e.getType());
		assertNull(e.getEntries());
		assertSame(KeyShareExtension.Mode.HELLO_RETRY_REQUEST, e.getMode());
		assertSame(NamedGroup.X25519, e.getNamedGroup());
		e.getBytes(buffer);
		assertArrayEquals(data, buffer());
		buffer.clear();
	}

	@Test
	public void testParseData() throws Exception {
		KeyPair kp1 = ecKey(SECP256R1.name());
		KeyShareExtension e = new KeyShareExtension(CLIENT_HELLO, new KeyShareEntry(SECP256R1, kp1.getPublic()));
		e.getBytes(buffer);
		byte[] data = buffer();
		KeyShareExtension e2 = (KeyShareExtension) KeyShareExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array(data, 4), data[3]);
		assertNull(e2.getEntries()[0].getKey());
		assertEquals(kp1.getPublic(), SECP256R1.spec().generateKey(e2.getEntries()[0].getParsedKey()));
		assertSame(KeyShareExtension.Mode.CLIENT_HELLO, e2.getMode());
		buffer.clear();
		e2.getBytes(buffer);
		assertArrayEquals(data, buffer());
		buffer.clear();
		
		KeyPair kp2 = ecKey(SECP384R1.name());
		e = new KeyShareExtension(CLIENT_HELLO, new KeyShareEntry(SECP256R1, kp1.getPublic()),new KeyShareEntry(SECP384R1, kp2.getPublic()));
		e.getBytes(buffer);
		data = buffer();
		e2 = (KeyShareExtension) KeyShareExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array(data, 4), (int)data[3] & 0xff);
		assertNull(e2.getEntries()[0].getKey());
		assertNull(e2.getEntries()[1].getKey());
		assertEquals(kp1.getPublic(), SECP256R1.spec().generateKey(e2.getEntries()[0].getParsedKey()));
		assertEquals(kp2.getPublic(), SECP384R1.spec().generateKey(e2.getEntries()[1].getParsedKey()));
		assertSame(KeyShareExtension.Mode.CLIENT_HELLO, e2.getMode());
		buffer.clear();
		e2.getBytes(buffer);
		assertArrayEquals(data, buffer());
		buffer.clear();
		
		e = new KeyShareExtension(CLIENT_HELLO);
		e.getBytes(buffer);
		data = buffer();
		e2 = (KeyShareExtension) KeyShareExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array(data, 4), data[3]);
		assertEquals(0, e2.getEntries().length);
		assertSame(KeyShareExtension.Mode.CLIENT_HELLO, e2.getMode());
		buffer.clear();
		e2.getBytes(buffer);
		assertArrayEquals(data, buffer());
		buffer.clear();

		KeyPair kp3 = ecKey(SECP521R1.name());
		e = new KeyShareExtension(SERVER_HELLO, new KeyShareEntry(SECP521R1, kp3.getPublic()));
		e.getBytes(buffer);
		data = buffer();
		e2 = (KeyShareExtension) KeyShareExtension.getParser().parse(HandshakeType.SERVER_HELLO, array(data, 4), (int)data[3] & 0xff);
		assertNull(e2.getEntries()[0].getKey());
		assertEquals(kp3.getPublic(), SECP521R1.spec().generateKey(e2.getEntries()[0].getParsedKey()));
		assertSame(KeyShareExtension.Mode.SERVER_HELLO, e2.getMode());
		buffer.clear();
		e2.getBytes(buffer);
		assertArrayEquals(data, buffer());
		buffer.clear();

		e = new KeyShareExtension(SECP521R1);
		e.getBytes(buffer);
		data = buffer();
		e2 = (KeyShareExtension) KeyShareExtension.getParser().parse(HandshakeType.SERVER_HELLO, array(data, 4), (int)data[3] & 0xff);
		assertNull(e2.getEntries());
		assertSame(KeyShareExtension.Mode.HELLO_RETRY_REQUEST, e2.getMode());
		buffer.clear();
		e2.getBytes(buffer);
		assertArrayEquals(data, buffer());
		buffer.clear();
	}
	
	@Test
	public void testParser() {
		assertSame(ExtensionType.KEY_SHARE, KeyShareExtension.getParser().getType());
		assertSame(KeyShareExtension.getParser().getType(), KeyShareExtension.getParser().getType());
	}

	@Test
	public void testConsumeBytes() throws Exception {
		KeyPair kp1 = ecKey(SECP256R1.name());
		KeyShareExtension e = new KeyShareExtension(CLIENT_HELLO, new KeyShareEntry(SECP256R1, kp1.getPublic()));
		e.getBytes(buffer);
		ByteBufferArray array = ByteBufferArray.wrap(array(buffer(),4));
		KeyShareExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array, (int)array.remaining());
		assertEquals(0,array.remaining());
		buffer.clear();
		
		KeyPair kp2 = ecKey(SECP384R1.name());
		e = new KeyShareExtension(CLIENT_HELLO, new KeyShareEntry(SECP256R1, kp1.getPublic()),new KeyShareEntry(SECP384R1, kp2.getPublic()));
		e.getBytes(buffer);
		array = ByteBufferArray.wrap(array(buffer(),4));
		KeyShareExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array, (int)array.remaining());
		assertEquals(0,array.remaining());
		buffer.clear();
		
		e = new KeyShareExtension(CLIENT_HELLO);
		e.getBytes(buffer);
		array = ByteBufferArray.wrap(array(buffer(),4));
		KeyShareExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array, (int)array.remaining());
		assertEquals(0,array.remaining());
		buffer.clear();

		KeyPair kp3 = ecKey(SECP521R1.name());
		e = new KeyShareExtension(SERVER_HELLO, new KeyShareEntry(SECP521R1, kp3.getPublic()));
		e.getBytes(buffer);
		array = ByteBufferArray.wrap(array(buffer(),4));
		KeyShareExtension.getParser().parse(HandshakeType.SERVER_HELLO, array, (int)array.remaining());
		assertEquals(0,array.remaining());
		buffer.clear();

		e = new KeyShareExtension(SECP521R1);
		e.getBytes(buffer);
		array = ByteBufferArray.wrap(array(buffer(),4));
		KeyShareExtension.getParser().parse(HandshakeType.SERVER_HELLO, array, (int)array.remaining());
		assertEquals(0,array.remaining());
		buffer.clear();
	}	
	
	void assertFailure(ByteBuffer[] array, Boolean server, int remaining, String message) {
		try {
			HandshakeType ht;
			if (server == null) {
				ht = HandshakeType.ENCRYPTED_EXTENSIONS;
			}
			else {
				ht = server ? HandshakeType.SERVER_HELLO : HandshakeType.CLIENT_HELLO;
			}
			KeyShareExtension.getParser().parse(ht, array, remaining);
			fail();
		}
		catch (AlertException e) {
			assertEquals(message, e.getMessage());
		}
	}

	@Test
	public void testParseFailures() throws Exception {
		String inconsistentLength = "Extension 'key_share' parsing failure: Inconsistent length";
		String isProhibited = "Extension 'key_share' is prohibited in encrypted_extensions";
		String ecUnexpectedSize = "Extension 'key_share' parsing failure: EC key exchange unexpected size";
		
		assertFailure(array(bytes(0,0x17), 0), true, 1, inconsistentLength);
		assertFailure(array(bytes(0,0x17), 0), false, 2, inconsistentLength);
		assertFailure(array(bytes(0,0), 0), true, 2, inconsistentLength);
		assertFailure(array(bytes(0,0x17,0,1,0), 0), true, 4, inconsistentLength);
		assertFailure(array(bytes(0,0x17,0,1,0,0), 0), true, 6, inconsistentLength);
		assertFailure(array(bytes(0,0x17,0,1,0,0), 0), null, 6, isProhibited);
		assertFailure(array(bytes(0,5,0,0x17,0,1,0,0), 0), false, 8, inconsistentLength);
		assertFailure(array(bytes(0,4,0,0x17,0,1,0,0), 0), false, 6, inconsistentLength);
		assertFailure(array(bytes(0,4,0,0x17,0,1,0,0), 0), false, 6, inconsistentLength);
		assertFailure(array(bytes(0,5,0,0x17,0,2,0,0), 0), false, 7, inconsistentLength);
		assertFailure(array(bytes(0,6,0,0x17,0,2,0,0), 0), false, 8, ecUnexpectedSize);
		
		KeyPair kp1 = ecKey(SECP256R1.name());
		KeyShareExtension e = new KeyShareExtension(CLIENT_HELLO, new KeyShareEntry(SECP256R1, kp1.getPublic()));
		e.getBytes(buffer);
		byte[] data = buffer();
		int len = data[3];
		KeyShareExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array(data, 4), len);
		for (int i=0; i<len; ++i) {
			try {
				KeyShareExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array(data, 4), i);
				fail();
			}
			catch (AlertException ex) {
			}
		}
		buffer.clear();
		
		e = new KeyShareExtension(SERVER_HELLO, new KeyShareEntry(SECP256R1, kp1.getPublic()));
		e.getBytes(buffer);
		data = buffer();
		len = data[3];
		KeyShareExtension.getParser().parse(HandshakeType.SERVER_HELLO, array(data, 4), len);
		for (int i=0; i<len; ++i) {
			try {
				if (i != 2) {
					KeyShareExtension.getParser().parse(HandshakeType.SERVER_HELLO, array(data, 4), i);
					fail();
				}
			}
			catch (AlertException ex) {
			}
		}
		buffer.clear();

		e = new KeyShareExtension(SECP256R1);
		e.getBytes(buffer);
		data = buffer();
		len = data[3];
		KeyShareExtension.getParser().parse(HandshakeType.SERVER_HELLO, array(data, 4), len);
		for (int i=0; i<len; ++i) {
			try {
				KeyShareExtension.getParser().parse(HandshakeType.SERVER_HELLO, array(data, 4), i);
				fail();
			}
			catch (AlertException ex) {
			}
		}
		buffer.clear();
	}	
	
	@Test
	public void testUnknownOrUnimplemented() throws Exception {
		byte[] data = new byte[] {0x78, 0x33, 0, 5, 1, 2, 3, 4, 5};
		KeyShareExtension e = (KeyShareExtension) KeyShareExtension.getParser().parse(HandshakeType.SERVER_HELLO, array(data, 0), data.length);
		
		assertEquals(1, e.getEntries().length);
		KeyShareEntry entry = e.getEntries()[0];
		assertNull(entry.getKey());
		assertArrayEquals(bytes(1,2,3,4,5), entry.getRawKey());
		assertFalse(entry.getNamedGroup().isKnown());
		assertEquals(0x7833, entry.getNamedGroup().value());
		
		if (!TLS1_3) {
			data = new byte[] {0,0x1D,0,31,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1};
			try {
				e = (KeyShareExtension) KeyShareExtension.getParser().parse(HandshakeType.SERVER_HELLO, array(data, 0), data.length);
				fail();
			}
			catch (AlertException ex) {
				
			}
			data = new byte[] {0,0x1D,0,32,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2};
			e = (KeyShareExtension) KeyShareExtension.getParser().parse(HandshakeType.SERVER_HELLO, array(data, 0), data.length);
			entry = e.getEntries()[0];
			assertNull(entry.getKey());
			assertArrayEquals(bytes(1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2), entry.getRawKey());
			assertTrue(entry.getNamedGroup().isKnown());
			assertEquals(0x1D, entry.getNamedGroup().value());
		}
	}	
}
