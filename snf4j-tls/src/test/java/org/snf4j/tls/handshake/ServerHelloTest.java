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
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.alert.DecodeErrorAlertException;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.ExtensionDecoder;
import org.snf4j.tls.extension.ExtensionType;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.KeyShareExtension;
import org.snf4j.tls.extension.ServerNameExtension;

public class ServerHelloTest extends HandshakeTest {
	
	byte[] random = new byte[32];
	
	byte[] sessionId = new byte[32];

	List<IExtension> extensions = new ArrayList<IExtension>();
	
	ExtensionDecoder decoder = new ExtensionDecoder();

	@Override
	public void before() throws Exception {
		super.before();
		for (int i=0; i<32; ++i) {
			random[i] = (byte) i;
			sessionId[i] = (byte) (0x20 + i);
		}
		extensions.clear();
		extensions.add(new ServerNameExtension("abcdefgh"));
		decoder.clearParsers();
		decoder.addParser(ServerNameExtension.getParser());
		decoder.addParser(KeyShareExtension.getParser());
	}

	@Test
	public void testParseRealData() throws AlertException {
		byte[] data = bytes(new int[] {
				0x02,0x00,0x00,0x76,
				0x03,0x03,0x70,0x71,0x72,0x73,0x74,0x75,0x76,0x77,0x78,0x79,0x7a,0x7b,
				0x7c,0x7d,0x7e,0x7f,0x80,0x81,0x82,0x83,0x84,0x85,0x86,0x87,0x88,0x89,
				0x8a,0x8b,0x8c,0x8d,0x8e,0x8f,0x20,0xe0,0xe1,0xe2,0xe3,0xe4,0xe5,0xe6,
				0xe7,0xe8,0xe9,0xea,0xeb,0xec,0xed,0xee,0xef,0xf0,0xf1,0xf2,0xf3,0xf4,
				0xf5,0xf6,0xf7,0xf8,0xf9,0xfa,0xfb,0xfc,0xfd,0xfe,0xff,0x13,0x02,0x00,
				0x00,0x2e,0x00,0x2b,0x00,0x02,0x03,0x04,0x00,0x33,0x00,0x24,0x00,0x1d,
				0x00,0x20,0x9f,0xd7,0xad,0x6d,0xcf,0xf4,0x29,0x8d,0xd3,0xf9,0x6d,0x5b,
				0x1b,0x2a,0xf9,0x10,0xa0,0x53,0x5b,0x14,0x88,0xd7,0xf8,0xfa,0xbb,0x34,
				0x9a,0x98,0x28,0x80,0xb6,0x15			
		});
		
		IHandshake h = ServerHello.getParser().parse(array(data, 4), data.length-4, decoder);
		assertSame(HandshakeType.SERVER_HELLO, h.getType());
		ServerHello sh = (ServerHello) h;
		assertEquals(0x0303, sh.getLegacyVersion());
		assertArrayEquals(bytes(0x70,0x71,0x72,0x73,0x74,0x75,0x76,0x77,0x78,0x79,0x7a,0x7b,0x7c,0x7d,0x7e,0x7f,0x80,
				0x81,0x82,0x83,0x84,0x85,0x86,0x87,0x88,0x89,0x8a,0x8b,0x8c,0x8d,0x8e,0x8f), sh.getRandom());
		assertArrayEquals(bytes(0xe0,0xe1,0xe2,0xe3,0xe4,0xe5,0xe6,0xe7,0xe8,0xe9,
				0xea,0xeb,0xec,0xed,0xee,0xef,0xf0,0xf1,0xf2,0xf3,0xf4,0xf5,0xf6,
				0xf7,0xf8,0xf9,0xfa,0xfb,0xfc,0xfd,0xfe,0xff), sh.getLegacySessionId());
		assertEquals(0, sh.getLegacyCompressionMethod());
		assertSame(CipherSuite.TLS_AES_256_GCM_SHA384, sh.getCipherSuite());
		assertEquals(2, sh.getExtensioins().size());
		assertSame(ExtensionType.KEY_SHARE, sh.getExtensioins().get(1).getType());	
		
		assertEquals(data.length-4, sh.getDataLength());
		sh.getBytes(buffer);
		assertArrayEquals(data, buffer());
		assertEquals(data.length, sh.getLength());
	}

	@Test
	public void testMinValues() throws Exception {
		extensions.clear();
		ServerHello sh = new ServerHello(0x0301, random, new byte[0], CipherSuite.TLS_AES_256_GCM_SHA384, (byte)1, extensions);
		sh.getBytes(buffer);
		byte[] bytes = buffer();
		sh = (ServerHello) ServerHello.getParser().parse(array(bytes, 4), bytes.length-4, decoder);
		assertEquals(0, sh.getLegacySessionId().length);
		assertEquals(1, sh.getLegacyCompressionMethod());
		assertEquals(0, sh.getExtensioins().size());
		assertSame(CipherSuite.TLS_AES_256_GCM_SHA384, sh.getCipherSuite());
	}

