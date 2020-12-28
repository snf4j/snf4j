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
import java.util.concurrent.Executor;

import org.snf4j.core.engine.IEngine;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.core.session.IEngineDatagramSession;

class EngineDatagramServerSession extends DatagramServerSession implements IEngineDatagramSession {
	
	private final static ILogger LOGGER = LoggerFactory.getLogger(EngineDatagramServerSession.class);
	
	private final EngineDatagramWrapper wrapper;

	EngineDatagramServerSession(IEngine engine, DatagramSession delegate, SocketAddress remoteAddress, IDatagramHandler handler) {
		super(delegate, remoteAddress, new EngineDatagramHandler(engine, remoteAddress, handler, LOGGER));
		wrapper = new EngineDatagramWrapper(remoteAddress, (EngineDatagramHandler) this.handler);
		wrapper.setExecutor(handler.getFactory().getExecutor());	
	}
	
	@Override
	IEncodeTaskWriter getEncodeTaskWriter() {
		if (encodeTaskWriter == null) {
			encodeTaskWriter = wrapper.getEncodeTaskWriter();
		}
		return encodeTaskWriter;
	}
	
	@Override
	IDatagramReader superCodec() {
		return (IDatagramReader) handler;
	}
	
	@Override
	public void setExecutor(Executor executor) {
		wrapper.setExecutor(executor);
	}
	
	@Override
	public Executor getExecutor() {
		return wrapper.getExecutor();
	}
	
	@Override
	public void beginHandshake() {
		wrapper.beginHandshake();
	}
	
	@Override
	public void beginLazyHandshake() {
		wrapper.beginLazyHandshake();
	}
	
	@Override
	public Object getEngineSession() {
		return wrapper.getEngineSession();
	}
	
	@Override
	public IDatagramHandler getHandler() {
		return wrapper.getHandler();
	}	
	
	@Override
	void event(SessionEvent event) {
		super.superEvent(event);
	}
	
	@Override
	public IFuture<Void> write(byte[] datagram) {
		return wrapper.write(datagram);
	}	
	
	@Override
	public void writenf(byte[] datagram) {
		wrapper.writenf(datagram);
	}
	
	@Override
	public IFuture<Void> write(byte[] datagram, int offset, int length) {
		return wrapper.write(datagram, offset, length);
	}
	
	@Override
	public void writenf(byte[] datagram, int offset, int length) {
		wrapper.writenf(datagram, offset, length);
	}
	
	@Override
	public IFuture<Void> write(ByteBuffer datagram) {
		return wrapper.write(datagram);
	}
	
	@Override
	public void writenf(ByteBuffer datagram) {
		wrapper.writenf(datagram);
	}
	
	@Override
	public IFuture<Void> write(ByteBuffer datagram, int length) {
		return wrapper.write(datagram, length);
	}
	
	@Override
	public void writenf(ByteBuffer datagram, int length) {
		wrapper.writenf(datagram, length);
	}
	
	@Override
	public IFuture<Void> write(Object msg) {
		return wrapper.write(msg);
	}
	
	@Override
	public void writenf(Object msg) {
		wrapper.writenf(msg);
	}
	
	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, byte[] datagram) {
		if (wrapper.connectedTo(remoteAddress)) {
			return wrapper.write(datagram);
		}
		return super.send(remoteAddress, datagram);
	}
	
	@Override
	public void sendnf(SocketAddress remoteAddress, byte[] datagram) {
		if (wrapper.connectedTo(remoteAddress)) {
			wrapper.writenf(datagram);
			return;
		}
		super.send(remoteAddress, datagram);
	}
	
	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, byte[] datagram, int offset, int length) {
		if (wrapper.connectedTo(remoteAddress)) {
			return wrapper.write(datagram, offset, length);
		}
		return super.send(remoteAddress, datagram, offset, length);
	}
	
	@Override
	public void sendnf(SocketAddress remoteAddress, byte[] datagram, int offset, int length) {
		if (wrapper.connectedTo(remoteAddress)) {
			wrapper.writenf(datagram, offset, length);
			return;
		}
		super.send(remoteAddress, datagram, offset, length);
	}
	
	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, ByteBuffer datagram) {
		if (wrapper.connectedTo(remoteAddress)) {
			return wrapper.write(datagram);
		}
		return super.send(remoteAddress, datagram);
	}
	
	@Override
	public void sendnf(SocketAddress remoteAddress, ByteBuffer datagram) {
		if (wrapper.connectedTo(remoteAddress)) {
			wrapper.writenf(datagram);
			return;
		}
		super.send(remoteAddress, datagram);
	}
	
	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, ByteBuffer datagram, int length) {
		if (wrapper.connectedTo(remoteAddress)) {
			return wrapper.write(datagram, length);
		}
		return super.send(remoteAddress, datagram, length);
	}
	
	@Override
	public void sendnf(SocketAddress remoteAddress, ByteBuffer datagram, int length) {
		if (wrapper.connectedTo(remoteAddress)) {
			wrapper.writenf(datagram, length);
			return;
		}
		super.send(remoteAddress, datagram, length);
	}
	
	@Override
	public IFuture<Void> send(SocketAddress remoteAddress, Object msg) {
		if (wrapper.connectedTo(remoteAddress)) {
			return wrapper.write(msg);
		}
		return super.send(remoteAddress, msg);
	}
	
	@Override
	public void sendnf(SocketAddress remoteAddress, Object msg) {
		if (wrapper.connectedTo(remoteAddress)) {
			wrapper.writenf(msg);
			return;
		}
		super.send(remoteAddress, msg);
	}
	
	@Override
	public void close() {
		wrapper.close();
	}
	
	@Override
	public void quickClose() {
		wrapper.quickClose();
	}	
	
	@Override
	public void dirtyClose() {
		wrapper.dirtyClose();
	}	
	
	@Override
	void preCreated() {
		super.preCreated();
		wrapper.preCreated();
	}
	
	@Override
	void postEnding() {
		wrapper.postEnding();		
		super.postEnding();
	}
	
}
