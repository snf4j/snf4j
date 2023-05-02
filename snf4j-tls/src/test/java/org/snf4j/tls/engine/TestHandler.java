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

import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.IServerNameExtension;
import org.snf4j.tls.record.ContentType;
import org.snf4j.tls.session.ISessionManager;
import org.snf4j.tls.session.SessionManager;

public class TestHandler implements IEngineHandler {

	public boolean verifyServerName = true;
	
	public volatile TestCertificateSelector certificateSelector = new TestCertificateSelector();

	public volatile TestCertificateValidator certificateValidator = new TestCertificateValidator();

	public volatile ISessionManager sessionManager = new SessionManager();
	
	@Override
	public boolean verify(IServerNameExtension serverName) {
		return verifyServerName;
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
		return 0;
	}
	
	@Override
	public long getKeyLimit(CipherSuite cipher, long defaultValue) {
		return defaultValue;
	}
	
	@Override
	public ISessionManager getSessionManager() {
		return sessionManager;
	}
}
