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
package org.snf4j.tls.handshake;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.alert.DecodeErrorAlertException;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.ExtensionDecoder;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.ServerNameExtension;

public class HandshakeDecoderTest extends HandshakeTest {
	
	final ExtensionDecoder decoder0 = new ExtensionDecoder();
	
	final HandshakeDecoder decoder = new HandshakeDecoder(decoder0);
	
	@Override
	public void before() throws Exception {
		super.before();
		decoder0.clearParsers();
		decoder0.addParser(ServerNameExtension.getParser());
		decoder.clearParsers();
		decoder.addParser(ClientHello.getParser());
	}
	
	@Test
	public void testDecode() throws AlertException {
		CipherSuite[] ciphers = new CipherSuite[] {CipherSuite.TLS_AES_128_GCM_SHA256};
		List<IExtension> extensions = Arrays.asList(new IExtension[0]);
		ClientHello ch = new ClientHello(0x303, new byte[32], new byte[0], ciphers, new byte[1], extensions);
		ch.getBytes(buffer);
		byte[] data = buffer();
		IHandshake h = decoder.decode(array(data,0), ch.getLength());
		assertSame(ClientHello.class, h.getClass());
		assertTrue(h.isKnown());
		ch = (ClientHello) h;
		assertEquals(0x303, ch.getLegacyVersion());
		assertEquals(0, ch.getExtensioins().size());

		try {
			decoder.decode(array(data,0), ch.getLength()-1);
			fail();
		} catch (AlertException e) {
			assertEquals("Handshake message 'client_hello' parsing failure: Data underflow", e.getMessage());
		}
		try {
			decoder.decode(array(Arrays.copyOf(data, data.length+1),0), ch.getLength()+1);
			fail();
		} catch (AlertException e) {
			assertEquals("Handshake message 'client_hello' parsing failure: Inconsistent length", e.getMessage());
		}
		
		buffer.clear();
		buffer.put(bytes(230,0,0,2,1,2));
		h = decoder.decode(array(buffer(),0), 6);
		assertSame(UnknownHandshake.class, h.getClass());
		UnknownHandshake uh = (UnknownHandshake) h;
		assertArrayEquals(bytes(1,2), uh.getData());
		assertFalse(h.isKnown());
		assertEquals(230, h.getType().value());
		
		buffer.clear();
		buffer.put(bytes(231,1,2,3));
		buffer.put(new byte[66051]);
		uh = (UnknownHandshake) decoder.decode(array(buffer(),0), 66055);
		assertFalse(uh.isKnown());
		assertEquals(231, uh.getType().value());
		assertEquals(66051, uh.getData().length);

		ByteBufferArray a = ByteBufferArray.wrap(array(bytes(230,0,0,2,1,2),0));
		uh = (UnknownHandshake) decoder.decode(a, (int)a.remaining());
		assertEquals(0, a.remaining());
	}
	
	void assertFailure(ByteBuffer[] array, int remaining, String message) {
		try {
			decoder.decode(array, remaining);
			fail();
		} catch (AlertException e) {
			assertEquals(message, e.getMessage());
		}
	}
	
	@Test
	public void testDecodeFailure() throws DecodeErrorAlertException {
		assertFailure(array(bytes(0,0,0,0), 0), 0, "Handshake message parsing failure: Data underflow");
		assertFailure(array(bytes(0,0,0,0), 0), 1, "Handshake message parsing failure: Data underflow");
		assertFailure(array(bytes(0,0,0,0), 0), 2, "Handshake message parsing failure: Data underflow");
		assertFailure(array(bytes(0,0,0,0), 0), 3, "Handshake message parsing failure: Data underflow");
		
		assertFailure(array(bytes(1,0,0,1), 0), 4, "Handshake message 'client_hello' parsing failure: Data underflow");
		assertFailure(array(bytes(230,0,0,1), 0), 4, "Handshake message 'unknown' parsing failure: Data underflow");
	}	
	
	@Test
	public void testParserManagement() throws AlertException {
		decoder.clearParsers();
		assertEquals(0, decoder.getParsers().size());
		
		IHandshake e = decoder.decode(array(bytes(0,0,0,0), 0), 4);
		assertFalse(e.isKnown());
		decoder.addParser(ClientHello.getParser());
		assertEquals(1, decoder.getParsers().size());
		e = decoder.decode(array(bytes(0,0,0,0), 0), 4);
		assertFalse(e.isKnown());
		assertTrue(decoder.hasParser(HandshakeType.CLIENT_HELLO));
		assertFalse(decoder.hasParser(HandshakeType.FINISHED));
		
		decoder.addParser(new TestHandshakeParser(HandshakeType.FINISHED));
		assertTrue(decoder.hasParser(HandshakeType.CLIENT_HELLO));
		assertTrue(decoder.hasParser(HandshakeType.FINISHED));
		assertEquals(2, decoder.getParsers().size());
		e = decoder.decode(array(bytes(44,0,0,0), 0), 4);
		assertEquals(44, e.getType().value());
		e = decoder.decode(array(bytes(0,0,0,0), 0), 4);
		assertEquals(0, e.getType().value());
		
		assertSame(ClientHello.getParser(), decoder.removeParser(HandshakeType.CLIENT_HELLO));
		assertEquals(1, decoder.getParsers().size());
		assertFalse(decoder.hasParser(HandshakeType.CLIENT_HELLO));
		assertTrue(decoder.hasParser(HandshakeType.FINISHED));
		assertEquals(20, decoder.getParsers().get(0).getType().value());
	}

}
