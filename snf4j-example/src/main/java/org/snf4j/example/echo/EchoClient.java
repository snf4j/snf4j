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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import org.snf4j.core.SSLSession;
import org.snf4j.core.SelectorLoop;
import org.snf4j.core.session.ssl.SSLContextBuilder;
import org.snf4j.core.session.ssl.SSLEngineBuilder;

public class EchoClient {
	static final String PREFIX = "org.snf4j.";
	static final String HOST = System.getProperty(PREFIX+"Host", "127.0.0.1");
	static final int PORT = Integer.getInteger(PREFIX+"Port", 8001);
	static final int SIZE = Integer.getInteger(PREFIX+"Size", 512);
	static final long TOTAL_SIZE = Long.getLong(PREFIX+"TotalSize", 1024*1024*1024);
	static final boolean SECURE = System.getProperty(PREFIX+"Secure") != null;
	static final int PIPELINE_SIZE = Integer.getInteger(PREFIX+"PipelineSize", 0);
	
	public static void main(String[] args) throws Exception {
		SelectorLoop loop = new SelectorLoop();

		try {
			loop.start();
			
			// Initialize the connection
			SocketChannel channel = SocketChannel.open();
			channel.configureBlocking(false);
			channel.connect(new InetSocketAddress(InetAddress.getByName(HOST), PORT));
			
			// Register the channel
			if (SECURE) {
				
				// Configure SSL connection
				SSLEngineBuilder builder = SSLContextBuilder.forClient()
					.keyManager(KeyStoreLoader.keyManagerFactory())
					.trustManager(KeyStoreLoader.trustManagerFactory())
					.engineBuilder();
				
				loop.register(channel, new SSLSession(new EchoClientHandler(builder), true));
			}
			else {
				loop.register(channel, new EchoClientHandler());
			}
			
			// Wait till the loop ends
			loop.join();
		}
		finally {

			// Gently stop the loop
			loop.stop();
		}
	}
}
