package org.snf4j.core;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.snf4j.core.allocator.DefaultAllocator;
import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.allocator.TestAllocator;
import org.snf4j.core.codec.DefaultCodecExecutor;
import org.snf4j.core.codec.ICodecExecutor;
import org.snf4j.core.factory.AbstractSctpSessionFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.AbstractSctpHandler;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.ISctpHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.DefaultSctpSessionConfig;
import org.snf4j.core.session.ISctpSessionConfig;
import org.snf4j.core.timer.ITimeoutModel;
import org.snf4j.core.timer.ITimer;

import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

public class SctpServer {
	
	SelectorLoop loop;	
	
	int port;
	
	SctpServerChannel ssc;
	
	SctpSession session;
	
	AtomicBoolean sessionOpenLock = new AtomicBoolean(false);
	
	AtomicBoolean sessionReadyLock = new AtomicBoolean(false);
	
	AtomicBoolean sessionEndingLock = new AtomicBoolean(false);
	
	AtomicBoolean dataReceivedLock = new AtomicBoolean(false);
	
	AtomicBoolean dataReadLock = new AtomicBoolean(false);
	
	AtomicBoolean dataSentLock = new AtomicBoolean(false);
	
	StringBuilder trace = new StringBuilder();
	
	public volatile boolean traceMsgInfo = true;
	
	public volatile boolean traceFullMsgInfo;
	
	public volatile boolean traceDataLength;
	
	public volatile boolean traceSessionFactory;
	
	public volatile int maxWriteSpinCount = -1;
	
	public volatile boolean optimizeCopying;
	
	public volatile long throughputCalculationInterval;
	
	public volatile int minInBufferCapacity = -1;
	
	public volatile int maxInBufferCapacity = -1;
	
	public volatile int defaultSctpStreamNumber = -1;
	
	public volatile int defaultSctpPayloadProtocolID = -1;
	
	public volatile int minSctpStreamNumber = -111;
	
	public volatile int maxSctpStreamNumber = -111;
	
	public volatile int minSctpPayloadProtocolID = -111;
	
	public volatile int maxSctpPayloadProtocolID = -111;
	
	public volatile boolean defaultSctpUnorderedFlag;
	
	EventType closeInEvent;
	
	StoppingType closeType = StoppingType.GENTLE;
	
	EventType writeInEvent;
	
	Packet packetToWriteInEvent;
	
	TestAllocator allocator;
	
	ByteBuffer readBuffer;
	
	DefaultCodecExecutor codecExecutor;

	DefaultCodecExecutor[][] codecExecutors = new DefaultCodecExecutor[10][10];
	
	SctpServer(int port) {
		this.port = port;
	}
	
	void addCodec(int streamNum, int protoID, DefaultCodecExecutor executor) {
		codecExecutors[streamNum][protoID] = executor;
	}
	
	DefaultCodecExecutor getCodec(int streamNum, int protoID) {
		return codecExecutors[streamNum][protoID];
	}
	
