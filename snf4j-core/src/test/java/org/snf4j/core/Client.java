/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2018 SNF4J contributors
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
package org.snf4j.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import org.snf4j.core.handler.SessionEvent;

public class Client extends Server {
	
	String ip = "127.0.0.1";
	
	boolean registerConnectedSession;
	
	Object sessionOpenLock = new Object();
	
	Integer localPort;
	
	boolean reuseAddress;
	
	SocketChannel channel;
	
	public Client(int port) {
		super(port);
	}

	public StreamSession createSession() {
		initSession = new StreamSession(new Handler());
		return initSession;
	}
	
	@Override
	public void start(boolean firstRegistrate, SelectorLoop loop) throws IOException {
		if (loop == null) {
			this.loop = new SelectorLoop();
			loop = this.loop;
		}
		else {
			this.loop = loop;
		}
		if (controller != null) {
			loop.setController(controller);
		}
		if (threadFactory != null) {
			loop.setThreadFactory(threadFactory);
		}
		if (!firstRegistrate) {
			loop.start();
		}

		SocketChannel sc = channel == null ? SocketChannel.open() : channel;
		if (channel == null) {
			if (reuseAddress) {
				sc.socket().setReuseAddress(true);
			}
			if (localPort != null) {
				sc.socket().bind(new InetSocketAddress(InetAddress.getByName(ip), localPort));
			}
			sc.configureBlocking(false);
			sc.connect(new InetSocketAddress(InetAddress.getByName(ip), port));
		}
		
		if (registerConnectedSession) {
			session = new StreamSession(new Handler());
			session.setChannel(sc);
			session.preCreated();
			session.event(SessionEvent.CREATED);
			loop.register(sc, session);
		}
		else {
			if (initSession == null) {
				loop.register(sc, new Handler());
			}
			else {
				loop.register(sc, initSession);
			}
		}

		if (firstRegistrate) {
			loop.start();
		}
	}
	
}
