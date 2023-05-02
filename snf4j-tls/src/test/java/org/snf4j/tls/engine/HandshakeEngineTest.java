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
package org.snf4j.tls.engine;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.alert.IllegalParameterAlert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.CookieExtension;
import org.snf4j.tls.extension.ExtensionDecoder;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.PskKeyExchangeMode;
import org.snf4j.tls.extension.ServerNameExtension;
import org.snf4j.tls.extension.SignatureScheme;
import org.snf4j.tls.handshake.ClientHello;
import org.snf4j.tls.handshake.HandshakeDecoder;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.ServerHello;
import org.snf4j.tls.handshake.ServerHelloRandom;
import org.snf4j.tls.handshake.TestHandshakeParser;
import org.snf4j.tls.record.RecordType;

public class HandshakeEngineTest extends EngineTest {

	@Test
	public void testStart() throws Exception {
		TestParameters params = new TestParameters();
		HandshakeEngine he = new HandshakeEngine(true, params, handler, handler);
		
		assertFalse(params.isCompatibilityMode());
		assertArrayEquals(params.getNamedGroups(), new NamedGroup[] {
				NamedGroup.SECP256R1, 
				NamedGroup.SECP384R1});
		assertArrayEquals(params.getSignatureSchemes(), new SignatureScheme[] {
				SignatureScheme.ECDSA_SECP256R1_SHA256, 
				SignatureScheme.ECDSA_SECP384R1_SHA384});
		
		assertFalse(he.getState().isStarted());
		he.start();
		assertTrue(he.getState().isStarted());
		try {
			he.start();
			fail();
		} catch (InternalErrorAlert e) {}
		ProducedHandshake[] produced = he.produce();
		assertEquals(0, produced.length);
		he.getTask().run();
		produced = he.produce();
		assertEquals(1, produced.length);
		assertProduced(produced[0], HandshakeType.CLIENT_HELLO, RecordType.INITIAL);
		
		ClientHello ch = (ClientHello) produced[0].getHandshake();
		
		assertEquals(0x0303, ch.getLegacyVersion());
		assertEquals(32, ch.getRandom().length);
		assertEquals(0, ch.getLegacySessionId().length);
		assertCipherSuites(ch, CipherSuite.TLS_AES_256_GCM_SHA384, CipherSuite.TLS_AES_128_GCM_SHA256);
		assertArrayEquals(bytes(0), ch.getLegacyCompressionMethods());
		assertEquals(5, ch.getExtensions().size());
		assertSupportedVersions(ch, 0x0304);
		assertSupportedGroups(ch, NamedGroup.SECP256R1, NamedGroup.SECP384R1);
		assertSignatureAlgorithms(ch, SignatureScheme.ECDSA_SECP256R1_SHA256, SignatureScheme.ECDSA_SECP384R1_SHA384);
		assertKeyShare(ch, false, NamedGroup.SECP256R1);
		assertPskModes(ch, PskKeyExchangeMode.PSK_DHE_KE);
		
		params.cipherSuites = new CipherSuite[] {CipherSuite.TLS_AES_128_GCM_SHA256};
		params.compatibilityMode = true;
		params.numberOfOfferedSharedKeys = 2;
		params.peerHost = "snf4j.org";
		params.signatureSchemes = new SignatureScheme[] {SignatureScheme.ECDSA_SECP384R1_SHA384};
		params.pskKeyExchangeModes = new PskKeyExchangeMode[0];
		he = new HandshakeEngine(true, params, handler, handler);
		he.start();
		produced = he.produce();
		assertEquals(0, produced.length);
		he.getTask().run();
		produced = he.produce();
		assertEquals(1, produced.length);

		ch = (ClientHello) produced[0].getHandshake();
		
		assertEquals(32, ch.getLegacySessionId().length);
		assertCipherSuites(ch, CipherSuite.TLS_AES_128_GCM_SHA256);
		assertEquals(5, ch.getExtensions().size());
		assertSupportedVersions(ch, 0x0304);
		assertSupportedGroups(ch, NamedGroup.SECP256R1, NamedGroup.SECP384R1);
		assertSignatureAlgorithms(ch, SignatureScheme.ECDSA_SECP384R1_SHA384);
		assertKeyShare(ch, false, NamedGroup.SECP256R1, NamedGroup.SECP384R1);
		assertServerName(ch, "snf4j.org");
		
		byte[] id1 = ch.getLegacySessionId();
		he = new HandshakeEngine(true, params, handler, handler);
		he.start();
		produced = he.produce();
		assertEquals(0, produced.length);
		he.getTask().run();
		produced = he.produce();
		assertEquals(1, produced.length);
		ch = (ClientHello) produced[0].getHandshake();
		byte[] id2 = ch.getLegacySessionId();
		assertEquals(id1.length, id2.length);
		int count = 0;
		for (int i=0; i<id1.length; ++i) {
			if (id1[i] != id2[i]) {
				++count;
			}
		}
		assertTrue(count > 0);

		params.namedGroups = new NamedGroup[] {new NamedGroup("XXX", 32, null) {}};
		he = new HandshakeEngine(true, params, handler, handler);
		try {
			he.start();
		} catch (InternalErrorAlert e) {}		
	}
	
