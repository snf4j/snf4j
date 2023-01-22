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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.DecodeErrorAlert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.ExtensionDecoder;
import org.snf4j.tls.extension.ExtensionType;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.ServerNameExtension;

public class ClientHelloTest extends HandshakeTest {
	
	byte[] random = new byte[32];
	
	byte[] sessionId = new byte[32];

	byte[] compressions = new byte[1];
	
	CipherSuite[] cipherSuites;
	
	List<IExtension> extensions = new ArrayList<IExtension>();
	
	ExtensionDecoder decoder = new ExtensionDecoder();
	
	@Override
	public void before() throws Exception {
		super.before();
		for (int i=0; i<32; ++i) {
			random[i] = (byte) i;
			sessionId[i] = (byte) (0x20 + i);
		}
		cipherSuites = new CipherSuite[2];
		cipherSuites[0] = CipherSuite.TLS_AES_128_GCM_SHA256;
		cipherSuites[1] = CipherSuite.TLS_AES_256_GCM_SHA384;
		extensions.clear();
		extensions.add(new ServerNameExtension("abcdefgh"));
		decoder.clearParsers();
		decoder.addParser(ServerNameExtension.getParser());
	}

	@Test
	public void testParseRealData() throws Alert {
		byte[] data = bytes(new int[] {
				0x01,0x00,0x00,0xf4,
				0x03,0x03,0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0a,0x0b,
				0x0c,0x0d,0x0e,0x0f,0x10,0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,
				0x1a,0x1b,0x1c,0x1d,0x1e,0x1f,0x20,0xe0,0xe1,0xe2,0xe3,0xe4,0xe5,0xe6,
				0xe7,0xe8,0xe9,0xea,0xeb,0xec,0xed,0xee,0xef,0xf0,0xf1,0xf2,0xf3,0xf4,
				0xf5,0xf6,0xf7,0xf8,0xf9,0xfa,0xfb,0xfc,0xfd,0xfe,0xff,0x00,0x08,0x13,
				0x02,0x13,0x03,0x13,0x01,0x00,0xff,0x01,0x00,0x00,0xa3,0x00,0x00,0x00,
				0x18,0x00,0x16,0x00,0x00,0x13,0x65,0x78,0x61,0x6d,0x70,0x6c,0x65,0x2e,
				0x75,0x6c,0x66,0x68,0x65,0x69,0x6d,0x2e,0x6e,0x65,0x74,0x00,0x0b,0x00,
				0x04,0x03,0x00,0x01,0x02,0x00,0x0a,0x00,0x16,0x00,0x14,0x00,0x1d,0x00,
				0x17,0x00,0x1e,0x00,0x19,0x00,0x18,0x01,0x00,0x01,0x01,0x01,0x02,0x01,
				0x03,0x01,0x04,0x00,0x23,0x00,0x00,0x00,0x16,0x00,0x00,0x00,0x17,0x00,
				0x00,0x00,0x0d,0x00,0x1e,0x00,0x1c,0x04,0x03,0x05,0x03,0x06,0x03,0x08,
				0x07,0x08,0x08,0x08,0x09,0x08,0x0a,0x08,0x0b,0x08,0x04,0x08,0x05,0x08,
				0x06,0x04,0x01,0x05,0x01,0x06,0x01,0x00,0x2b,0x00,0x03,0x02,0x03,0x04,
				0x00,0x2d,0x00,0x02,0x01,0x01,0x00,0x33,0x00,0x26,0x00,0x24,0x00,0x1d,
				0x00,0x20,0x35,0x80,0x72,0xd6,0x36,0x58,0x80,0xd1,0xae,0xea,0x32,0x9a,
				0xdf,0x91,0x21,0x38,0x38,0x51,0xed,0x21,0xa2,0x8e,0x3b,0x75,0xe9,0x65,
				0xd0,0xd2,0xcd,0x16,0x62,0x54
				});
		
		IHandshake h = ClientHello.getParser().parse(array(data, 4), data.length-4, decoder);
		assertSame(HandshakeType.CLIENT_HELLO, h.getType());
		ClientHello ch = (ClientHello) h;
		assertEquals(0x0303, ch.getLegacyVersion());
		assertArrayEquals(bytes(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,
				17,18,19,20,21,22,23,24,25,26,27,28,29,30,31), ch.getRandom());
		assertArrayEquals(bytes(0xe0,0xe1,0xe2,0xe3,0xe4,0xe5,0xe6,0xe7,0xe8,0xe9,
				0xea,0xeb,0xec,0xed,0xee,0xef,0xf0,0xf1,0xf2,0xf3,0xf4,0xf5,0xf6,
				0xf7,0xf8,0xf9,0xfa,0xfb,0xfc,0xfd,0xfe,0xff), ch.getLegacySessionId());
		assertArrayEquals(bytes(0), ch.getLegacyCompressionMethods());
		assertEquals(4, ch.getCipherSuites().length);
		assertSame(CipherSuite.TLS_AES_256_GCM_SHA384, ch.getCipherSuites()[0]);
		assertSame(CipherSuite.TLS_CHACHA20_POLY1305_SHA256, ch.getCipherSuites()[1]);
		assertSame(CipherSuite.TLS_AES_128_GCM_SHA256, ch.getCipherSuites()[2]);
		assertEquals(0x00ff, ch.getCipherSuites()[3].value());
		assertEquals(10, ch.getExtensioins().size());
		assertSame(ExtensionType.SERVER_NAME, ch.getExtensioins().get(0).getType());	
		
		assertEquals(data.length-4, ch.getDataLength());
		ch.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(data.length, ch.getLength());
	}
	
