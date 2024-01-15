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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import org.snf4j.core.SelectorLoop;
import org.snf4j.tls.TLSSession;
import org.snf4j.tls.engine.DelegatedTaskMode;
import org.snf4j.tls.engine.EngineHandlerBuilder;
import org.snf4j.tls.engine.EngineParameters;
import org.snf4j.tls.engine.EngineParametersBuilder;

public class EarlyDataClient {

	static final String PREFIX = "org.snf4j.";
	static final String HOST = System.getProperty(PREFIX+"Host", "localhost");
	static final int PORT = Integer.getInteger(PREFIX+"Port", 8001);
	
	public static void main(String[] args) throws Exception {
		SelectorLoop loop = new SelectorLoop();
		
		try {
			loop.start();
			
			Logger.inf("----- connect #1 --------------------------");
			connect(loop, "time", "p2");
			Logger.inf("----- connect #2 (accepted) ---------------");
			connect(loop, "time", "p2");
			Logger.inf("----- connect #3 (rejected) ---------------");
			connect(loop, "time", "p3");
			Logger.inf("----- connect #4 (accepted) ---------------");
			connect(loop, "time", "p3");
			Logger.inf("----- connect #5 (accepted) ---------------");
			connect(loop, "time", "p3");
		}
		finally {

			// Gently stop the loop
			loop.stop();
		}
	}
	
	static TLSSession connect(SelectorLoop loop, String cmd, String protocol) throws Exception {
		// Initialize the connection
		SocketChannel channel = SocketChannel.open();
		channel.configureBlocking(false);
		channel.connect(new InetSocketAddress(InetAddress.getByName(HOST), PORT));
		
		// Configure TLS connection
		EngineParameters params = new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.ALL)
				.peerHost(HOST)
				.peerPort(PORT)
				.applicationProtocols(protocol)
				.build();
		EngineHandlerBuilder builder = new EngineHandlerBuilder(
				KeyStoreLoader.keyManager(),
				KeyStoreLoader.trustManager())
				.padding(1);

		// Register the channel
		EarlyDataClientHandler handler = new EarlyDataClientHandler(cmd);
		TLSSession session = new TLSSession(
				params, 
				builder.build(handler, handler), 
				handler, 
				true);
		loop.register(channel, session);
		
		// Wait till the session ends
		session.getEndFuture().sync();
		return session;
	}
}
