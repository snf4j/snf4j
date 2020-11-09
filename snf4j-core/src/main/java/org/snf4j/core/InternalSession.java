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
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.codec.ICodecExecutor;
import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.future.SessionFuturesController;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.IAllocatingHandler;
import org.snf4j.core.handler.IHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.logger.ExceptionLogger;
import org.snf4j.core.logger.IExceptionLogger;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.session.AbstractSession;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.session.ISessionTimer;
import org.snf4j.core.session.IllegalSessionStateException;
import org.snf4j.core.session.SessionState;
import org.snf4j.core.session.UnsupportedSessionTimer;
import org.snf4j.core.timer.ITimer;

abstract class InternalSession extends AbstractSession implements ISession {
	
	final ILogger logger;

	final IExceptionLogger elogger = ExceptionLogger.getInstance();

	private final static AtomicLong nextId = new AtomicLong(0);
		
	volatile ClosingState closing = ClosingState.NONE;
	
	final AtomicBoolean closeCalled = new AtomicBoolean(false);
	
	private volatile long readBytes;
	
	private volatile long writtenBytes;

	private long lastThroughputCalculationTime;

	private long lastReadBytes;

	private long lastWrittenBytes;

	private volatile double readBytesThroughput;
	
	private volatile double writtenBytesThroughput;

	private final long creationTime;
	
	private volatile long lastReadTime;
	
	private volatile long lastWriteTime;
	
	private volatile long lastIoTime;
	
	private volatile boolean readSuspended;
	
	private volatile boolean writeSuspended;
	
	final IHandler handler;

	final ISessionConfig config;

	final IByteBufferAllocator allocator;
	
	volatile SelectionKey key;
	
	volatile SelectableChannel channel;
	
	volatile InternalSelectorLoop loop;

	/** Used to synchronize write operations and changing key's selection interests */
	final Object writeLock = new Object();
	
	/** Used to track already fired events. */
	int eventBits;
	
	final SessionFuturesController futuresController = new SessionFuturesController(this);
	
	final CodecExecutorAdapter codec;

	final boolean optimizeCopying;
	
	final boolean optimizeBuffers;
	
	private final ISessionTimer timer; 

	protected InternalSession(String name, IHandler handler, ILogger logger) {
		super("Session-", 
				nextId.incrementAndGet(), 
				name != null ? name : (handler != null ? handler.getName() : null),
				handler != null ? handler.getFactory().getAttributes() : null);
	
		if (handler == null) throw new IllegalArgumentException("handler is null");
		
		this.logger = logger;
		this.handler = handler;
		this.handler.setSession(this);
		if (handler instanceof IAllocatingHandler) {
			allocator = ((IAllocatingHandler)handler).getAllocator();
		}
		else {
			allocator = handler.getFactory().getAllocator();
		}
		config = handler.getConfig();
		optimizeCopying = config.optimizeDataCopying();
		optimizeBuffers = optimizeCopying && allocator.isReleasable();
		
		creationTime = System.currentTimeMillis();
		lastReadTime = lastWriteTime = lastIoTime = lastThroughputCalculationTime = creationTime; 
		
		ICodecExecutor executor = config.createCodecExecutor();
		codec = executor != null ? new CodecExecutorAdapter(executor, this) : null;
		
		ITimer timer = handler.getFactory().getTimer();
		if (timer == null) {
			this.timer = UnsupportedSessionTimer.INSTANCE;
		}
		else {
			this.timer = new InternalSessionTimer(InternalSession.this, timer);
		}
	}
	
	abstract IEncodeTaskWriter getEncodeTaskWriter(); 
	
	void abortFutures(Throwable cause) {
		futuresController.abort(cause);
	}
	
	@Override
	public IFuture<Void> getCreateFuture() {
		return futuresController.getCreateFuture();
	}

	@Override
	public IFuture<Void> getOpenFuture() {
		return futuresController.getOpenFuture();
	}

	@Override
	public IFuture<Void> getReadyFuture() {
		return futuresController.getReadyFuture();
	}

	@Override
	public IFuture<Void> getCloseFuture() {
		return futuresController.getCloseFuture();
	}

	@Override
	public IFuture<Void> getEndFuture() {
		return futuresController.getEndFuture();
	}
	
	/**
	 * Detects if the key was replaced. It can happen after rebuilding of the selector.
	 * 
	 * @throws IllegalSessionStateException
	 *             if replacement occurred and new key is invalid
	 */
	final SelectionKey detectRebuild(SelectionKey key) {
		if (key != this.key) {
			key = this.key;
			if (!key.isValid()) {
				throw new IllegalSessionStateException(SessionState.CLOSING);
			}
		}
		return key;
	}

	/**
	 * Throws unchecked exception if the key is not valid
	 *  
	 * @throws IllegalSessionStateException
	 *             if key is not valid
	 */
	static SelectionKey checkKey(SelectionKey key) {
		if (key == null) {
			throw new IllegalSessionStateException(SessionState.OPENING);
		}
		if (!key.isValid()) {
			throw new IllegalSessionStateException(SessionState.CLOSING);
		}
		return key;
	}
	
