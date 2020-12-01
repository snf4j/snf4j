/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020 SNF4J contributors
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

import org.snf4j.core.DatagramSession.DatagramRecord;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.future.ITwoThresholdFuture;
import org.snf4j.core.handler.IDatagramHandler;

class EngineDatagramWrapper {
	
	private final EngineDatagramHandler internal;
	
	private final DatagramSession session;
	
	private final SocketAddress remoteAddress;
	
	private volatile Executor executor;	
	
	EngineDatagramWrapper(SocketAddress remoteAddress, EngineDatagramHandler internal) {
		this.session = internal.session;
		if (!session.getTimer().isSupported()) {
			if (!Constants.YES.equals(System.getProperty(Constants.IGNORE_NO_SESSION_TIMER_EXCEPTION, Constants.NO))) {
				throw new IllegalStateException("no timer specified");
			}
		}
		this.internal = internal;
		this.remoteAddress = remoteAddress;
	}

	final boolean connectedTo(SocketAddress remoteAddress) {
		return remoteAddress == null || remoteAddress.equals(this.remoteAddress);
	}
	
	final void setExecutor(Executor executor) {
		this.executor = executor;
	}
	
	final Executor getExecutor() {
		InternalSelectorLoop loop = session.loop;
		
		return (executor == null && loop != null) ? loop.getExecutor() : executor;
	}
	
	final void beginHandshake() {
		internal.beginHandshake(false);
	}
	
	final void beginLazyHandshake() {
		internal.beginHandshake(true);
	}
	
	final IDatagramHandler getHandler() {
		return internal.getHandler();
	}
	
	final void exception(Throwable t) {
		internal.exception(t);
	}
	
	private final IFuture<Void> write0(byte[] datagram, int offset, int length, boolean needFuture) {
		EngineDatagramRecord record = new EngineDatagramRecord(remoteAddress);
		
		session.initRecord(record, datagram, offset, length);
		return write0(record, needFuture);
	}
	
	private final IFuture<Void> write0(ByteBuffer datagram, int length, boolean needFuture) {
		EngineDatagramRecord record = new EngineDatagramRecord(remoteAddress);
		
		session.initRecord(record, datagram, length);
		return write0(record, needFuture);
	}

	private final IFuture<Void> write0(EngineDatagramRecord record, boolean needFuture) {
		InternalSession.checkKey(session.key);
		if (session.closing == ClosingState.NONE) {
			return internal.write(record, needFuture);
		} 
		return needFuture ? session.futuresController.getCancelledFuture() : null;
	}

	IFuture<Void> write(byte[] datagram) {
		if (datagram == null) {
			throw new NullPointerException();
		}
		if (datagram.length == 0) {
			return session.futuresController.getSuccessfulFuture();
		}
		if (session.codec != null) {
			return new EncodeTask(session, datagram).register(null);
		}
		return write0(datagram, 0, datagram.length, true);
	}

	void writenf(byte[] datagram) {
		if (datagram == null) {
			throw new NullPointerException();
		}
		if (datagram.length > 0) {
			if (session.codec != null) {
				new EncodeTask(session, datagram).registernf(null);
			}
			else {
				write0(datagram, 0, datagram.length, false);
			}
		}
	}
	
	IFuture<Void> write(byte[] datagram, int offset, int length) {
		if (datagram == null) {
			throw new NullPointerException();
		}
		InternalSession.checkBounds(offset, length, datagram.length);
		if (length == 0) {
			return session.futuresController.getSuccessfulFuture();
		}
		if (session.codec != null) {
			return new EncodeTask(session, datagram, offset, length).register(null);
		}
		return write0(datagram, offset, length, true);
	}
	
	void writenf(byte[] datagram, int offset, int length) {
		if (datagram == null) {
			throw new NullPointerException();
		}
		InternalSession.checkBounds(offset, length, datagram.length);
		if (length > 0) {
			if (session.codec != null) {
				new EncodeTask(session, datagram, offset, length).registernf(null);
			}
			else  {
				write0(datagram, offset, length, false);
			}
		}
	}
	
