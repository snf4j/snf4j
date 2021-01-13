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
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.snf4j.core.factory.IStreamSessionFactory;

class ServerSocketChannelContext extends ServerChannelContext<IStreamSessionFactory>{

	ServerSocketChannelContext(IStreamSessionFactory factory) {
		super(factory);
	}

	@Override
	final void postClose(SelectableChannel channel) {
		context.closed((ServerSocketChannel) channel);
	}
	
	@Override
	final void postRegistration(SelectableChannel channel) {
		context.registered((ServerSocketChannel) channel);
	}
	
	@Override
	final void exception(SelectableChannel channel, Throwable t) {
		context.exception((ServerSocketChannel) channel, t);
	}
	
	@Override
	final InternalSession create(SelectableChannel channel) throws Exception {
		return context.create((SocketChannel) channel);
	}

	@Override
	final SelectableChannel accept(SelectableChannel channel) throws Exception {
		return ((ServerSocketChannel) channel).accept();
	}	
	
	@Override
	final ChannelContext<StreamSession> wrap(InternalSession session) {
		return new SocketChannelContext((StreamSession) session);
	}
}