	@Test
	public void testMinValues() throws Exception {
		cipherSuites = new CipherSuite[0];
		extensions.clear();
		ClientHello ch = new ClientHello(0x0301, random, new byte[0], cipherSuites, new byte[0], extensions);
		ch.getBytes(buffer);
		byte[] bytes = buffer();
		ch = (ClientHello) ClientHello.getParser().parse(array(bytes, 4), bytes.length-4, decoder);
		assertEquals(0, ch.getLegacySessionId().length);
		assertEquals(0, ch.getLegacyCompressionMethods().length);
		assertEquals(0, ch.getExtensioins().size());
		assertEquals(0, ch.getCipherSuites().length);
	}

	@Test
	public void testMaxValues() throws Exception {
		ClientHello ch = new ClientHello(0x0301, random, new byte[32], cipherSuites, new byte[255], extensions);
		ch.getBytes(buffer);
		byte[] bytes = buffer();
		ch = (ClientHello) ClientHello.getParser().parse(array(bytes, 4), bytes.length-4, decoder);
		assertEquals(32, ch.getLegacySessionId().length);
		assertEquals(255, ch.getLegacyCompressionMethods().length);
		assertEquals(1, ch.getExtensioins().size());
		assertEquals(2, ch.getCipherSuites().length);
	}
	
	void assertIllegalArguments(int legacyVersion, byte[] random, byte[] legacySessionId, 
			CipherSuite[] cipherSuites, byte[] legacyCompressionMethods, 
			List<IExtension> extensions, String message) {
		try {
			new ClientHello(legacyVersion, random, legacySessionId, cipherSuites, legacyCompressionMethods, extensions);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals(message, e.getMessage());
		}
	}
	
	@Test
	public void testIllegalArguments() {
		assertIllegalArguments(0, null, sessionId, cipherSuites, compressions, extensions, "random is null");
		assertIllegalArguments(0, new byte[31], sessionId, cipherSuites, compressions, extensions, "random's length is not 32");
		assertIllegalArguments(0, new byte[33], sessionId, cipherSuites, compressions, extensions, "random's length is not 32");
		assertIllegalArguments(0, random, null, cipherSuites, compressions, extensions, "legacySessionId is null");
		assertIllegalArguments(0, random, new byte[33], cipherSuites, compressions, extensions, "legacySessionId's length is greater than 32");
		assertIllegalArguments(0, random, sessionId, null, compressions, extensions, "cipherSuites is null");
		assertIllegalArguments(0, random, sessionId, cipherSuites, null, extensions, "legacyCompressionMethods is null");
		assertIllegalArguments(0, random, sessionId, cipherSuites, new byte[256], extensions, "legacyCompressionMethods's length is greater than 255");
		assertIllegalArguments(0, random, sessionId, cipherSuites, compressions, null, "extensions is null");
	}
	
