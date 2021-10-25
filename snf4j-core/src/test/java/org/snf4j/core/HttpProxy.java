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
package org.snf4j.core;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLEngine;

import org.snf4j.core.factory.AbstractSessionFactory;
import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.session.SSLEngineCreateException;

public class HttpProxy {
	
	final int port;

	volatile SelectorLoop loop;
	
	volatile StreamSession client;
	
	volatile StreamSession server;
	
	volatile String status = "200 Connection established";
	
	volatile boolean skipConnection;
	
	volatile Packet appendPacket;
	
	StringBuilder trace = new StringBuilder();
	
	HttpProxy(int port) {
		this.port = port;
	}
	
	public String getTrace() {
		String s; 
		
		synchronized (trace) {
			s = trace.toString();
			trace.setLength(0);
		}
		return s;
	}
	
	void trace(String s) {
		synchronized(trace) {
			trace.append(s);
			trace.append('|');
		}
	}

	public void start(long millis) throws Exception {
		start(millis, false);
	}
	
	public void start(long millis, boolean ssl) throws Exception {
		loop = new SelectorLoop();
		loop.start();
		
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		ssc.socket().bind(new InetSocketAddress(port));
		loop.register(ssc, new SessionFactory(ssl)).sync(millis);
	}
	
	public void stop(long millis) throws InterruptedException {
		loop.stop();
		loop.join(millis);
		if (loop.thread != null) {
			throw new InterruptedException();
		}
	}
	
	class ServerHandler extends AbstractStreamHandler {
		
		@Override
		public ISessionConfig getConfig() {
			DefaultSessionConfig config = new DefaultSessionConfig() {
			
				@Override
				public SSLEngine createSSLEngine(boolean clientMode) throws SSLEngineCreateException {
					SSLEngine engine;
					
					try {
						engine = Server.getSSLContext().createSSLEngine();
					} catch (Exception e) {
						throw new SSLEngineCreateException(e);
					}
					engine.setUseClientMode(clientMode);
					if (!clientMode) {
						engine.setNeedClientAuth(true);
					}
					return engine;
				}
			};
			return config;
		}
		
		@Override
		public void read(Object msg) {
			if (client != null) {
				client.writenf(msg);
				return;
			}
			
			if (skipConnection) {
				return;
			}
			
			BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream((byte[])msg)));
			
			try {
				String l = in.readLine();
				
				trace(l);
				if (l == null) {
					throw new Exception();
				}
				
				String[] items = l.split(" ");
				
				if (items.length != 3) {
					throw new Exception();
				}
				if (!items[0].equalsIgnoreCase("CONNECT") || !items[2].equalsIgnoreCase("HTTP/1.1")) {
					throw new Exception();
				}
				
				items = items[1].split(":");
				if (items.length != 2) {
					throw new Exception();
				}
				
				while ((l = in.readLine()) != null) {
					trace(l);
					if (l.isEmpty()) {
						if (in.readLine() != null) {
							throw new Exception();
						}
						break;
					}
				}
				
				SocketChannel sc = SocketChannel.open();
				sc.configureBlocking(false);
				sc.connect(new InetSocketAddress(InetAddress.getByName(items[0]), Integer.parseInt(items[1])));
				loop.register(sc, new ClientHandler());
			}
			catch (Exception e) {
				String s = "HTTP/1.1 400 Bad Request\r\n\r\n";
				
				trace(s.trim());
				trace("");
				getSession().writenf(s.getBytes());
				getSession().close();
			}
		}

		public void event(SessionEvent event) {
			switch (event) {
			case READY:
				server = (StreamSession) getSession();
				break;
				
			case ENDING:
				if (client != null) {
					client.close();
					client = null;
				}
				break;
				
			default:
			}
		}
	}

	class ClientHandler extends AbstractStreamHandler {

		@Override
		public void read(Object msg) {
			server.writenf(msg);
		}

		public void event(SessionEvent event) {
			switch (event) {
			case READY:
				client = (StreamSession) getSession();
				
				String s = "HTTP/1.1 "+status+"\r\n\r\n";
				
				trace(s.trim());
				trace("");
				if (appendPacket != null) {
					ByteBuffer buf = ByteBuffer.allocate(100);
					
					buf.put(s.getBytes());
					buf.put(appendPacket.toBytes());
					buf.flip();
					server.writenf(buf);
				}
				else {
					server.writenf(s.getBytes());
				}
				break;
				
			case ENDING:
				if (server != null) {
					server.close();
					server = null;
				}
				break;
				
			default:
			}
		}
	}
	
	class SessionFactory extends AbstractSessionFactory {

		SessionFactory(boolean ssl) {
			super(ssl);
		}
		
		@Override
		protected IStreamHandler createHandler(SocketChannel channel) {
			return new ServerHandler();
		}
		
	}
}
