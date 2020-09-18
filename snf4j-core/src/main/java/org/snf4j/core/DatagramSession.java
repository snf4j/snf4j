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
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.core.session.IDatagramSession;
import org.snf4j.core.session.IllegalSessionStateException;
import org.snf4j.core.session.SessionState;

/**
 * The core implementation of the {@link org.snf4j.core.session.IDatagramSession
 * IDatagramSession} interface.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class DatagramSession extends InternalSession implements IDatagramSession {
	
	private final static ILogger LOGGER = LoggerFactory.getLogger(DatagramSession.class);
	
	private ByteBuffer inBuffer;
	
	private ConcurrentLinkedQueue<DatagramRecord> outQueue;

	/** Number of bytes in the queue */
	private long outQueueSize;
	
	private final int minInBufferCapacity;
	
	private final int maxInBufferCapacity;
	
	private final boolean ignorePossiblyIncomplete;
	
	IEncodeTaskWriter encodeTaskWriter;

	/**
	 * Constructs a named datagram-oriented session associated with a handler.
	 * 
	 * @param name
	 *            the name for this session, or <code>null</code> if the
	 *            handler's name should be used for this session's name
	 * @param handler
	 *            the handler that should be associated with this session
	 */	
	public DatagramSession(String name, IDatagramHandler handler) {
		super(name, handler, LOGGER);
		minInBufferCapacity = config.getMinInBufferCapacity();
		maxInBufferCapacity = config.getMaxInBufferCapacity();
		ignorePossiblyIncomplete = config.ignorePossiblyIncompleteDatagrams();
	}

	IEncodeTaskWriter getEncodeTaskWriter() {
		if (encodeTaskWriter == null) {
			encodeTaskWriter = new EncodeTaskWriter();
		}
		return encodeTaskWriter;
	}
	
	/**
	 * Constructs a datagram-oriented session associated with a handler. 
	 * 
	 * @param handler
	 *            the handler that should be associated with this session
	 */
	public DatagramSession(IDatagramHandler handler) {
		this(null, handler);
	}
	
	private final DatagramSocket getSocket() {
		SelectableChannel channel = this.channel;
		
		if (channel instanceof DatagramChannel && channel.isOpen()) {
			return ((DatagramChannel)channel).socket();
		}
		return null;
	}

	void event(SocketAddress remoteAddress, DataEvent event, long length) {
		if (isValid(event.type())) {
			futuresController.event(event, length);
			try {
				getHandler().event(remoteAddress, event, length);
			}
			catch (Exception e) {
				elogger.error(logger, "Failed event {} for {}: {}", event, this, e);
			}
		}
	}
	
	@Override
	public IDatagramHandler getHandler() {
		return (IDatagramHandler) handler;
	}
	
	@Override
	public IDatagramSession getParent() {
		return null;
	}
	
	@Override
	public SocketAddress getLocalAddress() {
		DatagramSocket socket = getSocket();
		return socket != null ? socket.getLocalSocketAddress() : null;
	}

	@Override
	public SocketAddress getRemoteAddress() {
		DatagramSocket socket = getSocket();
		return socket != null ? socket.getRemoteSocketAddress() : null;
	}

	long superWrite(DatagramRecord record) {
		return write0(record);
	}
	
	IFuture<Void> superWrite(DatagramRecord record, boolean withFuture) {
		if (withFuture) {
			return write1(record);
		}
		write0(record);
		return null;
	}
	
	private final long write0(DatagramRecord record) {
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

	final DatagramRecord initRecord(DatagramRecord record, byte[] datagram, int offset, int length) {
		if (canOwnPassedData && allocator.usesArray()) {
			record.buffer = ByteBuffer.wrap(datagram, offset, length);
		}
		else {
			ByteBuffer buffer = allocator.allocate(length);
			
			buffer.put(datagram, offset, length).flip();
			record.buffer = buffer;
			record.release = true;
		}
		return record;
	}
	
	final DatagramRecord initRecord(DatagramRecord record, ByteBuffer datagram, int length) {
		boolean allRemaining = length == datagram.remaining();
		
		if (canOwnPassedData && allRemaining) {
			record.buffer = datagram;
		}
		else {
			ByteBuffer buffer = allocator.allocate(length);

			if (allRemaining) {
				buffer.put(datagram).flip();
			}
			else {
				ByteBuffer dup = datagram.duplicate();
				
				dup.limit(dup.position()+length);
				buffer.put(dup).flip();
				datagram.position(dup.position());
			}
			record.buffer = buffer;
			record.release = true;
		}
		return record;
	}
	
	private final long write0(DatagramRecord record, byte[] datagram, int offset, int length) {
		return write0(initRecord(record, datagram, offset, length));
	}

	private final long write0(DatagramRecord record, ByteBuffer datagram, int length) {
		return write0(initRecord(record, datagram, length));
	}

	private IFuture<Void> write1(DatagramRecord record) {
		long futureExpectedLen = write0(record);
		
		if (futureExpectedLen == -1) {
			return futuresController.getCancelledFuture();
		}
		return futuresController.getWriteFuture(futureExpectedLen);
	}
	
	@Override
	public IFuture<Void> write(byte[] datagram) {
		return send(null, datagram);
	}

	IFuture<Void> simpleSend(SocketAddress remoteAddress, byte[] datagram, boolean withFuture) {
		if (codec != null) {
			EncodeTask task = EncodeTask.simple(this, datagram);
			
			if (withFuture) {
				return task.register(remoteAddress);
			}
			task.registernf(remoteAddress);
			return null;
		}
		
		DatagramRecord record = new DatagramRecord(remoteAddress);
		long futureExpectedLen;
		
		record.buffer = ByteBuffer.wrap(datagram);
		futureExpectedLen = write0(record);
		if (withFuture) {
			if (futureExpectedLen == -1) {
				return futuresController.getCancelledFuture();
			}
			return futuresController.getWriteFuture(futureExpectedLen);
		}
		return null;
	}
	
	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, byte[] datagram) {
		if (datagram == null) {
			throw new NullPointerException();
		}
		if (datagram.length == 0) {
			return futuresController.getSuccessfulFuture();
		}
		if (codec != null) {
			return new EncodeTask(this, datagram).register(remoteAddress);
		}
		
		long futureExpectedLen = write0(new DatagramRecord(remoteAddress), datagram, 0, datagram.length);	

		if (futureExpectedLen == -1) {
			return futuresController.getCancelledFuture();
		}
		return futuresController.getWriteFuture(futureExpectedLen);
	}

	@Override
	public void writenf(byte[] datagram) {
		sendnf(null, datagram);
	}

	@Override
	public IFuture<Void> write(byte[] datagram, int offset, int length) {
		return send(null, datagram, offset, length);
	}

	@Override
	public void writenf(byte[] datagram, int offset, int length) {
		sendnf(null, datagram, offset, length);
	}

	@Override
	public IFuture<Void> write(ByteBuffer datagram) {
		return send(null, datagram);
	}

	@Override
	public void writenf(ByteBuffer datagram) {
		sendnf(null, datagram);
	}

	@Override
	public IFuture<Void> write(ByteBuffer datagram, int length) {
		return send(null, datagram, length);
	}

	@Override
	public void writenf(ByteBuffer datagram, int length) {
		sendnf(null, datagram, length);
	}

	@Override
	public IFuture<Void> write(Object msg) {
		return send(null, msg);
	}

	@Override
	public void writenf(Object msg) {
		sendnf(null, msg);
	}
	
	@Override
	public void sendnf(SocketAddress remoteAddress, byte[] datagram) {
		if (datagram == null) {
			throw new NullPointerException();
		}
		if (datagram.length > 0) {
			if (codec != null) {
				new EncodeTask(this, datagram).registernf(remoteAddress);
			}
			else {
				write0(new DatagramRecord(remoteAddress), datagram, 0, datagram.length);
			}
		}
	}

	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, byte[] datagram, int offset, int length) {
		if (datagram == null) {
			throw new NullPointerException();
		}
		checkBounds(offset, length, datagram.length);
		if (length == 0) {
			return futuresController.getSuccessfulFuture();
		}
		if (codec != null) {
			return new EncodeTask(this, datagram, offset, length).register(remoteAddress);
		}
		
		long futureExpectedLen = write0(new DatagramRecord(remoteAddress), datagram, offset, length);	

		if (futureExpectedLen == -1) {
			return futuresController.getCancelledFuture();
		}
		return futuresController.getWriteFuture(futureExpectedLen);
	}

	@Override
	public void sendnf(SocketAddress remoteAddress, byte[] datagram, int offset, int length) {
		if (datagram == null) {
			throw new NullPointerException();
		}
		checkBounds(offset, length, datagram.length);

		if (length > 0) {
			if (codec != null) {
				new EncodeTask(this, datagram, offset, length).registernf(remoteAddress);
			}
			else {
				write0(new DatagramRecord(remoteAddress), datagram, offset, length);
			}
		}
	}

	IFuture<Void> simpleSend(SocketAddress remoteAddress, ByteBuffer datagram, boolean withFuture) {
		if (codec != null) {
			EncodeTask task = EncodeTask.simple(this, datagram);
			
			if (withFuture) {
				return task.register(remoteAddress);
			}
			task.registernf(remoteAddress);
			return null;
		}
		DatagramRecord record = new DatagramRecord(remoteAddress);
		long futureExpectedLen;
		
		record.buffer = datagram;
		futureExpectedLen = write0(record);
		if (withFuture) {
			if (futureExpectedLen == -1) {
				return futuresController.getCancelledFuture();
			}
			return futuresController.getWriteFuture(futureExpectedLen);
		}
		return null;
	}
	
	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, ByteBuffer datagram) {
		if (datagram == null) {
			throw new NullPointerException();
		} else if (datagram.remaining() == 0) {
			return futuresController.getSuccessfulFuture();
		}		
		if (codec != null) {
			return new EncodeTask(this, datagram).register(remoteAddress);
		}
		
		long futureExpectedLen = write0(new DatagramRecord(remoteAddress), datagram, datagram.remaining());
		
		if (futureExpectedLen == -1) {
			return futuresController.getCancelledFuture();
		}
		return futuresController.getWriteFuture(futureExpectedLen);
	}

	@Override
	public void sendnf(SocketAddress remoteAddress, ByteBuffer datagram) {
		if (datagram == null) {
			throw new NullPointerException();
		} else if (datagram.remaining() > 0) {
			if (codec != null) {
				new EncodeTask(this, datagram).registernf(remoteAddress);
			}
			else {
				write0(new DatagramRecord(remoteAddress), datagram, datagram.remaining());
			}
		}
	}

	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, ByteBuffer datagram, int length) {
		if (datagram == null) {
			throw new NullPointerException();
		} else if (datagram.remaining() < length || length < 0) {
			throw new IndexOutOfBoundsException();
		} else if (length == 0) {
			return futuresController.getSuccessfulFuture();
		}		
		if (codec != null) {
			return new EncodeTask(this, datagram, length).register(remoteAddress);
		}

		long futureExpectedLen = write0(new DatagramRecord(remoteAddress), datagram, length);
		
		if (futureExpectedLen == -1) {
			return futuresController.getCancelledFuture();
		}
		return futuresController.getWriteFuture(futureExpectedLen);
	}

	@Override
	public void sendnf(SocketAddress remoteAddress, ByteBuffer datagram, int length) {
		if (datagram == null) {
			throw new NullPointerException();
		} else if (datagram.remaining() < length || length < 0) {
			throw new IndexOutOfBoundsException();
		} else if (length > 0) {
			if (codec != null) {
				new EncodeTask(this, datagram, length).registernf(remoteAddress);
			}
			else {
				write0(new DatagramRecord(remoteAddress), datagram, length);
			}
		}
	}

	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, Object msg) {
		if (msg == null) {
			throw new NullPointerException();
		}
		if (codec != null) {
			return new EncodeTask(this, msg).register(remoteAddress);
		}
		if (msg.getClass() == byte[].class) {
			return send(remoteAddress, (byte[])msg);
		}
		if (msg instanceof ByteBuffer) {
			return send(remoteAddress, (ByteBuffer)msg);
		}
		throw new IllegalArgumentException("msg is an unexpected object");
	}
	
	@Override
	public void sendnf(SocketAddress remoteAddress, Object msg) {
		if (msg == null) {
			throw new NullPointerException();
		}
		if (codec != null) {
			new EncodeTask(this, msg).registernf(remoteAddress);
		}
		else if (msg.getClass() == byte[].class) {
			sendnf(remoteAddress,(byte[])msg);
		}
		else if (msg instanceof ByteBuffer) {
			sendnf(remoteAddress,(ByteBuffer)msg);
		}
		else {
			throw new IllegalArgumentException("msg is an unexpected object");
		}
	}
	
	private final void close(SelectionKey key) throws IOException {
		close(key.channel());
		loop.finishInvalidatedKey(key);
	}

	@Override
	void close(SelectableChannel channel) throws IOException {
		if (channel instanceof DatagramChannel) {
			((DatagramChannel)channel).disconnect();
		}
		channel.close();
	}
	
	private void close0() {
		SelectionKey key = this.key;
		closeCalled.set(true);
		
		if (key != null && key.isValid()) {
			try {
				synchronized (writeLock) {
					key = detectRebuild(key);
					if (closing == ClosingState.NONE) {
						if ((key.interestOps() & SelectionKey.OP_WRITE) != 0) {
							closing = ClosingState.SENDING;
						}
						else {
							closing = ClosingState.FINISHED;
							close(key);
						}
					}
				}
			}
			catch (Exception e) {
			}
		}
		else {
			quickClose();
		}
	}
	
	@Override
	public void close() {
		close0();
	}
	
	private void quickClose0() {
		SelectionKey key = this.key;
		closeCalled.set(true);

		if (key != null && key.isValid()) {
			try {
				synchronized (writeLock) {
					key = detectRebuild(key);
					closing = ClosingState.FINISHED;
					close(key);
				}
			}
			catch (Exception e) {
			}			
		}
		else if (channel != null) {
			try {
				close(channel);
			} catch (IOException e) {
			}
		}
	}	

	@Override
	public void quickClose() {
		quickClose0();
	}	
	
	void superQuickClose() {
		quickClose0();
	}

	void superClose() {
		close0();
	}
	
	@Override
	public void dirtyClose() {
		quickClose();
	}
	
	final void superEvent(SessionEvent event) {
		super.event(event);
	}
	
	/**
	 * This method must be protected by the write lock. 
	 */
	final void consumedBytes(long number) {
		outQueueSize -= number;
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
	
	/**
	 * Handles closing operation being in progress. It should be executed only
	 * when the output queue have no more data after writing.
	 */
	void handleClosingInProgress() {
		if (closing == ClosingState.SENDING) {
			try {
				if (key.channel() instanceof DatagramChannel) {
					((DatagramChannel)key.channel()).disconnect();
				}
				closing = ClosingState.FINISHED;
				key.channel().close();
			}
			catch (IOException e) {
			}
		}
	}
	
	@Override
	void preCreated() {
		inBuffer = allocator.allocate(minInBufferCapacity);
		outQueue = new ConcurrentLinkedQueue<DatagramRecord>();
	}
	
	@Override
	void postEnding() {
		if (allocator.isReleasable()) {
			if (inBuffer != null) {
				allocator.release(inBuffer);
				inBuffer = null;
			}
			DatagramRecord record;
			while ((record = outQueue.poll()) != null) {
				if (record.release) {
					allocator.release(record.buffer);
				}
			}
		}
	}
	
	final Queue<DatagramRecord> getOutQueue() {
		return outQueue;
	}
	
	ByteBuffer getInBuffer() {
		if (inBuffer.position() == inBuffer.capacity()) {
			inBuffer = allocator.extend((ByteBuffer) inBuffer.clear(), maxInBufferCapacity);
			return inBuffer;
		}
		return (ByteBuffer) inBuffer.clear();
	}
	
	void consumeInBuffer(SocketAddress remoteAddress) {
		if (!ignorePossiblyIncomplete || inBuffer.hasRemaining()) {
			inBuffer.flip();
			if (inBuffer.hasRemaining()) {
				IDatagramReader handler = superCodec();
				byte[] data = new byte[inBuffer.remaining()];
				inBuffer.get(data);
				if (remoteAddress == null) {
					handler.read(data);
				}
				else {
					handler.read(remoteAddress, data);
				}
			}
		}
	}
	
	IDatagramReader superCodec() {
		return codec != null ? codec : (IDatagramReader) this.handler;
	}
	
	static class DatagramRecord {
		SocketAddress address;
		ByteBuffer buffer;
		boolean release;

		DatagramRecord(SocketAddress address) {
			this.address = address;
		}
		
	}
	
	private class EncodeTaskWriter implements IEncodeTaskWriter {

		@Override
		public final IFuture<Void> write(SocketAddress remoteAddress, ByteBuffer buffer, boolean withFuture) {
			DatagramRecord record = new DatagramRecord(remoteAddress);
			record.buffer = buffer;
			return write1(record);
		}

		@Override
		public final IFuture<Void> write(SocketAddress remoteAddress, byte[] bytes, boolean withFuture) {
			DatagramRecord record = new DatagramRecord(remoteAddress);
			record.buffer = ByteBuffer.wrap(bytes);
			return write1(record);
		}
		
	}
}
