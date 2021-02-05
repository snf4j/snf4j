package org.snf4j.core.session;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
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

	IFuture<Void> write(byte[] msg);
	
	IFuture<Void> write(byte[] msg, int offset, int length);
	
	IFuture<Void> write(byte[] msg, ImmutableSctpMessageInfo msgInfo);

	IFuture<Void> write(byte[] msg, int offset, int length, ImmutableSctpMessageInfo msgInfo);
	
	void writenf(byte[] msg);
	
	void writenf(byte[] msg, int offset, int length);
	
	void writenf(byte[] msg, ImmutableSctpMessageInfo msgInfo);
	
	void writenf(byte[] msg, int offset, int length, ImmutableSctpMessageInfo msgInfo);
	
	IFuture<Void> write(ByteBuffer msg);
	
	IFuture<Void> write(ByteBuffer msg, int length);
	
	IFuture<Void> write(ByteBuffer msg, ImmutableSctpMessageInfo msgInfo);
	
	IFuture<Void> write(ByteBuffer msg, int length, ImmutableSctpMessageInfo msgInfo);
	
	void writenf(ByteBuffer msg);
	
	void writenf(ByteBuffer msg, int length);
	
	void writenf(ByteBuffer msg, ImmutableSctpMessageInfo msgInfo);
	
	void writenf(ByteBuffer msg, int length, ImmutableSctpMessageInfo msgInfo);
	
	IFuture<Void> write(Object msg);
	
	IFuture<Void> write(Object msg, ImmutableSctpMessageInfo msgInfo);
	
	void writenf(Object msg);
	
	void writenf(Object msg, ImmutableSctpMessageInfo msgInfo);
	
}
