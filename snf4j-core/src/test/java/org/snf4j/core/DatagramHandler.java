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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

public class DatagramHandler {
	SelectorLoop loop;
	int port;
	volatile DatagramSession session;
	ThreadFactory threadFactory;
	boolean registerConnectedSession;
	long throughputCalcInterval = 1000;
	boolean ignorePossiblyIncomplete = true;
	volatile EndingAction endingAction = EndingAction.DEFAULT;
	volatile boolean createNullHandler;
	boolean directAllocator;
	TestAllocator allocator;
	boolean canOwnPasseData;
	volatile boolean useTestSession;
	DefaultCodecExecutor codecPipeline;
	volatile boolean incident;
	volatile boolean incidentRecordException;
	volatile boolean incidentClose;
	volatile boolean incidentQuickClose;
	volatile boolean incidentDirtyClose;
	volatile boolean exceptionClose;

	public volatile boolean throwInEvent;
	public final AtomicInteger throwInEventCount = new AtomicInteger();
	public volatile boolean throwInRead;
	public final AtomicInteger throwInReadCount = new AtomicInteger();
	public volatile boolean throwInSuperRead;
	public final AtomicInteger throwInSuperReadCount = new AtomicInteger();
	
	AtomicBoolean sessionOpenLock = new AtomicBoolean(false);
	AtomicBoolean sessionReadyLock = new AtomicBoolean(false);
	AtomicBoolean sessionEndingLock = new AtomicBoolean(false);
	AtomicBoolean dataReceivedLock = new AtomicBoolean(false);
	AtomicBoolean dataReadLock = new AtomicBoolean(false);
	AtomicBoolean dataSentLock = new AtomicBoolean(false);

	StringBuilder recorder = new StringBuilder();
	
	boolean recordDataEventDetails;
	
	DatagramSession initSession;

	static Map<EventType, String> eventMapping = new HashMap<EventType, String>();

	IDatagramHandlerFactory factory;
	boolean useDatagramServerHandler;
	
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

	public void startServer() throws IOException {
		start(false);
	}
	
	public void startClient() throws IOException {
		start(true);
	}
	
	void start(boolean connected) throws IOException {
		start(connected, false, null);
	}
	
	void start(boolean connected, SelectorLoop loop) throws IOException {
		start(connected, false, loop);
	}
	
	void start(boolean connected, boolean firstRegistrate, SelectorLoop loop) throws IOException {
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
				handler = new ServerHandler(factory);
			}
			else {
				handler = new ServerHandler(new IDatagramHandlerFactory() {
					
					@Override
					public IDatagramHandler create(
							SocketAddress remoteAddress) {
						return createNullHandler ? null : new Handler();
					}
				},
				new Handler().getConfig());
			}
		}
		else {
			handler = new Handler();
		}
		
		DatagramChannel dc = DatagramChannel.open();
		dc.configureBlocking(false);
		if (connected) {
			dc.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port));
			
			if (registerConnectedSession) {
				session = useTestSession ? new TestDatagramSession(new Handler()) 
						: new DatagramSession(new Handler());
				session.setChannel(dc);
				session.preCreated();
				session.event(SessionEvent.CREATED);
				loop.register(dc, session);
			}
			else {
				if (initSession == null) {
					if (useTestSession) {
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
			dc.socket().bind(new InetSocketAddress(port));
			if (registerConnectedSession) {
				session = useTestSession ? new TestDatagramSession(new Handler()) 
						: new DatagramSession(new Handler());
				session.setChannel(dc);
				session.preCreated();
				session.event(SessionEvent.CREATED);
				loop.register(dc, session);
			}
			else {
				session = (DatagramSession) loop.register(dc, useTestSession ? new TestDatagramSession(handler) 
						: new DatagramSession(handler)).getSession();
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
	
	class Handler extends AbstractDatagramHandler {

		@Override
		public ISessionConfig getConfig() {
			DefaultSessionConfig config = new DefaultSessionConfig() {
				@Override
				public ICodecExecutor createCodecExecutor() {
					return codecPipeline;
				}
			};
			
			config.setMinInBufferCapacity(1024);
			config.setMinOutBufferCapacity(1024);
			config.setThroughputCalculationInterval(throughputCalcInterval);
			config.setIgnorePossiblyIncompleteDatagrams(ignorePossiblyIncomplete);
			config.setEndingAction(endingAction);
			config.setCanOwnDataPassedToWriteAndSendMethods(canOwnPasseData);
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
			
			return DatagramHandler.this.incident;
		}
		
	}
}
