package org.snf4j.core.session;

import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.ISctpHandler;

import com.sun.nio.sctp.MessageInfo;

public interface ISctpSession extends ISession {
	
	@Override
	ISctpHandler getHandler();

	@Override
	ISctpSession getParent();

	IFuture<Void> write(byte[] data, MessageInfo msgInfo);
	
	void writenf(byte[] data, MessageInfo msgInfo);
}
