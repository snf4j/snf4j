/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019-2021 SNF4J contributors
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

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.Executor;

import org.snf4j.core.engine.IEngine;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.session.IEngineStreamSession;

/**
 * A stream-oriented session that handles protocols driven by customized protocol engines 
 * implementing the {@link IEngine} interface.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class EngineStreamSession extends StreamSession implements IEngineStreamSession {

	private final EngineStreamHandler internal;
	
	private volatile Executor executor;
	
	/**
	 * Constructs a named stream-oriented session associated with a protocol
	 * engine and a handler.
	 * 
	 * @param name
	 *            the name for this session, or <code>null</code> if the
	 *            handler's name should be used for this session's name
	 * @param engine
	 *            the protocol engine driving this session
	 * @param handler
	 *            the handler that should be associated with this session
	 * @param logger
	 *            the logger used to log messages related with this session
	 */
	public EngineStreamSession(String name, IEngine engine, IStreamHandler handler, ILogger logger) {
		super(name, new EngineStreamHandler(engine, handler, logger));
		internal = (EngineStreamHandler) this.handler;
		executor = handler.getFactory().getExecutor();
	}

	/**
	 * Constructs a stream-oriented session associated with a protocol
	 * engine and a handler.
	 * 
	 * @param engine
	 *            the protocol engine driving this session
	 * @param handler
	 *            the handler that should be associated with this session
	 * @param logger
	 *            the logger used to log messages related with this session
	 */
	public EngineStreamSession(IEngine engine, IStreamHandler handler, ILogger logger) {
		super(new EngineStreamHandler(engine, handler, logger));
		internal = (EngineStreamHandler) this.handler;
		executor = handler.getFactory().getExecutor();
	}

	@Override
	IEncodeTaskWriter getEncodeTaskWriter() {
		if (encodeTaskWriter == null) {
			encodeTaskWriter = new EncodeTaskWriter();
		}
		return encodeTaskWriter;
	}
	
	@Override
	IStreamReader superCodec() {
		return (IStreamReader) handler;
	}
	
	@Override
	ByteBuffer[] getInBuffersForCopying() {
		ByteBuffer[] ins, superIns = super.getInBuffersForCopying();
		ByteBuffer in = internal.getInNetBuffer();
		
		if (in == null || in.position() == 0) {
			return superIns;
		}
		ins = new ByteBuffer[superIns.length+1];
		ins[0] = in;
		System.arraycopy(superIns, 0, ins, 1, superIns.length);
		return ins;
	}
	
	@Override
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}
	
	@Override
	public Executor getExecutor() {
		return (executor == null && loop != null) ? loop.getExecutor() : executor;
	}
	
	@Override
	public void beginHandshake() {
		internal.beginHandshake(false);
	}

	@Override
	public void beginLazyHandshake() {
		internal.beginHandshake(true);
	}
	
	@Override
	public Object getEngineSession() {
		return internal.getEngineSession();
	}
	
	@Override
	public IStreamHandler getHandler() {
		return internal.getHandler();
	}	
	
	@Override
	void controlCloseException(Throwable t) {
		internal.exception(t);
	}
	
	@Override
	void exception(Throwable t) {
		if (isValid(EventType.EXCEPTION_CAUGHT)) {
			try {
				t = controlClose(t);
				if (t != null) {
					internal.exception(t);
					futuresController.exception(t);
					super.quickClose();
				}
			}
			catch (Exception e) {
				elogger.error(logger, "Failed event {} for {}: {}", EventType.EXCEPTION_CAUGHT, this, e);
				futuresController.exception(t);
				super.quickClose();
			}
		}
	}
	
	@Override
	void event(SessionEvent event) {
		super.superEvent(event);
	}
	
	private final IFuture<Void> write0(byte[] data, int offset, int length, boolean needFuture) {
		checkKey(key);
		if (closing == ClosingState.NONE) {
			return internal.write(data, offset, length, needFuture);
		} 
		return needFuture ? futuresController.getCancelledFuture() : null;
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
		return write0(data, 0, data.length, true);
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
		return write0(data, offset, length, true);
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

	private final IFuture<Void> write0(ByteBuffer data, int length, boolean needFuture) {
		checkKey(key);
		if (closing == ClosingState.NONE) {
			return internal.write(data, length, needFuture);
		}
		return needFuture ? futuresController.getCancelledFuture() : null;
		
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
		return write0(data, data.remaining(), true);
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
				write0(data, data.remaining(), false);
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
		return write0(data, length, true);
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
				write0(data, length, false);
			}
		}
	}
		
	@Override
	public void close() {
		SelectionKey key = this.key;
		closeCalled.set(true);
		
		if (key != null && key.isValid()) {
			internal.close();
		}
	}
	
	@Override
	public void quickClose() {
		SelectionKey key = this.key;
		closeCalled.set(true);
		
		if (key != null && key.isValid()) {
			internal.quickClose();
		}
	}

	void superQuickClose() {
		super.quickClose();
	}
	
	void superClose() {
		super.close();
	}
	
	@Override
	public void dirtyClose() {
		SelectionKey key = this.key;
		closeCalled.set(true);
		
		if (key != null && key.isValid()) {
			internal.dirtyClose();
		}
	}
	
	@Override
	void preCreated() {
		super.preCreated();
		internal.preCreated();
	}
	
	@Override
	void postEnding() {
		internal.postEnding();		
		super.postEnding();
	}
	
	private class EncodeTaskWriter implements IEncodeTaskWriter {

		@Override
		public IFuture<Void> write(SocketAddress remoteAddress, ByteBuffer buffer, boolean withFuture) {
			return write0(buffer, buffer.remaining(), withFuture);
		}

		@Override
		public IFuture<Void> write(SocketAddress remoteAddress, byte[] bytes, boolean withFuture) {
			return write0(bytes, 0, bytes.length, withFuture);
		}
	}
}