	final void lazyWakeup() {
		if (loop != null) {
			loop.lazyWakeup();
		}
		else if (key != null) {
			key.selector().wakeup();
		}
	}
	
	@Override
	public ISessionConfig getConfig() {
		return config;
	}
	
	@Override
	public ICodecPipeline getCodecPipeline() {
		return codec != null ? codec.getExecutor().getPipeline() : null;
	}
	
	@Override
	public SessionState getState() {
		SelectionKey key = this.key;

		if (key == null) {
			return SessionState.OPENING;
		}
		return key.isValid() ? SessionState.OPEN : SessionState.CLOSING;
	}

	@Override
	public boolean isOpen() {
		return getState() == SessionState.OPEN;
	}

	boolean isCreated() {
		return channel != null;
	}

	void setChannel(SelectableChannel channel) {
		this.channel = channel;
	}
	
	void setSelectionKey(SelectionKey key) {
		this.key = key;
	}
	
	void setLoop(InternalSelectorLoop loop) {
		this.loop = loop;
		futuresController.setExecutor(loop);
	}
	
	final Object getWriteLock() {
		return writeLock;
	}
	
	void incReadBytes(long bytes, long currentTime) {
		readBytes += bytes;
		lastReadTime = lastIoTime = currentTime;
	}

	void incWrittenBytes(long bytes, long currentTime) {
		writtenBytes += bytes;
		lastWriteTime = lastIoTime = currentTime;
	}

	/**
	 * Clears key's write interest if write is not suspended. It should be
	 * executed inside block synchronized on a write lock.
	 */
	void clearWriteInterestOps(SelectionKey key) {
		if (!writeSuspended) {
			int ops = key.interestOps();

			if ((ops & SelectionKey.OP_WRITE) != 0) {
				key.interestOps(ops & (~SelectionKey.OP_WRITE));
			}
		}
	}

	/**
	 * Sets key's write interest if write is not suspended. It should be
	 * executed inside block synchronized on a write lock.
	 *
	 * @throw CancelledKeyException if the key has been canceled
	 */
	void setWriteInterestOps(SelectionKey key) {
		if (!writeSuspended) {
			int ops = key.interestOps();

			if ((ops & SelectionKey.OP_WRITE) == 0) {
				key.interestOps(ops | SelectionKey.OP_WRITE);
			}
		}
	}

	/**
	 * Suspends read, write or both if session is not in closing state. It
	 * should be executed inside block synchronized on a write lock.
	 * 
	 * @param ops
	 *            SelectionKey.OP_RAED, SelectionKey.OP_WRITE or both
	 * @throws CancelledKeyException
	 *             if the selection key associated with this session has been
	 *             cancelled
	 */
	boolean suspend(int ops) {
		if (closing == ClosingState.NONE) {
			int tmpOps = 0;
			
			if ((ops & SelectionKey.OP_READ) != 0) {
				if (!readSuspended) {
					tmpOps |= SelectionKey.OP_READ;
					readSuspended = true;
				}
			}
			if ((ops & SelectionKey.OP_WRITE) != 0) {
				if (!writeSuspended) {
					tmpOps |= SelectionKey.OP_WRITE;
					writeSuspended = true;
				}
			}

			if (tmpOps != 0) {
				key.interestOps(key.interestOps() & (~tmpOps));
				return true;
			}
		}
		return false;
	}

