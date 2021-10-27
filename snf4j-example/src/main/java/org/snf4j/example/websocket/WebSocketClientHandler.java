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
package org.snf4j.example.websocket;

import java.net.URI;

import org.snf4j.core.EndingAction;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.websocket.AbstractWebSocketHandler;
import org.snf4j.websocket.IWebSocketSessionConfig;
import org.snf4j.websocket.extensions.compress.PerMessageDeflateExtension;
import org.snf4j.websocket.frame.BinaryFrame;

public class WebSocketClientHandler extends AbstractWebSocketHandler {

	private final URI requestUri;
	
	private final boolean compress;
	
	WebSocketClientHandler(URI requestUri, boolean compress) {
		this.requestUri = requestUri;
		this.compress = compress;
	}
	
	@Override
	public void read(Object msg) {
		getSession().writenf(msg);
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public void event(SessionEvent event) {
		switch (event) {
		case READY:
			byte[] payload = new byte[WebSocketClient.SIZE];
			
			for (int i=0; i<payload.length; ++i) {
				payload[i] = (byte)i;
			}
			getSession().writenf(new BinaryFrame(payload));
			break;
		}
	}
	
	@Override
	public IWebSocketSessionConfig getConfig() {
		SessionConfig config = new SessionConfig(requestUri);
				
		config.setEndingAction(EndingAction.STOP);		
		if (compress) {
			config.setSupportedExtensions(new PerMessageDeflateExtension());
		}
		return config;
	}
}
