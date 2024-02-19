/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023-2024 SNF4J contributors
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
import static org.junit.Assert.assertSame;

import java.nio.ByteBuffer;
import java.security.PublicKey;

import org.junit.Test;
import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.cipher.ICipherSuiteSpec;
import org.snf4j.tls.cipher.IHashSpec;
import org.snf4j.tls.crypto.IAead;
import org.snf4j.tls.crypto.IKeyExchange;
import org.snf4j.tls.crypto.ISignature;
import org.snf4j.tls.extension.INamedGroupSpec;
import org.snf4j.tls.extension.ISignatureSchemeSpec;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.ParsedKey;
import org.snf4j.tls.extension.SignatureScheme;

public class EngineDefaultsTest {

	@Test
	public void testImplementedCipherSuite() {
		 CipherSuite[] css = new  CipherSuite[] {CipherSuite.TLS_AES_128_GCM_SHA256, CipherSuite.TLS_AES_256_GCM_SHA384};
		 CipherSuite cs1 = new CipherSuite("name1",-1, null) {};
		 ICipherSuiteSpec spec = new ICipherSuiteSpec() {

			@Override
			public boolean isImplemented() {
				return false;
			}

			@Override
			public IAead getAead() {
				return null;
			}

			@Override
			public IHashSpec getHashSpec() {
				return null;
			}
			 
		 };
		 CipherSuite cs2 = new CipherSuite("name2",-1, spec) {};
		 
		 assertArrayEquals(css, EngineDefaults.implemented(css));
		 css[1] = cs1;
		 assertArrayEquals(new  CipherSuite[] {CipherSuite.TLS_AES_128_GCM_SHA256}, EngineDefaults.implemented(css));
		 css[1] = cs2;
		 assertArrayEquals(new  CipherSuite[] {CipherSuite.TLS_AES_128_GCM_SHA256}, EngineDefaults.implemented(css));
	}

	@Test
	public void testImplementedNamedGroup() {
		NamedGroup[] ngs = new  NamedGroup[] {NamedGroup.FFDHE2048, NamedGroup.FFDHE3072};
		NamedGroup ng1 = new NamedGroup("name1",-1, null) {};
		INamedGroupSpec spec = new INamedGroupSpec() {

			@Override
			public boolean isImplemented() {
				return false;
			}

			@Override
			public ParsedKey parse(ByteBufferArray srcs, int remaining) throws Alert {
				return null;
			}

			@Override
			public int getDataLength() {
				return 0;
			}

			@Override
			public void getData(ByteBuffer buffer, PublicKey key) {
			}

			@Override
			public void getData(ByteBuffer buffer, ParsedKey key) {
			}

			@Override
			public IKeyExchange getKeyExchange() {
				return null;
			}

			@Override
			public PublicKey generateKey(ParsedKey key) throws Alert {
				return null;
			}
		};
		NamedGroup ng2 = new NamedGroup("name2",-1, spec) {};

		assertArrayEquals(ngs, EngineDefaults.implemented(ngs));
		ngs[1] = ng1;
		assertArrayEquals(new  NamedGroup[] {NamedGroup.FFDHE2048}, EngineDefaults.implemented(ngs));
		ngs[1] = ng2;
		assertArrayEquals(new  NamedGroup[] {NamedGroup.FFDHE2048}, EngineDefaults.implemented(ngs));
	}

	@Test
	public void testImplementedSignatureScheme() {
		SignatureScheme[] sss = new  SignatureScheme[] {SignatureScheme.RSA_PKCS1_SHA256, SignatureScheme.RSA_PKCS1_SHA384};
		SignatureScheme ss1 = new SignatureScheme("name1",-1, null) {};
		ISignatureSchemeSpec spec = new ISignatureSchemeSpec() {

			@Override
			public boolean isImplemented() {
				return false;
			}

			@Override
			public ISignature getSignature() {
				return null;
			}
		};
		SignatureScheme ss2 = new SignatureScheme("name2",-1, spec) {};

		assertArrayEquals(sss, EngineDefaults.implemented(sss));
		sss[1] = ss1;
		assertArrayEquals(new  SignatureScheme[] {SignatureScheme.RSA_PKCS1_SHA256}, EngineDefaults.implemented(sss));
		sss[1] = ss2;
		assertArrayEquals(new  SignatureScheme[] {SignatureScheme.RSA_PKCS1_SHA256}, EngineDefaults.implemented(sss));
	}

