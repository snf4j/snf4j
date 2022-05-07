/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2022 SNF4J contributors
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
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Queue;

import org.snf4j.core.DatagramSession.DatagramRecord;
import org.snf4j.core.factory.ISelectorLoopStructureFactory;
import org.snf4j.core.factory.IStreamSessionFactory;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.core.pool.ISelectorLoopPool;

/**
 * A selector loop responsible for processing I/O operations of stream-oriented
 * and datagram-oriented connections
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class SelectorLoop extends InternalSelectorLoop {

	private final static ILogger LOGGER = LoggerFactory.getLogger(SelectorLoop.class);

	final ISelectorLoopPool parentPool;
	
	private volatile ISelectorLoopPool pool;
	
	private volatile ISelectorLoopController controller = DefaultSelectorLoopController.DEFAULT;

	/**
	 * Constructs a named selector loop with specified parent pool
	 * 
	 * @param name
	 *            the name for this selector loop, or <code>null</code> if the
	 *            name should be auto generated
	 * @param parentPool
	 *            the parent pool that owns this selector loop, or
	 *            <code>null</code> if the selector loop has no parent.
	 * @param factory
	 *            a factory that will be used by this selector loop to
	 *            open its selector, or <code>null</code> if default factory
	 *            should be used
	 * @throws IOException
	 *             if the {@link java.nio.channels.Selector Selector} associated
	 *             with this selector loop could not be opened
	 */
	public SelectorLoop(String name, ISelectorLoopPool parentPool, ISelectorLoopStructureFactory factory) throws IOException {
		super(name, LOGGER, factory);
		this.parentPool = parentPool;
	}
	
	/**
	 * Constructs a named selector loop
	 * 
	 * @param name
	 *            the name for this selector loop, or <code>null</code> if the name should 
	 *            be auto generated
	 * @throws IOException
	 *             if the {@link java.nio.channels.Selector Selector} associated with this
	 *             selector loop could not be opened
	 */
	public SelectorLoop(String name) throws IOException {
		this(name, null, null);
	}

	/**
	 * Constructs a unnamed selector loop. Its name will be auto generated.
	 * 
	 * @throws IOException
	 *             if the {@link java.nio.channels.Selector Selector} associated with this
	 *             selector loop could not be opened
	 */
	public SelectorLoop() throws IOException {
		this(null, null, null);
	}

	/**
	 * Gets the selector loop pool that owns this selector loop.
	 * 
	 * @return the parent selector loop pool, or <code>null</code> if it has
	 * no parent selector loop pool
	 */
	public ISelectorLoopPool getParentPool() {
		return parentPool;
	}

	/**
	 * Sets the pool that will be used by this selector loop to retrieve
	 * selector loops for accepted channels.
	 * <p>
	 * There is no need for the pool to be thread safe as long as it is used
	 * only by this selector loop.
	 * 
	 * @param pool
	 *            the pool with selector loops for accepted channels
	 */
	public void setPool(ISelectorLoopPool pool) {
		this.pool = pool;
	}
	
	/**
	 * Gets the pool that is used by this selector loop to retrieve selector
	 * loops for accepted channels.
	 * 
	 * @return the pool with selector loops for accepted channels
	 */
	public ISelectorLoopPool getPool() {
		return pool;
	}

	/**
	 * Sets the controller determining the behavior of this selector loop.
	 *
	 * @param controller the controller
	 */
	public void setController(ISelectorLoopController controller) {
		this.controller = controller;
	}

	/**
	 * Gets the controller determining the behavior of this selector loop.
	 *
	 * @return the controller
	 */
	public ISelectorLoopController getController() {
		return controller;
	}
	
	/**
	 * Registers a stream-oriented channel with this selector loop. The method
	 * only adds the channel to the selector-loop's pending registration queue.
	 * <p>
	 * This method is asynchronous.
	 * 
	 * @param channel
	 *            the stream-oriented channel to register with this selector
	 *            loop
	 * @param handler
	 *            the handler that will be associated with the channel
	 * @return the future associated with this registration
	 * @throws ClosedChannelException
	 *             if the channel is closed
	 * @throws SelectorLoopStoppingException
	 *             if selector loop is in the process of stopping
	 * @throws ClosedSelectorException
	 *             if the internal selector is closed
	 * @throws IllegalArgumentException
	 *             if a bit in ops does not correspond to an operation that is
	 *             supported by the channel
	 */
	public IFuture<Void> register(SocketChannel channel, IStreamHandler handler) throws ClosedChannelException {
		StreamSession session = new StreamSession(handler);
		
		return super.register(channel, 0, new SocketChannelContext(session));
	}
	
	/**
	 * Registers a stream-oriented channel with this selector loop. The method
	 * only adds the channel to the selector-loop's pending registration queue.
	 * <p>
	 * This method is asynchronous.
	 * 
	 * @param channel
	 *            the stream-oriented channel to register with this selector
	 *            loop
	 * @param session
	 *            the session that will be associated with the channel
	 * @return the future associated with this registration
	 * @throws ClosedChannelException 
	 *             if the channel is closed
	 * @throws SelectorLoopStoppingException
	 *             if selector loop is in the process of stopping
	 * @throws ClosedSelectorException
	 *             if the internal selector is closed
	 * @throws IllegalArgumentException
	 *             if a bit in ops does not correspond to an operation that is
	 *             supported by the channel
	 * @throws IllegalArgumentException
	 *             if the session argument is <code>null</code> or the session
	 *             object is reused (was already registered with some selector 
	 *             loop)
	 */
	public IFuture<Void> register(SocketChannel channel, StreamSession session) throws ClosedChannelException {
		if (session == null) throw new IllegalArgumentException("session is null");
		return super.register(channel, 0, new SocketChannelContext(session));
	}

	/**
	 * Registers a datagram-oriented channel with this selector loop. The method
	 * only adds the channel to the selector-loop's pending registration queue.
	 * <p>
	 * This method is asynchronous.
	 * 
	 * @param channel
	 *            the datagram-oriented channel to register with this selector
	 *            loop
	 * @param handler
	 *            the handler that will be associated with the channel
	 * @return the future associated with this registration
	 * @throws ClosedChannelException 
	 *             if the channel is closed
	 * @throws SelectorLoopStoppingException
	 *             if selector loop is in the process of stopping
	 * @throws ClosedSelectorException
	 *             if the internal selector is closed
	 * @throws IllegalArgumentException
	 *             if a bit in ops does not correspond to an operation that is
	 *             supported by the channel
	 */
	public IFuture<Void> register(DatagramChannel channel, IDatagramHandler handler) throws ClosedChannelException {
		DatagramSession session = new DatagramSession(handler);
		
		return super.register(channel, SelectionKey.OP_READ, new DatagramChannelContext(session));
	}

	/**
	 * Registers a datagram-oriented channel with this selector loop. The method
	 * only adds the channel to the selector-loop's pending registration queue.
	 * <p>
	 * This method is asynchronous.
	 * 
	 * @param channel
	 *            the datagram-oriented channel to register with this selector
	 *            loop
	 * @param session
	 *            the session that will be associated with the channel
	 * @return the future associated with this registration
	 * @throws ClosedChannelException 
	 *             if the channel is closed
	 * @throws SelectorLoopStoppingException
	 *             if selector loop is in the process of stopping
	 * @throws ClosedSelectorException
	 *             if the internal selector is closed
	 * @throws IllegalArgumentException
	 *             if a bit in ops does not correspond to an operation that is
	 *             supported by the channel
	 * @throws IllegalArgumentException
	 *             if the session argument is <code>null</code> or the session
	 *             object is reused (was already registered with some selector 
	 *             loop)
	 */
	public IFuture<Void> register(DatagramChannel channel, DatagramSession session) throws ClosedChannelException {
		if (session == null) throw new IllegalArgumentException("session is null");
		return super.register(channel, SelectionKey.OP_READ, new DatagramChannelContext(session));
	}
	
	/**
	 * Registers a listening stream-oriented channel with this selector loop.
	 * The method only adds the channel to the selector-loop's pending
	 * registration queue.
	 * <p>
	 * This method is asynchronous.
	 * 
	 * @param channel
	 *            the listening stream-oriented channel to register with this
	 *            selector loop
	 * @param factory
	 *            the factory that will be associated with the channel. It will
	 *            be used to create sessions for newly accepted channels
	 * @return the future associated with this registration
	 * @throws ClosedChannelException 
	 *             if the channel is closed
	 * @throws SelectorLoopStoppingException
	 *             if selector loop is in the process of stopping
	 * @throws ClosedSelectorException
	 *             if the internal selector is closed
	 * @throws IllegalArgumentException
	 *             if a bit in ops does not correspond to an operation that is
	 *             supported by the channel
	 * @throws IllegalArgumentException
	 *             if the factory argument is <code>null</code>
	 */
	public IFuture<Void> register(ServerSocketChannel channel, IStreamSessionFactory factory) throws ClosedChannelException {
		if (factory == null) throw new IllegalArgumentException("factory is null");
		return super.register(channel, SelectionKey.OP_ACCEPT, new ServerSocketChannelContext(factory));
	}
	
	@Override
	void handleRegisteredKey(SelectionKey key, PendingRegistration reg) throws Exception {
		SelectableChannel channel = reg.channel;
		ChannelContext<?> ctx = reg.ctx;
		
		if (!ctx.completeRegistration(this, key, channel)) {
			return;
		}
		
		InternalSession session = ctx.getSession();
		
		if (fireCreatedEvent(session, channel)) {
			session.setSelectionKey(key);
			if (debugEnabled) {
				logger.debug("Channel {} associated with {}", ctx.toString(channel), session);
			}
			fireEvent(session, SessionEvent.OPENED);
			if (session.closeCalled.get()) {
				session.closeAndFinish(channel);
				fireEvent(session, SessionEvent.CLOSED);
				fireEndingEvent(session, false);
			}
		}
		else {
			fireEndingEvent(session, false);
		}
	}

	@Override
	SelectionKey handleSelectedKey(SelectionKey key) {
		ChannelContext<?> ctx = (ChannelContext<?>) key.attachment();
		
		if (ctx.isSession()) {
			ctx.handle(this, key);
			if (areSwitchings) {
				handleSwitchings();
			}
		}
		else if (key.isAcceptable()) {
			return handleAccepting(key);
		}
		return key;
	}
	
	@Override
	void notifyAboutLoopSizeChange(int newSize, int prevSize) {
		ISelectorLoopPool parentPool = this.parentPool;
		
		if (parentPool != null) {
			parentPool.update(this, newSize, prevSize);
		}
	}
	
	@Override
	boolean notifyAboutLoopChanges() {
		return parentPool != null;
	}
	
	private final SelectionKey handleAccepting(final SelectionKey key) {
		SelectableChannel channel = null;
		ChannelContext<?> ctx = (ChannelContext<?>)key.attachment();
		
		if (debugEnabled) {
			logger.debug("Accepting from channel {}", ctx.toString(key.channel()));
		}
		
		try {
			channel = ctx.accept(key.channel());
			channel.configureBlocking(false);
			if (!controller.processAccepted(channel)) {
				channel.close();
				channel = null;
			}
			if (debugEnabled) {
				logger.debug("Accepted channel {}", ctx.toString(channel));
			}
		}
		catch (Throwable e) {
			elogWarnOrError(logger, "Accepting from channel {} failed: {}", ctx.toString(key.channel()), e);
			if (channel != null) {
				try {
					channel.close();
				} catch (Exception e1) {
					//Ignore
				}
				channel = null;
			}
		}
		
		if (channel != null) {
			InternalSession session = null; 
			SelectionKey acceptedKey = null;
			
			try {
				session = ctx.create(channel);
				ISelectorLoopPool pool = this.pool;
				SelectorLoop loop = pool != null ? pool.getLoop(channel) : null;
				if (loop != null) {
					if (debugEnabled) {
						logger.debug("Moving registration of channel {} to other selector loop {}", ctx.toString(channel), loop);
					}
					loop.register(channel, SelectionKey.OP_READ, ctx.wrap(session));
					return key;
				}
				acceptedKey = channel.register(getUnderlyingSelector(selector), SelectionKey.OP_READ, ctx.wrap(session));
			}
			catch (Throwable e) {
				if (session == null) {
					elogger.error(logger, "Unable to create session for accepted channel {}: {}", ctx.toString(channel), e);
					try {
						channel.close();
					}
					catch (Exception ex) {
						//Ignore
					}
					return key;
				}
				elogger.error(logger, "Unable to register channel {} with selector: {}", ctx.toString(channel), e);
				fireCreatedEvent(session, channel);
				fireException(session, e);
			}
			
			if (acceptedKey != null) {
				InternalSession first = session.getFirstInPipeline();
					
				if (first != null) {
					ctx = ctx.wrap(session = first);
					acceptedKey.attach(ctx);
				}
				if (fireCreatedEvent(session, channel)) {
					if (debugEnabled) {
						logger.debug("Channel {} is associated with {}", ctx.toString(channel), session);
					}
					session.setSelectionKey(acceptedKey);
					fireEvent(session, SessionEvent.OPENED);
					return acceptedKey;
				}
				else {
					fireEndingEvent(session, false);
				}
			}
			else {
				try {
					channel.close();
				} catch (IOException e) {
					//Ignore
				}
				
				if (!channel.isRegistered()) {
					fireEndingEvent(session, false);
				}
			}
		}
		return key;
	}
	
	final void handleConnecting(InternalSession session, final SelectionKey key) {
		ChannelContext<?> ctx = (ChannelContext<?>)key.attachment();
		
		if (debugEnabled) {
			logger.debug("Finishing connection of channel {}", ctx.toString(key.channel()));
		}

		boolean finished = false;
		InternalSession first = session.getFirstInPipeline();
			
		if (first != null) {
			ctx = ctx.wrap(session = first);
			key.attach(ctx);
		}
		if (fireCreatedEvent(session, key.channel())) {
			try {
				if (controller.processConnection(key.channel())) {
					finished = ctx.finishConnect(key.channel());
				}
				else {
					key.channel().close();
				}
			}
			catch (Throwable e) {
				elogWarnOrError(logger, "Finishing connection of channel {} failed: {}", ctx.toString(key.channel()), e);
				fireException(session, e);
			}
		}
		
		if (finished) {
			if (debugEnabled) {
				logger.debug("Channel {} associated with {}", ctx.toString(key.channel()), session);
			}
			key.interestOps(SelectionKey.OP_READ);
			session.setSelectionKey(key);
			fireEvent(session, SessionEvent.OPENED);
		}
	}
	
	final int handleWriting(final StreamSession session, final SelectionKey key, int spinCount) {
		long totalBytes = 0;

		if (traceEnabled) {
			logger.trace("Writting to channel in {}", session);
		}

		try {
			synchronized (session.getWriteLock()) {
				do {
					ByteBuffer[] b = session.getOutBuffers();

					//check if buffers has some data
					boolean areEmpty = true;
					for (int i = b.length-1; i >= 0; --i) {
						if (b[i].hasRemaining()) {
							areEmpty = false;
							break;
						}
					}

					if (areEmpty) {
						if (session.compactOutBuffers(0)) {
							session.clearWriteInterestOps(key);
							session.handleClosingInProgress();
						}
						break;
					}
					else {
						long bytes = ((SocketChannel)key.channel()).write(b);

						if (bytes > 0) {
							long currentTime = System.currentTimeMillis();
							totalBytes += bytes;
							--spinCount;

							if (traceEnabled) {
								logger.trace("{} byte(s) written to channel in {}", bytes, session);
							}
							session.calculateThroughput(currentTime, false);
							session.incWrittenBytes(bytes, currentTime);
							if (session.compactOutBuffers(bytes)) {
								session.clearWriteInterestOps(key);
								session.handleClosingInProgress();
								break;
							}
						}
						else {
							ByteBuffer buf = b[b.length-1];

							buf.position(buf.limit());
							buf.limit(buf.capacity());
							break;
						}
					}
				} while (spinCount > 0);
			}
		}
		catch (Throwable e) {
			if (totalBytes > 0) {
				fireEvent(session, DataEvent.SENT, totalBytes);
			}
			elogWarnOrError(logger, "Writting to channel in {} failed: {}", session, e);
			fireException(session, e);
			totalBytes = 0;
		}
		
		if (totalBytes > 0) {
			fireEvent(session, DataEvent.SENT, totalBytes);
			return spinCount;
		}
		return 0;
	}
	
	final void handleReading(final StreamSession session, final SelectionKey key) {
		int bytes;

		if (traceEnabled) {
			logger.trace("Reading from channel in {}", session);
		}
		
		try {
			bytes = ((SocketChannel)key.channel()).read(session.getInBuffer());
		}
		catch (Throwable e) {
			elogWarnOrError(logger, "Reading from channel in {} failed: {}", session, e);
			fireException(session, e);
			bytes = 0;
		}
		
		if (bytes > 0) {
			long currentTime = System.currentTimeMillis();
			
			if (traceEnabled) {
				logger.trace("{} byte(s) read from channel in {}", bytes, session);
			}
			session.calculateThroughput(currentTime, false);
			session.incReadBytes(bytes, currentTime);
			fireEvent(session, DataEvent.RECEIVED, bytes);
			session.consumeInBuffer();
		}
		else if (bytes < 0){
			if (debugEnabled) {
				logger.debug("Closing channel in {} after reaching end-of-stream", session);
			}
			if (session.pipelineItem != null) {
				session.pipelineItem.markEos();
			}
			session.close(true);
		}
		else {
			session.consumeInBufferAfterNoRead();
		}
	}
	
	final void fireEvent(final DatagramSession session, DataEvent event, long length, SocketAddress remoteAddress) {
		if (traceEnabled) {
			logger.trace("Firing event {} for {}", event.type(), session);
		}
		session.event(remoteAddress, event, length);
		if (traceEnabled) {
			logger.trace("Ending event {} for {}", event.type(), session);
		}
	}
	
	final void handleReading(final DatagramSession session, final SelectionKey key) {
		int bytes;
		
		if (traceEnabled) {
			logger.trace("Reading from channel in {}", session);
		}

		DatagramChannel channel = (DatagramChannel) key.channel();
		SocketAddress remoteAddress = null;
		
		try {
			if (channel.isConnected()) {
				bytes = channel.read(session.getInBuffer());
			}
			else {
				ByteBuffer buffer = session.getInBuffer();
				remoteAddress = channel.receive(buffer);
				if (remoteAddress != null) {
					bytes = buffer.position();
				}
				else {
					bytes = 0;
				}
			}
		} 
		catch (Throwable e) {
			elogWarnOrError(logger, "Reading from channel in {} failed: {}", session, e);
			fireException(session, e);
			bytes = 0;
		}
		
		if (bytes > 0) { 
			long currentTime = System.currentTimeMillis();
			
			if (traceEnabled) {
				if (remoteAddress == null) {
					logger.trace("{} byte(s) read from channel in {}", bytes, session);
				}
				else {
					logger.trace("{} byte(s) received from remote address {} in {}", bytes, remoteAddress, session);
				}
			}
			session.calculateThroughput(currentTime, false);
			session.incReadBytes(bytes, currentTime);
			if (remoteAddress != null) {
				fireEvent(session, DataEvent.RECEIVED, bytes, remoteAddress);
			}
			else {
				fireEvent(session, DataEvent.RECEIVED, bytes);				
			}
		}
		session.consumeInBuffer(remoteAddress);
	}
	
	final int handleWriting(final DatagramSession session, final SelectionKey key, int spinCount) {
		long totalBytes = 0;
		long leftBytes = 0;
		long bytes;
		Throwable exception = null;
		
		if (traceEnabled) {
			logger.trace("Writting to channel in {}", session);
		}
		
		Queue<DatagramRecord> outQueue = session.getOutQueue();
		DatagramRecord record;
		DatagramChannel channel = (DatagramChannel) key.channel();
		boolean isConnected = channel.isConnected();
		
		try {
			while (spinCount > 0 && (record = outQueue.peek()) != null) {
				long length = record.holder.remaining();
				ByteBuffer[] buffers = record.holder.toArray();
				
				if (isConnected) {
					if (buffers.length == 1) {
						bytes = channel.write(buffers[0]);
					}
					else {
						bytes = channel.write(buffers);
					}
				}
				else if (record.address != null) {
					if (buffers.length == 1) {
						bytes = channel.send(buffers[0], record.address);
					}
					else {
						ByteBuffer newBuffer = session.allocate((int)length);
						
						for (ByteBuffer buffer: buffers) {
							newBuffer.put(buffer);
							if (record.release) {
								session.release(buffer);
							}
						}
						newBuffer.flip();
						record.holder = new SingleByteBufferHolder(newBuffer);
						record.release = true;
						bytes = channel.send(newBuffer, record.address);
					}
				}
				else {
					throw new NotYetConnectedException();
				}
				
				if (bytes == length) {
					if (traceEnabled) {
						logger.trace("{} byte(s) written to channel in {}", bytes, session);
					}
					outQueue.poll();
					totalBytes += bytes;
					--spinCount;
					if (record.release) {
						for (ByteBuffer buffer: record.holder.toArray()) {
							session.release(buffer);
						}
					}
					if (record.address != null) {
						fireEvent(session, DataEvent.SENT, bytes, record.address);
					}
					else {
						leftBytes += bytes;
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
		catch (Throwable e) {
			exception = e;
		}

		if (leftBytes > 0) {
			fireEvent(session, DataEvent.SENT, leftBytes);
		}
		
		if (exception != null) {
			elogWarnOrError(logger, "Writting to channel in {} failed: {}", session, exception);
			fireException(session, exception);
			spinCount = 0;
		}
		return spinCount;
	}
	
}
