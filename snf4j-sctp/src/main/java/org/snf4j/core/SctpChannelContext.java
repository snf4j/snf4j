/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * -----------------------------------------------------------------------------
 */
package org.snf4j.core;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.Set;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.NotificationHandler;
import com.sun.nio.sctp.SctpChannel;

class SctpChannelContext extends AbstractSctpChannelContext<SctpSession> {

	SctpChannelContext(SctpSession session) {
		super(session);
	}
	
	@Override
	public boolean finishConnect(SelectableChannel channel) throws Exception {
		return ((SctpChannel)channel).finishConnect();
	}
	
	@Override
	final boolean completeRegistration(SelectorLoop loop, SelectionKey key, SelectableChannel channel) throws Exception {
		SctpChannel sc = (SctpChannel) channel;
		boolean open = sc.isOpen();
		
		if (open) {
			if (!sc.isConnectionPending()) {
				try {
					if (!sc.getRemoteAddresses().isEmpty()) {
						key.interestOps(SelectionKey.OP_READ);
						return true;
					}
				}
				catch (ClosedChannelException e) {
					open = false;
				}
			}
		}
			
		if (open) {
			key.interestOps(SelectionKey.OP_CONNECT);
		}
		else {
			//If the channel is closed notify session
			loop.fireCreatedEvent(getSession(), channel);
			loop.fireEndingEvent(getSession(), false);
		}			
		return false;
	}	
	
	@Override
	ChannelContext<SctpSession> wrap(InternalSession session) {
		return new SctpChannelContext((SctpSession) session);
	}

	@Override
	MessageInfo receive(SelectionKey key, ByteBuffer msg, final SctpSession session, NotificationHandler<InternalSctpSession> handler) throws Exception {
		return ((SctpChannel)key.channel()).receive(msg, session, HANDLER);
	}
	
	final void handleReading(final SelectorLoop loop, final SctpSession session, final SelectionKey key) {
		super.handleReading(loop, session, key);

		if (session.markedShutdown()) {
			if (loop.debugEnabled) {
				loop.logger.debug("Closing channel in {} after shutdown", session);
			}
			session.close(true);
		}
	}
	
	@Override
	int send(SelectionKey key, ByteBuffer msg, MessageInfo msgInfo) throws Exception {
		return ((SctpChannel)key.channel()).send(msg, msgInfo);
	}
	
	@Override
	final void shutdown(SelectableChannel channel) throws Exception {
		((SctpChannel)channel).shutdown();
	}
	
	private static boolean append(StringBuilder sb, Set<SocketAddress> addrs) {
		Iterator<SocketAddress> i = addrs.iterator();
		
		if (i.hasNext()) {
			sb.append(i.next());
			while (i.hasNext()) {
				sb.append(',');
				sb.append(i.next());
			}
			return true;
		}
		return false;
	}
	
	static String toString(SctpChannel channel) {
		StringBuilder sb = new StringBuilder(100);
		Set<SocketAddress> addrs;
		
		sb.append(channel.getClass().getName());
		sb.append('[');
		try {
			addrs = channel.getRemoteAddresses();
			if (!addrs.isEmpty()) {
				sb.append("connected ");
			}
			else if (channel.isConnectionPending()) {
				sb.append("connection-pending ");
			}
			else {
				sb.append("not-connected ");
			}
		} catch (IOException e) {
			addrs = null;
		}
		sb.append("local=");
		try {
			if (!append(sb, channel.getAllLocalAddresses())) {
				sb.append("not-bound");
			}
		} catch (IOException e) {
			sb.append("unknown");
		}
		if (addrs != null) {
			if (!addrs.isEmpty()) {
				sb.append(" remote=");
				append(sb, addrs);
			}
		}
		else {
			sb.append(" remote=unknown");
		}
		sb.append(']');
		return sb.toString();
	}
	
	@Override
	final String toString(SelectableChannel channel) {
		if (channel instanceof SctpChannel) {
			return toString((SctpChannel) channel);
		}
		return super.toString(channel);
	}

}
