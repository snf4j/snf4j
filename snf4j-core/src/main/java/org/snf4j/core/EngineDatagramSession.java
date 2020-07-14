package org.snf4j.core;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

import org.snf4j.core.engine.IEngine;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.session.IEngineDatagramSession;

public class EngineDatagramSession extends DatagramSession implements IEngineDatagramSession {
	
	private final EngineDatagramWrapper wrapper;
	
	private final SocketAddress remoteAddress;
	
	public EngineDatagramSession(String name, IEngine engine, SocketAddress remoteAddress, IDatagramHandler handler, ILogger logger) {
		super(name, new EngineDatagramHandler(engine, remoteAddress, handler, logger));
		wrapper = new EngineDatagramWrapper(remoteAddress, (EngineDatagramHandler) this.handler);
		wrapper.setExecutor(handler.getFactory().getExecutor());	
		this.remoteAddress = remoteAddress;
	}
	
	public EngineDatagramSession(IEngine engine, SocketAddress remoteAddress, IDatagramHandler handler, ILogger logger) {
		this(null, engine, remoteAddress, handler, logger);
	}
	
	public EngineDatagramSession(String name, IEngine engine, IDatagramHandler handler, ILogger logger) {
		this(name, engine, null, handler, logger);
	}
	
	public EngineDatagramSession(IEngine engine, IDatagramHandler handler, ILogger logger) {
		this(engine, null, handler, logger);
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
	long superWrite(DatagramRecord record) {
		if (record.address == null) {
			record.address = remoteAddress;
		}
		return super.superWrite(record);
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
	public IDatagramHandler getHandler() {
		return wrapper.getHandler();
	}	
	
	@Override
	void exception(Throwable t) {
		if (isValid(EventType.EXCEPTION_CAUGHT)) {
			try {
				wrapper.exception(t);
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
		super.sendnf(remoteAddress, datagram);
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
		super.sendnf(remoteAddress, datagram, offset, length);
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
		super.sendnf(remoteAddress, datagram);
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
		super.sendnf(remoteAddress, datagram, length);
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
		super.sendnf(remoteAddress, msg);
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
