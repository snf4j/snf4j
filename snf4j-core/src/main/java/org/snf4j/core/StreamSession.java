/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2024 SNF4J contributors
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

import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.core.session.ISessionPipeline;
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
	
	private final static ByteBuffer[] EMPTY_ARRAY = new ByteBuffer[0];
	
	private final ByteBuffer[] DEFAULT_ARRAY = new ByteBuffer[1];
	
	private ByteBuffer inBuffer;
	
	private ByteBuffer[] outBuffers;
	
	/** Number of bytes in outBuffers */
	private long outBuffersSize;
	
	private final int minInBufferCapacity;
	
	private final int maxInBufferCapacity;
	
	private final int minOutBufferCapacity;
	
	IEncodeTaskWriter encodeTaskWriter;
	
	final IConsumeController consumeController = new IConsumeController() {

		@Override
		public boolean skipConsuming() {
			return isSwitching || closeCalled.get();
		}	
	};

	ByteBuffer[] getInBuffersForCopying() {
		if (inBuffer == null || inBuffer.position() == 0) {
			return EMPTY_ARRAY;
		}
		return new ByteBuffer[] {inBuffer};
	}
	
	@Override
	int copyInBuffer(InternalSession oldSession) {
		ByteBuffer[] oldBufs = ((StreamSession)oldSession).getInBuffersForCopying();
		int totalSize = 0;
		
		for (int i=0; i<oldBufs.length; ++i) {
			totalSize += oldBufs[i].position();
		}
		if (totalSize == 0) {
			return 0;
		}
		
		ByteBuffer dup, buf = getInBuffer();
		
		if (maxInBufferCapacity < totalSize) {
			release(buf);
			inBuffer = buf = allocate(totalSize);
		}
		if (buf.remaining() >= totalSize) {
			for (ByteBuffer oldBuf: oldBufs) {
				oldBuf.flip();
				buf.put(oldBuf);
			}
			return buf.position();
		}
		
		for (ByteBuffer oldBuf: oldBufs) {
			oldBuf.flip();
			for (;;) {
				buf = getInBuffer();
				if (buf.remaining() >= oldBuf.remaining()) {
					buf.put(oldBuf);
					break;
				}
				dup = oldBuf.duplicate();
				dup.limit(dup.position() + buf.remaining());
				buf.put(dup);
				oldBuf.position(dup.position());
			}
		}
		return buf.position();
	}

	@Override
	SessionPipeline<?> createPipeline() {
		return new StreamSessionPipeline(this);
	}
	
	@Override
	public ISessionPipeline<IStreamSession> getPipeline() {
		return (StreamSessionPipeline) getPipeline0();
	}
	
	@Override
	public SessionState getState() {
		SelectionKey key = this.key;

		if (key == null) {
			return SessionState.OPENING;
		}
		if (key.isValid()) {
			Object attachement = key.attachment();
			
			if (attachement instanceof ChannelContext) {
				if (((ChannelContext<?>)attachement).context == this) {
					return SessionState.OPEN;
				}
			}
		}
		return SessionState.CLOSING;
	}
	
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
	}

	@Override
	IEncodeTaskWriter getEncodeTaskWriter() {
		if (encodeTaskWriter == null) {
			encodeTaskWriter = new EncodeTaskWriter();
		}
		return encodeTaskWriter;
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
	public IStreamSession getParent() {
		return null;
	}
	
	static ByteBuffer[] clearBuffers(ByteBuffer[] outBuffers, IByteBufferAllocator allocator, boolean optimize) {
		int lastIndex = outBuffers.length - 1;

		if (lastIndex < 0) {
			return EMPTY_ARRAY;
		}
		
		if (lastIndex > 0) {
			if (allocator.isReleasable()) {
				for (int i=0; i<lastIndex; ++i) {
					allocator.release(outBuffers[i]);
				}
			}

			ByteBuffer[] newBuffers = new ByteBuffer[1];
			
			newBuffers[0] = outBuffers[lastIndex];
			lastIndex = 0;
			outBuffers = newBuffers;
		}
		
		ByteBuffer lastBuffer = outBuffers[lastIndex];
		
		if (optimize) {
			allocator.release(lastBuffer);
			return EMPTY_ARRAY;
		}
		lastBuffer.clear();
		return outBuffers;
	}
	
	static ByteBuffer[] compactBuffers(ByteBuffer[] outBuffers, IByteBufferAllocator allocator, int minOutBufferCapacity, boolean optimize) {
		int lastIndex = outBuffers.length - 1;
		
		if (lastIndex > 0) {
			int count = 0;
			for (; count<lastIndex; ++count) {
				if (outBuffers[count].hasRemaining()) {
					break;
				}
			}
			if (count > 0) {
				ByteBuffer[] newBuffers = new ByteBuffer[outBuffers.length - count];
				System.arraycopy(outBuffers, count, newBuffers, 0, newBuffers.length);
				lastIndex -= count;
				if (allocator.isReleasable()) {
					for (--count; count >=0; --count) {
						allocator.release(outBuffers[count]);
					}
				}
				outBuffers = newBuffers;
			}
		}
		
		//compact last buffer which cannot be left flipped
		ByteBuffer lastBuffer = outBuffers[lastIndex];
		
		if (lastBuffer.hasRemaining()) {
			lastBuffer.compact();
			return outBuffers;
		}
		if (optimize && outBuffers.length == 1) {
			allocator.release(lastBuffer);
			return EMPTY_ARRAY;
		}
		lastBuffer.clear();
		outBuffers[lastIndex] = allocator.reduce(lastBuffer, minOutBufferCapacity);
		return outBuffers;
	}
	
	static ByteBuffer[] putToBuffers(ByteBuffer[] outBuffers, IByteBufferAllocator allocator, ByteBuffer data) {
		int lastIndex = outBuffers.length - 1;
		ByteBuffer lastBuffer = outBuffers[lastIndex];
		
		data.position(data.limit());
		data.limit(data.capacity());
		if (lastBuffer == null) {
			outBuffers[lastIndex] = data;
			return outBuffers;			
		}
		if (lastBuffer.position() == 0) {
			allocator.release(lastBuffer);
			outBuffers[lastIndex] = data;
			return outBuffers;
		}
		ByteBuffer[] newBuffers = new ByteBuffer[lastIndex+2];
		System.arraycopy(outBuffers, 0, newBuffers, 0, outBuffers.length);
		lastBuffer.flip();
		newBuffers[lastIndex+1] = data;
		return newBuffers;
	}

	static ByteBuffer[] putToBuffers(ByteBuffer[] outBuffers, IByteBufferAllocator allocator, IByteBufferHolder holder) {
		ByteBuffer[] bufs = holder.toArray();
		
		if (bufs.length == 1) {
			return putToBuffers(outBuffers, allocator, bufs[0]);
		}
		
		int lastIndex = outBuffers.length - 1;
		ByteBuffer lastBuffer = outBuffers[lastIndex];
		int off,inc;

		if (lastBuffer == null) {
			off = lastIndex;
			inc = bufs.length-1;
		}
		else if (lastBuffer.position() == 0) {
			allocator.release(lastBuffer);
			off = lastIndex;
			inc = bufs.length-1;
		}
		else {
			lastBuffer.flip();
			off = lastIndex+1;
			inc = bufs.length;
		}
		
		ByteBuffer[] newBuffers = Arrays.copyOf(outBuffers, outBuffers.length+inc);
		for (int i=0; i<bufs.length; ++i) {
			newBuffers[off++] = bufs[i];
		}
		lastBuffer = newBuffers[off-1];
		lastBuffer.position(lastBuffer.limit());
		lastBuffer.limit(lastBuffer.capacity());
		return newBuffers;
	}

	static ByteBuffer[] putToBuffers(ByteBuffer[] outBuffers, IByteBufferAllocator allocator, int minOutBufferCapacity, Object data, int offset, int length, boolean buffer) {
		int lastIndex = outBuffers.length - 1;
		ByteBuffer lastBuffer = outBuffers[lastIndex];
		int lastRemaining = lastBuffer.remaining();

		if (lastRemaining >= length) {
			if (buffer) {
				ByteBuffer buf = (ByteBuffer)data;
				if (buf.remaining() == length) {
					lastBuffer.put(buf);
				}
				else {
					ByteBuffer dup = buf.duplicate();
					dup.limit(dup.position()+length);
					lastBuffer.put(dup);
					buf.position(dup.position());
				}
			}
			else {
				lastBuffer.put((byte[])data, offset, length);
			}
			return outBuffers;
		}
		else {
			int remaining = length - lastRemaining;
			ByteBuffer[] newBuffers = new ByteBuffer[lastIndex+2];
			System.arraycopy(outBuffers, 0, newBuffers, 0, outBuffers.length);
			ByteBuffer newBuffer = allocator.allocate(Math.max(minOutBufferCapacity, remaining));
			if (buffer) {
				ByteBuffer buf = (ByteBuffer)data;
				ByteBuffer dup = buf.duplicate();
				dup.limit(dup.position() + lastRemaining);
				lastBuffer.put(dup).flip();
				dup.limit(dup.position() + remaining);
				newBuffer.put(dup);
				buf.position(dup.position());
			}
			else {
				lastBuffer.put((byte[])data, offset, lastRemaining).flip();
				newBuffer.put((byte[])data, offset + lastRemaining, remaining);
			}
			newBuffers[lastIndex+1] = newBuffer;
			return newBuffers;
		}
	}

	static ByteBuffer[] putToBuffers(ByteBuffer[] outBuffers, IByteBufferAllocator allocator, int minOutBufferCapacity, IByteBufferHolder holder) {
		for (ByteBuffer buf: holder.toArray()) {
			if (buf.hasRemaining()) {
				outBuffers = putToBuffers(outBuffers, allocator, minOutBufferCapacity, buf, 0, buf.remaining(), true);
			}
		}
		return outBuffers;
	}
	
	/** 
	 * Returns -1 if session is in closing state 
	 */
	private final long write0(Object data, int offset, int length, boolean buffer) {
		SelectionKey key = checkKey(this.key);
		long futureExpectedLen;
		
		synchronized (writeLock) {
			if (closing != ClosingState.NONE) {
				return -1;
			}
			
			boolean optimize = buffer && optimizeBuffers;
			
			if (length == -1) {
				IByteBufferHolder holder = (IByteBufferHolder)data;
				
				if (outBuffers.length == 0) {
					outBuffers = DEFAULT_ARRAY;
					outBuffers[0] = optimize ? null : allocator.allocate(minOutBufferCapacity);
				}
				length = holder.remaining();
				if (optimize) {
					outBuffers = putToBuffers(outBuffers, allocator, holder);
				}
				else {
					outBuffers = putToBuffers(outBuffers, allocator, minOutBufferCapacity, holder);
				}
			}
			else if (optimize && ((ByteBuffer)data).remaining() == length) {
				if (outBuffers.length == 0) {
					outBuffers = DEFAULT_ARRAY;
					outBuffers[0] = null;
				}
				outBuffers = putToBuffers(outBuffers, allocator, (ByteBuffer)data);
			}
			else {
				if (outBuffers.length == 0) {
					outBuffers = DEFAULT_ARRAY;
					outBuffers[0] = allocator.allocate(minOutBufferCapacity);
				}
				outBuffers = putToBuffers(outBuffers, allocator, minOutBufferCapacity,  data, offset, length, buffer);
			}
			outBuffersSize += length;
			futureExpectedLen = outBuffersSize + getWrittenBytes();  

			try {
				setWriteInterestOps(detectRebuild(key));
			}
			catch (CancelledKeyException e) {
				throw new IllegalSessionStateException(SessionState.CLOSING);
			}
		}
		lazyWakeup();
		return futureExpectedLen;
	}

	final long write0(ByteBuffer data) {
		return write0(data, 0, data.remaining(), true);
	}

	final long write0(IByteBufferHolder holder) {
		return write0(holder, 0, -1, true);
	}
	
	final IFuture<Void> write1(byte[] data) {
		long futureExpectedLen = write0(data, 0, data.length, false);
		
		if (futureExpectedLen == -1) {
			return futuresController.getCancelledFuture();
		}
		return futuresController.getWriteFuture(futureExpectedLen);
	}
	
	final IFuture<Void> write1(ByteBuffer data) {
		long futureExpectedLen = write0(data, 0, data.remaining(), true);
		
		if (futureExpectedLen == -1) {
			return futuresController.getCancelledFuture();
		}
		return futuresController.getWriteFuture(futureExpectedLen);
	}

	final IFuture<Void> write1(IByteBufferHolder holder) {
		long futureExpectedLen = write0(holder, 0, -1, true);
		
		if (futureExpectedLen == -1) {
			return futuresController.getCancelledFuture();
		}
		return futuresController.getWriteFuture(futureExpectedLen);
	}
	
	@Override
	public IFuture<Void> write(byte[] data) {
		if (data == null) {
			throw new NullPointerException();
		} else if (data.length == 0) {
			return futuresController.getSuccessfulFuture();
		}
		if (codec != null) {
			return new EncodeTask(this, data).register();
		}
		return write1(data);
	}

	@Override
	public void writenf(byte[] data) {
		if (data == null) {
			throw new NullPointerException();
		} else if (data.length > 0) {
			if (codec != null) {
				new EncodeTask(this, data).registernf();
			}
			else {
				write0(data, 0, data.length, false);
			}
		}
	}
	
	@Override
	public IFuture<Void> write(byte[] data, int offset, int length) {
		if (data == null) {
			throw new NullPointerException();
		}
		checkBounds(offset, length, data.length);
		if (length == 0) {
			return futuresController.getSuccessfulFuture();
		}
		if (codec != null) {
			return new EncodeTask(this, data, offset, length).register();
		}
		
		long futureExpectedLen = write0(data, offset, length, false);
		
		if (futureExpectedLen == -1) {
			return futuresController.getCancelledFuture();
		}
		return futuresController.getWriteFuture(futureExpectedLen);
	}

	@Override
	public void writenf(byte[] data, int offset, int length) {
		if (data == null) {
			throw new NullPointerException();
		}
		checkBounds(offset, length, data.length);
		if (length > 0) {
			if (codec != null) {
				new EncodeTask(this, data, offset, length).registernf();
			}
			else {
				write0(data, offset, length, false);
			}
		}
	}
	
	@Override
	public IFuture<Void> write(ByteBuffer data) {
		if (data == null) {
			throw new NullPointerException();
		} else if (data.remaining() == 0) {
			return futuresController.getSuccessfulFuture();
		}
		if (codec != null) {
			return new EncodeTask(this, data).register();
		}
		return write1(data);
	}

	@Override
	public void writenf(ByteBuffer data) {
		if (data == null) {
			throw new NullPointerException();
		} else if (data.remaining() > 0) {
			if (codec != null) {
				new EncodeTask(this, data).registernf();
			}
			else {
				write0(data, 0, data.remaining(), true);
			}
		}
	}

	@Override
	public IFuture<Void> write(IByteBufferHolder holder) {
		if (holder == null) {
			throw new NullPointerException();
		} else if (!holder.hasRemaining()) {
			return futuresController.getSuccessfulFuture();
		}
		if (codec != null) {
			return new EncodeTask(this, holder).register();
		}
		return write1(holder);
	}

	@Override
	public void writenf(IByteBufferHolder holder) {
		if (holder == null) {
			throw new NullPointerException();
		} else if (holder.hasRemaining()) {
			if (codec != null) {
				new EncodeTask(this, holder).registernf();
			}
			else {
				write0(holder, 0, -1, true);
			}
		}
	}

	@Override
	public IFuture<Void> write(ByteBuffer data, int length) {
		if (data == null) {
			throw new NullPointerException();
		} else if (data.remaining() < length || length < 0) {
			throw new IndexOutOfBoundsException();
		} else if (length == 0) {
			return futuresController.getSuccessfulFuture();
		}
		if (codec != null) {
			return new EncodeTask(this, data, length).register();
		}
		
		long futureExpectedLen = write0(data, 0, length, true);
		
		if (futureExpectedLen == -1) {
			return futuresController.getCancelledFuture();
		}
		return futuresController.getWriteFuture(futureExpectedLen);
	}

	@Override
	public void writenf(ByteBuffer data, int length) {
		if (data == null) {
			throw new NullPointerException();
		} else if (data.remaining() < length || length < 0) {
			throw new IndexOutOfBoundsException();
		} else if (length > 0) {
			if (codec != null) {
				new EncodeTask(this, data, length).registernf();
			}
			else {
				write0(data, 0, length, true);
			}
		}
	}
	
	@Override
	public IFuture<Void> write(Object msg) {
		if (msg == null) {
			throw new NullPointerException();
		}
		if (codec != null) {
			return new EncodeTask(this, msg).register();
		}
		if (msg.getClass() == byte[].class) {
			return write((byte[])msg);
		}
		if (msg instanceof ByteBuffer) {
			return write((ByteBuffer)msg);
		}
		if (msg instanceof IByteBufferHolder) {
			return write((IByteBufferHolder)msg);
		}
		throw new IllegalArgumentException("msg is an unexpected object");
	}
	
	@Override
	public void writenf(Object msg) {
		if (msg == null) {
			throw new NullPointerException();
		}
		if (codec != null) {
			new EncodeTask(this, msg).registernf();
		}
		else if (msg.getClass() == byte[].class) {
			writenf((byte[])msg);
		}
		else if (msg instanceof ByteBuffer) {
			writenf((ByteBuffer)msg);
		}
		else if (msg instanceof IByteBufferHolder) {
			writenf((IByteBufferHolder)msg);
		}
		else {
			throw new IllegalArgumentException("msg is an unexpected object");
		}
	}
	
	@Override
	public void close() {
		closeCalled.set(true);
		close(false);
	}
	
	final void superEvent(SessionEvent event) {
		super.event(event);
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
		if (optimizeBuffers) {
			outBuffers = EMPTY_ARRAY;
		}
		else {
			inBuffer = allocator.allocate(minInBufferCapacity);
			outBuffers = DEFAULT_ARRAY;
			outBuffers[0] = allocator.allocate(minOutBufferCapacity);
		}
	}
	
	@Override
	void postEnding() {
		if (allocator.isReleasable()) {
			if (inBuffer != null) {
				allocator.release(inBuffer);
				inBuffer = null;
			}
			for (int i = 0; i < outBuffers.length; ++i) {
				ByteBuffer buf = outBuffers[i];

				if (buf != null) {
					allocator.release(buf);
					outBuffers[i] = null;
				}
			}
		}
	}
	
	/**
	 * Gets the buffer into which received data should be written. 
	 * @return buffer in the write mode (i.e. not flipped yet).
	 */
	ByteBuffer getInBuffer() {
		if (inBuffer == null) {
			inBuffer = allocator.allocate(minInBufferCapacity);
		}
		else {
			inBuffer = allocator.ensureSome(inBuffer, minInBufferCapacity, maxInBufferCapacity);
		}
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
		if (outBuffers.length > 0) {
			outBuffers[outBuffers.length - 1].flip();
		}
		return outBuffers;
	}

	static ByteBuffer consumeBuffer(ByteBuffer inBuffer, IStreamReader handler, IByteBufferAllocator allocator, IConsumeController consumeController) {
		int available = handler.available(inBuffer, false);
		if (available > 0) {
			inBuffer.flip();
			if (available == inBuffer.remaining()) {
				handler.read(inBuffer);
				return null;
			}
			
			ByteBuffer dup = inBuffer.duplicate();
			ByteBuffer data;
		
			do {
				data = allocator.allocate(available);
				dup.limit(dup.position() + available);
				data.put(dup);
				data.flip();
				inBuffer.position(dup.position());
				handler.read(data);
				if (consumeController.skipConsuming()) {
					break;
				}
				available = handler.available(inBuffer, true);
				if (available == inBuffer.remaining()) {
					handler.read(inBuffer);
					return null;
				}
			} while (available > 0);
			inBuffer.compact();
		}
		return inBuffer;
	}
	
	static void consumeBuffer(ByteBuffer inBuffer, IStreamReader handler, IConsumeController consumeController) {
		boolean hasArray = inBuffer.hasArray();
		int available;
		byte[] array;
		int arrayOff;
		
		if (hasArray) {
			array = inBuffer.array();
			arrayOff = inBuffer.arrayOffset();
			available = handler.available(array, arrayOff, inBuffer.position());
		}
		else {
			array = null;
			arrayOff = 0;
			available = handler.available(inBuffer, false);
		}
		
		if (available > 0) {
			byte[] data = new byte[available];

			inBuffer.flip();
			inBuffer.get(data);
			handler.read(data);
			
			if (inBuffer.hasRemaining()) {
				if (hasArray) {
					while ((available = handler.available(array, arrayOff + inBuffer.position(), inBuffer.remaining())) > 0) {
						if (consumeController.skipConsuming()) {
							break;
						}
						data = new byte[available];
						inBuffer.get(data);
						handler.read(data);
					}
				}
				else {
					while ((available = handler.available(inBuffer, true)) > 0) {
						if (consumeController.skipConsuming()) {
							break;
						}
						data = new byte[available];
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
			else {
				inBuffer.clear();
			}
		}
	}
	
	/**
	 * Informs that input buffer has new data that may be ready to consume.
	 */
	@Override
	void consumeInBuffer() {
		if (optimizeBuffers) {
			inBuffer = consumeBuffer(inBuffer, superCodec(), allocator, consumeController);
		}
		else {
			consumeBuffer(inBuffer, superCodec(), consumeController);
		}
	}

	void consumeInBufferAfterNoRead() {
		if (optimizeBuffers && inBuffer.position() == 0) {
			allocator.release(inBuffer);
			inBuffer = null;
		}
	}
	
	IStreamReader superCodec() {
		return codec != null ? codec : (IStreamReader) this.handler;
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
		outBuffersSize -= consumedBytes;
		outBuffers = compactBuffers(outBuffers, allocator, minOutBufferCapacity, optimizeBuffers);
		return outBuffers.length == 0 || (outBuffers.length == 1 && outBuffers[0].position() == 0);
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
	
	private class EncodeTaskWriter implements IEncodeTaskWriter {

		@Override
		public final IFuture<Void> write(SocketAddress remoteAddress, ByteBuffer buffer, boolean withFuture) {
			if (withFuture) {
				return write1(buffer);
			}
			write0(buffer);
			return null;
		}

		@Override
		public final IFuture<Void> write(SocketAddress remoteAddress, byte[] bytes, boolean withFuture) {
			if (withFuture) {
				return write1(bytes);
			}
			write0(bytes, 0, bytes.length, false);
			return null;
		}

		@Override
		public IFuture<Void> write(SocketAddress remoteAddress, IByteBufferHolder holder, boolean withFuture) {
			if (withFuture) {
				return write1(holder);
			}
			write0(holder);
			return null;
		}
		
	};
}
