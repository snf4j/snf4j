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
package org.snf4j.websocket;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

import org.snf4j.core.LockUtils;
import org.snf4j.core.SSLSession;
import org.snf4j.core.SelectorLoop;
import org.snf4j.core.StreamSession;
import org.snf4j.core.TestSSLEngine;
import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.codec.ICodecExecutor;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.session.SSLEngineCreateException;
import org.snf4j.core.timer.ITimer;
import org.snf4j.core.timer.TestTimer;
import org.snf4j.websocket.extensions.IExtension;
import org.snf4j.websocket.frame.CloseFrame;
import org.snf4j.websocket.frame.Frame;
import org.snf4j.websocket.frame.Opcode;
import org.snf4j.websocket.frame.PongFrame;
import org.snf4j.websocket.handshake.HandshakeDecoder;
import org.snf4j.websocket.handshake.HandshakeEncoder;
import org.snf4j.websocket.handshake.HandshakeFrame;
import org.snf4j.websocket.handshake.HandshakeRequest;
import org.snf4j.websocket.handshake.HandshakeResponse;

public class WSServer {
	
	public SelectorLoop loop;

	public int port;
	
	public boolean onlyTcp;
	
	public boolean ssl;

	public volatile ServerSocketChannel ssc;
	
	public volatile StreamSession session;
	
	public volatile Object msgRead;
	
	public volatile HandshakeResponse response;
	
	public volatile HandshakeRequest request;
	
	public volatile URI requestUri;
	
	public volatile boolean traceExceptionDetails;
	
	public volatile boolean traceCloseDetails;

	public volatile boolean changeHsDecoderMode;

	public volatile boolean changeHsEncoderMode;
	
	public volatile RuntimeException throwInException;
	
	public volatile int maxPayloadLength = -1;
	
	public volatile int maxHandshakeLength = -1;
	
	public volatile String[] customizedRequest;
	
	public volatile String[] customizedResponse;
	
	public volatile TestTimer timer;
	
	public volatile IExtension[] extensions;
	
	public volatile boolean handleCloseFrame = false;
	
	public volatile boolean waitForCloseMessage;
	
	public volatile IByteBufferAllocator allocator;
	
	public volatile boolean optimieDataCopying;

	public volatile String[] subProtocols;
	
	public AtomicInteger throwInExceptionCount = new AtomicInteger();
	
	final AtomicBoolean readLock = new AtomicBoolean(false);

	final AtomicBoolean openLock = new AtomicBoolean(false);
	
	final AtomicBoolean readyLock = new AtomicBoolean(false);

	final AtomicBoolean endingLock = new AtomicBoolean(false);

	final StringBuilder trace = new StringBuilder();
	
	static volatile SSLContext sslContext = null; 
	
	public SSLContext getSSLContext() throws Exception {
		if (sslContext == null) {
			synchronized (WSServer.class) {
				if (sslContext == null) {
					KeyStore ks = KeyStore.getInstance("JKS");
					KeyStore ts = KeyStore.getInstance("JKS");
					char[] password = "password".toCharArray();

					File file = new File(getClass().getClassLoader().getResource("keystore.jks").getFile());

					ks.load(new FileInputStream(file), password);
					ts.load(new FileInputStream(file), password);

					KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
					kmf.init(ks, password);
					TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
					tmf.init(ts);

					SSLContext ctx = SSLContext.getInstance("TLS");
					ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
					sslContext = ctx;
				}
			}
		}
		return sslContext;
	}
	
	public WSServer(int port, boolean ssl) {
		this.port = port;
		this.ssl = ssl;
	}
	
	public void resetDataLocks() {
		readLock.set(false);
	}
	
	void trace(String s) {
		synchronized (trace) {
			trace.append(s);
			trace.append('|');
		}
	}
	
	String getTrace() {
		String s;
		
		synchronized(trace) {
			s = trace.toString();
			trace.setLength(0);
		}
		return s;
	}
	
	public void start() throws Exception {
		if (loop == null) {
			loop = new SelectorLoop();
		}
		loop.start();
		
		ssc = ServerSocketChannel.open();
		
		ssc.configureBlocking(false);
		ssc.socket().bind(new InetSocketAddress(port));
		loop.register(ssc, new SessionFactory(ssl)).sync();		
	}

	public void stop(long millis) throws InterruptedException {
		loop.stop();
		if (!loop.join(millis)) {
			throw new InterruptedException();
		}
	}

	public void waitForMsgRead(long millis) throws Exception {
		LockUtils.waitFor(readLock, millis);
	}

	public void waitForOpen(long millis) throws Exception {
		LockUtils.waitFor(openLock, millis);
	}
	
	public void waitForReady(long millis) throws Exception {
		LockUtils.waitFor(readyLock, millis);
	}

	public void waitForEnding(long millis) throws Exception {
		LockUtils.waitFor(endingLock, millis);
	}

	public boolean isClient() {
		return WSServer.this instanceof WSClient;
	}
	
	StreamSession createSession(IWebSocketHandler handler) throws Exception {
		if (onlyTcp) {
			return ssl ? new SSLSession(handler, isClient()) : new StreamSession(handler); 
		}
		return ssl ? new SSLWebSocketSession(handler, isClient()) : new WebSocketSession(handler, isClient());
	}
	
	class SessionFactory extends AbstractWebSocketSessionFactory {
		
		SessionFactory(boolean ssl) {
			super(ssl);
		}
		
		public StreamSession create(SocketChannel channel) throws Exception {
			if (onlyTcp) {
				return createSession(createHandler(channel));
			}
			return super.create(channel);
		}
		
		@Override
		protected IWebSocketHandler createHandler(SocketChannel channel) {
			return new Handler();
		}
	}	
	