	@Test
	public void testParser() {
		assertSame(HandshakeType.CLIENT_HELLO, ClientHello.getParser().getType());
		assertSame(ClientHello.getParser().getType(), ClientHello.getParser().getType());
	}
	
	@Test
	public void testGetLegacyVersion() throws Exception {
		ClientHello ch = new ClientHello(0x0301, random, sessionId, cipherSuites, compressions, extensions);
		ch.getBytes(buffer);
		assertEquals(94, ch.getDataLength());
		assertArrayEquals(bytes(1,0,0,94,3,1), buffer(0,6));
		ch = (ClientHello) ClientHello.getParser().parse(array(buffer(), 4), 94, decoder);
		assertEquals(0x0301, ch.getLegacyVersion());
	}
	
	@Test
	public void testGetRandom() throws Exception {
		ClientHello ch = new ClientHello(0x0303, random, sessionId, cipherSuites, compressions, extensions);
		ch.getBytes(buffer);
		assertEquals(94, ch.getDataLength());
		assertArrayEquals(bytes(1,0,0,94,3,3), buffer(0,6));
		assertArrayEquals(random, buffer(6,32));
		ch = (ClientHello) ClientHello.getParser().parse(array(buffer(), 4), 94, decoder);
		assertArrayEquals(random, ch.getRandom());
	}
	
	@Test
	public void testGetLegacySessionId() throws Exception {
		ClientHello ch = new ClientHello(0x0303, random, sessionId, cipherSuites, compressions, extensions);
		ch.getBytes(buffer);
		assertEquals(94, ch.getDataLength());
		assertArrayEquals(bytes(1,0,0,94,3,3), buffer(0,6));
		assertArrayEquals(random, buffer(6,32));
		assertEquals(32, buffer()[38]);
		assertArrayEquals(sessionId, buffer(39,32));
		ch = (ClientHello) ClientHello.getParser().parse(array(buffer(), 4), 94, decoder);
		assertArrayEquals(sessionId, ch.getLegacySessionId());	
		
		buffer.clear();
		ch = new ClientHello(0x0303, random, bytes(70,721,72,73), cipherSuites, compressions, extensions);
		ch.getBytes(buffer);
		assertEquals(66, ch.getDataLength());
		assertArrayEquals(bytes(1,0,0,66,3,3), buffer(0,6));
		assertArrayEquals(random, buffer(6,32));
		assertEquals(4, buffer()[38]);
		assertArrayEquals(bytes(70,721,72,73), buffer(39,4));
		ch = (ClientHello) ClientHello.getParser().parse(array(buffer(), 4), 66, decoder);
		assertArrayEquals(bytes(70,721,72,73), ch.getLegacySessionId());	

		buffer.clear();
		ch = new ClientHello(0x0303, random, bytes(), cipherSuites, compressions, extensions);
		ch.getBytes(buffer);
		assertEquals(62, ch.getDataLength());
		assertArrayEquals(bytes(1,0,0,62,3,3), buffer(0,6));
		assertArrayEquals(random, buffer(6,32));
		assertEquals(0, buffer()[38]);
		ch = (ClientHello) ClientHello.getParser().parse(array(buffer(), 4), 62, decoder);
		assertArrayEquals(bytes(), ch.getLegacySessionId());	
	}
	
