package org.snf4j.example.earlydata;

import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.allocator.ThreadLocalCachingAllocator;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.example.echo.Logger;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.engine.IApplicationProtocolHandler;
import org.snf4j.tls.engine.IEarlyDataHandler;

public abstract class EarlyDataHandler extends AbstractStreamHandler implements IApplicationProtocolHandler, IEarlyDataHandler {
	
	private static final IByteBufferAllocator ALLOCATOR = new ThreadLocalCachingAllocator(true);
	
	protected DefaultSessionConfig config = new SessionConfig()
			.setOptimizeDataCopying(true)
			.setWaitForInboundCloseMessage(true);
	
	@Override
	public String selectApplicationProtocol(String[] offeredProtocols, String[] supportedProtocols) throws Alert {
		return null;
	}
	
	@Override
	public void selectedApplicationProtocol(String protocol) throws Alert {
		Logger.inf("protocol: " + protocol);
		getSession().getAttributes().put(SessionConfig.PROTOCOL, protocol);
		SessionConfig.updateCodecPipeline(
				getSession().getCodecPipeline(), 
				Integer.parseInt(protocol.substring(1)));
	}
	
	@Override
	public long getMaxEarlyDataSize() {
		return 1024;
	}

	@Override
	public boolean hasEarlyData() {
		return false;
	}

	@Override
	public byte[] nextEarlyData(String protocol) {
		return null;
	}
	
	@Override
	public void acceptedEarlyData() {
	}

	@Override
	public void rejectedEarlyData() {
	}

	@Override
	public void event(SessionEvent event) {
		Logger.inf(event.toString());
	}
	
	@Override
	public void exception(Throwable e) {
		Logger.err(e.toString());
		e.printStackTrace();
	}
	
	@Override
	public boolean incident(SessionIncident incident, Throwable t) {
		Logger.err(incident + ": " + t.toString());
		t.printStackTrace();
		return true;
	}

	@Override
	public ISessionConfig getConfig() {
		return config;
	}
	
	@Override
	public ISessionStructureFactory getFactory() {
		return new DefaultSessionStructureFactory() {
			
			@Override
			public IByteBufferAllocator getAllocator() {
				return ALLOCATOR;
			}
		};
	}
}
