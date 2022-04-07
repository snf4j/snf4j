/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020-2022 SNF4J contributors
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
import java.util.concurrent.atomic.AtomicBoolean;

import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.session.IDatagramSession;
import org.snf4j.core.session.SessionState;

class DatagramServerSession extends DatagramSession {

	private final DatagramSession delegate;
	
	private final SocketAddress remoteAddress;
	
	private AtomicBoolean isClosing = new AtomicBoolean(false);
	
	DatagramServerSession(DatagramSession delegate, SocketAddress remoteAddress, IDatagramHandler handler) {
		super(null, handler);
		this.delegate = delegate;
		this.remoteAddress = remoteAddress;
		setLoop(delegate.loop);
	}

	void closingFinished() {
		isClosing.set(true);
		synchronized (writeLock) {
			closing = ClosingState.FINISHED;
		}
	}
	
	@Override
	IEncodeTaskWriter getEncodeTaskWriter() {
		if (encodeTaskWriter == null) {
			encodeTaskWriter = new EncodeTaskWriter();
		}
		return encodeTaskWriter;
	}
	
	@Override
	public IDatagramSession getParent() {
		return delegate;
	}
	
	@Override
	public SessionState getState() {
		if (isClosing.get()) {
			return SessionState.CLOSING;
		}
		return super.getState();
	}

	private void close0() {
		closeCalled.set(true);
		synchronized (writeLock) {
			if (closing != ClosingState.NONE) {
				return;
			}
			closing = ClosingState.FINISHING;
		}
		if (delegate.loop.inLoop()) {
			new ClosingTask().run();
		} 
		else {
			delegate.loop.executenf(new ClosingTask());
		}
	}
	
	@Override
	void preCreated() {
	}
	
	@Override
	void postEnding() {
	}
	
	@Override
	public void close() {
		close0();
	}

	@Override
	public void quickClose() {
		close0();
	}

	@Override
	public void dirtyClose() {
		close0();
	}

	@Override
	public SocketAddress getLocalAddress() {
		return delegate.getLocalAddress();
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return remoteAddress == null ? delegate.getRemoteAddress() : remoteAddress;
	}

	@Override
	public void suspendRead() {
		delegate.suspendRead();
	}

	@Override
	public void suspendWrite() {
		delegate.suspendWrite();
	}

	@Override
	public void resumeRead() {
		delegate.resumeRead();
	}

	@Override
	public void resumeWrite() {
		delegate.resumeWrite();
	}

	@Override
	public boolean isReadSuspended() {
		return delegate.isReadSuspended();
	}

	@Override
	public boolean isWriteSuspended() {
		return delegate.isWriteSuspended();
	}
	
	@Override
	long superWrite(DatagramRecord record) {
		record.address = remoteAddress;
		return delegate.superWrite(record);
	}
	
	@Override
	IFuture<Void> superWrite(DatagramRecord record, boolean withFuture) {
		return delegate.superWrite(record, withFuture);
	}
	
	@Override
	void superQuickClose() {
		close0();
	}
	
	@Override
	void superClose() {
		close0();
	}
	
	@Override
	public IFuture<Void> write(byte[] datagram) {
		if (codec != null) {
			return super.send(remoteAddress, datagram);
		}
		return delegate.send(remoteAddress, datagram);
	}

	@Override
	public void writenf(byte[] datagram) {
		if (codec != null) {
			super.sendnf(remoteAddress, datagram);
			return;
		}
		delegate.sendnf(remoteAddress, datagram);
	}

	@Override
	public IFuture<Void> write(byte[] datagram, int offset, int length) {
		if (codec != null) {
			return super.send(remoteAddress, datagram, offset, length);
		}
		return delegate.send(remoteAddress, datagram, offset, length);
	}

	@Override
	public void writenf(byte[] datagram, int offset, int length) {
		if (codec != null) {
			super.sendnf(remoteAddress, datagram, offset, length);
			return;
		}
		delegate.sendnf(remoteAddress, datagram, offset, length);		
	}

	@Override
	public IFuture<Void> write(ByteBuffer datagram) {
		if (codec != null) {
			return super.send(remoteAddress, datagram);
		}
		return delegate.send(remoteAddress, datagram);
	}

	@Override
	public void writenf(ByteBuffer datagram) {
		if (codec != null) {
			super.sendnf(remoteAddress, datagram);
			return;
		}
		delegate.sendnf(remoteAddress, datagram);
	}

	@Override
	public IFuture<Void> write(ByteBuffer datagram, int length) {
		if (codec != null) {
			return super.send(remoteAddress, datagram, length);
		}
		return delegate.send(remoteAddress, datagram, length);
	}