	@Test
	public void testGetCipherSuites() throws Exception {
		ClientHello ch = new ClientHello(0x0303, random, bytes(), cipherSuites, compressions, extensions);
		ch.getBytes(buffer);
		assertEquals(62, ch.getDataLength());
		assertArrayEquals(bytes(1,0,0,62,3,3), buffer(0,6));
		assertArrayEquals(random, buffer(6,32));
		assertEquals(0, buffer()[38]);
		assertArrayEquals(bytes(0,4,0x13,1,0x13,2), buffer(39,6));
		ch = (ClientHello) ClientHello.getParser().parse(array(buffer(), 4), 62, decoder);
		assertEquals(2, ch.getCipherSuites().length);	
		assertSame(CipherSuite.TLS_AES_128_GCM_SHA256, ch.getCipherSuites()[0]);
		assertSame(CipherSuite.TLS_AES_256_GCM_SHA384, ch.getCipherSuites()[1]);
		
		buffer.clear();
		cipherSuites = new CipherSuite[0];
		ch = new ClientHello(0x0303, random, bytes(), cipherSuites, compressions, extensions);
		ch.getBytes(buffer);
		assertEquals(58, ch.getDataLength());
		assertArrayEquals(bytes(1,0,0,58,3,3), buffer(0,6));
		assertArrayEquals(random, buffer(6,32));
		assertEquals(0, buffer()[38]);
		assertArrayEquals(bytes(0,0), buffer(39,2));
		ch = (ClientHello) ClientHello.getParser().parse(array(buffer(), 4), 58, decoder);
		assertEquals(0, ch.getCipherSuites().length);	
		
		buffer.clear();
		cipherSuites = new CipherSuite[0xfffe/2];
		for (int i=0; i<0xfffe/2; ++i) {
			cipherSuites[i] = CipherSuite.of(i);
		}
		ch = new ClientHello(0x0303, random, bytes(), cipherSuites, compressions, extensions);
		ch.getBytes(buffer);
		assertEquals(58+0xfffe, ch.getDataLength());
		assertArrayEquals(bytes(1,1,0,56,3,3), buffer(0,6));
		assertArrayEquals(random, buffer(6,32));
		assertEquals(0, buffer()[38]);
		assertArrayEquals(bytes(0xff,0xfe,0,0,0,1), buffer(39,6));
		ch = (ClientHello) ClientHello.getParser().parse(array(buffer(), 4), 58+0xfffe, decoder);
		assertEquals(0xfffe/2, ch.getCipherSuites().length);	
	}
	
	@Test
	public void testGetLegacyCompressionMethods() throws Exception {
		cipherSuites = new CipherSuite[0];
		ClientHello ch = new ClientHello(0x0303, random, bytes(), cipherSuites, compressions, extensions);
		ch.getBytes(buffer);
		assertEquals(58, ch.getDataLength());
		assertArrayEquals(bytes(1,0,0,58,3,3), buffer(0,6));
		assertArrayEquals(random, buffer(6,32));
		assertEquals(0, buffer()[38]);
		assertArrayEquals(bytes(0,0), buffer(39,2));
		assertArrayEquals(bytes(1,0), buffer(41,2));
		ch = (ClientHello) ClientHello.getParser().parse(array(buffer(), 4), 58, decoder);
		assertArrayEquals(bytes(0), ch.getLegacyCompressionMethods());	
		
		buffer.clear();
		ch = new ClientHello(0x0303, random, bytes(), cipherSuites, new byte[0], extensions);
		ch.getBytes(buffer);
		assertEquals(57, ch.getDataLength());
		assertArrayEquals(bytes(1,0,0,57,3,3), buffer(0,6));
		assertArrayEquals(random, buffer(6,32));
		assertEquals(0, buffer()[38]);
		assertArrayEquals(bytes(0,0), buffer(39,2));
		assertArrayEquals(bytes(0), buffer(41,1));
		ch = (ClientHello) ClientHello.getParser().parse(array(buffer(), 4), 57, decoder);
		assertArrayEquals(bytes(), ch.getLegacyCompressionMethods());	
		
		buffer.clear();
		byte[] bytes = new byte[255];
		for (int i=0; i<255; ++i) {
			bytes[i] = (byte) i;
		}
		ch = new ClientHello(0x0303, random, bytes(), cipherSuites, bytes, extensions);
		ch.getBytes(buffer);
		assertEquals(57+255, ch.getDataLength());
		assertArrayEquals(bytes(1,0,1,56,3,3), buffer(0,6));
		assertArrayEquals(random, buffer(6,32));
		assertEquals(0, buffer()[38]);
		assertArrayEquals(bytes(0,0), buffer(39,2));
		assertArrayEquals(bytes(255), buffer(41,1));
		assertArrayEquals(bytes, buffer(42,255));
		ch = (ClientHello) ClientHello.getParser().parse(array(buffer(), 4), 57+255, decoder);
		assertArrayEquals(bytes, ch.getLegacyCompressionMethods());	
	}
	
