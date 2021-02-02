package org.snf4j.core;

import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.util.Iterator;

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

	@Override
	final String toString(SelectableChannel channel) {		
		if (channel instanceof SctpChannel) {
			return SctpChannelContext.toString((SctpChannel) channel);
		}
		else if (channel instanceof SctpServerChannel) {
			StringBuilder sb = new StringBuilder(100);
			
			sb.append(channel.getClass().getName());
			sb.append('[');
			try {
				Iterator<SocketAddress> i = ((SctpServerChannel)channel).getAllLocalAddresses().iterator();
				
				if (i.hasNext()) {
					sb.append(i.next());
					while (i.hasNext()) {
						sb.append(',');
						sb.append(i.next());
					}
				}
				else {
					sb.append("not-bound");
				}
			}
			catch (Exception e) {
				sb.append("unknown");
			}
			sb.append(']');
			return sb.toString();
		}
		return super.toString(channel);
	}
	
}
