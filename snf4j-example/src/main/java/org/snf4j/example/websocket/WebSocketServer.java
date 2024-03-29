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

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.snf4j.core.SelectorLoop;
import org.snf4j.websocket.AbstractWebSocketSessionFactory;
import org.snf4j.websocket.IWebSocketHandler;

public class WebSocketServer {
	static final String PREFIX = "org.snf4j.";
	static final boolean SECURE = Integer.getInteger(PREFIX+"Secure") != null;
	static final int PORT = Integer.getInteger(PREFIX+"Port", SECURE ? 8443 : 8080);
	static final boolean COMPRESS = System.getProperty(PREFIX+"Compress") != null;
	
	public static void main(String[] args) throws Exception {
		SelectorLoop loop = new SelectorLoop();
		final String host = "127.0.0.1:" + PORT;
		String endpoint = (SECURE ? "https" : "http") + "://" + host + '/';
		
		try {
			loop.start();
		
			// Initialize the listener
			ServerSocketChannel channel = ServerSocketChannel.open();
			channel.configureBlocking(false);
			channel.socket().bind(new InetSocketAddress(PORT));
			
			// Register the listener
			loop.register(channel, new AbstractWebSocketSessionFactory(SECURE) {

				@Override
				protected IWebSocketHandler createHandler(SocketChannel channel) {
					return new WebSocketServerHandler(host, COMPRESS);
				}
			}).sync();
			
			info("Server is ready");
			info("To enter Web Socket Chat open a web browser and navigate to " + endpoint);
			
			// Wait till the loop ends
			loop.join();
		}
		finally {
			
			// Gently stop the loop
			loop.stop();
		}
	}
	
	static void info(String msg) {
		System.out.println("[INFO] " + msg);
	}
}
