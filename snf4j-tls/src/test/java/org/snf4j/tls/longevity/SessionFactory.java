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
package org.snf4j.tls.longevity;

import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.snf4j.core.StreamSession;
import org.snf4j.core.factory.AbstractSessionFactory;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.tls.TLSSession;
import org.snf4j.tls.engine.DelegatedTaskMode;
import org.snf4j.tls.engine.EngineHandlerBuilder;
import org.snf4j.tls.engine.EngineParametersBuilder;

public class SessionFactory extends AbstractSessionFactory {

	static final ExecutorService executor = Executors.newFixedThreadPool(10);

	protected SessionFactory(boolean ssl) {
		super(ssl);
	}
	
	@Override
	public StreamSession create(SocketChannel channel) throws Exception {
		channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		EngineParametersBuilder builder = new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.ALL);

		return new TLSSession(
				builder.build(),
				new EngineHandlerBuilder(SessionConfig.km, SessionConfig.tm)
				.sessionManager(Controller.serverManager)
				.build(),
				new ServerHandler(),
				false
				);
			}
	
	@Override
	protected IStreamHandler createHandler(SocketChannel channel) {
		return new ServerHandler();
	}

}
