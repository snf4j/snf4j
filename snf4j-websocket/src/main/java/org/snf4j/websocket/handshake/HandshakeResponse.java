/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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
package org.snf4j.websocket.handshake;

/**
 * A Web Socket handshake response frame. The content of this frame will be used
 * to format the HTTP response to a Web Socket handshake request.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class HandshakeResponse extends HandshakeFrame {
	
	private final int status;
	
	private final String reason;
	
	/**
	 * Constructs a Web Socket handshake response frame.
	 * 
	 * @param status the status code of the HTTP response
	 * @param reason the reason text of the HTTP response 
	 */
	public HandshakeResponse(int status, String reason) {
		this.status = status;
		this.reason = reason;
	}
	
	HandshakeResponse(HttpStatus status) {
		this.status = status.getStatus();
		this.reason = status.getReason();
	}

	/**
	 * Return the status code of the HTTP response
	 * 
	 * @return the status code
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Return the reason text of the HTTP response
	 * 
	 * @return the reason text
	 */
	public String getReason() {
		return reason;
	}
	
	@Override
	int getLength() {
		return super.getLength() + HandshakeUtils.RESPONSE_LENGTH + reason.length();
	}}
