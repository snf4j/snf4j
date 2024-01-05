package org.snf4j.example.earlydata;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.snf4j.core.StreamSession;
import org.snf4j.core.factory.IStreamSessionFactory;
import org.snf4j.tls.TLSSession;
import org.snf4j.tls.engine.EngineHandlerBuilder;
import org.snf4j.tls.engine.EngineParameters;

public class SessionFactory implements IStreamSessionFactory {

	private final EngineParameters params;
	
	private final EngineHandlerBuilder builder;
	
	public SessionFactory(EngineParameters params, EngineHandlerBuilder builder) {
		this.params = params;
		this.builder = builder;
	}
	
	@Override
	public StreamSession create(SocketChannel channel) throws Exception {
		EarlyDataServerHandler handler = new EarlyDataServerHandler();
		
		return new TLSSession(params, builder.build(handler, handler), handler, false);
	}

	@Override
	public void registered(ServerSocketChannel channel) {
	}

	@Override
	public void closed(ServerSocketChannel channel) {
	}

	@Override
	public void exception(ServerSocketChannel channel, Throwable exception) {
	}

}
