package org.snf4j.core;

import java.net.SocketAddress;

import javax.net.ssl.SSLEngine;

import org.snf4j.core.engine.IEngine;
import org.snf4j.core.factory.IDatagramHandlerFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.session.ISessionConfig;

public class DTLSServerHandler extends DatagramServerHandler {
	
	public DTLSServerHandler(IDatagramHandlerFactory handlerFactory) {
		super(handlerFactory);
	}
	
	public DTLSServerHandler(IDatagramHandlerFactory handlerFactory, ISessionConfig config) {
		super(handlerFactory, config);
	}
	
	public DTLSServerHandler(IDatagramHandlerFactory handlerFactory, ISessionConfig config, ISessionStructureFactory factory) {
		super(handlerFactory, config, factory);
	}
	
	@Override
	protected IEngine createEngine(SocketAddress remoteAddress, ISessionConfig config) throws Exception {
		SSLEngine engine = config.createSSLEngine(false);
		
		if (engine != null) {
			return new InternalSSLEngine(engine, config);
		}
		return null;
	}

}
