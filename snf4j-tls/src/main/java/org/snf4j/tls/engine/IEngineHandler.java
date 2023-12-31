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

import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.IALPNExtension;
import org.snf4j.tls.extension.IServerNameExtension;
import org.snf4j.tls.record.ContentType;
import org.snf4j.tls.session.ISessionManager;

public interface IEngineHandler {

	boolean verifyServerName(IServerNameExtension serverName);
	
	/**
	 * Called by servers to selects application protocol from the ALPN extension.
	 * 
	 * @param alpn               the offered ALPN extension or {@code null} if the
	 *                           extension was not present
	 * @param supportedProtocols the supported application protocols
	 * @return the selected protocol name or {@code null} if no protocol is used
	 * @throws NoApplicationProtocolAlert if the extension advertises
	 *                                    only protocols that the server does not
	 *                                    support
	 * @throws Alert if some other errors occurred
	 */
	String selectApplicationProtocol(IALPNExtension alpn, String[] supportedProtocols) throws Alert;
		
	/**
	 * Called by clients to verifies selected application protocol.
	 * 
	 * @param protocol the selected application protocol or {@code null} if no
	 *                 protocol was used
	 * @throws NoApplicationProtocolAlert if an application protocol was required
	 *                                    but none was used
	 * @throws Alert                      if some other errors occurred
	 */
	void selectedApplicationProtocol(String protocol) throws Alert;
	
	/**
	 * Called after successful handshake.
	 * 
	 * @param protocol the name of established protocol or {@code null} if no
	 *                 application protocol is used
	 * @throws Alert if an error occurred
	 */
	void handshakeFinished(String protocol) throws Alert;

	ICertificateSelector getCertificateSelector();
	
	ICertificateValidator getCertificateValidator();
	
	int calculatePadding(ContentType type, int contentLength);
	
	long getKeyLimit(CipherSuite cipher, long defaultValue);
	
	TicketInfo[] createNewTickets();
	
	ISessionManager getSessionManager();
	
	SecureRandom getSecureRandom();

	IEarlyDataHandler getEarlyDataHandler();
}
