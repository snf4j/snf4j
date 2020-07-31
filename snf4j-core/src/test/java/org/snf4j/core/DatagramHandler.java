/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2020 SNF4J contributors
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

import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

import org.snf4j.core.allocator.DefaultAllocator;
import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.allocator.TestAllocator;
import org.snf4j.core.codec.DefaultCodecExecutor;
import org.snf4j.core.codec.ICodecExecutor;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.IDatagramHandlerFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.AbstractDatagramHandler;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.session.SSLEngineCreateException;
import org.snf4j.core.timer.ITimeoutModel;
import org.snf4j.core.timer.ITimer;

public class DatagramHandler {
	SelectorLoop loop;
	int port;
	public TestDTLSEngine testEngine;
	public boolean nullEngine;
	public boolean engineException;
	public SSLEngine engine;
	volatile public String engineArguments;
	public boolean ssl;
	public boolean sslClient;
	public boolean sslClientMode = true;
	public boolean sslRemoteAddress;
	public boolean proxyAction;
	volatile DatagramSession session;
	volatile DatagramChannel channel;
	SocketAddress localAddress, remoteAddress;
	boolean connected;
	ThreadFactory threadFactory;
	boolean registerConnectedSession;
	long throughputCalcInterval = 1000;
	boolean ignorePossiblyIncomplete = true;
	volatile EndingAction endingAction = EndingAction.DEFAULT;
	volatile boolean createNullHandler;
	boolean directAllocator;
	TestAllocator allocator;
	ITimer timer;
	ITimeoutModel timeoutModel;
	boolean canOwnPasseData;
	volatile boolean useTestSession;
	DefaultCodecExecutor codecPipeline;
	DefaultCodecExecutor codecPipeline2;
	volatile boolean incident;
	volatile boolean incidentRecordException;
	volatile boolean incidentClose;
	volatile boolean incidentQuickClose;
	volatile boolean incidentDirtyClose;
	volatile boolean exceptionClose;
	volatile boolean waitForCloseMessage;
	public volatile boolean throwInException;
	public final AtomicInteger throwInExceptionCount = new AtomicInteger();
	public volatile boolean throwInEvent;
	public final AtomicInteger throwInEventCount = new AtomicInteger();
	public volatile boolean throwInRead;
	public final AtomicInteger throwInReadCount = new AtomicInteger();
	public volatile boolean throwInSuperRead;
	public final AtomicInteger throwInSuperReadCount = new AtomicInteger();
	public volatile boolean throwInIncident;
	
	AtomicBoolean sessionOpenLock = new AtomicBoolean(false);
	AtomicBoolean sessionReadyLock = new AtomicBoolean(false);
	AtomicBoolean sessionEndingLock = new AtomicBoolean(false);
	AtomicBoolean dataReceivedLock = new AtomicBoolean(false);
	AtomicBoolean dataReadLock = new AtomicBoolean(false);
	AtomicBoolean dataSentLock = new AtomicBoolean(false);
	
	EventType closeInEvent;
	StoppingType closeType = StoppingType.GENTLE;

	StringBuilder recorder = new StringBuilder();
	
	boolean recordDataEventDetails;
	boolean recordSessionNameInTimer;
	
	DatagramSession initSession;

	static Map<EventType, String> eventMapping = new HashMap<EventType, String>();

	IDatagramHandlerFactory factory;
	boolean useDatagramServerHandler;
	volatile SocketAddress handlerFactoryRemoteAddress;
	
	volatile long handshakeTimeout = 5000;
	volatile long reopenBlockedInterval = 4999;
	
	static volatile SSLContext sslContext = null; 
	
	public static double JAVA_VER = Double.parseDouble(System.getProperty("java.specification.version"));
	
	static {
		eventMapping.put(EventType.SESSION_CREATED, "SCR");
		eventMapping.put(EventType.SESSION_OPENED, "SOP");
		eventMapping.put(EventType.SESSION_READY, "RDY");		
		eventMapping.put(EventType.SESSION_CLOSED, "SCL");
		eventMapping.put(EventType.SESSION_ENDING, "SEN");
		eventMapping.put(EventType.DATA_RECEIVED, "DR");
		eventMapping.put(EventType.DATA_SENT, "DS");
		eventMapping.put(EventType.EXCEPTION_CAUGHT, "EXC");
	}
	
