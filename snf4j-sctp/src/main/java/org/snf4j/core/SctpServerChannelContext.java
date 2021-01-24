package org.snf4j.core;

import java.nio.channels.SelectableChannel;

import org.snf4j.core.factory.ISctpSessionFactory;

import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

public class SctpServerChannelContext extends ServerChannelContext<ISctpSessionFactory> {

	SctpServerChannelContext(ISctpSessionFactory factory) {
		super(factory);
	}

	@Override
	ChannelContext<SctpSession> wrap(InternalSession session) {
		return new SctpChannelContext((SctpSession) session);
	}

	@Override
	SelectableChannel accept(SelectableChannel channel) throws Exception {
		return ((SctpServerChannel) channel).accept();
	}

	@Override
	InternalSession create(SelectableChannel channel) throws Exception {
		return context.create((SctpChannel) channel);
	}
	
	@Override
	final void postClose(SelectableChannel channel) {
		context.closed((SctpServerChannel) channel);
	}
	
	@Override
	final void postRegistration(SelectableChannel channel) {
		context.registered((SctpServerChannel) channel);
	}
	
	@Override
	final void exception(SelectableChannel channel, Throwable t) {
		context.exception((SctpServerChannel) channel, t);
	}

}