	@Override
	public void writenf(ByteBuffer datagram, int length) {
		if (codec != null) {
			super.sendnf(remoteAddress, datagram, length);
			return;
		}
		delegate.sendnf(remoteAddress, datagram, length);
	}

	@Override
	public IFuture<Void> write(IByteBufferHolder datagram) {
		if (codec != null) {
			return super.send(remoteAddress, datagram);
		}
		return delegate.send(remoteAddress, datagram);
	}

	@Override
	public void writenf(IByteBufferHolder datagram) {
		if (codec != null) {
			super.sendnf(remoteAddress, datagram);
			return;
		}
		delegate.sendnf(remoteAddress, datagram);
	}
	
	@Override
	public IFuture<Void> write(Object msg) {
		if (codec != null) {
			return super.send(remoteAddress, msg);
		}
		return delegate.send(remoteAddress, msg);
	}

	@Override
	public void writenf(Object msg) {
		if (codec != null) {
			super.sendnf(remoteAddress, msg);
			return;
		}
		delegate.sendnf(remoteAddress, msg);		
	}

	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, byte[] datagram) {
		if (codec != null) {
			return super.send(remoteAddress, datagram);
		}
		return delegate.send(remoteAddress, datagram);
	}

	@Override
	public void sendnf(SocketAddress remoteAddress, byte[] datagram) {
		if (codec != null) {
			super.sendnf(remoteAddress, datagram);
			return;
		}
		delegate.sendnf(remoteAddress, datagram);		
	}

	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, byte[] datagram,
			int offset, int length) {
		if (codec != null) {
			return super.send(remoteAddress, datagram, offset, length);
		}
		return delegate.send(remoteAddress, datagram, offset, length);
	}

	@Override
	public void sendnf(SocketAddress remoteAddress, byte[] datagram,
			int offset, int length) {
		if (codec != null) {
			super.sendnf(remoteAddress, datagram, offset, length);
			return;
		}
		delegate.sendnf(remoteAddress, datagram, offset, length);
	}

	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, ByteBuffer datagram) {
		if (codec != null) {
			return super.send(remoteAddress, datagram);
		}
		return delegate.send(remoteAddress, datagram);
	}

	@Override
	public void sendnf(SocketAddress remoteAddress, ByteBuffer datagram) {
		if (codec != null) {
			super.sendnf(remoteAddress, datagram);
			return;
		}
		delegate.sendnf(remoteAddress, datagram);	
	}

	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, ByteBuffer datagram,
			int length) {
		if (codec != null) {
			return super.send(remoteAddress, datagram, length);
		}
		return delegate.send(remoteAddress, datagram, length);
	}

	@Override
	public void sendnf(SocketAddress remoteAddress, ByteBuffer datagram,
			int length) {
		if (codec != null) {
			super.sendnf(remoteAddress, datagram, length);
			return;
		}
		delegate.sendnf(remoteAddress, datagram, length);	
	}

	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, IByteBufferHolder datagram) {
		if (codec != null) {
			return super.send(remoteAddress, datagram);
		}
		return delegate.send(remoteAddress, datagram);
	}

	@Override
	public void sendnf(SocketAddress remoteAddress, IByteBufferHolder datagram) {
		if (codec != null) {
			super.sendnf(remoteAddress, datagram);
			return;
		}
		delegate.sendnf(remoteAddress, datagram);	
	}
	
	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, Object msg) {
		if (codec != null) {
			return super.send(remoteAddress, msg);
		}
		return delegate.send(remoteAddress, msg);
	}

	@Override
	public void sendnf(SocketAddress remoteAddress, Object msg) {
		if (codec != null) {
			super.sendnf(remoteAddress, msg);
			return;
		}
		delegate.sendnf(remoteAddress, msg);		
	}

	private class ClosingTask implements Runnable {

		@Override
		public void run() {
			((DatagramServerHandler)delegate.getHandler()).closeSession(remoteAddress);
		}
		
	}
	
	private class EncodeTaskWriter implements IEncodeTaskWriter {

		@Override
		public IFuture<Void> write(SocketAddress remoteAddress, ByteBuffer buffer, boolean withFuture) {
			return delegate.simpleSend(remoteAddress, buffer, withFuture);
		}

		@Override
		public IFuture<Void> write(SocketAddress remoteAddress, byte[] bytes, boolean withFuture) {
			return delegate.simpleSend(remoteAddress, bytes, withFuture);
		}

		@Override
		public IFuture<Void> write(SocketAddress remoteAddress, IByteBufferHolder holder, boolean withFuture) {
			return delegate.simpleSend(remoteAddress, holder, withFuture);
		}
	}
	
}
