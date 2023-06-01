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

public class EarlyDataExtensionTest extends ExtensionTest {
	
	@Test
	public void testGetBytes() {
		EarlyDataExtension e = new EarlyDataExtension(0x01020304L);
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,42,0,4,1,2,3,4), buffer());
		assertEquals(4, e.getDataLength());
		assertEquals(0x01020304, e.getMaxSize());
		buffer.clear();
		
		e = new EarlyDataExtension();
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,42,0,0), buffer());
		assertEquals(0, e.getDataLength());
		assertEquals(-1, e.getMaxSize());
		buffer.clear();

		e = new EarlyDataExtension(0);
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,42,0,4,0,0,0,0), buffer());
		buffer.clear();

		e = new EarlyDataExtension(0xffffffffL);
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,42,0,4,255,255,255,255), buffer());
		buffer.clear();
	}
	
	@Test(expected = IllegalArgumentException.class) 
	public void testNullArgument1() {
		new EarlyDataExtension(-1);
	}
	
	@Test(expected = IllegalArgumentException.class) 
	public void testNullArgument2() {
		new EarlyDataExtension(0x100000000L);
	}
	
	@Test
	public void testParser() {
		assertSame(ExtensionType.EARLY_DATA, EarlyDataExtension.getParser().getType());
		assertSame(EarlyDataExtension.getParser().getType(), EarlyDataExtension.getParser().getType());
	}
	
	@Test
	public void testParseRealData() throws Alert {
		byte[] data = new byte[] {
				0x00, 0x2A, 0x00, 0x04, 0x01, 0x02, 0x03, 0x04};
		
		IExtension e = EarlyDataExtension.getParser().parse(HandshakeType.NEW_SESSION_TICKET, array(data, 4), 4);
		
		assertSame(EarlyDataExtension.class, e.getClass());
		assertSame(ExtensionType.EARLY_DATA, e.getType());
		e.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(0x01020304L, ((EarlyDataExtension)e).getMaxSize());
		buffer.clear();
		
		data = new byte[] {0x00, 0x2A, 0x00, 0x00};
		e = EarlyDataExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array(data, 4), 0);
		e.getBytes(buffer);
		assertArrayEquals(data, buffer());
	}
	
	@Test
	public void testConsumeBytes() throws Alert {
		ByteBufferArray array = ByteBufferArray.wrap(array(bytes(1,2,3,4),0));
		
		assertEquals(4, array.remaining());
		IExtension e = EarlyDataExtension.getParser().parse(HandshakeType.NEW_SESSION_TICKET, array, 4);
		assertNotNull(e);
		assertEquals(0, array.remaining());
	}
	
	void assertFailure(ByteBuffer[] array, boolean nst, int remaining, String message) {
		try {
			EarlyDataExtension.getParser().parse(
					nst ? HandshakeType.NEW_SESSION_TICKET : HandshakeType.CLIENT_HELLO, 
					array, 
					remaining);
			fail();
		}
		catch (Alert e) {
			assertEquals(message, e.getMessage());
		}
	}

	@Test
	public void testParseFailures() {
		String inconsistentLength = "Extension 'early_data' parsing failure: Inconsistent length";
		
		assertFailure(array(bytes(0,1,2,3), 0), true, 0, inconsistentLength);
		assertFailure(array(bytes(0,1,2,3), 0), true, 1, inconsistentLength);
		assertFailure(array(bytes(0,1,2,3), 0), true, 2, inconsistentLength);
		assertFailure(array(bytes(0,1,2,3), 0), true, 3, inconsistentLength);
		assertFailure(array(bytes(0,1,2,3,0), 0), true, 5, inconsistentLength);
		assertFailure(array(bytes(0,1,2,3), 0), false, 1, inconsistentLength);
		assertFailure(array(bytes(0,1,2,3), 0), false, 4, inconsistentLength);
	}
	
}
