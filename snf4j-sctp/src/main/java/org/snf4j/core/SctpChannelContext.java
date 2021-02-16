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

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

import org.snf4j.core.SctpSession.SctpRecord;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.SctpNotificationType;

import com.sun.nio.sctp.AbstractNotificationHandler;
import com.sun.nio.sctp.AssociationChangeNotification;
import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.Notification;
import com.sun.nio.sctp.PeerAddressChangeNotification;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SendFailedNotification;
import com.sun.nio.sctp.ShutdownNotification;

class SctpChannelContext extends SessionChannelContext<SctpSession> {

	SctpChannelContext(SctpSession session) {
		super(session);
	}
	
	@Override
	public boolean finishConnect(SelectableChannel channel) throws Exception {
		return ((SctpChannel)channel).finishConnect();
	}
	
	@Override
	final boolean completeRegistration(SelectorLoop loop, SelectionKey key, SelectableChannel channel) throws Exception {
		SctpChannel sc = (SctpChannel) channel;
		boolean open = sc.isOpen();
		
		if (open) {
			if (!sc.isConnectionPending()) {
				try {
					if (!sc.getRemoteAddresses().isEmpty()) {
						key.interestOps(SelectionKey.OP_READ);
						return true;
					}
				}
				catch (ClosedChannelException e) {
					open = false;
				}
			}
		}
			
		if (open) {
			key.interestOps(SelectionKey.OP_CONNECT);
		}
		else {
			//If the channel is closed notify session
			loop.fireCreatedEvent(getSession(), channel);
			loop.fireEndingEvent(getSession(), false);
		}			
		return false;
	}	
	
	@Override
	ChannelContext<SctpSession> wrap(InternalSession session) {
		return new SctpChannelContext((SctpSession) session);
	}

	@Override
	final void handle(final SelectorLoop loop, final SelectionKey key) {	
		boolean doWrite = false;
		
		if (key.isReadable()) {
			handleReading(loop, context, key);
			doWrite = key.isValid() && ((key.interestOps() & SelectionKey.OP_WRITE) != 0);
		}
		else if (key.isWritable()) {
			doWrite = key.isWritable();
		}
		else if (key.isConnectable()) {
			loop.handleConnecting(context, key);
		}
		if (doWrite) {
			int spinCount = context.maxWriteSpinCount;

			do {
				spinCount = handleWriting(loop, context, key, spinCount);
			} while (spinCount > 0 && key.isValid() && ((key.interestOps() & SelectionKey.OP_WRITE) != 0));
		}
	}	
	
	final private static AbstractNotificationHandler<SctpSession> HANDLER = new AbstractNotificationHandler<SctpSession>() {
		
		@Override
		public HandlerResult handleNotification(PeerAddressChangeNotification notification, SctpSession session) {
			return session.notification(notification, SctpNotificationType.PEER_ADDRESS_CHANGE);
		}
		
		@Override
		public HandlerResult handleNotification(AssociationChangeNotification notification, SctpSession session) {
			if (notification.event() == AssociationChangeNotification.AssocChangeEvent.SHUTDOWN) {
				session.markShutdown();
			}
			return session.notification(notification, SctpNotificationType.ASSOCIATION_CHANGE);
		}
		
		@Override
		public HandlerResult handleNotification(Notification notification, SctpSession session) {
			return session.notification(notification, SctpNotificationType.GENERIC);
		}
		
		@Override
		public HandlerResult handleNotification(SendFailedNotification notification, SctpSession session) {
			return session.notification(notification, SctpNotificationType.SEND_FAILED);
		}
		
		@Override
		public HandlerResult handleNotification(ShutdownNotification notification, SctpSession session) {
			return session.notification(notification, SctpNotificationType.SHUTDOWN);
		}
	};
	