	public SSLContext getSSLContext() throws Exception {
		if (sslContext == null) {
			synchronized (Server.class) {
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

					SSLContext ctx = SSLContext.getInstance("DTLS");
					ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
					sslContext = ctx;
				}
			}
		}
		return sslContext;
	}
	public DatagramHandler(int port) {
		this.port = port;
	}

	public DatagramSession createSession() {
		initSession = useTestSession ? new TestDatagramSession(new Handler()) 
				: new DatagramSession(new Handler());
		return initSession;
	}

	public void setThreadFactory(ThreadFactory tf) {
		threadFactory = tf;
	}

	public DatagramSession getSession() {
		return session;
	}
	
	void record(String data) {
		synchronized(recorder) {
			recorder.append(data);
			recorder.append('|');
		}
	}
	
	public String getRecordedData(boolean clear) {
		String s;
		
		synchronized(recorder) {
			s = recorder.toString();
			if (clear) {
				recorder.setLength(0);
			}
		}
		return s;
	}
	
	public String trimRecordedData(String prefix) {
		String s;
		
		synchronized(recorder) {
			s = recorder.toString();
			recorder.setLength(0);
		}
		if (prefix != null && prefix.length() > 0) {
			if (s.startsWith(prefix)) {
				s = s.substring(prefix.length());
			}
		}
		return s;
	}

	public String getRecordedData(String limit, boolean clear) {
		String s;
		
		synchronized(recorder) {
			s = recorder.toString();
			if (clear) {
				recorder.setLength(0);
			}

			int i = s.indexOf(limit);
			if (i != -1) {
				if (clear) {
					recorder.append(s.substring(i + limit.length()));
				}
				s = s.substring(0, i);
			}
		}
		return s;
	}
	
	public void startServer() throws Exception {
		start(false);
	}
	
	public void startClient() throws Exception {
		start(true);
	}
	
	void start(boolean connected) throws Exception {
		start(connected, false, null);
	}
	
	void start(boolean connected, SelectorLoop loop) throws Exception {
		start(connected, false, loop);
	}
	
	void start(boolean connected, boolean firstRegistrate, SelectorLoop loop) throws Exception {
		if (loop == null) {
			this.loop = new SelectorLoop();
			loop = this.loop;
		}
		else {
			this.loop = loop;
		}

		if (threadFactory != null) {
			loop.setThreadFactory(threadFactory);
		}
		
		if (!firstRegistrate) {
			loop.start();
		}
		
		IDatagramHandler handler;
		if (useDatagramServerHandler) {
			if (factory != null) {
				handler = ssl ? new DTLSHandler(factory) : new ServerHandler(factory);
			}
			else {
				if (ssl) {
					handler = new DTLSHandler(new IDatagramHandlerFactory() {

						@Override
						public IDatagramHandler create(
								SocketAddress remoteAddress) {
							handlerFactoryRemoteAddress = remoteAddress;
							return createNullHandler ? null : new Handler();
						}
					},
					new Handler(true).getConfig(),
					new StructureFactory());
					
				}
				else {
					handler = new ServerHandler(new IDatagramHandlerFactory() {

						@Override
						public IDatagramHandler create(
								SocketAddress remoteAddress) {
							handlerFactoryRemoteAddress = remoteAddress;
							return createNullHandler ? null : new Handler();
						}
					},
					new Handler(true).getConfig(),
					new StructureFactory());
				}
			}
		}
		else {
			handler = new Handler();
		}
		
		DatagramChannel dc = DatagramChannel.open();
		channel = dc;
		dc.configureBlocking(false);
		if (this.connected) {
			connected = true;
		}
		if (connected) {
			if (localAddress != null) {
				dc.socket().bind(localAddress);
			}
			if (remoteAddress != null) {
				dc.connect(remoteAddress);
			}
			else {
				dc.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port));
			}
			
			if (registerConnectedSession) {
				if (ssl) {
					session = new DTLSSession(sslRemoteAddress ? remoteAddress : null, new Handler(), sslClientMode);
				}
				session = useTestSession ? new TestDatagramSession(new Handler()) 
						: new DatagramSession(new Handler());
				session.setChannel(dc);
				session.preCreated();
				session.event(SessionEvent.CREATED);
				loop.register(dc, session);
			}
			else {
				if (initSession == null) {
					if (ssl) {
						session = (DatagramSession) loop.register(dc, new DTLSSession(sslRemoteAddress ? remoteAddress : null, new Handler(), sslClientMode)).getSession();
					}
					else if (useTestSession) {
						session = (DatagramSession) loop.register(dc, new TestDatagramSession(handler)).getSession();
					}
					else {
						session = (DatagramSession) loop.register(dc, new DatagramSession(handler)).getSession();
					}
				}
				else {
					loop.register(dc, initSession);
				}
			}
		}
		else {
			if (localAddress != null) {
				dc.socket().bind(localAddress);
			}
			else {
				dc.socket().bind(new InetSocketAddress(port));
			}
			if (registerConnectedSession) {
				session = useTestSession ? new TestDatagramSession(new Handler()) 
						: new DatagramSession(new Handler());
				session.setChannel(dc);
				session.preCreated();
				session.event(SessionEvent.CREATED);
				loop.register(dc, session);
			}
			else {
				if (sslClient) {
					session = (DatagramSession) loop.register(dc, new DTLSSession(sslRemoteAddress ? remoteAddress : null, new Handler(), sslClientMode)).getSession();
				}
				else {
					session = (DatagramSession) loop.register(dc, useTestSession ? new TestDatagramSession(handler) 
							: new DatagramSession(handler)).getSession();
				}
			}
		}

		if (firstRegistrate) {
			loop.start();
		}
	}
	
	void quickStop(long millis) throws InterruptedException {
		loop.quickStop();
		loop.join(millis);
		if (loop.thread != null) {
			throw new InterruptedException();
		}
	}

	public void stop(long millis) throws InterruptedException {
		loop.stop();
		loop.join(millis);
		if (loop.thread != null) {
			throw new InterruptedException();
		}
	}

	void write(Packet packet) {
		session.write(packet.toBytes());
	}

	public void waitForSessionOpen(long millis) throws InterruptedException {
		LockUtils.waitFor(sessionOpenLock, millis);
	}

	public void waitForSessionReady(long millis) throws InterruptedException {
		LockUtils.waitFor(sessionReadyLock, millis);
	}

	public void waitForSessionEnding(long millis) throws InterruptedException {
		LockUtils.waitFor(sessionEndingLock, millis);
	}

	void waitForDataReceived(long millis) throws InterruptedException {
		LockUtils.waitFor(dataReceivedLock, millis);
	}

	public void waitForDataRead(long millis) throws InterruptedException {
		LockUtils.waitFor(dataReadLock, millis);
	}
	
	public void waitForDataSent(long millis) throws InterruptedException {
		LockUtils.waitFor(dataSentLock, millis);
	}

	class StructureFactory extends DefaultSessionStructureFactory {
		@Override
		public IByteBufferAllocator getAllocator() {
			if (allocator != null) {
				return allocator;
			}
			return new DefaultAllocator(directAllocator);
		}
		
		@Override
		public ITimer getTimer() {
			return timer;
		}
		
		@Override
		public ITimeoutModel getTimeoutModel() {
			return timeoutModel;
		}
	}
	
	class ServerHandler extends DatagramServerHandler {
		public ServerHandler(IDatagramHandlerFactory handlerFactory) {
			super(handlerFactory);
		}
		
		public ServerHandler(IDatagramHandlerFactory handlerFactory, ISessionConfig config) {
			super(handlerFactory, config, null);
		}
		
		public ServerHandler(IDatagramHandlerFactory handlerFactory, ISessionConfig config, ISessionStructureFactory factory) {
			super(handlerFactory, config, factory);
		}
		
		@Override
		public void read(SocketAddress remoteAddress, byte[] datagram) {
			if (throwInSuperRead) {
				throwInSuperReadCount.incrementAndGet();
				throw new NullPointerException();
			}
			super.read(remoteAddress, datagram);
		}
		
		@Override
		public void read(SocketAddress remoteAddress, Object datagram) {
			if (throwInSuperRead) {
				throwInSuperReadCount.addAndGet(100);
				throw new NullPointerException();
			}
			super.read(remoteAddress, datagram);
		}
		
	}
	
	class DTLSHandler extends DTLSServerHandler {
		public DTLSHandler(IDatagramHandlerFactory handlerFactory) {
			super(handlerFactory);
		}
		
		public DTLSHandler(IDatagramHandlerFactory handlerFactory, ISessionConfig config) {
			super(handlerFactory, config);
		}
		
		public DTLSHandler(IDatagramHandlerFactory handlerFactory, ISessionConfig config, ISessionStructureFactory factory) {
			super(handlerFactory, config, factory);
		}
	}
	
	class Handler extends AbstractDatagramHandler {
		boolean codec2;
		
		Handler() {}
		Handler(boolean codec2) { this.codec2 = codec2; } 

		@Override
		public ISessionConfig getConfig() {
			DefaultSessionConfig config = new DefaultSessionConfig() {
				@Override
				public SSLEngine createSSLEngine(boolean clientMode) throws SSLEngineCreateException {
					if (nullEngine) {
						return null;
					}
					if (engineException) {
						throw new SSLEngineCreateException();
					}
					
					SSLEngine engine = DatagramHandler.this.testEngine;
					
					if (engine == null && DatagramHandler.JAVA_VER < 9.0) {
						System.out.println("[INFO] JAVA_VER ("+DatagramHandler.JAVA_VER+") < 9.0 - using TestDTLSEngine");
						engine = new TestDTLSEngine();
					}
					
					try {
						engine = engine == null ? getSSLContext().createSSLEngine() : engine;
					} catch (Exception e) {
						throw new SSLEngineCreateException(e);
					}
					DatagramHandler.this.engine = engine;
					engine.setUseClientMode(clientMode);
					if (!clientMode) {
						engine.setNeedClientAuth(true);
					}
					return new TestSSLEngine(engine);
				}
				
				@Override
				public SSLEngine createSSLEngine(SocketAddress remoteAddress, boolean clientMode) throws SSLEngineCreateException {
					engineArguments = "" + remoteAddress + "|" + clientMode;
					return createSSLEngine(clientMode);
				}
				
				@Override
				public ICodecExecutor createCodecExecutor() {
					if (!codec2) {
						return codecPipeline;
					}
					return codecPipeline2;
				}
			};
			
			config.setMinInBufferCapacity(1024);
			config.setMinOutBufferCapacity(1024);
			config.setThroughputCalculationInterval(throughputCalcInterval);
			config.setIgnorePossiblyIncompleteDatagrams(ignorePossiblyIncomplete);
			config.setEndingAction(endingAction);
			config.setCanOwnDataPassedToWriteAndSendMethods(canOwnPasseData);
			config.setEngineHandshakeTimeout(handshakeTimeout);
			config.setDatagramServerSessionNoReopenPeriod(reopenBlockedInterval);
			config.setWaitForInboundCloseMessage(waitForCloseMessage);
			return config;
		}

		@Override
		public ISessionStructureFactory getFactory() {
			return new StructureFactory();
		}
		
		@Override
		public void read(byte[] datagram) {
			read(null, datagram);
		}

		@Override
		public void read(Object msg) {
			read(null, msg);
		}

		void write(SocketAddress remoteAddress, Object msg) {
			DatagramSession session = (DatagramSession) getSession();

			if (remoteAddress == null) {
				session.write(msg);
			}
			else {
				session.send(remoteAddress, msg);
			}
		}
		
		@Override
		public void read(SocketAddress remoteAddress, Object msg) {
			if (remoteAddress == null) {
				record("M("+msg.toString()+")");
			}
			else {
				record("$M("+msg.toString()+")");
			}
			
			if (msg instanceof Packet) {
				Packet packet = (Packet)msg;
				
				switch (packet.type) {
					case ECHO:
						write(remoteAddress, new Packet(PacketType.ECHO_RESPONSE, packet.payload));
						break;
						
					default:
						break;
				
				}

			}
			LockUtils.notify(dataReadLock);
			
			if(throwInRead) {
				throwInReadCount.incrementAndGet();
				throw new NullPointerException();
			}
			
		}
		
		void write(SocketAddress remoteAddress, byte[] datagram) {
			DatagramSession session = (DatagramSession) getSession();

			if (remoteAddress == null) {
				session.write(datagram);
			}
			else {
				session.send(remoteAddress, datagram);
			}
		}
		
		@Override
		public void read(SocketAddress remoteAddress, byte[] datagram) {
			Packet packet = Packet.fromBytes(datagram);
			
			if (remoteAddress == null) {
				record(packet.type+"("+packet.payload+")");
			}
			else {
				record("$"+packet.type+"("+packet.payload+")");
			}
			
			switch (packet.type) {
			case ECHO:
				write(remoteAddress, new Packet(PacketType.ECHO_RESPONSE, packet.payload).toBytes());
				break;

			case CLOSE:
				getSession().close();
				break;
				
			case QUICK_CLOSE:
				getSession().quickClose();
				break;

			case WRITE_AND_CLOSE:
				write(remoteAddress, new Packet(PacketType.WRITE_AND_CLOSE_RESPONSE, packet.payload).toBytes());
				getSession().close();
				write(remoteAddress, new Packet(PacketType.NOP).toBytes());
				break;
				
			case WRITE_AND_QUICK_CLOSE:
				write(remoteAddress, new Packet(PacketType.NOP).toBytes());
				getSession().quickClose();
				break;
				
			case WRITE_AND_WAIT:
				long i = Long.parseLong(packet.payload);

				write(remoteAddress, new Packet(PacketType.WRITE_AND_WAIT_RESPONSE, packet.payload).toBytes());
				try {
					Thread.sleep(i);
				} catch (InterruptedException e) {
				}
				break;
				
			case SUSPEND_WRITE_CLOSE:
				getSession().suspendWrite();
				if (packet.payload.indexOf('W') != -1) {
					getSession().write(new Packet(PacketType.NOP).toBytes());
				}
				if (packet.payload.indexOf('Q') != -1) {
					getSession().quickClose();
				}
				else {
					getSession().close();
				}
				break;
				
			default:
				break;
			}
			LockUtils.notify(dataReadLock);
			
			if(throwInRead) {
				throwInReadCount.incrementAndGet();
				throw new NullPointerException();
			}
		}
		
		private boolean event(EventType type, long length, SocketAddress remoteAddress, boolean ignoreAddress) {
			record(eventMapping.get(type));
			if (recordDataEventDetails && length != -1) {
				if (ignoreAddress) {
					record("" + length);
				}
				else {
					record("" + length + ";" + remoteAddress);
				}
			}

			switch (type) {
			case SESSION_CREATED:
				session = (DatagramSession) getSession();
				break;
				
			case SESSION_OPENED:
				LockUtils.notify(sessionOpenLock);
				break;

			case SESSION_READY:
				LockUtils.notify(sessionReadyLock);
				break;
				
			case SESSION_ENDING:
				LockUtils.notify(sessionEndingLock);
				break;
				
			case DATA_RECEIVED:
				LockUtils.notify(dataReceivedLock);
				break;
				
			case DATA_SENT:
				LockUtils.notify(dataSentLock);
				break;
				
			default:
				break;

			}
			
			if (closeInEvent == type) {
				switch (closeType) {
				case GENTLE:
					getSession().close();
					break;
				case QUICK:
					getSession().quickClose();
					break;
				case DIRTY:
					getSession().dirtyClose();
					break;
				}
			}
		
			if (throwInEvent) {
				throwInEventCount.incrementAndGet();
				throw new NullPointerException();
			}
			
			return false;
		}

		@Override
		public void event(SessionEvent event) {
			event(event.type(), -1, null, true);
		}

		@Override
		public void event(SocketAddress remoteAddress, DataEvent event, long length) {
			event(event.type(), length, remoteAddress, false);
		}
		
		@Override
		public void event(DataEvent event, long length) {
			event(event.type(), length, null, true);
		}

		@Override
		public void exception(Throwable t) {
			event(EventType.EXCEPTION_CAUGHT, -1, null, true);
			if (throwInException) {
				throwInExceptionCount.incrementAndGet();
				throw new NullPointerException();
			}
			if (exceptionClose) {
				record("close");
				getSession().close();
			}
		}

		@Override
		public boolean incident(SessionIncident incident, Throwable t) {
			if (incidentRecordException) {
				record(incident.toString() + "(" + t.getMessage() +")");
			}
			else {
				record(incident.toString());
			}
			if (incidentClose) {
				getSession().close();
				return true;
			}
			if (incidentQuickClose) {
				getSession().quickClose();
				return true;
			}
			if (incidentDirtyClose) {
				getSession().dirtyClose();
				return true;
			}
			
			if (DatagramHandler.this.throwInIncident) {
				throw new IllegalArgumentException();
			}
			
			return DatagramHandler.this.incident;
		}
		
		@Override
		public void timer(Object event) {
			if (recordSessionNameInTimer) {
				record("TIM;" + event + ";" + getSession().getName());
			}
			else {
				record("TIM;" + event);
			}
		}
		
	}
}