	@Test
	public void testConsume() throws Exception {
		TestParameters params = new TestParameters();
		HandshakeEngine he = new HandshakeEngine(true, params, handler, handler);
		he.start();
		he.getTask().run();
		ClientHello ch = (ClientHello) he.produce()[0].getHandshake();
		ch.getBytes(buffer);
		byte[] data = buffer();
		ByteBuffer[] array;
		
		HandshakeEngine he2 = new HandshakeEngine(false, params, handler, handler);
		he2.start();
		array = array(data, 0);
		he2.consume(array, data.length);
		he2.getTask().run();
		he2.getTask().run();
		assertEquals(0, ByteBufferArray.wrap(array).remaining());
		ProducedHandshake[] produced = he2.produce();
		assertEquals(5, produced.length);
		ServerHello sh = (ServerHello) produced[0].getHandshake();
		assertTrue(sh.isPrepared());
		
		he2 = new HandshakeEngine(false, params, handler, handler);
		he2.start();
		array = array(data, 0, 10,10);
		he2.consume(array, data.length);
		he2.getTask().run();
		he2.getTask().run();
		assertEquals(0, ByteBufferArray.wrap(array).remaining());
		produced = he2.produce();
		assertEquals(5, produced.length);
		assertNotNull((ServerHello) produced[0].getHandshake());

		he2 = new HandshakeEngine(false, params, handler, handler);
		he2.start();
		array = array(Arrays.copyOf(data, data.length+1), 0);
		he2.consume(array, data.length);
		he2.getTask().run();
		he2.getTask().run();
		assertEquals(1, ByteBufferArray.wrap(array).remaining());
		assertEquals(5, he2.produce().length);
		
		he2 = new HandshakeEngine(false, params, handler, handler);
		he2.start();
		array = array(Arrays.copyOf(data, data.length+1), 0, 10, 10);
		he2.consume(array, data.length);
		he2.getTask().run();
		he2.getTask().run();
		assertEquals(1, ByteBufferArray.wrap(array).remaining());
		assertEquals(5, he2.produce().length);

		he2 = new HandshakeEngine(false, params, handler, handler);
		he2.start();
		array = array(Arrays.copyOf(cat(bytes(0,0),data), data.length+3), 0);
		array[0].getShort();
		he2.consume(array, data.length);
		he2.getTask().run();
		he2.getTask().run();
		assertEquals(1, ByteBufferArray.wrap(array).remaining());
		assertEquals(5, he2.produce().length);
		
		he2 = new HandshakeEngine(false, params, handler, handler);
		he2.start();
		array = array(Arrays.copyOf(cat(bytes(0,0),data), data.length+3), 0, 10, 10);
		array[0].getShort();
		he2.consume(array, data.length);
		he2.getTask().run();
		he2.getTask().run();
		assertEquals(1, ByteBufferArray.wrap(array).remaining());
		assertEquals(5, he2.produce().length);

		he2 = new HandshakeEngine(false, params, handler, handler);
		he2.start();
		array = array(Arrays.copyOf(cat(bytes(0,0),data), data.length+3), 0, 1,1, 10, 10);
		ByteBufferArray bArray = ByteBufferArray.wrap(array);
		bArray.getShort();
		he2.consume(array, data.length);
		he2.getTask().run();
		he2.getTask().run();
		assertEquals(1, ByteBufferArray.wrap(array).remaining());
		assertEquals(5, he2.produce().length);
		
		he2 = new HandshakeEngine(false, params, handler, handler);
		he2.start();
		array = array(data, 0);
		try {
			he2.consume(array, data.length+1);
			fail();
		} catch(RuntimeException e) {}

		he2 = new HandshakeEngine(false, params, handler, handler);
		he2.start();
		array = array(data, 0, 10);
		try {
			he2.consume(array, data.length+1);
			fail();
		} catch (RuntimeException e) {}
		
		he2 = new HandshakeEngine(false, params, handler, handler);
		he2.start();
		data[0] = 99;
		array = array(data, 0);
		try {
			he2.consume(array, data.length);
			fail();
		} catch (UnexpectedMessageAlert e) {}
		data[0] = 1;
	}
	
