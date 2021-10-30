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
package org.snf4j.websocket;

import org.snf4j.core.session.IStreamSession;
import org.snf4j.websocket.handshake.IHandshaker;

/**
 * Extends the {@link IStreamSession} interface to cover the Web Socket functionalities.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IWebSocketSession extends IStreamSession {
	
	/**
	 * Returns the Web Socket handler associated with this session
	 * 
	 * @return the Web Socket handler
	 */
	IWebSocketHandler getWebSocketHandler();

	/**
	 * Returns the Web Socket handshaker associated with this session.
	 * 
	 * @return the Web Socket handshaker
	 */
	IHandshaker getHandshaker();
	
	/**
	 * Writes the Web Socket close frame with given status code and then gently
	 * closes this session.
	 * 
	 * @param status the status code of the close frame, or -1 if the close frame
	 *               should not contain the application body.
	 */
	void close(int status);
	
	/**
	 * Writes the Web Socket close frame with given status code and reason and then
	 * gently closes this session.
	 * 
	 * @param status the status code of the close frame, or -1 if the close frame
	 *               should not contain the application body.
	 * @param reason the reason of the close frame. If the {@code status} argument
	 *               equals -1 this argument is ignored.
	 */
	void close(int status, String reason);
}