	void trace(String data) {
		synchronized(trace) {
			trace.append(data);
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
	
	public IFuture<Void> start() throws Exception {
		if (loop == null) {
			loop = new SelectorLoop();
			loop.start();
		}
		
		ssc = SctpServerChannel.open();
		ssc.configureBlocking(false);
		ssc.bind(new InetSocketAddress(port));
		return Sctp.register(loop, ssc, new SessionFactory());
	}
	
	public void stop(long millis) throws Exception {
		if (loop != null) {
			loop.stop();
			loop.join(millis);
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

	class SessionFactory extends AbstractSctpSessionFactory {

		@Override
		protected ISctpHandler createHandler(SctpChannel channel) {
			return new Handler();
		}
		
		@Override
		public void registered(SctpServerChannel channel) {
			if (traceSessionFactory) trace("REGISTERED");
		}

		@Override
		public void closed(SctpServerChannel channel) {
			if (traceSessionFactory) trace("CLOSED");
		}

		@Override
		public void exception(SctpServerChannel channel, Throwable exception) {
			if (traceSessionFactory) trace("EXCEPTION(" + exception.getMessage() + ")");
		}
		
	}
	class SessionStructureFactory implements ISessionStructureFactory {

		@Override
		public IByteBufferAllocator getAllocator() {
			if (allocator != null) {
				return allocator;
			}
			return DefaultAllocator.DEFAULT;
		}

		@Override
		public ConcurrentMap<Object, Object> getAttributes() {
			return null;
		}

		@Override
		public Executor getExecutor() {
			return null;
		}

		@Override
		public ITimer getTimer() {
			return null;
		}

		@Override
		public ITimeoutModel getTimeoutModel() {
			return null;
		}
		
	}
	
	class Handler extends AbstractSctpHandler {

		void trace(String s) {
			SctpServer.this.trace(s);
		}
		
		@Override
		public ISctpSessionConfig getConfig() {
			DefaultSctpSessionConfig config = new DefaultSctpSessionConfig() {
				
				@Override
				public ICodecExecutor createCodecExecutor() {
					return codecExecutor;
				}
				
				@Override
				public ICodecExecutor createCodecExecutor(MessageInfo msgInfo) {
					return codecExecutors[msgInfo.streamNumber()][msgInfo.payloadProtocolID()];
				}
				
			};
			
			config.setOptimizeDataCopying(optimizeCopying);
			config.setThroughputCalculationInterval(throughputCalculationInterval);
			if (maxWriteSpinCount != -1) {
				config.setMaxWriteSpinCount(maxWriteSpinCount);
			}
			if (minInBufferCapacity != -1) {
				config.setMinInBufferCapacity(minInBufferCapacity);
			}
			if (maxInBufferCapacity != -1) {
				config.setMaxInBufferCapacity(maxInBufferCapacity);
			}
			if (defaultSctpStreamNumber != -1) {
				config.setDefaultSctpStreamNumber(defaultSctpStreamNumber);
			}
			if (defaultSctpPayloadProtocolID != -1) {
				config.setDefaultSctpPayloadProtocolID(defaultSctpPayloadProtocolID);
			}
			if (defaultSctpUnorderedFlag) {
				config.setDefaultSctpUnorderedFlag(defaultSctpUnorderedFlag);
			}
			if (minSctpStreamNumber != -111) {
				config.setMinSctpStreamNumber(minSctpStreamNumber);
			}
			if (maxSctpStreamNumber != -111) {
				config.setMaxSctpStreamNumber(maxSctpStreamNumber);
			}
			if (minSctpPayloadProtocolID != -111) {
				config.setMinSctpPayloadProtocolID(minSctpPayloadProtocolID);
			}
			if (maxSctpPayloadProtocolID != -111) {
				config.setMaxSctpPayloadProtocolID(maxSctpPayloadProtocolID);
			}
			return config;
		}
		
		ImmutableSctpMessageInfo respMsgInfo(MessageInfo msgInfo) {
			return ImmutableSctpMessageInfo.create(msgInfo.streamNumber());
		}
		
		String toString(MessageInfo msgInfo) {
			StringBuilder sb = new StringBuilder();
			int i;
			long l;
			boolean b;
			
			i = msgInfo.streamNumber();
			if (i != 0) {
				sb.append(i);
			}
			i = msgInfo.payloadProtocolID();
			if (i != 0) {
				sb.append("p");
				sb.append(i);
			}
			b = msgInfo.isComplete();
			if (!b) {
				sb.append("!c");
			}
			b = msgInfo.isUnordered();
			if (b) {
				sb.append("u");
			}
			l = msgInfo.timeToLive();
			if (l != 0) {
				sb.append("t");
				sb.append(l);
			}
			if (traceFullMsgInfo) {
				Association a = msgInfo.association();
				
				if (a != null) {
					sb.append("a");
					sb.append(a.associationID());
				}
				sb.append(msgInfo.address());
			}
			return sb.length() > 0 ? "[" + sb.toString() + "]" : "";
		}
		
		@Override
		public ISessionStructureFactory getFactory() {
			return new SessionStructureFactory();
		}
		
		@Override
		public void read(Object msg, MessageInfo msgInfo) {
			if (msg instanceof ByteBuffer) {
				ByteBuffer bb = (ByteBuffer)msg;
				byte[] b = new byte[bb.remaining()];
				
				bb.get(b);
				readBuffer = bb;
				trace("BUF");
				read(b, msgInfo);
				return;				
			}
			trace("M(" + msg.toString() + ")");
			LockUtils.notify(dataReadLock);
		}

		@SuppressWarnings("incomplete-switch")
		@Override
		public void read(byte[] msg, MessageInfo msgInfo) {
			Packet packet = Packet.fromBytes(msg);
			String info = "";
			
			if (traceMsgInfo) {
				info = toString(msgInfo);
			}
			
			trace(packet.type + "(" + packet.payload + ")" + info);
			
			switch (packet.type) {
			case ECHO:
				getSession().writenf(new Packet(PacketType.ECHO_RESPONSE).toBytes(), respMsgInfo(msgInfo));
				break;
				
			case WRITE_AND_QUICK_CLOSE:
				getSession().writenf(new Packet(PacketType.NOP).toBytes(), respMsgInfo(msgInfo));
				getSession().quickClose();
				break;
				
			case WRITE_AND_CLOSE:
				getSession().writenf(new Packet(PacketType.WRITE_AND_CLOSE_RESPONSE, packet.payload).toBytes(),
						respMsgInfo(msgInfo));
				getSession().close();
				getSession().writenf(new Packet(PacketType.NOP).toBytes(), respMsgInfo(msgInfo));
				break;

			}
			
			LockUtils.notify(dataReadLock);
		}
		
		private void event(EventType type, long length) {
			if (length != -1 && traceDataLength) {
				trace(Server.eventMapping.get(type) + "(" + length + ")");
			}
			else {
				trace(Server.eventMapping.get(type));
			}
			
			switch (type) {
			case SESSION_CREATED:
				session = (SctpSession) getSession();
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
			
			if (writeInEvent == type && packetToWriteInEvent != null) {
				getSession().writenf(packetToWriteInEvent.toBytes(), ImmutableSctpMessageInfo.create(0));
				packetToWriteInEvent = null;
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
			
			event(type, -1);
		}
		
	}
}
