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
package org.snf4j.core;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.IHandler;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.core.session.ISessionPipeline;
import org.snf4j.core.session.IStreamSession;

public class TestInternalSession extends InternalSession implements IStreamSession {
	
	private final static ILogger LOGGER = LoggerFactory.getLogger(TestInternalSession.class);

	protected TestInternalSession(String name, IHandler handler, CodecExecutorAdapter codec) {
		super(name, handler, codec, LOGGER);
	}

	@Override
	public IStreamHandler getHandler() {
		return null;
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
	public SocketAddress getLocalAddress() {
		return null;
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return null;
	}

	@Override
	IEncodeTaskWriter getEncodeTaskWriter() {
		return null;
	}

	@Override
	SessionPipeline<?> createPipeline() {
		return null;
	}

	@Override
	void preCreated() {
	}

	@Override
	void postEnding() {
	}

	@Override
	public IStreamSession getParent() {
		return null;
	}

	@Override
	public ISessionPipeline<IStreamSession> getPipeline() {
		return null;
	}

	@Override
	public IFuture<Void> write(byte[] data) {
		return null;
	}

	@Override
	public void writenf(byte[] data) {
	}

	@Override
	public IFuture<Void> write(byte[] data, int offset, int length) {
		return null;
	}

	@Override
	public void writenf(byte[] data, int offset, int length) {
	}

	@Override
	public IFuture<Void> write(ByteBuffer data) {
		return null;
	}

	@Override
	public void writenf(ByteBuffer data) {
	}

	@Override
	public IFuture<Void> write(ByteBuffer data, int length) {
		return null;
	}

	@Override
	public void writenf(ByteBuffer data, int length) {
	}

	@Override
	public IFuture<Void> write(Object msg) {
		return null;
	}

	@Override
	public void writenf(Object msg) {
	}

	@Override
	public IFuture<Void> write(IByteBufferHolder holder) {
		return null;
	}

	@Override
	public void writenf(IByteBufferHolder holder) {
	}

}
