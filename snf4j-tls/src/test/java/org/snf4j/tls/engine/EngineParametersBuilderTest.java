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
package org.snf4j.tls.engine;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.snf4j.core.session.ssl.ClientAuth;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.PskKeyExchangeMode;
import org.snf4j.tls.extension.SignatureScheme;

public class EngineParametersBuilderTest {
	
	@Test
	public void testDefaults() {
		EngineParametersBuilder b = new EngineParametersBuilder();
		EngineParameters p = b.build();
		
		assertArrayEquals(EngineDefaults.getDefaultCipherSuites(), p.getCipherSuites());
		assertArrayEquals(EngineDefaults.getDefaultCipherSuites(), b.getCipherSuites());
		assertArrayEquals(EngineDefaults.getDefaulSignatureSchemes(), p.getSignatureSchemes());
		assertArrayEquals(EngineDefaults.getDefaulSignatureSchemes(), b.getSignatureSchemes());
		assertNull(p.getSignatureSchemesCert());
		assertNull(b.getSignatureSchemesCert());
		assertArrayEquals(EngineDefaults.getDefaultNamedGroups(), p.getNamedGroups());
		assertArrayEquals(EngineDefaults.getDefaultNamedGroups(), b.getNamedGroups());
		assertArrayEquals(EngineDefaults.getDefaultPskKeyExchangeModes(), p.getPskKeyExchangeModes());
		assertArrayEquals(EngineDefaults.getDefaultPskKeyExchangeModes(), b.getPskKeyExchangeModes());
		assertFalse(p.isCompatibilityMode());
		assertFalse(b.getCompatibilityMode());
		assertNull(p.getPeerHost());
		assertNull(b.getPeerHost());
		assertEquals(-1, p.getPeerPort());
		assertEquals(-1, b.getPeerPort());
		assertFalse(p.isServerNameRequired());
		assertFalse(b.getServerNameRequired());
		assertEquals(1, p.getNumberOfOfferedSharedKeys());
		assertEquals(1, b.getNumberOfOfferedSharedKeys());
		assertSame(DelegatedTaskMode.NONE, p.getDelegatedTaskMode());
		assertSame(DelegatedTaskMode.NONE, b.getDelegatedTaskMode());
		assertSame(ClientAuth.NONE, p.getClientAuth());
		assertSame(ClientAuth.NONE, b.getClientAuth());
	}
	
	@Test
	public void testModifications() {
		EngineParametersBuilder b = new EngineParametersBuilder();
		
		CipherSuite[] cs = new CipherSuite[] {CipherSuite.TLS_AES_128_GCM_SHA256};
		b.cipherSuites(cs);
		assertArrayEquals(cs, b.build().getCipherSuites());
		SignatureScheme[] ss = new SignatureScheme[] {SignatureScheme.ECDSA_SECP256R1_SHA256};
		SignatureScheme[] ss2 = new SignatureScheme[] {SignatureScheme.ECDSA_SECP384R1_SHA384};
		b.signatureSchemes(ss);
		assertArrayEquals(ss, b.build().getSignatureSchemes());
		b.signatureSchemesCert(ss2);
		assertArrayEquals(ss2, b.build().getSignatureSchemesCert());
		b.signatureSchemesCert((SignatureScheme[])null);
		assertNull(b.build().getSignatureSchemesCert());
		NamedGroup[] ng = new NamedGroup[] {NamedGroup.FFDHE2048};
		b.namedGroups(ng);
		assertArrayEquals(ng, b.build().getNamedGroups());
		PskKeyExchangeMode[] m = new PskKeyExchangeMode[] {PskKeyExchangeMode.PSK_DHE_KE, PskKeyExchangeMode.PSK_KE};
		b.pskKeyExchangeModes(m);
		assertArrayEquals(m, b.build().getPskKeyExchangeModes());
		b.compatibilityMode(true);
		assertTrue(b.build().isCompatibilityMode());
		b.peerHost("host");
		assertEquals("host", b.build().getPeerHost());
		b.peerPort(555);
		assertEquals(555, b.build().getPeerPort());
		b.serverNameRequired(true);
		assertTrue(b.build().isServerNameRequired());
		b.numberOfOfferedSharedKeys(0);
		assertEquals(0, b.build().getNumberOfOfferedSharedKeys());
		b.delegatedTaskMode(DelegatedTaskMode.ALL);
		assertSame(DelegatedTaskMode.ALL, b.build().getDelegatedTaskMode());
		b.clientAuth(ClientAuth.REQUESTED);
		assertSame(ClientAuth.REQUESTED, b.build().getClientAuth());		
	}
}
