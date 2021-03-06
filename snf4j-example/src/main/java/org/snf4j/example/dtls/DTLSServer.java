/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020 SNF4J contributors
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
package org.snf4j.example.dtls;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

import org.snf4j.core.DTLSServerHandler;
import org.snf4j.core.DatagramServerHandler;
import org.snf4j.core.SelectorLoop;
import org.snf4j.core.factory.IDatagramHandlerFactory;
import org.snf4j.core.handler.IDatagramHandler;

public class DTLSServer {
	static final String PREFIX = "org.snf4j.";
	static final int PORT = Integer.getInteger(PREFIX+"Port", 8001);
	static final int SECURE = Integer.getInteger(PREFIX+"Secure", 1);
	
	public static void main(String[] args) throws Exception {
		SelectorLoop loop = new SelectorLoop();

		try {
			loop.start();
			
			// Initialize the connection
			DatagramChannel channel = DatagramChannel.open();
			channel.configureBlocking(false);
			channel.socket().bind(new InetSocketAddress(PORT));

			// Create handler factory
			IDatagramHandlerFactory factory = new IDatagramHandlerFactory() {

				@Override
				public IDatagramHandler create(SocketAddress remoteAddress) {
					return new SessionHandler(false);
				}
			};
			
			// Register the channel
			if (SECURE == 0) { 
				loop.register(channel, new DatagramServerHandler(factory, null, SessionStructureFactory.INSTANCE));
			}
			else {
				loop.register(channel, new DTLSServerHandler(factory, null, SessionStructureFactory.INSTANCE));
			}
			
			// Wait till the loop ends
			loop.join();
		}
		finally {
			loop.stop();
		}
	
	}
}
