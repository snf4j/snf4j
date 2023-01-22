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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.DecodeErrorAlert;
import org.snf4j.tls.handshake.HandshakeType;

public class ExtensionDecoderTest extends ExtensionTest {

	final ExtensionDecoder decoder = new ExtensionDecoder();
	
	@Override
	public void before() throws Exception {
		super.before();
		decoder.clearParsers();
		decoder.addParser(ServerNameExtension.getParser());
	}
	
	@Test
	public void testDecode() throws Alert {
		new ServerNameExtension("abc").getBytes(buffer);
		IExtension e = decoder.decode(HandshakeType.CLIENT_HELLO, array(buffer(),0), 100);
		assertSame(ServerNameExtension.class, e.getClass());
		assertEquals("abc", ((ServerNameExtension)e).getHostName());
		assertTrue(e.isKnown());
		
		buffer.put(bytes(1,0,0,2,1,2));
		ByteBufferArray a = ByteBufferArray.wrap(array(buffer(),0));
		e = decoder.decode(HandshakeType.CLIENT_HELLO, a, 100);
		assertSame(ServerNameExtension.class, e.getClass());
		e = decoder.decode(HandshakeType.CLIENT_HELLO, a, 100);
		assertSame(UnknownExtension.class, e.getClass());
		assertEquals(0x0100, e.getType().value());
		assertFalse(e.isKnown());
		UnknownExtension ue = (UnknownExtension) e;
		assertArrayEquals(bytes(1,2), ue.getData());
		
		a = ByteBufferArray.wrap(array(bytes(0,0,0,0),0));
		e = decoder.decode(HandshakeType.CLIENT_HELLO, a, 4);
		assertSame(ServerNameExtension.class, e.getClass());
		assertEquals(0, a.remaining());
		assertEquals("", ((ServerNameExtension)e).getHostName());
	}
	
	void assertFailure(ByteBuffer[] array, int remaining, String message) {
		try {
			decoder.decode(HandshakeType.CLIENT_HELLO, array, remaining);
			fail();
		} catch (Alert e) {
			assertEquals(message, e.getMessage());
		}
	}
	
	@Test
	public void testDecodeFailure() throws DecodeErrorAlert {
		assertFailure(array(bytes(0,0,0,0), 0), 0, "Extension parsing failure: Data underflow");
		assertFailure(array(bytes(0,0,0,0), 0), 1, "Extension parsing failure: Data underflow");
		assertFailure(array(bytes(0,0,0,0), 0), 2, "Extension parsing failure: Data underflow");
		assertFailure(array(bytes(0,0,0,0), 0), 3, "Extension parsing failure: Data underflow");
		
		assertFailure(array(bytes(0,0,0,1), 0), 4, "Extension 'server_name' parsing failure: Data underflow");
		assertFailure(array(bytes(1,0,0,1), 0), 4, "Extension 'unknown' parsing failure: Data underflow");
		assertFailure(array(bytes(0,14,0,1), 0), 4, "Extension 'use_srtp' parsing failure: Data underflow");
		assertFailure(array(bytes(0,0,0,2,0), 0), 5, "Extension 'server_name' parsing failure: Data underflow");

		assertFailure(array(bytes(0,0,0,1,0), 0), 5, "Extension 'server_name' parsing failure: Inconsistent length");
	}
	
	@Test
	public void testParserManagement() throws Alert {
		decoder.clearParsers();
		assertEquals(0, decoder.getParsers().size());
		
		IExtension e = decoder.decode(HandshakeType.CLIENT_HELLO, array(bytes(0,0,0,0), 0), 4);
		assertFalse(e.isKnown());
		decoder.addParser(ServerNameExtension.getParser());
		assertEquals(1, decoder.getParsers().size());
		e = decoder.decode(HandshakeType.CLIENT_HELLO, array(bytes(0,0,0,0), 0), 4);
		assertTrue(e.isKnown());
		assertTrue(decoder.hasParser(ExtensionType.SERVER_NAME));
		assertFalse(decoder.hasParser(ExtensionType.COOKIE));
		
		decoder.addParser(new TestExtensionParser(ExtensionType.COOKIE));
		assertTrue(decoder.hasParser(ExtensionType.SERVER_NAME));
		assertTrue(decoder.hasParser(ExtensionType.COOKIE));
		assertEquals(2, decoder.getParsers().size());
		e = decoder.decode(HandshakeType.CLIENT_HELLO, array(bytes(0,44,0,0), 0), 4);
		assertEquals(44, e.getType().value());
		e = decoder.decode(HandshakeType.CLIENT_HELLO, array(bytes(0,0,0,0), 0), 4);
		assertEquals(0, e.getType().value());
		
		assertSame(ServerNameExtension.getParser(), decoder.removeParser(ExtensionType.SERVER_NAME));
		assertEquals(1, decoder.getParsers().size());
		assertFalse(decoder.hasParser(ExtensionType.SERVER_NAME));
		assertTrue(decoder.hasParser(ExtensionType.COOKIE));
		assertEquals(44, decoder.getParsers().get(0).getType().value());
	}
}
