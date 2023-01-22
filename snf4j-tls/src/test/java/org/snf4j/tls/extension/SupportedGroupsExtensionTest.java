/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022-2023 SNF4J contributors
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

public class SupportedGroupsExtensionTest extends ExtensionTest {

	@Test
	public void testGetBytes() {
		SupportedGroupsExtension e = new SupportedGroupsExtension(new NamedGroup[] {NamedGroup.SECP256R1});
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,10,0,4,0,2,0,0x17), buffer());
		assertEquals(4, e.getDataLength());
		assertEquals(1, e.getGroups().length);
		assertEquals(ExtensionType.SUPPORTED_GROUPS, e.getType());
		assertEquals(NamedGroup.SECP256R1, e.getGroups()[0]);
		buffer.clear();
		
		e = new SupportedGroupsExtension(new NamedGroup[] {NamedGroup.SECP256R1, NamedGroup.FFDHE2048});
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,10,0,6,0,4,0,0x17,1,0), buffer());
		assertEquals(6, e.getDataLength());
		assertEquals(2, e.getGroups().length);
		assertEquals(NamedGroup.SECP256R1, e.getGroups()[0]);
		assertEquals(NamedGroup.FFDHE2048, e.getGroups()[1]);
	}	
	
	@Test(expected = IllegalArgumentException.class) 
	public void testNullArgument() {
		new SupportedGroupsExtension((NamedGroup[])null);
	}

	@Test(expected = IllegalArgumentException.class) 
	public void testEmptyArgument() {
		new SupportedGroupsExtension(new NamedGroup[0]);
	}
	
	@Test
	public void testParser() {
		assertSame(ExtensionType.SUPPORTED_GROUPS, SupportedGroupsExtension.getParser().getType());
		assertSame(SupportedGroupsExtension.getParser().getType(), SupportedGroupsExtension.getParser().getType());
	}
	
	@Test
	public void testParseRealData() throws Alert {
		byte[] data = new byte[] {
				0x00, 0x0a, 0x00, 0x16, 0x00, 0x14, 0x00, 0x1d, 0x00, 0x17, 0x00, 0x1e, 0x00, 0x19, 
				0x00, 0x18, 0x01, 0x00, 0x01, 0x01, 0x01, 0x02, 0x01, 0x03, 0x01, 0x04};
		
		SupportedGroupsExtension e = (SupportedGroupsExtension) SupportedGroupsExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array(data, 4), data.length-4);
		assertSame(SupportedGroupsExtension.class, e.getClass());
		assertSame(ExtensionType.SUPPORTED_GROUPS, e.getType());
		e.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(10, e.getGroups().length);
		assertSame(NamedGroup.X25519, e.getGroups()[0]);
		assertSame(NamedGroup.SECP256R1, e.getGroups()[1]);
		assertSame(NamedGroup.X448, e.getGroups()[2]);
		assertSame(NamedGroup.SECP521R1, e.getGroups()[3]);
		assertSame(NamedGroup.SECP384R1, e.getGroups()[4]);
		assertSame(NamedGroup.FFDHE2048, e.getGroups()[5]);
		assertSame(NamedGroup.FFDHE3072, e.getGroups()[6]);
		assertSame(NamedGroup.FFDHE4096, e.getGroups()[7]);
		assertSame(NamedGroup.FFDHE6144, e.getGroups()[8]);
		assertSame(NamedGroup.FFDHE8192, e.getGroups()[9]);
	}	
	
	@Test
	public void testConsumeBytes() throws Alert {
		ByteBufferArray array = ByteBufferArray.wrap(array(bytes(0,4,0,0x17,0,0x18),0));
		
		assertEquals(6, array.remaining());
		IExtension e = SupportedGroupsExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array, 6);
		assertNotNull(e);
		assertEquals(0, array.remaining());
	}
	
	void assertFailure(ByteBuffer[] array, int remaining, String message) {
		try {
			SupportedGroupsExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array, remaining);
			fail();
		}
		catch (Alert e) {
			assertEquals(message, e.getMessage());
		}
	}

	@Test
	public void testParseFailures() {
		String inconsistentLength = "Extension 'supported_groups' parsing failure: Inconsistent length";
		String incorrectLength = "Extension 'supported_groups' parsing failure: Incorrect length";
		
		assertFailure(array(bytes(0,4,0,0x17,0,0x18), 0), 0, inconsistentLength);
		assertFailure(array(bytes(0,4,0,0x17,0,0x18), 0), 3, inconsistentLength);
		assertFailure(array(bytes(0,0), 0), 2, inconsistentLength);
		assertFailure(array(bytes(0,0,0,0), 0), 4, incorrectLength);
		assertFailure(array(bytes(0,3,0,0x17,0), 0), 5, incorrectLength);
		assertFailure(array(bytes(0,4,0,0x17,0,0x18), 0), 5, inconsistentLength);
		assertFailure(array(bytes(0,4,0,0x17,0,0x18), 0), 7, inconsistentLength);
	}	

}
