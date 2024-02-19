/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022-2024 SNF4J contributors
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

import org.snf4j.core.session.ssl.ClientAuth;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.PskKeyExchangeMode;
import org.snf4j.tls.extension.SignatureScheme;

public class EngineParameters implements IEngineParameters {
	
	private final static String[] EMPTY = new String[0];
	
	private final CipherSuite[] cipherSuites;
	
	private final NamedGroup[] namedGroups;
	
	private final SignatureScheme[] signatureSchemes;

	private final SignatureScheme[] certSignatureSchemes;
	
	private final PskKeyExchangeMode[] pskKeyExchangeModes;
	
	private final boolean compatibilityMode;
	
	private final int numberOfOfferedSharedKeys;// = 1;
	
	private final String peerHost;
	
	private final int peerPort;
	
	private final boolean serverNameRequired;
	
	private final DelegatedTaskMode delegatedTaskMode;// = DelegatedTaskMode.NONE;
	
	private final ClientAuth clientAuth;
	
	private final String[] applicationProtocols;
	
	public EngineParameters(CipherSuite[] cipherSuites, NamedGroup[] namedGroups, SignatureScheme[] signatureSchemes,
			SignatureScheme[] certSignatureSchemes, PskKeyExchangeMode[] pskKeyExchangeModes,
			boolean compatibilityMode, int numberOfOfferedSharedKeys, String peerHost,
			int peerPort, boolean serverNameRequired, DelegatedTaskMode delegatedTaskMode,ClientAuth clientAuth,
			String[] applicationProtocols) {
		super();
		this.cipherSuites = cipherSuites;
		this.namedGroups = namedGroups;
		this.signatureSchemes = signatureSchemes;
		this.certSignatureSchemes = certSignatureSchemes;
		this.pskKeyExchangeModes = pskKeyExchangeModes;
		this.compatibilityMode = compatibilityMode;
		this.numberOfOfferedSharedKeys = numberOfOfferedSharedKeys;
		this.peerHost = peerHost;
		this.peerPort = peerPort;
		this.serverNameRequired = serverNameRequired;
		this.delegatedTaskMode = delegatedTaskMode;
		this.clientAuth = clientAuth;
		this.applicationProtocols = applicationProtocols == null ? EMPTY : applicationProtocols;
	}

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
	public SignatureScheme[] getCertSignatureSchemes() {
		return certSignatureSchemes;
	}
	
	@Override
	public PskKeyExchangeMode[] getPskKeyExchangeModes() {
		return pskKeyExchangeModes;
	}
	
	@Override
	public boolean isCompatibilityMode() {
		return compatibilityMode;
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
	public boolean isServerNameRequired() {
		return serverNameRequired;
	}

	@Override
	public int getNumberOfOfferedSharedKeys() {
		return numberOfOfferedSharedKeys;
	}

	@Override
	public DelegatedTaskMode getDelegatedTaskMode() {
		return delegatedTaskMode;
	}

	@Override
	public ClientAuth getClientAuth() {
		return clientAuth;
	}
	
	@Override
	public String[] getApplicationProtocols() {
		return applicationProtocols;
	}
}
