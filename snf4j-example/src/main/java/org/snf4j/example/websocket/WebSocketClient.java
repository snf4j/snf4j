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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.SocketChannel;

import org.snf4j.core.SelectorLoop;
import org.snf4j.websocket.SSLWebSocketSession;
import org.snf4j.websocket.WebSocketSession;

public class WebSocketClient {
	static final String PREFIX = "org.snf4j.";
	static final String HOST = System.getProperty(PREFIX+"Host", "127.0.0.1");
	static final boolean SECURE = Integer.getInteger(PREFIX+"Secure") != null;
	static final int PORT = Integer.getInteger(PREFIX+"Port", SECURE ? 8443 : 8080);
	static final boolean COMPRESS = System.getProperty(PREFIX+"Compress") != null;
	static final int SIZE = Integer.getInteger(PREFIX+"Size", 512);

	public static void main(String[] args) throws Exception {
		SelectorLoop loop = new SelectorLoop();
		URI endpoint = new URI((SECURE ? "wss" : "ws") + "://" + HOST + ":" + PORT + SessionConfig.ECHO_PATH);
		
		try {
			loop.start();
			
			// Initialize the connection
			SocketChannel channel = SocketChannel.open();
			channel.configureBlocking(false);
			channel.connect(new InetSocketAddress(InetAddress.getByName(HOST), PORT));
			
			// Register the channel
			if (SECURE) {
				loop.register(channel, new SSLWebSocketSession(
						new WebSocketClientHandler(endpoint, COMPRESS), true));
			}
			else {
				loop.register(channel, new WebSocketSession(
						new WebSocketClientHandler(endpoint, COMPRESS), true));
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
