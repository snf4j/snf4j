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

import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.NoApplicationProtocolAlert;

public interface IApplicationProtocolHandler {

	/**
	 * Called by servers to selects application protocol from offered application
	 * protocols.
	 * 
	 * @param offeredProtocols   the offered application protocols or {@code null}
	 *                           if no protocol was offered
	 * @param supportedProtocols the supported application protocols
	 * @return the selected protocol name (an empty name indicates that no protocol
	 *         should be used) or {@code null} if the protocol should be selected by
	 *         the caller
	 * @throws NoApplicationProtocolAlert if offered only protocols that the server
	 *                                    does not support
	 * @throws Alert                      if some other errors occurred
	 */
	String selectApplicationProtocol(String[] offeredProtocols, String[] supportedProtocols) throws NoApplicationProtocolAlert, Alert;
	
	/**
	 * Called by both clients and servers to signal selection of application protocol.
	 * 
	 * @param protocol the selected application protocol or {@code null} if no
	 *                 protocol was used
	 * @throws NoApplicationProtocolAlert if an application protocol was required
	 *                                    but none was used
	 * @throws Alert                      if some other errors occurred
	 */
	void selectedApplicationProtocol(String protocol) throws NoApplicationProtocolAlert, Alert;
	
}
