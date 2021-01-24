package org.snf4j.core.factory;

import org.snf4j.core.SctpSession;
import org.snf4j.core.handler.ISctpHandler;

import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

abstract public class AbstractSctpSessionFactory implements ISctpSessionFactory {

	@Override
	public SctpSession create(SctpChannel channel) {
		return new SctpSession(createHandler(channel));
	}

	abstract protected ISctpHandler createHandler(SctpChannel channel);
	
	@Override
	public void registered(SctpServerChannel channel) {
	}

	@Override
	public void closed(SctpServerChannel channel) {
	}

	@Override
	public void exception(SctpServerChannel channel, Throwable exception) {
	}

}
