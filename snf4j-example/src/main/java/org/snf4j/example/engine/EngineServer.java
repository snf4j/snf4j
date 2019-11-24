/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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
package org.snf4j.example.engine;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.snf4j.core.SelectorLoop;
import org.snf4j.core.StreamSession;
import org.snf4j.core.factory.IStreamSessionFactory;

public class EngineServer {
	static final String PREFIX = "org.snf4j.";
	static final int PORT = Integer.getInteger(PREFIX+"Port", 8003);
	static final int ENGINE = Integer.getInteger(PREFIX+"Engine", 2);
	static final int OFFSET = Integer.getInteger(PREFIX+"Offset", 66);

	public static void main(String[] args) throws Exception {
		SelectorLoop loop = new SelectorLoop();

		try {
			loop.start();
		
			// Initialize the listener
			ServerSocketChannel channel = ServerSocketChannel.open();
			channel.configureBlocking(false);
			channel.socket().bind(new InetSocketAddress(PORT));
			
			// Register the listener
			loop.register(channel, new IStreamSessionFactory() {

				@Override
				public StreamSession create(SocketChannel channel)
						throws Exception {
					return new EngineSession(EngineFactory.create(ENGINE, OFFSET, false), new EngineServerHandler());
				}

				@Override
				public void registered(ServerSocketChannel channel) {
				}

				@Override
				public void closed(ServerSocketChannel channel) {
				}

				@Override
				public void exception(ServerSocketChannel channel, Throwable exception) {
				}
			});
			
			// Wait till the loop ends
			loop.join();
		}
		finally {
			
			// Gently stop the loop
			loop.stop();
		}
	}
}
