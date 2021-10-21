/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2021 SNF4J contributors
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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import org.snf4j.core.handler.IStreamHandler;
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

	public Client(int port, boolean ssl) {
		super(port, ssl);
	}
	
	@Override
	public StreamSession addPreSession(String name, boolean ssl, IStreamHandler h) throws Exception {
		if (ssl) {
			StreamSession s;
			
			preSessions.add(s = new SSLSession(name, h == null ? createHandler() : h, true));
			return s;
		}
		return super.addPreSession(name, ssl, h);
	}

	public StreamSession createSession() throws Exception {
		if (ssl) {
			initSession = useTestSession ? new TestOwnSSLSession(new Handler(), true) 
					: new SSLSession(new Handler(), true);
		}
		else {
			initSession = useTestSession ? new TestStreamSession(new Handler()) 
					: new StreamSession(new Handler());
		}
		return initSession;
	}
	
	@Override
	public void start(boolean firstRegistrate, SelectorLoop loop) throws Exception {
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
			if (ssl) {
				session = useTestSession ? new TestOwnSSLSession(new Handler(), true) 
						: new SSLSession(new Handler(), true);
			}
			else {
				session = useTestSession ? new StreamSession(new Handler()) 
						: new StreamSession(new Handler());
			}
			session.setChannel(sc);
			session.preCreated();
			session.event(SessionEvent.CREATED);
			registeredSession = session;
			loop.register(sc, session);
		}
		else {
			if (initSession == null) {
				if (ssl) {
					if (useTestSession) {
						session = new TestOwnSSLSession(new Handler(), true);
					}
					else {
						session = new SSLSession(sslRemoteAddress ? sc.getRemoteAddress() : null, new Handler(), true);
					}
				}
				else {
					if (useTestSession) {
						session = new TestStreamSession(new Handler());
					}
					else {
						session = new StreamSession(new Handler());
					}
				}
			}
			else {
				session = initSession;
			}
			for (StreamSession s: preSessions) {
				session.getPipeline().add(s.getName(), s);
			}
			if (getPipeline) {
				session.getPipeline();
			}
			registeredSession = session;
			loop.register(sc, session);
		}

		if (firstRegistrate) {
			loop.start();
		}
	}
	
}
