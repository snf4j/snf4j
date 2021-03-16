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

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Queue;

import org.snf4j.core.InternalSctpSession.SctpRecord;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.SctpNotificationType;

import com.sun.nio.sctp.AbstractNotificationHandler;
import com.sun.nio.sctp.AssociationChangeNotification;
import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.Notification;
import com.sun.nio.sctp.NotificationHandler;
import com.sun.nio.sctp.PeerAddressChangeNotification;
import com.sun.nio.sctp.SendFailedNotification;
import com.sun.nio.sctp.ShutdownNotification;

abstract class AbstractSctpChannelContext<T extends InternalSctpSession> extends SessionChannelContext<T> {

	AbstractSctpChannelContext(T session) {
		super(session);
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
	
	final static AbstractNotificationHandler<InternalSctpSession> HANDLER = new AbstractNotificationHandler<InternalSctpSession>() {
		
		@Override
		public HandlerResult handleNotification(PeerAddressChangeNotification notification, InternalSctpSession session) {
			return session.notification(notification, SctpNotificationType.PEER_ADDRESS_CHANGE);
		}
		
		@Override
		public HandlerResult handleNotification(AssociationChangeNotification notification, InternalSctpSession session) {
			return session.notification(notification, SctpNotificationType.ASSOCIATION_CHANGE);
		}
		
		@Override
		public HandlerResult handleNotification(Notification notification, InternalSctpSession session) {
			return session.notification(notification, SctpNotificationType.GENERIC);
		}
		
		@Override
		public HandlerResult handleNotification(SendFailedNotification notification, InternalSctpSession session) {
			return session.notification(notification, SctpNotificationType.SEND_FAILED);
		}
		
		@Override
		public HandlerResult handleNotification(ShutdownNotification notification, InternalSctpSession session) {
			return session.notification(notification, SctpNotificationType.SHUTDOWN);
		}
	};
	
	abstract MessageInfo receive(SelectionKey key, ByteBuffer msg, final T session, NotificationHandler<InternalSctpSession> handler) throws Exception;
	
	void handleReading(final SelectorLoop loop, final T session, final SelectionKey key) {
		boolean traceEnabled = loop.traceEnabled;
		boolean consumed = false;
		MessageInfo minfo;
		
		if (traceEnabled) {
			loop.logger.trace("Reading from channel in {}", session);
		}
		
		try {
			minfo = receive(key, session.getInBuffer(), session, HANDLER);
		} catch (Throwable e) {
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
		
		if (session.closeNow()) {
			if (loop.debugEnabled) {
				loop.logger.debug("Closing channel in {} after shutdown", session);
			}
			session.close(true);
		}
	}
	
	abstract int send(SelectionKey key, ByteBuffer msg, MessageInfo msgInfo) throws Exception;
	
	int send(T session, SelectionKey key, SctpRecord record) throws Exception {
		return send(key, record.buffer, record.msgInfo.unwrap());
	}
	
	int handleWriting(final SelectorLoop loop, final T session, final SelectionKey key, int spinCount) {
		boolean traceEnabled = loop.traceEnabled;
		Throwable exception = null;
		long totalBytes = 0;
		long consumedBytes = 0;
		int bytes;
		
		if (traceEnabled) {
			loop.logger.trace("Writting to channel in {}", session);
		}
		
		Queue<SctpRecord> outQueue = session.getOutQueue();
		SctpRecord record;
		
		try {
			while (spinCount > 0 && (record = outQueue.peek()) != null) {
				int length = record.buffer.remaining();
				
				bytes = send(session, key, record);
				
				if (bytes == length) {
					if (traceEnabled) {
						loop.logger.trace("{} byte(s) written to channel in {}", bytes, session);
					}
					outQueue.poll();
					totalBytes += length;
					consumedBytes += length;
					--spinCount;
					if (record.release) {
						session.release(record.buffer);
					}					
				}
				else if (bytes == -length) {
					outQueue.poll();
					consumedBytes += length;
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
					session.consumedBytes(consumedBytes);
				}
				else if (consumedBytes > 0) {
					session.consumedBytes(consumedBytes);
				}
				if (outQueue.isEmpty()) {
					if (key.isValid()) {
						session.clearWriteInterestOps(key);
					}
					session.handleClosingInProgress();
				}
			}
		}
		catch (Throwable e) {
			exception = e;
		}
		
		if (totalBytes > 0) {
			loop.fireEvent(session, DataEvent.SENT, totalBytes);
		}
		if (consumedBytes > totalBytes) {
			session.futuresController.event(DataEvent.SENT, consumedBytes - totalBytes);
		}
		
		if (exception != null) {
			loop.elogWarnOrError(loop.logger, "Writting to channel in {} failed: {}", session, exception);
			loop.fireException(session, exception);
			spinCount = 0;
		}
		return spinCount;
	}
	
	@Override
	public final boolean exceptionOnDecodingFailure() {
		return true;
	}
}