	IFuture<Void> write(ByteBuffer datagram) {
		if (datagram == null) {
			throw new NullPointerException();
		} else if (datagram.remaining() == 0) {
			return session.futuresController.getSuccessfulFuture();
		}		
		if (session.codec != null) {
			return new EncodeTask(session, datagram).register(null);
		}
		return write0(datagram, datagram.remaining(), true);
	}
	
	void writenf(ByteBuffer datagram) {
		if (datagram == null) {
			throw new NullPointerException();
		} else if (datagram.remaining() > 0) {
			if (session.codec != null) {
				new EncodeTask(session, datagram).registernf(null);
			}
			else {
				write0(datagram, datagram.remaining(), false);
			}
		}		
	}
	
	IFuture<Void> write(ByteBuffer datagram, int length) {
		if (datagram == null) {
			throw new NullPointerException();
		} else if (datagram.remaining() < length || length < 0) {
			throw new IndexOutOfBoundsException();
		} else if (length == 0) {
			return session.futuresController.getSuccessfulFuture();
		}		
		if (session.codec != null) {
			return new EncodeTask(session, datagram, length).register(null);
		}
		return write0(datagram, length, true);
	}
	
	void writenf(ByteBuffer datagram, int length) {
		if (datagram == null) {
			throw new NullPointerException();
		} else if (datagram.remaining() < length || length < 0) {
			throw new IndexOutOfBoundsException();
		} else if (length > 0) {
			if (session.codec != null) {
				new EncodeTask(session, datagram, length).registernf(null);
			}
			else {
				write0(datagram, length, false);
			}
		}		
	}
	
	IFuture<Void> write(Object msg) {
		if (msg == null) {
			throw new NullPointerException();
		}
		if (session.codec != null) {
			return new EncodeTask(session, msg).register(null);
		}
		if (msg.getClass() == byte[].class) {
			return write((byte[])msg);
		}
		if (msg instanceof ByteBuffer) {
			return write((ByteBuffer)msg);
		}
		throw new IllegalArgumentException("msg is an unexpected object");
	}
	
	void writenf(Object msg) {
		if (msg == null) {
			throw new NullPointerException();
		}
		if (session.codec != null) {
			new EncodeTask(session, msg).registernf(null);
		}
		else if (msg.getClass() == byte[].class) {
			writenf((byte[])msg);
		}
		else if (msg instanceof ByteBuffer) {
			writenf((ByteBuffer)msg);
		}
		else {
			throw new IllegalArgumentException("msg is an unexpected object");
		}
	}

	void close() {
		SelectionKey key = session.key;
		session.closeCalled.set(true);
		
		if (key != null && key.isValid()) {
			internal.close();
		}
	}
	
	void quickClose() {
		SelectionKey key = session.key;
		session.closeCalled.set(true);
		
		if (key != null && key.isValid()) {
			internal.quickClose();
		}
	}	
	
	void dirtyClose() {
		SelectionKey key = session.key;
		session.closeCalled.set(true);
		
		if (key != null && key.isValid()) {
			internal.dirtyClose();
		}
	}
	
	void preCreated() {
		internal.preCreated();
	}
	
	void postEnding() {
		internal.postEnding();
	}
	
	IEncodeTaskWriter getEncodeTaskWriter() {
		return new EncodeTaskWriter();
	}
	
	static class EngineDatagramRecord extends DatagramRecord {
		ITwoThresholdFuture<Void> future;
		
		EngineDatagramRecord(SocketAddress address) {
			super(address);
		}
	}
	
	private class EncodeTaskWriter implements IEncodeTaskWriter {

		@Override
		public final IFuture<Void> write(SocketAddress remoteAddress, ByteBuffer buffer, boolean withFuture) {
			EngineDatagramRecord record = new EngineDatagramRecord(remoteAddress);
			record.buffer = buffer;
			record.release = session.optimizeBuffers;
			if (remoteAddress != null) {
				return session.superWrite(record, withFuture);
			}
			return write0(record, withFuture);
		}

		@Override
		public final IFuture<Void> write(SocketAddress remoteAddress, byte[] bytes, boolean withFuture) {
			EngineDatagramRecord record = new EngineDatagramRecord(remoteAddress);
			record.buffer = ByteBuffer.wrap(bytes);
			if (remoteAddress != null) {
				return session.superWrite(record, withFuture);
			}
			return write0(record, withFuture);
		}
	}
}
