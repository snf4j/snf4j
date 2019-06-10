/*
T * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.Executor;

import org.snf4j.core.engine.IEngine;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.logger.ILogger;

/**
 * The stream-oriented session that handle protocols driven by customized protocol engines 
 * implementing the {@link IEngine} interface.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class EngineStreamSession extends StreamSession {

	private final InternalEngineHandler internal;
	
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
		super(name, new InternalEngineHandler(engine, handler, logger));
		internal = (InternalEngineHandler) this.handler;
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
		super(new InternalEngineHandler(engine, handler, logger));
		internal = (InternalEngineHandler) this.handler;
	}
	
	/**
	 * Sets the executor that will be used to execute delegated tasks required
	 * by this sessions to complete operations that block, or may take an
	 * extended period of time to complete.
	 * 
	 * @param executor
	 *            the new executor, or <code>null</code> to use the executor
	 *            configured in the selector loop that handles this session.
	 */
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}
	
	/**
	 * Returns the executor that will be used to execute delegated tasks
	 * required by this sessions to complete operations that block, or may take
	 * an extended period of time to complete.
	 * <p>
	 * By default, this method returns the executor configured in the selector 
	 * loop that handles this session, or <code>null</code>.
	 * 
	 * @return the current executor, or <code>null</code> if the executor is
	 *         undefined (i.e. the session is not associated with the selector 
	 *         loop and the executor is not set)
	 */
	public Executor getExecutor() {
		return (executor == null && loop != null) ? loop.getExecutor() : executor;
	}
	
	/**
	 * Initiates handshaking (initial or renegotiation) on the protocol engine
	 * driving this session. After calling this method the handshake will start
	 * immediately.
	 * <p>
	 * This method is not needed for the initial handshake, as the <code>wrap</code> and 
	 * <code>unwrap</code> methods of the protocol engine should initiate it if the 
	 * handshaking has not already begun.
	 * <p>
	 * The operation is asynchronous.
	 */
	public void beginHandshake() {
		internal.beginHandshake(false);
	}

	/**
	 * Initiates lazy handshaking (initial or renegotiation) on the protocol
	 * engine driving this session. After calling this method the handshake will
	 * not start immediately. It will start when new data is received from a
	 * remote peer or following methods are called: <code>write</code>,
	 * <code>writenf</code>, <code>beginHandshake</code>.
	 * <p>
	 * This method is not needed for the initial handshake, as the
	 * <code>wrap</code> and <code>unwrap</code> methods of the protocol engine
	 * should initiate it if the handshaking has not already begun.
	 * <p>
	 * The operation is asynchronous.
	 */
	public void beginLazyHandshake() {
		internal.beginHandshake(true);
	}
	
	@Override
	public IStreamHandler getHandler() {
		return internal.getHandler();
	}	
	
	@Override
	void exception(Throwable t) {
		if (isValid(EventType.EXCEPTION_CAUGHT)) {
			try {
				internal.exception(t);
				futuresController.exception(t);
				super.quickClose();
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
	
	@Override
	public IFuture<Void> write(byte[] data) {
		if (data == null) {
			throw new NullPointerException();
		} else if (data.length == 0) {
			return futuresController.getSuccessfulFuture();
		}
		checkKey(key);
		if (closing == ClosingState.NONE) {
			return internal.write(data, 0, data.length, true);
		}
		return futuresController.getCancelledFuture();
	}
	
	@Override
	public void writenf(byte[] data) {
		if (data == null) {
			throw new NullPointerException();
		} else if (data.length > 0) {
			checkKey(key);
			if (closing == ClosingState.NONE) {
				internal.write(data, 0, data.length, false);
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
		checkKey(key);
		if (closing == ClosingState.NONE) {
			return internal.write(data, offset, length, true);
		}
		return futuresController.getCancelledFuture();
	}

	@Override
	public void writenf(byte[] data, int offset, int length) {
		if (data == null) {
			throw new NullPointerException();
		}
		checkBounds(offset, length, data.length);
		if (length > 0) {
			checkKey(key);
			if (closing == ClosingState.NONE) {
				internal.write(data, offset, length, false);
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
		checkKey(key);
		if (closing == ClosingState.NONE) {
			return internal.write(data, data.remaining(), true);
		}
		return futuresController.getCancelledFuture();
	}

	@Override
	public void writenf(ByteBuffer data) {
		if (data == null) {
			throw new NullPointerException();
		} else if (data.remaining() > 0) {
			checkKey(key);
			if (closing == ClosingState.NONE) {
				internal.write(data, data.remaining(), false);
			}
		}		
	}
	
	@Override
	public IFuture<Void> write(ByteBuffer data, int length) {
		if (data == null) {
			throw new NullPointerException();
		} else if (data.remaining() < length) {
			throw new IndexOutOfBoundsException();
		} else if (length == 0) {
			return futuresController.getSuccessfulFuture();
		}
		checkKey(key);
		if (closing == ClosingState.NONE) {
			return internal.write(data, length, true);
		}
		return futuresController.getCancelledFuture();
	}

	@Override
	public void writenf(ByteBuffer data, int length) {
		if (data == null) {
			throw new NullPointerException();
		} else if (data.remaining() < length) {
			throw new IndexOutOfBoundsException();
		} else if (length > 0) {
			checkKey(key);
			if (closing == ClosingState.NONE) {
				internal.write(data, length, false);
			}
		}
	}
		
	@Override
	public void close() {
		SelectionKey key = this.key;
		
		if (key != null && key.isValid()) {
			internal.close();
		}
	}
	
	@Override
	public void quickClose() {
		SelectionKey key = this.key;
		
		if (key != null && key.isValid()) {
			internal.quickClose();
		}
	}

	void superQuickClose() {
		super.quickClose();
	}
	
	@Override
	public void dirtyClose() {
		SelectionKey key = this.key;
		
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
}
