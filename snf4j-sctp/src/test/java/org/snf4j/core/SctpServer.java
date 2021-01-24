package org.snf4j.core;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.snf4j.core.factory.AbstractSctpSessionFactory;
import org.snf4j.core.handler.AbstractSctpHandler;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.ISctpHandler;
import org.snf4j.core.handler.SessionEvent;

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

	SctpServer(int port) {
		this.port = port;
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
	
	public void start() throws Exception {
		if (loop == null) {
			loop = new SelectorLoop();
			loop.start();
		}
		
		ssc = SctpServerChannel.open();
		ssc.configureBlocking(false);
		ssc.bind(new InetSocketAddress(port));
		Sctp.register(loop, ssc, new SessionFactory());
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
		
	}
	
	class Handler extends AbstractSctpHandler {

		void trace(String s) {
			SctpServer.this.trace(s);
		}
		MessageInfo respMsgInfo(MessageInfo msgInfo) {
			return MessageInfo.createOutgoing(null, msgInfo.streamNumber());
		}
		
		@Override
		public void read(Object msg, MessageInfo msgInfo) {
			if (msg instanceof ByteBuffer) {
				ByteBuffer bb = (ByteBuffer)msg;
				byte[] b = new byte[bb.remaining()];
				
				bb.get(b);
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
			
			trace(packet.type + "(" + packet.payload + ")");
			
			switch (packet.type) {
			case ECHO:
				getSession().writenf(new Packet(PacketType.ECHO_RESPONSE).toBytes(), respMsgInfo(msgInfo));
				break;
			}
			
			LockUtils.notify(dataReadLock);
		}
		
		private void event(EventType type, long length) {
			trace(Server.eventMapping.get(type));
			
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
		}	
		
		@Override
		public void event(SessionEvent event) {
			event(event.type(), -1);
		}

		@Override
		public void event(DataEvent event, long length) {
			event(event.type(), length);
		}
		
	}
}
