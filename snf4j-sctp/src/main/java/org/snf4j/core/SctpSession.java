package org.snf4j.core;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.snf4j.core.codec.ICodecExecutor;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.ISctpHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.core.session.ISctpSession;
import org.snf4j.core.session.IllegalSessionStateException;
import org.snf4j.core.session.SessionState;

import com.sun.nio.sctp.MessageInfo;

public class SctpSession extends InternalSession implements ISctpSession {
	
	private final static ILogger LOGGER = LoggerFactory.getLogger(SctpSession.class);

	private final SctpFragments fragments;
	
	private ByteBuffer inBuffer;
	
	private int inBufferCapacity;
	
	private ConcurrentLinkedQueue<SctpRecord> outQueue;
	
	private long outQueueSize;

	private final int minInBufferCapacity;
	
	private final int maxInBufferCapacity;
	
	private boolean shutdown;
	
	ISctpEncodeTaskWriter encodeTaskWriter;
	
	public SctpSession(String name, ISctpHandler handler) {
		super(name, handler, codec(handler), LOGGER);
		if (codec != null) {
			((SctpCodecExecutorAdapter)codec).setSession(this);
		}
		minInBufferCapacity = inBufferCapacity = config.getMinInBufferCapacity();
		maxInBufferCapacity = config.getMaxInBufferCapacity();
		fragments = new SctpFragments(allocator, minInBufferCapacity, maxInBufferCapacity, optimizeBuffers);
	}

	public SctpSession(ISctpHandler handler) {
		this(null, handler);
	}
	
	private static SctpCodecExecutorAdapter codec(ISctpHandler handler) {
		ICodecExecutor executor = handler.getConfig().createCodecExecutor();
		
		return executor != null ? new SctpCodecExecutorAdapter(executor, handler) : null;
	}
	
	void markShutdown() {
		shutdown = true;
	}
	
	boolean markedShutdown() {
		return shutdown;
	}
	
	@Override
	ISctpEncodeTaskWriter getEncodeTaskWriter() {
		if (encodeTaskWriter == null) {
			encodeTaskWriter = new EncodeTaskWriter();
		}
		return encodeTaskWriter;		
	}
	
	@Override
	public ISctpHandler getHandler() {
		return (ISctpHandler) handler;
	}
	
	@Override
	public ISctpSession getParent() {
		return null;
	}

	private final long write0(SctpRecord record) {
		SelectionKey key = checkKey(this.key);
		long futureExpectedLen;

		try {
			synchronized (writeLock) {
				key = detectRebuild(key);
				if (closing != ClosingState.NONE) {
					return -1;
				}
				outQueue.add(record);
				setWriteInterestOps(key);
				outQueueSize += record.buffer.remaining();
				futureExpectedLen = outQueueSize + getWrittenBytes();
			}
		}
		catch (CancelledKeyException e) {
			throw new IllegalSessionStateException(SessionState.CLOSING);
		}
		lazyWakeup();
		return futureExpectedLen;
	}
	
	private IFuture<Void> write1(SctpRecord record) {
		long futureExpectedLen = write0(record);
		
		if (futureExpectedLen == -1) {
			return futuresController.getCancelledFuture();
		}
		return futuresController.getWriteFuture(futureExpectedLen);
	}
	
	ByteBuffer getInBuffer() {
		if (inBuffer == null) {
			inBuffer = allocator.allocate(inBufferCapacity);
			return inBuffer;
		}
		if (inBuffer.position() == inBuffer.capacity()) {
			inBuffer = allocator.extend((ByteBuffer) inBuffer.clear(), maxInBufferCapacity);
			inBufferCapacity = inBuffer.capacity();
		}
		return inBuffer;
	}
	
