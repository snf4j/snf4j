package org.snf4j.core;

import java.net.SocketAddress;

import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.core.session.SSLEngineCreateException;

public class DTLSSession extends EngineDatagramSession {
	
	private final static ILogger LOGGER = LoggerFactory.getLogger(DTLSSession.class);
	
	public DTLSSession(String name, SocketAddress remoteAddress, IDatagramHandler handler, boolean clientMode) throws SSLEngineCreateException {
		super(name, new InternalSSLEngine(remoteAddress, handler.getConfig(), clientMode), remoteAddress, handler, LOGGER);
	}
	
	public DTLSSession(SocketAddress remoteAddress, IDatagramHandler handler, boolean clientMode) throws SSLEngineCreateException {
		super(new InternalSSLEngine(remoteAddress, handler.getConfig(), clientMode), remoteAddress, handler, LOGGER);
	}
	
	public DTLSSession(String name, IDatagramHandler handler, boolean clientMode) throws SSLEngineCreateException {
		super(name, new InternalSSLEngine(null, handler.getConfig(), clientMode), handler, LOGGER);
	}
	
	public DTLSSession(IDatagramHandler handler, boolean clientMode) throws SSLEngineCreateException {
		super(new InternalSSLEngine(null, handler.getConfig(), clientMode), handler, LOGGER);
	}
}