	@Test
	public void testMaxValues() throws Exception {
		ServerHello sh = new ServerHello(0x0301, random, new byte[32], CipherSuite.TLS_AES_256_GCM_SHA384, (byte)-1, extensions);
		sh.getBytes(buffer);
		byte[] bytes = buffer();
		sh = (ServerHello) ServerHello.getParser().parse(array(bytes, 4), bytes.length-4, decoder);
		assertEquals(32, sh.getLegacySessionId().length);
		assertEquals(-1, sh.getLegacyCompressionMethod());
		assertEquals(1, sh.getExtensioins().size());
	}
	
	void assertIllegalArguments(int legacyVersion, byte[] random, byte[] legacySessionId, 
			CipherSuite cipherSuite, List<IExtension> extensions, String message) {
		try {
			new ServerHello(legacyVersion, random, legacySessionId, cipherSuite, (byte)0, extensions);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals(message, e.getMessage());
		}
	}

	@Test
	public void testIllegalArguments() {
		CipherSuite cipherSuite = CipherSuite.TLS_AES_256_GCM_SHA384;
		assertIllegalArguments(0, null, sessionId, cipherSuite, extensions, "random is null");
		assertIllegalArguments(0, new byte[31], sessionId, cipherSuite, extensions, "random's length is not 32");
		assertIllegalArguments(0, new byte[33], sessionId, cipherSuite, extensions, "random's length is not 32");
		assertIllegalArguments(0, random, null, cipherSuite, extensions, "legacySessionId is null");
		assertIllegalArguments(0, random, new byte[33], cipherSuite, extensions, "legacySessionId's length is greater than 32");
		assertIllegalArguments(0, random, sessionId, null, extensions, "cipherSuite is null");
		assertIllegalArguments(0, random, sessionId, cipherSuite, null, "extensions is null");
	}
	
	@Test
	public void testParser() {
		assertSame(HandshakeType.SERVER_HELLO, ServerHello.getParser().getType());
		assertSame(ServerHello.getParser().getType(), ServerHello.getParser().getType());
	}
	
	@Test
	public void testGetLegacyVersion() throws Exception {
		ServerHello sh = new ServerHello(0x0301, random, sessionId, CipherSuite.TLS_AES_256_GCM_SHA384, (byte)0, extensions);
		sh.getBytes(buffer);
		assertEquals(89, sh.getDataLength());
		assertArrayEquals(bytes(2,0,0,89,3,1), buffer(0,6));
		sh = (ServerHello) ServerHello.getParser().parse(array(buffer(), 4), 89, decoder);
		assertEquals(0x0301, sh.getLegacyVersion());
	}

	@Test
	public void testGetRandom() throws Exception {
		ServerHello ch = new ServerHello(0x0303, random, sessionId, CipherSuite.TLS_AES_256_GCM_SHA384, (byte)0, extensions);
		ch.getBytes(buffer);
		assertEquals(89, ch.getDataLength());
		assertArrayEquals(bytes(2,0,0,89,3,3), buffer(0,6));
		assertArrayEquals(random, buffer(6,32));
		ch = (ServerHello) ServerHello.getParser().parse(array(buffer(), 4), 89, decoder);
		assertArrayEquals(random, ch.getRandom());
	}
	
	@Test
	public void testGetLegacySessionId() throws Exception {
		ServerHello sh = new ServerHello(0x0303, random, sessionId, CipherSuite.TLS_AES_256_GCM_SHA384, (byte)0, extensions);
		sh.getBytes(buffer);
		assertEquals(89, sh.getDataLength());
		assertArrayEquals(bytes(2,0,0,89,3,3), buffer(0,6));
		assertArrayEquals(random, buffer(6,32));
		assertEquals(32, buffer()[38]);
		assertArrayEquals(sessionId, buffer(39,32));
		sh = (ServerHello) ServerHello.getParser().parse(array(buffer(), 4), 89, decoder);
		assertArrayEquals(sessionId, sh.getLegacySessionId());	
		
		buffer.clear();
		sh = new ServerHello(0x0303, random, bytes(70,721,72,73), CipherSuite.TLS_AES_256_GCM_SHA384, (byte)0, extensions);
		sh.getBytes(buffer);
		assertEquals(61, sh.getDataLength());
		assertArrayEquals(bytes(2,0,0,61,3,3), buffer(0,6));
		assertArrayEquals(random, buffer(6,32));
		assertEquals(4, buffer()[38]);
		assertArrayEquals(bytes(70,721,72,73), buffer(39,4));
		sh = (ServerHello) ServerHello.getParser().parse(array(buffer(), 4), 61, decoder);
		assertArrayEquals(bytes(70,721,72,73), sh.getLegacySessionId());	

		buffer.clear();
		sh = new ServerHello(0x0303, random, bytes(), CipherSuite.TLS_AES_256_GCM_SHA384, (byte)0, extensions);
		sh.getBytes(buffer);
		assertEquals(57, sh.getDataLength());
		assertArrayEquals(bytes(2,0,0,57,3,3), buffer(0,6));
		assertArrayEquals(random, buffer(6,32));
		assertEquals(0, buffer()[38]);
		sh = (ServerHello) ServerHello.getParser().parse(array(buffer(), 4), 57, decoder);
		assertArrayEquals(bytes(), sh.getLegacySessionId());	
	}
	
