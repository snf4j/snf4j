package org.snf4j.core.session;

import java.net.SocketAddress;
import java.util.Set;

import org.snf4j.core.ImmutableSctpMessageInfo;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.ISctpHandler;

public interface ISctpSession extends ISession {
	
	@Override
	ISctpHandler getHandler();

	@Override
	ISctpSession getParent();
	
	Set<SocketAddress> getLocalAddresses();
	
	Set<SocketAddress> getRemoteAddresses();

	IFuture<Void> write(byte[] msg, ImmutableSctpMessageInfo msgInfo);
	
	void writenf(byte[] msg, ImmutableSctpMessageInfo msgInfo);
}
