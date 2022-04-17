/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021-2022 SNF4J contributors
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
package org.snf4j.websocket;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.snf4j.core.IByteBufferHolder;
import org.snf4j.core.TestHandler;
import org.snf4j.core.TestSession;
import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.session.ISessionPipeline;
import org.snf4j.core.session.IStreamSession;
import org.snf4j.websocket.handshake.Handshaker;

public class TestWSSession extends TestSession implements IWebSocketSession {
	
	List<Object> msgs = new ArrayList<Object>();
	
	boolean closed;
	
	IByteBufferAllocator allocator;
	
	public int incCapacity;
	
	public IWebSocketSessionConfig config;
	
	IStreamHandler handler;
	
	public TestWSSession(IByteBufferAllocator allocator, IStreamHandler handler) {
		this.allocator = allocator;
		this.handler = handler;
	}
	
	public TestWSSession(IByteBufferAllocator allocator) {
		this.allocator = allocator;
		this.handler = new WebSocketSessionHandler(new Handler(""), true);
	}

	public TestWSSession(IStreamHandler handler) {
		this.handler = handler;
	}
	
	public TestWSSession() {
		this.handler = new WebSocketSessionHandler(new Handler(""), true);
	}
	
	@Override
	public ByteBuffer allocate(int capacity) {
		if (allocator != null) {
			capacity += incCapacity;
			if (capacity < 1) {
				capacity = 1;
			}
			return allocator.allocate(capacity);
		}
		return super.allocate(capacity);
	}
	
	@Override
	public void release(ByteBuffer buffer) {
		if (allocator != null) {
			allocator.release(buffer);
		}
		else {
			super.release(buffer);
		}
	}
	
	@Override
	public ISessionConfig getConfig() {
		return config;
	}
	
	public void clear() {
		msgs.clear();
		closed = false;
	}
	
	public List<Object> msgs() {
		return msgs;
	}
	
	public boolean isClosed() {
		return closed;
	}
	
	@Override
	public void close() {
		closed = true;
	}
	
	@Override
	public IStreamHandler getHandler() {
		return handler;
	}

	@Override
	public IStreamSession getParent() {
		return null;
	}

	@Override
	public IFuture<Void> write(byte[] data) {
		return null;
	}

	@Override
	public void writenf(byte[] data) {
		msgs.add(data);
	}

	@Override
	public IFuture<Void> write(byte[] data, int offset, int length) {
		msgs.add(data);
		return null;
	}

	@Override
	public void writenf(byte[] data, int offset, int length) {
		msgs.add(data);
	}

	@Override
	public IFuture<Void> write(ByteBuffer data) {
		msgs.add(data);
		return null;
	}

	@Override
	public void writenf(ByteBuffer data) {
		msgs.add(data);
	}

	@Override
	public IFuture<Void> write(ByteBuffer data, int length) {
		msgs.add(data);
		return null;
	}

	@Override
	public void writenf(ByteBuffer data, int length) {
		msgs.add(data);
	}

	@Override
	public IFuture<Void> write(IByteBufferHolder holder) {
		msgs.add(holder);
		return null;
	}

	@Override
	public void writenf(IByteBufferHolder holder) {
		msgs.add(holder);
	}

	@Override
	public IFuture<Void> write(Object msg) {
		msgs.add(msg);
		return null;
	}

	@Override
	public void writenf(Object msg) {
		msgs.add(msg);
	}
	
	static class Handler extends TestHandler implements IWebSocketHandler {

		Handler(String name) {
			super(name);
		}
		
		@Override
		public IWebSocketSessionConfig getConfig() {
			return new DefaultWebSocketSessionConfig();
		}
	}

	@Override
	public IWebSocketHandler getWebSocketHandler() {
		return null;
	}

	@Override
	public Handshaker getHandshaker() {
		return new Handshaker(new DefaultWebSocketSessionConfig(), false);
	}

	@Override
	public void close(int status) {
	}

	@Override
	public void close(int status, String reason) {
	}

	@Override
	public ISessionPipeline<IStreamSession> getPipeline() {
		return null;
	}

}
