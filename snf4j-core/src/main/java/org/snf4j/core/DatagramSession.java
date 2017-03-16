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
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.core.session.IDatagramSession;

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

	private final int minInBufferCapacity;
	
	private final int maxInBufferCapacity;
	
	private final boolean ignorePossiblyIncomplete;

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

	@Override
	public void write(byte[] datagram) {
		write(null, datagram);
	}

	@Override
	public void write(SocketAddress remoteAddress, byte[] datagram) {
		SelectionKey key = this.key;
		
		if (key != null && key.isValid()) {
			try {
				synchronized (writeLock) {
					key = detectRebuild(key);
					if (closing != ClosingState.NONE) {
						return;
					}
					outQueue.add(new DatagramRecord(remoteAddress, datagram));
					setWriteInterestOps(key);
				}
			}
			catch (Exception e) {
			}
			lazyWakeup();
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
		byte[] datagram;

		DatagramRecord(SocketAddress address, byte[] datagram) {
			this.address = address;
			this.datagram = datagram;
		}
	}
}
