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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.handshake.HandshakeType;

public class PskKeyExchangeModesExtensionTest extends ExtensionTest {

	@Test
	public void testGetBytes() {
		PskKeyExchangeModesExtension e = new PskKeyExchangeModesExtension(PskKeyExchangeMode.PSK_KE);
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,45,0,2,1,0), buffer());
		assertEquals(2, e.getDataLength());
		assertEquals(1, e.getModes().length);
		assertSame(PskKeyExchangeMode.PSK_KE, e.getModes()[0]);
		buffer.clear();

		e = new PskKeyExchangeModesExtension(PskKeyExchangeMode.PSK_KE, PskKeyExchangeMode.PSK_DHE_KE);
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,45,0,3,2,0,1), buffer());
		assertEquals(3, e.getDataLength());
		assertEquals(2, e.getModes().length);
		assertSame(PskKeyExchangeMode.PSK_KE, e.getModes()[0]);
		assertSame(PskKeyExchangeMode.PSK_DHE_KE, e.getModes()[1]);
		buffer.clear();	
	}
	
	@Test(expected = IllegalArgumentException.class) 
	public void testInvalidArgument1() {
		new PskKeyExchangeModesExtension();
	}

	@Test(expected = IllegalArgumentException.class) 
	public void testInvalidArgument2() {
		new PskKeyExchangeModesExtension((PskKeyExchangeMode[])null);
	}

	@Test
	public void testParser() {
		assertSame(ExtensionType.PSK_KEY_EXCHANGE_MODES, PskKeyExchangeModesExtension.getParser().getType());
		assertSame(PskKeyExchangeModesExtension.getParser().getType(), PskKeyExchangeModesExtension.getParser().getType());
	}
	
	@Test
	public void testParseRealData() throws Alert {
		byte[] data = bytes(0, 0x2d, 0, 2, 1, 1);
		IPskKeyExchangeModesExtension e;
		
		e = (IPskKeyExchangeModesExtension)PskKeyExchangeModesExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array(data, 4), 2);
		assertSame(PskKeyExchangeModesExtension.class, e.getClass());
		assertSame(ExtensionType.PSK_KEY_EXCHANGE_MODES, e.getType());
		e.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(1, e.getModes().length);
		assertSame(PskKeyExchangeMode.PSK_DHE_KE, e.getModes()[0]);
		buffer.clear();
	}	
	
	void assertFailure(ByteBuffer[] array, int remaining, String message) {
		try {
			PskKeyExchangeModesExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array, remaining);
			fail();
		}
		catch (Alert e) {
			assertEquals(message, e.getMessage());
		}
	}

	@Test
	public void testParseFailures() {
		String inconsistentLength = "Extension 'psk_key_exchange_modes' parsing failure: Inconsistent length";
		assertFailure(array(bytes(1,1), 0), 0, inconsistentLength);
		assertFailure(array(bytes(1,1), 0), 1, inconsistentLength);
		assertFailure(array(bytes(0,0), 0), 1, inconsistentLength);
		assertFailure(array(bytes(1,1,0), 0), 3, inconsistentLength);
	}	
}
