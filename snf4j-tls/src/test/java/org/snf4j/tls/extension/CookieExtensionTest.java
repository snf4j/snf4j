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
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.handshake.HandshakeType;

public class CookieExtensionTest extends ExtensionTest {
	
	@Test
	public void testGetBytes() {
		CookieExtension e = new CookieExtension(bytes(1));
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,44,0,3,0,1,1), buffer());
		assertEquals(3, e.getDataLength());
		assertArrayEquals(bytes(1), e.getCookie());
		buffer.clear();
		
		e = new CookieExtension(bytes(1,2,3));
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,44,0,5,0,3,1,2,3), buffer());
		assertEquals(5, e.getDataLength());
		assertArrayEquals(bytes(1,2,3), e.getCookie());
		buffer.clear();
	}
	
	@Test(expected = IllegalArgumentException.class) 
	public void testNullArgument1() {
		new CookieExtension(null);
	}

	@Test(expected = IllegalArgumentException.class) 
	public void testNullArgument2() {
		new CookieExtension(new byte[0]);
	}

	@Test
	public void testParser() {
		assertSame(ExtensionType.COOKIE, CookieExtension.getParser().getType());
		assertSame(CookieExtension.getParser().getType(), CookieExtension.getParser().getType());
	}

	@Test
	public void testParseRealData() throws Alert {
		byte[] data = new byte[] {
				0x00, 0x2c, 0x00, 0x05, 0x00, 0x03, 0x01, 0x02, 0x03};
		
		IExtension e = CookieExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array(data, 4), 5);
		
		assertSame(CookieExtension.class, e.getClass());
		assertSame(ExtensionType.COOKIE, e.getType());
		e.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertArrayEquals(bytes(1,2,3), ((CookieExtension)e).getCookie());
		buffer.clear();
	}
	
	@Test
	public void testConsumeBytes() throws Alert {
		ByteBufferArray array = ByteBufferArray.wrap(array(bytes(0,3,1,2,3),0));
		
		assertEquals(5, array.remaining());
		IExtension e = CookieExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array, 5);
		assertNotNull(e);
		assertEquals(0, array.remaining());
	}

	void assertFailure(ByteBuffer[] array, int remaining, String message) {
		try {
			CookieExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array, remaining);
			fail();
		}
		catch (Alert e) {
			assertEquals(message, e.getMessage());
		}
	}
	
	@Test
	public void testParseFailures() {
		String inconsistentLength = "Extension 'cookie' parsing failure: Inconsistent length";
		String cookieEmpty = "Extension 'cookie' parsing failure: Cookie is empty";
		
		assertFailure(array(bytes(0,3,1,2,3), 0), 1, inconsistentLength);
		assertFailure(array(bytes(0,3,1,2,3), 0), 2, inconsistentLength);
		assertFailure(array(bytes(0,3,1,2,3), 0), 3, inconsistentLength);
		assertFailure(array(bytes(0,3,1,2,3), 0), 4, inconsistentLength);
		assertFailure(array(bytes(0,0), 0), 2, cookieEmpty);
	}
}
