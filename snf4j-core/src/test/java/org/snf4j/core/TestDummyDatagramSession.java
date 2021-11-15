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
import java.util.concurrent.ConcurrentMap;

import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.future.SessionFuturesController;
import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.session.IDatagramSession;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.session.ISessionTimer;
import org.snf4j.core.session.SessionState;

public class TestDummyDatagramSession implements IDatagramSession {
	
	SessionFuturesController futures = new SessionFuturesController(this);
	
	@Override
	public long getId() {
		return 0;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public IDatagramHandler getHandler() {
		return null;
	}

	@Override
	public IDatagramSession getParent() {
		return null;
	}
	
	@Override
	public ISessionConfig getConfig() {
		return null;
	}
	
	@Override
	public ICodecPipeline getCodecPipeline() {
		return null;
	}
	
	@Override
	public SessionState getState() {
		return null;
	}

	@Override
	public boolean isOpen() {
		return false;
	}

	@Override
	public IFuture<Void> write(byte[] data) {
		return futures.getCancelledFuture();
	}

	@Override
	public void close() {
	}

	@Override
	public void quickClose() {
	}

	@Override
	public void dirtyClose() {
	}

	@Override
	public ConcurrentMap<Object, Object> getAttributes() {
		return null;
	}

	@Override
	public SocketAddress getLocalAddress() {
		return null;
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return null;
	}

	@Override
	public void suspendRead() {
	}

	@Override
	public void suspendWrite() {
	}

	@Override
	public void resumeRead() {
	}

	@Override
	public void resumeWrite() {
	}

	@Override
	public boolean isReadSuspended() {
		return false;
	}

	@Override
	public boolean isWriteSuspended() {
		return false;
	}

	@Override
	public long getReadBytes() {
		return 0;
	}

	@Override
	public long getWrittenBytes() {
		return 0;
	}

	@Override
	public double getReadBytesThroughput() {
		return 0;
	}

	@Override
	public double getWrittenBytesThroughput() {
		return 0;
	}

	@Override
	public long getCreationTime() {
		return 0;
	}

	@Override
	public long getLastIoTime() {
		return 0;
	}

	@Override
	public long getLastReadTime() {
		return 0;
	}

	@Override
	public long getLastWriteTime() {
		return 0;
	}

	@Override
	public ISessionTimer getTimer() {
		return null;
	}
	
	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, byte[] datagram) {
		return futures.getCancelledFuture();
	}

	@Override
	public IFuture<Void> getCreateFuture() {
		return futures.getCancelledFuture();
	}

	@Override
	public IFuture<Void> getOpenFuture() {
		return futures.getCancelledFuture();
	}

	@Override
	public IFuture<Void> getReadyFuture() {
		return futures.getCancelledFuture();
	}

	@Override
	public IFuture<Void> getCloseFuture() {
		return futures.getCancelledFuture();
	}

	@Override
	public IFuture<Void> getEndFuture() {
		return futures.getCancelledFuture();
	}

	@Override
	public void writenf(byte[] datagram) {
	}

	@Override
	public IFuture<Void> write(byte[] datagram, int offset, int length) {
		return futures.getCancelledFuture();
	}

	@Override
	public void writenf(byte[] datagram, int offset, int length) {
	}

	@Override
	public IFuture<Void> write(ByteBuffer datagram) {
		return futures.getCancelledFuture();
	}

	@Override
	public void writenf(ByteBuffer datagram) {
	}

	@Override
	public IFuture<Void> write(ByteBuffer datagram, int length) {
		return futures.getCancelledFuture();
	}

	@Override
	public void writenf(ByteBuffer datagram, int length) {
	}

	@Override
	public IFuture<Void> write(Object msg) {
		return futures.getCancelledFuture();
	}

	@Override
	public void writenf(Object msg) {
	}
	
	@Override
	public void sendnf(SocketAddress remoteAddress, byte[] datagram) {
	}

	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, byte[] datagram, int offset, int length) {
		return futures.getCancelledFuture();
	}

	@Override
	public void sendnf(SocketAddress remoteAddress, byte[] datagram, int offset, int length) {
	}

	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, ByteBuffer datagram) {
		return futures.getCancelledFuture();
	}

	@Override
	public void sendnf(SocketAddress remoteAddress, ByteBuffer datagram) {
	}

	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, ByteBuffer datagram, int length) {
		return futures.getCancelledFuture();
	}

	@Override
	public void sendnf(SocketAddress remoteAddress, ByteBuffer datagram, int length) {
	}

	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, Object msg) {
		return futures.getCancelledFuture();
	}

	@Override
	public void sendnf(SocketAddress remoteAddress, Object msg) {
	}

	@Override
	public ByteBuffer allocate(int capacity) {
		return null;
	}

	@Override
	public void release(ByteBuffer buffer) {
	}

	@Override
	public IFuture<Void> execute(Runnable task) {
		return null;
	}

	@Override
	public void executenf(Runnable task) {
	}

	@Override
	public boolean isDataCopyingOptimized() {
		return false;
	}
}
