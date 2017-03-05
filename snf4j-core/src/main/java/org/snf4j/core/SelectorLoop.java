/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017 SNF4J contributors
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
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Queue;

import org.snf4j.core.DatagramSession.DatagramRecord;
import org.snf4j.core.factory.IStreamSessionFactory;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.core.pool.ISelectorLoopPool;

/**
 * A selector loop responsible for processing I/O operations of stream-oriented and datagram-oriented connections 
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
	 *            the name for this selector loop, or <code>null</code> if the name should 
	 *            be auto generated
	 * @param parentPool
	 *            the parent pool that owns this selector loop
	 * @throws IOException
	 *             if the {@link java.nio.channels.Selector Selector} associated with this
	 *             selector loop could not be opened
	 */
	public SelectorLoop(String name, ISelectorLoopPool parentPool) throws IOException {
		super(name, LOGGER);
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
		this(name, null);
	}

	/**
	 * Constructs a unnamed selector loop. Its name will be auto generated.
	 * 
	 * @throws IOException
	 *             if the {@link java.nio.channels.Selector Selector} associated with this
	 *             selector loop could not be opened
	 */
	public SelectorLoop() throws IOException {
		this(null, null);
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
	 * @param ops
	 *            the interest set that will be passed to the
	 *            {@link java.nio.channels.SocketChannel SocketChannel#register}
	 *            method when the registration will occur
	 * @param handler
	 *            the handler that will be associated with the channel
	 * @throws SelectorLoopStoppingException
	 *             if selector loop is in the process of stopping
	 * @throws ClosedSelectorException
	 *             if the internal selector is closed
	 * @throws IllegalArgumentException
	 *             if a bit in ops does not correspond to an operation that is
	 *             supported by the channel
	 */
	public void register(SocketChannel channel, int ops, IStreamHandler handler) {
		super.register(channel, ops, new StreamSession(handler));
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
	 * @param ops
	 *            the interest set that will be passed to the
	 *            {@link java.nio.channels.SocketChannel SocketChannel#register}
	 *            method when the registration will occur
	 * @param session
	 *            the session that will be associated with the channel
	 * @throws SelectorLoopStoppingException
	 *             if selector loop is in the process of stopping
	 * @throws ClosedSelectorException
	 *             if the internal selector is closed
	 * @throws IllegalArgumentException
	 *             if a bit in ops does not correspond to an operation that is
	 *             supported by the channel
	 * @throws IllegalArgumentException
	 *             if the session argument is <code>null</code>
	 */
	public void register(SocketChannel channel, int ops, StreamSession session) {
		if (session == null) throw new IllegalArgumentException("session is null");
		super.register(channel, ops, session);
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
	 * @param ops
	 *            the interest set that will be passed to the
	 *            {@link java.nio.channels.DatagramChannel DatagramChannel#register}
	 *            method when the registration will occur
	 * @param handler
	 *            the handler that will be associated with the channel
	 * @throws SelectorLoopStoppingException
	 *             if selector loop is in the process of stopping
	 * @throws ClosedSelectorException
	 *             if the internal selector is closed
	 * @throws IllegalArgumentException
	 *             if a bit in ops does not correspond to an operation that is
	 *             supported by the channel
	 */
	public void register(DatagramChannel channel, int ops, IDatagramHandler handler) {
		super.register(channel, ops, new DatagramSession(handler));
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
	 * @param ops
	 *            the interest set that will be passed to the
	 *            {@link java.nio.channels.DatagramChannel DatagramChannel#register}
	 *            method when the registration will occur
	 * @param session
	 *            the session that will be associated with the channel
	 * @throws SelectorLoopStoppingException
	 *             if selector loop is in the process of stopping
	 * @throws ClosedSelectorException
	 *             if the internal selector is closed
	 * @throws IllegalArgumentException
	 *             if a bit in ops does not correspond to an operation that is
	 *             supported by the channel
	 * @throws IllegalArgumentException
	 *             if the session argument is <code>null</code>
	 */
	public void register(DatagramChannel channel, int ops, DatagramSession session) {
		if (session == null) throw new IllegalArgumentException("session is null");
		super.register(channel, ops, session);
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
	 * @param ops
	 *            the interest set that will be passed to the
	 *            {@link java.nio.channels.ServerSocketChannel
	 *            ServerSocketChannel#register} method when the registration
	 *            will occur
	 * @param factory
	 *            the factory that will be associated with the channel. It will
	 *            be used to create sessions for newly accepted channels
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
	public void register(ServerSocketChannel channel, int ops, IStreamSessionFactory factory) {
		if (factory == null) throw new IllegalArgumentException("factory is null");
		super.register(channel, ops, factory);
	}
	
	@Override
	void handleRegisteredKey(SelectionKey key, SelectableChannel channel, InternalSession session) {
		if (session instanceof StreamSession) {
			StreamSession ssession = (StreamSession)session; 

			if (ssession.isMoving()) {
				ssession.setMoving(false);
				session.setSelectionKey(key);
				session.setLoop(this);
				if (debugEnabled) {
					logger.debug("Channel {} associated with {}", toString(channel), session);
				}
				fireEvent(session, SessionEvent.OPENED);
			}
		}
		else {
			session.setChannel(channel);
			fireEvent(session, SessionEvent.CREATED);
			session.setSelectionKey(key);
			session.setLoop(this);
			fireEvent(session, SessionEvent.OPENED);
		}
	}

	@Override
	void handleSelectedKey(SelectionKey key) {
		Object attachment = key.attachment();
		
		if (attachment instanceof StreamSession) {
			StreamSession session = (StreamSession)attachment;
			
			if (key.isValid() && key.isConnectable()) {
				handleConnecting(session, key);
			}
			if (key.isValid() && key.isReadable()) {
				handleReading(session, key);
			}
			if (key.isValid() && key.isWritable()) {
				handleWriting(session, key);
			}	
		}
		else if (attachment instanceof DatagramSession) {
			DatagramSession session = (DatagramSession)attachment;
			
			if (key.isValid() && key.isReadable()) {
				handleReading(session, key);
			}
			if (key.isValid() && key.isWritable()) {
				handleWriting(session, key);
			}
		}
		else if (key.isValid() && key.isAcceptable()) {
			handleAccepting((IStreamSessionFactory)key.attachment(), key);
		}
	}
	
	@Override
	void sizeChanged(int newSize, int prevSize) {
		ISelectorLoopPool parentPool = this.parentPool;
		
		if (parentPool != null) {
			parentPool.update(this, newSize, prevSize);
		}
	}
	
	@Override
	boolean trackSizeChanges() {
		return parentPool != null;
	}
	
	private final void handleAccepting(final IStreamSessionFactory factory, final SelectionKey key) {
		SocketChannel channel;
		boolean opened = false;

		if (debugEnabled) {
			logger.debug("Accepting from channel {}", toString(key.channel()));
		}
		
		try {
			channel = ((ServerSocketChannel)key.channel()).accept();
			if (!controller.processAccepted(channel)) {
				channel.close();
				channel = null;
			}
			if (debugEnabled) {
				logger.debug("Accepted channel {}", toString(channel));
			}
		}
		catch (Exception e) {
			elogWarnOrError(logger, "Accepting from channel {} failed: {}", toString(key.channel()), e);
			channel = null;
		}
		
		if (channel != null) {
			StreamSession session = factory.create(channel);
			session.setChannel(channel);
			fireEvent(session, SessionEvent.CREATED);
			try {
				ISelectorLoopPool pool = this.pool;
				SelectorLoop loop = pool != null ? pool.getLoop(channel) : null;
				
				channel.configureBlocking(false);
				if (loop != null) {
					if (debugEnabled) {
						logger.debug("Moving registration of channel {} to other selector loop {}", toString(channel), loop);
					}
					session.setMoving(true);
					loop.register(channel, SelectionKey.OP_READ, session);
					return;
				}
				else {
					session.setSelectionKey(channel.register(selector, SelectionKey.OP_READ, session));
					session.setLoop(this);
				}
				opened = true;
			}
			catch (Exception e) {
				elogger.error(logger, "Unable to reqister channel {} with selector: {}", toString(channel), e);
				fireException(session, e);
			}
			
			if (opened) {
				if (debugEnabled) {
					logger.debug("Channel {} is associated with {}", toString(channel), session);
				}
				fireEvent(session, SessionEvent.OPENED);
			}
			else {
				try {
					channel.close();
				} catch (IOException e) {
					//Ignore
				}
				
				if (!channel.isRegistered()) {
					fireEndingEvent(session);
				}
			}
		}
	}
	
	private final void handleConnecting(final StreamSession session, final SelectionKey key) {
		if (debugEnabled) {
			logger.debug("Finishing connection of channel {}", toString(key.channel()));
		}

		session.setChannel((SocketChannel)key.channel());
		fireEvent(session, SessionEvent.CREATED);
		boolean finished;
		
		try {
			if (controller.processConnection((SocketChannel)key.channel())) {
				finished = ((SocketChannel)key.channel()).finishConnect();
			}
			else {
				key.channel().close();
				finished = false;
			}
		}
		catch (Exception e) {
			elogWarnOrError(logger, "Finishing connection of channel {} failed: {}", toString(key.channel()), e);
			fireException(session, e);
			finished = false;
		}
		
		if (finished) {
			if (debugEnabled) {
				logger.debug("Channel {} associated with {}", toString(key.channel()), session);
			}
			key.interestOps(SelectionKey.OP_READ);
			session.setSelectionKey(key);
			session.setLoop(this);
			fireEvent(session, SessionEvent.OPENED);
		}
	}
	
	private final void handleWriting(final StreamSession session, final SelectionKey key) {
		long bytes;

		if (traceEnabled) {
			logger.trace("Writting to channel in {}", session);
		}

		try {
			synchronized (session.getWriteLock()) {
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
					bytes = 0;
					if (session.compactOutBuffers()) {
						session.clearWriteInterestOps(key);
						session.handleClosingInProgress();
					}
				}
				else {
					bytes = ((SocketChannel)key.channel()).write(b);
					if (bytes > 0) {
						long currentTime = System.currentTimeMillis();

						if (traceEnabled) {
							logger.trace("{} byte(s) written to channel in {}", bytes, session);
						}
						session.calculateThroughput(currentTime, false);
						session.incWrittenBytes(bytes, currentTime);
						if (session.compactOutBuffers()) {
							session.clearWriteInterestOps(key);
							session.handleClosingInProgress();
						}
					}
				}
			}
		}
		catch (Exception e) {
			elogWarnOrError(logger, "Writting to chennel in {} failed: {}", session, e);
			fireException(session, e);
			bytes = 0;
		}
		
		if (bytes > 0) {
			fireEvent(session, DataEvent.SENT, bytes);
		}
	}
	
	private final void handleReading(final StreamSession session, final SelectionKey key) {
		int bytes;

		if (traceEnabled) {
			logger.trace("Reading from channel in {}", session);
		}
		
		try {
			bytes = ((SocketChannel)key.channel()).read(session.getInBuffer());
		}
		catch (Exception e) {
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
			session.close(true);
		}
	}
	
	private final void handleReading(final DatagramSession session, final SelectionKey key) {
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
		catch (Exception e) {
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
			fireEvent(session, DataEvent.RECEIVED, bytes);
			session.consumeInBuffer(remoteAddress);
		}
	}
	
	private final void handleWriting(final DatagramSession session, final SelectionKey key) {
		long totalBytes = 0;
		int bytes;
		
		if (traceEnabled) {
			logger.trace("Writting to channel in {}", session);
		}
		
		Queue<DatagramRecord> outQueue = session.getOutQueue();
		DatagramRecord record;
		DatagramChannel channel = (DatagramChannel) key.channel();
		boolean isConnected = channel.isConnected();
		
		try {
			while ((record = outQueue.peek()) != null) {
				if (isConnected) {
					bytes = channel.write(ByteBuffer.wrap(record.datagram));
				}
				else if (record.address != null) {
					bytes = channel.send(ByteBuffer.wrap(record.datagram), record.address);
				}
				else {
					logger.error("No remote address for not connected channel in {}", session);
					bytes = 0;
				}
				
				if (bytes == record.datagram.length) {
					long currentTime = System.currentTimeMillis();

					if (traceEnabled) {
						logger.trace("{} byte(s) written to channel in {}", bytes, session);
					}
					session.calculateThroughput(currentTime, false);
					session.incWrittenBytes(bytes, currentTime);
					outQueue.poll();
					totalBytes += bytes;
				}
				else {
					break;
				}
			}
			
			synchronized (session.getWriteLock()) {
				if (outQueue.isEmpty()) {
					session.clearWriteInterestOps(key);
					session.handleClosingInProgress();
				}
			}
		}
		catch (Exception e) {
			elogWarnOrError(logger, "Writting to chennel in {} failed: {}", session, e);
			fireException(session, e);
		}

		if (totalBytes > 0) {
			fireEvent(session, DataEvent.SENT, totalBytes);
		}
	}
	
}