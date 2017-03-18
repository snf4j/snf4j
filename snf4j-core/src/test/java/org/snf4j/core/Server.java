/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017 SNF4J contributors
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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import org.snf4j.core.allocator.DefaultAllocator;
import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.factory.AbstractSessionFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.pool.ISelectorLoopPool;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISessionConfig;

public class Server {
	
	public SelectorLoop loop;
	public int port;
	public StreamSession session;
	public ThreadFactory threadFactory;
	public ISelectorLoopController controller;
	public long throughputCalcInterval = 1000;
	public boolean directAllocator;
	public ISelectorLoopPool pool;
	public volatile boolean exceptionResult;
	public volatile EndingAction endingAction = EndingAction.DEFAULT;
	public volatile StringBuilder serverSocketLogs = new StringBuilder();
	public volatile ServerSocketChannel ssc;
	public volatile ServerSocketChannel registeredSsc;
	public volatile ServerSocketChannel closedSsc;
 
	
	AtomicBoolean sessionOpenLock = new AtomicBoolean(false);
	AtomicBoolean sessionEndingLock = new AtomicBoolean(false);
	AtomicBoolean dataReceivedLock = new AtomicBoolean(false);
	AtomicBoolean dataReadLock = new AtomicBoolean(false);
	AtomicBoolean dataSentLock = new AtomicBoolean(false);

	StringBuilder recorder = new StringBuilder();
	
	static Map<EventType, String> eventMapping = new HashMap<EventType, String>();
	
	static {
		eventMapping.put(EventType.SESSION_CREATED, "SCR");
		eventMapping.put(EventType.SESSION_OPENED, "SOP");
		eventMapping.put(EventType.SESSION_CLOSED, "SCL");
		eventMapping.put(EventType.SESSION_ENDING, "SEN");
		eventMapping.put(EventType.DATA_RECEIVED, "DR");
		eventMapping.put(EventType.DATA_SENT, "DS");
		eventMapping.put(EventType.EXCEPTION_CAUGHT, "EXC");
	}
	
	public Server(int port) {
		this.port = port;
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
	
	public void start() throws IOException {
		start(false, null);
	}
	
	public void start(boolean firstRegistrate) throws IOException {
		start(firstRegistrate, null);
	}
	
	public void start(boolean firstRegistrate, SelectorLoop loop) throws IOException {
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
	
	public void write(Packet packet) {
		session.write(packet.toBytes());
	}
	
	public void quickStop(long millis) throws InterruptedException {
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
	
	void waitFor(AtomicBoolean lock, long millis) throws InterruptedException {
		synchronized (lock) {
			if (!lock.get()) {
				lock.wait(millis);
			}
			if (!lock.getAndSet(false)) {
				throw new InterruptedException();
			}
		}
	}
	
	void notify(AtomicBoolean lock) {
		synchronized(lock) {
			lock.set(true);
			lock.notify();
		}		
	}
	
	public void waitForSessionOpen(long millis) throws InterruptedException {
		waitFor(sessionOpenLock, millis);
	}

	public void waitForSessionEnding(long millis) throws InterruptedException {
		waitFor(sessionEndingLock, millis);
	}

	public void waitForDataReceived(long millis) throws InterruptedException {
		waitFor(dataReceivedLock, millis);
	}

	public void waitForDataRead(long millis) throws InterruptedException {
		waitFor(dataReadLock, millis);
	}
	
	public void waitForDataSent(long millis) throws InterruptedException {
		waitFor(dataSentLock, millis);
	}
	
	class SessionFactory extends AbstractSessionFactory {

		@Override
		public IStreamHandler createHandler(SocketChannel channel) {
			return new Handler();
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

	}
	
	class SessionStructureFactory implements ISessionStructureFactory {

		@Override
		public IByteBufferAllocator getAllocator() {
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
		
		@Override
		public ISessionConfig getConfig() {
			DefaultSessionConfig config = new DefaultSessionConfig();
			
			config.setMinInBufferCapacity(1024);
			config.setMinOutBufferCapacity(1024);
			config.setThroughputCalculationInterval(throughputCalcInterval);
			config.setEndingAction(endingAction);
			return config;
		}

		@Override
		public ISessionStructureFactory getFactory() {
			return new SessionStructureFactory();
		}

		@Override
		public int toRead(ByteBuffer buffer, boolean flipped) {
			if (!directAllocator) throw new IllegalStateException();

			ByteBuffer dupBuffer = buffer.duplicate();
			
			if (!flipped) {
				dupBuffer.flip();
			}
			
			byte[] d = new byte[dupBuffer.remaining()];
			
			dupBuffer.get(d);
			return Packet.toRead(d, 0, d.length);
		}

		@Override
		public int toRead(byte[] buffer, int off, int len) {
			if (directAllocator) throw new IllegalStateException();
			return Packet.toRead(buffer, off, len);
		}

		@Override
		public void read(byte[] data) {
			Packet packet = Packet.fromBytes(data);
			
			record(packet.type+"("+packet.payload+")");
			
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
			
			default:
				break;
			}
			Server.this.notify(dataReadLock);
		}

		private boolean event(EventType type) {
			record(eventMapping.get(type));
			switch (type) {
			case SESSION_CREATED:
				session = (StreamSession) getSession();
				break;
				
			case SESSION_OPENED:
				Server.this.notify(sessionOpenLock);
				break;
				
			case SESSION_ENDING:
				Server.this.notify(sessionEndingLock);
				break;
				
			case DATA_RECEIVED:
				Server.this.notify(dataReceivedLock);
				break;
				
			case DATA_SENT:
				Server.this.notify(dataSentLock);
				break;
				
			default:
				break;

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
		public boolean exception(Throwable t) {
			event(EventType.EXCEPTION_CAUGHT);
			return exceptionResult;
		}
		
	}
}
