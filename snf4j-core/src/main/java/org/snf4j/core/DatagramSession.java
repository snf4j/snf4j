/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2018 SNF4J contributors
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
	
	ConcurrentLinkedQueue<DatagramRecord> outQueue = new ConcurrentLinkedQueue<DatagramRecord>();

	/** Number of bytes in the queue */
	private long outQueueSize;
	
	private final int minInBufferCapacity;
	
	private final int maxInBufferCapacity;
	
	private final boolean ignorePossiblyIncomplete;
	
	private final boolean canOwnPassedData;

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
		canOwnPassedData = config.canOwnDataPassedToWriteAndSendMethods();
		inBuffer = allocator.allocate(minInBufferCapacity);
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
	
	@Override
	public IDatagramHandler getHandler() {
		return (IDatagramHandler) handler;
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

	private final long write0(DatagramRecord record, byte[] datagram, int offset, int length) {
		if (canOwnPassedData && allocator.usesArray()) {
			record.buffer = ByteBuffer.wrap(datagram, offset, length);
		}
		else {
			ByteBuffer buffer = allocator.allocate(length);
			
			buffer.put(datagram, offset, length).flip();
			record.buffer = buffer;
			record.release = true;
		}
		return write0(record);
	}

	private final long write0(DatagramRecord record, ByteBuffer datagram, int length) {
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
		return write0(record);
	}
	
	@Override
	public IFuture<Void> write(byte[] datagram) {
		return send(null, datagram);
	}

	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, byte[] datagram) {
		if (datagram == null) {
			throw new NullPointerException();
		}
		if (datagram.length == 0) {
			return futuresController.getSuccessfulFuture();
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
	public void sendnf(SocketAddress remoteAddress, byte[] datagram) {
		if (datagram == null) {
			throw new NullPointerException();
		}
		if (datagram.length > 0) {
			write0(new DatagramRecord(remoteAddress), datagram, 0, datagram.length);	
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
			write0(new DatagramRecord(remoteAddress), datagram, offset, length);	
		}
	}

	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, ByteBuffer datagram) {
		if (datagram == null) {
			throw new NullPointerException();
		} else if (datagram.remaining() == 0) {
			return futuresController.getSuccessfulFuture();
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
			write0(new DatagramRecord(remoteAddress), datagram, datagram.remaining());
		}
	}

	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, ByteBuffer datagram, int length) {
		if (datagram == null) {
			throw new NullPointerException();
		} else if (datagram.remaining() < length) {
			throw new IndexOutOfBoundsException();
		} else if (length == 0) {
			return futuresController.getSuccessfulFuture();
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
		} else if (datagram.remaining() < length) {
			throw new IndexOutOfBoundsException();
		} else if (length > 0) {
			write0(new DatagramRecord(remoteAddress), datagram, length);
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
	
	@Override
	public void close() {
		SelectionKey key = this.key;
		
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
	public void quickClose() {
		SelectionKey key = this.key;

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

	/**
	 * This method must be protected by the write lock. 
	 */
	final void consumedBytes(long number) {
		outQueueSize -= number;
	}
	
	@Override
	void event(SessionEvent event) {
		super.event(event);
		if (event == SessionEvent.OPENED) {
			super.event(SessionEvent.READY);
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
				byte[] data = new byte[inBuffer.remaining()];
				inBuffer.get(data);
				if (remoteAddress == null) {
					handler.read(data);
				}
				else {
					((IDatagramHandler)handler).read(remoteAddress, data);
				}
			}
		}
	}
	
	static class DatagramRecord {
		SocketAddress address;
		ByteBuffer buffer;
		boolean release;

		DatagramRecord(SocketAddress address) {
			this.address = address;
		}
		
	}
}
