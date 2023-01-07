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

import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.SignatureScheme;

public class EngineParameters implements IEngineParameters {
	
	private final CipherSuite[] cipherSuites;
	
	private final NamedGroup[] namedGroups;
	
	private final SignatureScheme[] signatureSchemes;
	
	private boolean compatibilityMode;
	
	private int numberOfOfferedSharedKeys = 1;
	
	private SecureRandom secureRandom = new SecureRandom();
	
	private String serverName;
	
	private boolean serverNameRequired;
	
	DelegatedTaskMode delegatedTaskMode = DelegatedTaskMode.NONE;
	
	public EngineParameters() {
		cipherSuites = EngineDefaults.getDefaultCipherSuites();
		namedGroups = EngineDefaults.getDefaultNamedGroups();
		signatureSchemes = EngineDefaults.getDefaulSignatureSchemes();
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
	public boolean isCompatibilityMode() {
		return compatibilityMode;
	}

	@Override
	public SecureRandom getSecureRandom() {
		return secureRandom;
	}

	@Override
	public String getServerName() {
		return serverName;
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
	
}
