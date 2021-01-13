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

abstract class SessionChannelContext<T extends InternalSession> extends ChannelContext<T> {

	SessionChannelContext(T session) {
		super(session);
	}
	
	@Override
	abstract ChannelContext<T> wrap(InternalSession session);

	@Override
	final boolean isServer() {
		return false;
	}
	
	@Override
	final boolean isSession() {
		return true;
	}
	
	@Override
	final T getSession() {
		return context;
	}

	@Override
	final SelectableChannel accept(SelectableChannel channel) throws Exception {
		throw new UnsupportedOperationException("accept is not supported");
	}
	
	@Override
	final InternalSession create(SelectableChannel channel) throws Exception {
		throw new UnsupportedOperationException("create is not supported");
	}
	
}