	@Test
	public void testConsumeNoConsumer() throws Exception {
		TestParameters params = new TestParameters();
		params.delegatedTaskMode = DelegatedTaskMode.CERTIFICATES;
		HandshakeEngine he = new HandshakeEngine(true, params, handler, handler);
		he.start();
		he.produce()[0].getHandshake().getBytes(buffer);
		byte[] data = buffer();

		HandshakeDecoder decoder = new HandshakeDecoder(ExtensionDecoder.DEFAULT);
		decoder.addParser(new TestHandshakeParser(HandshakeType.of(127), true));
		decoder.addParser(new TestHandshakeParser(HandshakeType.of(255), true));
		
		he = new HandshakeEngine(false, params, handler, handler, decoder);
		data[0] = -1;
		try {
			he.consume(array(data,0), data.length);
		} catch (UnexpectedMessageAlert e) {}
		he = new HandshakeEngine(false, params, handler, handler, decoder);
		data[0] = 127;
		try {
			he.consume(array(data,0), data.length);
		} catch (UnexpectedMessageAlert e) {}
	}
	
	@Test
	public void testConsumeNoAllowedExtension() throws Exception {
		List<IExtension> extensions = new ArrayList<IExtension>();
		
		extensions.add(new CookieExtension(new byte[10]));
		
		ServerHello sh = new ServerHello(
				0x0303,  
				new byte[32], 
				new byte[0],
				CipherSuite.TLS_AES_128_GCM_SHA256,
				(byte)0, extensions);
		sh.getBytes(buffer);
		byte[] data = buffer();
		buffer.clear();
		
		TestParameters params = new TestParameters();
		HandshakeEngine he = new HandshakeEngine(false, params, handler, handler);
		try {
			he.consume(array(data,0), data.length);
			fail();
		} catch (IllegalParameterAlert e) {}
		
		sh = new ServerHello(
				0x0303,  
				ServerHelloRandom.getHelloRetryRequestRandom(), 
				new byte[0],
				CipherSuite.TLS_AES_128_GCM_SHA256,
				(byte)0, extensions);
		sh.getBytes(buffer);
		data = buffer();
		buffer.clear();

		he = new HandshakeEngine(false, params, handler, handler);
		try {
			he.consume(array(data,0), data.length);
			fail();
		} catch (UnexpectedMessageAlert e) {
			assertEquals("Unexpected ServerHello", e.getMessage());
		}
		
		extensions.clear();
		extensions.add(new ServerNameExtension("rrrr"));
	
		sh = new ServerHello(
				0x0303,  
				ServerHelloRandom.getHelloRetryRequestRandom(), 
				new byte[0],
				CipherSuite.TLS_AES_128_GCM_SHA256,
				(byte)0, extensions);
		sh.getBytes(buffer);
		data = buffer();
		buffer.clear();

		he = new HandshakeEngine(false, params, handler, handler);
		try {
			he.consume(array(data,0), data.length);
			fail();
		} catch (IllegalParameterAlert e) {}
		
	}
}
