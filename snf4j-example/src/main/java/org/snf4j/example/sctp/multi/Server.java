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
package org.snf4j.example.sctp.multi;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.snf4j.core.SctpRegistrator;
import org.snf4j.core.SelectorLoop;

import com.sun.nio.sctp.SctpMultiChannel;

class Server {
	static final String PREFIX = "org.snf4j.";
	static final int SIZE = Integer.getInteger(PREFIX+"Size", 512);
	static final int MAX_COUNT = Integer.getInteger(PREFIX+"MaxCount", 1000000);
	static final String HOST1 = System.getProperty(PREFIX+"Host1", "127.0.0.1");
	static final String HOST2 = System.getProperty(PREFIX+"Host2", "127.0.0.2");
	static final String PEER1 = System.getProperty(PREFIX+"Peer1", "127.0.0.1");
	static final String PEER2 = System.getProperty(PREFIX+"Peer2", "127.0.0.1");

	void start(int port, int port1, int port2) throws Exception {
		SelectorLoop loop = new SelectorLoop();
		SocketAddress peer1 = new InetSocketAddress(InetAddress.getByName(PEER1), port1);
		SocketAddress peer2 = new InetSocketAddress(InetAddress.getByName(PEER2), port2);
		
		try {
			loop.start();
			
			// Initialize the connection
			SctpMultiChannel channel = SctpMultiChannel.open();
			channel.configureBlocking(false);
			channel.bind(new InetSocketAddress(InetAddress.getByName(HOST1), port));
			channel.bindAddress(InetAddress.getByName(HOST2));
			
			// Register the channel
			SctpRegistrator.register(loop, channel, new SctpMultiHandler(peer1, peer2));
			
			// Wait till the loop ends
			loop.join();
		}
		finally {

			// Gently stop the loop
			loop.stop();
		}
	}
}
