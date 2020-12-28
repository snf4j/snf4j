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
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
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
import org.snf4j.core.factory.AbstractSessionFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.future.BlockingFutureOperationException;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.pool.ISelectorLoopPool;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.session.SSLEngineCreateException;
import org.snf4j.core.timer.ITimeoutModel;
import org.snf4j.core.timer.ITimer;

public class Server {
	public SelectorLoop loop;
	public int port;
	public boolean ssl;
	public StreamSession session;
	public volatile SocketAddress sessionLocal;
	public volatile SocketAddress sessionRemote;
	public volatile boolean sslRemoteAddress;
	public StreamSession initSession;
	public ThreadFactory threadFactory;
	public ISelectorLoopController controller;
	public long throughputCalcInterval = 1000;
	public boolean directAllocator;
	public boolean ignoreAvailableException;
	public TestAllocator allocator;
	public ITimer timer;
	public ConcurrentMap<Object, Object> attributes;
	public Executor executor;
	public DefaultCodecExecutor codecPipeline;
	public ISelectorLoopPool pool;
	volatile ByteBuffer bufferRead;
	public volatile EndingAction endingAction = EndingAction.DEFAULT;
	public volatile StringBuilder serverSocketLogs = new StringBuilder();
	public volatile ServerSocketChannel ssc;
	public volatile ServerSocketChannel registeredSsc;
	public volatile ServerSocketChannel closedSsc;
	public volatile boolean useTestSession;
	public volatile boolean recordSessionId;
	public volatile boolean waitForCloseMessage;
	public volatile boolean dontReplaceException;
	public volatile boolean optimizeDataCopying;
	public volatile int maxWriteSpinCount = -1;

	public volatile int availableCounter;
	
	public volatile int minInBufferCapacity = 1024;
	public volatile int minOutBufferCapacity = 1024;
	
	public volatile boolean exceptionRecordException;
	public volatile boolean incident;
	public volatile boolean incidentRecordException;
	public volatile boolean incidentClose;
	public volatile boolean incidentQuickClose;
	public volatile boolean incidentDirtyClose;
	public volatile boolean throwInRead;
	public volatile boolean throwInException;
	public volatile boolean throwInIncident;
	public volatile boolean throwInEvent;
	public volatile boolean throwInCreateSession;
	public volatile boolean throwInTimer;
	public final AtomicInteger throwInEventCount = new AtomicInteger();
	public final AtomicInteger throwInExceptionCount = new AtomicInteger();
	public final AtomicInteger throwInTimerCount = new AtomicInteger();
	
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
	
	static Map<EventType, String> eventMapping = new HashMap<EventType, String>();

	static volatile SSLContext sslContext = null; 
	
	volatile long handshakeTimeout = 5000;
	
	static final LinkedList<Server> lastServers = new LinkedList<Server>();
	
	volatile int maxSSLAppBufRatio = 100;
	volatile int maxSSLNetBufRatio = 100;
	
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

