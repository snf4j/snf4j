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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Test;
import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.handshake.HandshakeType;

public class ServerNameExtenstionTest extends ExtensionTest {
	
	@Test
	public void testGetBytes() {
		ServerNameExtension e = new ServerNameExtension();
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,0,0,0), buffer());
		assertEquals(0, e.getDataLength());
		assertEquals("", e.getHostName());
		buffer.clear();
		
		e = new ServerNameExtension("a");
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,0,0,6,0,4,0,0,1,97), buffer());
		assertEquals(6, e.getDataLength());
		assertEquals("a", e.getHostName());
		buffer.clear();
		
		e = new ServerNameExtension("ab");
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,0,0,7,0,5,0,0,2,97,98), buffer());
		assertEquals(7, e.getDataLength());
		assertEquals("ab", e.getHostName());
		buffer.clear();

		e = new ServerNameExtension(bytes(97));
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,0,0,6,0,4,0,0,1,97), buffer());
		assertEquals(6, e.getDataLength());
		assertEquals("a", e.getHostName());
		buffer.clear();
		
		e = new ServerNameExtension(bytes(97,98,99));
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0,0,0,8,0,6,0,0,3,97,98,99), buffer());
		assertEquals(8, e.getDataLength());
		assertEquals("abc", e.getHostName());
		buffer.clear();
	}
	
	@Test
	public void testGetMaxBytes() {
		byte[] expected = new byte[0xffff+4];
		Arrays.fill(expected, (byte)'a');
		String name = new String(expected,9,0xffff-5, StandardCharsets.US_ASCII);
		System.arraycopy(bytes(0,0,255,255,255,253,0,255,250), 0, expected, 0, 9);

		ServerNameExtension e = new ServerNameExtension(name);
		e.getBytes(buffer);
		assertArrayEquals(expected, buffer());
		assertEquals(name, e.getHostName());
		assertEquals(0xffff, e.getDataLength());
		buffer.clear();

		e = new ServerNameExtension(name.getBytes(StandardCharsets.US_ASCII));
		e.getBytes(buffer);
		assertArrayEquals(expected, buffer());
		assertEquals(name, e.getHostName());
		assertEquals(0xffff, e.getDataLength());
		buffer.clear();
		
		try {
			new ServerNameExtension(name+'a');
			fail();
		} catch (IllegalArgumentException ex) {
			assertEquals("hostName length is greater than 65530", ex.getMessage());
		}

		try {
			new ServerNameExtension((name+'a').getBytes(StandardCharsets.US_ASCII));
			fail();
		} catch (IllegalArgumentException ex) {
			assertEquals("hostName length is greater than 65530", ex.getMessage());
		}
	}	
	
	@Test(expected = NullPointerException.class) 
	public void testNullArgument1() {
		new ServerNameExtension((String)null);
	}

	@Test(expected = NullPointerException.class) 
	public void testNullArgument2() {
		new ServerNameExtension((byte[])null);
	}
	
	@Test(expected = IllegalArgumentException.class) 
	public void testEmptyArgument1() {
		new ServerNameExtension("");
	}

	@Test(expected = IllegalArgumentException.class) 
	public void testEmptyArgument2() {
		new ServerNameExtension(new byte[0]);
	}
	
	@Test
	public void testParser() {
		assertSame(ExtensionType.SERVER_NAME, ServerNameExtension.getParser().getType());
		assertSame(ServerNameExtension.getParser().getType(), ServerNameExtension.getParser().getType());
	}
	
	@Test
	public void testParseRealData() throws AlertException {
		byte[] data = new byte[] {
				0x00, 0x00, 0x00, 0x18, 0x00, 0x16, 0x00, 0x00, 0x13, 0x65, 0x78, 0x61, 0x6d, 0x70, 
				0x6c, 0x65, 0x2e, 0x75, 0x6c, 0x66, 0x68, 0x65, 0x69, 0x6d, 0x2e, 0x6e, 0x65, 0x74};
		
		IExtension e = ServerNameExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array(data, 4), 0x18);
		
		assertSame(ServerNameExtension.class, e.getClass());
		assertSame(ExtensionType.SERVER_NAME, e.getType());
		e.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals("example.ulfheim.net", ((ServerNameExtension)e).getHostName());
		buffer.clear();
		
		data = new byte[] {0x00, 0x00, 0x00, 0x00};
		e = ServerNameExtension.getParser().parse(HandshakeType.CLIENT_HELLO, new ByteBuffer[] {ByteBuffer.wrap(data, 4, data.length-4)}, 0);
		e.getBytes(buffer);
		assertArrayEquals(data, buffer());
	}
	
	@Test
	public void testConsumeBytes() throws AlertException {
		ByteBufferArray array = ByteBufferArray.wrap(array(bytes(0,4,0,0,1,97),0));
		
		assertEquals(6, array.remaining());
		IExtension e = ServerNameExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array, 6);
		assertNotNull(e);
		assertEquals(0, array.remaining());
	}
	
	void assertFailure(ByteBuffer[] array, int remaining, String message) {
		try {
			ServerNameExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array, remaining);
			fail();
		}
		catch (AlertException e) {
			assertEquals(message, e.getMessage());
		}
	}
	
	@Test
	public void testParseFailures() {
		assertFailure(array(bytes(0,4,0,0,1,97), 0), 1, "Extension 'server_name' parsing failure: Inconsistent length");
		assertFailure(array(bytes(0,4,0,0,1,97), 0), 2, "Extension 'server_name' parsing failure: Inconsistent length");
		assertFailure(array(bytes(0,4,0,0,1,97), 0), 3, "Extension 'server_name' parsing failure: Inconsistent length");
		assertFailure(array(bytes(0,4,0,0,1,97), 0), 4, "Extension 'server_name' parsing failure: Inconsistent length");
		assertFailure(array(bytes(0,4,0,0,1,97), 0), 5, "Extension 'server_name' parsing failure: Inconsistent length");
		assertFailure(array(bytes(0,2,0,0,1,97), 0), 4, "Extension 'server_name' parsing failure: Inconsistent length");
		assertFailure(array(bytes(0,1,0,0,1,97), 0), 3, "Extension 'server_name' parsing failure: Inconsistent length");
		assertFailure(array(bytes(0,0,0,0,1,97), 0), 2, "Extension 'server_name' parsing failure: Inconsistent length");
		assertFailure(array(bytes(0,5,0,0,1,97,98), 0), 7, "Extension 'server_name' parsing failure: Inconsistent length");
		assertFailure(array(bytes(0,4,0,0,1,97,98), 0), 7, "Extension 'server_name' parsing failure: Inconsistent length");

		assertFailure(array(bytes(0,3,0,0,0,97), 0), 5, "Extension 'server_name' parsing failure: Empty name");
		assertFailure(array(bytes(0,4,0,0,0,97), 0), 6, "Extension 'server_name' parsing failure: Empty name");

		assertFailure(array(bytes(0,3,0,0,1,97), 0), 5, "Extension 'server_name' parsing failure: Inconsistent name length");

		assertFailure(array(bytes(0,4,1,0,1,97), 0), 6, "Extension 'server_name' parsing failure: Invalid name type");
	}
}
