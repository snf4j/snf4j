package org.snf4j.core;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Queue;

import org.snf4j.core.SctpSession.SctpRecord;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.ISctpHandler;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

public class SctpChannelContext extends SessionChannelContext<SctpSession> {

	SctpChannelContext(SctpSession session) {
		super(session);
	}
	
	@Override
	public boolean finishConnect(SelectableChannel channel) throws Exception {
		return ((SctpChannel)channel).finishConnect();
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
		else {
			doWrite = key.isWritable();
		}
		if (doWrite) {
			int spinCount = context.maxWriteSpinCount;

			do {
				spinCount = handleWriting(loop, context, key, spinCount);
			} while (spinCount > 0 && key.isValid() && ((key.interestOps() & SelectionKey.OP_WRITE) != 0));
		}
	}	
	
	private final void handleReading(final SelectorLoop loop, final SctpSession session, final SelectionKey key) {
		boolean traceEnabled = loop.traceEnabled;
		MessageInfo minfo;
		
		if (traceEnabled) {
			loop.logger.trace("Reading from channel in {}", session);
		}
		
		try {
			ISctpHandler handler = session.getHandler();
			minfo = ((SctpChannel)key.channel()).receive(session.getInBuffer(), 
					handler.getNotificationAttachment(), 
					handler.getNotificationHandler());
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
			}
			else if (bytes < 0) {
				if (loop.debugEnabled) {
					loop.logger.debug("Closing channel in {} after reaching end-of-stream", session);
				}
				session.close(true);
			}
			else {
				session.consumeInBufferAfterNoRead();
			}
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
				
				bytes = channel.send(record.buffer, record.msgInfo);
				
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
}