	@Test
	public void testDefaulNamedGroups() {
		NamedGroup[] expected = new NamedGroup[] { 
				NamedGroup.X25519,
				NamedGroup.SECP256R1,
				NamedGroup.X448,
				NamedGroup.SECP521R1,
				NamedGroup.SECP384R1,
				NamedGroup.FFDHE2048,
				NamedGroup.FFDHE3072,
				NamedGroup.FFDHE4096,
				NamedGroup.FFDHE6144,
				NamedGroup.FFDHE8192
				};
		NamedGroup[] groups = EngineDefaults.getDefaultNamedGroups();

		int j = 0;
		for (int i=0; i<expected.length; ++i) {
			NamedGroup exp = expected[i];
			
			if (exp.spec().isImplemented()) {
				assertSame(exp, groups[j++]);
			}
		}
		assertEquals(j, groups.length);
	}
	
	@Test
	public void testDefaulSignatureSchemes() {
		SignatureScheme[] expected = new SignatureScheme[] {
				SignatureScheme.ECDSA_SECP256R1_SHA256,
				SignatureScheme.ECDSA_SECP384R1_SHA384,
				SignatureScheme.ECDSA_SECP521R1_SHA512,
				SignatureScheme.ED25519,
				SignatureScheme.ED448,
				SignatureScheme.RSA_PSS_PSS_SHA256,
				SignatureScheme.RSA_PSS_PSS_SHA384,
				SignatureScheme.RSA_PSS_PSS_SHA512,
				SignatureScheme.RSA_PSS_RSAE_SHA256,
				SignatureScheme.RSA_PSS_RSAE_SHA384,
				SignatureScheme.RSA_PSS_RSAE_SHA512
				};
		SignatureScheme[] schemes = EngineDefaults.getDefaulSignatureSchemes();
		
		int j = 0;
		for (int i=0; i<expected.length; ++i) {
			SignatureScheme exp = expected[i];
			
			if (exp.spec().isImplemented()) {
				assertSame(exp, schemes[j++]);
			}
		}
		assertEquals(j, schemes.length);
		
		expected = new SignatureScheme[] {
				SignatureScheme.ECDSA_SECP256R1_SHA256,
				SignatureScheme.ECDSA_SECP384R1_SHA384,
				SignatureScheme.ECDSA_SECP521R1_SHA512,
				SignatureScheme.ED25519,
				SignatureScheme.ED448,
				SignatureScheme.RSA_PSS_PSS_SHA256,
				SignatureScheme.RSA_PSS_PSS_SHA384,
				SignatureScheme.RSA_PSS_PSS_SHA512,
				SignatureScheme.RSA_PSS_RSAE_SHA256,
				SignatureScheme.RSA_PSS_RSAE_SHA384,
				SignatureScheme.RSA_PSS_RSAE_SHA512,
				SignatureScheme.RSA_PKCS1_SHA256,
				SignatureScheme.RSA_PKCS1_SHA384,
				SignatureScheme.RSA_PKCS1_SHA512
				};
		schemes = EngineDefaults.getDefaulCertSignatureSchemes();

		j = 0;
		for (int i=0; i<expected.length; ++i) {
			SignatureScheme exp = expected[i];
			
			if (exp.spec().isImplemented()) {
				assertSame(exp, schemes[j++]);
			}
		}
		assertEquals(j, schemes.length);
	}
}
