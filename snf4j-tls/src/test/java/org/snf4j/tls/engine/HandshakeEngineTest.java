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
package org.snf4j.tls.engine;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.tls.alert.InternalErrorAlertException;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.SignatureScheme;
import org.snf4j.tls.handshake.ClientHello;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.record.RecordType;

public class HandshakeEngineTest extends EngineTest {

	@Test
	public void testStart() throws Exception {
		TestParameters params = new TestParameters();
		HandshakeEngine he = new HandshakeEngine(true, params);
		
		assertFalse(params.isCompatibilityMode());
		assertArrayEquals(params.getNamedGroups(), new NamedGroup[] {
				NamedGroup.SECP256R1, 
				NamedGroup.SECP384R1});
		assertArrayEquals(params.getSignatureSchemes(), new SignatureScheme[] {
				SignatureScheme.ECDSA_SECP256R1_SHA256, 
				SignatureScheme.ECDSA_SECP384R1_SHA384});
		
		assertFalse(he.isStarted());
		he.start();
		assertTrue(he.isStarted());
		try {
			he.start();
			fail();
		} catch (InternalErrorAlertException e) {}
		ProducedHandshake[] produced = he.produce();
		assertEquals(1, produced.length);
		assertProduced(produced[0], HandshakeType.CLIENT_HELLO, RecordType.INITIAL);
		
		ClientHello ch = (ClientHello) produced[0].getHandshake();
		
		assertEquals(0x0303, ch.getLegacyVersion());
		assertEquals(32, ch.getRandom().length);
		assertEquals(0, ch.getLegacySessionId().length);
		assertCipherSuites(ch, CipherSuite.TLS_AES_256_GCM_SHA384, CipherSuite.TLS_AES_128_GCM_SHA256);
		assertArrayEquals(bytes(0), ch.getLegacyCompressionMethods());
		assertEquals(4, ch.getExtensioins().size());
		assertSupportedVersions(ch, 0x0304);
		assertSupportedGroups(ch, NamedGroup.SECP256R1, NamedGroup.SECP384R1);
		assertSignatureAlgorithms(ch, SignatureScheme.ECDSA_SECP256R1_SHA256, SignatureScheme.ECDSA_SECP384R1_SHA384);
		assertKeyShare(ch, false, NamedGroup.SECP256R1);
		
		params.cipherSuites = new CipherSuite[] {CipherSuite.TLS_AES_128_GCM_SHA256};
		params.compatibilityMode = true;
		params.numberOfOfferedSharedKeys = 2;
		params.serverName = "snf4j.org";
		params.signatureSchemes = new SignatureScheme[] {SignatureScheme.ECDSA_SECP384R1_SHA384};
		he = new HandshakeEngine(true, params);
		he.start();
		produced = he.produce();
		assertEquals(1, produced.length);

		ch = (ClientHello) produced[0].getHandshake();
		
		assertEquals(32, ch.getLegacySessionId().length);
		assertCipherSuites(ch, CipherSuite.TLS_AES_128_GCM_SHA256);
		assertEquals(5, ch.getExtensioins().size());
		assertSupportedVersions(ch, 0x0304);
		assertSupportedGroups(ch, NamedGroup.SECP256R1, NamedGroup.SECP384R1);
		assertSignatureAlgorithms(ch, SignatureScheme.ECDSA_SECP384R1_SHA384);
		assertKeyShare(ch, false, NamedGroup.SECP256R1, NamedGroup.SECP384R1);
		assertServerName(ch, "snf4j.org");
		
		params.namedGroups = new NamedGroup[] {new NamedGroup("XXX", 32, null) {}};
		he = new HandshakeEngine(true, params);
		try {
			he.start();
		} catch (InternalErrorAlertException e) {}		
	}
	
	@Test
	public void testConsume() {
	}
}
