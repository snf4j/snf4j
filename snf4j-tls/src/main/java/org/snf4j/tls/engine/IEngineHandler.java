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
import org.snf4j.tls.extension.IServerNameExtension;
import org.snf4j.tls.record.ContentType;
import org.snf4j.tls.session.ISessionManager;

public interface IEngineHandler {

	boolean verify(IServerNameExtension serverName);
	
	ICertificateSelector getCertificateSelector();
	
	ICertificateValidator getCertificateValidator();
	
	int calculatePadding(ContentType type, int contentLength);
	
	long getKeyLimit(CipherSuite cipher, long defaultValue);
	
	long getMaxEarlyDataSize();
	
	TicketInfo[] createNewTickets();
	
	boolean hasEarlyData();
	
	byte[] nextEarlyData();
	
	ISessionManager getSessionManager();
	
	SecureRandom getSecureRandom();

}
