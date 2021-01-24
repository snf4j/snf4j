package org.snf4j.core.factory;

import org.snf4j.core.SctpSession;

import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

public interface ISctpSessionFactory {

	SctpSession create(SctpChannel channel);
	
	void registered(SctpServerChannel channel);
	
	void closed(SctpServerChannel channel);
	
	void exception(SctpServerChannel channel, Throwable exception);
}