	class Config extends DefaultWebSocketSessionConfig {
		
		public Config(URI requestUri) {
			super(requestUri);
		}

		public Config() {
			super();
		}
		
		@Override
		public SSLEngine createSSLEngine(SocketAddress remoteAddress, boolean clientMode) throws SSLEngineCreateException {
			SSLEngine engine;
			try {
				if (clientMode && remoteAddress instanceof InetSocketAddress) {
					String host = ((InetSocketAddress)remoteAddress).getHostString();
					int port = ((InetSocketAddress)remoteAddress).getPort();
					
					engine = getSSLContext().createSSLEngine(host, port);
				}
				else {
					engine = getSSLContext().createSSLEngine();
				}
			} catch (Exception e) {
				throw new SSLEngineCreateException(e);
			}
			engine.setUseClientMode(clientMode);
			if (!clientMode) {
				engine.setNeedClientAuth(true);
			}
			return new TestSSLEngine(engine);
		}
		
		@Override
		public SSLEngine createSSLEngine(boolean clientMode) throws SSLEngineCreateException {
			return createSSLEngine(null, clientMode);
		}
		
		@Override
		public ICodecExecutor createCodecExecutor() {
			ICodecExecutor e = super.createCodecExecutor();
		
			if (changeHsDecoderMode) {
				e.getPipeline().replace(HANDSHAKE_DECODER, HANDSHAKE_DECODER, 
						new HandshakeDecoder(!isClientMode()));
			}
			if (changeHsEncoderMode) {
				e.getPipeline().replace(HANDSHAKE_ENCODER, HANDSHAKE_ENCODER, 
						new HandshakeEncoder(!isClientMode()));
			}
			return e;
		}		
		
		void customize(HandshakeFrame frame, String[] fields) {
			for (int i=0; i<fields.length; i+=2) {
				frame.addValue(fields[i], fields[i+1]);
			}
		}
		
		@Override
		public void customizeHeaders(HandshakeRequest request) {
			if (customizedRequest != null) {
				customize(request, customizedRequest);
			}
		}

		@Override
		public void customizeHeaders(HandshakeResponse response) {
			if (customizedResponse != null) {
				customize(response, customizedResponse);				
			}
		}
		
	}
	
	class Handler extends AbstractStreamHandler implements IWebSocketHandler {
		
		@Override
		public IWebSocketSessionConfig getConfig() {
			Config config;
			
			if (requestUri != null) {
				config = new Config(requestUri);
			}
			else {
				config = new Config();
			}
			if (maxHandshakeLength != -1) {
				config.setMaxHandshakeFrameLength(maxHandshakeLength);
			}
			if (maxPayloadLength != -1) {
				config.setMaxFramePayloadLength(maxPayloadLength);
			}
			if (extensions != null) {
				config.setSupportedExtensions(extensions);
			}
			if (subProtocols != null) {
				config.setSupportedSubProtocols(subProtocols);
			}
			config.setHandleCloseFrame(handleCloseFrame);
			config.setWaitForInboundCloseMessage(waitForCloseMessage);
			config.setOptimizeDataCopying(optimieDataCopying);
			return config;
		}
		
		@Override
		public ISessionStructureFactory getFactory() {
			return new DefaultSessionStructureFactory() {
				@Override
				public ITimer getTimer() {
					return timer;
				}		
				
				@Override
				public IByteBufferAllocator getAllocator() {
					return allocator == null ? super.getAllocator() : allocator;
				}
			};
		}

		@Override
		public void read(Object msg) {
			msgRead = msg;
			request = null;
			response = null;
			if (msg instanceof HandshakeResponse) {
				response = (HandshakeResponse) msg;
				trace("RES(" + response.getStatus() + ")");
			}
			else if (msg instanceof HandshakeRequest) {
				request = (HandshakeRequest) msg;
				trace("REQ(" + request.getUri() + ")");
			}
			else if (msg instanceof Frame) {
				Frame frame = (Frame)msg;
				
				if (traceCloseDetails && frame.getOpcode() == Opcode.CLOSE) {
					trace("F(" + frame.getOpcode() + "=" + ((CloseFrame)frame).getStatus() + ":" + ((CloseFrame)frame).getReason() + ")");
				}
				else {
					trace("F(" + frame.getOpcode() + ")");
				}
				if (frame.getOpcode() == Opcode.PING) {
					getSession().writenf(new PongFrame(frame.getPayload()));
				}
			}
			LockUtils.notify(readLock);
		}
		
		@SuppressWarnings("incomplete-switch")
		@Override
		public void event(SessionEvent event) {
			trace(event.name().substring(0, 2));
			
			switch (event) {
			case CREATED:
				session = (StreamSession) getSession();
				break;
				
			case OPENED:
				LockUtils.notify(openLock);
				break;
				
			case READY:
				LockUtils.notify(readyLock);
				break;

			case ENDING:
				LockUtils.notify(endingLock);
				break;
				
			}
			if (event == SessionEvent.CREATED) {
				session = (StreamSession) getSession();
			}
		}
		
		@Override
		public void exception(Throwable t) {
			trace("EX");
			if (traceExceptionDetails) {
				trace(t.getMessage());
			}
			if (throwInException != null) {
				throwInExceptionCount.incrementAndGet();
				throw throwInException;
			}
		}		
		
		@Override
		public boolean incident(SessionIncident incident, Throwable t) {
			trace(incident.name());
			return super.incident(incident, t);
		}		
		
		@Override
		public void timer(Object event) {
			trace("TE(" + event + ")");
		}
		
		@Override
		public void timer(Runnable task) {
			trace("TT(" + task + ")");
		}

	}
}
