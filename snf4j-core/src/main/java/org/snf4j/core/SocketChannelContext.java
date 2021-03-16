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

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

class SocketChannelContext extends SessionChannelContext<StreamSession> {

	SocketChannelContext(StreamSession session) {
		super(session);
	}
	
	@Override
	public boolean finishConnect(SelectableChannel channel) throws Exception {
		return ((SocketChannel)channel).finishConnect();
	}

	@Override
	final boolean completeRegistration(SelectorLoop loop, SelectionKey key, SelectableChannel channel) {
		SocketChannel sc = (SocketChannel) channel;

		if (sc.isConnected()) {
			key.interestOps(SelectionKey.OP_READ);
		}
		else if (sc.isConnectionPending() || sc.isOpen()) {
			key.interestOps(SelectionKey.OP_CONNECT);
			return false;
		}
		else {
			//If the channel is closed notify session
			loop.fireCreatedEvent(getSession(), channel);
			loop.fireEndingEvent(getSession(), false);
			return false;
		}
		return true;
	}	
	
	@Override
	final void handle(final SelectorLoop loop, final SelectionKey key) {	
		boolean doWrite = false;
		
		if (key.isReadable()) {
			loop.handleReading(context, key);
			doWrite = key.isValid() && ((key.interestOps() & SelectionKey.OP_WRITE) != 0);
		}
		else if (key.isWritable()) {
			doWrite = true;
		}	
		else if (key.isConnectable()) {
			loop.handleConnecting(context, key);
		}
		if (doWrite) {
			int spinCount = context.maxWriteSpinCount;
			
			do {
				spinCount = loop.handleWriting(context, key, spinCount);
			} while (spinCount > 0 && key.isValid() && ((key.interestOps() & SelectionKey.OP_WRITE) != 0));
		}
	}
	
	@Override
	final ChannelContext<StreamSession> wrap(InternalSession session) {
		return new SocketChannelContext((StreamSession) session);
	}
	
	@Override
	final void shutdown(SelectableChannel channel) throws Exception {
		((SocketChannel)channel).socket().shutdownOutput();	
	}
	
	@Override
	final boolean exceptionOnDecodingFailure() {
		return true;
	}
	
}
