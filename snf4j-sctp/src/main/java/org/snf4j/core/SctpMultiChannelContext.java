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

import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.NotificationHandler;
import com.sun.nio.sctp.SctpMultiChannel;

class SctpMultiChannelContext extends AbstractSctpChannelContext<SctpMultiSession> {

	SctpMultiChannelContext(SctpMultiSession session) {
		super(session);
	}
	
	@Override
	MessageInfo receive(SelectionKey key, ByteBuffer msg, SctpMultiSession session,
			NotificationHandler<InternalSctpSession> handler) throws Exception {
		return ((SctpMultiChannel)key.channel()).receive(msg, session, HANDLER);
	}

	@Override
	int send(SelectionKey key, ByteBuffer msg, MessageInfo msgInfo) throws Exception {
		return ((SctpMultiChannel)key.channel()).send(msg, msgInfo);
	}

	@Override
	ChannelContext<SctpMultiSession> wrap(InternalSession session) {
		return new SctpMultiChannelContext((SctpMultiSession) session);
	}

	@Override
	void shutdown(SelectableChannel channel) throws Exception {
		if (!context.shutdown((SctpMultiChannel) channel)) {
			context.closing = ClosingState.FINISHED;
			channel.close();
		}
	}

}
