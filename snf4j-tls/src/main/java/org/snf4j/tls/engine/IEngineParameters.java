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

public interface IEngineParameters {
	
	CipherSuite[] getCipherSuites();

	NamedGroup[] getNamedGroups();

	SignatureScheme[] getSignatureSchemes();

	SignatureScheme[] getCertSignatureSchemes();

	PskKeyExchangeMode[] getPskKeyExchangeModes();
	
	boolean isCompatibilityMode();
	
	String getPeerHost();
	
	int getPeerPort();

	boolean isServerNameRequired();
	
	int getNumberOfOfferedSharedKeys();
	
	DelegatedTaskMode getDelegatedTaskMode();
	
	ClientAuth getClientAuth();
	
	/**
	 * Return supported application protocols or an empty array if application
	 * protocol negotiation (ALPN) should not be used.
	 * 
	 * @return supported application protocols or an empty array
	 */
	String[] getApplicationProtocols();
	
	/**
	 * Tells if the EndOfEarlyData handshake message should be skipped during TLS
	 * 1.3 handshake.
	 * <p>
	 * NOTE: This method should return {@code true} only when the handshake engine
	 * is used by other protocols which prohibit the usage of this handshake message
	 * (e.g. QUIC).
	 * 
	 * @return {@code true} to skip the EndOfEarlyData handshake message
	 */
	boolean skipEndOfEarlyData();
	
}
