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
package org.snf4j.example.heartbeat;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

import org.snf4j.core.DatagramServerHandler;
import org.snf4j.core.SelectorLoop;
import org.snf4j.core.factory.IDatagramHandlerFactory;
import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.timer.DefaultTimer;

public class HeartbeatServer {
	static final String PREFIX = "org.snf4j.";
	static final int PORT = Integer.getInteger(PREFIX+"Port", 8001);
	static final int BEAT_PERIOD = Integer.getInteger(PREFIX+"BeatPeriod", 3000);
	static final int DOWN_PERIOD = Integer.getInteger(PREFIX+"DownPeriod", 10000);

	public static void main(String[] args) throws Exception {
		SelectorLoop loop = new SelectorLoop();
		final DefaultTimer timer = new DefaultTimer(true);
		
		try {
			loop.start();
			
			// Initialize the connection
			DatagramChannel channel = DatagramChannel.open();
			channel.configureBlocking(false);
			channel.socket().bind(new InetSocketAddress(PORT));

			// Register the channel
			loop.register(channel, new DatagramServerHandler(new IDatagramHandlerFactory() {

				@Override
				public IDatagramHandler create(SocketAddress remoteAddress) {
					return new HeartbeatHandler(timer, BEAT_PERIOD, DOWN_PERIOD);
				}
				
			},
			new SessionConfig()));
			
			// Wait till the loop ends
			loop.join();
		}
		finally {
			loop.stop();
		}
		
	}
}