	@Test
	public void testGetExtensions() throws Exception {
		cipherSuites = new CipherSuite[0];
		extensions.clear();
		ClientHello ch = new ClientHello(0x0303, random, bytes(), cipherSuites, compressions, extensions);
		ch.getBytes(buffer);
		assertEquals(41, ch.getDataLength());
		assertArrayEquals(bytes(1,0,0,41,3,3), buffer(0,6));
		assertArrayEquals(random, buffer(6,32));
		assertEquals(0, buffer()[38]);
		assertArrayEquals(bytes(0,0), buffer(39,2));
		assertArrayEquals(bytes(1,0), buffer(41,2));
		assertArrayEquals(bytes(0,0), buffer(43,2));
		assertEquals(45, buffer().length);
		ch = (ClientHello) ClientHello.getParser().parse(array(buffer(), 4), 41, decoder);
		assertEquals(0, ch.getExtensioins().size());	
		
		buffer.clear();
		extensions.add(new ServerNameExtension("A"));
		ch = new ClientHello(0x0303, random, bytes(), cipherSuites, compressions, extensions);
		ch.getBytes(buffer);
		assertEquals(51, ch.getDataLength());
		assertArrayEquals(bytes(1,0,0,51,3,3), buffer(0,6));
		assertArrayEquals(random, buffer(6,32));
		assertEquals(0, buffer()[38]);
		assertArrayEquals(bytes(0,0), buffer(39,2));
		assertArrayEquals(bytes(1,0), buffer(41,2));
		assertArrayEquals(bytes(0,10), buffer(43,2));
		assertEquals(55, buffer().length);
		ch = (ClientHello) ClientHello.getParser().parse(array(buffer(), 4), 51, decoder);
		assertEquals(1, ch.getExtensioins().size());
		assertEquals("A", ((ServerNameExtension)ch.getExtensioins().get(0)).getHostName());
	}
	
	@Test
	public void testParsingFailures() throws Exception {
		ClientHello ch = new ClientHello(0x0301, random, sessionId, cipherSuites, compressions, extensions);
		ch.getBytes(buffer);
		buffer.put((byte) 255);
		byte[] bytes = buffer();
		
		for (int i=4; i<=bytes.length; ++i) {
			try {
				ClientHello.getParser().parse(array(bytes, 4), i-4, decoder);
				if (i != bytes.length-1) {
					fail();
				}
			} catch (DecodeErrorAlert e) {
				assertEquals("Handshake message 'client_hello' parsing failure: Inconsistent length", e.getMessage());
			}
		}
		
		bytes[4+34] = 33;
		try {
			ClientHello.getParser().parse(array(bytes, 4), bytes.length-5, decoder);
			fail();
		} catch (DecodeErrorAlert e) {
			assertEquals("Handshake message 'client_hello' parsing failure: Legacy session id is too big", e.getMessage());
		}
		bytes[4+34] = 32;
		ClientHello.getParser().parse(array(bytes, 4), bytes.length-5, decoder);
		
		assertEquals(0, bytes[4+34+32+1]);
		assertEquals(4, bytes[4+34+32+2]);
		bytes[4+34+32+2] = 5;
		try {
			ClientHello.getParser().parse(array(bytes, 4), bytes.length-5, decoder);
			fail();
		} catch (DecodeErrorAlert e) {
			assertEquals("Handshake message 'client_hello' parsing failure: Cipher suites invalid length", e.getMessage());
		}
		bytes[4+34+32+2] = 3;
		try {
			ClientHello.getParser().parse(array(bytes, 4), bytes.length-5, decoder);
			fail();
		} catch (DecodeErrorAlert e) {
			assertEquals("Handshake message 'client_hello' parsing failure: Cipher suites invalid length", e.getMessage());
		}
		bytes[4+34+32+2] = 4;
		ClientHello.getParser().parse(array(bytes, 4), bytes.length-5, decoder);

		assertEquals(1, bytes[4+34+32+7]);
		assertEquals(0, bytes[4+34+32+8]);
		bytes[4+34+32+7] = 21;
		try {
			ClientHello.getParser().parse(array(bytes, 4), bytes.length-5, decoder);
			fail();
		} catch (DecodeErrorAlert e) {
			assertEquals("Handshake message 'client_hello' parsing failure: Inconsistent length", e.getMessage());
		}
		bytes[4+34+32+7] = 1;
		ClientHello.getParser().parse(array(bytes, 4), bytes.length-5, decoder);
	}
	
}
