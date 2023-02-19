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

public class PreSharedKeyExtensionTest extends ExtensionTest {
	
	@Test
	public void testGetBytes() {
		PreSharedKeyExtension e = new PreSharedKeyExtension(33);
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,41,0,2,0,33), buffer());
		assertEquals(2, e.getDataLength());
		assertEquals(33, e.getSelectedIdentity());
		assertEquals(0, e.getOfferedPsks().length);
		buffer.clear();
		
		OfferedPsk psk1 = new OfferedPsk(new PskIdentity(bytes(1,2,3), 0x10111213), bytes(4,5,6,7));
		OfferedPsk psk2 = new OfferedPsk(new PskIdentity(bytes(4,5), 88), bytes(8,9,10));

		e = new PreSharedKeyExtension(psk1);
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,41,0,18,0,9,0,3,1,2,3,16,17,18,19,0,5,4,4,5,6,7), buffer());
		assertEquals(18, e.getDataLength());
		assertEquals(-1, e.getSelectedIdentity());
		assertEquals(1, e.getOfferedPsks().length);
		buffer.clear();
		
		e = new PreSharedKeyExtension(psk1,psk2);
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,41,0,30,0,17,0,3,1,2,3,16,17,18,19,0,2,4,5,0,0,0,88,0,9,4,4,5,6,7,3,8,9,10), buffer());
		assertEquals(30, e.getDataLength());
		assertEquals(-1, e.getSelectedIdentity());
		assertEquals(2, e.getOfferedPsks().length);
	}
	
	@Test(expected = IllegalArgumentException.class) 
	public void testInvalidArgument1() {
		new PreSharedKeyExtension(-1);
	}

	@Test(expected = IllegalArgumentException.class) 
	public void testInvalidArgument2() {
		new PreSharedKeyExtension(0x10000);
	}

	@Test(expected = IllegalArgumentException.class) 
	public void testInvalidArgument3() {
		new PreSharedKeyExtension();
	}
	
	@Test
	public void testParser() {
		assertSame(ExtensionType.PRE_SHARED_KEY, PreSharedKeyExtension.getParser().getType());
		assertSame(PreSharedKeyExtension.getParser().getType(), PreSharedKeyExtension.getParser().getType());
	}
	
	@Test
	public void testParseRealData() throws Alert {
		byte[] data = bytes(0,41,0,2,0,0);
		IPreSharedKeyExtension e;
		
		e = (IPreSharedKeyExtension)PreSharedKeyExtension.getParser().parse(HandshakeType.SERVER_HELLO, array(data, 4), 2);
		assertSame(PreSharedKeyExtension.class, e.getClass());
		assertSame(ExtensionType.PRE_SHARED_KEY, e.getType());
		e.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(0, e.getSelectedIdentity());
		assertEquals(0, e.getOfferedPsks().length);
		buffer.clear();

		data = bytes(0,41,0,2,255,255);
		e = (IPreSharedKeyExtension)PreSharedKeyExtension.getParser().parse(HandshakeType.SERVER_HELLO, array(data, 4), 2);
		assertEquals(0xffff, e.getSelectedIdentity());
		assertEquals(0, e.getOfferedPsks().length);
		buffer.clear();
		
		data = bytes(0,41,0,20,0,10,0,4,21,22,23,24,16,17,18,19,0,6,5,1,2,3,4,5);
		e = (IPreSharedKeyExtension)PreSharedKeyExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array(data, 4), 20);
		e.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(-1, e.getSelectedIdentity());
		assertEquals(1, e.getOfferedPsks().length);
		assertArrayEquals(bytes(21,22,23,24), e.getOfferedPsks()[0].getIdentity().getIdentity());
		assertEquals(0x10111213L, e.getOfferedPsks()[0].getIdentity().getObfuscatedTicketAge());
		assertArrayEquals(bytes(1,2,3,4,5), e.getOfferedPsks()[0].getBinder());
		buffer.clear();
		
		data = bytes(0,41,0,30,0,17,0,3,1,2,3,16,17,18,19,0,2,4,5,0,0,0,88,0,9,4,4,5,6,7,3,8,9,10);
		e = (IPreSharedKeyExtension)PreSharedKeyExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array(data, 4), 30);
		e.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(-1, e.getSelectedIdentity());
		assertEquals(2, e.getOfferedPsks().length);
		assertArrayEquals(bytes(1,2,3), e.getOfferedPsks()[0].getIdentity().getIdentity());
		assertEquals(0x10111213L, e.getOfferedPsks()[0].getIdentity().getObfuscatedTicketAge());
		assertArrayEquals(bytes(4,5,6,7), e.getOfferedPsks()[0].getBinder());
		assertArrayEquals(bytes(4,5), e.getOfferedPsks()[1].getIdentity().getIdentity());
		assertEquals(88L, e.getOfferedPsks()[1].getIdentity().getObfuscatedTicketAge());
		assertArrayEquals(bytes(8,9,10), e.getOfferedPsks()[1].getBinder());
		buffer.clear();
	}
	
	@Test
	public void testConsumeBytes() throws Alert {
		ByteBufferArray array = ByteBufferArray.wrap(array(bytes(0,0),0));
		
		assertEquals(2, array.remaining());
		IExtension e = PreSharedKeyExtension.getParser().parse(HandshakeType.SERVER_HELLO, array, 2);
		assertNotNull(e);
		assertEquals(0, array.remaining());
		
		array = ByteBufferArray.wrap(array(bytes(0,10,0,4,21,22,23,24,16,17,18,19,0,6,5,1,2,3,4,5),0));
		assertEquals(20, array.remaining());
		e = PreSharedKeyExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array, 20);
		assertNotNull(e);
		assertEquals(0, array.remaining());
	}

	void assertFailure(ByteBuffer[] array, int remaining, String message, boolean client) {
		try {
			PreSharedKeyExtension.getParser().parse(client ? HandshakeType.CLIENT_HELLO : HandshakeType.SERVER_HELLO, array, remaining);
			fail();
		}
		catch (Alert e) {
			assertEquals(message, e.getMessage());
		}
	}

	@Test
	public void testParseFailures() {
		String inconsistentLength = "Extension 'pre_shared_key' parsing failure: Inconsistent length";
		String noIdentities = "Extension 'pre_shared_key' parsing failure: No identities";
		String differentNumbers = "Extension 'pre_shared_key' parsing failure: Different numbers of binders and identities";
		assertFailure(array(bytes(1,2), 0), 0, inconsistentLength, false);
		assertFailure(array(bytes(1,2), 0), 1, inconsistentLength, false);
		assertFailure(array(bytes(1,2,3), 0), 3, inconsistentLength, false);
		
		byte[] data = bytes(0,10,0,4,21,22,23,24,16,17,18,19,0,6,5,1,2,3,4,5,0);
		for (int i=0; i<data.length; ++i) {
			if (i != data.length-1) {
				assertFailure(array(data, 0), i, inconsistentLength, true);
			}
		}
		data = bytes(0,10,0,4,21,22,23,24,16,17,18,19,0,6,5,1,2,3,4,5);
		data = bytes(0,9,0,4,21,22,23,24,16,17,18,19,0,6,5,1,2,3,4,5);
		assertFailure(array(data, 0), data.length, inconsistentLength, true);
		data = bytes(0,10,0,3,21,22,23,24,16,17,18,19,0,6,5,1,2,3,4,5);
		assertFailure(array(data, 0), data.length, inconsistentLength, true);
		data = bytes(0,10,0,4,21,22,23,24,16,17,18,19,0,5,5,1,2,3,4,5);
		assertFailure(array(data, 0), data.length, inconsistentLength, true);
		data = bytes(0,10,0,4,21,22,23,24,16,17,18,19,0,6,4,1,2,3,4,5);
		assertFailure(array(data, 0), data.length, inconsistentLength, true);
		data = bytes(0,0,0,6,5,1,2,3,4,5);
		assertFailure(array(data, 0), data.length, noIdentities, true);
		data = bytes(0,10,0,4,21,22,23,24,16,17,18,19,0,8,5,1,2,3,4,5,1,1);
		assertFailure(array(data, 0), data.length, differentNumbers, true);

	}
}
