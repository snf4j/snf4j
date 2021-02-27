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
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.snf4j.core.codec.ICodecExecutor;
import org.snf4j.core.future.CancelledFuture;
import org.snf4j.core.future.FailedFuture;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.future.SuccessfulFuture;
import org.snf4j.core.future.TaskFuture;
import org.snf4j.core.handler.ISctpHandler;
import org.snf4j.core.handler.SctpNotificationType;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.session.ISctpSession;
import org.snf4j.core.session.ISctpSessionConfig;
import org.snf4j.core.session.IllegalSessionStateException;
import org.snf4j.core.session.SessionState;

import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.Notification;

abstract class InternalSctpSession extends InternalSession implements ISctpSession {

	private final SctpFragments fragments;
	
	private int inBufferCapacity;
	
	private ByteBuffer inBuffer;
	
	private long outQueueSize;
	
	private ConcurrentLinkedQueue<SctpRecord> outQueue;
	
	private final int minInBufferCapacity;
	
	private final int maxInBufferCapacity;
	
	private final ImmutableSctpMessageInfo defaultMsgInfo;
	
	final boolean defaultPeerAddress;
	
	ISctpEncodeTaskWriter encodeTaskWriter;
	
	InternalSctpSession(String name, ISctpHandler handler, ILogger logger) {
		super(name, handler, codec(handler), logger);
		if (codec != null) {
			((SctpCodecExecutorAdapter)codec).setSession(this);
		}
		
		ISctpSessionConfig config = (ISctpSessionConfig) this.config;
		SocketAddress addr = config.getDefaultSctpPeerAddress();
		
		minInBufferCapacity = inBufferCapacity = config.getMinInBufferCapacity();
		maxInBufferCapacity = config.getMaxInBufferCapacity();
		fragments = new SctpFragments(allocator, minInBufferCapacity, maxInBufferCapacity, optimizeBuffers);
		
		if (addr == null) {
			defaultPeerAddress = false;
			defaultMsgInfo = ImmutableSctpMessageInfo.create(
					config.getDefaultSctpStreamNumber(),
					config.getDefaultSctpPayloadProtocolID(),
					config.getDefaultSctpUnorderedFlag());
		}
		else {
			defaultPeerAddress = true;
			defaultMsgInfo = ImmutableSctpMessageInfo.create(
					addr,
					config.getDefaultSctpStreamNumber(),
					config.getDefaultSctpPayloadProtocolID(),
					config.getDefaultSctpUnorderedFlag());			
		}
	}
	
	private static SctpCodecExecutorAdapter codec(ISctpHandler handler) {
		ICodecExecutor executor;
		
		if (handler == null) {
			throw new IllegalArgumentException("handler is null");
		}
		executor = handler.getConfig().createCodecExecutor();
		return executor != null ? new SctpCodecExecutorAdapter(executor, handler) : null;
	}
	
	@Override
	ISctpEncodeTaskWriter getEncodeTaskWriter() {
		if (encodeTaskWriter == null) {
			encodeTaskWriter = new EncodeTaskWriter();
		}
		return encodeTaskWriter;		
	}
	
	abstract boolean closeNow();
	
	@Override
	public ISctpHandler getHandler() {
		return (ISctpHandler) handler;
	}
	
	@Override
	public ISctpSession getParent() {
		return null;
	}
	
	@Override
	public void close() {
		closeCalled.set(true);
		close(false, true);
	}

