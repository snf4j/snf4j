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

import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.util.Iterator;

import org.snf4j.core.factory.ISctpSessionFactory;

import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

class SctpServerChannelContext extends ServerChannelContext<ISctpSessionFactory> {

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
