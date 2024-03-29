/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020-2021 SNF4J contributors
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
package org.snf4j.example.echo;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.snf4j.core.SelectorLoop;
import org.snf4j.core.factory.AbstractSessionFactory;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.pool.DefaultSelectorLoopPool;
import org.snf4j.core.pool.ISelectorLoopPool;
import org.snf4j.core.session.ssl.ClientAuth;
import org.snf4j.core.session.ssl.SSLContextBuilder;
import org.snf4j.core.session.ssl.SSLEngineBuilder;

public class EchoServer {
	static final String PREFIX = "org.snf4j.";
	static final int PORT = Integer.getInteger(PREFIX+"Port", 8001);
	static final int POOL_SIZE = Integer.getInteger(PREFIX+"PoolSize", 8);
	static final boolean SECURE = System.getProperty(PREFIX+"Secure") != null;
	static final int PIPELINE_SIZE = Integer.getInteger(PREFIX+"PipelineSize", 0);

	public static void main(String[] args) throws Exception {
		SelectorLoop loop = new SelectorLoop();
		ISelectorLoopPool pool = null;
		
		try {
			loop.start();
		
			// Configure the selector loop pool
			if (POOL_SIZE > 0) {
				pool = new DefaultSelectorLoopPool(POOL_SIZE);
				loop.setPool(pool);
			}
			
			// Initialize the listener
			ServerSocketChannel channel = ServerSocketChannel.open();
			channel.configureBlocking(false);
			channel.socket().bind(new InetSocketAddress(PORT));
			
			// Configure SSL connection
			final SSLEngineBuilder builder = !SECURE ? null : SSLContextBuilder.forServer(KeyStoreLoader.keyManagerFactory())
					.trustManager(KeyStoreLoader.trustManagerFactory())
					.clientAuth(ClientAuth.REQUIRED)
					.engineBuilder();
			
			// Register the listener
			loop.register(channel, new AbstractSessionFactory(SECURE) {

				@Override
				protected IStreamHandler createHandler(SocketChannel channel) {
					return new EchoServerHandler(builder);
				}
			});
			
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