	@Override
	public void quickClose() {
		closeCalled.set(true);
		close(false, false);
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
				outQueueSize += record.buffer.remaining();
				futureExpectedLen = outQueueSize + getWrittenBytes();
				outQueue.add(record);
				setWriteInterestOps(key);
			}
		}
		catch (CancelledKeyException e) {
			throw new IllegalSessionStateException(SessionState.CLOSING);
		}
		lazyWakeup();
		return futureExpectedLen;
	}
	
	ByteBuffer getInBuffer() {
		if (inBuffer == null) {
			inBuffer = allocator.allocate(inBufferCapacity);
			return inBuffer;
		}
		return inBuffer;
	}
	
	void consumeInBuffer(MessageInfo msgInfo) {
		if (!msgInfo.isComplete()) {
			inBuffer = fragments.store(msgInfo.streamNumber(), inBuffer);
			return;
		}
		inBuffer = fragments.complete(msgInfo.streamNumber(), inBuffer);
		if (inBuffer.capacity() > inBufferCapacity) {
			inBufferCapacity = inBuffer.capacity();
		}
		inBuffer.flip();
		
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
	
	void consumeInBufferAfterNoRead() {	
		if (optimizeBuffers) {
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
	
	abstract Set<SocketAddress> getAddresses(Association association, boolean local);

	@Override
	public SocketAddress getLocalAddress() {
		Iterator<SocketAddress> i = getAddresses(null, true).iterator();
		
		if (i.hasNext()) {
			return i.next();
		}
		return null;
	}
	
	@Override
	public SocketAddress getRemoteAddress() {
		Iterator<SocketAddress> i = getAddresses(null, false).iterator();
		
		if (i.hasNext()) {
			return i.next();
		}
		return null;
	}
	
	@Override
	public Set<SocketAddress> getLocalAddresses() {
		return getAddresses(null, true);
	}
	
	@Override
	public Set<SocketAddress> getRemoteAddresses() {
		return getAddresses(null, false);
	}
	
	abstract void bind(InetAddress address) throws IOException;
	
	abstract void unbind(InetAddress address) throws IOException;
	
	private void bindUnbind(InetAddress address, TaskFuture<Void> future, boolean bind) {
		try {
			if (bind) {
				bind(address);
			} else {
				unbind(address);
			}
			future.success();
		}
		catch (Throwable t) {
			future.abort(t);
		}
	}
	
	private IFuture<Void> bindUnbind(final InetAddress address, boolean bind) {
		InternalSelectorLoop loop = this.loop;
		SelectableChannel channel = this.channel;
		
		if (loop == null || channel == null) {
			return new CancelledFuture<Void>(this);
		}
		if (loop.inLoop()) {
			try {
				if (bind) {
					bind(address);
				} else {
					unbind(address);
				}
			}
			catch (Throwable t) {
				return new FailedFuture<Void>(this,t);
			}
			return new SuccessfulFuture<Void>(this);
		}
		
		final TaskFuture<Void> future = new TaskFuture<Void>(this);
		
		loop.executenf(new Runnable() {

			@Override
			public void run() {
				bindUnbind(address, future, bind);
			}
		});
		return future;
	}
	
	@Override
	public IFuture<Void> bindAddress(InetAddress address) {
		return bindUnbind(address, true);
	}
	
	@Override
	public IFuture<Void> unbindAddress(InetAddress address) {
		return bindUnbind(address, false);
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

	HandlerResult notification(Notification notification, SctpNotificationType type) {
		return ((ISctpHandler)handler).notification(notification, type);
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
	
	private IFuture<Void> writeFuture(long expectedLen) {
		if (expectedLen == -1) {
			return futuresController.getCancelledFuture();
		}
		return futuresController.getWriteFuture(expectedLen);
	}
	
	private SctpRecord createRecord(ImmutableSctpMessageInfo msgInfo, byte[] msg, int offset, int length) {
		SctpRecord record = new SctpRecord(msgInfo);
		
		if (optimizeCopying && allocator.usesArray()) {
			record.buffer = ByteBuffer.wrap(msg, offset, length);
		}
		else {
			ByteBuffer buffer = allocator.allocate(length);
			
			buffer.put(msg, offset, length).flip();
			record.buffer = buffer;
			record.release = true;
		}
		return record;
	}
	
	private SctpRecord createRecord(ImmutableSctpMessageInfo msgInfo, ByteBuffer msg, int length) {
		SctpRecord record = new SctpRecord(msgInfo);
		boolean allRemaining = length == msg.remaining();
		
		if (optimizeCopying && allRemaining) {
			record.buffer = msg;
			record.release = optimizeBuffers;
		}
		else {
			ByteBuffer buf = allocator.allocate(length);
			
			if (allRemaining) {
				buf.put(msg).flip();
			}
			else {
				ByteBuffer dup = msg.duplicate();
				
				dup.limit(dup.position() + length);
				buf.put(dup).flip();
				msg.position(dup.position());
			}
			record.buffer = buf;
			record.release = true;
		}
		return record;
	}	
	
	private long write0(ImmutableSctpMessageInfo msgInfo, byte[] msg, int offset, int length) {
		return write0(createRecord(msgInfo, msg, offset, length));
	}
	
	private long write0(ImmutableSctpMessageInfo msgInfo, ByteBuffer msg, int length) {
		return write0(createRecord(msgInfo, msg, length));
	}
	
	@Override
	public IFuture<Void> write(byte[] msg) {
		return write(msg, null);
	}
	
	@Override
	public IFuture<Void> write(byte[] msg, int offset, int length) {
		return write(msg, offset, length, null);
	}
	
	@Override
	public IFuture<Void> write(byte[] msg, ImmutableSctpMessageInfo msgInfo) {
		if (msg == null) throw new NullPointerException();
		else if (msg.length == 0) {
			return futuresController.getSuccessfulFuture();
		}
		if (msgInfo == null) msgInfo = defaultMsgInfo;
		if (codec != null) {
			return new SctpEncodeTask(this, msg).register(msgInfo);
		}
		return writeFuture(write0(msgInfo, msg, 0, msg.length));
	}
	
	@Override
	public IFuture<Void> write(byte[] msg, int offset, int length, ImmutableSctpMessageInfo msgInfo) {
		if (msg == null) throw new NullPointerException();
		checkBounds(offset, length, msg.length);
		if (length == 0) {
			return futuresController.getSuccessfulFuture();
		}
		if (msgInfo == null) msgInfo = defaultMsgInfo;
		if (codec != null) {
			return new SctpEncodeTask(this, msg, offset, length).register(msgInfo);
		}
		return writeFuture(write0(msgInfo, msg, offset, length));
	}
	
	@Override
	public void writenf(byte[] msg) {
		writenf(msg, null);
	}
	
	@Override
	public void writenf(byte[] msg, int offset, int length) {
		writenf(msg, offset, length, null);
	}

	@Override
	public void writenf(byte[] msg, ImmutableSctpMessageInfo msgInfo) {
		if (msg == null) throw new NullPointerException();
		else if (msg.length > 0) {
			if (msgInfo == null) msgInfo = defaultMsgInfo;
			if (codec != null) {
				new SctpEncodeTask(this, msg).registernf(msgInfo);
			}
			else {
				write0(msgInfo, msg, 0, msg.length);
			}
		}
	}
	
	@Override
	public void writenf(byte[] msg, int offset, int length, ImmutableSctpMessageInfo msgInfo) {
		if (msg == null) throw new NullPointerException();
		checkBounds(offset, length, msg.length);
		if (length > 0) {
			if (msgInfo == null) msgInfo = defaultMsgInfo;
			if (codec != null) {
				new SctpEncodeTask(this, msg, offset, length).registernf(msgInfo);
			}
			else {
				write0(msgInfo, msg, offset, length);
			}			
		}
	}
	
	@Override
	public IFuture<Void> write(ByteBuffer msg) {
		return write(msg, null);
	}

	@Override
	public IFuture<Void> write(ByteBuffer msg, int length) {
		return write(msg, length, null);
	}
	
	@Override
	public IFuture<Void> write(ByteBuffer msg, int length, ImmutableSctpMessageInfo msgInfo) {
		if (msg == null) throw new NullPointerException();
		else if (msg.remaining() < length || length < 0) {
			throw new IndexOutOfBoundsException();
		}
		else if (length == 0) {
			return futuresController.getSuccessfulFuture();
		}
		if (msgInfo == null) msgInfo = defaultMsgInfo;
		if (codec != null) {
			return new SctpEncodeTask(this, msg, length).register(msgInfo);
		}
		return writeFuture(write0(msgInfo, msg, length));
	}
	
	@Override
	public void writenf(ByteBuffer msg) {
		writenf(msg, null);
	}
	
	@Override
	public void writenf(ByteBuffer msg, int length) {
		writenf(msg, length, null);
	}
	
	@Override
	public void writenf(ByteBuffer msg, ImmutableSctpMessageInfo msgInfo) {
		if (msg == null) throw new NullPointerException();
		else if (msg.remaining() > 0) {
			if (msgInfo == null) msgInfo = defaultMsgInfo;
			if (codec != null) {
				new SctpEncodeTask(this, msg).registernf(msgInfo);
			}
			else {
				write0(msgInfo, msg, msg.remaining());
			}
		}
	}
	
	@Override
	public void writenf(ByteBuffer msg, int length, ImmutableSctpMessageInfo msgInfo) {
		if (msg == null) throw new NullPointerException();
		else if (msg.remaining() < length || length < 0) {
			throw new IndexOutOfBoundsException();
		}
		else if (length > 0) {
			if (msgInfo == null) msgInfo = defaultMsgInfo;
			if (codec != null) {
				new SctpEncodeTask(this, msg, length).registernf(msgInfo);
			}
			else {
				write0(msgInfo, msg, length);
			}
		}
	}

	@Override
	public IFuture<Void> write(Object msg) {
		return write(msg, null);
	}

	@Override
	public IFuture<Void> write(Object msg, ImmutableSctpMessageInfo msgInfo) {
		if (msg == null) throw new NullPointerException();
		if (msgInfo == null) msgInfo = defaultMsgInfo;
		if (codec != null) {
			return new SctpEncodeTask(this, msg).register(msgInfo);
		}
		if (msg.getClass() == byte[].class) {
			return write((byte[])msg, msgInfo);
		}
		if (msg instanceof ByteBuffer) {
			return write((ByteBuffer)msg, msgInfo);
		}
		throw new IllegalArgumentException("msg is an unexpected object");
	}
	
	@Override
	public void writenf(Object msg) {
		writenf(msg, null);
	}
	
	@Override
	public IFuture<Void> write(ByteBuffer msg, ImmutableSctpMessageInfo msgInfo) {
		if (msg == null) throw new NullPointerException();
		else if (msg.remaining() == 0) {
			return futuresController.getSuccessfulFuture();
		}
		if (msgInfo == null) msgInfo = defaultMsgInfo;
		if (codec != null) {
			return new SctpEncodeTask(this, msg).register(msgInfo);
		}
		return writeFuture(write0(msgInfo, msg, msg.remaining()));
	}

	@Override
	public void writenf(Object msg, ImmutableSctpMessageInfo msgInfo) {
		if (msg == null) throw new NullPointerException();
		if (msgInfo == null) msgInfo = defaultMsgInfo;
		if (codec != null) {
			new SctpEncodeTask(this, msg).registernf(msgInfo);
		}
		else if (msg.getClass() == byte[].class) {
			writenf((byte[])msg, msgInfo);
		}
		else if (msg instanceof ByteBuffer) {
			writenf((ByteBuffer)msg, msgInfo);
		}
		else {
			throw new IllegalArgumentException("msg is an unexpected object");
		}
	}
	
	static class SctpRecord {
		final ImmutableSctpMessageInfo msgInfo;
		ByteBuffer buffer;
		boolean release;
		
		SctpRecord(ImmutableSctpMessageInfo msgInfo) {
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
		public IFuture<Void> write(ImmutableSctpMessageInfo msgInfo, ByteBuffer buffer, boolean withFuture) {
			SctpRecord record = new SctpRecord(msgInfo);
			
			record.buffer = buffer;
			record.release = optimizeBuffers;
			if (withFuture) {
				return writeFuture(write0(record));
			}
			write0(record);
			return null;
		}

		@Override
		public IFuture<Void> write(ImmutableSctpMessageInfo msgInfo, byte[] bytes, boolean withFuture) {
			SctpRecord record = new SctpRecord(msgInfo);
			
			record.buffer = ByteBuffer.wrap(bytes);
			if (withFuture) {
				return writeFuture(write0(record));
			}
			write0(record);
			return null;
		}
		
	}	
}
