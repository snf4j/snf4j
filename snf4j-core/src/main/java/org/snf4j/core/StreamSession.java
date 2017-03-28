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
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.snf4j.core.concurrent.IFuture;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.core.session.IStreamSession;
import org.snf4j.core.session.IllegalSessionStateException;
import org.snf4j.core.session.SessionState;

/**
 * The core implementation of the {@link org.snf4j.core.session.IStreamSession
 * IStreamSession} interface.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class StreamSession extends InternalSession implements IStreamSession {
	
	private final static ILogger LOGGER = LoggerFactory.getLogger(StreamSession.class);
	
	private ByteBuffer inBuffer;
	
	private ByteBuffer[] outBuffers;
	
	/** Number of bytes in outBuffers */
	private long outBuffersSize;
	
	private volatile boolean isEOS;
	
	private final int minInBufferCapacity;
	
	private final int maxInBufferCapacity;
	
	private final int minOutBufferCapacity;
	
	/**
	 * Constructs a named stream-oriented session associated with a handler.
	 * 
	 * @param name
	 *            the name for this session, or <code>null</code> if the
	 *            handler's name should be used for this session's name
	 * @param handler
	 *            the handler that should be associated with this session
	 */
	public StreamSession(String name, IStreamHandler handler) {
		super(name, handler, LOGGER);
		minInBufferCapacity = config.getMinInBufferCapacity();
		maxInBufferCapacity = config.getMaxInBufferCapacity();
		minOutBufferCapacity = config.getMinOutBufferCapacity();
		inBuffer = allocator.allocate(minInBufferCapacity);
		outBuffers = new ByteBuffer[] {allocator.allocate(minOutBufferCapacity)};
	}

	/**
	 * Constructs a stream-oriented session associated with a handler. 
	 * 
	 * @param handler
	 *            the handler that should be associated with this session
	 */
	public StreamSession(IStreamHandler handler) {
		this(null, handler);
	}
	
	@Override
	public IStreamHandler getHandler() {
		return (IStreamHandler) handler;
	}
	
	@Override
	public IFuture<Void> write(byte[] data) {
		SelectionKey key = checkKey(this.key);
		long futureExpectedLen;
		
		synchronized (writeLock) {
			if (closing != ClosingState.NONE) {
				return futures.getCancelledFuture();
			}
			int lastIndex = outBuffers.length - 1;
			ByteBuffer lastBuffer = outBuffers[lastIndex];
			int lastRemaining = lastBuffer.remaining();

			if (lastRemaining >= data.length) {
				lastBuffer.put(data);
			}
			else {
				lastBuffer.put(data, 0, lastRemaining).flip();
				ByteBuffer[] newBuffers = new ByteBuffer[lastIndex+2];
				System.arraycopy(outBuffers, 0, newBuffers, 0, outBuffers.length);
				ByteBuffer newBuffer = allocator.allocate(Math.max(minOutBufferCapacity, data.length - lastRemaining));
				newBuffer.put(data, lastRemaining, data.length - lastRemaining);
				newBuffers[lastIndex+1] = newBuffer;
				outBuffers = newBuffers;
			}
			outBuffersSize += data.length;
			futureExpectedLen = outBuffersSize + getWrittenBytes();  

			try {
				setWriteInterestOps(detectRebuild(key));
			}
			catch (CancelledKeyException e) {
				throw new IllegalSessionStateException(SessionState.CLOSING);
			}
		}
		lazyWakeup();
		return futures.getWriteFuture(futureExpectedLen);
	}

	@Override
	public void close() {
		close(false);
	}
	
	void close(boolean isEos) {
		SelectionKey key = this.key;
		
		if (key != null && key.isValid()) {
			try {
				synchronized (writeLock) {
					key = detectRebuild(key);
					if (closing == ClosingState.NONE) {
						int ops = key.interestOps();
						
						this.isEOS = isEos;
						if ((ops & SelectionKey.OP_WRITE) != 0) {
							//To enable gentle close OP_READ must be set 
							if ((ops & SelectionKey.OP_READ) == 0) {
								key.interestOps(ops | SelectionKey.OP_READ);
								lazyWakeup();
							}
							closing = ClosingState.SENDING;
						}
						else {
							if (isEos) {
								//Executed in the selector loop thread, so we can skip sending events now
								closing = ClosingState.FINISHED;
								key.channel().close();
							}
							else {
								//To enable gentle close OP_READ must be set 
								if ((ops & SelectionKey.OP_READ) == 0) {
									key.interestOps(ops | SelectionKey.OP_READ);
									lazyWakeup();
								}
								closing = ClosingState.FINISHING;
								((SocketChannel)key.channel()).socket().shutdownOutput();
							}
						}
					}
					else if (isEos) {
						closing = ClosingState.FINISHED;
						key.channel().close();
					}
				}
			} catch (Exception e) {
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
					key.channel().close();
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

		if (key != null) {
			loop.finishInvalidatedKey(key);
		}
	}
	
	/**
	 * Gets the buffer into which received data should be written. 
	 * @return buffer in the write mode (i.e. not flipped yet).
	 */
	ByteBuffer getInBuffer() {
		inBuffer = allocator.assure(inBuffer, minInBufferCapacity, maxInBufferCapacity);
		return inBuffer;
	}

	/**
	 * Gets the buffers with data ready to be sent. 
	 * Any modification done to the returned buffers should be executed 
	 * inside synchronized block on the object returned from the method 
	 * getOutBuffersLock. After modification is done the method compactOutBuffers
	 * should be executed inside the same synchronized block to compact the buffers.
	 * @return buffers in the read mode (i.e. flipped).
	 * @see getOutBuffersLock
	 */
	ByteBuffer[] getOutBuffers() {
		//flip only the last one as other ones are already flipped
		outBuffers[outBuffers.length - 1].flip();
		return outBuffers;
	}

	/**
	 * Informs that input buffer has new data that may be ready to consume.
	 */
	void consumeInBuffer() {
		boolean hasArray = inBuffer.hasArray();
		int toRead;
		byte[] array;
		int arrayOff;
		IStreamHandler handler = (IStreamHandler) this.handler;
		
		if (hasArray) {
			array = inBuffer.array();
			arrayOff = inBuffer.arrayOffset();
			toRead = ((IStreamHandler)handler).toRead(array, arrayOff, inBuffer.position());
		}
		else {
			array = null;
			arrayOff = 0;
			toRead = handler.toRead(inBuffer, false);
		}
		
		if (toRead > 0) {
			byte[] data = new byte[toRead];

			inBuffer.flip();
			inBuffer.get(data);
			handler.read(data);
			
			if (hasArray) {
				while ((toRead = handler.toRead(array, arrayOff + inBuffer.position(), inBuffer.remaining())) > 0) {
					data = new byte[toRead];
					inBuffer.get(data);
					handler.read(data);
				}
			}
			else {
				while ((toRead = handler.toRead(inBuffer, true)) > 0) {
					data = new byte[toRead];
					inBuffer.get(data);
					handler.read(data);
				}
			}

			if (inBuffer.hasRemaining()) {
				inBuffer.compact();
			}
			else {
				inBuffer.clear();
			}
		}
	}

	/**
	 * Handles closing operation being in progress. It should be executed only
	 * when the output buffers have no more data after compacting. It should be executed inside 
	 * the same synchronized block as the compacting method
	 * @see compactOutBuffers
	 */
	void handleClosingInProgress() {
		if (closing == ClosingState.SENDING) {
			try {
				if (isEOS) {
					closing = ClosingState.FINISHED;
					key.channel().close();
				}
				else {
					closing = ClosingState.FINISHING;
					((SocketChannel)key.channel()).socket().shutdownOutput();
				}
			} catch (Exception e) {
			}
		}
	}
	
	/**
	 * Informs that some data was removed from output buffers. 
	 * It should be executed inside synchronized block on the object returned 
	 * from the method getOutBuffersLock
	 * @param consumedBytes number of bytes consumed from the buffers
	 * @return true if there is no data left in the buffers
	 * @see getOutBuffersLock 
	 */
	boolean compactOutBuffers(long consumedBytes) {
		int lastIndex = outBuffers.length - 1;
		boolean fullyCompacted = true;
		
		outBuffersSize -= consumedBytes;
		if (lastIndex > 0) {
			int count = 0;
			for (; count<lastIndex; ++count) {
				if (outBuffers[count].hasRemaining()) {
					fullyCompacted = false;
					break;
				}
			}
			if (count > 0) {
				ByteBuffer[] newBuffers = new ByteBuffer[outBuffers.length - count];
				System.arraycopy(outBuffers, count, newBuffers, 0, newBuffers.length);
				outBuffers = newBuffers;
				lastIndex -= count;
			}
		}
		
		//compact last buffer which cannot be left flipped
		ByteBuffer lastBuffer = outBuffers[lastIndex];
		
		if (lastBuffer.hasRemaining()) {
			lastBuffer.compact();
			return false;
		}
		lastBuffer.clear();
		outBuffers[lastIndex] = allocator.reduce(lastBuffer, minOutBufferCapacity);
		return fullyCompacted;
	}

	private final Socket getSocket() {
		SelectableChannel channel = this.channel;
		
		if (channel instanceof SocketChannel && channel.isOpen()) {
			return ((SocketChannel)channel).socket();
		}
		return null;
	}
	
	@Override
	public SocketAddress getLocalAddress() {
		Socket socket = getSocket();
		return socket != null ? socket.getLocalSocketAddress() : null;
	}

	@Override
	public SocketAddress getRemoteAddress() {
		Socket socket = getSocket();
		return socket != null ? socket.getRemoteSocketAddress() : null;
	}
}
