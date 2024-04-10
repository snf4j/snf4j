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

import org.snf4j.core.session.ssl.ClientAuth;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.PskKeyExchangeMode;
import org.snf4j.tls.extension.SignatureScheme;

public class EngineParametersBuilder {

	private CipherSuite[] cipherSuites = EngineDefaults.getDefaultCipherSuites();
	
	private NamedGroup[] namedGroups = EngineDefaults.getDefaultNamedGroups();
	
	private SignatureScheme[] signatureSchemes = EngineDefaults.getDefaulSignatureSchemes();

	private SignatureScheme[] certSignatureSchemes = EngineDefaults.getDefaulCertSignatureSchemes();
	
	private PskKeyExchangeMode[] pskKeyExchangeModes = EngineDefaults.getDefaultPskKeyExchangeModes();
		
	private boolean compatibilityMode;
	
	private int numberOfOfferedSharedKeys = 1;
	
	private String peerHost;

	private int peerPort = -1;
	
	private boolean serverNameRequired;
	
	private DelegatedTaskMode delegatedTaskMode = DelegatedTaskMode.NONE;
	
	private ClientAuth clientAuth = ClientAuth.NONE;
	
	private String[] applicationProtocols;
	
	private boolean skipEndOfEarlyData;
	
	public EngineParametersBuilder() {}
		
	public EngineParametersBuilder cipherSuites(CipherSuite... cipherSuites) {
		this.cipherSuites = cipherSuites.clone();
		return this;
	}
	
	public CipherSuite[] getCipherSuites() {
		return cipherSuites;
	}
	
	public EngineParametersBuilder namedGroups(NamedGroup... namedGroups) {
		this.namedGroups = namedGroups.clone();
		return this;
	}

	public NamedGroup[] getNamedGroups() {
		return namedGroups;
	}

	public EngineParametersBuilder signatureSchemes(SignatureScheme... signatureSchemes) {
		this.signatureSchemes = signatureSchemes.clone();
		return this;
	}
	
	public SignatureScheme[] getSignatureSchemes() {
		return signatureSchemes;
	}

	public EngineParametersBuilder certSignatureSchemes(SignatureScheme... signatureSchemes) {
		this.certSignatureSchemes = safeClone(signatureSchemes);
		return this;
	}
	
	public SignatureScheme[] getCertSignatureSchemes() {
		return certSignatureSchemes;
	}
	
	public EngineParametersBuilder pskKeyExchangeModes(PskKeyExchangeMode... pskKeyExchangeModes) {
		this.pskKeyExchangeModes = pskKeyExchangeModes.clone();
		return this;
	}
	
	public PskKeyExchangeMode[] getPskKeyExchangeModes() {
		return pskKeyExchangeModes;
	}

	public EngineParametersBuilder compatibilityMode(boolean compatibilityMode) {
		this.compatibilityMode = compatibilityMode;
		return this;
	}

	public boolean getCompatibilityMode() {
		return compatibilityMode;
	}

	public EngineParametersBuilder numberOfOfferedSharedKeys(int numberOfOfferedSharedKeys) {
		this.numberOfOfferedSharedKeys = numberOfOfferedSharedKeys;
		return this;
	}

	public int getNumberOfOfferedSharedKeys() {
		return numberOfOfferedSharedKeys;
	}

	public EngineParametersBuilder peerHost(String peerHost) {
		this.peerHost = peerHost;
		return this;
	}

	public String getPeerHost() {
		return peerHost;
	}

	public EngineParametersBuilder peerPort(int peerPort) {
		this.peerPort = peerPort;
		return this;
	}
	
	public int getPeerPort() {
		return peerPort;
	}

	public EngineParametersBuilder serverNameRequired(boolean serverNameRequired) {
		this.serverNameRequired = serverNameRequired;
		return this;
	}

	public boolean getServerNameRequired() {
		return serverNameRequired;
	}

	public EngineParametersBuilder delegatedTaskMode(DelegatedTaskMode delegatedTaskMode) {
		this.delegatedTaskMode = delegatedTaskMode;
		return this;
	}
	
	public DelegatedTaskMode getDelegatedTaskMode() {
		return delegatedTaskMode;
	}

	public EngineParametersBuilder clientAuth(ClientAuth clientAuth) {
		this.clientAuth = clientAuth;
		return this;
	}
	
	public ClientAuth getClientAuth() {
		return clientAuth;
	}

	public EngineParametersBuilder applicationProtocols(String... protocols) {
		if (protocols != null && protocols.length == 0) {
			protocols = null;
		}
		this.applicationProtocols = safeClone(protocols);
		return this;
	}
	
	public String[] getApplicationProtocols() {
		return applicationProtocols;
	}

	public EngineParametersBuilder skipEndOfEarlyData(boolean skip) {
		this.skipEndOfEarlyData = skip;
		return this;
	}

	public boolean getSkipEndOfEarlyData() {
		return skipEndOfEarlyData;
	}
	
	private static <T> T[] safeClone(T[] array) {
		return array == null ? null : array.clone();
	}
	
	public EngineParameters build() {
		return new EngineParameters(
				cipherSuites.clone(),
				namedGroups.clone(),
				signatureSchemes.clone(),
				safeClone(certSignatureSchemes),
				pskKeyExchangeModes.clone(),
				compatibilityMode,
				numberOfOfferedSharedKeys,
				peerHost,
				peerPort,
				serverNameRequired,
				delegatedTaskMode,
				clientAuth,
				safeClone(applicationProtocols),
				skipEndOfEarlyData
				);
	}

}
