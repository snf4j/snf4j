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

import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;

/**
 * The core implementation of the {@link org.snf4j.core.session.IStreamSession
 * IStreamSession} interface handling SSL/TLS connection.
 * <p>
 * It uses {@link javax.net.ssl.SSLEngine SSLEngine} to handle protocols such as 
 * the Secure Sockets Layer (SSL) or IETF RFC 2246 "Transport Layer Security" 
 * (TLS) protocols.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class SSLSession extends StreamSession {

	private final static ILogger LOGGER = LoggerFactory.getLogger(SSLSession.class);

	private final InternalSSLHandler internal;
	
	public SSLSession(String name, IStreamHandler handler, boolean clientMode) throws Exception {
		super(name, new InternalSSLHandler(handler, clientMode, LOGGER));
		internal = (InternalSSLHandler) this.handler;
	}

	public SSLSession(IStreamHandler handler, boolean clientMode) throws Exception {
		super(new InternalSSLHandler(handler, clientMode, LOGGER));
		internal = (InternalSSLHandler) this.handler;
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
