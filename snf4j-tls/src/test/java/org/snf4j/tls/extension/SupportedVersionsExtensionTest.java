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
import static org.snf4j.tls.extension.ISupportedVersionsExtension.Mode.CLIENT_HELLO;
import static org.snf4j.tls.extension.ISupportedVersionsExtension.Mode.SERVER_HELLO;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.handshake.HandshakeType;

public class SupportedVersionsExtensionTest extends ExtensionTest {
	
	@Test
	public void testGetBytes() {
		SupportedVersionsExtension e = new SupportedVersionsExtension(CLIENT_HELLO, 0x0304);
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,43,0,3,2,3,4), buffer());
		assertEquals(3, e.getDataLength());
		assertEquals(1, e.getVersions().length);
		assertEquals(ExtensionType.SUPPORTED_VERSIONS, e.getType());
		assertEquals(0x0304, e.getVersions()[0]);
		assertSame(CLIENT_HELLO, e.getMode());
		buffer.clear();

		e = new SupportedVersionsExtension(CLIENT_HELLO, 0x0304, 0x0506);
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,43,0,5,4,3,4,5,6), buffer());
		assertEquals(5, e.getDataLength());
		assertEquals(2, e.getVersions().length);
		assertEquals(ExtensionType.SUPPORTED_VERSIONS, e.getType());
		assertEquals(0x0304, e.getVersions()[0]);
		assertEquals(0x0506, e.getVersions()[1]);
		assertSame(CLIENT_HELLO, e.getMode());
		buffer.clear();

		e = new SupportedVersionsExtension(SERVER_HELLO, 0x0304);
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,43,0,2,3,4), buffer());
		assertEquals(2, e.getDataLength());
		assertEquals(1, e.getVersions().length);
		assertEquals(ExtensionType.SUPPORTED_VERSIONS, e.getType());
		assertEquals(0x0304, e.getVersions()[0]);
		assertSame(SERVER_HELLO, e.getMode());
		buffer.clear();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorEx1() {
		new SupportedVersionsExtension(null, 0x0304);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorEx2() {
		new SupportedVersionsExtension(CLIENT_HELLO);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorEx3() {
		new SupportedVersionsExtension(SERVER_HELLO);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorEx4() {
		new SupportedVersionsExtension(SERVER_HELLO, 0x0304, 0x0506);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorEx5() {
		new SupportedVersionsExtension(SERVER_HELLO, (int[])null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorEx6() {
		new SupportedVersionsExtension(CLIENT_HELLO, (int[])null);
	}
	
	@Test
	public void testParseRealData() throws AlertException {
		byte[] data = new byte[] { 0x00, 0x2b, 0x00, 0x03, 0x02, 0x03, 0x04 };
		
		SupportedVersionsExtension e = (SupportedVersionsExtension) SupportedVersionsExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array(data, 4), 3);
		assertSame(SupportedVersionsExtension.class, e.getClass());
		assertSame(ExtensionType.SUPPORTED_VERSIONS, e.getType());
		assertEquals(1, e.getVersions().length);
		assertEquals(0x0304, e.getVersions()[0]);
		assertSame(CLIENT_HELLO, e.getMode());
		e.getBytes(buffer);
		assertArrayEquals(data, buffer());
		buffer.clear();
		
		data = new byte[] { 0x00, 0x2b, 0x00, 0x02, 0x03, 0x04 };
		
		e = (SupportedVersionsExtension) SupportedVersionsExtension.getParser().parse(HandshakeType.SERVER_HELLO, array(data, 4), 2);
		assertSame(SupportedVersionsExtension.class, e.getClass());
		assertSame(ExtensionType.SUPPORTED_VERSIONS, e.getType());
		assertEquals(1, e.getVersions().length);
		assertEquals(0x0304, e.getVersions()[0]);
		assertSame(SERVER_HELLO, e.getMode());
		e.getBytes(buffer);
		assertArrayEquals(data, buffer());
	}
	
	@Test
	public void testParseData() throws Exception {
		SupportedVersionsExtension e = new SupportedVersionsExtension(CLIENT_HELLO, 0x0304, 0x0506);
		e.getBytes(buffer);
		byte[] data = buffer();
		SupportedVersionsExtension e2 = (SupportedVersionsExtension) SupportedVersionsExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array(data, 4), data[3]);
		assertEquals(2, e2.getVersions().length);
		assertEquals(0x0304, e2.getVersions()[0]);
		assertEquals(0x0506, e2.getVersions()[1]);
		buffer.clear();
		e2.getBytes(buffer);
		assertArrayEquals(data, buffer());
		buffer.clear();
		
		e = new SupportedVersionsExtension(CLIENT_HELLO, 0x0304, 0x0506, 0x0708);
		e.getBytes(buffer);
		data = buffer();
		e2 = (SupportedVersionsExtension) SupportedVersionsExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array(data, 4), data[3]);
		assertEquals(3, e2.getVersions().length);
		assertEquals(0x0304, e2.getVersions()[0]);
		assertEquals(0x0506, e2.getVersions()[1]);
		assertEquals(0x0708, e2.getVersions()[2]);
		buffer.clear();
		e2.getBytes(buffer);
		assertArrayEquals(data, buffer());
	}
	
	@Test
	public void testParser() {
		assertSame(ExtensionType.SUPPORTED_VERSIONS, SupportedVersionsExtension.getParser().getType());
		assertSame(SupportedVersionsExtension.getParser().getType(), SupportedVersionsExtension.getParser().getType());
	}
	
	void assertFailure(ByteBuffer[] array, boolean server, int remaining, String message) {
		try {
			HandshakeType ht = server ? HandshakeType.SERVER_HELLO : HandshakeType.CLIENT_HELLO;
			
			SupportedVersionsExtension.getParser().parse(ht, array, remaining);
			fail();
		}
		catch (AlertException e) {
			assertEquals(message, e.getMessage());
		}
	}
	
	@Test
	public void testParseFailures() throws Exception {
		String inconsistentLength = "Extension 'supported_versions' parsing failure: Inconsistent length";
		String incorrectLength = "Extension 'supported_versions' parsing failure: Incorrect length";
		
		assertFailure(array(bytes(2,3,4), 0), false, 1, inconsistentLength);
		assertFailure(array(bytes(2,3,4), 0), false, 2, inconsistentLength);
		assertFailure(array(bytes(0,3,4), 0), false, 3, incorrectLength);
		assertFailure(array(bytes(1,3,4), 0), false, 3, incorrectLength);
		assertFailure(array(bytes(3,3,4,5), 0), false, 4, incorrectLength);
		assertFailure(array(bytes(4,3,4,5,6), 0), false, 6, inconsistentLength);
		assertFailure(array(bytes(3,4), 0), true, 1, inconsistentLength);
		assertFailure(array(bytes(3,4,5), 0), true, 3, inconsistentLength);
	}
}