					SSLContext ctx = SSLContext.getInstance("TLS");
					ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
					sslContext = ctx;
				}
			}
		}
		return sslContext;
	}
	
	public Server(int port) {
		this.port = port;
		lastServers.add(this);
		if (lastServers.size() > 10) {
			lastServers.removeFirst();
		}
	}

	public Server(int port, boolean ssl) {
		this.port = port;
		this.ssl = ssl;
		lastServers.add(this);
		if (lastServers.size() > 10) {
			lastServers.removeFirst();
		}
	}
	
	public Server findRemote() {
		for (Iterator<Server> i=lastServers.iterator(); i.hasNext();) {
			Server s = i.next();
			
			if (s.sessionRemote != null && s.sessionLocal != null) {
				if (s.sessionRemote.equals(sessionLocal) && s.sessionLocal.equals(sessionRemote)) {
					return s;
				}
			}
		}
		return null;
	}
	
	public String getServerSocketLogs() {
		String s = serverSocketLogs.toString();
		serverSocketLogs.setLength(0);
		return s;
	}
	
	public void setThreadFactory(ThreadFactory tf) {
		threadFactory = tf;
	}
	
	public StreamSession getSession() {
		return session;
	}
	
	public SelectorLoop getSelectLoop() {
		return loop;
	}
	
	void record(String data) {
		synchronized(recorder) {
			recorder.append(data);
			recorder.append('|');
		}
	}
	
	public String getOrderedRecordedData(boolean clean) {
		String s = getRecordedData(clean);
		Map<Integer, StringBuilder> map = new HashMap<Integer, StringBuilder>();
		List<Integer> ids = new ArrayList<Integer>();
		StringBuilder sb = new StringBuilder();
		
		int i;

		while ((i = s.indexOf('|')) != -1) {
			int i0 = s.indexOf('@');
			
			if (i0 > i || i == -1) {
				sb.append(s.substring(0, i+1));
			}
			else {
				Integer id = Integer.parseInt(s.substring(i0+1, i));
				StringBuilder sb2 = map.get(id);
				if (sb2 == null) {
					sb2 = new StringBuilder();
					map.put(id, sb2);
					ids.add(id);
				}
				sb2.append(s.substring(0, i0));
				sb2.append('@');
				sb2.append(ids.indexOf(id)+1);
				sb2.append('|');
			}
			s = s.substring(i+1);
		}
		
		Integer[] sortedIds = new Integer[ids.size()];
		Arrays.sort(ids.toArray(sortedIds));
		
		for (i = 1; i<=sortedIds.length; ++i) {
			int origIndex = ids.indexOf(sortedIds[i-1]);
			s = map.get(ids.get(origIndex)).toString();
			++origIndex;
			if (origIndex != i) {
				s = s.replace("@"+origIndex+"|", "@" + i + "|");
			}
			sb.append(s);
		}
		return sb.toString();
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
	
	public void start() throws Exception {
		start(false, null);
	}
	
	public void start(boolean firstRegistrate) throws Exception {
		start(firstRegistrate, null);
	}
	
	public void start(boolean firstRegistrate, SelectorLoop loop) throws Exception {
		if (loop == null) {
			this.loop = new SelectorLoop();
			loop = this.loop;
		}
		else {
			this.loop = loop;
		}
		if (pool != null) {
			loop.setPool(pool);
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
		
		ssc = ServerSocketChannel.open();
		
		ssc.configureBlocking(false);
		ssc.socket().bind(new InetSocketAddress(port));
		loop.register(ssc, new SessionFactory());
		
		if (firstRegistrate) {
			loop.start();
		}
	}
	
	public IFuture<Void> write(Packet packet) {
		return session.write(packet.toBytes());
	}
	
	public void quickStop(long millis) throws InterruptedException {
		loop.quickStop();
		loop.join(millis);
		if (loop.thread != null) {
			throw new InterruptedException();
		}
	}

	public void dirtyStop(long millis) throws InterruptedException {
		loop.dirtyStop();
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
	
	public void waitForSessionOpen(long millis) throws InterruptedException {
		LockUtils.waitFor(sessionOpenLock, millis);
	}
	
	public void waitForSessionReady(long millis) throws InterruptedException {
		LockUtils.waitFor(sessionReadyLock, millis);
	}
	
	public void waitForSessionEnding(long millis) throws InterruptedException {
		LockUtils.waitFor(sessionEndingLock, millis);
	}

	public void waitForDataReceived(long millis) throws InterruptedException {
		LockUtils.waitFor(dataReceivedLock, millis);
	}

	public void waitForDataRead(long millis) throws InterruptedException {
		LockUtils.waitFor(dataReadLock, millis);
	}
	
	public void waitForDataSent(long millis) throws InterruptedException {
		LockUtils.waitFor(dataSentLock, millis);
	}
	
	class SessionFactory extends AbstractSessionFactory {

		@Override
		public IStreamHandler createHandler(SocketChannel channel) {
			return new Handler();
		}

		@Override
		public StreamSession create(SocketChannel channel) throws Exception {
			if (throwInCreateSession) {
				throw new Exception("");
			}
			if (ssl) {
				return new SSLSession(channel.socket().getRemoteSocketAddress(), createHandler(channel), false);
			}
			return useTestSession ? new TestStreamSession(createHandler(channel)) : 
				new StreamSession(createHandler(channel));
		}
		
		@Override
		public void registered(ServerSocketChannel channel) {
			registeredSsc = channel;
			serverSocketLogs.append("R|");
		}

		@Override
		public void closed(ServerSocketChannel channel) {
			closedSsc = channel; 
			serverSocketLogs.append("C|");
		}

		@Override
		public void exception(ServerSocketChannel channel, Throwable t) {
			closedSsc = channel; 
			serverSocketLogs.append("X|");
		}
		
	}
	
	class SessionStructureFactory implements ISessionStructureFactory {

		@Override
		public IByteBufferAllocator getAllocator() {
			if (allocator != null) {
				return allocator;
			}
			return directAllocator ? new DefaultAllocator(true) : DefaultAllocator.DEFAULT;
			
		}

		@Override
		public ConcurrentMap<Object, Object> getAttributes() {
			return attributes;
		}

		@Override
		public Executor getExecutor() {
			return executor;
		}
		
		@Override
		public ITimer getTimer() {
			return timer;
		}

		@Override
		public ITimeoutModel getTimeoutModel() {
			return null;
		}
		
	}
	
	class Handler extends AbstractStreamHandler {

		Handler() {
			super(null);
		}
		
		void record(String data) {
			if (recordSessionId) {
				Server.this.record(data + "@" + getSession().getId());
			}
			else {
				Server.this.record(data);
			}
		}
		
		@Override
		public ISessionConfig getConfig() {
			DefaultSessionConfig config = new DefaultSessionConfig() {
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
					return codecPipeline;
				}
			};
			
			config.setMinInBufferCapacity(minInBufferCapacity);
			config.setMinOutBufferCapacity(minOutBufferCapacity);
			config.setThroughputCalculationInterval(throughputCalcInterval);
			config.setEndingAction(endingAction);
			try {
				Field f = DefaultSessionConfig.class.getDeclaredField("maxSSLApplicationBufferSizeRatio");
				f.setAccessible(true);
				f.setInt(config, maxSSLAppBufRatio);
				f = DefaultSessionConfig.class.getDeclaredField("maxSSLNetworkBufferSizeRatio");
				f.setAccessible(true);
				f.setInt(config, maxSSLNetBufRatio);
			}
			catch (Exception e) {
			}
			config.setWaitForInboundCloseMessage(waitForCloseMessage);
			config.setOptimizeDataCopying(optimizeDataCopying);
			config.setEngineHandshakeTimeout(handshakeTimeout);
			if (maxWriteSpinCount != -1) {
				config.setMaxWriteSpinCount(maxWriteSpinCount);
			}
			return config;
		}

		@Override
		public ISessionStructureFactory getFactory() {
			return new SessionStructureFactory();
		}

		@Override
		public int available(ByteBuffer buffer, boolean flipped) {
			++availableCounter;
			if (!directAllocator && !ignoreAvailableException) throw new IllegalStateException();

			ByteBuffer dupBuffer = buffer.duplicate();
			
			if (!flipped) {
				dupBuffer.flip();
			}
			
			byte[] d = new byte[dupBuffer.remaining()];
			
			dupBuffer.get(d);
			return available0(d, 0, d.length);
		}

		@Override
		public int available(byte[] buffer, int off, int len) {
			++availableCounter;
			if (directAllocator && !ignoreAvailableException) throw new IllegalStateException();
			return available0(buffer, off, len);
		}

		ByteBuffer bigPacket;

		int available0(byte[] buffer, int off, int len) {
			if (bigPacket == null) {
				int read = Packet.available(buffer, off, len);
				
				if (read < 0) {
					read = -read;
					bigPacket = ByteBuffer.allocate(read);
					return len;
				}
				return read;
			}
			else {
				if (len <= bigPacket.remaining()) {
					return len;
				}
				else {
					return bigPacket.remaining();
				}
			}
		}
		
		@Override
		public void read(Object msg) {
			
			if (msg instanceof ByteBuffer) {
				ByteBuffer bb = (ByteBuffer)msg;
				byte[] b = new byte[bb.remaining()];
				
				bb.get(b);
				bufferRead = bb;
				record("BUF");
				read(b);
				return;
			}
			
			record("M("+msg.toString()+")");
			
			if (msg instanceof Packet) {
				Packet packet = (Packet)msg;
				
				switch (packet.type) {
					case ECHO:
						getSession().write(new Packet(PacketType.ECHO_RESPONSE, packet.payload));
						break;
						
					default:
						break;
				
				}

			}
			LockUtils.notify(dataReadLock);
		}
		
		@Override
		public void read(byte[] data) {
			Packet packet;
			
			if (bigPacket != null) {
				bigPacket.put(data);
				if (!bigPacket.hasRemaining()) {
					data = new byte[bigPacket.position()];
					bigPacket.flip();
					bigPacket.get(data);
					bigPacket = null;
					packet = Packet.fromBytes(data);
				}
				else {
					return;
				}
			}
			else {
				packet = Packet.fromBytes(data);
			}
			
			if (packet.type == PacketType.BIG_NOP) {
				record(packet.type+"("+packet.payload.length()+")");
			}
			else {
				record(packet.type+"("+packet.payload+")");
			}
			
			switch (packet.type) {
			case ECHO:
				getSession().write(new Packet(PacketType.ECHO_RESPONSE, packet.payload).toBytes());
				break;
				
			case ECHO_NF:
				getSession().writenf(new Packet(PacketType.ECHO_RESPONSE, packet.payload).toBytes());
				break;
			
			case GET_THREAD:
				getSession().write(new Packet(PacketType.GET_THREAD_RESPONSE, Thread.currentThread().getName()).toBytes());
				break;
				
			case CLOSE:
				getSession().close();
				break;
				
			case QUICK_CLOSE:
				getSession().quickClose();
				break;
				
			case WRITE_AND_CLOSE:
				getSession().write(new Packet(PacketType.WRITE_AND_CLOSE_RESPONSE, packet.payload).toBytes());
				getSession().close();
				getSession().write(new Packet(PacketType.NOP).toBytes());
				break;
				
			case WRITE_AND_QUICK_CLOSE:
				getSession().write(new Packet(PacketType.NOP).toBytes());
				getSession().quickClose();
				break;
				
			case WRITE_AND_WAIT:
				long i = Long.parseLong(packet.payload);

				getSession().write(new Packet(PacketType.WRITE_AND_WAIT_RESPONSE, packet.payload).toBytes());
				try {
					Thread.sleep(i);
				} catch (InterruptedException e) {
				}
				break;
				
			case SUSPEND_WRITE_CLOSE:
				if (packet.payload.indexOf('S') != -1) {
					getSession().suspendWrite();
				}
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
			
			case IN_LOOP:
				getSession().write(new Packet(PacketType.IN_LOOP_RESPONSE, Boolean.toString(loop.inLoop())).toBytes());
				break;				
			
			case DEADLOCK:
				String result = "NO";
				try {
					getSession().getEndFuture().await(1000);
				}
				catch (BlockingFutureOperationException e) {
					result = "YES";
				}
				catch (Exception e) {
				}
				getSession().write(new Packet(PacketType.DEADLOCK_RESPONSE, result).toBytes());
				break;
				
			default:
				break;
			}
			LockUtils.notify(dataReadLock);
			
			if (throwInRead) {
				throw new NullPointerException();
			}
		}

		private boolean event(EventType type, long length) {
			record(eventMapping.get(type));
			if (recordDataEventDetails && length != -1) {
				record(""+length);
			}
			switch (type) {
			case SESSION_CREATED:
				session = (StreamSession) getSession();
				break;
				
			case SESSION_OPENED:
				LockUtils.notify(sessionOpenLock);
				sessionLocal = session.getLocalAddress();
				sessionRemote = session.getRemoteAddress();
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
			
			if (Server.this.throwInEvent) {
				throwInEventCount.incrementAndGet();
				throw new NullPointerException();
			}
			return false;
		}

		@Override
		public void event(SessionEvent event) {
			event(event.type(), -1);
		}

		@Override
		public void event(DataEvent event, long length) {
			event(event.type(), length);
		}

		@Override
		public void exception(Throwable t) {
			EventType type = EventType.EXCEPTION_CAUGHT;

			if (!dontReplaceException) {
				Server remote = findRemote();

				if (t instanceof IOException) {
					if (remote != null && remote.getSession() != null) {
						SelectableChannel ch = remote.getSession().channel;

						if (!ch.isOpen()) {
							type = EventType.DATA_SENT;
							System.out.println("[INFO] EXCEPTION_CAUGHT event replaced by DATA_SENT: " + t);
						}
					}
				}
			}
			event(type, -1);
			if (exceptionRecordException) {
				record("(" + t.getMessage() + ")");
			}
			if (Server.this.throwInException) {
				Server.this.throwInExceptionCount.incrementAndGet();
				throw new IllegalArgumentException();
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
			
			if (Server.this.throwInIncident) {
				throw new IllegalArgumentException();
			}
			return Server.this.incident;
		}
		
		@Override
		public void timer(Object event) {
			record("TIM;" + event);
			
			if (throwInTimer) {
				throwInTimerCount.incrementAndGet();
				throw new IllegalArgumentException();
			}
		}
		
		@Override
		public void timer(Runnable task) {
			record("TIM;" + task);
			
			if (throwInTimer) {
				throwInTimerCount.incrementAndGet();
				throw new IllegalArgumentException();
			}
		}
		
	}
}
