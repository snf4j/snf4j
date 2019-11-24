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

import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
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

public class Server {
	
	public SelectorLoop loop;
	public int port;
	public boolean ssl;
	public StreamSession session;
	public StreamSession initSession;
	public ThreadFactory threadFactory;
	public ISelectorLoopController controller;
	public long throughputCalcInterval = 1000;
	public boolean directAllocator;
	public TestAllocator allocator;
	public DefaultCodecExecutor codecPipeline;
	public ISelectorLoopPool pool;
	public volatile EndingAction endingAction = EndingAction.DEFAULT;
	public volatile StringBuilder serverSocketLogs = new StringBuilder();
	public volatile ServerSocketChannel ssc;
	public volatile ServerSocketChannel registeredSsc;
	public volatile ServerSocketChannel closedSsc;
	public volatile boolean useTestSession;
	public volatile boolean recordSessionId;
	public volatile boolean waitForCloseMessage;

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
	public final AtomicInteger throwInEventCount = new AtomicInteger();
	public final AtomicInteger throwInExceptionCount = new AtomicInteger();
	
	AtomicBoolean sessionOpenLock = new AtomicBoolean(false);
	AtomicBoolean sessionReadyLock = new AtomicBoolean(false);
	AtomicBoolean sessionEndingLock = new AtomicBoolean(false);
	AtomicBoolean dataReceivedLock = new AtomicBoolean(false);
	AtomicBoolean dataReadLock = new AtomicBoolean(false);
	AtomicBoolean dataSentLock = new AtomicBoolean(false);

	StringBuilder recorder = new StringBuilder();
	
	static Map<EventType, String> eventMapping = new HashMap<EventType, String>();

	static volatile SSLContext sslContext = null; 
	
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
	}

	public Server(int port, boolean ssl) {
		this.port = port;
		this.ssl = ssl;
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
				return new SSLSession(createHandler(channel), false);
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
				public SSLEngine createSSLEngine(boolean clientMode) throws SSLEngineCreateException {
					SSLEngine engine;
					try {
						engine = getSSLContext().createSSLEngine();
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
				public ICodecExecutor createCodecExecutor() {
					return codecPipeline;
				}
			};
			
			config.setMinInBufferCapacity(minInBufferCapacity);
			config.setMinOutBufferCapacity(minOutBufferCapacity);
			config.setThroughputCalculationInterval(throughputCalcInterval);
			config.setEndingAction(endingAction);
			config.setMaxSSLApplicationBufferSizeRatio(1);
			config.setMaxSSLNetworkBufferSizeRatio(1);
			config.setWaitForInboundCloseMessage(waitForCloseMessage);
			return config;
		}

		@Override
		public ISessionStructureFactory getFactory() {
			return new SessionStructureFactory();
		}

		@Override
		public int available(ByteBuffer buffer, boolean flipped) {
			++availableCounter;
			if (!directAllocator) throw new IllegalStateException();

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
			if (directAllocator) throw new IllegalStateException();
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

		private boolean event(EventType type) {
			record(eventMapping.get(type));
			switch (type) {
			case SESSION_CREATED:
				session = (StreamSession) getSession();
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
			
			if (Server.this.throwInEvent) {
				throwInEventCount.incrementAndGet();
				throw new NullPointerException();
			}
			return false;
		}

		@Override
		public void event(SessionEvent event) {
			event(event.type());
		}

		@Override
		public void event(DataEvent event, long length) {
			event(event.type());
		}

		@Override
		public void exception(Throwable t) {
			event(EventType.EXCEPTION_CAUGHT);
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
		
	}
}
