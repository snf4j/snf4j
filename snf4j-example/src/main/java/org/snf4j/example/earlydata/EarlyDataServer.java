/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.example.earlydata;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import org.snf4j.core.SelectorLoop;
import org.snf4j.core.pool.ISelectorLoopPool;
import org.snf4j.tls.engine.DelegatedTaskMode;
import org.snf4j.tls.engine.EngineHandlerBuilder;
import org.snf4j.tls.engine.EngineParameters;
import org.snf4j.tls.engine.EngineParametersBuilder;

public class EarlyDataServer {
	
	static final String PREFIX = "org.snf4j.";	
	static final int PORT = Integer.getInteger(PREFIX+"Port", 8001);

	public static void main(String[] args) throws Exception {
		SelectorLoop loop = new SelectorLoop();
		ISelectorLoopPool pool = null;
		
		try {
			loop.start();
					
			// Initialize the listener
			ServerSocketChannel channel = ServerSocketChannel.open();
			channel.configureBlocking(false);
			channel.socket().bind(new InetSocketAddress(PORT));
			
			// Configure TLS connection
			EngineParameters params = new EngineParametersBuilder()
					.delegatedTaskMode(DelegatedTaskMode.ALL)
					.applicationProtocols("p1","p2","p3")
					.build();
			EngineHandlerBuilder builder = new EngineHandlerBuilder(
					KeyStoreLoader.keyManager(),
					KeyStoreLoader.trustManager())
					.ticketInfos(1024)
					.maxEarlyDataSize(1024)
					.padding(1);
			
			// Register the listener
			loop.register(channel, new SessionFactory(params, builder));
			
			// Wait till the loop ends
			loop.join();
		}
		finally {
			
			// Gently stop the loop
			loop.stop();
			
			// Gently stop the pool
			if (pool != null) {
				pool.stop();
			}
		}
	}
}
