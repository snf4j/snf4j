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

import java.security.SecureRandom;

import org.snf4j.core.session.ssl.ClientAuth;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.PskKeyExchangeMode;
import org.snf4j.tls.extension.SignatureScheme;

public class TestParameters implements IEngineParameters {

	private final static SecureRandom SECURE_RANDOM = new SecureRandom();

	CipherSuite[] cipherSuites = new CipherSuite[] {
			CipherSuite.TLS_AES_256_GCM_SHA384,
			CipherSuite.TLS_AES_128_GCM_SHA256
			};
	
	NamedGroup[] namedGroups = new NamedGroup[] {
			NamedGroup.SECP256R1,
			NamedGroup.SECP384R1
			};
	
	SignatureScheme[] signatureSchemes = new SignatureScheme[] {
			SignatureScheme.ECDSA_SECP256R1_SHA256,
			SignatureScheme.ECDSA_SECP384R1_SHA384
			};

	SignatureScheme[] signatureSchemesCert = null;
	
	PskKeyExchangeMode[] pskKeyExchangeModes = new PskKeyExchangeMode[] {
			PskKeyExchangeMode.PSK_DHE_KE
			};

	boolean compatibilityMode;
	
	int numberOfOfferedSharedKeys = 1;
	
	String peerHost;

	int peerPort;
	
	boolean serverNameRequired;

	DelegatedTaskMode delegatedTaskMode = DelegatedTaskMode.ALL;
	
	ClientAuth clientAuth = ClientAuth.NONE;
	
	@Override
	public CipherSuite[] getCipherSuites() {
		return cipherSuites;
	}

	@Override
	public NamedGroup[] getNamedGroups() {
		return namedGroups;
	}

	@Override
	public SignatureScheme[] getSignatureSchemes() {
		return signatureSchemes;
	}

	@Override
	public SignatureScheme[] getSignatureSchemesCert() {
		return signatureSchemesCert;
	}
	
	@Override
	public boolean isCompatibilityMode() {
		return compatibilityMode;
	}

	@Override
	public SecureRandom getSecureRandom() {
		return SECURE_RANDOM;
	}

	@Override
	public String getPeerHost() {
		return peerHost;
	}

	@Override
	public int getPeerPort() {
		return peerPort;
	}

	@Override
	public int getNumberOfOfferedSharedKeys() {
		return numberOfOfferedSharedKeys;
	}

	@Override
	public boolean isServerNameRequired() {
		return serverNameRequired;
	}

	@Override
	public DelegatedTaskMode getDelegatedTaskMode() {
		return delegatedTaskMode;
	}

	@Override
	public PskKeyExchangeMode[] getPskKeyExchangeModes() {
		return pskKeyExchangeModes;
	}

	@Override
	public ClientAuth getClientAuth() {
		return clientAuth;
	}

}