	/**
	 * Resumes read, write or both if session is not in closing state. It should
	 * be executed inside block synchronized on a write lock.
	 * 
	 * @param ops
	 *            SelectionKey.OP_RAED, SelectionKey.OP_WRITE or both
	 * @throws CancelledKeyException
	 *             if the selection key associated with this session has been
	 *             cancelled
	 */
	boolean resume(int ops) {
		if (closing == ClosingState.NONE) {
			int tmpOps = 0;
			
			if ((ops & SelectionKey.OP_READ) != 0) {
				if (readSuspended) {
					tmpOps |= SelectionKey.OP_READ;
					readSuspended = false;
				}
			}
			if ((ops & SelectionKey.OP_WRITE) != 0) {
				if (writeSuspended) {
					tmpOps |= SelectionKey.OP_WRITE;
					writeSuspended = false;
				}
			}
			
			if (tmpOps != 0) {
				key.interestOps(key.interestOps() | tmpOps);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void suspendRead() {
		SelectionKey key = checkKey(this.key);
		boolean wakeup;

		synchronized (writeLock) {
			detectRebuild(key);
			wakeup = suspend(SelectionKey.OP_READ);
		}
		if (wakeup) {
			lazyWakeup();
		}
	}

	@Override
	public void suspendWrite() {
		SelectionKey key = checkKey(this.key);
		boolean wakeup;

		synchronized (writeLock) {
			detectRebuild(key);
			wakeup = suspend(SelectionKey.OP_WRITE);
		}
		if (wakeup) {
			lazyWakeup();
		}
	}

	@Override
	public void resumeRead() {
		SelectionKey key = checkKey(this.key);
		boolean wakeup;

		synchronized (writeLock) {
			detectRebuild(key);
			wakeup = resume(SelectionKey.OP_READ);
		}
		if (wakeup) {
			lazyWakeup();
		}
	}

	@Override
	public void resumeWrite() {
		SelectionKey key = checkKey(this.key);
		boolean wakeup;

		synchronized (writeLock) {
			detectRebuild(key);
			wakeup = resume(SelectionKey.OP_WRITE);
		}
		if (wakeup) {
			lazyWakeup();
		}
	}
	
	@Override
	public boolean isReadSuspended() {
		return readSuspended;
	}
	
	@Override
	public boolean isWriteSuspended() {
		return writeSuspended;
	}
	
	@Override
	public final long getReadBytes() {
		return readBytes;
	}

	@Override
	public final long getWrittenBytes() {
		return writtenBytes;
	}

	void calculateThroughput(long currentTime, boolean force) {
		long minInterval = config.getThroughputCalculationInterval();
		long interval;
		
		if (minInterval > 0 && (interval = currentTime - lastThroughputCalculationTime) >= minInterval) {
			readBytesThroughput = (readBytes - lastReadBytes) * 1000.0 / interval;
			writtenBytesThroughput = (writtenBytes - lastWrittenBytes) * 1000.0 / interval;
			
			lastReadBytes = readBytes;
			lastWrittenBytes = writtenBytes;
			lastThroughputCalculationTime = currentTime;
		}
	}

	@Override
	public final double getReadBytesThroughput() {
		return readBytesThroughput;
	}
	
	@Override
	public final double getWrittenBytesThroughput() {
		return writtenBytesThroughput;
	}
	
	@Override
	public final long getCreationTime() {
		return creationTime;
	}
	
	@Override
	public final long getLastIoTime() {
		return lastIoTime;
	}
	
	@Override
	public final long getLastReadTime() {
		return lastReadTime;
	}
	
	@Override
	public final long getLastWriteTime() {
		return lastWriteTime;
	}
	
	@Override
	public ISessionTimer getTimer() {
		return timer;
	}
	
	@Override
	public ByteBuffer allocate(int capacity) {
		return allocator.allocate(capacity);
	}
	
	@Override
	public void release(ByteBuffer buffer) {
		if (allocator.isReleasable()) {
			allocator.release(buffer);
		}
	}
	
	
	final boolean wasException() {
		return (eventBits & EventType.EXCEPTION_CAUGHT.bitMask()) != 0;
	}
	
	final boolean isValid(EventType eventType) {
		if (eventType.isValid(eventBits)) {
			eventBits |= eventType.bitMask();
			return true;
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Skipping event {} for {}", eventType, this);
			}
			return false;
		}
	}
	
	void event(DataEvent event, long length) {
		if (isValid(event.type())) {
			futuresController.event(event, length);
			try {
				handler.event(event, length);
			}
			catch (Exception e) {
				elogger.error(logger, "Failed event {} for {}: {}", event, this, e);
			}
		}
	}
	
	void timer(Object event) {
		try {
			handler.timer(event);
		}
		catch (Exception e) {
			elogger.error(logger, "Failed timer event for {}: {}", this, e);
		}
	}
	
	void timer(Runnable task) {
		try {
			handler.timer(task);
		}
		catch (Exception e) {
			elogger.error(logger, "Failed timer task for {}: {}", this, e);
		}
	}
	
	void event(SessionEvent event) {
		if (isValid(event.type())) {
			futuresController.event(event);
			try {
				if (codec != null) {
					ICodecExecutor executor = codec.getExecutor();
					
					executor.syncEventDrivenCodecs(this);
					executor.event(this, event);
				}
				handler.event(event);
			}
			catch (Exception e) {
				elogger.error(logger, "Failed event {} for {}: {}", event, this, e);
			}
		}
	}
	
	void exception(Throwable t) {
		if (isValid(EventType.EXCEPTION_CAUGHT)) {
			try {
				handler.exception(t);
				futuresController.exception(t);
				quickClose();
			}
			catch (Exception e) {
				elogger.error(logger, "Failed event {} for {}: {}", EventType.EXCEPTION_CAUGHT, this, e);
				futuresController.exception(t);
				quickClose();
			}
		}
	}
	
	boolean incident(SessionIncident incident, Throwable t) {
		try {
			return handler.incident(incident, t);
		}
		catch (Exception e) {
			elogger.error(logger, "Failed incident {} for {}: {}", incident, this, e);
		}
		return false;
	}
	
	abstract void preCreated();
	
	abstract void postEnding();
	
	void close(SelectableChannel channel) throws IOException {
		channel.close();
	}
	
	void closeAndFinish(SelectableChannel channel) {
		synchronized (writeLock) {
			closing = ClosingState.FINISHED;
			try {
				close(channel);
			} catch (IOException e) {
				//Ignore
			}
		}
	}

    static void checkBounds(int offset, int length, int size) {
        if ((offset | length | (offset + length) | (size - (offset + length))) < 0) {
        	throw new IndexOutOfBoundsException();        	
        }
    } 
    
}
