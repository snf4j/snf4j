/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2019 SNF4J contributors
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

import org.snf4j.core.handler.SessionEvent;

public class Client extends Server {
	
	String ip = "127.0.0.1";
	
	boolean registerConnectedSession;
	
	Object sessionOpenLock = new Object();
	
	Integer localPort;
	
	boolean reuseAddress;
	
	int sendBufferSize;
	
	SocketChannel channel;
	
	public Client(int port) {
		super(port);
	}

	public Client(int port, boolean ssl) {
		super(port, ssl);
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
			if (sendBufferSize > 0) {
				int oldSize = sc.socket().getSendBufferSize();
				int newSize;
				
				if (oldSize > sendBufferSize) {
					sc.socket().setSendBufferSize(sendBufferSize); 
					newSize = sc.socket().getSendBufferSize();
				}
				else {
					newSize = oldSize;
				}
				sendBufferSize = newSize;
				System.out.println("[INFO] SO_SNDBUF changed requested (old=" + oldSize + ", new=" + newSize+ ")");
			}
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
			loop.register(sc, session);
		}
		else {
			if (initSession == null) {
				if (ssl) {
					if (useTestSession) {
						loop.register(sc, new TestOwnSSLSession(new Handler(), true));
					}
					else {
						loop.register(sc, new SSLSession(new Handler(), true));
					}
				}
				else {
					if (useTestSession) {
						loop.register(sc, new TestStreamSession(new Handler()));
					}
					else {
						loop.register(sc, new Handler());
					}
				}
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
