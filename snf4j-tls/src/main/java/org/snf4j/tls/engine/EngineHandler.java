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

import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.IServerNameExtension;
import org.snf4j.tls.record.ContentType;
import org.snf4j.tls.session.ISessionManager;
import org.snf4j.tls.session.SessionManager;

public class EngineHandler implements IEngineHandler {

	private final static SessionManager SESSION_MANAGER = new SessionManager(); 
	
	private final static int DEFAULT_PADDING = 4096; 

	private final static int MAX_CONTENT_LENGTH = 16384; 
	
	private final ISessionManager manager;
	
	private final ICertificateSelector certificateSelector;
	
	private final ICertificateValidator certificateValidator;
	
	private final SecureRandom random;
	
	private final int padding;
	
	public EngineHandler(X509KeyManager km, String alias, X509TrustManager tm, SecureRandom random, ISessionManager manager, int padding) {
		certificateSelector = new X509KeyManagerCertificateSelector(km, alias);
		certificateValidator = new X509TrustManagerCertificateValidator(tm);
		this.manager = manager == null ? SESSION_MANAGER : manager;
		this.random = random == null ? new SecureRandom() : random;
		this.padding = padding;
	}

	public EngineHandler(X509KeyManager km, String alias, X509TrustManager tm, ISessionManager manager, int padding) {
		this(km, alias, tm, null, manager, padding);
	}

	public EngineHandler(X509KeyManager km, String alias, X509TrustManager tm, SecureRandom random, ISessionManager manager) {
		this(km, alias, tm, random, manager, DEFAULT_PADDING);
	}
	
	public EngineHandler(X509KeyManager km, String alias, X509TrustManager tm, ISessionManager manager) {
		this(km, alias, tm, manager, DEFAULT_PADDING);
	}
	
	public EngineHandler(X509KeyManager km, String alias, X509TrustManager tm, SecureRandom random) {
		this(km, alias, tm, random, null, DEFAULT_PADDING);
	}

	public EngineHandler(X509KeyManager km, String alias, X509TrustManager tm) {
		this(km, alias, tm, null, DEFAULT_PADDING);
	}

	public EngineHandler(X509KeyManager km, X509TrustManager tm, SecureRandom random, ISessionManager manager, int padding) {
		this(km, null, tm, random, manager, padding);
	}

	public EngineHandler(X509KeyManager km, X509TrustManager tm, ISessionManager manager, int padding) {
		this(km, null, tm, null, manager, padding);
	}

	public EngineHandler(X509KeyManager km, X509TrustManager tm, SecureRandom random, ISessionManager manager) {
		this(km, null, tm, random, manager, DEFAULT_PADDING);
	}
	
	public EngineHandler(X509KeyManager km, X509TrustManager tm, ISessionManager manager) {
		this(km, null, tm, manager, DEFAULT_PADDING);
	}

	public EngineHandler(X509KeyManager km, X509TrustManager tm, SecureRandom random) {
		this(km, null, tm, random, null, DEFAULT_PADDING);
	}
	
	public EngineHandler(X509KeyManager km, X509TrustManager tm) {
		this(km, null, tm, null, DEFAULT_PADDING);
	}

	@Override
	public boolean verify(IServerNameExtension serverName) {
		return true;
	}

	@Override
	public ICertificateSelector getCertificateSelector() {
		return certificateSelector;
	}

	@Override
	public ICertificateValidator getCertificateValidator() {
		return certificateValidator;
	}

	@Override
	public int calculatePadding(ContentType type, int contentLength) {
		if (contentLength < MAX_CONTENT_LENGTH) {
			int padding = this.padding;

			while (padding <= MAX_CONTENT_LENGTH) {
				if (contentLength <= padding) {
					return padding - contentLength;
				}
				padding <<= 1;
			}
			return MAX_CONTENT_LENGTH - contentLength;
		}
		return 0;
	}

	@Override
	public long getKeyLimit(CipherSuite cipher, long defaultValue) {
		return defaultValue;
	}

	@Override
	public long getMaxEarlyDataSize() {
		return 0;
	}

	@Override
	public TicketInfo[] createNewTickets() {
		return new TicketInfo[] {TicketInfo.NO_MAX_EARLY_DATA_SIZE};
	}

	@Override
	public boolean hasEarlyData() {
		return false;
	}

	@Override
	public byte[] nextEarlyData() {
		return null;
	}

	@Override
	public ISessionManager getSessionManager() {
		return manager;
	}

	@Override
	public SecureRandom getSecureRandom() {
		return random;
	}

}
