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

import java.security.SecureRandom;

import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.PskKeyExchangeMode;
import org.snf4j.tls.extension.SignatureScheme;

public class EngineParametersBuilder {

	private CipherSuite[] cipherSuites = EngineDefaults.getDefaultCipherSuites();
	
	private NamedGroup[] namedGroups = EngineDefaults.getDefaultNamedGroups();
	
	private SignatureScheme[] signatureSchemes = EngineDefaults.getDefaulSignatureSchemes();
	
	private PskKeyExchangeMode[] pskKeyExchangeModes = EngineDefaults.getDefaultPskKeyExchangeModes();
	
	private SecureRandom secureRandom = new SecureRandom();
	
	private boolean compatibilityMode;
	
	private int numberOfOfferedSharedKeys = 1;
	
	private String peerHost;

	private int peerPort = -1;
	
	private boolean serverNameRequired;
	
	private DelegatedTaskMode delegatedTaskMode = DelegatedTaskMode.NONE;
	
	public EngineParametersBuilder cipherSuites(CipherSuite... cipherSuites) {
		this.cipherSuites = cipherSuites.clone();
		return this;
	}
	
	public EngineParametersBuilder namedGroups(NamedGroup... namedGroups) {
		this.namedGroups = namedGroups.clone();
		return this;
	}

	public EngineParametersBuilder signatureSchemes(SignatureScheme... signatureSchemes) {
		this.signatureSchemes = signatureSchemes.clone();
		return this;
	}
	
	public EngineParametersBuilder pskKeyExchangeModes(PskKeyExchangeMode... pskKeyExchangeModes) {
		this.pskKeyExchangeModes = pskKeyExchangeModes;
		return this;
	}
	
	public EngineParametersBuilder secureRandom(SecureRandom secureRandom) {
		this.secureRandom = secureRandom;
		return this;
	}

	public EngineParametersBuilder compatibilityMode(boolean compatibilityMode) {
		this.compatibilityMode = compatibilityMode;
		return this;
	}

	public EngineParametersBuilder numberOfOfferedSharedKeys(int numberOfOfferedSharedKeys) {
		this.numberOfOfferedSharedKeys = numberOfOfferedSharedKeys;
		return this;
	}

	public EngineParametersBuilder peerHost(String peerHost) {
		this.peerHost = peerHost;
		return this;
	}

	public EngineParametersBuilder peerPort(int peerPort) {
		this.peerPort = peerPort;
		return this;
	}
	
	public EngineParametersBuilder serverNameRequired(boolean serverNameRequired) {
		this.serverNameRequired = serverNameRequired;
		return this;
	}

	public EngineParametersBuilder delegatedTaskMode(DelegatedTaskMode delegatedTaskMode) {
		this.delegatedTaskMode = delegatedTaskMode;
		return this;
	}
	
	public EngineParameters build() {
		return new EngineParameters(
				cipherSuites.clone(),
				namedGroups.clone(),
				signatureSchemes.clone(),
				pskKeyExchangeModes.clone(),
				compatibilityMode,
				numberOfOfferedSharedKeys,
				secureRandom,
				peerHost,
				peerPort,
				serverNameRequired,
				delegatedTaskMode
				);
	}

}