	void consumeInBuffer(MessageInfo msgInfo) {
		if (!msgInfo.isComplete()) {
			inBuffer = fragments.store(msgInfo.streamNumber(), inBuffer);
			return;
		}
		inBuffer = fragments.complete(msgInfo.streamNumber(), inBuffer);
		inBuffer.flip();
		if (inBuffer.hasRemaining()) {
			ISctpReader handler = superCodec();
			
			if (optimizeBuffers) {
				ByteBuffer data = inBuffer;
				
				inBuffer = null;
				handler.read(data, msgInfo);
			}
			else {
				byte[] data = new byte[inBuffer.remaining()];
				
				inBuffer.get(data);
				inBuffer.clear();
				handler.read(data, msgInfo);
			}
		}
		else if (optimizeBuffers) {
			allocator.release(inBuffer);
			inBuffer = null;
		}
	}
	
	void consumeInBufferAfterNoRead() {	
		if (optimizeBuffers && inBuffer.position() == 0) {
			allocator.release(inBuffer);
			inBuffer = null;
		}
	}
	
	/**
	 * This method must be protected by the write lock. 
	 */
	final void consumedBytes(long number) {
		outQueueSize -= number;
	}
	
	ISctpReader superCodec() {
		return (ISctpReader) (codec != null ? codec : this.handler);
	}
	
	@Override
	public void close() {
		closeCalled.set(true);
		close(false);
	}

	@Override
	public SocketAddress getLocalAddress() {
		return null;
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return null;
	}
	
	@Override
	void event(SessionEvent event) {
		super.event(event);
		if (event == SessionEvent.OPENED && !closeCalled.get()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Firing event {} for {}", EventType.SESSION_READY, this);
			}
			super.event(SessionEvent.READY);
			if (logger.isTraceEnabled()) {
				logger.trace("Ending event {} for {}", EventType.SESSION_READY, this);
			}
		}
	}

	@Override
	void preCreated() {
		if (!optimizeBuffers) {
			inBuffer = allocator.allocate(minInBufferCapacity);
		}
		outQueue = new ConcurrentLinkedQueue<SctpRecord>();
	}

	@Override
	void postEnding() {
		fragments.release();
		if (allocator.isReleasable()) {
			if (inBuffer != null) {
				allocator.release(inBuffer);
			}
			SctpRecord record;
			while ((record = outQueue.poll()) != null) {
				if (record.release) {
					allocator.release(record.buffer);
				}
			}
		}
		inBuffer = null;
		outQueue = null;
	}

	final Queue<SctpRecord> getOutQueue() {
		return outQueue;
	}
	
	@Override
	public IFuture<Void> write(byte[] data, MessageInfo msgInfo) {
		return null;
	}

	@Override
	public void writenf(byte[] data, MessageInfo msgInfo) {
	}

	static class SctpRecord {
		final MessageInfo msgInfo;
		ByteBuffer buffer;
		boolean release;
		
		SctpRecord(MessageInfo msgInfo) {
			this.msgInfo = msgInfo;
		}
	}
	
	private class EncodeTaskWriter implements ISctpEncodeTaskWriter {

		@Override
		public IFuture<Void> write(SocketAddress remoteAddress, ByteBuffer buffer, boolean withFuture) {
			return null;
		}

		@Override
		public IFuture<Void> write(SocketAddress remoteAddress, byte[] bytes, boolean withFuture) {
			return null;
		}

		@Override
		public IFuture<Void> write(MessageInfo msgInfo, ByteBuffer buffer, boolean withFuture) {
			SctpRecord record = new SctpRecord(msgInfo);
			
			record.buffer = buffer;
			record.release = optimizeBuffers;
			if (withFuture) {
				return write1(record);
			}
			write0(record);
			return null;
		}

		@Override
		public IFuture<Void> write(MessageInfo msgInfo, byte[] bytes, boolean withFuture) {
			SctpRecord record = new SctpRecord(msgInfo);
			
			record.buffer = ByteBuffer.wrap(bytes);
			if (withFuture) {
				return write1(record);
			}
			write0(record);
			return null;
		}
		
	}
}
