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
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

class DatagramChannelContext extends SessionChannelContext<DatagramSession> {

	DatagramChannelContext(DatagramSession session) {
		super(session);
	}

	@Override
	final void close(SelectableChannel channel) throws IOException {		
		((DatagramChannel)channel).disconnect();
		super.close(channel);
	}
	
	@Override
	final String toString(SelectableChannel channel) {
		if (channel != null) {
			StringBuilder sb = new StringBuilder(100);
			
			sb.append(channel.getClass().getName());
			sb.append("[local=");
			try {
				sb.append(((DatagramChannel)channel).socket().getLocalSocketAddress().toString());
			}
			catch (Exception e) {
				sb.append("unknown");
			}
			if (((DatagramChannel)channel).isConnected()) {
				sb.append(",remote=");
				try {
					sb.append(((DatagramChannel)channel).socket().getRemoteSocketAddress().toString());
				}
				catch (Exception e) {
					sb.append("unknown");
				}
			}
			sb.append("]");
			return sb.toString();			
		}
		return super.toString(channel);
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
		if (doWrite) {
			int spinCount = context.maxWriteSpinCount;

			do {
				spinCount = loop.handleWriting(context, key, spinCount);
			} while (spinCount > 0 && key.isValid() && ((key.interestOps() & SelectionKey.OP_WRITE) != 0));
		}
	}	
	
	@Override
	final ChannelContext<DatagramSession> wrap(InternalSession session) {
		return new DatagramChannelContext((DatagramSession) session);
	}
	
	@Override
	final void shutdown(SelectableChannel channel) {
	}
	
	@Override
	final boolean exceptionOnDecodingFailure() {
		return false;
	}
	
}
