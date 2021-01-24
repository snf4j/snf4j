package org.snf4j.core;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;

import org.snf4j.core.factory.ISctpSessionFactory;
import org.snf4j.core.future.IFuture;

import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

public class Sctp {
	
	private Sctp() {
	}
	
	public static IFuture<Void> register(SelectorLoop loop, SctpChannel channel, SctpSession session)
			throws ClosedChannelException {
		if (session == null) {
			throw new IllegalArgumentException("session is null");
		}
		return loop.register(channel, 0, new SctpChannelContext(session));
	}
	
	public static IFuture<Void> register(SelectorLoop loop, SctpServerChannel channel, ISctpSessionFactory factory) 
			throws ClosedChannelException {
		if (factory == null) { 
			throw new IllegalArgumentException("factory is null");
		}		
		return loop.register(channel, SelectionKey.OP_ACCEPT, new SctpServerChannelContext(factory));
	}
}
