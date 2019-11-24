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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import org.snf4j.core.allocator.DefaultAllocator;
import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.allocator.TestAllocator;
import org.snf4j.core.codec.DefaultCodecExecutor;
import org.snf4j.core.codec.ICodecExecutor;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.AbstractDatagramHandler;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISessionConfig;

public class DatagramHandler {
	SelectorLoop loop;
	int port;
	DatagramSession session;
	ThreadFactory threadFactory;
	boolean registerConnectedSession;
	long throughputCalcInterval = 1000;
	boolean ignorePossiblyIncomplete = true;
	volatile EndingAction endingAction = EndingAction.DEFAULT;
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
	
	AtomicBoolean sessionOpenLock = new AtomicBoolean(false);
	AtomicBoolean sessionReadyLock = new AtomicBoolean(false);
	AtomicBoolean sessionEndingLock = new AtomicBoolean(false);
	AtomicBoolean dataReceivedLock = new AtomicBoolean(false);
	AtomicBoolean dataReadLock = new AtomicBoolean(false);
	AtomicBoolean dataSentLock = new AtomicBoolean(false);

	StringBuilder recorder = new StringBuilder();
	
	DatagramSession initSession;

	static Map<EventType, String> eventMapping = new HashMap<EventType, String>();
	
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
						loop.register(dc, new TestDatagramSession(new Handler()));
					}
					else {
						loop.register(dc, new Handler());
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
				loop.register(dc, useTestSession ? new TestDatagramSession(new Handler()) 
						: new DatagramSession(new Handler()));
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
		}
		
		private boolean event(EventType type) {
			record(eventMapping.get(type));
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