	private final void handleReading(final SelectorLoop loop, final SctpSession session, final SelectionKey key) {
		boolean traceEnabled = loop.traceEnabled;
		boolean consumed = false;
		MessageInfo minfo;
		
		if (traceEnabled) {
			loop.logger.trace("Reading from channel in {}", session);
		}
		
		try {
			minfo = ((SctpChannel)key.channel()).receive(session.getInBuffer(), session, HANDLER);
		} catch (IOException e) {
			loop.elogWarnOrError(loop.logger, "Reading from channel in {} failed: {}", session, e);
			loop.fireException(session, e);
			minfo = null;
		}
		
		if (minfo != null) {
			int bytes = minfo.bytes();
			
			if (bytes > 0) {
				long currentTime = System.currentTimeMillis();

				if (traceEnabled) {
					loop.logger.trace("{} byte(s) read from channel in {}", bytes, session);
				}
				session.calculateThroughput(currentTime, false);
				session.incReadBytes(bytes, currentTime);
				loop.fireEvent(session, DataEvent.RECEIVED, bytes);
				session.consumeInBuffer(minfo);
				consumed = true;
			}
			else if (bytes < 0) {
				if (loop.debugEnabled) {
					loop.logger.debug("Channel in {} reached end-of-stream", session);
				}
			}
		}	

		if (!consumed) {
			session.consumeInBufferAfterNoRead();
		}
		
		if (session.markedShutdown()) {
			if (loop.debugEnabled) {
				loop.logger.debug("Closing channel in {} after shutdown", session);
			}
			session.close(true);
		}
	}
	
	private final int handleWriting(final SelectorLoop loop, final SctpSession session, final SelectionKey key, int spinCount) {
		boolean traceEnabled = loop.traceEnabled;
		Exception exception = null;
		long totalBytes = 0;
		int bytes;
		
		if (traceEnabled) {
			loop.logger.trace("Writting to channel in {}", session);
		}
		
		Queue<SctpRecord> outQueue = session.getOutQueue();
		SctpRecord record;
		SctpChannel channel = (SctpChannel) key.channel();
		
		try {
			while (spinCount > 0 && (record = outQueue.peek()) != null) {
				long length = record.buffer.remaining();
				
				bytes = channel.send(record.buffer, record.msgInfo.unwrap());
				
				if (bytes == length) {
					if (traceEnabled) {
						loop.logger.trace("{} byte(s) written to channel in {}", bytes, session);
					}
					outQueue.poll();
					totalBytes += bytes;
					--spinCount;
					if (record.release) {
						session.release(record.buffer);
					}					
				}
				else {
					spinCount = 0;
					break;
				}
			}
			
			synchronized (session.getWriteLock()) {
				if (totalBytes > 0) {
					long currentTime = System.currentTimeMillis();
					
					session.calculateThroughput(currentTime, false);
					session.incWrittenBytes(totalBytes, currentTime);
					session.consumedBytes(totalBytes);
				}
				if (outQueue.isEmpty()) {
					session.clearWriteInterestOps(key);
					session.handleClosingInProgress();
				}
			}
		}
		catch (Exception e) {
			exception = e;
		}
		
		if (totalBytes > 0) {
			loop.fireEvent(session, DataEvent.SENT, totalBytes);
		}
		
		if (exception != null) {
			loop.elogWarnOrError(loop.logger, "Writting to chennel in {} failed: {}", session, exception);
			loop.fireException(session, exception);
			spinCount = 0;
		}
		return spinCount;
	}
	
	@Override
	final void shutdown(SelectableChannel channel) throws Exception {
		((SctpChannel)channel).shutdown();
	}
	
	private static boolean append(StringBuilder sb, Set<SocketAddress> addrs) {
		Iterator<SocketAddress> i = addrs.iterator();
		
		if (i.hasNext()) {
			sb.append(i.next());
			while (i.hasNext()) {
				sb.append(',');
				sb.append(i.next());
			}
			return true;
		}
		return false;
	}
	
	static String toString(SctpChannel channel) {
		StringBuilder sb = new StringBuilder(100);
		Set<SocketAddress> addrs;
		
		sb.append(channel.getClass().getName());
		sb.append('[');
		try {
			addrs = channel.getRemoteAddresses();
			if (!addrs.isEmpty()) {
				sb.append("connected ");
			}
			else if (channel.isConnectionPending()) {
				sb.append("connection-pending ");
			}
			else {
				sb.append("not-connected ");
			}
		} catch (IOException e) {
			addrs = null;
		}
		sb.append("local=");
		try {
			if (!append(sb, channel.getAllLocalAddresses())) {
				sb.append("not-bound");
			}
		} catch (IOException e) {
			sb.append("unknown");
		}
		if (addrs != null) {
			if (!addrs.isEmpty()) {
				sb.append(" remote=");
				append(sb, addrs);
			}
		}
		else {
			sb.append(" remote=unknown");
		}
		sb.append(']');
		return sb.toString();
	}
	
	@Override
	final String toString(SelectableChannel channel) {
		if (channel instanceof SctpChannel) {
			return toString((SctpChannel) channel);
		}
		return super.toString(channel);
	}

	@Override
	public final boolean exceptionOnDecodingFailure() {
		return true;
	}
}
