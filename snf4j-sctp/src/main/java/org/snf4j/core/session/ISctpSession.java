package org.snf4j.core.session;

import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.ISctpHandler;

public interface ISctpSession extends ISession {
	
	@Override
	ISctpHandler getHandler();

	@Override
	ISctpSession getParent();

	IFuture<Void> write(byte[] msg, SctpWriteInfo info);
	
	void writenf(byte[] msg, SctpWriteInfo info);
}
