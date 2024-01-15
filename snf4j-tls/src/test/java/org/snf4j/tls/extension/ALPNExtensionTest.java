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
import java.util.Arrays;

import org.junit.Test;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.DecodeErrorAlert;
import org.snf4j.tls.handshake.HandshakeType;

public class ALPNExtensionTest extends ExtensionTest {
	
	@Test
	public void testGetBytes() {
		ALPNExtension e = new ALPNExtension("http/1.1");
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0, 16, 0, 11, 0, 9, 8, 0x68, 0x74, 0x74, 0x70, 0x2f, 0x31, 0x2e, 0x31), buffer());
		assertEquals(11, e.getDataLength());
		assertArrayEquals(new String[] {"http/1.1"}, e.getProtocolNames());
		buffer.clear();

		e = new ALPNExtension("1","23");
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0, 16, 0, 7, 0, 5, 1, 0x31, 2, 0x32, 0x33), buffer());
		assertEquals(7, e.getDataLength());
		assertArrayEquals(new String[] {"1", "23"}, e.getProtocolNames());
		buffer.clear();

		e = new ALPNExtension(bytes(0x68, 0x74, 0x74, 0x70, 0x2f, 0x31, 0x2e, 0x31));
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0, 16, 0, 11, 0, 9, 8, 0x68, 0x74, 0x74, 0x70, 0x2f, 0x31, 0x2e, 0x31), buffer());
		assertEquals(11, e.getDataLength());
		assertArrayEquals(new String[] {"http/1.1"}, e.getProtocolNames());
		buffer.clear();

		e = new ALPNExtension(bytes(0x31), bytes(0x32, 0x33));
		assertTrue(e.isKnown());
		e.getBytes(buffer);
		assertArrayEquals(bytes(0, 16, 0, 7, 0, 5, 1, 0x31, 2, 0x32, 0x33), buffer());
		assertEquals(7, e.getDataLength());
		assertArrayEquals(new String[] {"1", "23"}, e.getProtocolNames());
		buffer.clear();
	}
	
	@Test(expected = NullPointerException.class) 
	public void testNullArgument1() {
		new ALPNExtension((String)null);
	}

	@Test(expected = NullPointerException.class) 
	public void testNullArgument2() {
		new ALPNExtension((byte[])null);
	}

	@Test(expected = IllegalArgumentException.class) 
	public void testIllegalArgument1() {
		new ALPNExtension(new String[0]);
	}

	@Test(expected = IllegalArgumentException.class) 
	public void testIllegalArgument2() {
		new ALPNExtension(new byte[0][]);
	}

	@Test(expected = IllegalArgumentException.class) 
	public void testIllegalArgument3() {
		new ALPNExtension((byte[][])null);
	}

	@Test(expected = IllegalArgumentException.class) 
	public void testIllegalArgument4() {
		new ALPNExtension((String[])null);
	}
	
	@Test
	public void testParser() {
		assertSame(ExtensionType.APPLICATION_LAYER_PROTOCOL_NEGOTIATION, ALPNExtension.getParser().getType());
		assertSame(ALPNExtension.getParser().getType(), ALPNExtension.getParser().getType());
	}
	
	@Test
	public void testParse() throws Exception {
		byte[] data = new byte[] {0, 16, 0, 11, 0, 9, 8, 0x68, 0x74, 0x74, 0x70, 0x2f, 0x31, 0x2e, 0x31};
		
		IExtension e = ALPNExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array(data, 4), 11);
		assertSame(ALPNExtension.class, e.getClass());
		assertSame(ExtensionType.APPLICATION_LAYER_PROTOCOL_NEGOTIATION, e.getType());
		e.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertArrayEquals(new String[] {"http/1.1"}, ((IALPNExtension)e).getProtocolNames());
		buffer.clear();

		e = ALPNExtension.getParser().parse(HandshakeType.ENCRYPTED_EXTENSIONS, array(data, 4), 11);
		assertSame(ALPNExtension.class, e.getClass());
		assertSame(ExtensionType.APPLICATION_LAYER_PROTOCOL_NEGOTIATION, e.getType());
		e.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertArrayEquals(new String[] {"http/1.1"}, ((IALPNExtension)e).getProtocolNames());
		buffer.clear();
		
		data = new byte[] {0, 16, 0, 7, 0, 5, 1, 0x31, 2, 0x32, 0x33};
		
		e = ALPNExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array(data, 4), 7);
		assertSame(ALPNExtension.class, e.getClass());
		assertSame(ExtensionType.APPLICATION_LAYER_PROTOCOL_NEGOTIATION, e.getType());
		e.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertArrayEquals(new String[] {"1","23"}, ((IALPNExtension)e).getProtocolNames());
		buffer.clear();

		try {
			e = ALPNExtension.getParser().parse(HandshakeType.ENCRYPTED_EXTENSIONS, array(data, 4), 7);
			fail();
		}
		catch (DecodeErrorAlert ex) {
			assertEquals("Extension 'application_layer_protocol_negotiation' parsing failure: Too many protocol names", ex.getMessage());
		}
		
		byte[] name = new byte[255];
		Arrays.fill(name, (byte)'1');
		data = cat(bytes(0, 16, 1, 2, 1, 0, 255), name);
		e = ALPNExtension.getParser().parse(HandshakeType.CLIENT_HELLO, array(data, 4), 256+2);
		e.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertArrayEquals(new String[] {new String(name)}, ((IALPNExtension)e).getProtocolNames());
		buffer.clear();
	}
	
	void assertFailure(ByteBuffer[] array, boolean clientHello, int remaining, String message) {
		try {
			ALPNExtension.getParser().parse(clientHello 
					? HandshakeType.CLIENT_HELLO 
					: HandshakeType.ENCRYPTED_EXTENSIONS, array, remaining);
			fail();
		}
		catch (Alert e) {
			assertEquals(message, e.getMessage());
		}
	}

	@Test
	public void testParseFailures() {
		String msg = "Extension 'application_layer_protocol_negotiation' parsing failure: Inconsistent length";
		assertFailure(array(bytes(0, 5, 1, 11, 2, 12, 13), 0), true, 0, msg);
		assertFailure(array(bytes(0, 5, 1, 11, 2, 12, 13), 0), true, 1, msg);
		assertFailure(array(bytes(0, 5, 1, 11, 2, 12, 13), 0), true, 2, msg);
		assertFailure(array(bytes(0, 5, 1, 11, 2, 12, 13), 0), true, 3, msg);
		assertFailure(array(bytes(0, 5, 1, 11, 2, 12, 13), 0), true, 4, msg);
		assertFailure(array(bytes(0, 5, 1, 11, 2, 12, 13), 0), true, 5, msg);
		assertFailure(array(bytes(0, 5, 1, 11, 2, 12, 13), 0), true, 6, msg);
		assertFailure(array(bytes(0, 5, 1, 11, 2, 12, 13), 0), true, 8, msg);
		assertFailure(array(bytes(0, 5, 1, 11, 3, 12, 13), 0), true, 7, msg);

		msg = "Extension 'application_layer_protocol_negotiation' parsing failure: No protocol names";
		assertFailure(array(bytes(0, 0), 0), true, 2, msg);
		msg = "Extension 'application_layer_protocol_negotiation' parsing failure: Empty protocol name";
		assertFailure(array(bytes(0, 1, 0), 0), true, 3, msg);
		assertFailure(array(bytes(0, 3, 1, 0, 0), 0), true, 5, msg);
		
		
		
	}
	
}
