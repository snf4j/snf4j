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
package org.snf4j.tls;

import org.snf4j.core.EngineStreamSession;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.tls.engine.IEngineHandler;
import org.snf4j.tls.engine.IEngineParameters;
import org.snf4j.tls.session.ISession;

public class TLSSession extends EngineStreamSession {

	private final static ILogger LOGGER = LoggerFactory.getLogger(TLSSession.class);
	
	public TLSSession(String name, IEngineParameters tlsParameters, IEngineHandler tlsHandler, IStreamHandler handler, boolean clientMode) {
		super(name, new TLSEngine(
				clientMode, 
				tlsParameters, 
				tlsHandler, 
				handler.getConfig().getMaxSSLApplicationBufferSizeRatio(),
				handler.getConfig().getMaxSSLNetworkBufferSizeRatio()), 
			handler, LOGGER);
	}

	public TLSSession(IEngineParameters tlsParameters, IEngineHandler tlsHandler, IStreamHandler handler, boolean clientMode) {
		super(new TLSEngine(
				clientMode, 
				tlsParameters, 
				tlsHandler, 
				handler.getConfig().getMaxSSLApplicationBufferSizeRatio(),
				handler.getConfig().getMaxSSLNetworkBufferSizeRatio()), 
			handler, LOGGER);
	}
	
	@Override
	public ISession getEngineSession() {
		return (ISession) super.getEngineSession();
	}
	
}