	@Test
	public void testGetCipherSuite() throws Exception {
		ServerHello sh = new ServerHello(0x0303, random, bytes(), CipherSuite.TLS_AES_256_GCM_SHA384, (byte)0, extensions);
		sh.getBytes(buffer);
		assertEquals(57, sh.getDataLength());
		assertArrayEquals(bytes(2,0,0,57,3,3), buffer(0,6));
		assertArrayEquals(random, buffer(6,32));
		assertEquals(0, buffer()[38]);
		assertArrayEquals(bytes(0x13,2), buffer(39,2));
		sh = (ServerHello) ServerHello.getParser().parse(array(buffer(), 4), 57, decoder);
		assertSame(CipherSuite.TLS_AES_256_GCM_SHA384, sh.getCipherSuite());
	}
	
	@Test
	public void testGetLegacyCompressionMethods() throws Exception {
		ServerHello sh = new ServerHello(0x0303, random, bytes(), CipherSuite.TLS_AES_256_GCM_SHA384, (byte)3, extensions);
		sh.getBytes(buffer);
		assertEquals(57, sh.getDataLength());
		assertArrayEquals(bytes(2,0,0,57,3,3), buffer(0,6));
		assertArrayEquals(random, buffer(6,32));
		assertEquals(0, buffer()[38]);
		assertArrayEquals(bytes(0x13,2), buffer(39,2));
		assertArrayEquals(bytes(3), buffer(41,1));
		sh = (ServerHello) ServerHello.getParser().parse(array(buffer(), 4), 57, decoder);
		assertEquals(3, sh.getLegacyCompressionMethod());	
	}
	
	@Test
	public void testGetExtensions() throws Exception {
		extensions.clear();
		ServerHello sh = new ServerHello(0x0303, random, bytes(), CipherSuite.TLS_AES_256_GCM_SHA384, (byte)3, extensions);
		sh.getBytes(buffer);
		assertEquals(40, sh.getDataLength());
		assertArrayEquals(bytes(2,0,0,40,3,3), buffer(0,6));
		assertArrayEquals(random, buffer(6,32));
		assertEquals(0, buffer()[38]);
		assertArrayEquals(bytes(0x13,2), buffer(39,2));
		assertArrayEquals(bytes(3), buffer(41,1));
		assertArrayEquals(bytes(0,0), buffer(42,2));
		assertEquals(44, buffer().length);
		sh = (ServerHello) ServerHello.getParser().parse(array(buffer(), 4), 40, decoder);
		assertEquals(0, sh.getExtensioins().size());	
		
		buffer.clear();
		extensions.add(new ServerNameExtension("A"));
		sh = new ServerHello(0x0303, random, bytes(), CipherSuite.TLS_AES_256_GCM_SHA384, (byte)3, extensions);
		sh.getBytes(buffer);
		assertEquals(50, sh.getDataLength());
		assertArrayEquals(bytes(2,0,0,50,3,3), buffer(0,6));
		assertArrayEquals(random, buffer(6,32));
		assertEquals(0, buffer()[38]);
		assertArrayEquals(bytes(0x13,2), buffer(39,2));
		assertArrayEquals(bytes(3), buffer(41,1));
		assertArrayEquals(bytes(0,10), buffer(42,2));
		assertEquals(54, buffer().length);
		sh = (ServerHello) ServerHello.getParser().parse(array(buffer(), 4), 50, decoder);
		assertEquals(1, sh.getExtensioins().size());
		assertEquals("A", ((ServerNameExtension)sh.getExtensioins().get(0)).getHostName());
	}

	@Test
	public void testParsingFailures() throws Exception {
		ServerHello ch = new ServerHello(0x0301, random, sessionId, CipherSuite.TLS_AES_256_GCM_SHA384, (byte)3, extensions);
		ch.getBytes(buffer);
		buffer.put((byte) 255);
		byte[] bytes = buffer();
		
		for (int i=4; i<=bytes.length; ++i) {
			try {
				ServerHello.getParser().parse(array(bytes, 4), i-4, decoder);
				if (i != bytes.length-1) {
					fail();
				}
			} catch (DecodeErrorAlertException e) {
				assertEquals("Handshake message 'server_hello' parsing failure: Inconsistent length", e.getMessage());
			}
		}
		
		bytes[4+34] = 33;
		try {
			ServerHello.getParser().parse(array(bytes, 4), bytes.length-5, decoder);
			fail();
		} catch (DecodeErrorAlertException e) {
			assertEquals("Handshake message 'server_hello' parsing failure: Legacy session id is too big", e.getMessage());
		}
		
	}
}
